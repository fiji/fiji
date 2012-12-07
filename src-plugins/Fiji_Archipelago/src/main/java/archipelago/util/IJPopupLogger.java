package archipelago.util;


import ij.IJ;
/**
 *
 * @author Larry Lindsey
 */
public class IJPopupLogger implements EasyLogger
{
    public void log(String msg) {
        IJ.showMessage(msg);
    }
}
