package edu.utexas.clm.archipelago.ui;


import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import ij.Prefs;
import ij.io.DirectoryChooser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

public class RootNodeConfigDialog implements ActionListener
{
    private final Hashtable<String, String> paramMap;
    private final Hashtable<String, TextField> fieldMap;
    private final String prefRoot;
    private final Frame rootConfigFrame;
    private final Panel panel;
    private final Cluster cluster;
    private final AtomicBoolean isConfigured;


    public RootNodeConfigDialog(Cluster cluster, AtomicBoolean configured)
    {
        final GridBagConstraints gbc = new GridBagConstraints();
        final Button execRootButton = new Button("Select exec root...");
        final Button fileRootButton = new Button("Select file root...");
        final Button okButton = new Button("OK");
        final Button cancelButton = new Button("Cancel");
        final boolean useDefault = cluster.getState() == Cluster.ClusterState.INSTANTIATED;

        this.cluster = cluster;
        isConfigured = configured;

        panel = new Panel();
        paramMap = new Hashtable<String, String>();
        fieldMap = new Hashtable<String, TextField>();
        prefRoot = FijiArchipelago.PREF_ROOT;
        rootConfigFrame = new Frame("Root Node Configuration");

        if (useDefault)
        {
            FijiArchipelago.debug("RootNodeConfigDialog: Using default parameters");
        }
        else
        {
            FijiArchipelago.debug("RootNodeConfigDialog: Using configured parameters");
        }
        
        setEntry("execRoot", FijiArchipelago.getExecRoot(), useDefault);
        setEntry("fileRoot", FijiArchipelago.getFileRoot(), useDefault);
        setEntry("execRootRemote",
                cluster.getNodeManager().getDefaultParameters().getExecRoot(), useDefault);
        setEntry("fileRootRemote",
                cluster.getNodeManager().getDefaultParameters().getFileRoot(), useDefault);
        setEntry("username",
                cluster.getNodeManager().getDefaultParameters().getUser(), useDefault);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        panel.setLayout(new GridBagLayout());

        panel.add(new Label("Root Node Configuration"), gbc);

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

        okButton.setActionCommand("ok");
        cancelButton.setActionCommand("cancel");
        fileRootButton.setActionCommand("fileRoot");
        execRootButton.setActionCommand("execRoot");

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
        }
    }

    public boolean isVisible()
    {
        return rootConfigFrame.isVisible();
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

    /*public int getIntegerValue(String key)
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
    }*/

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

    private void setEntry(final String key, final String dValue, boolean useDefault)
    {
        final String value = useDefault ? Prefs.get(prefRoot + "." + key, dValue) :
                dValue;
        paramMap.put(key, value);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        final String command = actionEvent.getActionCommand();
        if (command.equals("ok"))
        {
            rootConfigFrame.setVisible(false);
            rootConfigFrame.removeAll();
            syncMap();
            isConfigured.set(Cluster.configureCluster(
                    cluster,
                    getStringValue("execRootRemote"),
                    getStringValue("fileRootRemote"),
                    getStringValue("execRoot"),
                    getStringValue("fileRoot"),
                    getStringValue("username")));
        }
        else if (command.equals("cancel"))
        {
            rootConfigFrame.setVisible(false);
            //rootConfigFrame.removeAll();
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
    }
}