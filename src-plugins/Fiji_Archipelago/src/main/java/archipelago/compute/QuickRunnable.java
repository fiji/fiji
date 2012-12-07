package archipelago.compute;

import java.util.concurrent.Callable;

public class QuickRunnable implements Runnable
{
    
    private final Callable callable;
    private Object result;
    private Exception exception;
    
    public QuickRunnable(Callable c)
    {
        callable = c;
        result = null;
        exception = null;
    }
    
    public void run()
    {
        try
        {
            result = callable.call();
        }
        catch (Exception e)
        {
            exception = e;
        }
    }
    
    public Object getResult()
    {
        return result;
    }
    
    public Exception getException()
    {
        return exception;
    }
}
