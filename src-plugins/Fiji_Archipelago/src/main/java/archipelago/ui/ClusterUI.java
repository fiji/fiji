package archipelago.ui;

import archipelago.Cluster;
import archipelago.FijiArchipelago;
import archipelago.listen.ClusterStateListener;
import archipelago.listen.ShellExecListener;
import archipelago.network.node.NodeManager;
import archipelago.network.shell.JSchNodeShell;
import archipelago.network.shell.NodeShell;
import archipelago.util.IJLogger;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import ij.io.OpenDialog;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 */
public class ClusterUI implements ClusterStateListener
{

    private class StateLabel extends Label
    {
        private final String stateString;
        
        public StateLabel(String stateString)
        {
            this.stateString = stateString;
            update("");
        }
        
        public void update(Object o)
        {
            super.setText(stateString + " " + o.toString());
        }
    }
    
    private class CUIMainPanel extends Panel implements ActionListener
    {
        final Button statButton, startStopButton;
        final StateLabel clusterLabel, queueLabel, runningLabel, nodesLabel;
        final ClusterConfigPanel configPanel;
        
        public CUIMainPanel()
        {
            super();
            GridBagConstraints gbc = new GridBagConstraints();

            configPanel = new ClusterConfigPanel();
            // initialize controls
            //cfgButton = new Button("Configure Root Node...");
            statButton = new Button("Show Node Statistics");
            startStopButton = new Button("Start Cluster");

            clusterLabel = new StateLabel("Cluster is");
            queueLabel = new StateLabel("Jobs in queue:");
            runningLabel = new StateLabel("Running jobs: ");
            nodesLabel = new StateLabel("Active nodes: ");
            
            // layout
            super.setLayout(new GridBagLayout());
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            
            super.add(configPanel, gbc);
            super.add(clusterLabel, gbc);
            //super.add(cfgButton, gbc);
            super.add(queueLabel, gbc);
            super.add(runningLabel, gbc);
            super.add(nodesLabel, gbc);

            super.add(statButton, gbc);
            super.add(startStopButton, gbc);
            
            // action commands
            //cfgButton.setActionCommand("configure");
            statButton.setActionCommand("stats");
            startStopButton.setActionCommand("");

            //cfgButton.addActionListener(this);
            statButton.addActionListener(this);
            startStopButton.addActionListener(this);

            startStopButton.setEnabled(false);

            update();

            super.setVisible(true);
        }
        
        
        
        public void actionPerformed(ActionEvent ae)
        {
            if (ae.getActionCommand().equals("configure"))
            {
                configureRootNode();
            }
            else if (ae.getActionCommand().equals("stats"))
            {
                toggleStatsWindow();
            }
            else if (ae.getActionCommand().equals("start"))
            {
                startCluster();
                //cluster.start();
            }
            else if (ae.getActionCommand().equals("stop"))
            {
                GenericDialog gd = new GenericDialog("Really?");
                gd.addMessage("Really stop server?");
                gd.showDialog();
                if (gd.wasOKed())
                {
                    cluster.shutdown();
                }
            }
        }

        private String clusterStateString()
        {
            if (cluster.isActive())
            {
                return "active.";                
            }
            else if (cluster.isStarted())
            {
                return "started.";                
            }
            else if (cluster.isShutdown())
            {
                return "waiting for termination.";
            }
            else if(cluster.isTerminated())
            {
                return "terminated.";
            }
            else if (!isConfigured)
            {
                return "in need of configuration.";
            }
            else
            {
                return "ready to start.";
            }
        }
        
        
        public void update() {
            if (isConfigured)
            {
                startStopButton.setEnabled(true);
            }
            
            if (nodeStatusUI !=null && nodeStatusUI.isVisible())
            {
                statButton.setLabel("Hide Node Statistics");
            }
            else
            {
                statButton.setLabel("Show Node Statistics");
            }
            
            if (cluster != null)
            {
                clusterLabel.update(clusterStateString());
                queueLabel.update(cluster.getQueuedJobCount());
                runningLabel.update(cluster.getRunningJobCount());
                nodesLabel.update(cluster.getRunningNodeCount());

                if (cluster.isStarted())
                {
                    startStopButton.setLabel("Stop Cluster");
                    startStopButton.setActionCommand("stop");
                }
                else
                {
                    startStopButton.setLabel("Start Cluster");
                    startStopButton.setActionCommand("start");
                }
            }            
            else
            {
                clusterLabel.update("is null");
                queueLabel.update(0);
                runningLabel.update(0);
                nodesLabel.update(0);
            }
        }
    }


