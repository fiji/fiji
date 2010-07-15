import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.ImageJ;

/**
 * This class displays the container contents in one single window
 * and offers features like the use of different LUTs.
 *
 */
public class SingleWindowDisplay extends ImageWindow implements Display, ItemListener, ActionListener {
	static final int WIN_WIDTH = 300;
	static final int WIN_HEIGHT = 240;
	static final int HIST_WIDTH = 256;
	static final int HIST_HEIGHT = 128;
	static final int BAR_HEIGHT = 12;
	static final int XMARGIN = 20;
	static final int YMARGIN = 10;

	protected Rectangle frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
	protected List<Result.ImageResult> listOfImageResults = new ArrayList<Result.ImageResult>();
	protected List<Result.Histogram2DResult> listOfHistograms = new ArrayList<Result.Histogram2DResult>();

	// this is the image result that is currently selected by the drop down menu
	protected Result.ImageResult currentlyDisplayedImageResult;

	//make a cursor so we can get pixel values from the image
	protected LocalizableByDimCursor<FloatType> pixelAccessCursor;

	// GUI elements
	JButton listButton, copyButton;
	JCheckBox log;

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

		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));

		listButton = new JButton("List");
		//listButton.addActionListener(this);
		buttons.add(listButton);

		copyButton = new JButton("Copy");
		//copyButton.addActionListener(this);
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
		add(buttons);
		pack();
    }

	public void display(DataContainer container) {
		Iterator<Result> iterator = container.iterator();
		while (iterator.hasNext()){
			Result r = iterator.next();
			if (r instanceof Result.SimpleValueResult){
				Result.SimpleValueResult result = (Result.SimpleValueResult)r;
			} else if ( r instanceof Result.ImageResult) {
				Result.ImageResult result = (Result.ImageResult)r;
				listOfImageResults.add(result);

				// if it is a histogram remember that as well
				if ( r instanceof Result.Histogram2DResult) {
					Result.Histogram2DResult histogram = (Result.Histogram2DResult)r;
					listOfHistograms.add(histogram);
				}
			}
		}

		setup();
		if (listOfImageResults.size() > 0) {
			adjustDisplayedImage(listOfImageResults.get(0));
		}

		this.show();
	}

	public void mouseMoved( final int x, final int y) {
	ImageJ ij = IJ.getInstance();
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
					pixelAccessCursor.setPosition(0, x);
					pixelAccessCursor.setPosition(1, y);

					// get current value at position
					float val = pixelAccessCursor.getType().getRealFloat();
				}

				double calibratedXBinBottom = histogram.getHistXMin() + x / histogram.getXBinWidth();
				double calibratedXBinTop = histogram.getHistXMin() + (x + 1) / histogram.getXBinWidth();

				double calibratedYBinBottom = histogram.getHistYMin() + y / histogram.getYBinWidth();
				double calibratedYBinTop = histogram.getHistYMin() + (y + 1) / histogram.getYBinWidth();

				IJ.showStatus("x = " + IJ.d2s(calibratedXBinBottom) + " to " + IJ.d2s(calibratedXBinTop) +
						", y = " + IJ.d2s(calibratedYBinBottom) + " to " + IJ.d2s(calibratedYBinTop) + ", value = " + IJ.d2s(val) );
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
		double max = ImageStatistics.getImageMax(result.getData());
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
}
