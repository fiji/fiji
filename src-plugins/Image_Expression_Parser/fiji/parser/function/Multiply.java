package fiji.parser.function;

import java.util.Stack;

import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

import org.nfunk.jep.ParseException;

public class Multiply<T extends NumericType<T>> extends org.nfunk.jep.function.Multiply {

	public Multiply() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {

		checkStack(stack); // check the stack

		Object product = stack.pop();
		Object param;
		int i = 1;
		
		while (i < curNumberOfParameters) {
			
			param = stack.pop();

			if (product instanceof Image<?>) {
				
				if (param instanceof Image<?>) {
					// Element by element product
					product = multiplyImages( (Image<T>) product, (Image<T>) param) ;
				} else {
					// Singleton expansion
					product = multiplyImageByConstant( (Image<T>) product, (Number) param);
				}
				
			} else {
				
				if (param instanceof Image<?>) {
					// Singleton expansion
					product = multiplyImageByConstant( (Image<T>) param, (Number) product);
				} else {
					// Numbers product
					product = mul((Number) product, (Number) param) ;
				}
				
			}
			
			i++;
		}

		stack.push(product);

		return;
	}

	private Image<FloatType> multiplyImageByConstant(Image<T> img, Number alpha) {
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory())
			.createImage(img.getDimensions(), String.format("%f x %s", alpha.doubleValue(), img.getName()));
		Cursor<FloatType> res_cursor = result.createCursor();
		Cursor<T> img_cursor = img.createCursor();
		while (res_cursor.hasNext()) {
			res_cursor.fwd();
			img_cursor.fwd();
			res_cursor.getType().set( alpha.floatValue() * img_cursor.getType().getReal());
		}
		return result;
	}

	/**
	 * Careful, we assume we have compatible storage containers!
	 * @param i1
	 * @param i2
	 * @return
	 */
	private Image<FloatType> multiplyImages(Image<T> i1, Image<T> i2) {
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory())
		.createImage(i1.getDimensions(), String.format("%s x %s", i1.getName(), i2.getName()));
		Cursor<T> i1c = i1.createCursor();
		Cursor<T> i2c = i2.createCursor();
		Cursor<FloatType> rc = result.createCursor();
		while (rc.hasNext()) {
			rc.fwd();
			i1c.fwd();
			i2c.fwd();
			rc.getType().set( i1c.getType().getReal() * i2c.getType().getReal() );
		}
		return result;
	}
	
}
