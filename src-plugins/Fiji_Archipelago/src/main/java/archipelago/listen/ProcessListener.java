package archipelago.listen;

import archipelago.compute.ProcessManager;

/**
 * ProcessListener interface, used to notify when a ProcessManager has been returned from
 * the cluster.
 *
 * @author Larry Lindsey
 */
public interface ProcessListener
{
    /**
     * This method will be called when a ProcessManager is returned from the cluster.
     * @param process a ProcessManager that just returned from the cluster
     * @return true or not. This method may be a void return in the future.
     */
    public boolean processFinished(ProcessManager<?> process);
    
}
