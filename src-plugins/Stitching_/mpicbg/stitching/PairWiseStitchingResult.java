package mpicbg.stitching;

public class PairWiseStitchingResult 
{
	float[] offset;
	float crossCorrelation, phaseCorrelation;

	public PairWiseStitchingResult( final float[] offset, final float crossCorrelation, final float phaseCorrelation )
	{
		this.offset = offset;
		this.crossCorrelation = crossCorrelation;
		this.phaseCorrelation = phaseCorrelation;
	}
	
	public int getNumDimensions() { return offset.length; }
	public float[] getOffset() { return offset; }
	public float getOffset( final int dim ) { return offset[ dim ]; }
	public float getCrossCorrelation() { return crossCorrelation; }
	public float getPhaseCorrelation() { return phaseCorrelation; }
}
