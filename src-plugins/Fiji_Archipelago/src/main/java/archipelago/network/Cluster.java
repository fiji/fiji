package archipelago.network;

import archipelago.network.factory.JSchNodeShellFactory;
import archipelago.network.factory.NodeShellFactory;
import archipelago.network.server.ArchipelagoServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;


public class Cluster
{
    public static int DEFAULT_PORT = 3501;
    private int port;
    private NodeShellFactory nodeShellFactory;
    HashMap<InetAddress, ClusterNode> nodes;
    private ArchipelagoServer server;
    String clientExecRoot = "/nfs/data1/public/fiji/";
    
    public Cluster()
    {
        this(DEFAULT_PORT);
    }
    
    public Cluster(int p)
    {
        port = p;
        nodeShellFactory = new JSchNodeShellFactory(new File("/home/larry/.ssh/id_dsa"));
        server = null;
        
        try
        {
            nodes.put(InetAddress.getByName(("khlab-cortex")), new ClusterNode("khlab-cortex",
                    "larry", nodeShellFactory));
        }
        catch (UnknownHostException uhe)
        {
            //
        }
    }
    
    public void startCluster()
    {
        server = new ArchipelagoServer(this);
        server.start();
        initNodes();
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
            for (ClusterNode node: nodes.values())
            {
                node.exec(clientExecRoot + "/fiji --jar-path " + clientExecRoot + "/plugins/ --main-class archipelago.Fiji_Archipelago " + InetAddress.getLocalHost());
            }
        }
        catch (UnknownHostException uhe)
        {

        }
    }


}
