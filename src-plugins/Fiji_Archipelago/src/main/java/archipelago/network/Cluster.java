package archipelago.network;


import archipelago.FijiArchipelago;
import archipelago.NodeManager;
import archipelago.ShellExecListener;
import archipelago.compute.ProcessManager;
import archipelago.network.server.ArchipelagoServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Larry Lindsey
 */
public class Cluster
{
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
    private final ProcessScheduler scheduler;
    private final AtomicBoolean running;
    private final AtomicBoolean active;
    private final NodeManager nodeManager;
    private ArchipelagoServer server;
    private String localHostName;


    private Cluster(int p)
    {        
        nodes = new Vector<ClusterNode>();
                 
        running = new AtomicBoolean(false);
        active = new AtomicBoolean(true);

        scheduler = new ProcessScheduler(1024, 1000);
        
        nodeManager = new NodeManager();

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
        if (!running.get())
        {
            port = p;
            server = null;
            nodes.clear();
            scheduler.close();
            nodeManager.clear();
            active.set(true);
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

    
    
    
    public void close()
    {
        scheduler.close();

        for (ClusterNode node : nodes)
        {
            node.close();
            nodeManager.removeParam(node.getID());
        }

        nodes.clear();

        server.close();
        running.set(false);
        active.set(false);
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
        boolean wait = true;

        final long sTime = System.currentTimeMillis();
        
        FijiArchipelago.log("Waiting for ready nodes");
        
        while (wait)
        {

            try
            {
                Thread.sleep(1000); //sleep for a second

                if ((System.currentTimeMillis() - sTime) > timeout)
                {
                    FijiArchipelago.err("Cluster timed out while waiting for nodes to be ready");
                    wait = false;
                }
                else
                {                    
                    wait = countReadyNodes() <= 0;
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
    
    public boolean queueProcess(ProcessManager process)
    {
        return scheduler.queueJobs(Collections.singleton(process));
    }
    
    public boolean queueProcesses(Collection<ProcessManager> processes)
    {
        return scheduler.queueJobs(processes);
    }
    
    public void start()
    {
        running.set(true);
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
        return active.get();
    }

}
