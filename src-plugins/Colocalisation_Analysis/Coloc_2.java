import gadgets.DataContainer;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import results.PDFWriter;
import results.ResultHandler;
import results.SingleWindowDisplay;
import results.Warning;
import algorithms.Algorithm;
import algorithms.AutoThresholdRegression;
import algorithms.CostesSignificanceTest;
import algorithms.Histogram2D;
import algorithms.InputCheck;
import algorithms.LiHistogram2D;
import algorithms.LiICQ;
import algorithms.MandersCorrelation;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;

/**
   Copyright 2010, 2011 Daniel J. White, Tom Kazimiers, Johannes Schindelin
   and the Fiji project. Fiji is just imageJ - batteries included.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see http://www.gnu.org/licenses/ .
 */

/**
 * A plugin which does analysis colocalisation on a pair of images.
 *
 * @param <T>
 */
public class Coloc_2<T extends RealType<T>> implements PlugIn {

	// Allowed types of ROI configuration
	protected enum RoiConfiguration {None, Img1, Img2, Mask};
	// the ROI configuration to use
	RoiConfiguration roiConfig = RoiConfiguration.Img1;
	// the ROI to use (null if none)
	Rectangle roi = null;
	// the mask corresponding to the ROI
	protected Image<T> mask = null;
	// default indices of image, mask and roi choices
	protected static int index1 = 0;
	protected static int index2 = 1;
	protected static int indexMask = 0;
	protected static int indexRoi = 0;

	// the images to work on
	Image<T> img1, img2;
	// the channels of the images to use
	int img1Channel = 1, img2Channel = 1;

	/* The different algorithms this plug-in provides.
	* If a reference is null it will not get run.
	*/
	PearsonsCorrelation<T> pearsonsCorrelation = null;
	LiHistogram2D<T> liHistogramCh1 = null;
	LiHistogram2D<T> liHistogramCh2 = null;
	LiICQ<T> liICQ = null;
	MandersCorrelation<T> mandersCorrelation = null;
	Histogram2D<T> histogram2D = null;
	CostesSignificanceTest<T> costesSignificance = null;

	/* GUI related members */
	String[] roiLabels =  { "None","Channel 1", "Channel 2",};

	public void run(String arg0) {
		if (showDialog()) {
			colocalise(img1, img2);
		}
	}

