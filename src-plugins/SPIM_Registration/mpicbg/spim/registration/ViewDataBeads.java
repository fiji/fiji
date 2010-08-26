package mpicbg.spim.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;

import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AffineModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.mpicbg.TileSPIM;
import mpicbg.spim.registration.bead.BeadStructure;
import mpicbg.spim.registration.bead.error.ViewErrorStatistics;
import mpicbg.spim.registration.segmentation.Nucleus;

public class ViewDataBeads
{
	public ViewDataBeads( final int id, final AffineModel3D model, final String fileName, final double zStretching )
	{		
		setID( id );
		setFileName( fileName );
		setZStretching( zStretching );
		
		this.tile = new TileSPIM( model.clone(), this );
		this.beads = new BeadStructure();
	}
		
	/**
	 * if the view is connected to any other view or excluded from the registration because no true correspondences were found
	 * @return true if it is connected, otherwise false
	 */
	public boolean isConnected() { return getViewErrorStatistics().getNumConnectedViews() > 0; }
		
	/**
	 * The structure of the views that will be aligned
	 */
	protected ViewStructure viewStructure = null;
	protected void setViewStructure( final ViewStructure viewStructure ) { this.viewStructure = viewStructure; }
	public ViewStructure getViewStructure() { return viewStructure; }
	public int getNumViews() { return getViewStructure().getViews().size(); }
	
	/**
	 * The Object for storing the potential beads
	 */
	protected BeadStructure beads;
	public BeadStructure getBeadStructure() { return beads; }
	public void setBeadStructure( final BeadStructure beads ) { this.beads = beads; }
	
	/**
	 * The object for storing View-related error statistics
	 */
	protected ViewErrorStatistics viewError = null;
	public void initErrorStatistics() { this.viewError = new ViewErrorStatistics( this ); }
	public ViewErrorStatistics getViewErrorStatistics() { return viewError; }
	
	/**
	 * For segmentation, future use
	 */
	//public SegmentationProperties segmentationProperties;
	
	/**
	 * for 3d visualization
	 */
	public ArrayList<BranchGroup> branchGroups = new ArrayList<BranchGroup>();
	/**
	 * for 3d visualization
	 */
	public ArrayList<BranchGroup> beadBranchGroups = new ArrayList<BranchGroup>();

	/**
	 * the unique(!) identification of the view
	 */
	protected int id;
	public int getID() { return id; }
	public void setID( final int id ){ this.id = id; }

	/**
	 * the acquisition angle
	 */
	protected int angle;
	public int getAcqusitionAngle() { return angle; }
	public void setAcqusitionAngle( final int angle ){ this.angle = angle; }
	
	/**
	 * The file name of the current view
	 */
	private String fileName;
	public String getFileName() { return fileName; }
	public void setFileName( final String fileName ) 
	{ 
		this.fileName = fileName;
		this.shortName = IOFunctions.getShortName( fileName );
	}
	
	/**
	 * The short version of the file name of the current view
	 */
	private String shortName;
	public String getName() { return shortName; }
	public void setName( String shortName ) { this.shortName = shortName; }
			
	/**
	 * The tile of this view which is responsible for the global optimization 
	 */
	final private TileSPIM tile;
	public TileSPIM getTile() { return tile; }

	/**
	 * Returns the affine transformation of this {@link ViewDataBeads} as Java3D {@link Transform3D}
	 * @return - the transform
	 */
	public Transform3D getTransform3D()
	{
		final Matrix4f matrix = new Matrix4f();
		final float[] m = getTile().getModel().getMatrix( null );
		
		matrix.m00 = m[ 0 ];
		matrix.m01 = m[ 1 ];
		matrix.m02 = m[ 2 ];
		matrix.m03 = m[ 3 ];
		matrix.m10 = m[ 4 ];
		matrix.m11 = m[ 5 ];
		matrix.m12 = m[ 6 ];
		matrix.m13 = m[ 7 ];
		matrix.m20 = m[ 8 ];
		matrix.m21 = m[ 9 ];
		matrix.m22 = m[ 10 ];
		matrix.m23 = m[ 11 ];
		matrix.m30 = 0;
		matrix.m31 = 0;
		matrix.m32 = 0;
		matrix.m33 = 1;
			
		return new Transform3D( matrix );
	}
			
