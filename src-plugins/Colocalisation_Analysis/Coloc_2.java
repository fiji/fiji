import java.util.ArrayList;
import java.util.List;
import java.io.File;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

/**
 * A plugin which does colocalisation on two images.
 *
 * @param <T>
 */
public class Coloc_2<T extends RealType<T>> implements PlugIn {

	// the images to work on
	Image<T> img1, img2;

	public void run(String arg0) {
		// Development code
		ImagePlus imp1 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/red.tif");
		if (imp1 == null)
			imp1 = WindowManager.getImage(1);
		img1 = ImagePlusAdapter.wrap(imp1);

		ImagePlus imp2 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/green.tif");
		if (imp2 == null)
			imp2 = WindowManager.getImage(2);
		img2 = ImagePlusAdapter.wrap(imp2);

		int theImg1Channel = 1, theImg2Channel = 1;
		// Development code end

		// create a new container for the selected images and channels
		DataContainer container = new DataContainer(img1, img2, theImg1Channel, theImg2Channel);

		// this list contains the algorithms that will be run when the user clicks ok
		List<Algorithm> userSelectedJobs = new ArrayList<Algorithm>();

		userSelectedJobs.add(new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast));
		userSelectedJobs.add(new LiHistogram2D());
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
		theResultDisplay.display(container);


	}
}
