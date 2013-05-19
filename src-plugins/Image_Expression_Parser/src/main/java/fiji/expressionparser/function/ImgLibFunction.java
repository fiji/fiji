package fiji.expressionparser.function;

import org.nfunk.jep.function.PostfixMathCommandI;

import mpicbg.imglib.type.numeric.RealType;

public interface ImgLibFunction <T extends RealType<T>> extends PostfixMathCommandI {

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
	
		
}
