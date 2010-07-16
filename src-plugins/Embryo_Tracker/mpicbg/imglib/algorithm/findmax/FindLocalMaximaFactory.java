package mpicbg.imglib.algorithm.findmax;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

public class FindLocalMaximaFactory<T extends RealType<T>> {	
    public LocalMaximaFinder createLocalMaximaFinder(final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory, boolean allowEdgeMax) {
        if (image.getNumDimensions() == 2) {
        	return new FindMaxima2D<T>(image, outOfBoundsFactory, allowEdgeMax);
        } else if (image.getNumDimensions() == 3) {
        	return new FindMaxima3D<T>(image, outOfBoundsFactory, allowEdgeMax);
        } else {
        	throw new IllegalArgumentException("Argument " + image.getNumDimensions() + " is not recognized.");
        }
    }
}
