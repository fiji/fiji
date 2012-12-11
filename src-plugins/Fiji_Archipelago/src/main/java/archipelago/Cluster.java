package archipelago;


import archipelago.compute.*;
import archipelago.listen.NodeStateListener;
import archipelago.listen.ProcessListener;
import archipelago.listen.ShellExecListener;
import archipelago.network.node.ClusterNode;
import archipelago.network.node.ClusterNodeState;
import archipelago.network.node.NodeManager;
import archipelago.network.server.ArchipelagoServer;
import ij.IJ;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Larry Lindsey
 */
public class Cluster implements ExecutorService, NodeStateListener
{
    

    /**
     * This is a very dumb scheduler. It attempts to submit the first ProcessManager in its
     * queue to the first ClusterNode that it finds available. This should work well as long as
     * all ProcessManagers are more-or-less equals, and all ClusterNodes can accept them as
     * equally as any other. There is code here that will attempt to recover in the case that
     * a ProcessManager is rejected, but if this becomes a common occurrence, then this
     * scheduler should be replaced with something more sophisticated.
     * @author Larry Lindsey
     */
    public class ProcessScheduler extends Thread
    {
        private final ArrayBlockingQueue<ProcessManager> jobQueue, priorityJobQueue;
        private final AtomicInteger pollTime;
        private final AtomicBoolean running;
        private final Hashtable<Long, ProcessManager> runningProcesses;
        private final Vector<ProcessManager<?>> remainingJobList;
        private final int guaranteeCapacity;

        private ProcessScheduler(int jobCapacity, int t)
        {
            jobQueue = new ArrayBlockingQueue<ProcessManager>(jobCapacity);
            priorityJobQueue = new ArrayBlockingQueue<ProcessManager>(jobCapacity);
            running = new AtomicBoolean(true);
            pollTime = new AtomicInteger(t);
            runningProcesses = new Hashtable<Long, ProcessManager>();
            remainingJobList = new Vector<ProcessManager<?>>();
            guaranteeCapacity = jobCapacity;
        }
        

        public void setPollTimeMillis(int t)
        {
            pollTime.set(t);
        }

        //public synchronized boolean submit()

        public synchronized <T> boolean queueJob(Callable<T> c, long id)
        {
            return queueJob(c, id, false);
        }
        
        public synchronized <T> boolean queueJob(Callable<T> c, long id, boolean priority)
        {
            ProcessManager<T> pm = new ProcessManager<T>(c, id);
            return queueJob(pm, priority);
        }
        
        public synchronized boolean queueJob(ProcessManager pm, boolean priority)
        {

            ArrayBlockingQueue<ProcessManager> queue = priority ? priorityJobQueue : jobQueue;

            // This test guarantees that we will always have at least a certain capacity in the queue
            // We have two queues that have a total capacity of guaranteeCapacity each.
            // This means that if remainingCapacity() returns exactly guaranteeCapacity, then we
            // have guaranteeCapacity's worth of jobs already in queue.
            if (priority || remainingCapacity() >= guaranteeCapacity)
            {
                // This is done in the event that the ProcessManager in question is being
                // re-queued.
                pm.setRunningOn(null);
                queue.add(pm);
                return true;
            }
            else
            {
                return false;
            }
        }
        
