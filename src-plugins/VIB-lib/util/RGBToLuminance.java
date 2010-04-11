/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;

public class RGBToLuminance {

	/** Takes an RGB ImagePlus and converts it to a 8-bit grey
	    ImagePlus of luminance values. */

	static public ImagePlus convertToLuminance( ImagePlus colourImage ) {
		if( colourImage.getType() != ImagePlus.COLOR_RGB ) {
			return null;
		}
		int depth = colourImage.getStackSize();
		int width = colourImage.getWidth();
		int height = colourImage.getHeight();
		Calibration calibration = colourImage.getCalibration();
		ImageStack stack=colourImage.getStack();
		ImageStack luminanceStack=new ImageStack(width,height);
		byte [][] luminancePixels = new byte[depth][];
		for( int z = 0; z < depth; ++z ) {
			int [] intPixels = (int[])stack.getPixels(z+1);
			luminancePixels[z] = new byte[width*height];
			int valuesInSlice = intPixels.length;
			for( int i = 0; i < valuesInSlice; ++i ) {
				int iv = intPixels[i];
				int b = iv & 0xFF;
				int g = (iv & 0xFF00) >> 8;
				int r = (iv & 0xFF0000) >> 16;
				// Using the definition from: http://en.wikipedia.org/wiki/Luminance_%28relative%29
				int luminance = (int)Math.round( 0.2126 * r + 0.7152 * g + 0.0722 * b );
				luminancePixels[z][i] = (byte)luminance;
			}
			ByteProcessor bp = new ByteProcessor(width,height);
			bp.setPixels(luminancePixels[z]);
			luminanceStack.addSlice("", bp);
			IJ.showProgress( z / (float)depth );
		}
		ImagePlus result = new ImagePlus("luminance of "+colourImage.getTitle(),luminanceStack);
		result.setCalibration(calibration);
		IJ.showProgress(1.0);
		return result;
	}
}
