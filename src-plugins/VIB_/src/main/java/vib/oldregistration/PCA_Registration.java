/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.oldregistration;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Point3d;
import math3d.JacobiFloat;

import java.util.ArrayList;
import java.util.Comparator;

import vib.FastMatrix;

import vib.transforms.FastMatrixTransform;
import vib.transforms.OrderedTransformations;
import vib.transforms.Threshold;

import util.CombinationGenerator;

/* ------------------------------------------------------------------------

    Terminology note:

       I refer to the image that we transform as the "domain" image
       and the one which is kept in its original orientation as the
       "template" image.

   ------------------------------------------------------------------------ */

// The intensity mapping defined by this technique is rather crude,
// but seems to work well enough for this application.  (Note that it
// won't actually work as a standalone PlugInFilter at the moment.)

class IntensityMap implements PlugInFilter {
	
        public static int [] histogramToCumulative( int [] valueHistogram ) {
		
                int [] cumulative = new int[valueHistogram.length];
		
                // Each entry in cumulative gives us the number of points with
                // that value or lower.  (So,
                // cumulative[valueHistogram.length-1] should be the total
                // number of points referred to in the image.
		
                int acc = 0;
		
                for( int i = 0; i < valueHistogram.length; ++i ) {
                        acc += valueHistogram[i];
                        cumulative[i] = acc;
                }
		
                return cumulative;
		
        }
	
        private ImagePlus image;
        private String arg;
	
        public int setup( String arg, ImagePlus image ) {
		
                this.image = image;
                return DOES_8G;
		
        }
	
        public void run(ImageProcessor ip) {
		
                ImageStack stack = image.getStack();
		
                for( int z = 0; z < image.getStackSize(); ++z ) {
			
                        byte [] pixels = ( byte [] ) stack.getPixels( z + 1 );
			
                        for( int i = 0; i < pixels.length; ++i ) {
                                int v = domainToTemplate[
                                        (int)(pixels[i] & 0xFF )];
                                pixels[i] = (byte)v;
                        }
			
                        stack.setPixels( pixels, z + 1 );
			
                }
		
        }
	
        private int[] domainToTemplate;
        private int[] templateToDomain;
	
        public static IntensityMap fromHistograms(int[] valueHistogramTemplate,
                                                  int[] valueHistogramDomain ) {
		
                return fromHistograms(valueHistogramTemplate,
                                      valueHistogramDomain,
                                      new Threshold( 0, 0 ),
                                      new Threshold( 0, 0 ) );
        }
	
        public static IntensityMap fromHistograms(
                int[] valueHistogramTemplateOriginal,
                int[] valueHistogramDomainOriginal,
                Threshold thresholdTemplate,
                Threshold thresholdDomain ) {
		
                IntensityMap result = new IntensityMap();
		
                int[] valueHistogramTemplate = valueHistogramTemplateOriginal.clone();
                int[] valueHistogramDomain = valueHistogramDomainOriginal.clone();
		
                // Clear all the below-threshold buckets in the histogram.
		
                for( int i = 0; i < thresholdTemplate.value; ++i )
                        valueHistogramTemplate[i] = 0;
		
                for( int i = 0; i < thresholdDomain.value; ++i )
                        valueHistogramDomain[i] = 0;
		
                // Work out the cumulative distribution from those.
		
                int[] cumulativeTemplate = histogramToCumulative( valueHistogramTemplate );
                int[] cumulativeDomain = histogramToCumulative( valueHistogramDomain );
		
                // How many super-threshold points are in each?
		
                long pointsInTemplate = cumulativeTemplate[cumulativeTemplate.length-1];
                long pointsInDomain = cumulativeDomain[cumulativeDomain.length-1];
		
                // Convert those to proportions:
		
                float[] cumulativeProportionsTemplate = new float[cumulativeTemplate.length];
                float[] cumulativeProportionsDomain = new float[cumulativeDomain.length];
		
                for (int i = 0; i < cumulativeTemplate.length; ++i)
                        cumulativeProportionsTemplate[i] =
                                cumulativeTemplate[i] / (float)pointsInTemplate;
		
                for( int i = 0; i < cumulativeDomain.length; ++i )
                        cumulativeProportionsDomain[i] =
                                cumulativeDomain[i] / (float)pointsInDomain;
		
                // -----------------------------------------------------------
		
                // Now build the map; whether we start with the domain
                // or template.
		
                int [] domainToTemplate = new int[valueHistogramDomain.length];
                int [] templateToDomain = new int[valueHistogramTemplate.length];
		
                {
                        int j = thresholdTemplate.value;
			
                        for (int i = thresholdDomain.value; i < valueHistogramDomain.length; ++i) {
                                float propGEinDomain = cumulativeProportionsDomain[i];
                                while( propGEinDomain > cumulativeProportionsTemplate[j] ) {
                                        ++j;
                                }
                                domainToTemplate[i] = j;
                        }
			
                }
		
                {
                        int j = 0;
			
                        for( int i = 1; i < valueHistogramTemplate.length; ++i ) {
                                float propGEinTemplate = cumulativeProportionsTemplate[i];
                                while( propGEinTemplate > cumulativeProportionsDomain[j] ) {
                                        ++j;
                                }
                                templateToDomain[i] = j;
                        }
			
                }
		
                result.domainToTemplate = domainToTemplate;
                result.templateToDomain = templateToDomain;
		
                return result;
		
        }
	
	
}

