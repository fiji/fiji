package mpicbg.spim.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.registration.ViewStructure;

public class SPIMConfiguration 
{	
	// general	
	public String timepointPattern;	
	public int timepoints[];
	public String anglePattern;	
	public int angles[];
	//public String angleString;
	public String inputFilePattern;//spim_TL{i}_Angle\d*\.lsm
	public File file[][];
	public String inputdirectory;
	public String outputdirectory;// = "";
	public String registrationFiledirectory;// = "";
	public String debugLevel;
	public int debugLevelInt = ViewStructure.DEBUG_MAIN;
	public boolean showImageJWindow = false;
	
	// time lapse
	public boolean timeLapseRegistration = false;
	public int referenceTimePoint = 1;
	
	// image factories
	public ContainerFactory imageFactory = new ArrayContainerFactory();
	public ContainerFactory recursiveGaussFactory = new ArrayContainerFactory();
	public ContainerFactory imageFactoryFusion = new ArrayContainerFactory();
	public ContainerFactory outputImageFactory = new ArrayContainerFactory();
	public ContainerFactory entropyFactory = new ArrayContainerFactory();
	public ContainerFactory scaleSpaceFactory = new ArrayContainerFactory();

	// for cached image arrays
	public String tempDir = null;
	
	// for the interpolation
	public OutOfBoundsStrategyFactory<FloatType> strategyFactoryOutput = new OutOfBoundsStrategyValueFactory<FloatType>();
	public InterpolatorFactory<FloatType> interpolatorFactorOutput = new LinearInterpolatorFactory<FloatType>( strategyFactoryOutput );
	
	// outofbounds strategy factories
	public OutOfBoundsStrategyFactory<FloatType> strategyFactoryGauss = new OutOfBoundsStrategyValueFactory<FloatType>();
		
	// segmentation	
	public boolean writeOutputImage = true;
	public boolean showOutputImage = false;
	public boolean useScaleSpace = true;	

	// which fusion weightening
    public boolean useEntropy = false;
    public boolean useGauss = false;
    public boolean useLinearBlening = true;
    
    public boolean paralellFusion = true;    
    public boolean sequentialFusion = false; 
    public int numParalellViews = 1;
    public boolean multipleImageFusion = false;    
    
    public boolean registerOnly = false;
    public boolean displayRegistration = false;
    public boolean readSegmentation = false;
    public boolean writeSegmentation = true;
    public boolean readRegistration = false;
    public boolean writeRegistration = true;   
    
	public boolean overrideImageZStretching = false;
	public double zStretching = 1;
	public int background = 0;
	
	// threshold segmentation
	public float threshold = 0.9f;
	public float fixedThreshold = 0.02f;
	public boolean useFixedThreshold = false; 
	public double circularityFactor = 0.5;
	public int minBlackBorder = 1;
	public int minSize = 10;
	public int maxSize = 15 * 15 * 15;
    public boolean useCenterOfMass = false;
	
	// ScaleSpace Segmentation
	public float minPeakValue = 0.01f;
	public float minInitialPeakValue = minPeakValue/10;
	public float identityRadius = 3f;
	public float maximaTolerance = 0.01f;
	public float imageSigma = 0.5f;
	public float initialSigma = 1.8f;
	public int stepsPerOctave = 4;
	public int steps = 3;
	public boolean detectSmallestStructures = false;
	public int scaleSpaceNumberOfThreads = 0;
	
	// PointDescriptor properties
	public double differenceThreshold = 50;
	public double ratioOfDistance = 10;
	public int neighbors = 3;
    public boolean useAssociatedBeads = false;
    public boolean useRANSAC = true;

    // RANSAC
    public float max_epsilon = 5;
    public float min_inlier_ratio = 0.1f;   
    public int numIterations = 1000;
	
	// output image
	public int scale = 1;
	public int cropOffsetX = 0;
	public int cropOffsetY = 0;
	public int cropOffsetZ = 0;
	public int cropSizeX = 0;
	public int cropSizeY = 0;
	public int cropSizeZ = 0;
	public int numberOfThreads = 0;
	
	// defines the sigma of the volumes injected
    public float sigma = 0.25f;

    // where the injected Gaussian Distributions are cut off
    public int cutOffRadiusGauss = 2;
    
