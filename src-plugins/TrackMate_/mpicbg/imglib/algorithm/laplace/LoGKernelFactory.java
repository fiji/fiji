package mpicbg.imglib.algorithm.laplace;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

/**
 * This utility class is dedicated to creating Laplacian of Gaussian (LoG) kernels. 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - July 2010
 *
 */
public class LoGKernelFactory {
	
	
	/**
	 * Return a LoG kernel, of dimension nDims, and of size automatically determined from
	 * the value of sigma. Only 1D, 2D or 3D are supported. 
	 * For convenient use in blob segmentation, a flag allow to scale the kernel by σ².
	 * @param sigma  the sigma value of the gaussian 
	 * @param nDims  the dimension array 
	 * @param sigmaScaled  if true, the kernel will be multiplied by σ² 
	 * @return  the LoG kernel, as a {@link FloatType} {@link Image}
	 */
	public static Image<FloatType> createLoGKernel(double sigma, int nDims, boolean sigmaScaled, boolean invertSign) {
		int size = Util.getSuggestedKernelDiameter(sigma);
		switch (nDims) {
		case 1:
			return create1DLoGKernel(sigma, size, sigmaScaled, invertSign);
		case 2:
			return create2DLoGKernel(sigma, size, size, sigmaScaled, invertSign);
		case 3:
			return create3DLoGKernel(sigma, size, size, size, sigmaScaled, invertSign);
		default:
			throw new IllegalArgumentException("Kernel in dimension "+nDims+" are not implemented.");
		}
	}
	
