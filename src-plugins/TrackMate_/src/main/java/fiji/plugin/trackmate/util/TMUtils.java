package fiji.plugin.trackmate.util;

import ij.ImagePlus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.Metadata;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;

/**
 * List of static utilities for the {@link TrackMate} trackmate
 */
public class TMUtils {


	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
	
	/*
	 * STATIC METHODS
	 */

	/**
	 * Return a new map sorted by its values.
	 * Adapted from http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map, final Comparator<V> comparator) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>( map.entrySet() );
		Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
			public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )			{
				return comparator.compare(o1.getValue(), o2.getValue()); 
			}
		} );

		LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put( entry.getKey(), entry.getValue() );
		}
		return result;
	}


	/**
	 * Generate a string representation of a map, typically a settings map.
	 */
	public static final String echoMap(final Map<String, Object> map, int indent) {
		// Build string
		StringBuilder builder = new StringBuilder();
		for (String key : map.keySet()) {
			for (int i = 0; i < indent; i++) {
				builder.append(" ");
			}
			builder.append("- ");
			builder.append(key.toLowerCase().replace("_", " "));
			builder.append(": ");
			Object obj = map.get(key);
			if (obj instanceof Map) {
				builder.append('\n');
				@SuppressWarnings("unchecked")
				Map<String, Object> submap = (Map<String, Object>) obj;
				builder.append(echoMap(submap , indent + 2));
			} else {
				builder.append(obj.toString());
				builder.append('\n');
			}
		}
		return builder.toString();
	}

	/** 
	 * Wraps an IJ {@link ImagePlus} in an imglib2 {@link ImgPlus}, without parameterized types.
	 * The only way I have found to beat javac constraints on bounded multiple wildcard.
	 */
	@SuppressWarnings("rawtypes")
	public static final ImgPlus rawWraps(final ImagePlus imp)	 {
		ImgPlus<DoubleType> img = ImagePlusAdapter.wrapImgPlus(imp);
		ImgPlus raw = img;
		return raw;
	}



	/**
	 * Check that the given map has all some keys. Two String collection allows specifying 
	 * that some keys are mandatory, other are optional.
	 * @param map  the map to inspect.
	 * @param mandatoryKeys the collection of keys that are expected to be in the map. Can be <code>null</code>.
	 * @param optionalKeys the collection of keys that can be - or not - in the map. Can be <code>null</code>.
	 * @param errorHolder will be appended with an error message.
	 * @return if all mandatory keys are found in the map, and possibly some optional ones, but no others.
	 */
	public static final <T> boolean checkMapKeys(final Map<T, ?> map, Collection<T> mandatoryKeys, Collection<T> optionalKeys, final StringBuilder errorHolder) {
		if (null == optionalKeys) {
			optionalKeys = new ArrayList<T>();
		}
		if (null == mandatoryKeys) {
			mandatoryKeys = new ArrayList<T>();
		}
		boolean ok = true;
		Set<T> keySet = map.keySet();
		for(T key : keySet) {
			if (! (mandatoryKeys.contains(key) || optionalKeys.contains(key)) ) {
				ok = false;
				errorHolder.append("Map contains unexpected key: "+key+".\n");
			}
		}

		for(T key : mandatoryKeys) {
			if (!keySet.contains(key)) {
				ok = false;
				errorHolder.append("Mandatory key "+key+" was not found in the map.\n");
			}
		}
		return ok;

	}


	/**
	 * Check the presence and the validity of a key in a map, and test it is of the desired class.
	 * @param map the map to inspect.
	 * @param key  the key to find.
	 * @param expectedClass  the expected class of the target value .
	 * @param errorHolder will be appended with an error message.
	 * @return  true if the key is found in the map, and map a value of the desired class.
	 */
	public static final boolean checkParameter(final Map<String, Object> map, String key, final Class<?> expectedClass, final StringBuilder errorHolder) {
		Object obj = map.get(key);
		if (null == obj) {
			errorHolder.append("Parameter "+key+" could not be found in settings map.\n");
			return false;
		}
		if (!expectedClass.isInstance(obj)) {
			errorHolder.append("Value for parameter "+key+" is not of the right class. Expected "+expectedClass.getName()+", got "+obj.getClass().getName()+".\n");
			return false;
		}
		return true;
	}



