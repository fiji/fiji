package mpicbg.spim.registration.detection;

import java.util.ArrayList;

public class DetectionStructure< T extends DetectionView< ?, T > >
{
	final ArrayList< T > detections = new ArrayList< T >();
		
	public void addDetection( final T detection ) { detections.add( detection ); }
	public ArrayList<T> getDetectionList() { return detections; }
	public T getDetection( final long detectionID ) { return detections.get( (int)detectionID ); }
	
	public T getDetection( final float x, final float y, final float z )
	{
		for ( final T detection : getDetectionList() )
		{
			float[] location = detection.getL();
			
			if ( x == location[ 0 ] && y == location[ 1 ] && z == location[ 2 ] )
				return detection;
		}
		return null;
	}

	public void clearAllCorrespondenceCandidates()
	{
		for ( final T detection : getDetectionList() )
			detection.getDescriptorCorrespondence().clear();
	}
	
	public void clearAllRANSACCorrespondences()
	{
		for ( final T detection : getDetectionList() )
			detection.getRANSACCorrespondence().clear();
	}

	public void clearAllICPCorrespondences()
	{
		for ( final T detection : getDetectionList() )
			detection.getICPCorrespondence().clear();
	}
	
}
