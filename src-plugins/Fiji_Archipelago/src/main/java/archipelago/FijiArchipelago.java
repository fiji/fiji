package archipelago;


import archipelago.network.Cluster;
import archipelago.network.shell.NodeShell;

import java.util.concurrent.atomic.AtomicLong;

public final class FijiArchipelago
{
    public static final String PREF_ROOT = "FijiArchipelago";
    private static EasyLogger logger = new NullLogger();
    private static EasyLogger errorLogger = new NullLogger();
    private static EasyLogger debugLogger = new NullLogger();
    private static final AtomicLong nextID = new AtomicLong(0);

    private FijiArchipelago(){}



    
    
    public static synchronized void setInfoLogger(final EasyLogger l)
    {
        logger = l;
    }
    
    public static synchronized void setErrorLogger(final EasyLogger l)
    {
        errorLogger = l;
    }

    public static synchronized void setDebugLogger(final EasyLogger l)
    {
        debugLogger = l;
    }
    
    public static synchronized void log(final String s)
    {
        logger.log(s);
    }
    
    public static synchronized void err(final String s)
    {
        errorLogger.log(s);
    }
    
    public static synchronized void debug(final String s)
    {
        debugLogger.log(s);
    }

    public static synchronized long getUniqueID()
    {
        return nextID.incrementAndGet();
    }
}
