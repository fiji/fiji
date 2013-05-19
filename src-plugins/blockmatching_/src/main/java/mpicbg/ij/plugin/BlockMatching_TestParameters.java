package mpicbg.ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.SpringMesh;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.Vertex;
import mpicbg.trakem2.util.Downsampler;
import net.imglib2.RealPoint;
import net.imglib2.collection.KDTree;
import net.imglib2.collection.RealPointSampleList;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.ARGBType;

public class BlockMatching_TestParameters extends AbstractBlockMatching
{
	final static int minGridSize = 8;
	final static boolean exportDisplacementVectors = true;
	
	protected ImagePlus imp1;
	protected ImageStack stack;
	
	final protected boolean setup()
	{
		imp1 = IJ.getImage();
		
		if ( imp1 == null )
		{
			IJ.error( "Please open an image stack first." );
			return false;
		}
		
		stack = imp1.getStack();
		if ( stack.getSize() < 2 )
		{
			IJ.error( "The image stack should contain at least two slices." );
			return false;
		}
		
		final GenericDialog gdBlockMatching = new GenericDialog( "Test Block Matching Parameters" );
		
		addFields( gdBlockMatching );
		
		gdBlockMatching.showDialog();
		
		if ( gdBlockMatching.wasCanceled() )
			return false;
		
		readFields( gdBlockMatching );
		
		return true;		
	}
	
	public ArrayList< PointMatch > match(
			final FloatProcessor ip1,
			final FloatProcessor ip2,
			final FloatProcessor ip1Mask,
			final FloatProcessor ip2Mask )
	{
		final SpringMesh mesh = new SpringMesh( meshResolution, imp1.getWidth(), imp1.getHeight(), 1, 1000, 0.9f );
		
		final ArrayList< PointMatch > pm12 = new ArrayList< PointMatch >();

		final Set< Vertex > v1 = mesh.getVertices();

		final TranslationModel2D ct = new TranslationModel2D();

		try
		{
			BlockMatching.matchByMaximalPMCC(
					ip1,
					ip2,
					ip1Mask,
					ip2Mask,
					scale,
					ct,
					blockRadius,
					blockRadius,
					searchRadius,
					searchRadius,
					minR,
					rodR,
					maxCurvatureR,
					v1,
					pm12,
					new ErrorStatistic( 1 ) );
		}
		catch ( final InterruptedException e )
		{
			IJ.log( "Block Matching interrupted." );
			return pm12;
		}
		catch ( final ExecutionException e )
		{
			IJ.log( "Execution Exception occured during Block Matching." );
			e.printStackTrace();
			return pm12;
		}
		
		return pm12;
	}
	
	protected void filter( final ArrayList< PointMatch > pm12 )
	{
		final Model< ? > model = mpicbg.trakem2.align.Util.createModel( localModelIndex );
		try
		{
			model.localSmoothnessFilter( pm12, pm12, localRegionSigma, maxLocalEpsilon, maxLocalTrust );
		}
		catch ( final NotEnoughDataPointsException e )
		{
			pm12.clear();
		}
		catch ( final IllDefinedDataPointsException e )
		{
			pm12.clear();
		}
	}
	
