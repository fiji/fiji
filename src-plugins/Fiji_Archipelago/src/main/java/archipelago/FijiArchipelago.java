package archipelago;

import archipelago.network.Cluster;
import archipelago.ui.ClusterNodeConfigUI;
import ij.gui.GenericDialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
/**
 *
 * @author Larry Lindsey
 */
public final class FijiArchipelago
{
    public static final String PREF_ROOT = "FijiArchipelago";
    private static EasyLogger logger = new NullLogger();
    private static EasyLogger errorLogger = new NullLogger();
    private static EasyLogger debugLogger = new NullLogger();
    private static final AtomicLong nextID = new AtomicLong(0);
    private static String fileRoot = "";
    private static String execRoot = "";
    

    private FijiArchipelago(){}

    
    public static boolean fileIsInRoot(final String path)
    {
        File file = new File(path);
        return file.getAbsolutePath().startsWith(fileRoot);
    }

    public static synchronized void setFileRoot(final String root)
    {
        //Ensure that file root ends with /
        fileRoot = root.endsWith("/") ? root : root + "/";
    }
    
    public static synchronized void setExecRoot(final String root)
    {
        execRoot = root.endsWith("/") ? root : root + "/";
    }
    
    public static String getFileRoot()
    {
        return fileRoot;
    }
    
    public static String getExecRoot()
    {
        return execRoot;
    }
    
    public static String truncateFileRoot(String filename)
    {
        return truncateFileRoot(new File(filename));
    }
    
    public static String truncateFileRoot(File file)
    {
        String filename = file.getAbsolutePath();
        if (filename.startsWith(fileRoot))
        {
            return filename.replaceFirst(fileRoot, "");
        }
        else
        {
            return filename;
        }
    }
    
    
    public static synchronized void setInfoLogger(final EasyLogger l)
    {
        logger = l;
    }
    
    public static synchronized void setErrorLogger(final EasyLogger l)
    {
        errorLogger = l;
    }

    public static synchronized void setDebugLogger(final EasyLogger l)
    {
        debugLogger = l;
    }
    
    public static synchronized void log(final String s)
    {
        logger.log(s);
    }
    
    public static synchronized void err(final String s)
    {
        errorLogger.log(s);
    }
    
    public static synchronized void debug(final String s)
    {
        debugLogger.log(s);
    }

    public static synchronized long getUniqueID()
    {
        return nextID.incrementAndGet();
    }

    public static void runClusterGUI()
    {
        //Start Cluster... called through the plugin menu.
        FijiArchipelago.setDebugLogger(new PrintStreamLogger());
        FijiArchipelago.setInfoLogger(new IJLogger());
        FijiArchipelago.setErrorLogger(new IJPopupLogger());

        FijiArchipelago.log("Starting ");


        ClusterNodeConfigUI ui = new ClusterNodeConfigUI();

        if (ui.wasOKed())
        {
            final Frame stopFrame = new Frame("Cluster is Running");
            final Button stopButton = new Button("Stop Cluster");

            //Dumb ShellExecListener
            ShellExecListener meh = new ShellExecListener() {
                public void execFinished(long nodeID, Exception e)
                {
                    if (e == null)
                    {
                        FijiArchipelago.log("Node " + nodeID + " has finished");
                    }
                    else
                    {
                        FijiArchipelago.log("Connection to nodeID " + nodeID
                                + " disconnected with Exception: " + e);
                    }
                }
            };

            // Start the cluster server, add node params
            Cluster.getCluster().startServer();
            for (NodeManager.NodeParameters np
                    : ui.parameterList(Cluster.getCluster().getNodeManager()))
            {
                FijiArchipelago.debug("" + np);
                Cluster.getCluster().startNode(np, meh);
            }

            // Setup a dialog to stop the server when we're done.
            // TODO: Make a window that gives better information, ie, number of nodes currently
            // running, jobs in queue, etc.
            stopFrame.add(stopButton);

            stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    GenericDialog gd = new GenericDialog("Really?");
                    gd.addMessage("Really stop server?");
                    gd.showDialog();
                    if (gd.wasOKed())
                    {
                        Cluster.getCluster().close();
                        stopFrame.setVisible(false);
                        stopFrame.remove(stopButton);
                    }
                }
            });

            stopFrame.setPreferredSize(new Dimension(512, 512));
            stopButton.setPreferredSize(new Dimension(512, 512));
            stopFrame.validate();
            stopFrame.setVisible(true);
        }
    }
}