    /*
    Overall Cluster Configuration
     */

    private class ClusterConfigPanel extends Panel implements ActionListener
    {
        private final Button loadButton, saveButton, rootButton, nodeButton;
        
        public ClusterConfigPanel()
        {
            super();
            

            // initialize controls
            loadButton = new Button("Load Configuration File...");
            saveButton = new Button("Save to Configuration File...");
            rootButton = new Button("Configure Root Node...");
            nodeButton = new Button("Configure Cluster Nodes...");

            // layout
            super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            
            super.add(loadButton);
            super.add(saveButton);
            super.add(rootButton);
            super.add(nodeButton);
            

            // action commands
            loadButton.setActionCommand("load");
            saveButton.setActionCommand("save");
            rootButton.setActionCommand("rootconfig");
            nodeButton.setActionCommand("nodeconfig");

            loadButton.addActionListener(this);
            saveButton.addActionListener(this);
            rootButton.addActionListener(this);
            nodeButton.addActionListener(this);

            super.setVisible(true);
        }


        public void actionPerformed(ActionEvent ae) {
            if (ae.getActionCommand().equals("load"))
            {
                final OpenDialog od = new OpenDialog("Select a .cluster file to load", null);
                final String dirName = od.getDirectory();
                final String fileName = od.getFileName();
                if (fileName != null)
                {
                    loadFromFile(dirName + fileName);
                }
            }
            else if (ae.getActionCommand().equals("save"))
            {
                final OpenDialog od = new OpenDialog("Select a .cluster file to save", null);
                final String dirName = od.getDirectory();
                final String fileName = od.getFileName();
                if (fileName != null)
                {
                    final File f = new File(dirName + fileName);
                    if (f.exists())
                    {
                        GenericDialog gd = new GenericDialog("File Exists");
                        gd.addMessage(fileName + " already exists. Overwrite?");
                        gd.showDialog();
                        if (gd.wasCanceled())
                        {
                            return;
                        }
                    }
                    saveToFile(dirName + fileName);
                }
            }
            else if (ae.getActionCommand().equals("rootconfig"))
            {
                if (cluster.isStarted())
                {
                    GenericDialog whoaDialog = new GenericDialog("Whoa");
                    whoaDialog.addMessage("Changing the root node configuration of a running" +
                            "cluster may result in inexplicable errors. Continue anyway?");
                    whoaDialog.showDialog();
                    if (whoaDialog.wasCanceled())
                    {
                        return;
                    }                    
                }
                
                configureRootNode();
            }
            else if (ae.getActionCommand().equals("nodeconfig"))
            {
                configureClusterNodes();
            }
        }
    }



    private final Cluster cluster;
    private final Frame frame;
    private final ReentrantLock configLock;
    private boolean isConfigured;
    private final CUIMainPanel mainPanel;
    private List<NodeManager.NodeParameters> nodeParameters;
    private final ClusterNodeStatusUI nodeStatusUI;
    private final AtomicBoolean active;
    private final Thread pollThread;
    
