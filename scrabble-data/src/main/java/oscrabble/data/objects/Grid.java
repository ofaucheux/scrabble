package oscrabble.data.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.exception.IllegalCoordinate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grid
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Grid.class);
	public static final int GRID_SIZE = 15;

	/**
	 * Arrays with 17 cases, as the first and last are border.
	 */
	private final Square[][] squares;

	public Grid()
	{
		this.squares = new Square[GRID_SIZE +2][];
		for (int x = 0; x < this.squares.length; x++)
		{
			this.squares[x] = new Square[GRID_SIZE + 2];
			for (int y = 0; y < this.squares.length; y++)
			{
				this.squares[x][y] = new Square(x, y);
			}
		}
	}

	public Square get(final Coordinate coordinate)
	{
		return get(coordinate.x, coordinate.y);
	}

	public Square get(final String coordinate)
	{
		final Coordinate c = getCoordinate(coordinate);
		return get(c);
	}

	/**
	 * (0-based coordinates)
	 * @param x
	 * @param y
	 * @return the square
	 */
	public Square get(int x, int y)
	{
		return this.squares[x][y];
	}

	/**
	 * @return if the complete grid is empty
	 */
	public boolean isEmpty()
	{
		for (final Square[] lines : this.squares)
		{
			for (final Square square : lines)
			{
				if (square.c != null)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isEmpty(final String coordinate) throws ScrabbleException.ForbiddenPlayException
	{
		final Coordinate triple = getCoordinate(coordinate);
		return this.get(triple).c == null;
	}

	public Collection<Square> getAllSquares()
	{
		final ArrayList<Square> list = new ArrayList<>();
		for (final Square[] line : this.squares)
		{
			Collections.addAll(list, line);
		}
		return list;
	}

	public int getSize()
	{
		return GRID_SIZE;
	}


	public void play(final String move) throws ScrabbleException.ForbiddenPlayException
	{
		play((Action.PlayTiles) Action.parse(move));
	}

	public void play(final Action.PlayTiles playTiles)
	{
		Grid.Square sq = get(playTiles.startSquare);
		for (int i = 0; i < playTiles.word.length(); i++)
		{
			final char c = playTiles.word.charAt(i);
			if (sq.isEmpty())
			{
				sq.c = c;
			}
			else if (Character.toUpperCase(sq.c) != Character.toUpperCase(c))
			{
				throw new AssertionError("The case is already occupied");
			}

			sq = sq.getNeighbour(playTiles.startSquare.direction, 1);
		}
	}

	/**
	 * @param coordinate coordinate of square
	 * @return the words which the square is part of.
	 */
	public Set<String> getWords(final String coordinate)
	{
		final Square origin = get(coordinate);
		if (origin.isEmpty())
		{
			return Collections.emptySet();
		}

		final LinkedHashSet<String> words = new LinkedHashSet<>();
		for (final Direction dir : Direction.values())
		{
			final StringBuffer sb = new StringBuffer();
			Square sq = origin;
			while (!(sq = sq.getPrevious(dir)).isBorder() && !sq.isEmpty())
			{
				sb.insert(0, sq.getLetter());
			}
			sb.append((sq = origin).getLetter());
			while (!(sq = sq.getNext(dir)).isBorder() && !sq.isEmpty())
			{
				sb.append(sq.getLetter());
			}
			if (sb.length() > 1)
			{
				words.add(sb.toString());
			}
		}
		return words;
	}

	/**
	 * A square
	 */
	public class Square
	{
		/**
		 * Values are 1-based: the case A1 has the coordinate (1,1). The case (1,0) exists, but is marked as border one.
		 */
		private final int x, y;

		public final boolean isBorder;

		public int letterBonus;

		public int wordBonus;


		/**
		 * Action, which has filled this field.
		 */
		public UUID action;

		public Character c;

		private Square(final int x, final int y)
		{
			this.x = x;
			this.y = y;
			this.isBorder = x == 0 || x == GRID_SIZE + 1
					|| y == 0 || y == GRID_SIZE + 1;

			final Bonus bonus = calculateBonus(x, y);
			this.wordBonus = bonus.wordFactor;
			this.letterBonus = bonus.charFactor;
		}

		public boolean isEmpty()
		{
			return this.c == null;
		}

		public Square getNeighbour(final Direction direction, int value)
		{
			return Grid.this.get(
					this.x + (direction == Direction.HORIZONTAL ? value : 0),
					this.y + (direction == Direction.VERTICAL ? value : 0)
			);
		}

		public boolean isBorder()
		{
			return this.x == 0 || this.x == Grid.GRID_SIZE + 1
					|| this.y == 0 || this.y == Grid.GRID_SIZE + 1;
		}

		public Set<Square> getNeighbours()
		{
			final Set<Square> neighbours = new HashSet<>(4);
			for (final Direction dir : Direction.values())
			{
				if (!isFirstOfLine(dir))
				{
					neighbours.add(getPrevious(dir));
				}
				if (!isLastOfLine(dir))
				{
					neighbours.add(getNext(dir));
				}
			}
			return neighbours;
		}

		public boolean isFirstOfLine(final Direction direction)
		{
			final int position = direction == Direction.HORIZONTAL ? this.x : this.y;
			return position == 0;
		}

		public Square getPrevious(final Direction direction)
		{
			return getNeighbour(direction, -1);
		}

		public Square getNext(final Direction direction)
		{
			return getNeighbour(direction, +1);
		}

		public boolean isLastOfLine(final Direction direction)
		{
			final int position = direction == Direction.HORIZONTAL ? this.x : this.y;
			return position == GRID_SIZE - 1;
		}

		oscrabble.data.Square toData()
		{
			final oscrabble.data.Square square = new oscrabble.data.Square();
			square.tile = this.c;
//			square.x =this.x;
//			square.y = this.y;
			return square;
		}

		@Override
		public String toString()
		{
			return getNotation(Direction.HORIZONTAL);
		}

		/**
		 *
		 * @return the uppercase character of the stone played on this square, {@code null} if no such.
		 */
		Character getLetter()
		{
			return this.c == null ? null : Character.toUpperCase(this.c);
		}

		public String getNotation(final Direction direction)
		{
			return Coordinate.getNotation(this, direction);
		}

		/**
		 *
		 * @return x-coordinate, 1-based
		 */
		public int getX()
		{
			return this.x;
		}

		/**
		 *
		 * @return y-coordinate, 1-based.
		 */
		public int getY()
		{
			return this.y;
		}
	}

	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("(\\w)(\\d+)");
	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("(\\d+)(\\w)");

	public static Coordinate getCoordinate(final String notation) throws IllegalCoordinate
	{
		final Direction direction;
		final int groupX, groupY;
		Matcher m;
		if ((m = HORIZONTAL_COORDINATE_PATTERN.matcher(notation)).matches()) // horizontal ist zuerst x, heißt: Buchstabe.
		{
			direction = Direction.HORIZONTAL;
			groupX = 1;
			groupY = 2;
		}
		else if ((m = VERTICAL_COORDINATE_PATTERN.matcher(notation)).matches())
		{
			direction = Direction.VERTICAL;
			groupX = 2;
			groupY = 1;
		}
		else
		{
			throw new IllegalCoordinate(notation);
		}

		final Coordinate c = new Coordinate();
		c.direction = direction;
		c.x = m.group(groupX).charAt(0) - 'A' + 1;
		c.y = Integer.parseInt(m.group(groupY));
		return c;
	}

	public Square getCentralSquare()
	{
		final int center = (int) Math.ceil(GRID_SIZE / 2f);
		return this.squares[center][center];
	}

	/**
	 * Liefert den Bonus einer Zelle.
	 * @param x {@code 0} for border
	 */
	private Bonus calculateBonus(final int x, final int y)
	{

		final int midColumn = GRID_SIZE / 2 + 1;

		if (x > midColumn)
		{
			return calculateBonus(GRID_SIZE - x +1, y);
		}
		else if (y > midColumn)
		{
			return calculateBonus(x, GRID_SIZE - y + 1);
		}

		if (x > y)
		{
			//noinspection SuspiciousNameCombination
			return calculateBonus(y, x);
		}

		if (x == 0 || y == 0)
		{
			return Bonus.BORDER;
		}

//		if (GRID_SIZE != SCRABBLE_SIZE)
//		{
//			return Bonus.NONE;
//		}
//
		assert 1 <= x && x <= y;

		if (x == y)
		{
			switch (x)
			{
				case 1:
					return Bonus.RED;
				case 6:
					return Bonus.DARK_BLUE;
				case 7:
					return Bonus.LIGHT_BLUE;
				default:
					return Bonus.ROSE;
			}
		}
		else if (x == 1 && y == midColumn)
		{
			return Bonus.RED;
		}
		else if (x == 1 && y == 4)
		{
			return Bonus.LIGHT_BLUE;
		}
		else if (x == 2 && y == 6)
		{
			return Bonus.DARK_BLUE;
		}
		else if (x == 3 && y == 7)
		{
			return Bonus.LIGHT_BLUE;
		}
		else if (x == 4 && y == 8)
		{
			return Bonus.LIGHT_BLUE;
		}
		else
		{
			return Bonus.NONE;
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("Grid{\n");
		for (final Square[] line : this.squares)
		{
			for (final Square square : line)
			{
				sb.append(square.isBorder ? '#' : square.c == null ? ' ' : square.c);
			}
			sb.append('\n');
		}
		sb.append("}");
		return sb.toString();
	}

	public oscrabble.data.Grid toData()
	{
		final oscrabble.data.Grid grid = new oscrabble.data.Grid();
		grid.squares = new ArrayList<>();
		for (final Square[] line : this.squares)
		{
			for (final Square square : line)
			{
				grid.squares.add(square.toData());
			}
		}
		return grid;
	}

	/**
	 * Definition der Bonus-Zellen.
	 */
	public enum Bonus
	{
		BORDER, NONE, RED(3, 1), ROSE(2, 1), DARK_BLUE(1, 3), LIGHT_BLUE(1, 2);

		private final int wordFactor;
		private final int charFactor;

		Bonus()
		{
			this(1, 1);
		}

		Bonus(int wordFactor, int charFactor)
		{
			this.wordFactor = wordFactor;
			this.charFactor = charFactor;
		}
	}

	/**
	 * Get the column nummer matching a letter, as in A8.
	 */
	public enum Direction
	{
		HORIZONTAL, VERTICAL;
		public Direction other()
		{
			return (this == HORIZONTAL ? VERTICAL : HORIZONTAL);
		}
	}

	public static class Coordinate
	{
		public Direction direction;
		public int x, y;

		public String getNotation()
		{
			return getNotation(this.x, this.y, this.direction);
		}

		// tODO: move
		public static String getNotation(final Grid.Square square, final Direction direction)
		{
			return getNotation(square.x, square.y, direction);
		}

		/**
		 * The coordinates are 0-based.
		 *
		 * @param x
		 * @param y
		 * @param direction
		 * @return
		 */
		private static String getNotation(final int x, final int y, final Direction direction)
		{
			String sx = Character.toString((char) ('A' + x-1));
			switch (direction)
			{
				case HORIZONTAL:
 					return sx + (y);
				case VERTICAL:
					return y + sx;
				default:
					throw new AssertionError();
			}
		}
	}
}


