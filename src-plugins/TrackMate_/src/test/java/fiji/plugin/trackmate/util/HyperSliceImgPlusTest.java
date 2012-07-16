/**
 * 
 */
package fiji.plugin.trackmate.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Jean-Yves Tinevez
 *
 */
public class HyperSliceImgPlusTest {

	private static final long[] dim = new long[] { 16, 16, 64, 8, 32 };
	private static final AxisType[] axes = new AxisType[] {
		Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL, Axes.TIME };
	private static final float[] calibration = new float[] { 0.5f, 0.5f, 0.8f, 1, 5 };
	private static final String name = "Source";
	private static final int REMOVED_DIM_1 = 2; // we will remove Z
	private static final long DIM_POS_1 = dim[REMOVED_DIM_1] / 2; // fix it there
	private static final int REMOVED_DIM_2 = 3; // we will remove T in the Z-removed imgplus
	private static final long DIM_POS_2 = dim[REMOVED_DIM_2+1] / 2; // fix it there

	/** The source {@link ImgPlus}. */
	private ImgPlus<UnsignedByteType> source;
	/** We fix one Z. */
	private HyperSliceImgPlus<UnsignedByteType> imgplusZ;
	/** We fix one Z AND one T. */
	private HyperSliceImgPlus<UnsignedByteType> imgplusZT;



	/**
	 * Create a 5D image and store it in {@link #source}
	 */
	@Before
	public void setUp() throws Exception {
		// Set up source
		ArrayImgFactory<UnsignedByteType> factory = new ArrayImgFactory<UnsignedByteType>();
		ArrayImg<UnsignedByteType, ?> img = factory.create(dim , new UnsignedByteType());
		source = new ImgPlus<UnsignedByteType>(img);
		for (int d = 0; d < dim.length; d++) {
			source.setAxis(axes[d], d);
			source.setCalibration(calibration[d], d);
		}
		source.setName(name);

		// Set up first hyerpslice
		imgplusZ = new HyperSliceImgPlus<UnsignedByteType>(source, REMOVED_DIM_1, dim[REMOVED_DIM_1]/2);

		// Set up second hyperslice
		imgplusZT = new HyperSliceImgPlus<UnsignedByteType>(imgplusZ, REMOVED_DIM_2, dim[REMOVED_DIM_2+1]/2); // Time is now dim 3
	}



	private void reset() {
		for (UnsignedByteType type : source) {
			type.setZero();
		}
	}


