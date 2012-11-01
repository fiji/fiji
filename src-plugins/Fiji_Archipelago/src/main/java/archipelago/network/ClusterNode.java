package archipelago.network;

import archipelago.ShellExecListener;
import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.shell.NodeShell;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClusterNode implements MessageListener
{

    private Socket nodeSocket;
    private String hostname;
    private String username;
    private final NodeShell shell;
    private final InetAddress address;
    private MessageTX tx;
    private MessageRX rx;
    private final Hashtable<Long, ProcessListener> processHandlers;
    private final AtomicBoolean ready;

    public ClusterNode(String host, String user, NodeShell s) throws UnknownHostException
    {
        hostname = host;
        username = user;
        shell = s;

        nodeSocket = null;
        address = InetAddress.getByName(hostname);
        processHandlers = new Hashtable<Long, ProcessListener>();
        ready = new AtomicBoolean(false);
    }

    public boolean setClientSocket(final Socket s) throws IOException
    {
        if (s.getInetAddress().equals(getAddress()))
        {
            nodeSocket = s;
            
            tx = new MessageTX(nodeSocket);
            rx = new MessageRX(nodeSocket, this);
            
            System.out.println("Got Socket");

            ready.set(true);

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

    public boolean isready()
    {
        return ready.get();
    }
    
    public boolean exec(String command, ShellExecListener listener)
    {
        return shell.exec(this, command, listener);
    }
    
    public NodeShell getShell()
    {
        return shell;
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
    
    public void ping()
    {
        ClusterMessage message = new ClusterMessage();
        message.message = "ping";
        tx.queueMessage(message);
    }

    public void handleMessage(ClusterMessage message) {
        if (message.message.equals("process"))
        {
            ProcessManager<?,?> pm = (ProcessManager<?,?>)message.o;
            ProcessListener listener = processHandlers.remove(pm.getID());
            listener.processFinished(pm);
        }
        else
        {
            System.out.println("Got Message " + message.message);
        }
    }

    public void close()
    {
        sendShutdown();
        tx.close();
        rx.close();
    }


    public boolean sendShutdown()
    {
        ClusterMessage message = new ClusterMessage();
        message.message = "halt";
        return tx.queueMessage(message);
    }
}