	/**
	 * The z-stretching of the input stack
	 */
	private double zStretching = 1;
	public double getZStretching() { return zStretching; }
	public void setZStretching( final double zStretching ) { this.zStretching = zStretching; }

	/**
	 * The size of the input stack
	 */
	protected int[] imageSize = null;	
	public int[] getImageSize() 
	{
		if ( imageSize == null )
			loadDimensions();
		
		return imageSize.clone();	
	}
	public void setImageSize( final int[] size ) { this.imageSize = size; }

	/**
	 * The input image
	 */
	private Image<FloatType> image = null;
	private float maxValue = 0;

	/**
	 * Gets the number of dimensions from view 1
	 * @return - the number of dimensions
	 */
	public int getNumDimensions() { return getImageSize().length; }
	public float getMaxValueUnnormed() { return maxValue; }
	
	/**
	 * The link to the input image of this view
	 * @return the link or null unable to open
	 */
	public Image<FloatType> getImage() 
	{
		return getImage( getViewStructure().getSPIMConfiguration().imageFactory );
	}
	
	/**
	 * The currently downsampled image cached 
	 */
	private Image<FloatType> downSampledImage = null;
	private int currentDownSamplingFactor = -1;

	public Image<FloatType> getDownSampledImage( final int downSamplingFactor )
	{
		// if there is no downsampling we just return the image as is
		if ( downSamplingFactor == 1 )
			return getImage();
		
		// we have the image already here
		if ( downSampledImage != null && downSamplingFactor == currentDownSamplingFactor )
			return downSampledImage;
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( "Computing " + downSamplingFactor + "x Downsampling for " + getName() );
		
		currentDownSamplingFactor = downSamplingFactor;
		final Image<FloatType> img = getImage();
		
		final DownSample<FloatType> downSample = new DownSample<FloatType>( img, 1.0f / downSamplingFactor );
		if ( !downSample.checkInput() || !downSample.process() )
		{
			IOFunctions.println("Error, cannot downSample image: " + downSample.getErrorMessage() );
			return null;
		}
		
		downSampledImage = downSample.getResult();
		
		return downSampledImage;
	}

	/**
	 * The link to the input image of this view
	 * @return the link or null unable to open
	 */
	public Image<FloatType> getImage( final ContainerFactory imageFactory ) 
	{
		if ( image == null)
		{
			//TODO: remove that crap later
			if ( getName().contains( "Angle72" ) )
				image = LOCI.openLOCIFloatType( "", getFileName(), imageFactory, 0, 162 );
			else
				image = LOCI.openLOCIFloatType( getFileName(), imageFactory );
			
			if ( image == null )
				System.exit( 0 );
			
			image.setName( getName() );
						
			maxValue = normalizeImage( image );
			setImageSize( image.getDimensions() );
		}
		
		return image;
	}
	
	protected ArrayList<Nucleus> nuclei;
	public void setNucleiList( final Collection<Nucleus> nuclei ) 
	{ 
		this.nuclei = new ArrayList<Nucleus>();
		
		for ( final Nucleus nucleus : nuclei )
			this.nuclei.add( nucleus );
	}
	public ArrayList<Nucleus> getNucleiList() { return this.nuclei; }
	
	/**
	 * Normalizes the image to the range [0...1]
	 * @param image - the image to normalize
	 */
	public static float normalizeImage( final Image<FloatType> image )
	{
		image.getDisplay().setMinMax();
		
		final float min = (float)image.getDisplay().getMin();
		final float max = (float)image.getDisplay().getMax();
		final float diff = max - min;
		
		if ( Float.isNaN( diff ) || Float.isInfinite(diff) || diff == 0 )
		{
			IOFunctions.println("Cannot normalize image " + image.getName() + ", min=" + min + "  + max=" + max );
			return max;
		}
		
		final Cursor<FloatType> cursor = image.createCursor();
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			
			final float value = cursor.getType().get();
			final float norm = (value - min) / diff;
			
			cursor.getType().set( norm );
		}
		