// A convenience class for returning the results of a Principal
// Components Analysis:

class PrincipalComponents {
	
        public double vectors[][];
        public double values[];
        public double meanXYZ[];
	
        public String toString( ) {
		
                String result = "Means in each dimension: ( " + meanXYZ[0] +
                        ", " + meanXYZ[1] + ", " + meanXYZ[2] + ")\n";
		
                for( int i = 0; i < 3; ++ i ) {
                        result += "  [ " + vectors[i][0] + ",   (eigenvalue: " +
                                values[i] + ")\n";
                        result += "    " + vectors[i][1] + ",\n";
                        result += "    " + vectors[i][2] + " ]\n";
                }
                return result;
        }
	
        class MagnitudeComparator implements Comparator {
		
                public int compare( Object a, Object b ) {
                        double x = (Double)a;
                        double y = (Double)b;
                        return Double.compare( Math.abs(x), Math.abs(y) );
                }
		
        }
	
        public FastMatrixTransform correctAspect;
	
        public PrincipalComponents( double [] values,
                                    double [][] vectors,
                                    double meanXYZ[],
                                    double relativeSpacingX,
                                    double relativeSpacingY,
                                    double relativeSpacingZ ) {
		
                correctAspect = new FastMatrixTransform(1.0).scale( relativeSpacingX,
								    relativeSpacingY,
								    relativeSpacingZ );
		
                // The only subtlety here is that we sort the passed-in
                // eigevectors and eigenvalues based on the absolute size of
                // the eigenvalues.
		
                if( values.length != 3 )
                        throw new IllegalArgumentException(
                                "There must be 3 eigenvalues (not " +
                                values.length + ")");
		
                // Sort based on the magnitude, but the Arrays.sort method
                // with a comparator only works on objects, so...
		
                Double [] boxedEigenValues = new Double[3];
                for( int i = 0; i < 3; ++ i ) boxedEigenValues[i] = values[i];
		
                java.util.Arrays.sort(boxedEigenValues,
                                      new MagnitudeComparator() );
		
                this.values = new double[3];
                for (int i = 0; i < 3; ++ i)
                        this.values[i] = boxedEigenValues[i];
		
                if( (vectors.length != 3) || (vectors[0].length != 3) ||
                    (vectors[1].length != 3) || (vectors[2].length != 3) ) {
                        throw new IllegalArgumentException(
                                "The eigenvecctors must be passed as double[3][3] array" );
                }
		
                boolean vectorsFilled[] = new boolean[3];
                vectorsFilled[0] = vectorsFilled[1] = vectorsFilled[2] = false;
		
                this.vectors = new double[3][];
		
                for (int i = 0; i < 3; ++i) {
                        int j;
                        for( j = 0;
                             (vectorsFilled[j]) || (this.values[j] != values[i]);
                             ++j )
                                ;
                        this.vectors[j] = vectors[i].clone();
                        vectorsFilled[j] = true;
                }
		
                if( meanXYZ.length != 3 )
                        throw new IllegalArgumentException(
                                "There must be 3 mean values (not " +
                                meanXYZ.length + ")" );
		
                this.meanXYZ = meanXYZ.clone();
		
                assert (this.values != null);
		
                assert (this.vectors[0] != null);
                assert (this.vectors[1] != null);
                assert (this.vectors[2] != null);
		
                assert (this.meanXYZ != null);
		
        }
	
}

