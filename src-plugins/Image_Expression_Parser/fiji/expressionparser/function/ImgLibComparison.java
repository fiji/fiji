package fiji.expressionparser.function;

import org.nfunk.jep.ParseException;

import mpicbg.imglib.type.numeric.RealType;

public class ImgLibComparison  {

	public static class GreaterThan <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		@Override
		public float evaluate(T t1, T t2) throws ParseException {
			return t1.getRealFloat() > t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return ">";
		}
		
		@Override
		public String getDocumentationString() {
			return "<h3>Element-wise 'greater than' comparison</h3>" +
			"This function compares its two operands, element-wise," +
			"and return the <i>FloatType</i> value 1 if the first operand is greater " +
			"than the second, and the <i>FloatType</i> value 0 otherwise.<br>" +
			"Comparisons are made on float.<br>" +
			"This function does singleton expansion, that is " +
			"if A and B are images and alpha and beta are numbers, then:" +
				"<ul>" +
				"	<li><code>A>B</code> will compare each pixel of A to the corresponding" +
				"pixels in B. This operation is defined only if A and B have the same number of pixels." +
				"	<li><code>A+alpha</code>will do singleton expansion and return a new image in which " +
				"each pixel of A is compared to alpha." +
				"	<li><code>alpha+beta</code>simply compare the two numbers." +
				"</ul>";
		}
	}

	public static class LowerThan <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		@Override
		public float evaluate(T t1, T t2) throws ParseException {
			return t1.getRealFloat() < t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "<";
		}
		
		@Override
		public String getDocumentationString() {
			return "<h3>Element-wise 'lower than' comparison</h3>" +
			"This function compares its two operands, element-wise," +
			"and return the <i>FloatType</i> value 1 if the first operand is lower " +
			"than the second, and the <i>FloatType</i> value 0 otherwise.<br>" +
			"See 'element-wise greater than' for more information.";
		}

	}

	public static class GreaterOrEqual <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		@Override
		public float evaluate(T t1, T t2) throws ParseException {
			return t1.getRealFloat() >= t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return ">=";
		}

		@Override
		public String getDocumentationString() {
			return "<h3>Element-wise 'greater or equal' comparison</h3>" +
			"This function compares its two operands, element-wise, " +
			"and return the <i>FloatType</i> value 1 if the first operand is greater than or equal to " +
			"the second, and the <i>FloatType</i> value 0 otherwise.<br>" +
			"See 'element-wise greater than' for more information.";
		}

	}

	public static class LowerOrEqual <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		@Override
		public float evaluate(T t1, T t2) throws ParseException {
			return t1.getRealFloat() <= t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "<=";
		}

		@Override
		public String getDocumentationString() {
			return "<h3>Element-wise 'lower or equal' comparison</h3>" +
			"This function compares its two operands, element-wise, " +
			"and return the <i>FloatType</i> value 1 if the first operand is lower than or equal to " +
			"the second, and the <i>FloatType</i> value 0 otherwise.<br>" +
			"See 'element-wise greater than' for more information.";
		}

	}

	public static class Equal <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		@Override
		public float evaluate(T t1, T t2) throws ParseException {
			return t1.getRealFloat() == t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "==";
		}

		@Override
		public String getDocumentationString() {
			return "<h3>Element-wise 'equal' comparison</h3>" +
			"This function compares its two operands, element-wise, " +
			"and return the <i>FloatType</i> value 1 if the first operand is equal to " +
			"the second, and the <i>FloatType</i> value 0 otherwise.<br>" +
			"Note the comparison is made with <i>float<i> precision. " +
			"See 'element-wise greater than' for more information.";
		}
	}


	public static class NotEqual <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

		@Override
		public float evaluate(T t1, T t2) throws ParseException {
			return t1.getRealFloat() != t2.getRealFloat() ? 1.0f :0.0f;
		}

		@Override
		public String getFunctionString() {
			return "!=";
		}

		@Override
		public String getDocumentationString() {
			return "<h3>Element-wise 'not equal' comparison</h3>" +
			"This function compares its two operands, element-wise, " +
			"and return the <i>FloatType</i> value 0 if the first operand is equal to " +
			"the second, and the <i>FloatType</i> value 1 otherwise.<br>" +
			"Note the comparison is made with <i>float<i> precision. " +
			"See 'element-wise greater than' for more information.";
		}
	}
}
