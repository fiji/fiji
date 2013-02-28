package archipelago.network.node;

import archipelago.*;
import archipelago.data.HeartBeat;
import archipelago.exception.ShellExecutionException;
import archipelago.listen.*;
import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.MessageXC;
import archipelago.network.shell.NodeShell;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Larry Lindsey
 */
public class ClusterNode implements TransceiverListener
{
    

    public class TimeOutException extends Exception
    {
        public TimeOutException()
        {
            super("Timed out waiting for ID");
        }
    }


    //private Socket nodeSocket;
    private MessageXC xc;
    
    private final Hashtable<Long, ProcessListener> processHandlers;
    private final Hashtable<Long, ProcessManager> runningProcesses;
    private final AtomicBoolean ready;
    private final AtomicInteger ramMBAvail, ramMBTot, ramMBMax, runningCores;
    private long nodeID;
    private long lastBeatTime;
    private NodeManager.NodeParameters nodeParam;
    private final AtomicBoolean idSet, cpuSet;
    private ClusterNodeState state;
    private final Vector<NodeStateListener> stateListeners;
    private final TransceiverExceptionListener xcEListener;

   
    public ClusterNode(final Socket socket, final TransceiverExceptionListener tel)
            throws IOException, InterruptedException, TimeOutException
    {
        xc = null;
        lastBeatTime = 0;
        state = ClusterNodeState.INACTIVE;
        int waitCnt = 0;
        ready = new AtomicBoolean(false);
        idSet = new AtomicBoolean(false);
        cpuSet = new AtomicBoolean(false);
        ramMBAvail = new AtomicInteger(0);
        ramMBTot = new AtomicInteger(0);
        ramMBMax = new AtomicInteger(0);
        runningCores = new AtomicInteger(0);
        processHandlers = new Hashtable<Long, ProcessListener>();
        runningProcesses = new Hashtable<Long, ProcessManager>();
        nodeID = -1;
        nodeParam = null;
        stateListeners = new Vector<NodeStateListener>();
        xcEListener = tel;

        setClientSocket(socket);
        
        xc.queueMessage(MessageType.GETID);
        
        while (!idSet.get() && !cpuSet.get())
        {
            Thread.sleep(100);
            if (waitCnt++ > 120)
            {
                throw new TimeOutException();
            }
        }
        doSyncEnvironment();
    }
    
    private void doSyncEnvironment()
    {
        if (getUser() == null || getUser().equals(""))
        {
            xc.queueMessage(MessageType.USER);
        }

        if (getExecPath() == null || getExecPath().equals(""))
        {
            xc.queueMessage(MessageType.GETEXECROOT);
        }
        else
        {
            xc.queueMessage(MessageType.SETEXECROOT, getExecPath());
        }

        if (getFilePath() == null || getFilePath().equals(""))
        {
            xc.queueMessage(MessageType.GETEXECROOT);
        }
        else
        {
            xc.queueMessage(MessageType.SETFILEROOT, getFilePath());
        }
        xc.queueMessage(MessageType.BEAT);
    }
    
    public boolean setExecPath(String path)
    {
        if (nodeParam == null)
        {
            return false;
        }
        else
        {
            nodeParam.setExecRoot(path);
            return true;
        }
    }
    
    public boolean setFilePath(String path)
    {
        if (nodeParam == null)
        {
            return false;
        }
        else
        {
            nodeParam.setFileRoot(path);
            return xc.queueMessage(MessageType.SETFILEROOT, path);
        }
    }

    public void streamClosed()
    {
        FijiArchipelago.log("Stream closed on " + getHost());
        if (state != ClusterNodeState.STOPPED)
        {
            close();
        }
    }

    private void setClientSocket(final Socket s) throws IOException
    {

        xc = new MessageXC(s.getInputStream(), s.getOutputStream(), this,
                xcEListener, s.getInetAddress().getHostName());

        FijiArchipelago.log("Got Socket from " + s.getInetAddress());

        ready.set(true);
        setState(ClusterNodeState.ACTIVE);
    }

