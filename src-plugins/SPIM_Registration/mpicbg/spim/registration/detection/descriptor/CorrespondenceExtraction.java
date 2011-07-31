package mpicbg.spim.registration.detection.descriptor;

import java.util.ArrayList;

import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.registration.detection.AbstractDetection;

public interface CorrespondenceExtraction <T extends AbstractDetection<T> >
{
	public ArrayList<PointMatchGeneric<T>> extractCorrespondenceCandidates( 
			final ArrayList< T > pointListA, 
			final ArrayList< T > pointListB, 
			final double differenceThreshold, 
			final double ratioOfDistance, 
			final boolean useAssociatedBeads );
}
