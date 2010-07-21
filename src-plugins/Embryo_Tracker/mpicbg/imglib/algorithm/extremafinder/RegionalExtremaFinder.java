package mpicbg.imglib.algorithm.extremafinder;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Provides a quick algorithm for finding regional maxima in 2- or 3-dimensional images.
 * 
 * @author Nick Perry
 *
 * @param <T>
 */
public interface RegionalExtremaFinder<T extends RealType<T>> extends Algorithm, Benchmark {
	
	/**
	 * Returns the ArrayList containing the coordinates of the local extrema found. Each 
	 * element of the ArrayList is a double array, representing the coordinate of the found
	 * extrema, in the same order that of the source {@link Image}.
	 * 
	 * @return  the ArrayList containing the extrema coordinates
	 */
	public ArrayList< ArrayList< int[] > > getRegionalMaxima();
	
	public ArrayList< double[] > getRegionalMaximaCenters(ArrayList< ArrayList< int[] > > regionalMaxima);
	
	/**
	 * If called, then regional <b>maxima</b> will be found.
	 * 
	 */
	public void findMaxima();
	
	/**
	 * If called, then regional <b>minima</b> will be found.
	 * 
	 */
	public void findMinima();
	
	/**
	 * If set to true before the {@link #process()} method is called, then extrema locations
	 * will be interpolated using intensity interpolation by a paraboloid. 
	 * @param flag
	 */
	public void doInterpolate(boolean flag);
	
	/**
	 * If set to true before the {@link #process()} method is called, then extrema found 
	 * at the edges of the image bounds (including time edges) will not be priuned, and will
	 * be included in the result array.
	 * @param flag
	 */
	public void allowEdgeExtrema(boolean flag);
	
	/**
	 * Set the strategy used by this extrema finder to deal with edge pixels.
	 * By default, it is an {@link OutOfBoundsStrategyValueFactory} set with the value
	 * 0 to avoid nasty edge effects.
	 * @param strategy  the strategy to set
	 */
	void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy);
}