public class PCA_Registration implements PlugIn {
	
        boolean keepSourceImages;
        ImagePlus [] sourceImages;
	
        public PCA_Registration( ) {
                keepSourceImages = true;
                sourceImages = null;
        }
	
        public void run(String arg) {
                chooseStacks();
                closeSourceImages();
        }
	
        public void closeSourceImages( ) {
                if( ! keepSourceImages ) {
                        for( int i = 0; i < sourceImages.length; ++i ) {
                                ImageWindow window = sourceImages[i].getWindow();
                                if( window != null )
                                        window.close();
                        }
                }
        }
	
        public Threshold threshold( int [] valueHistogram, float fraction ) {
		
                long belowThreshold = 0;
                int threshold = 0;
                long totalWithoutEnds = 0;
                long cumulative = 0;
                long cumulativeWithoutEnds = 0;
                for( int i = 1; i < 255; ++i ) {
                        totalWithoutEnds += valueHistogram[i];
                }
                for( int i = 0; i < 256; ++i ) {
                        cumulative += valueHistogram[i];
                        if( (i != 0) && (i != 255) )
                                cumulativeWithoutEnds += valueHistogram[i];
                        if( cumulativeWithoutEnds >= (fraction * totalWithoutEnds) ) {
                                return new Threshold( i, belowThreshold );
                        } else {
                                belowThreshold = cumulative;
                        }
			
                }
		
                return null;
        }
	
        public Color indexToColor( int index ) {
		
                Color foregroundColor = Toolbar.getForegroundColor();
		
                Color midColor = new Color( foregroundColor.getRed() / 3,
                                            foregroundColor.getGreen() / 3,
                                            foregroundColor.getBlue() / 3 );
		
                Color lowColor = new Color( foregroundColor.getRed() / 4,
                                            foregroundColor.getGreen() / 4,
                                            foregroundColor.getBlue() / 4 );
		
                switch( index ) {
                case 0:
                        return lowColor;
                case 1:
                        return midColor;
                case 2:
                        return foregroundColor;
                default:
                        return null;
                }
		
        }
	
        public static void scaleDoubleArray( double [] a, double factor ) {
                for( int i = 0; i < a.length; ++i )
                        a[i] *= factor;
        }
	
