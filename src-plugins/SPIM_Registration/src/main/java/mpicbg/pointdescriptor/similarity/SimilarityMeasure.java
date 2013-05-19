package mpicbg.pointdescriptor.similarity;

import java.util.ArrayList;

import mpicbg.models.PointMatch;

public interface SimilarityMeasure
{
	public double getSimilarity( final ArrayList<PointMatch> matches );
}
