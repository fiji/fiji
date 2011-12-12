package mpicbg.stitching.fusion;

public class AveragePixelFusion implements PixelFusion
{
	double avg;
	int count;
	
	public AveragePixelFusion() { clear(); }
	
	@Override
	public void clear() 
	{
		count = 0;
		avg = 0;
	}

	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition ) 
	{
		avg += value;
		++count;
	}

	@Override
	public float getValue() 
	{ 
		if ( count == 0 )
			return 0;
		else
			return (float) (avg/(double)count); 
	}

	@Override
	public PixelFusion copy() { return new AveragePixelFusion(); }

}
