/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/** This class contains methods I would like to see incorporated into
    HandleExtraFileTypes, or elsewhere.  The main features are:

     * An open method that returns an array of ImagePlus objects,
       one per channel, without calling show() on any of them.

     * Files are identified as particular types by their content,
       (magic numbers, etc.) rather then their file extension.  (The
       exception to this is the TorstenRaw_GZ_Reader, since the image
       files are raw and have no distinctive header.)

     * The method doesn't rely on plugins being present at compile
       time - instead it uses reflection to check whether the required
       classes are available.  This is a bit ugly, but means that this
       could be incorporated into the main ImageJ source code more
       easily.

    The types of file that should be coped with properly at the
    moment are listed below:

      Tested file types:

	- Zeiss LSM files (using LSM_Toolbox rather than LSM_Reader)
	- Leica SP files (using the Leica_SP_Reader plugin)
	- Ordinary TIFF files (using the default ImageJ opener)
	- AmiraMesh files (using the AmiraMeshReader plugin)

      Untested file types (please send me example files!):

	- Biorad PIC files (using the Biorad_Reader plugin)
	- IPLab files (using the IPLab_Reader plugin)
	- Packard InstantImager format (.img) files
	- Gatan Digital Micrograph DM3 handler (DM3_Reader plugin)

    Mark Longair <mark-imagej@longair.net>

 */

public class BatchOpener {

	public static class NoSuchChannelException extends Exception {

		NoSuchChannelException(String message) {
			super(message);
		}
	}

	public static class ImageLoaderException extends Exception {

		ImageLoaderException(String message) {
			super(message);
		}
	}

	/**
	 * Returns an ImagePlus corresponding to the first (and possibly only)
	 * channel in the image file, without calling show() on the ImagePlus
	 * object.  If the file contains no channels, or the file is not found
	 * this returns null.
	 *
	 * @param  the path of the image file to open
	 */
	public static ImagePlus openFirstChannel(String path) {
		ImagePlus [] channels = BatchOpener.open(path);
		if( channels == null )
			return null;
		if( channels.length == 0 )
			return null;
		return channels[0];
	}

	/**
	 * Returns an ImagePlus corresponding to the channel with the specified
	 * index in the image file.  If that channel is not found in the image,
	 * a util.NoSuchChannelException is thrown.  If the file cannot be
	 * found or there is any other error in opening, null is returned.
	 *
	 * @param path   the path of the image file to open
	 * @param i      the (zero-indexed) index of the channel to return
	 */
	public static ImagePlus openParticularChannel(String path, int i) throws NoSuchChannelException {
		ImagePlus [] channels = BatchOpener.open(path);
		if( channels == null )
			return null;
		if( i >= channels.length || i < 0 ) {
			throw new NoSuchChannelException("No channel "+i+
				" in file "+path+", which has "+channels.length+
				" channels.  (Channel indices start at zero.)");
		}
		return channels[i];
	}

	/**
	 * Returns an array of ImagePlus objects corresponding to all of the
	 * channels in the image file.  If the file cannot be
	 * found or there is any other error in opening, null is returned.
	 *
	 * @param path   the path of the image file to open
	 */
	public static ImagePlus[] open(String path) {
		ChannelsAndLoader cal;
		try {
			cal=openToChannelsAndLoader(path);
		} catch( ImageLoaderException e ) {
			return null;
		}
		if( cal == null )
			return null;
		return cal.channels;
	}

	/** A helper class to return the array of ImagePlus as well as an
	    indication of the file loader that was used. */
	public static class ChannelsAndLoader {
		public ChannelsAndLoader( ImagePlus [] channels, String loaderUsed ) {
			this.channels = channels;
			this.loaderUsed = loaderUsed;
		}
		public ImagePlus [] channels;
		public String loaderUsed;
	}

