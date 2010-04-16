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

	}



}
