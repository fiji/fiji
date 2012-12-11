package archipelago;

import archipelago.listen.ClusterStateListener;
import archipelago.listen.ShellExecListener;
import archipelago.network.node.NodeManager;
import archipelago.ui.ClusterNodeConfigUI;
import archipelago.util.*;
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

    private static class ClusterStateUpdater implements ClusterStateListener
    {
        private final Frame stopFrame;
        private final Label nodeCount, jobCount, queuedCount, state;
        
        public ClusterStateUpdater()
        {
            final Button stopButton = new Button("Stop Cluster");

            stopFrame = new Frame("Cluster is Running");            
            stopFrame.setLayout(new GridLayout(5,1));
            nodeCount = new Label();
            jobCount = new Label();
            queuedCount = new Label();
            state = new Label();
            
            stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    GenericDialog gd = new GenericDialog("Really?");
                    gd.addMessage("Really stop server?");
                    gd.showDialog();
                    if (gd.wasOKed())
                    {
                        Cluster.getCluster().shutdown();
                    }
                }
            });

            stopFrame.add(state);
            stopFrame.add(nodeCount);
            stopFrame.add(queuedCount);
            stopFrame.add(jobCount);
            stopFrame.add(stopButton);

            stopFrame.setSize(new Dimension(256, 256));

            stopFrame.validate();
            stopFrame.setVisible(true);
        }

        public synchronized void stateChanged(Cluster cluster)
        {
            if (cluster.isActive())
            {
                state.setText("Cluster is Active");                
            }
            else if (cluster.isShutdown() && !cluster.isTerminated())
            {
                state.setText("Cluster is Stopping...");
            }
            else if (cluster.isTerminated())
            {
                stopFrame.setVisible(false);
                stopFrame.removeAll();
            }
            
            jobCount.setText("Running jobs: " + cluster.getRunningJobCount());
            queuedCount.setText("Queued jobs: " + cluster.getQueuedJobCount());
            nodeCount.setText("Running Nodes: " + cluster.getRunningNodeCount());

            stopFrame.repaint();
        }
    }
    
    
    
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
    


    public static boolean runClusterGUI()
    {
        //Start Cluster... called through the plugin menu.
        FijiArchipelago.setDebugLogger(new PrintStreamLogger());
        FijiArchipelago.setInfoLogger(new IJLogger());
        FijiArchipelago.setErrorLogger(new IJPopupLogger());

        FijiArchipelago.log("Starting ");


        ClusterNodeConfigUI ui = new ClusterNodeConfigUI();

        if (ui.wasOKed())
        {
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

            Cluster.getCluster().addStateListener(new ClusterStateUpdater());
            return true;
        }
        else
        {
            return false;
        }
    }
}
