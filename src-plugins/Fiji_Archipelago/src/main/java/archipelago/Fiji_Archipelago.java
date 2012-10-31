package archipelago;

import archipelago.network.Cluster;
import archipelago.network.client.ArchipelagoClient;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.IOException;

public class Fiji_Archipelago implements PlugIn 
{


    public void run(String arg)
    {
        if (arg.equals("gui"))
        {
            Cluster cluster = new Cluster();
            cluster.startCluster();
            cluster.join();
        }        
    }
    
    public static void main(String[] args) throws IOException
    {
        String host = "";
        ArchipelagoClient client = null;
        
        if (args.length == 0)
        {
            ImageJ.main(args);
            System.exit(0);
        }
        else
        {
            host = args[0];
        }
        
        if (args.length > 1)
        {
            int port = Integer.parseInt(args[1]);
            client = new ArchipelagoClient(host, port);
        }
        else
        {
            client = new ArchipelagoClient(host);
        }

        client.join();
    }
}
