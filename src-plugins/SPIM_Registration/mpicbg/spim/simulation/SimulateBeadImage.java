package mpicbg.spim.simulation;
/*
import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3f;

import mpicbg.spim.fusion.SPIMImageFusion;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.Bead;
import mpicbg.spim.registration.ViewDataBeads;
*/
public class SimulateBeadImage
{
	/*
	final ArrayList<ViewDataBeads> views;
	final int numViews;
	final Bead beads[];
	final int imageSize[];
	
	final Point3f min = new Point3f();
	final Point3f max = new Point3f();
	final Point3f size = new Point3f();
	
	
	public SimulateBeadImage( final String[] registrationFiles, final int numBeads, final int imageSize[] )
	{
		numViews = registrationFiles.length;
		this.imageSize = imageSize;
		
		// read some existing registrations
		views = readAffineTransforms( registrationFiles, numViews, imageSize );
		
		// get the output size
		getSize(views);
		
		// create random beads
		beads = createBeads( numBeads );
	
		// volume inject beads
		volumeInjectBeads( views, beads );
	}
	
	protected void volumeInjectBeads( final ViewDataBeads[] views, final Bead[] beads )
	{
		
		for ( final ViewDataBeads view : views )
		{
			//final ViewDataBeads view = views[ 0 ];
			
			OldFloatArray3D image = new OldFloatArray3D( view.imageSizeInt[0], view.imageSizeInt[1], view.imageSizeInt[2] );
			
			for ( final Bead bead : beads )
			{
				Point3f loc = new Point3f( bead.getL() );			
				view.transformation.transform( loc );
				
				//IOFunctions.println( bead );
				ImageFilter.addGaussianSphere(image, null, 255, loc.x, loc.y, loc.z, 1.5f, 5, false);
			}		
			
			IOFunctions.println( "Showing: "  + view.shortName );
			
			image.toImagePlus( view.shortName, 0, 255).show();
		}
	}
	
	protected void getSize( final ViewDataBeads[] views )
	{		
		SPIMImageFusion.computeImageSize(views, min, max, size, 1, 0, 0, 0, true );
	}
	
	protected Bead[] createBeads( final int numBeads )
	{
		Bead beads[] = new Bead[ numBeads ];
		
		Random rnd = new Random( 4363456634634l );
		
		for ( int b = 0; b < numBeads; b++ )
		{
			float x = rnd.nextFloat();
			float y = rnd.nextFloat();
			float z = rnd.nextFloat();
			
			x = x * size.x + min.x;
			y = y * size.y + min.y;
			z = z * size.z + min.z;
			
			beads[ b ] = new Bead( b, new float[]{ x, y, z}, null );
		}
		
		return beads;
	}
	
	protected ViewDataBeads[] readAffineTransforms( final String[] registrationFiles, final int numViews, final int imageSize[] )
	{
		ViewDataBeads views[] = new ViewDataBeads[ numViews ];
		
		for ( int i = 0; i < numViews; i++ )
		{
			views[ i ] = new ViewDataBeads( i, numViews );
			ViewDataBeads view = views[ i ];
			view.fileName = registrationFiles[ i ];
			view.getShortName();
			
			view.imageSizeInt = imageSize.clone();
			view.imageSize = new float[ imageSize.length ];
			
			for ( int l = 0; l < imageSize.length; l++ )
				view.imageSize[ l ] = imageSize[ l ];
			
			IOFunctions.readSingleRegistration( view, view.getFileName() );
		}
		
		return views;		
	}
	
	public static void main( String args[] )
	{
		// read&parse configuration file
		programConfiguration conf = null;
		try
		{
			conf = parseFile("config\\configuration.txt");
		} catch (final Exception e)
		{
			IOFunctions.printErr("Cannot open configuration file: " + e);
			e.printStackTrace();
			return;
		}

		// open imageJ window
		System.getProperties().setProperty("plugins.dir", conf.pluginsDir);
		IOFunctions.println(conf.pluginsDir);
		final String params[] = {"-ijpath " + conf.pluginsDir};
		ij.ImageJ.main(params);

		final String start = "F:/Stephan/worm/registration/good_TL1_angle";
		final String end = ".lsm.registration";
		final String[] inputFiles = new String[]
              { start + "0" + end, 
				start + "45" + end,
				start + "90" + end,
				start + "135" + end,
				start + "180" + end,
				start + "225" + end,
				start + "270" + end,
				start + "315" + end
			  };
		
		new SimulateBeadImage( inputFiles, 10000, new int[]{1024, 1024, 400} );
	}
	*/
}
