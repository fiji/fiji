/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.ui;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.ClusterStateListener;
import edu.utexas.clm.archipelago.listen.ShellExecListener;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.shell.JSchNodeShell;
import edu.utexas.clm.archipelago.network.shell.NodeShell;
import edu.utexas.clm.archipelago.util.IJLogger;
import edu.utexas.clm.archipelago.util.NullLogger;
import edu.utexas.clm.archipelago.util.PrintStreamLogger;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 */
public class ClusterUI implements ClusterStateListener, ArchipelagoUI
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
        final Checkbox debugCheck;
        final StateLabel clusterLabel, queueLabel, runningLabel, nodesLabel;
        final ClusterConfigPanel configPanel;
        final ReentrantLock updateLock;
        final NullLogger nullLogger;
        final PrintStreamLogger sysoutLogger;
        
        public CUIMainPanel()
        {
            super();
            GridBagConstraints gbc = new GridBagConstraints();

            nullLogger = new NullLogger();
            sysoutLogger = new PrintStreamLogger(System.out);

            updateLock = new ReentrantLock();

            configPanel = new ClusterConfigPanel();

            // initialize controls
            statButton = new Button("Show Node Statistics");
            startStopButton = new Button("Start Cluster");

            clusterLabel = new StateLabel("Cluster is");
            queueLabel = new StateLabel("Jobs in queue:");
            runningLabel = new StateLabel("Running jobs: ");
            nodesLabel = new StateLabel("Active nodes: ");
            
            debugCheck = new Checkbox("Debug output", false);
            
            debugCheck.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (debugCheck.getState())
                    {
                        FijiArchipelago.setDebugLogger(sysoutLogger);
                    }
                    else
                    {
                        FijiArchipelago.setDebugLogger(nullLogger);
                    }
                }
            });
            FijiArchipelago.setDebugLogger(nullLogger);
            
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
            super.add(debugCheck, gbc);
            
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
            switch(cluster.getState())
            {
                case RUNNING:
                    return "active.";
                case STARTED:
                    return "started.";
                case STOPPING:
                    return "waiting for termination.";
                case STOPPED:
                    return "terminated.";
                case INSTANTIATED:
                    return "waiting for initialization";
                case INITIALIZED:
                    if (isConfigured.get())
                    {
                        return "ready to start.";
                    }
                    else
                    {
                        return "in need of configuration.";
                    }
                default:
                    return "partying like its 1999";
            }
        }
        
        
        public void update() {

            if (!updateLock.tryLock())
            {
                FijiArchipelago.debug("UI: Could not acquire main panel update lock");
                return;
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
                final Cluster.ClusterState state = cluster.getState();
                if (isConfigured.get() && state != Cluster.ClusterState.STOPPING && state !=
                        Cluster.ClusterState.STOPPED)
                {
                    startStopButton.setEnabled(true);
                }
                else
                {
                    startStopButton.setEnabled(false);
                }
                
                clusterLabel.update(clusterStateString());
                queueLabel.update(cluster.getQueuedJobCount());
                runningLabel.update(cluster.getRunningJobCount());
                nodesLabel.update(cluster.getRunningNodeCount());

                switch(state)
                {
                    case INSTANTIATED:
                        startStopButton.setEnabled(false);
                        configPanel.setActive(true);
                        break;
                    case INITIALIZED:
                        startStopButton.setEnabled(true);
                        startStopButton.setLabel("Start Cluster");
                        startStopButton.setActionCommand("start");
                        configPanel.setActive(true);
                        break;
                    case STARTED:
                    case RUNNING:
                        startStopButton.setEnabled(true);
                        startStopButton.setLabel("Stop Cluster");
                        startStopButton.setActionCommand("stop");
                        configPanel.setActive(true);
                        break;
                    case STOPPING:
                    case STOPPED:
                        startStopButton.setEnabled(false);
                        startStopButton.setLabel("Stop Cluster");
                        startStopButton.setActionCommand("");
                        configPanel.setActive(false);
                        break;
                }

            }
            else
            {
                clusterLabel.update("is null");
                queueLabel.update(0);
                runningLabel.update(0);
                nodesLabel.update(0);
                startStopButton.setEnabled(false);
            }
            updateLock.unlock();
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

        public void setActive(boolean active)
        {
            loadButton.setEnabled(active);
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
                if (started(cluster))
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
    
    private class NodeStartResult
    {
        public final boolean ok;
        public final NodeManager.NodeParameters param;
        
        public NodeStartResult(boolean b, NodeManager.NodeParameters p)
        {
            ok = b;
            param = p;
        }
    }

    private class UIShellExecListener implements ShellExecListener
    {

        public void execFinished(long nodeID, Exception e, int status)
        {
            NodeManager.NodeParameters param = cluster.getNodeManager().getParam(nodeID); 
            String hostname = param == null ? "" + nodeID : param.getHost();
            if (e == null)
            {
                FijiArchipelago.log("Node " + hostname + " finished.");
            }
            else
            {
                FijiArchipelago.log("Node " + hostname + " finished with Exception: " + e);
                FijiArchipelago.err("Node " + hostname + " finished with Exception: " + e);
            }
        }
    }


    private class NodeStartCallable implements Callable<NodeStartResult>
    {
        final NodeManager.NodeParameters param;
        final ShellExecListener listener;
        
        public NodeStartCallable(final NodeManager.NodeParameters p)
        {
            param = p;
            listener = new UIShellExecListener();
        }
        
        public NodeStartResult call()
        {
            try
            {
                return new NodeStartResult(cluster.startNode(param, listener), param);
            }
            catch (ShellExecutionException see)
            {
                FijiArchipelago.debug("Caught ShellExecution Exception: " + see);
                FijiArchipelago.err(see.getMessage());
                
                // return true not because we were successful (we weren't), but in order
                // not to show a second failure message.
                return new NodeStartResult(true, param);
            }
            
        }
    }
    
    private class RootNodeConfigDialog implements ActionListener
    {
        final Hashtable<String, String> paramMap;
        final Hashtable<String, TextField> fieldMap;
        final String prefRoot;
        final Frame rootConfigFrame;
        final Panel panel;
        final AtomicBoolean ok;
        final Vector<Thread> waitThreads;
        
        public RootNodeConfigDialog()
        {
            final GridBagConstraints gbc = new GridBagConstraints();
            final Button keyButton = new Button("Select ssh key...");
            final Button execRootButton = new Button("Select exec root...");
            final Button fileRootButton = new Button("Select file root...");
            final Button okButton = new Button("OK");
            final Button cancelButton = new Button("Cancel");

            panel = new Panel();
            waitThreads = new Vector<Thread>();
            ok = new AtomicBoolean(false);
            paramMap = new Hashtable<String, String>();
            fieldMap = new Hashtable<String, TextField>();
            prefRoot = FijiArchipelago.PREF_ROOT;
            rootConfigFrame = new Frame("Root Node Configuration");

            setEntry("port", "" + Cluster.DEFAULT_PORT);
            setEntry("keyfile", IJ.isWindows() ? "" : System.getenv("HOME") + "/.ssh/id_dsa");
            setEntry("execRoot", "");
            setEntry("fileRoot", "");
            setEntry("execRootRemote", "");
            setEntry("fileRootRemote", "");
            setEntry("username", System.getenv("USER"));

            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            panel.setLayout(new GridBagLayout());
            
            panel.add(new Label("Root Node Configuration"), gbc);

            addField("\tServer Port Number", "port", gbc, null);
            addField("\tSSH Private Key File", "keyfile", gbc, keyButton);
            addField("\tLocal Exec Root", "execRoot", gbc, execRootButton);
            addField("\tLocal File Root", "fileRoot", gbc, fileRootButton);
            addField("\tUser Name", "username", gbc, null);
            addField("\tDefault Client Exec Root", "execRootRemote", gbc, null);
            addField("\tDefault Client File Root", "fileRootRemote", gbc, null);
            
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            panel.add(okButton, gbc);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(cancelButton, gbc);
            
            okButton.addActionListener(this);
            cancelButton.addActionListener(this);
            fileRootButton.addActionListener(this);
            execRootButton.addActionListener(this);
            keyButton.addActionListener(this);
            
            okButton.setActionCommand("ok");
            cancelButton.setActionCommand("cancel");
            fileRootButton.setActionCommand("fileRoot");
            execRootButton.setActionCommand("execRoot");
            keyButton.setActionCommand("key");

            rootConfigFrame.add(panel);
            panel.setVisible(true);
            rootConfigFrame.setPreferredSize(new Dimension(768, 384));
            rootConfigFrame.setMinimumSize(new Dimension(768, 384));
            panel.validate();
            rootConfigFrame.validate();

            rootConfigFrame.addWindowListener(
                    new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    ok.set(false);
                    rootConfigFrame.setVisible(false);
                    rootConfigFrame.removeAll();
                }
            });
        }
        
        public void show()
        {
            FijiArchipelago.debug("Showing config");
            if (!rootConfigFrame.isVisible())
            {

                FijiArchipelago.debug("Setting frame visible");
                rootConfigFrame.validate();
                rootConfigFrame.setVisible(true);
                ok.set(false);
            }
        }


        
        private void syncMap()
        {
            for (String key : paramMap.keySet())
            {
                String value = fieldMap.get(key).getText();
                paramMap.put(key, value);
                Prefs.set(prefRoot + "." + key, value);
            }
            Prefs.savePreferences();
        }
        
        public String getStringValue(String key)
        {
            return paramMap.get(key); 
        }
        
        public int getIntegerValue(String key)
        {
            String value = getStringValue(key);
            if (value == null)
            {
                return Integer.MIN_VALUE;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        
        private void addField(final String label, final String key, final GridBagConstraints gbc,
                              final Button b)
        {
            final int buttonWidth = b == null ? 0 : 128;
            final int h = 32, lw = 256, fw = 384;
            final Label l = new Label(label);
            final TextField tf = new TextField(paramMap.get(key));
            fieldMap.put(key, tf);
            
            
            l.setMinimumSize(new Dimension(lw, h));
            l.setSize(new Dimension(lw, h));
            
            tf.setMinimumSize(new Dimension(fw - buttonWidth, h));
            tf.setSize(new Dimension(fw - buttonWidth, h));
            

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            panel.add(l, gbc);

            if (b == null)
            {
                gbc.gridwidth = GridBagConstraints.REMAINDER;
            }
            
            panel.add(tf, gbc);
            
            if(b != null)
            {
                b.setMinimumSize(new Dimension(buttonWidth, h));
                b.setSize(new Dimension(buttonWidth, h));
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                panel.add(b, gbc);
            }
        }
        
        private void setEntry(final String key, final String dValue)
        {
            String value = Prefs.get(prefRoot + "." + key, dValue);
            paramMap.put(key, value);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            final String command = actionEvent.getActionCommand();
            if (command.equals("ok"))
            {
                ok.set(true);               
                rootConfigFrame.setVisible(false);
                rootConfigFrame.removeAll();
                syncMap();
                isConfigured.set(configureCluster(
                        cluster,
                        getIntegerValue("port"),
                        getStringValue("execRootRemote"),
                        getStringValue("fileRootRemote"),
                        getStringValue("execRoot"),
                        getStringValue("fileRoot"),
                        getStringValue("username"),
                        getStringValue("keyfile")));
            }
            else if (command.equals("cancel"))
            {
                ok.set(false);
                rootConfigFrame.setVisible(false);
                rootConfigFrame.removeAll();               
            }
            else if (command.equals("fileRoot"))
            {
                DirectoryChooser dc = new DirectoryChooser("Choose File Root");
                if (dc.getDirectory() != null)
                {
                    fieldMap.get("fileRoot").setText(dc.getDirectory());
                }
            }
            else if (command.equals("execRoot"))
            {
                DirectoryChooser dc = new DirectoryChooser("Choose Exec Root");
                if (dc.getDirectory() != null)
                {
                    fieldMap.get("execRoot").setText(dc.getDirectory());
                }
            }
            else if (command.equals("key"))
            {
                OpenDialog od = new OpenDialog("Select Private Key File", null);
                if (od.getFileName() != null)
                {
                    fieldMap.get("keyfile").setText(od.getDirectory() + "/" + od.getFileName());
                }
            }
        }
    }
    
    public static boolean started(final Cluster c)
    {
        Cluster.ClusterState state = c.getState();
        return state != Cluster.ClusterState.INSTANTIATED &&
                state != Cluster.ClusterState.INITIALIZED;
    }

    private final Cluster cluster;
    private final Frame frame;
    private final ReentrantLock configLock;
    private final AtomicBoolean isConfigured;
    private final CUIMainPanel mainPanel;
    private List<NodeManager.NodeParameters> nodeParameters;
    private final ClusterNodeStatusUI nodeStatusUI;
    private final AtomicBoolean active;
    private final ExecutorService exec;
    private final ClusterUI self = this;

    public ClusterUI(Cluster c)
    {
        configLock = new ReentrantLock();
        this.cluster = c;
        isConfigured = new AtomicBoolean(started(c));
        mainPanel = new CUIMainPanel();
        nodeParameters = new ArrayList<NodeManager.NodeParameters>();
        nodeStatusUI = new ClusterNodeStatusUI(cluster, this);
        active = new AtomicBoolean(true);

        frame = new Frame();        
        frame.add(mainPanel);
        frame.setSize(new Dimension(300, 300));
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                boolean ok;
                final Cluster.ClusterState state = cluster.getState();
                
                if (state == Cluster.ClusterState.STOPPED ||
                        state == Cluster.ClusterState.STOPPING)
                {
                    ok = true;
                }
                else
                {
                    final GenericDialog gd = new GenericDialog("Shutdown?");
                    gd.addMessage("Really Close?");
                    gd.showDialog();

                    ok = gd.wasOKed();
                }
                
                if (ok)
                {
                    cluster.shutdownNow();
                    frame.setVisible(false);
                    frame.removeAll();                    
                    nodeParameters.clear();
                    nodeStatusUI.stop();
                    active.set(false);
                    cluster.removeStateListener(self);
                }
            }
        });

        exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        frame.validate();

        // Refresh the UI at least once a second
        new Thread()
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
        }.start();

        cluster.registerUI(this);
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
            FijiArchipelago.debug("UI: Got changed state for cluster");            
            nodeStatusUI.stateChanged();
            updateUI();
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
        boolean isConfigured = cluster.getState() != Cluster.ClusterState.INSTANTIATED || cluster.init(port);

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
    
    private void configError(String file, String erStr)
    {
        IJ.error("Error loading file " + file + ":" + erStr);
    }
    
    public synchronized boolean loadFromFile(final String file)
    {
        try
        {
            ArrayList<NodeManager.NodeParameters> params = new ArrayList<NodeManager.NodeParameters>();
            isConfigured.set(loadClusterFile(file, cluster, params));

            for (NodeManager.NodeParameters p : params)
            {
                addNode(p);
            }

            if (!isConfigured.get())
            {
                configError(file, "Could not configure Cluster on port " + cluster.getPort());
            }
        }
        catch (ParserConfigurationException pce)
        {
            configError(file, "" + pce);
        }
        catch (SAXException se)
        {
            configError(file, "" + se);
        }
        catch (IOException ioe)
        {
            configError(file, "" + ioe);
        }
        updateUI();
        return isConfigured.get();
    }

    private void toggleStatsWindow()
    {
        nodeStatusUI.toggleVisible();
        updateUI();
    }

    private void handleUnstartedNode(NodeManager.NodeParameters param, Exception e)
    {
        FijiArchipelago.err("Could not start node " + param.getHost() + ": " + e);
        FijiArchipelago.log("Could not start node " + param.getHost() + ": " + e);
    }
    

    private synchronized void startCluster()
    {
        FijiArchipelago.debug("Startcluster called");
        if (cluster.getState() == Cluster.ClusterState.INITIALIZED)
        {
            final ArrayList<Future<NodeStartResult>> startNodeResults =
                    new ArrayList<Future<NodeStartResult>>(nodeParameters.size());
            
            FijiArchipelago.debug("State was Initialized");
            FijiArchipelago.log("Starting Cluster");
            FijiArchipelago.debug("Calling cluster.start()");

            if (!cluster.start())
            {
                IJ.error("Could not start cluster");
                return;
            }
            FijiArchipelago.debug("cluster.start() returned");
            
            for (NodeManager.NodeParameters param : nodeParameters)
            {                
                startNodeResults.add(exec.submit(new NodeStartCallable(param)));
            }
            
            new Thread(){
                public void run()
                {                   
                    try
                    {
                        for (Future<NodeStartResult> result : startNodeResults)
                        {
                            if (!result.get().ok)
                            {
                                handleUnstartedNode(result.get().param, null);
                            }
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        FijiArchipelago.err("Interrupted while starting nodes!");
                    }
                    catch (ExecutionException ee)
                    {
                        FijiArchipelago.err("Execution Exception while starting nodes!");
                    }
                }
            }.start();
        }
    }
    
    private void addNode(final NodeManager.NodeParameters param)
    {
        if (!started(cluster))
        {
            FijiArchipelago.debug("Cluster not yet started, adding param to queue");
            nodeParameters.add(param);
        }
        else
        {
            FijiArchipelago.debug("Cluster has been started, starting node directly.");
            
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        Future<NodeStartResult> future = exec.submit(new NodeStartCallable(param));
                        NodeStartResult result = future.get();

                        if (!result.ok)
                        {
                            handleUnstartedNode(param, null);
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        FijiArchipelago.debug("UI.addNode: Interrupted!");
                        handleUnstartedNode(param, ie);
                    }
                    catch (ExecutionException ee)
                    {
                        FijiArchipelago.debug("UI.addNode: Execution Error!");
                        handleUnstartedNode(param, ee);
                    }
                }
            }.start();
            
        }
    }
    
    private synchronized void saveToFile(final String file)
    {
        FijiArchipelago.debug("Save called");
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

            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            params.addAll(nodeParameters);
            params.addAll(cluster.getNodesParameters());
            
            doc.appendChild(clusterXML);

            addXMLField(doc, rootNode, "port", "" + cluster.getServerPort());
            addXMLField(doc, rootNode, "exec", FijiArchipelago.getExecRoot());
            addXMLField(doc, rootNode, "file", FijiArchipelago.getFileRoot());
            addXMLField(doc, rootNode, "default-exec", nm.getStdExecRoot());
            addXMLField(doc, rootNode, "default-file", nm.getStdFileRoot());
            addXMLField(doc, rootNode, "default-user", nm.getStdUser());
            addXMLField(doc, rootNode, "key", Prefs.get(FijiArchipelago.PREF_ROOT +
                    ".keyfile", IJ.isWindows() ? "" : System.getenv("HOME") + "/.ssh/id_dsa"));
            clusterXML.appendChild(rootNode);

            FijiArchipelago.debug("Writing " + params.size() + " nodes");

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
            
            FijiArchipelago.log("Saved configuration to " + file);
            
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
            FijiArchipelago.debug("Got " + newParams.size() + " new params");
            nodeParameters.clear();
            for (NodeManager.NodeParameters param : newParams)
            {
                addNode(param);
            }
            FijiArchipelago.debug("Now we have " + nodeParameters.size() + " params");
        }
    }

    private void configureRootNode()
    {
        if (!configLock.tryLock())
        {
            return;
        }

        FijiArchipelago.debug("Creating and showing root node configuration");
        
        RootNodeConfigDialog rncd = new RootNodeConfigDialog();
        rncd.show();

    }
}
