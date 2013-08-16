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
import edu.utexas.clm.archipelago.listen.NodeStateListener;
import edu.utexas.clm.archipelago.network.node.ClusterNode;
import edu.utexas.clm.archipelago.network.node.ClusterNodeState;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ClusterNodeStatusUI implements ActionListener
{
    private class NodeStatusPanel extends Panel implements NodeStateListener, ActionListener
    {
        private final ClusterNode node;
        private final Label hostLabel, stateLabel, jobsLabel, beatLabel, ramUseLabel, ramTotLabel;
        private final Button killButton;
        
        public NodeStatusPanel(ClusterNode n)
        {
            super();

            node = n;
            hostLabel = new Label();
            jobsLabel = new Label();
            beatLabel = new Label();
            ramUseLabel = new Label();
            ramTotLabel = new Label();
            stateLabel = new Label();
            killButton = new Button("Stop");
            killButton.setActionCommand("stop");
            killButton.addActionListener(this);

            doNodeLayout(this, hostLabel, stateLabel, jobsLabel, beatLabel, ramUseLabel,
                    ramTotLabel, killButton);
            
            hostLabel.setText(node.getHost());

            super.validate();
            doValidate();
            
            node.addListener(this);
        }
        
        public synchronized void update()
        {
            final long lastBeat = node.lastBeat();
            final int beatSec = (int)(((float)(System.currentTimeMillis() - lastBeat)) / 1000f);
            final int ramTot = node.getTotalRamMB();
            final int ramAvail = node.getAvailableRamMB();
            final int ramMax = node.getMaxRamMB();
            String beatString = lastBeat == 0 ? "~" : beatSec > 0 ? "" + beatSec : "< 1";

            hostLabel.setText(node.getHost());
            jobsLabel.setText("" + node.numRunningThreads() + "/" + node.getThreadLimit());
            beatLabel.setText("" +  beatString + "s ago");
            ramUseLabel.setText("" + (ramTot - ramAvail) + "MB");
            ramTotLabel.setText("" + ramMax + "MB");
            validate();
        }
        
        public void actionPerformed(ActionEvent actionEvent)
        {
            if (actionEvent.getActionCommand().equals("stop"))
            {
                GenericDialog gd = new GenericDialog("Sure?");
                gd.addMessage("Really stop node " + node.getHost() + "?");
                gd.addMessage("Running processes may be rescheduled on other nodes.");
                gd.showDialog();
                if (gd.wasOKed())
                {
                    node.close();
                }
            }
        }

        public synchronized void stateChanged(ClusterNode node,
                                 ClusterNodeState stateNow,
                                 ClusterNodeState lastState)
        {
            if (stateNow != lastState)
            {
                switch (stateNow)
                {
                    case STOPPED:
                        stateLabel.setText("stopped");
                        break;
                    case ACTIVE:
                        stateLabel.setText("active");
                        break;
                    case INACTIVE:
                        stateLabel.setText("inactive");
                        break;
                    default:
                        stateLabel.setText("unknown");
                        break;
                }
            }
        }
    }

    /**
     * Places Components into a Container with the layout used by NodeStatusPanel.
     * @param container the container to lay out
     * @param components the components to add to the container
     */
    public static void doNodeLayout(Container container, Component... components)
    {
        /*
         Typical label might look like:
         host            | state  | thread | beat     | used   | total
         host.domain.net | active | 12/16  | < 1s ago | 4096MB | 16384MB
        */
        final double containerWidth = 640;
        final double[] widthWeight = new double[]{5, 1, 1, 2, 2, 2, 1};
        final double totalWeight = 5 + 2 + 1 + 1 + 2 + 2 + 1;// + widthWeight.length;
        
        int x = 0;
        
        //container.setMinimumSize(new Dimension((int)containerWidth, container.getHeight()));
        //container.setPreferredSize(new Dimension((int) containerWidth, container.getHeight()));
        //container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
//        container.setLayout(null);

        for (int i = 0; i < components.length && i < widthWeight.length; ++i)
        {
            final int w = (int)(containerWidth * widthWeight[i] / totalWeight);
            final int h = 24;
            final Component c = components[i];
            container.add(c);
            c.setBounds(x, 0, w, h);
            c.setMaximumSize(new Dimension(w, h));
            c.setPreferredSize(new Dimension(w, h));
            x += w;
        }

        container.validate();
    }
    
    private final Frame frame;
    private final Panel mainPanel, uiPanel;    
    private final ScrollPane pane;
    private final Cluster cluster;
    private final Hashtable<Long, NodeStatusPanel> statusPanels;
    private final Hashtable<Long, ClusterNode> nodeTable;
    private final ReentrantLock updateLock;
    private final AtomicBoolean active;
    private final ClusterUI ui;
    
    public ClusterNodeStatusUI(Cluster c, ClusterUI cui)
    {
        final Button clearButton = new Button("Clear Inactive Nodes");
        final Button hideButton = new Button("Hide");
        final GridBagConstraints gbc = new GridBagConstraints();
        
        statusPanels = new Hashtable<Long, NodeStatusPanel>();
        nodeTable = new Hashtable<Long, ClusterNode>();
        active = new AtomicBoolean(true);

        cluster = c;
        ui = cui;
        frame = new Frame("Cluster Node Status");
        mainPanel = new Panel();
        uiPanel = new Panel();
        pane = new ScrollPane();
        updateLock = new ReentrantLock();
        
        clearButton.setActionCommand("clear");
        hideButton.setActionCommand("hide");
        clearButton.addActionListener(this);
        hideButton.addActionListener(this);
        
        mainPanel.setPreferredSize(new Dimension(512, 300));
        mainPanel.setMinimumSize(new Dimension(512, 300));
        pane.setPreferredSize(new Dimension(512, 300));
        pane.setMinimumSize(new Dimension(512, 300));


        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.add(pane, gbc);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 1.0;
        mainPanel.add(clearButton, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.add(hideButton,  gbc);
        
        uiPanel.setLayout(new BoxLayout(uiPanel, BoxLayout.Y_AXIS));
        
        pane.add(uiPanel);
        
        addUIHeaders();
        
        stateChanged();

        // Hide the Node Status UI when the "x" button is pressed
        frame.addWindowListener(
                new WindowAdapter()
                {


                    public void windowClosing(WindowEvent we)
                    {
                        toggleVisible();
                        ui.updateUI();
                    }
                }
        );

        // If the window is resized, size the scrollframe to fit.
        frame.addComponentListener(
                new ComponentAdapter()
                {
                    public void componentResized(ComponentEvent ce)
                    {
                        Dimension d = new Dimension(frame.getSize());
                        d.width -= 38;
                        d.height -= 50;
                        pane.setPreferredSize(d);
                    }
                }
        );

        frame.add(mainPanel);

        // start invisible
        frame.setVisible(false);
//        frame.setPreferredSize(new Dimension(512, 768));
//        frame.setMinimumSize(new Dimension(512, 768));
        frame.setPreferredSize(new Dimension(550, 350));
        frame.setMinimumSize(new Dimension(550, 350));
        frame.validate();
    }
    
    private void addUIHeaders()
    {
        final Panel hp = new Panel();
        //final Button invisibleButton = new Button("Stop");
        ClusterUI.doRowPanelLayout(hp, 640, 24, new float[]{5, 1, 1, 2, 2, 2, 1},
                new Label("Host"), new Label("state"), new Label("n Jobs"), new Label("Beat"),
                new Label("MB used"), new Label("MB Total"), new Label(""));

//        doNodeLayout(hp, new Label("Host"), new Label("state"), new Label("n Jobs"),
//                new Label("Beat"), new Label("MB used"), new Label("MB Total"), new Label(""));
        //invisibleButton.setVisible(false);
        uiPanel.add(hp);
        uiPanel.validate();
    }
    
    public synchronized void stateChanged()
    {
        final List<ClusterNode> nodes = cluster.getNodes();
        final Set<Long> keySet = nodeTable.keySet(); 
        
        for (ClusterNode node : nodes)
        {
            final long id = node.getID();

            if (node.getState() != ClusterNodeState.STOPPED && !keySet.contains(id))                
            {
                final NodeStatusPanel panel = new NodeStatusPanel(node);
                nodeTable.put(id, node);
                statusPanels.put(id, panel);
                uiPanel.add(panel);
                frame.validate();
            }
        }
    }

    private void doValidate()
    {
        frame.validate();
        pane.validate();
    }

    public void update()
    {
        if (updateLock.tryLock())
        {
            for (NodeStatusPanel panel : statusPanels.values())
            {
                panel.update();
            }
            frame.validate();
            updateLock.unlock();
        }
    }

    public synchronized void clearStoppedNodes()
    {
        final ArrayList<ClusterNode> nodes = new ArrayList<ClusterNode>(nodeTable.values());
        for (ClusterNode node : nodes)
        {
            if (node.getState() == ClusterNodeState.STOPPED)
            {
                final long id = node.getID();
                nodeTable.remove(id);
                uiPanel.remove(statusPanels.get(id));
                statusPanels.remove(id);
            }                
        }
    }

    public boolean isVisible()
    {
        return frame.isVisible();
    }
    
    public void setVisible(boolean v)
    {
        frame.setVisible(v);
    }

    public void toggleVisible()
    {
        setVisible(!frame.isVisible());
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("clear"))
        {
            clearStoppedNodes();
        }
        else if (ae.getActionCommand().equals("hide"))
        {
            toggleVisible();
            ui.updateUI();
        }
    }
    
    public void stop()
    {
        active.set(false);
        frame.setVisible(false);        
        frame.removeAll();
        mainPanel.removeAll();
        uiPanel.removeAll();
    }
}
