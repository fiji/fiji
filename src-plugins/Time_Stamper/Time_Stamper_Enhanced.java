/**
 * This plugin is a merge of the Time_Stamper plugins from ImageJ and from Tony Collins' plugin collection at macbiophotonics. 
it aims to combine all the functionality of both plugins and refine and enhance their functionality
for instance by adding the preview functionality suggested by Michael Weber.

 *It does not know about hyper stacks - multiple channels..... only works as expected for normal stacks.
That means a single channel time series or z stack. 

 *We might want to rename this tool "Stack Labeler", since it will handle labeling of Z stacks as well as time stacks. 

 *The sequence of calls to an ExtendedPlugInFilter is the following:
- setup(arg, imp): The filter should return its flags.
- showDialog(imp, command, pfr): The filter should display the dialog asking for parameters (if any)
and do all operations needed to prepare for processing the individual image(s) (E.g., slices of a stack).
For preview, a separate thread may call setNPasses(nPasses) and run(ip) while the dialog is displayed.
The filter should return its flags.
- setNPasses(nPasses): Informs the filter of the number of calls of run(ip) that will follow.
- run(ip): Processing of the image(s). With the CONVERT_TO_FLOAT flag,
this method will be called for each color channel of an RGB image.
With DOES_STACKS, it will be called for each slice of a stack.
- setup("final", imp): called only if flag FINAL_PROCESSING has been specified.
Flag DONE stops this sequence of calls.

 *We are using javas calendar for formatting the time stamps for "digital" style, 
 *but this has limitations, as you can t count over 59 min and the zero date is 01 01 1970, not zero. 

 *Here is a list (in no particular order) of requested and "would be nice" features that could be added:
-prevent longest label running off side of image  - ok
-choose colour  -ok
-font selection -ok
-top left, bottom right etc.  drop down menu 
-Hyperstacks z, t, c
-read correct time / z units, start and intervals from image metadata. Get it from Image Properties?
-every nth slice labelled -ok
-label only slices where time became greater than multiples of some time eg every 5 min. 
-preview with live update when change GUI -ok, changes in GUI are read into the preview. 
-preview with stack slider in the GUI. - slider now in GUI but functionality is half broken
-Use Java Date for robust formatting of dates/times counted in milliseconds. - added hybrid date form at for 
	versatile formatting of the digital time. 
-switch unit according to magnitude of number eg sec or min or nm or microns etc. 
- background colour for label. -0K.  

 *Dan White MPI-CBG , began hacking on 15.04.09. Work continued from 02-2010 by Tomka and Dan
 */