    public String getHost()
    {
        return nodeParam == null ? null : nodeParam.getHost();
    }
    
    public String getUser()
    {
        return nodeParam.getUser();
    }
    
    public String getExecPath()
    {
        return nodeParam.getExecRoot();
    }
    
    public String getFilePath()
    {
        return nodeParam.getFileRoot();
    }

    public boolean isReady()
    {
        return state == ClusterNodeState.ACTIVE;
    }
    
    public boolean exec(final String command, final ShellExecListener listener)
            throws ShellExecutionException
    {
        return getShell().exec(nodeParam, command, listener);
    }

    public long getID()
    {
        return nodeID;
    }
    
    public NodeShell getShell()
    {
        return nodeParam.getShell();
    }

    public NodeManager.NodeParameters getParam()
    {
        return nodeParam;
    }

    public void setShell(final NodeShell shell)
    {
        nodeParam.setShell(shell);
    }
    
    public int numRunningThreads()
    {
        return runningCores.get();
    }

    public int numAvailableThreads()
    {
        int n = nodeParam.getThreadLimit() - runningCores.get();
        return n > 0 ? n : 0;
    }
    
    public int getThreadLimit()
    {
        return nodeParam.getThreadLimit();
    }
    
    public void setActive(boolean active)
    {        
        setState(active ? ClusterNodeState.ACTIVE : ClusterNodeState.INACTIVE);
    }

