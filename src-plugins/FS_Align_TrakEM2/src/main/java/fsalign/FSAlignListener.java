package fsalign;

import ini.trakem2.Project;
import ini.trakem2.display.*;
import ini.trakem2.utils.Filter;
import mpicbg.trakem2.align.AlignLayersTask;

import java.io.File;
import java.util.ArrayList;


/**
 * @author Larry Lindsey
 */
public class FSAlignListener extends ImageListener
{
    public static final String SMTP_KEY = "fsalign.FSAlignListener.smtp";
    public static final String EMAIL_KEY = "fsalign.FSAlignListener.email";
    public static final String PASS_KEY = "fsalign.FSAlignListener.password";
    public static final String PORT_KEY = "fsalign.FSAlignListener.port";
    public static final String TOMAIL_KEY = "fsalign.FSAlignListener.notifymail";

    
    
    private final Project trakemProject;
    private final ArrayList<String> existingImageFiles;
    protected final Filter<Patch> patchFilter;           
    private final double thickness;
    private double nextZ;
    private String path;
    private String notifyEmail;

    private static class PatchSieve implements Filter<Patch>
    {
        public boolean accept(Patch patch) {
            return true;
        }
    }
    
    public FSAlignListener()
    {
        this(Display.getFront().getProject(), 1.0, true);
    }
    
    public FSAlignListener(Project project, double t, boolean append)
    {
        this(project, t, append ? project.getRootLayerSet().getDepth() + t : 0);
    }

    public FSAlignListener(Project project, double t, double z0)
    {
        trakemProject = project;
        thickness = t;
        patchFilter = new PatchSieve();
        notifyEmail = "";
        existingImageFiles = new ArrayList<String>();
        nextZ = z0;
        initImageFileList();
        setEnabled(true);

    }

    @Override
    protected void processImage(File imageFile)
    {
        if (isEnabled() && trakemProject != null && okToAdd(imageFile))
        {
            // Add image to the stack
            //Patch patch = Patch.createPatch(trakemProject, imageFile.getAbsolutePath());
            Patch patch = new Patch(trakemProject, imageFile.getName(), 0, 0, getImageFromPath(imageFile.getAbsolutePath()));
            LayerSet layerSet = trakemProject.getRootLayerSet();
            Layer layer = layerSet.getLayer(nextZ, thickness, true);

            trakemProject.getLoader().addedPatchFrom(imageFile.getAbsolutePath(), patch);
            layer.add(patch);

            dropImage(imageFile);

            try
            {
                patch.updateMipMaps().get();
            }
            catch (Exception e) {}

            if (layerSet.size() > 1)
            {
                AlignLayersTask.alignLayersLinearlyJob(layerSet, layerSet.size() - 2, layerSet.size() -1, false, null, patchFilter);
                imageDigest(patch);
                imageCheck(patch);
            }
            else
            {
                imageDigest(patch);
            }
            
            trakemProject.save();

            nextZ+=thickness;
        }
    }


    private void initImageFileList()
    {
        for (Layer l : trakemProject.getRootLayerSet().getLayers())
        {
            for (Displayable d: l.getDisplayables())
            {
                try
                {
                    Patch p = (Patch)d;                    
                    existingImageFiles.add(new File(p.getImageFilePath()).getName());
                }
                catch (ClassCastException cce){}
            }
        }
    }

    /**
     * Check whether its ok to add the given file, as determined by whether it already exists in
     * the TrakEM2 project. This method is intended to be called once per file.
     *
     * We assume that if it is ok to add a File, then it is added to the project immediately after
     * checking. We also assume that all Files added by this listener are checked through this
     * function.
     *
     * This listener holds an internal list of files in the project, which is initialized upon
     * creation. Afterward, files are added to this list as they are checked by this function.
     * @param imageFile
     * @return
     */
    protected boolean okToAdd(File imageFile)
    {
        if (existingImageFiles.contains(imageFile.getName()))
        {
            return false;
        }
        else
        {
            existingImageFiles.add(imageFile.getName());
            return true;
        }
    }

    public void setNotifyEmail(final String notify)
    {
        notifyEmail = notify;
    }

    protected void imageDigest(final Patch p)
    {

    }

    protected void imageCheck(final Patch p)
    {

    }

    protected void digest(final String message)
    {

    }

    protected void immediate(final String message)
    {

    }

    /*public void sendEmail(final String subject, final String message)
    {

    }

    public static void setPrefs()
    {
        final GenericDialog gd = new GenericDialog("File System Aligner Email Account Setup");
        boolean cont = true;

        String host = "", email = "", pass = "";
        int port = 0;

        gd.addMessage("The file system aligner needs an account login to send notification emails.\n" +
                "Do NOT use an email account that you care about. It is recommended to set up a dummy gmail account for this purpose.\n" +
                "The password for this account will be stored in PLAIN TEXT");
        gd.addStringField("SMTP Server", "smtp.gmail.com");
        gd.addStringField("Account (email before the @)", "");
        gd.addStringField("Password", "14758f1afd44c09b7992073ccf00b43d");
        gd.addNumericField("SMTP Port", 587, 0);

        while (cont)
        {
            gd.showDialog();
            if (gd.wasCanceled())
            {
                return;
            }
            else
            {
                host = gd.getNextString();
                email = gd.getNextString();
                pass = gd.getNextString();
                port = (int)gd.getNextNumber();

                //Check a bunch of conditions to see if we should re-display the dialog
                cont = !(host.contains(".") && host.length() > 1);
                cont |= email.length() < 1;
                cont |= pass.length() < 1;
                cont |= port < 1;
            }
        }

        Prefs.set(SMTP_KEY, host);
        Prefs.set(EMAIL_KEY, email);
        Prefs.set(PASS_KEY, pass);
        Prefs.set(PORT_KEY, port);

        Prefs.savePreferences();
    }*/

}
