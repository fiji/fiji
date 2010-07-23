package mpicbg.imglib.algorithm.extremafinder;

import java.util.ArrayList;
import java.util.Iterator;

import fiji.plugin.nperry.Spot;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

public abstract class AbstractRegionalExtremaFinder<T extends RealType<T>> implements RegionalExtremaFinder<T> {

	protected OutOfBoundsStrategyFactory<T> outOfBoundsFactory = new OutOfBoundsStrategyValueFactory<T>();		// holds the outOfBoundsStrategy used by the cursors in this algorithm
	protected boolean allowEdgeMax = false;		// if true, maxima found on the edge of the images will be included in the results; if false, edge maxima are excluded
	protected boolean doInterpolate = false;
	protected Image<T> image;					// holds the image the algorithm is to be applied to
	final protected ArrayList< ArrayList< int[] > > maxima = new ArrayList< ArrayList< int[] > >();	// an array list which holds the coordinates of the maxima found in the image.
	
	@Override
	public void allowEdgeExtrema(boolean flag) {
		this.allowEdgeMax = flag;
	}

	@Override
	public ArrayList< ArrayList< int[] > > getRegionalExtrema() {
		return maxima;
	}
	
	@Override
	public ArrayList< double[] > getRegionalExtremaCenters(boolean doInterpolate)
	{
		ArrayList< double[] > centeredRegionalMaxima = new ArrayList< double[] >();
		ArrayList< ArrayList< int[] >  > regionalMaxima = new ArrayList< ArrayList< int[] >  >(maxima); // make a copy
		ArrayList< int[] > curr = null;
		while (!regionalMaxima.isEmpty()) {
			curr = regionalMaxima.remove(0);
			double averagedCoord[] = findAveragePosition(curr);
			centeredRegionalMaxima.add(averagedCoord);
		}
		return centeredRegionalMaxima;
	}
	
	public ArrayList< Spot > convertToSpots(ArrayList< double[] > coords) {
		ArrayList< Spot > spots = new ArrayList< Spot >();
		Iterator< double[] > itr = coords.iterator();
		while (itr.hasNext()) {
			Spot spot = new Spot(itr.next());
			spots.add(spot);
		}
		return spots;
	}
	
	/**
	 * Given an ArrayList of int[] (coordinates), computes the averaged coordinates and returns them.
	 * 
	 * @param searched
	 * @return
	 */
	protected double[] findAveragePosition(ArrayList < int[] > coords) {
		// Determine dimensionality
		int[] firstArray = coords.get(0);
		int nDims = firstArray.length;
		double[] array = new double[nDims];
		int count = 0;
		while(!coords.isEmpty()) {
			int curr[] = coords.remove(0);
			for (int i = 0; i<nDims; i++) {
				array[i] += curr[i];
			}
			count++;
		}
		for (int i = 0; i < array.length; i++) {
			array[i] /= count;
		}
		return array;
	}
	
	@Override
	public void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy) {
		this.outOfBoundsFactory = strategy;		
	}
}
