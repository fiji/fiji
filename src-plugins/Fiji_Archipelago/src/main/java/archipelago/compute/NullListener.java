package archipelago.compute;

/**
 * A ProcessListener that does nothing
 *
 * @author Larry Lindsey
 */
public class NullListener implements ProcessListener
{

    private static final NullListener listener = new NullListener();

    public static ProcessListener getNullListener()
    {
        return listener;
    }

    private NullListener(){}
    
    public boolean processFinished(ProcessManager<?, ?> process) {
        return true;
    }
}
