package plugin;

import static stitching.CommonFunctions.addHyperLinkListener;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.io.TextFileAccess;
import mpicbg.stitching.ImagePlusTimePoint;
import mpicbg.stitching.StitchingParameters;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;
import stitching.CommonFunctions;

public class Stitching_Grid implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	
	public static int defaultGridChoice1 = 0;
	public static int defaultGridChoice2 = 0;

	public static int defaultGridSizeX = 2, defaultGridSizeY = 3;
	public static double defaultOverlap = 20;
	
	//TODO: change to ""
	public static String defaultDirectory = "/Volumes/Macintosh HD 2/Truman/standard";
	
	//TODO: change back to "tile{iii}.tif"
	public static String defaultFileNames = "{ii}.tif";
	public static String defaultTileConfiguration = "TileConfiguration.txt";
	public static boolean defaultComputeOverlap = true;
	public static boolean defaultSubpixelAccuracy = true;
	public static boolean writeOnlyTileConfStatic = false;
	
	//TODO: change to 1
	public static int defaultStartI = 73;
	public static int defaultStartX = 1;
	public static int defaultStartY = 1;
	public static int defaultFusionMethod = 0;
	public static double defaultR = 0.3;
	public static double defaultRegressionThreshold = 0.3;
	public static double defaultDisplacementThresholdRelative = 2.5;		
	public static double defaultDisplacementThresholdAbsolute = 3.5;		
	public static boolean defaultOnlyPreview = false;
	public static int defaultMemorySpeedChoice = 0;
	
	
	@Override
	public void run( String arg0 ) 
	{
		final GridType grid = new GridType();
		
		final int gridType = grid.getType();
		final int gridOrder = grid.getOrder();
		
		if ( gridType == -1 || gridOrder == -1 )
			return;

		final GenericDialogPlus gd = new GenericDialogPlus( "Grid stitching: " + GridType.choose1[ gridType ] + ", " + GridType.choose2[ gridType ][ gridOrder ] );

		gd.addNumericField( "Grid_size_x", defaultGridSizeX, 0 );
		gd.addNumericField( "Grid_size_y", defaultGridSizeY, 0 );
		
		gd.addSlider( "Tile_overlap [%]", 0, 100, defaultOverlap );
		
		// row-by-row, column-by-column or snake
		// needs the same questions
		if ( grid.getType() < 4 )
		{
			gd.addNumericField( "First_file_index_i", defaultStartI, 0 );
		}
		else
		{
			gd.addNumericField( "First_file_index_x", defaultStartX, 0 );
			gd.addNumericField( "First_file_index_y", defaultStartY, 0 );
		}

		gd.addDirectoryField( "Directory", defaultDirectory, 50 );
		gd.addStringField( "File_names for tiles", defaultFileNames, 50 );
		gd.addStringField( "Output_textfile_name", defaultTileConfiguration, 50 );
				
		gd.addChoice( "Fusion_method", CommonFunctions.fusionMethodListGrid, CommonFunctions.fusionMethodListGrid[ defaultFusionMethod ] );
		gd.addNumericField( "Regression_threshold", defaultRegressionThreshold, 2 );
		gd.addNumericField( "Max/avg_displacement_threshold", defaultDisplacementThresholdRelative, 2 );		
		gd.addNumericField( "Absolute_displacement_threshold", defaultDisplacementThresholdAbsolute, 2 );		
		gd.addCheckbox( "Compute_overlap (otherwise use approximate grid coordinates)", defaultComputeOverlap );
		gd.addCheckbox( "Subpixel_accuracy", defaultSubpixelAccuracy );
		gd.addChoice( "Computation_parameters", CommonFunctions.cpuMemSelect, CommonFunctions.cpuMemSelect[ defaultMemorySpeedChoice ] );
		gd.addMessage("");
		gd.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		// the general stitching parameters
		final StitchingParameters params = new StitchingParameters();
				
		final int gridSizeX = defaultGridSizeX = (int)Math.round(gd.getNextNumber());
		final int gridSizeY = defaultGridSizeY = (int)Math.round(gd.getNextNumber());
		final double overlap = defaultOverlap = gd.getNextNumber()/100.0;

		int startI = 0, startX = 0, startY = 0;
		
		// row-by-row, column-by-column or snake
		// needs the same questions
		if ( grid.getType() < 4 )
		{
			startI = defaultStartI = (int)Math.round(gd.getNextNumber());
		}
		else // position
		{
			startX = defaultStartI = (int)Math.round(gd.getNextNumber());
			startY = defaultStartI = (int)Math.round(gd.getNextNumber());			
		}
		
		String directory = defaultDirectory = gd.getNextString();
		final String filenames = defaultFileNames = gd.getNextString();
		final String outputFile = defaultTileConfiguration = gd.getNextString();
		params.fusionMethod = defaultFusionMethod = gd.getNextChoiceIndex();
		params.regThreshold = defaultRegressionThreshold = gd.getNextNumber();
		params.relativeThreshold = defaultDisplacementThresholdRelative = gd.getNextNumber();		
		params.absoluteThreshold = defaultDisplacementThresholdAbsolute = gd.getNextNumber();
		params.computeOverlap = defaultComputeOverlap = gd.getNextBoolean();
		params.subpixelAccuracy = defaultSubpixelAccuracy = gd.getNextBoolean();
		params.cpuMemChoice = defaultMemorySpeedChoice = gd.getNextChoiceIndex();
		
		// define the parsing of filenames
		// find how to parse
		String replaceX = "{", replaceY = "{", replaceI = "{";
		int numXValues = 0, numYValues = 0, numIValues = 0;

		if ( grid.getType() < 4 )
		{
			int i1 = filenames.indexOf("{i");
			int i2 = filenames.indexOf("i}");
			if (i1 >= 0 && i2 > 0)
			{
				numIValues = i2 - i1;
				for (int i = 0; i < numIValues; i++)
					replaceI += "i";
				replaceI += "}";
			}
			else
			{
				replaceI = "\\\\\\\\";
			}			
		}
		else
		{
			int x1 = filenames.indexOf("{x");
			int x2 = filenames.indexOf("x}");
			if (x1 >= 0 && x2 > 0)
			{
				numXValues = x2 - x1;
				for (int i = 0; i < numXValues; i++)
					replaceX += "x";
				replaceX += "}";
			}
			else
			{
				replaceX = "\\\\\\\\";
			}

			int y1 = filenames.indexOf("{y");
			int y2 = filenames.indexOf("y}");
			if (y1 >= 0 && y2 > 0)
			{
				numYValues = y2 - y1;
				for (int i = 0; i < numYValues; i++)
					replaceY += "y";
				replaceY += "}";
			}
			else
			{
				replaceY = "\\\\\\\\";
			}			
		}
		
		// for reading in writing the tileconfiguration file
		directory = directory.replace('\\', '/');
		directory = directory.trim();
		if (directory.length() > 0 && !directory.endsWith("/"))
			directory = directory + "/";
		
		// determine the layout
		final ImageCollectionElement[][] gridLayout = new ImageCollectionElement[ gridSizeX ][ gridSizeY ];
		
		// all snakes, row, columns, whatever
		if ( grid.getType() < 4 )
		{
			// the current position[x, y] 
			final int[] position = new int[ 2 ];
			
			// we have gridSizeX * gridSizeY tiles
			for ( int i = 0; i < gridSizeX * gridSizeY; ++i )
			{
				// get the vector where to move
				getPosition( position, i, gridType, gridOrder, gridSizeX, gridSizeY );

				// get the filename
            	final String file = filenames.replace( replaceI, getLeadingZeros( numIValues, i + startI ) );
            	gridLayout[ position[ 0 ] ][ position [ 1 ] ] = new ImageCollectionElement( new File( directory, file ), i ); 
			}
		}
		else // fixed positions
		{
			// an index for the element
			int i = 0;
			
			for ( int y = 0; y < gridSizeY; ++y )
				for ( int x = 0; x < gridSizeX; ++x )
				{
					final String file = filenames.replace( replaceX, getLeadingZeros( numXValues, x + startX ) ).replace( replaceY, getLeadingZeros( numYValues, y + startY ) );
	            	gridLayout[ x ][ y ] = new ImageCollectionElement( new File( directory, file ), i++ );
				}
		}

		// based on the minimum size we will compute the initial arrangement
		int minWidth = Integer.MAX_VALUE;
		int minHeight = Integer.MAX_VALUE;
		int minDepth = Integer.MAX_VALUE;
		
		int numChannels = -1;
		int numTimePoints = -1;
		
		boolean is2d = false;
		boolean is3d = false;
		
		// open all images and test them, collect information
		for ( int y = 0; y < gridSizeY; ++y )
			for ( int x = 0; x < gridSizeX; ++x )
			{
				IJ.log( "Loading (" + x + ", " + y + "): " + gridLayout[ x ][ y ].file.getAbsolutePath() + " ... " );
				
				long time = System.currentTimeMillis();
				final ImagePlus imp = gridLayout[ x ][ y ].open();
				time = System.currentTimeMillis() - time;
				
				if ( imp == null )
					return;
				
				int lastNumChannels = numChannels;
				int lastNumTimePoints = numTimePoints;
				numChannels = imp.getNChannels();
				numTimePoints = imp.getNFrames();
				
				if ( imp.getNSlices() > 1 )
				{
					IJ.log( "" + imp.getWidth() + "x" + imp.getHeight() + "x" + imp.getNSlices() + "px, channels=" + numChannels + ", timepoints=" + numTimePoints + " (" + time + " ms)" );
					is3d = true;					
				}
				else
				{
					IJ.log( "" + imp.getWidth() + "x" + imp.getHeight() + "px, channels=" + numChannels + ", timepoints=" + numTimePoints + " (" + time + " ms)" );
					is2d = true;
				}
				
				// test validity of images
				if ( is2d && is3d )
				{
					IJ.log( "Some images are 2d, some are 3d ... cannot proceed" );
					return;
				}
				
				if ( ( lastNumChannels != numChannels ) && x != 0 && y != 0 )
				{
					IJ.log( "Number of channels per image changes ... cannot proceed" );
					return;					
				}

				if ( ( lastNumTimePoints != numTimePoints ) && x != 0 && y != 0 )
				{
					IJ.log( "Number of timepoints per image changes ... cannot proceed" );
					return;					
				}

				if ( imp.getWidth() < minWidth )
					minWidth = imp.getWidth();

				if ( imp.getHeight() < minHeight )
					minHeight = imp.getHeight();
				
				if ( imp.getNSlices() < minDepth )
					minDepth = imp.getNSlices();
			}
		
		// the dimensionality of each image that will be correlated (might still have more channels or timepoints)
		final int dimensionality;
		
		if ( is2d )
		{
			IJ.log( "Reference dimensions (smallest of all images) for grid layout: " + minWidth + " x " + minHeight + "px." );
			dimensionality = 2;
		}
		else
		{
			IJ.log( "Reference dimensions (smallest of all images) for grid layout: " + minWidth + " x " + minHeight + " x " + minDepth + "px." );
			dimensionality = 3;
		}
		
		params.dimensionality = dimensionality;
		
		// now get the approximate coordinates for each element
		// that is easiest done incrementally
		int xoffset = 0, yoffset = 0, zoffset = 0;
    	
    	// write the tileconfiguration
		final String fileName = directory + outputFile;
		final PrintWriter out = TextFileAccess.openFileWrite( fileName );

		// an ArrayList containing all the ImageCollectionElements
		final ArrayList< ImageCollectionElement > elements = new ArrayList< ImageCollectionElement >();
		
    	for ( int y = 0; y < gridSizeY; y++ )
    	{
        	if ( y == 0 )
        		yoffset = 0;
        	else 
        		yoffset += (int)( minWidth * ( 1 - overlap ) );

        	for ( int x = 0; x < gridSizeX; x++ )
            {
        		final ImageCollectionElement element = gridLayout[ x ][ y ];
        		
            	if ( x == 0 && y == 0 && out != null )
            	{
        			out.println( "# Define the number of dimensions we are working on" );
        	        out.println( "dim = " + dimensionality );
        	        out.println( "" );
        	        out.println( "# Define the image coordinates" );
            	}
            	
            	if ( x == 0 && y == 0 )
            		xoffset = yoffset = zoffset = 0;
            	
            	if ( x == 0 )
            		xoffset = 0;
            	else 
            		xoffset += (int)( minWidth * ( 1 - overlap ) );
            	
            	if ( out != null )
            	{
            		if ( dimensionality == 3 )
            			out.println( element.getFile().getAbsolutePath() + "; ; (" + xoffset + ", " + yoffset + ", " + zoffset + ")");
            		else
            			out.println( element.getFile().getAbsolutePath() + "; ; (" + xoffset + ", " + yoffset + ")");
            	}            	
            	
            	element.setDimensionality( dimensionality );
            	
            	if ( dimensionality == 3 )
            	{
            		element.setModel( new TranslationModel3D() );
            		element.setOffset( new float[]{ xoffset, yoffset, zoffset } );
            	}
            	else
            	{
            		element.setModel( new TranslationModel2D() );
            		element.setOffset( new float[]{ xoffset, yoffset } );
            	}
            	
            	elements.add( element );
            }
    	}
    	
    	out.close();
    	
    	// call the final stitiching
    	Stitching_Collection.stitchCollection( elements, params );
	}
	
	// current snake directions ( if necessary )
	// they need a global state
	int snakeDirectionX = 0; 
	int snakeDirectionY = 0; 

	protected void getPosition( final int[] currentPosition, final int i, final int gridType, final int gridOrder, final int sizeX, final int sizeY )
	{
		// gridType: "Row-by-row", "Column-by-column", "Snake by rows", "Snake by columns", "Fixed position"
		// gridOrder:
		//		choose2[ 0 ] = new String[]{ "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
		//		choose2[ 1 ] = new String[]{ "Down & Right", "Down & Left", "Up & Right", "Up & Left" };
		//		choose2[ 2 ] = new String[]{ "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
		//		choose2[ 3 ] = new String[]{ "Down & Right", "Down & Left", "Up & Right", "Up & Left" };
			
		// init the position
		if ( i == 0 )
		{
			if ( gridOrder == 0 || gridOrder == 2 )
				currentPosition[ 0 ] = 0;
			else
				currentPosition[ 0 ] = sizeX - 1;
			
			if ( gridOrder == 0 || gridOrder == 1 )
				currentPosition[ 1 ] = 0;
			else
				currentPosition[ 1 ] = sizeY - 1;
			
			// it is a snake
			if ( gridType == 2 || gridType == 3 )
			{
				// starting with right
				if ( gridOrder == 0 || gridOrder == 2 )
					snakeDirectionX = 1;
				else // starting with left
					snakeDirectionX = -1;
				
				// starting with down
				if ( gridOrder == 0 || gridOrder == 1 )
					snakeDirectionY = 1;
				else // starting with up
					snakeDirectionY = -1;
			}
		}
		else // a move is required
		{
			// row-by-row, "Right & Down", "Left & Down", "Right & Up", "Left & Up"
			if ( gridType == 0 )
			{
				// 0="Right & Down", 2="Right & Up"
				if ( gridOrder == 0 || gridOrder == 2 )
				{
					if ( currentPosition[ 0 ] < sizeX - 1 )
					{
						// just move one more right
						++currentPosition[ 0 ];
					}
					else
					{
						// we have to change rows
						if ( gridOrder == 0 )
							++currentPosition[ 1 ];
						else
							--currentPosition[ 1 ];
						
						// row-by-row going right, so only set position to 0
						currentPosition[ 0 ] = 0;
					}
				}
				else // 1="Left & Down", 3="Left & Up"
				{
					if ( currentPosition[ 0 ] > 0 )
					{
						// just move one more left
						--currentPosition[ 0 ];
					}
					else
					{
						// we have to change rows
						if ( gridOrder == 1 )
							++currentPosition[ 1 ];
						else
							--currentPosition[ 1 ];
						
						// row-by-row going left, so only set position to 0
						currentPosition[ 0 ] = sizeX - 1;
					}					
				}
			}
			else if ( gridType == 1 ) // col-by-col, "Down & Right", "Down & Left", "Up & Right", "Up & Left"
			{
				// 0="Down & Right", 1="Down & Left"
				if ( gridOrder == 0 || gridOrder == 1 )
				{
					if ( currentPosition[ 1 ] < sizeY - 1 )
					{
						// just move one down
						++currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						if ( gridOrder == 0 )
							++currentPosition[ 0 ];
						else
							--currentPosition[ 0 ];
						
						// column-by-column going down, so position = 0
						currentPosition[ 1 ] = 0;
					}
				}
				else // 2="Up & Right", 3="Up & Left"
				{
					if ( currentPosition[ 1 ] > 0 )
					{
						// just move one up
						--currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						if ( gridOrder == 2 )
							++currentPosition[ 0 ];
						else
							--currentPosition[ 0 ];
						
						// column-by-column going up, so position = sizeY - 1
						currentPosition[ 1 ] = sizeY - 1;
					}
				}
			}
			else if ( gridType == 2 ) // "Snake by rows"
			{
				// currently going right
				if ( snakeDirectionX > 0 )
				{
					if ( currentPosition[ 0 ] < sizeX - 1 )
					{
						// just move one more right
						++currentPosition[ 0 ];
					}
					else
					{
						// just we have to change rows
						currentPosition[ 1 ] += snakeDirectionY;
						
						// and change the direction of the snake in x
						snakeDirectionX *= -1;
					}
				}
				else
				{
					// currently going left
					if ( currentPosition[ 0 ] > 0 )
					{
						// just move one more left
						--currentPosition[ 0 ];
						return;
					}
					else
					{
						// just we have to change rows
						currentPosition[ 1 ] += snakeDirectionY;
						
						// and change the direction of the snake in x
						snakeDirectionX *= -1;
					}
				}
			}
			else if ( gridType == 3 ) // "Snake by columns" 
			{
				// currently going down
				if ( snakeDirectionY > 0 )
				{
					if ( currentPosition[ 1 ] < sizeY - 1 )
					{
						// just move one more down
						++currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						currentPosition[ 0 ] += snakeDirectionX;
						
						// and change the direction of the snake in y
						snakeDirectionY *= -1;
					}
				}
				else
				{
					// currently going up
					if ( currentPosition[ 1 ] > 0 )
					{
						// just move one more up
						--currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						currentPosition[ 0 ] += snakeDirectionX;
						
						// and change the direction of the snake in y
						snakeDirectionY *= -1;
					}
				}
			}
		}
	}
	
	public static String getLeadingZeros( final int zeros, final int number )
	{
		String output = "" + number;
		
		while (output.length() < zeros)
			output = "0" + output;
		
		return output;
	}
	
	public static ImagePlus openHyperStack()
	{
		return null;
	}
}
 