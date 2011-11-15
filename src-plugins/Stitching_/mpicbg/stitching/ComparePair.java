package mpicbg.stitching;

import ij.ImagePlus;
import mpicbg.models.Model;

public class ComparePair 
{
	final ImagePlus impA, impB;
	final int timePointA, timePointB;
	Model<?> model;
	float crossCorrelation;
	
	public ComparePair( final ImagePlus impA, final ImagePlus impB, final int timePointA, final int timePointB, final Model<?> model )
	{
		this.impA = impA;
		this.impB = impB;
		this.timePointA = timePointA;
		this.timePointB = timePointB;
		this.model = model.copy();
	}
	
	public ImagePlus getImagePlus1() { return impA; }
	public ImagePlus getImagePlus2() { return impB; }
	public int getTimePoint1() { return timePointA; }
	public int getTimePoint2() { return timePointB; }
	public Model< ? > getModel() { return model; }
	public void setModel( final Model< ? > model ) { this.model = model; }
	public void setCrossCorrelation( final float r ) { this.crossCorrelation = r; }
	public float getCrossCorrelation() { return crossCorrelation; }
}
