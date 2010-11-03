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
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * A plugin which does colocalisation on two images.
 *
 * @param <T>
 */
public class Coloc_2<T extends RealType<T>> implements PlugIn {

	// Allowed types of ROI configuration
	protected enum RoiConfiguration {None, Img1, Img2};
    /* GUI related members */
	protected final String[] roiLabels =  { "None","Channel 1", "Channel 2",};
	// the ROI configuration to use
	RoiConfiguration roiConfig = RoiConfiguration.None;
    // the ROI to use (null if none)
    Rectangle roi = null;
	// default indices of image, mask and roi choices
    protected static int index1 = 0;
    protected static int index2 = 1;
	protected static int indexMask = 0;
	protected static int indexRoi = 0;

	// the images to work on
	Image<T> img1, img2;
    // the channels of the amages to use
    int img1Channel = 1, img2Channel = 1;

	public void run(String arg0) {
        if (showDialog()) {
            colocalise(img1, img2);
        }
    }

    public void colocalise(Image<T> img1, Image<T> img2) {
	    // indicates if a ROI should be used
	boolean useRoi = (roi != null);

		// create a new container for the selected images and channels
		DataContainer<T> container;
		if (useRoi) {
			int roiOffset[] = new int[] {roi.x, roi.y};
			int roiSize[] = new int[] {roi.width, roi.height};
			container = new DataContainer<T>(img1, img2, img1Channel, img2Channel,
					roiOffset, roiSize);
		} else {
			container = new DataContainer<T>(img1, img2, img1Channel, img2Channel);
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
			new Histogram2D<T>("2D intensity histogram") );
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
		String[] chooseMask=  new String[windowList.length + 1];
		chooseMask[0]="<None>";

		for (int i=0; i < windowList.length; i++) {
			ImagePlus imp = WindowManager.getImage(windowList[i]);
            if (imp != null) {
                titles[i] = imp.getTitle();
                chooseMask[i + 1] =imp.getTitle();
            } else {
                titles[i] = "";
            }
		}

        /* make sure the default indices are no bigger
         * than the amount of images we have
         */
        index1 = clip( index1, 0, titles.length );
        index2 = clip( index2, 0, titles.length );
        indexMask = clip( indexMask, 0, titles.length);

		gd.addChoice("Channel_1", titles, titles[index1]);
		gd.addChoice("Channel_2", titles, titles[index2]);
		//gd.addChoice("Use ROI", roiLabels, roiLabels[indexRoi]);

        // show the dialog, finally
        gd.showDialog();
        // do nothing if dialog has been canceled
        if (gd.wasCanceled())
            return false;

		ImagePlus imp1 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
		ImagePlus imp2 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
	    // save the ImgLib wrapped images as members
		img1 = ImagePlusAdapter.wrap(imp1);
		img2 = ImagePlusAdapter.wrap(imp2);

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

        return true;
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
}
