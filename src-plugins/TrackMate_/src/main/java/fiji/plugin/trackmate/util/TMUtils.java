package fiji.plugin.trackmate.util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.ColorProcessor;
import ij.process.StackConverter;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.imglib2.exception.ImgLibException;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.Metadata;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMate_;

/**
 * List of static utilities for the {@link TrackMate_} plugin
 */
public class TMUtils {



	/*
	 * STATIC CONSTANTS
	 */

	/** The name of the spot quality feature. */
	public static final String QUALITY = "QUALITY";
	/** The name of the radius spot feature. */
	public static final String RADIUS = "RADIUS";
	/** The name of the spot X position feature. */
	public static final String POSITION_X = "POSITION_X";
	/** The name of the spot Y position feature. */
	public static final String POSITION_Y = "POSITION_Y";
	/** The name of the spot Z position feature. */
	public static final String POSITION_Z = "POSITION_Z";
	/** The name of the spot T position feature. */
	public static final String POSITION_T = "POSITION_T";
	/** The name of the frame feature. */
	public static final String FRAME = "FRAME";

	/** The position features. */
	public final static String[] POSITION_FEATURES = new String[] { POSITION_X, POSITION_Y, POSITION_Z };
	/** The 6 privileged spot features that must be set by a spot detector. */
	public final static Collection<String> FEATURES = new ArrayList<String>(6);
	/** The 6 privileged spot feature names. */
	public final static Map<String, String> FEATURE_NAMES = new HashMap<String, String>(6);
	/** The 6 privileged spot feature short names. */
	public final static Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(6);
	/** The 6 privileged spot feature dimensions. */
	public final static Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(6);

	static {
		FEATURES.add(QUALITY);
		FEATURES.add(POSITION_X);
		FEATURES.add(POSITION_Y);
		FEATURES.add(POSITION_Z);
		FEATURES.add(POSITION_T);
		FEATURES.add(FRAME);
		FEATURES.add(RADIUS);

		FEATURE_NAMES.put(POSITION_X, "X");
		FEATURE_NAMES.put(POSITION_Y, "Y");
		FEATURE_NAMES.put(POSITION_Z, "Z");
		FEATURE_NAMES.put(POSITION_T, "T");
		FEATURE_NAMES.put(FRAME, "Frame");
		FEATURE_NAMES.put(RADIUS, "Radius");
		FEATURE_NAMES.put(QUALITY, "Quality");

		FEATURE_SHORT_NAMES.put(POSITION_X, "X");
		FEATURE_SHORT_NAMES.put(POSITION_Y, "Y");
		FEATURE_SHORT_NAMES.put(POSITION_Z, "Z");
		FEATURE_SHORT_NAMES.put(POSITION_T, "T");
		FEATURE_SHORT_NAMES.put(FRAME, "Frame");
		FEATURE_SHORT_NAMES.put(RADIUS, "R");
		FEATURE_SHORT_NAMES.put(QUALITY, "Quality");

		FEATURE_DIMENSIONS.put(POSITION_X, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(POSITION_Y, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(POSITION_Z, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(POSITION_T, Dimension.TIME);
		FEATURE_DIMENSIONS.put(FRAME, Dimension.NONE);
		FEATURE_DIMENSIONS.put(RADIUS, Dimension.LENGTH);
		FEATURE_DIMENSIONS.put(QUALITY, Dimension.QUALITY);
	}



	/*
	 * STATIC METHODS
	 */
	
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
	 * Prompt the user for a target xml file.
	 *  
	 * @param file  a default file, will be used to display a default choice in the file chooser
	 * @param parent  the {@link Frame} to lock on this dialog
	 * @param logger  a {@link Logger} to report what is happening
	 * @return  the selected file
	 */
	public static File askForFile(File file, Frame parent, Logger logger) {

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, "Save to a XML file", FileDialog.SAVE);
			dialog.setDirectory(file.getParent());
			dialog.setFile(file.getName());
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Save data aborted.\n");
				return null;
			}
			if (!selectedFile.endsWith(".xml"))
				selectedFile += ".xml";
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			JFileChooser fileChooser = new JFileChooser(file.getParent());
			fileChooser.setSelectedFile(file);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);

			int returnVal = fileChooser.showSaveDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Save data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}




	/** 
	 * Read and return an integer attribute from a JDom {@link Element}, and substitute a default value of 0
	 * if the attribute is not found or of the wrong type.
	 */
	public static final int readIntAttribute(Element element, String name, Logger logger) {
		return readIntAttribute(element, name, logger, 0);
	}

