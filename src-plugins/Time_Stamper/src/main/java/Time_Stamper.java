import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Time_Stamper implements ExtendedPlugInFilter {
    ImagePlus imp;
    static int x = 2;
    static int y = 15;
    static int size = 12;
    int maxWidth;
    Font font;
    static double start = 0;
    static double interval = 1;
    static String suffix = "sec";
    static int decimalPlaces = 0;
    int idx = 1;
    static boolean digital = false;
    boolean AAtext=true;

    final int flags = DOES_ALL + DOES_STACKS + STACK_REQUIRED;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        IJ.register(Time_Stamper.class);
        return flags;
    }

    public void setNPasses(int nPasses) {
        // nothing to do here
    }

    public void run(ImageProcessor ip) {
        ip.setFont(font);
        ip.setColor(Toolbar.getForegroundColor());
        ip.setAntialiasedText(AAtext);

        // use frames, if possible
        boolean useFrames = imp.isHyperStack() || imp.getNFrames() > 1;
        // get current time
        int[] pos = imp.convertIndexToPosition(idx);
        int frame = useFrames ? pos[2] : pos[1];
        double time = start + (frame - 1) * interval;
        // create output string
        String s = "";
        if (digital)
            s = getString2(time);
        else
            s = getString(time);

        ip.moveTo(x+maxWidth-ip.getStringWidth(s), y);
        ip.drawString(s);
        // increment frame number
        idx++;
    }

    String getString(double time) {
        if (Math.abs(interval) < 0.00001)
            return suffix;
        // cut decimal places if they are not wanted
        if (decimalPlaces == 0)
            return (int)time + " " + suffix;
        else
            return IJ.d2s(time, decimalPlaces) + " " + suffix;
    }

    String getString2(double time) {
        if (time < 10) return "00:0"+(int)time;
        if (time < 60) return "00:"+(int)time;
        if (time >= 60) {
            int hour = (int) time/60;
            int min  = (int) time%60;
            if (hour < 10 && min < 10) return "0"+hour+":0"+min;
            if (hour < 10 && min >=10) return "0"+hour+":"+min;
            if (hour >= 10 && min < 10) return hour+":0"+min;
            if (hour >= 10 && min >=10) return hour+":"+min;
        }
        // cut decimal places if they are not wanted
        if (decimalPlaces==0)
            return "" + (int)time;
        else
            return IJ.d2s(time,decimalPlaces);
    }

    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        ImageProcessor ip = imp.getProcessor();
        Rectangle roi = ip.getRoi();
        if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
            x = roi.x;
            y = roi.y+roi.height;
            size = (int) ((roi.height - 1.10526)/0.934211);
            if (size<7) size = 7;
            if (size>80) size = 80;
        }
        String[] timeunitsoptions =  { "hours","minutes", "seconds"};

        GenericDialog gd = new GenericDialog("Time Stamper");
        gd.addNumericField("Starting Time:", start, 2);
        gd.addNumericField("Interval:", interval, 2);
        // gd.addChoice("Time units:", timeunitsoptions, timeunitsoptions[1]);

        gd.addNumericField("X Location:", x, 0);
        gd.addNumericField("Y Location:", y, 0);
        gd.addNumericField("Font Size:", size, 0);

        gd.addCheckbox("'00:00' format:", digital);
        gd.addNumericField("Decimal Places:", decimalPlaces, 0);
        gd.addStringField("Or with a suffix  Suffix:", suffix);
        gd.addCheckbox("Anti-Aliased text?", true);

        gd.showDialog();
        if (gd.wasCanceled())
            return DONE;

        start = gd.getNextNumber();
        interval = gd.getNextNumber();
        int timeindex = gd.getNextChoiceIndex();
        //String timeunits = timeunitsoptions[timeindex];

        x = (int)gd.getNextNumber();
        y = (int)gd.getNextNumber();
        size = (int)gd.getNextNumber();

        digital = gd.getNextBoolean();

        decimalPlaces = (int)gd.getNextNumber();
        AAtext = gd.getNextBoolean();
        suffix = gd.getNextString();
        font = new Font("SansSerif", Font.PLAIN, size);
        ip.setFont(font);
        if (y<size)
            y = size;
        maxWidth = ip.getStringWidth(getString(start+interval*imp.getStackSize()));
        imp.startTiming();

        return flags;
    }
}
