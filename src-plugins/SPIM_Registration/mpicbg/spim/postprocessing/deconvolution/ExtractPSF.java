package mpicbg.spim.postprocessing.deconvolution;

import ij.IJ;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.transformation.ImageTransform;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.CoordinateTransform;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadRegistration;

public class ExtractPSF
{
	final ViewStructure viewStructure;
	final ArrayList<Image<FloatType>> pointSpreadFunctions;
	Image<FloatType> avgPSF;
	
	int size = 19;
	boolean isotropic = false;
	
	public ExtractPSF( final SPIMConfiguration config )
	{
		//
		// load the files
		//
		final ViewStructure viewStructure = ViewStructure.initViewStructure( config, 0, new AffineModel3D(), "ViewStructure Timepoint 0", config.debugLevelInt );						

		for ( ViewDataBeads view : viewStructure.getViews() )
		{
			view.loadDimensions();
			view.loadSegmentation();
			view.loadRegistration();

			BeadRegistration.concatenateAxialScaling( view, ViewStructure.DEBUG_MAIN );
		}
		
		this.viewStructure = viewStructure;
		this.pointSpreadFunctions = new ArrayList<Image<FloatType>>();
	}
	
	public ExtractPSF( final ViewStructure viewStructure )
	{		
		this.viewStructure = viewStructure;
		this.pointSpreadFunctions = new ArrayList<Image<FloatType>>();
	}
	
	/**
	 * Defines the size of the PSF that is extracted
	 * 
	 * @param size - number of pixels in xy
	 * @param isotropic - if isotropic, than same size applies to z (in px), otherwise it is divided by half the z-stretching
	 */
	public void setPSFSize( final int size, final boolean isotropic )
	{
		this.size = size;
		this.isotropic = isotropic;
	}
	
	public ArrayList< Image< FloatType > > getPSFs() { return pointSpreadFunctions; }
	public Image< FloatType > getPSF( final int index ) { return pointSpreadFunctions.get( index ); }
	public Image< FloatType > getAveragePSF() { return avgPSF; }
	
	public void extract()
	{
		final ArrayList<ViewDataBeads > views = viewStructure.getViews();
		final int numDimensions = 3;
		
		final int[] size = Util.getArrayFromValue( this.size, numDimensions );
		if ( !this.isotropic )
			size[ numDimensions - 1 ] /= Math.max( 1, views.get( 0 ).getZStretching()/2 );
		
		IJ.log ( Util.printCoordinates( size ) );
		
		final int[] maxSize = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			maxSize[ d ] = 0;
		
		for ( final ViewDataBeads view : views )		
		{			
			final Image<FloatType> psf = getTransformedPSF(view, size); 
			psf.setName( "PSF_" + view.getName() );
			
			for ( int d = 0; d < numDimensions; ++d )
				if ( psf.getDimension( d ) > maxSize[ d ] )
					maxSize[ d ] = psf.getDimension( d );
			
			pointSpreadFunctions.add( psf );
			
			psf.getDisplay().setMinMax();
		}
		
		avgPSF = pointSpreadFunctions.get( 0 ).createNewImage( maxSize );
		
		final int[] avgCenter = new int[ numDimensions ];		
		for ( int d = 0; d < numDimensions; ++d )
			avgCenter[ d ] = avgPSF.getDimension( d ) / 2;
			
		for ( final Image<FloatType> psf : pointSpreadFunctions )
		{
			final LocalizableByDimCursor<FloatType> avgCursor = avgPSF.createLocalizableByDimCursor();
			final LocalizableCursor<FloatType> psfCursor = psf.createLocalizableCursor();
			
			final int[] loc = new int[ numDimensions ];
			final int[] psfCenter = new int[ numDimensions ];		
			for ( int d = 0; d < numDimensions; ++d )
				psfCenter[ d ] = psf.getDimension( d ) / 2;
			
			while ( psfCursor.hasNext() )
			{
				psfCursor.fwd();
				psfCursor.getPosition( loc );
				
				for ( int d = 0; d < numDimensions; ++d )
					loc[ d ] = psfCenter[ d ] - loc[ d ] + avgCenter[ d ];
				
				avgCursor.moveTo( loc );
				avgCursor.getType().add( psfCursor.getType() );				
			}
			
			avgCursor.close();
			psfCursor.close();
		}
		
		avgPSF.getDisplay().setMinMax();
	}
	
