package mpicbg.stitching.fusion;

public class MinPixelFusion implements PixelFusion 
{
	float min;
	boolean set;
	
	public MinPixelFusion() { clear(); }
	
	@Override
	public void clear() 
	{ 
		min = 0;
		set = false;
	}

	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition )
	{
		if ( set )
		{
			min = Math.min( value, min );
		}
		else
		{
			min = value;
			set = true;
		}
	}

	@Override
	public float getValue() { return min; }

	@Override
	public PixelFusion copy() { return new MinPixelFusion(); }
}
