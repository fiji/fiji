package mpicbg.imglib.algorithm.laplace;

import mpicbg.imglib.Factory;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.real.FloatType;


public class LoGKernelFactory {
	
	/*
	 * STATIC METHODS
	 */
	
	
	public static <R extends RealType<R>> Image<R> createLoGKernel(R type, double sigma, int nDims) {
		int size = MathLib.getSuggestedKernelDiameter(sigma);
		switch (nDims) {
		case 1:
			return create1DLoGKernel(type, sigma, size);
		case 2:
			return create2DLoGKernel(type, sigma, size, size);
		case 3:
			return create3DLoGKernel(type, sigma, size, size, size);
		default:
			throw new IllegalArgumentException("Kernel in dimension "+nDims+" are not implemented.");
		}
	}
	
	public static <R extends RealType<R>> Image<R> create1DLoGKernel(R type, double sigma, int size) {
		if (size % 2 == 0)
			size++; // make it odd
		final ArrayContainerFactory containerFactory = new ArrayContainerFactory();
		final ImageFactory<R> factory = new ImageFactory<R>(type, containerFactory);
		final Image<R> img = factory.createImage(new int[] {size}, "LoG 1D kernel");
		final int center = (size-1)/2;
		final LocalizableCursor<R> cursor = img.createLocalizableCursor();
		int x;
		final double A = -1/(sigma*sigma*sigma*sigma);
		while (cursor.hasNext()) {
			cursor.fwd();
			x = cursor.getPosition(0) - center;
			cursor.getType().setReal( A * (x*x - sigma*sigma) * Math.exp(-x*x/2/sigma/sigma) );
		}
		return img;
	}
	
	public static <R extends RealType<R>> Image<R> create2DLoGKernel(R type, double sigma, int xSize, int ySize) {
		int[] dims = new int[] {xSize, ySize};
		for (int i = 0; i < dims.length; i++) {
			if (dims[i] % 2 == 0)
				dims[i]++; // make it odd
		}
		final ArrayContainerFactory containerFactory = new ArrayContainerFactory();
		final ImageFactory<FloatType> floatFactory = new ImageFactory<FloatType>(new FloatType(), containerFactory);
		final Image<FloatType> img = floatFactory.createImage(dims, "Intermediate float image");
		final LocalizableByDimCursor<FloatType> cursor = img.createLocalizableByDimCursor();
		final int xCenter = (dims[0]-1)/2;
		final int yCenter = (dims[1]-1)/2;
		int x, y;
		// Calculate values
		while (cursor.hasNext()) {
			cursor.fwd();
			x = cursor.getPosition(0) - xCenter;
			y = cursor.getPosition(1) - yCenter;
//			cursor.getType().setReal((x*x+y*y-2*sigma*sigma) / (sigma*sigma*sigma*sigma) * Math.exp(-(x*x+y*y)/(2*sigma*sigma)));
			cursor.getType().setReal((x*x+y*y-2*sigma*sigma) / (sigma*sigma) * Math.exp(-(x*x+y*y)/(2*sigma*sigma)));
		}
		// Scale values so that border center is equal to 1
		FloatType centerValue = img.createType();
		int[] centerPosition = new int[2];
		if (xSize > ySize) {
			centerPosition[0] = xCenter;
			centerPosition[0] = 0;			
		} else {
			centerPosition[0] = 0;
			centerPosition[0] = yCenter;
		}
		cursor.setPosition(centerPosition);
		centerValue.set(cursor.getType());
		cursor.reset();
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().div(centerValue);
		}
		// Copy to kernel
		final ImageFactory<R> factory = new ImageFactory<R>(type, containerFactory);
		final Image<R> kernel = factory.createImage(dims, "LoG 2D kernel");
		cursor.reset();
		LocalizableByDimCursor<R> kernelCursor = kernel.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			kernelCursor.setPosition(cursor);
			kernelCursor.getType().setReal(cursor.getType().getRealDouble());
		}
		// Make the laplacian sum to 0
//		R mean = kernel.createType();
//		R area = kernel.createType();
//		mean.setZero();
//		area.setReal(dims[0]*dims[1]);
//		kernelCursor.reset();
//		while (kernelCursor.hasNext()) {
//			kernelCursor.fwd();
//			mean.add(kernelCursor.getType());
//		}
//		mean.div(area);
//		kernelCursor.reset();
//		while (kernelCursor.hasNext()) {
//			kernelCursor.fwd();
//			kernelCursor.getType().sub(mean);
//		}
		// Return kernel
		return kernel;
	}
	
	public static <R extends RealType<R>> Image<R> create3DLoGKernel(R type, double sigma, int xSize, int ySize, int zSize) {
		int[] dims = new int[] {xSize, ySize, zSize};
		for (int i = 0; i < dims.length; i++) {
			if (dims[i] % 2 == 0)
				dims[i]++; // make it odd
		}
		final ArrayContainerFactory containerFactory = new ArrayContainerFactory();
		final ImageFactory<R> factory = new ImageFactory<R>(type, containerFactory);
		final Image<R> img = factory.createImage(dims, "LoG 3D kernel");
		final LocalizableCursor<R> cursor = img.createLocalizableCursor();
		final int xCenter = (dims[0]-1)/2;
		final int yCenter = (dims[1]-1)/2;
		final int zCenter = (dims[2]-1)/2;
		int x, y, z;
		final double A = 1/(sigma*sigma*sigma*sigma);
		while (cursor.hasNext()) {
			cursor.fwd();
			x = cursor.getPosition(0) - xCenter;
			y = cursor.getPosition(1) - yCenter;
			z = cursor.getPosition(2) - zCenter;
			cursor.getType().setReal( A * (x*x + y*y + z*z -3*sigma*sigma) * Math.exp(-(x*x + y*y + z*z)/2/sigma/sigma));
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
		double sigma = 1.0;
		int size = 5;
		System.out.println(String.format("1D LoG kernel with sigma = %.1f and of size %d", sigma, size) );
		Image<FloatType> kernel1D = LoGKernelFactory.create1DLoGKernel(new FloatType(), sigma, size);
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
		Image<FloatType> kernel2D = LoGKernelFactory.create2DLoGKernel(new FloatType(), sigma, size, size);
		LocalizableByDimCursor<FloatType> c2 = kernel2D.createLocalizableByDimCursor();
//		Image<LongType> kernel2D = LoGKernelFactory.create2DLoGKernel(new LongType(), sigma, size, size);
//		LocalizableByDimCursor<LongType> c2 = kernel2D.createLocalizableByDimCursor();
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
