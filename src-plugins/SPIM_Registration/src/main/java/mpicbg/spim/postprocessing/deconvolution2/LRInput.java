package mpicbg.spim.postprocessing.deconvolution2;

import java.util.ArrayList;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.spim.postprocessing.deconvolution2.LRFFT.PSFTYPE;

public class LRInput 
{
	public final static float minValue = 0.0001f;	
	final ArrayList< LRFFT > views = new ArrayList<LRFFT>();
	
	public void add( final LRFFT view )
	{
		views.add( view );
		
		for ( final LRFFT v : views )
			v.setNumViews( getNumViews() );
	}
		
	/**
	 * init all views
	 * 
	 * @param exponentialKernel - use exponential kernel?
	 * 
	 * @return the same instance again for convinience
	 */
	public LRInput init( final PSFTYPE iterationType )
	{
		for ( final LRFFT view : views )
			view.init( iterationType, views );
		
		return this;
	}
	
	/**
	 * @return - the image data
	 */
	public ArrayList< LRFFT > getViews() { return views; }
	
	/**
	 * The number of views for this deconvolution
	 * @return
	 */
	public int getNumViews() { return views.size(); }
	
	@Override
	public LRInput clone()
	{
		LRInput clone = new LRInput();
		
		for ( final LRFFT view : views )
			clone.add( view.clone() );
		
		return clone;
	}
}
