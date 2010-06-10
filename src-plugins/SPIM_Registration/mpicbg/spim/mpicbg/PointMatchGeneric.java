package mpicbg.spim.mpicbg;

import java.util.ArrayList;
import java.util.Collection;

import mpicbg.models.CoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.Point;

public class PointMatchGeneric <P extends Point>
{
	private static final long serialVersionUID = 4021585829747053663L;

	final private P p1;
	final public P getP1() { return p1; }
	
	final private P p2;
	final public P getP2() { return p2; }
	
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
	 * Create a {@link PointMatchSPIM<P>} with an Array of weights and a strength.
	 * The Array of weights will be copied.
	 * 
	 * Strength gives the amount of application:
	 *   strength = 0 means {@link #p1} will not be transferred,
	 *   strength = 1 means {@link #p1} will be fully transferred
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 * @param strength how much should {@link #apply(Model, float)}
	 *   affect {@link #p1}
	 */
	public PointMatchGeneric(
			P p1,
			P p2,
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
	 * Create a {@link PointMatchSPIM<P>} with an Array of weights.
	 * The Array of weights will be copied.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 */
	public PointMatchGeneric(
			P p1,
			P p2,
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
	 * Create a {@link PointMatchSPIM<P>} with one weight.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 */
	public PointMatchGeneric(
			P p1,
			P p2,
			float weight )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ weight };
		this.weight = weight;
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatchSPIM<P>} with one weight and strength.
	 * 
	 * Strength gives the amount of application:
	 *   strength = 0 means {@link #p1} will not be transferred,
	 *   strength = 1 means {@link #p1} will be fully transferred
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 *  @param strength how much should {@link #apply(Model, float)}
	 *   affect {@link #p1}
	 */
	public PointMatchGeneric(
			P p1,
			P p2,
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
	 * Create a {@link PointMatchSPIM<P>} without weight.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Weight
	 */
	public PointMatchGeneric(
			P p1,
			P p2 )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ 1.0f };
		weight = 1.0f;
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Apply a {@link CoordinateTransform} to {@link #p1}, update distance.
	 * 
	 * @param t
	 */
	final public void apply( final CoordinateTransform t )
	{
		p1.apply( t );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Apply a {@link CoordinateTransform} to {@link #p1} with a given amount,
	 * update distance.
	 * 
	 * @param t
	 * @param amount
	 */
	final public void apply( final CoordinateTransform t, final float amount )
	{
		p1.apply( t, strength * amount );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Apply a {@link CoordinateTransform} to {@link #p1} a {@link Collection}
	 * of {@link PointMatchSPIM<P> PointMatchSPIM<P>es}, update their distances.
	 * 
	 * @param matches
	 * @param t
	 */
	static public void apply( final Collection< PointMatchGeneric<?> > matches, final CoordinateTransform t )
	{
		for ( final PointMatchGeneric<?> match : matches )
			match.apply( t );
	}
	
	/**
	 * Flip all {@link PointMatchSPIM<P> PointMatchSPIM<P>es} from
	 * {@linkplain Collection matches} symmetrically and fill
	 * {@linkplain Collection flippedMatches} with them, weights remain
	 * unchanged.
	 * 
	 * @param matches original set
	 * @param flippedMatches result set
	 */
	final public static <P extends Point> void flip(
			final Collection< PointMatchGeneric<P> > matches,
			final Collection< PointMatchGeneric<P> > flippedMatches )
	{
		for ( final PointMatchGeneric<P> match : matches )
			flippedMatches.add(
					new PointMatchGeneric<P>(
							match.p2,
							match.p1,
							match.weights ) );
	}
	
	/**
	 * Flip symmetrically, weights remains unchanged.
	 * 
	 * @param matches
	 * @return
	 */
	final public static <P extends Point> Collection< PointMatchGeneric<P> > flip( final Collection< PointMatchGeneric<P> > matches )
	{
		final ArrayList< PointMatchGeneric<P> > list = new ArrayList< PointMatchGeneric<P> >();
		flip( matches, list );
		return list;
	}
	
	final public static <P extends Point> void sourcePoints( final Collection< PointMatchGeneric<P> > matches, final Collection< P > sourcePoints )
	{
		for ( final PointMatchGeneric<P> m : matches )
			sourcePoints.add( m.getP1() );
	}
	
	final public static <P extends Point> void targetPoints( final Collection< PointMatchGeneric<P> > matches, final Collection< P > targetPoints )
	{
		for ( final PointMatchGeneric<P> m : matches )
			targetPoints.add( m.getP2() );
	}
	
	static public float meanDistance( final Collection< PointMatchGeneric<?> > matches )
	{
		double d = 0.0;
		for ( final PointMatchGeneric<?> match : matches )
			d += match.getDistance();
		return ( float )( d / matches.size() );
	}
	
	static public float maxDistance( final Collection< PointMatchGeneric<?> > matches )
	{
		float max = -Float.MAX_VALUE;
		for ( final PointMatchGeneric<?> match : matches )
		{
			final float d = match.getDistance();
			if ( d > max ) max = d;
		}
		return max;
	}
}