	/**
	 * Returns an object with (a) references to the array of ImagePlus
	 * objects corresponding to all of the channels in the image
	 * file and (b) a string indicating which loader was used.
	 *
	 * If the file cannot be found or there is any other
	 * error in opening, null is returned.
	 *
	 * @param path        the path of the image file to open
	 */
	public static ChannelsAndLoader openToChannelsAndLoader(String path) throws ImageLoaderException {

		String loaderUsed = null;

		File file = new File(path);

		/* Read a few bytes from the beginning of the file
		   into a buffer in order to look for magic
		   numbers: */

		byte[] buf = new byte[132];

		InputStream is;
		try {
			is = new FileInputStream(path);
			is.read(buf, 0, 132);
			is.close();
		} catch (IOException e) {
			return null;
		}

		String name = file.getName();
		String nameLowerCase = name.toLowerCase();

		String directory = file.getParent();

		// Test if this is a TIFF-based file of some kind:
		byte[] tiffMagicIntel = {73, 73, 42, 0};
		byte[] tiffMagicMotorola = {77, 77, 0, 42};

		byte[] firstFour = new byte[4];
		System.arraycopy(buf, 0, firstFour, 0, 4);

		boolean tiffLittleEndian = Arrays.equals(tiffMagicIntel, firstFour);
		boolean tiffBigEndian = Arrays.equals(tiffMagicMotorola, firstFour);

		if (tiffLittleEndian || tiffBigEndian) {

			RandomAccessFile in = null;

			try {
				in = new RandomAccessFile(path, "r");
			} catch (IOException e) {
				return null;
			}

			if (in == null) {
				// throw new RuntimeException("Can (no longer!) open the file '" + path + "'");
				return null;
			}

			boolean isLSM;
			try {
				isLSM = findLSMTag(in, tiffLittleEndian);
			} catch (IOException e) {
				return null;
			}

			if (isLSM) {

				try {
					in.close();
				} catch( IOException e ) {
					// throw new RuntimeException("Couldn't close the LSM file.");
					return null;
				}

				loaderUsed = "LSM_Toolbox";

				// Zeiss Confocal LSM 510 image file (.lsm) handler
				// Insist on LSM_Toolbox for this rather than LSM_Reader,
				// which doesn't have an appropriate open method.
				// http://imagejdocu.tudor.lu/Members/ppirrotte/lsmtoolbox
				ClassLoader loader = IJ.getClassLoader();
				if (loader == null)
					throw new RuntimeException("IJ.getClassLoader() failed (!)");

				try {

					/* This unfortunate ugliness is because at
					   compile time we can't be sure that the
					   LSM_Toolbox jar is in the classpath. */

					Class<?> c = loader.loadClass("org.imagearchive.lsm.toolbox.Reader");
					Object newInstance = c.newInstance();

					/* This version of open doesn't show() them... */
					Class [] parameterTypes = { String.class,
								    String.class,
								    Boolean.TYPE,
								    Boolean.TYPE,
								    Boolean.TYPE };

					Method m = c.getMethod( "open", parameterTypes );
					Object [] parameters = new Object[5];
					parameters[0] = file.getParent();
					parameters[1] = file.getName();
					parameters[2] = false;
					parameters[3] = false;
					parameters[4] = false;

					ImagePlus [] result;
					Object invokeResult = m.invoke(newInstance,parameters);
					if( invokeResult instanceof ImagePlus &&
						((ImagePlus)invokeResult).isComposite()) {
						CompositeImage composite = (CompositeImage)invokeResult;
						result = splitChannelsToArray(composite,true);
					} else if( invokeResult instanceof ImagePlus ) {
						result = new ImagePlus[1];
						result[0] = (ImagePlus)invokeResult;
					} else
						result = (ImagePlus [])invokeResult;

					return new ChannelsAndLoader(result,loaderUsed);

				} catch (IllegalArgumentException e) {
					throw new ImageLoaderException("There was an illegal argument when trying to invoke the LSM_Toolbox reader: " + e);
				} catch (InvocationTargetException e) {
					Throwable realException = e.getTargetException();
					throw new ImageLoaderException("There was an exception thrown by the LSM_Toolbox plugin: " + realException);
				} catch (ClassNotFoundException e) {
					throw new ImageLoaderException("The LSM_Toolbox plugin was not found: " + e);
				} catch (InstantiationException e) {
					throw new ImageLoaderException("Failed to instantiate the LSM_Toolbox reader: " + e);
				} catch ( IllegalAccessException e ) {
					throw new ImageLoaderException("IllegalAccessException when trying to create an instance of the LSM_Toolbox reader: "+e);
				} catch (NoSuchMethodException e) {
					throw new ImageLoaderException("There was a NoSuchMethodException when trying to invoke the LSM_Toolbox reader: " + e);
				} catch (SecurityException e) {
					throw new ImageLoaderException("There was a SecurityException when trying to invoke the LSM_Toolbox reader: " + e);
				}

				/* Unreachable...  But we can't put
				   "assert false;" here, because it
				   warns that the statement is
				   unreachable.  sigh... */
			}

			/* Now test to see if this is a Leica TIFF, which
			   unfortunately seems to involve seeking to near the
			   end of the file.  This code is copied from
			   HandleExtraFileTypes */

			byte[] leicaBytes = new byte[44];
			long seekTo = -1;

			try {
				seekTo = in.length() - 1658;
				in.seek(seekTo);
				in.readFully(leicaBytes);
			} catch( IOException e ) {
				IJ.error("Couldn't seek to "+seekTo+" in "+path);
				return null;
			}

			String leicaString = new String(leicaBytes);

			if (leicaString.equals("Leica Lasertechnik GmbH, " +
					       "Heidelberg, Germany")) {

				try {
					in.close();
				} catch( IOException e ) {
					IJ.error("Couldn't close the Leica TIFF file.");
					return null;
				}

				/* Then this is a Leica TIFF file.  Look for the VIB
				   Leica_SP_Reader plugin, which allows us to get an
				   ImagePlus for each channel. */

				ClassLoader loader = IJ.getClassLoader();
				if (loader == null) {
					IJ.error("IJ.getClassLoader() failed (!)");
					return null;
				}

				try {

					loaderUsed = "Leica_SP_Reader";

					/* This unfortunate ugliness is because at
					   compile time we can't be sure that
					   leica.Leica_SP_Reader is in the classpath,
					   but we can't just use runPlugin, since we
					   need to call particular methods*/

					Class<?> c = loader.loadClass("leica.Leica_SP_Reader");
					Object newInstance = c.newInstance();

					Class [] parameterTypes = { String.class };
					Object [] parameters = new Object[1];
					parameters[0] = path;
					Method m = c.getMethod( "run", parameterTypes );
					m.invoke(newInstance,parameters);

					/* That should have loaded the file or
					   thrown an IOException. */

					parameterTypes = new Class[0];
					parameters = new Object[0];
					m = c.getMethod("getNumberOfChannels", parameterTypes);
					Integer n=(Integer)m.invoke(newInstance,parameters);

					if( n < 1 ) {
						IJ.error("Error: got "+n+" channels from "+path+" with the Leica SP Reader");
						return null;
					}

					ImagePlus [] result = new ImagePlus[n];

					for( int i = 0; i < n; ++i ) {
						parameterTypes = new Class[1];
						parameterTypes[0] = Integer.TYPE;
						parameters = new Object[1];
						parameters[0] =  new Integer(i);
						m = c.getMethod("getImage", parameterTypes);
						result[i] = (ImagePlus)m.invoke(newInstance,parameters);
					}

					return new ChannelsAndLoader(result,loaderUsed);

				} catch (IllegalArgumentException e) {
					IJ.error("There was an illegal argument when trying to invoke a method on the Leica SP Reader plugin: " + e);
				} catch (InvocationTargetException e) {
					Throwable realException = e.getTargetException();
					IJ.error("There was an exception thrown by the Leica SP Reader plugin: " + realException);
				} catch (ClassNotFoundException e) {
					IJ.error("The Leica SP Reader plugin was not found: " + e);
				} catch (InstantiationException e) {
					IJ.error("Failed to instantiate the Leica SP Reader plugin: " + e);
				} catch ( IllegalAccessException e ) {
					IJ.error("IllegalAccessException when trying the Leica SP Reader plugin: "+e);
				} catch (NoSuchMethodException e) {
					IJ.error("Couldn't find a method in the Leica SP Reader plugin: " + e);
				} catch (SecurityException e) {
					IJ.error("There was a SecurityException when trying to invoke a method of the Leica SP Reader plugin: " + e);
				}

				return null;
			}

			try {
				in.close();
			} catch( IOException e ) {
				IJ.error("Couldn't close the file.");
				return null;
			}

			// Use the default opener:
			loaderUsed = "ImageJ TIFF";
			ImagePlus invokeResult = IJ.openImage(path);
			ImagePlus[] result;

			if( invokeResult instanceof ImagePlus &&
				((ImagePlus)invokeResult).isComposite()) {
				CompositeImage composite = (CompositeImage)invokeResult;
				result = splitChannelsToArray(composite,true);
			} else {
				result = new ImagePlus[1];
				result[0] = (ImagePlus)invokeResult;
			}

			return new ChannelsAndLoader(result,loaderUsed);
		}

		ImagePlus imp = null;

		// MHL: the code below is essentially the same as in
		// HandleExtraFileTypes.  I've just dropped those
		// types that open and show the images themselves,
		// since they're probably not useful for non-GUI use.
		// I've also dropped any tests that are based on file
		// extensions, which are generally much less
		// trustworthy than the magic numbers at the beginning
		// of the file.

		// GJ: added Biorad PIC confocal file handler
		// Note that the Biorad_Reader plugin extends the ImagePlus class,
		// which is why the IJ.runPlugIn() call below returns an ImagePlus object.
		// ------------------------------------------
		// These make 12345 if you read them as the right kind of short
		// and should have this value in every Biorad PIC file
		if (buf[54] == 57 && buf[55] == 48) {
			// Ok we've identified the file type
			// Now load it using the relevant plugin
			loaderUsed = "Biorad_Reader";
			imp = (ImagePlus) IJ.runPlugIn("Biorad_Reader", path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return new ChannelsAndLoader(i,loaderUsed);
		}

		// GJ: added Gatan Digital Micrograph DM3 handler
		// Note that the DM3_Reader plugin extends the ImagePlus class,
		// which is why the IJ.runPlugIn() call below returns an ImagePlus object.
		// ----------------------------------------------
		// These make an int value of 3 which is the DM3 version number
		if (buf[0] == 0 && buf[1] == 0 && buf[2] == 0 && buf[3] == 3) {
			// Ok we've identified the file type - now load it
			loaderUsed = "DM3_Reader";
			String [] possibleClassNames = {
				"DM3_Reader",
				"io.DM3_Reader" };
			imp = findAndRunPlugIn(possibleClassNames,path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return new ChannelsAndLoader(i,loaderUsed);
		}

		// IPLab file handler
		// Note that the IPLab_Reader plugin extends the ImagePlus class.
		// Little-endian IPLab files start with "iiii" or "mmmm".
		if ((buf[0] == 105 && buf[1] == 105 && buf[2] == 105 && buf[3] == 105) || (buf[0] == 109 && buf[1] == 109 && buf[2] == 109 && buf[3] == 109)) {
			loaderUsed = "IPLab_Reader";
			String [] possibleClassNames = {
				"IPLab_Reader",
				"io.IPLab_Reader" };
			imp = findAndRunPlugIn(possibleClassNames,path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return new ChannelsAndLoader(i,loaderUsed);
		}

		// Amira file handler
		if (buf[0] == 0x23 && buf[1] == 0x20 && buf[2] == 0x41 &&
		    buf[3] == 0x6d && buf[4] == 0x69 && buf[5] == 0x72 &&
		    buf[6] == 0x61 && buf[7] == 0x4d && buf[8] == 0x65 &&
		    buf[9] == 0x73 && buf[10] == 0x68 && buf[11] == 0x20) {
			loaderUsed = "AmiraMeshReader_";
			ImagePlus[] i = new ImagePlus[1];
			imp = (ImagePlus) IJ.runPlugIn("AmiraMeshReader_", path);
			if (imp == null) {
				return null;
			}
			i[0] = imp;
			return new ChannelsAndLoader(i,loaderUsed);
		}

		// Packard InstantImager format (.img) handler -> check HERE before Analyze check below!
		// Note that the InstantImager_Reader plugin extends the ImagePlus class.
		// Check extension and signature bytes KAJ_
		if (buf[0] == 75 && buf[1] == 65 && buf[2] == 74 && buf[3] == 0) {
			loaderUsed = "InstantImager_Reader";
			String [] possibleClassNames = {
				"InstantImager_Reader",
				"io.InstantImager_Reader" };
			imp = findAndRunPlugIn(possibleClassNames,path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = imp;
			return new ChannelsAndLoader(i,loaderUsed);
		}

		/* Try to detect if a file is output from the
		   registration algorithm used by the registration
		   algorithm from http://flybrain.stanford.edu/

		   This is slightly awkward, since the image files
		   themselves are just raw data files.  We check that
		   the filename ends in .bin or .bin.gz and check that
		   the name of the directory that contains the file is
		   alongside a corresponding .study directory that has
		   an "images" file with metadata.

		   This code is taken from the plugin's
		   "getHeaderFile" method.
		 */

		if (name.endsWith(".bin") || name.endsWith(".bin.gz")) {
			File imageFile = new File(directory,name);
			File imageDir = new File(imageFile.getParent());
			File commonDir = new File(imageDir.getParent());
			String[] list = commonDir.list();
			if (list!=null) {
				for (int e=0; e<list.length; e++) {
					File f = new File(commonDir.getPath(),list[e]);
					if (f.isDirectory() &&
					    f.getName().equals(imageDir.getName()+".study")) {
						File headerFile=new File(f.getPath(),"images");
						if (headerFile.exists()){
							// Now we can run the plugin:
							loaderUsed = "TorstenRaw_GZ_Reader";
							ImagePlus[] i = new ImagePlus[1];
							// But we have to find it.  Look for the class names
							// io.TorstenRaw_GZ_Reader or TorstenRaw_GZ_Reader
							String [] possibleClassNames = {
								"io.TorstenRaw_GZ_Reader",
								"TorstenRaw_GZ_Reader" };
							imp = findAndRunPlugIn(possibleClassNames,path);
							if( imp == null ) {
								return null;
							} else {
								i[0] = imp;
								return new ChannelsAndLoader(i,loaderUsed);
							}
						}
					}
				}
			}
		}

		// [FIXME: add detection of more file types here]

		return null;
	}

	static private ImagePlus findAndRunPlugIn( String [] possibleClassNames, String path ) {
		ImagePlus imp = null;
		for( int si = 0; si < possibleClassNames.length; ++si ) {
			String className = possibleClassNames[si];
			try {
				Class c = Class.forName(className);
				PlugIn p = (PlugIn)c.newInstance();
				p.run(path);
				imp = (ImagePlus)p;
				break;
			} catch( ClassNotFoundException cnfe ) {
			} catch( InstantiationException ie ) {
			} catch( IllegalAccessException iae ) { }
		}
		return imp;
	}

	/*
           Hopefully this will go into ImageJ so it doesn't need to be
	   here, see:

             http://www.nabble.com/Re%3A-How-do-I-disable-the-automatic-generation-of-Hyperstacks--p18027821.html
	*/

	public static ImagePlus[] splitChannelsToArray(ImagePlus imp, boolean closeAfter) {
		if(!imp.isComposite()) {
			String error="splitChannelsToArray was called "+
				"on a non-composite image";
			IJ.error(error);
			return null;
		}
		int width = imp.getWidth();
		int height = imp.getHeight();
		int channels = imp.getNChannels();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();
		int bitDepth = imp.getBitDepth();
		int size = slices*frames;
		ImagePlus[] result=new ImagePlus[channels];
		FileInfo fi = imp.getOriginalFileInfo();
		HyperStackReducer reducer = new HyperStackReducer(imp);
		for (int c=1; c<=channels; c++) {
			ImageStack stack2 = new ImageStack(width, height, size); // create empty stack
			stack2.setPixels(imp.getProcessor().getPixels(), 1); // can't create ImagePlus will null 1st image
			ImagePlus imp2 = new ImagePlus("C"+c+"-"+imp.getTitle(), stack2);
			stack2.setPixels(null, 1);
			imp.setPosition(c, 1, 1);
			imp2.setDimensions(1, slices, frames);
			reducer.reduce(imp2);
			imp2.setOpenAsHyperStack(true);
			imp2.setFileInfo(fi);
			result[c-1]=imp2;
		}
		imp.changes = false;
		if (closeAfter)
			imp.close();
		return result;
	}

	private static boolean findLSMTag(RandomAccessFile in, boolean littleEndian) throws IOException {
		return findTag(34412, in, littleEndian);
	}
	// This is a stripped down version of TiffDecoder.getTiffInfo() that just
	// looks for an particular TIFF tag...

	private static boolean findTag(long tagToLookFor, RandomAccessFile in, boolean littleEndian) throws IOException {

		int byteOrder = in.readShort();
		int magicNumber = getShort(in, littleEndian); // 42
		int offset = getInt(in, littleEndian);
		if (magicNumber != 42) {
			IJ.error("Not really a TIFF file (BUG: should have been detected earlier.)");
			// FIXME: throw an exception...
		}

		if (offset < 0) {
			IJ.error("TIFF file probably corrupted: offset is negative");
			return false;
		}

		while (offset > 0) {

			in.seek(offset);

			// Get Image File Directory data
			int tag;
			int fieldType;
			int count;
			int value;
			int nEntries = getShort(in, littleEndian);
			if (nEntries < 1 || nEntries > 1000) {
				return false;
			}
			for (int i = 0; i < nEntries; i++) {
				tag = getShort(in, littleEndian);
				fieldType = getShort(in, littleEndian);
				count = getInt(in, littleEndian);
				value = getValue(in, littleEndian, fieldType, count);
				if (tag == tagToLookFor) {
					return true;
				}
			}
			offset = getInt(in, littleEndian);
		}
		return false;
	}

	private static int getInt(RandomAccessFile in, boolean littleEndian) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		if (littleEndian) {
			return (b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0);
		} else {
			return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
		}
	}

	private static int getShort(RandomAccessFile in, boolean littleEndian) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if (littleEndian) {
			return (b2 << 8) + b1;
		} else {
			return (b1 << 8) + b2;
		}
	}
	private static final int TIFF_FIELD_TYPE_SHORT = 3;
	private static final int TIFF_FIELD_TYPE_LONG = 4;

	private static int getValue(RandomAccessFile in, boolean littleEndian, int fieldType, int count) throws IOException {
		int value = 0;
		int unused;
		if (fieldType == TIFF_FIELD_TYPE_SHORT && count == 1) {
			value = getShort(in, littleEndian);
			unused = getShort(in, littleEndian);
		} else {
			value = getInt(in, littleEndian);
		}
		return value;
	}
}
