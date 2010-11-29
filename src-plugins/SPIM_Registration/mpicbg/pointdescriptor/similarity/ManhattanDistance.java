package mpicbg.pointdescriptor.similarity;

import java.util.ArrayList;

import mpicbg.models.PointMatch;

public class ManhattanDistance implements SimilarityMeasure
{
	@Override
	public double getSimilarity( final ArrayList<PointMatch> matches )
	{
		final int numDimensions = matches.get( 0 ).getP1().getL().length;
		
		double difference = 0;
		
		for ( final PointMatch match : matches )
		{
			final float[] t1 = match.getP2().getW();
			final float[] t2 = match.getP1().getW();
		
			for ( int d = 0; d < numDimensions; ++d )
				difference += Math.abs( t1[ d ] - t2[ d ] );
		}
						
		return difference / (double)numDimensions;		
	}
}
