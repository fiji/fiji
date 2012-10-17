package mpicbg.spim.registration;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;

import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.algorithm.mirror.MirrorImage;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.mpicbg.TileSPIM;
import mpicbg.spim.registration.bead.BeadStructure;
import mpicbg.spim.registration.bead.error.ViewErrorStatistics;
import mpicbg.spim.registration.segmentation.NucleusStructure;
import spimopener.SPIMExperiment;

public class ViewDataBeads implements Comparable< ViewDataBeads >
{
	/**
	 * @param <M> - an implementation of the {@link AbstractAffineModel3D}
	 * @param id - an unique id
	 * @param model - the model instance, unintitialized
	 * @param fileName - the filename (or directory with 2d planes) of the view
	 * @param zStretching - the z/xy stretching
	 */
	public <M extends AbstractAffineModel3D<M>> ViewDataBeads( final int id, final M model, final String fileName, final double zStretching )
	{
		setID( id );
		setFileName( fileName );
		setZStretching( zStretching );

		this.uninitializedModel = model.copy();
		this.tile = new TileSPIM<M>( model.copy(), this );
		this.beads = new BeadStructure();
		this.nuclei = new NucleusStructure();
	}

	/**
	 * An uninitialized {@link AbstractAffineModel3D}
	 */
	final AbstractAffineModel3D<?> uninitializedModel;
	public AbstractAffineModel3D<?> getUninitializedModel() { return uninitializedModel; }

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
	 * The Object for storing the potential nuclei
	 */
	protected NucleusStructure nuclei;
	public NucleusStructure getNucleiStructure() { return nuclei; }
	public void setNucleiStructure( final NucleusStructure nuclei ) { this.nuclei = nuclei; }

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
	 * the channel
	 */
	protected int channel = 0, channelIndex = 0;
	protected boolean useForRegistration = true;
	protected boolean useForFusion = true;
	protected float initialSigma = 1.8f;
	protected float minPeakValue = 0.01f;
	protected float minInitialPeakValue = 0.001f;

	/**
	 * the timepoint of this view
	 */
	protected int timePoint = 0;

	public int getChannel() { return channel; }
	public void setChannel( final int channel ){ this.channel = channel; }
	public int getChannelIndex() { return channelIndex; }
	public void setChannelIndex( final int channelIndex ){ this.channelIndex = channelIndex; }
	public int getTimePoint() { return timePoint; }
	public void setTimePoint( final int timePoint ){ this.timePoint = timePoint; }
	public boolean getUseForRegistration() { return useForRegistration; }
	public void setUseForRegistration( final boolean useForRegistration ) { this.useForRegistration = useForRegistration; }
	public boolean getUseForFusion() { return useForFusion; }
	public void setUseForFusion( final boolean useForFusion ) { this.useForFusion = useForFusion; }

	public void setInitialSigma( final float s ) { this.initialSigma = s; }
	public void setMinPeakValue( final float m ) { this.minPeakValue = m; this.minInitialPeakValue = m/10f;}
	public void setMinInitialPeakValue( final float m ) { this.minInitialPeakValue = m;}
	public float getInitialSigma() { return this.initialSigma; }
	public float getMinPeakValue() { return this.minPeakValue;}
	public float getMinInitialPeakValue() { return this.minInitialPeakValue;}


	/**
	 * the acquisition angle
	 */
	protected int angle;
	public int getAcqusitionAngle() { return angle; }
	public void setAcqusitionAngle( final int angle ){ this.angle = angle; }

	/**
	 * the index of the illumination direction
	 */
	protected int illumination;
	public int getIllumination() { return illumination; }
	public void setIllumination( final int illumination )	{ this.illumination = illumination; }

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
	public void setName( final String shortName ) { this.shortName = shortName; }

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
		final float[] m = ((AbstractAffineModel3D<?>)getTile().getModel()).getMatrix( null );

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
	protected double zStretching = 1;
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
	 * The offset of the input stack
	 */
	protected int[] imageSizeOffset = null;
	public int[] getImageSizeOffset()
	{
		if ( imageSizeOffset == null )
			return new int[ getImageSize().length ];

		return imageSizeOffset.clone();
	}
	public void setImageSizeOffset( final int[] imageSizeOffset ) { this.imageSizeOffset = imageSizeOffset; }

	/**
	 * The input image
	 */
	private Image<FloatType> image = null;
	private boolean isNormalized;
	private float minValue = 0;
	private float maxValue = 0;

