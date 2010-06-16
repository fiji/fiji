import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

public abstract class Result {
	String name;

	public Result(String name) {
		super();
		this.name = name;
	}

	static class ImageResult <T extends RealType<T>> extends Result {
		Image<T> data;
		//Calibaration / MetaData info?

		public ImageResult(String name, Image<T> data) {
			super(name);
			this.data = data;
		}
	}

	static class Histogram2DResult <T extends RealType<T>> extends Result{
		Image<T> data;
		String xLabel, yLabel;

		public Histogram2DResult(String name, Image<T> data, String xLabel, String yLabel) {
			super(name);
			this.data = data;
			this.xLabel = xLabel;
			this.yLabel = yLabel;
		}
	}

	static class ValueArrayResults extends Result{
		double[] values;
		double[] time;
		double[] thresholds;

		public ValueArrayResults(String name, double[] values, double[] time, double[] thresholds) {
			super(name);
			this.values = values;
			this.time = time;
			this.thresholds = thresholds;
		}
	}

	static class ValueResult extends Result{
		double value;
		double time;
		double thresholds;
		int decimalPlaces;

		public ValueResult(String name, double value, double time, double thresholds, int decimalPlaces) {
			super(name);
			this.value = value;
			this.time = time;
			this.thresholds = thresholds;
			this.decimalPlaces = decimalPlaces;
		}
	}
}