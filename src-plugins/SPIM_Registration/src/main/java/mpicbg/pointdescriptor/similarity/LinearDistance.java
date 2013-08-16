package mpicbg.pointdescriptor.similarity;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class LinearDistance implements SimilarityMeasure
{
	@Override
	public double getSimilarity( final ArrayList<PointMatch> matches )
	{
		final int numDimensions = matches.get( 0 ).getP1().getL().length;
		
		double difference = 0;
		
		for ( final PointMatch match : matches )
			difference += Point.distance( match.getP1(), match.getP2() );
						
		return difference / (double)numDimensions;		
	}
}