	/**
	 * Gets the number of dimensions from view 1
	 * @return - the number of dimensions
	 */
	public int getNumDimensions() { return getImageSize().length; }
	public float getMaxValueUnnormed() { return maxValue; }

	/**
	 * The link to the input image of this view, normalized to [0...1]
	 * @return the link or null unable to open
	 */
	public Image<FloatType> getImage()
	{
		return getImage( true );
	}

	/**
	 * The link to the input image of this view
	 * @return the link or null unable to open
	 */
	public Image<FloatType> getImage( final boolean normalize )
	{
		return getImage( getViewStructure().getSPIMConfiguration().imageFactory, normalize );
	}

	/**
	 * The currently downsampled image cached
	 */
	protected Image<FloatType> downSampledImage = null;
	protected int currentDownSamplingFactor = -1;

	/**
	 * Gets a downsampled and normalized [0...1] version of the input image
	 *
	 * @param downSamplingFactor - the factor
	 */
	public Image<FloatType> getDownSampledImage( final int downSamplingFactor )
	{
		return getDownSampledImage( downSamplingFactor, true );
	}

	/**
	 * Gets a downsampled version of the input image
	 *
	 * @param downSamplingFactor - the factor
	 * @param normalize - if normalized to [0...1] or not
	 * @return
	 */
	public Image<FloatType> getDownSampledImage( final int downSamplingFactor, final boolean normalize )
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

	protected boolean mirrorVertically = false, mirrorHorizontally = false;
	public void setMirrorHorizontally( final boolean state ) { mirrorHorizontally = state; }
	public void setMirrorVertically( final boolean state ) { mirrorVertically = state; }
	public boolean getMirrorHorizontally() { return mirrorHorizontally; }
	public boolean getMirrorVertically() { return mirrorVertically; }

	/**
	 * The link to the input image of this view normalized to [0...1]
	 * @return the link or null unable to open
	 */
	public Image<FloatType> getImage( final ContainerFactory imageFactory )
	{
		return getImage( imageFactory, true );
	}

