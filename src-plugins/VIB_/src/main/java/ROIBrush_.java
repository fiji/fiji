
import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.OvalRoi;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.*;

/**
 * User: Tom Larkworthy
 * Date: 08-Jun-2006
 * Time: 17:32:42
 */
public class ROIBrush_ implements PlugIn {
    public static final String MACRO_CMD =
					"var roiBrushWidth = 10;\n" +
					"var pollDelay = 10;\n" +
                    "var leftClick=16, altOrShift=9;\n" +
                    "macro 'Roi Brush Tool - C111o11ff' {\n" +
                    " while (true) {\n" +
                    "  getCursorLoc(x, y, z, flags);\n" +
                    "  if (flags&leftClick==0) exit();\n" +
                    "  if (flags&altOrShift==0){\n" +
                    "   call('ROIBrush_.label', x,y,z,flags,roiBrushWidth);\n" +
                    "  }else{\n" +
                    "   call('ROIBrush_.unlabel', x,y,z,flags,roiBrushWidth);\n" +
                    "  }\n" +
                    "  wait(pollDelay);\n" +
                    " }\n" +
                    "}\n" +
                    "\n" +
                    "macro 'Roi Brush Tool Options...' {\n" +
                    " roiBrushWidth = getNumber('Roi Brush Width (pixels):', brushWidth);\n" +
                    "}";

    public void run(String arg) {
        if (IJ.versionLessThan("1.37c"))
            return;

        MacroInstaller installer = new MacroInstaller();
        installer.install(MACRO_CMD);
    }

    //methods in a macro accessable format
    public static void label(String x, String y, String z, String flags, String width) {
        System.out.println("ROIBrush_.label called from a macro at: "+x+","+y);
        label((int)Float.parseFloat(x),
              (int)Float.parseFloat(y),
              (int)Float.parseFloat(z),
              (int)Float.parseFloat(flags),
              (int)Float.parseFloat(width));
    }

    public static void unlabel(String x, String y, String z, String flags, String width) {
        unlabel((int)Float.parseFloat(x),
                (int)Float.parseFloat(y),
                (int)Float.parseFloat(z),
                (int)Float.parseFloat(flags),
                (int)Float.parseFloat(width));
    }

    public synchronized static void label(int x, int y, int z, int flags, int width) {        
        /*
        try {
            Class c=Class.forName("ROIBrush_");
            System.out.println("Now in synchronized label() method of class with classloader: "+c.getClassLoader());
        } catch( ClassNotFoundException e ) {
            System.out.println("Can't find class.");
        }
        */
        
        Roi roi = IJ.getImage().getRoi();

        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.or(getBrushRoi(x, y, width));
        } else {
            roi = getBrushRoi(x, y, width);
        }

        IJ.getImage().setRoi(roi);
        /*
        try {
            System.out.println("Leaving synchronized label() method of class "+Class.forName("ROIBrush_"));
        } catch( ClassNotFoundException e ) {
            System.out.println("Can't find class.");
        }
        */
    }

    public synchronized static void unlabel(int x, int y, int z, int flags, int width) {
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.not(getBrushRoi(x, y, width));

            IJ.getImage().setRoi(roi);
        }
    }


    private static ShapeRoi getBrushRoi(int x, int y, int width) {
        return new ShapeRoi(new OvalRoi(x - width / 2, y - width / 2, width, width));
    }


}
