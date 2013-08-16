/*  -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.util.Enumeration;
import java.util.Hashtable;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.measure.Calibration;

import pal.math.ConjugateDirectionSearch;
import pal.math.MultivariateFunction;
import vib.TransformedImage;

/* This class just maps a "fluorescence" value to an integer in the
   range [minValue...maxValue] */

 class IdealImager {
	 int minValue;
	 int maxValue;
	 float gradient;
	 float offset;
	 public IdealImager( int minValue, int maxValue, float gradient, float offset ) {
		 this.offset = offset;
		 this.gradient = gradient;
		 this.minValue = minValue;
		 this.maxValue = maxValue;
	 }
	 public int map(double fluorescence) {
		 int iv = (int)( gradient * fluorescence + offset );
		 if( iv < minValue )
			 iv = minValue;
		 else if( iv > maxValue )
			 iv = maxValue;
		 return iv;
	 }
}


class FluorescenceOptimizer implements MultivariateFunction {
	
	int n;
	IdealImager imagers[];

	int minValue;
	int maxValue;

	double upperFluorescenceBound;
	double lowerFluorescenceBound;

	public FluorescenceOptimizer(IdealImager [] imagers) {
		this.imagers = imagers;
		n = imagers.length;
		if( n < 1 ) {
			throw new RuntimeException("There must be at least one IdealImager in the array");
		}
		minValue = imagers[0].minValue;
		maxValue = imagers[0].maxValue;
		for( int i = 1; i < n; ++i ) {
			if( imagers[i].minValue != minValue || imagers[i].maxValue != maxValue ) {
				throw new RuntimeException("All IdealImagers must have the same value range.");
			}
		}
	}

	int [] valuesInImages;

	public void setRealResult( int [] valuesInImages ) {
		this.valuesInImages = valuesInImages;
	}

	public double evaluate(double argument[]) {
		float total = 0;
		for( int i = 0; i < n; ++i ) {
			int value = imagers[i].map(argument[0]);
			int diff = valuesInImages[i] - value;
			total += diff * diff;
		}
		return total;
	}

	public double optimize(float startFluorescence) {
		return -1;
		// FIXME
	}

	public int getNumArguments() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public double getLowerBound(int n) {
		return lowerFluorescenceBound;
	}

	public double getUpperBound(int n) {
		return upperFluorescenceBound;
	}

	void setLowerBound(float min) {
		lowerFluorescenceBound = min;
	}

	void setUpperBound(float max) {
		upperFluorescenceBound = max;
	}

}
public class Exposure_Blend_Two_Stacks implements PlugIn {

	public void run(String arg0) {

		/* Make sure image 0 is d and image 1 is b... */

		int maxCountsAllowed = 10000;

		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("No images are open.");
			return;
		}

