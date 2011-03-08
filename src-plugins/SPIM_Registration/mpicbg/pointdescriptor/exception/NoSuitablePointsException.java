package mpicbg.pointdescriptor.exception;

public class NoSuitablePointsException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoSuitablePointsException( final String message ) { super( message ); }

	public NoSuitablePointsException( final Object obj, final String message ) { super( obj.getClass().getCanonicalName() + ": " + message ); }

	public NoSuitablePointsException() { super(); }
	
}