	@Test
	public void testMetadata() {
		int index1 = 0;
		for (int d = 0; d < dim.length; d++) {
			if (d != REMOVED_DIM_1) {
				assertEquals(source.calibration(d), imgplusZ.calibration(index1), Float.MIN_VALUE);
				assertEquals(source.axis(d), imgplusZ.axis(index1));
				index1++;
			}
		}

		int index2 = 0;
		for (int d = 0; d < dim.length; d++) {
			if (d != REMOVED_DIM_1 && d != (REMOVED_DIM_2+1)) {
				assertEquals(source.calibration(d), imgplusZT.calibration(index2), Float.MIN_VALUE);
				assertEquals(source.axis(d), imgplusZT.axis(index2));
				index2++;
			}
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#randomAccess()}.
	 */
	@Test
	public void testRandomAccess() {

		reset();

		// Set one value in source image
		RandomAccess<UnsignedByteType> ra = source.randomAccess();
		long[] position = new long[source.numDimensions()];
		for (int d = 0; d < dim.length; d++) {
			position[d] = dim[d] / 2; // middle
		}
		position[REMOVED_DIM_1] = DIM_POS_1;
		position[REMOVED_DIM_2] = DIM_POS_2; // move to the hyper plane of interest
		ra.setPosition(position);
		final int val = 50;
		ra.get().set(val);

		int index1, index2;


		// check value in target images
		RandomAccess<UnsignedByteType> ra1 = imgplusZ.randomAccess();
		RandomAccess<UnsignedByteType> ra2 = imgplusZT.randomAccess();
		index1 = 0;
		index2 = 0;
		for (int d = 0; d < position.length; d++) {
			if (d != REMOVED_DIM_1) {
				ra1.setPosition(position[d], index1);
				index1++;
			}
			if (d != REMOVED_DIM_1 && d != REMOVED_DIM_2) {
				ra2.setPosition(position[d], index2);
				index2++;  
			}
		}

		assertEquals(
				String.format("Set the value %d at %s in source, so expected to find it at %s in target; instead got %d.", 
						val, printCoords(ra), printCoords(ra1), ra1.get().get()),
						val, ra1.get().get());

		assertEquals(
				String.format("Set the value %d at %s in source, so expected to find it at %s in target; instead got %d.", 
						val, printCoords(ra), printCoords(ra2), ra2.get().get()),
						val, ra2.get().get());

	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#numDimensions()}.
	 */
	@Test
	public void testNumDimensions() {
		assertEquals(source.numDimensions()-1, imgplusZ.numDimensions());
		assertEquals(source.numDimensions()-2, imgplusZT.numDimensions());
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#min(int)}.
	 */
	@Test
	public void testMinInt() {
		for (int d = 0; d < imgplusZ.numDimensions(); d++) {
			assertEquals(0, imgplusZ.min(d));
		}
		for (int d = 0; d < imgplusZT.numDimensions(); d++) {
			assertEquals(0, imgplusZT.min(d));
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#min(long[])}.
	 */
	@Test
	public void testMinLongArray() {
		long[] min1 = new long[imgplusZ.nDimensions];
		long[] expected1 = Util.getArrayFromValue(0l, imgplusZ.nDimensions);
		long[] min2 = new long[imgplusZT.nDimensions];
		long[] expected2 = Util.getArrayFromValue(0l, imgplusZT.nDimensions);
		imgplusZ.min(min1);
		imgplusZT.min(min2);
		assertArrayEquals(expected1, min1);
		assertArrayEquals(expected2, min2);
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#max(int)}.
	 */
	@Test
	public void testMaxInt() {
		int index1 = 0;
		for (int d = 0; d < imgplusZ.numDimensions(); d++) {
			if (d != REMOVED_DIM_1) {
				assertEquals(dim[d]-1, imgplusZ.max(index1));
				index1++;
			}
		}
		int index2 = 0;
		for (int d = 0; d < imgplusZT.numDimensions(); d++) {
			if (d != REMOVED_DIM_1 && d != (REMOVED_DIM_2+1)) {
				assertEquals(dim[d]-1, imgplusZT.max(index2));
				index2++;
			}
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#max(long[])}.
	 */
	@Test
	public void testMaxLongArray() {
		long[] max1 = new long[imgplusZ.nDimensions];
		long[] expected1 = new long[imgplusZ.nDimensions];
		long[] max2 = new long[imgplusZT.nDimensions];
		long[] expected2 = new long[imgplusZT.nDimensions];

		int index1 = 0;
		int index2 = 0;
		for (int d = 0; d < dim.length; d++) {
			if (d != REMOVED_DIM_1) {
				expected1[ index1 ] = dim[d]-1;
				index1++;
			}
			if (d != REMOVED_DIM_1 && d != (REMOVED_DIM_2+1)) {
				expected2[ index2 ] = dim[d]-1;
				index2++;
			}
		}

		imgplusZ.max(max1);
		imgplusZT.max(max2);
		assertArrayEquals(
				String.format("Expected max[] to be %s, but was %s", Util.printCoordinates(expected1), Util.printCoordinates(max1)),
				expected1, max1);
		assertArrayEquals(
				String.format("Expected max[] to be %s, but was %s", Util.printCoordinates(expected2), Util.printCoordinates(max2)),
				expected2, max2);
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#dimensions(long[])}.
	 */
	@Test
	public void testDimensions() {
		int index1 = 0;
		int index2 = 0;
		for (int d = 0; d < imgplusZ.numDimensions(); d++) {
			if (d != REMOVED_DIM_1) {
				assertEquals(source.dimension(d), imgplusZ.dimension(index1));
				index1++;
			}
		}
		for (int d = 0; d < imgplusZT.numDimensions(); d++) {
			if (d != REMOVED_DIM_1 && d != (REMOVED_DIM_2+1)) {
				assertEquals(source.dimension(d), imgplusZ.dimension(index2));
				index2++;
			}
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#cursor()}.
	 */
	@Test
	public void testCursor() {
		
		reset();
		
		// Write something in the target image
		Cursor<UnsignedByteType> cursor = imgplusZ.cursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			// Sum X & C in ( X, Y, C, T ) image
			cursor.get().set( cursor.getIntPosition(0) + cursor.getIntPosition(2) ); 
		}
		
		/* Compare what happened to the source. It should be zero everywhere
		 * BUT in the slice we modified with the hyper slice accessor. */
		Cursor<UnsignedByteType> scursor = source.cursor();
		while (scursor.hasNext()) {
			scursor.fwd();
			if (scursor.getIntPosition(REMOVED_DIM_1) == DIM_POS_1) {
				assertEquals( scursor.getIntPosition(0) + scursor.getIntPosition(3) , scursor.get().get() );
			} else {
				assertEquals( 0 , scursor.get().get() );
			}
		}

		reset();
		
		// Write something in the target image
		cursor = imgplusZT.cursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			// Still sum X & C in ( X, Y, C ) image		
			cursor.get().set( cursor.getIntPosition(0) + cursor.getIntPosition(2) ); 
		}
		
		/* Compare what happened to the source. It should be zero everywhere
		 * BUT in the slice we modified with the hyper slice accessor. */
		scursor = source.cursor();
		while (scursor.hasNext()) {
			scursor.fwd();
			if (scursor.getIntPosition(REMOVED_DIM_1) == DIM_POS_1 && scursor.getIntPosition(REMOVED_DIM_2+1) == DIM_POS_2) {
				int expected = scursor.getIntPosition(0) + scursor.getIntPosition(3);
				int got = scursor.get().get();
				assertEquals(
						String.format("At position %s in the source,  expected %d, but got %d.", 
								printCoords(scursor), expected, got), 
						 expected, got );
			} else {
				assertEquals( 0 , scursor.get().get() );
			}
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.util.HyperSliceImgPlus#size()}.
	 */
	@Test
	public void testSize() {
		long expected1 = source.size() / source.dimension(REMOVED_DIM_1);
		assertEquals(expected1, imgplusZ.size());

		long expected2 = source.size() / source.dimension(REMOVED_DIM_1) / source.dimension(REMOVED_DIM_2+1);
		assertEquals(expected2, imgplusZT.size());
	}

	/*
	 * UTILS
	 */

	private static final String printCoords(Localizable loc) {
		int[] coords = new int[loc.numDimensions()];
		loc.localize(coords);
		return Util.printCoordinates(coords);
	}

}