	/**
	 * Returns the mapping in a map that is targeted by a list of keys, in the order given in the list.
	 */
	public static final <J,K> List<K> getArrayFromMaping(List<J> keys, Map<J, K> mapping) {
		List<K> names = new ArrayList<K>(keys.size());
		for (int i = 0; i < keys.size(); i++) {
			names.add(mapping.get(keys.get(i)));
		}
		return names;
	}

	/**
	 * Translate each spot of the given collection by the amount specified in 
	 * argument. The distances are all understood in physical units.
	 * <p>
	 * This is meant to deal with a cropped image. The translation will bring the spot
	 * coordinates back to the top-left corner of the un-cropped image reference. 
	 */
	public static void translateSpots(final Collection<Spot> spots, double dx, double dy, double dz) {
		double[] dval = new double[] {dx, dy, dz};
		String[] features = new String[] { Spot.POSITION_X, Spot.POSITION_Y, Spot.POSITION_Z }; 
		Double val;
		for(Spot spot : spots) {
			for (int i = 0; i < features.length; i++) {
				val = spot.getFeature(features[i]);
				if (null != val)
					spot.putFeature(features[i], val+dval[i]);
			}
		}
	}

	/**
	 * Create a new list of spots, made from the given list by excluding overlapping spots.
	 * <p>
	 * Overlapping is checked by ensuring that the two compared spots are no closer than the sum
	 * of their respective radius. If two spots are overlapping, only the one that has the highest
	 * value of the {@link SpotFeature}  given in argument is retained, and the other one is discarded.
	 * 
	 * @param spots  the list of spot to suppress. It will be sorted by descending feature value by this call.
	 * @param feature  the feature to consider when choosing what spot to retain in an overlapping couple. 
	 * @return a new pruned list of non-overlapping spots. Incidentally, this list will be sorted by descending feature value.
	 */
	public static final List<Spot> suppressSpots(List<Spot> spots, final String feature) {
		Collections.sort(spots, createDescendingComparatorFor(feature));
		final List<Spot> acceptedSpots = new ArrayList<Spot>(spots.size());
		boolean ok;
		double r2;
		for (final Spot spot : spots) {
			ok = true;
			for (final Spot target : acceptedSpots) {
				r2 = (spot.getFeature(Spot.RADIUS) + target.getFeature(Spot.RADIUS)) * (spot.getFeature(Spot.RADIUS) + target.getFeature(Spot.RADIUS));
				if (spot.squareDistanceTo(target) < r2 ) {
					ok = false;
					break;
				}
			}
			if (ok)
				acceptedSpots.add(spot);
		}
		return acceptedSpots;
	}


	public static final Comparator<Spot> createAscendingComparatorFor(final String feature) {
		return new Comparator<Spot>() {
			@Override
			public int compare(Spot o1, Spot o2) {
				return o1.getFeature(feature).compareTo(o2.getFeature(feature));
			}
		};
	}

	public static final Comparator<Spot> createDescendingComparatorFor(final String feature) {
		return new Comparator<Spot>() {
			@Override
			public int compare(Spot o1, Spot o2) {
				return o2.getFeature(feature).compareTo(o1.getFeature(feature));
			}
		};
	}

