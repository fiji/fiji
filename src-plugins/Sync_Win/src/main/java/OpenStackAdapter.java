import ij.*;
import ij.gui.*;

/*
 * Created on 08.07.2005
 */

/** This class contains static methods, which handle the "open"-specific features of 
 * OpenStackWindow: 
 * - converting a StackWindow to the appropriate class
 * - adding and removing displayChangeListener 
 * OpenStackAdapter is the implementation for the case that the Image5D plugins
 * are not installed along with SyncWindows.
 * When Image5DWindow is referenced in SyncWindow, directly, an Exception occurs, if
 * Image5D is not installed. 
 * @author Joachim Walter
 */
public class OpenStackAdapter {
    
    public static ImageWindow makeOpenWindow(ImageWindow iw) {
        ImageWindow iwOut = iw;
        if (iw instanceof StackWindow && !(iw instanceof OpenStackWindow)) {
            ImageCanvas ic = iw.getCanvas();
            ImagePlus img = iw.getImagePlus();
            double magn = ic.getMagnification();
            iwOut = new OpenStackWindow(img, ic);
            
            // set zoom to previous zoom
            ic.setMagnification(magn);
            img.repaintWindow();                
        }
        return iwOut;
    }
    
    public static boolean isOpenWindow(ImageWindow iw) {
        return (iw instanceof OpenStackWindow);
    }
    
    public static void addDisplayChangeListener(ImageWindow iw, DisplayChangeListener dcl) {
        if (iw instanceof OpenStackWindow) {
            ((OpenStackWindow)iw).addDisplayChangeListener(dcl);
        }
    }
    
    public static void removeDisplayChangeListener(ImageWindow iw, DisplayChangeListener dcl) {
        if (iw instanceof OpenStackWindow) {
            ((OpenStackWindow)iw).removeDisplayChangeListener(dcl);
        }
    }
}
