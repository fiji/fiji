package mpicbg.stitching;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.ArrayContainerFactory;

public class StitchingParameters 
{
	/**
	 * If we cannot wrap, which factory do we use for computing the phase correlation
	 */
	public static ContainerFactory phaseCorrelationFactory = new ArrayContainerFactory();
	
	/**
	 * If you want to force that the {@link ContainerFactory} above is always used set this to true
	 */
	public static boolean alwaysCopy = false;
	
	public int dimensionality;
	public int fusionMethod;
	public String fusedName;
	public int checkPeaks;
	public boolean computeOverlap, subpixelAccuracy;
	public boolean invertX, invertY;
	public double xOffset;
	public double yOffset;
	public double zOffset;

	public boolean virtual = false;
	public int channel1;
	public int channel2;

	public int timeSelect;
	
	public int cpuMemChoice = 0;
	// 0 == fuse&display, 1 == writeToDisk
	public int outputVariant = 0;
	public String outputDirectory = null;
	
	public double regThreshold = -2;
	public double relativeThreshold = 2.5;
	public double absoluteThreshold = 3.5;
	
	//added by John Lapage: allows storage of a sequential comparison range
	public boolean sequential = false;
	public int seqRange = 1;
}
