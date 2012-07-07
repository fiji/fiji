package process;

import java.util.ArrayList;

import mpicbg.models.Model;
import mpicbg.models.PointMatch;

public class ComparePair 
{
	final int indexA, indexB;
	Model<?> model;
	final ArrayList<PointMatch> inliers = new ArrayList<PointMatch>();
	
	public ComparePair( final int indexA, final int indexB, final Model<?> model )
	{
		this.indexA = indexA;
		this.indexB = indexB;
		this.model = model.copy();
	}
	
}
