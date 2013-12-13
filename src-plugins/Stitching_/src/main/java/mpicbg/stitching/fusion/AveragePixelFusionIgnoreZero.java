package mpicbg.stitching.fusion;

public class AveragePixelFusionIgnoreZero extends AveragePixelFusion
{
	public AveragePixelFusionIgnoreZero() { super(); }
	
	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition ) 
	{
		if ( value != 0.0 )
		{
			avg += value;
			++count;
		}
	}

	@Override
	public PixelFusion copy() { return new AveragePixelFusionIgnoreZero(); }
}
