package mpicbg.spim.registration.bead.error;

public interface GlobalErrorStatistics
{
	public double getMinAlignmentError();
	public double getAverageAlignmentError();
	public double getMaxAlignmentError();

	public void setMinAlignmentError( double min );
	public void setAverageAlignmentError( double avg );
	public void setMaxAlignmentError( double max );

    public int getNumDetections();
    public int getNumCandidates();
    public int getNumCorrespondences();

    public void setNumDetections( int numDetection );
    public void setNumCandidates( int numCandidates );
    public void setNumCorrespondences( int numCorrespondences );
    
    public void setAbsoluteLocalAlignmentError( double error );
    public void setAlignmentErrorCount( int count );
    public double getAbsoluteLocalAlignmentError();
    public int getAlignmentErrorCount();
    
    public double getAverageLocalAlignmentError();
    
    public void reset();
}
