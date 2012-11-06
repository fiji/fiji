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
            NodeShell shell = new JSchNodeShell(params, new PrintStreamLogger());
            Cluster.initCluster(shell);

            Cluster cluster = Cluster.getCluster();
            cluster.startServer();
            cluster.join();
        }        
    }
    
    public static void main(String[] args) throws IOException, InterruptedException
    {
        
        System.out.println("Fiji Archipelago main called");
        
        
        if (args.length == 3)
        {
            ArchipelagoClient client = null;
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            long id = Long.parseLong(args[2]);

            FijiArchipelago.setDebugLogger(new PrintStreamLogger());
            FijiArchipelago.setErrorLogger(new PrintStreamLogger());
            FijiArchipelago.setInfoLogger(new PrintStreamLogger());

            client = new ArchipelagoClient(id, host, port);
            
            
            while (client.isActive())
            {
                Thread.sleep(1000);
            }
        }
        else
        {
            System.err.println("Usage: Fiji_Archipelago host port ID");
        }


    }
}