	public static final int readIntAttribute(Element element, String name, Logger logger, int defaultValue) {
		int val = defaultValue;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value: "+defaultValue+".\n");
			return val;
		}
		try {
			val = att.getIntValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value: "+defaultValue+".\n");
		}
		return val;
	}

	public static final double readFloatAttribute(Element element, String name, Logger logger) {
		double val = 0;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getFloatValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n"); 
		}
		return val;
	}

	public static final double readDoubleAttribute(Element element, String name, Logger logger) {
		double val = 0;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getDoubleValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n"); 
		}
		return val;
	}

	public static final boolean readBooleanAttribute(Element element, String name, Logger logger) {
		boolean val = false;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getBooleanValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n"); 
		}
		return val;
	}



	/**
	 * Return the mapping in a map that is targeted by a list of keys, in the order given in the list.
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
	 * http://www.rgagnon.com/javadetails/java-0541.html
	 */
	public static String renameFileExtension (String source, String newExtension) {
		String target;
		String currentExtension = getFileExtension(source);

		if (currentExtension.equals("")) {
			target = source + "." + newExtension;
		}
		else {
			target = source.replaceFirst(Pattern.quote("." + currentExtension) 
					+ "$", Matcher.quoteReplacement("." + newExtension));
		}
		return target;
	}

	/**
	 * http://www.rgagnon.com/javadetails/java-0541.html
	 */
	public static String getFileExtension(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');
		if (i > 0 &&  i < f.length() - 1) {
			ext = f.substring(i + 1);
		}
		return ext;
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
	 * Return a copy 3D stack or a 2D slice as an {@link Img} corresponding to the frame number <code>iFrame</code>
	 * in the given 4D or 3D {@link ImagePlus}. The resulting image will be cropped according the cropping
	 * cube set in the {@link Settings} object given.
	 * @param imp  the 4D or 3D source ImagePlus
	 * @param iFrame  the frame number to extract, 0-based
	 * @param iChannel  the channel number to extract, careful: <b>1-based</b>
	 * @param settings  the settings object that will be used to compute the crop rectangle
	 * @return  a 3D or 2D {@link Img} with the single time-point required 
	 */
	//	@SuppressWarnings({ "rawtypes", "unchecked" })
	//	public static Img<? extends RealType<?>> getCroppedSingleFrameAsImage(ImagePlus imp, int iFrame, int iChannel, Settings settings) {
	//		ImageStack stack = imp.getImageStack();
	//		ImageStack frame = new ImageStack(settings.xend-settings.xstart, settings.yend-settings.ystart, stack.getColorModel());
	//		int numSlices = imp.getNSlices();
	//
	//		// ...create the slice by combining the ImageProcessors, one for each Z in the stack.
	//		ImageProcessor ip, croppedIp;
	//		Roi cropRoi = new Roi(settings.xstart, settings.ystart, settings.xend-settings.xstart, settings.yend-settings.ystart);
	//		for (int j = settings.zstart; j <= settings.zend; j++) {
	//			int stackIndex = imp.getStackIndex(iChannel, j, iFrame+1);
	//			ip = stack.getProcessor(stackIndex);
	//			ip .setRoi(cropRoi);
	//			croppedIp = ip.crop();
	//			frame.addSlice(Integer.toString(j + (iFrame * numSlices)), croppedIp);
	//		}
	//
	//		ImagePlus ipSingleFrame = new ImagePlus(imp.getShortTitle()+"-Frame_" + Integer.toString(iFrame + 1), frame);
	//		ipSingleFrame.setCalibration(imp.getCalibration());
	//		Img<? extends RealType> obj =  ImagePlusAdapter.wrap(ipSingleFrame);
	//		Img<? extends RealType<?>> img = (Img<? extends RealType<?>>) obj;
	//		return img;
	//	}

	/**
	 * Return a copy 3D stack or a 2D slice as an {@link Img} corresponding to the frame number <code>iFrame</code>
	 * in the given 4D or 3D {@link ImagePlus}. The resulting image will <u>not</u> be cropped and will have the 
	 * same size in X, Y and Z that of the source {@link ImagePlus}. 
	 * @param imp  the 4D or 3D source ImagePlus
	 * @param iFrame  the frame number to extract, 0-based
	 * @param iChannel  the channel number to extract, careful: <b>1-based</b>
	 * @return  a 3D or 2D {@link Img} with the single time-point required 
	 */
	//	@SuppressWarnings({ "rawtypes", "unchecked" })
	//	public static Img<? extends RealType<?>> getUncroppedSingleFrameAsImage(ImagePlus imp, int iFrame, int iChannel) {
	//		ImageStack stack = imp.getImageStack();
	//		ImageStack frame = new ImageStack(imp.getWidth(), imp.getHeight(), stack.getColorModel());
	//		int numSlices = imp.getNSlices();
	//
	//		// ...create the slice by combining the ImageProcessors, one for each Z in the stack.
	//		ImageProcessor ip;
	//		for (int j = 1; j <= numSlices; j++) {
	//			int stackIndex = imp.getStackIndex(iChannel, j, iFrame+1);
	//			ip = stack.getProcessor(stackIndex);
	//			frame.addSlice(Integer.toString(j + (iFrame * numSlices)), ip.duplicate());
	//		}
	//
	//		ImagePlus ipSingleFrame = new ImagePlus(imp.getShortTitle()+"-Frame_" + Integer.toString(iFrame + 1), frame);
	//		ipSingleFrame.setCalibration(imp.getCalibration());
	//		Img obj =  ImagePlusAdapter.wrap(ipSingleFrame);
	//		Img img = (Img<? extends RealType<?>>) obj;
	//		return img;
	//	}


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
	 * Return the feature values of this Spot collection as a new double array.
	 */
	public static final double[] getFeature(final Collection<SpotImp> spots, final String feature) {
		final double[] values = new double[spots.size()];
		int index = 0;
		for(SpotImp spot : spots) {
			values[index] = spot.getFeature(feature);
			index++;
		}
		return values;
	}

	/**
	 * Build and return a map of {@link SpotFeature} values for the spot collection given.
	 * Each feature maps a double array, with 1 element per {@link Spot}, all pooled
	 * together.
	 */
	public static Map<String, double[]> getSpotFeatureValues(final SpotCollection spots, final List<String> features, final Logger logger) {
		final Map<String, double[]> featureValues = new  ConcurrentHashMap<String, double[]>(features.size());
		if (null == spots || spots.isEmpty())
			return featureValues;
		// Get the total quantity of spot we have
		final int spotNumber = spots.getNSpots();

		final AtomicInteger ai = new AtomicInteger();
		final AtomicInteger progress = new AtomicInteger();
		Thread[] threads = SimpleMultiThreading.newThreads();

		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate collecting spot feature values thread "+ithread) {

				public void run() {

					int index;
					Double val;
					boolean noDataFlag = true;

					for (int i = ai.getAndIncrement(); i < features.size(); i = ai.getAndIncrement()) {

						String feature = features.get(i);

						// Make a double array to comply to JFreeChart histograms
						double[] values = new double[spotNumber];
						index = 0;
						for (Spot spot : spots) {
							val = spot.getFeature(feature);
							if (null == val)
								continue;
							values[index] = val; 
							index++;
							noDataFlag = false;
						}
						if (noDataFlag) {
							featureValues.put(feature, new double[0]);
						} else { 
							featureValues.put(feature, values);
						}

						logger.setProgress(progress.incrementAndGet() / (double) features.size());
					}
				}

			};

		}

		logger.setStatus("Collecting feature values");
		SimpleMultiThreading.startAndJoin(threads);
		logger.setProgress(0);
		logger.setStatus("");
		return featureValues;
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
	 * Ensure an 8-bit gray image is sent to the 3D viewer.
	 * @throws ImgLibException 
	 */
	public static final <T extends RealType<T> & NativeType<T>> ImagePlus[] makeImageForViewer(final Settings<T> settings) throws ImgLibException {

		final ImagePlus origImp = settings.imp;
		origImp.killRoi();
		final ImagePlus imp;

		if (origImp.getType() == ImagePlus.GRAY8)
			imp = origImp;
		else {
			imp = new Duplicator().run(origImp);
			new StackConverter(imp).convertToGray8();
		}

		int nChannels = imp.getNChannels();
		int nSlices = settings.nslices;
		int nFrames = settings.nframes;
		ImagePlus[] ret = new ImagePlus[nFrames];
		int w = imp.getWidth(), h = imp.getHeight();

		ImageStack oldStack = imp.getStack();
		String oldTitle = imp.getTitle();

		for(int i = 0; i < nFrames; i++) {

			ImageStack newStack = new ImageStack(w, h);
			for(int j = 0; j < nSlices; j++) {
				int index = imp.getStackIndex(1, j+1, i+settings.tstart+1);
				Object pixels;
				if (nChannels > 1) {
					imp.setPositionWithoutUpdate(1, j+1, i+1);
					pixels = new ColorProcessor(imp.getImage()).getPixels();
				}
				else
					pixels = oldStack.getPixels(index);
				newStack.addSlice(oldStack.getSliceLabel(index), pixels);
			}
			ret[i] = new ImagePlus(oldTitle	+ " (frame " + i + ")", newStack);
			ret[i].setCalibration(imp.getCalibration().copy());

		}
		return ret;
	}

	/**
	 * Return a String unit for the given dimension. When suitable, the unit is taken from the settings
	 * field, which contains the spatial and time units. Otherwise, default units are used.
	 */
	public static final <T extends RealType<T> & NativeType<T>> String getUnitsFor(final Dimension dimension, final Settings<T> settings) {
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
			units = settings.spaceUnits;
			break;
		case QUALITY:
			units = "Quality";
			break;
		case TIME:
			units = settings.timeUnits;
			break;
		case VELOCITY:
			units = settings.spaceUnits + "/" + settings.timeUnits;
		default:
			break;
		}
		return units;
	}




}