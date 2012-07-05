/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import java.awt.Font;
import java.awt.Dialog;
import java.awt.FontMetrics;
import java.awt.image.ColorModel;
import vib.TransformedImage;
import java.util.Arrays;

/* TODOs:

    The problem with this plugin is that it's not enough like the
    Colocalization Threshold plugin. :) It can draw best fit lines,
    but doesn't look for the threshold above which it makes sense.

    I think this should work by producing candidate histogram images
    and allowing the user to pick LUT and method before clicking
    "Frame" to add the axes, legend, etc.
*/

public class Histogram_2D implements PlugIn {

	int binsA;
	int binsB;
	long totalValues;
	long[][] counts = new long[binsA][binsB];
	double minValueA;
	double maxValueA;
	double minValueB;
	double maxValueB;
	double rangeWidthA;
	double rangeWidthB;
	long countMin;
	long countMax;

	int [] dimensions;
	int width, depth, height;
	int typeA, typeB;
	int bitDepthA, bitDepthB;

	// These will remain null apart from the ones corresponding to
	// the source image type:

	byte[] pixelsABytes = null;
	byte[] pixelsBBytes = null;

	short[] pixelsAShorts = null;
	short[] pixelsBShorts = null;

	float[] pixelsAFloats = null;
	float[] pixelsBFloats = null;

	// This helper inner class is to return the results from getStatistics:

	public class Statistics {
		public double sumX;
		public double sumY;
		public double sumXY;
		public double sumXX;
		public double sumYY;
		public long n;
		public double minimumXThreshold = Float.MIN_VALUE;
		public double minimumYThreshold = Float.MIN_VALUE;
		public double maximumXThreshold = Float.MAX_VALUE;
		public double maximumYThreshold = Float.MAX_VALUE;
		public double meanX;
		public double meanY;
		public double sdX;
		public double sdY;
		public double varX;
		public double varY;
		public double numeratorSum;
		public double denominatorSum;
		public double covariance;
		public double correlation;
		public void print() {
			System.out.println("== meanX: "+meanX);
			System.out.println("== meanY: "+meanY);
			System.out.println("== covariance: "+covariance);
			System.out.println("== correlation: "+correlation);
			System.out.println("== fitted gradient: "+getFittedGradient());
			System.out.println("== fitted Y intercept: "+getFittedYIntercept());
			System.out.println("== n: "+n);
			System.out.println("== sumX: "+sumX);
			System.out.println("== sumY: "+sumY);
			System.out.println("== sumXX: "+sumXX);
			System.out.println("== sumXY: "+sumXY);
			System.out.println("== sumYY: "+sumYY);
		}
		public double getFittedGradient() {
			return numeratorSum / denominatorSum;
		}
		public double getFittedYIntercept() {
			return meanY - getFittedGradient() * meanX;
		}
	}

