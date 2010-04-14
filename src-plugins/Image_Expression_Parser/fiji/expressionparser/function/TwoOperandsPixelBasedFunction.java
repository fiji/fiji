package fiji.expressionparser.function;

import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

public abstract class TwoOperandsPixelBasedFunction<T extends NumericType<T>> extends TwoOperandsFunction<T> {

	@Override
	public Image<FloatType> evaluate(Image<T> img1, Image<T> img2) {
		
		// Create target image
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory())
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
		}

		
		
		
		return result;
	}



}
