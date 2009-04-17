// this plugin is a merge of the Time_Stamper plugins from ImageJ and from Tony Collins' plugin collection at macbiophotonics. 
// it aims to combine all the functionality of both plugins and refine and enhance the functionality. 
// Dan White MPI-CBG 15.04.09


import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Time_Stamper_Enhanced implements PlugInFilter {
	// declare the variables we are going to use in the plugin
	ImagePlus imp;
	double time;
	static int x = 2;
	static int y = 15;
	static int size = 12;
	//int maxWidth; // not using maxWidth anymore, see below
	Font font;
	static double start = 0;
	static double interval = 1;
	static String customSuffix = "";
	static String suffix = "s";
	static int decimalPlaces = 3;
	boolean canceled;
	static boolean digital = false;
	boolean AAtext = true;
	int frame, first, last;

	// setup the plugin and tell imagej it needs to work on a stack
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		IJ.register(Time_Stamper_Enhanced.class);
		if (imp!=null) {
			first = 1;
			last = imp.getStackSize();
		}
		return DOES_ALL+DOES_STACKS+STACK_REQUIRED;
	}

	// make the GUI for the plugin, with fields to fill all the variables we need. 
	void showDialog(ImageProcessor ip) {
		// Here we work out the size of the font to use from the size of the ROI box drawn, if one was drawn
		// and set x and y at the ROI if there is one, so time stamp is drawn there, not at default x and y. 
		Rectangle roi = ip.getRoi();
		if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
			x = roi.x;
			y = roi.y+roi.height;
			size = (int) ((roi.height - 1.10526)/0.934211);	// whats up with these numbers? Seems to make font too big?
			// make sure the font is not too big or small.... but why?
			if (size<7) size = 7;
			if (size>80) size = 80;
		}
	
		// here is a list of SI? approved time units for a drop down list to choose from 
		String[] timeunitsoptions =  { "y", "d", "h", "min", "s", "ms", "µs", "ns", "ps", "fs", "as" };
		
		// This makes the actual GUI 
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
		gd.addCheckbox("use digital 'hh:mm:ss.ms' format:", digital);  // but what about mm:ss 
		gd.addStringField("Or a customised suffix:", customSuffix);
		gd.addCheckbox("Anti-Aliased text?", true);
		
		// we can choose time units from a drop down list, list defined in timeunitsoptions
		gd.addChoice("Time units:", timeunitsoptions, timeunitsoptions[4]); 
		
		gd.showDialog();  // shows the dialog GUI!
		
		// handle the plugin cancel button being pressed.
		if (gd.wasCanceled())
			{canceled = true; return;}
		
		// This reads user input parameters from the GUI
		start = gd.getNextNumber();
 		interval = gd.getNextNumber();
 		x = (int)gd.getNextNumber();
		y = (int)gd.getNextNumber();
		size = (int)gd.getNextNumber();
		decimalPlaces = (int)gd.getNextNumber();
		first = (int)gd.getNextNumber();
		last = (int)gd.getNextNumber();
		digital = gd.getNextBoolean();
		customSuffix = gd.getNextString();
		AAtext = gd.getNextBoolean(); 
		suffix = gd.getNextChoice();
		font = new Font("SansSerif", Font.PLAIN, size);
		ip.setFont(font);
		time = start;
		if (y<size)
			y = size;
    	
		// maxWidth is an integer the length of the timeString for the last slice of the stack to be stamped
		// it is used to work out where to start writing the timestamp in the image,
		// was:
		// maxWidth = ip.getStringWidth(getString(start+interval*imp.getStackSize()));
		// actually why bother, just start writing the time stamp at specificed xy accoriofng to default or ROI
		// maxWidth = ip.getStringWidth(timeString(start+interval*imp.getStackSize()));
		imp.startTiming(); //What is this for?
	}	
	
	
		// makes the string containing the number for the time stamp, with specified
		// decimal places format is decimal number with specificed no of digits after
		// the point if specificed no. of decimal places is 0 then just return the
		// speficied customSuffix
	String decimalString(double time) { 
	  if (interval==0.0) 
	    return suffix; 
	  else
	    return (decimalPlaces == 0 ? ""+(int)time : IJ.d2s(time, decimalPlaces)) + " " + suffix; 
	}	
	
		// makes the string containing the number for the time stamp,
		// with hh:mm:ss.decimalPlaces format
		// which is nice, but also really need hh:mm:ss and mm:ss.ms etc. 
		// could use the java time/date formating stuff for that?
	String twoDigits(int value) {
		return (value < 10 ? "0" : "") + value;
	}
	
	String digitalString(double time) {
		int hour = (int)(time / 3600);
		time -= hour * 3600;
		int minute = (int)(time / 60);
		time -= minute * 60;
		return twoDigits(hour) + ":" + twoDigits(minute) + ":"
			+ (time < 10 ? "0" : "") 
			+ IJ.d2s(time, decimalPlaces);
	}

	
	

	
	public void run(ImageProcessor ip) {
		// is there a non empty string in the custom suffix box in the dialog GUI?
		// if so use it as suffix
		if (customSuffix != "")
				suffix = customSuffix;
		frame++;
		if (frame==1) showDialog(ip);
		if (canceled || frame<first || frame>last) return;
		ip.setFont(font);
		ip.setColor(Toolbar.getForegroundColor());
		ip.setAntialiasedText(AAtext);
		
		
		if (frame==last) imp.updateAndDraw();
	
		// decide if the time format is digital or decimal according to the plugin GUI input
		// if it is decimal (not digital) then need to set suffix from drop down list (or custom suffix if one is entered)
		// if it is digital, then there is no suffix as format is set yy:ddd:hh:mm:ss.ms? 
		String timeString = "";
		if (digital) 
			timeString = digitalString(time);
		else
			timeString = decimalString(time);
		// the next line tries to moive the time stamp right a bit to account for the max length th etime stamp will be.
		// possible superfulous, since you really want the time stamp to be written at the bottom left of the ROI you drew 
		// or from the default of x and y. So just move to x y instead
		//ip.moveTo(x+maxWidth-ip.getStringWidth(timeString), y);
		ip.moveTo(x, y);
		ip.drawString(timeString);
		time += interval;  // increments the time by the time interval

	}

	

}	// thats the end of Time_Stamper_Enhanced 


