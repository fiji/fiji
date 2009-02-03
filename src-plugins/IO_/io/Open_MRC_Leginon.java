package io;

/**
 * Open .mrc files from numerous sources, including Leginon (software for automated imaging in FEI electron microscopes, see http://ami.scripps.edu )
 * Copyright Albert Cardona. This work is in the public domain.
 *
 * The format is described in http://bio3d.colorado.edu/imod/doc/mrc_format.txt:

 The MRC file format used by IMOD.

 The MRC header. length 1024 bytes

 SIZE DATA    NAME	Description

 4    int     nx;	Number of Columns
 4    int     ny;        Number of Rows
 4    int     nz;        Number of Sections.

 4    int     mode;      Types of pixel in image.  Values used by IMOD:
 0 = unsigned bytes,
 1 = signed short integers (16 bits),
 2 = float,
 3 = short * 2, (used for complex data)
 4 = float * 2, (used for complex data)
 6 = unsigned 16-bit integers (non-standard)
 16 = unsigned char * 3 (for rgb data, non-standard)

 4    int     nxstart;     Starting point of sub image.
 4    int     nystart;
 4    int     nzstart;

 4    int     mx;         Grid size in X, Y, and Z
 4    int     my;
 4    int     mz;

 4    float   xlen;       Cell size; pixel spacing = xlen/mx
 4    float   ylen;
 4    float   zlen;

 4    float   alpha;      cell angles
 4    float   beta;
 4    float   gamma;

 Ignored by imod.
 4    int     mapc;       map column  1=x,2=y,3=z.
 4    int     mapr;       map row     1=x,2=y,3=z.
 4    int     maps;       map section 1=x,2=y,3=z.

 These need to be set for proper scaling of
 non byte data.
 4    float   amin;       Minimum pixel value.
 4    float   amax;       Maximum pixel value.
 4    float   amean;      Mean pixel value.

 2    short   ispg;       image type
 2    short   nsymbt;     space group number
 4    int     next;       number of bytes in extended header
 2    short   creatid;    Creator ID
 30   ---     extra data (not used)

 These two values specify the structure of data in the
 extended header; their meaning depend on whether the
 extended header has the Agard format, a series of
 4-byte integers then real numbers, or has data
 produced by SerialEM, a series of short integers.
 SerialEM stores a float as two shorts, s1 and s2, by:
 value = (sign of s1)*(|s1|*256 + (|s2| modulo 256))
 * 2**((sign of s2) * (|s2|/256))
 2    short   nint;       Number of integers per section (Agard format) or
 number of bytes per section (SerialEM format)
 2    short   nreal;      Number of reals per section (Agard format) or
 flags for which types of short data (SerialEM format):
 1 = tilt angle * 100  (2 bytes)
 2 = piece coordinates for montage  (6 bytes)
 4 = Stage position * 25    (4 bytes)
 8 = Magnification / 100 (2 bytes)
16 = Intensity * 25000  (2 bytes)
	32 = Exposure dose in e-/A2, a float in 4 bytes
	128, 512: Reserved for 4-byte items
	64, 256, 1024: Reserved for 2-byte items
	If the number of bytes implied by these flags does
	not add up to the value in nint, then nint and nreal
	are interpreted as ints and reals per section

	28   ---     extra data (not used)
	Explanation of type of data.
	2    short   idtype;  ( 0 = mono, 1 = tilt, 2 = tilts, 3 = lina, 4 = lins)
	2    short   lens;
	2    short   nd1;	for idtype = 1, nd1 = axis (1, 2, or 3)
	2    short   nd2;
	2    short   vd1;                       vd1 = 100. * tilt increment
	2    short   vd2;                       vd2 = 100. * starting angle

	Used to rotate model to match new rotated image.
	24   float   tiltangles[6];  0,1,2 = original:  3,4,5 = current

	OLD-STYLE MRC HEADER - IMOD 2.6.19 and below:
	2    short   nwave;     # of wavelengths and values
	2    short   wave1;
	2    short   wave2;
	2    short   wave3;
	2    short   wave4;
	2    short   wave5;

	4    float   zorg;      Origin of image.  Used to auto translate model
	4    float   xorg;      to match a new image that has been translated.
	4    float   yorg;

	NEW-STYLE MRC image2000 HEADER - IMOD 2.6.20 and above:
	4    float   xorg;      Origin of image.  Used to auto translate model
	4    float   yorg;      to match a new image that has been translated.
	4    float   zorg;

	4    char    cmap;      Contains "MAP "
	4    char    stamp;     First byte has 17 for big- or 68 for little-endian
	4    float   rms;

	ALL HEADERS:
	4    int     nlabl;  	Number of labels with useful data.
	800  char[10][80]    	10 labels of 80 charactors.
	------------------------------------------------------------------------

	Total size of header is 1024 bytes plus the size of the extended header.

	Image data follows with the origin in the lower left corner,
	looking down on the volume.

	The size of the image is nx * ny * nz * (mode data size).


	o translate model
	4    float   xorg;      to match a new image that has been translated.
	4    float   yorg;

	NEW-STYLE MRC image2000 HEADER - IMOD 2.6.20 and above:
	4    float   xorg;      Origin of image.  Used to auto translate model
	4    float   yorg;      to match a new image that has been translated.
	4    float   zorg;

	4    char    cmap;      Contains "MAP "
	4    char    stamp;     First byte has 17 for big- or 68 for little-endian
	4    float   rms;

	ALL HEADERS:
	4    int     nlabl;  	Number of labels with useful data.
	800  char[10][80]    	10 labels of 80 charactors.
	------------------------------------------------------------------------

	Total size of header is 1024 bytes plus the size of the extended header.

	Image data follows with the origin in the lower left corner,
	looking down on the volume.

	The size of the image is nx * ny * nz * (mode data size).
*/
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

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

