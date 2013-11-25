package edu.utexas.clm.crop;

import ij.IJ;
import ij.gui.GenericDialog;
import ini.trakem2.display.AreaList;
import ini.trakem2.plugin.TPlugIn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 */
public class AreaList_Crop implements TPlugIn
{
    private static AreaListCrop areaListCrop = null;
    private static Frame frame = null;
    private static Label areaNameLabel = null;



    private class WindowCloseAdapter extends WindowAdapter
    {
        public void windowClosing(WindowEvent windowEvent)
        {
            cancel();
        }
    }


    private static void cancel()
    {
        boolean ok;
        final GenericDialog gd = new GenericDialog("Cancel?");
        gd.addMessage("Really Cancel?");
        gd.showDialog();

        ok = gd.wasOKed();

        if (ok)
        {
            clear();
        }
    }

    private static void clear()
    {
        frame.setVisible(false);
        frame.removeAll();
        areaListCrop = null;
        frame = null;
        areaNameLabel = null;
    }

    public void init()
    {
        final Panel panel = new Panel();
        final Panel buttonPanel = new Panel();

        final Button okButton = new Button("Create");
        final Button cancelButton = new Button("Cancel");

        areaListCrop = new AreaListCrop()
        {
            protected void progress(double p)
            {
                IJ.showStatus("AreaList crop");
                areaNameLabel.setText("Processing " + ((int)(100 * p)) + "%");
            }
        };
        frame = new Frame("Do Crop");

        areaNameLabel = new Label("");

        areaNameLabel.setSize(new Dimension(120, 240));
        areaNameLabel.setPreferredSize(new Dimension(120, 240));

        panel.setLayout(new GridLayout(2, 1));
        panel.setSize(new Dimension(240, 240));
        panel.setPreferredSize(new Dimension(240, 240));

        buttonPanel.setSize(new Dimension(240, 64));
        buttonPanel.setPreferredSize(new Dimension(240, 64));

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        panel.add(areaNameLabel);
        panel.add(buttonPanel);

        frame.add(panel);
        frame.setSize(new Dimension(240, 240));
        frame.addWindowListener(new WindowCloseAdapter());

        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                okButton.setEnabled(false);
                cancelButton.setEnabled(false);
                new Thread()
                {
                    public void run()
                    {
                        GenericDialog gd = new GenericDialog("Background Value");
                        gd.addNumericField("Background Value (0-255)", 255, 0);
                        gd.showDialog();
                        if (gd.wasOKed())
                        {
                            int val = (int)gd.getNextNumber();
                            val = val > 255 ? 255 : val < 0 ? 0 : val;
                            areaNameLabel.setText(areaNameLabel.getText() + ". Processing...");
                            areaListCrop.getCropImage(val).show();
                            clear();
                        }
                    }
                }.start();
            }
        });

        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                cancel();
            }
        });

        frame.validate();
        frame.setVisible(true);
    }

    public boolean setup(final Object... params)
    {


        return true;
    }

    public Object invoke(final Object... params)
    {
        if (areaListCrop == null)
        {
            init();
        }

        for (final Object ob : params)
        {
            if (ob instanceof AreaList)
            {
                AreaList al = (AreaList)ob;
                String list = "";
                areaListCrop.addAreaList(al);

                for (AreaList areaList : areaListCrop.getAreaLists())
                {
                    if (list.equals(""))
                    {
                        list = areaList.getProject().findProjectThing(areaList).getParent().getTitle();
                    }
                    else
                    {
                        list += ", " +
                                areaList.getProject().findProjectThing(areaList).getParent().getTitle();
                    }
                }

                areaNameLabel.setText(list);
                frame.validate();
            }
        }

        return null;
    }

    public boolean applies(final Object ob)
    {
        return ob != null && ob instanceof AreaList;
    }

}

