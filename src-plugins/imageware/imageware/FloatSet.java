package imageware;

import ij.ImagePlus;
import ij.ImageStack;

import java.awt.Image;

/**
 * Class FloatSet.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class FloatSet extends FloatProcess implements ImageWare {

	//------------------------------------------------------------------
	//
	//	Constructors section
	//
	//------------------------------------------------------------------
	protected FloatSet(int nx, int ny, int nz) 		{ super(nx, ny, nz);}
	protected FloatSet(Image image, int mode) 		{ super(image, mode); }

	protected FloatSet(ImageStack stack, int mode) 	{ super(stack, mode); }
	protected FloatSet(ImageStack stack, byte chan) { super(stack, chan); }

	protected FloatSet(byte[] array, int mode) 		{ super(array, mode); }
	protected FloatSet(byte[][] array, int mode) 	{ super(array, mode); }
	protected FloatSet(byte[][][] array, int mode) 	{ super(array, mode); }
	protected FloatSet(short[] array, int mode) 	{ super(array, mode); }
	protected FloatSet(short[][] array, int mode) 	{ super(array, mode); }
	protected FloatSet(short[][][] array, int mode) { super(array, mode); }
	protected FloatSet(float[] array, int mode) 	{ super(array, mode); }
	protected FloatSet(float[][] array, int mode) 	{ super(array, mode); }
	protected FloatSet(float[][][] array, int mode) { super(array, mode); }
	protected FloatSet(double[] array, int mode) 	{ super(array, mode); }
	protected FloatSet(double[][] array, int mode) 	{ super(array, mode); }
	protected FloatSet(double[][][] array, int mode){ super(array, mode); }
	
	/**
	* Duplicate the imageware.
	*
	* Create a new imageware with the same size, same type and same data
	* than the calling one.
	*
	* @return a duplicated version of this imageware 
	*/
	public ImageWare duplicate() {
		ImageWare out = new FloatSet(nx, ny, nz);
		float[] outdata;
		for (int z=0; z<nz; z++) {
			outdata = (float[])(((FloatSet)out).data[z]);
			System.arraycopy(data[z], 0, outdata, 0, nxy);
		}
		return out;
	}

	/**
	* Replicate the imageware.
	*
	* Create a new imageware with the same size, same type
	* than the calling one. The data are not copied.
	*
	* @return a replicated version of this imageware 
	*/
	public ImageWare replicate() {
		return new FloatSet(nx, ny, nz);
	}

	/**
	* Replicate the imageware.
	*
	* Create a new imageware with the same size and a specified type
	* than the calling one. The data are not copied.
	*
	* @param type	requested type
	* @return a replicated version of this imageware 
	*/
	public ImageWare replicate(int type) {
		switch(type) {
		case ImageWare.BYTE:
			return new ByteSet(nx, ny, nz);
		case ImageWare.SHORT:
			return new ShortSet(nx, ny, nz);
		case ImageWare.FLOAT:
			return new FloatSet(nx, ny, nz);
		case ImageWare.DOUBLE:
			return new DoubleSet(nx, ny, nz);
		default :
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unknown type " + type + "].\n" +
				"-------------------------------------------------------\n"
			);
		}
	}

	/**
	* Copy all the data of source in the current imageware.
	* The source should have the same size and same type than the 
	* calling one.
	*
	* @param source	a source imageware
	*/
	public void copy(ImageWare source) {
		if (nx != source.getSizeX())
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to copy because it is not the same size (" + 
				nx + " != " + source.getSizeX() + ").\n" +
				"-------------------------------------------------------\n"
			);
		if (ny != source.getSizeY())
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to copy because it is not the same size (" + 
				ny + " != " + source.getSizeY() + ").\n" +
				"-------------------------------------------------------\n"
			);
		if (nz != source.getSizeZ())
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to copy because it is not the same size (" + 
				nz + " != " + source.getSizeZ() + ").\n" +
				"-------------------------------------------------------\n"
			);
		if (getType() != source.getType())
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to copy because it is not the same type (" + 
				getType() + " != " + source.getType() + ").\n" +
				"-------------------------------------------------------\n"
			);
		float[] src;
		for (int z=0; z<nz; z++) {
			src = (float[])(((FloatSet)source).data[z]);
			System.arraycopy(src, 0, data[z], 0, nxy); 
		}
	}

	/**
	* convert the imageware in a specified type.
	*
	* Create a new imageware with the same size and converted data
	* than the calling one.
	*
	* @param type	indicates the type of the output
	* @return a converted version of this imageware 
	*/
	public ImageWare convert(int type) {
		if (type == ImageWare.FLOAT)
			return duplicate();
		ImageWare out = null;
		switch(type) {
		case ImageWare.BYTE: 
			{
				float[] slice;
				out = new ByteSet(nx, ny, nz);
				byte[] outslice;
				for(int z=0; z<nz; z++) {
					slice = ((float[])data[z]);
					outslice = ((byte[])((ByteSet)out).data[z]);
					for(int k=0; k<nxy; k++) {
						outslice[k] = (byte)(slice[k]);
					}
				}
			}
			break;
		case ImageWare.SHORT:  
			{
				float[] slice;
				out = new ShortSet(nx, ny, nz);
				short[] outslice;
				for(int z=0; z<nz; z++) {
					slice = ((float[])data[z]);
					outslice = ((short[])((ShortSet)out).data[z]);
					for(int k=0; k<nxy; k++) {
						outslice[k] = (short)(slice[k]);
					}
				}
			}
			break;
		case ImageWare.FLOAT: 
			{
				float[] slice;
				out = new FloatSet(nx, ny, nz);
				float[] outslice;
				for(int z=0; z<nz; z++) {
					slice = ((float[])data[z]);
					outslice = ((float[])((FloatSet)out).data[z]);
					for(int k=0; k<nxy; k++) {
						outslice[k] = (float)(slice[k]);
					}
				}
			}
			break;
		case ImageWare.DOUBLE: 
			{
				float[] slice;
				out = new DoubleSet(nx, ny, nz);
				double[] outslice;
				for(int z=0; z<nz; z++) {
					slice = ((float[])data[z]);
					outslice = ((double[])((DoubleSet)out).data[z]);
					for(int k=0; k<nxy; k++) {
						outslice[k] = (double)(slice[k]);
					}
				}
			}
			break;
		default :
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unknown type " + type + "].\n" +
				"-------------------------------------------------------\n"
			);
		}
		return out;
	}
	
	/**
	* Print information of this ImageWare object.
	*/
	public void printInfo() {
		System.out.println("ImageWare object information");
		System.out.println("Dimension: " + getDimension() );
		System.out.println("Size: [" + nx + ", " + ny + ", " + nz + "]");
		System.out.println("TotalSize: " + getTotalSize());
		System.out.println("Type: " + getTypeToString() );
		System.out.println("Maximun: " + getMaximum());
		System.out.println("Minimun: " + getMinimum());
		System.out.println("Mean: " + getMean());
		System.out.println("Norm1: " + getNorm1());
		System.out.println("Norm2: " + getNorm2());
		System.out.println("Total: " + getTotal());
		System.out.println("");
	}
	

	/**
	* Show this ImageWare object.
	*/
	public void show() {
		String title = getTypeToString();
		switch(getDimension()) {
		case 1:
			title += " line";
			break;
		case 2:
			title += " image";
			break;
		case 3:
			title += " volume";
			break;
		}	
		ImagePlus imp = new ImagePlus(title, buildImageStack());
		imp.show();
	}


	/**
	* Show the data in ImagePlus object with a specify title.
	*
	* @param	title	a string given the title of the window
	*/
	public void show(String title) {
		ImagePlus imp = new ImagePlus(title, buildImageStack());
		imp.show();
	}


	/**
	* Return the minimum value of this imageware.
	*
	* @return	the min value of this imageware
	*/
	public double getMinimum() {
		double min = Double.MAX_VALUE;
		float[] slice;
		for(int z=0; z<nz; z++) {
			slice = ((float[])data[z]);
			for(int k=0; k<nxy; k++)
				if ((slice[k]) < min) 
					min = slice[k];
		}
		return min;
	}

	/**
	* Return the maximum value of this imageware.
	*
	* @return	the max value of this imageware
	*/
	public double getMaximum() {
		double max = -Double.MAX_VALUE;
		float[] slice;
		for(int z=0; z<nz; z++) {
			slice = ((float[])data[z]);
			for(int k=0; k<nxy; k++)
				if ((slice[k]) > max) 
					max = slice[k];
		}
		return max;
	}

	/**
	* Return the mean value of this imageware.
	*
	* @return	the mean value of this imageware
	*/
	public double getMean() {
		return getTotal() / (nz*nxy);
	}

	/**
	* Return the norm value of order 1.
	*
	* @return	the norm value of this imageware in L1 sense
	*/
	public double getNorm1() {
		double norm = 0.0;
		double value = 0;
		float[] slice;
		for(int z=0; z<nz; z++) {
			slice = ((float[])data[z]);
			for(int k=0; k<nxy; k++) {
				value = (double)(slice[k]);
				norm += (value > 0.0 ? value : -value);
			}
		}
		return norm;
	}

	/**
	* Return the norm value of order 2.
	*
	* @return	the norm value of this imageware in L2 sense
	*/
	public double getNorm2() {
		double norm = 0.0;
		float[] slice;
		for(int z=0; z<nz; z++) {
			slice = ((float[])data[z]);
			for(int k=0; k<nxy; k++)
				norm += (slice[k])*(slice[k]);
		}
		return norm;
	}

	/**
	* Return the sum of all pixel in this imageware.
	*
	* @return	the total sum of all pixel in this imageware
	*/
	public double getTotal() {
		double total = 0.0;
		float[] slice;
		for(int z=0; z<nz; z++) {
			slice = ((float[])data[z]);
			for(int k=0; k<nxy; k++)
				total += slice[k];
		}
		return total;
	}

	/**
	* Return the the minumum [0] and the maximum [1] value of this imageware.
	* Faster routine than call one getMinimum() and then one getMaximum().
	*
	* @return	an array of two values, the min and the max values of the images
	*/
	public double[] getMinMax() {
		double max = -Double.MAX_VALUE;
		double min =  Double.MAX_VALUE;
		float[] slice;
		for(int z=0; z<nz; z++) {
			slice = ((float[])data[z]);
			for(int k=0; k<nxy; k++) {
				if ((slice[k]) > max) 
					max = slice[k];
				if ((slice[k]) < min) 
					min = slice[k];
			}
		}
		double minmax[] = {min, max};
		return minmax;
	}

}	// end of class