        public PrincipalComponents doPCA(ImagePlus image,
                                         Threshold threshold,
                                         boolean drawAxes ) {
		
                try {
			
                        int width = image.getWidth();
                        int height = image.getHeight();
                        int depth = image.getStackSize();
			
                        Calibration calibration = image.getCalibration();
			
                        double x_spacing = 1.0;
                        double y_spacing = 1.0;
                        double z_spacing = 1.0;
			
                        if( (calibration.pixelWidth != 0.0) &&
                            (calibration.pixelHeight != 0.0) &&
                            (calibration.pixelDepth != 0.0) ) {
				
                                x_spacing = 1.0;
                                y_spacing = calibration.pixelHeight /
                                        calibration.pixelWidth;
                                z_spacing = calibration.pixelDepth /
                                        calibration.pixelWidth;
                        }
			
                        System.out.println( "Aspect ratio is: " + x_spacing +
                                            ", " + y_spacing + ", " +
                                            z_spacing );
			
                        System.out.println( "Number below threshold is: " +
                                            threshold.belowThreshold );
                        int overAndAtThreshold = (width * height * depth) -
                                (int)threshold.belowThreshold;
                        System.out.println( "Number over and at threshold is: "
                                            + overAndAtThreshold );
			
                        int vectorLength = 3;
			
                        double [] mean = new double[vectorLength];
                        double [] sum = new double[vectorLength];
                        double [] variance = new double[vectorLength];
                        double [] sd = new double[vectorLength];
			
                        int [] n = new int[vectorLength];
			
                        ImageStack stack = image.getStack();
			
                        int i = 0;
			
                        byte [][] data = new byte[ depth ][];
			
                        for( int z = 0; z < image.getStackSize(); ++z )
                                data[z] = ( byte [] ) stack.getPixels( z + 1 );
			
                        double x_scaled, y_scaled, z_scaled;
			
                        // Calculate the sum of each dimension...
			
                        for( int z = 0; z < image.getStackSize(); ++z ) {
                                z_scaled = z * z_spacing;
                                for( int y = 0; y < image.getHeight(); ++y ) {
                                        y_scaled = y * y_spacing;
                                        for( int x = 0; x < image.getWidth(); ++x ) {
                                                x_scaled = x;
                                                byte value = data[ z ][ y * width + x ];
                                                int value_int = (int)( value & 0xFF );
						
                                                if( value_int >= threshold.value ) {
                                                        sum[0] += x_scaled;
                                                        sum[1] += y_scaled;
                                                        sum[2] += z_scaled;
                                                        i ++;
							
                                                }
						
                                        }
                                }
                        }
			
                        System.out.println( "Considered " + i + " points..." );
			
                        for( int j = 0; j < vectorLength; ++j ) {
                                System.out.println( "The sum of dimension " + j
                                                    + " was " + sum[j] );
                        }
			
                        // Calculate the mean of each dimension...
			
                        for( int j = 0; j < vectorLength; ++j ) {
                                mean[j] = sum[j] / overAndAtThreshold;
                                System.out.println( "The mean of dimension " +
                                                    j + " was " + mean[j] );
                        }
			
                        // Calculate the variance of each dimension,
                        // and the covariance matrix.
			
                        float[][] covariance =
                                new float[vectorLength][vectorLength];
			
                        for( int z = 0; z < image.getStackSize(); ++z ) {
                                z_scaled = z * z_spacing;
                                for( int y = 0; y < image.getHeight(); ++y ) {
                                        y_scaled = y * y_spacing;
                                        for( int x = 0; x < image.getWidth(); ++x ) {
                                                x_scaled = x;
                                                byte value = data[ z ][ y * width + x ];
                                                int value_int = (int)( value & 0xFF );
						
                                                if( value_int >= threshold.value ) {
							
                                                        double diff0 = x_scaled - mean[0];
                                                        double diff1 = y_scaled - mean[1];
                                                        double diff2 = z_scaled - mean[2];
							
                                                        variance[0] += diff0 * diff0;
                                                        variance[1] += diff1 * diff1;
                                                        variance[2] += diff2 * diff2;
							
                                                        covariance[0][0] += diff0 * diff0;
                                                        covariance[0][1] += diff0 * diff1;
                                                        covariance[0][2] += diff0 * diff2;
                                                        covariance[1][0] += diff1 * diff0;
                                                        covariance[1][1] += diff1 * diff1;
                                                        covariance[1][2] += diff1 * diff2;
                                                        covariance[2][0] += diff2 * diff0;
                                                        covariance[2][1] += diff2 * diff1;
                                                        covariance[2][2] += diff2 * diff2;
							
                                                        i ++;
                                                }
						
                                        }
                                }
                        }
			
                        for( int j = 0; j < vectorLength; ++j ) {
                                variance[j] /= ( overAndAtThreshold - 1 );
                                for( i = 0; i < vectorLength; ++i )
                                        covariance[j][i] /= ( overAndAtThreshold - 1 );
                                sd[j] = Math.sqrt( variance[j] );
                                // System.out.println( "The variance of dimension " + j + " was " +  variance[j] );
                                System.out.println( "The standard deviation of dimension " + j + " was " + sd[j] );
                        }
			
                        System.out.println( "Covariance:" );
                        for( int j = 0; j < vectorLength; ++j ) {
                                System.out.print( "  [" );
                                for( i = 0; i < vectorLength; ++i )
                                        System.out.print( " " + covariance[j][i] + " " );
                                System.out.println( "]" );
                        }
			
                        JacobiFloat jc=new JacobiFloat(covariance,200);
			
                        float[] eigenValuesFloat=jc.getEigenValues();
                        float[][] eigenVectorMatrixFloat=jc.getEigenVectors();
			
                        double[][] vectorsPacked=new double[3][3];
			
                        vectorsPacked[0][0] = eigenVectorMatrixFloat[0][0];
                        vectorsPacked[0][1] = eigenVectorMatrixFloat[1][0];
                        vectorsPacked[0][2] = eigenVectorMatrixFloat[2][0];
			
                        vectorsPacked[1][0] = eigenVectorMatrixFloat[0][1];
                        vectorsPacked[1][1] = eigenVectorMatrixFloat[1][1];
                        vectorsPacked[1][2] = eigenVectorMatrixFloat[2][1];
			
                        vectorsPacked[2][0] = eigenVectorMatrixFloat[0][2];
                        vectorsPacked[2][1] = eigenVectorMatrixFloat[1][2];
                        vectorsPacked[2][2] = eigenVectorMatrixFloat[2][2];
			
                        double[] eigenValues=new double[3];
			
                        eigenValues[0] = eigenValuesFloat[0];
                        eigenValues[1] = eigenValuesFloat[1];
                        eigenValues[2] = eigenValuesFloat[2];
			
                        PrincipalComponents pcaResults = new PrincipalComponents(
                                eigenValues,
                                vectorsPacked,
                                mean,
                                x_spacing,
                                y_spacing,
                                z_spacing );
			
                        System.out.println( pcaResults );
			
                        // if( drawAxes ) {
                        if( true ) {
				
                                // assert false; // This was for early debugging; if you
                                // draw the axes now, it'll disrupt the
                                // crude scaling done later...
				
                                double [] big0 = pcaResults.vectors[0].clone();
                                double [] big1 = pcaResults.vectors[1].clone();
                                double [] big2 = pcaResults.vectors[2].clone();
				
                                scaleDoubleArray( big0, 256 );
                                scaleDoubleArray( big1, 256 );
                                scaleDoubleArray( big2, 256 );
				
                                for( i = 0; i < depth; ++i ) {
					
                                        ImageProcessor imp = stack.getProcessor( i + 1 );
					
                                        imp.setColor( indexToColor( 0 ) );
					
                                        imp.moveTo( (int)( mean[0] ),
                                                    (int)( mean[1] / y_spacing) );
                                        imp.lineTo( (int) ((mean[0]) + big0[0]),
                                                    (int) ((mean[1] / y_spacing) + big0[1]) );
					
                                        imp.setColor( indexToColor( 1 ) );
					
                                        imp.moveTo( (int)( mean[0] ),
                                                    (int)( mean[1] / y_spacing ) );
                                        imp.lineTo( (int)( (mean[0]) + big1[0] ),
                                                    (int)( (mean[1] / y_spacing) + big1[1] ) );
					
                                        imp.setColor( indexToColor( 2 ) );
					
                                        imp.moveTo( (int)( mean[0] ),
                                                    (int)( mean[1] / y_spacing) );
                                        imp.lineTo( (int)( (mean[0]) + big2[0] ),
                                                    (int)( (mean[1] / y_spacing) + big2[1] ) );
					
                                }
				
                                image.updateAndDraw();
				
                        }
			
                        return pcaResults;
			
                } catch( Exception e ) {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        String stacktrace = sw.toString();
                        if (IJ.getApplet()==null) {
                                IJ.log("Error during PCA: " + e + "\n" + stacktrace );
                        }
                        return null;
                }
		
        }
	
