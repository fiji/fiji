package reconstructreader.trakem2;

import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ini.trakem2.display.Display;

import java.io.File;

public class Reconstruct_Writer implements PlugIn
{

    public void run(final String arg) {
        DirectoryChooser dc = new DirectoryChooser("Select Output Directory");
        String outdir = dc.getDirectory();
        Trakem2Translator t2t = new Trakem2Translator(Display.getFront().getProject(),
                new File(outdir));
        t2t.run();
    }
}
