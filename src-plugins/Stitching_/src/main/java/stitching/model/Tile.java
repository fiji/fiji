/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package stitching.model;

import java.util.ArrayList;
import java.util.Collection;

public class Tile
{
	final private float width;
	final public float getWidth(){ return width; }
	
	final private float height;
	final public float getHeight(){ return height; }
	

	/**
	 * Local center coordinates of the {@link Tile}.
	 */
	private float[] lc;
	
	
	/**
	 * World center coordinates of the {@link Tile}.
	 */
	private float[] wc;
	public float[] getWC() { return wc; }

	
	/**
	 * The transformation {@link Model} of the {@link Tile}.  All local points
	 * in the {@link Tile} share (and thus determine) this common
	 * {@link Model}.
	 */
	private Model model;
	final public Model getModel() { return model; }
	
	
	/**
	 * A set of point correspondences with {@link PointMatch.getP1 p1} being a
	 * local point in this {@link Tile} and {@link PointMatch.getP2 p2} being
	 * the corresponding  local point in another {@link Tile}.  {@link Tile}s
	 * are perfectly registered if both {@link PointMatch.getP1 p1} and
	 * {@link PointMatch.getP1 p1} have the same world coordinates.
	 */
	final private ArrayList< PointMatch > matches = new ArrayList< PointMatch >();
	final public ArrayList< PointMatch > getMatches() { return matches; }
	final public void resetMatches(){ matches.clear(); }
	
	
	/**
	 * A set of {@link Tile}s that share point correpondences with this
	 * {@link Tile}.
	 * 
	 * Note that point correspondences do not know about the tiles they belong
	 * to.
	 */
	final private ArrayList< Tile > connectedTiles = new ArrayList< Tile >();
	final public ArrayList< Tile > getConnectedTiles() { return connectedTiles; }
	final public void resetConnectedTiles(){ connectedTiles.clear(); }
	
	final public void resetTile()
	{
		lc = new float[]{ this.width / 2.0f - 1.0f, this.height / 2.0f - 1.0f };
		wc = new float[]{ lc[ 0 ], lc[ 1 ] };
		resetConnectedTiles();
		resetMatches();
	}
	
	/**
	 * Add a {@link Tile} to the set of connected tiles.  Checks if this
	 * {@link Tile} is present already.
	 * 
	 * @param t the new {@link Tile}.
	 * @return Success of the operation.
	 */
	final public boolean addConnectedTile( Tile t )
	{
		if ( connectedTiles.contains( t ) ) return true;
		else return connectedTiles.add( t );
	}
	
	
	/**
	 * Remove a {@link Tile} from the set of connected {@link Tile}s.
	 * 
	 * @param t the {@link Tile} to be removed.
	 * @return Success of the operation.
	 */
	final public boolean removeConnectedTile( Tile t )
	{
		return connectedTiles.remove( t );
	}
	
	
	/**
	 * The transfer error of this {@link Tile}'s {@link Model} as estimated
	 * from weighted square point correspondence displacement.
	 */
	private double error;
	final public double getError() { return error; }
	
	
	/**
	 * The average point correpondence displacement.
	 */
	private double distance;
	final public double getDistance() { return distance; }
	
	/**
	 * Constructor
	 * 
	 * @param width width of the {@link Tile} in world unit dimension
	 *   (e.g. pixels).
	 * @param height height of the {@link Tile} in world unit dimension
	 *   (e.g. pixels).
	 * @param model the transformation {@link Model} of the {@link Tile}.
	 */
	public Tile(
			float width,
			float height,
			Model model )
	{
		this.width = width;
		this.height = height;
		this.model = model;
		
		lc = new float[]{ width / 2.0f - 1.0f, height / 2.0f - 1.0f };
		wc = new float[]{ lc[ 0 ], lc[ 1 ] };
	}
	
	
	/**
	 * Constructor
	 * 
	 * @param width width of the {@link Tile} in world unit dimension
	 *   (e.g. pixels).
	 * @param height height of the {@link Tile} in world unit dimension
	 *   (e.g. pixels).
	 * @param model the transformation {@link Model} of the {@link Tile}.
	 */
	public Tile(
			double width,
			double height,
			Model model )
	{
		this.width = ( float )width;
		this.height = ( float )height;
		this.model = model;
		
		lc = new float[]{ this.width / 2.0f - 1.0f, this.height / 2.0f - 1.0f };
		wc = new float[]{ lc[ 0 ], lc[ 1 ] };
	}
	
