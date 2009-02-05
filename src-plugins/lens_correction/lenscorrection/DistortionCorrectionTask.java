/**
 * 
 */
package lenscorrection;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import lenscorrection.Distortion_Correction.BasicParam;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.trakem2.align.AbstractAffineTile2D;
import mpicbg.trakem2.align.Align;

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
		public boolean visualize = false;
		
		public void addFields( final GenericDialog gd, final Selection selection )
		{
			addFields( gd );
			gd.addMessage( "Apply Distortion Correction :" );
			
			final List< Layer > layers = selection.getLayer().getParent().getLayers();
			final String[] layerTitles = new String[ layers.size() ];
			for ( int i = 0; i < layers.size(); ++i )
				layerTitles[ i ] = layers.get( i ).getTitle();

			gd.addChoice( "first layer :", layerTitles, selection.getLayer().getTitle() );
			gd.addChoice( "last layer :", layerTitles, selection.getLayer().getTitle() );
			gd.addCheckbox( "visualize distortion model", visualize );
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
			p.visualize = visualize;
			
			return p;
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
		
		for ( int i = p.firstLayerIndex; i <= p.lastLayerIndex; ++i )
		{
			final Layer layer = selection.getLayer().getParent().getLayer( i );
			final List< Displayable > displayables = layer.getDisplayables( Patch.class );
			for ( Displayable d : displayables )
			{
				final Patch o = ( Patch )d;
				IJ.log( "Setting lens model for patch \"" + o.getTitle() + "\"" );
				o.setCoordinateTransform( lensModel );
				o.updateBucket();
				o.updateMipmaps();
			}
		}
		
		Display.repaint();
	}
}