	public static Image<FloatType> getTransformedPSF( final ViewDataBeads view, final int[] size )
	{
		final Image<FloatType> psf = extractPSF( view, size );
		return transformPSF( psf, (AbstractAffineModel3D<?>)view.getTile().getModel() );		
	}
	
	/**
	 * Transforms the extracted PSF using the affine transformation of the corresponding view
	 * 
	 * @param psf - the extracted psf (NOT z-scaling corrected)
	 * @param model - the transformation model
	 * @return the transformed psf which has odd sizes and where the center of the psf is also the center of the transformed psf
	 */
	protected static Image<FloatType> transformPSF( final Image<FloatType> psf, final AbstractAffineModel3D<?> model )
	{
		// here we compute a slightly different transformation than the ImageTransform does
		// two things are necessary:
		// a) the center pixel stays the center pixel
		// b) the transformed psf has a odd size in all dimensions
		
		final int numDimensions = psf.getNumDimensions();
		
		final float[][] minMaxDim = ExtractPSF.getMinMaxDim( psf.getDimensions(), model );
		final float[] size = new float[ numDimensions ];		
		final int[] newSize = new int[ numDimensions ];		
		final float[] offset = new float[ numDimensions ];
		
		// the center of the psf has to be the center of the transformed psf as well
		// this is important!
		final float[] center = new float[ numDimensions ]; 
		
		for ( int d = 0; d < numDimensions; ++d )
			center[ d ] = psf.getDimension( d ) / 2;
		
		model.applyInPlace( center );

		for ( int d = 0; d < numDimensions; ++d )
		{						
			size[ d ] = minMaxDim[ d ][ 1 ] - minMaxDim[ d ][ 0 ];
			
			newSize[ d ] = (int)size[ d ] + 3;
			if ( newSize[ d ] % 2 == 0 )
				++newSize[ d ];
				
			// the offset is defined like this:
			// the transformed coordinates of the center of the psf
			// are the center of the transformed psf
			offset[ d ] = center[ d ] - newSize[ d ]/2;
			
			//System.out.println( MathLib.printCoordinates( minMaxDim[ d ] ) + " size " + size[ d ] + " newSize " + newSize[ d ] );
		}
		
		final ImageTransform<FloatType> transform = new ImageTransform<FloatType>( psf, model, new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyValueFactory<FloatType>()));
		transform.setOffset( offset );
		transform.setNewImageSize( newSize );
		
		if ( !transform.checkInput() || !transform.process() )
		{
			System.out.println( "Error transforming psf: " + transform.getErrorMessage() );
			return null;
		}
		
		final Image<FloatType> transformedPSF = transform.getResult();
		
		ViewDataBeads.normalizeImage( transformedPSF );
		