		String[] titles = new String[wList.length + 1];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp != null ? imp.getTitle() : "";
		}

		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Exposure Blend");
		gd.addChoice("Stack with detail in dark regions (appears brighter):", titles, titles[0]);
		gd.addChoice("Stack with detail in bright regions (appears darker):", titles, titles[1]);
		gd.addCheckbox("Keep source images", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		// The number of images to blend; currently set at 2:
		int imagesToBlend = 2;

		int[] index = new int[imagesToBlend];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		ImagePlus[] sourceImages = new ImagePlus[imagesToBlend];

		sourceImages[0] = WindowManager.getImage(wList[index[0]]);
		sourceImages[1] = WindowManager.getImage(wList[index[1]]);

		int width = sourceImages[0].getWidth();
		int height = sourceImages[0].getHeight();
		int depth = sourceImages[0].getStackSize();

		int i;

		for (i = 1; i < imagesToBlend; ++i) {
			if (!(width == sourceImages[i].getWidth() &&
			    height == sourceImages[i].getHeight() &&
			    depth == sourceImages[i].getStackSize())) {
				IJ.error("The dimensions of image stack " +
				    sourceImages[i].getTitle() +
				    " do not match those of " + sourceImages[0].getTitle());
				return;
			}
		}

		int type0 = sourceImages[0].getType();
		for (i = 1; i < imagesToBlend; ++i) {
			if (type0 != sourceImages[i].getType()) {
				IJ.error("Can't exposure blend images of different types.");
				return;
			}

			int bitDepth = sourceImages[0].getBitDepth();

			System.out.println("bitDepth: " + bitDepth);

			if (!(bitDepth == 8 || bitDepth == 16 || bitDepth == 24)) {
				IJ.error("Images must be 8-bit, 16-bit or RGB");
				return;
			}

			if (bitDepth == 24) {
				for (i = 0; i < imagesToBlend; ++i) {
					sourceImages[i] = RGB_to_Luminance.convertToLuminance(sourceImages[i]);
					sourceImages[i].show();
				}
				type0 = sourceImages[0].getType();
				bitDepth = sourceImages[0].getBitDepth();
			}

			ImageStack[] stacks = new ImageStack[imagesToBlend];
			for (i = 0; i < imagesToBlend; ++i) {
				stacks[i] = sourceImages[i].getStack();
			}

			// FIXME: extend to more images...

			/* We need to know the value range in both stacks before
			 * picking over- and under-saturation thresholds. */

			TransformedImage ti = new TransformedImage(
			    sourceImages[0],
			    sourceImages[1]);

			float[] range = ti.getValuesRange();

			IJ.error("FIXME: deliberately broken for the moment...");

/*
			Histogram_2D histogram = new Histogram_2D();

			histogram.start2DHistogram(
			    range[0], // minimumValue
			    range[1], // maximumValue
			    256);    // number of bins

			float valueRangeWidth = range[1] - range[0];
			histogram.collectStatisticsFor(
			    range[0] + (valueRangeWidth / 128),
			    range[1] - (valueRangeWidth / 128));

			histogram.addImagePlusPair(sourceImages[0], sourceImages[1]);

			histogram.calculateCorrelation();

			float a = histogram.fittedGradient;
			float b = histogram.fittedYIntercept;

			ImagePlus[] resultHistograms = histogram.getHistograms();

			histogram.frame2DHistogram(
			    "2D Histogram of Values",
			    resultHistograms[1],
			    sourceImages[0].getTitle(),
			    range[0], range[1],
			    sourceImages[1].getTitle(),
			    range[0], range[1],
			    Histogram_2D.SELF_INFORMATION);

			// So we have a and b from which we can calculate m and n:

			float m = -b / (a * 255);
			float n = (255 - b) / (a * 255);

			System.out.println("Got m: " + m);
			System.out.println("Got n: " + n);

			IdealImager[] imagers = new IdealImager[imagesToBlend];
			imagers[0] = new IdealImager(0, 255, 255, 0);
			imagers[1] = new IdealImager(0, 255, (255 / (n - m)), ((-m * 255) / (n - m)));

			FluorescenceOptimizer fo = new FluorescenceOptimizer(imagers);

			fo.setLowerBound(Math.min(0,m));
			fo.setUpperBound(Math.max(1,n));
			
			ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();
			optimizer.illc = true;
			optimizer.step = 0.1;
			
			int[] valuesArray = new int[imagesToBlend];
			double[] startValues = new double[1];
*/

			/* Now go through the images finding pairs of values and working
			 * out what fluorescence value would map to these most closely. */

/*
			ImageStack finalStack = new ImageStack(width, height);

			for (int z = 0; z < depth; ++z) {
				System.out.println("Making final stack slice: " + z);
				float[] sliceFloats = new float[width * height];
				FloatProcessor fp = new FloatProcessor(width, height);
				byte[][] bytePixels = new byte[imagesToBlend][];
				short[][] shortPixels = new short[imagesToBlend][];
				if (bitDepth == 8) {
					for (i = 0; i < imagesToBlend; ++i) {
						bytePixels[i] = (byte[]) stacks[i].getPixels(z + 1);
					}
				} else if (bitDepth == 16) {
					for (i = 0; i < imagesToBlend; ++i) {
						shortPixels[i] = (short[]) stacks[i].getPixels(z + 1);
					}
				}
				for (int p = 0; p < sliceFloats.length; ++p) {
					
					if( p < width * 73 ) {
						continue;
					}
					
					if( p > width * 73 ) {
						break;
					}
					
					System.out.print("Doing one: ");
					
					for (i = 0; i < imagesToBlend; ++i) {
						if (bitDepth == 8) {
							valuesArray[i] = bytePixels[i][p] & 0xFF;
						} else if (bitDepth == 16) {
							valuesArray[i] = shortPixels[i][p];
						}
					}
					
					
					System.out.println("Considering values array:");
					for( i = 0; i < imagesToBlend; ++i ) {
						System.out.println("   "+valuesArray[i]);
					}
					// Now we have the values, run the optimizer.
					fo.setRealResult(valuesArray);

					startValues[0] = 0.5;

					optimizer.optimize(fo, startValues, 100, 0.001);

					// Now startValues[0] should be something more helpful:
					sliceFloats[p] = (float) startValues[0];
					
					System.out.println(""+sliceFloats[p]);
				}

				fp.setPixels(sliceFloats);
				finalStack.addSlice("", fp);

			}

			ImagePlus finalImage = new ImagePlus("final blended image", finalStack);
*/		}
	}
}

	/*
        Hashtable<String,Integer> cases = new Hashtable<String,Integer>();

        cases.put("both 0", 0);     // 0 Black
        cases.put("both 255", 0);   // 1 White
        cases.put("undefined", 0);  // 2 Magenta
        cases.put("just d / 0", 0); // 3 Yellow
        cases.put("just b / 1", 0); // 4 Blue
        cases.put("b and d", 0);    // 5 Green
	*/

