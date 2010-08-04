import java.util.ArrayList;
import java.util.List;
import java.awt.Rectangle;
import java.io.File;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
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
		DataContainer container;
		if (useRoi) {
			int roiOffset[] = new int[] {roi.x, roi.y};
			int roiSize[] = new int[] {roi.width, roi.height};
			container = new DataContainer(img1, img2, theImg1Channel, theImg2Channel,
					roiOffset, roiSize);
		} else {
			container = new DataContainer(img1, img2, theImg1Channel, theImg2Channel);
		}

		// this list contains the algorithms that will be run when the user clicks ok
		List<Algorithm> userSelectedJobs = new ArrayList<Algorithm>();

		// add some preprocessing jobs:
		userSelectedJobs.add(new InputCheck());
		userSelectedJobs.add(new AutoThresholdRegression());

		// add user selected algorithms
		userSelectedJobs.add(new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast));
		userSelectedJobs.add(new LiHistogram2D("Li - Ch1", true));
		userSelectedJobs.add(new LiHistogram2D("Li - Ch2", false));
		userSelectedJobs.add(new LiICQ());
		userSelectedJobs.add(new Histogram2D("hello"));

		try {
			for (Algorithm a : userSelectedJobs){
				a.execute(container);
			}
		}
		catch (MissingPreconditionException e){
			System.out.println("Exception occured in Algorithm preconditions: " + e.getMessage());
		}

		Display theResultDisplay = new SingleWindowDisplay();
		//Display theResultDisplay = new EasyDisplay();
		theResultDisplay.display(container);


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
