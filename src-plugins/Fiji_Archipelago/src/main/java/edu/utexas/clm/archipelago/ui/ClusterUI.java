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
import edu.utexas.clm.archipelago.listen.ClusterStateListener;
import edu.utexas.clm.archipelago.network.server.ArchipelagoServer;
import edu.utexas.clm.archipelago.util.NullLogger;
import edu.utexas.clm.archipelago.util.PrintStreamLogger;
import ij.IJ;
import ij.gui.GenericDialog;


import javax.swing.BoxLayout;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import ij.io.OpenDialog;
import org.xml.sax.SAXException;

/**
 * User interface for the Archipelago Cluster
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
        final Button statButton, startStopButton, serverButton;
        final Checkbox debugCheck;
        final StateLabel clusterLabel, queueLabel, runningLabel, nodesLabel;
        final ClusterConfigPanel configPanel;
        final ReentrantLock updateLock;
        final NullLogger nullLogger;
        final PrintStreamLogger sysoutLogger;
        ArchipelagoServer server = null;
        
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
            serverButton = new Button("Start Insecure Server (At Your Own Risk)");

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
            super.add(serverButton, gbc);
            super.add(debugCheck, gbc);
            
            // action commands
            statButton.setActionCommand("stats");
            startStopButton.setActionCommand("");
            serverButton.setActionCommand("server-start");

            statButton.addActionListener(this);
            startStopButton.addActionListener(this);
            serverButton.addActionListener(this);

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
                cluster.start();
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
            else if (ae.getActionCommand().equals("server-start"))
            {
                server = ArchipelagoServer.getServer(cluster);
                if (!server.active())
                {
                    server.start();
                }
                serverButton.setActionCommand("server-stop");
                serverButton.setLabel("Stop Insecure Server");
            }
            else if (ae.getActionCommand().equals("server-stop"))
            {                   
                if (server != null)
                {
                    GenericDialog gd = new GenericDialog("Stop Server?");
                    gd.addMessage("Really Stop Server?");
                    gd.showDialog();
                    if (gd.wasOKed())
                    {
                        server.close();
                        serverButton.setActionCommand("server-start");
                        serverButton.setLabel("Start Insecure Server (At Your Own Risk)");
                    }
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
                        serverButton.setEnabled(false);
                        break;
                    case INITIALIZED:
                        startStopButton.setEnabled(true);
                        startStopButton.setLabel("Start Cluster");
                        startStopButton.setActionCommand("start");
                        configPanel.setActive(true);
                        serverButton.setEnabled(false);
                        break;
                    case STARTED:
                    case RUNNING:
                        startStopButton.setEnabled(true);
                        startStopButton.setLabel("Stop Cluster");
                        startStopButton.setActionCommand("stop");
                        configPanel.setActive(true);
                        serverButton.setEnabled(true);
                        break;
                    case STOPPING:
                    case STOPPED:
                        startStopButton.setEnabled(false);
                        startStopButton.setLabel("Stop Cluster");
                        startStopButton.setActionCommand("");
                        configPanel.setActive(false);
                        serverButton.setEnabled(false);
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
                    ClusterXML.saveToFile(cluster, new File(dirName + fileName));
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
                FijiArchipelago.debug("Node config");
                NodeConfigurationUI.nodeConfigurationUI(cluster);
            }
        }
    }

    private class WindowCloseAdapter extends WindowAdapter
    {
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
                nodeStatusUI.stop();
                active.set(false);
                cluster.removeStateListener(self);
            }
        }
    }
    
    public static boolean started(final Cluster c)
    {
        Cluster.ClusterState state = c.getState();
        return state != Cluster.ClusterState.INSTANTIATED &&
                state != Cluster.ClusterState.INITIALIZED;
    }

    /**
     * Places Components into a Container with the layout used by NodeStatusPanel. Only n 
     * components will be placed, where n is the smaller of the number of elements in
     * the weights array and the number of Components passed as arguments.
     * @param container the container to lay out
     * @param containerWidth the total width of the container
     * @param height the height to use for the components
     * @param weights an array containing the width-weights for each component. This array should
     *                have at least as many elements as there are components.
     * @param components the components to add to the container
     */
    public static void doRowPanelLayout(final Container container, 
                                        final float containerWidth,
                                        final int height,
                                        final float[] weights,
                                        final Component... components)
    {
        /*
         Ask for width and weights as floats so we don't have to cast internally.

         Typical label might look like:
         host            | state  | thread | beat     | used   | total
         host.domain.net | active | 12/16  | < 1s ago | 4096MB | 16384MB
        */
        float totalWeight = 0;
        int x = 0;
        
        for (float weight : weights)
        {
            totalWeight += weight;
        }

        for (int i = 0; i < components.length && i < weights.length; ++i)
        {
            final int w = (int)(containerWidth * weights[i] / totalWeight);
            final int h = 24;
            final Component c = components[i];
            final Dimension d = new Dimension(w, height); 
            container.add(c);

            c.setMaximumSize(d);
            c.setPreferredSize(d);
            c.setSize(d);
            c.setBounds(x, 0, w, h);
            
            x += w;
        }

        container.validate();
    }

    private final Cluster cluster;
    private final Frame frame;
    private final AtomicBoolean isConfigured;
    private final CUIMainPanel mainPanel;
    //private List<NodeManager.NodeParameters> nodeParameters;
    private final ClusterNodeStatusUI nodeStatusUI;
    private final AtomicBoolean active;
    private final ClusterUI self = this;
    private final RootNodeConfigDialog rncd;
    
    public static String[] shellClasses = {"edu.utexas.clm.archipelago.network.shell.SocketNodeShell"};

    public ClusterUI(Cluster c)
    {
        this.cluster = c;
        isConfigured = new AtomicBoolean(started(c));
        mainPanel = new CUIMainPanel();
        nodeStatusUI = new ClusterNodeStatusUI(cluster, this);
        active = new AtomicBoolean(true);
        rncd = new RootNodeConfigDialog(cluster, isConfigured);

        frame = new Frame();        
        frame.add(mainPanel);
        frame.setSize(new Dimension(300, 300));
        
        frame.addWindowListener(new WindowCloseAdapter());

        frame.validate();

        //register node shells so we can select them
        for (String className : shellClasses)
        {
            try
            {
                ClassLoader.getSystemClassLoader().loadClass(className);
            } catch (ClassNotFoundException cnfe){/**/}
        }

        cluster.registerUI(this);
        cluster.addStateListener(this);

        frame.setVisible(true);

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
            FijiArchipelago.debug("UI: Got changed state for cluster. New state: "
                    + Cluster.stateString(cluster.getState()));
            nodeStatusUI.stateChanged();
            updateUI();
        }
    }

    private void configError(String file, String erStr)
    {
        IJ.error("Error loading file " + file + ":" + erStr);
    }
    
    public synchronized boolean loadFromFile(final String file)
    {
        try
        {
            final ArrayList<Exception> nodeExceptions = new ArrayList<Exception>();
            isConfigured.set(ClusterXML.loadClusterFile(new File(file), cluster, nodeExceptions));

            if (nodeExceptions.size() > 0)
            {               
                FijiArchipelago.err("Could not load " + nodeExceptions.size() + " nodes. See log for details");
                for (final Exception e : nodeExceptions)
                {
                    FijiArchipelago.log("Could not load node: " + e);
                    FijiArchipelago.debug("Could not load node: ", e);
                }
            }
            
            if (!isConfigured.get())
            {
                configError(file, "Could not configure Cluster");
            }
        }
        catch (ParserConfigurationException pce)
        {
            configError(file, pce.toString());
        }
        catch (SAXException se)
        {
            configError(file, se.toString());
        }
        catch (IOException ioe)
        {
            configError(file, ioe.toString());
        }
        updateUI();
        return isConfigured.get();
    }

    private void toggleStatsWindow()
    {
        nodeStatusUI.toggleVisible();
        updateUI();
    }


    private void configureRootNode()
    {
        FijiArchipelago.debug("Creating and showing root node configuration");
        rncd.show();
    }
}
