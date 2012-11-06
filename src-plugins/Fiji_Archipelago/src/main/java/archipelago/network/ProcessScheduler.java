package archipelago.network;

import archipelago.FijiArchipelago;
import archipelago.compute.NullListener;
import archipelago.compute.ProcessListener;
import archipelago.compute.ProcessManager;

import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessScheduler extends Thread
{
    
    private final Vector<ClusterNode> nodes;
    private final ArrayBlockingQueue<ProcessManager> jobQueue;
    private final AtomicInteger pollTime;    
    private final AtomicBoolean running;

    public ProcessScheduler(int jobCapacity, int t)
    {
        nodes = new Vector<ClusterNode>();
        jobQueue = new ArrayBlockingQueue<ProcessManager>(jobCapacity);
        running = new AtomicBoolean(true);
        pollTime = new AtomicInteger(t);
        start();
    }
    
    public void setPollTimeMillis(int t)
    {
        pollTime.set(t);
    }
    
    public void addNode(ClusterNode node)
    {
        nodes.add(node);
    }
    
    public void addNodes(Collection<ClusterNode> nodesIn)
    {
        nodes.addAll(nodesIn);
    }
    
    public void removeNode(ClusterNode node)
    {
        nodes.remove(node);
    }
    
    public synchronized boolean queueJobs(Collection<ProcessManager> jobs)
    {
        if (jobs.size() <= jobQueue.remainingCapacity())
        {
            jobQueue.addAll(jobs);
            return true;
        }
        else
        {
            return false;
        }
    }
    

    private synchronized ClusterNode getFreeNode()
    {
        for (ClusterNode node : nodes)
        {
            if (node.numAvailableThreads() > 0)
            {
                return node;
            }
        }
        return null;
    }

    public void run()
    {
        while (running.get())
        {
            ProcessManager<?, ?> pm;
            ClusterNode node;
            
            FijiArchipelago.debug("Scheduler: Currently " + jobQueue.size() + " jobs in queue");
            FijiArchipelago.debug("Scheduler: Currently " + Cluster.getCluster().countReadyNodes() + " nodes ready");
            
            try
            {
                node = getFreeNode();
                if (node == null)
                {
                    FijiArchipelago.debug("Scheduler: No free nodes found. Sleeping.");
                    Thread.sleep(pollTime.get());
                }
                
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.err("Scheduler interrupted while sleeping");
                running.set(false);
                node = null;
            }

            if (node != null)
            {                
                try
                {
                    FijiArchipelago.debug("Scheduler waiting for job from queue");
                    pm = jobQueue.poll(pollTime.get(), TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException ie)
                {
                    FijiArchipelago.err("Scheduler interrupted while waiting for jobs");
                    running.set(false);
                    pm = null;
                }

                if (pm != null)
                {
                    ProcessListener listener = pm.getListener();
                    FijiArchipelago.log("Scheduling job on " + node.getHost());
                    node.runProcessManager(pm, listener == null ? NullListener.getNullListener() : listener);
                }
                else
                {
                    FijiArchipelago.debug("No job in queue for " + node.getHost());
                }
            }
            else
            {
                FijiArchipelago.debug("No available nodes found");
            }
        }
        FijiArchipelago.log("Scheduler exited");
    }
    
    public void close()
    {
        running.set(false);
        interrupt();
    }
    
}
