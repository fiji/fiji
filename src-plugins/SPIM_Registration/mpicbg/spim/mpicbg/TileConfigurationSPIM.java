package mpicbg.spim.mpicbg;

import ij3d.Image3DUniverse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.models.AffineModel3D;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.vis3d.BeadTransformGroup;
import mpicbg.spim.vis3d.Motion3D;
import mpicbg.spim.vis3d.Viewer3dFunctions;
import mpicbg.spim.vis3d.VisualizationFunctions;
import mpicbg.spim.vis3d.VisualizationSketchTikZ;

public class TileConfigurationSPIM
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();

	final private Set< TileSPIM > tiles = new HashSet< TileSPIM >();
	final public Set< TileSPIM > getTiles(){ return tiles; }
	
	final private Set< TileSPIM > fixedTiles = new HashSet< TileSPIM >();
	final public Set< TileSPIM > getFixedTiles(){ return fixedTiles; }
	
	private double minError = Double.MAX_VALUE;
	final public double getMinError() {	return minError; }
	
	private double maxError = 0.0;
	final public double getMaxError() { return maxError; }
	
	private double error = Double.MAX_VALUE;
	final public double getError() { return error; }

	protected int debugLevel;
	
	public TileConfigurationSPIM( final int debugLevel )
	{
		this.debugLevel = debugLevel;
		
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	protected void println( String s )
	{
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( s ); 
	}
	
	/**
	 * Cleanup.
	 */
	public void clear()
	{
		tiles.clear();
		fixedTiles.clear();
		
		minError = Double.MAX_VALUE;
		maxError = 0.0;
		error = Double.MAX_VALUE;
	}
	
	/**
	 * Add a single {@link Tile}.
	 * 
	 * @param t
	 */
	final public void addTile( final TileSPIM t ){ tiles.add( t ); }
	
	/**
	 * Add a {@link Collection} of {@link Tile Tiles}.
	 * 
	 * @param t
	 */
	final public void addTiles( final Collection< ? extends TileSPIM > t ){ tiles.addAll( t ); }
	
	/**
	 * Add all {@link Tile Tiles} of another {@link TileConfiguration}.
	 * 
	 * @param t
	 */
	final public void addTiles( final TileConfigurationSPIM t ){ tiles.addAll( t.tiles ); }
	
	/**
	 * Fix a single {@link Tile}.
	 * 
	 * @param t
	 */
	final public void fixTile( final TileSPIM t ){ fixedTiles.add( t ); }
	
	/**
	 * Update all {@link PointMatch Correspondences} in all {@link Tile Tiles}
	 * and estimate the average displacement. 
	 */
	final protected void update()
	{
		double cd = 0.0;
		minError = Double.MAX_VALUE;
		maxError = 0.0;
		for ( TileSPIM t : tiles )
		{
			t.update();
			double d = t.getDistance();
			if ( d < minError ) minError = d;
			if ( d > maxError ) maxError = d;
			cd += d;
		}
		cd /= tiles.size();
		error = cd;
	}
	
	final public void computeError()
	{
		update();
		
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
		{
			println( "  average displacement: " + decimalFormat.format( error ) + "px" );
			println( "  minimal displacement: " + decimalFormat.format( minError ) + "px" );
			println( "  maximal displacement: " + decimalFormat.format( maxError ) + "px" );
		}
	}
	
	/**
	 * Minimize the displacement of all {@link PointMatch Correspondence pairs}
	 * of all {@link Tile Tiles}
	 * 
	 * @param maxAllowedError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average absolute
	 *   slope in an interval of this size and half this size is smaller than
	 *   0.0001 (in double accuracy).  This is assumed to prevent the algorithm
	 *   from stopping at plateaus smaller than this value.
	 * @param debugLevel defines if the Optimizer prints the output at the end of the process
	 */
	public void optimize(
			final float maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth,
			final int debugLevel ) throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		
		int i = 0;
		
		boolean proceed = i < maxIterations;
		
		while ( proceed )
		{
			for ( final TileSPIM tile : tiles )
			{
				if ( fixedTiles.contains( tile ) ) continue;
				tile.update();
				tile.fitModel();
				tile.update();
			}
			update();
			observer.add( error );
			
			if ( i > maxPlateauwidth )
			{
				proceed = error > maxAllowedError;
				
				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0001;
					}
					catch ( Exception e ) { e.printStackTrace(); }
					d /= 2;
				}
			}
			
			proceed &= ++i < maxIterations;
		}
		
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
		{
			println( "Successfully optimized configuration of " + tiles.size() + " tiles after " + i + " iterations:" );
			println( "  average displacement: " + decimalFormat.format( error ) + "px" );
			println( "  minimal displacement: " + decimalFormat.format( minError ) + "px" );
			println( "  maximal displacement: " + decimalFormat.format( maxError ) + "px" );
		}
	}
	
	/**
	 * Minimize the displacement of all {@link PointMatch Correspondence pairs}
	 * of all {@link Tile Tiles}
	 * 
	 * @param maxAllowedError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average absolute
	 *   slope in an interval of this size and half this size is smaller than
	 *   0.0001 (in double accuracy).  This is assumed to prevent the algorithm
	 *   from stopping at plateaus smaller than this value.
	 * @param debugLevel defines if the Optimizer prints the output at the end of the process
	 */
	public void optimizeWithSketchTikZ(
			final float maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth,
			final int debugLevel ) throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		
		int i = 0;
		final float factor = 0.005f;
		
		boolean proceed = i < maxIterations;
		
		while ( proceed )
		{
			for ( final TileSPIM tile : tiles )
			{
				if ( fixedTiles.contains( tile ) ) continue;
				tile.updateWithBeads();
				if ( i > 0 )
				{
					tile.fitModel();
					tile.updateWithBeads();
				}
			}
			
			//if ( i == 0 )
			if ( error < 7.5 )
			{
				SketchTikZFileObject files = SketchTikZFileObject.initOutputFile( "src/templates/beadimage-movie.sk", "src/templates/movie/dros_confocal_" + i + ".sk" );
				
				for ( TileSPIM tile : tiles )
				{
					final AffineModel3D m = new AffineModel3D();
					m.set( tile.getModel() );
					
					Transform3D t = MathLib.getTransform3D( m ); 
					ViewDataBeads parent = tile.getParent();
													
					// the bounding box is not scaled yet, so we have to apply
					// the correct z stretching
					Transform3D tmp = new Transform3D();
					Transform3D tmp2 = new Transform3D(t);
					tmp.setScale( new Vector3d(1, 1, parent.getZStretching()) );							
					tmp2.mul( tmp );				
					t = tmp2;
					
					// back up the model
					AffineModel3D backUp = new AffineModel3D( );
					backUp.set( parent.getTile().getModel() );
					
					parent.getTile().getModel().set( MathLib.getAffineModel3D( t ) );
					
					/*
					Transform3D backUpT3D = parent.transformation;
					Transform3D backUpIT3D = parent.inverseTransformation;
					parent.transformation = t;
					parent.inverseTransformation = new Transform3D( t );
					parent.inverseTransformation.invert();
					*/
					
					System.out.println("Writing view " + parent.getName() + " @ iteration " + i );
					files.getOutput().println( VisualizationSketchTikZ.drawView( parent, factor ) );
					files.getOutput().println( VisualizationSketchTikZ.drawBeads( parent.getBeadStructure().getBeadList(), MathLib.getTransform3D( m ), "Bead", factor, 2 ) );
					
					for ( Bead bead : parent.getBeadStructure().getBeadList() )
					{
						float distance = bead.getDistance();
						if ( distance >= 0 )
						{
							int color = Math.round( (float)Math.log10( distance + 1 ) * 256f );
							
							// max value == 100
							if ( color > 511 )
								color = 511;
							
							if ( color < 0)
								color = 0;
							
							files.getOutput().println( VisualizationSketchTikZ.drawBead( bead, MathLib.getTransform3D( m ), "RansacBead" + color, factor ) );
						}
					}
				
					// write back old (unscaled) model
					
					parent.getTile().getModel().set( backUp );
					
					//parent.transformation = backUpT3D;
					//parent.inverseTransformation = backUpIT3D;
				}
				
				files.finishFiles();
				
				System.exit( 0 );
			}
			
			update();
			observer.add( error );
			
			if ( i > maxPlateauwidth )
			{
				proceed = error > maxAllowedError;
				
				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0001;
					}
					catch ( Exception e ) { e.printStackTrace(); }
					d /= 2;
				}
			}
			
			proceed &= ++i < maxIterations;
		}
		
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
		{
			println( "Successfully optimized configuration of " + tiles.size() + " tiles after " + i + " iterations:" );
			println( "  average displacement: " + decimalFormat.format( error ) + "px" );
			println( "  minimal displacement: " + decimalFormat.format( minError ) + "px" );
			println( "  maximal displacement: " + decimalFormat.format( maxError ) + "px" );
		}
	}

	/*
	public void optimizeWithSketchTikZ(
			float maxAllowedError,
			int maxIterations,
			int maxPlateauwidth,
			ViewDataBeads[] views,
			boolean showDetails ) throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		final float factor = 0.005f;
		ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			for ( TileSPIM tile : tiles )
			{
				if ( !fixedTiles.contains( tile ) )
				{
					tile.updateWithBeads();
					if ( i > 0 )
					{
						tile.fitModel();
						tile.updateWithBeads();
					}
				}
			}
			
			if ( i == 10 )
			{
				FileObject files = FileObject.initOutputFile( "src/templates/beadimage-movie.sk", "src/templates/movie/beadimage_" + i + ".sk" );
				
				for ( TileSPIM tile : tiles )
				{
					AffineModel3D m = (AffineModel3D) tile.getModel();
					Transform3D t = MathLib.getTransform3D( m ); 
					ViewDataBeads parent = tile.getParent();
													
					// the bounding box is not scaled yet, so we have to apply
					// the correct z stretching
					Transform3D tmp = new Transform3D();
					Transform3D tmp2 = new Transform3D(t);
					tmp.setScale( new Vector3d(1, 1, parent.getZStretching()) );							
					tmp2.mul( tmp );				
					t = tmp2;
					
					// back up the model
					AffineModel3D backUp = new AffineModel3D( );
					backUp.set( parent.getTile().getModel() );
					
					parent.getTile().getModel().set( MathLib.getAffineModel3D( t ) );
					
					System.out.println("Writing view " + parent.getName() + " @ iteration " + i );
					files.getOutput().println( VisualizationSketchTikZ.drawView( parent, factor ) );
					files.getOutput().println( VisualizationSketchTikZ.drawBeads( parent.getBeadStructure().getBeadList(), MathLib.getTransform3D( m ), "Bead", factor ) );
					
					for ( Bead bead : parent.getBeadStructure().getBeadList() )
					{
						float distance = bead.getDistance();
						if ( distance >= 0 )
						{
							int color = Math.round( (float)Math.log10( distance + 1 ) * 256f );
							
							// max value == 100
							if ( color > 511 )
								color = 511;
							
							if ( color < 0)
								color = 0;
							
							files.getOutput().println( VisualizationSketchTikZ.drawBead( bead, MathLib.getTransform3D( m ), "RansacBead" + color, factor ) );
						}
					}
				
					// write back old (unscaled) model
					
					parent.getTile().getModel().set( backUp );
				}
				
				files.finishFiles();
				
				System.exit( 0 );
			}
			
			update();
			observer.add( error );			
			
			if (
					i >= maxPlateauwidth &&
					error < maxAllowedError &&
					Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 &&
					Math.abs( observer.getWideSlope( maxPlateauwidth / 2 ) ) <= 0.0001 )
			{
				break;
			}
			++i;
		}
		
		if (showDetails)
		{
			System.out.println( "Successfully optimized configuration of " + tiles.size() + " tiles after " + i + " iterations:" );
			System.out.println( "  average displacement: " + decimalFormat.format( error ) + "px" );
			System.out.println( "  minimal displacement: " + decimalFormat.format( minError ) + "px" );
			System.out.println( "  maximal displacement: " + decimalFormat.format( maxError ) + "px" );
		}
	}
	*/
	
	/**
	 * Minimize the displacement of all correspondence pairs of all tiles.
	 * 
	 * @param maxAllowedError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average absolute
	 *   slope in an interval of this size and half this size is smaller than
	 *   0.0001 (in double accuracy).  This is assumed to prevent the algorithm
	 *   from stopping at plateaus smaller than this value.
	 */
	public void optimizeWith3DViewer(
			final float maxAllowedError,
			int maxIterations,
			final int maxPlateauwidth,
			final ArrayList<ViewDataBeads> views,
			final int debugLevel ) throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		final float multiplicator = 0.16f;
		
		//ImageStack movie = null;
		final Color3f beadColor = new Color3f( 0.3f, 0.3f, 0.3f );
		final float beadSize = 7f;		
		final float transparency = 0.65f;
		final Color3f boundingBoxColor = new Color3f( 0.7f, 0.7f, 0.85f );
		final Color3f fixedBoundingBoxColor = new Color3f( 0.85f, 0.7f, 0.7f );
		final LineAttributes boundingBoxLineAttributes = new LineAttributes();
		boundingBoxLineAttributes.setLineWidth(2f);
		
		Image3DUniverse univ = VisualizationFunctions.initStandardUniverse( 640, 480 );
		
		for ( TileSPIM tile : getTiles() )
		{
			AffineModel3D m = tile.getModel();
			Transform3D t = MathLib.getTransform3D( m );
			
			ViewDataBeads parent = tile.getParent();
			
			Color3f col = boundingBoxColor;
			if ( fixedTiles.contains( tile ) )
				col = fixedBoundingBoxColor;
			
			parent.beadBranchGroups.add
			( 
			 	VisualizationFunctions.drawBeads( univ, parent.getBeadStructure().getBeadList(), t, 
			                                     beadColor, beadSize, transparency, VisualizationFunctions.storeBeadPosition ) 
			);
			
			// the bounding box is not scaled yet, so we have to apply
			// the correct z stretching
			Transform3D tmp = new Transform3D();
			Transform3D tmp2 = new Transform3D(t);
			tmp.setScale( new Vector3d(1, 1, parent.getZStretching()) );							
			tmp2.mul( tmp );

			parent.branchGroups.add
			( 
			 	VisualizationFunctions.drawView( univ, parent, col, boundingBoxLineAttributes, tmp2 ) 
			);
			
		}
		
		ErrorStatistic observer = new ErrorStatistic( 10000 );
		
		int start = 5;
		for (int i = 0; i < start; i++)
		{
			Viewer3dFunctions.setStatusBar( univ, "Starting in " + (start-i) + "seconds." );
			SimpleMultiThreading.threadWait( 1000 );
		}
		
		int i = 0;
		
		boolean proceed = i < maxIterations;
		
		while ( proceed )  // do not run forever
		{
			/*for ( Tile< ? > tile : tiles )
			{
				if ( fixedTiles.contains( tile ) ) continue;
				tile.updateWithBeads();
				tile.fitModel();
				tile.updateWithBeads();
			}
			update();*/
			
			Viewer3dFunctions.setStatusBar( univ, "  Average Displacement: " + decimalFormat.format( error ) + " px ( " + decimalFormat.format( error * multiplicator ) + " um ), Iteration " + i );
			
			//if ( i == 0 )
				//SimpleMultiThreading.threadWait(2000);
			//else 
				//SimpleMultiThreading.threadWait(20);
			
			/*if ( i < 3000 )
			{
				if ( i >= 10 && i % 2 == 0)
				{
					ImagePlus screenShot = Motion3D.getScreenShot();
					if ( movie == null )
						movie = new ImageStack(screenShot.getWidth(), screenShot.getHeight());
					movie.addSlice("slice", screenShot.getProcessor() );
				}
			}
			else
			{
				break;
			}*/
			
			int tileCount = 0;

			int countTile = 0;
			
			for ( TileSPIM tile : tiles )
			{
				++ countTile;
				if ( !fixedTiles.contains( tile ) )
				{
					tile.updateWithBeads();
					if ( i > 0 )
					{
						tile.fitModel();
						tile.updateWithBeads();                                                                                                                                 
					}
				}
				
				if ( i < 5 || i % 15 == 0 )
				{
					final AffineModel3D m = tile.getModel();
					final Transform3D t = MathLib.getTransform3D( m );
					final ViewDataBeads parent = tile.getParent();
					
					final Transform3D newTransform = new Transform3D();
					final Vector3f vector = new Vector3f();
					final Point3f translation = new Point3f();
					
					for ( final BranchGroup branchGroup : parent.beadBranchGroups )
					{
						final Enumeration<?> en = branchGroup.getAllChildren();
						
						while (en.hasMoreElements())
						{
							final BeadTransformGroup beadTransformGroup = (BeadTransformGroup)en.nextElement();
							
							// get the original position in the view
							//final Point3f translation = beadTransformGroup.getBeadPositionPoint();
							beadTransformGroup.getBeadPositionPoint( translation );
							
							// transform the bead coordinates into the position of the view
							t.transform( translation );						
	
							// update TransformGroup with the altered coordinates 
							//final Transform3D newTransform = new Transform3D();
							vector.set( translation );
							newTransform.setTranslation( vector );			
													
							beadTransformGroup.setTransform( newTransform );
							
							final Sphere sphere = (Sphere)beadTransformGroup.getChild(0);
							final Appearance app = new Appearance();
							
							final float distance = beadTransformGroup.getBead().getDistance();
							
							if ( distance < 0 )
							{
								app.setColoringAttributes( new ColoringAttributes( new Color3f(0,0,0), ColoringAttributes.SHADE_FLAT ) );
								app.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, 0.95f ) );
							}
							else
							{								
								final float color = Math.min( 2, (float)Math.log10( distance + 1 ) );
								
								// max value == 100
								//if ( color > 2 )
									//color = 2;
								
								final float red, green;
								
								if ( color >= 1 )
								{
									red = 1f;
									green = 2 - color;
								}
								else
								{
									green = 1f;
									red = color;
								}
						
								app.setColoringAttributes( new ColoringAttributes( new Color3f( red, green ,0f ), ColoringAttributes.SHADE_FLAT ) );
								app.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, transparency ) );
							}
							
							sphere.setAppearance(app);
						}		
					}
					
					// the bounding box is not scaled yet, so we have to apply
					// the correct z stretching
					Transform3D tmp = new Transform3D();
					Transform3D tmp2 = new Transform3D(t);
					tmp.setScale( new Vector3d(1, 1, parent.getZStretching()) );							
					tmp2.mul( tmp );
									
					for ( BranchGroup branchGroup : parent.branchGroups )
					{
						Motion3D.replaceTransformBranchGroup( branchGroup, tmp2 );
					}
					
					//TestBene.setStatusBar( univ, "  i = " + i + " tileCount = " + tileCount );				
					tileCount++;
				}

				//SimpleMultiThreading.threadWait( 100 );		
				/*
				if ( i <= 70 )
				{
					SimpleMultiThreading.threadWait( 200 );
					
					ImagePlus screenShot = Motion3D.getScreenShot();
					if ( movie == null )
						movie = new ImageStack(screenShot.getWidth(), screenShot.getHeight());
					movie.addSlice("slice", screenShot.getProcessor() );
				}
				*/
			}

			
			update();
			observer.add( error );			
			
			if ( i > maxPlateauwidth )
			{
				proceed = error > maxAllowedError;
				
				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0001;
					}
					catch ( Exception e ) { e.printStackTrace(); }
					d /= 2;
				}
			}
			
			proceed &= ++i < maxIterations;
		}
		
		
		//ImagePlus movieImp = new ImagePlus("Movie", movie);
		//movieImp.show();
		
		println( "Successfully optimized configuration of " + tiles.size() + " tiles after " + i + " iterations:" );
		println( "  average displacement: " + decimalFormat.format( getError() ) + "px" );
		println( "  minimal displacement: " + decimalFormat.format( getMinError() ) + "px" );
		println( "  maximal displacement: " + decimalFormat.format( getMaxError() ) + "px" );
	}
	
}
