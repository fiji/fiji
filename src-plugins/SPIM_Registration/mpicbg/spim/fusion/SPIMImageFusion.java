package mpicbg.spim.fusion;

import ij.ImagePlus;

import javax.vecmath.Point3f;

import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public abstract class SPIMImageFusion
{
	protected SPIMConfiguration conf;
	protected ArrayList <ViewDataBeads> views;
	final protected int numViews;
	final protected ArrayList <IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories;
	final protected ArrayList <CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories;
	protected Point3f min = null, max = null, size = null;	
	protected int cropOffsetX, cropOffsetY, cropOffsetZ, imgW, imgH, imgD, scale;
	
	final protected ViewStructure viewStructure;
		
	public SPIMImageFusion( ViewStructure viewStructure, ViewStructure referenceViewStructure, 
			 			    ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories, 
			 			    ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		this.conf = viewStructure.getSPIMConfiguration();
		this.viewStructure = viewStructure;
		this.views = viewStructure.getViews();
		this.numViews = viewStructure.getNumViews();
		this.scale = conf.scale;
		this.isolatedWeightenerFactories = isolatedWeightenerFactories;
		this.combinedWeightenerFactories = combinedWeightenerFactories;
		
		// compute the final image size
		computeFinalImageSize( referenceViewStructure.getViews() );
		
		// compute cropped image size
		initFusion();
	}
	
	public abstract void fuseSPIMImages();	
	public abstract Image<FloatType> getFusedImage();
	
	public ImagePlus getFusedImageCopy() { return ImageJFunctions.copyToImagePlus( getFusedImage() ); } 
	public ImagePlus getFusedImageVirtual() { return ImageJFunctions.displayAsVirtualStack( getFusedImage() ); } 
	public void closeImages() { getFusedImage().close(); }
	public boolean saveAsTiffs( final String dir, final String name) { return ImageJFunctions.saveAsTiffs( getFusedImage(), dir, name, ImageJFunctions.GRAY32 ); }  

	
	public Point3f getOutputImageMinCoordinate() { return min; }
	public Point3f getOutputImageMaxCoordinate() { return max; }
	public Point3f getOutputImageSize() { return size; }

	protected void initFusion()
	{
		cropOffsetX = conf.cropOffsetX/scale;
		cropOffsetY = conf.cropOffsetY/scale;
		cropOffsetZ = conf.cropOffsetZ/scale;

		if (conf.cropSizeX == 0)
			imgW = (Math.round((float)Math.ceil(size.x)) + 1)/scale;
		else
			imgW = conf.cropSizeX/scale;
		
		if (conf.cropSizeY == 0)
			imgH = (Math.round((float)Math.ceil(size.y)) + 1)/scale;
		else
			imgH = conf.cropSizeY/scale;

		if (conf.cropSizeZ == 0)
			imgD = (Math.round((float)Math.ceil(size.z)) + 1)/scale;
		else
			imgD = conf.cropSizeZ/scale;	
	}
	
	public void computeFinalImageSize( final ArrayList <ViewDataBeads> views )
	{
		min = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		max = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		size = new Point3f();
	
		computeImageSize( views, min, max, size, conf.scale, conf.cropSizeX, conf.cropSizeY, conf.cropSizeZ, viewStructure.getDebugLevel() );
	}
		
	public static void computeImageSize( final ArrayList <ViewDataBeads> views, final Point3f min, final Point3f max, final Point3f size, final int scale, 
									     final int cropSizeX, final int cropSizeY, final int cropSizeZ, final int debugLevel )
	{
		min.x = Float.MAX_VALUE;
		min.y = Float.MAX_VALUE;
		min.z = Float.MAX_VALUE;
		
		max.x = -Float.MAX_VALUE;
		max.y = -Float.MAX_VALUE;
		max.z = -Float.MAX_VALUE;
				
		for ( final ViewDataBeads view : views )
		{
			if ( view.getViewErrorStatistics().getNumConnectedViews() <= 0 && view.getViewStructure().getNumViews() > 1 )
			{
				if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.printErr( "Cannot use view " + view + ", it is not connected to any other view!" );
				continue;
			}
			else if ( view.getViewStructure().getNumViews() == 1 )
			{
				if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.printErr( "Warning: Only one view given: " + view );				
			}
			
			final int[] dim = view.getImageSize();			
			
			final float[] minCoordinate = new float[]{ 0, 0, 0 };
			final float[] maxCoordinate = new float[]{ dim[0], dim[1], dim[2] };
			
			view.getTile().getModel().estimateBounds( minCoordinate, maxCoordinate );

			if ( minCoordinate[ 0 ] < min.x ) min.x = minCoordinate[ 0 ];
			if ( minCoordinate[ 1 ] < min.y ) min.y = minCoordinate[ 1 ];
			if ( minCoordinate[ 2 ] < min.z ) min.z = minCoordinate[ 2 ];
			
			if ( maxCoordinate[ 0 ] > max.x) max.x = maxCoordinate[ 0 ];
			if ( maxCoordinate[ 1 ] > max.y) max.y = maxCoordinate[ 1 ];
			if ( maxCoordinate[ 2 ] > max.z) max.z = maxCoordinate[ 2 ];
			
			/*Point3f points[] = new Point3f[8];
			points[0] = new Point3f(0, 0, 0);
			points[1] = new Point3f((dim[0] - 1), 0, 0);
			points[2] = new Point3f((dim[0] - 1), (dim[1] - 1), 0);
			points[3] = new Point3f(0, (dim[1] - 1), 0);

			points[4] = new Point3f(0, 0, (dim[2] - 1));
			points[5] = new Point3f((dim[0] - 1), 0, (dim[2] - 1));
			points[6] = new Point3f((dim[0] - 1), (dim[1] - 1), (dim[2] - 1));
			points[7] = new Point3f(0, (dim[1] - 1) , (dim[2] - 1));
			
			// transform the points
			for (Point3f point : points)
				view.transformation.transform(point);

			for (Point3f point : points)
			{
				if (point.x < min.x) min.x = point.x;
				if (point.y < min.y) min.y = point.y;
				if (point.z < min.z) min.z = point.z;
				
				if (point.x > max.x) max.x = point.x;
				if (point.y > max.y) max.y = point.y;
				if (point.z > max.z) max.z = point.z;
			}*/
		}
		
		size.sub(max, min);
		
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
		{
			IOFunctions.println("Dimension of final output image:");
			IOFunctions.println("From : " + min + " to " + max);
			double ram = (4l * size.x * size.y * size.z)/(1024l * 1024l);
			IOFunctions.println("Size: " + size + " needs " + Math.round( ram ) + " MB of RAM" );
			
			if ( scale != 1 )
			{
				ram = (4l * size.x/scale * size.y/scale * size.z/scale)/(1024l * 1024l);
				IOFunctions.println("Scaled size("+scale+"): (" + Math.round(size.x/scale) + ", " + Math.round(size.y/scale) + ", " + Math.round(size.z/scale) + ") needs " + Math.round( ram ) + " MB of RAM" );				
			}
			
			if ( cropSizeX > 0 && cropSizeY > 0 && cropSizeZ > 0)
			{
				if (scale != 1 )
					IOFunctions.println("Cropped & scaled("+scale+") image size: " + cropSizeX/scale + "x" + cropSizeY/scale + "x" + cropSizeZ/scale);
				else
					IOFunctions.println("Cropped image size: " + cropSizeX + "x" + cropSizeY + "x" + cropSizeZ);
				
				ram = (4l * cropSizeX/scale * cropSizeY/scale * cropSizeZ/scale)/(1024l * 1024l);
				IOFunctions.println("Needs " + Math.round( ram ) + " MB of RAM");
			}
		}
	}
	
}
