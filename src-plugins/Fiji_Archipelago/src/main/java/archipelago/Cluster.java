package archipelago;


import archipelago.compute.*;
import archipelago.exception.ShellExecutionException;
import archipelago.listen.ClusterStateListener;
import archipelago.listen.NodeStateListener;
import archipelago.listen.ProcessListener;
import archipelago.listen.ShellExecListener;
import archipelago.network.MessageXC;
import archipelago.network.node.ClusterNode;
import archipelago.network.node.ClusterNodeState;
import archipelago.network.node.NodeManager;
import archipelago.network.server.ArchipelagoServer;
import archipelago.util.XCErrorAdapter;
import mpicbg.trakem2.concurrent.DefaultExecutorProvider;
import mpicbg.trakem2.concurrent.ExecutorProvider;
import mpicbg.trakem2.concurrent.ThreadPool;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Larry Lindsey
 */
public class Cluster implements ExecutorService, NodeStateListener
{

    public static enum ClusterState
    {
        INSTANTIATED,
        INITIALIZED,
        STARTED,
        RUNNING,
        STOPPING,
        STOPPED,
        UNKNOWN
    }
    

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
        private final LinkedBlockingQueue<ProcessManager> jobQueue, priorityJobQueue;
        private final AtomicInteger pollTime;
        private final AtomicBoolean running;
        private final Hashtable<Long, ProcessManager> runningProcesses;
        private final Vector<ProcessManager<?>> remainingJobList;
        private final int guaranteeCapacity;

