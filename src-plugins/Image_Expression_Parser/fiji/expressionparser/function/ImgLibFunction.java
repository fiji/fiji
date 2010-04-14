package fiji.expressionparser.function;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

public interface ImgLibFunction <T extends NumericType<T>> {

	/**
	 * Return a String describing this operator
	 */
	public abstract String toString();
	
	/**
	 * Evaluate this function on two images, and return result as an image.
	 * @param img1  The first image 
	 * @param img2  The second image 
	 * @return  The resulting image
	 */
	public abstract Image<FloatType> evaluate(final Image<T> img1, final Image<T> img2);

	/**
	 * Singleton expansion. Evaluate this function on an image and an image that would be of same 
	 * dimension but with all element being the number passed in argument.
	 * @param img  The image 
	 * @param alpha  The number to do singleton expansion on 
	 * @return  The resulting image 
	 */
	public abstract Image<FloatType> evaluate(final Image<T> img, final T alpha);

	/**
	 * Evaluate this function on two numeric types. Argument types can be of any numeric type, but a float must
	 * be returned, so as to avoid underflow and overflow problems on bounded types (e.g. ByeType).
	 * @param alpha1  The first number 
	 * @param alpha2  The second number
	 * @return  The resulting number
	 */
	 public abstract float evaluate(final T t1, final T t2);

	
}
