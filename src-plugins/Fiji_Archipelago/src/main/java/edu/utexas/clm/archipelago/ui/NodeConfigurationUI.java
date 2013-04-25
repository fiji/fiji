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
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.shell.NodeShell;
import edu.utexas.clm.archipelago.network.shell.NodeShellParameters;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

public class NodeConfigurationUI extends Panel implements ActionListener
{
    private class NodeShellPanel extends Panel implements ItemListener
    {

        private final Hashtable<String, NodeShellParameters> parameterMap;
        private final Hashtable<String, TextField> textMap;
        private final Choice shellChoice;
        private final Collection<NodeShell> shells;
        private NodeShellParameters currentShellParam, lastShellParam;
        private final NodeManager.NodeParameters nodeParam;
        private final Panel shellChoicePanel;
        
        public NodeShellPanel(NodeManager.NodeParameters param)
        {
            nodeParam = param;
            parameterMap = new Hashtable<String, NodeShellParameters>();
            textMap = new Hashtable<String, TextField>();
            parameterMap.put(param.getShell().name(), param.getShellParams());
            currentShellParam = param.getShellParams();
            lastShellParam = null;
            shells = Cluster.registeredShells();
            shellChoicePanel = new Panel();

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            // Shell Selection Choice
            shellChoice = new Choice();
            shellChoice.addItemListener(this);
            for (NodeShell shell : shells)
            {
                shellChoice.add(shell.name());
            }
            
            shellChoice.select(param.getShell().name());

            shellChoicePanel.add(shellChoice);

            refresh();
        }
        
        public synchronized void refresh()
        {
            FijiArchipelago.debug("Last Shell Param: " + lastShellParam);
            FijiArchipelago.debug("Current Shell Param: " + currentShellParam);
            if (lastShellParam != currentShellParam)
            {
                lastShellParam = currentShellParam;
                super.removeAll();
                super.add(shellChoicePanel);
                
                textMap.clear();

                FijiArchipelago.debug("Refreshing...");
                
                for (final String key : currentShellParam.getKeys())
                {
                    final Panel p = new Panel();
                    final TextField tf = new TextField(currentShellParam.getStringOrEmpty(key), 48);

                    FijiArchipelago.debug("Adding key " + key);
                    //p.add(new Label(key));
                    //p.add(tf);
                    textMap.put(key, tf);
                    
                    if (currentShellParam.isFile(key))
                    {
                        /*
                        Create a file selection button, add it to the panel,
                        and add a click listener. When the button is clicked,
                        a file selection dialog will open. If the dialog is
                        OK'ed, the path of the selected file will show up 
                        in the text field.
                         */
                        Button fileSelect = new Button("Select file...");
                        p.add(fileSelect);
                        fileSelect.addActionListener(
                                new ActionListener()
                                {
                                    public void actionPerformed(ActionEvent e)
                                    {
                                        final OpenDialog od = new OpenDialog("Select a file",
                                                null);
                                        final String dirName = od.getDirectory();
                                        final String fileName = od.getFileName();
                                        if (fileName != null)
                                        {
                                            tf.setText(dirName + fileName);
                                        }

                                    }
                                }
                        );

                        ClusterUI.doRowPanelLayout(p, 640, 24, new float[]{1, 3, 1},
                                new Label(key), tf, fileSelect);
                    }
                    else
                    {
                        ClusterUI.doRowPanelLayout(p, 640, 24, new float[]{1, 4},
                                new Label(key), tf);
                    }
                    
                    super.add(p);
                    
                }
            }
            validate();
        }
        
        public void syncParams()
        {
            for (String key: currentShellParam.getKeys())
            {                
                try
                {
                    currentShellParam.putValue(key, textMap.get(key).getText());
                }
                catch (Exception e)
                {
                    handleKeyError(key, e);
                }
            }
            
            for (NodeShell shell : shells)
            {
                if (shell.name().equals(shellChoice.getSelectedItem()))
                {
                    nodeParam.setShell(shell, currentShellParam);
                    //nodeParam.setShellParams(currentShellParam);
                    break;
                }
            }
        }
        
        private void handleKeyError(String key, Exception e)
        {
            FijiArchipelago.err("Could not save key " + key + ": " + e);
        }
        
        public void itemStateChanged(ItemEvent e)
        {
            final String selection = shellChoice.getSelectedItem(); 
            NodeShellParameters param = parameterMap.get(selection);
            
            if (param == null)
            {
                for (NodeShell shell : shells)
                {
                    if (shell.name().equals(selection))
                    {
                        param = shell.defaultParameters();
                        parameterMap.put(selection, param);
                        break;
                    }
                }
            }
            
            currentShellParam = param;
            
            refresh();
        }
    }
    
    private class NodePanel extends Panel implements ActionListener
    {
        private final Label label;

        public String hostName;
        public String execRoot;
        public String fileRoot;
        public int cpuLimit;
        public String user;
        public NodeShellParameters shellParams;
        public final NodeManager.NodeParameters param;
       
        public NodePanel(NodeManager.NodeParameters param)
        {
            this.param = param;
            label = new Label();
            init();
        }

