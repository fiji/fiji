/**
 * 
 */
package lenscorrection;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.trakem2.align.AbstractAffineTile2D;
import mpicbg.trakem2.align.Align;

import ij.IJ;
import ini.trakem2.display.Display;
import ini.trakem2.display.Displayable;
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
	static Distortion_Correction.BasicParam p = Distortion_Correction.p;
	
	final static public Bureaucrat correctDistortionFromSelectionTask ( final Selection selection )
	{
		Worker worker = new Worker("Correct Lens Distortion", false, true) {
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
		
		if ( !Distortion_Correction.p.setup( "Align Selected Tiles" ) ) return;
		
		final Distortion_Correction.BasicParam p = Distortion_Correction.p.clone();
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
		
		final NonLinearTransform lensModel = Distortion_Correction.distortionCorrection(
	    		matches,
	    		affines,
	    		p.dimension,
	    		p.lambda,
	    		( int )fixedTile.getWidth(),
	    		( int )fixedTile.getHeight() );
		
		IJ.log( "Lens model estimated." );
		IJ.log( "Going to visualize the lens model ..." );
		lensModel.visualizeSmall( p.lambda );
		
		/**
		 * Apply the lens model to all patches in the layer.
		 * 
		 * TODO Make this behaviour a parameter.  The user might want to apply
		 *   the estimated model to more than this layer or to a selection of
		 *   patches only.
		 */ 
		for ( Displayable d : selection.getLayer().getDisplayables( Patch.class ) )
		{
			final Patch o = ( Patch )d;
			IJ.log( "Setting lens model for patch \"" + o.getTitle() + "\"" );
			o.setCoordinateTransform( lensModel );
			o.updateMipmaps();
		}
		
		Display.repaint();
	}
}