	/**
	 * The link to the input image of this view
	 * @return the link or null unable to open
	 */
	public Image<FloatType> getImage( final ContainerFactory imageFactory, final boolean normalize )
	{
		if ( image == null )
		{
			if ( getViewStructure().getSPIMConfiguration().isHuiskenFormat() )
			{
				final SPIMExperiment exp = getViewStructure().getSPIMConfiguration().getSpimExperiment();
				final int s = exp.sampleStart;
				final int r = exp.regionStart;
				final int f = exp.frameStart;
				final int zMin = exp.planeStart;
				final int zMax = exp.planeEnd;
				final int xMin = 0;
				final int xMax = exp.w - 1;
				final int yMin = 0;
				final int yMax = exp.h - 1;

				ImagePlus imp;
				if ( getViewStructure().getSPIMConfiguration().hasAlternatingIllumination() )
				{
					final int zStep = 2;
					if ( illumination == 0 )
						imp = exp.openNotProjected( s, timePoint, timePoint, r, angle, channel, zMin, zMax-1, zStep, f, f, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
					else
						imp = exp.openNotProjected( s, timePoint, timePoint, r, angle, channel, zMin+1, zMax, zStep, f, f, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
				}
				else
				{
					imp = exp.openNotProjected( s, timePoint, timePoint, r, angle, channel, zMin, zMax, f, f, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
				}
				image = ImageJFunctions.convertFloat( imp );
				image.setCalibration( new float[]{ 1, 1, (float)getViewStructure().getSPIMConfiguration().getZStretchingHuisken() } );
			}
			else
			{
				final String s = getFileName();

				//TODO: REMOVE!!!!!!
				//if ( s.contains("red-h2amcherry-nuclei") )
				//	s = s.replace( "red-h2amcherry-nuclei", "green-rasgfp-membranes" ) + ".tif";

				//System.out.println( s );

				try
				{
					image = LOCI.openLOCIFloatType( s, imageFactory );
				}
				catch ( Exception e )
				{
					image = null;
				}
				
				if ( image == null )
				{
					IJ.log( "Cannot open file: " + s );
					
					File f = new File( s );
					if ( f.exists() )
					{
						IJ.log( "File: " + f.getAbsolutePath() + " exists, trying to open with CellImg." );
						image = LOCI.openLOCIFloatType( s, new CellContainerFactory( 256 ) );
						
						if ( image == null )
						{
							IJ.log( "Opening file: " + f.getAbsolutePath() + " with CellImg failed, too." );
							return null;
						}
					}
					else
					{
						IJ.log( "File does not exist: " + f.getAbsolutePath() );
						return null;	
					}
				}
			}

			// set different calibration if override is activated
			if ( getViewStructure().getSPIMConfiguration().overrideImageZStretching )
				image.setCalibration( new float[]{ 1, 1, (float)getViewStructure().getSPIMConfiguration().zStretching } );

			if ( getMirrorHorizontally() )
			{
				IOFunctions.println( "Mirroring horizontally: " + this );
				final MirrorImage<FloatType> mirror = new MirrorImage<FloatType>( image, 0 );
				mirror.process();
			}

			if ( getMirrorVertically() )
			{
				IOFunctions.println( "Mirroring vertically: " + this );
				final MirrorImage<FloatType> mirror = new MirrorImage<FloatType>( image, 1 );
				mirror.process();
			}

			image.setName( getName() );

			if ( normalize )
			{
				final float[] minmax = normalizeImage( image );
				minValue = minmax[ 0 ];
				maxValue = minmax[ 1 ];
				isNormalized = true;
			}
			else
			{
				image.getDisplay().setMinMax();
				minValue = (float)image.getDisplay().getMin();
				maxValue = (float)image.getDisplay().getMax();
				isNormalized = false;
			}
			setImageSize( image.getDimensions() );
			
			// now write dims for further use
			IOFunctions.writeDim( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory );
		}
		
		if ( isNormalized && !normalize)
			unnormalizeImage();

		if ( !isNormalized && normalize)
			normalizeImage();

		return image;
	}

	public void unnormalizeImage()
	{
		if ( image == null )
		{
			getImage( false );
		}
		else if ( isNormalized )
		{
			final float diff = maxValue - minValue;

			for ( final FloatType f : image )
				f.set( f.get() * diff + minValue );

			isNormalized = false;
		}
	}

	public void normalizeImage()
	{
		if ( image == null )
		{
			getImage( true );
		}
		else if ( !isNormalized )
		{
			final float diff = maxValue - minValue;

			for ( final FloatType f : image )
				f.set( (f.get() - minValue) / diff );

			isNormalized = true;
		}
	}

	/**
	 * Normalizes the image to the range [0...1]
	 * @param image - the image to normalize
	 */
	public static float[] normalizeImage( final Image<FloatType> image )
	{
		image.getDisplay().setMinMax();

		final float min = (float)image.getDisplay().getMin();
		final float max = (float)image.getDisplay().getMax();
		final float diff = max - min;

		if ( Float.isNaN( diff ) || Float.isInfinite(diff) || diff == 0 )
		{
			IOFunctions.println("Cannot normalize image " + image.getName() + ", min=" + min + "  + max=" + max );
			return new float[]{ min, max };
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

		return new float[]{ min, max };
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

		final String dir = getViewStructure().getSPIMConfiguration().registrationFiledirectory;
		final boolean readReg = IOFunctions.readRegistration( this, dir + getName() + ".registration" );

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

		final String dir = getViewStructure().getSPIMConfiguration().registrationFiledirectory;
		final boolean readReg = IOFunctions.readRegistration( this, dir + getName() + ".registration.to_" + referenceTimePoint );

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

		final boolean readSeg = IOFunctions.readSegmentation( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory, getViewStructure().getSPIMConfiguration() );

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
		{
			if ( getBeadStructure() != null )
				IOFunctions.println("Loaded " + getBeadStructure().getDetectionList().size() + " beads for " + getName() + "[" + getImageSize()[0] + "x" + getImageSize()[1] + "x" + getImageSize()[2] + "]" );
			else
				IOFunctions.println("Detection loading FAILED for " + getName() + "[" + getImageSize()[0] + "x" + getImageSize()[1] + "x" + getImageSize()[2] + "]" );
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

		final boolean readDim = IOFunctions.readDim( this, getViewStructure().getSPIMConfiguration().registrationFiledirectory );

		if ( !readDim )
		{
			if ( getViewStructure().debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Cannot read dimensions for " + this + ", trying to open image to determine them.");

			if ( getImage() == null )
				return false;

			closeImage();

			if ( getImageSize() != null )
				return true;
		}

		return readDim;
	}
	@Override
	public int compareTo( final ViewDataBeads o )
	{
		if ( getID() < o.getID() )
			return -1;
		else if ( o.getID() == getID() )
			return 0;
		else
			return 1;
	}

}
