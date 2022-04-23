package oscrabble.client;

import oscrabble.ScrabbleException;
import oscrabble.client.utils.I18N;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.GameState;
import oscrabble.data.IDictionary;
import oscrabble.data.Score;
import oscrabble.data.objects.Grid;
import oscrabble.player.ai.BruteForceMethod;
import oscrabble.player.ai.Strategy;

import java.util.*;

public abstract class AbstractPossibleMoveDisplayer {

	protected static final Strategy DO_NOT_DISPLAY_STRATEGIE = new Strategy() {
		@Override
		public TreeMap<Integer, List<String>> sort(final Set<String> moves) {
			throw new AssertionError("Should not be called");
		}
	};

	protected final Set<PossibleMoveDisplayer.AttributeChangeListener> attributeChangeListeners = new HashSet<>();

	/**
	 * The server to calculate the scores
	 */
	protected ScrabbleServerInterface server;

	private final BruteForceMethod bfm;

	protected AbstractPossibleMoveDisplayer(final IDictionary dictionary) {
		this.bfm = new BruteForceMethod(dictionary);
	}

	/**
	 * the game
	 */
	protected UUID game;

	protected List<Character> rack;

	protected GameState state;

	protected LinkedHashMap<Strategy, String> getStrategyList() {
		final Strategy.BestScore bestScore = new Strategy.BestScore(null, null);
		this.attributeChangeListeners.add((fieldName, newValue) -> {
			switch (fieldName) {
				case "game": //NON-NLS
					bestScore.setGame((UUID) newValue);
					break;
				case "server":
					bestScore.setServer(((ScrabbleServerInterface) newValue));
					break;
			}
		});

		final LinkedHashMap<Strategy, String> orderStrategies = new LinkedHashMap<>();
		orderStrategies.put(DO_NOT_DISPLAY_STRATEGIE, I18N.get("nothing"));
		orderStrategies.put(bestScore, I18N.get("best.scores"));
		orderStrategies.put(new Strategy.BestSize(), I18N.get("best.sizes"));
		return orderStrategies;
	}

	/**
	 * Set the server this displayer is for. This information is transferred to the subcomponents too.
	 *
	 * @param server
	 */
	void setServer(final ScrabbleServerInterface server) {
		this.server = server;
		invokeListeners("server", server);
	}

	/**
	 * Set the game this displayer is for. This information is transferred to the under-components too.
	 *
	 * @param game
	 */
	public void setGame(final UUID game) {
		this.game = game;
		invokeListeners("game", game); //NON-NLS
	}

	public synchronized void setData(final GameState state, final List<Character> rack) {
		this.state = state;
		this.rack = rack;
	}

	protected void invokeListeners(final String fieldName, final Object newValue) {
		this.attributeChangeListeners.forEach(l -> l.onChange(fieldName, newValue));
	}

	protected abstract Strategy getSelectedStrategy();

	public synchronized void refresh() {
		Strategy selectedOrderStrategy = getSelectedStrategy();

		if (selectedOrderStrategy == DO_NOT_DISPLAY_STRATEGIE) {
			setListData(Collections.emptyList());
			return;
		}

		if (!this.state.gameId.equals(this.game)) {
			throw new IllegalArgumentException("GameId is " + this.state.gameId + ", expected was " + this.game);
		}

		final Collection<Score> scores;
		if (selectedOrderStrategy == null) {
			scores = Collections.emptyList();
		} else {
			this.bfm.setGrid(Grid.fromData(this.state.grid));
			final ArrayList<String> words = new ArrayList<>();
			for (final List<String> subWords : selectedOrderStrategy.sort(this.bfm.getLegalMoves(this.rack)).values()) {
				words.addAll(0, subWords);
			}
			try {
				scores = this.server.getScores(this.game, words);
			} catch (ScrabbleException e) {
				throw new Error(e);
			}
		}
		setListData(new Vector<>(scores));
	}

	protected abstract void setListData(Collection<Score> scores);

	public interface AttributeChangeListener {
		void onChange(String fieldName, Object newValue);
	}
}