		return transformedPSF;
	}
		
	/**
	 * Extracts the PSF by averaging the local neighborhood RANSAC correspondences
	 * @param view - the SPIM view
	 * @param size - the size in which the psf is extracted (in pixel units, z-scaling is ignored)
	 * @return - the psf, NOT z-scaling corrected
	 */
	protected static Image<FloatType> extractPSF( final ViewDataBeads view, final int[] size )
	{
		final int numDimensions = size.length;
		
		final OutOfBoundsStrategyFactory<FloatType> outside = new OutOfBoundsStrategyMirrorFactory<FloatType>();
		final InterpolatorFactory<FloatType> interpolatorFactory = new LinearInterpolatorFactory<FloatType>( outside );
		
		final ImageFactory<FloatType> imageFactory = new ImageFactory<FloatType>( new FloatType(), new ArrayContainerFactory() );
		final Image<FloatType> img = view.getImage();
		final Image<FloatType> psf = imageFactory.createImage( size );
		
		final Interpolator<FloatType> interpolator = img.createInterpolator( interpolatorFactory );
		final LocalizableCursor<FloatType> psfCursor = psf.createLocalizableCursor();
		
		final int[] sizeHalf = size.clone();		
		for ( int d = 0; d < numDimensions; ++d )
			sizeHalf[ d ] /= 2;
		
		int numRANSACBeads = 0;
		
		for ( final Bead bead : view.getBeadStructure().getBeadList() )
		{			
			final float[] position = bead.getL().clone();
			final int[] tmpI = new int[ position.length ];
			final float[] tmpF = new float[ position.length ];
			
			// check if it is a true correspondence
			if ( bead.getRANSACCorrespondence().size() > 0 ) 
			{
				++numRANSACBeads;
				psfCursor.reset();
				
				while ( psfCursor.hasNext() )
				{
					psfCursor.fwd();
					psfCursor.getPosition( tmpI );

					for ( int d = 0; d < numDimensions; ++d )
						tmpF[ d ] = tmpI[ d ] - sizeHalf[ d ] + position[ d ];
					
					interpolator.moveTo( tmpF );
					
					psfCursor.getType().add( interpolator.getType() );
				}
			}
		}

		// compute the average		
		final FloatType n = new FloatType( numRANSACBeads );
		
		psfCursor.reset();
		while ( psfCursor.hasNext() )
		{
			psfCursor.fwd();
			psfCursor.getType().div( n );			
		}	
	
		ViewDataBeads.normalizeImage( psf );
		
		return psf;
	}

	private static float[][] getMinMaxDim( final int[] dimensions, final CoordinateTransform transform )
	{
		final int numDimensions = dimensions.length;
		
		final float[] tmp = new float[ numDimensions ];
		final float[][] minMaxDim = new float[ numDimensions ][ 2 ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			minMaxDim[ d ][ 0 ] = Float.MAX_VALUE;
			minMaxDim[ d ][ 1 ] = -Float.MAX_VALUE;
		}
		
		// recursively get all corner points of the image, assuming they will still be the extremum points
		// in the transformed image
		final boolean[][] positions = new boolean[ Util.pow( 2, numDimensions ) ][ numDimensions ];
		Util.setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );
		
		// get the min and max location for each dimension independently  
		for ( int i = 0; i < positions.length; ++i )
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				if ( positions[ i ][ d ])
					tmp[ d ] = dimensions[ d ] - 1;
				else
					tmp[ d ] = 0;
			}
			
			transform.applyInPlace( tmp );
			
			for ( int d = 0; d < numDimensions; ++d )
			{				
				if ( tmp[ d ] < minMaxDim[ d ][ 0 ]) 
					minMaxDim[ d ][ 0 ] = tmp[ d ];

				if ( tmp[ d ] > minMaxDim[ d ][ 1 ]) 
					minMaxDim[ d ][ 1 ] = tmp[ d ];
			}				
		}
		
		return minMaxDim;
	}
		
	public static void main( String args[] )
	{
		new ExtractPSF( IOFunctions.initSPIMProcessing() );
	}

	/*
	public static int debugLevel = Reconstruction.DEBUG_MAIN;
	protected final ViewDataBeads[] views;
	protected final Point3f min = new Point3f();
	protected final Point3f max = new Point3f();
	protected final Point3f size = new Point3f();
	protected final Point3f cropOffset = new Point3f();
	protected final Point3f cropSize = new Point3f();

	protected final Point3i neighborhood = new Point3i( 29, 29, 29 );


	public ExtractBeads( final SPIMConfiguration config )
	{
		// load all views
		views = IOFunctions.loadViewsWithData( "Visualize Beads", config.file[0], config.registrationFiledirectory, config.zStretching, new AffineModel3D(), true, this, null, Reconstruction.DEBUG_ALL);
		BeadRegistration.concatZScaling( views, Reconstruction.DEBUG_ALL );

		float[] dim = views[0].imageSize;
		IOFunctions.println( dim[0] );
		IOFunctions.println( dim[1] );
		IOFunctions.println( dim[2] );
		
		cropOffset.x = config.cropOffsetX;
		cropOffset.y = config.cropOffsetY;
		cropOffset.z = config.cropOffsetZ;
		
		cropSize.x = config.cropSizeX;
		cropSize.y = config.cropSizeY;
		cropSize.z = config.cropSizeZ;
		
		
		SPIMImageFusion.computeImageSize( views, min, max, size, config.scale, config.cropSizeX, config.cropSizeY, config.cropSizeZ, true );
								
		IOFunctions.println( "min: " + min );
		IOFunctions.println( "max: " + max );
		IOFunctions.println( "cropOffset: " + cropOffset );
		
		for ( int i = 1; i < 5; i++)
			extractBeads( views[i], min, max, cropOffset, cropSize, neighborhood, config );
	}
	
	
	protected void extractBeads( final ViewDataBeads view, final Point3f min, final Point3f max, 
			                     final Point3f cropOffset, final Point3f cropSize, final Point3i neighborhood,
			                     final SPIMConfiguration config )
	{
		view.imageFloat = ViewDataBeads.loadImage( view, 0, config.scaleSpaceFactory);
		
		final Float3D psf = view.imageFloat.createNewImage( neighborhood.x, neighborhood.y, neighborhood.z );		
		final FloatLocalizableIterator i = psf.createLocalizableIterator();		
		final FloatInterpolatedAccess interpolator = view.imageFloat.createInterpolatedAccessor( new FloatOutOfBoundsStrategyValueFactory( 0 ), new FloatLinearInterpolatorFactory() );
		
		int count = 0;
		
		for ( final Bead bead : view.beads.getBeadList() )
		{
			final Point3f location = new Point3f( bead.getLocationOutputSpace() );
			
			location.sub( min );
			location.sub( cropOffset );
			
			if ( bead.getRANSACCorrespondence().size() > 0 && 
				 location.x >= 0 && location.y >= 0 && location.z >= 0 && 
				 location.x < cropSize.x && location.y < cropSize.y && location.z < cropSize.z )
			{		
				++count;
				
				//IOFunctions.println( bead.getLocationImage() + " " + bead.getLocationOutputSpace() );
				//IOFunctions.println( location );
				
				i.reset();

				while ( i.hasNext() )
				{
					i.next();	
					
					final float x = ((i.getX() - neighborhood.x/2) + location.x); 
					final float y = ((i.getY() - neighborhood.y/2) + location.y); 
					final float z = ((i.getZ() - neighborhood.z/2) + location.z); 
					
					//IOFunctions.println( x + " " + y + " " +  z + " = " + interpolator.get( x, y, z ) );
					
					i.set( i.get() + interpolator.get( x, y, z ) );
					
				}
			}
		}

		i.reset();

		while ( i.hasNext() )
		{
			i.next();
			i.set( i.get()/(float)(count) );
		}		
		
		view.imageFloat.close();
		
		psf.convertToImagePlus("psf_"+view.shortName).show();
		
	}
	
	public static void main(String[] args) 
	{
		// read&parse configuration file
		programConfiguration conf = null;
		try
		{
			conf = parseFile("config/configuration.txt");
		} 
		catch (final Exception e)
		{
			IOFunctions.printErr("Cannot open configuration file: " + e);
			e.printStackTrace();
			return;
		}

		// open imageJ window
		System.getProperties().setProperty("plugins.dir", conf.pluginsDir);
		final String params[] = {"-ijpath " + conf.pluginsDir};
		ij.ImageJ.main(params);

		// read SPIM configuration
		SPIMConfiguration config = null;

		try
		{
			config = ConfigurationParserSPIM.parseFile("spimconfig/configuration.txt");
			config.printProperties();
		}
		catch (final ConfigurationParserException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		new ExtractBeads( config );

	}
	// statistics
    protected double avgError = -1;
    protected double minError = -1;
    protected double maxError = -1;
    protected int numCorrespondences = 0;
    protected int countAvgErrors = 0;
    protected double avgLocalError = 0;

    public double getAverageAlignmentError(){ return avgError; }
    public double getMinAlignmentError(){ return minError; }
    public double getMaxAlignmentError(){ return maxError; }
    
    public void setAverageAlignmentError( final double avg ){ avgError = avg; }
    public void setMinAlignmentError( final double min ){ minError = min; }
    public void setMaxAlignmentError( final double max ){ maxError = max; }
    
    public double getAverageLocalAlignmentError(){ return avgLocalError/(double)countAvgErrors; }
    public int getNumCorrespondences(){ return numCorrespondences; }
    public double[] getAngleSpecificErrors() 
    {
    	double[] errors = new double[views.length];
    	
    	for (int i = 0; i < views.length; i++)
    		if (views[i].angleSpecificCount > 1)
    			errors[i] = views[i].angleSpecificError / (double)views[i].angleSpecificCount;
    	
    	return errors; 
    }
    
    public void printAngleSpecificErrors()
    {
    	for (int i = 0; i < views.length; i++)
			IOFunctions.println(views[i].shortName + " " + (views[i].angleSpecificError / (double)views[i].angleSpecificCount) + " " + (((double)views[i].ransacCorrespondences / (double)views[i].beadDescriptorCorrespondences)*100) + "%");
    }
    */
}
