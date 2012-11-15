package archipelago.network;

import archipelago.FijiArchipelago;
import archipelago.NodeManager;
import archipelago.ShellExecListener;
import archipelago.StreamCloseListener;
import archipelago.compute.ProcessListener;
import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.shell.NodeShell;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Larry Lindsey
 */
public class ClusterNode implements MessageListener, StreamCloseListener
{
    

    public class TimeOutException extends Exception
    {
        public TimeOutException()
        {
            super("Timed out waiting for ID");
        }
    }


    private Socket nodeSocket;
//    private final NodeShell shell;
    private MessageTX tx;
    private MessageRX rx;
    private final Hashtable<Long, ProcessListener> processHandlers;
    private final AtomicBoolean ready;
    private long nodeID;
    private NodeManager.NodeParameters nodeParam;
    private AtomicBoolean idSet;

   
    public ClusterNode(Socket socket) throws IOException, InterruptedException,
            TimeOutException
    {
        int waitCnt = 0;
        ready = new AtomicBoolean(false);
        idSet = new AtomicBoolean(false);
        processHandlers = new Hashtable<Long, ProcessListener>();
        nodeID = -1;
        nodeParam = null;

        setClientSocket(socket);
        
        tx.queueMessage("getid");
        
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
            tx.queueMessage("getuser");
        }

        if (getExecPath() == null || getExecPath().equals(""))
        {
            tx.queueMessage("getexecroot");
        }
        else
        {
            tx.queueMessage("setexecroot", getExecPath());
        }

        if (getFilePath() == null || getFilePath().equals(""))
        {
            tx.queueMessage("getfileroot");
        }
        else
        {
            tx.queueMessage("setfileroot", getFilePath());
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
            return tx.queueMessage("setfileroot", path);
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

        tx = new MessageTX(nodeSocket, this);
        rx = new MessageRX(nodeSocket, this, this);

        FijiArchipelago.log("Got Socket from " + s.getInetAddress());

        ready.set(true);
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
        return ready.get();
    }
    
    public boolean exec(String command, ShellExecListener listener)
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
    
    public void setShell(NodeShell shell)
    {
        nodeParam.setShell(shell);
    }
    
    public int numAvailableThreads()
    {
        int n = nodeParam.getNumThreads() - processHandlers.size();
        return n > 0 ? n : 0;
    }

    public <S, T> boolean runProcessManager(ProcessManager<S, T> process, ProcessListener listener)
    {
        if (ready.get())
        {
            if (processHandlers.get(process.getID()) == null)
            {
                FijiArchipelago.debug(getHost() + " scheduling process");
                processHandlers.put(process.getID(), listener);
                return tx.queueMessage("process", process);
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
        tx.queueMessage(message);
    }

    public void handleMessage(ClusterMessage cm)
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
                ProcessManager<?,?> pm = (ProcessManager<?,?>)object;
                ProcessListener listener = processHandlers.remove(pm.getID());

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
        if (ready.get())
        {
            ready.set(false);
            Cluster.getCluster().removeNode(this);
            sendShutdown();
            tx.close();
            rx.close();
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

    public boolean sendShutdown()
    {
        ClusterMessage message = new ClusterMessage();
        message.message = "halt";
        return tx.queueMessage(message);
    }
}