        private void init()
        {
            Button editButton = new Button("Edit");
            Button rmButton = new Button("Remove");

            this.hostName = param.getHost();
            this.execRoot = param.getExecRoot();
            this.fileRoot = param.getFileRoot();
            this.cpuLimit = param.getThreadLimit();
            this.user = param.getUser();
            this.shellParams = param.getShellParams();

            add(label);
            add(editButton);
            add(rmButton);

            editButton.setActionCommand("edit");
            rmButton.setActionCommand("rm");

            editButton.addActionListener(this);
            rmButton.addActionListener(this);

            updateLabel();
        }

        private void updateLabel()
        {
            label.setText(user + "@" + hostName);
        }

        private void updateParam()
        {
            param.setHost(hostName);
            param.setUser(user);
            param.setThreadLimit(cpuLimit);
            param.setExecRoot(execRoot);
            param.setFileRoot(fileRoot);
        }


        public void actionPerformed(ActionEvent ae)
        {
            if (ae.getActionCommand().equals("edit"))
            {
                doEdit();
            }
            else if (ae.getActionCommand().equals("rm"))
            {
                GenericDialog gd = new GenericDialog("Really Remove?");
                gd.addMessage("Really remove this node?");
                gd.showDialog();
                if (gd.wasOKed())
                {
                    //manager.removeParam(param.getID());
                    removeNodePanel(this);
                    
                }
            }
        }

        public boolean doEdit()
        {
            GenericDialog gd = new GenericDialog("Edit Cluster Node");
            NodeShellPanel nsp = new NodeShellPanel(param);
            gd.addStringField("Hostname", hostName, Math.max(hostName.length(), 64));
            gd.addStringField("User name", user);
            gd.addNumericField("Thread Limit", cpuLimit, 0);
            gd.addStringField("Remote Fiji Root", execRoot, 64);
            gd.addStringField("Remote File Root", fileRoot, 64);
            gd.addPanel(nsp);
            gd.validate();
            gd.showDialog();

            if (gd.wasOKed())
            {
                hostName = gd.getNextString();
                user = gd.getNextString();
                cpuLimit = (int)gd.getNextNumber();
                execRoot = gd.getNextString();
                fileRoot = gd.getNextString();
                nsp.syncParams();
                updateLabel();
                updateParam();
                validate();
            }
            
            return gd.wasOKed();
        }

        public NodeManager.NodeParameters getNodeParam()
        {
            return param;
        }
    }

    
    private final NodeManager manager;
    private final Vector<NodePanel> nodePanels;
    private final Vector<Long> removedNodes;
    private final Panel centralPanel;
    
    
    
    private NodeConfigurationUI(NodeManager nm, Collection<NodeManager.NodeParameters> nodeParams)
    {
        super();
        centralPanel = new Panel();
        final ScrollPane pane = new ScrollPane();        
        final Button addButton = new Button("Add Node...");
        //final Dimension panelSize = new Dimension(512, 256);
        
        nodePanels = new Vector<NodePanel>();
        removedNodes = new Vector<Long>();
        manager = nm;
        
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        super.add(pane);
        super.add(addButton);

        addButton.addActionListener(this);

        centralPanel.setLayout(new BoxLayout(centralPanel, BoxLayout.Y_AXIS));
        pane.add(centralPanel);
        
        //super.setSize(new Dimension(512, 256));
        super.setMinimumSize(new Dimension(512, 256));
        super.setPreferredSize(new Dimension(512, 256));
        addButton.setPreferredSize(new Dimension(480, 48));
        addButton.setSize(new Dimension(480, 48));
        addButton.setMaximumSize(new Dimension(480, 48));
        addButton.setMinimumSize(new Dimension(480, 48));

        for (NodeManager.NodeParameters p : nodeParams)
        {
            addNode(p);                        
        }
        
        super.validate();
    }

    private NodePanel addNode(final NodeManager.NodeParameters p)
    {
        NodePanel panel = new NodePanel(p);
        nodePanels.add(panel);
        centralPanel.add(panel);
        centralPanel.validate();
        super.validate();
        return panel;
    }
    
    private void removeNodePanel(final NodePanel panel)
    {
        nodePanels.remove(panel);
        centralPanel.remove(panel);
        removedNodes.add(panel.getNodeParam().getID());
        super.validate();
    }

    public void actionPerformed(final ActionEvent actionEvent)
    {
        final NodePanel np = addNode(manager.newParam());
        if (!np.doEdit())
        {
            removeNodePanel(np);
        }
    }
    

    public static void nodeConfigurationUI(final Cluster cluster)
    {
        FijiArchipelago.debug("nodeConfigurationUI called");
        final GenericDialog gd = new GenericDialog("Cluster Nodes");
        final ArrayList<NodeManager.NodeParameters> existingParameters = cluster.getNodeParameters();
        NodeConfigurationUI ui = new NodeConfigurationUI(cluster.getNodeManager(), existingParameters);
        gd.addPanel(ui);
        gd.showDialog();

        if (gd.wasOKed())            
        {
            FijiArchipelago.debug("Was ok'ed. Got " + ui.nodePanels.size() + " nodes");

            for (NodePanel np : ui.nodePanels)
            {
                final NodeManager.NodeParameters param = np.getNodeParam();
                if (!existingParameters.contains(param))
                {
                    cluster.addNode(param);
                }
            }
            
            for (long id : ui.removedNodes)
            {
                cluster.removeNode(id);
            }


        }
    }
}
