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

package script.imglib.math;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategy;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.DoubleType;
import mpicbg.imglib.type.numeric.real.FloatType;
import script.imglib.math.fn.IFunction;
import script.imglib.math.fn.ImageFunction;

/** Compute an {@link IFunction} into an {@link Image}. In essence, the {@link IFunction}
 * defines an operation with one or two pixels as arguments, such as {@link Multiply},
 * {@link Divide}, etc. The {@link Compute#inFloats(IFunction)} evaluates the function
 * and places the result in a new {@link Image}. To specify a different type, use the
 * {@link Compute#apply(IFunction, RealType, int)}, and for {@link RGBALegacyType}, use
 * {@link Compute#apply(IFunction, RGBALegacyType, int)} or {@link Compute#inRGBA(IFunction)}.
 * <p>
 * The underlying machinery first inspects the {@link IFunction} and all its nested
 * {@link IFunction} instances, collecting all visible {@link Cursor} instances,
 * using the method {@link Compute#findImages(IFunction)}.
 * <p>
 * All {@link Image} instances related to the found {@link Cursor} instances are
 * inspected to ensure that their {@link mpicbg.imglib.container.Container} are compatible.
 * If the {@link mpicbg.imglib.container.Container} are not compatible, the content
 * of the images would not be iterated in a way that would make sense. So an {@link Exception}
 * will be thrown.
 * <p>
 * Finally, the results of evaluating the {@link IFunction} are stored and returned in
 * an {@link Image}. The dimensions of the returned image are the same as those of the
 * first image found. If the dimensions do not match, an error will eventually pop up, or
 * the computation result in images that have unexpected data in chunks of them (for example,
 * when there is an {@link OutOfBoundsStrategy} that prevents an early error from occurring).
 * <p>
 * An example program: correct the background illumination of an image, given the associated
 * brighfield and a darkfield images, and the mean value of the image:
 * <p>
   <pre>
   public Image<FloatType> scriptCorrectIllumination(
            final Image<? extends RealType<?>> img,
            final Image<? extends RealType<?>> brightfield,
            final Image<? extends RealType<?>> darkfield,
            final double mean) throws Exception
   {
       Image<FloatType> corrected = Compute.inFloats(1,
           new Multiply(
               new Divide(
                   new Subtract(img, brightfield),
                   new Subtract(brightfield, darkfield)),
           mean));
       return corrected;
   }
   </pre>
 * 
 * @version 1.0 2010-11-30
 * @see IFunction
 * @see Image
 *
 * @author Albert Cardona
 */
public class Compute {

	/** Ensure that the {@link Container} of each {@link Image} of @param images is compatible
	 * with all the others. */
	static public final void checkContainers(final Collection<Image<?>> images) throws Exception {
		if (images.isEmpty())
			throw new Exception("There aren't any images!");

		final Image<?> first = images.iterator().next();

		for ( final Image<?> img : images ) 
		{
			if ( !img.getContainer().compareStorageContainerDimensions( first.getContainer() ) )
				throw new Exception("Images have different dimensions!");
			
			if ( !img.getContainer().compareStorageContainerCompatibility( first.getContainer() ) ) 
				throw new Exception("Images are of incompatible container types!");
		}
	}

	/** Find all images in @param op and nested {@link IFunction} instances. */ 
	static public final Set<Image<?>> findImages(final IFunction op) throws Exception {
		final HashSet<Cursor<?>> cs = new HashSet<Cursor<?>>();
		op.findCursors(cs);
		//
		final HashSet<Image<?>> images = new HashSet<Image<?>>();
		for (final Cursor<?> c : cs) {
			images.add(c.getImage());
		}
		return images;
	}

	/** Implements the core functionality of the {@code apply} method. */ 
	static private abstract class Loop<R extends NumericType<R>>
	{
		private final IFunction op;
		private final Collection<Image<?>> images;
		private final Collection<Cursor<?>> cursors;
		private final R output;
		private int numThreads;

		public Loop(final IFunction op, final R output, final int numThreads) throws Exception {
			this.op = op;
			this.output = output;
			this.numThreads = Math.max(1, numThreads);
			// Collect all cursors and their images involved in the operation
			this.cursors = new HashSet<Cursor<?>>();
			op.findCursors(this.cursors);
			//
			this.images = new HashSet<Image<?>>();
			for (final Cursor<?> c : cursors) {
				images.add(c.getImage());
			}
		}

