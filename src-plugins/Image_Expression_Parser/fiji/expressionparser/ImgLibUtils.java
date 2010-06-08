package fiji.expressionparser;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class ImgLibUtils  {
	
	/**
	 * Copy the given Image of type extending RealType to a FloatType image. 
	 * @param <T>
	 * @param img
	 * @return
	 */
	public static final <T extends RealType<T>> Image<FloatType> copyToFloatTypeImage(Image<T> img) {
		// Create target image
		Image<FloatType> target = new ImageFactory<FloatType>(new FloatType(), img.getContainerFactory())
			.createImage(img.getDimensions(), img.getName());
		// Check if all Containers are compatibles
		boolean compatible_containers = img.getContainer().compareStorageContainerCompatibility(target.getContainer());
		
		if (compatible_containers) {
			
			Cursor<T> ic = img.createCursor();
			Cursor<FloatType> tc = target.createCursor();
			while (ic.hasNext()) {
				ic.fwd();
				tc.fwd();
				tc.getType().set( ic.getType().getRealFloat() );
			}
			ic.close();
			tc.close();
			
		} else {
			
			LocalizableCursor<FloatType> tc = target.createLocalizableCursor();
			LocalizableByDimCursor<T> ic = img.createLocalizableByDimCursor();
			while (tc.hasNext()) {
				tc.fwd();
				ic.setPosition(tc);
				tc.getType().set( ic.getType().getRealFloat() );
			}
			ic.close();
			tc.close();
			
		}
		
		return target;
	}
	

}
