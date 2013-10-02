package mpicbg.spim.postprocessing.deconvolution;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.transformation.ImageTransform;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyPeriodicFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.Type;
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
import fiji.plugin.Multi_View_Deconvolution;

public class ExtractPSF
{
	final ViewStructure viewStructure;
	final ArrayList<Image<FloatType>> pointSpreadFunctions, originalPSFs;
	Image<FloatType> avgPSF, avgOriginalPSF;
	final SPIMConfiguration conf;
	
	int[] size3d = null;
	int size = 17;
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
		this.originalPSFs = new ArrayList<Image<FloatType>>();
		this.conf = config;
		
		setPSFSize( Multi_View_Deconvolution.psfSize, Multi_View_Deconvolution.isotropic, Multi_View_Deconvolution.psfSize3d );
	}
	
	public ExtractPSF( final ViewStructure viewStructure )
	{		
		this.viewStructure = viewStructure;
		this.pointSpreadFunctions = new ArrayList<Image<FloatType>>();
		this.originalPSFs = new ArrayList<Image<FloatType>>();
		this.conf = viewStructure.getSPIMConfiguration();
		
		setPSFSize( Multi_View_Deconvolution.psfSize, Multi_View_Deconvolution.isotropic, Multi_View_Deconvolution.psfSize3d );
	}
	
	/**
	 * Defines the size of the PSF that is extracted
	 * 
	 * @param size - number of pixels in xy
	 * @param isotropic - if isotropic, than same size applies to z (in px), otherwise it is divided by half the z-stretching
	 */
	public void setPSFSize( final int size, final boolean isotropic, final int[] size3d )
	{
		this.size = size;
		this.isotropic = isotropic;
		this.size3d = size3d;
	}
	
	/**
	 * @return - the extracted PSFs after applying the transformations of each view
	 */
	public ArrayList< Image< FloatType > > getPSFs() { return pointSpreadFunctions; }

	/**
	 * @return - the extracted PSFs in original calibration for each view
	 */
	public ArrayList< Image< FloatType > > getPSFsInInputCalibration() { return originalPSFs; }

	/**
	 * @return - the average extracted PSF after applying the transformations of each view
	 */
	public Image< FloatType > getAveragePSF() { return avgPSF; }
	
	/**
	 * @return - the average extracted PSF in the same calibration as the input data
	 */
	public Image< FloatType > getAverageOriginalPSF() { return avgOriginalPSF; }
	
	/**
	 * Get projection along the smallest dimension (which is usually the rotation axis)
	 * 
	 * @return - the averaged, projected PSF
	 */
	public Image< FloatType > getMaxProjectionAveragePSF()
	{
		final int[] dimensions = avgPSF.getDimensions();
		
		int minSize = dimensions[ 0 ];
		int minDim = 0;
		
		for ( int d = 0; d < dimensions.length; ++d )
		{
			if ( avgPSF.getDimension( d ) < minSize )
			{
				minSize = avgPSF.getDimension( d );
				minDim = d;
			}
		}
		
		final int[] projDim = new int[ dimensions.length - 1 ];
		
		int dim = 0;
		int sizeProjection = 0;
		
		// the new dimensions
		for ( int d = 0; d < dimensions.length; ++d )
			if ( d != minDim )
				projDim[ dim++ ] = dimensions[ d ];
			else
				sizeProjection = dimensions[ d ];
		
		final Image< FloatType > proj = avgPSF.getImageFactory().createImage( projDim );
		
		final LocalizableByDimCursor< FloatType > psfIterator = avgPSF.createLocalizableByDimCursor();
		final LocalizableCursor< FloatType > projIterator = proj.createLocalizableCursor();
		
		final int[] tmp = new int[ avgPSF.getNumDimensions() ];
		
		while ( projIterator.hasNext() )
		{
			projIterator.fwd();

			dim = 0;
			for ( int d = 0; d < dimensions.length; ++d )
				if ( d != minDim )
					tmp[ d ] = projIterator.getPosition( dim++ );

			tmp[ minDim ] = -1;
			
			float maxValue = -Float.MAX_VALUE;
			
			psfIterator.setPosition( tmp );
			for ( int i = 0; i < sizeProjection; ++i )
			{
				psfIterator.fwd( minDim );
				final float value = psfIterator.getType().get();
				
				if ( value > maxValue )
					maxValue = value;
			}
			
			projIterator.getType().set( maxValue );
		}
		
		proj.setName( "MIP of PSF's of " + viewStructure.getID() );
		
		return proj;
	}
	
	/**
	 * compute the average psf in original calibration and after applying the transformations
	 * 
	 * @param maxSize
	 */
	public void computeAveragePSF( final int[] maxSize )
	{
		final int numDimensions = maxSize.length;
		
		IJ.log( "maxSize: " + Util.printCoordinates( maxSize ) );
		
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
		avgPSF.setName( "PSF's of " + viewStructure.getID() );

		avgOriginalPSF = originalPSFs.get( 0 ).createNewImage();

		try
		{		
			for ( final Image<FloatType> psf : originalPSFs )
			{
				final Cursor< FloatType > cursor = psf.createCursor();
				
				for ( final FloatType t : avgOriginalPSF )
					t.add( cursor.next() );
			}
		}
		catch (Exception e) 
		{
			IOFunctions.printErr( "Input PSFs were most likely of different size ... not computing average image in original scale." );
			e.printStackTrace();
		}
		
		avgOriginalPSF.getDisplay().setMinMax();
		avgOriginalPSF.setName( "(original scale) PSF's of " + viewStructure.getID() );
	}

	public void extract( final int viewID, final int[] maxSize )
	{
		final ArrayList<ViewDataBeads > views = viewStructure.getViews();
		final int numDimensions = 3;
		
		final int[] size;
		
		if ( this.size3d == null )
		{
			size = Util.getArrayFromValue( this.size, numDimensions );
			if ( !this.isotropic )
			{
				size[ numDimensions - 1 ] *= Math.max( 1, 5.0/views.get( 0 ).getZStretching() );
				if ( size[ numDimensions - 1 ] % 2 == 0 )
					size[ numDimensions - 1 ]++;
			}
		}
		else
		{
			size = this.size3d.clone();
		}
		
		IJ.log ( "PSF size: " + Util.printCoordinates( size ) );
		
		final ViewDataBeads view = views.get( viewID );		
			
		final Image<FloatType> originalPSF = extractPSF( view, size );
		final Image<FloatType> psf = transformPSF( originalPSF, (AbstractAffineModel3D<?>)view.getTile().getModel() );
		psf.setName( "PSF_" + view.getName() );
		
		for ( int d = 0; d < numDimensions; ++d )
			if ( psf.getDimension( d ) > maxSize[ d ] )
				maxSize[ d ] = psf.getDimension( d );
		
		pointSpreadFunctions.add( psf );
		originalPSFs.add( originalPSF );
		
		psf.getDisplay().setMinMax();
	}

	public void extract()
	{
		final ArrayList<ViewDataBeads > views = viewStructure.getViews();
		final int numDimensions = 3;
		
		final int[] size;
		if ( this.size3d == null )
		{
			size = Util.getArrayFromValue( this.size, numDimensions );
		
			if ( !this.isotropic )
			{
				size[ numDimensions - 1 ] *= Math.max( 1, 5.0/views.get( 0 ).getZStretching() );
				if ( size[ numDimensions - 1 ] % 2 == 0 )
					size[ numDimensions - 1 ]++;
			}
		}
		else
		{
			size = this.size3d.clone();
		}
		
		IJ.log ( "PSF size: " + Util.printCoordinates( size ) );
		
		final int[] maxSize = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			maxSize[ d ] = 0;
		
		for ( final ViewDataBeads view : views )		
		{
			final Image<FloatType> originalPSF = extractPSF( view, size );
			final Image<FloatType> psf = transformPSF( originalPSF, (AbstractAffineModel3D<?>)view.getTile().getModel() );
			psf.setName( "PSF_" + view.getName() );
			
			for ( int d = 0; d < numDimensions; ++d )
				if ( psf.getDimension( d ) > maxSize[ d ] )
					maxSize[ d ] = psf.getDimension( d );
			
			pointSpreadFunctions.add( psf );
			originalPSFs.add( originalPSF );
			
			psf.getDisplay().setMinMax();
		}
		
		computeAveragePSF( maxSize );
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
		
		// Mirror produces some artifacts ...
		final OutOfBoundsStrategyFactory<FloatType> outside = new OutOfBoundsStrategyPeriodicFactory<FloatType>();
		final InterpolatorFactory<FloatType> interpolatorFactory = new LinearInterpolatorFactory<FloatType>( outside );
		
		final ImageFactory<FloatType> imageFactory = new ImageFactory<FloatType>( new FloatType(), view.getViewStructure().getSPIMConfiguration().imageFactory );
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
		
		// TODO: Remove
		//ImageJFunctions.show( psf );
		
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

	/**
	 * Make image the same size as defined, center it
	 * 
	 * @param img
	 * @return
	 */
	public static Image< FloatType > makeSameSize( final Image< FloatType > img, final int[] sizeIn )
	{
		final int[] size = sizeIn.clone();

		float min = Float.MAX_VALUE;

		for ( final FloatType f : img )
			min = Math.min( min, f.get() );
		
		final Image< FloatType > square = img.createNewImage( size );
		
		final LocalizableCursor< FloatType > squareCursor = square.createLocalizableCursor();
		final LocalizableByDimCursor< FloatType > inputCursor = img.createLocalizableByDimCursor( new OutOfBoundsStrategyValueFactory<FloatType>( new FloatType( min ) ) );
		
		while ( squareCursor.hasNext() )
		{
			squareCursor.fwd();
			squareCursor.getPosition( size );
			
			for ( int d = 0; d < img.getNumDimensions(); ++d )
				size[ d ] =  size[ d ] - square.getDimension( d )/2 + img.getDimension( d )/2;

			inputCursor.setPosition( size );
			squareCursor.getType().set( inputCursor.getType().get() );
		}
		
		return square;
	}
	
	/**
	 * Returns the bounding box so that all images can fit in there
	 * or null if input is null or input.size is 0
	 * 
	 * @param images
	 * @return
	 */
	public static < T extends Type< T > > int[] commonSize( final List< Image< T > > images )
	{
		if ( images == null || images.size() == 0 )
			return null;
		
		int[] size = images.get( 0 ).getDimensions();
		
		for ( final Image< ? > image : images )
			for ( int d = 0; d < image.getNumDimensions(); ++d )
				size[ d ] = Math.max( size[ d ], image.getDimension( d ) );
		
		return size;
	}

	public static ExtractPSF loadAndTransformPSF( final ArrayList< String > fileName, final boolean transformPSFs, final ViewStructure viewStructure )
	{
		ExtractPSF extractPSF = new ExtractPSF( viewStructure );
		
		final ArrayList<ViewDataBeads > views = viewStructure.getViews();
		final int numDimensions = 3;
				
		final int[] maxSize = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
			maxSize[ d ] = 0;

		int i = 0;
		for ( final ViewDataBeads view : views )		
		{
	        // extract the PSF for this one	        
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
    			IOFunctions.println( "Loading PSF file '" + fileName.get( i ) + "' for " + view.getName() );

			final Image< FloatType > psfImage = LOCI.openLOCIFloatType( fileName.get( i ), viewStructure.getSPIMConfiguration().imageFactory );
			
			if ( psfImage == null )
			{
				IJ.log( "Could not find PSF file '" + fileName.get( i ) + "' - quitting." );
				return null;
			}
			
			++i;

			final Image<FloatType> psf;
			
			if ( transformPSFs )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
					IOFunctions.println( "Transforming PSF for " + view.getName() );

				psf = transformPSF( psfImage, (AbstractAffineModel3D<?>)view.getTile().getModel() );
			}
			else
			{
				psf = psfImage.clone();
			}
			
			psf.setName( "PSF_" + view.getName() );
			
			for ( int d = 0; d < numDimensions; ++d )
				if ( psf.getDimension( d ) > maxSize[ d ] )
					maxSize[ d ] = psf.getDimension( d );
			
			extractPSF.pointSpreadFunctions.add( psf );
			extractPSF.originalPSFs.add( psfImage );
			
			psf.getDisplay().setMinMax();
		}
		
		extractPSF.computeAveragePSF( maxSize );
		
		return extractPSF;
	}
}
