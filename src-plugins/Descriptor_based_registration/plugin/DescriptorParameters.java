package plugin;

import ij.gui.Roi;
import mpicbg.models.AbstractModel;

public class DescriptorParameters 
{
	public static int ransacIterations = 1000;
	
	public int dimensionality;
	public double sigma1, sigma2, threshold;
	public boolean lookForMaxima, lookForMinima;
	public AbstractModel<?> model;
	public int numNeighbors;
	public int redundancy;
	public double significance;
	public double ransacThreshold;
	public int channel1, channel2;
	
	// for stack-registration
	public int globalOpt; // 0=all-to-all; 1=all-to-all-withrange; 2=all-to-1; 3=Consecutive
	public int range;	
	
	public Roi roi1, roi2;
	
	public boolean setPointsRois = true;
	public boolean fuse = true;
}
