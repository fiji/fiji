package archipelago;

import ij.IJ;

public class IJLogger implements EasyLogger
{

    public synchronized void log(String msg)
    {
        IJ.log(msg);
    }
}