	/**
	 * Return a 1D LoG kernel, of dimension matching the nDims parameter. For convenient use 
	 * in blob segmentation, a flag allow to scale the kernel by σ².
	 * @param sigma  the sigma value of the gaussian 
	 * @param size   the size of the kernel
	 * @param sigmaScaled  if true, the kernel will be multiplied by σ² 
	 * @return  the LoG kernel, as a {@link FloatType} {@link Image}
	 */
	public static Image<FloatType> create1DLoGKernel(double sigma, int size, boolean sigmaScaled, boolean invertSign) {
		if (size % 2 == 0)
			size++; // make it odd
		final int sign = invertSign ? -1 : 1;
		final ArrayContainerFactory containerFactory = new ArrayContainerFactory();
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), containerFactory);
		final Image<FloatType> img = factory.createImage(new int[] {size}, "LoG 1D kernel");
		final int center = (size-1)/2;
		final LocalizableCursor<FloatType> cursor = img.createLocalizableCursor();
		int x;
		final double A;
		if (sigmaScaled) {
			A = 1/(Math.sqrt(2*Math.PI)*Math.pow(sigma, 3));
		} else {
			A = 1/(Math.sqrt(2*Math.PI)*Math.pow(sigma, 5));
		}
		while (cursor.hasNext()) {
			cursor.fwd();
			x = cursor.getPosition(0) - center;
			cursor.getType().setReal( sign * A * (x*x - sigma*sigma) * Math.exp(-x*x/(2*sigma*sigma) ));
		}
		return img;
	}
	
	/**
	 * Return a 2D LoG kernel, of dimension matching the nDims parameter. For convenient use 
	 * in blob segmentation, a flag allow to scale the kernel by σ².
	 * @param sigma  the sigma value of the gaussian 
	 * @param xSize  the size in X of the kernel
	 * @param ySize  the size in Y of the kernel
	 * @param sigmaScaled  if true, the kernel will be multiplied by σ² 
	 * @return  the LoG kernel, as a {@link FloatType} {@link Image}
	 */
	public static Image<FloatType> create2DLoGKernel(double sigma, int xSize, int ySize, boolean sigmaScaled, boolean invertSign) {
		int[] dims = new int[] {xSize, ySize};
		for (int i = 0; i < dims.length; i++) {
			if (dims[i] % 2 == 0)
				dims[i]++; // make it odd
		}
		final int sign = invertSign ? -1 : 1;
		final ArrayContainerFactory containerFactory = new ArrayContainerFactory();
		final ImageFactory<FloatType> floatFactory = new ImageFactory<FloatType>(new FloatType(), containerFactory);
		final Image<FloatType> img = floatFactory.createImage(dims,  "LoG 2D kernel");
		final LocalizableByDimCursor<FloatType> cursor = img.createLocalizableByDimCursor();
		final int xCenter = (dims[0]-1)/2;
		final int yCenter = (dims[1]-1)/2;
		int x, y;
		final double A;
		if (sigmaScaled) {
			A = 1/(Math.sqrt(2*Math.PI)*Math.pow(sigma, 4));
		} else {
			A = 1/(Math.sqrt(2*Math.PI)*Math.pow(sigma, 6));
		}
		while (cursor.hasNext()) {
			cursor.fwd();
			x = cursor.getPosition(0) - xCenter;
			y = cursor.getPosition(1) - yCenter;
			cursor.getType().setReal( sign * A * (x*x+y*y-2*sigma*sigma) * Math.exp(- (x*x+y*y) / (2*sigma*sigma) ) );
		}
		return img;
	}
	
	/**
	 * Return a 3D LoG kernel, of dimension matching the nDims parameter. For convenient use 
	 * in blob segmentation, a flag allow to scale the kernel by σ².
	 * @param sigma  the sigma value of the gaussian 
	 * @param xSize  the size in X of the kernel
	 * @param ySize  the size in Y of the kernel
	 * @param zSize  the size in Y of the kernel
	 * @param sigmaScaled  if true, the kernel will be multiplied by σ² 
	 * @return  the LoG kernel, as a {@link FloatType} {@link Image}
	 */
	public static Image<FloatType> create3DLoGKernel(double sigma, int xSize, int ySize, int zSize, boolean sigmaScaled, boolean invertSign) {
		int[] dims = new int[] {xSize, ySize, zSize};
		for (int i = 0; i < dims.length; i++) {
			if (dims[i] % 2 == 0)
				dims[i]++; // make it odd
		}
		final int sign = invertSign ? -1 : 1;
		final ArrayContainerFactory containerFactory = new ArrayContainerFactory();
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), containerFactory);
		final Image<FloatType> img = factory.createImage(dims, "LoG 3D kernel");
		final LocalizableCursor<FloatType> cursor = img.createLocalizableCursor();
		final int xCenter = (dims[0]-1)/2;
		final int yCenter = (dims[1]-1)/2;
		final int zCenter = (dims[2]-1)/2;
		int x, y, z;
		final double A;
		if (sigmaScaled) {
			A = 1/(Math.pow(2*Math.PI, 1.5)*Math.pow(sigma, 5));
		} else {
			A = 1/(Math.pow(2*Math.PI, 1.5)*Math.pow(sigma, 7));
		}
		while (cursor.hasNext()) {
			cursor.fwd();
			x = cursor.getPosition(0) - xCenter;
			y = cursor.getPosition(1) - yCenter;
			z = cursor.getPosition(2) - zCenter;
			cursor.getType().setReal( sign * A * (x*x + y*y + z*z -3*sigma*sigma) * Math.exp(-(x*x + y*y + z*z) / (2*sigma*sigma) ) );
		}
		return img;
	}

	/*
	 * 	MAIN METHOD
	 */
	
	/**
	 * For testing
	 */
	public static void main(String[] args) {
		double sigma = 0.5;
		int size = 5;
		System.out.println(String.format("1D LoG kernel with sigma = %.1f and of size %d", sigma, size) );
		Image<FloatType> kernel1D = LoGKernelFactory.create1DLoGKernel(sigma, size, false, false);
		LocalizableByDimCursor<FloatType> c1 = kernel1D.createLocalizableByDimCursor();
		for (int i = 0; i < c1.getDimensions()[0]; i++) {
			c1.setPosition(i, 0);
			System.out.print(String.format("%.1f\t ", c1.getType().get()));
		}
		System.out.println(" ");
		
		sigma = 1;
		size = 9;
		System.out.println(" ");
		System.out.println(String.format("2D LoG kernel with sigma = %.1f and of size %d", sigma, size) );
		Image<FloatType> kernel2D = LoGKernelFactory.create2DLoGKernel(sigma, size, size, false, false);
		LocalizableByDimCursor<FloatType> c2 = kernel2D.createLocalizableByDimCursor();
		for (int i = 0; i < c2.getDimensions()[0]; i++) {
			c2.setPosition(i, 0);
			for (int j = 0; j < c2.getDimensions()[1]; j++) {
				c2.setPosition(j, 1);
				System.out.print(String.format("%.1f\t ", c2.getType().getRealDouble()));
			}
			System.out.println(" ");
		}
		
	}
}
