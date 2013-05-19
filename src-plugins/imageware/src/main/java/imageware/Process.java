package imageware;

/**
 * Class Process.
 *
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

 
public interface Process extends Pointwise {	
	public void 	smoothGaussian(double sigma);
	public void 	smoothGaussian(double sigmaX, double sigmaY, double sigmaZ);
	public void 	max(ImageWare imageware);
	public void 	min(ImageWare imageware);
	public void 	add(ImageWare imageware);
	public void 	multiply(ImageWare imageware);
	public void 	subtract(ImageWare imageware);
	public void 	divide(ImageWare imageware);
}
