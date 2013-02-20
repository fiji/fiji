package archipelago.util;

import archipelago.FijiArchipelago;
import archipelago.listen.TransceiverExceptionListener;
import archipelago.network.MessageXC;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class XCErrorAdapter implements TransceiverExceptionListener
{

    private final HashSet<Class> throwablesSeenTX;
    private final HashSet<Class> throwablesSeenRX;

    private final AtomicBoolean isQuiet;

    public XCErrorAdapter()
    {
        throwablesSeenRX = new HashSet<Class>();
        throwablesSeenTX = new HashSet<Class>();
        isQuiet = new AtomicBoolean(false);
    }
    
    protected boolean handleCustomRX(final Throwable t, final MessageXC mxc)
    {
        return true;
    }

    protected boolean handleCustomTX(final Throwable t, final MessageXC mxc)
    {
        return true;
    }

    protected boolean handleCustom(final Throwable t, final MessageXC mxc)
    {
        return true;
    }

    public void report(final Throwable t, final String message,
                       final HashSet<Class> throwablesSeen)
    {
        FijiArchipelago.log(message);
        if (!throwablesSeen.contains(t.getClass()))
        {
            if (!isQuiet.get())
            {
                FijiArchipelago.err(message + "\nThis error dialog will only be shown once.");
            }
            throwablesSeen.add(t.getClass());
        }
    }
    
    protected void reportRX(final Throwable t, final String message, final MessageXC mxc)
    {
        report(t, "RX: " + mxc.getHostName() + ": " + message, throwablesSeenRX);
    }

    protected void reportTX(final Throwable t, final String message, final MessageXC mxc)
    {
        report(t, "TX: " + mxc.getHostName() + ": " + message, throwablesSeenTX);
    }

    public void handleRXThrowable(final Throwable t, final MessageXC mxc) {


        if (handleCustom(t, mxc) && handleCustomRX(t, mxc))
        {
            reportRX(t, t.toString(), mxc);
        }
    }

    public void handleTXThrowable(final Throwable t, final MessageXC mxc) {
        if (handleCustom(t, mxc) && handleCustomTX(t, mxc))
        {
            reportTX(t, t.toString(), mxc);
        }
    }
    
    public void silence()
    {
        silence(true);
    }
    
    public void silence(boolean s)
    {
        isQuiet.set(s);
    }
}
