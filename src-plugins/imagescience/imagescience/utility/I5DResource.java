package imagescience.utility;

import i5d.Image5D;
import i5d.cal.ChannelCalibration;
import i5d.cal.ChannelDisplayProperties;
import i5d.gui.ChannelControl;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import java.awt.image.ColorModel;

/** Provides Image5D utility methods. Use of this class requires the <a href="http://rsb.info.nih.gov/ij/plugins/image5d.html" target="newbrowser">Image5D</a> package to be installed. */
public class I5DResource {
	
	/** The gray-scale displaying mode. */
	public final static int GRAY = ChannelControl.ONE_CHANNEL_GRAY;
	
	/** The color displaying mode. */
	public final static int COLOR = ChannelControl.ONE_CHANNEL_COLOR;
	
	/** The overlay displaying mode. */
	public final static int OVERLAY = ChannelControl.OVERLAY;
	
	/** The tiled displaying mode. */
	public final static int TILED = ChannelControl.TILED;
	
	/** Default constructor. */
	public I5DResource() { }
	
	/** Sets the displaying mode of an image.
		
		@param image the image.
		
		@param mode the displaying mode. Must be one of {@link #GRAY}, {@link #COLOR}, {@link #OVERLAY}, {@link #TILED}.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object or if {@code mode} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static void mode(final ImagePlus image, final int mode) {
		
		if (!(image instanceof Image5D))
			throw new IllegalArgumentException("Image is not an Image5D object");
		if (mode != GRAY && mode != COLOR && mode != OVERLAY && mode != TILED)
			throw new IllegalArgumentException("Displaying mode is invalid");
		final Image5D img5d = (Image5D)image;
		img5d.setDisplayMode(mode);
	}
	
	/** Returns the density calibration of a specific channel of an image.
		
		@param image the image.
		
		@param channel the channel number. Must be larger than or equal to {@code 1} and less than or equal to the number of channels.
		
		@return the density calibration of the given channel of the given image.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object or if {@code channel} is out of range.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static Calibration density(final ImagePlus image, final int channel) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		img5d.storeCurrentChannelProperties();
		final ChannelCalibration cc = img5d.getChannelCalibration(channel);
		final Calibration c = new Calibration();
		c.setFunction(cc.getFunction(),cc.getCoefficients(),cc.getValueUnit(),cc.isZeroClip());
		
		return c;
	}
	
	/** Indicates whether an image is an instance of {@code Image5D}.
		
		@param image the image whose identity is to be tested.
		
		@return {@code true} if {@code image} is an instance of {@code Image5D}; {@code false} if this is not the case.
	*/
	public static boolean instance(final ImagePlus image) {
		
		return (image instanceof Image5D);
	}
	
