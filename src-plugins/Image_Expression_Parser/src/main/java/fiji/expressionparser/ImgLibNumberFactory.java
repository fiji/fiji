package fiji.expressionparser;

import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.type.Complex;
import org.nfunk.jep.type.NumberFactory;

/**
 * This is a number factory that will be used to generate internal 
 * representation of number within the expression parser logic. 
 * For coherence with ImgLib, we choose to represent number as ImgLib
 * types, and choose the concrete {@link FloatType} class.
 * <p> 
 * As of now, we do not deal with complex numbers. Everytime we meet
 * a complex number, we take only its real part.  
 * @author Jean-Yves Tinevez (jeanyves.tinevez@gmail.com)
 *
 * May 2, 2010
 *
 */
public class ImgLibNumberFactory implements NumberFactory {

	@Override
	public Object createNumber(String value) throws ParseException {
		return new FloatType(new Float(value).floatValue());
	}

	@Override
	public Object createNumber(double value) throws ParseException {
		return new FloatType((float)value);
	}

	@Override
	public Object createNumber(int value) throws ParseException {
		return new FloatType(value);
	}

	@Override
	public Object createNumber(short value) throws ParseException {
		return new FloatType(value);
	}

	@Override
	public Object createNumber(float value) throws ParseException {
		return new FloatType(value);
	}

	@Override
	public Object createNumber(boolean value) throws ParseException {
		return new FloatType(value? 1.0f : 0.0f);
	}

	@Override
	public Object createNumber(Number value) throws ParseException {
		return new FloatType(value.floatValue());
	}

	@Override
	public Object createNumber(Complex value) throws ParseException {
		return new FloatType(value.floatValue());
	}

	@Override
	public Object getMinusOne() {
		return new FloatType(-1.0f);
	}

	@Override
	public Object getOne() {
		return new FloatType(1.0f);
	}

	@Override
	public Object getTwo() {
		return new FloatType(2.0f);
	}

	@Override
	public Object getZero() {
		return new FloatType(0.0f);
	}

}
