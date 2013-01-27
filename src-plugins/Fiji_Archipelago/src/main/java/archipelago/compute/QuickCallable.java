package archipelago.compute;

public class QuickCallable<T> implements SerializableCallable<T>
{
    private final Runnable r;
    
    public QuickCallable(Runnable r)
    {
        this.r = r;
    }
    
    public T call() throws Exception
    {
        r.run();
        return null;
    }
}
