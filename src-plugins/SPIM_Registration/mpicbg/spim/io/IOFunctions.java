package mpicbg.spim.io;

import ij.IJ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import mpicbg.models.AffineModel3D;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadIdentification;
import mpicbg.spim.registration.segmentation.Nucleus;

public class IOFunctions
{
	/**
	 * Never instantiate this class, it contains only static methods
	 */
	protected IOFunctions() { }
	
	public static boolean printIJLog = false;

	public static void printlnTS() { printlnTS( "" ); }
	public static void printlnTS( final Object object) { printlnTS( object.toString() ); }
	public static void printlnTS( final String string ) 
	{
		println( new Date( System.currentTimeMillis() ) + ": " + string );
	}

	public static void println() { println( "" ); }
	public static void println( final Object object) { println( object.toString() ); }
	public static void println( final String string )
	{
		if ( printIJLog )
			IJ.log( string );
		else
			System.out.println( string );
	}

	public static void printErr() { printErr( "" ); }
	public static void printErr( final Object object) { printErr( object.toString() ); }
	public static void printErr( final String string )
	{
		if ( printIJLog )
			IJ.error( string );
		else
			System.err.println( string );
	}

	public static SPIMConfiguration initSPIMProcessing()
	{
		return initSPIMProcessing( "config/configuration.txt", "spimconfig/configuration.txt" );
	}
	
	public static SPIMConfiguration initSPIMProcessing( String configFile, String spimConfigFile )
	{
        SPIMConfiguration config = null;
        
        try 
        {
 			config = ConfigurationParserSPIM.parseFile( spimConfigFile );
			
			if ( config.debugLevelInt <= ViewStructure.DEBUG_ALL )
				config.printProperties();
		} 
        catch ( ConfigurationParserException e ) 
        {
        	IOFunctions.println( "Cannot open SPIM configuration file: \n" + e );
        	return null;
		}
    	
    	if ( config.showImageJWindow )
    	{
			// read&parse configuration file
			ProgramConfiguration conf = null;
			
			try
			{
				conf = ConfigurationParserGeneral.parseFile( configFile );
			} 
			catch (final Exception e)
			{
				IOFunctions.println( "Cannot open configuration file: \n" + e );
			}
	
			// open imageJ window
			if ( conf != null )
			{
				System.getProperties().setProperty( "plugins.dir", conf.pluginsDir );
				final String params[] = { "-ijpath " + conf.pluginsDir };
				
				// call the imageJ main class
				ij.ImageJ.main( params );
			}
			else
			{
				final String params[] = { "-ijpath ." };
				
				// call the imageJ main class
				ij.ImageJ.main( params );
			}
    	}
    	
    	return config;
	}
	
	public static String getShortName( final String fileName )
	{
		String shortName = fileName;
		shortName = shortName.replace('\\', '/');
		while (shortName.contains("/"))
			shortName = shortName.substring(shortName.indexOf("/") + 1, shortName.length());
		
		return shortName;
	}
	
	public static boolean writeNuclei( final ViewDataBeads view, final Collection<Nucleus> nuclei, final String directory )
	{
		final String fileName = directory + view.getName() + ".nuclei.txt";
		
		try
		{
			PrintWriter out = TextFileAccess.openFileWriteEx( fileName );
			out.println("ID" + "\t" + "ViewID" + "\t" + "Lx" + "\t" + "Ly" + "\t" + "Lz" + "\t" + "Wx" + "\t" + "Wy" + "\t" + "Wz" + "\t" + "Weight" );
			
			for ( final Nucleus nucleus : nuclei )
			{
				out.print(nucleus.getID() + "\t" + nucleus.getViewID() + "\t");
				out.print(nucleus.getL()[0] + "\t" + nucleus.getL()[1] + "\t" + nucleus.getL()[2] + "\t");
				out.print(nucleus.getW()[0] + "\t" + nucleus.getW()[1] + "\t" + nucleus.getW()[2] + "\t");
				out.print(nucleus.getWeight() + "\t");

				out.println();
			}
						
			out.close();
			
			out = TextFileAccess.openFileWriteEx( directory + view.getName() + ".dim" );				
			out.println("image width: " + view.getImageSize()[0]);
			out.println("image height: " + view.getImageSize()[1]);
			out.println("image depth: " + view.getImageSize()[2]);				
			out.close();
		}
		catch (IOException e)
		{
			IOFunctions.printErr("BeadDetection(): " + e);
			return false;
		}		
		
		return true;
	}
	
