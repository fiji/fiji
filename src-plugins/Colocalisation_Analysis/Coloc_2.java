import java.util.ArrayList;
import java.util.List;
import java.awt.Rectangle;
import java.io.File;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.LongType;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.PlugIn;

/**
 * A plugin which does colocalisation on two images.
 *
 * @param <T>
 */
public class Coloc_2<T extends RealType<T>> implements PlugIn {

	// Allowed types of ROI configuration
	protected enum RoiConfiguration {None, Img1, Img2};
	// indicates if a ROI should be used
	boolean useRoi = false;

	// the images to work on
	Image<T> img1, img2;

	public void run(String arg0) {
		// Development code
		ImagePlus imp1 = WindowManager.getImage(1);
		if (imp1 == null)
			imp1 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/red.tif");
		img1 = ImagePlusAdapter.wrap(imp1);

		ImagePlus imp2 =  WindowManager.getImage(2);
		if (imp2 == null)
			imp2 =IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/green.tif");
		img2 = ImagePlusAdapter.wrap(imp2);

		int theImg1Channel = 1, theImg2Channel = 1;


		// configure the ROI
		RoiConfiguration roiConfig = RoiConfiguration.Img1;

		// Development code end

		// configure the ROI
		Rectangle roi = null;
		/* check if we have a valid ROI for the selected configuration
		 * and if so, get the ROI's bounds. Currently, only rectangular
		 * ROIs are supported.
		 */
		if (roiConfig == RoiConfiguration.Img1 && hasValidRoi(imp1)) {
			roi = imp1.getRoi().getBounds();
		} else if (roiConfig == RoiConfiguration.Img2 && hasValidRoi(imp2)) {
			roi = imp2.getRoi().getBounds();
		}
		useRoi = (roi != null);

		// create a new container for the selected images and channels
		DataContainer<T> container;
		if (useRoi) {
			int roiOffset[] = new int[] {roi.x, roi.y};
			int roiSize[] = new int[] {roi.width, roi.height};
			container = new DataContainer<T>(img1, img2, theImg1Channel, theImg2Channel,
					roiOffset, roiSize);
		} else {
			container = new DataContainer<T>(img1, img2, theImg1Channel, theImg2Channel);
		}

		// create a results handler
		ResultHandler<T> resultHandler = new SingleWindowDisplay<T>(container);
		//ResultHandler<T> resultHandler = new EasyDisplay<T>(container);

		// this list contains the algorithms that will be run when the user clicks ok
		List<Algorithm<T>> userSelectedJobs = new ArrayList<Algorithm<T>>();

		// add some pre-processing jobs:
		userSelectedJobs.add( container.setInputCheck(
			new InputCheck<T>()) );
		userSelectedJobs.add( container.setAutoThreshold(
			new AutoThresholdRegression<T>()) );

		// add user selected algorithms
		PearsonsCorrelation pc = new PearsonsCorrelation<T>(PearsonsCorrelation.Implementation.Classic);
		userSelectedJobs.add( pc );
		userSelectedJobs.add(
			new LiHistogram2D<T>("Li - Ch1", true) );
		userSelectedJobs.add(
			new LiHistogram2D<T>("Li - Ch2", false) );
		userSelectedJobs.add(
			new LiICQ<T>() );
		userSelectedJobs.add(
			new MandersCorrelation<T>() );
		userSelectedJobs.add(
			new Histogram2D<T>("hello") );
		userSelectedJobs.add(
			new CostesSignificanceTest(pc, 3, 100) );


		for (Algorithm a : userSelectedJobs){
			try {
				a.execute(container);
			}
			catch (MissingPreconditionException e){
				String aName = a.getClass().getName();
				System.out.println("MissingPreconditionException occured in " + aName + " algorithm: " + e.getMessage());
				resultHandler.handleWarning(
						new Warning( "Probem with input data", aName + " - " + e.getMessage() ) );
			}
		}

		// let the algorithms feed their results to the handler
		for (Algorithm a : userSelectedJobs){
			a.processResults(resultHandler);
		}
		// do the actual results processing
		resultHandler.process();
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
}
