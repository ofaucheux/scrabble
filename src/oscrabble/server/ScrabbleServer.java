package oscrabble.server;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.log4j.Logger;
import oscrabble.*;
import oscrabble.client.Exchange;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class ScrabbleServer implements IScrabbleServer
{
	private final static Logger LOGGER = Logger.getLogger(ScrabbleServer.class);


	public final static int RACK_SIZE = 7;

	private final LinkedHashMap<AbstractPlayer, PlayerInfo> players = new LinkedHashMap<>();
	private final LinkedList<AbstractPlayer> toPlay = new LinkedList<>();
	private final Grid grid;
	private CountDownLatch waitingForPlay;
	final LinkedList<Stone> bag = new LinkedList<>();
	private final Dictionary dictionary;

	public ScrabbleServer(final Dictionary dictionary)
	{
		this.dictionary = dictionary;
		this.grid = new Grid(this.dictionary);
	}


	@Override
	public synchronized void register(final AbstractPlayer newPlayer)
	{
		final PlayerInfo info = new PlayerInfo();
		info.player = newPlayer;
		info.key = UUID.randomUUID();
		newPlayer.setPlayerKey(info.key);
		info.rack = new Rack();
		info.eventQueue = new LinkedBlockingDeque<>();
		info.dispatchThread = new Thread(() -> {
			try
			{
				while (true)
				{
					final ScrabbleEvent event = info.eventQueue.poll(Integer.MAX_VALUE, TimeUnit.DAYS);
					assert event != null;
					event.dispatch(info.player);
				}
			}
			catch (InterruptedException e)
			{
				reportError(info, e);
			}
		});
		this.players.put(newPlayer, info);

		info.dispatchThread.start();
		if (!newPlayer.isObserver())
		{
			this.toPlay.push(newPlayer);
		}

	}


	/**
	 * Report an error coming from a client
	 *
	 * @param info
	 * @param e
	 */
	private void reportError(final PlayerInfo info, final InterruptedException e)
	{
		// TODO
		System.out.println(e);
	}



	@Override
	public synchronized int play(final AbstractPlayer player, final IAction action)
	{
		assertIsCurrentlyPlaying(player);

		try
		{
			int score = 0;
			final PlayerInfo playerInfo = this.players.get(player);
			if (action instanceof Move)
			{
				final Move move = (Move) action;

				// check possibility
				final Grid.MoveMetaInformation moveMI = this.grid.getMetaInformation(move);
				final TreeBag<Character> rackLetters = new TreeBag<>();
				playerInfo.rack.forEach(stone -> {
					if (!stone.isWhiteStone())
					{
						rackLetters.add(stone.getChar(), 1);
					}
				});

				final Bag<Character> requiredLetters = moveMI.getRequiredLetters();
				final Bag<Character> missingLetters = new HashBag<>(requiredLetters);
				missingLetters.removeAll(rackLetters);
				if (missingLetters.size() > playerInfo.rack.getCountBlank())
				{
					final String details = "<html>Rack with " + rackLetters + "<br>has not the required stones " + requiredLetters;
					player.onDispatchMessage(details);
					throw new ScrabbleException(ScrabbleException.ERROR_CODE.MISSING_LETTER);
				}

				// position blanks for missing letters
				if (!missingLetters.isEmpty())
				{
					LOGGER.debug("Word before positioning white tiles: " + move.word);
					for (int i = 0; i < move.word.length(); i++)
					{
						final char c = move.word.charAt(i);
						if (Character.isUpperCase(c) && missingLetters.contains(c))
						{
							if (missingLetters.getCount(c) == requiredLetters.getCount(c))
							{
								move.word = move.word.replaceAll(Character.toString(c), Character.toString(Character.toLowerCase(c)));
							}
							else
							{
								throw new ScrabbleException(ScrabbleException.ERROR_CODE.WHITE_POSITION_REQUIRED);
							}
						}
					}
					LOGGER.debug("Word after having positioned white tiles: " + move.word);
				}

				// check dictionary
				final Set<String> toTest = new LinkedHashSet<>();
				toTest.add(move.word);
				toTest.addAll(moveMI.crosswords);
				for (final String crossword : toTest)
				{
					if (!this.dictionary.containUpperCaseWord(crossword.toUpperCase()))
					{
						final String details = "Word \"" + crossword + "\" is not allowed";
						player.onDispatchMessage(details);
						throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN, details);
					}
				}

				if (this.grid.isEmpty())
				{
					final Grid.Square center = this.grid.getCenter();
					Grid.Square square = move.startSquare;
					boolean onCenter = false;
					for (int i = 0; i < move.word.length(); i++)
					{
						if (square == center)
						{
							onCenter = true;
							break;
						}
						square = square.getFollowing(move.getDirection());
					}
					if (!onCenter)
					{
						throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN,
								"The first word must be on the center square");
					}

				}

				Grid.Square square = move.startSquare;
				for (int i = 0; i < move.word.length(); i++)
				{
					final char c = move.word.charAt(i);
					if (square.isEmpty())
					{
						final Stone stone =
								playerInfo.rack.removeStones(
										Collections.singletonList(move.isPlayedByBlank(i) ? ' ' : c)
								).get(0);
						if (stone.isWhiteStone())
						{
							stone.setCharacter(c);
						}
						this.grid.set(square, stone);
					}
					else
					{
						assert square.stone.getChar() == c; //  sollte schon oben getestet sein.
					}
					square = square.getFollowing(move.getDirection());
				}

				score = moveMI.getScore();
				playerInfo.score += score;
			}
			else if (action instanceof Exchange)
			{
				final List<Stone> stones1 = playerInfo.rack.removeStones(((Exchange) action).getChars());
				this.bag.addAll(stones1);
				Collections.shuffle(this.bag);
			}
			else
			{
				throw new AssertionError("Command not treated: " + action);
			}

			refillRack(player);
			dispatch(toInform -> toInform.afterPlay(playerInfo, action, 0));

			this.waitingForPlay.countDown();

			LOGGER.debug("Grid after play\n" + this.grid.asASCIIArt());

			return score;
		}
		catch (final ScrabbleException e)
		{
			if (e.acceptRetry())
			{
				player.onDispatchMessage(e.toString());
				this.toPlay.addFirst(player);
				return 0;
			}
			else
			{
				dispatch(p -> p.onDispatchMessage(
						"Player " + player + " would have play an illegal move: " + e + ". Skip its turn"));
				return 0;
			}
		}
		finally
		{
			this.toPlay.pop();
			this.toPlay.add(player);
		}
	}


	private void assertIsCurrentlyPlaying(final AbstractPlayer player)
	{
		if (player != this.toPlay.getFirst())
		{
			throw new IllegalStateException("The player " + player.toString() + " is not playing");
		}
	}

	@Override
	public List<IPlayerInfo> getPlayers()
	{
		return List.copyOf(this.players.values());
	}


	private void refillRack(final AbstractPlayer player)
	{
		final Rack rack = this.players.get(player).rack;
		while (!this.bag.isEmpty() && rack.size() < RACK_SIZE)
		{
			final Stone stone = this.bag.poll();
			this.bag.remove(stone);
			rack.add(stone);
		}
	}

	@Override
	public void markAsIllegal(final String word)
	{
		this.getDictionary().markAsIllegal(word);
		dispatch(AbstractPlayer::onDictionaryChange);
	}

	public void startGame()
	{
		assert !this.toPlay.isEmpty();

		fillBag();
		sortPlayers();

		// Fill racks
		for (final AbstractPlayer player : this.toPlay)
		{
			refillRack(player);
		}

		dispatch(player -> player.beforeGameStart());


		try
		{
			while (!this.bag.isEmpty())  // TODO: andere Möglichkeiten fürs Ende des Spieles
			{
				final AbstractPlayer player = this.toPlay.peekFirst();
				LOGGER.info("Let's play " + player);
				this.waitingForPlay = new CountDownLatch(1);
				player.onPlayRequired();
				if (this.waitingForPlay.await(1, TimeUnit.MINUTES))
				{
					// OK
				}
				else
				{
					// TODO: timeout
				}
				Thread.sleep(500);
			}
		}
		catch (InterruptedException e)
		{
			LOGGER.error(e, e);
		}

		informClients("done!");
	}

	/**
	 * Sortiert (oder mischt) die Spieler, um eine Spielreihenfolge zu definieren.
	 */
	protected void sortPlayers()
	{
		// select the player playing the first
		Collections.shuffle(this.toPlay);
	}

	/**
	 * Füllt das Säckchen mit den Buchstaben.
	 */
	void fillBag()
	{
		// Fill bag
		final Dictionary dictionary = this.getDictionary();
		for (final Dictionary.Letter letter : dictionary.getLetters())
		{
			for (int i = 0; i < letter.prevalence; i++)
			{
				this.bag.add(dictionary.generateStone(letter.c));
			}
		}
		for (int i = 0; i < dictionary.getNumberBlanks(); i++)
		{
			this.bag.add(dictionary.generateStone(null));
		}
		Collections.shuffle(this.bag);
	}

	/**
	 *
	 * @param message
	 */
	private void informClients(final String message)
	{
		dispatch(player -> player.onDispatchMessage(message));
	}

	/**
	 *
	 */
	private void dispatch(final ScrabbleEvent scrabbleEvent)
	{
		for (final AbstractPlayer player : this.players.keySet())
		{
			scrabbleEvent.dispatch(player);
		}
	}

	@Override
	public Dictionary getDictionary()
	{
		return this.dictionary;
	}

	@Override
	public Grid getGrid()
	{
		return this.grid;
	}


	@Override
	public Rack getRack(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException
	{
		checkKey(player, clientKey);
		if (player.isObserver())
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.PLAYER_IS_OBSERVER);
		}
		return this.players.get(player).rack.copy();
	}

	@Override
	public int getScore(final AbstractPlayer player)
	{
		return this.players.get(player).getScore();
	}

	private void checkKey(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException
	{
		if (clientKey == null || !clientKey.equals(this.players.get(player).key))
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.NOT_IDENTIFIED);
		}
	}

	private static class PlayerInfo implements IPlayerInfo
	{
		AbstractPlayer player;

		/**
		 * Password für die Kommunikation Player &lt;&gt; Server
		 */
 		UUID key;
 		BlockingQueue<ScrabbleEvent> eventQueue;
		Thread dispatchThread;
		Rack rack;
		int score;

		@Override
		public String getName()
		{
			return this.player.getName();
		}

		@Override
		public int getScore()
		{
			return this.score;
		}
	}


	public interface ScrabbleEvent
	{
		void dispatch(final AbstractPlayer player);
	}



}
