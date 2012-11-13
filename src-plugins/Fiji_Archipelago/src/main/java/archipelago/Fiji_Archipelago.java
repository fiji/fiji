package archipelago;

import archipelago.network.Cluster;
import archipelago.network.client.ArchipelagoClient;
import archipelago.network.shell.JSchNodeShell;
import archipelago.network.shell.NodeShell;
import archipelago.ui.ClusterNodeConfigUI;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

public class Fiji_Archipelago implements PlugIn 
{
    



    
    public void clusterOptionUI()
    {
        FijiArchipelago.setDebugLogger(new PrintStreamLogger());
        FijiArchipelago.setInfoLogger(new IJLogger());
        FijiArchipelago.setErrorLogger(new IJPopupLogger());

        FijiArchipelago.debug("Cluster Option UI Starting");

        if (Cluster.activeCluster())
        {
            GenericDialog gd = new GenericDialog("Active Cluster");
            gd.addMessage("There is an active cluster already, shut it down?");
            gd.showDialog();
            if (gd.wasCanceled())
            {
                return;
            }
            else
            {
                Cluster.getCluster().close();
            }
        }

        if (!Cluster.activeCluster())
        {
            final String prefRoot = FijiArchipelago.PREF_ROOT;
            final GenericDialog gd = new GenericDialog("Start Cluster");
            int port, dPort;
            String dKeyfile, dExecRoot, dFileRoot, dUserName; 
            String keyfile, execRoot, fileRoot, userName;
            NodeShell shell;

            FijiArchipelago.debug("Cluster was not active, activating...");
            
            //Set default variables
            dPort = Prefs.getInt(prefRoot + ".port", Cluster.DEFAULT_PORT);
            dKeyfile = Prefs.getString(prefRoot  + ".keyfile", IJ.isWindows() ? ""
                    : System.getenv("HOME") + "/.ssh/id_dsa");
            dExecRoot = Prefs.getString(prefRoot  + ".execRoot", "");
            dFileRoot = Prefs.getString(prefRoot + ".fileRoot", "");
            dUserName = Prefs.getString(prefRoot + ".username", System.getenv("USER"));
            
            //Setup the dialog
            gd.addMessage("Cluster Configuration");            
            gd.addNumericField("Server Port Number", dPort, 0);
            gd.addStringField("Remote Machine User Name", dUserName);
            gd.addStringField("SSH Private Key File", dKeyfile, 64);
            gd.addStringField("Default Exec Root for Remote Nodes", dExecRoot, 64);
            gd.addStringField("Default File Root for Remote Nodes", dFileRoot, 64);

            
            gd.showDialog();
            if (gd.wasCanceled())
            {
                FijiArchipelago.debug("Dialog was cancelled");
                return;
            }

            //Get results
            port = (int)gd.getNextNumber();
            userName = gd.getNextString();
            keyfile = gd.getNextString();
            execRoot = gd.getNextString();
            fileRoot = gd.getNextString();
            

            //Do initialization
            shell = new JSchNodeShell(new JSchNodeShell.JSchShellParams(new File(keyfile)),
                    new IJLogger());


            FijiArchipelago.debug("Initializing Cluster");
            
            Cluster.initCluster(port);
            Cluster.getCluster().getNodeManager().setStdUser(userName);
            Cluster.getCluster().getNodeManager().setStdExecRoot(execRoot);
            Cluster.getCluster().getNodeManager().setStdFileRoot(fileRoot);
            Cluster.getCluster().getNodeManager().setStdShell(shell);

            //Set prefs
            Prefs.set(prefRoot + ".port", port);
            Prefs.set(prefRoot + ".keyfile", keyfile);
            Prefs.set(prefRoot + ".username", userName);
            Prefs.set(prefRoot + ".execRoot", execRoot);
            Prefs.set(prefRoot + ".fileRoot", fileRoot);
            Prefs.savePreferences();
            
            System.out.println("Exec Root: " + Prefs.getString(prefRoot + ".execRoot"));
            System.out.println("File Root: " + Prefs.getString(prefRoot + ".fileRoot"));
        }


    }

    public void run(String arg)
    {
        System.out.println("Fiji_Archipelago: Got arg " + arg);
        
        if (arg.equals("gui"))
        {
            
                        
            clusterOptionUI();
            if (Cluster.activeCluster())
            {
                FijiArchipelago.debug("UI: Cluster was active. Doing node config.");
                ClusterNodeConfigUI ui = new ClusterNodeConfigUI();


                if (ui.wasOKed())
                {
                    FijiArchipelago.debug("UI: Got OK");
                    for (NodeManager.NodeParameters np : ui.parameterList(Cluster.getCluster().getNodeManager()))
                    {
                        FijiArchipelago.debug("" + np);
                    }
                }
                else
                {
                    FijiArchipelago.debug("UI: Cancelled");
                }
            }
            else
            {
                FijiArchipelago.debug("UI: Cluster was not active");
            }
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
