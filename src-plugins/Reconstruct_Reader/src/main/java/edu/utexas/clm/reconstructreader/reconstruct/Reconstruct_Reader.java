package edu.utexas.clm.reconstructreader.reconstruct;

import ij.IJ;
import ij.gui.MessageDialog;

import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ini.trakem2.Project;

public class Reconstruct_Reader implements PlugIn
{

    public void run(final String arg) {
        String fname;
        ReconstructTranslator translator;
        long sTime;

        if (arg.equals(""))
        {
            OpenDialog od = new OpenDialog("Select Reconstruct ser File", "");
            fname = od.getDirectory() + od.getFileName();
        }
        else
        {
            fname = arg;
        }

        IJ.log("Creating Reconstruct Translator.");
        translator = new ReconstructTranslator(fname);
        IJ.log("Done.");

        sTime = System.currentTimeMillis();
        IJ.log("Beginning translation");
        if (translator.process())
        {
            String projectFileName;
            float pTime = ((float)(System.currentTimeMillis() - sTime)) / 1000;
            IJ.log("Done translating, took " + pTime + " seconds. Writing XML");
            projectFileName = translator.writeTrakEM2();

            if (!translator.getPostTranslationMessage().isEmpty())
            {
                IJ.showMessage(translator.getPostTranslationMessage());
            }

            if (projectFileName != null)
            {
                Project t2p;
                IJ.log("Opening project " + projectFileName);
                t2p = Project.openFSProject(projectFileName);
                t2p.getRootLayerSet().setMinimumDimensions();

            }
        }
        else
        {
            IJ.log(translator.getLastErrorMessage());
            new MessageDialog(IJ.getInstance(), "Error", "Encountered an Error while translating");
        }

    }

}