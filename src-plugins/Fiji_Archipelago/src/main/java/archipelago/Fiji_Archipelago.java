package archipelago;

import archipelago.network.Cluster;
import archipelago.network.client.ArchipelagoClient;
import archipelago.network.shell.JSchNodeShell;
import archipelago.network.shell.NodeShell;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

public class Fiji_Archipelago implements PlugIn 
{


    public void run(String arg)
    {
        if (arg.equals("gui"))
        {
            JSchNodeShell.JSchShellParams params = new JSchNodeShell.JSchShellParams(new File("/home/larry/.ssh/id_dsa"));
            NodeShell shell = new JSchNodeShell(params, new SysoutLogger());
            
            Cluster cluster = new Cluster(shell);
            cluster.startCluster();
            cluster.join();
        }        
    }
    
    public static void main(String[] args) throws IOException, InterruptedException
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

        while (client.isActive())
        {
            Thread.sleep(1000);
        }
    }
}
