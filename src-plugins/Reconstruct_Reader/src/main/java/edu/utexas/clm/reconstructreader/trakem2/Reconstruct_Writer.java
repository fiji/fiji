package edu.utexas.clm.reconstructreader.trakem2;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ini.trakem2.display.Display;
import ini.trakem2.plugin.TPlugIn;

import java.io.File;

public class Reconstruct_Writer implements PlugIn, TPlugIn
{

    public void run(final String arg) {
        DirectoryChooser dc = new DirectoryChooser("Select Output Directory");
        String outdir = dc.getDirectory();

        if (outdir != null && !outdir.isEmpty())
        {
            Trakem2Translator t2t = new Trakem2Translator(Display.getFront().getProject(),
                    new File(outdir));
            t2t.run();

            if (t2t.getSuccess())
            {
                String message = "Export to Reconstruct finished successfully\n" +
                        "This process only exports Series and Section files. Be sure to export flat images as well.\n" +
                        "It is necessary to export all sections in the TrakEM2 project.";
                IJ.showMessage("Reconstruct Exporter", message);
            }
            else
            {
                IJ.error("Reconstruct Exporter", "Encountered an error while exporting Reconstruct project.");
            }
        }
    }

    public boolean setup(Object... params) {
        return false;
    }

    public Object invoke(Object... params) {
        run("");
        return null;
    }

    public boolean applies(Object ob) {
        return true;
    }
}
