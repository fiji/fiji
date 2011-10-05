package gadgets;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.MaskCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;

public class MaskFactory {
	
	public enum CombinationMode {
		AND, OR, NONE
	}
	
	/**
	 * Create a new mask image without any specific content, but with
	 * a defined size.
	 */
	public static Image<BitType> createMask(int[] dim) {
		ImageFactory<BitType> imageFactory = new ImageFactory<BitType>(
				new BitType(), new ArrayContainerFactory());

		return imageFactory.createImage(dim);
	}
	
	/**
	 * Create a new mask image with a defined size and preset content.
	 */
	public static Image<BitType> createMask(int[] dim, boolean val) {
		Image<BitType> mask = createMask(dim);
		
		for (BitType t : mask)
			t.set(val);
		
		return mask;
	}
	
	/**
	 * Create a new mask image with a defined size and preset content.
	 */
	public static Image<BitType> createMask(int[] dim, int[] roiOffset, int[] roiDim) {
		final Image<BitType> mask = createMask(dim);
		final LocalizableCursor<BitType> cursor = mask.createLocalizableCursor();

		final int[] pos = mask.createPositionArray();
		final int dims = pos.length;

		// create an array with the max corner of the ROI
		final int[] roiOffsetMax = mask.createPositionArray();
		for (int i=0; i<dims; ++i)
			roiOffsetMax[i] = roiOffset[i] + roiDim[i];
		// go through the mask and mask points as valid that are in the ROI
		while ( cursor.hasNext() ) {
			cursor.getPosition(pos);
			boolean valid = true;
			// test if the current position is contained in the ROI
			for(int i=0; i<dims; ++i)
				valid &= pos[i] > roiOffset[i] && pos[i] < roiOffsetMax[i];
			cursor.getType().set(valid);
		}
		cursor.close();

		return mask;
	}

	/**
	 * Create a new mask based on a threshold condition for two images.
	 */
	public static<T extends RealType<T>> Image<BitType> createMask(Image<T> ch1, Image<T> ch2,
			T threshold1, T threshold2, ThresholdMode tMode, CombinationMode cMode) {
		
		Image<BitType> mask = createMask(ch1.getDimensions());
		Cursor<T> cursor1 = ch1.createCursor();
		Cursor<T> cursor2 = ch2.createCursor();
		Cursor<BitType> maskCursor = mask.createCursor();
		
		while (cursor1.hasNext() && cursor2.hasNext() && maskCursor.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			maskCursor.fwd();
			
			boolean ch1Valid, ch2Valid;
			
			T data1 = cursor1.getType();
			T data2 = cursor2.getType();
			
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
			
			BitType maskData = maskCursor.getType();
			
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
		
		cursor1.close();
		cursor2.close();
		maskCursor.close();
		
		return mask;
	}
}