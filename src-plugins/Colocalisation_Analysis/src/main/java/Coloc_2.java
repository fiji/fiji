import gadgets.DataContainer;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.awt.Checkbox;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
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
import algorithms.MandersColocalization;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import algorithms.SpearmanRankCorrelation;

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
public class Coloc_2<T extends RealType< T > & NativeType< T >> implements PlugIn {

	// a small bounding box container
	protected class BoundingBox {
		public long[] offset;
		public long[] size;
		public BoundingBox(long [] offset, long[] size) {
			this.offset = offset.clone();
			this.size = size.clone();
		}
	}

	// a storage class for ROI information
	protected class MaskInfo {
		BoundingBox roi;
		public Img<T> mask;

		// constructors
		public MaskInfo(BoundingBox roi, Img<T> mask) {
			this.roi = roi;
			this.mask = mask;
		}

		public MaskInfo() { }
	}

	// the storage key for Fiji preferences
	protected final static String PREF_KEY = "Coloc_2.";

	// Allowed types of ROI configuration
	protected enum RoiConfiguration {
		None,
		Img1,
		Img2,
		Mask,
		RoiManager
	};

	// the ROI configuration to use
	protected RoiConfiguration roiConfig = RoiConfiguration.Img1;

	// A list of all ROIs/masks found
	protected ArrayList<MaskInfo> masks = new ArrayList<MaskInfo>();

	// default indices of image, mask and ROI choices
	protected static int index1 = 0;
	protected static int index2 = 1;
	protected static int indexMask = 0;
	protected static int indexRoi = 0;

	// the images to work on
	protected Img<T> img1, img2;

	// names of the images working on
	protected String Ch1Name = "";
	protected String Ch2Name = "";

	// the channels of the images to use
	protected int img1Channel = 1, img2Channel = 1;

	/* The different algorithms this plug-in provides.
	 * If a reference is null it will not get run.
	 */
	protected PearsonsCorrelation<T> pearsonsCorrelation;
	protected LiHistogram2D<T> liHistogramCh1;
	protected LiHistogram2D<T> liHistogramCh2;
	protected LiICQ<T> liICQ;
	protected SpearmanRankCorrelation<T> SpearmanRankCorrelation;
	protected MandersColocalization<T> mandersCorrelation;
	protected Histogram2D<T> histogram2D;
	protected CostesSignificanceTest<T> costesSignificance;
	// indicates if images should be printed in result
	protected boolean displayImages;

	// indicates if a PDF should be saved automatically
	protected boolean autoSavePdf;

	public void run(String arg0) {
		if (showDialog()) {
			try {
				for (MaskInfo mi : masks) {
					colocalise(img1, img2, mi.roi, mi.mask);
				}
			} catch (MissingPreconditionException e) {
				IJ.handleException(e);
				IJ.showMessage("An error occured, could not colocalize!");
				return;
			}
		}
	}

