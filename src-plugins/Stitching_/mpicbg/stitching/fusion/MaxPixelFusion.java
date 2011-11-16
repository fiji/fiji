package mpicbg.stitching.fusion;

public class MaxPixelFusion implements PixelFusion 
{
	float max;
	boolean set;
	
	public MaxPixelFusion() { clear(); }
	
	@Override
	public void clear()
	{
		set = false;
		max = 0; 
	}

	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition ) 
	{
		if ( set )
		{
			max = Math.max( value, max );
		}
		else
		{
			max = value;
			set = true;
		}
		
	}

	@Override
	public float getValue() { return max; }

	@Override
	public PixelFusion copy() { return new MaxPixelFusion(); }
}
