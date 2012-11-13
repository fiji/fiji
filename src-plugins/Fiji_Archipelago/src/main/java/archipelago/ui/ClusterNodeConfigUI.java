package archipelago.ui;

import archipelago.FijiArchipelago;
import archipelago.NodeManager;
import archipelago.network.Cluster;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

public class ClusterNodeConfigUI implements ActionListener
{
    
    private static String SixtyFour = "                                                ";

    private class NodePanel extends Panel implements ActionListener
    {
        private final Label label;
        
        public String hostName;
        public String execRoot;
        public String fileRoot;
        public int port;
        public String user;
        
        private final String sep = "\t";
        
        public NodePanel(String fromFile)
        {
            super();
            Button editButton = new Button("Edit");
            Button rmButton = new Button("Remove");
            String[] split = new String[0];

            label = new Label(SixtyFour);

            add(label);
            add(editButton);
            add(rmButton);

            editButton.setActionCommand("edit");
            rmButton.setActionCommand("rm");

            editButton.addActionListener(this);
            rmButton.addActionListener(this);

            if (fromFile != null)
            {
                split = fromFile.split(sep);
            }

            if (fromFile == null || split.length < 5)
            {
                hostName = "";
                execRoot = Cluster.getCluster().getNodeManager().getStdExecRoot();
                fileRoot = Cluster.getCluster().getNodeManager().getStdFileRoot();
                user = Cluster.getCluster().getNodeManager().getStdUser();
                port = Cluster.getCluster().getNodeManager().getStdPort();
                if (fromFile != null)
                {
                    FijiArchipelago.log("Node Configuration: Could not parse line: " + fromFile);
                }
            }
            else
            {
                //Order: host, user, port, execRoot, fileRoot
                
                hostName = split[0];
                user = split[1];
                port = Integer.parseInt(split[2]);
                execRoot = split[3];
                fileRoot = split[4];

                label.setText(user + "@" + hostName + ":" + port);
                mainPanel.validate();
                centerPanel.validate();
            }

            addNodePanel(this);
        }
        
        public NodePanel()
        {            
            this(null);
        }
        
