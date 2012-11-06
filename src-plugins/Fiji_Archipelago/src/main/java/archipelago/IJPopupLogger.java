package archipelago;


import ij.IJ;

public class IJPopupLogger implements EasyLogger
{
    public void log(String msg) {
        IJ.showMessage(msg);
    }
}
