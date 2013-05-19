package imageware;

import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Image;

/**
 * Class Builder.
 * 
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class Builder {

	/**
	* Wrap a imageware of from the focused image of ImageJ.
	*/
	public static ImageWare wrapOnFocus() {
		ImagePlus imp = WindowManager.getCurrentImage();
		return wrap(imp.getStack());
	}

	
	/**
	* Wrap a imageware around a ImagePlus object.
	*
	* @param imp		an ImagePlus object to wrap
	*/
	public static ImageWare wrap(ImagePlus imp) {
		return wrap(imp.getStack());
	}

	
	/**
	* Wrap a imageware around a ImageStack object.
	*
	* @param stack		an ImageStack object to wrap
	*/
	public static ImageWare wrap(ImageStack stack) {
		if (stack == null)
			throw_null();
		ImageProcessor ip = stack.getProcessor(1);
		if (ip instanceof ByteProcessor) {
			return new ByteSet(stack, ImageWare.WRAP);
		}
		else if (ip instanceof ShortProcessor) {
			return new ShortSet(stack, ImageWare.WRAP);
		}
		else if (ip instanceof FloatProcessor) {
			return new FloatSet(stack, ImageWare.WRAP);
		}
		else {
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to wrap this ImageStack object.\n" + 
				"Support only the 8-bit, 16-bit and 32-bits type.\n" +
				"-------------------------------------------------------\n"
			);
		}	
	}
	
	/**
	* Create an empty imageware of a specified type.
	* @param nx		size in X axis
	* @param ny		size in Y axis
	* @param nz		size in Z axis
	* @param type	type of the imageware
	*/
	public static ImageWare create(int nx, int ny, int nz, int type) {
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
				"Unknown type " + type + ".\n" +
				"-------------------------------------------------------\n"
			);
		}
	}
	
	/**
	* Create a imageware of from an Java AWT Image.
	*
	* @param image		an Java AWT Image object
	*/
	public static ImageWare create(Image image) {
		if (image == null)
			throw_null();
		return new ByteSet(image, ImageWare.CREATE);
	}

	/**
	* Create a imageware of from the focussed image of ImageJ.
	*/
	public static ImageWare createOnFocus() {
		return create(WindowManager.getCurrentImage());
	}
	
	/**
	* Create a imageware of from an ImageStack.
	*
	* @param stack		an ImageStack object
	*/
	public static ImageWare create(ImageStack stack) {
		if (stack == null)
			throw_null();
		ImageWare wrapped = wrap(stack);
		return wrapped.duplicate();
	}
		
	/**
	* Create a imageware of from an ImagePlus.
	*
	* @param imp		an ImagePlus object
	*/
	public static ImageWare create(ImagePlus imp) {
		if (imp == null)
			throw_null();
		ImageWare wrapped = wrap(imp);
		return wrapped.duplicate();
	}
		
	/**
	* Create an array of 3 datasets from an ImagePlus.
	*
	* @param imp		an ImagePlus object
	*/
	public static ImageWare[] createColors(ImagePlus imp) {
		if (imp == null)
			throw_null();
		return createColors(imp.getStack());
	}
	
	/**
	* Create an array of 3 imageware from an ImageStack.
	*
	* @param stack		an ImageStack object
	*/
	public static ImageWare[] createColors(ImageStack stack) {
		if (stack == null)
			throw_null();
		ImageWare color[] = new ImageWare[3];
		color[0] = new ByteSet(stack, ImageWare.RED);
		color[1] = new ByteSet(stack, ImageWare.GREEN);
		color[2] = new ByteSet(stack, ImageWare.BLUE);
		return color;
	}

	/**
	* Create a imageware of a specific channel if the ImagePlus is
	* a color image.
	*
	* @param imp		an ImagePlus object
	*/
	public static ImageWare createColorChannel(ImagePlus imp, byte channel) {
		if (imp == null)
			throw_null();
		return createColorChannel(imp.getStack(), channel);
	}
	
	/**
	* Create a imageware of a specific channel if the ImageStack is
	* a color image.
	* @param stack		an ImageStack object
	*/
	public static ImageWare createColorChannel(ImageStack stack, byte channel) {
		if (stack == null)
			throw_null();
		return new ByteSet(stack, channel);
	}
	

	/**
	* Create a imageware of from an array.
	*
	* @param object		byte 1D array
	*/
	public static ImageWare create(byte[] object) {
		if (object == null)
			throw_null();
		return new ByteSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		short 1D array
	*/
	public static ImageWare create(short[] object) {
		if (object == null)
			throw_null();
		return new ShortSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		float 1D array
	*/
	public static ImageWare create(float[] object) {
		if (object == null)
			throw_null();
		return new FloatSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		double 1D array
	*/
	public static ImageWare create(double[] object) {
		if (object == null)
			throw_null();
		return new DoubleSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		byte 2D array
	*/
	public static ImageWare create(byte[][] object) {
		if (object == null)
			throw_null();
		return new ByteSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		short 2D array
	*/
	public static ImageWare create(short[][] object) {
		if (object == null)
			throw_null();
		return new ByteSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		float 2D array
	*/
	public static ImageWare create(float[][] object) {
		if (object == null)
			throw_null();
		return new FloatSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		double 2D array
	*/
	public static ImageWare create(double[][] object) {
		if (object == null)
			throw_null();
		return new DoubleSet(object, ImageWare.CREATE);
	}
	/**
	* Create a imageware of from an array.
	*
	* @param object		byte 3D array
	*/
	public static ImageWare create(byte[][][] object) {
		if (object == null)
			throw_null();
		return new ByteSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		short 3D array
	*/
	public static ImageWare create(short[][][] object) {
		if (object == null)
			throw_null();
		return new ShortSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		float 3D array
	*/
	public static ImageWare create(float[][][] object) {
		if (object == null)
			throw_null();
		return new FloatSet(object, ImageWare.CREATE);
	}
	
	/**
	* Create a imageware of from an array.
	*
	* @param object		double 3D array
	*/
	public static ImageWare create(double[][][] object) {
		if (object == null)
			throw_null();
		return new DoubleSet(object, ImageWare.CREATE);
	}

	/**
	* Create a imageware of a specified type from an Java AWT Image object.
	*
	* @param image	an Java AWT Image object
	* @param type	type of the imageware
	*/
	public static ImageWare create(Image image, int type) {
		if (image == null)
			throw_null();
		switch(type) {
		case ImageWare.BYTE:
			return new ByteSet(image, ImageWare.CREATE);
		case ImageWare.SHORT:
			return new ShortSet(image, ImageWare.CREATE);
		case ImageWare.FLOAT:
			return new FloatSet(image, ImageWare.CREATE);
		case ImageWare.DOUBLE:
			return new DoubleSet(image, ImageWare.CREATE);
		default :
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unknown type " + type + ".\n" +
				"-------------------------------------------------------\n"
			);
		}
	}

	/**
	* Create a imageware of from the focused image of ImageJ.
	*
	* @param type	type of the imageware
	*/
	public static ImageWare createOnFocus(int type) {
		return create(WindowManager.getCurrentImage(), type);
	}

	/**
	* Create a imageware of a specified type from an ImagePlus object.
	*
	* @param imp	an ImagePlus object
	* @param type	type of the imageware
	*/
	public static ImageWare create(ImagePlus imp, int type) {
		if (imp == null)
			throw_null();
		switch(type) {
		case ImageWare.BYTE:
			return new ByteSet(imp.getStack(), ImageWare.CREATE);
		case ImageWare.SHORT:
			return new ShortSet(imp.getStack(), ImageWare.CREATE);
		case ImageWare.FLOAT:
			return new FloatSet(imp.getStack(), ImageWare.CREATE);
		case ImageWare.DOUBLE:
			return new DoubleSet(imp.getStack(), ImageWare.CREATE);
		default :
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unknown type " + type + ".\n" +
				"-------------------------------------------------------\n"
			);
		}
	}
	
	/**
	* Create a imageware of a specified type from an ImageStack object.
	*
	* @param object	an ImageStack object
	* @param type	type of the imageware
	*/
	public static ImageWare create(ImageStack object, int type) {
		if (object == null)
			throw_null();
		ImageWare wrapped = wrap(object);
		return wrapped.convert(type); 
	}
	
	/**
	* Create an array of 3 datasets from an ImagePlus.
	*
	* @param imp		an ImagePlus object
	* @param type	type of the imageware
	*/
	public static ImageWare[] createColor(ImagePlus imp, int type) {
		if (imp == null)
			throw_null();
		return createColor(imp.getStack(), type);
	}
	
	/**
	* Create an array of 3 imageware from an ColorProcessor.
	*
	* @param stack		an ColorProcessor object
	* @param type	type of the imageware
	*/
	public static ImageWare[] createColor(ImageStack stack, int type) {
		if (stack == null)
			throw_null();
		ImageWare color[] = new ImageWare[3];
		switch(type) {
		case ImageWare.BYTE:
			color[0] = new ByteSet(stack, ImageWare.RED);
			color[1] = new ByteSet(stack, ImageWare.GREEN);
			color[2] = new ByteSet(stack, ImageWare.BLUE);
			break;
		case ImageWare.SHORT:
			color[0] = new ShortSet(stack, ImageWare.RED);
			color[1] = new ShortSet(stack, ImageWare.GREEN);
			color[2] = new ShortSet(stack, ImageWare.BLUE);
			break;
		case ImageWare.FLOAT:
			color[0] = new FloatSet(stack, ImageWare.RED);
			color[1] = new FloatSet(stack, ImageWare.GREEN);
			color[2] = new FloatSet(stack, ImageWare.BLUE);
			break;
		case ImageWare.DOUBLE:
			color[0] = new DoubleSet(stack, ImageWare.RED);
			color[1] = new DoubleSet(stack, ImageWare.GREEN);
			color[2] = new DoubleSet(stack, ImageWare.BLUE);
			break;
		default:
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unknown type " + type + ".\n" +
				"-------------------------------------------------------\n"
			);
		}
		return color;
	}

	/**
	* Create a imageware of a specific channel if the ImagePlus is
	* a color image.
	*
	* @param imp		an ImagePlus object
	*/
	public static ImageWare createColorChannel(ImagePlus imp, byte channel, int type) {
		if (imp == null)
			throw_null();
		return createColorChannel(imp.getStack(), channel, type);
	}
	
	/**
	* Create a imageware of a specific channel if the ImageStack is
	* a color image.
	* @param stack		an ImageStack object
	*/
	public static ImageWare createColorChannel(ImageStack stack, byte channel, int type) {
		if (stack == null)
			throw_null();
		ImageWare out = null;
		switch(type) {
		case ImageWare.BYTE:
			out = new ByteSet(stack, channel);
			break;
		case ImageWare.SHORT:
			out = new ShortSet(stack, channel);
			break;
		case ImageWare.FLOAT:
			out = new FloatSet(stack, channel);
			break;
		case ImageWare.DOUBLE:
			out = new DoubleSet(stack, channel);
			break;
		default:
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unknown type " + type + ".\n" +
				"-------------------------------------------------------\n"
			);
		}
		return out;
	}
	
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		byte 1D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(byte[] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, 1, 1, type);
		out.putX(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		short 1D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(short[] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, 1, 1, type);
		out.putX(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		float 1D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(float[] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, 1, 1, type);
		out.putX(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		double 1D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(double[] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, 1, 1, type);
		out.putX(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		byte 2D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(byte[][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, 1, type);
		out.putXY(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		short 2D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(short[][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, 1, type);
		out.putXY(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		float 2D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(float[][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, 1, type);
		out.putXY(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		double 2D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(double[][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, 1, type);
		out.putXY(0, 0, 0, object);
		return out;	
	}
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		byte 3D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(byte[][][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, object[0][0].length, type);
		out.putXYZ(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		short 3D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(short[][][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, object[0][0].length, type);
		out.putXYZ(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		float 3D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(float[][][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, object[0][0].length, type);
		out.putXYZ(0, 0, 0, object);
		return out;	
	}
	
	/**
	* Create a imageware of a specified type from an array.
	*
	* @param object		double 3D array
	* @param type		type of the imageware
	*/
	public static ImageWare create(double[][][] object, int type) {
		if (object == null)
			throw_null();
		ImageWare out = createType(object.length, object[0].length, object[0][0].length, type);
		out.putXYZ(0, 0, 0, object);
		return out;	
	}
	
	/**
	*/
	private static ImageWare createType(int nx, int ny , int nz, int type) {
		ImageWare out = null;
		switch(type) {
			case ImageWare.BYTE:
				out = new ByteSet(nx, ny, nz);
				break;
			case ImageWare.SHORT:
				out = new ShortSet(nx, ny, nz);
				break;
			case ImageWare.FLOAT:
				out = new FloatSet(nx, ny, nz);
				break;
			case ImageWare.DOUBLE:
				out = new DoubleSet(nx, ny, nz);
				break;
			default:
				throw new ArrayStoreException(
					"\n-------------------------------------------------------\n" +
					"Error in imageware package\n" +
					"Unable to create this object.\n" + 
					"-------------------------------------------------------\n"
				);
		}
		return out;
	}
	
	private static void throw_null() {			
	 throw new ArrayStoreException(
			"\n-------------------------------------------------------\n" +
			"Error in imageware package\n" +
			"Unable to wrap the ImagePlus.\n" +
			"The object parameter is null.\n" +
			"-------------------------------------------------------\n"
		);
	}

}
