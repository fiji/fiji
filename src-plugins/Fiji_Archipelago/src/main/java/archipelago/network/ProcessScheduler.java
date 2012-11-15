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

    public void start()
    {
        if (!running.get())
        {
            running.set(true);
            super.start();
        }
    }
    
    public void run()
    {
        FijiArchipelago.log("Scheduler: Started. Running flag: " + running.get());

        while (running.get())
        {
            ProcessManager<?, ?> pm;
            ClusterNode node;

            try
            {
                node = getFreeNode();
                if (node == null)
                {
                    Thread.sleep(pollTime.get());
                }
                
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.log("Scheduler interrupted while sleeping, stopping.");
                running.set(false);
                node = null;
            }

            if (node != null)
            {

                try
                {
                    pm = jobQueue.poll(pollTime.get(), TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException ie)
                {
                    FijiArchipelago.log("Scheduler interrupted while waiting for jobs, stopping.");
                    running.set(false);
                    pm = null;
                }

                if (pm != null)
                {
                    ProcessListener listener = pm.getListener();
                    FijiArchipelago.log("Scheduling job on " + node.getHost());
                    FijiArchipelago.log("There are now " + jobQueue.size() + " jobs in queue");
                    //TODO: handle failed jobs, requeue jobs from nodes that halt unexpectedly
                    node.runProcessManager(pm,
                            listener == null ? NullListener.getNullListener() : listener);
                }
            }
        }
        FijiArchipelago.log("Scheduler exited");
    }
    
    public void close()
    {
        if (running.get())
        {
            running.set(false);
            interrupt();
            jobQueue.clear();
            nodes.clear();
        }
    }
    
}
