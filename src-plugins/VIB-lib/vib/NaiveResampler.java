/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import amira.AmiraParameters;

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;
import ij.plugin.filter.*;

/* This plugin takes a binned image as input. It then reassigns equally spaced
   gray values to the pixels. */
public class NaiveResampler {
	private static int getPixel(byte[] b,int index) {
		return b[index] & 0xff;
	}

	private static interface Accumulator {
		void reset();
		void add(int value);
		int get();
	}

	public static class Averager implements Accumulator {
		long count, cumul;
		public void reset() { cumul = count = 0; }
		public void add(int value) { cumul += value; count++; }
		public int get() { return (int)(cumul/count); }
	}

	public static class IntAverager implements Accumulator {
		long count, cumulR, cumulG, cumulB;

		public void reset() {
			count = cumulR = cumulG = cumulB = 0;
		}

		public void add(int value) {
			count++;
			cumulR += (value & 0xff0000) >> 16;
			cumulG += (value & 0xff00) >> 8;
			cumulB += (value & 0xff);
		}

		public int get() {
			return (int)(((cumulR / count) << 16) 
				+ ((cumulG / count) << 8)
				+ cumulB / count);
		}
	}

	public static class MaxLikelihood implements Accumulator {
		int[] histo;
		int[] empty;
		int max;
		int indexOfHighest = -1;
		int highest = -1;
		MaxLikelihood(int max) {
			this.max = max;
			histo = new int[max+1];
			empty = new int[max+1];
		}
		MaxLikelihood() {
			this(255);
		}
		public void reset() {
			highest = -1;
			indexOfHighest = -1;
			System.arraycopy(empty,0,histo,0,max+1);
		}
		public void add(int value) {
			histo[value]++;
			if (histo[value] > highest) {
				highest = histo[value];
				indexOfHighest = value;
			}
		}
		public int get() {
			return indexOfHighest;
		}
		public String toString() {
			String result = "";
			for (int i = 0; i <= max; i++) {
				if (i > 0)
					result += " ";
				result += histo[i];
			}
			result += ": " + get();
			return result;
		}
	}

	/*
	 * In order to preserve the weaker structures, take the value
	 * which was in the original least often.
	 */
	public static class MinEntropy implements Accumulator {
		int[] histogram;

		public MinEntropy(ImagePlus image) {
			this(new InterpolatedImage(image));
		}

		public MinEntropy(InterpolatedImage ii) {
			int type = ii.image.getType();
			if(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256)
				histogram = new int[256];
			else if(type==ImagePlus.GRAY16)
				histogram = new int[1<<16];
			else {
				IJ.error("MinEntropy only works on 8bit or 16bit gray images.");
				return;
			}
			InterpolatedImage.Iterator iter = ii.iterator();
			if(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256)
				while (iter.next() != null)
					histogram[ii.getNoInterpol(iter.i, iter.j, iter.k)]++;
			else if (type==ImagePlus.GRAY16)
				while (iter.next() != null)
					histogram[ii.getNoInterpolShort(iter.i, iter.j, iter.k)]++;
		}

		int currentValue = -1;
		
		public void reset() {
			currentValue = -1;
		}

		public void add(int value) {
			if (currentValue < 0 ||
					histogram[value]
					< histogram[currentValue])
				currentValue = value;
		}

		public int get() {
			return currentValue;
		}
	}

	public static ImagePlus resample(ImagePlus image, int factorX, 
					int factorY, int factorZ) {
		Accumulator accu = null;
		int type = image.getType();
		if (image.getProcessor().isColorLut()) {
			if (type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256)
				accu = new MaxLikelihood(255);
			else if (type==ImagePlus.GRAY16)
				accu = new MaxLikelihood((1<<16)-1);
		} else if(type == ImagePlus.COLOR_RGB)
			accu = new IntAverager();
		else
			accu = new Averager();

		return resample(image, factorX, factorY, factorZ, accu);
	}

	public static ImagePlus resample(ImagePlus image, int factor){
		return resample(image, factor, factor, factor);
	}
	
	public static ImagePlus resampleMinEnt(ImagePlus image, int factorX, 
					int factorY, int factorZ) {
		return resample(image, factorX, factorY, factorZ,new MinEntropy(image));
	}