        public void getSampleDistribution( ImagePlus image, double [] com, int [] valueHistogram ) {
		
                for( int i = 0; i < 256; ++i ) {
                        valueHistogram[i] = 0;
                }
		
                com[0] = 0.0f;
                com[1] = 0.0f;
                com[2] = 0.0f;
		
                double totalmass = 0;
		
                double x_acc = 0;
                double y_acc = 0;
                double z_acc = 0;
		
                ImageStack stack = image.getStack();
		
                int width = stack.getWidth();
		
                for( int z = 0; z < image.getStackSize(); ++z ) {
			
                        byte [] pixels = ( byte [] ) stack.getPixels( z + 1 );
			
                        for( int y = 0; y < image.getHeight(); ++y )
                                for( int x = 0; x < image.getWidth(); ++x ) {
					
                                        byte value = pixels[ y * width + x ];
                                        int value_int = (int)( value & 0xFF );
					
                                        valueHistogram[value_int]++;
					
                                        totalmass += value_int;
					
                                        x_acc += x * value_int;
                                        y_acc += y * value_int;
                                        z_acc += z * value_int;
					
                                }
			
                }
		
                com[0] = x_acc / totalmass;
                com[1] = y_acc / totalmass;
                com[2] = z_acc / totalmass;
		
                System.out.println( "x centre of mass is " + com[0] );
                System.out.println( "y centre of mass is " + com[1] );
                System.out.println( "z centre of mass is " + com[2] );
		
        }
	
