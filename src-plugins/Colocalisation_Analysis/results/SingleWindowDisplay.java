package results;

import algorithms.AutoThresholdRegression;
import algorithms.Histogram2D;

import fiji.util.gui.JImagePanel;

import gadgets.DataContainer;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Overlay;

import ij.process.ImageProcessor;

import ij.text.TextWindow;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import mpicbg.imglib.algorithm.math.ImageStatistics;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;

import mpicbg.imglib.image.display.imagej.ImageJFunctions;

import mpicbg.imglib.type.numeric.RealType;

import mpicbg.imglib.type.numeric.integer.LongType;

/**
 * This class displays the container contents in one single window
 * and offers features like the use of different LUTs.
 *
 */
public class SingleWindowDisplay<T extends RealType<T>> extends JFrame implements ResultHandler<T>, ItemListener, ActionListener, ClipboardOwner {
	protected static final int WIN_WIDTH = 350;
	protected static final int WIN_HEIGHT = 240;

	// a static list for keeping track of all other SingleWindowDisplays
	protected static ArrayList<SingleWindowDisplay> displays = new ArrayList<SingleWindowDisplay>();

	// indicates if original images should be displayed or not
	protected boolean displayOriginalImages = false;

	// this is the image currently selected by the drop down menu
	protected Image<? extends RealType> currentlyDisplayedImageResult;

	// a list of the available result images, no matter what specific kinds
	protected List<Image<? extends RealType>> listOfImages
		= new ArrayList<Image<? extends RealType>>();
	protected Map<Image<LongType>, Histogram2D<T>> mapOf2DHistograms
	= new HashMap<Image<LongType>, Histogram2D<T>>();
	// a list of warnings
	protected List<Warning> warnings = new ArrayList<Warning>();
	// a list of named values, collected from algorithms
	protected List<ValueResult> valueResults = new ArrayList<ValueResult>();

	/* a map of images and corresponding LUTs. When an image is not in
	 * there no LUT should be applied.
	 */
	protected Map<Object, String> listOfLUTs = new HashMap<Object, String>();

	//make a cursor so we can get pixel values from the image
	protected LocalizableByDimCursor<? extends RealType> pixelAccessCursor;

	// A PDF writer to call if user wants PDF print
	protected PDFWriter<T> pdfWriter;

	// The current image
	protected ImagePlus imp;

	// GUI elements
	protected JImagePanel imagePanel;
	protected JButton listButton, copyButton;
	protected JCheckBox log;

	/* The data container with general information about
	 * source images
	 */
	protected DataContainer<T> dataContainer = null;

	public SingleWindowDisplay(DataContainer<T> container, PDFWriter<T> pdfWriter){
		super("Single Window Display");

		setPreferredSize(new Dimension(WIN_WIDTH, WIN_HEIGHT));

		// save a reference to the container
		dataContainer = container;
		this.pdfWriter = pdfWriter;
		// don't show ourself on instantiation
		this.setVisible(false);
		// add ourself to the list of single window displays
		displays.add(this);
	}

