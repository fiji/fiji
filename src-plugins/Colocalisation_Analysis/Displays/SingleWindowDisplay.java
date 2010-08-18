import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * This class displays the container contents in one single window
 * and offers features like the use of different LUTs.
 *
 */
public class SingleWindowDisplay extends ImageWindow implements Display, ItemListener, ActionListener, ClipboardOwner {
	static final int WIN_WIDTH = 350;
	static final int WIN_HEIGHT = 240;

	protected List<Result.ImageResult> listOfImageResults = new ArrayList<Result.ImageResult>();
	protected List<Result.Histogram2DResult> listOfHistograms = new ArrayList<Result.Histogram2DResult>();
	protected List<Result.WarningResult> listOfWarnings = new ArrayList<Result.WarningResult>();
	protected List<Result.SimpleValueResult> listOfSimpleValues = new ArrayList<Result.SimpleValueResult>();
	protected List<Result.CompositeImageResult> listOfImageComposites = new ArrayList<Result.CompositeImageResult>();

	// this is the image result that is currently selected by the drop down menu
	protected Result.ImageResult currentlyDisplayedImageResult;

	//make a cursor so we can get pixel values from the image
	protected LocalizableByDimCursor<FloatType> pixelAccessCursor;

	// GUI elements
	JButton listButton, copyButton;
	JCheckBox log;

	// during execution the data container is accessible
	DataContainer dataContainer = null;

