package mpicbg.imglib.algorithm.extremafinder;

import java.util.ArrayList;
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
	protected T threshold;
	
	@Override
	public void allowEdgeExtrema(boolean flag) {
		this.allowEdgeMax = flag;
	}

	@Override
	public ArrayList< ArrayList<int[]>> getRegionalExtrema() {
		return maxima;
	}
	
	@Override
	public ArrayList< float[] > getRegionalExtremaCenters() {
		ArrayList<float[]> centeredRegionalMaxima = new ArrayList<float[]>();
		ArrayList<ArrayList<int[]>> regionalMaxima = new ArrayList<ArrayList<int[]>>(maxima); // make a copy
		ArrayList<int[]> curr = null;
		while (!regionalMaxima.isEmpty()) {
			curr = regionalMaxima.remove(0);
			float averagedCoord[] = findAveragePosition(curr);
			centeredRegionalMaxima.add(averagedCoord);
		}
		return centeredRegionalMaxima;
	}

	public void setThreshold(T threshold) {
		this.threshold = threshold;
	};
	

	/**
	 * Given an ArrayList of int[] (coordinates), computes the averaged coordinates and returns them.
	 * This will always return a 3-elements arrays, even for 2D.
	 * 
	 * @param searched
	 * @return
	 */
	protected float[] findAveragePosition(final ArrayList<int[]> coords) {
		int nDims = coords.get(0).length;
		final float[] array = new float[3];
		int[] curr;
		for (int j = 0; j < coords.size(); j++) {
			curr = coords.get(j);
			for (int i = 0; i<nDims; i++) 
				array[i] += curr[i];
		}
		for (int i = 0; i < array.length; i++)
			array[i] /= coords.size();
		return array;
	}
	
	@Override
	public void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy) {
		this.outOfBoundsFactory = strategy;		
	}
}
