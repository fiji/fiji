package plugin;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AbstractModel;

public class DescriptorParameters 
{
	/*
	defaultDetectionBrightness = gd.getNextChoiceIndex();
	defaultDetectionRadius = gd.getNextChoiceIndex();
	defaultDetectionType = gd.getNextChoiceIndex();
	defaultTransformationModel = gd.getNextChoiceIndex();
	defaultNumNeighbors = (int)Math.round( gd.getNextNumber() );
	defaultRedundancy = (int)Math.round( gd.getNextNumber() );
	defaultSignificance = gd.getNextNumber();
	defaultRansacThreshold = gd.getNextNumber();
	*/
	
	public int dimensionality;
	public double sigma1, sigma2, threshold;
	public boolean lookForMaxima, lookForMinima;
	public AbstractModel<?> model;
	public int numNeighbors;
	public int redundancy;
	public double significance;
	public double ransacThreshold;
	public int channel1, channel2;
	
	public boolean setPointsRois = true;
	public boolean fuse = true;
	
	
	// if the interactive dog is run, this one exists already
	public Image<FloatType> img1 = null;
}