/*
        float maxFloat = 0;
        
        {
            float floatValues[] = new float[256 * 256];

            for (int avalue = 0; avalue < 256; ++avalue) {
                for (int bvalue = 0; bvalue < 256; ++bvalue) {

                    long count = correspondingValues[avalue][bvalue];

                    if (count > maxCountsAllowed) {
                        count = maxCountsAllowed;
                    }

                    float fcount = (float)Math.log(count);
                    // float fcount = (float)count;
                    
                    floatValues[(255 - bvalue) * 256 + avalue] = fcount;

                    if( fcount > maxFloat )
                        maxFloat = fcount;
                }
            }

            for( int x = 0; x < 256; ++x ) {
                int y = (int)( a * x + b );
                if( y >= 0 && y < 256 ) {
                    floatValues[(255-y)*256+x] = (x % 2 == 0) ? maxFloat : 0;
                }
            }
            
            FloatProcessor fp = new FloatProcessor(256,256);
            fp.setPixels(floatValues);
            ImageStack newStack = new ImageStack(256, 256);
            newStack.addSlice("", fp);
            ImagePlus newImagePlus = new ImagePlus("2D Histogram", newStack);
            newImagePlus.show();
            
        }
        
        ImageStack blendedStack = new ImageStack(width, height);
        
        ImageStack caseStack = new ImageStack(width,height);
        
        for( int z = 0; z < depth; ++z ) {
            
            float [] blendedPixels = new float[width*height];
            byte [] casePixels = new byte[width*height];
            
            for( int i = 0; i < width*height; ++i ) {
                
                int v0 = pixels0[z][i] & 0xFF;
                int v1 = pixels1[z][i] & 0xFF;
                
                if( v0 == 0 && v1 == 0 ) {
                    blendedPixels[i] = 0;
                    casePixels[i] = 0;
                    cases.put("both 0", cases.get("both 0") + 1);                    
                } else if( v0 == 255 && v1 == 255 ) {
                    blendedPixels[i] = (float)n;
                    casePixels[i] = 1;
                    cases.put("both 255", cases.get("both 255") + 1);
                } else if( (v0 == 0 && v1 == 255) || (v0 == 255 && v1 == 0) ) {
                    // Strictly speaking, we don't know what to
                    // make of these cases:
                    System.out.println("Warning: v0: "+v0+", v1: "+v1);
                    blendedPixels[i] = 0;
                    casePixels[i] = 2;
                    cases.put("undefined", cases.get("undefined") + 1);
                } else if( (v0 == 0 || v0 == 255) && (v1 > 0 && v1 < 255) ) {
                    // Then binverse exists:
                    double binverse = v1 * (n - m) /  255  + m;
                    blendedPixels[i] = (float) binverse;
                    casePixels[i] = 4;
                    cases.put("just b / 1", cases.get("just b / 1") + 1);
                } else if( (v1 == 0 || v1 == 255) && (v0 > 0 && v0 < 255) ) {
                    // Then dinverse exists:
                    double dinverse = v0 / 255;
                    blendedPixels[i] = (float) dinverse;
                    casePixels[i] = 3;
                    cases.put("just d / 0", cases.get("just d / 0") + 1);
                } else {
                    // Both inverses exist:
                    double dinverse = v0 / 255;
                    double binverse = v1 * (n - m) /  255  + m;
                    blendedPixels[i] = (float)( (dinverse + binverse) / 2 );
                    casePixels[i] = 5;
                    cases.put("b and d", cases.get("b and d") + 1);
                }
                                  
            }
            
            FloatProcessor fp = new FloatProcessor(width,height);
            fp.setPixels(blendedPixels);
            
            ByteProcessor bp = new ByteProcessor(width,height);
            bp.setPixels(casePixels);
            
            blendedStack.addSlice("",fp);
            caseStack.addSlice("",bp);
        }
        
        ImagePlus blendedImage = new ImagePlus( "blended", blendedStack );
        blendedImage.show();
                     
        byte [] reds =   { (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00 };
        byte [] greens = { (byte)0x00, (byte)0xFF, (byte)0x00, (byte)0xFF, (byte)0x00, (byte)0xFF };
        byte [] blues =  { (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0xFF, (byte)0x00 };

        ColorModel cm = new IndexColorModel( 8, 6, reds, greens, blues );

        caseStack.setColorModel(cm);
        ImagePlus caseImage = new ImagePlus( "cases", caseStack );
        caseImage.show();
        
        Enumeration<String> e = cases.keys();
        while( e.hasMoreElements() ) {
            String k = e.nextElement();
            System.out.println(""+k+": "+cases.get(k));
        }
*/