	public static ImagePlus resampleMinEnt(ImagePlus image, int factor){
		return resampleMinEnt(image, factor, factor, factor);
	}

	public static ImagePlus resample(ImagePlus image, int factorX, int factorY,
			int factorZ, Accumulator accu) {
		int type = image.getType();
		ImageStack stack=image.getStack();
		int w=image.getWidth(),h=image.getHeight(),d=stack.getSize();

		ImageStack result = new ImageStack(w/factorX,h/factorY,
				stack.getColorModel());

		for(int z=0;z<d;z+=factorZ) {
			int kfactor=(z+factorZ<d?factorZ:d-z);

			byte[][] slices = null;
			short[][] slicesShort = null;
			int[][] slicesInt = null;

			if(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256) {
				slices = new byte[kfactor][];
				for(int k=0;k<kfactor;k++)
					slices[k]=(byte[])stack.getProcessor(z+k+1).getPixels();
			} else if (type==ImagePlus.GRAY16) {
				slicesShort = new short[kfactor][];
				for(int k=0;k<kfactor;k++)
					slicesShort[k]=(short[])stack.getProcessor(z+k+1).getPixels();
			} else if (type==ImagePlus.COLOR_RGB) {
				slicesInt = new int[kfactor][];
				for(int k=0;k<kfactor;k++)
					slicesInt[k]=(int[])stack.getProcessor(z+k+1).getPixels();
			} else {
				IJ.error("Resample only currently works on 8 bit and 16 bit images.");
				return null;
			}

			byte[] newSlice = null;
			short[] newSliceShort = null;
			int[] newSliceInt = null;

			int pointsInNewSlice = (1+(w-1)/factorX)*(1+(h-1)/factorY);

			if(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256)
				newSlice = new byte[pointsInNewSlice];
			else if(type==ImagePlus.GRAY16)
				newSliceShort = new short[pointsInNewSlice];
			else if(type==ImagePlus.COLOR_RGB)
				newSliceInt = new int[pointsInNewSlice];

			for(int y=0;y<h;y+=factorY) {
				for(int x=0;x<w;x+=factorX) {
					int ifactor=(x+factorX<w?factorX:w-x);
					int jfactor=(y+factorY<h?factorY:h-y);
					accu.reset();
					if(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256) {
						for(int i=0;i<ifactor;i++)
							for(int j=0;j<jfactor;j++)
								for(int k=0;k<kfactor;k++)
									accu.add(getPixel(slices[k],x+i+w*(y+j)));
						newSlice[(x/factorX)+(w/factorX)*(y/factorY)]=
							(byte)accu.get();
					} else if (type==ImagePlus.GRAY16) {
						for(int i=0;i<ifactor;i++)
							for(int j=0;j<jfactor;j++)
								for(int k=0;k<kfactor;k++)
									accu.add(slicesShort[k][x+i+w*(y+j)]);
						newSliceShort[(x/factorX)+(w/factorX)*(y/factorY)]=
							(short)accu.get();
					} else if (type==ImagePlus.COLOR_RGB) {
						for(int i=0;i<ifactor;i++)
							for(int j=0;j<jfactor;j++)
								for(int k=0;k<kfactor;k++)
									accu.add(slicesInt[k][x+i+w*(y+j)]);
						newSliceInt[(x/factorX)+(w/factorX)*(y/factorY)]=
							(int)accu.get();
					}
				}
				IJ.showProgress(z*h+y+1, h*d);
			}
			if(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256)
				result.addSlice(null,newSlice);
			else if(type==ImagePlus.GRAY16)
				result.addSlice(null,newSliceShort);				
			else if(type==ImagePlus.COLOR_RGB)
				result.addSlice(null,newSliceInt);
			
		}

		ImagePlus res = new ImagePlus(image.getTitle()+" resampled",
				result);
		if (AmiraParameters.isAmiraMesh(image)) {
			AmiraParameters p = new AmiraParameters(image);
			p.setParameters(res);
		}

		Calibration cal = image.getCalibration().copy();
		cal.pixelWidth *= image.getWidth() / (double)res.getWidth();
		cal.pixelHeight *= image.getHeight() / (double)res.getHeight();
		cal.pixelDepth *= image.getStack().getSize()
			/ (double)res.getStack().getSize();
		res.setCalibration(cal);

		return res;
	}
}
