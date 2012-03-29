package fsalign;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ini.trakem2.Project;
import ini.trakem2.display.Display;
import mpicbg.trakem2.align.Align;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Vector;

/**
 * @author Larry Lindsey
 */
public class FS_Align_TrakEM2 implements PlugIn
{
    private static class RegexpListener implements ActionListener
    {
        final TextField tf0, tf1, tf2;

        public RegexpListener(TextField intf0, TextField intf1, TextField intf2)
        {
            tf0 = intf0;
            tf1 = intf1;
            tf2 = intf2;
            tf2.setEditable(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            final boolean match = tf1.getText().matches(tf0.getText());
            final String add =  match ? " Matches" : " Does Not Match";
            tf2.setText(tf1.getText() + add);
        }

    }
    
    private static class DoneListener implements ActionListener
    {        
        private final FolderWatcher watcher;
        private final Frame frame;

        public DoneListener(Frame f, FolderWatcher fw)
        {
            frame = f;
            watcher = fw;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            GenericDialog gd = new GenericDialog("Sure?");
            gd.addMessage("Are You Sure?");
            gd.showDialog();

            if (gd.wasOKed())
            {
                watcher.cancel();
                frame.dispose();
            }
        }
    }

    private static final String REGEX_KEY = "fsalign.FSAlignTrakEM2.regex";
    private static final String REGEX_TEST_KEY = "fsalign.FSAlignTrakEM2.regextest";
    private static final String POLL_KEY = "fsalign.FSAlignTrakEM2.poll";
    
    private String email;
    private String regexp;
    private int delayMS;
    private double thickness;

    
    
    private void setupFinishWindow(FolderWatcher watcher)
    {
        Frame f = new Frame("File System Image Registration Plugin");
        Panel p = new Panel(new FlowLayout(FlowLayout.CENTER));
        Button b = new Button("Quit");

        b.addActionListener(new DoneListener(f, watcher));
        
        p.add(b);
        f.add(p);
        f.pack();

        f.setVisible(true);
    }
    
    private boolean getPluginParams()
    {
        final int tfOffset = 0;
        final GenericDialog gd = new GenericDialog("Folder Watch Alignment Plugin Settings");
        final Button button = new Button("Update Result");
        final Panel panel = new Panel(new FlowLayout(FlowLayout.CENTER));
        final double pollTime = Prefs.get(POLL_KEY, 30.0);
        final String regexField = Prefs.get(REGEX_KEY, "Tile.*(tif|tiff|png)$");
        final String regexTestField = Prefs.get(REGEX_TEST_KEY, "Tile_r1-c1_Something Something.tif");
        
        
        Vector stringFields;
        RegexpListener rl;

        panel.add(button);

        gd.addNumericField("Section Thickness (pixels)", 1.0, 2);
        gd.addNumericField("Poll Frequency (seconds)", pollTime, 2);
        //gd.addStringField("Notification Email Address (Blank for none)", "");
        gd.addMessage("Regular Expressions are used to match file names");
        gd.addMessage("Test your regular expression below");
        gd.addStringField("Regular Expression", regexField);
        gd.addStringField("Test", regexTestField);
        gd.addStringField("Result", "");
        gd.addPanel(panel);

        stringFields = gd.getStringFields();
        for (Object o : stringFields)
        {
            TextField tf = (TextField)o;
            tf.setColumns(96);
        }

        rl = new RegexpListener((TextField)stringFields.get(tfOffset),
                (TextField)stringFields.get(tfOffset + 1), (TextField)stringFields.get(tfOffset + 2));
        button.addActionListener(rl);

        gd.showDialog();

        if (gd.wasCanceled())
        {
            return false;
        }

        thickness = gd.getNextNumber();
        delayMS = (int)(1000 * gd.getNextNumber());
        //email = gd.getNextString();
        regexp = gd.getNextString();

        Prefs.set(POLL_KEY, ((double)delayMS)/1000);
        Prefs.set(REGEX_KEY, regexp);
        Prefs.set(REGEX_TEST_KEY, gd.getNextString());
        Prefs.savePreferences();
        
        return true;
    }
    
    private boolean getAlignParams()
    {
        final GenericDialog gd = new GenericDialog("Alignment Parameters");
        Align.param.addFields(gd);
        gd.showDialog();

        if (gd.wasCanceled())
        {
            return false;
        }
        else
        {
            Align.param.readFields(gd);
            return true;
        }
    }

    public void run(String s) {        
        Project trakemProject = Display.getFront().getProject();

        if (trakemProject != null)
        {
            FSAlignListener alignListener;
            final DirectoryChooser folderDialog = new DirectoryChooser("Choose Folder to Watch");
            final String watchPath = folderDialog.getDirectory();

            if (!getPluginParams())
            {
                return;
            }
            
            if (!getAlignParams())
            {
                return;
            }

            alignListener = new FSAlignListener(trakemProject, thickness);
            alignListener.setNotifyEmail(email);
            
            try
            {
                final FolderWatcher fw = ImageListener.imageFolderWatcher(watchPath, delayMS, alignListener, regexp);
                setupFinishWindow(fw);
                fw.start();
            }
            catch (IOException ioe)
            {
                IJ.log("FS Align Plugin Caught IOException:\n" + ioe);
            }
            
        }
    }
}
