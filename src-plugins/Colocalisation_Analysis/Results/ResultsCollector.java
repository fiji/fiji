import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.LongType;

public abstract class ResultsCollector<T extends RealType<T>> implements ResultHandler<T> {

	// a list of the available result images, no matter what specific kinds
	protected List<Image<? extends RealType>> listOfImages
		= new ArrayList<Image<? extends RealType>>();
	protected Map<Image<LongType>, Histogram2D<T>> mapOf2DHistograms
		= new HashMap<Image<LongType>, Histogram2D<T>>();
	// a list of warnings
	protected List<Warning> warnings = new ArrayList<Warning>();

	// a list of named values, collected from algorithms
	protected List<ValueResult> valueResults = new ArrayList<ValueResult>();

	public void handleImage(Image<T> image) {
		listOfImages.add( image );
	}

	public void handleHistogram(Histogram2D<T> histogram) {
		listOfImages.add(histogram.getPlotImage());
		mapOf2DHistograms.put(histogram.getPlotImage(), histogram);
	}

	public void handleWarning(Warning warning) {
		warnings.add( warning );
	}

	public void handleValue(String name, double value) {
		handleValue(name, value, 3);
	}

	public void handleValue(String name, double value, int decimals) {
		valueResults.add( new ValueResult(name, value, decimals));
	}

	public abstract void process();
}
