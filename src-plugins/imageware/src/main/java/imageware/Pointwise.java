package imageware;

import ij.ImageStack;

/**
 * Class Pointwise.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */


public interface Pointwise extends Access {	
	public void 	fillConstant(double value);
	public void 	fillRamp();
	public void 	fillGaussianNoise(double amplitude);
	public void 	fillUniformNoise(double amplitude);
	public void 	fillSaltPepper(double amplitudeSalt, double amplitudePepper, double percentageSalt, double percentagePepper);
	
	public ImageStack buildImageStack();
	
	public void		invert();
	public void		negate();
	public void		rescale();
	public void		clip();
	public void		clip(double minLevel, double maxLevel);
	public void		rescale(double minLevel, double maxLevel);
	public void 	rescaleCenter(double minLevel, double maxLevel);
	public void 	abs();
	public void 	log();
	public void 	exp();
	public void 	sqrt();
	public void 	sqr();
	public void 	pow(double a);
	public void 	add(double constant);
	public void 	multiply(double constant);
	public void 	subtract(double constant);
	public void 	divide(double constant);
	public void		threshold(double thresholdValue);
	public void		threshold(double thresholdValue, double minLevel, double maxLevel);
	public void		thresholdHard(double thresholdValue);
	public void		thresholdSoft(double thresholdValue);
	public void 	addGaussianNoise(double amplitude);
	public void 	addUniformNoise(double amplitude);
	public void 	addSaltPepper(double amplitudeSalt, double amplitudePepper, double percentageSalt, double percentagePepper);
}
