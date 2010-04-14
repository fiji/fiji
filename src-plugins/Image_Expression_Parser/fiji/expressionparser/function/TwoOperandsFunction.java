package fiji.expressionparser.function;

import java.util.Stack;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

public abstract class TwoOperandsFunction <T extends NumericType<T>>  extends PostfixMathCommand {

	/**
	 * Evaluate this function on two images, and return result as an image.
	 * @param img1  The first image 
	 * @param img2  The second image 
	 * @return  The resulting image
	 */
	public abstract Image<FloatType> evaluate(Image<T> img1, Image<T> img2);

	/**
	 * Singleton expansion. Evaluate this function on an image and an image that would be of same 
	 * dimension but with all element being the number passed in argument.
	 * @param img  The image 
	 * @param alpha  The number to do singleton expansion on 
	 * @return  The resulting image 
	 */
	public abstract Image<FloatType> evaluate(Image<T> img, T alpha);

	/**
	 * Evaluate this function on two numbers. This class only allow operation on two numbers 
	 * to return a number.
	 * @param alpha1  The first number 
	 * @param alpha2  The second number
	 * @return  The resulting number
	 */
	 public abstract FloatType evaluate(T type, T type2);

	 
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack inStack)	throws ParseException {
		checkStack(inStack); // check the stack

		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		Object result = null;

		if (param1 instanceof Image<?>) {
			
			if (param2 instanceof Image<?>) {
				result = evaluate((Image<T>)param1, (Image<T>)param2);
			} else if (param2 instanceof Number) {
				T t2 = (T) new FloatType(((Number)param2).floatValue());
				result = evaluate((Image<T>)param1, t2);
			} else {
				throw new ParseException("In function "+this.getClass().getSimpleName()
						+": Bad type of operand 2: "+param2.getClass().getSimpleName() );
			}
		
		} else if (param1 instanceof Number) {

			T t1 = (T) new FloatType(((Number)param1).floatValue());
			
			if (param2 instanceof Image<?>) {
				result = evaluate((Image<T>)param2, t1);
			} else if (param2 instanceof Number) {
				T t2 = (T) new FloatType(((Number)param2).floatValue());
				result = evaluate(t1, t2);
			} else {
				throw new ParseException("In function "+this.getClass().getSimpleName()
						+": Bad type of operand 2: "+param2.getClass().getSimpleName() );
			}

		} else {
			throw new ParseException("In function "+this.getClass().getSimpleName()
					+": Bad type of operand 1: "+param1.getClass().getSimpleName() );
		}
		
		inStack.push(result);
	}


}
