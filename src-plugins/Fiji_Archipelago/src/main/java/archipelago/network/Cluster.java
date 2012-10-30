package archipelago.network;

import archipelago.network.factory.JSchNodeShellFactory;
import archipelago.network.factory.NodeShellFactory;

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
    
    public Cluster()
    {
        this(DEFAULT_PORT);
    }
    
    public Cluster(int p)
    {
        port = p;
        nodeShellFactory = new JSchNodeShellFactory(new File("/home/larry/.ssh/id_dsa"));
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
    
    public void initNodes()
    {
        for (ClusterNode node: nodes.values())
        {
            node.exec("/data/fiji/fiji");
        }
    }


}