	/** Returns the current position of an image in a specific dimension.
		
		@param image the image.
		
		@param dimension the dimension. Must be {@code 0} (x), {@code 1} (y), {@code 2} (ch), {@code 3} (z), or {@code 4} (t).
		
		@return the current position of the given image in the given dimension.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object.
		
		@exception ArrayIndexOutOfBoundsException if {@code dimension} is out of range.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static int position(final ImagePlus image, final int dimension) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		return img5d.getCurrentPosition(dimension);
	}
	
	/** Sets the position of an image in a specific dimension.
		
		@param image the image.
		
		@param dimension the dimension. Must be {@code 0} (x), {@code 1} (y), {@code 2} (ch), {@code 3} (z), or {@code 4} (t).
		
		@param position the position. Should be larger than or equal to {@code 0} and less than {@code N}, where {@code N} is the size of the image in the specified dimension. Values outside this range are simply ignored (no exception is generated).
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object, or if {@code dimension} is out of range.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static void position(final ImagePlus image, final int dimension, final int position) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		img5d.setCurrentPosition(dimension,position);
	}
	
	/** Sets the position of an image in each dimension.
		
		@param image the image.
		
		@param x {@code y} - {@code ch} - {@code z} - {@code t} - the position in, respectively, the x, y, ch, z, and t dimension. Should be larger than or equal to {@code 0} and less than {@code N}, where {@code N} is the size of the image in the corresponding dimension. Values outside this range are simply ignored (no exception is generated).
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static void position(final ImagePlus image, final int x, final int y, final int ch, final int z, final int t) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		img5d.setCurrentPosition(x,y,ch,z,t);
	}
	
	/** Returns the processor of a specific channel of an image.
		
		@param image the image.
		
		@param channel the channel number. Must be larger than or equal to {@code 1} and less than or equal to the number of channels.
		
		@return the processor of the given channel of the given image.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object, or if {@code channel} is out of range.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static ImageProcessor processor(final ImagePlus image, final int channel) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		return img5d.getProcessor(channel);
	}
	
	/** Converts an image to an {@code Image5D} object.
		
		@param image the image to be converted.
		
		@param properties if {@code true}, the density calibration, color model, and window/level settings of {@code image} are copied to every channel of the new {@code Image5D} object; if {@code false}, these properties assume their default values.
		
		@return an {@code Image5D} version of the given image. Returns the given image directly if it is already an {@code Image5D} object. Otherwise it converts the image to a new {@code Image5D} object.
		
		@exception IllegalArgumentException if {@code image} is not a gray-level image or if the ImageJ version is not supported.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static ImagePlus convert(final ImagePlus image, final boolean properties) {
		
		if (image instanceof Image5D) return image;
		
		final int xdim = image.getWidth();
		final int ydim = image.getHeight();
		final int cdim = image.getNChannels();
		final int zdim = image.getNSlices();
		final int tdim = image.getNFrames();
		final Image5D img5d = new Image5D(image.getTitle(),image.getType(),xdim,ydim,cdim,zdim,tdim,false);
		final Object[] slices = image.getImageStack().getImageArray();
		final int[] pos = new int[] {0,0,0,0,0};
		int tzdimcdim, zcdim;
		for (int t=0; t<tdim; ++t) {
			pos[4] = t; tzdimcdim = t*zdim*cdim;
			for (int z=0; z<zdim; ++z) {
				pos[3] = z; zcdim = z*cdim;
				for (int c=0; c<cdim; ++c) {
					pos[2] = c; img5d.setCurrentPosition(pos);
					img5d.setPixels(slices[tzdimcdim + zcdim + c]);
				}
			}
		}
		pos[2] = pos[3] = pos[4] = 0;
		img5d.setCurrentPosition(pos);
		
		if (properties) {
			img5d.setCalibration(image.getCalibration());
			img5d.storeCurrentChannelProperties();
			final ChannelCalibration cc = img5d.getChannelCalibration(1);
			final ImageProcessor ip = image.getProcessor();
			final ColorModel cm = ip.getColorModel();
			for (int c=1; c<=cdim; ++c) {
				img5d.setChannelCalibration(c,cc.copy());
				final ChannelDisplayProperties cdps = img5d.getChannelDisplayProperties(c);
				cdps.setMinValue(ip.getMin());
				cdps.setMaxValue(ip.getMax());
				cdps.setColorModel(cm);
			}
			img5d.restoreCurrentChannelProperties();
		}
		
		img5d.setDefaultChannelNames();
		
		return img5d;
	}
	
	/** Sets the minimum and maximum displayed value of all channels of an image.
		
		@param image the image whose minimum and maximum displayed values are to be set.
		
		@param min the minimum value to be displayed.
		
		@param max the maximum value to be displayed.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static void minmax(final ImagePlus image, final double min, final double max) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		final int chdim = img5d.getNChannels();
		for (int c=1; c<=chdim; ++c) {
			final ChannelDisplayProperties cdps = img5d.getChannelDisplayProperties(c);
			cdps.setMinValue(min); cdps.setMaxValue(max);
			img5d.restoreChannelProperties(c);
		}
		img5d.restoreCurrentChannelProperties();
	}
	
	/** Transfers all channel properties from source to destination image. These include both the display properties and the calibration properties.
		
		@param source the source image from which channel properties are to be copied.
		
		@param destination the destination image to which channel properties are to be copied.
		
		@exception IllegalArgumentException if {@code source} or {@code destination} is not an {@code Image5D} object, or if they do not have an equal number of channels.
		
		@exception NullPointerException if {@code source} or {@code destination} is {@code null}.
	*/
	public static void transfer(final ImagePlus source, final ImagePlus destination) {
		
		if (!(source instanceof Image5D)) throw new IllegalArgumentException("Source is not an Image5D object");
		final Image5D src5d = (Image5D)source;
		
		if (!(destination instanceof Image5D)) throw new IllegalArgumentException("Destination is not an Image5D object");
		final Image5D des5d = (Image5D)destination;
		
		final int schdim = src5d.getNChannels();
		final int dchdim = des5d.getNChannels();
		if (schdim != dchdim) throw new IllegalArgumentException("Source and destination have different numbers of channels");
		
		src5d.storeCurrentChannelProperties();
		for (int c=1; c<=schdim; ++c) {
			des5d.setChannelDisplayProperties(c,src5d.getChannelDisplayProperties(c).copy());
			des5d.setChannelCalibration(c,src5d.getChannelCalibration(c).copy());
			des5d.restoreChannelProperties(c);
		}
		des5d.restoreCurrentChannelProperties();
		des5d.setDisplayMode(src5d.getDisplayMode());
	}
	
