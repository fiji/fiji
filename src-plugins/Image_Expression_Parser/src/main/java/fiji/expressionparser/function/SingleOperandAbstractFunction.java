package fiji.expressionparser.function;

import java.util.Stack;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

public abstract class SingleOperandAbstractFunction <T extends RealType<T>> extends PostfixMathCommand implements ImgLibFunction<T> {

	@SuppressWarnings("unchecked")
	@Override
	public final void run(final Stack inStack) throws ParseException {
		checkStack(inStack); // check the stack

		Object param = inStack.pop();
		Object result = null;

		if (param instanceof Image<?>) {
			
			Image<T> img = (Image) param;
			result = evaluate(img);			
		
		} else if (param instanceof FloatType) {

			FloatType t = (FloatType) param;
			result = evaluate(t);

		} else {
			throw new ParseException("In function '"+ getFunctionString()
					+"': Bad type of operand: "+param.getClass().getSimpleName() );
		}
		
		inStack.push(result);
	}
	
	 /**
	  * Evaluate this function on one ImgLib images. A new {@link Image} of {@link FloatType}  
	  * is returned, so as to avoid underflow and overflow problems on bounded types (e.g. ByeType).
	  * @param img the image 
	  * @return  The new resulting image
	  */
	 public abstract <R extends RealType<R>> Image<FloatType> evaluate(final Image<R> img) throws ParseException;

	 /**
	  * Evaluate this function on a numeric {@link RealType} type. 
	  * A new {@link Image} of {@link FloatType}  
	  * is returned, so as to avoid underflow and overflow problems on 
	  * bounded types (e.g. ByeType). 
	  * 
	  * @param alpha the numeric type 
	  * @return  The new resulting image
	  */
	 public abstract <R extends RealType<R>> Image<FloatType> evaluate(final R alpha) throws ParseException;	
}