public class Open_MRC_Leginon extends ImagePlus implements PlugIn {

	/** Expects path as argument, or will ask for it and then open the image.*/
	public void run(final String arg) {
		String path = arg;
		String directory = null;
		String filename = null;
		if (null == path || 0 == path.length()) {
			OpenDialog od = new OpenDialog("Choose .mrc file", null);
			directory = od.getDirectory();
			if (null == directory) return;
			filename = od.getFileName();
			path = directory + "/" + filename;
		} else {
			// the argument is the path
			File file = new File(path);
			directory = file.getParent(); // could be a URL
			filename = file.getName();
			if (directory.startsWith("http:/")) directory = "http://" + directory.substring(6); // the double '//' has been eliminated by the File object call to getParent()
		}

		if (!directory.endsWith("/")) directory += "/"; // works in windows too

		InputStream is;
		byte[] buf = new byte[0xd5];
		try {
			if (0 == path.indexOf("http://")) {
				is = new java.net.URL(path).openStream();
			} else {
				is = new FileInputStream(path);
			}
			is.read(buf, 0, buf.length);
			is.close();
		} catch (IOException e) {
			return;
		}
		bigEndian = false;
		if (readInt(buf, 0xd0) == 0x2050414d /* "MAP " */ &&
				buf[0xd4] == 17)
			bigEndian = true;
		int w = readInt(buf, 0);
		int h = readInt(buf, 4);
		int n = readInt(buf, 8);
		int dtype = getType(readInt(buf, 12));
		if (-1 == dtype) return;
		ImagePlus imp = openRaw(
					dtype,
					directory,
					filename,
					w,
					h,
					1024L,
					n,
					0,
					true, // little-endian
					false);

		// integrate, the HandleExtraFileTypes way
		ImageStack stack = imp.getStack();
		setStack(imp.getTitle(),stack);
		setCalibration(imp.getCalibration());
		Object obinfo = imp.getProperty("Info");
		if (null != obinfo) setProperty("Info", obinfo);
		setFileInfo(imp.getOriginalFileInfo());

		if (null == arg || 0 == arg.length()) {
			// was opened with a dialog
			this.show();
		}
	}

	private int getType(int datatype) {
		switch (datatype) {
			case 0: return FileInfo.GRAY8;
			case 1: return FileInfo.GRAY16_SIGNED;
			case 2: return FileInfo.GRAY32_FLOAT;
			case 6: return FileInfo.GRAY16_UNSIGNED;
		}
		// else, error:
		return -1;
	}

	private boolean bigEndian;

	private final int readInt(byte[] buf, int start) {
		/* need to expand without the sign */
		int b0 = buf[start] & 0xff;
		int b1 = buf[start + 1] & 0xff;
		int b2 = buf[start + 2] & 0xff;
		int b3 = buf[start + 3] & 0xff;
		if (bigEndian)
			return b3 | (b2 << 8) | (b1 << 16) | (b0 << 24);
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}

	/** Copied and modified from ij.io.ImportDialog. @param imageType must be a static field from FileInfo class. */
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