	SingleWindowDisplay(){
		super(NewImage.createFloatImage("Single Window Display", WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
	}

	public void setup() {
		Panel imageSelectionPanel = new Panel();
		imageSelectionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JComboBox dropDownList = new JComboBox();
		for(Result.ImageResult r : listOfImageResults) {
			dropDownList.addItem(r);
		}
		dropDownList.addItemListener(this);
		imageSelectionPanel.add(dropDownList);

		Panel textPanel = new Panel();
		textPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		// Create something to display it in
	    final JEditorPane editor = new JEditorPane();
	    editor.setEditable(false);				// we're browsing not editing
	    editor.setContentType("text/html");		// must specify HTML text
	    editor.setText(makeHtmlText());			// specify the text to display

		// Put the JEditorPane in a scrolling window and add it
	    JScrollPane sp = new JScrollPane(editor);
	    sp.setPreferredSize(new Dimension(256, 150));
	    textPanel.add(sp);

		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));

		listButton = new JButton("List");
		listButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showList();
			}
		});
		buttons.add(listButton);

		copyButton = new JButton("Copy");
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				copyToClipboard();
			}
		});
		buttons.add(copyButton);

		/* We want the image to be log scale by default
		 * so the user can see something.
		 */
		log = new JCheckBox("Log");
		log.setSelected(true);
		log.addActionListener(this);
		buttons.add(log);

		remove(ic);
		add(imageSelectionPanel);
		add(ic);
	    add(textPanel);
		add(buttons);
		pack();
    }

	public void display(DataContainer container) {
		// save a reference to the container
		dataContainer = container;
		// parse the results
		parseResults( container.iterator() );
		// set up the GUI
		setup();
		// display the first image available, if any
		if (listOfImageResults.size() > 0) {
			adjustDisplayedImage(listOfImageResults.get(0));
		}

		this.show();
	}

	/**
	 * Iterates over results and passes the results to
	 * the parseResult method.
	 */
	protected void parseResults(Iterator<Result> iterator) {
		while (iterator.hasNext()){
			parseResult( iterator.next() );
		}
	}

	/**
	 * Adds the passed Result to the appropiate list.
	 */
	protected void parseResult(Result r) {
		if (r instanceof Result.SimpleValueResult){
			Result.SimpleValueResult result = (Result.SimpleValueResult)r;
			listOfSimpleValues.add(result);
		} else if ( r instanceof Result.ImageResult) {
			Result.ImageResult result = (Result.ImageResult)r;
			listOfImageResults.add(result);

			// if it is a histogram remember that as well
			if ( r instanceof Result.Histogram2DResult) {
				Result.Histogram2DResult histogram = (Result.Histogram2DResult)r;
				listOfHistograms.add(histogram);
			}
		} else if ( r instanceof Result.CompositeImageResult ) {
			Result.CompositeImageResult result = (Result.CompositeImageResult)r;
			listOfImageComposites.add( result );
			// parse the image data to put the image in the correct list
			parseResult( result.getImageResult() );
		} else if ( r instanceof Result.WarningResult ) {
			Result.WarningResult result = (Result.WarningResult)r;
			listOfWarnings.add(result);
		}
	}

	/**
	 * This method creates CSS formatted HTML source out of the
	 * results stored in the member variables and adds some
	 * image statistics found in the data container.
	 * @return The HTML source to display
	 */
	protected String makeHtmlText() {
		// Set up an output stream we can print the table to.
	    // This is easier than concatenating strings all the time.
	    StringWriter sout = new StringWriter();
	    PrintWriter out = new PrintWriter(sout);

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
	    if ( listOfWarnings.size() > 0 ) {
		    out.print("<H1 class=\"warn\">Warnings</H1>");
		    // Print out the table
		    out.print("<TABLE class=\"warn\"><TR>");
		    out.print("<TH>Type</TH><TH>Message</TH></TR>");
		    for (Result.WarningResult r : listOfWarnings) {
		      out.println("<TR><TD>" + r.getName() +
				  "</TD><TD>" + r.getMessage() +
				  "</TD></TR>");
		    }
		    out.println("</TABLE>");
	    } else {
		out.print("<H1 class=\"nowarn\">No warnings occured</H1>");
	    }

	    // print out simple value results, if anny
	    if ( listOfSimpleValues.size() > 0 ) {
		    out.print("<H1>Results</H1>");
		    // Print out the table
		    out.print("<TABLE><TR>");
		    out.print("<TH>Name</TH><TH>Result</TH></TR>");
		    for (Result.SimpleValueResult r : listOfSimpleValues) {
		      out.println("<TR><TD>" + r.getName() +
				  "</TD><TD>" + IJ.d2s(r.getValue(), r.getDecimalPlaces()) +
				  "</TD></TR>");
		    }
		    out.println("</TABLE>");
	    } else {
		out.print("<H1 class=\"warn\">No results generated</H1>");
	    }

	    // print some image statistics
	    out.print("<H1>Image statistics</H1>");
	    out.print("<TABLE>");
	    out.print("<TR><TD>Min channel 1</TD><TD>" + dataContainer.getMinCh1() + "</TD></TR");
	    out.print("<TR><TD>Max channel 1</TD><TD>" + dataContainer.getMaxCh1() + "</TD></TR");
	    out.print("<TR><TD>Mean channel 1</TD><TD>" + dataContainer.getMeanCh1() + "</TD></TR");
	    out.print("<TR><TD>Min threshold channel 1</TD><TD>" + dataContainer.getCh1MinThreshold() + "</TD></TR");
	    out.print("<TR><TD>Max threshold channel 1</TD><TD>" + dataContainer.getCh1MaxThreshold() + "</TD></TR");

	    out.print("<TR><TD>Min channel 2</TD><TD>" + dataContainer.getMinCh2() + "</TD></TR");
	    out.print("<TR><TD>Max channel 2</TD><TD>" + dataContainer.getMaxCh2() + "</TD></TR");
	    out.print("<TR><TD>Mean channel 2</TD><TD>" + dataContainer.getMeanCh2() + "</TD></TR");
	    out.print("<TR><TD>Min threshold channel 2</TD><TD>" + dataContainer.getCh2MinThreshold() + "</TD></TR");
	    out.print("<TR><TD>Max threshold channel 2</TD><TD>" + dataContainer.getCh2MaxThreshold() + "</TD></TR");
	    out.println("</TABLE>");

	    out.print("</html>");
	    out.close();

	    // Get the string of HTML from the StringWriter and return it.
	    return sout.toString();
	}

	/**
	 * If the currently selected ImageResult is an HistrogramResult,
	 * a table of x-values, y-values and the counts is generated and
	 * returned as a string. If the current ImageResult is no
	 * histogram, an empty string is returned.
	 */
	protected String getCurrentHistogramData() {
		StringBuffer sb = new StringBuffer();
		/* check if we are dealing with an histogram result
		 * or a generic image result
		 */
		if (listOfHistograms.contains(currentlyDisplayedImageResult)) {
			Result.Histogram2DResult hr = (Result.Histogram2DResult)currentlyDisplayedImageResult;
			double xBinWidth = 1.0 / hr.getXBinWidth();
			double yBinWidth = 1.0 / hr.getYBinWidth();
			double xMin = hr.getHistXMin();
			double yMin = hr.getHistYMin();
			// check if we have bins of size one or other ones
			boolean xBinWidthIsOne = Math.abs(xBinWidth - 1.0) < 0.00001;
			boolean yBinWidthIsOne = Math.abs(yBinWidth - 1.0) < 0.00001;
			// configure decimal places accordingly
			int xDecimalPlaces = xBinWidthIsOne ? 0 : 3;
			int yDecimalPlaces = yBinWidthIsOne ? 0 : 3;
			// create a cursor to access the histogram data
			LocalizableByDimCursor<FloatType> cursor = hr.getData().createLocalizableByDimCursor();;
			// loop over 2D histogram
			for (int i=0; i < hr.getData().getDimension(0); ++i) {
				for (int j=0; j < hr.getData().getDimension(1); ++j) {
					cursor.setPosition(i, 0);
					cursor.setPosition(j, 1);
					sb.append(
							ResultsTable.d2s(xMin + (i * xBinWidth), xDecimalPlaces) + "\t" +
							ResultsTable.d2s(yMin + (j * yBinWidth), yDecimalPlaces) + "\t" +
							ResultsTable.d2s(cursor.getType().getRealDouble(), 0) + "\n");
				}
			}
			cursor.close();
		}
		return sb.toString();
	}

	/**
	 * If the currently selected ImageResult is an HistrogramResult,
	 * a table of x-values, y-values and the counts.
	 */
	protected void showList() {
		/* check if we are dealing with an histogram result
		 * or a generic image result
		 */
		if (listOfHistograms.contains(currentlyDisplayedImageResult)) {
			Result.Histogram2DResult hr = (Result.Histogram2DResult)currentlyDisplayedImageResult;
			double xBinWidth = 1.0 / hr.getXBinWidth();
			double yBinWidth = 1.0 / hr.getYBinWidth();
			// check if we have bins of size one or other ones
			boolean xBinWidthIsOne = Math.abs(xBinWidth - 1.0) < 0.00001;
			boolean yBinWidthIsOne = Math.abs(yBinWidth - 1.0) < 0.00001;
			// configure table headings accordingly
			String vheadingX = xBinWidthIsOne ? "X value" : "X bin start";
			String vheadingY = yBinWidthIsOne ? "Y value" : "Y bin start";
			// get the actual histogram data
			String histogramData = getCurrentHistogramData();

			TextWindow tw = new TextWindow(getTitle(), vheadingX + "\t" + vheadingY + "\tcount", histogramData, 250, 400);
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
		if (listOfHistograms.contains(currentlyDisplayedImageResult)) {
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

			String text = getCurrentHistogramData();
			StringSelection contents = new StringSelection( text );
			systemClipboard.setContents(contents, this);

			IJ.showStatus(text.length() + " characters copied to Clipboard");
		}
	}

	public void mouseMoved( final int x, final int y) {
	final ImageJ ij = IJ.getInstance();
	if (ij != null) {
		/* If Alt key is not pressed, display the calibrated data.
		 * If not, display image positions and data.
		 * Non log image intensity from original image or 2D histogram result is always shown in status bar,
		 * not the log intensity that might actually be displayed in the image.
		 */
		if (!IJ.altKeyDown()){

			// the alt key is not pressed use x and y values that are bin widths or calibrated intensities not the x y image coordinates.
			if (currentlyDisplayedImageResult instanceof Result.Histogram2DResult) {
				Result.Histogram2DResult histogram = (Result.Histogram2DResult)currentlyDisplayedImageResult;

				synchronized( pixelAccessCursor )
				{
					// set position of output cursor
					pixelAccessCursor.setPosition(x, 0);
					pixelAccessCursor.setPosition(y, 1);

					// get current value at position
					float val = pixelAccessCursor.getType().getRealFloat();

					double calibratedXBinBottom = histogram.getHistXMin() + x / histogram.getXBinWidth();
					double calibratedXBinTop = histogram.getHistXMin() + (x + 1) / histogram.getXBinWidth();

					double calibratedYBinBottom = histogram.getHistYMin() + y / histogram.getYBinWidth();
					double calibratedYBinTop = histogram.getHistYMin() + (y + 1) / histogram.getYBinWidth();

					IJ.showStatus("x = " + IJ.d2s(calibratedXBinBottom) + " to " + IJ.d2s(calibratedXBinTop) +
							", y = " + IJ.d2s(calibratedYBinBottom) + " to " + IJ.d2s(calibratedYBinTop) + ", value = " + IJ.d2s(val) );
				}
			} else if (currentlyDisplayedImageResult instanceof Result.ImageResult) {
				ImagePlus imp = ImageJFunctions.displayAsVirtualStack( currentlyDisplayedImageResult.getData() );
				imp.mouseMoved(x, y);
			}
		} else {
			// alt key is down, so show the image coordinates for x y in status bar.
			ImagePlus imp = ImageJFunctions.displayAsVirtualStack( currentlyDisplayedImageResult.getData() );
			imp.mouseMoved(x, y);
		}
	}
    }

	protected void drawImageResult(Result.ImageResult result) {
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( result.getData() );
		this.imp.setProcessor(imp.getProcessor());
		ImageProcessor ip = this.imp.getProcessor();
		double max = ImageStatistics.<RealType>getImageMax(result.getData()).getRealDouble();
		this.imp.setDisplayRange(0.0, max);
		IJ.run(this.imp, "Fire", null);
		this.imp.updateAndDraw();
	}

	protected void adjustDisplayedImage (Result.ImageResult result) {
		/* when changing the result image to display
		 * need to set the image we were looking at
		 * back to not log scale,
		 * so we don't log it twice if its reselected.
		 */
		if (log.isSelected())
			toggleLogarithmic(false);

		currentlyDisplayedImageResult = result;
		pixelAccessCursor = result.getData().createLocalizableByDimCursor();

		// disable list and copy button if it is no histogram result
		listButton.setEnabled( listOfHistograms.contains(result) );
		copyButton.setEnabled( listOfHistograms.contains(result) );

		drawImageResult(result);
		toggleLogarithmic(log.isSelected());
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			// get current image result to view
			Result.ImageResult result = (Result.ImageResult)(e.getItem());
			adjustDisplayedImage(result);
		}
	}

	protected void toggleLogarithmic(boolean enabled){
		if (enabled){
			this.imp.getProcessor().snapshot();
			this.imp.getProcessor().log();
			IJ.resetMinAndMax();
		}
		else {
			this.imp.getProcessor().reset();
		}
		this.imp.updateAndDraw();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == log) {
			toggleLogarithmic(log.isSelected());
		}
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// nothing to do here
	}
}
