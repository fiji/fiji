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

import stitching.OverlapProperties;

/**
 * A link between two {@link Point Points} that are expected to be ideally at
 * the same location in the world coordinate space.
 * 
 * The link is directed, such that each link touches only {@link #p1}.
 *
 */
public class PointMatch
{
	public OverlapProperties o = null;
	
	final private Point p1;
	final public Point getP1() { return p1; }
	
	final private Point p2;
	final public Point getP2() { return p2; }
	
	protected float[] weights;
	final public float[] getWeights(){ return weights; }
	final public void setWeights( float[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}
	
	protected float weight;
	final public float getWeight(){ return weight; }
	final public void setWeight( int index, float weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}
	
	final protected void calculateWeight()
	{
		weight = 1.0f;
		for ( float wi : weights )
			weight *= wi;
	}
	
	protected float strength = 1.0f;
	
	private float distance;
	final public float getDistance(){ return distance; }
	
	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatch} with an Array of weights and a strength.
	 * The Array of weights will be copied.
	 * 
	 * Strength gives the amount of application:
	 *   strength = 0 means {@link #p1} will not be transferred,
	 *   strength = 1 means {@link #p1} will be fully transferred
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 * @param strength how much should {@link #applyByStrength(Model, float)}
	 *   affect {@link #p1}
	 */
	public PointMatch(
			Point p1,
			Point p2,
			float[] weights,
			float strength )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		this.weights = weights.clone();
		calculateWeight();
		
		this.strength = strength;
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatch} with an Array of weights.
	 * The Array of weights will be copied.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 */
	public PointMatch(
			Point p1,
			Point p2,
			float[] weights )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		this.weights = weights.clone();
		calculateWeight();
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatch} with one weight.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 */
	public PointMatch(
			Point p1,
			Point p2,
			float weight )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ weight };
		this.weight = weight;
		
		distance = Point.distance( p1, p2 );
	}

	public PointMatch(
			Point p1,
			Point p2,
			float weight,
			OverlapProperties o)
	{
		this.p1 = p1;
		this.p2 = p2;
		this.o = o;
		
		weights = new float[]{ weight };
		this.weight = weight;
		
		distance = Point.distance( p1, p2 );
	}

	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatch} with one weight and strength.
	 * 
	 * Strength gives the amount of application:
	 *   strength = 0 means {@link #p1} will not be transferred,
	 *   strength = 1 means {@link #p1} will be fully transferred
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 *  @param strength how much should {@link #applyByStrength(Model, float)}
	 *   affect {@link #p1}
	 */
	public PointMatch(
			Point p1,
			Point p2,
			float weight,
			float strength )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ weight };
		this.weight = weight;
		
		this.strength = strength;
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatch} without weight.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Weight
	 */
	public PointMatch(
			Point p1,
			Point p2 )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weight = 1.0f;
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Apply a {@link Model} to {@link #p1}, update distance.
	 * 
	 * @param model
	 */
	final public void apply( Model model )
	{
		p1.apply( model );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Apply a {@link Model} to {@link #p1} with a given amount, update
	 * distance.
	 * 
	 * @param model
	 * @param amount
	 */
	final public void applyByStrength( Model model, float amount )
	{
		p1.apply( model, strength * amount );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Flip symmetrically, weights remains unchanged.
	 * 
	 * @param matches
	 * @return
	 */
	final public static ArrayList< PointMatch > flip( Collection< PointMatch > matches )
	{
		ArrayList< PointMatch > list = new ArrayList< PointMatch >();
		for ( PointMatch match : matches )
		{
			list.add(
					new PointMatch(
							match.p2,
							match.p1,
							match.weights ) );
		}
		return list;
	}	
}
