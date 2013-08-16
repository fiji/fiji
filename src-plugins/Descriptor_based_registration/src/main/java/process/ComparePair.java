package process;

import java.util.ArrayList;

import mpicbg.models.Model;
import mpicbg.models.PointMatch;

public class ComparePair 
{
	final public int indexA, indexB;
	public Model<?> model;
	final public ArrayList<PointMatch> inliers = new ArrayList<PointMatch>();
	
	public ComparePair( final int indexA, final int indexB, final Model<?> model )
	{
		this.indexA = indexA;
		this.indexB = indexB;
		this.model = model.copy();
	}
	
}