        public int aboveThresholdLengthAlong( PrincipalComponents pcaResults,
                                              int eigenvectorNumber,
                                              ImagePlus image,
                                              Threshold threshold ) {
		
                if( (eigenvectorNumber < 0) || (eigenvectorNumber >= 3) )
                        throw new IllegalArgumentException( "Eigenvector number must be 0, 1 or 2 (not " +
                                                            eigenvectorNumber + ")" );
		
                FastMatrixTransform backFromCorrectAspect = pcaResults.correctAspect.inverse();
		
                backFromCorrectAspect.apply( pcaResults.vectors[eigenvectorNumber] );
		
                double [] v = new double[3];
		
                v[0] = backFromCorrectAspect.x;
                v[1] = backFromCorrectAspect.y;
                v[2] = backFromCorrectAspect.z;
		
                backFromCorrectAspect.apply( pcaResults.meanXYZ );
		
                double [] start_at = new double[3];
		
                start_at[0] = backFromCorrectAspect.x;
                start_at[1] = backFromCorrectAspect.y;
                start_at[2] = backFromCorrectAspect.z;
		
                ImageStack stack = image.getStack();
		
                double [] v_unit = FastMatrixTransform.normalize( v );
		
                int w = image.getWidth();
                int h = image.getHeight();
                int d = image.getStackSize();
		
                int [] lastAlong = new int[2];
                lastAlong[0] = lastAlong[1] = 0;
		
                for( int j = 0; j <2; ++j ) {
			
                        int sign = (j * 2) - 1;
                        int i = 0;
			
                        while( true ) {
				
                                int x, y, z;
				
                                x = (int)( start_at[0] + v_unit[0] * i * sign );
                                y = (int)( start_at[1] + v_unit[1] * i * sign );
                                z = (int)( start_at[2] + v_unit[2] * i * sign);
				
                                if( ( x >= 0 ) && ( x < w ) &&
                                    ( y >= 0 ) && ( y < h ) &&
                                    ( z >= 0 ) && ( z < d ) ) {
					
                                        byte value = ((byte [])stack.getPixels( z + 1 ))[ x + y * w ];
					
                                        int valueInteger = (int)( value & 0xFF );
					
                                        if( valueInteger >= threshold.value )
                                                lastAlong[j] = i;
					
                                } else {
					
                                        break;
					
                                }
				
                                ++i;
				
                        }
			
                }
		
                return ( lastAlong[0] + lastAlong[1] );
		
        }
	
