package oscrabble.data;

import lombok.Builder;

/**
 * A tile
 */
@Builder
public class Tile
{
	public boolean isJoker;
	/** The character of a joker will be set when it is played */
	public Character c;
	/** The points of an joker are always 0 */
	public int points;
}
