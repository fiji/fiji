package archipelago.util;


import ij.IJ;
/**
 *
 * @author Larry Lindsey
 */
public class IJPopupLogger implements EasyLogger
{
    public void log(final String msg)
    {
        new Thread()
        {
            public void run()
            {
                IJ.showMessage(msg);
            }
        }.start();
    }
}