	public boolean showDialog() {
		// get IDs of open windows
		int[] windowList = WindowManager.getIDList();
		// if theer are no windows open, cancel
		if (windowList == null) {
			IJ.noImage();
			return false;
		}
		/* create a new generic dialog for the
		 * display of various options.
		 */
		final GenericDialog gd
			= new GenericDialog("Coloc 2");

		String[] titles = new String[windowList.length];
		/* the masks and rois array needs three more entries than
		 * windows to contain "none", "roi ch 1" and "roi ch 2"
		 */
		String[] roisAndMasks= new String[windowList.length + 3];
		roisAndMasks[0]="<None>";
		roisAndMasks[1]="ROI in channel 1";
		roisAndMasks[2]="ROI in channel 2";

		// go through all open images and add them to GUI
		for (int i=0; i < windowList.length; i++) {
			ImagePlus imp = WindowManager.getImage(windowList[i]);
			if (imp != null) {
				titles[i] = imp.getTitle();
				roisAndMasks[i + 3] =imp.getTitle();
			} else {
				titles[i] = "";
			}
		}

		/* make sure the default indices are no bigger
		 * than the amount of images we have
		 */
		index1 = clip( index1, 0, titles.length );
		index2 = clip( index2, 0, titles.length );
		indexMask = clip( indexMask, 0, roisAndMasks.length);

		gd.addChoice("Channel_1", titles, titles[index1]);
		gd.addChoice("Channel_2", titles, titles[index2]);
		gd.addChoice("ROI or mask", roisAndMasks, roisAndMasks[indexMask]);
		//gd.addChoice("Use ROI", roiLabels, roiLabels[indexRoi]);

		// Add algorithm options
		gd.addMessage("Algorithms:");
		gd.addCheckbox("Li Histogram Channel 1", true);
		gd.addCheckbox("Li Histogram Channel 2", true);
		gd.addCheckbox("Li ICQ", true);
		gd.addCheckbox("Manders' Correlation", true);
		gd.addCheckbox("2D Instensity Histogram", true);
		gd.addCheckbox("Costes' Significance Test", true);

		// show the dialog, finally
		gd.showDialog();
		// do nothing if dialog has been canceled
		if (gd.wasCanceled())
			return false;

		ImagePlus imp1 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
		ImagePlus imp2 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
		// get information about the mask/roi to use
		indexMask = gd.getNextChoiceIndex();
		if (indexMask == 0)
			roiConfig = RoiConfiguration.None;
		else if (indexMask == 1)
			roiConfig = RoiConfiguration.Img1;
		else if (indexMask == 2)
			roiConfig = RoiConfiguration.Img2;
		else {
			roiConfig = RoiConfiguration.Mask;
			/* Make indexMask the reference to the mask image to use.
			 * To do this we reduce it by three for the first three
			 * entries in the combo box.
			 */
			indexMask = indexMask - 3;
		}

		// save the ImgLib wrapped images as members
		img1 = ImagePlusAdapter.wrap(imp1);
		img2 = ImagePlusAdapter.wrap(imp2);

		// configure ROIs and masks
		roi = null;
		mask = null;
		/* check if we have a valid ROI for the selected configuration
		 * and if so, get the ROI's bounds. Alternatively, a mask can
		 * be selected (that is basically all, but a rectangle).
		 */
		if (roiConfig == RoiConfiguration.Img1 && hasValidRoi(imp1)) {
			roi = imp1.getRoi().getBounds();
			ImageProcessor ip = imp1.getMask();
			// check if we got an irregular ROI
			if (ip != null) {
				ImagePlus maskImp = new ImagePlus("Mask", ip);
				mask = ImagePlusAdapter.wrap( maskImp );
			}
		} else if (roiConfig == RoiConfiguration.Img2 && hasValidRoi(imp2)) {
			roi = imp2.getRoi().getBounds();
			ImageProcessor ip = imp2.getMask();
			// check if we got an irregular ROI
			if (ip != null) {
				ImagePlus maskImp = new ImagePlus("Mask", ip);
				mask = ImagePlusAdapter.wrap( maskImp );
			}
		} else if (roiConfig == RoiConfiguration.Mask) {
			// get the  which image we should use as mask
			ImagePlus maskImp = WindowManager.getImage(indexMask);
			mask = ImagePlusAdapter.wrap( maskImp );
		}

		// Parse algorithm options
		pearsonsCorrelation = new PearsonsCorrelation<T>(PearsonsCorrelation.Implementation.Fast);
		if (gd.getNextBoolean())
				liHistogramCh1 = new LiHistogram2D<T>("Li - Ch1", true);
		if (gd.getNextBoolean())
				liHistogramCh2 = new LiHistogram2D<T>("Li - Ch2", false);
		if (gd.getNextBoolean())
				liICQ = new LiICQ<T>();
		if (gd.getNextBoolean())
				mandersCorrelation = new MandersCorrelation<T>();
		if (gd.getNextBoolean())
				histogram2D = new Histogram2D<T>("2D intensity histogram");
		if (gd.getNextBoolean()) {
				costesSignificance = new CostesSignificanceTest<T>(pearsonsCorrelation, 1, 10);
		}

		return true;
	}

