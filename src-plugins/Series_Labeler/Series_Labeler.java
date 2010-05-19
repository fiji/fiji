/**
 * This plugin is a merge of the Time_Stamper plugins from ImageJ
 * and from Tony Collins' plugin collection at macbiophotonics.
 * It aims to combine all the functionality of both plugins and
 * refine and enhance their functionality for instance by adding
 * the preview functionality suggested by Michael Weber.
 *
 * It currently does not know about hyper stacks - multiple
 * channels... (i.e. only works as expected for normal stacks).
 * That means a single channel time series or z stack.
 *
 * We might want to rename this tool "Series Labeler", since it
 * will handle labeling of Z stacks as well as time stacks, as
 * well as any other series of images contained in an imageJ
 * stack. Spectral series, etc.
 *
 * The sequence of calls to an ExtendedPlugInFilter is the
 * following:
 *  - setup(arg, imp): The filter should return its flags.
 *  - showDialog(imp, command, pfr): The filter should display
 *    the dialog asking for parameters (if any) and do all
 *    operations needed to prepare for processing the individual
 *    image(s) (E.g., slices of a stack). For preview, a separate
 *    thread may call setNPasses(nPasses) and run(ip) while the
 *    dialog is displayed. The filter should return its flags.
 *  - setNPasses(nPasses): Informs the filter of the number of
 *    calls of run(ip) that will follow.
 *  - run(ip): Processing of the image(s). With the
 *    CONVERT_TO_FLOAT flag, this method will be called for each
 *    color channel of an RGB image. With DOES_STACKS, it will be
 *    called for each slice of a stack. But is this irrelevant
 *    here?
 *  - setup("final", imp): called only if flag FINAL_PROCESSING
 *    has been specified. Flag DONE stops this sequence of calls.
 *
 * We are using javas calendar for formatting the time stamps for
 * "digital" style, but this has limitations, as you can't count
 * over 59 min and the zero date is 01 01 1970, not zero.
 *
 * Here is a list (in no particular order) of requested and "would
 * be nice" features that could be added:
 *  - prevent longest label running off sides and top/bottom of
 *     image (done)
 *  - choose colour (done. uses imageJ builtin color chooser)
 *  - font selection (done, uses imageJ builtin font chooser
 *  - top left, bottom right etc.  drop down menu (done)
 *  - Hyperstacks z, t, c
 *  - read correct time / z units, start and intervals from image
 *    metadata. Get it from Image Properties?
 *  - every nth slice labelled (done)
 *  - label only slices where time became greater than multiples
 *     of some time eg every 5 min.
 *  - preview with live update when change GUI -ok, changes in
 *    GUI are read into the preview.
 *  - preview with stack slider in the GUI. - slider now in GUI
 *    but functionality is half broken
 *  - Use Java Date for robust formatting of dates/times counted
 *    in milliseconds. (done, added hybrid date form at for
 *    versatile formatting of the digital time)
 *  - switch unit according to magnitude of number eg sec or min
 *    or nm or microns etc.
 *  - background colour for label (done)
 *
 * Dan White MPI-CBG , began hacking on 15.04.09. Work continued
 * from 02-2010 by Tomka and Dan
 */

import ij.IJ;
import ij.IJEventListener;
import ij.ImagePlus;
import ij.Macro;
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
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.DocumentListener;

