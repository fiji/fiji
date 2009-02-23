/**
 * 
 */
package lenscorrection;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lenscorrection.Distortion_Correction.BasicParam;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.trakem2.align.AbstractAffineTile2D;
import mpicbg.trakem2.align.Align;
import mpicbg.trakem2.transform.CoordinateTransform;

import ij.IJ;
import ij.gui.GenericDialog;
import ini.trakem2.display.Display;
import ini.trakem2.display.Displayable;
import ini.trakem2.display.Layer;
import ini.trakem2.display.Patch;
import ini.trakem2.display.Selection;
import ini.trakem2.utils.Worker;
import ini.trakem2.utils.Bureaucrat;
import ini.trakem2.utils.IJError;
import ini.trakem2.utils.Utils;

/**
 * Methods collection to be called from the GUI for alignment tasks.
 *
 */
final public class DistortionCorrectionTask
{
	static public class CorrectDistortionFromSelectionParam extends BasicParam
	{
		public int firstLayerIndex;
		public int lastLayerIndex;
		public boolean clearTransform = false;
		public boolean visualize = false;
		
		public void addFields( final GenericDialog gd, final Selection selection )
		{
			addFields( gd );
			gd.addMessage( "Apply Distortion Correction :" );
			
			final List< Layer > layers = selection.getLayer().getParent().getLayers();
			final String[] layerTitles = new String[ layers.size() ];
			for ( int i = 0; i < layers.size(); ++i )
				layerTitles[ i ] = layers.get( i ).getTitle();

			gd.addChoice( "first_layer :", layerTitles, selection.getLayer().getTitle() );
			gd.addChoice( "last_layer :", layerTitles, selection.getLayer().getTitle() );
			gd.addCheckbox( "clear_present_transforms", clearTransform );
			gd.addCheckbox( "visualize_distortion_model", visualize );
		}
		
		@Override
		public boolean readFields( final GenericDialog gd )
		{
			super.readFields( gd );
			firstLayerIndex = gd.getNextChoiceIndex();
			lastLayerIndex = gd.getNextChoiceIndex();
			if ( firstLayerIndex > lastLayerIndex )
			{
				final int b = firstLayerIndex;
				firstLayerIndex = lastLayerIndex;
				lastLayerIndex = b;
			}
			clearTransform = gd.getNextBoolean();
			visualize = gd.getNextBoolean();
			return !gd.invalidNumber();
		}
		
		public boolean setup( final Selection selection )
		{
			final GenericDialog gd = new GenericDialog( "Distortion Correction" );
			addFields( gd, selection );
			do
			{
				gd.showDialog();
				if ( gd.wasCanceled() ) return false;
			}			
			while ( !readFields( gd ) );
			
			return true;
		}
		
		@Override
		public CorrectDistortionFromSelectionParam clone()
		{
			final CorrectDistortionFromSelectionParam p = new CorrectDistortionFromSelectionParam();
			p.sift.set( sift );
			p.dimension = dimension;
			p.expectedModelIndex = expectedModelIndex;
			p.lambda = lambda;
			p.maxEpsilon = maxEpsilon;
			p.minInlierRatio = minInlierRatio;
			p.rod = rod;
			p.firstLayerIndex = firstLayerIndex;
			p.lastLayerIndex = lastLayerIndex;
			p.clearTransform = clearTransform;
			p.visualize = visualize;
			
			return p;
		}
	}
	
	
	/**
	 * Sets a {@link CoordinateTransform} to a list of patches.
	 */
	final static protected class SetCoordinateTransformThread extends Thread
	{
		final protected List< Patch > patches;
		final protected CoordinateTransform transform;
		final protected AtomicInteger ai;
		
		public SetCoordinateTransformThread(
				final List< Patch > patches,
				final CoordinateTransform transform,
				final AtomicInteger ai )
		{
			this.patches = patches;
			this.transform = transform;
			this.ai = ai;
		}
		
		@Override
		final public void run()
		{
			for ( int i = ai.getAndIncrement(); i < patches.size() && !isInterrupted(); i = ai.getAndIncrement() )
			{
				final Patch patch = patches.get( i );
				//IJ.log( "Setting transform \"" + transform + "\" for patch \"" + patch.getTitle() + "\"." );
				patch.setCoordinateTransform( transform );
				patch.updateMipmaps();
				
				IJ.showProgress( i, patches.size() );
			}
		}
	}
	
