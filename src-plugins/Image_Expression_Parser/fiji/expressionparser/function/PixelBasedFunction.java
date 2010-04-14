package fiji.expressionparser.function;

import org.nfunk.jep.function.PostfixMathCommand;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

public abstract class PixelBasedFunction <T extends NumericType<T>> extends PostfixMathCommand implements ImgLibFunction<T> {
	
	@Override
	public final Image<FloatType> evaluate(final Image<T> img1, final Image<T> img2) {
		
		// Create target image
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), img1.getContainerFactory())
			.createImage(img1.getDimensions(), String.format("%s %s %s", img1.getName(), toString(), img2.getName()));
		
		// Check if all Containers are compatibles
		boolean compatible_containers = img1.getContainer().compareStorageContainerCompatibility(img2.getContainer());
		
		if (compatible_containers) {
			
			Cursor<T> c1 = img1.createCursor();
			Cursor<T> c2 = img2.createCursor();
			Cursor<FloatType> rc = result.createCursor();
			while (c1.hasNext()) {
				c1.fwd();
				c2.fwd();
				rc.fwd();
				rc.getType().set( evaluate(c1.getType(), c2.getType()) );
			}
			
		} else {
			
			LocalizableCursor<FloatType> rc = result.createLocalizableCursor();
			LocalizableByDimCursor<T> c1 = img1.createLocalizableByDimCursor();
			LocalizableByDimCursor<T> c2 = img2.createLocalizableByDimCursor();
			while (rc.hasNext()) {
				rc.fwd();
				c1.setPosition(rc);
				c2.setPosition(rc);
				rc.getType().set( evaluate(c1.getType(), c2.getType()) );
			}
			
		}
		
		return result;
	}
	
	@Override
	public final Image<FloatType> evaluate(final Image<T> img, final T alpha) {
		// Create target image
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), img.getContainerFactory())
		.createImage(img.getDimensions(), String.format("%5f %s %s", alpha.getReal(), toString(), img.getName()) );
		
		Cursor<T> ic = img.createCursor();
		Cursor<FloatType> rc = result.createCursor();
		
		while (rc.hasNext()) {
			rc.fwd();
			ic.fwd();
			rc.getType().set(evaluate(alpha, ic.getType()));
		}
		return result;
	}



}
