package plugin;

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
	
	public double sigma, threshold;
	public boolean lookForMaxima, lookForMinima;
	public AbstractModel<?> model;
	public int numNeighbors;
	public int redundancy;
	public double significance;
	public double ransacThreshold;
	
}
