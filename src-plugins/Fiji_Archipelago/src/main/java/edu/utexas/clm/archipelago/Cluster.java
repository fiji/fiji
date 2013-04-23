/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago;


import edu.utexas.clm.archipelago.compute.*;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.ClusterStateListener;
import edu.utexas.clm.archipelago.listen.NodeStateListener;
import edu.utexas.clm.archipelago.listen.ProcessListener;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.node.ClusterNode;
import edu.utexas.clm.archipelago.network.node.ClusterNodeState;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.shell.NodeShell;
import edu.utexas.clm.archipelago.network.shell.SSHNodeShell;
import edu.utexas.clm.archipelago.network.shell.SocketNodeShell;
import edu.utexas.clm.archipelago.ui.ArchipelagoUI;
import edu.utexas.clm.archipelago.util.ProcessManagerCoreComparator;
import edu.utexas.clm.archipelago.util.XCErrorAdapter;
import ij.Prefs;
import java.io.*;
import java.net.InetAddress;
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
public class Cluster implements NodeStateListener, NodeShellListener
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
        private final LinkedList<ProcessManager> internalQueue;
        private final ReentrantLock lock;

        private ProcessScheduler(int t)
        {
            jobQueue = new LinkedBlockingQueue<ProcessManager>();
            priorityJobQueue = new LinkedBlockingQueue<ProcessManager>();
            running = new AtomicBoolean(true);
            pollTime = new AtomicInteger(t);
            runningProcesses = new Hashtable<Long, ProcessManager>();
            remainingJobList = new Vector<ProcessManager<?>>();
            internalQueue = new LinkedList<ProcessManager>();
            lock = new ReentrantLock();
        }
        

        public void setPollTimeMillis(int t)
        {
            pollTime.set(t);
        }

        public synchronized <T> boolean queueJob(Callable<T> c, long id, float np, boolean f)
        {
            return queueJob(c, id, false, np, f);
        }
        
        public synchronized <T> boolean queueJob(Callable<T> c, long id,
                                                 boolean priority, float np, boolean f)
        {
            ProcessManager<T> pm = new ProcessManager<T>(c, id, np, f);
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
                if (priority)
                {
                    FijiArchipelago.debug("Scheduler: Put job " + pm.getID() + " on the priority queue");
                }
                queue.add(pm);
                return true;
            } catch (IllegalStateException ise)
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

        private void rotate(LinkedList<ClusterNode> nodeList)
        {
            if (nodeList.size() > 1)
            {
                final ClusterNode front = nodeList.remove(0);
                nodeList.addLast(front);
            }
        }

        /**
         * Attempts to submit the ProcessManager pm on each node in nodeList. This function
         * runs on the same thread as run().
         * @param pm a queued ProcessManager that is to be run to the Cluster
         * @param nodeList a List of ClusterNodes with available Threads
         * @return true if pm was scheduled, false otherwise.
         */
        private boolean trySubmit(final ProcessManager<?> pm,
                                  final LinkedList<ClusterNode> nodeList)
        {
            if (nodeList.isEmpty())
            {
                return false;
            }
            else
            {
                ProcessListener listener = new ProcessListener() {
                    /**
                     * processFinished is called when the given ClusterNode recieves a message
                     * from its remote counterpart indicating that the job has finished.
                     * @param process a ProcessManager that just returned from the cluster
                     * @return true if the Future was finished successfully, false otherwise.
                     */
                    public boolean processFinished(ProcessManager<?> process)
                    {
                        final long id = process.getID();
                        final ArchipelagoFuture<?> future = futures.remove(id);

                        if (future == null)
                        {
                            return false;
                        }
                        
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
                
                /*
                 Run through the available ClusterNodes, attempting to submit the job to each one.
                 When a node rejects the PM, rotate the list. Assuming that our job list is rather
                 uniform, this should reduce our overhead.
                 */
                for (int i = 0; i < nodeList.size(); ++i)
                {
                    final ClusterNode node = nodeList.getFirst();

                    if (node.numAvailableThreads() >= pm.requestedCores(node) &&
                            node.submit(pm, listener))
                    {
                        runningProcesses.put(pm.getID(), pm);
                        incrementJobCount();
                        if (node.numAvailableThreads() <= 0)
                        {
                            nodeList.remove(node);
                        }
                        return true;
                    }
                    rotate(nodeList);
                }

                return false;
            }
        }
        
        public void run()
        {
            FijiArchipelago.log("Scheduler: Started. Running flag: " + running.get());

            final ArrayList<ProcessManager> tempQ = new ArrayList<ProcessManager>();
            final ArrayList<ProcessManager> pmQ = new ArrayList<ProcessManager>();
            final LinkedList<ClusterNode> nodeList = new LinkedList<ClusterNode>();           
            final ProcessManagerCoreComparator comparator = new ProcessManagerCoreComparator();
            
            while (running.get())
            {
                lock.lock();

                // Remove de-activated nodes, and those that are saturated with jobs
                for (ClusterNode node : new ArrayList<ClusterNode>(nodeList))
                {
                    if (node.getState() != ClusterNodeState.ACTIVE ||
                            node.numAvailableThreads() <= 0)
                    {
                        nodeList.remove(node);
                    }
                }

                // Add any newly-available nodes we might have.
                for (ClusterNode node : nodes)
                {
                    if (node.numAvailableThreads() > 0 && !nodeList.contains(node))
                    {
                        nodeList.addFirst(node);
                    }
                }

                comparator.setThreadCount(getMaxThreads());
                
                //Put priority jobs in front of the internal queue, in the correct order
//                FijiArchipelago.debug("Scheduler: " + priorityJobQueue.size() +
//                        " jobs in priority queue");
//
                priorityJobQueue.drainTo(tempQ);                
                Collections.sort(tempQ, comparator);
                for (int i = tempQ.size(); i > 0; --i)
                {
                    ProcessManager pm  = tempQ.get(i - 1);
                    FijiArchipelago.debug("Scheduler: Adding job " + pm.getID() +
                            " to internal queue");
                    internalQueue.addFirst(pm);
                }
                tempQ.clear();


                //Put non-priority jobs at the end of the internal queue.
                jobQueue.drainTo(tempQ);
                Collections.sort(tempQ, comparator);
                internalQueue.addAll(tempQ);
                tempQ.clear();
                
                //FijiArchipelago.debug("Scheduler: " + internalQueue.size() + " jobs in queue");

                //If we have both jobs and nodes to run them on...
                if (!internalQueue.isEmpty() && !nodeList.isEmpty())
                {
                    // Temporary queue so we can remove jobs from the internalQueue
                    // without suffering ConcurrentModificationExceptions
                    pmQ.addAll(internalQueue);
                    for (ProcessManager<?> pm : pmQ)
                    {
                        if (trySubmit(pm, nodeList))
                        {
                            FijiArchipelago.debug("Scheduler: Job " + pm.getID() +
                                    " scheduled on host " + getNode(pm.getRunningOn()));
                            internalQueue.remove(pm);
                        }
                        // Deal PM's to the nodes like we're playing poker.
                        rotate(nodeList);
                    }
                    
                    pmQ.clear();
                }
                
                lock.unlock();
                
                // At this stage, all PM's that can be run should be running on a ClusterNode
                // somewhere. Sleep for a bit. Perhaps a new node will become available, or we'll
                // get a PM that can be slotted on an available node.
                try
                {                    
                    Thread.sleep(pollTime.get());
                }
                catch (InterruptedException ie)
                {
                    FijiArchipelago.log("Scheduler interrupted while sleeping, stopping.");
                    running.set(false);
                }
                
            }
            FijiArchipelago.log("Scheduler exited");
        }

        public synchronized void setActive(boolean active)
        {
            running.set(active);
        }

        private boolean removeFromQueue(Collection<ProcessManager> pms, long id)
        {
            ProcessManager rmpm = null;
            for (ProcessManager pm : pms)
            {
                if (pm.getID() == id)
                {
                    rmpm = pm;
                    break;
                }
            }

            return rmpm != null && pms.remove(rmpm);
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
            lock.lock();

            if (removeFromQueue(priorityJobQueue, id) ||
                    removeFromQueue(jobQueue, id) ||
                    removeFromQueue(internalQueue, id))
            {
                lock.unlock();
                return true;
            }
                

            ProcessManager<?> pm = runningProcesses.get(id);             
            if (force && pm != null)
            {
                ArchipelagoFuture future = futures.remove(id);                                
                ClusterNode runningOn = getNode(pm.getRunningOn());

                if (future == null)
                {
                    FijiArchipelago.err("Scheduler.cancelJob: got null future. " +
                            "This should not have happened");
                }
                else
                {
                    if(future.finish(null))
                    {
                        decrementJobCount();
                    }
                }

                if (runningOn != null)
                {
                    if (runningOn.cancelJob(id))
                    {
                        runningProcesses.remove(id);
                        lock.unlock();
                        return true;
                    }
                    else
                    {
                        FijiArchipelago.err("Could not cancel job " + id + " running on node "
                                + runningOn.getHost());
                        lock.unlock();
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
                    
                    lock.unlock();
                    return false;
                }
            }
            else
            {
                lock.unlock();
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

            for (ProcessManager pm : internalQueue)
            {
                remainingJobList.add(pm);
                futures.get(pm.getID()).cancel(false);
            }
            
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
            internalQueue.clear();
        }
        
        public int queuedJobCount()
        {
            return internalQueue.size() + priorityJobQueue.size() + jobQueue.size();
        }

    }

    private class ClusterExecutorService implements ExecutorService
    {

        private final boolean isFractional;
        private final float numCores;

        public ClusterExecutorService(final float ft)
        {
            isFractional = true;
            numCores = ft;
        }

        public ClusterExecutorService(final int nc)
        {
            isFractional = false;
            numCores = nc;
        }

        public synchronized void shutdown()
        {
            self.shutdown();
        }

        public synchronized List<Runnable> shutdownNow() {
            return self.shutdownNow();
        }

        public boolean isShutdown()
        {
            return self.isShutdown();
        }

        public boolean isTerminated() {
            return self.isTerminated();
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
            if (!scheduler.queueJob(tCallable, future.getID(), numCores, isFractional))
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
            scheduler.queueJob(tCallable, future.getID(), numCores, isFractional);
            return future;
        }

        public Future<?> submit(Runnable runnable) {
            return submit(runnable, null);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables)
                throws InterruptedException
        {
            /*ArrayList<Future<T>> waitFutures = new ArrayList<Future<T>>(callables.size());
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
                {*//**//*}
            }

            return waitFutures;*/
            return invokeAll(callables, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }

        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> callables,
                                             final long l,
                                             final TimeUnit timeUnit)
                throws InterruptedException
        {
            final ArrayList<Future<T>> waitFutures = new ArrayList<Future<T>>();
            final ArrayList<Future<T>> remainingFutures = new ArrayList<Future<T>>();
            final AtomicBoolean timeOut = new AtomicBoolean(false);
            final AtomicBoolean done = new AtomicBoolean(false);
            final Thread t = Thread.currentThread();
            final Thread timeOutThread = new Thread()
            {
                public void run()
                {
                    try
                    {
                        FijiArchipelago.debug("Invoke All: Waiting for at most " +
                                timeUnit.convert(l, TimeUnit.MILLISECONDS) + "ms ");

                        Thread.sleep(timeUnit.convert(l, TimeUnit.MILLISECONDS));
                        FijiArchipelago.debug("Invoke All: Timed Out after " +
                                timeUnit.convert(l, TimeUnit.MILLISECONDS) + "ms");
                        timeOut.set(true);
                        t.interrupt();
                    }
                    catch (InterruptedException ie)
                    {
                        if (!done.get())
                        {
                            t.interrupt();
                        }
                    }
                }
            };

            timeOutThread.start();

            try
            {
                for (Callable<T> c : callables)
                {
                    waitFutures.add(submit(c));
                }

                remainingFutures.addAll(waitFutures);

                for (Future<T> f : waitFutures)
                {
                    try
                    {
                        f.get();
                    }
                    catch (ExecutionException e)
                    {/**/}
                    remainingFutures.remove(f);
                }

                done.set(true);
                timeOutThread.interrupt();

                return waitFutures;
            }
            catch (InterruptedException ie)
            {
                if (timeOut.get())
                {
                    FijiArchipelago.debug("Invoke All: Cancelling remaining " +
                            remainingFutures.size() + " futures");
                    for (Future<T> future : remainingFutures)
                    {
                        future.cancel(true);
                    }
                    return waitFutures;
                }
                else
                {
                    done.set(true);
                    timeOutThread.interrupt();
                    throw ie;
                }
            }
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> callables)
                throws InterruptedException, ExecutionException
        {
            try
            {
                return invokeAny(callables, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException te)
            {
                throw new ExecutionException(te);
            }
        }

        public <T> T invokeAny(final Collection<? extends Callable<T>> callables,
                               final long l,
                               final TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, TimeoutException
        {
            final ArrayList<Future<T>> waitFutures = new ArrayList<Future<T>>(callables.size());
            final ArrayList<Future<T>> remainingFutures =
                    new ArrayList<Future<T>>(callables.size());
            final AtomicBoolean timeOut = new AtomicBoolean(false);
            final AtomicBoolean done = new AtomicBoolean(false);
            final Thread t = Thread.currentThread();
            final Thread timeOutThread = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(timeUnit.convert(l, TimeUnit.MILLISECONDS));
                        timeOut.set(true);
                        t.interrupt();
                    }
                    catch (InterruptedException ie)
                    {
                        if (!done.get())
                        {
                            t.interrupt();
                        }
                    }
                }
            };
            ExecutionException lastException = null;
            // Because java doesn't have pointer primitives

            timeOutThread.start();

            try
            {
                boolean stillGoing = true;
                Future<T> okFuture = null;

                for (Callable<T> c : callables)
                {
                    waitFutures.add(submit(c));
                }

                remainingFutures.addAll(waitFutures);


                for (int i = 0; stillGoing && i < waitFutures.size(); ++i)
                {
                    okFuture = waitFutures.get(i);
                    try
                    {
                        okFuture.get();
                        stillGoing = false;
                    }
                    catch (ExecutionException e)
                    {
                        lastException = e;
                    }
                    remainingFutures.remove(okFuture);
                }

                for (Future<T> future : remainingFutures)
                {
                    future.cancel(true);
                }

                if (stillGoing)
                {
                    throw lastException == null ?
                            new ExecutionException(new Exception("No completed callables")) :
                            lastException;
                }

                done.set(true);
                timeOutThread.interrupt();

                return okFuture.get();
            }
            catch (InterruptedException ie)
            {
                if (timeOut.get())
                {
                    throw new TimeoutException();
                }
                else
                {
                    done.set(true);
                    timeOutThread.interrupt();
                    throw(ie);
                }
            }
        }

        public void execute(Runnable runnable) {
            submit(runnable);
        }
    }
    
    private class NodeStarter extends Thread
    {
        private final AtomicBoolean running = new AtomicBoolean(true);
        
        public void run()
        {
            // While running...
            while (running.get())
            {
                // Poll the startQueue queue.

                try
                {
                    final NodeManager.NodeParameters params = startQueue.take();
                    nodeLock.lock();
                    waitNodes.add(params);
                    nodeLock.unlock();

                    // Yes, check running again. We might have been close()d while polling.
                    if (running.get() && (getNode(params.getID()) == null))
                    {
                        /*
                         If we're running start a new thread (yes, meta) to start the node.
                         This is done because sometimes, starting nodes takes a super long time,
                         and we don't want to wait for slow nodes to get things going.
                        */
                        new Thread()
                        {
                            public void run()
                            {
                                try
                                {
                                    params.startNode(self);
                                }
                                catch (ShellExecutionException see)
                                {
                                    nodeLock.lock();
                                    waitNodes.remove(params);
                                    nodeLock.unlock();
                                    FijiArchipelago.err("Could not start node " + params.getHost() +
                                            ": " + see);
                                }
                            }
                        }.start();
                    }
                }
                catch (InterruptedException ie)
                {/* If running has been set to false, we'll quit, otherwise, keep going*/}
            }
        }
        
        public void close()
        {
            running.set(false);
        }
    }

    /* Static Members and Methods */

    private static final HashMap<String, NodeShell> shellMap = new HashMap<String, NodeShell>();
    private static Cluster cluster = null;
    
    public static Cluster getCluster()
    {
        if (!initializedCluster())
        {
            cluster = new Cluster();
        }
        return cluster;
    }
    
    public static boolean activeCluster()
    {
        return cluster != null && cluster.getState() == ClusterState.RUNNING;
    }

    public static boolean initializedCluster()
    {
        ClusterState state = cluster == null ? null : cluster.getState();
        return !(cluster == null ||
                state == ClusterState.STOPPING ||
                state == ClusterState.STOPPED);
    }


    /*
   Backing the ClusterState with an atomic integer reduces the chances for a race condition
   or some other concurrent modification nightmare. This is much prefered to the alternative
   of using a ReentrantLock
    */

    private static ClusterState stateIntToEnum(final int s)
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

    private static int stateEnumToInt(final ClusterState s)
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

    public static void registerNodeShell(NodeShell shell)
    {
        shellMap.put(shell.name(), shell);
    }

    public static NodeShell getNodeShell(String description)
    {
        return shellMap.get(description);
    }
    
    public static Collection<NodeShell> registeredShells()
    {
        return shellMap.values();
    }

    public static boolean configureCluster(
            Cluster cluster,
            String execRootRemote,
            String fileRootRemote,
            String execRoot,
            String fileRoot,
            String userName)
    {

        final String prefRoot = FijiArchipelago.PREF_ROOT;
        //boolean isConfigured = cluster.getState() != Cluster.ClusterState.INSTANTIATED;

        cluster.getNodeManager().getDefaultParameters().setUser(userName);
        cluster.getNodeManager().getDefaultParameters().setExecRoot(execRootRemote);
        cluster.getNodeManager().getDefaultParameters().setFileRoot(fileRootRemote);
        //cluster.getNodeManager().getDefaultParameters().setShell(SocketNodeShell.getShell());

        FijiArchipelago.setExecRoot(execRoot);
        FijiArchipelago.setFileRoot(fileRoot);

        //Set prefs
        Prefs.set(prefRoot + ".username", userName);
        Prefs.set(prefRoot + ".execRoot", execRoot);
        Prefs.set(prefRoot + ".fileRoot", fileRoot);
        Prefs.set(prefRoot + ".execRootRemote", execRootRemote);
        Prefs.set(prefRoot + ".fileRootRemote", fileRootRemote);
        Prefs.savePreferences();

        return cluster.init();
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

    private final Vector<ClusterNode> nodes;
    private final Vector<Thread> waitThreads;
    private final Vector<ArchipelagoUI> registeredUIs;
    private final ProcessScheduler scheduler;
    
    private final Hashtable<Long, ArchipelagoFuture<?>> futures;
    
    private final NodeManager nodeManager;
    private String localHostName;
    
    private final Vector<ClusterStateListener> listeners;

    private final XCErrorAdapter xcEListener;
    
    private final Cluster self = this;
    
    private final LinkedBlockingQueue<NodeManager.NodeParameters> startQueue;
    private final Vector<NodeManager.NodeParameters> waitNodes;
    
    private final ReentrantLock nodeLock;
    
    private final NodeStarter nodeStarter;

    private Cluster()
    {
        state = new AtomicInteger(0);
        nodes = new Vector<ClusterNode>();
        waitThreads = new Vector<Thread>();
        registeredUIs = new Vector<ArchipelagoUI>();
        
        jobCount = new AtomicInteger(0);
        runningNodes = new AtomicInteger(0);
        
        startQueue = new LinkedBlockingQueue<NodeManager.NodeParameters>();
        waitNodes = new Vector<NodeManager.NodeParameters>();
        
        nodeLock = new ReentrantLock();
        
        nodeStarter = new NodeStarter();

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

        scheduler = new ProcessScheduler(1000);
        
        nodeManager = new NodeManager();

        xcEListener.setNodeManager(nodeManager);

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
    
    public boolean init()
    {
        if (getState() == ClusterState.INSTANTIATED)
        {
            nodes.clear();
            scheduler.close();
            nodeManager.clear();
            jobCount.set(0);
            runningNodes.set(0);
            
            setState(ClusterState.INITIALIZED);
            
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

    public void addNode(NodeManager.NodeParameters param)
    {
        if (param == null)
        {
            System.out.println("Tried to add NULL param");
            Thread.dumpStack();
        }
        if (!hasNode(param))
        {
            try
            {
                startQueue.add(param);
            } catch (IllegalStateException ise)
            {/**/}
        }
    }
    
    private void addNode(ClusterNode node)
    {
        nodeLock.lock();
        waitNodes.remove(node.getParam());
        nodes.add(node);        
        nodeLock.unlock();
        node.addListener(this);
    }

    public void removeNode(ClusterNode node)
    {
        nodeLock.lock();
        nodes.remove(node);
        nodeManager.removeParam(node.getID());
        nodeLock.unlock();
    }
    
    public boolean hasNode(long id)
    {
        nodeLock.lock();
        if (getNode(id) != null)
        {
            nodeLock.unlock();
            return true;
        }
        else
        {
            for (NodeManager.NodeParameters param : waitNodes)
            {
                if (param.getID() == id)
                {
                    nodeLock.unlock();
                    return true;
                }
            }
            
            for (NodeManager.NodeParameters param : startQueue)
            {
                if (param.getID() == id)
                {
                    nodeLock.unlock();
                    return true;
                }
            }
        }

        nodeLock.unlock();
        return false;
    }
    
    public boolean hasNode(final ClusterNode node)
    {
        return hasNode(node.getID());
    }

    public boolean hasNode(final NodeManager.NodeParameters param)
    {
        return hasNode(param.getID());
    }

    public void addStateListener(ClusterStateListener listener)
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }
    
    public synchronized void removeStateListener(ClusterStateListener listener)
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
                    if (isShutdown())
                    {
                        FijiArchipelago.debug("Cancelling running job " + pm.getID());
                        decrementJobCount();
                        futures.get(pm.getID()).cancel(true);
                    }
                    else
                    {
                        FijiArchipelago.debug("Rescheduling job " + pm.getID());
                        decrementJobCount();
                        
                        if (!scheduler.queueJob(pm, true))
                        {
                            FijiArchipelago.err("Could not reschedule job " + pm.getID());
                            futures.get(pm.getID()).setException(
                                    new Exception("Could not reschedule job"));
                            futures.get(pm.getID()).finish(null);
                        }
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

    private boolean nodesWaiting()
    {
        for (NodeManager.NodeParameters param : nodeManager.getParams())
        {
            if (param.getNode() == null)
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void waitForAllNodes(final long timeout) throws InterruptedException, TimeoutException
    {
        if (timeout <= 0)
        {
            return;
        }

        boolean wait = true;        
        final long sTime = System.currentTimeMillis();
        
        while (wait)
        {
            long wTime = System.currentTimeMillis() - sTime;
            if (nodesWaiting())
            {
                if (wTime > timeout)
                {
                    throw new TimeoutException();
                }
                Thread.sleep(1000);
            }
            else
            {
                wait = false;
            }
        }
    }
    
    
    public void waitUntilReady()
    {
        waitUntilReady(Long.MAX_VALUE);
    }
    
    public void waitUntilReady(final long timeout)
    {
        boolean wait = !isReady();

        final long sTime = System.currentTimeMillis();
        
        FijiArchipelago.log("Cluster: Waiting for ready nodes");

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

    public ArrayList<ClusterNode> getNodes()
    {
        return new ArrayList<ClusterNode>(nodes);
    }

    /**
     * Compiles a list of NodeManager.NodeParameters corresponding to the current state of this
     * Cluster. This List will include parameters for all ClusterNodes that are currently running
     * or that have been added, but not yet started. It will not included stopped or cancelled
     * nodes.
     * @return a list of NodeManager.NodeParameters corresponding to the current state of this
     * Cluster.
     */
    public ArrayList<NodeManager.NodeParameters> getNodeParameters()
    {
        final ArrayList<NodeManager.NodeParameters> paramList =
                new ArrayList<NodeManager.NodeParameters>();
        
        nodeLock.lock();
        paramList.addAll(startQueue);
        paramList.addAll(waitNodes);

        for (ClusterNode node : getNodes())
        {
            paramList.add(node.getParam());
        }
        nodeLock.unlock();

        FijiArchipelago.debug("Cluster: I have " + paramList.size() + " parameters");

        for (NodeManager.NodeParameters params : paramList)
        {
            FijiArchipelago.debug("\t" + params);
        }

        return paramList;
    }

    /**
     * Starts the Cluster if it has not yet been started
     */
    public void start()
    {
        FijiArchipelago.debug("Cluster: start called");
        Thread.dumpStack();
        if (getState() == ClusterState.INITIALIZED)
        {
            FijiArchipelago.debug("Scheduler alive? :" + scheduler.isAlive());
            nodeStarter.start();
            scheduler.start();
            setState(ClusterState.STARTED);
        }
    }

    public NodeManager getNodeManager()
    {
        return nodeManager;
    }
    
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
        
        nodeManager.clear();

        setState(ClusterState.STOPPED);
        scheduler.close();
        nodeStarter.close();

        for (Thread t : waitThreadsCP)
        {
            t.interrupt();
        }
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

        nodeStarter.close();
        setState(ClusterState.STOPPING);

        for (ClusterNode node : nodescp)
        {
            node.close();
        }

        /*
        Now, wait synchronously up to ten seconds for terminated to get to true.
        If everything went well, the last node.close() should have resulted in a call to
        terminateFinished()
        */

        //ThreadPool.setProvider(new DefaultExecutorProvider());

        return remainingRunnables();
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
        nodeStarter.close();

        for (ClusterNode node : nodes)
        {
            node.setActive(false);
        }

        if (jobCount.get() <= 0)
        {
            haltFinished();
        }

        //ThreadPool.setProvider(new DefaultExecutorProvider());
    }

    public int getMaxThreads()
    {
        int maxThreads = -1;
        for (ClusterNode node : nodes)
        {
            int ncpu = node.getThreadLimit();
            maxThreads = maxThreads < node.getThreadLimit() ? ncpu : maxThreads;
        }
        return maxThreads < 1 ? 1 : maxThreads;
    }
    
    public ExecutorService getService(final int nThreads)
    {
        int maxThreads = getMaxThreads(), nt;

        if (nThreads > maxThreads)
        {
            FijiArchipelago.log("Requested " + nThreads + " but there are only " + maxThreads
                    + " available. Using " + maxThreads + " threads");
            nt = maxThreads;
        }
        else
        {
            nt = nThreads;
        }

        return new ClusterExecutorService(nt);
    }
    
    public ExecutorService getService(final float fractionThreads)
    {
        return new ClusterExecutorService(fractionThreads);
    }
    
    public String getLocalHostName()
    {
        return localHostName;
    }
    
    public synchronized void registerUI(ArchipelagoUI ui)
    {
        registeredUIs.add(ui);
    }
    
    public synchronized void deRegisterUI(ArchipelagoUI ui)
    {
        registeredUIs.remove(ui);
    }
    
    public int numRegisteredUIs()
    {
        return registeredUIs.size();
    }

    public void execFinished(final long nodeID, final Exception e, final int status)
    {
        final ClusterNode node = getNode(nodeID);
        if (node.isReady())
        {
            node.close();
        }
    }

    public void ioStreamsReady(final InputStream is, final OutputStream os)
    {
        ClusterNode node = new ClusterNode(xcEListener);
        try
        {
            node.setIOStreams(is, os);
            addNode(node);
        }
        catch (IOException ioe)
        {

        }
        catch (TimeoutException te)
        {

        }
        catch (InterruptedException ie)
        {

        }
        
    }

    static
    {
        registerNodeShell(SSHNodeShell.getShell());
        registerNodeShell(SocketNodeShell.getShell());
    }
}