public class Series_Labeler implements ExtendedPlugInFilter,
		DialogListener {

	ImagePlus imp;
	// the distance from left of image in px to put the label. 
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
	/* the suffix to append to the time stamp, used if
	 * format supports supports no custom suffix
	 */
	static String chosenSuffix = "s";
	// the amount of decimal places, used by some formats
	int decimalPlaces = 3;
	// indicates if processing has been canceled or not
	boolean canceled;
	/* indicates if we are in preview mode or doing the
	 * actual processing
	 */
	boolean preview = true;
	/* the custom pattern for labeling, used if format
	 * supports it
	 */
	String customFormat = "";
	// indicates if the text should be anti-aliased or not
	boolean antiAliasedText = true;
	// the current frame 
	int frame;
	/* a generation range for the labels, these default to 0 as no
	 * values are given
	 */
	int first, last;
	/* the 'n' for 'label every n-th frame'. Treated as 1 for values
	 * below one
	 */
	int frameMask = 1;
	// the object that runs the plugin.
	PlugInFilterRunner pluginFilterRunner; 
	/* a combination (bitwise OR) of the flags specified in
         * interfaces PlugInFilter and ExtendedPlugInFilter.
         * Determines what kind of image the plug-in can run on etc.
	 */
	int flags = DOES_ALL + DOES_STACKS + STACK_REQUIRED;

	// the currently selected format
	AbstractStampFormat selectedFormat;
	// background of timestamp/label enabled
	private boolean backgroundEnabled = false;

	// member variable for the GUI dialog
	private GenericDialog gd;
	
	// String representations of the location presets offered
	static final String[] locations = {"Upper Right", "Lower Right",
		"Lower Left", "Upper Left", "Custom"};
	// Some index aliases for the locations array
	static final int UPPER_RIGHT=0, LOWER_RIGHT=1, LOWER_LEFT=2,
		UPPER_LEFT=3, CUSTOM=4;
	// the currently selected location preset
	int locationPreset;
	
	// the available kinds of stack we can label. 
	final String[] stackTypes = { "z-stack",
		"time series or movie" }; //, "spectral series" };
	
	// the available time formats
	final AbstractStampFormat[] formats = {new DecimalLabelFormat(),
		new DigitalLabelFormat(), new CustomLabelFormat()};
	
	// GUI variables that are needed to read out data
	// from the components
	private Choice labelUnitsComboBox;
	private Choice locationPresetsComboBox;
	private JPanel generalSettingsContainer;
	private JPanel unitsFormattingContainer;
	private JPanel startStopIntervalsContainer;
	private JPanel locationFontContainer;
	private JPanel fontPropertiesContainer;
	private TextField locationXTextField;
	private TextField locationYTextField;
	
	// the panel containing the units selection
	private JPanel labelUnitsPanel;
	// the panel containing the custom suffix elements
	private JPanel customSuffixPanel;
	// the panel containing the custom formats elements
	private JPanel customLabelFormatPanel;
	// the panel containing the Decimal Places elements
	private JPanel decimalPlacesPanel;

	/**
	 * Setup the plug-in and tell ImageJ it needs to work on
	 * a stack by returning the flags
	 */
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		IJ.register(Series_Labeler.class);
		if (imp != null) {
			first = 1;
			last = imp.getStackSize();
			setFontParams();
		}

		/* set the currently selected format to the
		 * first available
		 */
		selectedFormat = formats[0];

		// return supported flags
		return flags;
	}

	/**
	 * Make the GUI for the plug-in, with fields to fill all
	 * the variables we need. We use ExtendedPluginFilter,
	 * so first argument is imp not ip.
	 */
	public int showDialog(ImagePlus imp, String command,
			PlugInFilterRunner pfr) {
		preview = !IJ.isMacro();
		this.pluginFilterRunner = pfr;
		int subpanelHeight = 30;
		int left = 20;
		
		// This makes the GUI object
		gd = new NonBlockingGenericDialog(
				"Series Labeler");

		/*
		 * General settings panel
		 */
		generalSettingsContainer = createContainerPanel(70,
			"General Settings");
		
		//add combobox for stack type
		JPanel stackTypePanel = createComboBoxPanel(
			"Stack_Type", stackTypes, 1, 100, 180);
		stackTypePanel.setLocation(left, 30);
		
		addPanelsToDialog(generalSettingsContainer,
			new JPanel[] {stackTypePanel} );
		
		/*
		 * Units formatting panel
		 */
		unitsFormattingContainer = createContainerPanel(100,
			"Units_Formatting");
		
		// add combobox for label format
		JPanel pLabelFormat = createComboBoxPanel("Label_Format",
			getAvailableFormats(), 0);
		pLabelFormat.setLocation(left, 30);
        
		// add combobox for label unit
		labelUnitsPanel = createComboBoxPanel("Label_Unit",
			selectedFormat.getAllowedFormatUnits(), 0);
		labelUnitsComboBox = (Choice) gd.getChoices().lastElement();
		labelUnitsPanel.setLocation(left, 60);
        
		// add Custom Suffix panel
		customSuffixPanel = createTextFieldPanel("Custom_Suffix",
			customSuffix);
		customSuffixPanel.setLocation(left, 60);
		
		// add Custom Format panel
		customLabelFormatPanel = createTextFieldPanel("Custom_Format",
			customFormat);
		customLabelFormatPanel.setLocation(300, 30);
		
		// add Decimal Places panel
		decimalPlacesPanel = createNumericFieldPanel("Decimal_Places",
			decimalPlaces, 0);
		decimalPlacesPanel.setLocation(300, 30);
		
		addPanelsToDialog(unitsFormattingContainer,
			new JPanel[] {pLabelFormat, customSuffixPanel,
				labelUnitsPanel, decimalPlacesPanel,
				customLabelFormatPanel} );
		
		/*
		 * Start/Stop/Interval
		 */
		startStopIntervalsContainer = createContainerPanel(130,
			"Start/Stop/Interval of Stack");
		
		// add a panel for the series stamper start value
		JPanel pStartup = createNumericFieldPanel("Startup",
			start, 2);
		pStartup.setLocation(left, 30);
		
		// add a panel for the interval settings
		JPanel pInterval = createNumericFieldPanel("Interval",
			interval, 2);
		pInterval.setLocation(left, 60);
		
		// add panel for the everyNth setting
		JPanel pEveryNth = createNumericFieldPanel("Every_n-th",
			frameMask, 0);
		pEveryNth.setLocation(left, 90);
		
		// add panel for First Frame setting
		JPanel pFirstFrame = createNumericFieldPanel("First",
			first, 0);
		pFirstFrame.setLocation(300, 30);
		
		// add panel for Last Frame setting
		JPanel pLastFrame = createNumericFieldPanel("Last",
			last, 0);
		pLastFrame.setLocation(300, 60);
		
		addPanelsToDialog(startStopIntervalsContainer,
			new JPanel[] {pStartup, pInterval,
				 pEveryNth, pFirstFrame, pLastFrame} );
		
		/*
		 * Location and Font panel
		 */
		locationFontContainer = createContainerPanel(110,
			"Location & Font");
		
		// add panel for X location
		JPanel pLocationX = createNumericFieldPanel("X_",
			x, 0, 20, 50);
		locationXTextField =
			(TextField) gd.getNumericFields().lastElement();
		pLocationX.setLocation(left, 30);
		
		// add panel for Y location
		JPanel pLocationY =
			createNumericFieldPanel("Y_", y, 0, 20, 50);
		locationYTextField =
			(TextField) gd.getNumericFields().lastElement();
		pLocationY.setLocation(120, 30);
		
		if (isCustomROI())
			 locationPreset = CUSTOM;
		else locationPreset = UPPER_LEFT;
		
		// add combobox for location presets
		JPanel pLocationPresets = createComboBoxPanel(
			"Location_Presets", locations,
			locationPreset, 110, 150);
		locationPresetsComboBox =
			(Choice) gd.getChoices().lastElement();
		pLocationPresets.setLocation(240, 30);
        
	        fontPropertiesContainer = new FontPropertiesPanel();
		fontPropertiesContainer.setBounds(left, 70, 400,
		subpanelHeight);
  		
  		addPanelsToDialog(locationFontContainer,
			new JPanel[] {pLocationX,
				pLocationY, pLocationPresets,
				fontPropertiesContainer} );

		/* adds preview checkbox - needs
		 * ExtendedPluginFilter and DialogListener!
		 */
		gd.addPreviewCheckbox(pfr);
		gd.addMessage("Series Labeler plugin for Fiji (is just ImageJ - batteries included)\n" +
				"maintained by Dan White MPI-CBG dan(at)chalkie.org.uk");

		/* needed for listening to dialog,
		 * field/button/checkbox changes
		 */
		gd.addDialogListener(this);

		/* add an ImageJ event listener to react to
		 * color changes, etc.
		 */
		IJ.addEventListener(new IJEventListener() {
			public void eventOccurred(int event) {
				if (event == IJEventListener.FOREGROUND_COLOR_CHANGED
						|| event == IJEventListener.BACKGROUND_COLOR_CHANGED) {
					updatePreview();
				}
			}
		});
		
		/* add a help button that opens a link to the
		 * documentation wiki page.
		 */
		gd.addHelp("http://pacific.mpi-cbg.de/wiki/index.php/Stack_labeler");

		if (!IJ.isMacro()){
			// update GUI parts that are dependent on current variable contents
			updateUI();
		}
		System.out.println(Macro.getOptions());
		gd.showDialog(); // shows the dialog GUI!

		// handle the plug-in cancel button being pressed.
		if (gd.wasCanceled())
			return DONE;

		/* if the ok button was pressed, we are really running the plug-in,
		 * so later we can tell what time stamp to make
		 * as its not the last as used by preview
		 */
		preview = !gd.wasOKed();

		/* extendedpluginfilter showDialog method should
		 * return a combination (bitwise OR) of the flags specified in
		 * interfaces PlugInFilter and ExtendedPlugInFilter.
		 */
		return flags;
	}

	/**
	 * This is part of the preview functionality. Informs the filter of the
	 * number of calls of run(ip) that will follow. The argument nPasses is
	 * worked out by the plug-in runner.
	 */
	public void setNPasses(int nPasses) {
		/* so the value of frame is reset to 1 each time the
		 * plugin is run or the preview checkbox is checked.
		 */
		if (preview) {
			frame = last;
		} else {
			frame = 1;	
		}
		frame--;
	}

	/**
	 * Run the plug-in on the ip object, which is the ImageProcessor object
	 * associated with the open/selected image. but remember that showDialog
	 * method is run before this in ExtendedPluginFilter
	 */
	public void run(ImageProcessor ip) {
		// this increments frame integer by 1.
		frame++;

		/* Updates this image from the pixel data in its associated
		 * ImageProcessor object and then displays it
		 * if it is the last frame. Why do we need this when there is
		 * ip.drawString(timeString); below?
		 */
		if (frame == last)
			imp.updateAndDraw();

		// tell the run method when to not do anything just return
		if ( (!preview)
			  && (canceled
				|| frame < first
				|| frame > last
				|| (frame % frameMask != 0) ) )
			return;

		ip.setFont(font);
		ip.setAntialiasedText(antiAliasedText);

		/* set the background according to label
		 * Rectangle
		 */
		Rectangle backgroundRectangle = getBoundingRectangle(ip);

		if (backgroundEnabled){
			ip.setColor(Toolbar.getBackgroundColor());
			ip.fill(new Roi(backgroundRectangle));
		}
		ip.setColor(Toolbar.getForegroundColor());
		ip.moveTo(backgroundRectangle.x,
			(backgroundRectangle.y + backgroundRectangle.height) );
		
		/* ask the getTimeFromFrame method to return the
		 * time for that frame
		 */
		double time = getTimeFromFrame(frame); 
		// draw the label string into the image
		ip.drawString(selectedFormat.getTimeString(time));
	}
	
	/**
	 * container panel from swing for gui items to be put in.
	 */
	private JPanel createContainerPanel(int height, String label){
		JPanel panel = new JPanel(null);
		panel.setPreferredSize(new Dimension(540, height));
		panel.setBorder(javax.swing.BorderFactory.createTitledBorder(label));
		return panel;
	}
	
	/**
	 * drop down selection with a text label
	 */
	private JPanel createComboBoxPanel(String labelText, String[] values, int defaultIndex) {
		return createComboBoxPanel(labelText, values, defaultIndex, 100, 150);
	}
	
	/**
	 * generate gd awt drop down selections for use in a
	 * different container later
	 */
	private JPanel createComboBoxPanel(String labelText, String[] values, int defaultIndex, int labelWidth, int comboboxWidth) {
		gd.addChoice(labelText, values, values[defaultIndex]);
		// get the previously added choice
		Choice choice = (Choice) gd.getChoices().lastElement();
		// get the label belonging to it
		Label label = (Label) gd.getComponent(gd.getComponentCount()-2);
		/* remove components from dialog, since we use
		 * elsewhere, but be still registered with GerericDialog
		 */
		gd.remove(choice);
		gd.remove(label);
		
		JPanel panel = new JPanel(null);
        label.setBounds(0, 0, labelWidth, 20);
        choice.setBounds(labelWidth, 0, comboboxWidth, 22);
        panel.add(label);
        panel.add(choice);
        panel.setSize(labelWidth+comboboxWidth, 30);
        return panel;
	}
	
	/**
	 * This is a text field panel for use in the GUI
	 */
	private JPanel createTextFieldPanel(String labelText, String defaultText) {
		return createTextFieldPanel(labelText, defaultText, 100, 100);
	}
	
	/* Generates gd awt string fields for use in a
	 * different container later
	 */
	private JPanel createTextFieldPanel(String labelText,
			String defaultText, int labelWidth, int textFieldWidth) {
		// add the text field
		gd.addStringField(labelText, defaultText);
		// get the previously added text field object
		TextField textField = (TextField)gd.getStringFields().lastElement();
		// get the label belonging to it
		Label label = (Label) gd.getComponent(gd.getComponentCount()-2);
		/* remove components from dialog, since we use
		 * elsewhere, but be still registered with GerericDialog
		 */
		gd.remove(textField);
		gd.remove(label);
		
		JPanel panel = new JPanel(null);
		label.setBounds(0, 0, labelWidth, 20);
		textField.setBounds(labelWidth,0,textFieldWidth, 22);
		panel.add(label);
		panel.add(textField);
		panel.setSize(labelWidth+textFieldWidth, 30);
		return panel;
	}
	
	/**
	 * A numeric field for the GUI
	 */
	private JPanel createNumericFieldPanel(String labelText, double defaultValue, int digits) {
		return createNumericFieldPanel(labelText, defaultValue, digits, 100, 100); 
	}
	
	/* Generates gd awt numeric fields for use in a
	 * different container later
	 */
	private JPanel createNumericFieldPanel(String labelText, double defaultValue, int digits, int labelWidth, int textFieldWidth) {
		// add the numeric field
		gd.addNumericField(labelText, defaultValue, digits);
		// get the previously added numeric field object
		TextField textField = (TextField)gd.getNumericFields().lastElement();
		// get the label belonging to it
		Label label = (Label) gd.getComponent(gd.getComponentCount()-2);
		// remove components from dialog, since we use elsewhere, but be still registered with GerericDialog
		gd.remove(textField);
		gd.remove(label);
		
		JPanel panel = new JPanel(null);
		label.setBounds(0, 0, labelWidth, 20);
		textField.setBounds(labelWidth,0,textFieldWidth, 22);
		panel.add(label);
		panel.add(textField);
		panel.setSize(labelWidth+textFieldWidth, 30);
		return panel;
	}
	
	/**
	 * Adds different Swing JPanels to a container JPanel.
	 * Moreover the container JPanel is encapsulated in a
	 * AWT Panel which in turn is added to the generic
	 * dialog.
	 */
	private void addPanelsToDialog(JPanel container, JPanel[] panels) {
		for (JPanel p : panels) {
			container.add(p);
		}
		
		Panel awtPanel = new Panel();
		awtPanel.add(container);
		gd.addPanel(awtPanel, GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));
	}

	/**
	 * Updates the preview.
	 * 
	 * @param dialog     are these params still right?
	 * @param e
	 */
	private void updatePreview() {
		if (gd != null){
			/* tell the plug-in filter runner to update
			 * the preview. Apparently, this is "OK"
			 */
			pluginFilterRunner.dialogItemChanged(gd, null);
		}
	}
	
	/**
	 * Shows and Hides relevant GUI components,
	 * based on the current state of the selected label format.
	 */
	private void updateUI() {
		/* if the new format supports custom suffixes,
		 * enable the custom suffix panel and deactivate
		 * unit selection
		 */
		boolean supportsCustomSuffix = selectedFormat.supportsCustomSuffix();
		customSuffixPanel.setVisible(supportsCustomSuffix);
		labelUnitsPanel.setVisible(!supportsCustomSuffix);
		/* if the current format supports custom format,
		 * enable the custom format panel
		 */
		customLabelFormatPanel.setVisible(selectedFormat.supportsCustomFormat());
		/* if the current format supports decimal places,
		 * enable the decimal places panel
		 */
		decimalPlacesPanel.setVisible(selectedFormat.supportsDecimalPlaces());
	}
	
	/**
	 * This method deals with changes the user makes in the GUI
	 */
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// the stack type choice
		int stackType = gd.getNextChoiceIndex();
		// has the label format been changed?
		int currentFormat = gd.getNextChoiceIndex();
		AbstractStampFormat lf = formats[currentFormat];
		if (lf != selectedFormat) {
			selectedFormat = lf;
			/* if format changed, must modify units
			 * choice accordingly
			 */
			labelUnitsComboBox.removeAll();
			for (String unit : selectedFormat.getAllowedFormatUnits()) {
				labelUnitsComboBox.addItem(unit);
			}
		}
		customSuffix = gd.getNextString();
		customFormat = gd.getNextString();

		// get the selected suffix of drop-down list
		chosenSuffix = gd.getNextChoice();

		decimalPlaces = (int) gd.getNextNumber();
		
		start = gd.getNextNumber();
		interval = gd.getNextNumber();
		frameMask = (int) gd.getNextNumber();
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		
		// has a different location preset has been selected?
		int preset = gd.getNextChoiceIndex();
		if (preset != locationPreset){
			locationPreset = preset;
			Point p = getPresetPosition(preset);

			locationXTextField.setText(Integer.toString(p.x));
			locationYTextField.setText(Integer.toString(p.y));
		}
		
		try {
			int curX = (int) gd.getNextNumber();
			if (curX != x) {
				x = curX;
				locationPresetsComboBox.select(CUSTOM);
			}
		}
		catch (NumberFormatException ex) { return false; }
		
		try {
			int curY = (int) gd.getNextNumber();
			if (curY != y) {
				y = curY;
				locationPresetsComboBox.select(CUSTOM);
			}
		}
		catch (NumberFormatException ex) { return false; }
		

		updateUI();

		return true; // or else the dialog will have the ok button deactivated!
	}
	
	/**
	 * Creates a string array out of the names of the available formats.
	 */
	private String[] getAvailableFormats() {
		String[] formatArray = new String[formats.length];
		int i = 0;
		for (AbstractStampFormat t : formats) {
			formatArray[i] = t.getName();
			i++;
		}
		return formatArray;
	}
	
	/**
	 * Returns true if a custom ROI has been selected, i.e if the current
	 * ROI does not have the extent of the whole image.
	 * @return true if custom ROI selected, false otherwise
	 */
	boolean isCustomROI(){
		Roi roi = imp.getRoi();
		if (roi == null)
			return false;
		
		Rectangle theROI = roi.getBounds();
		
		// if the ROI is the same size as the image (default ROI), return false
		return (theROI.height != imp.getHeight()
					|| theROI.width != imp.getWidth());
	}

	/**
	 * Works out the size of the font to use,
	 * from the size of the ROI box drawn so it fits in the ROI.
	 * If no ROI is set by the user, 
	 * x and y are defaulted or set according to text fields in GUI
	 */
	void setFontParams() {

		// the default font size
		int size = 12;
		
		if (isCustomROI()){
			Roi roi = imp.getRoi();
			Rectangle theROI = roi.getBounds();
			
			/**
			 * Remember the ROIs top left coordinates to be the X and Y values
			 * of the font rectangle
			 */
			x = theROI.x;
			y = theROI.y;

			/**
			 * size as the image, leave size as it was set by the gui.
			 * For now, ignore point to pixel conversion.
			 */
			size = (int) (theROI.height);

			/**
			 * make sure the font is not too big or small
			 * ImageJ font dialog smallest size is 8
			 * and largest is 72. Should stick to those limits. 
			 */
			if (size < 8)
				size = 8;
			else if (size > 72)
				size = 72;
		}
		
		font = new Font(TextRoi.getFont(), TextRoi.getStyle(), size);
	}

	/**
	 * Gets a valid bounding rectangle for the label positioning.
	 * It aims for having all of the time stamp on the image, even for
	 * the last frames/slices with the biggest numbers,
	 * and if the ROI is large in y so font size is also big
	 * It tries to move the time stamp right a bit to account for the
	 * max length the time stamp will be, and checks if the
	 * label will fall off the image and if it does move it in appropriately.
	 */
	Rectangle getBoundingRectangle(ImageProcessor ip) {
		Rectangle roi = ip.getRoi();
		/* set the xy time stamp drawing position, for ROI smaller than the
		 * image, to top left of ROI and make its height be the font size
		 */
		if (roi.width == imp.getWidth() && roi.height == imp.getHeight()) {
			roi.x = x;
			roi.y = y;
			roi.height = font.getSize();
		}
		
		/* make sure the y position is not less than the font height: size,
		 * so the time stamp is not off the top of the image?
		 */
		if ( (roi.y + roi.height) < font.getSize()) {
			y = roi.y = 1;
		} 
		
		/* make sure the stamp does not fall off the bottom of the image
		 */
		if (ip.getHeight() < (roi.y + font.getSize())) {
			y = roi.y = ip.getHeight() - font.getSize();
		}
		
		/* assume that the last time stamp is the widest one to calculate
		 * the maximum width of the bounding rectangle. This is not always
		 * true (e.g. 0:00 vs. 0:11) and could be made more precise.
		 */
		roi.width = ip.getStringWidth(selectedFormat.lastTimeStampString());

		/* if longest time stamp is wider than (image width - ROI width),
		 * move x in appropriately
		 */
		if (roi.width > (ip.getWidth() - roi.x)) {
			x = roi.x = (ip.getWidth() - roi.width);
		}
		
		return roi;
	}
	
	/**
	 * Work out where in the image the preset positions are.
	 */
	Point getPresetPosition(int preset) {
		ImageProcessor ip = imp.getProcessor();
		if (preset == UPPER_LEFT){
			x = 0;
			y = 0;
		}
		else if (preset == UPPER_RIGHT){
			x = ip.getWidth();
			y = 0;
		}
		else if (preset == LOWER_LEFT){
			x = 0;
			y = ip.getHeight();
		}
		else if (preset == LOWER_RIGHT){
			x = ip.getWidth();
			y = ip.getHeight();
		}
		else return new Point(x, y);
		
		Rectangle rect = getBoundingRectangle(ip);
		
		return new Point(rect.x, rect.y);
	}

	/**
	 * Returns the time of the last frame where a stamp will be made.
	 */
	double lastTime() { //should remane it so no time, as can be z or whatever series. 
		return getTimeFromFrame(last); 
	}

	/**
	 * Returns the time of a specific frame.
	 * 
	 * @param f The frame to calculate the time of
	 * @return The time at frame f
	 */
	double getTimeFromFrame(int f) {  //should remane it so no time, as can be z or whatever series. 
		return start + (interval * (f - 1));
	}

	/**
	 * A class representing a supported label format 
	 * It relates supported format units/suffixes to a format
	 * name. Besides that it determines if a custom suffix
	 * should be avaialble to the user.
	 */
	protected abstract class AbstractStampFormat {
		// an array of all the supported units for this format
		String[] allowedFormatUnits;
		// the display name for this format
		String name;
		// a member indicating if a custom suffix should be supported
		boolean customSuffixSupported;
		// a member indicating if a custom format should be supported
		boolean customFormatSupported;
		// a member indicating if decimal places modification is supported
		boolean decimalPlacesSupported;
		
		/**
		 * 
		 * @param allowedFormatUnits
		 * @param name
		 * @param supportCustomSuffix
		 * @param supportCustomFormat
		 */
		public AbstractStampFormat(String[] allowedFormatUnits, String name,
				boolean supportCustomSuffix, boolean supportCustomFormat,
				boolean supportDecimalPlaces) {
			this.allowedFormatUnits = allowedFormatUnits;
			this.name = name;
			this.customSuffixSupported = supportCustomSuffix;
			this.customFormatSupported = supportCustomFormat;
			this.decimalPlacesSupported = supportDecimalPlaces;
		}

		/**
		 * Here we make the strings to print into the images. decide if the
		 * format is digital or decimal according to the plugin GUI input if it
		 * is decimal (not digital) then need to set suffix from drop down list
		 * which might be custom suffix if one is entered and selected. if it is
		 * digital, then there is no suffix as format is set default as hh:mm:ss.ms
		 * 
		 * @param time
		 * @return
		 */
		public abstract String getTimeString(double time); //rename to remove time

		/**
		 * this method returns the string of the label for the last frame to
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
		public String lastTimeStampString() { //rename to remove time, use label instead
			return getTimeString(lastTime());
		}

		/**
		 * Gets the suffix (if any) that should be appended to the label.
		 * 
		 * @return The suffix to display.
		 */
		public String suffix() {
			return chosenSuffix;
		}

		/**
		 * Gets an array of allowed units for the format. 
		 * eg. to display them in a drop down choice list.
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
		 * Indicates whether custom (user input) suffixes are allowed for this
		 * format or not.
		 * 
		 * @return True if custom suffixes are allowed, false otherwise
		 */
		public boolean supportsCustomSuffix() {
			return customSuffixSupported;
		}

		/**
		 * Indicates whether a custom time format can be used by the format.
		 * This could be useful for digital formats like HH:mm which should be
		 * user definable.
		 * 
		 * @return True if the format supports custom formats, false otherwise.
		 */
		public boolean supportsCustomFormat() {
			return customFormatSupported;
		}
		
		/**
		 * Indicates whether decimal places are part of the format.
		 * This could be useful for decimal formats for which the number of
		 * decimal places should be user definable.
		 * 
		 * @return True if the format supports decimal places, false otherwise.
		 */
		public boolean supportsDecimalPlaces() {
			return decimalPlacesSupported;
		}
	}

	/**
	 * Represents a decimal label format, e. g. 7.3 days.
	 */
	protected class DecimalLabelFormat extends AbstractStampFormat {

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
					supportCustomFormat, true);
		}

		/**
		 * Makes the string containing the number for the label stamp, with
		 * specified decimal places format is decimal number with specified no.
		 * of digits after the point
		 * If specified no. of decimal places is 0
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
	protected class DigitalLabelFormat extends AbstractStampFormat {
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
			super(allowedFormatUnits, name, false, true, false);
		}

		/**
		 * Makes the string containing the number for the label stamp, with
		 * hh:mm:ss.decimalPlaces format which is nice, 
		 * and we do have that functionality in HybridDateFormat?
		 */
		@Override
		public String getTimeString(double time) {
			calendar.setTimeInMillis(0);
			if (chosenSuffix.equals("min")) {
				time = time * (60.0 * 1000.0);
			} else if (chosenSuffix.equals("s")) {
				time = time * 1000.0;
			} else if (chosenSuffix.equals("ms")) {
				// trivial case, nothing to do:
				// time = time;
			}
			else {
				IJ.error("For a digital 00:00:00.000 time you must use min, s or ms only as the time units.");
			}

			// set the time
			calendar.setTimeInMillis(Math.round(time));
			DateFormat f;
			try {
				// check if a custom format has been entered in the GUI
				if (customFormat.length() > 0) {
					// Make sure that our custom format can be handled
					f = new HybridDateFormat(customFormat);
				} else {
					f = new SimpleDateFormat("HH:mm:ss.SSS");
				}
				// the SimpleDateFormat needs to know thetime zone is UTC!
				f.setTimeZone(tz);

				return f.format(calendar.getTime());
			} catch (IllegalArgumentException ex) {
				return ex.getMessage();
			}
		}
	}

	/**
	 * Represents a label format that is essentially the same as a decimal
	 * format, but allows custom suffixes.
	 */
	protected class CustomLabelFormat extends DecimalLabelFormat {

		/**
		 * Creates a new {@link CustomLabelFormat}. It allows only custom
		 * suffixes as units and it's display name is set to "Custom Format".
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
	 * Class for a panel containing several font manipulation items.
	 * It consists of a font style and colour buttons, 
	 * and also a check box for toggling drawing a background behind the label.
	 */
	@SuppressWarnings("serial")
	protected class FontPropertiesPanel extends JPanel{
		/**
		 * Creates a new {@link FontPropertiesPanel} containing the font setting
		 * and font colour buttons.
		 */
		public FontPropertiesPanel() {
			super(null);
			
			JButton fontStyleButton = new JButton("Font Settings");
			fontStyleButton.setBounds(0, 0, 120, 25);
			fontStyleButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					new ExtendedFonts();

				}
			});
			
			JButton fontColourButton = new JButton("Font Color");
			fontColourButton.setBounds(130, 0, 120, 25);
			fontColourButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					new ColorPicker();

				}
			});
			
			final JCheckBox drawBackground = new JCheckBox("Background");
			drawBackground.setBounds(260, 0, 120, 25);
			drawBackground.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					backgroundEnabled = drawBackground.isSelected();
					updatePreview();
				}
			});
			
			add(fontStyleButton);
			add(fontColourButton);
			add(drawBackground);
		}
	}
	
	/**
	 * This class extends Fonts so we can listen to the changes in the 
	 * imageJ fonts GUI that we reuse for font settings 
	 * and update the preview accordingly.
	 */
	@SuppressWarnings("serial")
	protected class ExtendedFonts extends Fonts{
		@Override
		public void itemStateChanged(ItemEvent e) {
			super.itemStateChanged(e);
			font = new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi.getSize());
			antiAliasedText = TextRoi.isAntialiased();
			updatePreview();
		}
		
	}
} // thats the end of Series_Labeler

