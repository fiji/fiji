package fiji.plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import mpicbg.imglib.util.Util;
import mpicbg.models.AffineModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.io.TextFileAccess;
import mpicbg.spim.registration.ViewStructure;

public class Apply_External_Transformation implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";
	
	@Override
	public void run(String arg0) 
	{
		// output to IJ.log
		IOFunctions.printIJLog = true;

		final SPIMConfiguration conf = getParameters();
		
		if ( conf == null )
			return;
		
	}
	
	public static int defaultApplyTo = 0;
	public static int defaultHowToProvide = 0;
	public static String[] howToProvideAffine = new String[] { "As one string (e.g. from 3d-viewer)", "As individual entries" };
	public static double m00 = 1, m01 = 0, m02 = 0, m03 = 0;
	public static double m10 = 0, m11 = 1, m12 = 0, m13 = 0;
	public static double m20 = 0, m21 = 0, m22 = 1, m23 = 0;

	protected SPIMConfiguration getParameters() 
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Apply external transformation" );
		
		gd.addDirectoryField( "SPIM_data_directory", Bead_Registration.spimDataDirectory );
		gd.addStringField( "Pattern_of_SPIM files", Bead_Registration.fileNamePattern, 25 );
		gd.addStringField( "Timepoints_to_process", Bead_Registration.timepoints );
		gd.addStringField( "Angles to process", Bead_Registration.angles );
		
		gd.addMessage("");

		gd.addChoice( "How_to_provide_affine_matrix", howToProvideAffine, howToProvideAffine[ defaultHowToProvide ] );
		
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener(text, myURL);
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;

		Bead_Registration.spimDataDirectory = gd.getNextString();
		Bead_Registration.fileNamePatternMC = gd.getNextString();
		Bead_Registration.timepoints = gd.getNextString();
		Bead_Registration.angles = gd.getNextString();

		int numChannels = 0;
		ArrayList<Integer> channels;
				
		numChannels = 1;
		channels = new ArrayList<Integer>();
		channels.add( 0 );
		
		defaultHowToProvide = gd.getNextChoiceIndex();
		
		// create the configuration object
		final SPIMConfiguration conf = new SPIMConfiguration();

		conf.timepointPattern = Bead_Registration.timepoints;
		conf.anglePattern = Bead_Registration.angles;
		conf.channelPattern = "";
		conf.channelsToRegister = "";
		conf.channelsToFuse = "";			
		conf.inputFilePattern = Bead_Registration.fileNamePattern;
		conf.inputdirectory = Bead_Registration.spimDataDirectory;
		
		// get filenames and so on...
		if ( !Bead_Registration.init( conf ) )
			return null;
		
		// test which registration files are there for each channel
		// file = new File[ timepoints.length ][ channels.length ][ angles.length ];
		final ArrayList<Integer> timepoints = new ArrayList<Integer>();
		int numChoices = 0;
		conf.zStretching = -1;
		
		final String name = conf.file[ 0 ][ 0 ][ 0 ].getName();			
		final File regDir = new File( conf.registrationFiledirectory );
		
		if ( !regDir.isDirectory() )
		{
			IOFunctions.println( conf.registrationFiledirectory + " is not a directory. " );
			return null;
		}
		
		final String entries[] = regDir.list( new FilenameFilter() 
		{				
			@Override
			public boolean accept(File directory, String filename) 
			{
				if ( filename.contains( name ) && filename.contains( ".registration" ) )
					return true;
				else 
					return false;
			}
		});

		for ( final String s : entries )
		{
			if ( s.endsWith( ".registration" ) )
			{
				if ( !timepoints.contains( -1 ) )
				{
					timepoints.add( -1 );
					numChoices++;
				}
			}
			else
			{
				final int timepoint = Integer.parseInt( s.substring( s.indexOf( ".registration.to_" ) + 17, s.length() ) );
				
				if ( !timepoints.contains( timepoint ) )
				{
					timepoints.add( timepoint );
					numChoices++;
				}
			}
			
			if ( conf.zStretching < 0 )
			{
				conf.zStretching = Multi_View_Fusion.loadZStretching( conf.registrationFiledirectory + s );
				IOFunctions.println( "Z-stretching = " + conf.zStretching );
			}
		}
		
		if ( numChoices == 0 )
		{
			IOFunctions.println( "No registration files available." );
			return null;
		}


		final GenericDialog gd2 = new GenericDialog( "Apply external transformation" );
		
		// build up choices
		final String[] choices = new String[ numChoices ];
		// no suggestion yet
		int suggest = -1;

		int firstSuggestion = -1;
		int index = 0;
		
		for ( int i = 0; i < timepoints.size(); ++i )
		{
			if ( timepoints.get( i ) == -1 )
				choices[ index ] = "Individual registration";
			else
				choices[ index ] = "Time-point registration (reference=" + timepoints.get( i ) + ")";

			if ( suggest == -1 )
				suggest = index;

			index++;
		}
			
		gd2.addMessage( "Please provide affine 3d matrix either as string or as individual enties" );
		gd2.addMessage( "" );
		gd2.addMessage( "m00 m01 m02 m03" );
		gd2.addMessage( "m10 m11 m12 m13" );
		gd2.addMessage( "m20 m21 m22 m23" );
		gd2.addMessage( "" );
		
		if ( defaultHowToProvide == 0 )
		{
			gd2.addMessage( "Please provide 3d affine in this form (any brackets will be ignored):" );
			gd2.addMessage( "m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23" );
			gd2.addStringField( "Affine_matrix", m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " + m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " + m20 + ", " + m21 + ", " + m22 + ", " + m23, 80 );
		}
		else
		{
			gd2.addNumericField( "m00: ", m00, 0 );
			gd2.addNumericField( "m01: ", m01, 0 );
			gd2.addNumericField( "m02: ", m02, 0 );
			gd2.addNumericField( "m03: ", m03, 0 );
			gd2.addNumericField( "m10: ", m10, 0 );
			gd2.addNumericField( "m11: ", m11, 0 );
			gd2.addNumericField( "m12: ", m12, 0 );
			gd2.addNumericField( "m13: ", m13, 0 );
			gd2.addNumericField( "m20: ", m20, 0 );
			gd2.addNumericField( "m21: ", m21, 0 );
			gd2.addNumericField( "m22: ", m22, 0 );
			gd2.addNumericField( "m23: ", m23, 0 );
		}
		gd2.addMessage("");
		gd2.addChoice( "Apply_to ", choices, choices[ defaultApplyTo ]);
		gd2.addMessage("");
		gd2.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		text = (MultiLineLabel) gd2.getMessage();
		Bead_Registration.addHyperLinkListener(text, myURL);

		gd2.showDialog();
		
		if ( gd2.wasCanceled() )
			return null;

		if ( defaultHowToProvide == 0 )
		{
			String entry = removeSequences( gd2.getNextString().trim(), new String[] { "(", ")", "{", "}", "[", "]", "<", ">", ":", "m00", "m01", "m02", "m03", "m10", "m11", "m12", "m13", "m20", "m21", "m22", "m23", " " } );			
			final String[] numbers = entry.split( "," );
			
			if  ( numbers.length != 12 )
			{
				IJ.log( "Affine matrix has to have 12 entries: m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23" );
				IJ.log( "This one has only " + numbers.length + " after trimming: " + entry );
				
				return null;				
			}
			
			m00 = Double.parseDouble( numbers[ 0 ] );
			m01 = Double.parseDouble( numbers[ 1 ] );
			m02 = Double.parseDouble( numbers[ 2 ] );
			m03 = Double.parseDouble( numbers[ 3 ] );
			m10 = Double.parseDouble( numbers[ 4 ] );
			m11 = Double.parseDouble( numbers[ 5 ] );
			m12 = Double.parseDouble( numbers[ 6 ] );
			m13 = Double.parseDouble( numbers[ 7 ] );
			m20 = Double.parseDouble( numbers[ 8 ] );
			m21 = Double.parseDouble( numbers[ 9 ] );
			m22 = Double.parseDouble( numbers[ 10 ] );
			m23 = Double.parseDouble( numbers[ 11 ] );
		}
		else
		{
			m00 = gd2.getNextNumber();
			m01 = gd2.getNextNumber();
			m02 = gd2.getNextNumber();
			m03 = gd2.getNextNumber();
			m10 = gd2.getNextNumber();
			m11 = gd2.getNextNumber();
			m12 = gd2.getNextNumber();
			m13 = gd2.getNextNumber();
			m20 = gd2.getNextNumber();
			m21 = gd2.getNextNumber();
			m22 = gd2.getNextNumber();
			m23 = gd2.getNextNumber();
		}
		
		final AffineModel3D preConcatenate = new AffineModel3D();
		preConcatenate.set( (float)m00, (float)m01, (float)m02, (float)m03, (float)m10, (float)m11, (float)m12, (float)m13, (float)m20, (float)m21, (float)m22, (float)m23 );
		
		IJ.log( "Pre-concatenating model:" );
		IJ.log( "" + preConcatenate );
		
		final int whichRegistration = gd2.getNextChoiceIndex();
		final int tpIndex = timepoints.get( whichRegistration );		
		
		final int numTimePoints = conf.file.length;
		final int numAngles = conf.file[ 0 ][ 0 ].length;
		
		for ( int indexT = 0; indexT < numTimePoints; ++indexT )
			for ( int indexA = 0; indexA < numAngles; ++indexA )
			{
				final String file = conf.file[ indexT ][ 0 ][ indexA ].getName();			

				final String fileList[] = regDir.list( new FilenameFilter() 
				{				
					@Override
					public boolean accept(File directory, String filename) 
					{
						if ( filename.contains( file ) && filename.contains( ".registration" ) )
							return true;
						else 
							return false;
					}
				});

				String target = null;
				
				for ( final String s : fileList )
				{
					if ( s.endsWith( ".registration" ) && tpIndex == -1 )
					{
						target = s;
					}
					else if ( s.contains( ".registration.to_" ) )
					{
						final int timepoint = Integer.parseInt( s.substring( s.indexOf( ".registration.to_" ) + 17, s.length() ) );
						
						if ( timepoint == tpIndex )
							target = s;
					}
				}

				if ( target != null )
				{
					final File regFile = new File ( conf.registrationFiledirectory, target );
					
					IJ.log( "Applying model to: " + target );
					final AffineModel3D oldModel = IOFunctions.getModelFromFile( regFile );
					final AffineModel3D newModel = oldModel.copy();
					
					newModel.preConcatenate( preConcatenate );
					
					IOFunctions.reWriteRegistrationFile( regFile, newModel, oldModel, preConcatenate );
				}
			}
		

		return conf;
	}
		
	/**
	 * Removes any of those characters from a String: (, ), [, ], {, }, <, >
	 * 
	 * @param entry input (with brackets)
	 * @return input, but without any brackets
	 */
	public static String removeBrackets( String entry )
	{
		return removeSequences( entry, new String[] { "(", ")", "{", "}", "[", "]", "<", ">" } );
	}
	
	public static String removeSequences( String entry, final String[] sequences )
	{
		while ( contains( entry, sequences ) )
		{
			for ( final String s : sequences )
			{
				final int index = entry.indexOf( s );

				if ( index == 0 )
					entry = entry.substring( s.length(), entry.length() );
				else if ( index == entry.length() - s.length() )
					entry = entry.substring( 0, entry.length() - s.length() );
				else if ( index > 0 )
					entry = entry.substring( 0, index ) + entry.substring( index + s.length(), entry.length() );
			}
		}

		return entry;		
	}
	
	public static boolean contains( final String entry, final String[] sequences )
	{
		for ( final String seq : sequences )
			if ( entry.contains( seq ) )
				return true;
		
		return false;
	}
	
	public static void main( String args[ ] )
	{
		String test1 = "dsgsdgsdg324 45 45, 45, 4545 ";
		System.out.println( test1 );
		
		String test2 = "m02:(dsg[[[sdgsd(){}g3{{{24 45 45, m00: 0.43242- ]]]]]]]]45, 4545]] )]]m22";
		System.out.println( removeSequences( test2, new String[] { "(", ")", "{", "}", "[", "]", "<", ">", ":", "m00", "m01", "m02", "m03", "m10", "m11", "m12", "m13", "m20", "m21", "m22", "m23", " " } ) );
	}
}
