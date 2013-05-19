package gadgets;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import algorithms.MissingPreconditionException;

public class MaskFactory {
	
	public enum CombinationMode {
		AND, OR, NONE
	}
	
	/**
	 * Create a new mask image without any specific content, but with
	 * a defined size.
	 */
	public static RandomAccessibleInterval<BitType> createMask(long[] dim) {
		ImgFactory< BitType > imgFactory = new ArrayImgFactory< BitType >();
		return imgFactory.create(dim, new BitType());
	}
	
	/**
	 * Create a new mask image with a defined size and preset content.
	 */
	public static RandomAccessibleInterval<BitType> createMask(long[] dim, boolean val) {
		RandomAccessibleInterval<BitType> mask = createMask(dim);
		
		for (BitType t : Views.iterable(mask))
			t.set(val);
		
		return mask;
	}
	
	/**
	 * Create a new mask image with a defined size and preset content.
	 * @throws MissingPreconditionException
	 */
	public static RandomAccessibleInterval<BitType> createMask(long[] dim, long[] roiOffset, long[] roiDim)
			throws MissingPreconditionException {
		if (dim.length != roiOffset.length || dim.length != roiDim.length) {
			throw new MissingPreconditionException("The dimensions of the mask as well as the ROIs and his offset must be the same.");
		}

		final RandomAccessibleInterval<BitType> mask = createMask(dim);
		final int dims = mask.numDimensions();
		final long[] pos = new long[dims];
		

		// create an array with the max corner of the ROI
		final long[] roiOffsetMax = new long[dims];
		for (int i=0; i<dims; ++i)
			roiOffsetMax[i] = roiOffset[i] + roiDim[i];
		// go through the mask and mask points as valid that are in the ROI
		Cursor<BitType> cursor = Views.iterable(mask).localizingCursor();
		while ( cursor.hasNext() ) {
			cursor.fwd();
			cursor.localize(pos);
			boolean valid = true;
			// test if the current position is contained in the ROI
			for(int i=0; i<dims; ++i)
				valid &= pos[i] >= roiOffset[i] && pos[i] < roiOffsetMax[i];
			cursor.get().set(valid);
		}

		return mask;
	}

	/**
	 * Create a new mask based on a threshold condition for two images.
	 */
	public static<T extends RealType< T >> RandomAccessibleInterval<BitType> createMask(
			RandomAccessibleInterval<T> ch1, RandomAccessibleInterval<T> ch2,
			T threshold1, T threshold2, ThresholdMode tMode, CombinationMode cMode) {
		
		final long[] dims = new long[ ch1.numDimensions() ];
		ch1.dimensions(dims);
		RandomAccessibleInterval<BitType> mask = createMask(dims);
		Cursor<T> cursor1 = Views.iterable(ch1).cursor();
		Cursor<T> cursor2 = Views.iterable(ch2).cursor();
		Cursor<BitType> maskCursor = Views.iterable(mask).cursor();
		
		while (cursor1.hasNext() && cursor2.hasNext() && maskCursor.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			maskCursor.fwd();
			
			boolean ch1Valid, ch2Valid;
			
			T data1 = cursor1.get();
			T data2 = cursor2.get();
			
			// get relation to threshold
			if (tMode == ThresholdMode.Above) {
				ch1Valid = data1.compareTo(threshold1) > 0;
				ch2Valid = data2.compareTo(threshold2) > 0;
			} else if (tMode == ThresholdMode.Below) {
				ch1Valid = data1.compareTo(threshold1) < 0;
				ch2Valid = data2.compareTo(threshold2) < 0;
			} else {
				throw new UnsupportedOperationException();
			}
			
			BitType maskData = maskCursor.get();
			
			// combine the results into mask
			if (cMode == CombinationMode.AND) {
				maskData.set( ch1Valid && ch2Valid );
			} else if (cMode == CombinationMode.OR) {
				maskData.set( ch1Valid || ch2Valid );
			} else if (cMode == CombinationMode.NONE) {
				maskData.set( !(ch1Valid || ch2Valid) );
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		return mask;
	}

	/**
	 * Creates a new mask of the given dimensions, based on the image data
	 * in the passed image. If the requested dimensionality is higher than
	 * what is available in the data, the data gets repeated in the higher
	 * dimensions.
	 *
	 * @param dim The dimensions of the new mask image
	 * @param origMask The image from which the mask should be created from
	 */
	public static<T extends RealType< T >> RandomAccessibleInterval<BitType> createMask(
			final long[] dim, final RandomAccessibleInterval<T> origMask) {
		final RandomAccessibleInterval<BitType> mask = createMask(dim);
		final long[] origDim = new long[ origMask.numDimensions() ];
		origMask.dimensions(origDim);

		// test if original mask and new mask have same dimensions
		if (Arrays.equals(dim, origDim)) {
			// copy the input image to the mask output image
			Cursor<T> origCursor = Views.iterable(origMask).localizingCursor();
			RandomAccess<BitType> maskCursor = mask.randomAccess();
			while (origCursor.hasNext()) {
				origCursor.fwd();
				maskCursor.setPosition(origCursor);
				boolean value = origCursor.get().getRealDouble() > 0.001;
				maskCursor.get().set(value);
			}
		} else if (dim.length > origDim.length) {
			// sanity check
			for (int i=0; i<origDim.length; i++) {
				if (origDim[i] != dim[i])
					throw new UnsupportedOperationException("Masks with lower dimensionality than the image, "
							+ " but a different extent are not yet supported.");
			}
			// mask and image have different dimensionality and maybe even a different extent
			Cursor<T> origCursor = Views.iterable(origMask).localizingCursor();
			RandomAccess<BitType> maskCursor = mask.randomAccess();
			final long[] pos = new long[ origMask.numDimensions() ];
			// iterate over the original mask
			while (origCursor.hasNext()) {
				origCursor.fwd();
				origCursor.localize(pos);
				boolean value = origCursor.get().getRealDouble() > 0.001;
				// set available (lower dimensional) position information
				for (int i=0; i<origDim.length; i++)
					// setPosition requires first the position and then the dimension
					maskCursor.setPosition(pos[i], i);
				// go through the missing dimensions and set the value
				for (int i=origDim.length; i<dim.length; i++)
					for (int j=0; j<dim[i]; j++) {
						// setPosition requires first the position and then the dimension
						maskCursor.setPosition(j, i);
						maskCursor.get().set(value);
					}
			}
		} else if (dim.length < origDim.length) {
			// mask has more dimensions than image
			throw new UnsupportedOperationException("Masks with more dimensions than the image are not supported, yet.");
		} else {
			// mask and image have a different extent, but are equal in dimensionality. Scale it?
			throw new UnsupportedOperationException("Masks with same dimensionality, but a different extent than the image are not supported, yet.");
		}

		return mask;
	}
}