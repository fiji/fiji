package mpicbg.pointdescriptor.model;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * This model just applies some other {@link TranslationInvariantModel} without computing anything.
 * This could be considered a translation invariant translation model :)
 */
public class TranslationInvariantFixedModel extends TranslationInvariantModel<TranslationInvariantFixedModel> 
{
	static final protected int MIN_NUM_MATCHES = 1;

	protected float
		m00 = 1.0f, m01 = 0.0f, m02 = 0.0f, 
		m10 = 0.0f, m11 = 1.0f, m12 = 0.0f, 
		m20 = 0.0f, m21 = 0.0f, m22 = 1.0f;
	
	public TranslationInvariantFixedModel( final float m00, final float m01, final float m02,
										   final float m10, final float m11, final float m12,
										   final float m20, final float m21, final float m22 )
	{
		this.m00 = m00;
		this.m10 = m10;
		this.m20 = m20;
		this.m01 = m01;
		this.m11 = m11;
		this.m21 = m21;
		this.m02 = m02;
		this.m12 = m12;
		this.m22 = m22;		
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
	final public void set( final TranslationInvariantFixedModel m )
	{
		m00 = m.m00;
		m10 = m.m10;
		m20 = m.m20;
		m01 = m.m01;
		m11 = m.m11;
		m21 = m.m21;
		m02 = m.m02;
		m12 = m.m12;
		m22 = m.m22;		

		cost = m.cost;
	}

	@Override
	public TranslationInvariantFixedModel copy()
	{
		TranslationInvariantFixedModel m = new TranslationInvariantFixedModel( m00, m01, m02, 
		                                                                       m10, m11, m12, 
		                                                                       m20, m21, m22 );
	
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
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22;
	}
	
	final public String toString()
	{
		return "3d-3x3: (" +
		m00 + ", " + m01 + ", " + m02 + ", " +
		m10 + ", " + m11 + ", " + m12 + ", " +
		m20 + ", " + m21 + ", " + m22 + ")";
	}
	
}