	public static ArrayList<Nucleus> readNuclei( final ViewDataBeads view, final String directory )
	{
		final int debugLevel = view.getViewStructure().getDebugLevel();

		final ArrayList<Nucleus> nuclei = new ArrayList<Nucleus>();
		
		int countLine = 0;
		try
		{			
			BufferedReader in = TextFileAccess.openFileRead( directory + view.getName() + ".nuclei.txt" );

			// read header
			in.readLine();
						
			
			boolean printedOnce = false;
						
			while ( in.ready() )
			{
				++countLine;
				final String line = in.readLine();
				final String entries[] = line.split( "\t" );
				
				final int beadID = Integer.parseInt(entries[0]);
				final int viewID = Integer.parseInt(entries[1]);
				
				if ( view.getID() != viewID && !printedOnce )
				{
					if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
						IOFunctions.printErr("ViewID messed up, should be " + viewID + "(file) but is " + view.getID() + "(view). We have to recompute the registration (WILL BE OVERWRITTEN).");
					
					printedOnce = true;
				}
				
				final float[] l = new float[]{ Float.parseFloat(entries[2]), Float.parseFloat(entries[3]), Float.parseFloat(entries[4])};
				final float[] w = new float[]{ Float.parseFloat(entries[5]), Float.parseFloat(entries[6]), Float.parseFloat(entries[7])};
				final double weight = Double.parseDouble( entries[8] );
				
				Nucleus nucleus = new Nucleus( beadID, l, view );				
				nucleus.setW( w );
				nucleus.setWeight( weight );
								
				nuclei.add( nucleus );
			}
			
			if ( debugLevel <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("Read " + nuclei.size() + " nuclei for " + view );
			
			in.close();
		}
		catch (Exception e)
		{
			if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("IOFunctions.readNuclei(): " + e + " in view " + view.getName() + " of " + view.getViewStructure() + " in line " + countLine );
		
			return null;
		}
		
		return nuclei;
	}

	public static boolean writeSegmentation( final ViewDataBeads view, final String directory )
	{
		return writeSegmentation( view.getBeadStructure().getBeadList(), view.getImageSize(), view.getName(), view.getID(), directory );
	}
	
	public static boolean writeSegmentation( final ArrayList<Bead> beads, final int[] imgSize, final String viewName, final int viewID, final String directory )
	{
		final String fileName = directory + viewName + ".beads.txt";
		
		try
		{
			PrintWriter out = TextFileAccess.openFileWriteEx( fileName );
			out.println("ID" + "\t" + "ViewID" + "\t" + "Lx" + "\t" + "Ly" + "\t" + "Lz" + "\t" + "Wx" + "\t" + "Wy" + "\t" + "Wz" + "\t" + "Weight" + "\t" + "DescCorr"+ "\t" + "RansacCorr");
			
			for ( final Bead bead : beads )
			{
				out.print(bead.getID() + "\t" + viewID + "\t");
				out.print(bead.getL()[0] + "\t" + bead.getL()[1] + "\t" + bead.getL()[2] + "\t");
				out.print(bead.getW()[0] + "\t" + bead.getW()[1] + "\t" + bead.getW()[2] + "\t");
				out.print(bead.getWeight() + "\t");
				
				for ( final BeadIdentification descBead : bead.getDescriptorCorrespondence() )
					out.print( descBead.getBeadID() + ":" + descBead.getViewID() + ";" );

				if (bead.getDescriptorCorrespondence().size() == 0)
					out.print( "0\t" );
				else
					out.print( "\t" );
				
				for ( final BeadIdentification ransacBead : bead.getRANSACCorrespondence() )
					out.print( ransacBead.getBeadID() + ":" + ransacBead.getViewID() + ";" );

				if (bead.getRANSACCorrespondence().size() == 0)
					out.print( "0" );
				
				out.println();
			}
						
			out.close();
			
			if ( imgSize != null )
			{
				out = TextFileAccess.openFileWriteEx( directory + viewName + ".dim" );				
				out.println("image width: " + imgSize[0]);
				out.println("image height: " + imgSize[1]);
				out.println("image depth: " + imgSize[2]);				
				out.close();
			}
		}
		catch (IOException e)
		{
			IOFunctions.printErr("BeadDetection(): " + e);
			return false;
		}		
		
		return true;
	}

	public static boolean readSegmentation( final ViewDataBeads view, final String directory, final SPIMConfiguration conf )
	{
		boolean readSeg = true;
		final int debugLevel = view.getViewStructure().getDebugLevel();

		int countLine = 0;
		try
		{			
			BufferedReader in = TextFileAccess.openFileRead( directory + view.getName() + ".beads.txt" );

			// read header
			in.readLine();
						
			// remove all that were in before
			view.getBeadStructure().getBeadList().clear();
			
			boolean printedOnce = false;
						
			while ( in.ready() )
			{
				++countLine;
				final String line = in.readLine();
				final String entries[] = line.split( "\t" );
				
				final int beadID = Integer.parseInt(entries[0]);
				final int viewID = Integer.parseInt(entries[1]);
				
				if ( view.getID() != viewID && !printedOnce )
				{
					if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
						IOFunctions.printErr("ViewID messed up, should be " + viewID + "(file) but is " + view.getID() + "(view). We have to recompute the registration (WILL BE OVERWRITTEN).");
					
					if ( conf != null)
					{
						conf.readRegistration = false;
						conf.writeRegistration = true;
					}
					
					printedOnce = true;
				}
				
				final float[] l = new float[]{ Float.parseFloat(entries[2]), Float.parseFloat(entries[3]), Float.parseFloat(entries[4])};
				final float[] w = new float[]{ Float.parseFloat(entries[5]), Float.parseFloat(entries[6]), Float.parseFloat(entries[7])};
				final double weight = Double.parseDouble( entries[8] );
				
				Bead bead = new Bead( beadID, l, view );				
				bead.setW( w );
				bead.setWeight( weight );
				
				if (entries.length >= 10)
				{
					final String descCorrespondences = entries[9].trim();
					if ( descCorrespondences.length() > 2 )
					{
						final String corr[] = descCorrespondences.split(";");
						if (corr.length > 0)
							for ( final String entry : corr )
							{
								final int corrBeadID = Integer.parseInt(entry.substring(0, entry.indexOf(':')));
								final int corrViewID = Integer.parseInt(entry.substring(entry.indexOf(':')+1, entry.length()));
								try
								{
									bead.getDescriptorCorrespondence().add( new BeadIdentification( corrBeadID, view.getViewStructure().getViewFromID(corrViewID) ));
								}
								catch(Exception e ){}
							}
					}
				}
				
				if (entries.length >= 11)
				{
					final String ransacCorrespondences = entries[10].trim();
					if ( ransacCorrespondences.length() > 2 )
					{
						final String corr[] = ransacCorrespondences.split(";");
						if (corr.length > 0)
							for ( final String entry : corr )
							{
								final int corrBeadID = Integer.parseInt(entry.substring(0, entry.indexOf(':')));
								final int corrViewID = Integer.parseInt(entry.substring(entry.indexOf(':')+1, entry.length()));
								try
								{
									bead.getRANSACCorrespondence().add( new BeadIdentification(corrBeadID, view.getViewStructure().getViewFromID(corrViewID) ));
								}
								catch(Exception e ){}									
							}
					}
				}
				
				view.getBeadStructure().addBead( bead );
			}
			
			if ( debugLevel <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("Read " + view.getBeadStructure().getBeadList().size() + " beads for " + view );
			
			in.close();
		}
		catch (Exception e)
		{
			if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("IOFunctions.readSegmentation(): " + e + " in view " + view.getName() + " of " + view.getViewStructure() + " in line " + countLine );
			
			readSeg = false;
			view.getBeadStructure().getBeadList().clear();
		}
		
		return readSeg;
	}
	
	public static boolean readDim( final ViewDataBeads view, final String directory )
	{
		boolean readDim = true;

		try
		{
			BufferedReader in = TextFileAccess.openFileRead( directory + view.getName() + ".dim" );
			final int[] imageSize = new int[3];
			
			for (int j = 0; j < imageSize.length; j++)
				imageSize[j] = -1;
			
			while (in.ready())
			{					
				String entry = in.readLine().trim();					
				if (entry.startsWith("image width:"))
				{
					imageSize[0] = Integer.parseInt(entry.substring(13, entry.length()).trim());
				}
				else if (entry.startsWith("image height:"))
				{
					imageSize[1] = Integer.parseInt(entry.substring(13, entry.length()).trim());
				}
				else if (entry.startsWith("image depth:"))
				{
					imageSize[2] = Integer.parseInt(entry.substring(13, entry.length()).trim());
				}
			}				
			in.close();
	
			for (int j = 0; j < imageSize.length; j++)
				if ( imageSize[j] == -1)
				{
					view.setImageSize( null );					
					return false;
				}
			
			view.setImageSize( imageSize );
		}
		catch (Exception e)
		{
			IOFunctions.printErr("BeadDetection(): " + e);
			readDim = false;
			
			view.setImageSize( null );
		}
		
		return readDim;	
	}
	
	/*public static boolean[] readSegmentation( final ViewDataBeads[] views, final String directory, final SPIMConfiguration conf )
	{
		boolean readSeg[] = new boolean[views.length];
		boolean readDim[] = new boolean[views.length];
		
		for (int i = 0; i < views.length; i++)
		{
			ViewDataBeads view = views[i];

			readDim[i] = readDim( view, directory );			
			if (!readDim[i]) 
				continue;
			
			readSeg[i] = readSegmentation( view, directory, conf );			
			if (!readSeg[i])
			{
				readDim[i] = false;
				continue;
			}

			if (readSeg[i] && readDim[i])
			{
				IOFunctions.println("Loaded " + view.getBeadStructure().getBeadList().size() + " beads for " + view.shortName + 
								   "[" + view.getImageSize()[0] + "x" + view.getImageSize()[1] + "x" + view.getImageSize()[2] + "]");				
			}
		}

		return readSeg;
	}*/

	public static boolean writeRegistration( final ViewDataBeads view, final String directory )
	{
		return writeRegistration( view, directory, "" );
	}
	
	public static boolean writeRegistration( final ViewDataBeads view, final String directory, final String extension )
	{
		final String fileName = directory + view.getName() + ".registration" + extension;
		if ( view.getTile().getModel() != null )
		{			
			try
			{			
				PrintWriter out = TextFileAccess.openFileWrite( fileName );
				
				final AffineModel3D model = (AffineModel3D)view.getTile().getModel();
				final float m[] = model.getMatrix( null );
				
				out.println("m00: " + m[ 0 ] );
				out.println("m01: " + m[ 1 ] );
				out.println("m02: " + m[ 2 ] );
				out.println("m03: " + m[ 3 ] );
				out.println("m10: " + m[ 4 ] );
				out.println("m11: " + m[ 5 ] );
				out.println("m12: " + m[ 6 ] );
				out.println("m13: " + m[ 7 ] );
				out.println("m20: " + m[ 8 ] );
				out.println("m21: " + m[ 9 ] );
				out.println("m22: " + m[ 10 ] );
				out.println("m23: " + m[ 11 ] );
				out.println("m30: " + "0" );
				out.println("m31: " + "0" );
				out.println("m32: " + "0" );
				out.println("m33: " + "1" );
				out.println("");
				out.println("minError: " + view.getViewStructure().getGlobalErrorStatistics().getMinAlignmentError());
				out.println("avgError: " + view.getViewStructure().getGlobalErrorStatistics().getAverageAlignmentError());
				out.println("maxError: " + view.getViewStructure().getGlobalErrorStatistics().getMaxAlignmentError());
				out.println("");
				out.println("z-scaling: " + view.getZStretching() );
				out.println("Angle Specific Average Error: " + view.getViewErrorStatistics().getAverageViewError() );
				out.println("Overlapping Views: " + view.getViewErrorStatistics().getNumConnectedViews() );
				out.println("Num beads having true correspondences: " + view.getViewErrorStatistics().getNumDetectionsWithTrueCorrespondences() );
				out.println("Sum of true correspondences pairs: " + view.getViewErrorStatistics().getNumTrueCorrespondencePairs() );
				out.println("Num beads having correspondences candidates: " + view.getViewErrorStatistics().getNumDetectionsWithCandidates() );				
				out.println("Sum of correspondences candidates pairs: " + view.getViewErrorStatistics().getNumCandidatePairs() );				
				
				
				for ( final ViewDataBeads otherView : view.getViewStructure().getViews() )
				{
					if ( otherView != view )
					{
						out.println( "" );
						out.println( otherView.getName() + " - Average Error: " + view.getViewErrorStatistics().getViewSpecificError( otherView ) );
						out.println( otherView.getName() + " - Bead Correspondences: " + view.getViewErrorStatistics().getNumCandidatePairs( otherView ) );
						out.println( otherView.getName() + " - Ransac Correspondences: " + view.getViewErrorStatistics().getNumTrueCorrespondencePairs( otherView ) );
					}
				}
				
				out.close();
			}
			catch (Exception e)
			{
				IOFunctions.printErr("Cannot write registration file: " + fileName);
				return false;
			}
		}		
		
		return true;
	}
	
	/*
	public static void writeSegmentation( final ViewDataBeads[] views, final String directory )
	{
		for (ViewDataBeads view : views)
			writeSegmentation(view, directory);			
	}

	public static void writeRegistration( final ViewDataBeads[] views, final String directory )
	{
		for (ViewDataBeads view : views)
		{
			String fileName = directory + view.shortName + ".registration";
			writeSingleRegistration( view, fileName );			
		}		
	}
	*/
		
	public static boolean readRegistration( final ViewDataBeads view, final String fileName )
	{
		final AffineModel3D model = (AffineModel3D)view.getTile().getModel();
		final float m[] = model.getMatrix( null );
		
		boolean readReg = true;
		
		BufferedReader in = TextFileAccess.openFileRead( fileName );
		try
		{
			while (in.ready())
			{
				String entry = in.readLine().trim();
				if (entry.startsWith("m00:"))
					m[ 0 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m01:"))
					m[ 1 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m02:"))
					m[ 2 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m03:"))
					m[ 3 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m10:"))
					m[ 4 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m11:"))
					m[ 5 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m12:"))
					m[ 6 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m13:"))
					m[ 7 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m20:"))
					m[ 8 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m21:"))
					m[ 9 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m22:"))
					m[ 10 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m23:"))
					m[ 11 ] = Float.parseFloat(entry.substring(5, entry.length()));
				/*else if (entry.startsWith("m30:"))
					m[ 12 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m31:"))
					m[ 13 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m32:"))
					m[ 14 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m33:"))
					m[ 15 ] = Float.parseFloat(entry.substring(5, entry.length()));*/
				else if (entry.startsWith("minError:") )					 
					view.getViewStructure().getGlobalErrorStatistics().setMinAlignmentError( Double.parseDouble(entry.substring(10, entry.length())) );
				else if (entry.startsWith("maxError:") )
					view.getViewStructure().getGlobalErrorStatistics().setMaxAlignmentError( Double.parseDouble(entry.substring(10, entry.length())) );
				else if (entry.startsWith("avgError:") )
					view.getViewStructure().getGlobalErrorStatistics().setAverageAlignmentError( Double.parseDouble(entry.substring(10, entry.length())) );
				else
				{
					for ( ViewDataBeads otherView : view.getViewStructure().getViews() )
						if ( entry.startsWith( otherView.getName() + " - Average Error:" ) )
							view.getViewErrorStatistics().setViewSpecificError( otherView,  Double.parseDouble(entry.substring( 17 + otherView.getName().length(), entry.length())) );
				}
			}
			in.close();

			model.set( m[ 0 ], m[ 1 ], m[ 2 ], m[ 3 ], m[ 4 ], m[ 5 ], m[ 6 ], m[ 7 ], m[ 8 ], m[ 9 ], m[ 10 ], m[ 11 ] );
			
			if ( view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ALL )
				IOFunctions.println( "Transformation for (" + view.getName() + "):\n" + model );
		}
		catch (Exception e)
		{
			IOFunctions.printErr("Cannot read registration file: " + fileName + e);
			readReg = false;
		}
		
		return readReg;
	}	
}
