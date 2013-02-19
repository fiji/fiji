package fiji.expressionparser.function;

import org.nfunk.jep.ParseException;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibComparison  {


	public static final String DOCUMENTATION_STRING_GREATER_THAN = "<h3>Element-wise 'greater than' comparison</h3> " +
		"This function compares its two operands, element-wise," +
		"and return the <i>FloatType</i> value 1 if the first operand is greater " +
		"than the second, and the <i>FloatType</i> value 0 otherwise.<br> " +
		"Comparisons are made on float.<br>" +
		"This function does singleton expansion, that is " +
		"if A and B are images and alpha and beta are numbers, then: " +
		"<ul> " +
		"	<li><code>A>B</code> will compare each pixel of A to the corresponding " +
		"pixels in B. This operation is defined only if A and B have the same number of pixels. " +
		"	<li><code>A+alpha</code>will do singleton expansion and return a new image in which " +
		"each pixel of A is compared to alpha. " +
		"	<li><code>alpha+beta</code>simply compare the two numbers. " +
		"</ul>";

	public static final String DOCUMENTATION_STRING_LOWER_THAN = "<h3>Element-wise 'lower than' comparison</h3> " +
		"This function compares its two operands, element-wise, " +
		"and return the <i>FloatType</i> value 1 if the first operand is lower " +
		"than the second, and the <i>FloatType</i> value 0 otherwise.<br> " +
		"See 'element-wise greater than' for more information.";

	public static final String DOCUMENTATION_STRING_GREATER_OR_EQUAL = "<h3>Element-wise 'greater or equal' comparison</h3> " +
		"This function compares its two operands, element-wise, " +
		"and return the <i>FloatType</i> value 1 if the first operand is greater than or equal to " +
		"the second, and the <i>FloatType</i> value 0 otherwise.<br> " +
		"See 'element-wise greater than' for more information.";

	public static final String DOCUMENTATION_STRING_LOWER_OR_EQUAL = "<h3>Element-wise 'lower or equal' comparison</h3> " +
		"This function compares its two operands, element-wise, " +
		"and return the <i>FloatType</i> value 1 if the first operand is lower than or equal to " +
		"the second, and the <i>FloatType</i> value 0 otherwise.<br> " +
		"See 'element-wise greater than' for more information.";

	public static final String DOCUMENTATION_STRING_EQUAL = "<h3>Element-wise 'equal' comparison</h3> " +
		"This function compares its two operands, element-wise, " +
		"and return the <i>FloatType</i> value 1 if the first operand is equal to " +
		"the second, and the <i>FloatType</i> value 0 otherwise.<br> " +
		"Note the comparison is made with <i>float<i> precision. " +
		"See 'element-wise greater than' for more information.";

	public static final String DOCUMENTATION_STRING_NOT_EQUAL = "<h3>Element-wise 'not equal' comparison</h3> " +
		"This function compares its two operands, element-wise, " +
		"and return the <i>FloatType</i> value 0 if the first operand is equal to " +
		"the second, and the <i>FloatType</i> value 1 otherwise.<br> " +
		"Note the comparison is made with <i>float<i> precision. " +
		"See 'element-wise greater than' for more information.";
	

	
	public static final class GreaterThan <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {
		
		public GreaterThan() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() > t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return ">";
		}
		
		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_GREATER_THAN;
		}
		
		@Override
		public String toString() {
			return "Pixel-wise greater than";
		}
	}

	public static final class LowerThan <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

			
		public LowerThan() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() < t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "<";
		}
		
		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_LOWER_THAN;
		}
		
		@Override
		public String toString() {
			return "Pixel-wise lower than";
		}

	}

	public static final class GreaterOrEqual <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		public GreaterOrEqual() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() >= t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return ">=";
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_GREATER_OR_EQUAL; 
		}
		
		@Override
		public String toString() {
			return "Pixel-wise greater or equal";
		}

	}

	public static final class LowerOrEqual <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {
		
		public LowerOrEqual() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() <= t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "<=";
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_LOWER_OR_EQUAL;
		}

		@Override
		public String toString() {
			return "Pixel-wise lower or equal";
		}
	}

	public static final class Equal <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		public Equal() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() == t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "==";
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_EQUAL; 
		}
		
		@Override
		public String toString() {
			return "Pixel-wise equal";
		}
	}

	public static final class NotEqual <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		public NotEqual() {
			numberOfParameters = 2;
		}
		
		@Override
		public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
			return t1.getRealFloat() != t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "!=";
		}

		@Override
		public String getDocumentationString() {
			return DOCUMENTATION_STRING_NOT_EQUAL; 
		}
		
		@Override
		public String toString() {
			return "Pixel-wise not equal";
		}
	}
}