	/**
	 * Add more {@link PointMatch}es.
	 *  
	 * @param more the {@link PointMatch}es to be added.
	 * @return True if the list changed as a result of the call.
	 */
	final public boolean addMatches( Collection< PointMatch > more )
	{
		return matches.addAll( more );
	}
	
	/**
	 * Add one match.
	 *  
	 * @param match the {@link PointMatch} to be added.
	 * @return True if the list changed as a result of the call.
	 */
	final public boolean addMatch( PointMatch match )
	{
		return matches.add( match );
	}
	
	
	/**
	 * Apply the current {@link Model} to all local point coordinates.
	 * Update average transfer error.
	 *
	 */
	final public void update()
	{
		// tile center world coordinates
		//wc = model.apply( lc );
		
		double d = 0.0;
		double e = 0.0;
		
		int num_matches = matches.size();
		if ( num_matches > 0 )
		{
			double sum_weight = 0.0;
			for ( PointMatch match : matches )
			{
				match.apply( model );
				double dl = match.getDistance();
				d += dl;
				e += dl * dl * match.getWeight();
				sum_weight += match.getWeight();
			}
			d /= num_matches;
			e /= sum_weight;
		}
		distance = ( float )d;
		error = ( float )e;
		model.setError( e );
	}
	
	/**
	 * Apply the current {@link Model} to all local point coordinates by weight.
	 * Update average transfer error.
	 *
	 */
	final public void updateByStrength( float amount )
	{
		// tile center world coordinates
		wc = model.apply( lc );
		
		double d = 0.0;
		double e = 0.0;
		
		int num_matches = matches.size();
		if ( num_matches > 0 )
		{
			double sum_weight = 0.0;
			for ( PointMatch match : matches )
			{
				match.applyByStrength( model, amount );
				double dl = match.getDistance();
				d += dl;
				e += dl * dl * match.getWeight();
				sum_weight += match.getWeight();
			}
			d /= num_matches;
			e /= sum_weight;
		}
		distance = ( float )d;
		error = ( float )e;
		model.setError( e );
	}
	
	/**
	 * randomly dice new model until the error is smaller than the old one
	 * 
	 * @param max_num_tries maximal number of tries before returning false (which means "no better model found")
	 * @param scale strength of shaking
	 * @return true if a better model was found
	 */
	final public boolean diceBetterModel( int max_num_tries, float scale )
	{
		// store old model
		Model old_model = model;
		
		for ( int t = 0; t < max_num_tries; ++t )
		{
			model = model.clone();
			model.shake( matches, scale, lc );
			update();
			if ( model.betterThan( old_model ) )
			{
				return true;
			}
			else model = old_model;
		}
		// no better model found, so roll back
		update();
		return false;
	}
	
	/**
	 * Update the transformation {@link Model}.  That is, fit it to the
	 * current set of {@link PointMatch}es.
	 */
	final public void fitModel() throws NotEnoughDataPointsException
	{
		model.fit( matches );
		//update(); // Do not update!  The user wants to decide about the update strategy (weighted or unweighted)
	}
	
	/**
	 * Find all {@link Tile}s that represent one connectivity graph by
	 * recursively tracing the {@link #connectedTiles }.
	 * 
	 * @param graph
	 * @return the number of connected tiles in the graph
	 */
	final private int traceConnectedGraph( ArrayList< Tile > graph )
	{
		graph.add( this );
		for ( Tile t : connectedTiles )
		{
			if ( !graph.contains( t ) )
				t.traceConnectedGraph( graph );
		}
		return graph.size();
	}
	
