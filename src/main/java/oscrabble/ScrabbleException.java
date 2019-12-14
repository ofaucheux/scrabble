package oscrabble;

import oscrabble.player.AbstractPlayer;

public abstract class ScrabbleException extends Exception
{

	public ScrabbleException(final String message)
	{
		this(message, null);
	}

	public ScrabbleException(final String message, final Throwable cause)
	{
		super(message, cause);
	}


	/**
	 * Exception reporting the use of an invalid secret.
	 */
	static public class InvalidSecretException extends ScrabbleException
	{
		public InvalidSecretException()
		{
			super("Invalid secret");
		}
	}

	/**
	 * Exception reporting a forbidden action.
	 */
	static public class ForbiddenPlayException extends ScrabbleException
	{
		public ForbiddenPlayException(final String message)
		{
			super(message);
		}
	}

	/**
	 * Exception reporting the use of a function at an time it is not valid to use it.
	 */
	static public class InvalidStateException extends ScrabbleException
	{
		public InvalidStateException(final String message)
		{
			super(message);
		}

	}

	/**
	 * Exception informing about the attempt of a player to play outside of his turn.
	 */
	public static class NotInTurn extends ScrabbleException
	{
		public NotInTurn(final AbstractPlayer player)
		{
			super("The player " + player.toString() + " is not in turn");
		}
	}
}