	final static protected void setCoordinateTransform(
			final List< Patch > patches,
			final CoordinateTransform transform,
			final int numThreads )
	{
		final AtomicInteger ai = new AtomicInteger( 0 );
		final List< SetCoordinateTransformThread > threads = new ArrayList< SetCoordinateTransformThread >();
	
		for ( int i = 0; i < numThreads; ++i )
		{
			final SetCoordinateTransformThread thread = new SetCoordinateTransformThread( patches, transform, ai );
			threads.add( thread );
			thread.start();
		}
		try
		{
			for ( final Thread thread : threads )
				thread.join();
		}
		catch ( InterruptedException e )
		{
			IJ.log( "Setting CoordinateTransform failed.\n" + e.getMessage() + "\n" + e.getStackTrace() );
		}
	}
	
	
	/**
	 * Appends a {@link CoordinateTransform} to a list of patches.
	 */
	final static protected class AppendCoordinateTransformThread extends Thread
	{
		final protected List< Patch > patches;
		final protected CoordinateTransform transform;
		final protected AtomicInteger ai;
		
		public AppendCoordinateTransformThread(
				final List< Patch > patches,
				final CoordinateTransform transform,
				final AtomicInteger ai )
		{
			this.patches = patches;
			this.transform = transform;
			this.ai = ai;
		}
		
		@Override
		final public void run()
		{
			for ( int i = ai.getAndIncrement(); i < patches.size() && !isInterrupted(); i = ai.getAndIncrement() )
			{
				final Patch patch = patches.get( i );
				patch.appendCoordinateTransform( transform );
				patch.updateMipmaps();
				
				IJ.showProgress( i, patches.size() );
			}
		}
	}
	
	final static protected void appendCoordinateTransform(
			final List< Patch > patches,
			final CoordinateTransform transform,
			final int numThreads )
	{
		final AtomicInteger ai = new AtomicInteger( 0 );
		final List< AppendCoordinateTransformThread > threads = new ArrayList< AppendCoordinateTransformThread >();
	
		for ( int i = 0; i < numThreads; ++i )
		{
			final AppendCoordinateTransformThread thread = new AppendCoordinateTransformThread( patches, transform, ai );
			threads.add( thread );
			thread.start();
		}
		try
		{
			for ( final Thread thread : threads )
				thread.join();
		}
		catch ( InterruptedException e )
		{
			IJ.log( "Appending CoordinateTransform failed.\n" + e.getMessage() + "\n" + e.getStackTrace() );
		}
	}
	
	final static public CorrectDistortionFromSelectionParam correctDistortionFromSelectionParam = new CorrectDistortionFromSelectionParam();
	
	final static public Bureaucrat correctDistortionFromSelectionTask ( final Selection selection )
	{
		Worker worker = new Worker("Distortion Correction", false, true) {
			public void run() {
				startedWorking();
				try {
					correctDistortionFromSelection( selection );
					Display.repaint(selection.getLayer());
				} catch (Throwable e) {
					IJError.print(e);
				} finally {
					finishedWorking();
				}
			}
			public void cleanup() {
				if (!selection.isEmpty())
					selection.getLayer().getParent().undoOneStep();
			}
		};
		return Bureaucrat.createAndStart( worker, selection.getProject() );
	}
	
