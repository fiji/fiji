package fiji.expressionparser.function;

import java.util.Stack;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.ParseException;

public abstract class TwoOperandsPixelBasedFunction <T extends RealType<T>> extends PixelBasedFunction<T> {

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
				throw new ParseException("In function "+this.getClass().getSimpleName()
						+": Bad type of operand 2: "+param2.getClass().getSimpleName() );
			}
		
		} else if (param1 instanceof Number) {

			T t1 = (T) new FloatType(((Number)param1).floatValue());
			
			if (param2 instanceof Image<?>) {
				result = evaluate((Image<T>)param2, t1);
			} else if (param2 instanceof Number) {
				T t2 = (T) new FloatType(((Number)param2).floatValue());
				result = new Float(evaluate(t1, t2));
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
