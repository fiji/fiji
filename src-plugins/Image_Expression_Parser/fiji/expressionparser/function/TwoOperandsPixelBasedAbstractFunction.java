package fiji.expressionparser.function;

import java.util.Stack;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

public abstract class TwoOperandsPixelBasedAbstractFunction <T extends RealType<T>> extends PostfixMathCommand implements ImgLibFunction<T> {

	@SuppressWarnings("unchecked")
	@Override
	public void run(final Stack inStack) throws ParseException {
		checkStack(inStack); // check the stack

		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		Object result = null;

		if (param1 instanceof Image<?>) {
			
			if (param2 instanceof Image<?>) {
				result = evaluate((Image<T>)param1, (Image<T>)param2);
			} else if (param2 instanceof Number) {
				T t2 = (T) new FloatType(1.0f);
				result = evaluate((Image<T>)param1, t2);
			} else {
				throw new ParseException("In function '" + getFunctionString()
						+"': Bad type of operand 2: "+param2.getClass().getSimpleName() );
			}
		
		} else if (param1 instanceof Number) {

			T t1 = (T) new FloatType(((Number)param1).floatValue());
			
			if (param2 instanceof Image<?>) {
				result = evaluate((Image<T>)param2, t1);
			} else if (param2 instanceof Number) {
				T t2 = (T) new FloatType(((Number)param2).floatValue());
				result = new Float(evaluate(t1, t2));
			} else {
				throw new ParseException("In function '" + getFunctionString()
						+"': Bad type of operand 2: "+param2.getClass().getSimpleName() );
			}

		} else {
			throw new ParseException("In function '" + getFunctionString()
					+"': Bad type of operand 1: "+param1.getClass().getSimpleName() );
		}
		
		inStack.push(result);
	}

	/**
	 * Evaluate this function on two images, and return result as an image.
	 * @param img1  The first image 
	 * @param img2  The second image 
	 * @return  The resulting image
	 */
	public final Image<FloatType> evaluate(final Image<T> img1, final Image<T> img2) throws ParseException {
		
		// Create target image
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), img1.getContainerFactory())
			.createImage(img1.getDimensions(), String.format("%s %s %s", img1.getName(), getFunctionString(), img2.getName()));
		
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
			c1.close();
			c2.close();
			rc.close();
			
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
			c1.close();
			c2.close();
			rc.close();
			
		}
		
		return result;
	}
	
	/**
	 * Singleton expansion. Evaluate this function on an image and an image that would be of same 
	 * dimension but with all element being the number passed in argument.
	 * @param img  The image 
	 * @param alpha  The number to do singleton expansion on 
	 * @return  The resulting image 
	 */
	public final Image<FloatType> evaluate(final Image<T> img, final T alpha) throws ParseException {
		// Create target image
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), img.getContainerFactory())
			.createImage(img.getDimensions(), String.format("%.1f %s %s", alpha.getRealFloat(), getFunctionString(), img.getName()) );
		
		Cursor<T> ic = img.createCursor();
		Cursor<FloatType> rc = result.createCursor();
		
		while (rc.hasNext()) {
			rc.fwd();
			ic.fwd();
			rc.getType().set(evaluate(alpha, ic.getType()));
		}
		rc.close();
		ic.close();
				
		return result;
	}

	/**
	 * Evaluate this function on two numeric types. Argument types can be of any numeric type, but a float must
	 * be returned, so as to avoid underflow and overflow problems on bounded types (e.g. ByeType).
	 * @param alpha1  The first number 
	 * @param alpha2  The second number
	 * @return  The resulting number
	 */
	 public abstract float evaluate(final T t1, final T t2) throws ParseException;

}
