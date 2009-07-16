package org.imagearchive.lsm.reader.info;

public class ImageDirectory {

	public long TIF_NEWSUBFILETYPE = 0;

	public long TIF_IMAGEWIDTH = 0;

	public long TIF_IMAGELENGTH = 0;

	public long TIF_BITSPERSAMPLE_LENGTH = 0;

	public long[] TIF_BITSPERSAMPLE_CHANNEL = new long[3];

	public long TIF_COMPRESSION = 0;

	public long TIF_PHOTOMETRICINTERPRETATION = 0;

	public long TIF_STRIPOFFSETS_LENGTH = 0;

	public long[] TIF_STRIPOFFSETS;

	public long TIF_SAMPLESPERPIXEL = 0;

	public long TIF_STRIPBYTECOUNTS_LENGTH = 0;

	public long[] TIF_STRIPBYTECOUNTS;

	public long TIF_PLANARCONFIGURATION = 0;

	public long TIF_PREDICTOR = 0;

	public byte[][] TIF_COLORMAP;

	public long TIF_CZ_LSMINFO_OFFSET = 0; // OFFSET

	public Object TIF_CZ_LSMINFO; // STRUCT

	public long OFFSET_NEXT_DIRECTORY = 0;

	public String toString() {
		return new String("TIF_NEWSUBFILETYPE:  " + TIF_NEWSUBFILETYPE + "\n"
				+ "TIF_IMAGEWIDTH:  " + TIF_IMAGEWIDTH + "\n"
				+ "TIF_IMAGELENGTH:  " + TIF_IMAGELENGTH + "\n"
				+ "TIF_SAMPLESPERPIXEL:  " + TIF_SAMPLESPERPIXEL + "\n"
				+ "TIF_COMPRESSION:  " + TIF_COMPRESSION + "\n"
				+ "TIF_PREDICTOR:  " + TIF_PREDICTOR + "\n");
	}
}
