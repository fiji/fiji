package archipelago.network;

import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.factory.NodeShellFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;


public class ClusterNode implements MessageListener
{

    private Socket nodeSocket;
    private String hostname;
    private String username;
    private NodeShell shell;
    private boolean shellVerified;
    private final InetAddress address;
    private MessageTX tx;
    private MessageRX rx;
    private final Hashtable<Long, ProcessListener> processHandlers;

    public ClusterNode(String host, String user, NodeShellFactory factory) throws UnknownHostException
    {
        hostname = host;
        username = user;
        shell = factory.getShell(this);
        shellVerified = shell != null && shell.verify();
        nodeSocket = null;
        address = InetAddress.getByName(hostname);
        processHandlers = new Hashtable<Long, ProcessListener>();
    }

    public boolean shellReady()
    {
        return shellVerified;
    }

    public boolean setShell(NodeShellFactory factory)
    {
        shell = factory.getShell(this);
        shellVerified = shell != null && shell.verify();
        return shellReady();
    }

    public boolean setClientSocket(final Socket s) throws IOException
    {
        if (s.getInetAddress().equals(getAddress()))
        {
            nodeSocket = s;
            
            tx = new MessageTX(nodeSocket);
            rx = new MessageRX(nodeSocket, this);
            
            return true;
        }
        else
        {
            return false;
        }
    }

    public InetAddress getAddress()
    {
        return address;
    }
    
    public String getHost()
    {
        return hostname;
    }
    
    public String getUser()
    {
        return username;
    }

    public boolean shellActive()
    {
        return shell.isActive();
    }
    
    public boolean exec(String command)
    {
        return shellReady() && shell.exec(command);
    }

    public <S, T> boolean runProcessManager(ProcessManager<S, T> process, ProcessListener listener)
    {
        ClusterMessage message = new ClusterMessage();
        message.message = "process";
        message.o = process;
        if (processHandlers.get(process.getID()) == null)
        {
            processHandlers.put(process.getID(), listener);
            return tx.queueMessage(message);
        }
        else
        {
            return false;
        }
    }

    public void handleMessage(ClusterMessage message) {
        if (message.message.equals("process"))
        {
            ProcessManager<?,?> pm = (ProcessManager<?,?>)message.o;
            ProcessListener listener = processHandlers.remove(pm.getID());
            listener.processFinished(pm);
        }
    }
    
    public boolean sendShutdown()
    {
        ClusterMessage message = new ClusterMessage();
        message.message = "halt";
        return tx.queueMessage(message);
    }
}
