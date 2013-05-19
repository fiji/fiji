package mpicbg.ij.plugin;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import mpicbg.ij.util.Util;
import mpicbg.models.PointMatch;
import mpicbg.util.Timer;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealPoint;
import net.imglib2.collection.RealPointSampleList;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;

abstract public class AbstractBlockMatching implements PlugIn
{
	static protected float scale = 1.0f;
	static protected int blockRadius = 50;
	static protected int searchRadius = 50;

	static protected float minR = 0.1f;
	static protected float rodR = 1.0f;
	static protected float maxCurvatureR = 1000.0f;

	static protected boolean useLocalSmoothnessFilter = true;
	static protected String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
	static protected int localModelIndex = 1;
	static protected float localRegionSigma = 65.0f;
	static protected float maxLocalEpsilon = 12.0f;
	static protected float maxLocalTrust = 3.0f;

	static protected int meshResolution = 24;
	
	protected void addFields( final GenericDialog gd )
	{
		gd.addMessage( "Block Matching:" );
		gd.addNumericField( "layer_scale :", scale, 2 );
		gd.addNumericField( "search_radius :", searchRadius, 0, 6, "px" );
		gd.addNumericField( "block_radius :", blockRadius, 0, 6, "px" );
		gd.addNumericField( "resolution :", meshResolution, 0 );
		
		gd.addMessage( "Correlation Filters:" );
		gd.addNumericField( "minimal_PMCC_r :", minR, 2 );
		gd.addNumericField( "maximal_curvature_ratio :", maxCurvatureR, 2 );
		gd.addNumericField( "maximal_second_best_r/best_r :", rodR, 2 );
				
		gd.addMessage( "Local Smoothness Filter:" );
		gd.addCheckbox( "use_local_smoothness_filter", useLocalSmoothnessFilter );
		gd.addChoice( "approximate_local_transformation :", modelStrings, modelStrings[ localModelIndex ] );
		gd.addNumericField( "local_region_sigma:", localRegionSigma, 2, 6, "px" );
		gd.addNumericField( "maximal_local_displacement (absolute):", maxLocalEpsilon, 2, 6, "px" );
		gd.addNumericField( "maximal_local_displacement (relative):", maxLocalTrust, 2 );
	}
	
	protected void readFields( final GenericDialog gd )
	{
		scale = ( float )gd.getNextNumber();
		searchRadius = ( int )gd.getNextNumber();
		blockRadius = ( int )gd.getNextNumber();
		meshResolution = ( int )gd.getNextNumber();
		minR = ( float )gd.getNextNumber();
		maxCurvatureR = ( float )gd.getNextNumber();
		rodR = ( float )gd.getNextNumber();
		useLocalSmoothnessFilter = gd.getNextBoolean();
		localModelIndex = gd.getNextChoiceIndex();
		localRegionSigma = ( float )gd.getNextNumber();
		maxLocalEpsilon = ( float )gd.getNextNumber();
		maxLocalTrust = ( float )gd.getNextNumber();
		
		return;
	}

	final static protected FloatProcessor createMask( final ColorProcessor source )
	{
		final FloatProcessor mask = new FloatProcessor( source.getWidth(), source.getHeight() );
		final int maskColor = 0x0000ff00;
		final int[] sourcePixels = ( int[] )source.getPixels();
		final int n = sourcePixels.length;
		final float[] maskPixels = ( float[] )mask.getPixels();
		for ( int i = 0; i < n; ++i )
		{
			final int sourcePixel = sourcePixels[ i ] & 0x00ffffff;
			if ( sourcePixel == maskColor )
				maskPixels[ i ] = 0;
			else
				maskPixels[ i ] = 1;
		}
		return mask;
	}

	final static protected RealPointSampleList< ARGBType > matches2ColorSamples( final Iterable< PointMatch > matches )
	{
		final RealPointSampleList< ARGBType > samples = new RealPointSampleList< ARGBType >( 2 );
		for ( final PointMatch match : matches )
		{
			final float[] p = match.getP1().getL();
			final float[] q = match.getP2().getW();
			final float dx = ( q[ 0 ] - p[ 0 ] ) / searchRadius;
			final float dy = ( q[ 1 ] - p[ 1 ] ) / searchRadius;
			
			final int rgb = Util.colorVector( dx, dy );
			
			samples.add( new RealPoint( p ), new ARGBType( rgb ) );
		}
		return samples;
	}
	
	final static protected RealPointSampleList< ARGBType > matches2ColorSamples2( final Iterable< PointMatch > matches )
	{
		final RealPointSampleList< ARGBType > samples = new RealPointSampleList< ARGBType >( 2 );
		for ( final PointMatch match : matches )
		{
			final float[] p = match.getP1().getL();
			final float[] q = match.getP2().getW();
			final float dx = ( q[ 0 ] - p[ 0 ] ) / searchRadius;
			final float dy = ( q[ 1 ] - p[ 1 ] ) / searchRadius;
			
			final int rgb = Util.colorVector( dx, dy );
			
			samples.add( new RealPoint( q ), new ARGBType( rgb ) );
		}
		return samples;
	}
	
	final static protected < T extends Type< T > > long drawNearestNeighbor(
			final IterableInterval< T > target,
			final NearestNeighborSearch< T > nnSearchSamples,
			final NearestNeighborSearch< T > nnSearchMask )
	{
		final Timer timer = new Timer();
		timer.start();
		final Cursor< T > c = target.localizingCursor();
		while ( c.hasNext() )
		{
			c.fwd();
			nnSearchSamples.search( c );
			nnSearchMask.search( c );
			if ( nnSearchSamples.getSquareDistance() <= nnSearchMask.getSquareDistance() )
				c.get().set( nnSearchSamples.getSampler().get() );
			else
				c.get().set( nnSearchMask.getSampler().get() );
		}
		return timer.stop();
	}
}
