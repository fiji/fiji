package mpicbg.spim.postprocessing.deconvolution2;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

public interface Deconvolver 
{	
	public String getName();
	public double getAvg();
	public LRInput getData();
	
	public Image<FloatType> getPsi();
	public void runIteration();
}
