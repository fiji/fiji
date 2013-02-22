package archipelago.compute;

import archipelago.network.node.ClusterNode;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 *
 * @author Larry Lindsey
 */
public class ProcessManager<T> implements Runnable, Serializable
{
    
    private Callable<T> callable;
    private T output;
    private final long id;
    private Exception remoteException;
    private long runningOn;
    private final float numCores;
    private final boolean isFractional;
     
    
    //public <S extends Callable<T> & Serializable> ProcessManager(final S c, final ProcessListener pl, long idArg)
    
    public ProcessManager(final Callable<T> c, final long idArg, final float nc, final boolean f)
    {
        callable = c;
        output = null;
        id = idArg;
        remoteException = null;
        runningOn = -1;
        numCores = nc;
        isFractional = f;
    }

    /**
     * Runs this ProcessManager. This will typically be called on a remote node.
     */
    public void run()
    {
        try
        {
            output = callable.call();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            remoteException = e;
        }
        // Nullify the callable so we don't have to transfer extra data back home.
        callable = null;
    }

    public synchronized void setRunningOn(final ClusterNode node)
    {
        runningOn = node == null ? -1 : node.getID();
    }

    public long getRunningOn()
    {
        return runningOn;
    }
    
    public T getOutput()
    {
        return output;
    }
    
    public Callable<T> getCallable()
    {
        return callable;
    }

    public long getID()
    {
        return id;
    }
    
    public Exception getRemoteException()
    {
        return remoteException;
    }
    
    public int requestedCores(ClusterNode node)
    {
        int c;
        if (isFractional)
        {
            c = (int)(numCores * (float)node.getThreadLimit());
        }
        else
        {
            c = (int)numCores;
        }
        
        return c > 0 ? c : 1;
    }
    
}
