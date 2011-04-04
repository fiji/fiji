package imageware;

import ij.ImageStack;
import ij.process.FloatProcessor;

import java.awt.Image;
import java.util.Random;

/**
 * Class DoublePointwise.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class DoublePointwise extends DoubleAccess implements Pointwise {

	//------------------------------------------------------------------
	//
	//	Constructors section
	//
	//------------------------------------------------------------------
	protected DoublePointwise(int nx, int ny, int nz) 		{ super(nx, ny, nz); }
	protected DoublePointwise(Image image, int mode) 		{ super(image, mode); }

	protected DoublePointwise(ImageStack stack, int mode) 	{ super(stack, mode); }
	protected DoublePointwise(ImageStack stack, byte chan) 		{ super(stack, chan); }

	protected DoublePointwise(byte[] array, int mode) 		{ super(array, mode); }
	protected DoublePointwise(byte[][] array, int mode) 		{ super(array, mode); }
	protected DoublePointwise(byte[][][] array, int mode) 	{ super(array, mode); }
	protected DoublePointwise(short[] array, int mode) 		{ super(array, mode); }
	protected DoublePointwise(short[][] array, int mode) 	{ super(array, mode); }
	protected DoublePointwise(short[][][] array, int mode) 	{ super(array, mode); }
	protected DoublePointwise(float[] array, int mode) 		{ super(array, mode); }
	protected DoublePointwise(float[][] array, int mode) 	{ super(array, mode); }
	protected DoublePointwise(float[][][] array, int mode) 	{ super(array, mode); }
	protected DoublePointwise(double[] array, int mode) 		{ super(array, mode); }
	protected DoublePointwise(double[][] array, int mode) 	{ super(array, mode); }
	protected DoublePointwise(double[][][] array, int mode)	{ super(array, mode); }

	/**
	* Fill this imageware with a constant value.
	*
	* @param	value	the constant value
	*/
	public void fillConstant(double value) {
		double typedValue = (double)value;
		double[] slice = null;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++)
				slice[k] = typedValue;
		}
	}

	/**
	* Fill this imageware with ramp.
	*/
	public void fillRamp() {
		int off = 0;
		double[] slice = null;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++)
				slice[k] = (double)(off+k);
			off += nxy;
		}
	}
	/**
	* Generate a gaussian noise with a range [-amplitude..amplitude].
	*
	* @param amplitude		amplitude of the noise
	*/
	public void fillGaussianNoise(double amplitude) {
		Random rnd = new Random();
		double[] slice = null;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)((rnd.nextGaussian())*amplitude);
			}
		}
	}
	
	/**
	* Generate a uniform noise with a range [-amplitude..amplitude].
	*
	* @param amplitude		amplitude of the noise
	*/
	public void fillUniformNoise(double amplitude) {
		Random rnd = new Random();
		double[] slice = null;
		amplitude *= 2.0;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)((rnd.nextDouble()-0.5)*amplitude);
			}
		}
	}
	
	/**
	* Generate a salt and pepper noise.
	*
	* @param	amplitudeSalt		amplitude of the salt noise
	* @param	amplitudePepper		amplitude of the pepper noise
	* @param	percentageSalt		percentage of the salt noise
	* @param	percentagePepper	percentage of the pepper noise
	*/
	public void fillSaltPepper(double amplitudeSalt, double amplitudePepper, double percentageSalt, double percentagePepper) {
		Random rnd = new Random();
		int index, z;
		if (percentageSalt > 0) {
			double nbSalt = nxy*nz/percentageSalt;
			for( int k=0; k < nbSalt; k++) {
				index = (int)(rnd.nextDouble() * nxy);
				z = (int)(rnd.nextDouble() * nz);
				((double[])data[z])[index] = (double)(rnd.nextDouble()*amplitudeSalt);
			}
		}
		if (percentagePepper > 0) {
			double nbPepper = nxy*nz/percentagePepper;
			for( int k=0; k < nbPepper; k++) {
				index = (int)(rnd.nextDouble() * nxy);
				z = (int)(rnd.nextDouble() * nz);
				((double[])data[z])[index] = (double)(-rnd.nextDouble()*amplitudeSalt);
			}
		}	
	}
	


	/**
	* Build an ImageStack of ImageJ.
	*/
	public ImageStack buildImageStack() {
		ImageStack imagestack = new ImageStack(nx, ny);
		for (int z=0; z<nz; z++) {

			FloatProcessor ip = new FloatProcessor(nx, ny);
			float pix[] = (float[])ip.getPixels();
			for (int k=0; k<nxy; k++)
				pix[k] = (float)(((double[])data[z])[k]);
			imagestack.addSlice("" + z, ip);
		}
		return imagestack;
	}


	/**
	* Invert the pixel intensity.
	*/
	public void invert() {
		double max = -Double.MAX_VALUE;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				if ((slice[k]) > max) 
					max = slice[k];
			}
		}
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)(max-((double)(slice[k])));
			}
		}
	}

	/**
	* Negate the pixel intensity.
	*/
	public void negate() {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)(-((double)(slice[k])));
			}
		}
	}

	/**
	* Clip the pixel intensity into [0..255].
	*/
	public void clip() {
		clip(0.0, 255.0);
	}

	/**
	* Clip the pixel intensity into [minLevel..maxLevel].
	*
	* @param minLevel		double value given the threshold 
	* @param maxLevel		double value given the threshold 
	*/
	public void clip(double minLevel, double maxLevel) {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			double value;
			double min = (double)minLevel;
			double max = (double)maxLevel;
			for(int k=0; k<nxy; k++) {
				value = (double)(slice[k]);
				if (value < min)
					slice[k] = min;
				if (value > max)
					slice[k] = max;
			}
		}
	}
	
	/**
	* Rescale the pixel intensity into [0..255].
	*/
	public void rescale() {
		double maxImage = -Double.MAX_VALUE;
		double minImage =  Double.MAX_VALUE;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				if ((slice[k]) > maxImage) 
					maxImage = slice[k];
				if ((slice[k]) < minImage) 
					minImage = slice[k];
			}
		}
		double a;
		if (minImage-maxImage == 0) {
			a = 1.0;
			minImage = 128.0;
		}
		else {
			a = 255.0 / (maxImage-minImage);
		}
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)(a*(((double)(slice[k]))-minImage));
			}
		}
	}
	
	/**
	* Rescale the pixel intensity into [minLevel..maxLevel].
	*
	* @param minLevel		double value given the threshold 
	* @param maxLevel		double value given the threshold 
	*/
	public void rescale(double minLevel, double maxLevel) {
		double maxImage = -Double.MAX_VALUE;
		double minImage =  Double.MAX_VALUE;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				if ((slice[k]) > maxImage) 
					maxImage = slice[k];
				if ((slice[k]) < minImage) 
					minImage = slice[k];
			}
		}
		double a;
		if (minImage-maxImage == 0) {
			a = 1.0;
			minImage = (maxLevel-minLevel)/2.0;
		}
		else {
			a = (maxLevel-minLevel) / (maxImage-minImage);
		}
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)(a*(((double)(slice[k]))-minImage) + minLevel);
			}
		}
	}

	/**
	* Rescale the pixel intensity with a linear curve passing through 
	* (maxLevel-minLevel)/2 at the 0 input intensity.
	*
	* @param minLevel		double value given the threshold 
	* @param maxLevel		double value given the threshold 
	*/
	public void rescaleCenter(double minLevel, double maxLevel) {
		double maxImage = -Double.MAX_VALUE;
		double minImage =  Double.MAX_VALUE;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				if ((slice[k]) > maxImage) 
					maxImage = slice[k];
				if ((slice[k]) < minImage) 
					minImage = slice[k];
			}
		}
		double center = (maxLevel+minLevel)/2.0;
		double a;
		if ( minImage-maxImage == 0) {
			a = 1.0;
			minImage = (maxLevel-minLevel)/2.0;
		}
		else {
			if ( Math.abs(maxImage) > Math.abs(minImage) )
				a = (maxLevel-center) / Math.abs(maxImage);
			else
				a = (center-minLevel) / Math.abs(minImage);
		}
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)(a*(((double)(slice[k]))-minImage) + center);
			}
		}
	}
	
	/**
	* Compute the absolute value of this imageware.
	*/
	public void abs() {
		double zero = (double)0.0;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				if (slice[k] < zero)
					slice[k] = -slice[k];
			}
		}
	}

	/**
	* Compute the log of this imageware.
	*/
	public void log() {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)Math.log(slice[k]);
			}
		}
	}
	
	/**
	* Compute the exponential of this imageware.
	*/
	public void exp() {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)Math.exp(slice[k]);
			}
		}
	}

	/**
	* Compute the square root of this imageware.
	*/
	public void sqrt() {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)Math.sqrt(slice[k]);
			}
		}
	}

	/**
	* Compute the square of this imageware.
	*/
	public void sqr() {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] *= slice[k];
			}
		}
	}

	/**
	* Compute the power of a of this imageware.
	*
	* @param	a	exponent
	*/
	public void pow(double a) {
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = (double)Math.pow(slice[k], a);
			}
		}
	}
	
	/**
	* Add a constant value to this imageware.
	*/
	public void add(double constant) {
		double cst = (double)constant;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] += cst;
			}
		}
	}
	
	/**
	* Multiply a constant value to this imageware.
	*
	* @param	constant	the constant value
	*/
	public void multiply(double constant) {
		double cst = (double)constant;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] *= cst;
			}
		}
	}
	
	/**
	* Subtract a constant value to this imageware.
	*
	* @param	constant	the constant value
	*/
	public void subtract(double constant) {
		double cst = (double)constant;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] -= cst;
			}
		}
	}
	
	/**
	* Divide by a constant value to this imageware.
	*
	* @param	constant	the constant value
	*/
	public void divide(double constant) {
		if (constant == 0.0)
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to divide because the constant is 0.\n" +
				"-------------------------------------------------------\n"
			);
		double cst = (double)constant;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] /= cst;
			}
		}
	}

	/**
	* Threshold a imageware in two levels 0 and 255.
	*
	* All the pixels values strictly greater than 'thresholdValue' and are set
	* to 0. The remaining values are set to 255.
	*
	* @param thresholdValue		double value given the threshold 
	*/
	public void threshold(double thresholdValue) {
		threshold(thresholdValue, 0.0, 255.0);
	}

	/**
	* Threshold a imageware in two levels minLevel and maxLevel.
	*
	* All the pixels values strictly greater than 'thresholdValue' and are set
	* to maxLevel. The remaining values are set to minLevel.
	*
	* @param thresholdValue		double value given the threshold 
	* @param minLevel			double value given the minimum level 
	* @param maxLevel			double value given the maximum level 
	*/
	public void threshold(double thresholdValue, double minLevel, double maxLevel) {
		double low = (double)(minLevel);
		double high = (double)(maxLevel);
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] = ((double)(slice[k]) > thresholdValue ? high : low);
			}
		}
	}

	/**
	* Apply a soft thresholding.
	*
	* All the pixels values strictly greater than '-thresholdValue' and stricty 
	* lower than 'thresholdValue' set to 0. The remaining positive values are 
	* reduced by 'thresholdvalue'; the remaining negative values are 
	* augmented by 'thresholdValue'.
	*
	* @param thresholdValue		double value given the threshold 
	*/
	public void thresholdSoft(double thresholdValue) {
		double zero = (double)(0.0);
		double pixel;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				pixel = (double)(slice[k]);
				slice[k] = (pixel<=-thresholdValue ? 
					(double)(pixel+thresholdValue) : 
					(pixel>thresholdValue ? (double)(pixel-thresholdValue) : zero));
			}
		}
	}
	
	/**
	* Apply a hard thresholding.
	*
	* All the pixels values strictly greater than '-thresholdValue' and stricty 
	* lower than 'thresholdValue' are set to 0. The remaining values are 
	* unchanged.
	*
	* @param thresholdValue		double value given the threshold 
	*/
	public void thresholdHard(double thresholdValue) {
		double zero = (double)(0.0);
		double pixel;
		double[] slice;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				pixel = (double)(slice[k]);
				if (pixel > -thresholdValue && pixel < thresholdValue)
					slice[k] =  zero; 
			}
		}
	}
	
	/**
	* Add a gaussian noise with a range [-amplitude..amplitude].
	*
	* @param amplitude		amplitude of the noise
	*/
	public void addGaussianNoise(double amplitude) {
		Random rnd = new Random();
		double[] slice = null;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] += (double)((rnd.nextGaussian())*amplitude);
			}
		}
	}

	/**
	* Add a uniform noise with a range [-amplitude..amplitude].
	*
	* @param amplitude		amplitude of the noise
	*/
	public void addUniformNoise(double amplitude) {
		Random rnd = new Random();
		double[] slice = null;
		amplitude *= 2.0;
		for(int z=0; z<nz; z++) {
			slice = (double[])data[z];
			for(int k=0; k<nxy; k++) {
				slice[k] += (double)((rnd.nextDouble()-0.5)*amplitude);
			}
		}
	}

	/**
	* Add a salt and pepper noise.
	*
	* @param	amplitudeSalt		amplitude of the salt noise
	* @param	amplitudePepper		amplitude of the pepper noise
	* @param	percentageSalt		percentage of the salt noise
	* @param	percentagePepper	percentage of the pepper noise
	*/
	public void addSaltPepper(double amplitudeSalt, double amplitudePepper, double percentageSalt, double percentagePepper) {
		Random rnd = new Random();
		int index, z;
		if (percentageSalt > 0) {
			double nbSalt = nxy*nz/percentageSalt;
			for( int k=0; k < nbSalt; k++) {
				index = (int)(rnd.nextDouble() * nxy);
				z = (int)(rnd.nextDouble() * nz);
				((double[])data[z])[index] += (double)(rnd.nextDouble()*amplitudeSalt);
			}
		}
		if (percentagePepper > 0) {
			double nbPepper = nxy*nz/percentagePepper;
			for( int k=0; k < nbPepper; k++) {
				index = (int)(rnd.nextDouble() * nxy);
				z = (int)(rnd.nextDouble() * nz);
				((double[])data[z])[index] -= (double)(rnd.nextDouble()*amplitudeSalt);
			}
		}
	}
	
}	// end of class
