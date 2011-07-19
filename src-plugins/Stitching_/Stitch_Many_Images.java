import static stitching.CommonFunctions.LIN_BLEND;
import static stitching.CommonFunctions.addHyperLinkListener;
import static stitching.CommonFunctions.colorList;
import static stitching.CommonFunctions.methodListCollection;
import static stitching.CommonFunctions.rgbTypes;

import java.awt.Checkbox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;


public class Stitch_Many_Images implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://bioinformatics.oxfordjournals.org/cgi/content/abstract/btp184";
	public static int stitchingType = 0;
	
	final String[] stichingTypes = new String[]{ 
			"Stitch_grid of images (different files in one directory)",
			"Stitch_all_images in one directory (unknown configuration, test all possiblities)",
			"Stitch_series of images in one file (including the stage coordinates from the microscope)",
			"Stitch_collection of images (defined by a configuration file)" };
	
	final String[] extraExplanations = new String[]{ "Please select the type of stitching you want to perform"	};
	
	final int numStitchingTypes = stichingTypes.length;
	
	@Override
	public void run( final String arg0 ) 
	{
		final GenericDialog gd = new GenericDialog( "Stitching of Many Images (2d/3d)" );
	
		// set the default clicked on ( the one that was used last time )
		final boolean defaultState[] = new boolean[ numStitchingTypes ];
		defaultState[ stitchingType ] = true;
		
		// add the checkbox group
		gd.addCheckboxGroup( numStitchingTypes, 1, stichingTypes, defaultState, extraExplanations );
		
		// make sure only one can be clicked
		final Vector<?> checkBoxes = gd.getCheckboxes();
		for ( final Object o : checkBoxes )
			( (Checkbox)o ).addItemListener( new IntroItemListener( (Checkbox)o, checkBoxes ) );		

		gd.addMessage( "" );
		gd.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL );

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener( text, myURL );

		gd.addMessage( "Please note that the Stitching is based on a publication.\n" +
						"If you use it successfully for your research please be so kind to cite our work:\n" +
						"Preibisch et al., Bioinformatics (2009), 25(11):1463-1465\n" );

		text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener( text, paperURL );
			
		// show the dialog
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		int result = 0;
		
		for ( int i = 0; i < numStitchingTypes; ++i )
			if( gd.getNextBoolean() )
				result = i;
		
		IJ.log( "You selected " + stichingTypes[ result ] );
		
		// store choice for next run
		stitchingType = result;
		
		switch ( result ) 
		{
			case 0:
				stitchGrid();
				break;
				
			case 1:
				stitchDirectory();
				break;

			case 2:
				stitchMultiSeriesFile();
				break;

			case 3:
				stitchCollection();
				break;				

			default:
				break;
		}
	}

	/**
	 * Static variables for the individual dialogs
	 */
	public static String fileNameStatic = "TileConfiguration.txt";
	public static boolean computeOverlapStatic = true;
	public static String handleRGBStatic = colorList[colorList.length - 1];
	public static String rgbOrderStatic = rgbTypes[0];	
	public static String fusionMethodStatic = methodListCollection[LIN_BLEND];
	public static double alphaStatic = 1.5;
	public static double thresholdRStatic = 0.3;
	public static double thresholdDisplacementRelativeStatic = 2.5;
	public static double thresholdDisplacementAbsoluteStatic = 3.5;
	public static boolean previewOnlyStatic = false;	
	
	/**
	 * Manages the dialog for stitching a grid of images
	 */
	public void stitchGrid()
	{
		
	}
	
	/**
	 * Manages the dialog for stitching all files in a directory with unknown configuration
	 */
	public void stitchDirectory()
	{
		
	}

	/**
	 * Manages the dialog for stitching all tiles in a multi-series file
	 */
	public void stitchMultiSeriesFile()
	{
		
	}
	
	/**
	 * Manages the dialog for stitching a collection of images defined by a Tileconfiguration file
	 */
	public void stitchCollection()
	{
		final GenericDialogPlus gd = new GenericDialogPlus("Stitch Image Collection");
		
		//gd.addStringField("Layout file", fileNameStatic, 50);
		gd.addFileField( "Layout file", fileNameStatic, 50 );		
		gd.addCheckbox( "compute_overlap (otherwise use the coordinates given in the layout file)", computeOverlapStatic );
		gd.addChoice( "Channels_for_Registration", colorList, handleRGBStatic );
		gd.addChoice( "rgb_order", rgbTypes, rgbOrderStatic );
		gd.addChoice( "Fusion_Method", methodListCollection, methodListCollection[LIN_BLEND] );
		gd.addNumericField( "Fusion alpha", alphaStatic, 2 );
		gd.addNumericField( "Regression Threshold", thresholdRStatic, 2 );
		gd.addNumericField( "Max/Avg Displacement Threshold", thresholdDisplacementRelativeStatic, 2 );		
		gd.addNumericField( "Absolute Avg Displacement Threshold", thresholdDisplacementAbsoluteStatic, 2 );		
		gd.addCheckbox( "Create_only_Preview", previewOnlyStatic );
		gd.addMessage( "");
		gd.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL );

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		String fileName = gd.getNextString();
		fileNameStatic = fileName;

		boolean computeOverlap = gd.getNextBoolean();
		computeOverlapStatic = computeOverlap;

		String handleRGB = gd.getNextChoice();
		handleRGBStatic = handleRGB;
		
		String rgbOrder = gd.getNextChoice();
		rgbOrderStatic = rgbOrder;
		
		String fusionMethod = gd.getNextChoice();
		fusionMethodStatic = fusionMethod;
		
		double alpha = gd.getNextNumber();
		alphaStatic = alpha;
		
		double thresholdR = gd.getNextNumber();
		thresholdRStatic = thresholdR;
		
		double thresholdDisplacementRelative = gd.getNextNumber();
		thresholdDisplacementRelativeStatic = thresholdDisplacementRelative;
		
		double thresholdDisplacementAbsolute = gd.getNextNumber();
		thresholdDisplacementAbsoluteStatic = thresholdDisplacementAbsolute;
		
		boolean previewOnly = gd.getNextBoolean();
		previewOnlyStatic = previewOnly;	
		
		runStitching();
	}

	/**
	 * Calls the Stitching with the parameters provided
	 */
	protected void runStitching()
	{
		
	}
	
	/**
	 * Makes sure that only one item is selected at a time
	 * 
	 * @author Stephan Preibisch
	 */
	protected class IntroItemListener implements ItemListener
	{
		final Checkbox checkbox;
		final Vector<?> checkboxes;
		
		public IntroItemListener( final Checkbox checkbox, final Vector<?> checkboxes )
		{
			this.checkboxes = checkboxes;
			this.checkbox = checkbox;
		}
		
		@Override
		public void itemStateChanged( final ItemEvent event ) 
		{
			// one cannot deselect, just select another one
			if ( event.getStateChange() == ItemEvent.DESELECTED )
				checkbox.setState( true );
			else if ( event.getStateChange() == ItemEvent.SELECTED )
				for ( final Object o : checkboxes )
					if ( o == checkbox )
						((Checkbox)o).setState( true );
					else
						((Checkbox)o).setState( false );
		}		
	}
}
