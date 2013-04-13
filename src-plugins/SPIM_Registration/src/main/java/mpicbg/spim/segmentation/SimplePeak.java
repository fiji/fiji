package mpicbg.spim.segmentation;

public class SimplePeak
{
	public SimplePeak( final int[] location, final float intensity, final boolean isMin, final boolean isMax )
	{
		this.location = location.clone();
		this.isMin = isMin;
		this.isMax = isMax;
		this.intensity = intensity;
	}
	
	public int[] location;
	public boolean isMax, isMin;
	public float intensity;
}