	/**
	 * connect two tiles by a set of point correspondences
	 * 
	 * re-weighs the point correpondences
	 * 
	 * We set a weigh of 1.0 / num_matches to each correspondence to equalize
	 * the connections between tiles during minimization.
	 * TODO Check if this is a good idea...
	 * TODO What about the size of a detection, shouldn't it be used as a
	 * weight factor as	well?
	 * 
	 * Change 2007-10-27
	 * Do not normalize by changing the weight, correpondences are weighted by
	 * feature scale. 
	 * 
	 * @param o
	 * @param matches
	 */
	final public void connect(
			Tile o,
			Collection< PointMatch > matches )
	{
//		float num_matches = ( float )matches.size();
//		for ( PointMatch m : matches )
//			m.setWeight( 1.0f / num_matches );
		
		this.addMatches( matches );
		o.addMatches( PointMatch.flip( matches ) );
		
		this.addConnectedTile( o );
		o.addConnectedTile( this );
	}
	
	/**
	 * Identify the set of connected graphs that contains all given tiles.
	 * 
	 * @param tiles
	 * @return
	 */
	final static public ArrayList< ArrayList< Tile > > identifyConnectedGraphs(
			Collection< Tile > tiles )
	{
		ArrayList< ArrayList< Tile > > graphs = new ArrayList< ArrayList< Tile > >();
		int numInspectedTiles = 0;
A:		for ( Tile tile : tiles )
		{
			for ( ArrayList< Tile > knownGraph : graphs )
				if ( knownGraph.contains( tile ) ) continue A; 
			ArrayList< Tile > current_graph = new ArrayList< Tile >();
			numInspectedTiles += tile.traceConnectedGraph( current_graph );
			graphs.add( current_graph );
			if ( numInspectedTiles == tiles.size() ) break;
		}
		return graphs;
	}
	
	/**
	 * Check if a point that is given in world coordinates is inside the tile.
	 * 
	 * TODO: This implies, that the transformation is linear and invertable.
	 * 
	 * @param point
	 * @return True is the point is inside the 
	 */
	final public boolean isInside( float[] point ) throws Exception
	{
		float[] local = ( ( InvertibleModel )model ).applyInverse( point );
		return (
				local[ 0 ] >= 0.0f && local[ 0 ] < width &&
				local[ 1 ] >= 0.0f && local[ 1 ] < height );
	}
	
	
	/**
	 * Check if the {@link Tile} intersects another.
	 * 
	 * TODO: The intersection test checks for the eight corners of both tiles
	 *   only.  This implies, that the transformation is linear and invertable.
	 *   
	 *   !!!!This is wrong:
	 *   
	 *      +-+     +-----+
	 *      | |     |     |
	 *    +-+-+-+   | +-+ |
	 *    | | | |   | | | |
	 *    +-+-+-+   | +-+ |
	 *      | |     |     |
	 *      +-+     +-----+
	 *      
	 *   Test for edge intersections and points:  If only one single edge of
	 *   this intersects with one of t both tiles intersect.
	 * 
	 * @param t the other {@link Tile}
	 */
	final public boolean intersects( Tile t ) throws Exception
	{
		float[] p = new float[]{ 0.0f, 0.0f };
		model.applyInPlace( p );
		if ( t.isInside( p ) ) return true;
		
		p = new float[]{ width, 0.0f };
		model.applyInPlace( p );
		if ( t.isInside( p ) ) return true;
		
		p = new float[]{ width, height };
		model.applyInPlace( p );
		if ( t.isInside( p ) ) return true;
		
		p = new float[]{ 0.0f, height };
		model.applyInPlace( p );
		if ( t.isInside( p ) ) return true;
		
		Model m = t.getModel();
		
		p = new float[]{ 0.0f, 0.0f };
		m.applyInPlace( p );
		if ( isInside( p ) ) return true;
		
		p = new float[]{ t.width, 0.0f };
		m.applyInPlace( p );
		if ( isInside( p ) ) return true;
		
		p = new float[]{ t.width, t.height };
		m.applyInPlace( p );
		if ( isInside( p ) ) return true;
		
		p = new float[]{ 0.0f, t.height };
		m.applyInPlace( p );
		if ( isInside( p ) ) return true;
		
		return false;
	}
}