    public boolean submit(final ProcessManager<?> process, final ProcessListener listener)
    {
        if (isReady())
        {
            if (processHandlers.get(process.getID()) == null)
            {
                //FijiArchipelago.debug(getHost() + " scheduling process");
                processHandlers.put(process.getID(), listener);
                runningProcesses.put(process.getID(), process);
                process.setRunningOn(this);
                runningCores.addAndGet(process.requestedCores(this));
                return xc.queueMessage(MessageType.PROCESS, process);
            }
            else
            {
                FijiArchipelago.debug("There is already a process " + process.getID() + " on "
                        + getHost());
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public void ping()
    {
        xc.queueMessage(MessageType.PING);
    }

    public long lastBeat()
    {
        return lastBeatTime;
    }

    public void handleMessage(final ClusterMessage cm)
    {
        MessageType type = cm.type;
        Object object = cm.o;

        try
        {
            switch (type)
            {
                case GETID:
                    Long id = (Long)object;
                    nodeParam = Cluster.getCluster().getNodeManager().getParam(id);
                    FijiArchipelago.debug("Got id message. Setting ID to " + id + ". Param: " + nodeParam);
                    nodeID = id;
                    nodeParam.setNode(this);
                    idSet.set(true);
                    if (nodeParam.getThreadLimit() <= 0)
                    {
                        xc.queueMessage(MessageType.NUMTHREADS);
                    }
                    else
                    {
                        cpuSet.set(true);
                    }
                    break;

                case PROCESS:
                    ProcessManager<?> pm = (ProcessManager<?>)object;
                    ProcessListener listener = processHandlers.remove(pm.getID());
                    removeProcess(pm);
                    //runningProcesses.remove(pm.getID());

                    listener.processFinished(pm);
                    break;

                case NUMTHREADS:
                    int n = (Integer)object;
                    nodeParam.setThreadLimit(n);
                    cpuSet.set(true);
                    break;
                
                case PING:                
                    FijiArchipelago.log("Received ping from " + getHost());
                    break;
            
                case USER:
                    if (nodeParam == null)
                    {
                        FijiArchipelago.err("Tried to set user but params are null");
                    }
                    if (object != null)
                    {
                        String username = (String)object;
                        nodeParam.setUser(username);
                    }
                    else
                    {
                        FijiArchipelago.err("Got username message with null user");
                    }
                    break;

                case GETFILEROOT:
                    // Results of a GETFILEROOT request sent to the client.
                    setFilePath((String)object);
                    break;
                
                case GETEXECROOT:
                    // Results of a GETEXECROOT request sent to the client.
                    setExecPath((String)object);
                    break;

                case ERROR:
                    Exception e = (Exception)object;
                    xcEListener.handleRXThrowable(e, xc);
                    break;
                
                case BEAT:
                    HeartBeat beat = (HeartBeat)object;
                    lastBeatTime = System.currentTimeMillis();
                    ramMBAvail.set(beat.ramMBAvailable);
                    ramMBTot.set(beat.ramMBTotal);
                    ramMBMax.set(beat.ramMBMax);
                    break;
                
                default:
                    FijiArchipelago.log("Got unexpected message type. The local version " +
                            "of Archipelago may not be up to date with the clients.");
            
            }
            
        }
        catch (ClassCastException cce)
        {
            FijiArchipelago.err("Caught ClassCastException while handling message " 
                    + ClusterMessage.typeToString(type) + " on " + getHost() + " : "+ cce);
        }
        catch (NullPointerException npe)
        {
            FijiArchipelago.err("Expected a message object but got null for " +
                    ClusterMessage.typeToString(type) + " on "+ getHost());
        }
    }

    public int getMaxRamMB()
    {
        return ramMBMax.get();
    }
    
    public int getAvailableRamMB()
    {
        return ramMBAvail.get();
    }
    
    public int getTotalRamMB()
    {
        return ramMBTot.get();
    }
    
    public synchronized void close()
    {        
        if (state != ClusterNodeState.STOPPED)
        {
            FijiArchipelago.debug("Setting state");

            setState(ClusterNodeState.STOPPED);

            FijiArchipelago.debug("Sending shutdown");

            sendShutdown();
            
            for (ProcessManager pm : new ArrayList<ProcessManager>(runningProcesses.values()))
            {
                removeProcess(pm);
            }

            FijiArchipelago.debug("Closing XC");

            xc.close();

            FijiArchipelago.debug("Node: Close finished");
        }
        else
        {
            FijiArchipelago.debug("Node: Close() called, but I'm already stopped");
        }
    }

    private boolean sendShutdown()
    {
        return xc.queueMessage(MessageType.HALT);
    }
    
    public static String stateString(final ClusterNodeState state)
    {
        switch(state)
        {
            case ACTIVE:
                return "active";
            case INACTIVE:
                return "inactive";
            case STOPPED:
                return "stopped";
            default:
                return "unknown";
        }
    }
    
    protected synchronized void setState(final ClusterNodeState nextState)
    {
        if (state != nextState)
        {
            // Order is very important
            ClusterNodeState lastState = state;
            state = nextState;
            FijiArchipelago.log("Node state changed from "
                    + stateString(lastState) + " to " + stateString(nextState));
            for (NodeStateListener listener : stateListeners)
            {
                listener.stateChanged(this, state, lastState);
            }
        }
    }
    
    public boolean cancelJob(long id)
    {
        ProcessManager<?> pm = runningProcesses.get(id);
        if (pm == null)
        {
            return false;
        }
        else if (xc.queueMessage(MessageType.CANCELJOB, id))
        {
            removeProcess(pm);
            return true;
        }
        return false;
    }
    
    private void removeProcess(ProcessManager pm)
    {
        runningProcesses.remove(pm.getID());
        processHandlers.remove(pm.getID());
        runningCores.addAndGet(-(pm.requestedCores(this)));
    }

    public List<ProcessManager> getRunningProcesses()
    {
        return new ArrayList<ProcessManager> (runningProcesses.values());
    }
    
    /**
     * Adds a NodeStateListener to the list of listeners that are notified when the state of this
     * ClusterNode changes. Immediately upon addition, the listener is called with the current
     * state, and given that the last state was ClusterNodeState.INACTIVE to indicate that this is
     * the initial call.
     * @param listener a NodeStateListener to register with the ClusterNode
     */
    public void addListener(final NodeStateListener listener)
    {
        stateListeners.add(listener);
        listener.stateChanged(this, state, ClusterNodeState.INACTIVE);
    }
    
    public void removeListener(final NodeStateListener listener)
    {
        stateListeners.remove(listener);
    }
    
    public ClusterNodeState getState()
    {
        return state;
    }
    
    public String toString()
    {
        return getHost();
    }
}
