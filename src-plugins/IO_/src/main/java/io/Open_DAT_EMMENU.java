package io;

/**
 * Open .dat files from EMMENU software for FEI electron microscopes.
 * Copyright Albert Cardona. This work is in the public domain.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

public class Open_DAT_EMMENU extends ImagePlus implements PlugIn {

	public void run(final String arg) {
		String path = arg;
		String directory = null;
		String filename = null;
		if (null == path || 0 == path.length()) {
			OpenDialog od = new OpenDialog("Choose .dat file", null);
			directory = od.getDirectory();
			if (null == directory) return; // dialog canceled
			filename = od.getFileName();
			if (!filename.toLowerCase().endsWith(".dat")) {
				IJ.error("Not a .dat file.");
				return;
			}
			path = directory + "/" + filename; // works in Windows too
		} else {
			// the argument is the path
			File file = new File(path);
			directory = file.getParent(); // could be a URL
			filename = file.getName();
			if (directory.startsWith("http:/")) directory = "http://" + directory.substring(6); // the double '//' has been eliminated by the File object call to getParent()
		}

		if (!filename.toLowerCase().endsWith(".dat")) {
			this.width = this.height = -1;
			return;
		}

		if (!directory.endsWith("/")) directory += "/"; // works in windows too

		InputStream is;
		final byte[] buf = new byte[136];
		try {
			if (0 == path.indexOf("http://")) {
				is = new java.net.URL(path).openStream();
			} else {
				is = new FileInputStream(path);
			}
			is.read(buf, 0, 136);
			is.close();
		}
		catch (IOException e) {
			// couldn't open the file for reading
			this.width = this.height = -1;
			return;
		}

		int datatype = (int)buf[3]; // 1-byte, 2-int (16bit), 4-int(32bit, 5-float(32bit), 8-complex(2*float)
		int width = (buf[4]<<24) + (buf[5]<<16) + (buf[6]<<8) + buf[7];
		int height = (buf[8]<<24) + (buf[9]<<16) + (buf[10]<<8) + buf[11];
		int n_images = (buf[12]<<24) + (buf[13]<<16) + (buf[14]<<8) + buf[15];
		byte[] b_msg = new byte[80];
		System.arraycopy(buf, 16, b_msg, 0, 80);
		String comment = new String(b_msg);
		int high_tension = (buf[96]<<24) + (buf[97]<<16) + (buf[98]<<8) + buf[99];
		int spherical_aberration = (buf[100]<<24) + (buf[101]<<16) + (buf[102]<<8) + buf[103]; // in Cs [m m]
		int illum_aperture = (buf[104]<<24) + (buf[105]<<16) + (buf[106]<<8) + buf[107]; // illum. aperture [m rad]
		int magnification = (buf[108]<<24) + (buf[109]<<16) + (buf[110]<<8) + buf[111]; // electron optical magnification [x 1]
		int post_magnification = (buf[112]<<24) + (buf[113]<<16) + (buf[114]<<8) + buf[115]; // [x 0.001]
		int ccd_exposure = (buf[116]<<24) + (buf[117]<<16) + (buf[118]<<8) + buf[119]; // in ms
		int ccd_pixels = (buf[120]<<24) + (buf[121]<<16) + (buf[122]<<8) + buf[123];// in x and y direction
		int ccd_pixel_size = (buf[124]<<24) + (buf[125]<<16) + (buf[126]<<8) + buf[127]; // [m m]
		int image_length = (buf[128]<<24) + (buf[129]<<16) + (buf[130]<<8) + buf[131]; // in nm
		int defocus = (buf[132]<<24) + (buf[133]<<16) + (buf[134]<<8) + buf[135]; // [0.1 nm]
		// prepare big info String
		StringBuffer sb_info = new StringBuffer("Tecnai EMMENU .dat file info:");
		sb_info.append("\ndirectory=").append(directory)
			.append("\nname=").append(filename)
			.append("\ndatatype=").append(datatype)
			.append("\nwidth=").append(width)
			.append("\nheight=").append(height)
			.append("\nn_images=").append(n_images)
			.append("\ncomment=").append(comment)
			.append("\nhigh_tension=").append(high_tension)
			.append("\nspherical_aberration=").append(spherical_aberration)
			.append("\nillum_aperture=").append(illum_aperture)
			.append("\nmagnification=").append(magnification)
			.append("\npost_magnification=").append(post_magnification)
			.append("\nccd_exposure=").append(ccd_exposure)
			.append("\nccd_pixels=").append(ccd_pixels)
			.append("\nccd_pixel_size=").append(ccd_pixel_size)
			.append("\nimage_length=").append(image_length)
			.append("\ndefocus=").append(defocus)
		;

		ImagePlus imp = openRaw(getType(datatype),
					directory, /* or URL directory */
					filename,
					width,
					height,
					512L,
					n_images,
					0,
					false,
					false);
		// gather info
		String info = (String)imp.getProperty("Info");
		if (null == info) info = sb_info.toString();
		else info += "\n" + sb_info.toString();

		// integrate, the HandleExtraFileTypes way
		ImageStack stack = imp.getStack();
		setStack(imp.getTitle(), stack);
		setCalibration(imp.getCalibration());
		setProperty("Info", sb_info.toString());
		setFileInfo(imp.getOriginalFileInfo());

		if (null == arg || 0 == arg.length()) {
			// was opened with a dialog
			this.show();
		}
	}

	private int getType(int datatype) {
		switch (datatype) {
			case 1: return FileInfo.GRAY8;
			case 2: return FileInfo.GRAY16_UNSIGNED; //was: GRAY16_SIGNED and was making ImageJ behave badly when creating images
			case 4: return FileInfo.GRAY32_INT;
			case 5: return FileInfo.GRAY32_FLOAT;
			case 8: return FileInfo.GRAY64_FLOAT;
		}
		// else, error:
		return -1;
	}

	/** Copied and modified from ij.io.ImportDialog. @param imageType must be a static field from FileInfo class. The directory may be an URL directory that contains the image file. */
	static public ImagePlus openRaw(int imageType, String directory, String fileName, int width, int height, long offset, int nImages, int gapBetweenImages, boolean intelByteOrder, boolean whiteIsZero) {
		FileInfo fi = new FileInfo();
		fi.fileType = imageType;
		fi.fileFormat = fi.RAW;
		fi.fileName = fileName;
		if (0 == directory.indexOf("http://")) {
			fi.url = directory; // the ij.io.FileOpener will open a java.net.URL(fi.url).openStream() from it
		} else {
			fi.directory = directory;
		}
		fi.width = width;
		fi.height = height;
		if (offset>2147483647)
			fi.longOffset = offset;
		else
			fi.offset = (int)offset;
		fi.nImages = nImages;
		fi.gapBetweenImages = gapBetweenImages;
		fi.intelByteOrder = intelByteOrder;
		fi.whiteIsZero = whiteIsZero;
		FileOpener fo = new FileOpener(fi);
		try {
			return fo.open(false);
		} catch (Exception e) {
			return null;
		}
	}
}
