package archipelago.ui;

import archipelago.FijiArchipelago;
import archipelago.IJLogger;
import archipelago.NodeManager;
import archipelago.network.Cluster;
import archipelago.network.shell.JSchNodeShell;
import archipelago.network.shell.NodeShell;
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
    
    private class NodePanel extends Panel implements ActionListener
    {
        private final Label label;
        
        public String hostName;
        public String execRoot;
        public String fileRoot;
        public int port;
        public int nCpu;
        public String user;
        
        private final String sep = "\t";
        
        public NodePanel(String fromFile)
        {
            super();
            Button editButton = new Button("Edit");
            Button rmButton = new Button("Remove");
            String[] split = new String[0];

            label = new Label();

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

            if (fromFile == null || split.length < 6)
            {
                hostName = "";
                execRoot = Cluster.getCluster().getNodeManager().getStdExecRoot();
                fileRoot = Cluster.getCluster().getNodeManager().getStdFileRoot();
                user = Cluster.getCluster().getNodeManager().getStdUser();
                port = Cluster.getCluster().getNodeManager().getStdPort();
                nCpu = 1;
                if (fromFile != null)
                {
                    FijiArchipelago.log("Node Configuration: Could not parse line: " + fromFile);
                }
            }
            else
            {
                //Order: host, user, port, nCpu, execRoot, fileRoot
                
                hostName = split[0];
                user = split[1];
                port = Integer.parseInt(split[2]);
                nCpu = Integer.parseInt(split[3]);
                execRoot = split[4];
                fileRoot = split[5];


                label.setText(user + "@" + hostName + ":" + port + " x" + nCpu);
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
            //Order: host, user, port, nCpu, execRoot, fileRoot
            StringBuilder sb = new StringBuilder(256);
            sb.append(hostName).append(sep).append(user).append(sep).append(port).append(sep)
                    .append(nCpu).append(sep).append(execRoot).append(sep).append(fileRoot);
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
            gd.addNumericField("Number of Threads", nCpu, 0);
            gd.addStringField("Remote Fiji Root", execRoot, 64);
            gd.addStringField("Remote File Root", fileRoot, 64);
            gd.showDialog();
            
            if (gd.wasOKed())
            {
                hostName = gd.getNextString();
                user = gd.getNextString();
                port = (int)gd.getNextNumber();
                nCpu = (int)gd.getNextNumber();
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
            param.setNumThreads(nCpu);
            return param;
        }
    }

    private TextField fileName;
    private Vector<NodePanel> nodePanels;
    private final String prefRoot;
    private final Panel centerPanel;
    private final Panel mainPanel;
    private final GenericDialog gd;
    private final ScrollPane pane;
    private boolean ok;



    public ClusterNodeConfigUI()
    {
        Button addButton = new Button("Add Node...");
        pane = new ScrollPane();
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
        if (ok = clusterOptionUI())
        {
            gd.showDialog();
            ok = gd.wasOKed();
        }
    }

    public boolean clusterOptionUI()
    {
        FijiArchipelago.debug("Cluster Option UI Starting");

        if (Cluster.activeCluster())
        {
            GenericDialog gd = new GenericDialog("Active Cluster");
            gd.addMessage("There is an active cluster already, shut it down?");
            gd.showDialog();
            if (gd.wasCanceled())
            {
                return false;
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
            gd.addMessage("Cluster Configuration");
            gd.addNumericField("Server Port Number", dPort, 0);
            gd.addStringField("Remote Machine User Name", dUserName);
            gd.addStringField("SSH Private Key File", dKeyfile, 64);
            gd.addStringField("Local Exec Root", dExecRoot, 64);
            gd.addStringField("Local File Root", dFileRoot, 64);
            gd.addStringField("Default Exec Root for Remote Nodes", dExecRootRemote, 64);
            gd.addStringField("Default File Root for Remote Nodes", dFileRootRemote, 64);


            gd.showDialog();
            if (gd.wasCanceled())
            {
                return false;
            }

            //Get results
            port = (int)gd.getNextNumber();
            userName = gd.getNextString();
            keyfile = gd.getNextString();
            execRoot = gd.getNextString();
            fileRoot = gd.getNextString();
            execRootRemote = gd.getNextString();
            fileRootRemote = gd.getNextString();


            //Do initialization
            shell = new JSchNodeShell(new JSchNodeShell.JSchShellParams(new File(keyfile)),
                    new IJLogger());

            Cluster.initCluster(port);
            Cluster.getCluster().getNodeManager().setStdUser(userName);
            Cluster.getCluster().getNodeManager().setStdExecRoot(execRootRemote);
            Cluster.getCluster().getNodeManager().setStdFileRoot(fileRootRemote);
            Cluster.getCluster().getNodeManager().setStdShell(shell);
            
            FijiArchipelago.setExecRoot(execRoot);
            FijiArchipelago.setFileRoot(fileRoot);

            //Set prefs
            Prefs.set(prefRoot + ".port", port);
            Prefs.set(prefRoot + ".keyfile", keyfile);
            Prefs.set(prefRoot + ".username", userName);
            Prefs.set(prefRoot + ".execRoot", execRoot);
            Prefs.set(prefRoot + ".fileRoot", fileRoot);
            Prefs.set(prefRoot + ".execRootRemote", execRootRemote);
            Prefs.set(prefRoot + ".fileRootRemote", fileRootRemote);
            Prefs.savePreferences();
            return true;
        }
        else
        {
            return false;
        }
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
        final Button load = new Button("Load from file");
        final Button save = new Button("Save to file");
        final Button select = new Button("Select file...");
        fileName = new TextField(Prefs.get(prefRoot + ".nodeFile", "fiji.cluster"),64);
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
            gd.showDialog();
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
        
        FijiArchipelago.log("Saved configuration to " + fileName.getText());
        
        Prefs.set(prefRoot + ".nodeFile", fileName.getText());
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
            FijiArchipelago.err(fileName.getText() + " not found");
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
            Prefs.set(prefRoot + ".nodeFile", fileName.getText());
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
        new NodePanel().doEdit();
        pane.setScrollPosition(0, Integer.MAX_VALUE);
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
