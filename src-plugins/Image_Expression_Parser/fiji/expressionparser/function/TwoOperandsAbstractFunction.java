package fiji.expressionparser.function;

import java.util.Stack;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

public abstract class TwoOperandsAbstractFunction <T extends RealType<T>> extends PostfixMathCommand 
		implements ImgLibFunction<T> {

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
				T t2 = (T) new FloatType(((Number) param2).floatValue());
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
			throw new ParseException("In function '"+ getFunctionString()
					+"': Bad type of operand 1: "+param1.getClass().getSimpleName() );
		}
		
		inStack.push(result);
	}
	
	/**
	 * Evaluate this function on two numeric types. Argument types can be of any numeric type, but a float must
	 * be returned, so as to avoid underflow and overflow problems on bounded types (e.g. ByeType).
	 * @param alpha1  The first number 
	 * @param alpha2  The second number
	 * @return  The resulting number
	 */
	 public abstract float evaluate(final T t1, final T t2) throws ParseException;

	 /**
	  * Evaluate this function on two ImgLib images. A new {@link Image} of {@link FloatType}  
	  * is returned, so as to avoid underflow and overflow problems on bounded types (e.g. ByeType).
	  * @param img1 the first image 
	  * @param img2 the second image 
	  * @return  The new resulting image
	  */
	 public abstract Image<FloatType> evaluate(Image<T> img1, Image<T> img2) throws ParseException;

	 /**
	  * Evaluate this function on an ImgLib images and a numeric {@link RealType} type.
	  * A new {@link Image} of {@link FloatType}  
	  * is returned, so as to avoid underflow and overflow problems on 
	  * bounded types (e.g. ByeType). This method should implement a singleton expansion
	  * of the method {@link #evaluate(Image, Image)}, as meant by the implement
	  * function.
	  * 
	  * @param img the image 
	  * @param alpha the numeric type 
	  * @return  The new resulting image
	  */
	 public abstract Image<FloatType> evaluate(Image<T> img, T alpha) throws ParseException;

}
