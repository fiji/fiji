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

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import ij.gui.GenericDialog;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class NodeConfigurationUI extends Panel implements ActionListener
{
    private class NodePanel extends Panel implements ActionListener
    {
        private final Label label;

        public String hostName;
        public String execRoot;
        public String fileRoot;
        public int port;
        public int cpuLimit;
        public String user;
        public final NodeManager.NodeParameters param;

        public NodePanel(String hostName, String execRoot, String fileRoot, int port,
                         int cpuLimit, String user)
        {
            super();
            label = new Label();
            param = manager.newParam(
                    user,
                    hostName,
                    manager.getStdShell(),
                    execRoot,
                    fileRoot,
                    port);
            param.setThreadLimit(cpuLimit);
            init();
        }

        public NodePanel(NodeManager.NodeParameters param)
        {
            this.param = param;
            label = new Label();
            init();
        }

        public NodePanel()
        {
            this(
                    "",
                    manager.getStdExecRoot(),
                    manager.getStdFileRoot(),
                    manager.getStdPort(),
                    0,
                    manager.getStdUser());
        }

        private void init()
        {
            Button editButton = new Button("Edit");
            Button rmButton = new Button("Remove");

            this.hostName = param.getHost();
            this.execRoot = param.getExecRoot();
            this.fileRoot = param.getFileRoot();
            this.port = param.getPort();
            this.cpuLimit = param.getThreadLimit();
            this.user = param.getUser();

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
            label.setText(user + "@" + hostName + ":" + port);
        }

        private void updateParam()
        {
            param.setHost(hostName);
            param.setUser(user);
            param.setPort(port);
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
                    manager.removeParam(param.getID());
                    removeNodePanel(this);
                }
            }
        }

        public boolean doEdit()
        {
            GenericDialog gd = new GenericDialog("Edit Cluster Node");
            gd.addStringField("Hostname", hostName, Math.max(hostName.length(), 128));
            gd.addStringField("User name", user);
            gd.addNumericField("Port", port, 0);
            gd.addNumericField("Thread Limit", cpuLimit, 0);
            gd.addStringField("Remote Fiji Root", execRoot, 64);
            gd.addStringField("Remote File Root", fileRoot, 64);
            gd.showDialog();

            if (gd.wasOKed())
            {
                hostName = gd.getNextString();
                user = gd.getNextString();
                port = (int)gd.getNextNumber();
                cpuLimit = (int)gd.getNextNumber();
                execRoot = gd.getNextString();
                fileRoot = gd.getNextString();
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
    private final Panel centralPanel;
    
    
    
    private NodeConfigurationUI(NodeManager nm, List<NodeManager.NodeParameters> nodeParams)
    {
        super();
        centralPanel = new Panel();
        final ScrollPane pane = new ScrollPane();        
        final Button addButton = new Button("Add Node...");
        //final Dimension panelSize = new Dimension(512, 256);
        
        nodePanels = new Vector<NodePanel>();
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
    

    public static boolean nodeConfigurationUI(
            final NodeManager nm, final List<NodeManager.NodeParameters> nodeParams,
            final List <NodeManager.NodeParameters> newParams)
    {
        GenericDialog gd = new GenericDialog("Cluster Nodes");
        NodeConfigurationUI ui = new NodeConfigurationUI(nm, nodeParams);
        gd.addPanel(ui);
        gd.showDialog();

        if (gd.wasOKed())            
        {
            FijiArchipelago.debug("Was ok'ed. Got " + ui.nodePanels.size() + " nodes");

            for (NodePanel np : ui.nodePanels)
            {
                newParams.add(np.getNodeParam());
            }
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public static boolean nodeConfigurationUI(NodeManager nm, final List <NodeManager.NodeParameters> newParams)
    {
         return nodeConfigurationUI(nm, new ArrayList<NodeManager.NodeParameters>(), newParams);
    }
}
