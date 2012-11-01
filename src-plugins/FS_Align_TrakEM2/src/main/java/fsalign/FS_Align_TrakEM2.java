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
    /**
     * ActionListener that, given three TextFields tf0, tf1, and tf2, updates the text in tf2
     * to reflect whether the text in tf1 matches the regular expression held in tf0.
     */
    private static class RegexpListener implements ActionListener
    {
        final TextField tf0, tf1, tf2;

        /**
         * Creating the RegexpListener triggers the first update.
         * @param intf0 TextField holding the regular expression
         * @param intf1 TextField holding the test expression
         * @param intf2 TextField holding the output
         */
        public RegexpListener(TextField intf0, TextField intf1, TextField intf2)
        {
            tf0 = intf0;
            tf1 = intf1;
            tf2 = intf2;
            tf2.setEditable(false);
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e)
        {
            final boolean match = tf1.getText().matches(tf0.getText());
            final String add =  match ? " Matches" : " Does Not Match";
            tf2.setText(tf1.getText() + add);
        }

    }

    /**
     * ActionListener that kills a given FolderWatcher and closes a given Frame, after displaying a
     * GenericDialog to check for user sureness.
     */
    private static class DoneListener implements ActionListener
    {        
        private final FolderWatcher watcher;
        private final Frame frame;

        /**
         * Default DoneListener
         * @param f the frame to close, should be the one containing the Button that we are
         *          listening to
         * @param fw the FolderWatcher to kill
         */
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

    // IJ.Prefs keys
    private static final String REGEX_KEY = "fsalign.FSAlignTrakEM2.regex";
    private static final String REGEX_TEST_KEY = "fsalign.FSAlignTrakEM2.regextest";
    private static final String POLL_KEY = "fsalign.FSAlignTrakEM2.poll";
    private static final String APPEND_KEY = "fsalign.FSAlignTrakEM2.append";

    // email not implemented yet.
    private String email;
    private String regexp;
    private int delayMS;
    private double thickness;
    private boolean doAppend;

    /**
     * Sets up a Frame with a single button in it that reads "Done."
     * When clicked, this button cancels the given FolderWatcher, and
     * closes the Frame.
     * @param watcher the FolderWatcher to cancel when the Done button is clicked.
     */
    private void setupFinishWindow(FolderWatcher watcher)
    {
        Frame f = new Frame("File System Image Registration Plugin");
        Panel p = new Panel(new FlowLayout(FlowLayout.CENTER));
        Button b = new Button("Done");

        b.addActionListener(new DoneListener(f, watcher));
        
        p.add(b);
        f.add(p);
        f.pack();

        f.setVisible(true);
    }

    /**
     * Get plugin-centric parameters using a GenericDialog
     * @return true if the GenericDialog was OKed
     */
    private boolean getPluginParams()
    {
        //How many String fields to count until we get to the regex ones, in the GenericDialog.
        final int tfOffset = 0;
        final GenericDialog gd = new GenericDialog("Folder Watch Alignment Plugin Settings");
        final Button button = new Button("Update Result");
        final Panel panel = new Panel(new FlowLayout(FlowLayout.CENTER));
        final double pollTime = Prefs.get(POLL_KEY, 30.0);
        final String regexField = Prefs.get(REGEX_KEY, "Tile.*(tif|tiff|png)$");
        final String regexTestField = Prefs.get(REGEX_TEST_KEY,
                "Tile_r1-c1_Something Something.tif");
        final boolean appendField = Prefs.get(APPEND_KEY, "true").equals("true");
        Vector stringFields;

        panel.add(button);

        gd.addNumericField("Section Thickness (pixels)", 1.0, 2);
        gd.addCheckbox("Append After the Last Section", appendField);
        gd.addNumericField("Poll Frequency (seconds)", pollTime, 2);
        //gd.addStringField("Notification Email Address (Blank for none)", "");
        gd.addStringField("Regular Expression", regexField);
        gd.addMessage("Regular Expressions are used to match file names");
        gd.addMessage("Test your regular expression below");
        gd.addStringField("Test", regexTestField);
        gd.addStringField("Result", "");
        gd.addPanel(panel);

        //Get the String TextFields from the GD, and make sure they're wide enough.
        stringFields = gd.getStringFields();
        for (Object o : stringFields)
        {
            TextField tf = (TextField)o;
            tf.setColumns(96);
        }

        //Push those TextFields to the RegexpListener
        button.addActionListener(new RegexpListener((TextField)stringFields.get(tfOffset),
                (TextField)stringFields.get(tfOffset + 1),
                (TextField)stringFields.get(tfOffset + 2)));

        gd.showDialog();


        if (gd.wasCanceled())
        {
            return false;
        }
        else
        {
            // Set parameters, save some of them as preferences
            thickness = gd.getNextNumber();
            delayMS = (int)(1000 * gd.getNextNumber());
            //email = gd.getNextString();
            regexp = gd.getNextString();
            doAppend = gd.getNextBoolean();

            Prefs.set(POLL_KEY, ((double)delayMS)/1000);
            Prefs.set(REGEX_KEY, regexp);
            Prefs.set(REGEX_TEST_KEY, gd.getNextString());
            if (doAppend)
            {
                Prefs.set(APPEND_KEY, "true");
            }
            else
            {
                Prefs.set(APPEND_KEY, "false");
            }
            Prefs.savePreferences();

            return true;
        }
    }

    /**
     * Get alignment-centric parameters from the user
     * @return true if the dialog was OKed
     */
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
        Display display = Display.getFront();
        Project trakemProject = display == null ? null : display.getProject();

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

            alignListener = new FSAlignListener(trakemProject, thickness, doAppend);
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
        else
        {
            GenericDialog gd = new GenericDialog("Need a TrakEM2 Project");
            gd.addMessage("Please either open a TrakEM2 project or create a new one through File->New->TrakEM2 Project");
            gd.showDialog();
        }
    }
}