	public void colocalise(Image<T> img1, Image<T> img2) {
		// create a new container for the selected images and channels
		DataContainer<T> container;
		if (mask != null) {
			// if we have a mask, a irregular ROI or a mask is in use
			container = new DataContainer<T>(img1, img2,
					img1Channel, img2Channel, mask);
		} else if (roi != null) {
			// if we have no musk, but a ROI, a regular ROI is in use
			int roiOffset[] = new int[] {roi.x, roi.y};
			int roiSize[] = new int[] {roi.width, roi.height};
			container = new DataContainer<T>(img1, img2,
					img1Channel, img2Channel, roiOffset, roiSize);
		} else {
			// no mask and no ROI is present
			container = new DataContainer<T>(img1, img2,
					img1Channel, img2Channel);
		}

		// create a results handler
		List<ResultHandler<T>> listOfResultHandlers = new ArrayList<ResultHandler<T>>();
		listOfResultHandlers.add(new SingleWindowDisplay<T>(container));
		listOfResultHandlers.add(new PDFWriter<T>(container));
		//ResultHandler<T> resultHandler = new EasyDisplay<T>(container);

		// this list contains the algorithms that will be run when the user clicks ok
		List<Algorithm<T>> userSelectedJobs = new ArrayList<Algorithm<T>>();

		// add some pre-processing jobs:
		userSelectedJobs.add( container.setInputCheck(
			new InputCheck<T>()) );
		userSelectedJobs.add( container.setAutoThreshold(
			new AutoThresholdRegression<T>(pearsonsCorrelation)) );

		// add user selected algorithms
		addIfValid(pearsonsCorrelation, userSelectedJobs);
		addIfValid(liHistogramCh1, userSelectedJobs);
		addIfValid(liHistogramCh2, userSelectedJobs);
		addIfValid(liICQ, userSelectedJobs);
		addIfValid(mandersCorrelation, userSelectedJobs);
		addIfValid(histogram2D, userSelectedJobs);
		addIfValid(costesSignificance, userSelectedJobs);

		// execute all algorithms
		for (Algorithm<T> a : userSelectedJobs){
			try {
				a.execute(container);
			}
			catch (MissingPreconditionException e){
				String aName = a.getClass().getName();
				System.out.println("MissingPreconditionException occured in " + aName + " algorithm: " + e.getMessage());
				for (ResultHandler<T> r : listOfResultHandlers){
					r.handleWarning(
							new Warning( "Probem with input data", aName + " - " + e.getMessage() ) );
				}
			}
		}

		// let the algorithms feed their results to the handler
		for (Algorithm<T> a : userSelectedJobs){
			for (ResultHandler<T> r : listOfResultHandlers)
				a.processResults(r);
		}
		// if we have ROIs/masks, add them to results
		if (mask != null || roi != null) {
			Image<T> mask1 = createMaskImage( container.getSourceImage1(), "Channel 1" );
			Image<T> mask2 = createMaskImage( container.getSourceImage2(), "Channel 2" );
			for (ResultHandler<T> r : listOfResultHandlers) {
				r.handleImage (mask1);
				r.handleImage (mask2);
			}
		}
		// do the actual results processing
		for (ResultHandler<T> r : listOfResultHandlers)
			r.process();
    }

	/**
	* Adds the provided Algorithm to the list if it is not null.
	*/
	protected void addIfValid(Algorithm<T> a, List<Algorithm<T>> list) {
		if (a != null)
			list.add(a);
	}

	/**
	 * Returns true if a custom ROI has been selected, i.e if the current
	 * ROI does not have the extent of the whole image.
	 * @return true if custom ROI selected, false otherwise
	 */
	protected boolean hasValidRoi(ImagePlus imp) {
		Roi roi = imp.getRoi();
		if (roi == null)
			return false;

		Rectangle theROI = roi.getBounds();

		// if the ROI is the same size as the image (default ROI), return false
		return (theROI.height != imp.getHeight()
					|| theROI.width != imp.getWidth());
	}

	/**
	* Clips a value to the specified bounds.
	*/
	protected static int clip(int val, int min, int max) {
		return Math.max( Math.min( val, max ), min );
	}

	/**
	 * This method duplicates the given images, but respects
	 * ROIs if present. Meaning, a subpicture will be created when
	 * source images are ROI/MaskImages.
	 */
	protected Image<T> createMaskImage(Image<T> sourceImage, String name) {
		LocalizableCursor<T> cursor = sourceImage.createLocalizableCursor();
		ImageFactory<T> maskFactory = new ImageFactory<T>(sourceImage.createType(), new ArrayContainerFactory());
		Image<T> maskImage = maskFactory.createImage( sourceImage.getDimensions(), name );
		LocalizableByDimCursor<T> maskCursor = maskImage.createLocalizableByDimCursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			maskCursor.setPosition( cursor );

			maskCursor.getType().set( cursor.getType() );
		}

		cursor.close();
		maskCursor.close();

		return maskImage;
	}
}