	final static public void correctDistortionFromSelection( final Selection selection )
	{
		List< Patch > patches = new ArrayList< Patch >();
		for ( Displayable d : Display.getFront().getSelection().getSelected() )
			if ( d instanceof Patch ) patches.add( ( Patch )d );
		
		if ( patches.size() < 2 )
		{
			Utils.log("No images in the selection.");
			return;
		}
		
		if ( !correctDistortionFromSelectionParam.setup( selection ) ) return;
		
		final CorrectDistortionFromSelectionParam p = correctDistortionFromSelectionParam.clone();
		final Align.ParamOptimize ap = Align.paramOptimize.clone();
		ap.sift.set( p.sift );
		ap.desiredModelIndex = ap.expectedModelIndex = p.expectedModelIndex;
		ap.maxEpsilon = p.maxEpsilon;
		ap.minInlierRatio = p.minInlierRatio;
		ap.rod = p.rod;
		
		/** Get all patches that will be affected. */
		final List< Patch > allPatches = new ArrayList< Patch >();
		for ( int i = p.firstLayerIndex; i <= p.lastLayerIndex; ++i )
			for ( Displayable d : selection.getLayer().getParent().getLayer( i ).getDisplayables( Patch.class ) )
				allPatches.add( ( Patch )d );
		
		/** Unset the coordinate transforms of all patches if desired. */
		if ( p.clearTransform )
		{
			IJ.log( "Clearing present transforms." );
			setCoordinateTransform( allPatches, null, Runtime.getRuntime().availableProcessors() );
			Display.repaint();
		}
		
		List< AbstractAffineTile2D< ? > > tiles = new ArrayList< AbstractAffineTile2D< ? > >();
		List< AbstractAffineTile2D< ? > > fixedTiles = new ArrayList< AbstractAffineTile2D< ? > > ();
		List< Patch > fixedPatches = new ArrayList< Patch >();
		final Displayable active = selection.getActive();
		if ( active != null && active instanceof Patch )
			fixedPatches.add( ( Patch )active );
		Align.tilesFromPatches( ap, patches, fixedPatches, tiles, fixedTiles );
		
		final List< AbstractAffineTile2D< ? >[] > tilePairs = new ArrayList< AbstractAffineTile2D< ? >[] >();
		
		/** TODO Verena: Why not each to each? */
//		AbstractAffineTile2D.pairTiles( tiles, tilePairs );
		final AbstractAffineTile2D< ? > fixedTile = fixedTiles.iterator().next();
		for ( final AbstractAffineTile2D< ? > t : tiles )
			 if ( t != fixedTile )
				 tilePairs.add( new AbstractAffineTile2D< ? >[]{ t, fixedTile } );
		
		Align.connectTilePairs( ap, tiles, tilePairs, Runtime.getRuntime().availableProcessors() );
		
		
		/** Shift all local coordinates into the original image frame */
		for ( final AbstractAffineTile2D< ? > tile : tiles )
		{
			final Rectangle box = tile.getPatch().getCoordinateTransformBoundingBox();
			for ( final PointMatch m : tile.getMatches() )
			{
				final float[] l = m.getP1().getL();
				final float[] w = m.getP1().getW();
				l[ 0 ] += box.x;
				l[ 1 ] += box.y;
				w[ 0 ] = l[ 0 ];
				w[ 1 ] = l[ 1 ];
			}
		}
		
		if ( Thread.currentThread().isInterrupted() ) return;
		
		List< Set< Tile< ? > > > graphs = AbstractAffineTile2D.identifyConnectedGraphs( tiles );
		if ( graphs.size() > 1 )
			IJ.log( "Could not interconnect all images with correspondences.  " );
		
		final List< AbstractAffineTile2D< ? > > interestingTiles;
		
		/** Find largest graph. */
		Set< Tile< ? > > largestGraph = null;
		for ( Set< Tile< ? > > graph : graphs )
			if ( largestGraph == null || largestGraph.size() < graph.size() )
				largestGraph = graph;
		
		interestingTiles = new ArrayList< AbstractAffineTile2D< ? > >();
		for ( Tile< ? > t : largestGraph )
			interestingTiles.add( ( AbstractAffineTile2D< ? > )t );
		
		if ( Thread.currentThread().isInterrupted() ) return;
		
		Align.optimizeTileConfiguration( ap, interestingTiles, fixedTiles );
		
		/** Some data shuffling for the lens correction interface */
		final List< Collection< PointMatch > > matches = new ArrayList< Collection< PointMatch > >();
		final List< AffineTransform > affines = new ArrayList< AffineTransform >();
		for ( AbstractAffineTile2D< ? >[] tilePair : tilePairs )
		{
			matches.add( tilePair[ 0 ].getMatches() );
			final AffineTransform a = tilePair[ 0 ].createAffine();
			a.preConcatenate( fixedTile.getModel().createInverseAffine() );
			affines.add( a );
		}
		
		final NonLinearTransform lensModel = Distortion_Correction.createInverseDistortionModel(
	    		matches,
	    		affines,
	    		p.dimension,
	    		p.lambda,
	    		( int )fixedTile.getWidth(),
	    		( int )fixedTile.getHeight() );
		
		IJ.log( "Lens model estimated." );
		
		if ( p.visualize )
		{
			IJ.log( "Going to visualize the lens model ..." );
			lensModel.visualizeSmall( p.lambda );
		}
		
		IJ.log( "Appending transform \"" + lensModel + "\" for all patches ..." );
		appendCoordinateTransform( patches, lensModel, Runtime.getRuntime().availableProcessors() );
		
		IJ.log( "Done." );
		
		Display.repaint();
	}
}
