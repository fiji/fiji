/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.pde;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * <h1>Perona & Malik Anisotropic diffusion</h1>
 * 
 * <h2>Algorithm</h2>
 * 
 * This algorithm implements the so-called anisotropic diffusion scheme of Perona & Malik, 1990,
 * with imglib. For details on the anisotropic diffusion principles, see 
 * {@link http://en.wikipedia.org/wiki/Anisotropic_diffusion}, and the original paper:
 * <pre>
 * Perona and Malik. 
 * Scale-Space and Edge Detection Using Anisotropic Diffusion. 
 * IEEE Transactions on Pattern Analysis and Machine Intelligence (1990) vol. 12 pp. 629-639
 * </pre>
 * 
 * <h2>Implementation</h2>
 * 
 * This implementation uses Imglib for its core. Filtering is done in place, and a call
 * to the {@link #process()} method does only one iteration of the process on the given
 * image. This allow to change all parameters at each iteration if desired.
 * <p>
 * This implementation is dimension generic: the filtering is done considering a 3x3 neighborhood
 * for a 2D image, a 3x3x3 neighborhood for a 3D image, and so on.  
 * <p>
 * For every pixel of the image, the contribution
 * of all close neighbors in a cube (whatever is the dimensionality) around the central pixel 
 * is considered. Image gradient is evaluated by finite differences in direction of the neighbor
 * currently inspected. The value of this component of the gradient is used to compute the 
 * diffusion coefficient, through a function that must implements the {@link DiffusionFunction}
 * interface. Users can specify their own function. Two functions are offered, taken from 
 * Perona and Malik original paper: {@link StrongEdgeEnhancer} and {@link WideRegionEnhancer}.
 * <p>
 * On top of that, one can specify what dimensions to parse when calculating laplacian and gradient. 
 * This allow to do smoothin only in some directions. See {@link #setDimensions(int[])}.
 * <p>
 * This implementation is multithreaded; the number of used thread can
 * be specified with the {@link #setNumThreads(int)} or {@link #setNumThreads()} methods.
 * 
 * @param <T>  the type of the target image.
 * @author Jean-Yves Tinevez
 */
public class AnisotropicDiffusion <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm {
	
	/*
	 * FIELDS
	 */
	
	private Image<T> image;
	private double deltat;
	private DiffusionFunction fun;
	private int[] dimensions;
	
	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Instantiate the Perona & Malik anisotropic diffusion process, with a custom diffusion function.
	 *  
	 * @param image  the target image, will be modified in place
	 * @param deltat  the integration constant for the numerical integration scheme. Typically less that 1.
	 * @param function  the custom diffusion function.
	 * 
	 * @see DiffusionFunction
	 */
	public AnisotropicDiffusion(Image<T> image, double deltat, DiffusionFunction function) {
		this.image = image;
		this.deltat = deltat;
		this.fun = function;
		this.processingTime = 0;
		this.dimensions = new int[image.getNumDimensions()];
		// By default, we consider neighborhood in all dimensions
		for (int i = 0; i < dimensions.length; i++) {
			dimensions[i] = i;
		}
	}
	
	
	/**
	 * Instantiate the Perona & Malik anisotropic diffusion process, with the default strong-edge
	 * diffusion function.
	 *  
	 * @param image  the target image, will be modified in place
	 * @param deltat  the integration constant for the numerical integration scheme. Typically less that 1.
	 * @param kappa  the constant for the diffusion function that sets its gradient threshold 
	 * 
	 * @see StrongEdgeEnhancer
	 */
	public AnisotropicDiffusion(Image<T> image, double deltat, double kappa) {
		this(image, deltat, new StrongEdgeEnhancer(kappa));
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		if (deltat <= 0) {
			errorMessage = "Time interval must bu strictly positive, got "+deltat+".";
			return false;
		}
		return true;
	}

	/**
	 * Execute one step of the numerical integration scheme. To achieve several iterations of the scheme, 
	 * one has to call this methods several times.
	 */
	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		final int ndim = dimensions.length;
		
		final double nneighbors = Math.pow(3, ndim) - 1 ;

		final AtomicInteger ai = new AtomicInteger(0);			
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(image.getNumPixels(), numThreads);
		final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread(new Runnable() {

				public void run() {

					int[] centralPosition = new int[ndim];
					int[] position = new int[ndim];
					LocalizableCursor<T> mainCursor = image.createLocalizableCursor();
					LocalizableByDimCursor<T> cursor = image.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());

					final int threadNumber = ai.getAndIncrement();
					final Chunk chunk = chunks.get( threadNumber );
					T increment = mainCursor.getType().createVariable();

					mainCursor.fwd(chunk.getStartPosition());

					for ( long j = 0; j < chunk.getLoopSize(); ++j ) {

						mainCursor.fwd();
						cursor.setPosition(mainCursor);
						T centralValue = mainCursor.getType();

						// Init neighbor cursor position
						for (int dim = 0; dim < ndim; dim++) {
							centralPosition[dim] = mainCursor.getPosition( dimensions [ dim ] );
							position[dim] = -1;
						}
						position[0] = -2;

						// Loop over all neighbors
						double amount = 0;
						
						while (true) {

							// Move to next neighbor
							for (int dim = 0; dim < ndim; dim++) {
								if (position[dim] < 1) {
									position[dim]++;
									break;
								} else {
									position[dim] = -1;
								}
							}

							for (int dim = 0; dim < ndim; dim++) {
								cursor.setPosition(centralPosition[dim] + position[dim], dimensions [ dim ] );
							}

							// Lattice length
							double dx2 = 0;
							for (int dim = 0; dim < ndim; dim++) {
								dx2 += position[dim] * position[dim];
							}

							if (dx2 == 0) {
								continue; // Skip central point
							}

							// Finite differences
							double di = cursor.getType().getRealDouble() - centralValue.getRealDouble();

							// Diffusion function
							double g = fun.eval(di, position);

							// Amount
							amount += 1/dx2 * g * di;

							// Test if we are finished (all position indices to 1)
							boolean finished = true;
							for (int dim = 0; dim < ndim; dim++) {
								if (position[dim] != 1) {
									finished = false;
									break;
								}
							}
							if (finished) {
								break;
							}

						} // Finished looping over neighbors

						// Update current value
						increment.setReal(deltat * amount / nneighbors);
						mainCursor.getType().add(increment);

					}
				}
			});
		}

		SimpleMultiThreading.startAndJoin(threads);

		long end = System.currentTimeMillis();
		processingTime += (end - start);
		return true;
	}

	/**
	 * Set the integration constant value for the numerical integration scheme.
	 * @param deltat
	 */
	public void setDeltaT(float deltat) {
		this.deltat = deltat;
	}
	
	/**
	 * Set the diffusion function used to compute conduction coefficients.
	 * @param function
	 * @see DiffusionFunction
	 * @see StrongEdgeEnhancer
	 * @see WideRegionEnhancer
	 */
	public void setDiffusionFunction(DiffusionFunction function) {
		this.fun = function;
	}
	
	/**
	 * Set the dimensions to be considered in neighborhood.
	 * <p>
	 * By default, all dimensions are parsed when iterating around a pixel to compute gradient and laplacian.
	 * However, there might be situations where one wants to limit that. For instance, when given a 3D 
	 * volume that is a 2D+time movie, it is sometimes desirable to do the anisotropic diffusion in the
	 * 2D planes, and not to incorporate the derivatives in the time direction. It is possible to do
	 * that by specifying the desired dimensions with this method. In the aforementioned example,
	 * to limit the diffusion in the X & Y directions only, the array {0, 1} has to be given.
	 */
	public void setDimensions(int[] dimensions) {
		this.dimensions = dimensions;
	}

	
	/*
	 * PUBLIC CLASSES
	 */
	
	/**
	 * The interface that function suitable to be diffusion function must implement.
	 * It is very simple and has some limitation: in Perona & Malik scheme, the gradient 
	 * at each arc location is approximated by the absolute value of its projection along the 
	 * direction of the arc (see paper, p. 633). Functions implementing this interface are 
	 * therefore provided only with a single component of the gradient, and must return the
	 * diffusion contribution in that direction. 
	 */
	public static interface DiffusionFunction {
		/**
		 * Return the conduction coefficient in a given direction, from the value
		 * of the image gradient in that direction
		 * @param gradi  value of the image gradient in the given direction
		 * @param position  an int array that holds the relative gradient direction
		 * @return  the conduction coefficient
		 */
		public double eval(double gradi, final int[] position);
	}
	
	/**
	 * The first diffusion function proposed by Perona & Malik. This one 
	 * privileges strong edges over weak ones.
	 * <pre> g(∇I) = exp( - (||∇I/κ||²) )</pre>
	 */
	public static class StrongEdgeEnhancer implements DiffusionFunction {
		private double kappa;
		public StrongEdgeEnhancer(double kappa) { this.kappa = kappa; }
		
		@Override
		public double eval(double gradi, int[] position) {
			return Math.exp(- (gradi*gradi/kappa/kappa));
		}
		
	}
	
	/**
	 * The second diffusion function proposed by Perona & Malik. This one 
	 * privileges wide regions over smaller ones.
	 * <pre> g(∇I) = 1 / ( 1 + (||∇I/κ||²) )</pre>
	 */
	public static class WideRegionEnhancer implements DiffusionFunction {
		private double kappa;
		public WideRegionEnhancer(double kappa) { this.kappa = kappa; }
		
		@Override
		public double eval(double gradi, int[] position) {
			return 1 / ( 1 + (gradi*gradi/kappa/kappa));
		}
		
	}
}
