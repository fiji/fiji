package mpicbg.spim.registration.bead.error;


public class GlobalErrorStatisticsImpl implements GlobalErrorStatistics 
{
	// statistics
	protected double avgError = -1;
	protected double minError = -1;
	protected double maxError = -1;
	protected int numDetections = 0;
	protected int numCorrespondences = 0;
	protected int numCandidates = 0;
	protected int countAvgErrors = 0;
	protected double avgLocalError = 0;

	@Override
	public void reset()
	{
		avgError = minError = maxError = -1;
		numDetections = numCorrespondences = numCandidates = countAvgErrors = 0;
		avgLocalError = 0;
	}
	
    //
	// Statics methods
    //    
	@Override
    public double getAverageAlignmentError(){ return avgError; }
	@Override
    public double getMinAlignmentError(){ return minError; }
	@Override
    public double getMaxAlignmentError(){ return maxError; }
    
	@Override
    public void setAverageAlignmentError( final double avg ){ avgError = avg; }
	@Override
    public void setMinAlignmentError( final double min ){ minError = min; }
	@Override
    public void setMaxAlignmentError( final double max ){ maxError = max; }

	@Override
    public int getNumDetections(){ return numDetections; }
	@Override
    public int getNumCandidates(){ return numCandidates; }
	@Override
    public int getNumCorrespondences(){ return numCorrespondences; }
	
	@Override
    public void setNumDetections( int numDetection ) { this.numDetections = numDetection; }
	@Override
	public void setNumCandidates( int numCandidates ) { this.numCandidates = numCandidates; }
	@Override
	public void setNumCorrespondences( int numCorrespondences ) { this.numCorrespondences = numCorrespondences; }
	
	@Override
    public double getAverageLocalAlignmentError(){ return avgLocalError/(double)countAvgErrors; }
	@Override
	public void setAbsoluteLocalAlignmentError( final double error ) { avgLocalError = error; }
	@Override
	public void setAlignmentErrorCount( final int count ) { countAvgErrors = count; }
	@Override
	public double getAbsoluteLocalAlignmentError() { return avgLocalError; }
	@Override
	public int getAlignmentErrorCount() { return countAvgErrors; }
}
