package mpicbg.ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.SpringMesh;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.Vertex;
import net.imglib2.RealPoint;
import net.imglib2.collection.KDTree;
import net.imglib2.collection.RealPointSampleList;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.ARGBType;

public class BlockMatching_ExtractPoinRoi extends AbstractBlockMatching
{
	protected ImagePlus imp1;
	protected ImagePlus imp2;
	
	static protected boolean exportPointRoi = true;
	static protected boolean exportDisplacementVectors = false;
	
	final protected boolean setup()
	{
		final int[] ids = WindowManager.getIDList();
		if ( ids == null || ids.length < 2 )
		{
			IJ.showMessage( "You should have at least two images open." );
			return false;
		}
		
		final String[] titles = new String[ ids.length ];
		for ( int i = 0; i < ids.length; ++i )
		{
			titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
		}
		
		
		final GenericDialog gdBlockMatching = new GenericDialog( "Block Matching" );
				
		gdBlockMatching.addMessage( "Image Selection:" );
		final String current = WindowManager.getCurrentImage().getTitle();
		gdBlockMatching.addChoice( "source_image", titles, current );
		gdBlockMatching.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		
		addFields( gdBlockMatching );
		
		gdBlockMatching.addMessage( "Export:" );
		gdBlockMatching.addCheckbox( "export point correspondences", exportPointRoi );
		gdBlockMatching.addCheckbox( "export colorized displacement vectors", exportDisplacementVectors );
		
		gdBlockMatching.showDialog();
		
		if ( gdBlockMatching.wasCanceled() )
			return false;
		
		imp1 = WindowManager.getImage( ids[ gdBlockMatching.getNextChoiceIndex() ] );
		imp2 = WindowManager.getImage( ids[ gdBlockMatching.getNextChoiceIndex() ] );
		
		readFields( gdBlockMatching );
		
		exportPointRoi = gdBlockMatching.getNextBoolean();
		exportDisplacementVectors = gdBlockMatching.getNextBoolean();
		
		return true;		
	}
	
	
	@Override
	public void run( final String arg )
	{
		if ( !setup() )
			return;
		
		final SpringMesh mesh = new SpringMesh( meshResolution, imp1.getWidth(), imp1.getHeight(), 1, 1000, 0.9f );
		
		final Set< Vertex > vertices = mesh.getVertices();
		final RealPointSampleList< ARGBType > maskSamples = new RealPointSampleList< ARGBType >( 2 );
		for ( final Vertex vertex : vertices )
			maskSamples.add( new RealPoint( vertex.getL() ), new ARGBType( 0xffffffff ) );

		final ArrayList< PointMatch > pm12 = new ArrayList< PointMatch >();

		final Set< Vertex > v1 = mesh.getVertices();

		final FloatProcessor ip1 = ( FloatProcessor )imp1.getProcessor().convertToFloat().duplicate();
		final FloatProcessor ip2 = ( FloatProcessor )imp2.getProcessor().convertToFloat().duplicate();
					
		final FloatProcessor ip1Mask = createMask( ( ColorProcessor )imp1.getProcessor().convertToRGB() );
		final FloatProcessor ip2Mask = createMask( ( ColorProcessor )imp2.getProcessor().convertToRGB() );

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
			return;
		}
		catch ( final ExecutionException e )
		{
			IJ.log( "Execution Exception occured during Block Matching." );
			e.printStackTrace();
			return;
		}
			
		IJ.log( pm12.size() + " blockmatch candidates found." );
		
		if ( useLocalSmoothnessFilter )		
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
			IJ.log( pm12.size() + " blockmatch candidates passed local smoothness filter." );
		}
		
		if ( exportPointRoi )
		{
			final ArrayList< Point > pm12Sources = new ArrayList< Point >();
			final ArrayList< Point > pm12Targets = new ArrayList< Point >();
	
			PointMatch.sourcePoints( pm12, pm12Sources );
			PointMatch.targetPoints( pm12, pm12Targets );
	
			final PointRoi roi1 = mpicbg.ij.util.Util.pointsToPointRoi( pm12Sources );
			final PointRoi roi2 = mpicbg.ij.util.Util.pointsToPointRoi( pm12Targets );
	
			imp1.setRoi( roi1 );
			imp2.setRoi( roi2 );
		}
		
		if ( exportDisplacementVectors )
		{
			final ArrayList< Point > pm12Targets = new ArrayList< Point >();
			PointMatch.targetPoints( pm12, pm12Targets );
			
			final RealPointSampleList< ARGBType > maskSamples2 = new RealPointSampleList< ARGBType >( 2 );
			for ( final Point point : pm12Targets )
				maskSamples2.add( new RealPoint( point.getW() ), new ARGBType( 0xffffffff ) );

			final ImagePlusImgFactory< ARGBType > factory = new ImagePlusImgFactory< ARGBType >();
			
			final KDTree< ARGBType > kdtreeMatches = new KDTree< ARGBType >( matches2ColorSamples( pm12 ) );
			final KDTree< ARGBType > kdtreeMask = new KDTree< ARGBType >( maskSamples );
			
			
			/* nearest neighbor */
			final ImagePlusImg< ARGBType, ? > img = factory.create( new long[]{ imp1.getWidth(), imp1.getHeight() }, new ARGBType() );
			drawNearestNeighbor(
					img,
					new NearestNeighborSearchOnKDTree< ARGBType >( kdtreeMatches ),
					new NearestNeighborSearchOnKDTree< ARGBType >( kdtreeMask ) );
			
			try
			{
				img.getImagePlus().show();
			}
			catch ( final ImgLibException e )
			{
				IJ.log( "ImgLib2 Exception, vectors could not be painted." );
				e.printStackTrace();
			}
		}
	}
}
