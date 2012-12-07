package archipelago.network.node;

import archipelago.*;
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


    private Socket nodeSocket;
    private MessageXC xc;
    
    private final Hashtable<Long, ProcessListener> processHandlers;
    private final Hashtable<Long, ProcessManager> runningProcesses;
    private final AtomicBoolean ready;
    private long nodeID;
    private NodeManager.NodeParameters nodeParam;
    private AtomicBoolean idSet;
    private ClusterNodeState state;
    private final Vector<NodeStateListener> stateListeners;

   
    public ClusterNode(Socket socket) throws IOException, InterruptedException,
            TimeOutException
    {
        state = ClusterNodeState.INACTIVE;
        int waitCnt = 0;
        ready = new AtomicBoolean(false);
        idSet = new AtomicBoolean(false);
        processHandlers = new Hashtable<Long, ProcessListener>();
        runningProcesses = new Hashtable<Long, ProcessManager>();
        nodeID = -1;
        nodeParam = null;
        stateListeners = new Vector<NodeStateListener>();

        setClientSocket(socket);
        
        xc.queueMessage("getid");
        
        while (!idSet.get())
        {
            Thread.sleep(100);
            if (waitCnt++ > 1000)
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
            xc.queueMessage("getuser");
        }

        if (getExecPath() == null || getExecPath().equals(""))
        {
            xc.queueMessage("getexecroot");
        }
        else
        {
            xc.queueMessage("setexecroot", getExecPath());
        }

        if (getFilePath() == null || getFilePath().equals(""))
        {
            xc.queueMessage("getfileroot");
        }
        else
        {
            xc.queueMessage("setfileroot", getFilePath());
        }
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
            return xc.queueMessage("setfileroot", path);
        }
    }

    public synchronized void streamClosed()
    {
        FijiArchipelago.log("Stream closed on " + getHost());
        close();
    }
    
    private void setClientSocket(final Socket s) throws IOException
    {
        nodeSocket = s;

        xc = new MessageXC(s.getInputStream(), s.getOutputStream(), this,
                s.getInetAddress().getHostName());

        FijiArchipelago.log("Got Socket from " + s.getInetAddress());

        ready.set(true);
        setState(ClusterNodeState.ACTIVE);
    }

    public String getHost()
    {
        return nodeParam.getHost();
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
    
    public void setShell(final NodeShell shell)
    {
        nodeParam.setShell(shell);
    }
    
    public int numAvailableThreads()
    {
        int n = nodeParam.getNumThreads() - processHandlers.size();
        return n > 0 ? n : 0;
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
                FijiArchipelago.debug(getHost() + " scheduling process");
                processHandlers.put(process.getID(), listener);
                runningProcesses.put(process.getID(), process);
                process.setRunningOn(this);
                return xc.queueMessage("process", process);
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
        ClusterMessage message = new ClusterMessage();
        message.message = "ping";
        xc.queueMessage(message);
    }

    public void handleMessage(final ClusterMessage cm)
    {
        String message = cm.message;
        Object object = cm.o;

        try
        {
            if (message.equals("id"))
            {
                Long id = (Long)object;
                nodeParam = Cluster.getCluster().getNodeManager().getParam(id);
                FijiArchipelago.debug("Got id message. Setting ID to " + id + ". Param: " + nodeParam);
                nodeID = id;
                nodeParam.setNode(this);
                idSet.set(true);
            }
            else if (message.equals("process"))
            {
                ProcessManager<?> pm = (ProcessManager<?>)object;
                ProcessListener listener = processHandlers.remove(pm.getID());
                runningProcesses.remove(pm.getID());

                FijiArchipelago.log("Got process results from " + getHost());

                listener.processFinished(pm);
            }
            else if (message.equals("ping"))
            {
                FijiArchipelago.log("Received ping from " + getHost());
            }
            else if (message.equals("user"))
            {
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
            } 
            else if (message.equals("setfileroot"))
            {
                setFilePath((String)object);
            }
            else if (message.equals("setexecroot"))
            {
                setExecPath((String)object);
            }
            else if (message.equals("error"))
            {
                Exception e = (Exception)object;
                FijiArchipelago.err("Remote client on " + getHost() + " experienced an error: "
                        + e);
            }
            else
            {
                FijiArchipelago.log("Got unexpected message " + message);
            }
            
            
        }
        catch (ClassCastException cce)
        {
            FijiArchipelago.err("Caught ClassCastException while handling message " 
                    + message + " on " + getHost() + " : "+ cce);
        }
        catch (NullPointerException npe)
        {
            FijiArchipelago.err("Expected a message object but got null for " + message + " on "
                    + getHost());
        }
    }

    public synchronized void close()
    {
        if (isReady())
        {
            sendShutdown();
            xc.close();
            setState(ClusterNodeState.STOPPED);

            try
            {
                nodeSocket.close();
            }
            catch (IOException ioe)
            {
                //meh
            }
        }
    }

    private boolean sendShutdown()
    {
        ClusterMessage message = new ClusterMessage();
        message.message = "halt";
        return xc.queueMessage(message);
    }
    
    protected synchronized void setState(final ClusterNodeState nextState)
    {
        if (state != nextState)
        {
            // Order is very important
            ClusterNodeState lastState = state;
            state = nextState;
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
        else if (xc.queueMessage("cancel", id))
        {
            processHandlers.remove(id);
            runningProcesses.remove(id);
            return true;
        }
        return false;
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
}
