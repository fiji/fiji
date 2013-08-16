package algorithms;

/**
 * An exception class for missing preconditions for algorithm execution.
 */
public class MissingPreconditionException extends Exception{

	private static final long serialVersionUID = 1L;

	public MissingPreconditionException() {
		super();
	}

	public MissingPreconditionException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingPreconditionException(String message) {
		super(message);
	}

	public MissingPreconditionException(Throwable cause) {
		super(cause);
	}
}