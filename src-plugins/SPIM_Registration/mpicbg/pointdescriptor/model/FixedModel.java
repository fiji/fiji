package mpicbg.pointdescriptor.model;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * This model just applies some other {@link TranslationInvariantModel} without computing anything.
 * This could be considered a translation invariant translation model :)
 */
public class FixedModel extends TranslationInvariantModel<FixedModel> 
{
	static final protected int MIN_NUM_MATCHES = 1;

	protected float
		m00 = 1.0f, m01 = 0.0f, m02 = 0.0f, m03 = 0.0f,
		m10 = 0.0f, m11 = 1.0f, m12 = 0.0f, m13 = 0.0f,
		m20 = 0.0f, m21 = 0.0f, m22 = 1.0f, m23 = 0.0f;
	
	public FixedModel( final float m00, final float m01, final float m02, final float m03,
			           final float m10, final float m11, final float m12, final float m13,
			           final float m20, final float m21, final float m22, final float m23 )
	{
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;		
		this.m23 = m23;		
	}
	
	@Override
	public boolean canDoNumDimension( final int numDimensions ) { return numDimensions == 3; }

	@Override
	final public <P extends PointMatch> void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " matches given, we need at least " + MIN_NUM_MATCHES + " data point." );
	}
	
	@Override
	final public void set( final FixedModel m )
	{
		m00 = m.m00;
		m01 = m.m01;
		m02 = m.m02;
		m03 = m.m03;
		m10 = m.m10;
		m11 = m.m11;
		m12 = m.m12;
		m13 = m.m13;
		m20 = m.m20;
		m21 = m.m21;
		m22 = m.m22;		
		m23 = m.m23;		

		cost = m.cost;
	}

	@Override
	public FixedModel copy()
	{
		FixedModel m = new FixedModel( m00, m01, m02, m03, 
		                               m10, m11, m12, m13,
		                               m20, m21, m22, m23 );
	
		m.cost = cost;

		return m;
	}
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public float[] apply( final float[] l )
	{
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 3 : "3d 3x3 transformations can be applied to 3d points only.";
		
		final float l0 = l[ 0 ];
		final float l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02 + m03;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12 + m13;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22 + m23;
	}
	
	final public String toString()
	{
		return "3d-3x3: (" +
		m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " +
		m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " +
		m20 + ", " + m21 + ", " + m22 + ", " + m23 + ")";
	}
	
}