	public void setup() {

		JComboBox dropDownList = new JComboBox();
		for(Image<? extends RealType> img : listOfImages) {
			dropDownList.addItem(new NamedContainer<Image<? extends RealType> >(img, img.getName()));
		}
		dropDownList.addItemListener(this);

		imagePanel = new JImagePanel(ij.IJ.createImage("dummy", "8-bit", 10, 10, 1));


		// Create something to display it in
		final JEditorPane editor = new JEditorPane();
		editor.setEditable(false);				// we're browsing not editing
		editor.setContentType("text/html");		// must specify HTML text
		editor.setText(makeHtmlText());			// specify the text to display

		// Put the JEditorPane in a scrolling window and add it
		JScrollPane scrollPane = new JScrollPane(editor);
		scrollPane.setPreferredSize(new Dimension(256, 150));

		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		// add button for data display of histograms
		listButton = new JButton("List");
		listButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showList();
			}
		});
		buttons.add(listButton);
		// add button for data copy of histograms
		copyButton = new JButton("Copy");
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				copyToClipboard();
			}
		});
		buttons.add(copyButton);
		// add button for PDF printing
		JButton pdfButten = new JButton("PDF");
		pdfButten.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pdfWriter.process();
			}
		});
		buttons.add(pdfButten);

		/* We want the image to be log scale by default
		 * so the user can see something.
		 */
		log = new JCheckBox("Log");
		log.setSelected(true);
		log.addActionListener(this);
		buttons.add(log);

		final GridBagLayout layout = new GridBagLayout();
		final Container pane = getContentPane();
		getContentPane().setLayout(layout);
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.BOTH;
		c.gridy++;
		pane.add(dropDownList, c);
		c.gridy++;  c.weighty = 1;
		pane.add(imagePanel, c);
		c.gridy++; c.weighty = 1;
		pane.add(scrollPane, c);
		c.weighty = 0; c.gridy++;
		pane.add(buttons, c);
		pack();
    }

	public void process() {
		// if wanted, display source images
		if ( displayOriginalImages ) {
			listOfImages.add( dataContainer.getSourceImage1() );
			listOfImages.add( dataContainer.getSourceImage2() );
		}
		// create HTML table
		makeHtmlText();
		// set up the GUI
		setup();
		// display the first image available, if any
		if (listOfImages.size() > 0) {
			adjustDisplayedImage(listOfImages.get(0));
		}
		// show the GUI
		this.show();
	}

	public void handleImage(Image<T> image) {
		listOfImages.add( image );
	}

	public void handleHistogram(Histogram2D<T> histogram) {
		listOfImages.add(histogram.getPlotImage());
		mapOf2DHistograms.put(histogram.getPlotImage(), histogram);
		// link the histogram to a LUT
		listOfLUTs.put(histogram.getPlotImage(), "Fire");
	}

	public void handleWarning(Warning warning) {
		warnings.add( warning );
	}

	public void handleValue(String name, double value) {
		handleValue(name, value, 3);
	}

	public void handleValue(String name, double value, int decimals) {
		valueResults.add( new ValueResult(name, value, decimals));
	}

	/**
	 * Prints an HTML table entry onto the stream.
	 */
	protected void printTableRow(PrintWriter out, String name, String text) {
		out.print("<TR><TD>" + name + "</TD><TD>" + text + "</TD></TR");
	}

	/**
	 * Prints an HTML table entry onto the stream.
	 */
	protected void printTableRow(PrintWriter out, String name, double number) {
		printTableRow(out, name, number, 3);
	}

	/**
	 * Prints an HTML table entry onto the stream.
	 */
	protected void printTableRow(PrintWriter out, String name, double number, int decimalPlaces) {
		printTableRow(out, name, IJ.d2s(number, decimalPlaces));
	}

	/**
	 * This method creates CSS formatted HTML source out of the
	 * results stored in the member variables and adds some
	 * image statistics found in the data container.
	 * @return The HTML source to display
	 */
	protected String makeHtmlText() {

		StringWriter sout = new StringWriter();
		PrintWriter out = new PrintWriter( sout );

	    out.print("<html><head>");
	    // add some style information
	    out.print("<style type=\"text/css\">"
			+ "body {font-size: 9px; font-family: sans-serif;}"
			+ "h1 {color: black; font-weight: bold; font-size: 10px;}"
			+ "h1.warn {color: red;}"
			+ "h1.nowarn {color: green;}"
			+ "table {width: 175px;}"
			+ "td { border-width:1px; border-style: solid; vertical-align:top; overflow:hidden;}"
			+ "</style>");
	    out.print("</head>");

	    // print out warnings, if any
	    if ( warnings.size() > 0 ) {
		    out.print("<H1 class=\"warn\">Warnings</H1>");
		    // Print out the table
		    out.print("<TABLE class=\"warn\"><TR>");
		    out.print("<TH>Type</TH><TH>Message</TH></TR>");
		    for (Warning w : warnings) {
				printTableRow(out, w.getShortMessage(), w.getLongMessage());
		    }
		    out.println("</TABLE>");
	    } else {
		out.print("<H1 class=\"nowarn\">No warnings occured</H1>");
	    }

	    // print out simple value results
	    out.print("<H1>Results</H1>");
	    // Print out the table
	    out.print("<TABLE><TR>");
	    out.print("<TH>Name</TH><TH>Result</TH></TR>");

	    for ( ValueResult vr : valueResults) {
		    printTableRow(out, vr.name, vr.number, vr.decimals);
	    }

	    out.println("</TABLE>");

	    // print some image statistics
	    out.print("<H1>Image statistics</H1>");
	    out.print("<TABLE>");
	    printTableRow(out, "Min channel 1",  dataContainer.getMinCh1());
	    printTableRow(out, "Max channel 1",  dataContainer.getMaxCh1());
	    printTableRow(out, "Mean channel 1", dataContainer.getMeanCh1());

	    printTableRow(out, "Min channel 2", dataContainer.getMinCh2());
	    printTableRow(out, "Max channel 2", dataContainer.getMaxCh2());
	    printTableRow(out, "Mean channel 2", dataContainer.getMeanCh2());

	    out.println("</TABLE>");

	    out.print("</html>");
	    out.close();

	    // Get the string of HTML from the StringWriter and return it.
	    return sout.toString();
	}

	/**
	 * If the currently selected ImageResult is an HistrogramResult,
	 * a table of x-values, y-values and the counts.
	 */
	protected void showList() {
		/* check if we are dealing with an histogram result
		 * or a generic image result
		 */
		if (isHistogram(currentlyDisplayedImageResult)) {
			Histogram2D<T> hr = mapOf2DHistograms.get(currentlyDisplayedImageResult);
			double xBinWidth = 1.0 / hr.getXBinWidth();
			double yBinWidth = 1.0 / hr.getYBinWidth();
			// check if we have bins of size one or other ones
			boolean xBinWidthIsOne = Math.abs(xBinWidth - 1.0) < 0.00001;
			boolean yBinWidthIsOne = Math.abs(yBinWidth - 1.0) < 0.00001;
			// configure table headings accordingly
			String vHeadingX = xBinWidthIsOne ? "X value" : "X bin start";
			String vHeadingY = yBinWidthIsOne ? "Y value" : "Y bin start";
			// get the actual histogram data
			String histogramData = hr.getData();

			TextWindow tw = new TextWindow(getTitle(), vHeadingX + "\t" + vHeadingY + "\tcount", histogramData, 250, 400);
		}
	}

	/**
	 * If the currently selected ImageResult is an HistogramRestult,
	 * this method copies its data into to the clipboard.
	 */
	protected void copyToClipboard() {
		/* check if we are dealing with an histogram result
		 * or a generic image result
		 */
		if (isHistogram(currentlyDisplayedImageResult)) {
			/* try to get the system clipboard and return
			 * if we can't get it
			 */
			Clipboard systemClipboard = null;
			try {
				systemClipboard = getToolkit().getSystemClipboard();
			} catch (Exception e) {
				systemClipboard = null;
			}

			if (systemClipboard==null) {
				IJ.error("Unable to copy to Clipboard.");
				return;
			}
			// copy histogram values
			IJ.showStatus("Copying histogram values...");

			String text = mapOf2DHistograms.get(currentlyDisplayedImageResult).getData();
			StringSelection contents = new StringSelection( text );
			systemClipboard.setContents(contents, this);

			IJ.showStatus(text.length() + " characters copied to Clipboard");
		}
	}

	public void mouseMoved( final int x, final int y) {
	final ImageJ ij = IJ.getInstance();
	if (ij != null && currentlyDisplayedImageResult != null) {
		/* If Alt key is not pressed, display the calibrated data.
		 * If not, display image positions and data.
		 * Non log image intensity from original image or 2D histogram result is always shown in status bar,
		 * not the log intensity that might actually be displayed in the image.
		 */
		if (!IJ.altKeyDown()){

			// the alt key is not pressed use x and y values that are bin widths or calibrated intensities not the x y image coordinates.
			if (isHistogram(currentlyDisplayedImageResult)) {
				Histogram2D<T> histogram = mapOf2DHistograms.get(currentlyDisplayedImageResult);

				synchronized( pixelAccessCursor )
				{
					// set position of output cursor
					pixelAccessCursor.setPosition(x, 0);
					pixelAccessCursor.setPosition(y, 1);

					// get current value at position
					LocalizableByDimCursor<LongType> cursor = (LocalizableByDimCursor<LongType>)pixelAccessCursor;
					long val = cursor.getType().getIntegerLong();

					double calibratedXBinBottom = histogram.getXMin() + x / histogram.getXBinWidth();
					double calibratedXBinTop = histogram.getXMin() + (x + 1) / histogram.getXBinWidth();

					double calibratedYBinBottom = histogram.getYMin() + y / histogram.getYBinWidth();
					double calibratedYBinTop = histogram.getYMin() + (y + 1) / histogram.getYBinWidth();

					IJ.showStatus("x = " + IJ.d2s(calibratedXBinBottom) + " to " + IJ.d2s(calibratedXBinTop) +
							", y = " + IJ.d2s(calibratedYBinBottom) + " to " + IJ.d2s(calibratedYBinTop) + ", value = " + val );
				}
			} else {
				ImagePlus imp = ImageJFunctions.displayAsVirtualStack( currentlyDisplayedImageResult );
				imp.mouseMoved(x, y);
			}
		} else {
			// alt key is down, so show the image coordinates for x y in status bar.
			ImagePlus imp = ImageJFunctions.displayAsVirtualStack( currentlyDisplayedImageResult );
			imp.mouseMoved(x, y);
		}
	}
    }

	/**
	 * Draws the passed ImageResult on the ImagePlus of this class.
	 * If the image is part of a CompositeImageResult then contained
	 * lines will also be drawn
	 */
	protected void drawImage(Image<? extends RealType> img) {
		// get Imglib image as ImageJ image
		imp = ImageJFunctions.displayAsVirtualStack( img );
		imagePanel.updateImage(imp);
		// set the display range

		// check if a LUT should be applied
		if ( listOfLUTs.containsKey(img) ) {
			// select linked look up table
			IJ.run(imp, listOfLUTs.get(img), null);
		}
		imp.getProcessor().resetMinAndMax();

		boolean overlayModified = false;
		Overlay overlay = new Overlay();

		// if it is the 2d histogram, we want to show the regression line
		if (isHistogram(img)) {
			Histogram2D<T> histogram = mapOf2DHistograms.get(img);
			/* check if we should draw a regression line for the
			 * current histogram.
			 */
			if ( histogram.getDrawingSettings().contains(Histogram2D.DrawingFlags.RegressionLine) ) {
				AutoThresholdRegression<T> autoThreshold = dataContainer.getAutoThreshold();
				if (histogram != null && autoThreshold != null) {
					if (img == histogram.getPlotImage()) {
						drawLine(overlay, img,
								autoThreshold.getAutoThresholdSlope(),
								autoThreshold.getAutoThresholdIntercept());
						overlayModified = true;
					}
				}
			}
		}

		if (overlayModified) {
			overlay.setStrokeColor(java.awt.Color.WHITE);
			imp.setOverlay(overlay);
		}

		imagePanel.repaint();
	}

	/**
	 * Tests whether the given image is a histogram or not.
	 * @param img The image to test
	 * @return true if histogram, false otherwise
	 */
	protected boolean isHistogram(Image<? extends RealType> img) {
		return mapOf2DHistograms.containsKey(img);
	}

	/**
	 * Draws the line on the overlay.
	 */
	protected void drawLine(Overlay overlay, Image<? extends RealType> img, double slope, double intercept) {
		double startX, startY, endX, endY;
		int imgWidth = img.getDimension(0);
		int imgHeight = img.getDimension(1);
		/* since we want to draw the line over the whole image
		 * we can directly use screen coordinates for x values.
		 */
		startX = 0.0;
		endX = imgWidth;

		// check if we can get some exta information for drawing
		if (isHistogram(img)) {
			Histogram2D<T> histogram = mapOf2DHistograms.get(img);
			// get calibrated start y coordinates
			double calibratedStartY = slope * histogram.getXMin() + intercept;
			double calibratedEndY = slope * histogram.getXMax() + intercept;
			// convert calibrated coordinates to screen coordinates
			startY = calibratedStartY * histogram.getYBinWidth();
			endY = calibratedEndY * histogram.getYBinWidth();
		} else {
			startY = slope * startX + intercept;
			endY = slope * endX + intercept;
		}

		/* since the screen origin is in the top left
		 * of the image, we need to x-mirror our line
		 */
		 startY = ( imgHeight - 1 ) - startY;
		 endY = ( imgHeight - 1 ) - endY;
		// create the line ROI and add it to the overlay
		Line lineROI = new Line(startX, startY, endX, endY);
		/* Set drawing width of line to one, in case it has
		 * been changed globally.
		 */
		lineROI.setStrokeWidth(1.0f);
		overlay.add(lineROI);
	}

	protected void adjustDisplayedImage (Image<? extends RealType> img) {
		/* when changing the result image to display
		 * need to set the image we were looking at
		 * back to not log scale,
		 * so we don't log it twice if its reselected.
		 */
		if (log.isSelected())
			toggleLogarithmic(false);

		currentlyDisplayedImageResult = img;
		if (pixelAccessCursor != null){
			pixelAccessCursor.close();
		}
		pixelAccessCursor = img.createLocalizableByDimCursor();

		// Currently disabled, due to lag of non-histograms :-)
		// disable list and copy button if it is no histogram result
		listButton.setEnabled( isHistogram(img) );
		copyButton.setEnabled( isHistogram(img) );

		toggleLogarithmic(log.isSelected());
		drawImage(img);
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			Image<? extends RealType> img = ((NamedContainer<Image<? extends RealType> >)(e.getItem())).getObject();
			adjustDisplayedImage(img);
		}
	}

	protected void toggleLogarithmic(boolean enabled){
		if (imp == null)
			return;
		if (enabled) {
			imp.getProcessor().snapshot();
			imp.getProcessor().log();
			IJ.resetMinAndMax();
		}
		else
			imp.getProcessor().reset();
		imagePanel.repaint();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == log) {
			toggleLogarithmic(log.isSelected());
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// nothing to do here
	}
}
