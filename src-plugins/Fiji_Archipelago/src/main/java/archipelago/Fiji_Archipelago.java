package archipelago;

import archipelago.network.client.ArchipelagoClient;
import ij.plugin.PlugIn;
import java.io.IOException;

public class Fiji_Archipelago implements PlugIn 
{

    public void run(String arg)
    {
        if (arg.equals("gui"))
        {
            FijiArchipelago.runClusterGUI();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        /*
        main should only be called on client nodes. This sets up a socket connection with the
        cluster server, whose information is passed into args[] at the command line.
        */
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