	public Statistics getStatistics( double minimumXThreshold,
					 double maximumXThreshold,
					 double minimumYThreshold,
					 double maximumYThreshold ) {

		Statistics result = new Statistics();

		double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, sumYY = 0;
		long n = 0;

		IJ.showStatus("Calculating statistics");
		IJ.showProgress(0);

		double meanX = 0;
		double meanY = 0;
		double varX = 0;
		double varY = 0;
		double sdX = 0;
		double sdY = 0;

		double correlation = 0;

		double numeratorSum = 0;
		double denominatorSum = 0;

		double covarianceSum = 0;
		double covariance = 0;

		/* You can almost certainly do this in one pass, but
		   it's more obviously correct if you calculate the
		   means after the first time through. */

		for( int pass = 0; pass < 2; ++pass ) {
			for (int z = 0; z < depth; ++z) {
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						float valueA = -1;
						float valueB = -1;
						if( bitDepthA == 8 ) {
							valueA = pixelsABytes[y * width + x] & 0xFF;
						} else if( bitDepthA == 16 ) {
							valueA = pixelsAShorts[y * width + x];
						} else if( bitDepthA == 32 ) {
							valueA = pixelsAFloats[y * width + x];
						}
						if( bitDepthB == 8 ) {
							valueB = pixelsBBytes[y * width + x] & 0xFF;
						} else if( bitDepthB == 16 ) {
							valueB = pixelsBShorts[y * width + x];
						} else if( bitDepthB == 32 ) {
							valueB = pixelsBFloats[y * width + x];
						}
						if( pass == 0 ) {
							if( (valueA >= minimumXThreshold) &&
							    (valueB >= minimumYThreshold) &&
							    (valueA <= maximumXThreshold) &&
							    (valueB <= maximumYThreshold) ) {
								sumX += valueA;
								sumY += valueB;
								sumXX += valueA * valueA;
								sumXY += valueA * valueB;
								sumYY += valueB * valueB;
								++ n;
							}
						} else {
							double xResidual = valueA - meanX;
							double yResidual = valueB - meanY;
							numeratorSum += xResidual * yResidual;
							denominatorSum +=  xResidual * xResidual;
							covarianceSum += xResidual * yResidual;
						}
					}
				}
				IJ.showProgress(z/(double)depth);
			}
			if( pass == 0 ) {
				meanX = sumX / (double)n;
				meanY = sumY / (double)n;
				varX = sumXX / (double)n - meanX * meanX;
				varY = sumYY / (double)n - meanY * meanY;
				sdX = Math.sqrt(varX);
				sdY = Math.sqrt(varY);
			}
		}
		IJ.showProgress(1);

		result.sumX = sumX;
		result.sumY = sumY;
		result.sumXX = sumXX;
		result.sumXY = sumXY;
		result.sumYY = sumYY;
		result.n = n;

		result.minimumXThreshold = minimumXThreshold;
		result.minimumYThreshold = minimumYThreshold;
		result.maximumXThreshold = maximumXThreshold;
		result.maximumYThreshold = maximumYThreshold;

		result.meanX = meanX;
		result.meanY = meanY;
		result.varX = varX;
		result.varY = varY;
		result.sdX = sdX;
		result.sdY = sdY;
		result.numeratorSum = numeratorSum;
		result.denominatorSum = denominatorSum;

		result.covariance = covarianceSum / n;
		result.correlation = (covarianceSum / n) / (sdX * sdY);

		return result;
	}


	public double [] getMinimumThresholdsForCorrelation(Statistics overall) {
		double [] result = new double[2];
		double thresholdA = maxValueA;
		while( true ) {
			double thresholdB = thresholdA * overall.getFittedGradient() + overall.getFittedYIntercept();
			// Still not sure if it's a good idea to leave out the zero pixels:
			Statistics s = getStatistics( minValueA, thresholdA,
						      minValueB, thresholdB );
			System.out.println("Width thresholdA: "+thresholdA+", thresholdB: "+thresholdB+" got correlation: "+s.correlation);
			thresholdA -= rangeWidthA / binsA;

			if( s.correlation <= 0 ) {
				result[0] = thresholdA;
				result[1] = thresholdB;
				return result;
			}
			if( thresholdA < minValueA )
				break;
		}
		result[0] = minValueA;
		result[1] = minValueB;
		return result;
	}

	protected void start2DHistogram(
		double minValueA,
		double maxValueA,
		double minValueB,
		double maxValueB,
		int binsA,
		int binsB ) {

		this.binsA = binsA;
		this.binsB = binsB;
		this.totalValues = 0;
		this.counts = new long[binsA][binsB];
		this.minValueA = minValueA;
		this.maxValueA = maxValueA;
		this.minValueB = minValueB;
		this.maxValueB = maxValueB;
		this.rangeWidthA = maxValueA - minValueA;
		this.rangeWidthB = maxValueB - minValueB;
		countMin = Long.MAX_VALUE;
		countMax = Long.MIN_VALUE;
	}

	public boolean allowedBitDepth( int bitDepth ) {
		return (bitDepth == 8) || (bitDepth == 16) || (bitDepth == 32);
	}

	/* These constants are so that I don't have to remember which
	   is which in the results from ImagePlus.getDimensions() */
	public static final int WIDTH = 0;
	public static final int HEIGHT = 1;
	public static final int CHANNELS = 2;
	public static final int SLICES = 3;
	public static final int FRAMES = 4;

	/* Add the initial images - this bins the values and then */

	public boolean addImagePlusPair(
		ImagePlus imageA,
		ImagePlus imageB ) {
		return addImagePlusPair( imageA, imageB, 256 );
	}

	public boolean addImagePlusPair(
		ImagePlus imageA,
		ImagePlus imageB,
		int bins ) {
		// Now find the maximum ranges in each to use as defaults:
		float [] valueRangeA = Limits.getStackLimits( imageA, true );
		float [] valueRangeB = Limits.getStackLimits( imageB, true );
		return addImagePlusPair(
			imageA,
			imageB,
			valueRangeA[0],
			valueRangeA[1],
			valueRangeB[0],
			valueRangeB[1],
			bins,
			bins );
	}

	public boolean addImagePlusPair(
	    ImagePlus imageA,
	    ImagePlus imageB,
	    double minimumA,
	    double maximumA,
	    double minimumB,
	    double maximumB,
	    int binsA,
	    int binsB ) {

		imageA.getProcessor().setMinAndMax(minimumA,maximumA);
		imageB.getProcessor().setMinAndMax(minimumB,maximumB);

		ImageStack stackA = imageA.getStack();
		ImageStack stackB = imageB.getStack();

		dimensions = imageA.getDimensions();
		{
			int [] dimensionsB = imageB.getDimensions();
			if( ! Arrays.equals(dimensions,dimensionsB) ) {
				IJ.error("The two images must be of the same dimensions.");
				return false;
			}
		}

		if( dimensions[FRAMES] != 1 ) {
			IJ.error("Currently this plugin does not work with time series");
			return false;
		}

		if( dimensions[CHANNELS] != 1 ) {
			IJ.error("Currently this plugin does not work with composite images (i.e. those with multiple channels)");
			return false;
		}

		depth = imageA.getStackSize();
		width = imageA.getWidth();
		height = imageA.getHeight();

		typeA=imageA.getType();
		typeB=imageB.getType();

		bitDepthA=imageA.getBitDepth();
		bitDepthB=imageB.getBitDepth();

		pixelsABytes = null;
		pixelsBBytes = null;

		pixelsAShorts = null;
		pixelsBShorts = null;

		pixelsAFloats = null;
		pixelsBFloats = null;

		if( ! allowedBitDepth(bitDepthA) ) {
			IJ.error(""+imageA.getTitle()+" has an unsupported bit depth: "+bitDepthA);
			return false;
		}

		if( ! allowedBitDepth(bitDepthB) ) {
			IJ.error(""+imageB.getTitle()+" has an unsupported bit depth: "+bitDepthB);
			return false;
		}

		start2DHistogram( minimumA, maximumA, minimumB, maximumB, binsA, binsB );

		IJ.showStatus( "Binning image values..." );
		IJ.showProgress(0);

		float valueA = -1;
		float valueB = -1;

		for (int z = 0; z < depth; ++z) {

			if( bitDepthA == 8 ) {
				pixelsABytes = (byte[]) stackA.getPixels(z + 1);
			} else if( bitDepthA == 16 ) {
				pixelsAShorts = (short[]) stackA.getPixels(z + 1);
			} else if( bitDepthA == 32 ) {
				pixelsAFloats = (float[]) stackA.getPixels(z + 1);
			}

			if( bitDepthB == 8 ) {
				pixelsBBytes = (byte[]) stackB.getPixels(z + 1);
			} else if( bitDepthB == 16 ) {
				pixelsBShorts = (short[]) stackB.getPixels(z + 1);
			} else if( bitDepthB == 32 ) {
				pixelsBFloats = (float[]) stackB.getPixels(z + 1);
			}

			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {

					if( bitDepthA == 8 ) {
						valueA = pixelsABytes[y * width + x] & 0xFF;
					} else if( bitDepthA == 16 ) {
						valueA = pixelsAShorts[y * width + x];
					} else if( bitDepthA == 32 ) {
						valueA = pixelsAFloats[y * width + x];
					}

					if( bitDepthB == 8 ) {
						valueB = pixelsBBytes[y * width + x] & 0xFF;
					} else if( bitDepthB == 16 ) {
						valueB = pixelsBShorts[y * width + x];
					} else if( bitDepthB == 32 ) {
						valueB = pixelsBFloats[y * width + x];
					}

					int i1 = (int)Math.floor((valueA - minValueA) * binsA / rangeWidthA);
					int i2 = (int)Math.floor((valueB - minValueB) * binsB / rangeWidthB);
					if( i1 >= binsA )
						i1 = binsA - 1;
					if( i2 >= binsB )
						i2 = binsB - 1;

					++counts[i1][i2];
					++totalValues;
				}
			}
			IJ.showProgress(z/(double)depth);
		}
		IJ.showProgress(1);
		for( int a = 0; a < binsA; ++a ) {
			for( int b = 0; b < binsB; ++b ) {
				if( counts[a][b] < countMin )
					countMin = counts[a][b];
				if( counts[a][b] > countMax )
					countMax = counts[a][b];
			}
		}

		return true;
	}

	public ImagePlus [] getHistogramImages( ) {

		System.out.println("totalValues is: "+totalValues);

		double [][] p = new double[binsA][binsB];
		double [][] selfInformation = new double[binsA][binsB];

		for( int avalue = 0; avalue < binsA; ++avalue )
			for( int bvalue = 0; bvalue < binsB; ++bvalue ) {

				p[avalue][bvalue] = (double)counts[avalue][bvalue] / totalValues;
				selfInformation[avalue][bvalue] = - Math.log(p[avalue][bvalue]) / Math.log(2);
			}

		ImagePlus probImagePlus;
		{
			float floatValues [] = new float[binsA*binsB];

			for( int avalue = 0; avalue < binsA; ++avalue )
				for( int bvalue = 0; bvalue < binsB; ++bvalue ) {
					floatValues[((binsB-1)-bvalue)*binsA+avalue] = (float)p[avalue][bvalue];
				}

			FloatProcessor fp = new FloatProcessor(binsA,binsB);
			fp.setPixels(floatValues);
			ImageStack newStack=new ImageStack(binsA,binsB);
			newStack.addSlice("", fp);
			probImagePlus=new ImagePlus("2D Histogram Log Probabilities",newStack);
		}

		ImagePlus logProbImagePlus;
		{
			float floatValues [] = new float[binsA*binsB];

			for( int avalue = 0; avalue < binsA; ++avalue )
				for( int bvalue = 0; bvalue < binsB; ++bvalue ) {
					floatValues[((binsB-1)-bvalue)*binsA+avalue] = (float)Math.log( p[avalue][bvalue] );
				}

			FloatProcessor fp = new FloatProcessor(binsA,binsB);
			fp.setPixels(floatValues);
			ImageStack newStack=new ImageStack(binsA,binsB);
			newStack.addSlice("", fp);
			logProbImagePlus=new ImagePlus("2D Histogram Probabilities",newStack);
		}

		ImagePlus selfNewImagePlus;
		{
			float selfValues [] = new float[binsA*binsB];

			for( int avalue = 0; avalue < binsA; ++avalue )
				for( int bvalue = 0; bvalue < binsB; ++bvalue ) {

					selfValues[((binsB-1)-bvalue)*binsA+avalue] = (float)selfInformation[avalue][bvalue];
				}

			FloatProcessor selfFP = new FloatProcessor(binsA,binsB);
			selfFP.setPixels(selfValues);
			ImageStack selfNewStack=new ImageStack(binsA,binsB);
			selfNewStack.addSlice("", selfFP);
			selfNewImagePlus=new ImagePlus("2D Histogram Self Information",selfNewStack);
		}

		ImagePlus [] result = new ImagePlus[3];
		result[PROBABILITIES] = probImagePlus;
		result[LOG_PROBABILITIES] = logProbImagePlus;
		result[SELF_INFORMATION] = selfNewImagePlus;

		IJ.showStatus("Setting limits for each histogram");
		for( ImagePlus i : result ) {
			float [] valueRange = Limits.getStackLimits(i,true);
			System.out.println("Got valueRange: "+valueRange[0]+" -> "+valueRange[1]);
			i.getProcessor().setMinAndMax(valueRange[0],valueRange[1]);
		}

		return result;
	}

	// If statistics is non-null, draw the best fit line:
	public ImagePlus frame2DHistogram(
	    String title,
	    ImagePlus histogram,
	    String xLabel, double xmin, double xmax,
	    String yLabel, double ymin, double ymax,
	    int method,
	    Statistics statistics ) {

		int tickSize = 5;
		int tickMargin = 10;
		boolean serifFont = false;
		int fontSize = 10;
		int titleSize = 12;

		int leftBorder = 100;
		int rightBorder = 180;
		int topBorder = 60;
		int bottomBorder = 100;

		if( histogram.getType()	!= ImagePlus.GRAY32 ) {
			IJ.error("frame2DHistogram only works on GRAY32 (Float) 2D histogram images");
			return null;
		}

		if( histogram.getStackSize() != 1 ) {
			IJ.error("The histogram must not be a stack.");
			return null;
		}

		ColorModel colorModel = histogram.getProcessor().getColorModel();

		int oldWidth=histogram.getWidth();
		int oldHeight=histogram.getHeight();
		FloatProcessor oldFP=(FloatProcessor)histogram.getProcessor();
		float oldMin=(float) oldFP.getMin();
		float oldMax=(float) oldFP.getMax();
		float [] oldFloats=(float[])oldFP.getPixels();

		int newWidth=oldWidth+leftBorder+rightBorder;
		int newHeight=oldHeight+topBorder+bottomBorder;
		float[] newFloats=new float[newWidth*newHeight];
		for(int i=0;i<newFloats.length;++i)
			newFloats[i]=oldMax;

		for(int y=0;y<oldHeight;++y) {
			for(int x=0;x<oldWidth;++x) {
				newFloats[(y+topBorder)*newWidth+(x+leftBorder)] =
				    oldFloats[y*oldWidth+x];
			}
		}

		FloatProcessor newFP=new FloatProcessor(newWidth,newHeight);
		newFP.setPixels(newFloats);
		newFP.setMinAndMax(oldMin, oldMax);

		newFP.setValue(oldMin);

		// Draw ticks:
		newFP.drawLine(
		    leftBorder,
		    topBorder+oldHeight,
		    leftBorder,
		    topBorder+oldHeight+tickSize);
		newFP.drawLine(
		    leftBorder+oldWidth-1,
		    topBorder+oldHeight,
		    leftBorder+oldWidth-1,
		    topBorder+oldHeight+tickSize);
		newFP.drawLine(
		    leftBorder-1,
		    topBorder,
		    (leftBorder-1)-tickSize,
		    topBorder);
		newFP.drawLine(
		    leftBorder-1,
		    topBorder+oldHeight-1,
		    (leftBorder-1)-tickSize,
		    topBorder+oldHeight-1);

		ImagePlus newImagePlus=new ImagePlus(
		    "Framed Histogram",
		    newFP );

		if( colorModel != null ) {
			newImagePlus.getProcessor().setColorModel( colorModel );
		}

		String fontName = serifFont ? "Serif" : "SanSerif";
		int fontType = false ? Font.BOLD : Font.PLAIN;
		Font font=new Font(fontName, fontType, fontSize);

		newImagePlus.show();
		ImageCanvas ic=newImagePlus.getCanvas();
		FontMetrics fm=ic.getFontMetrics(font);

		newFP.setFont(font);
		newFP.setAntialiasedText(true);

		String sXmin=""+xmin;
		String sXmax=""+xmax;
		String sYmin=""+ymin;
		String sYmax=""+ymax;

		newFP.drawString(
		    sXmin,
		    leftBorder - (fm.stringWidth(sXmin) / 2),
		    topBorder + oldHeight + tickMargin + fm.getHeight() );
		newFP.drawString(
		    sXmax,
		    leftBorder+oldWidth - (fm.stringWidth(sXmax) / 2),
		    topBorder + oldHeight + tickSize + tickMargin + fm.getHeight() );
		newFP.drawString(
		    sYmin,
		    leftBorder - tickMargin - fm.stringWidth(sYmin) - tickSize,
		    topBorder + oldHeight + fm.getHeight() / 2 );
		newFP.drawString(
		    sYmax,
		    leftBorder - tickMargin - fm.stringWidth(sYmax) - tickSize,
		    topBorder + fm.getHeight() / 2 );

		newFP.drawString(
		    xLabel,
		    leftBorder + oldWidth / 2 - fm.stringWidth(xLabel) / 2,
		    topBorder + oldHeight + tickSize + 2 * tickMargin + 2 * fm.getHeight() );

		/* Draw a similar label in a new FloatProcessor and copy
		 * it over. */

		int labelWidth=fm.stringWidth(yLabel);
		int labelHeight=fm.getHeight();

		FloatProcessor fpToRotate=new FloatProcessor(labelWidth,labelHeight);
		float [] labelFloats=new float[labelWidth*labelHeight];
		for( int i = 0; i < labelFloats.length; ++i )
			labelFloats[i] = oldMax;
		fpToRotate.setFont(font);
		fpToRotate.setPixels(labelFloats);
		fpToRotate.setValue(oldMin);
		fpToRotate.setMinAndMax(oldMin,oldMax);
		fpToRotate.drawString(yLabel,0,labelHeight);

		int yLabelTopLeftX = leftBorder - tickSize - tickMargin - labelHeight * 2;
		int yLabelTopLeftY = topBorder + (oldHeight / 2) - (labelWidth / 2);

		for(int y=0;y<labelHeight;++y)
			for(int x=0;x<labelWidth;++x) {
				int newX= yLabelTopLeftX + y;
				int newY= yLabelTopLeftY + labelWidth - x;
				newFloats[newY*newWidth+newX]=labelFloats[y*labelWidth+x];
			}

		/* Now draw a bar at the side showing the value range. */

		int barWidth = 30;
		int barHeight = (oldHeight * 2) / 3;

		int barTopLeftX = leftBorder + oldWidth + 40;
		int barTopLeftY = topBorder + (oldHeight - barHeight) / 2;

		newFP.drawRect(barTopLeftX, barTopLeftY, barWidth+2, barHeight+2);

		for(int barOffset=0;barOffset<barHeight;++barOffset) {
			int barLineX1=barTopLeftX+1;
			int barLineX2=barTopLeftX+barWidth;
			int barLineY=barTopLeftY+1+(barHeight-(barOffset+1));
			float value=((float)barOffset*(oldMax-oldMin))/(barHeight-1)+oldMin;
			newFP.setValue(value);
			newFP.drawLine(barLineX1,barLineY,barLineX2,barLineY);
		}

		/* Now add some tick marks to the bar */
		newFP.setValue(oldMin);
		newFP.drawLine(
		    barTopLeftX+barWidth+2,
		    barTopLeftY,
		    barTopLeftX+barWidth+2+tickSize,
		    barTopLeftY);
		newFP.drawString(
		    ""+oldMax,
		    barTopLeftX+barWidth+2+tickSize+tickMargin,
		    barTopLeftY+fm.getHeight()/2
		    );
		newFP.drawLine(
		    barTopLeftX+barWidth+2,
		    barTopLeftY+barHeight+1,
		    barTopLeftX+barWidth+2+tickSize,
		    barTopLeftY+barHeight+1);
		newFP.drawString(
		    ""+oldMin,
		    barTopLeftX+barWidth+2+tickSize+tickMargin,
		    barTopLeftY+barHeight+fm.getHeight()/2
		    );

		/* Now just draw the title */

		fontType = Font.BOLD;
		Font titleFont=new Font(fontName, fontType, titleSize);

		FontMetrics titleFM=ic.getFontMetrics(font);

		newFP.setFont(titleFont);
		newFP.drawString(
		    title,
		    newWidth / 2 - titleFM.stringWidth(title) / 2,
		    topBorder / 2 + titleFM.getHeight() / 2 );

		/* If a line fit has been calculated, draw that over
		 * the image... */

		if( statistics != null ) {

			// Draw the fitted line onto the histogram (as
			// a dotted line)...

			newFP.drawPixel( 10, 10 );

			double fittedGradient = statistics.getFittedGradient();
			double fittedYIntercept = statistics.getFittedYIntercept();

			if( fittedGradient <= 1 ) {
				// FIXME:
				for( int xBin=0; xBin<binsA; ++xBin ) {
					double realX = minValueA + ( (xBin+0.5f) / binsA ) * rangeWidthA;
					System.out.println("xBin "+xBin+" mapped to "+realX);
					double realY = (double)( fittedGradient * realX + fittedYIntercept );
					int yBin = (int)Math.floor((realY - minValueB) * binsB / rangeWidthB);
					System.out.println("bin: ("+xBin+","+yBin+")");
					if( yBin >= 0 && yBin < binsB ) {
						newFP.setValue( (xBin % 2) == 0 ? oldMin : oldMax );
						newFP.drawPixel( leftBorder+xBin, topBorder+oldHeight-yBin );
					}
				}

			} else {

				for( int yBin=0; yBin<binsB; ++yBin ) {
					double realY = minValueB + ( (yBin+0.5f) / binsB ) * rangeWidthB;
					System.out.println("yBin "+yBin+" mapped to "+realY);
					double realX = (float)( (realY - fittedYIntercept) / fittedGradient );
					int xBin = (int)Math.floor((realX - minValueA) * binsA / rangeWidthA);
					System.out.println("bin: ("+xBin+","+yBin+")");
					if( xBin >= 0 && xBin < binsB ) {
						newFP.setValue( (yBin % 2) == 0 ? oldMin : oldMax );
						newFP.drawPixel( leftBorder+xBin, topBorder+oldHeight-yBin );
					}
				}
			}
		}

		newImagePlus.updateAndRepaintWindow();

		return newImagePlus;
	}

	public final static int PROBABILITIES = 0;
	public final static int LOG_PROBABILITIES = 1;
	public final static int SELF_INFORMATION = 2;

	public void run(String ignored) {

		String titleSubstring = "";

		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("No images are open.");
			return;
		}

		String [] matchingTitles=new String[wList.length];
		ImagePlus [] matchingImagePlus=new ImagePlus[wList.length];
		ImagePlus [] allImages=new ImagePlus[wList.length];

		int totalMatchingTitles = 0;
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			String title = (imp == null) ? "" : imp.getTitle();
			if(title.indexOf(titleSubstring) >= 0) {
				matchingTitles[totalMatchingTitles] = title;
				matchingImagePlus[totalMatchingTitles] = imp;
				++totalMatchingTitles;
			}
			allImages[i] = imp;
		}

		if( totalMatchingTitles < 2 ) {
		    IJ.error("There are only "+totalMatchingTitles+" matching images; need at least 2.");
		    return;
		}

		String [] onlyMatchingTitles = new String[totalMatchingTitles];
		System.arraycopy(matchingTitles,0,onlyMatchingTitles,0,totalMatchingTitles);
		ImagePlus [] onlyMatchingImagePlus = new ImagePlus[totalMatchingTitles];
		System.arraycopy(matchingImagePlus, 0, onlyMatchingImagePlus, 0, totalMatchingTitles);

		String [] methods = { "p (Probability)", "ln(p) (Log Probabilities)", "-log\u2082(p) (Self-information)" };

		GenericDialog gd = new GenericDialog("2D Histogram");
		gd.addChoice("A:", onlyMatchingTitles, onlyMatchingTitles[0]);
		gd.addChoice("B:", onlyMatchingTitles, onlyMatchingTitles[1]);
		gd.addChoice("Values to plot: ", methods, methods[LOG_PROBABILITIES]);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		int method = gd.getNextChoiceIndex();

		ImagePlus imageA = onlyMatchingImagePlus[index[0]];
		ImagePlus imageB = onlyMatchingImagePlus[index[1]];

		// Now ask about the limits on each axis:

		float [] valueRangeA = Limits.getStackLimits( imageA, true );
		float [] valueRangeB = Limits.getStackLimits( imageB, true );

		double minimumA = valueRangeA[0];
		double maximumA = valueRangeA[1];
		double minimumB = valueRangeB[0];
		double maximumB = valueRangeB[1];

		GenericDialog limitsDialog = new GenericDialog("2D Histogram Limits");
		limitsDialog.addMessage("(If in doubt, leave these at the default values.)");
		limitsDialog.addNumericField("Minimum in "+imageA.getTitle(), minimumA, 10, 15, "");
		limitsDialog.addNumericField("Maximum in "+imageA.getTitle(), maximumA, 10, 15, "");
		limitsDialog.addNumericField("Minimum in "+imageB.getTitle(), minimumB, 10, 15, "");
		limitsDialog.addNumericField("Maximum in "+imageB.getTitle(), maximumB, 10, 15, "");
		limitsDialog.addMessage("");
		int binsA = 256;
		int binsB = 256;
		limitsDialog.addNumericField("Bins for "+imageA.getTitle(), binsA, 0, 4, "");
		limitsDialog.addNumericField("Bins for "+imageB.getTitle(), binsB, 0, 4, "");
		limitsDialog.showDialog();
		if( limitsDialog.wasCanceled() )
			return;

		if( ! (minimumA < maximumA && minimumB < maximumB) ) {
			IJ.error("The minimum must be less than the maximum for each image.");
			return;
		}

		minimumA = limitsDialog.getNextNumber();
		maximumA = limitsDialog.getNextNumber();
		minimumB = limitsDialog.getNextNumber();
		maximumB = limitsDialog.getNextNumber();

		binsA = (int)limitsDialog.getNextNumber();
		binsB = (int)limitsDialog.getNextNumber();

		if( binsA <= 0 || binsB <= 0 ) {
			IJ.error("The number of bins for each image must be non-negative");
		}

		addImagePlusPair( imageA, imageB, minimumA, maximumA, minimumB, maximumB, binsA, binsB );

		double binWidthA = rangeWidthA / binsA;
		double binWidthB = rangeWidthB / binsB;

		Statistics allValues = getStatistics( minimumA + binWidthA, maximumA - binWidthA, minimumB + binWidthB, maximumB - binWidthB );
		System.out.println("--------------------------------------------");
		allValues.print();

		System.out.println("fitted gradient is: "+allValues.getFittedGradient());
		System.out.println("fitted Y intercept is: "+allValues.getFittedYIntercept());

		ImagePlus[] results = getHistogramImages();

		double [] correlationThresholds = getMinimumThresholdsForCorrelation(allValues);

		IJ.runPlugIn( results[method], "ij.plugin.LutLoader", "fire" );

		frame2DHistogram(
			methods[method] + " for Corresponding Values",
			results[method],
			imageA.getTitle(),
			minimumA, maximumA,
			imageB.getTitle(),
			minimumB, maximumB,
			method,
			allValues);
	}


	public class HistogramOptionsDialog extends Dialog {
		HistogramOptionsDialog() {
			super(IJ.getInstance());
		}

	}
}