	/** Transfers the properties of specific channels from source to destination image. These include both the display properties and the calibration properties.
		
		@param source the source image from which channel properties are to be copied.
		
		@param destination the destination image to which channel properties are to be copied.
		
		@param channels a double array containing the source and destination channel indices. The {@code channels[0]} array contains the channel indices of the source image, and the {@code channels[1]} array the corresponding channel indices of the destination image. Channel indices may range from {@code 1} to the number of channels (inclusive).
		
		@exception IllegalArgumentException if {@code source} or {@code destination} is not an {@code Image5D} object, if the {@code channels[0]} and {@code channels[1]} arrays have different lengths, or if any of the indices in these arrays is out of range.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public static void transfer(final ImagePlus source, final ImagePlus destination, final int[][] channels) {
		
		if (!(source instanceof Image5D)) throw new IllegalArgumentException("Source is not an Image5D object");
		final Image5D src5d = (Image5D)source;
		
		if (!(destination instanceof Image5D)) throw new IllegalArgumentException("Destination is not an Image5D object");
		final Image5D des5d = (Image5D)destination;
		
		final int[] si = channels[0];
		final int[] di = channels[1];
		final int len = si.length;
		if (di.length != len) throw new IllegalArgumentException("Source and destination channel index arrays have different lengths");
		
		src5d.storeCurrentChannelProperties();
		for (int i=0; i<len; ++i) {
			des5d.setChannelDisplayProperties(di[i],src5d.getChannelDisplayProperties(si[i]).copy());
			des5d.setChannelCalibration(di[i],src5d.getChannelCalibration(si[i]).copy());
			des5d.restoreChannelProperties(di[i]);
		}
		des5d.restoreCurrentChannelProperties();
		des5d.setDisplayMode(src5d.getDisplayMode());
	}
	
	/** Transfers the properties of a specific source image channel to the current processor and stack of a destination image. These include both the display properties and the calibration properties.
		
		@param source the source image from which channel properties are to be copied.
		
		@param destination the destination image to which channel properties are to be copied.
		
		@param channel the source channel index. Must be larger than or equal to {@code 1} and less than or equal to the number of channels.
		
		@exception IllegalArgumentException if {@code source} is not an {@code Image5D} object, or if {@code channel} is out of range.
		
		@exception NullPointerException if {@code source} or {@code destination} is {@code null}.
	*/
	public static void transfer(final ImagePlus source, final ImagePlus destination, final int channel) {
		
		if (!(source instanceof Image5D)) throw new IllegalArgumentException("Source is not an Image5D object");
		final Image5D src5d = (Image5D)source;
		
		final ChannelCalibration srccal = src5d.getChannelCalibration(channel);
		final Calibration descal = destination.getCalibration();
		descal.setFunction(srccal.getFunction(),srccal.getCoefficients(),srccal.getValueUnit(),srccal.isZeroClip());
		
		final ChannelDisplayProperties srcdps = src5d.getChannelDisplayProperties(channel);
		final ImageProcessor desip = destination.getProcessor();
		final ImageStack desis = destination.getStack();
		if (srcdps.isDisplayedGray() || src5d.isDisplayAllGray()) {
			final ColorModel gcm = LookUpTable.createGrayscaleColorModel(false);
			desip.setColorModel(gcm);
			desis.setColorModel(gcm);
		} else {
			desip.setColorModel(srcdps.getColorModel());
			desis.setColorModel(srcdps.getColorModel());
		}
		desip.setMinAndMax(srcdps.getMinValue(),srcdps.getMaxValue());
		if(srcdps.getMinThreshold() != ImageProcessor.NO_THRESHOLD)
			desip.setThreshold(srcdps.getMinThreshold(),srcdps.getMaxThreshold(),srcdps.getLutUpdateMode());
	}
	
	/** Disables the density calibration of all channels of an image.
		
		@param image the image whose density calibrations are to be disabled.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static void nodensity(final ImagePlus image) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		final int chdim = img5d.getNChannels();
		for (int c=1; c<=chdim; ++c)
			img5d.getChannelCalibration(c).disableDensityCalibration();
		img5d.restoreCurrentChannelProperties();
	}
	
	/** Disables the color model of all channels of an image. The color model of each channel is set to the gray-scale color model.
		
		@param image the image whose color models are to be disabled.
		
		@exception IllegalArgumentException if {@code image} is not an {@code Image5D} object.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public static void nocolor(final ImagePlus image) {
		
		if (!(image instanceof Image5D)) throw new IllegalArgumentException("Image is not an Image5D object");
		final Image5D img5d = (Image5D)image;
		
		final int chdim = img5d.getNChannels();
		final ColorModel gcm = LookUpTable.createGrayscaleColorModel(false);
		for (int c=1; c<=chdim; ++c) {
			img5d.getChannelDisplayProperties(c).setColorModel(gcm);
			img5d.restoreChannelProperties(c);
		}
		img5d.restoreCurrentChannelProperties();
	}
	
}
