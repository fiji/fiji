package results;

import gadgets.DataContainer;
import ij.IJ;
import ij.ImagePlus;
import ij.text.TextWindow;
import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;
import algorithms.Histogram2D;

public class EasyDisplay<T extends RealType<T>> implements ResultHandler<T> {
	// the text window to present value and text results
	protected static TextWindow textWindow;
	/* the data container with general information about the
	 * source images that were processed by the algorithms.
	 */
	protected DataContainer<T> container;

	public EasyDisplay(DataContainer<T> container) {
		final int twWidth = 170;
		final int twHeight = 250;
		//test if the results windows is already there, if so use it.
		if (textWindow == null || !textWindow.isVisible())
			textWindow = new TextWindow("Results",
				"Result\tValue\n", "", twWidth, twHeight);
		else {
			// set dimensions
			textWindow.setSize(twWidth, twHeight);
		}
		// deactivate the window for now
		textWindow.setVisible(false);
		// save a reference to the data container
		this.container = container;
	}

	public void handleImage(Image<T> image) {
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( image );
		double max = ImageStatistics.getImageMax( image ).getRealDouble();
		showImage( imp, max );
	}

	public void handleHistogram(Histogram2D<T> histogram) {
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( histogram.getPlotImage() );
		double max = ImageStatistics.getImageMax( histogram.getPlotImage() ).getRealDouble();
		showImage( imp, max );
	}

	protected void showImage(ImagePlus imp, double max) {
		// set the display range
		imp.setDisplayRange(0.0, max);
		imp.show();
	}

	public void handleWarning(Warning warning) {
		// no warnings are shown in easy display
	}

	public void handleValue(String name, double value) {
		handleValue(name, value, 3);
	}

	public void handleValue(String name, double value, int decimals) {
		textWindow.getTextPanel().appendLine(name + "\t"
				+ IJ.d2s(value, decimals) + "\n");
	}

	protected void printTextStatistics(DataContainer<T> container){
		textWindow.getTextPanel().appendLine("Ch1 Mean\t" + container.getMeanCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Mean\t" + container.getMeanCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Min\t" + container.getMinCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Min\t" + container.getMinCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Max\t" + container.getMaxCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Max\t" + container.getMaxCh2() + "\n");
	}

	public void process() {
		// print some general information about images
		printTextStatistics(container);
	    // show the results
		textWindow.setVisible(true);
		IJ.selectWindow("Results");
	}
}
