package fiji.expressionparser.function;

import org.nfunk.jep.ParseException;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibLogical {

	public static final String DOCUMENTATION_STRING_AND = "<h3>Element-wise 'AND' logical operation</h3> " +
		"This function performs the pixel-wise logical AND operation, extended to real numbers. " +
		"That is: the value returned by this operation will be the float value 1 if " +
		"and only if the its operands are non zero, and 0 otherwise.";

	public static final String DOCUMENTATION_STRING_OR = "<h3>Element-wise 'OR' logical operation</h3> " +
		"This function performs the pixel-wise logical OR operation, extended to real numbers. " +
		"That is: the value returned by this operation will be the float value 1 if " +
		"one of its two operands is non zero, and 0 if and only if the two operands " +
		"are 0.";
	
	public static final String DOCUMENTATION_STRING_NOT = "<h3>Element-wise 'NOT' logical operation</h3> " +
	"This function performs the pixel-wise logical NOT operation, extended to real numbers. " +
	"That is: the value returned by this operation will be the float value 1 if " +
	"its operand is zero, and will be 0 otherwise.";

	public static final class And <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		public And() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() != 0f && t2.getRealFloat() != 0f ? 1.0f : 0.0f;
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_AND;
		}

		@Override
		public String getFunctionString() {			
			return "&&";
		}
		
	}

	public static final class Or <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		public Or() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() != 0f || t2.getRealFloat() != 0f ? 1.0f : 0.0f;
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_OR;
		}

		@Override
		public String getFunctionString() {			
			return "||";
		}
		
	}

	public static final class Not <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

		public Not() {
			numberOfParameters = 1;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t) throws  ParseException {
			return t.getRealFloat() == 0f ? 1.0f : 0.0f;
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_NOT;
		}

		@Override
		public String getFunctionString() {			
			return "!";
		}
		
	}

}