		public abstract void loop(final Cursor<R> resultCursor, final long loopSize, final IFunction fn);

		protected void cleanupCursors() {
			for (Cursor<?> c : this.cursors) {
				c.close();
			}
		}

		/** Runs the operation on each voxel and ensures all cursors of {@code op}, and all
		 * interim cursors created for multithreading, are closed. */
		public Image<R> run() throws Exception {
			try {
				return innerRun();
			} finally {
				cleanupCursors();
			}
		}

		private final Image<R> innerRun() throws Exception {
			if (images.size() > 0) {
				// 2 - Check that they are all compatible: same dimensions, same container type
				checkContainers(images);

				final Image<?> first = images.iterator().next();

				// 3 - Operate on an empty result image
				final ImageFactory<R> factory = new ImageFactory<R>( output, first.getContainerFactory() );
				final Image<R> result = factory.createImage( first.getDimensions(), "result" );

				final AtomicInteger ai = new AtomicInteger(0);

				// Duplicate all: also sets a new cursor for each that has one, so it's unique and reset.
				final IFunction[] functions = new IFunction[ numThreads ];
				try
				{
					for ( int i = 0; i < numThreads; ++i )				
						functions[ i ] = op.duplicate();
				}
				catch ( Exception e ) 
				{
					System.out.println( "Running single threaded, operations cannot be duplicated:\n" + e);
					numThreads = 1;
				}

				final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
				final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( first.getNumPixels(), numThreads );

				for (int ithread = 0; ithread < threads.length; ++ithread)
					threads[ithread] = new Thread(new Runnable()
					{
						public void run()
						{
							// Thread ID
							final int myNumber = ai.getAndIncrement();

							// get chunk of pixels to process
							final Chunk myChunk = threadChunks.get( myNumber );

							final Cursor<R> resultCursor = result.createCursor();
							resultCursor.fwd( myChunk.getStartPosition() );

							final IFunction fn = functions[ myNumber ];

							Collection<Cursor<?>> cs = new HashSet<Cursor<?>>();
							fn.findCursors(cs);
							for (Cursor<?> c : cs) {
								c.fwd( myChunk.getStartPosition() );
							}

							// Store for cleanup later
							Loop.this.cursors.addAll(cs);
							Loop.this.cursors.add(resultCursor);

							loop(resultCursor, myChunk.getLoopSize(), fn);
						}
					});

				SimpleMultiThreading.startAndJoin( threads );

				return result;
			} else {
				// Operations that only involve numbers (for consistency)
				final ImageFactory<R> factory = new ImageFactory<R>(output, new ArrayContainerFactory());
				final Image<R> result = factory.createImage( new int[]{1}, "result" );

				final Cursor<R> c = result.createCursor();
				this.cursors.add(c); // store for cleanup later
				loop(c, result.size(), op);

				return result;
			}
		}
	}

	/** Execute the given {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} with the type defined by {@param output},
	 * which has to be a subclass of {@link RealType}.
	 * 
	 * @param op The {@link IFunction} to execute.
	 * @param output An instance of the type of the result image returned by this method.
	 * @param numThreads The number of threads for parallel execution. */
	static public final <R extends RealType<R>> Image<R> apply(final IFunction op, final R output, int numThreads) throws Exception
	{
		final Loop<R> loop = new Loop<R>(op, output, numThreads) {
			public final void loop(final Cursor<R> resultCursor, final long loopSize, final IFunction fn) {
				for ( long j = loopSize; j > 0 ; --j )
				{
					resultCursor.fwd();
					resultCursor.getType().setReal( fn.eval() );
				}
			}
		};
		return loop.run();
	}