	public boolean showDialog() {
		// get IDs of open windows
		int[] windowList = WindowManager.getIDList();
		// if there are less than 2 windows open, cancel
		if (windowList == null || windowList.length < 2) {
			IJ.showMessage("At least 2 images must be open!");
			return false;
		}

		/* create a new generic dialog for the
		 * display of various options.
		 */
		final GenericDialog gd
			= new GenericDialog("Coloc 2");

		String[] titles = new String[windowList.length];
		/* the masks and ROIs array needs three more entries than
		 * windows to contain "none", "ROI ch 1" and "ROI ch 2"
		 */
		String[] roisAndMasks= new String[windowList.length + 4];
		roisAndMasks[0]="<None>";
		roisAndMasks[1]="ROI(s) in channel 1";
		roisAndMasks[2]="ROI(s) in channel 2";
		roisAndMasks[3]="ROI Manager";

		// go through all open images and add them to GUI
		for (int i=0; i < windowList.length; i++) {
			ImagePlus imp = WindowManager.getImage(windowList[i]);
			if (imp != null) {
				titles[i] = imp.getTitle();
				roisAndMasks[i + 4] =imp.getTitle();
			} else {
				titles[i] = "";
			}
		}

		// set up the users preferences
		displayImages = Prefs.get(PREF_KEY+"displayImages", false);
		autoSavePdf = Prefs.get(PREF_KEY+"autoSavePdf", true);
		boolean displayShuffledCostes = Prefs.get(PREF_KEY+"displayShuffledCostes", false);
		boolean useLiCh1 = Prefs.get(PREF_KEY+"useLiCh1", true);
		boolean useLiCh2 = Prefs.get(PREF_KEY+"useLiCh2", true);
		boolean useLiICQ = Prefs.get(PREF_KEY+"useLiICQ", true);
		boolean useSpearmanRank = Prefs.get(PREF_KEY+"useSpearmanRank", true);
		boolean useManders = Prefs.get(PREF_KEY+"useManders", true);
		boolean useScatterplot = Prefs.get(PREF_KEY+"useScatterplot", true);
		boolean useCostes = Prefs.get(PREF_KEY+"useCostes", true);
		int psf = (int) Prefs.get(PREF_KEY+"psf", 3);
		int nrCostesRandomisations = (int) Prefs.get(PREF_KEY+"nrCostesRandomisations", 10);

		/* make sure the default indices are no bigger
		 * than the amount of images we have
		 */
		index1 = clip( index1, 0, titles.length );
		index2 = clip( index2, 0, titles.length );
		indexMask = clip( indexMask, 0, roisAndMasks.length - 1);

		gd.addChoice("Channel_1", titles, titles[index1]);
		gd.addChoice("Channel_2", titles, titles[index2]);
		gd.addChoice("ROI_or_mask", roisAndMasks, roisAndMasks[indexMask]);
		//gd.addChoice("Use ROI", roiLabels, roiLabels[indexRoi]);

		gd.addCheckbox("Show_\"Save_PDF\"_Dialog", autoSavePdf);
		gd.addCheckbox("Display_Images_in_Result", displayImages);
		gd.addCheckbox("Display_Shuffled_Images", displayShuffledCostes);
		final Checkbox shuffleCb = (Checkbox) gd.getCheckboxes().lastElement();
		// Add algorithm options
		gd.addMessage("Algorithms:");
		gd.addCheckbox("Li_Histogram_Channel_1", useLiCh1);
		gd.addCheckbox("Li_Histogram_Channel_2", useLiCh2);
		gd.addCheckbox("Li_ICQ", useLiICQ);
		gd.addCheckbox("Spearman's_Rank_Correlation", useSpearmanRank);
		gd.addCheckbox("Manders'_Correlation", useManders);
		gd.addCheckbox("2D_Instensity_Histogram", useScatterplot);
		gd.addCheckbox("Costes'_Significance_Test", useCostes);
		final Checkbox costesCb = (Checkbox) gd.getCheckboxes().lastElement();
		gd.addNumericField("PSF", psf, 1);
		gd.addNumericField("Costes_randomisations", nrCostesRandomisations, 0);

		// disable shuffle checkbox if costes checkbox is set to "off"
		shuffleCb.setEnabled(useCostes);
		costesCb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				shuffleCb.setEnabled(costesCb.getState());
			}
		});

		// show the dialog, finally
		gd.showDialog();
		// do nothing if dialog has been canceled
		if (gd.wasCanceled())
			return false;

		ImagePlus imp1 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
		ImagePlus imp2 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);

		// get image names for output
		Ch1Name = imp1.getTitle();
		Ch2Name = imp2.getTitle();

		// make sure both images have the same bit-depth
		if (imp1.getBitDepth() != imp2.getBitDepth()) {
			IJ.showMessage("Both images must have the same bit-depth.");
			return false;
		}

		// get information about the mask/ROI to use
		indexMask = gd.getNextChoiceIndex();
		if (indexMask == 0)
			roiConfig = RoiConfiguration.None;
		else if (indexMask == 1)
			roiConfig = RoiConfiguration.Img1;
		else if (indexMask == 2)
			roiConfig = RoiConfiguration.Img2;
		else if (indexMask == 3)
			roiConfig = RoiConfiguration.RoiManager;
		else {
			roiConfig = RoiConfiguration.Mask;
			/* Make indexMask the reference to the mask image to use.
			 * To do this we reduce it by three for the first three
			 * entries in the combo box.
			 */
			indexMask = indexMask - 4;
		}

		// save the ImgLib wrapped images as members
		img1 = ImagePlusAdapter.wrap(imp1);
		img2 = ImagePlusAdapter.wrap(imp2);

		/* check if we have a valid ROI for the selected configuration
		 * and if so, get the ROI's bounds. Alternatively, a mask can
		 * be selected (that is basically all, but a rectangle).
		 */
		if (roiConfig == RoiConfiguration.Img1 && hasValidRoi(imp1)) {
			createMasksFromImage(imp1);
		} else if (roiConfig == RoiConfiguration.Img2 && hasValidRoi(imp2)) {
			createMasksFromImage(imp2);
		} else if (roiConfig == RoiConfiguration.RoiManager) {
			if (!createMasksFromRoiManager(imp1.getWidth(), imp1.getHeight()))
				return false;
		} else if (roiConfig == RoiConfiguration.Mask) {
			// get the image to be used as mask
			ImagePlus maskImp = WindowManager.getImage(windowList[indexMask]);
			Img<T> maskImg = ImagePlusAdapter.<T>wrap( maskImp );
			// get a valid mask info for the image
			MaskInfo mi = getBoundingBoxOfMask(maskImg);
			masks.add( mi ) ;
		} else {
			/* if no ROI/mask is selected, just add an empty MaskInfo
			 * to colocalise both images without constraints.
			 */
			masks.add(new MaskInfo(null, null));
		}

		// read out GUI data
		autoSavePdf = gd.getNextBoolean();
		displayImages = gd.getNextBoolean();
		displayShuffledCostes = gd.getNextBoolean();
		useLiCh1 = gd.getNextBoolean();
		useLiCh2 = gd.getNextBoolean();
		useLiICQ = gd.getNextBoolean();
		useSpearmanRank = gd.getNextBoolean();
		useManders = gd.getNextBoolean();
		useScatterplot = gd.getNextBoolean();
		useCostes = gd.getNextBoolean();
		psf = (int) gd.getNextNumber();
		nrCostesRandomisations = (int) gd.getNextNumber();

		// save user preferences
		Prefs.set(PREF_KEY+"autoSavePdf", autoSavePdf);
		Prefs.set(PREF_KEY+"displayImages", displayImages);
		Prefs.set(PREF_KEY+"displayShuffledCostes", displayShuffledCostes);
		Prefs.set(PREF_KEY+"useLiCh1", useLiCh1);
		Prefs.set(PREF_KEY+"useLiCh2", useLiCh2);
		Prefs.set(PREF_KEY+"useLiICQ", useLiICQ);
		Prefs.set(PREF_KEY+"useSpearmanRank", useSpearmanRank);
		Prefs.set(PREF_KEY+"useManders", useManders);
		Prefs.set(PREF_KEY+"useScatterplot", useScatterplot);
		Prefs.set(PREF_KEY+"useCostes", useCostes);
		Prefs.set(PREF_KEY+"psf", psf);
		Prefs.set(PREF_KEY+"nrCostesRandomisations", nrCostesRandomisations);

		// Parse algorithm options
		pearsonsCorrelation = new PearsonsCorrelation<T>(PearsonsCorrelation.Implementation.Fast);

		if (useLiCh1)
			liHistogramCh1 = new LiHistogram2D<T>("Li - Ch1", true);
		if (useLiCh2)
			liHistogramCh2 = new LiHistogram2D<T>("Li - Ch2", false);
		if (useLiICQ)
			liICQ = new LiICQ<T>();
		if (useSpearmanRank)
		    SpearmanRankCorrelation = new SpearmanRankCorrelation<T>();
		if (useManders)
			mandersCorrelation = new MandersColocalization<T>();
		if (useScatterplot)
			histogram2D = new Histogram2D<T>("2D intensity histogram");
		if (useCostes) {
			costesSignificance = new CostesSignificanceTest<T>(pearsonsCorrelation,
					psf, nrCostesRandomisations, displayShuffledCostes);
		}

		return true;
	}

	/**
	 * Call this method to run a whole colocalisation configuration,
	 * all selected algorithms get run on the supplied images. You
	 * can specify the data further by supplying appropriate
	 * information in the mask structure.
	 *
	 * @param img1
	 * @param img2
	 * @param roi
	 * @param mask
	 * @param maskBB
	 * @throws MissingPreconditionException
	 */
	public void colocalise(Img<T> img1, Img<T> img2, BoundingBox roi,
			Img<T> mask) throws MissingPreconditionException {
		// create a new container for the selected images and channels
		DataContainer<T> container;
		if (mask != null) {
			container = new DataContainer<T>(img1, img2,
					img1Channel, img2Channel, Ch1Name, Ch2Name, mask, roi.offset, roi.size);
		} else if (roi != null) {
				// we have no mask, but a regular ROI in use
				container = new DataContainer<T>(img1, img2,
						img1Channel, img2Channel, Ch1Name, Ch2Name, roi.offset, roi.size);
		} else {
			// no mask and no ROI is present
			container = new DataContainer<T>(img1, img2,
					img1Channel, img2Channel, Ch1Name, Ch2Name);
		}

		// create a results handler
		final List<ResultHandler<T>> listOfResultHandlers = new ArrayList<ResultHandler<T>>();
		final PDFWriter<T> pdfWriter = new PDFWriter<T>(container);
		final SingleWindowDisplay<T> swDisplay = new SingleWindowDisplay<T>(container, pdfWriter);
		listOfResultHandlers.add(swDisplay);
		listOfResultHandlers.add(pdfWriter);
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
		addIfValid(SpearmanRankCorrelation, userSelectedJobs);
		addIfValid(mandersCorrelation, userSelectedJobs);
		addIfValid(histogram2D, userSelectedJobs);
		addIfValid(costesSignificance, userSelectedJobs);

		// execute all algorithms
		int count = 0;
		int jobs = userSelectedJobs.size();
		for (Algorithm<T> a : userSelectedJobs){
			try {
				count++;
				IJ.showStatus(count + "/" + jobs + ": Running " + a.getName());
				a.execute(container);
			}
			catch (MissingPreconditionException e){
				for (ResultHandler<T> r : listOfResultHandlers){
					r.handleWarning(
							new Warning( "Probem with input data", a.getName() + ": " + e.getMessage() ) );
				}
			}
		}
		// clear status
		IJ.showStatus("");

		// let the algorithms feed their results to the handler
		for (Algorithm<T> a : userSelectedJobs){
			for (ResultHandler<T> r : listOfResultHandlers)
				a.processResults(r);
		}
		// if we have ROIs/masks, add them to results
		if (displayImages) {
			RandomAccessibleInterval<T> channel1, channel2;
			if (mask != null || roi != null) {
				long[] offset = container.getMaskBBOffset();
				long[] size = container.getMaskBBSize();
				channel1 = createMaskImage( container.getSourceImage1(),
						container.getMask(), offset, size );
				channel2 = createMaskImage( container.getSourceImage2(),
						container.getMask(), offset, size );
			} else {
				channel1 = container.getSourceImage1();
				channel2 = container.getSourceImage2();
			}
			for (ResultHandler<T> r : listOfResultHandlers) {
				r.handleImage (channel1, "Channel 1");
				r.handleImage (channel2, "Channel 2");
			}
		}
		// do the actual results processing
		swDisplay.process();
		// add window to the IJ window manager
		swDisplay.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				WindowManager.removeWindow((Frame) swDisplay);
			}
		});
		WindowManager.addWindow(swDisplay);
		// show PDF saving dialog if requested
		if (autoSavePdf)
			pdfWriter.process();
    }

	/**
	 * A method to get the bounding box from the data in the given
	 * image that is above zero. Those values are interpreted as a
	 * mask. It will return null if no mask information was found.
	 *
	 * @param mask The image to look for "on" values in
	 * @return a new MaskInfo object or null
	 */
	protected MaskInfo getBoundingBoxOfMask(Img<T> mask) {
		Cursor<T> cursor = mask.localizingCursor();

		int numMaskDims = mask.numDimensions();
		// the "off type" of the mask
		T offType = mask.firstElement().createVariable();
		offType.setZero();
		// the corners of the bounding box
		long[] min = null;
		long[] max = null;
		// indicates if mask data has been found
		boolean maskFound = false;
		// a container for temporary position information
		long[] pos = new long[numMaskDims];
		// walk over the mask
		while (cursor.hasNext() ) {
			cursor.fwd();
			T data = cursor.get();
			// test if the current mask data represents on or off
			if (data.compareTo(offType) > 0) {
				// get current position
				cursor.localize(pos);
				if (!maskFound) {
					// we found mask data, first time
					maskFound = true;
					// init min and max with the current position
					min = Arrays.copyOf(pos, numMaskDims);
					max = Arrays.copyOf(pos, numMaskDims);
				} else {
					/* Is is at least second hit, compare if it
					 * has new "extreme" positions, i.e. does
					 * is make the BB bigger?
					 */
					for (int d=0; d<numMaskDims; d++) {
						if (pos[d] < min[d]) {
							// is it smaller than min
							min[d] = pos[d];
						} else if (pos[d] > max[d]) {
							// is it larger than max
							max[d] = pos[d];
						}
					}
				}
			}
		}

		if (!maskFound) {
			return null;
		} else {
			// calculate size
			long[] size = new long[numMaskDims];
			for (int d=0; d<numMaskDims; d++)
				size[d] = max[d] - min[d] + 1;
			// create and add bounding box
			BoundingBox bb = new BoundingBox(min, size);
			return new MaskInfo(bb, mask);
		}
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
	 * This method checks if the given ImagePlus contains any
	 * masks or ROIs. If so, the appropriate date structures
	 * are created and filled.
	 */
	protected void createMasksFromImage(ImagePlus imp) {
		// get ROIs from current image in Fiji
		Roi[] impRois = split(imp.getRoi());
		// create the ROIs
		createMasksAndRois(impRois, imp.getWidth(), imp.getHeight());
	}

	/**
	 * A method to fill the masks array with data based on the ROI manager.
	 */
	protected boolean createMasksFromRoiManager(int width, int height) {
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			IJ.error("Could not get ROI Manager instance.");
			return false;
		}
		Roi[] selectedRois = roiManager.getSelectedRoisAsArray();
		// create the ROIs
		createMasksAndRois(selectedRois, width, height);
		return true;
	}

	/**
	 * Creates appropriate data structures from the ROI information
	 * passed. If an irregular ROI is found, it will be put into a
	 * frame of its bounding box size and put into an Image<T>.
	 *
	 * In the end the members ROIs, masks and maskBBs will be
	 * filled if ROIs or masks were found. They will be null
	 * otherwise.
	 */
	protected void createMasksAndRois(Roi[] rois, int width, int height) {
		// create empty list
		masks.clear();

		for (Roi r : rois ){
			MaskInfo mi = new MaskInfo();
			// add it to the list of masks/ROIs
			masks.add(mi);
			// get the ROIs/masks bounding box
			Rectangle rect = r.getBounds();
			mi.roi = new BoundingBox(
					new long[] {rect.x, rect.y} ,
					new long[] {rect.width, rect.height});
			ImageProcessor ipMask = r.getMask();
			// check if we got a regular ROI and return if so
			if (ipMask == null) {
				continue;
			}

			// create a mask processor of the same size as a slice
			ImageProcessor ipSlice = ipMask.createProcessor(width, height);
			// fill the new slice with black
			ipSlice.setValue(0.0);
			ipSlice.fill();
			// position the mask on the new  mask processor
			ipSlice.copyBits(ipMask, (int)mi.roi.offset[0], (int)mi.roi.offset[1], Blitter.COPY);
			// create an Image<T> out of it
			ImagePlus maskImp = new ImagePlus("Mask", ipSlice);
			// and remember it and the masks bounding box
			mi.mask = ImagePlusAdapter.<T>wrap( maskImp );
		}
	}

	/**
	 * This method duplicates the given images, but respects
	 * ROIs if present. Meaning, a sub-picture will be created when
	 * source images are ROI/MaskImages.
	 * @throws MissingPreconditionException
	 */
	protected RandomAccessibleInterval<T> createMaskImage(
			RandomAccessibleInterval<T> image, RandomAccessibleInterval<BitType> mask,
			long[] offset, long[] size) throws MissingPreconditionException {
		long[] pos = new long[ image.numDimensions() ];
		// sanity check
		if (pos.length != offset.length || pos.length != size.length) {
			throw new MissingPreconditionException("Mask offset and size must be of same dimensionality like image.");
		}
		// use twin cursor for only one image
		TwinCursor<T> cursor = new TwinCursor<T>(
				image.randomAccess(),
				image.randomAccess(),
				Views.iterable(mask).localizingCursor());
		// prepare output image
		ImgFactory<T> maskFactory = new ArrayImgFactory<T>();
		//Img<T> maskImage = maskFactory.create( size, name );
		RandomAccessibleInterval<T> maskImage = maskFactory.create( size, Util.getTypeFromRandomAccess(image).createVariable() );
		RandomAccess<T> maskCursor = maskImage.randomAccess();
		// go through the visible data and copy it to the output
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			// shift coordinates by offset
			for (int i=0; i < pos.length; ++i) {
				pos[i] = pos[i] - offset[i];
			}
			// write out to correct position
			maskCursor.setPosition( pos );
			maskCursor.get().set( cursor.getChannel1() );
		}

		return maskImage;
	}

	/**
	 * Splits a non overlapping composite ROI into its sub ROIs.
	 *
	 * @param roi The ROI to split
	 * @return A list of one or more ROIs
	 */
	public static Roi[] split(Roi roi) {
		if (roi instanceof ShapeRoi)
			return ((ShapeRoi)roi).getRois();
		return new Roi[] { roi };
	}
}
