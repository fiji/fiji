package mpicbg.pointdescriptor;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.matcher.Matcher;
import mpicbg.pointdescriptor.similarity.SimilarityMeasure;

public class SimplePointDescriptor < P extends Point > extends AbstractPointDescriptor< P, SimplePointDescriptor<P> >
{
	public SimplePointDescriptor( final P basisPoint, final ArrayList<P> orderedNearestNeighboringPoints, final SimilarityMeasure similarityMeasure, final Matcher matcher ) throws NoSuitablePointsException
	{
		super( basisPoint, orderedNearestNeighboringPoints, similarityMeasure, matcher );
		
		/* check that number of nearest neighbors is at least ONE, otherwise relative distances are useless */
		if ( numNeighbors() < 1 )
			throw new NoSuitablePointsException( "At least 1 nearest neighbor is required for matching descriptors." );
		
	}

	@Override
	public Object fitMatches( final ArrayList<PointMatch> matches ) { return null; }

	@Override
	public boolean resetWorldCoordinatesAfterMatching() { return false; }

	@Override
	public boolean useWorldCoordinatesForDescriptorBuildUp() { return true; }
}