	/**
	 * Convenience static method that executes the thresholding part.
	 * <p>
	 * Given a list of spots, only spots with the feature satisfying the threshold given
	 * in argument are returned.
	 */
	public static TreeMap<Integer, List<Spot>> thresholdSpots(final TreeMap<Integer, List<Spot>> spots, final FeatureFilter filter) {
		TreeMap<Integer, List<Spot>> selectedSpots = new TreeMap<Integer, List<Spot>>();
		Collection<Spot> spotThisFrame, spotToRemove;
		List<Spot> spotToKeep;
		Double val, tval;	

		for (int timepoint : spots.keySet()) {

			spotThisFrame = spots.get(timepoint);
			spotToKeep = new ArrayList<Spot>(spotThisFrame);
			spotToRemove = new ArrayList<Spot>(spotThisFrame.size());

			tval = filter.value;
			if (null != tval) {

				if (filter.isAbove) {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(filter.feature);
						if (null == val)
							continue;
						if ( val < tval)
							spotToRemove.add(spot);
					}

				} else {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(filter.feature);
						if (null == val)
							continue;
						if ( val > tval)
							spotToRemove.add(spot);
					}
				}
				spotToKeep.removeAll(spotToRemove); // no need to treat them multiple times

			}

			selectedSpots.put(timepoint, spotToKeep);
		}
		return selectedSpots;
	}

	/**
	 * Convenience static method that executes the thresholding part.
	 * <p>
	 * Given a list of spots, only spots with the feature satisfying <b>all</b> of the thresholds given
	 * in argument are returned. 
	 */
	public static TreeMap<Integer, List<Spot>> thresholdSpots(final TreeMap<Integer, List<Spot>> spots, final List<FeatureFilter> filters) {
		TreeMap<Integer, List<Spot>> selectedSpots = new TreeMap<Integer, List<Spot>>();
		Collection<Spot> spotThisFrame, spotToRemove;
		List<Spot> spotToKeep;
		Double val, tval;	

		for (int timepoint : spots.keySet()) {

			spotThisFrame = spots.get(timepoint);
			spotToKeep = new ArrayList<Spot>(spotThisFrame);
			spotToRemove = new ArrayList<Spot>(spotThisFrame.size());

			for (FeatureFilter threshold : filters) {

				tval = threshold.value;
				if (null == tval)
					continue;
				spotToRemove.clear();

				if (threshold.isAbove) {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(threshold.feature);
						if (null == val)
							continue;
						if ( val < tval)
							spotToRemove.add(spot);
					}

				} else {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(threshold.feature);
						if (null == val)
							continue;
						if ( val > tval)
							spotToRemove.add(spot);
					}
				}
				spotToKeep.removeAll(spotToRemove); // no need to treat them multiple times
			}
			selectedSpots.put(timepoint, spotToKeep);
		}
		return selectedSpots;
	}


	/*
	 * ImgPlus & calibration & axes 
	 */

	/**
	 * @return the index of the target axisd in the given metadata. Return -1 if 
	 * the azis was not found.
	 */
	public static final int findAxisIndex(final Metadata img, final AxisType axis) {
		AxisType[] axes = new AxisType[img.numDimensions()];
		img.axes(axes);
		int index = Arrays.asList(axes).indexOf(axis);
		return index;
	}

	public static final int findXAxisIndex(final Metadata img) {
		return findAxisIndex(img, Axes.X);
	}

	public static final int findYAxisIndex(final Metadata img) {
		return findAxisIndex(img, Axes.Y);
	}

	public static final int findZAxisIndex(final Metadata img) {
		return findAxisIndex(img, Axes.Z);
	}

	public static final int findTAxisIndex(final Metadata img) {
		return findAxisIndex(img, Axes.TIME);
	}

	public static final int findCAxisIndex(final Metadata img) {
		return findAxisIndex(img, Axes.CHANNEL);
	}

	/**
	 * Return the xyz calibration stored in an {@link Metadata} in a 3-elements
	 * double array. Calibration is ordered as X, Y, Z. If one axis is not found,
	 * then the calibration for this axis takes the value of 1.
	 */
	public static final double[] getSpatialCalibration(final Metadata img) {
		final double[] calibration = Util.getArrayFromValue(1d, 3);
		for (int d = 0; d < img.numDimensions(); d++) {
			if (img.axis(d).equals(Axes.X)) {
				calibration[0] = img.calibration(d);
			} else if (img.axis(d).equals(Axes.Y)) {
				calibration[1] = img.calibration(d);
			} else if (img.axis(d).equals(Axes.Z)) {
				calibration[2] = img.calibration(d);
			}
		}
		return calibration;
	}

	public static double[] getSpatialCalibration(ImagePlus imp) {
		final double[] calibration = Util.getArrayFromValue(1d, 3);
		calibration[0] = imp.getCalibration().pixelWidth;
		calibration[1] = imp.getCalibration().pixelHeight;
		if (imp.getNSlices() > 1)
			calibration[2] = imp.getCalibration().pixelDepth;
		return calibration;
	}

	/**
	 * Returns an estimate of the <code>p</code>th percentile of the values
	 * in the <code>values</code> array. Taken from commons-math.
	 */
	public static final double getPercentile(final double[] values, final double p) {

		final int size = values.length;
		if ((p > 1) || (p <= 0)) {
			throw new IllegalArgumentException("invalid quantile value: " + p);
		}
		if (size == 0) {
			return Double.NaN;
		}
		if (size == 1) {
			return values[0]; // always return single value for n = 1
		}
		double n = size;
		double pos = p * (n + 1);
		double fpos = Math.floor(pos);
		int intPos = (int) fpos;
		double dif = pos - fpos;
		double[] sorted = new double[size];
		System.arraycopy(values, 0, sorted, 0, size);
		Arrays.sort(sorted);

		if (pos < 1) {
			return sorted[0];
		}
		if (pos >= n) {
			return sorted[size - 1];
		}
		double lower = sorted[intPos - 1];
		double upper = sorted[intPos];
		return lower + dif * (upper - lower);
	}


	/** 
	 * Returns <code>[range, min, max]</code> of the given double array.
	 * @return A double[] of length 3, where index 0 is the range, index 1 is the min, and index 2 is the max.
	 */
	public static final double[] getRange(final double[] data) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double value;
		for (int i = 0; i < data.length; i++) {
			value = data[i];
			if (value < min) min = value;
			if (value > max) max = value;
		}		
		return new double[] {(max-min), min, max};
	}
	
	/**
	 * Store the x, y, z coordinates of the specified spot 
	 * in the first 3 elements of the specified double array.
	 */
	public static final void localize(final Spot spot, final double[] coords) {
		coords[0] = spot.getFeature(Spot.POSITION_X).doubleValue();
		coords[1] = spot.getFeature(Spot.POSITION_Y).doubleValue();
		coords[2] = spot.getFeature(Spot.POSITION_Z).doubleValue();
	}

	/**
	 * Return the optimal bin number for a histogram of the data given in array, using the 
	 * Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)).
	 * It is ensured that the bin number returned is not smaller and no bigger than the bounds given
	 * in argument.
	 */
	public static final int getNBins(final double[] values, int minBinNumber, int maxBinNumber) {
		final int size = values.length;
		final double q1 = getPercentile(values, 0.25);
		final double q3 = getPercentile(values, 0.75);
		final double iqr = q3 - q1;
		final double binWidth = 2 * iqr * Math.pow(size, -0.33);
		final double[] range = getRange(values);
		int nBin = (int) ( range[0] / binWidth + 1 );
		if (nBin > maxBinNumber)
			nBin = maxBinNumber;
		else if (nBin < minBinNumber)
			nBin = minBinNumber;
		return  nBin;
	}

	/**
	 * Return the optimal bin number for a histogram of the data given in array, using the 
	 * Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)).
	 * It is ensured that the bin number returned is not smaller than 8 and no bigger than 256.
	 */
	public static final int getNBins(final double[] values){
		return getNBins(values, 8, 256);
	}


	/**
	 * Create a histogram from the data given.
	 */
	public static final int[] histogram(final double data[], final int nBins) {
		final double[] range = getRange(data);
		final double binWidth = range[0]/nBins;
		final int[] hist = new int[nBins];
		int index;

		if (nBins > 0)
			for (int i = 0; i < data.length; i++) {
				index = Math.min((int) Math.floor((data[i] - range[1]) / binWidth), nBins - 1);
				hist[index]++;
			}
		return hist;
	}

	/**
	 * Create a histogram from the data given, with a default number of bins given by {@link #getNBins(double[])}.
	 * @param data
	 * @return
	 */
	public static final int[] histogram(final double data[]) {
		return histogram(data, getNBins(data));
	}

	/**
	 * Return a threshold for the given data, using an Otsu histogram thresholding method.
	 */
	public static final double otsuThreshold(double[] data) {
		return otsuThreshold(data, getNBins(data));
	}

	/**
	 * Return a threshold for the given data, using an Otsu histogram thresholding method with a given bin number.
	 */
	public static final double otsuThreshold(double[] data, int nBins) {
		final int[] hist = histogram(data, nBins);
		final int thresholdIndex = otsuThresholdIndex(hist, data.length);
		double[] range = getRange(data);
		double binWidth = range[0] / nBins;
		return 	range[1] + binWidth * thresholdIndex;
	}

	/**
	 * Given a histogram array <code>hist</code>, built with an initial amount of <code>nPoints</code>
	 * data item, this method return the bin index that thresholds the histogram in 2 classes. 
	 * The threshold is performed using the Otsu Threshold Method, 
	 * {@link http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html}.
	 * @param hist  the histogram array
	 * @param nPoints  the number of data items this histogram was built on
	 * @return the bin index of the histogram that thresholds it
	 */
	public static final int otsuThresholdIndex(final int[] hist, final int nPoints)	{
		int total = nPoints;

		double sum = 0;
		for (int t = 0 ; t < hist.length ; t++) 
			sum += t * hist[t];

		double sumB = 0;
		int wB = 0;
		int wF = 0;

		double varMax = 0;
		int threshold = 0;

		for (int t = 0 ; t < hist.length ; t++) {
			wB += hist[t];               // Weight Background
			if (wB == 0) continue;

			wF = total - wB;                 // Weight Foreground
			if (wF == 0) break;

			sumB += (t * hist[t]);

			double mB = sumB / wB;            // Mean Background
			double mF = (sum - sumB) / wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = wB * wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = t;
			}
		}
		return threshold;
	}

	/**
	 * Computes the square Euclidean distance between two spots.
	 * @param i Spot i.
	 * @param j Spot j.
	 * @return The Euclidean distance between Featurable i and j, based on their
	 * position features X, Y, Z.
	 */
	public static final double euclideanDistanceSquared(Spot i, Spot j) {
		final Double xi, xj, yi, yj, zi, zj;
		double eucD = 0;
		xi = i.getFeature(Spot.POSITION_X);
		xj = j.getFeature(Spot.POSITION_X);
		yi = i.getFeature(Spot.POSITION_Y);
		yj = j.getFeature(Spot.POSITION_Y);
		zi = i.getFeature(Spot.POSITION_Z);
		zj = j.getFeature(Spot.POSITION_Z);

		if (xj != null && xi != null)
			eucD += (xj-xi)*(xj-xi);
		if (yj != null && yi != null)
			eucD += (yj-yi)*(yj-yi);
		if (zj != null && zi != null)
			eucD += (zj-zi)*(zj-zi);
		return eucD;
	}

	/**
	 * Return a String unit for the given dimension. When suitable, the unit is taken from the settings
	 * field, which contains the spatial and time units. Otherwise, default units are used.
	 */
	public static final String getUnitsFor(final Dimension dimension, String spaceUnits, String timeUnits) {
		String units = "no unit";
		switch (dimension) {
		case ANGLE:
			units = "Radians";
			break;
		case INTENSITY:
			units = "Counts";
			break;
		case INTENSITY_SQUARED:
			units = "Counts^2";
			break;
		case NONE:
			units = "";
			break;
		case POSITION:
		case LENGTH:
			units = spaceUnits;
			break;
		case QUALITY:
			units = "Quality";
			break;
		case TIME:
			units = timeUnits;
			break;
		case VELOCITY:
			units = spaceUnits + "/" + timeUnits;
			break;
		default:
			break;
		case STRING:
			return null;
		}
		return units;
	}


	public static final String getCurrentTimeString() {
		return DATE_FORMAT.format(new Date());
	}
	
}