	/** Execute the given {@param op} {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link RGBALegacyType}.
	 * 
	 * @param op The {@link IFunction} to execute.
	 * @param output An instance of the {@link RGBALegacyType} type of the result image returned by this method.
	 * @param numThreads The number of threads for parallel execution. */
	static public final Image<RGBALegacyType> apply(final IFunction op, final RGBALegacyType output, int numThreads ) throws Exception
	{
		final Loop<RGBALegacyType> loop = new Loop<RGBALegacyType>(op, output, numThreads) {
			public final void loop(final Cursor<RGBALegacyType> resultCursor, final long loopSize, final IFunction fn) {
				for ( long j = loopSize; j > 0 ; --j )
				{
					resultCursor.fwd();
					resultCursor.getType().set( (int) fn.eval() );
				}
			}
		};
		return loop.run();
	}

	/** Execute the given {@param} op {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link FloatType}.
	 * Uses as many concurrent threads as CPUs, defined by {@link Runtime#availableProcessors()}.
	 * 
	 * @param op The {@link IFunction} to execute. */
	static public final Image<FloatType> inFloats(final IFunction op) throws Exception
	{
		return inFloats( Runtime.getRuntime().availableProcessors(), op );
	}

	/** Execute the given {@param op} {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link FloatType} with
	 * as many threads as desired.
	 * 
	 * @param numThreads The number of threads for parallel execution. 
	 * @param op The {@link IFunction} to execute. */
	static public final Image<FloatType> inFloats(final int numThreads, final IFunction op) throws Exception
	{
		return apply(op, new FloatType(), numThreads );
	}

	/** Execute the given {@param op} {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link DoubleType} with
	 * as many threads as desired.
	 * 
	 * @param numThreads The number of threads for parallel execution. 
	 * @param op The {@link IFunction} to execute. */
	static public final Image<DoubleType> inDoubles(final int numThreads, final IFunction op) throws Exception
	{
		return apply(op, new DoubleType(), numThreads );
	}

	/** Execute the given {@param op} {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link DoubleType}.
	 * Uses as many concurrent threads as CPUs, defined by {@link Runtime#availableProcessors()}.
	 * 
	 * @param op The {@link IFunction} to execute. */
	static public final Image<DoubleType> inDoubles(final IFunction op) throws Exception
	{
		return inDoubles(Runtime.getRuntime().availableProcessors(), op);
	}
	
	/** Execute the given {@param op} {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link RGBALegacyType} with
	 * as many threads as desired.
	 * 
	 * @param numThreads The number of threads for parallel execution. 
	 * @param op The {@link IFunction} to execute. */
	static public final Image<RGBALegacyType> inRGBA(final int numThreads, final IFunction op) throws Exception
	{
		return apply(op, new RGBALegacyType(), numThreads);
	}

	/** Execute the given {@param op} {@link IFunction}, which runs for each pixel,
	 * and store the results in an {@link Image} of type {@link RGBALegacyType} with
	 * as many threads as desired.
	 * Uses as many concurrent threads as CPUs, defined by {@link Runtime#availableProcessors()}.
	 * 
	 * @param op The {@link IFunction} to execute. */
	static public final Image<RGBALegacyType> inRGBA(final IFunction op) throws Exception
	{
		return apply(op, new RGBALegacyType(), Runtime.getRuntime().availableProcessors());
	}

	/** Convenience method to avoid confusion with script wrappers that are themselves {@link Image}
	 *  rather than {@link IFunction}; this method ends up creating a copy of the image, in {@link FloatType}. */
	static public final Image<FloatType> inFloats(final Image<? extends RealType<?>> img) throws Exception {
		return inFloats(new ImageFunction(img));
	}

	/** Convenience method to avoid confusion with script wrappers that are themselves {@link Image}
	 *  rather than {@link IFunction}; this method ends up creating a copy of the image, in {@link DoubleType}. */
	static public final Image<DoubleType> inDoubles(final Image<? extends RealType<?>> img) throws Exception {
		return inDoubles(new ImageFunction(img));
	}

	/** Convenience method to avoid confusion with script wrappers that are themselves {@link Image}
	 *  rather than {@link IFunction}; this method ends up creating a copy of the image, in {@link RGBALegacyType}.
	 *  This method transforms an {@link IFunction} operation that returns a {@code double} for every pixel
	 *  into an RGBA image, by casting each double to an {@code int}. */
	static public final Image<RGBALegacyType> inRGBA(final Image<? extends RealType<?>> img) throws Exception
	{
		return inRGBA(new ImageFunction(img));
	}
}
