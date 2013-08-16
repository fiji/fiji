/* Copyright 2012 Tiago Ferreira, 2005 Tom Maddock
 *
 * This file is part of the ImageJ plugin "Bitmap Sholl Analysis".
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.io.Opener;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.image.IndexColorModel;

/** Simple auxiliary commands related to Sholl_Analysis */
public class Sholl_Utils implements PlugIn {

    private static final String BUILD = " 2012.11.27";
    private static final String SRC_URL = "https://github.com/tferr/ASA";
    private static final String DOC_URL = "http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:asa:start";

    private static final String[] METHODS = Sholl_Analysis.SHOLL_TYPES;
    private static int method = Sholl_Analysis.SHOLL_N;
    private static int background = Sholl_Analysis.maskBackground;

    public void run(String arg) {
        if (arg.equalsIgnoreCase("about"))
            showAbout();
        else if (arg.equalsIgnoreCase("sample"))
            displaySample();
        else if(arg.equalsIgnoreCase("jet"))
            applyJetLut();
    }

    /** Displays the ddaC sample image in ./resources */
    void displaySample() {
        InputStream is = getClass().getResourceAsStream("/resources/ddaC.tif");
        if (is!=null) {
            Opener opener = new Opener();
            ImagePlus imp = opener.openTiff(is, "Drosophila_ddaC_Neuron.tif");
            if (imp!=null) imp.show();
        }
    }

    /** Applies the "MATLAB Jet" to frontmost image */
    void applyJetLut() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp!=null && imp.getType()==ImagePlus.COLOR_RGB)
            IJ.error("LUTs cannot be assiged to RGB Images.");
        final int[] values = getLUTindex();
        final IndexColorModel cm = Sholl_Analysis.matlabJetColorMap(values[0], values[1]);
        if (imp==null) {
            imp = new ImagePlus("MATLAB Jet",ij.plugin.LutLoader.createImage(cm));
            imp.show();
        } else {
            if (imp.isComposite())
                ((CompositeImage)imp).setChannelColorModel(cm);
            else
                imp.getProcessor().setColorModel(cm);
            imp.updateAndDraw();
        }
    }

    /** Prompts for background color */
    static int[] getLUTindex() {
        final GenericDialog gd = new GenericDialog("LUT of Intersections Mask");
        gd.addSlider("Background:", 0, 255, background);
        gd.addChoice("Sholl Method:", METHODS, METHODS[method]);
        gd.showDialog();
        background = (int) Math.min(Math.max(gd.getNextNumber(), 0), 255);
        method = gd.getNextChoiceIndex();

        int[] values = new int[2];
        values[0] = background;
        values[1] = method==Sholl_Analysis.SHOLL_SLOG || method==Sholl_Analysis.SHOLL_LOG
                    ? 255 : 0;

        return values;
    }

    /** Displays an "about" info box */
    void showAbout() {
        final String msg1 = " Version " + Sholl_Analysis.VERSION + BUILD;
        final String msg2 = "Quantitative Sholl of untraced neuronal arbors in 2D/3D\n"
                           +"Tiago Ferreira, Tom Maddock";

        final GenericDialog gd = new GenericDialog("Sholl Analysis Plugins");
        gd.addMessage(msg1, new Font("SansSerif", Font.BOLD, 12));
        gd.addMessage(msg2, new Font("SansSerif", Font.PLAIN, 12));
        gd.enableYesNoCancel("Browse Documentation", "Browse Repository");
        gd.hideCancelButton();
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        else if (gd.wasOKed())
            IJ.runPlugIn("ij.plugin.BrowserLauncher", DOC_URL);
        else
            IJ.runPlugIn("ij.plugin.BrowserLauncher", SRC_URL);
    }
}
