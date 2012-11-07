package archipelago.network;


import archipelago.FijiArchipelago;
import archipelago.NodeManager;
import archipelago.ShellExecListener;
import archipelago.compute.ProcessManager;
import archipelago.network.shell.NodeShell;
import archipelago.network.server.ArchipelagoServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;


public class Cluster
{
    public static final int DEFAULT_PORT = 3501;
    public static final long DEFAULT_READY_TIMEOUT = 60000;
    public static final int QUEUE_SIZE = 1024;
    private static Cluster cluster = null;

    public static boolean initCluster(NodeShell shell, int port)
    {
        if (cluster == null)
        {
            cluster = new Cluster(shell, port);
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public static boolean initCluster(NodeShell shell)
    {
        return initCluster(shell, DEFAULT_PORT);
    }
    
    public static Cluster getCluster()
    {
        return cluster;
    }
    
    private final int port;
    private NodeShell nodeShell;    
    private final Vector<ClusterNode> nodes;
    private ArchipelagoServer server;
    private final long readyTimeout;
    private long startTime = 0;
    private final ProcessScheduler scheduler;
    private final AtomicBoolean running;
    private final NodeManager nodeManager;
    private String localHostName;


    private Cluster(NodeShell shell, int p)
    {
        port = p;
        readyTimeout = DEFAULT_READY_TIMEOUT;

        nodeShell = shell;
        server = null;
        nodes = new Vector<ClusterNode>();
                 
        running = new AtomicBoolean(false);

        scheduler = new ProcessScheduler(1024, 1000);
        
        nodeManager = new NodeManager();
        
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

    public void setLocalHostName(String host)
    {        
        localHostName = host;
    }

    
    public boolean startServer()
    {
        scheduler.start();
        server = new ArchipelagoServer(this);
        return server.start();
    }
    
    public void close()
    {
        scheduler.close();

        for (ClusterNode node : nodes)
        {
            node.close();
        }

        server.close();
        running.set(false);
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
                + "/plugins/ --allow-multiple --main-class archipelago.Fiji_Archipelago "
                + localHostName + " " + port + " " + id + " 2>&1 > ~/" + host + "_" + id + ".log";
        
        return nodeShell.exec(params, execCommandString,  listener);
    }


    private void addNode(ClusterNode node)
    {
        nodes.add(node);
        scheduler.addNode(node);
    }

    public void removeNode(ClusterNode node)
    {
        nodes.remove(node);
        scheduler.removeNode(node);
        nodeManager.removeParam(node.getID());
    }
    
    public void nodeFromSocket(Socket socket)
    {
        try
        {
            ClusterNode node = new ClusterNode(nodeShell, socket);
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
        boolean go = true;

        final long sTime = System.currentTimeMillis();
        
        FijiArchipelago.log("Waiting for ready nodes");
        
        while (go)
        {

            try
            {
                Thread.sleep(1000); //sleep for a second

                if ((System.currentTimeMillis() - sTime) > readyTimeout)
                {
                    FijiArchipelago.err("Cluster timed out while waiting for nodes to be ready");
                    go = false;
                }
                else
                {                    
                    go = countReadyNodes() <= 0;
                }
            }
            catch (InterruptedException ie)
            {
                go = false;
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
        for (ClusterNode node : nodes)
        {
            if (node.isReady())
            {
                return true;
            }
        }
        return false;
    }

    public ArrayList<ClusterNode> getNodes()
    {
        return new ArrayList<ClusterNode>(nodes);
    }
    
    public boolean queueProcesses(Collection<ProcessManager> processes)
    {
        return scheduler.queueJobs(processes);
    }
    
    public boolean start()
    {
        running.set(true);
        return true;
    }

    public NodeManager getNodeManager()
    {
        return nodeManager;
    }

    private void setTimeoutStart()
    {
        if (startTime == 0)
        {
            startTime = System.currentTimeMillis();
        }
    }

}
