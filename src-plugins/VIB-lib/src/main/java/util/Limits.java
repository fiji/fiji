/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;

public class Limits {

	public static float[] getStackLimits( ImagePlus imagePlus ) {
		return getStackLimits(imagePlus,false);
	}

	public static float[] getStackLimits( ImagePlus imagePlus, boolean mustBeFinite ) {

		int depth = imagePlus.getStackSize();

		ImageStack stack = imagePlus.getStack();

		int bitDepth = imagePlus.getBitDepth();
		int type = imagePlus.getType();
		if( type == ImagePlus.COLOR_RGB )
			throw new RuntimeException( "Limits.getStackLimits can't do anything sensible with RGB images" );

		float minValue = Float.MAX_VALUE;
		float maxValue = Float.MIN_VALUE;

		int z;
		for( z = 0; z < depth; ++z ) {
			if( 8 == bitDepth) {
				byte [] pixels = (byte[])stack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					int value = pixels[i] & 0xFF;
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			} else if( type == ImagePlus.GRAY16 ) {
				short [] pixels = (short[])stack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					short value = pixels[i];
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			} else if( type == ImagePlus.GRAY32 ) {
				float [] pixels = (float[])stack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					float value = pixels[i];
					if( ! (mustBeFinite && (Float.isNaN(value) || Float.isInfinite(value))) ) {
						if( value > maxValue )
							maxValue = value;
						if( value < minValue )
							minValue = value;
					}
				}
			}
		}

		float [] result = new float[2];

		result[0] = minValue;
		result[1] = maxValue;

		return result;
	}
}
