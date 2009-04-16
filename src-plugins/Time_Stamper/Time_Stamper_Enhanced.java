// this plugin is a merge of the Time_Stamper plugins from ImageJ and from Tony Collins' plugin collection at macbiophotonics. 
// it aims to combine all the functionality of both plugins and refine and enhance the functionality. 
// Dan Whhite MPI-CBG 15.04.09


import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Time_Stamper_Enhanced implements PlugInFilter {
	ImagePlus imp;
	double time;
	static int x = 2;
	static int y = 15;
	static int size = 12;
	int maxWidth;
	Font font;
	static double start = 0;
	static double interval = 1;
	static String suffix = "s";
	static int decimalPlaces = 3;
	boolean canceled;
	static boolean digital = false;
	boolean AAtext = true;
	int frame, first, last;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		IJ.register(Time_Stamper_Enhanced.class);
		if (imp!=null) {
			first = 1;
			last = imp.getStackSize();
		}
		return DOES_ALL+DOES_STACKS+STACK_REQUIRED;
	}

	public void run(ImageProcessor ip) {
		frame++;
		if (frame==1) showDialog(ip);
		if (canceled || frame<first || frame>last) return;
		ip.setFont(font);
		ip.setColor(Toolbar.getForegroundColor());
		ip.setAntialiasedText(AAtext);
		String s = "";
		
		if (frame==last) imp.updateAndDraw();
	
		// decide if the time format is digital or decimal according to the plugin GUI input
		if (!digital) 
			s = getString(time);
		else
			s = getString2(time);
		ip.moveTo(x+maxWidth-ip.getStringWidth(s), y);
		ip.drawString(s);
		time += interval;  // increments the time by the time interval
	}

		// makes the string containing the number for the time stamp, with specified decimal places
		// format is decimal number with specificed no of digits after the point
		// if specificed no. of decimal places is 0 then just return the speficied suffix
		String getString(double time) {
			if (interval==0.0)
				return suffix;
			else
				return (decimalPlaces==0?""+(int)time:IJ.d2s(time, decimalPlaces))+" "+suffix;
		}



		// makes the string containing the number for the time stamp,
		// with hh:mm:ss.decimalPlaces format
		// which is nice, but also really need hh:mm:ss and mm:ss.ms etc. 
		// could use the java time/date formating stuff for that.
		String twoDigits(int value) {
			return (value < 10 ? "0" : "") + value;
		}

		String getString2(double time) {
			int hour = (int)(time / 3600);
			time -= hour * 3600;
			int minute = (int)(time / 60);
			time -= minute * 60;
			return twoDigits(hour) + ":" + twoDigits(minute) + ":"
				+ (time < 10 ? "0" : "") 
				+ IJ.d2s(time, decimalPlaces);
		}




	void showDialog(ImageProcessor ip) {
		// Here we work out the size of the font to use from the size of the ROI box drawn, if one was drawn		
		Rectangle roi = ip.getRoi();
		if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
			x = roi.x;
			y = roi.y+roi.height;
			size = (int) ((roi.height - 1.10526)/0.934211);	
			if (size<7) size = 7;
			if (size>80) size = 80;
		}
		
//		String[] timeunitsoptions =  { "y", "d", "h", "min", "s", "ms", "μs", "ns", "ps", "fs", suffix };
		
		// This makes the GUI and reads user input parameters from it
		GenericDialog gd = new GenericDialog("Time Stamper Enhanced");
		gd.addNumericField("Starting Time:", start, 2);
		gd.addNumericField("Time Interval Between Frames:", interval, 2);
		gd.addNumericField("X Location:", x, 0);
		gd.addNumericField("Y Location:", y, 0);
		gd.addNumericField("Font Size:", size, 0);
		gd.addNumericField("Decimal Places:", decimalPlaces, 0);
		gd.addNumericField("First Frame:", first, 0);
		gd.addNumericField("Last Frame:", last, 0);
		// should change this to a choice between digital or decimal
		gd.addCheckbox("'hh:mm:ss.ms' format:", digital);  // but what about mm:ss 
		gd.addStringField("Or with a suffix:", suffix);
		gd.addCheckbox("Anti-Aliased text?", true);
		
		// should be able to choose h m or s from a list?
	    gd.addChoice("Time units:", timeunitsoptions, timeunitsoptions[1]); 
		
		gd.showDialog();  // shows the dialog GUI!
		
		if (gd.wasCanceled())
			{canceled = true; return;}
		start = gd.getNextNumber();
 		interval = gd.getNextNumber();
		x = (int)gd.getNextNumber();
		y = (int)gd.getNextNumber();
		size = (int)gd.getNextNumber();
		decimalPlaces = (int)gd.getNextNumber();
		first = (int)gd.getNextNumber();
		last = (int)gd.getNextNumber();
		digital = gd.getNextBoolean();
		suffix = gd.getNextString();
		AAtext = gd.getNextBoolean(); 
		font = new Font("SansSerif", Font.PLAIN, size);
		ip.setFont(font);
		time = start;
		if (y<size)
			y = size;
		maxWidth = ip.getStringWidth(getString(start+interval*imp.getStackSize()));
		imp.startTiming();
	}

}