import ij.IJ;
import ij.IJEventListener;
import ij.ImagePlus;
import ij.gui.ColorChooser;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.ColorPicker;
import ij.plugin.frame.Fonts;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Time_Stamper_Enhanced implements ExtendedPlugInFilter,
		DialogListener { // , ActionListener {
	// http://rsb.info.nih.gov/ij/developer/api/ij/plugin/filter/ExtendedPlugInFilter.html
	// should use extended plugin filter for preview ability and for stacks!
	// then need more methods: setNPasses(int last-first) thats the number of
	// frames to stamp.
	// showDialog method needs another argument: PlugInFilterRunner pfr
	// also need Dialog listener and Action listener to listen to GUI changes?
	// declare the variables we are going to use in the plugin
	// note to self - static variables are things that should never change and
	// always be the same no matter what instance of the object is alive.
	// class member variables that need to change during execution should not be
	// static!
	// Static can be used to remember last used values,
	// so next time the plugin is run, it remembers the value it used last time.
	ImagePlus imp;
	// the px distance to the left
	int x = 2;
	// the px distance to the top
	int y = 15;
	// the font to draw the text with
	Font font;
	double start = 1.0;
	double interval = 1.0;
	double lastTime;
	// the custom suffix, used if format supports it
	String customSuffix = "";
	// the suffix to append to the time stamp, used if
	// format supports supports no custom suffix
	static String chosenSuffix = "s";
	// the amount of decimal places, used by some formats
	int decimalPlaces = 3;
	// indicates if processing has been canceled or not
	boolean canceled;
	// indicates if we are in preview mode or do the actual processing
	boolean preview = true;
	// the custom pattern for labeling, used if format supports it
	String customFormat = "";
	// indicates if the text should be anti-aliased or not
	boolean AAtext = true;
	// the current frame 
	int frame;
	// a visibility range for the stamps
	// these default to 0 as no values are given
	int first, last;
	// the 'n' for 'label every n-th frame'. Treated as 1 for values below one
	int frameMask = 1;
	// the object that runs the plugin.
	PlugInFilterRunner pluginFilterRunner; 
	// the slice currently worked on
	int currentSlice;
	// a combination (bitwise OR) of the flags specified in
	// interfaces PlugInFilter and ExtendedPlugInFilter.
	// determines what kind of image the plug-in can run on etc.
	int flags = DOES_ALL + DOES_STACKS + STACK_REQUIRED;

	// a list containing all supported formats, set up at runtime
	ArrayList<LabelFormat> formats = new ArrayList<LabelFormat>();
	// the currently selected format
	LabelFormat selectedFormat;
	// background of timestamp/label enabled
	private boolean backgroundEnabled = false;

	// a reference to the units drop-down list
	private Choice unitsChoice;
	// a reference to the custom suffix text field
	private TextField customUnitStringField;
	// a reference to the custom format text field
	private TextField customFormatStringField;
	// a reference to the font editor
	private FontPropertiesPanel fontProperties;
	// member variable for the GUI dialog
	private GenericDialog gd;

	/**
	 * Setup the plug-in and tell ImageJ it needs to work on a stack by
	 * returning the flags
	 */
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		IJ.register(Time_Stamper_Enhanced.class);
		if (imp != null) {
			first = 1;
			last = imp.getStackSize();
			setFontParams(imp);
		}

		// add all supported formats
		formats.add(new DecimalLabelFormat());
		formats.add(new DigitalLabelFormat());
		formats.add(new CustomLabelFormat());
		selectedFormat = formats.get(0);

		// return supported flags
		return flags;
	}

	/**
	 * Creates a string array out of the names of the available formats. Should
	 * move this after the run method to keep code style method order: setup,
	 * showDialog, setNPasses, run, other methods.
	 */
	private String[] getAvailableFormats() {
		String[] formatArray = new String[formats.size()];
		int i = 0;
		for (LabelFormat t : formats) {
			formatArray[i] = t.getName();
			i++;
		}
		return formatArray;
	}

	/**
	 * Make the GUI for the plug-in, with fields to fill all the variables we
	 * need. we are using ExtendedPluginFilter, so first argument is imp not ip.
	 */
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pluginFilterRunner = pfr;

		// This makes the GUI object
		gd = new NonBlockingGenericDialog(
				"Time Stamper Enhanced");

		// these are the fields of the GUI
		currentSlice = last;
		gd.addSlider("Slice (disabled, dont use me)", 1, imp.getStackSize(),
				currentSlice);
		((Scrollbar) (gd.getSliders().elementAt(0))).setEnabled(false);

		// this is a choice between digital or decimal
		// but what about mm:ss???
		// options are in the string array timeFormats, default is Decimal:
		// something.somethingelse
		String[] fl = getAvailableFormats();
		gd.addChoice("Label format:", fl, fl[0]);

		gd.addStringField("Custom Time Format:", customFormat);

		// the custom formats text-box could potentially be disabled,
		// depending on the format selection. So save a reference to it.
		customFormatStringField = (TextField) gd.getStringFields().get(0);

		// the list of supported units is determined by the currently selected
		// format
		String[] un = selectedFormat.getAllowedFormatUnits();
		gd.addChoice("Time units:", un, un[0]);

		// since we want to modify the contents of the formats drop-down list
		// later, we save s reference to it
		unitsChoice = (Choice) gd.getChoices().get(1);

		// we can set a custom suffix and use that by selecting custom
		// suffix in the time units drop down list above
		gd.addStringField("Custom Suffix:", customSuffix);

		// the custom suffix text-box could potentially be disabled,
		// depending on the format selection. So save a reference to it.
		customUnitStringField = (TextField) gd.getStringFields().get(1);

		gd.addNumericField("Starting Time (in selected units):", start, 2);
		gd.addNumericField("Time Interval Between Frames (in selected units):",
				interval, 3);
		gd.addNumericField("X Location:", x, 0);
		gd.addNumericField("Y Location:", y, 0);
		gd.addNumericField("Decimal Places:", decimalPlaces, 0);
		gd.addNumericField("First Frame:", first, 0);
		gd.addNumericField("Last Frame:", last, 0);
		gd.addNumericField("Label every n-th frame", frameMask, 0);
		
		fontProperties = new FontPropertiesPanel();
		gd.addPanel(fontProperties, GridBagConstraints.CENTER, new Insets(5, 0,
				0, 0));

		gd.addPreviewCheckbox(pfr); // adds preview checkbox - needs
		// ExtendedPluginFilter and DialogListener!
		gd.getPreviewCheckbox();
		gd.addMessage("Time Stamper plugin for Fiji (is just ImageJ - batteries included)\nmaintained by Dan White MPI-CBG dan(at)chalkie.org.uk");

		gd.addDialogListener(this); // needed for listening to dialog
		// field/button/checkbok changes?

		// add an ImageJ event listener to react to
		// color changes, etc.
		IJ.addEventListener(new IJEventListener() {
			public void eventOccurred(int event) {
				if (event == IJEventListener.FOREGROUND_COLOR_CHANGED
						|| event == IJEventListener.BACKGROUND_COLOR_CHANGED) {
					updatePreview(null);
				}
			}
		});

		updateUI();
		updateImg();

		gd.showDialog(); // shows the dialog GUI!

		// handle the plug-in cancel button being pressed.
		if (gd.wasCanceled())
			return DONE;

		// if the ok button was pressed, we are really running the plug-in,
		// so later we can tell what time stamp to make as its not the last
		// as used by preview
		preview = !gd.wasOKed();

		// initialise time with the value of the starting time
		// /time = start; moved to setNPasses

		// imp.startTiming(); //What is this for? Why need to know when it was
		// started... is this used elsewhere..?

		// extendedpluginfilter showDialog method should
		// return a combination (bitwise OR) of the flags specified in
		// interfaces PlugInFilter and ExtendedPlugInFilter.
		return DOES_ALL + DOES_STACKS + STACK_REQUIRED;
	}

	/**
	 * method to deal with changes in the GUI Should move this after the run
	 * method to keep code style method order: setup, showDialog, setNPasses,
	 * run, other methods.
	 */
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// This reads user input parameters from the GUI and listens to changes
		// in GUI fields
		int slice = (int) gd.getNextNumber(); // we dont use this as we read the
		// value of the slider in the
		// getCurrentSliceFromSlider
		// method, but we need to read
		// it so the next hetNextNumber
		// is right.

		// has the label format been changed?
		int currentFormat = gd.getNextChoiceIndex();
		LabelFormat lf = formats.get(currentFormat);
		if (lf != selectedFormat) {
			selectedFormat = lf;
			// if the format has changed, we need to modify the
			// units choice accordingly
			unitsChoice.removeAll();
			for (String unit : selectedFormat.getAllowedFormatUnits()) {
				unitsChoice.addItem(unit);
			}
		}

		customFormat = gd.getNextString();

		// get the selected suffix of drop-down list
		chosenSuffix = gd.getNextChoice();

		customSuffix = gd.getNextString();
		start = gd.getNextNumber();
		interval = gd.getNextNumber();
		x = (int) gd.getNextNumber();
		y = (int) gd.getNextNumber();
		decimalPlaces = (int) gd.getNextNumber();
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		frameMask = (int) gd.getNextNumber();

		updateUI();

		return true; // or else the dialog will have the ok button deactivated!
	}

	/**
	 * Updates the preview.
	 * 
	 * @param dialog
	 * @param e
	 */
	private void updatePreview(AWTEvent e) {
		if (gd != null){
			// tell the plug-in filter runner to update
			// the preview. Apparently, this is "OK"
			pluginFilterRunner.dialogItemChanged(gd, e);
		}
	}
	
	/**
	 * Updates some GUI components, based on the current state of the selected
	 * label format.
	 */
	private void updateUI() {
		// if the new format supports custom suffixes, enable
		// the custom suffix text box
		customUnitStringField.setEnabled(selectedFormat.supportsCustomSuffix());
		// if the current format supports custom format, enable
		// the custom format text box
		customFormatStringField.setEnabled(selectedFormat
				.supportsCustomFormat());
	}

	/**
	 * This is part of the preview functionality. Informs the filter of the
	 * number of calls of run(ip) that will follow. The argument nPasses is
	 * worked out by the plug-in runner.
	 */
	public void setNPasses(int nPasses) {
		// frame = first; // dont need this
		// time = lastTime(); // set the time to lastTime, when doing the
		// preview run,
		// so the preview does not increment time when clicking the preview box
		// causes run method execution.
		// and i see the longest time stamp that will be made when i do a
		// preview, so i can make sure its where
		// i wanted it.
		// that works, but now the time stamper counts up from time = lastTime
		// value not from time = (start + (first*interval))
		// when making the time stamps for the whole stack...

		if (preview) {
			frame = currentSlice;
		} else {
			frame = 1; // so the value of frame is reset to 0 each time the
			// plugin is run or the preview checkbox is checked.
		}
		frame--;
	}

	/**
	 * Run the plug-in on the ip object, which is the ImageProcessor object
	 * associated with the open/selected image. but remember that showDialog
	 * method is run before this in ExtendedPluginFilter
	 */
	public void run(ImageProcessor ip) {
		// this increments frame integer by 1. If an int is declared with no
		// value, it defaults to 0
		frame++;

		// Updates this image from the pixel data in its associated
		// ImageProcessor object and then displays it
		// if it is the last frame. Why do we need this when there is
		// ip.drawString(timeString); below?
		if (frame == last)
			imp.updateAndDraw();

		// the following line isnt needed in ExtendedPluginFilter because
		// setNPasses takes care of number of frames to write in
		// and ExtendedPluginFilter executes the showDialog method before the
		// run method, always, so don't need to call it in run.
		// if (frame==1) showDialog(imp, "TimeStamperEnhanced", pfr); // if at
		// the 1st frame of the stack, show the GUI by calling the showDialog
		// method
		// and set the variables according to the GUI input.

		// tell the run method when to not do anything just return
		// Here there is a bug: with the new use of ExtendedPluginFilter,
		// using preview on, the first time stamp is placed in frame first-1 not
		// first...
		// and the last time stamp in last-1. With preview off it works as
		// expected.
		if ((!preview) && (canceled || frame < first || frame > last || (frame % frameMask != 0)))
			return;

		// if (fontProperties != null)
		// fontProperties.updateGUI(font);
		ip.setFont(font);
		ip.setAntialiasedText(AAtext);

		// Have moved the font size and xy location calculations for timestamp
		// stuff out of the run method, into their own methods.
		// set the font size according to ROI size, or if no ROI the GUI text
		// input
		Rectangle backgroundRectangle = getBackgroundRectangle(ip);
		if (backgroundEnabled){
			ip.setColor(Toolbar.getBackgroundColor());
			ip.fill(new Roi(backgroundRectangle));
		}
		ip.setColor(Toolbar.getForegroundColor());
		ip.moveTo(backgroundRectangle.x, (backgroundRectangle.y + backgroundRectangle.height) );

		double time = getTimeFromFrame(frame); // ask the getTimeFromFrame
		// method to return the time for
		// that frame
		ip.drawString(selectedFormat.getTimeString(time)); // draw the
		// timestring into
		// the image
		// showProgress(percent done calc here); // dont really need a progress
		// bar... but seem to get one anyway...
	}

	/*
	 * private void setLabelFormat(){ String format = ""; if
	 * (timeFormat.equals("hh:mm:ss.ms")) format = "HH:mm:ss:SSS"; else if
	 * (timeFormat.equals("Decimal")) format = ""; else return
	 * ("timeFormat was not selected!"); }
	 */

	void setFontParams(ImagePlus imp) { // work out the size of the font to use
		// from the size of the ROI box drawn,
		// if one was drawn (how does it know?)
		int size = 12;

		Roi roi = imp.getRoi();
		if (roi != null) {
			Rectangle theROI = roi.getBounds();
			// single characters fit the ROI, but if the time stamper string is
			// long
			// then the font is too big to fit the whole thing in!

			// need to see if we should use the ROI height to set the font size
			// or read it from the plugin gui
			// if (theROI != null) doesnt work as if there is no ROI set, the
			// ROI is the size of the image! There is always an ROI!
			// So we can say, if there is no ROI set , its the same size as the
			// image, and if that is the case,
			// we should then use the size as read from the GUI.

			if (theROI.height != imp.getHeight()
					|| theROI.width != imp.getWidth()) // if the ROI is the same
				// size as the image
				// leave size as it was
				// set by the gui
				size = (int) (theROI.height); // - 1.10526)/0.934211) whats up
			// with these numbers? Are they
			// special? pixel to point size
			// conversion? // if there is an
			// ROI not the same size as the
			// image then set size to its
			// height.

			// make sure the font is not too big or small.... but why? Too -
			// small cant read it. Too Big - ?
			// should this use private and public and get / set methods?
			// in any case it doesnt seem to work... i can set the font < 7 and
			// it is printed that small.
			if (size < 7)
				size = 7;
			else if (size > 80)
				size = 80;
			// if no ROI, x and y are defaulted or set according to text in gui
		}

		font = new Font(TextRoi.getFont(), TextRoi.getStyle(), size);
	}

	/**
	 * 	method to position the time stamp string correctly, so it is all on the
	 *	image, even for the last frames with bigger numbers.
	 *	ip.moveTo(x, y); // move to x y position for Timestamp writing
	 *
	 *	the maxwidth if statement tries to move the time stamp right a bit to
	 *  account for the max length the time stamp will be.
	 *  it's nice to not have the time stamp run off the right edge of the image.
	 *  how about subtracting the
	 *  maxWidth from the width of the image (x dimension) only if its so close
	 *  that it will run off.
	 *  this seems to work now with digital and decimal time formats.
	 */
	Rectangle getBackgroundRectangle(ImageProcessor ip) {
		// Here we set x and y at the ROI if there is one (how does it know?),
		// so time stamp is drawn there, not at default x and y.
		Rectangle roi = ip.getRoi();
		// set the xy time stamp drawing position for ROI smaller than the
		// image, to bottom left of ROI
		if (roi.width == ip.getWidth() && roi.height == ip.getHeight()) {
			roi.x = x;
			roi.y = y;
			roi.height = font.getSize();
		}
			
		// make sure the y position is not less than the font height: size,
		// so the time stamp is not off the top of the image?
		if ( (roi.y + roi.height) < font.getSize())
			roi.y = 1;
		
		roi.width = maxWidth(ip, selectedFormat.lastTimeStampString());
		
		// if longest timestamp is wider than (image width - ROI width) , move x
		// in appropriately
		if (roi.width > (ip.getWidth() - roi.x)) {
			roi.x = (ip.getWidth() - roi.width);
		}
		
		return roi;
	}

	/**
	 * this method adds a preceeding 0 to a number if it only has one digit
	 * instead of two. Which is handy for making 00:00 type format strings
	 * later. Thx Dscho.
	 */
	String twoDigits(int value) {
		return (value < 10 ? "0" : "") + value;
	}

	/**
	 * Returns the time of the last frame.
	 */
	double lastTime() {
		return getTimeFromFrame(last); // is the last time for which a time
		// stamp will be made
	}

	/**
	 * Returns the time of a specific frame.
	 * 
	 * @param f
	 *            The frame to calculate the time of
	 * @return The time at frame f
	 */
	double getTimeFromFrame(int f) {
		return start + (interval * (f - 1)); // is the time for a certain frame
	}

	// moved out of run method to its own method.
	// maxWidth is an integer = length of the decimal time stamp string in
	// pixels
	// for the last slice of the stack to be stamped. It is used in the run
	// method,
	// to prevent the time stamp running off the right edge of the image
	// ip.getStringWidth(string) seems to return the # of pixels long a string
	// is in x?
	// how does it take care of font size i wonder? The font is set
	// using the variable size... so i guess the ip object knows how big the
	// font is.
	int maxWidth(ImageProcessor ip, String lastTimeStampString) {
		return ip.getStringWidth(lastTimeStampString);
	}

	// to make progress bar in main imagej panel. ... looks like we get one
	// anyway...can see with several hundred frame stack?
	// void showProgress(double percent) { // dont really need a progress bar...
	// percent = (double)(frame-1)/nPasses + percent/nPasses; //whats this for?
	// IJ.showProgress(percent);
	// }

	private void updateImg() {
		imp.setSlice(currentSlice);
	}

	/**
	 * A class representing a supported label format of the time stamper
	 * plug-in. It relates supported format units/suffixes to a format name.
	 * Besides that it determines if a custom suffix should be usable.
	 */
	private abstract class LabelFormat {
		// an array of all the supported units for this format
		String[] allowedFormatUnits;
		// the display name for this format
		String name;
		// a member indicating if a custom suffix should be supported
		boolean customSuffixSupported;
		// a member indicating if a custom format should be supported
		boolean customFormatSupported;

		/**
		 * 
		 * @param allowedFormatUnits
		 * @param name
		 * @param supportCustomSuffix
		 * @param supportCustomFormat
		 */
		public LabelFormat(String[] allowedFormatUnits, String name,
				boolean supportCustomSuffix, boolean supportCustomFormat) {
			this.allowedFormatUnits = allowedFormatUnits;
			this.name = name;
			this.customSuffixSupported = supportCustomSuffix;
			this.customFormatSupported = supportCustomFormat;
		}

		/**
		 * Here we make the strings to print into the images. decide if the time
		 * format is digital or decimal according to the plugin GUI input if it
		 * is decimal (not digital) then need to set suffix from drop down list
		 * which might be custom suffix if one is entered and selected. if it is
		 * digital, then there is no suffix as format is set hh:mm:ss.ms
		 * 
		 * @param time
		 * @return
		 */
		public abstract String getTimeString(double time);

		/**
		 * this method returns the string of the TimeStamp for the last frame to
		 * be stamped which is for the frame with value of the last variable. It
		 * should be the longest string the timestamp will be. we should use
		 * this in maxWidth method and for the preview of the timestamp used to
		 * be: maxWidth = ip.getStringWidth(decimalString(start +
		 * interval*imp.getStackSize())); but should use last not stacksize,
		 * since no time stamp is made for slices after last? It also needs to
		 * calculate maxWidth for both digital and decimal time formats:
		 * 
		 * @return
		 */
		public String lastTimeStampString() {
			return getTimeString(lastTime());
		}

		/**
		 * Gets the suffix (if any) that should be appended to the time stamp.
		 * 
		 * @return The suffix to display.
		 */
		public String suffix() {
			return chosenSuffix;
		}

		/**
		 * Gets an array of allowed units for the format. E. g. to display them
		 * in a drop down component.
		 * 
		 * @return The allowed units.
		 */
		public String[] getAllowedFormatUnits() {
			return allowedFormatUnits;
		}

		/**
		 * Gets the display name of the format.
		 * 
		 * @return The display name of the format
		 */
		public String getName() {
			return name;
		}

		/**
		 * Indicates whether custom (user input) suffixes are allowed to this
		 * format or not.
		 * 
		 * @return True if custom suffixes are allowed, false otherwise
		 */
		public boolean supportsCustomSuffix() {
			return customSuffixSupported;
		}

		/**
		 * Indicates whether a custom time format can be used by the format.
		 * This could be useful for decimal formats like HH:mm which should be
		 * user definable.
		 * 
		 * @return True if the format supports custom formats, false otherwise.
		 */
		public boolean supportsCustomFormat() {
			return customFormatSupported;
		}
	}

	/**
	 * Represents a decimal label format, e. g. 7.3 days.
	 */
	private class DecimalLabelFormat extends LabelFormat {

		/**
		 * Constructs a new {@link DecimalLabelFormat}. This default constructor
		 * allows years, weeks, days, hours, minutes, seconds and milli-,
		 * micro-, nano-, pico- pico-, femto and attoseconds as formats. The
		 * display name is set to "Decimal".
		 */
		public DecimalLabelFormat() {
			this(new String[] { "y", "w", "d", "h", "min", "s", "ms", "us",
					"ns", "ps", "fs", "as" }, "Decimal");
		}

		/**
		 * Constructs a new {@link DecimalLabelFormat}. The allowed units and a
		 * name are requested as parameters. No custom time format input is
		 * supported, but one can use a custom suffix with this class.
		 * 
		 * @param allowedFormatUnits
		 *            The allowed units for this format.
		 * @param name
		 *            The display name of the format.
		 */
		public DecimalLabelFormat(String[] allowedFormatUnits, String name) {
			this(allowedFormatUnits, name, false, false);
		}

		protected DecimalLabelFormat(String[] allowedFormatUnits, String name,
				boolean supportCustomSuffix, boolean supportCustomFormat) {
			super(allowedFormatUnits, name, supportCustomSuffix,
					supportCustomFormat);
		}

		/**
		 * Makes the string containing the number for the time stamp, with
		 * specified decimal places format is decimal number with specified no
		 * of digits after the point if specified no. of decimal places is 0
		 * then just return the specified suffix
		 */
		@Override
		public String getTimeString(double time) {
			if (interval == 0.0)
				return suffix();
			else
				return (decimalPlaces == 0 ? "" + (int) time : IJ.d2s(time,
						decimalPlaces))
						+ " " + suffix();
		}
	}

	/**
	 * Represents a digital label format, e. g. HH:mm:ss:ms.
	 */
	private class DigitalLabelFormat extends LabelFormat {
		// A calendar to calculate time representation with.
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar calendar = new GregorianCalendar();

		/**
		 * Constructs a new {@link DigitalLabelFormat}. This default constructor
		 * allows minutes, seconds and milliseconds only as possible formats.
		 * The display name is set to "Digital".
		 */
		public DigitalLabelFormat() {
			this(new String[] { "min", "s", "ms" }, "Digital");
		}

		/**
		 * Constructs a new {@link DigitalLabelFormat}. The allowed units and a
		 * name are requested as parameters. No custom time format input is
		 * supported, but one can use a custom suffix with this class.
		 * 
		 * @param allowedFormatUnits
		 *            The allowed units for this format.
		 * @param name
		 *            The display name of the format.
		 */
		public DigitalLabelFormat(String[] allowedFormatUnits, String name) {
			super(allowedFormatUnits, name, false, true);
		}

		/**
		 * Makes the string containing the number for the time stamp, with
		 * hh:mm:ss.decimalPlaces format which is nice, but also really need
		 * hh:mm:ss and mm:ss.ms etc. could use the java time/date formating
		 * stuff for that?
		 */
		@Override
		public String getTimeString(double time) {
			calendar.setTimeInMillis(0);
			// if (chosenSuffix.equals("y")){ //"y", "d", "h", "min", "s", "ms",
			// "us", "ns", "ps", "fs", "as", "Custom Suffix"
			// time = time * (365.25*24.0*60.0*60.0*1000.0); //
			// c.set(Calendar.YEAR, time); // time would have to be an
			// integer... so can't use that
			// }
			// if (chosenSuffix.equals("w")){
			// time = time * (7.0*24.0*60.0*60.0*1000.0);
			// }
			// else if (chosenSuffix.equals("d")){
			// time = time * (24.0*60.0*60.0*1000.0);
			// }
			// if (chosenSuffix.equals("h")){
			// time = time * (60.0*60.0*1000.0);
			// }
			if (chosenSuffix.equals("min")) {
				time = time * (60.0 * 1000.0);
			} else if (chosenSuffix.equals("s")) {
				time = time * 1000.0;
			} else if (chosenSuffix.equals("ms")) {
				// trivial case, nothing to do:
				// time = time;
			}
			// if its not h m s or ms then wont use SimpleDateFormat below for
			// digital time.
			// so reset chosenSuffix to s.
			else {
				IJ
						.error("For a digital 00:00:00.000 time you must use min, s or ms only as the time units.");

			}

			// set the time
			calendar.setTimeInMillis(Math.round(time));
			DateFormat f;
			try {
				// check if a custom format has been entered
				if (customFormat.length() > 0) {
					// Make sure that our custom format can be handled

					f = new HybridDateFormat(customFormat);

				} else {
					f = new SimpleDateFormat("HH:mm:ss.SSS");
				}
				f.setTimeZone(tz); // the SimpleDateFormat needs to know the
				// time zone is UTC!

				return f.format(calendar.getTime());
			} catch (IllegalArgumentException ex) {
				return ex.getMessage();
			}

			/*
			 * int hour = (int)(time / 3600); time -= hour * 3600; int minute =
			 * (int)(time / 60); time -= minute * 60; return twoDigits(hour) +
			 * ":" + twoDigits(minute) + ":" + (time < 10 ? "0" : "") +
			 * IJ.d2s(time, decimalPlaces);
			 */
		}
	}

	/**
	 * Represents a label format that is essentially the same as a decimal
	 * format, but allows custom suffixes.
	 */
	private class CustomLabelFormat extends DecimalLabelFormat {

		/**
		 * Creates a new {@link CustomLabelFormat}. It allows only custom
		 * suffixes as units and its display name is set to "Custom Format".
		 */
		public CustomLabelFormat() {
			this(new String[] { "Custom Suffix" }, "Custom Format");
		}

		protected CustomLabelFormat(String[] allowedFormatUnits, String name) {
			super(allowedFormatUnits, name, true, false);
		}

		@Override
		public String suffix() {
			return customSuffix;
		}
	}

	/**
	 * A panel containing several font manipulation items. It consists of a type
	 * face chooser, font size chooser, style chooser and a checkbox to
	 * enable/disable anti-aliased text (smoothing).
	 */
	@SuppressWarnings("serial")
	private class FontPropertiesPanel extends Panel{
		/**
		 * Creates a new {@link FontPropertiesPanel} containing the font setting
		 * and font colour buttons.
		 */
		public FontPropertiesPanel() {
			Button fontStyleButton = new Button("Font Settings");
			
			fontStyleButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					new ExtendedFonts();

				}
			});
			
			Button fontColourButton = new Button("Font Color");

			fontColourButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					new ColorPicker();

				}
			});
			
			final Checkbox drawBackground = new Checkbox("Background");
			
			drawBackground.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					backgroundEnabled = drawBackground.getState();
					updatePreview(e);
				}
			});
			
			add(fontStyleButton);
			add(fontColourButton);
			add(drawBackground);
		}
	}
	
	@SuppressWarnings("serial")
	class ExtendedFonts extends Fonts{
		@Override
		public void itemStateChanged(ItemEvent e) {
			super.itemStateChanged(e);
			font = new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi.getSize());
			AAtext = TextRoi.isAntialiased();
			updatePreview(e);
		}
		
	}
} // thats the end of Time_Stamper_Enhanced class