    public ClusterUI(Cluster c)
    {
        configLock = new ReentrantLock();
        this.cluster = c;
        isConfigured = this.cluster.isStarted();
        mainPanel = new CUIMainPanel();
        nodeParameters = new ArrayList<NodeManager.NodeParameters>();
        nodeStatusUI = new ClusterNodeStatusUI(cluster, this);
        active = new AtomicBoolean(true);

        frame = new Frame();        
        frame.add(mainPanel);
        frame.setSize(new Dimension(300, 300));
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                GenericDialog gd = new GenericDialog("Shutdown?");
                gd.addMessage("Really Close?");
                gd.showDialog();
                if (gd.wasOKed())
                {
                    cluster.shutdownNow();
                    frame.setVisible(false);
                    frame.removeAll();                    
                    nodeParameters.clear();
                    nodeStatusUI.stop();
                    active.set(false);
                }
            }
        });

        pollThread = new Thread()
        {
            public void run()
            {
                try
                {
                    while (active.get())
                    {
                        Thread.sleep(1000);
                        updateUI();
                    }
                }
                catch (InterruptedException ie)
                {/*Nope*/}
            }
        };

        
        //frame.pack();
        frame.validate();
        pollThread.start();
        cluster.addStateListener(this);
        
        frame.setVisible(true);
    }
    
    public ClusterUI()
    {
        this(Cluster.getCluster());
    }
    
    public void updateUI()
    {
        if (frame.isVisible())
        {
            mainPanel.update();
            nodeStatusUI.update();
        }
    }

    public synchronized void stateChanged(final Cluster c)
    {
        if (c == cluster)
        {
            updateUI();
            nodeStatusUI.stateChanged();
        }
    }

    

    private static void addXMLField(Document doc, Element parent, String field, String value)
    {
        Element e = doc.createElement(field);
        e.appendChild(doc.createTextNode(value));
        parent.appendChild(e);
    }

    private static String getXMLField(Element e, String tag)
    {
        return e.getElementsByTagName(tag).item(0).getTextContent();
    }


    private static boolean configureCluster(
            Cluster cluster,
            int port,
            String execRootRemote,
            String fileRootRemote,
            String execRoot,
            String fileRoot,
            String userName,
            String key)
    {
        final NodeShell shell = new JSchNodeShell(new JSchNodeShell.JSchShellParams(new File(key)),
                new IJLogger());
        final String prefRoot = FijiArchipelago.PREF_ROOT;
        boolean isConfigured = cluster.isStarted() || cluster.init(port);

        cluster.getNodeManager().setStdUser(userName);
        cluster.getNodeManager().setStdExecRoot(execRootRemote);
        cluster.getNodeManager().setStdFileRoot(fileRootRemote);
        cluster.getNodeManager().setStdShell(shell);

        FijiArchipelago.setExecRoot(execRoot);
        FijiArchipelago.setFileRoot(fileRoot);

        //Set prefs
        Prefs.set(prefRoot + ".port", port);
        Prefs.set(prefRoot + ".keyfile", key);
        Prefs.set(prefRoot + ".username", userName);
        Prefs.set(prefRoot + ".execRoot", execRoot);
        Prefs.set(prefRoot + ".fileRoot", fileRoot);
        Prefs.set(prefRoot + ".execRootRemote", execRootRemote);
        Prefs.set(prefRoot + ".fileRootRemote", fileRootRemote);
        Prefs.savePreferences();

        //updateUI();

        return isConfigured;
    }
    
    public static boolean loadClusterFile(final String filePath, final Cluster cluster,
                                  final List<NodeManager.NodeParameters> nodeParams)
            throws ParserConfigurationException, SAXException, IOException
    {
        final File f = new File(filePath);
        final DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(f);
        int port;
        Element rootNode;
        NodeList clusterNodes;
        String execRoot, fileRoot, dExecRoot, dFileRoot, user, key;
        boolean ok;

        doc.getDocumentElement().normalize();

        // Should only have one node in this list             
        rootNode = (Element)doc.getElementsByTagName("rootNode").item(0);

        port = Integer.parseInt(getXMLField(rootNode, "port"));
        execRoot = getXMLField(rootNode, "exec");
        fileRoot = getXMLField(rootNode, "file");
        dExecRoot = getXMLField(rootNode, "default-exec");
        dFileRoot = getXMLField(rootNode, "default-file");
        user = getXMLField(rootNode, "default-user");
        key = getXMLField(rootNode, "key");

        ok = configureCluster(cluster, port, dExecRoot, dFileRoot, execRoot, fileRoot,  user, key);

        

        nodeParams.clear();


        clusterNodes = doc.getElementsByTagName("clusterNode");

        for (int i = 0; i < clusterNodes.getLength(); ++i)
        {
            final Element node = (Element)clusterNodes.item(i);
            final NodeManager.NodeParameters nodeParam = cluster.getNodeManager().newParam();

            nodeParam.setHost(getXMLField(node, "host"));
            nodeParam.setUser(getXMLField(node, "user"));
            nodeParam.setPort(Integer.parseInt(getXMLField(node, "ssh-port")));
            nodeParam.setExecRoot(getXMLField(node, "exec"));
            nodeParam.setFileRoot(getXMLField(node, "file"));
            nodeParam.setThreadLimit(Integer.parseInt(getXMLField(node, "limit")));

            nodeParams.add(nodeParam);
            //nodeParameters.add(nodeParam);
        }

        return ok;
    }
    
    public synchronized boolean loadFromFile(final String file)
    {
        try
        {
            ArrayList<NodeManager.NodeParameters> params = new ArrayList<NodeManager.NodeParameters>();
            isConfigured = loadClusterFile(file, cluster, params);

            for (NodeManager.NodeParameters p : params)
            {
                addNode(p);
            }

            if (!isConfigured)
            {
                configError("Could not configure Cluster on port " + cluster.getPort());
            }
        }
        catch (ParserConfigurationException pce)
        {
            configError("" + pce);
        }
        catch (SAXException se)
        {
            configError("" + se);
        }
        catch (IOException ioe)
        {
            configError("" + ioe);
        }
        updateUI();
        return isConfigured;
    }

    private void toggleStatsWindow()
    {
        nodeStatusUI.toggleVisible();
        updateUI();
    }


    private synchronized void startCluster()
    {
        if (!cluster.isStarted())
        {
            FijiArchipelago.log("Starting Cluster");
            ShellExecListener meh = new ShellExecListener() {
                public void execFinished(long nodeID, Exception e) {

                }
            };
            cluster.start();
            for (NodeManager.NodeParameters param : nodeParameters)
            {
                FijiArchipelago.log("Starting Node " + param.getHost());
                cluster.startNode(param, meh);
            }
            
            updateUI();
        }
    }
    
    private void addNode(NodeManager.NodeParameters param)
    {
        
        
        if (cluster.isStarted())
        {
            ShellExecListener meh = new ShellExecListener() {
                public void execFinished(long nodeID, Exception e) {
                    
                }
            };
            cluster.startNode(param, meh);
        }
        else
        {
            nodeParameters.add(param);
        }
    }
    
    private void configError(String reason)
    {
        IJ.error("Error loading configuration file: " + reason);
    }
    
    private synchronized void saveToFile(final String file)
    {
        System.out.println("Save called");
        try
        {
            final File f = new File(file);
            final DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final NodeManager nm = cluster.getNodeManager();
            final ArrayList<NodeManager.NodeParameters> params = new ArrayList<NodeManager.NodeParameters>();
            final Document doc = docBuilder.newDocument();
            final Element clusterXML = doc.createElement("cluster");
            final Element rootNode = doc.createElement("rootNode");            
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            final StreamResult result = new StreamResult(f);
            DOMSource source;
            
            params.addAll(nodeParameters);
            params.addAll(cluster.getNodesParameters());
            
            doc.appendChild(clusterXML);

            System.out.println("Writing cluster params");

            addXMLField(doc, rootNode, "port", "" + cluster.getServerPort());
            addXMLField(doc, rootNode, "exec", FijiArchipelago.getExecRoot());
            addXMLField(doc, rootNode, "file", FijiArchipelago.getFileRoot());
            addXMLField(doc, rootNode, "default-exec", nm.getStdExecRoot());
            addXMLField(doc, rootNode, "default-file", nm.getStdFileRoot());
            addXMLField(doc, rootNode, "default-user", nm.getStdUser());
            addXMLField(doc, rootNode, "key", Prefs.get(FijiArchipelago.PREF_ROOT +
                    ".keyfile", IJ.isWindows() ? "" : System.getenv("HOME") + "/.ssh/id_dsa"));
            clusterXML.appendChild(rootNode);

            System.out.println("Writing " + params.size() + " nodes");

            for (NodeManager.NodeParameters np : params)
            {
                Element clusterNode = doc.createElement("clusterNode");
                
                addXMLField(doc, clusterNode, "host", np.getHost());
                addXMLField(doc, clusterNode, "user", np.getUser());
                addXMLField(doc, clusterNode, "ssh-port", "" + np.getPort());
                addXMLField(doc, clusterNode, "exec", np.getExecRoot());
                addXMLField(doc, clusterNode, "file", np.getFileRoot());
                addXMLField(doc, clusterNode, "limit", "" + np.getThreadLimit());

               clusterXML.appendChild(clusterNode);
            }

            source = new DOMSource(doc);
            transformer.transform(source, result);
                    
        }
        catch (ParserConfigurationException pce)
        {
            IJ.error("Could not save to " + file + ": " + pce);
        }
        catch (TransformerException te)
        {
            IJ.error("Could not save to " + file + ": " + te);
        }
        
    }

    private synchronized void configureClusterNodes()
    {
        ArrayList<NodeManager.NodeParameters> newParams = new ArrayList<NodeManager.NodeParameters>();
        if(NodeConfigurationUI.nodeConfigurationUI(cluster.getNodeManager(), nodeParameters, newParams))
        {
            System.out.println("Got " + newParams.size() + " new params");
            nodeParameters.clear();
            for (NodeManager.NodeParameters param : newParams)
            {
                addNode(param);
            }
            System.out.println("Now we have " + nodeParameters.size() + " params");
        }
    }

    private boolean configureRootNode()
    {
        if (!configLock.tryLock())
        {
            return false;
        }

        final String prefRoot = FijiArchipelago.PREF_ROOT;
        final GenericDialog gd = new GenericDialog("Start Cluster");
        int port, dPort;
        String dKeyfile, dExecRoot, dFileRoot, dUserName, dExecRootRemote, dFileRootRemote;
        String keyfile, execRoot, fileRoot, userName, execRootRemote, fileRootRemote;
        NodeShell shell;

        //Set default variables
        dPort = Integer.parseInt(Prefs.get(prefRoot + ".port", "" + Cluster.DEFAULT_PORT));
        dKeyfile = Prefs.get(prefRoot + ".keyfile", IJ.isWindows() ? ""
                : System.getenv("HOME") + "/.ssh/id_dsa");
        dExecRoot = Prefs.get(prefRoot + ".execRoot", "");
        dFileRoot = Prefs.get(prefRoot + ".fileRoot", "");
        dExecRootRemote = Prefs.get(prefRoot + ".execRootRemote", "");
        dFileRootRemote = Prefs.get(prefRoot + ".fileRootRemote", "");
        dUserName = Prefs.get(prefRoot + ".username", System.getenv("USER"));

        //Setup the dialog
        gd.addMessage("Root Node Configuration");
        gd.addNumericField("\tServer Port Number", dPort, 0);
        gd.addStringField("\tSSH Private Key File", dKeyfile, 64);
        gd.addStringField("\tLocal Exec Root", dExecRoot, 64);
        gd.addStringField("\tLocal File Root", dFileRoot, 64);
        gd.addMessage("Remote Node Defaults");
        gd.addStringField("\tUser Name", dUserName);
        gd.addStringField("\tDefault Client Exec Root", dExecRootRemote, 64);
        gd.addStringField("\tDefault Client File Root", dFileRootRemote, 64);


        gd.showDialog();
        if (gd.wasCanceled())
        {
            return false;
        }

        //Get results
        port = (int)gd.getNextNumber();
        keyfile = gd.getNextString();
        execRoot = gd.getNextString();
        fileRoot = gd.getNextString();
        userName = gd.getNextString();
        execRootRemote = gd.getNextString();
        fileRootRemote = gd.getNextString();


        //Do initialization
        configureCluster(cluster, port, execRootRemote, fileRootRemote, execRoot, fileRoot, userName,
                keyfile);

        if (!isConfigured)
        {
            configError("Could not configure Cluster on port " + cluster.getPort());
        }
        
        configLock.unlock();
        
        updateUI();
        return isConfigured;
    }
    

}