        public String toFileText()
        {
            //Order: host, user, port, execRoot, fileRoot
            StringBuilder sb = new StringBuilder(256);
            sb.append(hostName).append(sep).append(user).append(sep).append(port).append(sep)
                    .append(execRoot).append(sep).append(fileRoot);
            return sb.toString();
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
                    removeNodePanel(this);                    
                }
            }
        }
        
        public void doEdit()
        {
            GenericDialog gd = new GenericDialog("Edit Cluster Node");
            gd.addStringField("Hostname", hostName, Math.max(hostName.length(), 128));
            gd.addStringField("User name", user);
            gd.addNumericField("Port", port, 0);
            gd.addStringField("Remote Fiji Root", execRoot, 64);
            gd.addStringField("Remote File Root", fileRoot, 64);
            gd.showDialog();
            
            if (gd.wasOKed())
            {
                hostName = gd.getNextString();
                user = gd.getNextString();
                port = (int)gd.getNextNumber();
                execRoot = gd.getNextString();
                fileRoot = gd.getNextString();
                label.setText(user + "@" + hostName + ":" + port);
                mainPanel.validate();
                centerPanel.validate();
            }
        }
        
        public NodeManager.NodeParameters toNodeParam(NodeManager manager)
        {
            NodeManager.NodeParameters param = manager.newParam(hostName);
            param.setExecRoot(execRoot);
            param.setFileRoot(fileRoot);
            param.setPort(port);
            param.setUser(user);
            return param;
        }
    }

    private TextField fileName;
    private Vector<NodePanel> nodePanels;
    private final String prefRoot;
    private final Panel centerPanel;
    private final Panel mainPanel;
    private final GenericDialog gd;


    public ClusterNodeConfigUI()
    {
        Button addButton = new Button("Add Node...");
        ScrollPane pane = new ScrollPane();
        Dimension panelSize = new Dimension(512, 256);
        
        mainPanel = new Panel();
        centerPanel = new Panel();
        nodePanels = new Vector<NodePanel>();
        prefRoot  = FijiArchipelago.PREF_ROOT;

        addButton.setActionCommand("add");
        addButton.addActionListener(this);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        mainPanel.add(loadSaveNodesPanel(), BorderLayout.NORTH);
        pane.add(centerPanel);
        mainPanel.add(pane, BorderLayout.SOUTH);

        mainPanel.add(addButton, BorderLayout.SOUTH);

        mainPanel.setPreferredSize(panelSize);
        pane.setPreferredSize(panelSize);

        mainPanel.validate();

        gd = new GenericDialog("Configure Nodes");
        gd.addPanel(getPanel());
        gd.showDialog();
    }

    public boolean wasOKed()
    {
        return gd.wasOKed();
    }
    
    public boolean wasCanceled()
    {
        return gd.wasCanceled();
    }
    
    public Panel getPanel()
    {
        return mainPanel;
    }
    
    private void addNodePanel(NodePanel np)
    {
        centerPanel.add(np);
        nodePanels.add(np);
        
        centerPanel.validate();
        mainPanel.validate();
    }
    
    private void removeNodePanel(NodePanel np)
    {
        nodePanels.remove(np);
        centerPanel.remove(np);

        centerPanel.validate();
        mainPanel.validate();
    }
    
    private Panel loadSaveNodesPanel()
    {
        //Controls
        final Panel p = new Panel();
        fileName = new TextField(Prefs.get(prefRoot + ".nodeFile", SixtyFour),64);
        final Button load = new Button("Load from file");
        final Button save = new Button("Save to file");
        final Button select = new Button("Select file...");
        GridBagConstraints gbc = new GridBagConstraints();

        //Init controls
        load.setActionCommand("load");
        save.setActionCommand("save");
        select.setActionCommand("select");
        
        load.addActionListener(this);
        save.addActionListener(this);
        select.addActionListener(this);

        //Set mainPanel layout
        p.setLayout(new GridBagLayout());


        
        gbc.gridx = 0;
        gbc.gridy = 1;
        p.add(load, gbc);
        
        gbc.gridx += 1;
        p.add(save, gbc);
        
        gbc.gridx += 1;
        p.add(select, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        //gbc.weightx = 1;
        p.add(fileName, gbc);

        return p;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("save"))
        {
            saveConfiguration();
        }
        else if (ae.getActionCommand().equals("load"))
        {
            loadConfiguration();
        }
        else if (ae.getActionCommand().equals("select"))
        {
            selectConfigFile();
        }
        else if (ae.getActionCommand().equals("add"))
        {
            addNodeConfig();
        }

    }

    private void saveConfiguration()
    {
        final File f = new File(fileName.getText());
        PrintStream out;

        if (f.exists())
        {
            GenericDialog gd = new GenericDialog("File Exists");
            gd.addMessage("" + fileName.getText() + " exists, overwrite?");
            if (gd.wasCanceled())
            {
                return;
            }
        }
        
        try
        {
            out = new PrintStream(new FileOutputStream(f));
        }
        catch (IOException ioe)
        {
            FijiArchipelago.err("Error while opening " + fileName.getText() + " for write: "
                    + ioe);
            return;
        }

        for (NodePanel np : nodePanels)
        {
            out.println(np.toFileText());            
        }
        
        out.close();
    }

    private void loadConfiguration()
    {
        final File f = new File(fileName.getText());
        BufferedReader in;
        
        try
        {
            in = new BufferedReader(new FileReader(f));            
        }
        catch (FileNotFoundException fnfe)
        {
            FijiArchipelago.err("File not found: " + fnfe);
            return;
        }
        
        try
        {
            String line = in.readLine();

            while (line != null)
            {
                new NodePanel(line);
                line = in.readLine();
            }
        }
        catch (IOException ioe)
        {
            FijiArchipelago.err("IO Exception while reading from " + fileName.getText()
                    + ": " + ioe);

        }
    }

    private void selectConfigFile()
    {
        OpenDialog od = new OpenDialog("Select Config File", null);
        fileName.setText(od.getFileName());
    }

    private void addNodeConfig()
    {
        FijiArchipelago.debug("Got add config click");
        new NodePanel().doEdit();
    }
            
    public ArrayList<NodeManager.NodeParameters> parameterList(NodeManager manager)
    {
        ArrayList<NodeManager.NodeParameters> paramList = new ArrayList<NodeManager.NodeParameters>();
        for (NodePanel np : nodePanels)
        {
            paramList.add(np.toNodeParam(manager));
        }
        return paramList;
    }

}
