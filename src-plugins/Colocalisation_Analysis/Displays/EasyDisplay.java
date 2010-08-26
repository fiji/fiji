import java.util.Iterator;

import ij.IJ;
import ij.ImagePlus;
import ij.text.TextWindow;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

public class EasyDisplay<T extends RealType<T>> implements Display {

	protected static TextWindow textWindow;

	public void display(DataContainer container) {

		//test if the results windows is already there, if so use it.
		if (textWindow == null || !textWindow.isVisible())
			textWindow = new TextWindow("Results",
				"Result\tValue\n", "", 400, 250);

		printTextStatistics(container);

		// 2D Histogram
		if (container.getHistogram2D() != null)
			showImage( container.getHistogram2D().getPlotImage() );

		// Two Li Histograms
		if (container.getLiHistogramCh1() != null)
			showImage( container.getLiHistogramCh1().getPlotImage() );
		if (container.getLiHistogramCh2() != null)
			showImage( container.getLiHistogramCh2().getPlotImage() );

		AutoThresholdRegression autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null) {
			showValue( "m (slope)", autoThreshold.getAutoThresholdSlope() , 2 );
			showValue( "b (y-intercept)", autoThreshold.getAutoThresholdIntercept(), 2 );
			showValue( "b to y-max ratio", autoThreshold.getBToYMaxRatio(), 2 );
		}

		PearsonsCorrelation pearsons = container.getPearsonsCorrelation();
		if (pearsons != null) {
			showValue("Pearson's R value (no threshold)", pearsons.getPearsonsCorrelationValue(), 2);
			showValue("Pearson's R value (threshold)", pearsons.getThresholdedPearsonsCorrelationValue(), 2);
		}

		IJ.selectWindow("Results");
	}

	protected void showImage(Image<T> img) {
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( img );
		imp.show();
	}

	protected void showValue(String name, double value, int decimalPlaces) {
		textWindow.getTextPanel().appendLine(name + "\t"
			+ IJ.d2s(value, decimalPlaces) + "\n");
	}

	protected void printTextStatistics(DataContainer container){
		textWindow.getTextPanel().appendLine("Ch1 Mean\t" + container.getMeanCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Mean\t" + container.getMeanCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Min\t" + container.getMinCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Min\t" + container.getMinCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Max\t" + container.getMaxCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Max\t" + container.getMaxCh2() + "\n");

		AutoThresholdRegression autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null) {
			textWindow.getTextPanel().appendLine("Ch1 Max Threshold\t" + autoThreshold.getCh1MaxThreshold() + "\n");
			textWindow.getTextPanel().appendLine("Ch2 Max Threshold\t" + autoThreshold.getCh2MaxThreshold() + "\n");
		}
	}
}