        private int remainingCapacity()
        {
            return priorityJobQueue.remainingCapacity() + jobQueue.remainingCapacity();
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
                ProcessManager<?> pm;
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
                        ProcessListener listener = new ProcessListener() {
                            public boolean processFinished(ProcessManager<?> process) {
                                final long id = process.getID();
                                final ArchipelagoFuture<?> future = futures.remove(id);

                                runningProcesses.remove(id);
                                decrementJobCount();
                                
                                try
                                {
                                    future.finish(process);
                                    return true;
                                }
                                catch (ClassCastException cce)
                                {
                                    return false;
                                }
                            }
                        };
                                
                        FijiArchipelago.log("Scheduling job on " + node.getHost());
                        FijiArchipelago.log("There are now " + jobQueue.size() + " jobs in queue");

                        /*
                        Here, we have a ProcessManager, a ProcessListener, and a ClusterNode.
                        The PM has been removed from the queue already.
                        If we successfully submit it on a ClusterNode, we're done. If that didn't
                        work, we push it back onto the priority queue. Unless we're awash in
                        closed ClusterNodes, scheduling should be re-attempted later.
                         */

                        if (node.submit(pm, listener))
                        {
                            runningProcesses.put(pm.getID(), pm);
                            incrementJobCount();
                        }
                        else
                        {
                            queueJob(pm, true);
                        }

                    }
                }
            }
            FijiArchipelago.log("Scheduler exited");
        }

        public synchronized void setActive(boolean active)
        {
            running.set(active);
        }

        /**
         * Attempts to cancel a running job with the given id, optionally canclling jobs that have
         * already been submitted to a node.
         * @param id the id of the job to cancel
         * @param force set true to cancel a job that is currently executing, false otherwise.
         * @return true if the job was cancelled, false if not. If false, either the job has
         * already finished, or it is already executing and force is set false.
         */
        public synchronized boolean cancelJob(long id, boolean force)
        {
            final ArrayList<ProcessManager> pmInQ = new ArrayList<ProcessManager>();
            boolean cont = true;

            // These for loops look funny because we're running concurrently

            for (ProcessManager pm : priorityJobQueue)
            {
                pmInQ.add(pm);
            }
            
            for (int i = 0; cont && i < pmInQ.size(); ++i)
            {
                final ProcessManager pm = pmInQ.get(i);
                if (pm.getID() == id)
                {
                    /*
                    It is very possible that we popped the ProcessManager in question off
                    of the queue and submitted it to a node in between copying the job queue
                    into pmInQ and finding the correct ProcessManager.

                    This handles that potential case.
                     */
                    if(priorityJobQueue.remove(pm))
                    {
                        return true;
                    }
                    else
                    {
                        cont = false;
                    }
                }
            }
            
            pmInQ.clear();
            for (ProcessManager pm : jobQueue)
            {
                pmInQ.add(pm);
            }

            for (int i = 0; cont && i < pmInQ.size(); ++i)
            {
                final ProcessManager pm = pmInQ.get(i);
                if (pm.getID() == id)
                {
                    if(jobQueue.remove(pm))
                    {
                        return true;
                    }
                    else
                    {
                        cont = false;
                    }
                }
            }

            /*
            If we have gotten this far, and if the ProcessManager in question is running somewhere,
            then we should be able to pull it out of the runningProcesses hash table.
             */

            ProcessManager<?> pm = runningProcesses.get(id);             
            if (force && pm != null)
            {
                ClusterNode runningOn = getNode(pm.getRunningOn()); 
                if (runningOn != null)
                {
                    if (runningOn.cancelJob(id))
                    {
                        runningProcesses.remove(id);
                        decrementJobCount();
                        return true;

                    }
                    else
                    {
                        FijiArchipelago.err("Could not cancel job " + id + " running on node "
                                + runningOn.getHost());
                        return false;
                    }

                }
                else
                {
                    /*
                    If we have reached this block, we're probably in trouble.
                    This can only happen if:
                        runningProcesses contains jobs that exist on a ClusterNode that has been
                        halted. This shouldn't happen, as all jobs should be removed from
                        runningProcesses in that case.
                    or:
                        Somehow, something is very wrong and we've managed to corrupt or overload
                        the ClusterNodes unique ID
                     */
                    FijiArchipelago.err("Queue: ProcessManager " + pm.getID()
                            + " was running, but could not find its Node. Cannot cancel");
                    
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        
        public Vector<ProcessManager<?>> remainingJobs()
        {
            return remainingJobList;
        }
        
        public synchronized void close()
        {
            running.set(false);
            interrupt();
            
            remainingJobList.clear();

            for (ProcessManager pm : priorityJobQueue)
            {
                remainingJobList.add(pm);
                futures.get(pm.getID()).cancel(false);
            }
            
            for (ProcessManager pm : jobQueue)
            {
                remainingJobList.add(pm);
                futures.get(pm.getID()).cancel(false);
            }

            priorityJobQueue.clear();
            jobQueue.clear();
            //nodes.clear();
        }

    }

    public static final int DEFAULT_PORT = 3501;
    private static Cluster cluster = null;

    public static boolean initCluster(int port)
    {
        if (cluster == null)
        {
            cluster = new Cluster(port);
            return true;
        }
        else
        {
            return cluster.init(port);
        }
    }
    
    public static Cluster getCluster()
    {
        return cluster;
    }
    
    public static boolean activeCluster()
    {
        return cluster != null && cluster.isActive();
    }
    
    private int port;
    private final Vector<ClusterNode> nodes;
    private final Vector<Thread> waitThreads;
    private final ProcessScheduler scheduler;


    /*
     After construction, halted, ready and terminated are all false

     Once the cluster has been initialized and has at least one node associated with it,
     ready will transition to true.

     If ready is true, a call to shutdown() will cause ready to become false and halted to
     become true. The cluster will continue to process queued jobs until the queue empties, at
     which point terminated also becomes true. A call to reset() at this point will place the
     cluster into a state just after initialization, ie, ready will be true and the others state
     variables will be false.

     A call to shutdownNow() will halt all processing on all cluster nodes, and return a list of
     Callables representing any job that was in queue or processing at the time of the call. At
     this time, ready will become false, halted and terminated will become true.



     */
    private final AtomicBoolean halted, ready, terminated;
    
    private final AtomicInteger jobCount, runningNodes;
    
    private final Hashtable<Long, ArchipelagoFuture<?>> futures;
    
    private final NodeManager nodeManager;
    private ArchipelagoServer server;
    private String localHostName;

    private Cluster(int p)
    {        
        nodes = new Vector<ClusterNode>();
        waitThreads = new Vector<Thread>();
        
        ready = new AtomicBoolean(false);
        halted = new AtomicBoolean(false);
        terminated = new AtomicBoolean(false);
        jobCount = new AtomicInteger(0);
        runningNodes = new AtomicInteger(0); 

        scheduler = new ProcessScheduler(1024, 1000);
        
        nodeManager = new NodeManager();
        futures = new Hashtable<Long, ArchipelagoFuture<?>>();

        init(p);
        
        try
        {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException uhe)
        {
            localHostName = "localhost";
            FijiArchipelago.err("Could not get canonical host name for local machine. Using localhost instead");
        }
    }
    
    public boolean init(int p)
    {
        if (!isActive())
        {
            port = p;
            server = null;
            nodes.clear();
            scheduler.close();
            nodeManager.clear();
            halted.set(false);
            terminated.set(false);
            jobCount.set(0);
            runningNodes.set(0);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void setLocalHostName(String host)
    {        
        localHostName = host;
    }
    
    public int getServerPort()
    {
        return port;
    }
    
    public boolean startNode(final NodeManager.NodeParameters params, final ShellExecListener listener)
    {
        String host = params.getHost();
        String exec = params.getExecRoot();
        long id = params.getID();
        String execCommandString = exec + "/fiji --jar-path " + exec
                + "/plugins/ --jar-path " + exec +"/jars/" + " --allow-multiple --main-class archipelago.Fiji_Archipelago "
                + localHostName + " " + port + " " + id + " 2>&1 > ~/" + host + "_" + id + ".log";
        
        return params.getShell().exec(params, execCommandString, listener);
    }


    public ClusterNode getNode(long id)
    {        
        for (ClusterNode node : nodes)
        {
            if (node.getID() == id)
            {
                return node;
            }
        }
        return null;
    }
    
    private void addNode(ClusterNode node)
    {
        nodes.add(node);
        node.addListener(this);
        //ready.set(true);
    }

    public void removeNode(ClusterNode node)
    {
        nodes.remove(node);
        nodeManager.removeParam(node.getID());
    }

    public synchronized void stateChanged(ClusterNode node, ClusterNodeState stateNow,
                             ClusterNodeState lastState) {
        switch (stateNow)
        {
            case ACTIVE:
                FijiArchipelago.debug("Got state change to active for " + node.getHost());
                if (!isShutdown())
                {                    
                    runningNodes.incrementAndGet();
                    ready.set(true);
                    FijiArchipelago.debug("Not shut down. Currently " + runningNodes.get()
                            + " running nodes");
                }
                break;
            
            case STOPPED:

                FijiArchipelago.debug("Got state change to stopped for " + node.getHost());

                for (ProcessManager<?> pm : node.getRunningProcesses())
                {
                    FijiArchipelago.debug("Rescheduling job " + pm.getID());
                    scheduler.queueJob(pm, true);
                    if (isShutdown())
                    {
                        FijiArchipelago.debug("Cancelling running job " + pm.getID());
                        futures.get(pm.getID()).cancel(true);
                    }
                }
                
                removeNode(node);                
                
                if (runningNodes.decrementAndGet() <= 0)
                {
                    FijiArchipelago.debug("Node more running nodes");
                    if (runningNodes.get() < 0)
                    {
                        FijiArchipelago.log("Number of running nodes is negative. " +
                                "That shouldn't happen.");
                    }
                    
                    ready.set(false);
                    
                    if (isShutdown())
                    {
                        terminateFinished();
                    }
                }
                
                FijiArchipelago.debug("There are now " + runningNodes.get() + " running nodes");
                break;
        }
    }

    
    public void nodeFromSocket(Socket socket)
    {
        try
        {
            ClusterNode node = new ClusterNode(socket);
            addNode(node);
        }
        catch (IOException ioe)
        {
            FijiArchipelago.err("Caught IOException while initializing node for "
                    + socket.getInetAddress() + ": " + ioe);
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.err("Caught InteruptedException while initializing node for "
                    + socket.getInetAddress() + ": " + ie);
        }
        catch (ClusterNode.TimeOutException toe)
        {
            FijiArchipelago.err("Caught TimeOutException while initializing node for "
                    + socket.getInetAddress() + ": " + toe);
        }
    }
    
    public void waitUntilReady()
    {
        waitUntilReady(Long.MAX_VALUE);
    }
    
    public void waitUntilReady(long timeout)
    {
        boolean wait = !isReady();

        final long sTime = System.currentTimeMillis();
        
        FijiArchipelago.log("Waiting for ready nodes");

        // Wait synchronously
        while (wait)
        {

            try
            {
                Thread.sleep(1000); //sleep for a second

                if ((System.currentTimeMillis() - sTime) > timeout)
                {
                    if (!isReady())
                    {
                        FijiArchipelago.err("Cluster timed out while waiting for nodes to be ready");
                    }
                    wait = false;
                }
                else
                {                    
                    wait = !isReady();
                }
            }
            catch (InterruptedException ie)
            {
                wait = false;
            }
        }
        
        FijiArchipelago.log("Cluster is ready");
    }

    public int countReadyNodes()
    {
        int i = 0;
        for (ClusterNode node : nodes)
        {            
            if (node.isReady())
            {
                ++i;
            }
        }
        return i;
    }
    
    public boolean join()
    {
        return server.join();
    }

    public boolean isReady()
    {
        return (!isShutdown() && !isTerminated() && ready.get());
    }

    private void incrementJobCount()
    {
        jobCount.incrementAndGet();
    }
    
    private synchronized void decrementJobCount()
    {
        int nJobs = jobCount.decrementAndGet();

        if (nJobs < 0)
        {
            FijiArchipelago.log("Job Count is negative. That shouldn't happen.");
        }

        FijiArchipelago.debug("Cluster: Job finished. Running job count is now " + nJobs
                + "state: " + isShutdown() + " " + isTerminated());
        
        if (nJobs <= 0 && isShutdown() && !isTerminated())
        {
            FijiArchipelago.debug("Cluster: Calling haltFinished");
            haltFinished();
        }
    }

    
    public ArrayList<ClusterNode> getNodes()
    {
        return new ArrayList<ClusterNode>(nodes);
    }
    
    public boolean startServer()
    {
        FijiArchipelago.debug("Scheduler alive? :" + scheduler.isAlive());
        scheduler.start();
        server = new ArchipelagoServer(this);
        return server.start();
    }

    public NodeManager getNodeManager()
    {
        return nodeManager;
    }
    
    public boolean isActive()
    {
        return ready.get();
    }

    
    public synchronized void shutdown()
    {
        /*
        Shutdown sets halted to true, then de-activates the scheduler and all cluster nodes
        When the last ClusterNode finishes processing, the running job count goes to zero
         When the count reaches zero, haltFinished() is called, closing all of the nodes
         When the last node is finished closing down, the cluster's state changes to terminated
         At this point, all queued but un-run jobs may be returned in a List, and any Threads
         that are waiting for us to terminate are unblocked.
         */

        ready.set(false);
        scheduler.setActive(false);
        halted.set(true);
        
        for (ClusterNode node : nodes)
        {
            node.setActive(false);
        }
    }

    protected synchronized void haltFinished()
    {
        ArrayList<ClusterNode> nodesToClose = new ArrayList<ClusterNode>(nodes);
        FijiArchipelago.debug("Cluster: closing " + nodesToClose.size() + " nodes");
        for (ClusterNode node : nodesToClose)
        {
            FijiArchipelago.debug("Cluster: Closing node " + node.getHost());
            node.close();
            FijiArchipelago.debug("Cluster: Done closing node");
        }
        FijiArchipelago.debug("Cluster: Halt has finished");
    }
    
    protected synchronized void terminateFinished()
    {
        ArrayList<Thread> waitThreadsCP = new ArrayList<Thread>(waitThreads);
        
        terminated.set(true);
        scheduler.close();
        nodeManager.clear();
        
        for (Thread t : waitThreadsCP)
        {
            t.interrupt();
        }
    }
            

    public synchronized List<Runnable> shutdownNow() {
        /*
        shutdownNow sets halted to true, de-activates the scheduler, and closes all ClusterNodes
        Any jobs running on the clusternodes are placed on the priority queue in the scheduler,
        but since its no longer active, these jobs are never re-submitted to a new ClusterNode
         When the last node is finished closing down, the cluster's state changes to terminated
         At this point, all queued but un-run jobs may be returned in a List, and any Threads
         that are waiting for us to terminate are unblocked.
         */

        
        ready.set(false);
        halted.set(true);
        
        for (ClusterNode node : nodes)
        {
            node.close();
        }
        
        /*
        Now, wait synchronously up to ten seconds for terminated to get to true.
        If everything went well, the last node.close() should have resulted in a call to
        terminateFinished()
        */

        return remainingRunnables();
    }

    public ArrayList<Callable<?>> remainingCallables()
    {
        ArrayList<Callable<?>> callables = new ArrayList<Callable<?>>(
                scheduler.remainingJobs().size());
        for (final ProcessManager<?> pm : scheduler.remainingJobs())
        {
            callables.add(pm.getCallable());
        }
        return callables;
    }
    
    public ArrayList<Runnable> remainingRunnables()
    {
        ArrayList<Runnable> runnables = new ArrayList<Runnable>(scheduler.remainingJobs().size());
        // Well, running these will be a little rough...
        for (final ProcessManager<?> pm : scheduler.remainingJobs())
        {
            runnables.add(new QuickRunnable(pm.getCallable()));
        }
        return runnables;
    }
    
    public boolean isShutdown()
    {
        return halted.get();
    }

    public boolean isTerminated() {
        return terminated.get();
    }

    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException
    {
        final Thread t = Thread.currentThread(); 
        waitThreads.add(t);
        try
        {
            Thread.sleep(timeUnit.convert(l, TimeUnit.MILLISECONDS));
            waitThreads.remove(t);
            return isTerminated();
        }
        catch (InterruptedException ie)
        {
            waitThreads.remove(t);
            if (isTerminated())
            {
                return true;
            }
            else
            {
                throw ie;
            }
        }
    }

    public <T> Future<T> submit(Callable<T> tCallable)
    {
        ArchipelagoFuture<T> future = new ArchipelagoFuture<T>(scheduler);
        futures.put(future.getID(), future);
        if (!scheduler.queueJob(tCallable, future.getID()))
        {
            future.finish(null);
            futures.remove(future.getID());
        }
        return future;
    }

    public <T> Future<T> submit(final Runnable runnable, T t) {
        Callable<T> tCallable = new QuickCallable<T>(runnable);
        ArchipelagoFuture<T> future = new ArchipelagoFuture<T>(scheduler, t);
        futures.put(future.getID(), future);
        scheduler.queueJob(tCallable, future.getID());
        return future;
    }

    public Future<?> submit(Runnable runnable) {
        return submit(runnable, null);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables) throws InterruptedException
    {
        ArrayList<Future<T>> waitFutures = new ArrayList<Future<T>>(callables.size());
        for (Callable<T> c : callables)
        {
            waitFutures.add(submit(c));
        }
        
        for (Future<T> f : waitFutures)
        {
            try
            {
                f.get();
            }
            catch (ExecutionException e)
            {}
        }

        return waitFutures;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit) throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> callables) throws InterruptedException, ExecutionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void execute(Runnable runnable) {
        submit(runnable);
    }
}
