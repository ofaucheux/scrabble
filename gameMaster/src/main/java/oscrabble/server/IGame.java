package oscrabble.server;

import oscrabble.ScrabbleException;
import oscrabble.data.IDictionary;
import oscrabble.data.MessageFromServer;
import oscrabble.data.Player;
import oscrabble.server.action.Action;

import java.util.List;

public interface IGame
{

	/**
	 * Registriert einen neuen Spieler.
	 * @param player der Spieler
	 */
	void addPlayer(Player player);

//	/**
//	 * Register a listener.
//	 * @param listener listener to register
//	 */
//	void addListener(Game.GameListener listener);
//
	/**
	 * Play an action. The player must call this function to inform the server of the action he plays.
//	 * @param clientKey key of the client
	 * @param action action to be done
	 * @return score score of this play
	 */
	int play(/* TODO UUID clientKey */ Action action) throws ScrabbleException.NotInTurn, ScrabbleException.InvalidSecretException, ScrabbleException.NotInTurn, oscrabble.data.ScrabbleException;

	/**
	 *
	 * @return den Spiel, der am Ball ist.
	 */
	Game.Player getPlayerToPlay();

	List<IPlayerInfo> getPlayers();

	/**
	 * @return the history of the game.
	 */
	Iterable<Game.HistoryEntry> getHistory();

	IDictionary getDictionary();

//	/**
//	 * Editiere die Parameters eines Spielers.
//	 *
//	 * @param caller UUID des Players, der die Funktion aufruft
//	 * @param player anderer Player
//	 */
//	void editParameters(UUID caller, IPlayerInfo player);

	/**
	 * Send a message to all players.
	 *
	 * @param message message
	 */
	void sendMessage(MessageFromServer message);

//	/** TODO?
//	 * Inform the server that a player leaves the game. This can lead to the end of game, dependently
//	 * of the implementation.
//	 *
//	 * @param player leaving player
//	 * @param key key of player, for identification
//	 * @param message human readable message to transmit, if any.
//	 */
//	void quit(AbstractPlayer player, UUID key, String message) throws ScrabbleException;
//

//	/** TODO
//	 *
//	 * @return the configuration object of the game. Never null.
//	 */
//	Configuration getConfiguration();

	// TODO?
//	/**
//	 * Inform the server about the configuration change of a client.
//	 *
//	 * @param player the player which configuration has changed
//	 * @param playerKey key of the client
//	 */
//	void playerConfigHasChanged(AbstractPlayer player, UUID playerKey);

//	TODO: smtg like "getRules?"
//	 /**
//	 * An exchange of tiles is forbidden, if the number of remaining tiles is strictly smaller as this minimum.
//	 *
//	 * @return the minimum
//	 */
//	int getRequiredTilesInBagForExchange();

//	/**
//	 *
//	 * @param tile a set tile
//	 * @return the action which set the tile
//	 */
//	Action getSettingAction(Tile tile);

}
