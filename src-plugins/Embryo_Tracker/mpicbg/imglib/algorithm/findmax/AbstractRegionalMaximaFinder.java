package mpicbg.imglib.algorithm.findmax;

import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

public abstract class AbstractRegionalMaximaFinder<T extends RealType<T>> implements RegionalMaximaFinder<T> {

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
	public void doInterpolate(boolean flag) {
		this.doInterpolate = flag;
	}

	@Override
	public ArrayList< ArrayList< int[] > > getRegionalMaxima() {
		return maxima;
	}
	
	@Override
	public void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy) {
		this.outOfBoundsFactory = strategy;		
	}
}
