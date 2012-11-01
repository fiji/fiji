package archipelago;

public class SysoutLogger implements EasyLogger
{
    
    public synchronized void log(final String msg)
    {
        System.out.println(msg);
    }
}
