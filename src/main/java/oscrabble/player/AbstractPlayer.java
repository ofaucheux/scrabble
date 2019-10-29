package oscrabble.player;

import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;

import java.util.UUID;

public abstract class AbstractPlayer
{
	private String name;

	protected UUID playerKey;

	protected AbstractPlayer(final String name)
	{
		this.name = name;
	}

	public final String getName()
	{
		return this.name;
	}

	public void setPlayerKey(final UUID playerKey)
	{
		this.playerKey = playerKey;
	}

	/**
	 * Sent to all players to indicate who now has to play.
	 */
	public abstract void onPlayRequired(AbstractPlayer currentPlayer);

	public abstract void onDictionaryChange();

	public abstract void onDispatchMessage(String msg);

	/**
	 * Send to all players after a player has played
	 */
	public abstract void afterPlay(IPlayerInfo info, IAction action, int score);

	public void beforeGameStart()
	{
	}

	public void afterGameEnd()
	{
	}

	public abstract boolean isObserver();

	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * @return {@code true} wenn manche Parameters editierbar sind.
	 */
	public boolean hasEditableParameters()
	{
		return false;
	}

	/**
	 * Gibt die Möglichkeit, die Parameters zu ändern. Es kann z.B. durch die Anzeige eines JPanels erfolgen.
	 */
	public void editParameters()
	{
		throw new AssertionError("Default implementation has no editable parameter");
	}

}