		image.getDisplay().setMinMax(0, 1);
		
		return max;
	}
	
	/**
	 * Closes the input image stack of this view
	 */
	public void closeImage() 
	{
		if ( image != null )
		{
			image.close();
			image = null;
		}
	}
		
	@Override
	public String toString() { return getName() + " (id = " + getID() + ")"; }
	
	/**
	 * Loads the registration matrix and the errors from the *.registration files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadRegistration()
	{
		if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading " + this + " registration");
		
		String dir = getViewStructure().getSPIMConfiguration().registrationFiledirectory;
		boolean readReg = IOFunctions.readRegistration( this, dir + getName() + ".registration" );
		
		if ( !readReg )
		{
			if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Cannot read registration for " + this );
			return false;
		}		
		
		return true;
	}

	/**
	 * Loads the registration matrix and the errors relative to a reference timepoint from the *.registration files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadRegistrationTimePoint( final int referenceTimePoint )
	{
		if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading " + this + " registration relative to time point " + referenceTimePoint );
		
		String dir = getViewStructure().getSPIMConfiguration().registrationFiledirectory;
		boolean readReg = IOFunctions.readRegistration( this, dir + getName() + ".registration.to_" + referenceTimePoint );
		
		if ( !readReg )
		{
			if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Cannot read registration for " + this + " relative to time point " + referenceTimePoint );
			return false;
		}		
		
		return true;
	}

	/**
	 * Writes the registration matrix and the errors into the *.registration files in the registration file directory
	 *  
	 * @return true if successful, false otherwise
	 */
	public boolean writeRegistration()
	{
		if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Writing " + this + " registration");
		
		return IOFunctions.writeRegistration( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory );
	}

	/**
	 * Writes the registration matrix and the errors into the *.registration files in the registration file directory
	 * @param referenceTimePoint - the reference time point
	 * @return true if successful, false otherwise
	 */
	public boolean writeRegistrationTimeLapse( final int referenceTimePoint )
	{
		if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Writing " + this + " registration relative to time point " + referenceTimePoint );
		
		return IOFunctions.writeRegistration( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory, ".to_" + referenceTimePoint );
	}
	
	/**
	 * Loads the bead detections and its correspondences candidates as well as true correspondences from the *.beads.txt files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadSegmentation()
	{
		if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading " + this + " segmentation");
		
		boolean readSeg = IOFunctions.readSegmentation( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory, getViewStructure().getSPIMConfiguration() );

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
		{
			if ( getBeadStructure() != null )
				IOFunctions.println("Loaded " + getBeadStructure().getBeadList().size() + " beads for " + getName() + "[" + getImageSize()[0] + "x" + getImageSize()[1] + "x" + getImageSize()[2] + "]" );				
			else
				IOFunctions.println("Bead loading FAILED for " + getName() + "[" + getImageSize()[0] + "x" + getImageSize()[1] + "x" + getImageSize()[2] + "]" );
		}
		
		if (!readSeg)
		{
			if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Cannot read segmentation for " + this );
			return false;
		}		
		
		return true;
	}
	
	/**
	 * Writes the segmentated beads, correspondence candidates and ransac correspondences into the *.beads.txt files in the registration file directory
	 *  
	 * @return true if successful, false otherwise
	 */
	public boolean writeSegmentation()
	{
		if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Writing " + this + " registration");
		
		return IOFunctions.writeSegmentation( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory );
	}

	/**
	 * Loads the dimensions from the *.dim files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadDimensions()
	{
		if ( getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading " + this + " dimensions");
		
		boolean readDim = IOFunctions.readDim( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory );

		if ( !readDim )
		{
			if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Cannot read dimensions for " + this + ", trying to open image to determine them.");
			
			getImage();
			closeImage();
			
			if ( getImageSize() != null )
				return true;
		}
		
		return readDim;
	}
	
}
