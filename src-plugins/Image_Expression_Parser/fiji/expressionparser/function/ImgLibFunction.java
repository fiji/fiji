package fiji.expressionparser.function;

import org.nfunk.jep.ParseException;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.type.numeric.RealType;

public interface ImgLibFunction <T extends RealType<T>> {

	/**
	 * Return a String describing this operator. Example: "Addition of two operands",
	 * "Gaussian convolution", "Element-wise cosine", ...
	 */
	public abstract String toString();
	
	
	/**
	 * Return a String containing the function name, that is, how this function 
	 * must be represented in an expression to be called. Examples: "+", "gauss",
	 * "cos", ...
	 */
	public abstract String getFunctionString();
	
	/**
	 * Returns a documentation string that documents in enough details what the function
	 * does, and how. The String can use html syntax in it for formatting. 
	 * <p>
	 * Example:
	 * <p>
	 * <h3>Gaussian convolution</h3>
	 * This function implements the isotropic gaussian convolution, as coded 
	 * in ImgLib, effectively implementing a gaussian filter.
	 * Syntax:
 	 * <br> 
	 * <code> >> gauss(A, sigma)</code> <br>
	 * with A an image and sigma a number. Sigma is the standard deviation
	 * of the gaussian kernel applied to image A.
	 * <br>
	 * Input image is converted to <i>FloatType</i> then convolved. If the source image is a 3D image, 
	 * the convolution will be made in 3D as well.
	 */
	public abstract String getDocumentationString();
	
	/**
	 * Evaluate this function on two images, and return result as an image.
	 * @param img1  The first image 
	 * @param img2  The second image 
	 * @return  The resulting image
	 */
	public abstract Image<FloatType> evaluate(final Image<T> img1, final Image<T> img2) throws ParseException;

	/**
	 * Singleton expansion. Evaluate this function on an image and an image that would be of same 
	 * dimension but with all element being the number passed in argument.
	 * @param img  The image 
	 * @param alpha  The number to do singleton expansion on 
	 * @return  The resulting image 
	 */
	public abstract Image<FloatType> evaluate(final Image<T> img, final T alpha) throws ParseException;

	/**
	 * Evaluate this function on two numeric types. Argument types can be of any numeric type, but a float must
	 * be returned, so as to avoid underflow and overflow problems on bounded types (e.g. ByeType).
	 * @param alpha1  The first number 
	 * @param alpha2  The second number
	 * @return  The resulting number
	 */
	 public abstract float evaluate(final T t1, final T t2) throws ParseException;

	
}
