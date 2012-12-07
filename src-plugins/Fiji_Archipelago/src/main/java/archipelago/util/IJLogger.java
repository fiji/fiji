package archipelago.util;

import ij.IJ;
/**
 *
 * @author Larry Lindsey
 */
public class IJLogger implements EasyLogger
{

    public synchronized void log(String msg)
    {
        IJ.log(msg);
    }
}