	protected void display( final ArrayList< PointMatch > pm12, final RealPointSampleList< ARGBType > maskSamples, final ImagePlus impTable, final ColorProcessor ipTable, final int w, final int h, final int i, final int j )
	{
		
		if ( pm12.size() > 0 )
		{
			final ImagePlusImgFactory< ARGBType > factory = new ImagePlusImgFactory< ARGBType >();
			
			final KDTree< ARGBType > kdtreeMatches = new KDTree< ARGBType >( matches2ColorSamples( pm12 ) );
			final KDTree< ARGBType > kdtreeMask = new KDTree< ARGBType >( maskSamples );
			
			/* nearest neighbor */
			final ImagePlusImg< ARGBType, ? > img = factory.create( new long[]{ imp1.getWidth(), imp1.getHeight() }, new ARGBType() );
			drawNearestNeighbor(
					img,
					new NearestNeighborSearchOnKDTree< ARGBType >( kdtreeMatches ),
					new NearestNeighborSearchOnKDTree< ARGBType >( kdtreeMask ) );
			
			final ImagePlus impVis;
			ColorProcessor ipVis;
			try
			{
				impVis = img.getImagePlus();
				ipVis = ( ColorProcessor )impVis.getProcessor();
				while ( ipVis.getWidth() > meshResolution * minGridSize )
					ipVis = Downsampler.downsampleColorProcessor( ipVis );
				ipTable.copyBits( ipVis, i * w + w, j * h + h, Blitter.COPY );
				impTable.updateAndDraw();
			}
			catch ( final ImgLibException e )
			{
				IJ.log( "ImgLib2 Exception, vectors could not be painted." );
				e.printStackTrace();
			}
		}
		else
		{
			final Roi roi = new Roi( i * w + w, j * h + h, w, h );
			final Color c = IJ.getInstance().getForeground();
			ipTable.setColor( Color.WHITE );
			ipTable.fill( roi );
			ipTable.setColor( c );
		}
	}

	
	@Override
	public void run( final String arg )
	{
		if ( !setup() )
			return;
		
		int w = stack.getWidth();
		int h = stack.getHeight();
		while ( w > meshResolution * minGridSize )
		{
			w /= 2;
			h /= 2;
		}
		
		final ImagePlus impTable;
		final ColorProcessor ipTable;
		if ( exportDisplacementVectors )
		{
			ipTable = new ColorProcessor( w * stack.getSize() + w, h * stack.getSize() + h );
			
			final ColorProcessor ipScale = new ColorProcessor( w, h );
			final Color c = IJ.getInstance().getForeground();
			ipScale.setColor( Color.WHITE );
			ipScale.fill();
			ipScale.setColor( c );
			mpicbg.ij.util.Util.colorCircle( ipScale );
			
			ipTable.copyBits( ipScale, 0, 0, Blitter.COPY );
		
			for ( int i = 0; i < stack.getSize(); ++i )
			{
				ColorProcessor ip = ( ColorProcessor )stack.getProcessor( i + 1 ).convertToRGB();
				while ( ip.getWidth() > w )
					ip = Downsampler.downsampleColorProcessor( ip );
				ipTable.copyBits( ip, i * w + w, 0, Blitter.COPY );
				ipTable.copyBits( ip, 0, i * h + h, Blitter.COPY );
			}
			impTable = new ImagePlus( "Block Matching Results", ipTable );
			impTable.show();
		}
		else
		{
			impTable = null;
			ipTable = null;
		}
		
		final SpringMesh mesh = new SpringMesh( meshResolution, imp1.getWidth(), imp1.getHeight(), 1, 1000, 0.9f );
		final Set< Vertex > vertices = mesh.getVertices();
		final RealPointSampleList< ARGBType > maskSamples = new RealPointSampleList< ARGBType >( 2 );
		for ( final Vertex vertex : vertices )
			maskSamples.add( new RealPoint( vertex.getL() ), new ARGBType( 0xffffffff ) );
		
		for ( int i = 0; i < stack.getSize(); ++i )
			for ( int j = i + 1; j < stack.getSize(); ++j )
			{
				final FloatProcessor ip1 = ( FloatProcessor )stack.getProcessor( i + 1 ).convertToFloat().duplicate();
				final FloatProcessor ip2 = ( FloatProcessor )stack.getProcessor( j + 1 ).convertToFloat().duplicate();
					
				final FloatProcessor ip1Mask = createMask( ( ColorProcessor )stack.getProcessor( i + 1 ).convertToRGB() );
				final FloatProcessor ip2Mask = createMask( ( ColorProcessor )stack.getProcessor( j + 1 ).convertToRGB() );
				
				final ArrayList< PointMatch > pm12 = match( ip1, ip2, ip1Mask, ip2Mask );
				IJ.log( i + " > " + j + " " + pm12.size() + " blockmatch candidates found." );
				
				if ( useLocalSmoothnessFilter )		
				{
					filter( pm12 );
					IJ.log( pm12.size() + " blockmatch candidates passed local smoothness filter." );
				}
				if ( exportDisplacementVectors )
					display( pm12, maskSamples, impTable, ipTable, w, h, i, j );
				
				
				final ArrayList< PointMatch > pm21 = match( ip2, ip1, ip2Mask, ip1Mask );
				IJ.log( i + " < " + j + " " + pm21.size() + " blockmatch candidates found." );
				
				if ( useLocalSmoothnessFilter )		
				{
					filter( pm21 );
					IJ.log( pm21.size() + " blockmatch candidates passed local smoothness filter." );
				}
				if ( exportDisplacementVectors )
					display( pm21, maskSamples, impTable, ipTable, w, h, j, i );
			}
	}
}
