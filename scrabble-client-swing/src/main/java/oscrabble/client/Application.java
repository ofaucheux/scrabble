package oscrabble.client;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.Data;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.ui.ConnectionParameterPanel;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.IDictionary;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;
import oscrabble.server.Server;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;

@SuppressWarnings("BusyWait")
public class Application {
	public static final Logger LOGGER = LoggerFactory.getLogger(Thread.class);
	private static final Random RANDOM = new Random();

	private final IDictionary dictionary;
	private final ScrabbleServerInterface server;
	private final Properties properties;

	public Application(final IDictionary dictionary, final ScrabbleServerInterface server, final Properties properties) {
		this.dictionary = dictionary;
		this.server = server;
		this.properties = properties;
	}

	public static void main(String[] unused) throws InterruptedException, IOException, ScrabbleException {

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("uncaught.exception", e));

		initializeLogging();

		//
		// load the properties
		//

		final Properties properties = new Properties();
		try (final InputStream resource = Application.class.getResourceAsStream("client.properties")) {
			properties.load(resource);
		}

		//
		// select the server
		//

		final ConnectionParameters connectionParameters = new ConnectionParameters();
		JOptionPane.showMessageDialog(
				null,
				new ConnectionParameterPanel(connectionParameters)
		);

		final IDictionary dictionary = Dictionary.getDictionary(Language.FRENCH);
		final ScrabbleServerInterface server   // todo new MicroServiceScrabbleServer(connectionParameters.serverName, connectionParameters.serverPort);
				;
		server = connectionParameters.localServer
				? new Server()
				: null; // todo

		//
		// start the application
		//

		final Application application = new Application(dictionary, server, properties);
		application.playGame();
	}

	/**
	 *
	 */
	private static void initializeLogging() {
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(Application.class.getResourceAsStream("/logback.xml")); //NON-NLS
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	@SneakyThrows
	static ArrayList<String> getFrenchFirstNames() {
		final ArrayList<String> names = new ArrayList<>();
		final BufferedReader is = new BufferedReader(new InputStreamReader(
				Application.class.getResourceAsStream("frenchFirstNames.txt"),
				StandardCharsets.UTF_8
		));
		String line;
		while ((line = is.readLine()) != null) {
			names.add(line);
		}
		return names;
	}

	/**
	 * Prepare the server and the client, start the game and play it till it ends.
	 * @throws InterruptedException
	 */
	private void playGame() throws InterruptedException, ScrabbleException {

		final UUID game = this.server.newGame();
		final String humanName = System.getProperty("user.name");
		final UUID humanPlayer = this.server.addPlayer(game, humanName);

		final List<String> names = getFrenchFirstNames();
		names.remove(humanName);
		final HashSet<AIPlayer> aiPlayers = new HashSet<>();
		for (int i = 0; i < (Integer.parseInt((String) this.properties.get("players.number"))) - 1; i++) {
			final String aiPlayerName = names.get(RANDOM.nextInt(names.size()));
			names.remove(aiPlayerName);
			final UUID aiPlayerId = this.server.addPlayer(game, aiPlayerName);
			// TODO: tell the server it is an AI Player
			final AIPlayer ai = new AIPlayer(new BruteForceMethod(this.dictionary), game, aiPlayerId, this.server);
			ai.setThrottle(Duration.ofSeconds(1));
			ai.startDaemonThread();
			aiPlayers.add(ai);
		}

		final Client client = new Client(this.server, this.dictionary, game, humanPlayer);
		client.setAIPlayers(aiPlayers);
		client.displayAll();
		this.server.startGame(game);

		do {
			Thread.sleep(500);
		} while (client.isVisible());
		this.server.attach(game, humanPlayer, true);
	}

	@Data
	public static class ConnectionParameters {
		int serverPort;
		String serverName;
		private boolean localServer;
	}
}
