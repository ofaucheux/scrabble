package oscrabble.client;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.log4j.Logger;
import oscrabble.*;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;
import oscrabble.server.ScrabbleServer;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwingClient extends AbstractPlayer
{
	private final static int CELL_SIZE = 40;
	public static final Logger LOGGER = Logger.getLogger(SwingClient.class);
	private static final Pattern PATTERN_EXCHANGE_COMMAND = Pattern.compile("-\\s*(.*)");

	private final JGrid jGrid;
	private final JTextField commandPrompt;
	private final ScrabbleServer server;
	private final JRack jRack;
	private final JScoreboard jScoreboard;

	private boolean isObserver;

	public SwingClient(final ScrabbleServer server, final String name)
	{
		super(name);
		this.server = server;

		this.jGrid = new JGrid(server.getGrid(), server.getDictionary());
		this.jRack = new JRack();
		this.jScoreboard = new JScoreboard();
		this.commandPrompt = new JTextField();
		final CommandPromptAction promptListener = new CommandPromptAction();
		this.commandPrompt.addActionListener(promptListener);
		this.commandPrompt.setFont(this.commandPrompt.getFont().deriveFont(20f));
		final AbstractDocument document = (AbstractDocument) this.commandPrompt.getDocument();
		document.addDocumentListener(promptListener);
		document.setDocumentFilter(UPPER_CASE_DOCUMENT_FILTER);
		display();
	}

	/**
	 *
	 */
	private void display()
	{
		final JFrame gridFrame = new JFrame();
		gridFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		gridFrame.setLayout(new BorderLayout());
		gridFrame.add(this.jGrid);

		final JButton butHelp = new JButton();
		final BruteForceMethod bruteForceMethod = new BruteForceMethod(this.server.getDictionary());
		butHelp.setAction(new AbstractAction("Help")
		{

			@Override
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					final ArrayList<Move> legalMoves = new ArrayList<>(
							bruteForceMethod.getLegalMoves(SwingClient.this.server.getGrid(),
									SwingClient.this.server.getRack(SwingClient.this, SwingClient.this.playerKey)));
					Move.sort(legalMoves, SwingClient.this.server.getGrid(), Grid.MoveMetaInformation.WORD_LENGTH_COMPARATOR.reversed());
					final ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
					final JList<Move> moveList = new JList<>(legalMoves.toArray(new Move[0]));
					sp.add(moveList);
					moveList.addListSelectionListener(event -> {
						Move move = null;
						for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++)
						{
							if (moveList.isSelectedIndex(i))
							{
								move = moveList.getModel().getElementAt(i);
								break;
							}
						}
						SwingClient.this.jGrid.setPreparedMove(move);

					});

					final JFrame jFrame = new JFrame("Moves");
					jFrame.add(sp);
					jFrame.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
					jFrame.setSize(new Dimension(200, 200));
					jFrame.setVisible(true);
					jFrame.pack();
				}
				catch (ScrabbleException e1)
				{
					e1.printStackTrace();
				}
			}
		});

		final JPanel eastPanel = new JPanel(new BorderLayout());
		final JPanel panel1 = new JPanel(new BorderLayout());
		panel1.add(this.jScoreboard, BorderLayout.NORTH);
		eastPanel.add(panel1, BorderLayout.CENTER);
		eastPanel.add(butHelp, BorderLayout.SOUTH);
		gridFrame.add(eastPanel, BorderLayout.LINE_END);

		gridFrame.add(this.commandPrompt, BorderLayout.SOUTH);
		gridFrame.pack();
		gridFrame.setResizable(false);
		gridFrame.setVisible(true);

		final Window rackFrame = new JDialog(gridFrame);
		rackFrame.setLayout(new BorderLayout());
		rackFrame.add(this.jRack);
		rackFrame.pack();
		rackFrame.setVisible(true);
		rackFrame.setLocation(
					gridFrame.getX() + gridFrame.getWidth(),
					gridFrame.getY() + gridFrame.getHeight() / 2
			);
		rackFrame.setFocusableWindowState(false);
		rackFrame.setFocusable(false);

		SwingUtilities.invokeLater(() -> {
			gridFrame.requestFocus();
			this.commandPrompt.requestFocusInWindow();
			this.commandPrompt.grabFocus();
		});
	}

	void setCommandPrompt(final String text)
	{
		this.commandPrompt.setText(text);
	}

	@Override
	public void onPlayRequired()
	{
		this.jRack.update();
	}

	@Override
	public void onDictionaryChange()
	{
		// nichts
	}

	@Override
	public void onDispatchMessage(final String msg)
	{
		JOptionPane.showMessageDialog(null, msg);
	}


	@Override
	public void afterPlay(final IPlayerInfo info, final IAction action, final int score)
	{
		this.jGrid.repaint();
		this.jScoreboard.refreshDisplay(info);
		if (info.getName().equals(this.getName()))
		{
			this.jRack.update();
		}
	}

	@Override
	public void beforeGameStart()
	{
		this.jScoreboard.prepareBoard();
		this.jRack.update();
	}

	@Override
	public boolean isObserver()
	{
		return this.isObserver;
	}

	private SwingClient setObserver()
	{
		this.isObserver = true;
		return this;
	}


	private static final Color STONE_BACKGROUND_COLOR = Color.decode("0xF3E5AB");
	private static final int ARC_WIDTH = 14;
	private static void drawStone(final Graphics2D g2,
								  final JComponent component,
								  final Stone stone,
								  final Color foregroundColor)
	{
		if (stone == null)
		{
			return;
		}

		g2.setPaint(STONE_BACKGROUND_COLOR);
		final Insets insets = component.getInsets();
		//noinspection SuspiciousNameCombination
		g2.fillRoundRect(
				insets.right,
				insets.top,
				component.getWidth() - insets.left - insets.right,
				component.getHeight() - insets.bottom - insets.top,
				ARC_WIDTH,
				ARC_WIDTH);

		if (stone.hasCharacterSet())
		{
			final float characterSize = getCharacterSize(component);

			// Draw the letter
			g2.setColor(stone.isJoker() ? Color.GRAY : foregroundColor);
			final Font font = g2.getFont().deriveFont(characterSize).deriveFont(Font.BOLD);
			g2.setFont(font);
			final String letter = Character.toString(stone.getChar());
			FontMetrics metrics = g2.getFontMetrics(font);
			int tx = (component.getWidth() - metrics.stringWidth(letter)) / 2;
			int ty = ((component.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
			g2.drawString(letter, tx, ty);


			// Draw the point-value
			if (stone.getPoints() != 0)
			{
				g2.setFont(font.deriveFont(characterSize * 10 / 18));
				metrics = g2.getFontMetrics(font);
				final String points = Integer.toString(stone.getPoints());
				int px = (component.getWidth() * 4 / 5) - (metrics.stringWidth(points) / 2);
				int py = (component.getHeight() * 3 / 4) - (metrics.getHeight() / 2) + metrics.getAscent() - 1;
				g2.drawString(points, px, py);
			}
		}
	}


	private static float getCharacterSize(final JComponent cell)
	{
		return cell.getWidth() * 18 / 32f;
	}


	private class JScoreboard extends JPanel
	{

		private final HashMap<String, JLabel> scoreLabels = new HashMap<>();

		JScoreboard()
		{
			setPreferredSize(new Dimension(200, 0));
			setLayout(new GridLayout(0, 2));
		}

		void refreshDisplay(final IPlayerInfo playerInfo)
		{
			this.scoreLabels.get(playerInfo.getName())
					.setText(playerInfo.getScore() + " pts");
		}

		void prepareBoard()
		{
			final List<IPlayerInfo> players = SwingClient.this.server.getPlayers();
			for (final IPlayerInfo player : players)
			{
				final String name = player.getName();
				final JLabel score = new JLabel();
				this.scoreLabels.put(name, score);
				add(new JLabel(name));
				add(score);
			}
			setPreferredSize(new Dimension(200, 50 * players.size()));
			getParent().validate();
		}
	}

	private class JRack extends JPanel
	{
		static final int RACK_SIZE = 7;
		final RackCell[] cells = new RackCell[7];

		private JRack()
		{
			this.setLayout(new GridLayout(1,7));
			for (int i = 0; i < RACK_SIZE; i++)
			{
				this.cells[i] = new RackCell();
				add(this.cells[i]);
			}
		}

		void update()
		{
			try
			{
				final ArrayList<Stone> stones = new ArrayList<>(
						SwingClient.this.server.getRack(SwingClient.this, SwingClient.this.playerKey));

				for (int i = 0; i < RACK_SIZE; i++)
				{
					this.cells[i].setStone(
							i >= stones.size() ? null : stones.get(i)
					);
				}
				this.repaint();
			}
			catch (ScrabbleException e)
			{
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
		}
	}

	/**
	 * Darstellung der Spielfläche
	 */
	public static class JGrid extends JPanel
	{
		private final HashMap<Grid.Square, MatteBorder> specialBorders = new HashMap<>();

		private final int numberOfRows;

		private final Grid grid;
		private final Dictionary dictionary;
		private final Map<Grid.Square, Stone> preparedMoveStones;

		final JComponent background;

		/** Spielfeld des Scrabbles */
		public JGrid(final Grid grid, final Dictionary dictionary)
		{
			this.grid = grid;
			this.numberOfRows = grid.getSize() + 2;
			this.dictionary = dictionary;

			this.setLayout(new BorderLayout());
			this.background = new JPanel();
			this.background.setLayout(new GridLayout(this.numberOfRows, this.numberOfRows));

			// Draw each Cell
			final int borderColumn = this.numberOfRows - 1;
			for (int y = 0; y < this.numberOfRows; y++)
			{
				for (int x = 0; x < this.numberOfRows; x++)
				{
					if (x == 0 || x == borderColumn)
					{
						this.background.add(new BorderCell(
								y == 0 || y == borderColumn ? "" : Integer.toString(y)));
					}
					else if (y == 0 || y == borderColumn)
					{
						this.background.add(new BorderCell(Character.toString((char) ((int) 'A' + x - 1))));
					}
					else
					{
						final StoneCell cell = new StoneCell(x, y);
						this.background.add(cell);

						final Color cellColor;
						switch (cell.square.getBonus())
						{
							case NONE:
								cellColor = Color.green.darker().darker();
								break;
							case BORDER:
								cellColor = Color.black;
								break;
							case LIGHT_BLUE:
								cellColor = Color.decode("0x00BFFF");
								break;
							case DARK_BLUE:
								cellColor = Color.blue;
								break;
							case RED:
								cellColor = Color.red;
								break;
							case ROSE:
								cellColor = Color.decode("#F6CEF5").darker();
								break;
							default:
								throw new AssertionError();
						}

						cell.setBackground(cellColor);
						cell.setOpaque(true);
						cell.setBorder(new LineBorder(Color.BLACK, 1));
					}
				}
			}
			final int size = this.numberOfRows * CELL_SIZE;
			this.setPreferredSize(new Dimension(size, size));
			this.add(this.background);
			this.preparedMoveStones = new LinkedHashMap<>();
		}

		void setPreparedMove(final Move move)
		{
			this.preparedMoveStones.clear();
			// Calculate the border for prepared word
			this.specialBorders.clear();
			if (move != null)
			{
				this.preparedMoveStones.putAll(move.getStones(this.grid, this.dictionary));
				final int INSET = 4;
				final Color preparedMoveColor = Color.RED;
				final boolean isHorizontal = move.getDirection() == Move.Direction.HORIZONTAL;
				final ArrayList<Grid.Square> squares = new ArrayList<>(this.preparedMoveStones.keySet());
				for (int i = 0; i < squares.size(); i++)
				{
					final int top = (isHorizontal || i == 0) ? INSET : 0;
					final int left = (!isHorizontal || i == 0) ? INSET : 0;
					final int bottom = (isHorizontal || i == squares.size() - 1) ? INSET : 0;
					final int right = (!isHorizontal || i == squares.size() - 1) ? INSET : 0;

					final MatteBorder border = new MatteBorder(
							top, left, bottom, right, preparedMoveColor
					);

					this.specialBorders.put(
							squares.get(i),
							border
					);

				}
			}

			repaint();
		}


		private JFrame descriptionFrame;
		private JTabbedPane descriptionTabPanel;

		/**
		 * Holt und zeigt die Definiton eines Wortes
		 * @param word Wort
		 */
		private void showDefinition(final String word)
		{
			if (this.descriptionFrame == null)
			{
				this.descriptionFrame = new JFrame("Description");
				this.descriptionFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				this.descriptionTabPanel = new JTabbedPane();
				this.descriptionFrame.add(this.descriptionTabPanel);
				this.descriptionFrame.setSize(600, 600);
				this.descriptionFrame.setLocationRelativeTo(null);
			}

			final Dictionary dictionary = this.grid.getDictionary();
			for (final String mutation : dictionary.getMutations(word))
			{
				final String description = dictionary.getDescription(mutation);
				if (description != null)
				{
					this.descriptionTabPanel.addTab(mutation, new JScrollPane(new JLabel(description)));
					this.descriptionFrame.setVisible(true);
				}
			}
		}

		class StoneCell extends JComponent
		{
			private final Grid.Square square;

			StoneCell(final int x, final int y)
			{
				this.square = JGrid.this.grid.getSquare(x , y );
				if (JGrid.this.grid.hasDictionary())
				{
					this.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseClicked(final MouseEvent e)
						{
							if (e.getButton() == 3)
							{
								for (final Move.Direction direction : Move.Direction.values())
								{
									final String word = JGrid.this.grid.getWord(StoneCell.this.square, direction);
									if (word != null)
									{
										showDefinition(word);
									}
								}
							}
						}
					});
				}
			}

			@Override
			protected void paintComponent(final Graphics g)
			{
				super.paintComponent(g);

				final Graphics2D g2 = (Graphics2D) g;
				final Insets insets = getInsets();

				// Wir erben direkt aus JComponent und müssen darum den Background selbst zeichnen
				if (isOpaque() && getBackground() != null)
				{
					g2.setPaint(getBackground());
					g2.fillRect(insets.right, insets.top, getWidth() - insets.left, getHeight() - insets.bottom);
				}

				Stone stone;
				if (this.square.stone != null)
				{
					drawStone(g2, this, this.square.stone, Color.black);
				}
				else if ((stone = JGrid.this.preparedMoveStones.get(this.square)) != null)
				{
					drawStone(g2, this, stone, Color.blue);
				}

				final MatteBorder specialBorder = JGrid.this.specialBorders.get(this.square);
				if (specialBorder != null)
				{
					specialBorder.paintBorder(
							this, g, 0, 0, getWidth(), getHeight()
					);
				}
			}

		}

		private class BorderCell extends JComponent
		{

			private final String label;

			BorderCell(final String label)
			{
				this.label = label;
			}

			@Override
			protected void paintComponent(final Graphics g)
			{
				super.paintComponent(g);

				final Graphics2D g2 = (Graphics2D) g;
				final Insets insets = getInsets();

				// Wir erben direkt aus JComponent und müssen darum den Background selbst zeichnen
				if (isOpaque() && getBackground() != null)
				{
					g2.setPaint(Color.lightGray);
					g2.fillRect(insets.right, insets.top, getWidth() - insets.left, getHeight() - insets.bottom);
				}

				// Draw the label
				g2.setColor(Color.BLACK);
				final Font font = g2.getFont().deriveFont(getCharacterSize(this)).deriveFont(Font.BOLD);
				g2.setFont(font);
				FontMetrics metrics = g.getFontMetrics(font);
				int tx = (getWidth() - metrics.stringWidth(this.label)) / 2;
				int ty = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
				g.drawString(this.label, tx, ty);
			}
		}
	}


	private class CommandPromptAction extends AbstractAction implements DocumentListener
	{

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final String command = SwingClient.this.commandPrompt.getText();
			if (command.isEmpty())
			{
				return;
			}

			final Move preparedMove;
			try
			{
				preparedMove = getPreparedMove();
				final Matcher m;
				if ((m = PATTERN_EXCHANGE_COMMAND.matcher(command)).matches())
				{
					final ArrayList<Character> chars = new ArrayList<>();
					for (final char c : m.group(1).toCharArray())
					{
						chars.add(c);
					}
					SwingClient.this.server.play(SwingClient.this, new Exchange(chars));
					SwingClient.this.commandPrompt.setText("");
				}
				else if (preparedMove == null)
				{
					onDispatchMessage("Not a coordinate: " + SwingClient.this.commandPrompt.getText());
				}
				else
				{
					SwingClient.this.server.play(SwingClient.this, preparedMove);
					SwingClient.this.commandPrompt.setText("");
				}
			}
			catch (JokerPlacementException e1)
			{
				onDispatchMessage("Cannot place the jokers: " + e1);
			}


		}

		private Move getPreparedMove() throws JokerPlacementException
		{
			String command = SwingClient.this.commandPrompt.getText();
			final StringBuilder sb = new StringBuilder();


			boolean joker = false;
			for (final char c : command.toCharArray())
			{
				if (c == '*')
				{
					joker = true;
				}
				else
				{
					sb.append(joker ? Character.toLowerCase(c) : c);
					joker = false;
				}
			}

			final Pattern playCommandPattern = Pattern.compile("(?:play\\s+)?(.*)", Pattern.CASE_INSENSITIVE);
			Matcher matcher;
			Move move;
			if ((matcher = playCommandPattern.matcher(sb.toString())).matches())
			{
				try
				{
					final Rack rack;
					try
					{
						rack = SwingClient.this.server.getRack(SwingClient.this, SwingClient.this.playerKey);
					}
					catch (ScrabbleException e)
					{
						throw new JokerPlacementException("Error", e);
					}
					final StringBuilder inputWord = new StringBuilder(matcher.group(1));
					move = Move.parseMove(SwingClient.this.server.getGrid(), inputWord.toString());

					//
					// Check if jokers are needed and try to position them
					//

					LOGGER.debug("Word before positioning jokers: " + move.word);
					int remainingJokers = rack.countJoker();
					final HashSetValuedHashMap<Character, Integer> requiredLetters = new HashSetValuedHashMap<>();
					int i = inputWord.indexOf(" ") + 1;
					for (final Map.Entry<Grid.Square, Character> square : move.getSquares().entrySet())
					{
						if (square.getKey().isEmpty())
						{
							if (Character.isLowerCase(inputWord.charAt(i)))
							{
								remainingJokers--;
							}
							else
							{
								requiredLetters.put(square.getValue(), i);
							}
						}
						i++;
					}

					for (final Character letter : requiredLetters.keys())
					{
						final int inRack = rack.countLetter(letter);
						final int required = requiredLetters.get(letter).size();
						final int missing = required - inRack;
						if (missing > 0)
						{
							if (remainingJokers < missing)
							{
								throw new JokerPlacementException("No enough jokers", null);
							}

							if (missing == required)
							{
								for (final Integer pos : requiredLetters.get(letter))
								{
									inputWord.replace(pos, pos + 1, Character.toString(Character.toLowerCase(letter)));
								}
								remainingJokers -= missing;
							}
							else
							{
								throw new JokerPlacementException(
										"Cannot place the jokers: several emplacement possible. Use the *A notation.",
										null);
							}
						}
					}
					move = Move.parseMove(SwingClient.this.server.getGrid(), inputWord.toString());
					LOGGER.debug("Word after having positioned white tiles: " + inputWord);

					SwingClient.this.jGrid.setPreparedMove(move);
				}
				catch (ParseException e)
				{
					return null;
				}
			}
			else
			{
				move = null;
			}
			return move;
		}


		@Override
		public void insertUpdate(final DocumentEvent e)
		{
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(final DocumentEvent e)
		{
			changedUpdate(e);
		}

		@Override
		public void changedUpdate(final DocumentEvent e)
		{
			try
			{
				final Move move = getPreparedMove();
				SwingClient.this.jGrid.setPreparedMove(move);
				SwingClient.this.jGrid.repaint();
			}
			catch (JokerPlacementException e1)
			{
				LOGGER.warn(e1.getMessage());
			}
		}
	}

	private class RackCell extends JComponent
	{
		private Stone stone;

		@Override
		protected void paintComponent(final Graphics g)
		{
			super.paintComponent(g);
			drawStone((Graphics2D) g, this, this.stone, Color.black);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(CELL_SIZE, CELL_SIZE);
		}

		public void setStone(final Stone stone)
		{
			this.stone = stone;
		}
	}

	/**
	 * Filter, das alles Eingetragene Uppercase schreibt
	 */
	private final static DocumentFilter UPPER_CASE_DOCUMENT_FILTER = new DocumentFilter()
	{
		public void insertString(DocumentFilter.FilterBypass fb, int offset,
								 String text, AttributeSet attr) throws BadLocationException
		{

			fb.insertString(offset, toUpperCase(text), attr);
		}

		public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
							String text, AttributeSet attrs) throws BadLocationException
		{

			fb.replace(offset, length, toUpperCase(text), attrs);
		}

		/**
		 * Entfernt die Accente und liefert alles Uppercase.
		 * TODO: für Frz. sinnvoll, für Deutsch aber sicherlich nicht..
		 */
		private String toUpperCase(String text)
		{
			text = Normalizer.normalize(text, Normalizer.Form.NFD);
			text = text.replaceAll("[^\\p{ASCII}]", "");
			text = text.replaceAll("\\p{M}", "");
			return text.toUpperCase();
		}
	};

	/**
	 * Problem while placing joker.
	 */
	private class JokerPlacementException extends Throwable
	{
		JokerPlacementException(final String message, final ScrabbleException e)
		{
			super(message, e);
		}
	}
}
