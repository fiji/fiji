package fiji.plugin.trackmate.detection.subpixel;

import net.imglib2.Localizable;
import net.imglib2.type.numeric.NumericType;

public class SubPixelLocalization< T extends NumericType<T> & Comparable<T>> implements Localizable, Comparable<SubPixelLocalization<T>> {

	public static enum LocationType { INVALID, MIN, MAX };
	
	protected long[] pixelCoordinates;
	protected double[] subPixelLocationOffset;
	protected T value;
	protected T interpolatedValue;
	protected String errorMessage = "";
	protected LocationType locationType;
	
	public SubPixelLocalization(final long[] position, final T value, final LocationType locationType) {
		this.locationType = locationType;
		this.pixelCoordinates = position;
		this.subPixelLocationOffset = new double[position.length];
		this.value = value.copy();
		this.interpolatedValue = value.createVariable();
		this.interpolatedValue.setZero();
	}

	@Override
	public void localize(float[] position) {
		for (int i = 0; i < position.length; i++) {
			position[i] = (float) (pixelCoordinates[i] + subPixelLocationOffset[i]);
		}
	}

	@Override
	public void localize(double[] position) {
		for (int i = 0; i < position.length; i++) {
			position[i] = pixelCoordinates[i] + subPixelLocationOffset[i];
		}
	}

	@Override
	public float getFloatPosition(int d) {
		return (float) (pixelCoordinates[d] + subPixelLocationOffset[d]);
	}

	@Override
	public double getDoublePosition(int d) {
		return pixelCoordinates[d] + subPixelLocationOffset[d];
	}

	@Override
	public int numDimensions() {
		return pixelCoordinates.length;
	}

	@Override
	public void localize(int[] position) {
		for (int i = 0; i < position.length; i++) {
			position[i] = (int) pixelCoordinates[i];
		}
	}

	@Override
	public void localize(long[] position) {
		for (int i = 0; i < position.length; i++) {
			position[i] = pixelCoordinates[i];
		}
	}

	@Override
	public int getIntPosition(int d) {
		return (int) pixelCoordinates[d];
	}

	@Override
	public long getLongPosition(int d) {
		return pixelCoordinates[d];
	}

	public void setSubPixelLocationOffset(double offset, int d) {
		this.subPixelLocationOffset[d] = offset;
	}

	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}

	public T getInterpolatedValue() {
		return interpolatedValue;
	}

	public void setErrorMessage(String error) {
		this.errorMessage = error;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}

	public LocationType getLocationType() {
		return locationType;
	}

	public void setLocationType(LocationType locationType) {
		this.locationType = locationType;
	}

	
	@Override
	public int compareTo(final SubPixelLocalization<T> inPeak) {
		/*
		 * You're probably wondering why this is negated.
		 * It is because Collections.sort() sorts only in the forward direction.
		 * I want these to be sorted in the descending order, and the Collections.sort
		 * method is the only thing that should ever touch Peak.compareTo.
		 * This is faster than sorting, then reversing.
		 */
		if (value.compareTo(inPeak.value) == 1) 	{
			return -1;
		} else if (value.compareTo(inPeak.value) == 0) {
			return 0;
		} else {
			return 1;
		}
	}
}