        public void chooseStacks() {
		
                int[] wList = WindowManager.getIDList();
                if (wList==null) {
                        IJ.error("PCA_Registration.chooseStacks(): No images are open.");
                        return;
                }
		
                String[] titles = new String[wList.length+1];
                for (int i=0; i<wList.length; i++) {
                        ImagePlus imp = WindowManager.getImage(wList[i]);
                        titles[i] = imp!=null?imp.getTitle():"";
                }
		
                String none = "*None*";
                titles[wList.length] = none;
		
                GenericDialog gd = new GenericDialog("RGB Merge");
                gd.addChoice("Template stack:", titles, titles[0]);
                gd.addChoice("Stack to transform:", titles, titles[1]);
                gd.addCheckbox("Keep source images", true);
                gd.showDialog();
                if (gd.wasCanceled())
                        return;
		
                int[] index = new int[2];
                index[0] = gd.getNextChoiceIndex();
                index[1] = gd.getNextChoiceIndex();
                keepSourceImages = gd.getNextBoolean();
		
                sourceImages = new ImagePlus[2];
		
                int stackSize = 0;
                int width = 0;
                int height = 0;
		
                sourceImages[0] = WindowManager.getImage(wList[index[0]]);
                sourceImages[1] = WindowManager.getImage(wList[index[1]]);
		
                double [] com0 = new double[3];
                double [] com1 = new double[3];
		
                int [] valueHistogram0 = new int[256];
                int [] valueHistogram1 = new int[256];
		
                getSampleDistribution( sourceImages[0], com0, valueHistogram0 );
                getSampleDistribution( sourceImages[1], com1, valueHistogram1 );
		
                Threshold threshold0 = threshold( valueHistogram0, 0.15f );
		
                System.out.println( "Threshold for image 0 is at: " + threshold0.value +
                                    " (number below: " + threshold0.belowThreshold + ")" );
		
                Threshold threshold1 = threshold( valueHistogram1, 0.15f );
		
                System.out.println( "Threshold for image 1 is at: " + threshold1.value +
                                    " (number below: " + threshold1.belowThreshold + ")" );
		
                PrincipalComponents templatePCs = doPCA( sourceImages[0], threshold0, false );
                PrincipalComponents domainPCs = doPCA( sourceImages[1], threshold1, false );
		
                // Now find an intensity mapping...
		
                IntensityMap intensityMap = IntensityMap.fromHistograms( valueHistogram0, valueHistogram1, threshold0, threshold1 );
		
                intensityMap.setup( "", sourceImages[1] );
		
                // Try it on the domain image...
		
                intensityMap.run( sourceImages[1].getProcessor() );
		
                /* We only consider the first and second eigenvectors here; we
                   line up the first and rotate around that until the second
                   eigenvectors line up... */

                // ----- Calculate the translations ---------------------------------------
		
                double [][] tmpTranslateDomain = { { 0, 0, 0, -domainPCs.meanXYZ[0] },
                                                   { 0, 0, 0, -domainPCs.meanXYZ[1] },
                                                   { 0, 0, 0, -domainPCs.meanXYZ[2] } };
		
                FastMatrixTransform translateDomainToMean = new FastMatrixTransform( tmpTranslateDomain ).plus( new FastMatrixTransform(1) );
		
                FastMatrixTransform translateDomainBackFromMean = new FastMatrixTransform( tmpTranslateDomain ).scale(-1,-1,-1).plus( new FastMatrixTransform(1) );
		
                double [][] tmpTranslateTemplate = { { 0, 0, 0, -templatePCs.meanXYZ[0] },
                                                     { 0, 0, 0, -templatePCs.meanXYZ[1] },
                                                     { 0, 0, 0, -templatePCs.meanXYZ[2] } };
		
                FastMatrixTransform translateTemplateToMean = new FastMatrixTransform( tmpTranslateTemplate ).plus( new FastMatrixTransform(1) );
                FastMatrixTransform translateTemplateBackFromMean = new FastMatrixTransform( tmpTranslateTemplate ).scale(-1,-1,-1).plus( new FastMatrixTransform(1) );
		
                FastMatrixTransform finalTranslationsMatrix = FastMatrixTransform.translate( templatePCs.meanXYZ[0] - domainPCs.meanXYZ[0],
											     templatePCs.meanXYZ[1] - domainPCs.meanXYZ[1],
											     templatePCs.meanXYZ[2] - domainPCs.meanXYZ[2] );
		
                System.out.println( "finalTranslationsMatrix is:\n" + finalTranslationsMatrix.toStringIndented("  ") );
		
                // ------------------------------------------------------------------------
		
                double [] v2_domain = domainPCs.vectors[2].clone();
                double [] v1_domain = domainPCs.vectors[1].clone();
                double [] v2_template = templatePCs.vectors[2].clone();
                double [] v1_template = templatePCs.vectors[1].clone();
		
                FastMatrixTransform [] rotations = new FastMatrixTransform[4];
		
                rotations[0] = new FastMatrixTransform( FastMatrix.rotateToAlignVectors( v2_template,
											 v1_template,
											 v2_domain,
											 v1_domain ) );
		
                scaleDoubleArray( v1_domain, -1 );
		
                rotations[1] = new FastMatrixTransform( FastMatrix.rotateToAlignVectors( v2_template,
											 v1_template,
											 v2_domain,
											 v1_domain ) );
		
                scaleDoubleArray( v2_domain, -1 );
                scaleDoubleArray( v1_domain, -1 );
		
                rotations[2] = new FastMatrixTransform( FastMatrix.rotateToAlignVectors( v2_template,
											 v1_template,
											 v2_domain,
											 v1_domain ) );
		
                scaleDoubleArray( v1_domain, -1 );
		
                rotations[3] = new FastMatrixTransform( FastMatrix.rotateToAlignVectors( v2_template,
											 v1_template,
											 v2_domain,
											 v1_domain ) );
		
                int lengthIn0, lengthIn1;
		
                lengthIn0 = aboveThresholdLengthAlong( templatePCs,
                                                       2,
                                                       sourceImages[0],
                                                       threshold0 );
		
                lengthIn1 = aboveThresholdLengthAlong( domainPCs,
                                                       2,
                                                       sourceImages[1],
                                                       threshold1 );
		
                System.out.println( "Length in image 0: " + lengthIn0 );
                System.out.println( "Length in image 1: " + lengthIn1 );
		
                double scaleFactor = ((double)lengthIn0) / lengthIn1;
		
                FastMatrixTransform scaling;
		
                if( (lengthIn0 == 0) || (lengthIn1 == 0) )
                        // (Something's gone wrong, but struggle on without a
                        // guess at the scaling...)
                        scaling = new FastMatrixTransform( 1.0 );
                else
                        scaling = new FastMatrixTransform( scaleFactor );
		
                System.out.println( "... so scaling factor is: " + scaleFactor );
		
                // Now chain these together...
		
                OrderedTransformations [] transformations = new OrderedTransformations[ 4 ];
                double [] scores = new double[4];
		
                for( int i = 0; i < 4; ++i ) {
			
                        transformations[i] = new OrderedTransformations();
			
                        transformations[i].addLast( domainPCs.correctAspect );
                        transformations[i].addLast( translateDomainToMean );
                        transformations[i].addLast( rotations[i] );
                        transformations[i].addLast( scaling );
                        transformations[i].addLast( translateDomainBackFromMean );
                        transformations[i].addLast( finalTranslationsMatrix );
                        transformations[i].addLast( templatePCs.correctAspect.inverse() );
			
                        System.out.println( "... before reduction, transform has " + transformations[i].number()
                                            + " elements:\n" + transformations[i] );
			
                        transformations[i].reduce( );
			
                        System.out.println( "... after reduction, transform has " + transformations[i].number()
                                            + " elements:\n" + transformations[i] );
			
                        scores[i] = transformations[i].scoreTransformationThresholded(
                                sourceImages[0],
                                sourceImages[1],
                                threshold0,
                                threshold1,
                                7 );
			
                        System.out.println( "Score of transformation " + i + " was " + scores[i] );
			
                        System.gc();
			
                }
		
                int bestIndex = 0;
                double bestScore = scores[0];
                for( int i = 1; i < 4; ++ i )
                        if( scores[i] < bestScore ) {
                                bestIndex = i;
                                bestScore = scores[i];
                        }
		
                System.out.println( "Picked transformation with index: " + bestIndex +
                                    " and score: " + bestScore );
		
                transformations[bestIndex].createNewImage( sourceImages[0], sourceImages[1], true );
		
        }
	
}
