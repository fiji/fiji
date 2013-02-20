package archipelago.exception;

public class ShellExecutionException extends Exception
{
    public ShellExecutionException()
    {
        super();
    }
    
    public ShellExecutionException(final String message)
    {
        super(message);
    }

    public ShellExecutionException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
    
    public ShellExecutionException(final Throwable cause)
    {
        super(cause);
    }
}