    // the number of histogram bins for computing the entropy
    public int histogramBins = 256;

    // the window Sizes for computing the local entropy
    public int windowSizeX = 19;
    public int windowSizeY = 19;
        
    // linear blending
    public float alpha = 1.5f;
    
    // gauss fusion
    public float fusionSigma1 = 20;//42;
    public float fusionSigma2 = 40;//88;
      
    public int getIndexForTimePoint( final int timepoint )
    {
    	for ( int i = 0; i < timepoints.length; i++ )
    	{
    		if ( timepoints[ i ] == timepoint )
    			return i;
    	}
    	
    	return -1;
    }
    
    public static ArrayList<Integer> parseIntegerString(String integers) throws ConfigurationParserException
    {
    	if ( integers.trim().length() == 0 )
    		return new ArrayList<Integer>();
    	
    	ArrayList<Integer> tmp = null;
    	
		try
		{	    	
	    	tmp = new ArrayList<Integer>();
	    	String[] entries = integers.split(",");
	    	for (String s: entries)
	    	{
	    		s = s.trim();
	    		
	    		if (s.contains("-"))
	    		{
	    			int start = 0, end = 0, step;
	    			start = Integer.parseInt(s.substring(0, s.indexOf("-")));
	    			
	    			if (s.indexOf(":") < 0)
	    			{
	    				end = Integer.parseInt(s.substring(s.indexOf("-") + 1, s.length()));
	    				step = 1;
	    			}
	    			else
	    			{
	    				end = Integer.parseInt(s.substring(s.indexOf("-") + 1, s.indexOf(":")));
	    				step = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.length()));
	    			}
	    			
	    			if (end > start)
	    				for (int i = start; i <= end; i += step)
	    					tmp.add(i);
	    			else
	    				for (int i = start; i >= end; i -= step)
	    					tmp.add(i);	    				
	    		}
	    		else
	    		{
	    			tmp.add(Integer.parseInt(s));
	    		}
	    	}
		}
		catch (Exception e)
		{
			throw new ConfigurationParserException("Cannot parse pattern '" + integers + "'");
		}
		
		return tmp;
    }
    
    public void parseAngles() throws ConfigurationParserException
    {
    	ArrayList<Integer> tmp = parseIntegerString(anglePattern);
    	angles = new int[tmp.size()];
    	
    	for (int i = 0; i < tmp.size(); i++)
    		angles[i] = tmp.get(i);
    	
    	// if there are no angles given take all 
    	if ( angles.length == 0 )
    	{
    		String replaceTL = getReplaceStringTimePoints( inputFilePattern );
    		int numDigitsTL = replaceTL.length() - 2;
    		String replaceAngle = getReplaceStringAngle( inputFilePattern );    		
    		String filePattern = inputFilePattern;
    		
    		filePattern = filePattern.replace( replaceTL, getLeadingZeros( numDigitsTL, timepoints[ 0 ] ) );
    		filePattern = filePattern.replace( replaceAngle, "*" );
    		
    		String filePatternStart = filePattern.substring( 0, filePattern.indexOf( '*' ));
    		String filePatternEnd = filePattern.substring( filePattern.indexOf( '*' ) + 1, filePattern.length() );
    		
     		String[] listing = getDirListing( inputdirectory, filePatternStart, filePatternEnd );
    		angles = new int[ listing.length ];
    		
    		for ( int i = 0; i < listing.length; ++i )
    		{
    			String entry = listing[ i ];    			
    			entry = entry.substring( filePatternStart.length(), entry.length() - filePatternEnd.length() );
    			
    			angles[ i ] = Integer.parseInt( entry );
    		}
    	}
    }
    
	protected String[] getDirListing( final String directory, final String filePatternStart, final String filePatternEnd )
	{
		File dir = new File( directory );
	    	    
	    // It is also possible to filter the list of returned files.
	    // This example does not return any files that start with `.'.
	    FilenameFilter filter = new FilenameFilter() 
	    {
	        public boolean accept(File dir, String name) 
	        {
	            return name.startsWith( filePatternStart) && name.endsWith( filePatternEnd );
	        }
	    };
	    
	    return dir.list(filter);		
	}

    public void parseTimePoints() throws ConfigurationParserException
    {
    	ArrayList<Integer> tmp = parseIntegerString(timepointPattern);

    	timepoints = new int[tmp.size()];
    	
    	for (int i = 0; i < tmp.size(); i++)
    		timepoints[i] = tmp.get(i);
    }
    
    /*
    public String getUniqueNameForTimePoint(int timePoint)
    {
		// find how to parse
		String replaceTL = null, replaceAngle = null;
		int numDigitsTL = 0;
		int numDigitsAngle = 0;
		
		int i1 = inputFilePattern.indexOf("{t");
		int i2 = inputFilePattern.indexOf("t}");
		if (i1 > 0 && i2 > 0)
		{
			replaceTL = "{";
			
			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replaceTL += "t";
			
			replaceTL += "}";
		}

		i1 = inputFilePattern.indexOf("{a");
		i2 = inputFilePattern.indexOf("a}");
		if (i1 > 0 && i2 > 0)
		{
			replaceAngle = "{";
			
			numDigitsAngle = i2 - i1;
			for (int i = 0; i < numDigitsAngle; i++)
				replaceAngle += "a";
			
			replaceAngle += "}";
		}
		
		String fileName = inputFilePattern;
		
		if (replaceTL != null)
			fileName = fileName.replace(replaceTL, getLeadingZeros(numDigitsTL, timePoint));
		
		fileName = fileName.replace(replaceAngle, "");

		// cut the extension
		fileName = fileName.substring(0, fileName.lastIndexOf("."));
		
		// remove the angle string
		fileName = fileName.replace(angleString, "");
		
		return fileName;
    }
    */
    public static String getReplaceStringTimePoints( String inputFilePattern )
    {
    	String replacePattern = null;
    	int numDigitsTL = 0;
    	
		int i1 = inputFilePattern.indexOf("{t");
		int i2 = inputFilePattern.indexOf("t}");
		if (i1 >= 0 && i2 > 0)
		{
			replacePattern = "{";
			
			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replacePattern += "t";
			
			replacePattern += "}";
		}
		
		return replacePattern;
    }
    
    public static String getReplaceStringAngle( String inputFilePattern )
    {
    	String replacePattern = null;
    	int numDigitsTL = 0;
    	
		int i1 = inputFilePattern.indexOf("{a");
		int i2 = inputFilePattern.indexOf("a}");
		if (i1 >= 0 && i2 > 0)
		{
			replacePattern = "{";
			
			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replacePattern += "a";
			
			replacePattern += "}";
		}
		
		return replacePattern;
    }
    
    public void getFileNames() throws ConfigurationParserException
    {
		// find how to parse
		String replaceTL = getReplaceStringTimePoints( inputFilePattern );
		String replaceAngle = getReplaceStringAngle( inputFilePattern );
		
		if ( replaceTL == null )
			replaceTL = "\\";
		
		if ( replaceAngle == null )
			replaceAngle = "\\";
		
		int numDigitsTL = replaceTL.length() - 2;
		int numDigitsAngle = replaceAngle.length() - 2;
		
		if ( numDigitsTL < 0 )
			numDigitsTL = 0;
		
		if ( numDigitsAngle < 0 )
			numDigitsAngle = 0;
		
		/*
		int i1 = inputFilePattern.indexOf("{t");
		int i2 = inputFilePattern.indexOf("t}");
		if (i1 > 0 && i2 > 0)
		{
			replaceTL = "{";
			
			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replaceTL += "t";
			
			replaceTL += "}";
		}

		i1 = inputFilePattern.indexOf("{a");
		i2 = inputFilePattern.indexOf("a}");
		if (i1 > 0 && i2 > 0)
		{
			replaceAngle = "{";
			
			numDigitsAngle = i2 - i1;
			for (int i = 0; i < numDigitsAngle; i++)
				replaceAngle += "a";
			
			replaceAngle += "}";
		}
		*/
		
		parseTimePoints();
		parseAngles();
		
		if (replaceAngle == null)
			throw new ConfigurationParserException("You gave no pattern to substitute the angles in the file name");
		
		if (angles.length < 2)
			IOFunctions.println( "Warning: You gave less than two angles to process: " + anglePattern );
		
		//throw new ConfigurationParserException("You gave less than two angles to process: " + anglePattern);
		
		if (timepoints.length > 1 && replaceTL == null)
			throw new ConfigurationParserException("You gave more than one timepoint but no pattern to replace");				
		
		file = new File[timepoints.length][angles.length];
		
		for (int tp = 0; tp < timepoints.length; tp++)
		{			
			for (int angle = 0; angle < angles.length; angle++)
			{
				String fileName = inputFilePattern;
				if (replaceTL != null)
					fileName = fileName.replace(replaceTL, getLeadingZeros(numDigitsTL, timepoints[tp]));

				fileName = fileName.replace(replaceAngle, getLeadingZeros(numDigitsAngle, angles[angle]));
				
				file[tp][angle] = new File(inputdirectory, fileName);
			}
		}
    }
    
    public File[] getFileName(int timepoint)
    {
		// find how to parse
		String replaceTL = null, replaceAngle = null;
		int numDigitsTL = 0;
		int numDigitsAngle = 0;
		
		int i1 = inputFilePattern.indexOf("{t");
		int i2 = inputFilePattern.indexOf("t}");
		if (i1 > 0 && i2 > 0)
		{
			replaceTL = "{";
			
			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replaceTL += "t";
			
			replaceTL += "}";
		}

		i1 = inputFilePattern.indexOf("{a");
		i2 = inputFilePattern.indexOf("a}");
		if (i1 > 0 && i2 > 0)
		{
			replaceAngle = "{";
			
			numDigitsAngle = i2 - i1;
			for (int i = 0; i < numDigitsAngle; i++)
				replaceAngle += "a";
			
			replaceAngle += "}";
		}
						
		File[] file = new File[angles.length];
		
		for (int angle = 0; angle < angles.length; angle++)
		{
			String fileName = inputFilePattern;
			if (replaceTL != null)
				fileName = fileName.replace(replaceTL, getLeadingZeros(numDigitsTL, timepoint));

			fileName = fileName.replace(replaceAngle, getLeadingZeros(numDigitsAngle, angles[angle]));
			
			file[angle] = new File(inputdirectory, fileName);
		}
		
		return file;
    }
    
	private static String getLeadingZeros(int zeros, int number)
	{
		String output = "" + number;
		
		while (output.length() < zeros)
			output = "0" + output;
		
		return output;
	}
    
    public void printProperties()
    {    	
    	IOFunctions.println("timepointPattern: " + timepointPattern);   	
    	if (timepoints != null)
    	{
    		System.out.print("Time Points: ");
    		for (int tp : timepoints)
    			System.out.print(tp + " ");
    		
    		IOFunctions.println();
    	}

    	IOFunctions.println("anglePattern: " + anglePattern);   	
    	if (angles != null)
    	{
    		System.out.print("Angles: ");
    		for (int angle : angles)
    			System.out.print(angle + " ");
    		
    		IOFunctions.println();
    	}
    	
    	//IOFunctions.println("angleString: " + angleString);   	

    	IOFunctions.println("inputFilePattern: " + inputFilePattern);
    	if (file != null)
    		for (int x = 0; x < file.length; x++)
    			for (int y = 0; y < file[x].length; y++)
    				IOFunctions.println("File["+x+"]["+y+"] = " + file[x][y]);
    	
    	IOFunctions.println("inputdirectory: " + inputdirectory);
    	IOFunctions.println("outputdirectory: " + outputdirectory);
    	IOFunctions.println("registrationFiledirectory: " + registrationFiledirectory);
    	IOFunctions.println("debugLevel: " + debugLevel);
    	IOFunctions.println("showImageJWindow: " + showImageJWindow);
    
    	IOFunctions.println("timeLapseRegistration: " + timeLapseRegistration);
    	IOFunctions.println("referenceTimePoint: " + referenceTimePoint);
    	
    	// image factories
    	imageFactory.printProperties();
    	recursiveGaussFactory.printProperties();
    	imageFactoryFusion.printProperties();
    	outputImageFactory.printProperties();
    	entropyFactory.printProperties();
    	scaleSpaceFactory.printProperties();

    	// for cached image arrays
    	IOFunctions.println("tempDir: " + tempDir);
    	
    	// for the interpolation
    	//interpolatorFactorOutput.printProperties();
    	strategyFactoryOutput.printProperties();
    	
    	// outofbounds strategy factories
    	strategyFactoryGauss.printProperties();
    	
    	IOFunctions.println("writeOutputImage: " + writeOutputImage); 
    	IOFunctions.println("showOutputImage: " + showOutputImage); 
    	IOFunctions.println("useScaleSpace: " + useScaleSpace);
    	IOFunctions.println("useEntropy: " + useEntropy);
    	IOFunctions.println("useGauss: " + useGauss);
    	IOFunctions.println("useLinearBlening: " + useLinearBlening);
        
    	IOFunctions.println("paralellFusion: " + paralellFusion);    
    	IOFunctions.println("sequentialFusion: " + sequentialFusion);
    	IOFunctions.println("multipleImageFusion: " + multipleImageFusion);
    	
    	IOFunctions.println("registerOnly: " + registerOnly);
    	IOFunctions.println("readSegmentation: " + readSegmentation);
    	IOFunctions.println("writeSegmentation: " + writeSegmentation);
    	IOFunctions.println("readRegistration: " + readRegistration);
    	IOFunctions.println("writeRegistration: " + writeRegistration);   
        
    	IOFunctions.println("zStretching: " + zStretching);
    	IOFunctions.println("background: " + background);
    	
    	// threshold segmentation
    	IOFunctions.println("threshold: " + threshold);
    	IOFunctions.println("fixed threshold: " + fixedThreshold);
    	IOFunctions.println("useFixedThreshold: " + useFixedThreshold);
    	IOFunctions.println("minBlackBorder: " + minBlackBorder);
    	IOFunctions.println("minSize: " + minSize);
    	IOFunctions.println("maxSize: " + maxSize);
    	IOFunctions.println("useCenterOfMass: " + useCenterOfMass);
    	
    	// ScaleSpace Segmentation
    	IOFunctions.println("minPeakValue: " + minPeakValue);
    	IOFunctions.println("minInitialPeakValue: " + minInitialPeakValue);
    	IOFunctions.println("identityRadius: " + identityRadius);
    	IOFunctions.println("maximaTolerance: " + maximaTolerance);
    	IOFunctions.println("imageSigma: " + imageSigma);
    	IOFunctions.println("initialSigma: " + initialSigma);
    	IOFunctions.println("stepsPerOctave: " + stepsPerOctave);
    	IOFunctions.println("steps: " + steps);
    	
    	// PointDescriptor properties
    	IOFunctions.println("differenceThreshold: " + differenceThreshold);
    	IOFunctions.println("ratioOfDistance: " + ratioOfDistance);
    	IOFunctions.println("neighbors: " + neighbors);
    	IOFunctions.println("useAssociatedBeads: " + useAssociatedBeads);
    	IOFunctions.println("useRANSAC: " + useRANSAC);

        // RANSAC
    	IOFunctions.println("max_epsilon: " + max_epsilon);
    	IOFunctions.println("min_inlier_ratio: " + min_inlier_ratio);
    	IOFunctions.println("numIterations: " + numIterations);
    	
    	// output image
    	IOFunctions.println("scale: " + scale);
    	IOFunctions.println("cropOffsetX: " + cropOffsetX);
    	IOFunctions.println("cropOffsetY: " + cropOffsetY);
    	IOFunctions.println("cropOffsetZ: " + cropOffsetZ);
    	IOFunctions.println("cropSizeX: " + cropSizeX);
    	IOFunctions.println("cropSizeY: " + cropSizeY);
    	IOFunctions.println("cropSizeZ: " + cropSizeZ);
    	IOFunctions.println("numberOfThreads: " + numberOfThreads);
    	
    	// defines the sigma of the volumes injected
    	IOFunctions.println("sigma: " + sigma);

        // where the injected Gaussian Distributions are cut off
    	IOFunctions.println("cutOffRadiusGauss: " + cutOffRadiusGauss);
        
        // the number of histogram bins for computing the entropy
    	IOFunctions.println("histogramBins: " + histogramBins);

        // the window Sizes for computing the local entropy
    	IOFunctions.println("windowSizeX: " + windowSizeX);
    	IOFunctions.println("windowSizeY: " + windowSizeY);
            
        // linear blending
    	IOFunctions.println("alpha: " + alpha);
        
        // gauss fusion
    	IOFunctions.println("fusionSigma1: " + fusionSigma1);
    	IOFunctions.println("fusionSigma2: " + fusionSigma2);
    	
    }
}