        private ProcessScheduler(int jobCapacity, int t)
        {
            jobQueue = new LinkedBlockingQueue<ProcessManager>();
            priorityJobQueue = new LinkedBlockingQueue<ProcessManager>();
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

            BlockingQueue<ProcessManager> queue = priority ? priorityJobQueue : jobQueue;

            // This is done in the event that the ProcessManager in question is being
            // re-queued.
            pm.setRunningOn(null);

            try
            {
                queue.put(pm);
                return true;
            } catch (InterruptedException ie)
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
                                    FijiArchipelago.debug("Scheduler: Finishing Future " + future.getID());
                                    future.finish(process);
                                    return true;
                                }
                                catch (ClassCastException cce)
                                {
                                    return false;
                                }
                            }
                        };

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
        }
        
        public int queuedJobCount()
        {
            return priorityJobQueue.size() + jobQueue.size();
        }

    }

    public static final int DEFAULT_PORT = 3501;
    private static Cluster cluster = null;    
    
    public static Cluster getCluster()
    {
        if (cluster == null || cluster.getState() == ClusterState.STOPPED)
        {
            cluster = new Cluster();
        }
        return cluster;
    }
    
    public static boolean activeCluster()
    {
        return cluster != null && cluster.getState() == ClusterState.RUNNING;
    }
    
    private class ClusterProvider implements ExecutorProvider
    {
        private final Cluster c;
        
        public ClusterProvider(Cluster cluster)
        {
            c = cluster;
        }

        public ExecutorService getExecutor(int nThreads) {
            return c; 
        }
    }


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
    private AtomicInteger state;
    
    //private final AtomicBoolean halted, ready, terminated, initted, started;        
    private final AtomicInteger jobCount, runningNodes;

    private int port;
    private final Vector<ClusterNode> nodes;
    private final Vector<Thread> waitThreads;
    private final ProcessScheduler scheduler;
    
    private final Hashtable<Long, ArchipelagoFuture<?>> futures;
    
    private final NodeManager nodeManager;
    private ArchipelagoServer server;
    private String localHostName;
    
    private final Vector<ClusterStateListener> listeners;

    private final XCErrorAdapter xcEListener;

    private Cluster()
    {
        state = new AtomicInteger(0);
        nodes = new Vector<ClusterNode>();
        waitThreads = new Vector<Thread>();
        
        jobCount = new AtomicInteger(0);
        runningNodes = new AtomicInteger(0);

        xcEListener = new XCErrorAdapter()
        {
            public boolean handleCustom(final Throwable t, final MessageXC mxc)
            {
                if (t instanceof StreamCorruptedException ||
                        t instanceof EOFException)
                {
                    mxc.close();
                    return false;
                }
                return true;
            }
            
            public boolean handleCustomRX(final Throwable t, final MessageXC xc)
            {
                if (t instanceof ClassNotFoundException)
                {
                    reportRX(t, "Check that your jars are all correctly synchronized. " + t, xc);
                    return false;
                }
                else if (t instanceof NotSerializableException)
                {
                    reportRX(t, "Your Callable returned a " +
                            "value that was not Serializable. " + t, xc);
                    return false;
                }
                else if (t instanceof InvalidClassException)
                {
                    reportRX(t, "Caught remote InvalidClassException.\n" +
                            "This means you likely have multiple jars for the given class: " +
                            t, xc);
                    silence();
                    return false;
                }
                return true;
            }
            
            public boolean handleCustomTX(final Throwable t, MessageXC xc)
            {
                if (t instanceof NotSerializableException)
                {
                    reportTX(t, "Ensure that your class and all" +
                            " member objects are Serializable.", xc);
                    return false;
                }
                else if (t instanceof ConcurrentModificationException)
                {
                    reportTX(t, "Take care not to modify member objects" +
                            " as your Callable is being Serialized.", xc);
                    return false;
                }
                else if (t instanceof IOException)
                {
                    reportTX(t, "Stream closed, closing node. ", xc);
                    xc.close();
                    return false;
                }
                
                return true;
            }
        };
        
        
        

        scheduler = new ProcessScheduler(1024, 1000);
        
        nodeManager = new NodeManager();
        futures = new Hashtable<Long, ArchipelagoFuture<?>>();
        
        listeners = new Vector<ClusterStateListener>();

        try
        {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException uhe)
        {
            localHostName = "localhost";
            FijiArchipelago.err("Could not get canonical host name for local machine. Using localhost instead");
        }
        
        ThreadPool.setProvider(new ClusterProvider(this));
    }
    
    /*
    Backing the ClusterState with an atomic integer reduces the chances for a race condition
    or some other concurrent modification nightmare. This is much prefered to the alternative
    of using a ReentrantLock
     */
    
    private ClusterState stateIntToEnum(final int s)
    {
        switch (s)
        {
            case 0:
                return ClusterState.INSTANTIATED;
            case 1:
                return ClusterState.INITIALIZED;
            case 2:
                return ClusterState.STARTED;
            case 3:
                return ClusterState.RUNNING;
            case 4:
                return ClusterState.STOPPING;
            case 5:
                return ClusterState.STOPPED;
            default:
                return ClusterState.UNKNOWN;
        }
    }

    private int stateEnumToInt(final ClusterState s)
    {
        switch (s)
        {
            case INSTANTIATED:
                return 0;
            case INITIALIZED:
                return 1;
            case STARTED:
                return 2;
            case RUNNING:
                return 3;
            case STOPPING:
                return 4;
            case STOPPED:
                return 5;
            default:
                return -1;
        }
    }
    
    public static String stateString(final ClusterState s)
    {
        switch (s)
        {
            case INSTANTIATED:
                return "Instantiated";
            case INITIALIZED:
                return "Initialized";
            case STARTED:
                return "Started";
            case RUNNING:
                return "Running";
            case STOPPING:
                return "Stopping";
            case STOPPED:
                return "Stopped";
            default:
                return "Unknown";
        }
    }
    
    private void setState(final ClusterState state)
    {
        if (stateIntToEnum(this.state.get()) == ClusterState.STOPPED)
        {
            FijiArchipelago.debug("Attempted to change state on a STOPPED cluster.");
            new Exception().printStackTrace();
            return;
        }
        
        FijiArchipelago.debug("Cluster: State changed from " + stateString(getState()) +
                " to " + stateString(state));        
        this.state.set(stateEnumToInt(state));
        triggerListeners();
    }

    public ClusterState getState()
    {        
        return stateIntToEnum(state.get());
    }
    
    public boolean init(int p)
    {
        if (getState() == ClusterState.INSTANTIATED)
        {
            port = p;
            server = null;
            nodes.clear();
            scheduler.close();
            nodeManager.clear();
            jobCount.set(0);
            runningNodes.set(0);
            
            setState(ClusterState.INITIALIZED);

/*
            halted.set(false);
            terminated.set(false);
            initted.set(true);
*/

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
            throws ShellExecutionException
    {
        String host = params.getHost();
        String exec = params.getExecRoot();
        long id = params.getID();

        // getNode(id) == null indicates that this node has not yet been started
        if (getNode(id) == null)
        {
            final Thread t = Thread.currentThread();
            final AtomicInteger result = new AtomicInteger();
            final ReentrantLock testLock = new ReentrantLock(true); 

            // First, check that the fiji executable exists where we think it exists.

            // Use a lock to prevent the ShellExecListener from triggering the result
            // before we're ready.
            testLock.lock();

            final ShellExecListener fijiExistsListener = new ShellExecListener() {
                public void execFinished(long nodeID, Exception e, int status) {
                    // Wait for the lock to release
                    testLock.lock();
                    // Set the result status
                    result.set(status);
                    testLock.unlock();
                    // Restart the startNode(...) thread.
                    t.interrupt();
                }
            };

            if (params.getShell().exec(params, "test -e " + exec + "/fiji", fijiExistsListener))
            {
                try
                {
                    // Unlock the lock to allow the SEL to run, then sleep
                    testLock.unlock();
                    Thread.sleep(Long.MAX_VALUE);
                    throw new ShellExecutionException("Timed out " +
                            "waiting to test for existence of executable");
                }
                catch (InterruptedException ie)
                {
                    // We expect to be interrupted
                    if (result.get() != 0)
                    {
                        // If we get a nonzero return, then fiji does not exist.
                        throw new ShellExecutionException(exec + "/fiji" +
                                " does not exist on host " + host);
                    }
                }                
            }
            else
            {
                return false;
            }

            // Run fiji with jar path plugins, jars, and classpath containing
            // the root fiji directory. Execute main(String[] args) in Fiji_Archipelago.
            // Sys.out and Sys.err are piped to ~/${host}_${id}.log on the remote machine.
            String execCommandString = exec + "/fiji --jar-path " + exec
                    + "/plugins/ --jar-path " + exec +"/jars/ --classpath " + exec +
                    " --allow-multiple --main-class archipelago.Fiji_Archipelago "
                    + localHostName + " " + port + " " + id + " 2>&1 > ~/" + host + "_" + id + ".log";

            return params.getShell().exec(params, execCommandString, listener);
        }
        else
        {
            return true;
        }
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
    }

    public void removeNode(ClusterNode node)
    {
        nodes.remove(node);
        nodeManager.removeParam(node.getID());
    }

    public void addStateListener(ClusterStateListener listener)
    {
        listeners.add(listener);
    }
    
    public void removeStateListener(ClusterStateListener listener)
    {
        listeners.remove(listener);
    }
    
    protected void triggerListeners()
    {
        Vector<ClusterStateListener> listenersLocal = new Vector<ClusterStateListener>(listeners);
        for (ClusterStateListener listener : listenersLocal)
        {
            listener.stateChanged(this);
        }
    }
    
    public boolean acceptingNodes()
    {
        ClusterState state = getState();
        return state == ClusterState.RUNNING || state == ClusterState.STARTED;
    }
    
    public synchronized void stateChanged(ClusterNode node, ClusterNodeState stateNow,
                             ClusterNodeState lastState) {
        switch (stateNow)
        {
            case ACTIVE:
                FijiArchipelago.debug("Got state change to active for " + node.getHost());
                if (acceptingNodes())
                {                    
                    runningNodes.incrementAndGet();
                    setState(ClusterState.RUNNING);
                    //ready.set(true);
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
                    
//                    ready.set(false);
                    
                    if (getState() == ClusterState.STOPPING)
                    {
                        terminateFinished();
                    }
                    else
                    {
                        setState(ClusterState.STARTED);
                    }
                }
                
                FijiArchipelago.debug("There are now " + runningNodes.get() + " running nodes");
                break;
        }

        triggerListeners();
    }

    
    public void nodeFromSocket(Socket socket)
    {
        try
        {
            ClusterNode node = new ClusterNode(socket, xcEListener);
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
                    if (getState() != ClusterState.RUNNING)
                    {
                        FijiArchipelago.err("Cluster timed out while waiting for nodes to be ready");
                    }
                    wait = false;
                }
                else
                {                    
                    wait = getState() != ClusterState.RUNNING; 
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
        return getState() == ClusterState.RUNNING;
    }

    private void incrementJobCount()
    {
        jobCount.incrementAndGet();
        triggerListeners();
    }
    
    private synchronized void decrementJobCount()
    {
        int nJobs = jobCount.decrementAndGet();

        triggerListeners();

        if (nJobs < 0)
        {
            FijiArchipelago.log("Cluster: Job Count is negative. That shouldn't happen.");
        }

        if (nJobs <= 0 && isShutdown() && !isTerminated())
        {
            FijiArchipelago.debug("Cluster: Calling haltFinished");
            haltFinished();
        }
    }

    
    public int getPort()
    {
        return port;
    }
    
    public ArrayList<ClusterNode> getNodes()
    {
        return new ArrayList<ClusterNode>(nodes);
    }
    
    public ArrayList<NodeManager.NodeParameters> getNodesParameters()
    {
        final ArrayList<NodeManager.NodeParameters> paramList =
                new ArrayList<NodeManager.NodeParameters>();
        for (ClusterNode node : getNodes())
        {
            paramList.add(node.getParam());
        }
        
        return paramList;
    }

    /**
     * Starts the Cluster if it has not yet been started, otherwise simply returns true.
     * @return true if the Cluster is already running or if it was started successfully,
     * false if it was not started successfully. An unsuccessful start is typically caused
     * by the inability to reserve the specified network port.
     */
    public boolean start()
    {
        if (getState() == ClusterState.INITIALIZED)
        {

            FijiArchipelago.debug("Scheduler alive? :" + scheduler.isAlive());
            scheduler.start();
            server = new ArchipelagoServer(this);
            if (server.start())
            {
                setState(ClusterState.STARTED);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    public NodeManager getNodeManager()
    {
        return nodeManager;
    }
    
    /*public boolean isActive()
    {
        return ready.get();
    }
    
    public boolean isStarted()
    {
        return started.get();
    }*/

    public int getRunningJobCount()
    {
        return jobCount.get();
    }
    
    public int getRunningNodeCount()
    {
        return runningNodes.get();
    }
    
    public int getQueuedJobCount()
    {
        return scheduler.queuedJobCount();
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

        setState(ClusterState.STOPPING);
        scheduler.setActive(false);
        
        for (ClusterNode node : nodes)
        {
            node.setActive(false);
        }
        
        if (jobCount.get() <= 0)
        {
            haltFinished();
        }
        
        ThreadPool.setProvider(new DefaultExecutorProvider());
    }

    protected synchronized void haltFinished()
    {
        ArrayList<ClusterNode> nodesToClose = new ArrayList<ClusterNode>(nodes);
        FijiArchipelago.debug("Cluster: closing " + nodesToClose.size() + " nodes");
        for (ClusterNode node : nodesToClose)
        {
            FijiArchipelago.debug("Cluster: Closing node " + node.getHost());
            node.close();
        }
        triggerListeners();
        FijiArchipelago.debug("Cluster: Halt has finished");
    }
    
    protected synchronized void terminateFinished()
    {
        ArrayList<Thread> waitThreadsCP = new ArrayList<Thread>(waitThreads);
        
        server.close();

        nodeManager.clear();

        setState(ClusterState.STOPPED);
        scheduler.close();

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
        final ArrayList<ClusterNode> nodescp = new ArrayList<ClusterNode>(nodes);
        
        setState(ClusterState.STOPPING);
        
/*
        ready.set(false);
        halted.set(true);
*/

        for (ClusterNode node : nodescp)
        {
            node.close();
        }
        
        /*
        Now, wait synchronously up to ten seconds for terminated to get to true.
        If everything went well, the last node.close() should have resulted in a call to
        terminateFinished()
        */

        ThreadPool.setProvider(new DefaultExecutorProvider());

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
        ClusterState state = getState();
        return state == ClusterState.STOPPING || state == ClusterState.STOPPED; 
    }

    public boolean isTerminated() {
        return getState() == ClusterState.STOPPED;
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
