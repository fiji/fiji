package archipelago.network;


import archipelago.ShellExecListener;
import archipelago.network.shell.NodeShell;
import archipelago.network.server.ArchipelagoServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class Cluster
{
    public static int DEFAULT_PORT = 3501;
    public static long DEFAULT_READY_TIMEOUT = 60000;
    private int port;
    private NodeShell nodeShell;
    private final HashMap<InetAddress, ClusterNode> nodes;
    private ArchipelagoServer server;
    private String clientExecRoot = "/nfs/data1/home/larry/workspace/fiji/";
    private final long readyTimeout;
    private long startTime = 0;
    
    public Cluster(NodeShell shell)
    {
        this(DEFAULT_PORT, shell);
    }
    
    public Cluster(int p, NodeShell shell)
    {
        port = p;
        readyTimeout = DEFAULT_READY_TIMEOUT;

        nodeShell = shell;
        server = null;
        nodes = new HashMap<InetAddress, ClusterNode>();

        try
        {
            ClusterNode node = new ClusterNode("khlab-cortex",
                    "larry", nodeShell);
            nodes.put(InetAddress.getByName(("khlab-cortex")), node);
        }
        catch (UnknownHostException uhe)
        {
            System.err.println("Error while initting cluster");
        }
    }
    
    public boolean startCluster()
    {
        server = new ArchipelagoServer(this);
        if (server.start())
        {
            initNodes();
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public void close()
    {
        for (ClusterNode node : nodes.values())
        {
            node.close();
        }

        server.close();
    }
    
    public int getServerPort()
    {
        return port;
    }
    
    public void assignSocketToNode(Socket s)
    {
        ClusterNode node = nodes.get(s.getInetAddress());
        if (node != null)
        {
            try
            {
                node.setClientSocket(s);
            }
            catch (IOException ioe)
            {
                System.err.println("Error assigning socket to node");
            }
        }
    }

    public void waitUntilReady()
    {
        boolean go = true;
        
        System.out.println("Waiting until read");
        
        while (go)
        {
            try
            {
                System.out.println("Sleeping");
                Thread.sleep(1000); //sleep for a second

                System.out.println("Done sleeping");
                
                if ((System.currentTimeMillis() - startTime) > readyTimeout)
                {
                    System.out.println("Timed out while waiting");
                    go = false;
                }
                else
                {
                    System.out.println("Checking all nodes for readiness");
                    go = false;

                    for (ClusterNode node : nodes.values())
                    {
                        go |= !node.isready();
                        if (node.isready())
                        {
                            System.out.println("Node " + node.getHost() + " is ready ");
                        }
                    }

                    if (go)
                    {
                        System.out.println("Not ready");
                    }
                    else
                    {
                        System.out.println("All ready");
                    }
                        
                }
            }
            catch (InterruptedException ie)
            {
                go = false;
            }
        }
    }

    public boolean join()
    {
        return server.join();
    }
    
    protected void initNodes()
    {
        try
        {
            String host = InetAddress.getLocalHost().getCanonicalHostName();
            startTime = System.currentTimeMillis();
            for (ClusterNode node: nodes.values())
            {
                node.exec(clientExecRoot + "/fiji --jar-path " + clientExecRoot + "/plugins/ --main-class archipelago.Fiji_Archipelago " + host + "> ~/arch.log",
                        new ShellExecListener()
                        {
                            public void execFinished(ClusterNode node, Exception e) {
                                System.out.println("Finished executing on " + node.getHost());
                            }
                        });
            }
        }
        catch (UnknownHostException uhe)
        {
            System.err.println("Error getting my own host name");
            startTime = 0;
        }
    }


    public ArrayList<ClusterNode> getNodes()
    {
        return new ArrayList<ClusterNode>(nodes.values());
    }
}
