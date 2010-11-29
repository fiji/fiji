package mpicbg.pointdescriptor.matcher;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.AbstractPointDescriptor;
import mpicbg.pointdescriptor.LinkedPoint;
import mpicbg.pointdescriptor.fit.FitResult;

public interface Matcher
{
	/**
	 * @return An {@link ArrayList} of corresponding set of {@link PointMatch}es which contain {@link LinkedPoint}s linking to the actual {@link Point} instance they are created from
	 */
	public < P extends Point, F extends AbstractPointDescriptor<P, F> > ArrayList<ArrayList<PointMatch>> createCandidates( AbstractPointDescriptor<P, F> pd1, AbstractPointDescriptor<P, F> pd2 );
	
	/**
	 * Computes a normalization factor for the case that the different set of {@link PointMatch}es are not comparable 
	 * (for example number of neighbors used is not constant)
	 * 
	 * @param matches the set of {@link PointMatch}es
	 * @return The normalization factor for a certain set of {@link PointMatch}es 
	 */
	public double getNormalizationFactor( final ArrayList<PointMatch> matches, FitResult fitResult );
	
	/**
	 * @return The number of nearest neighbors required for this {@link Matcher} 
	 */
	public int getRequiredNumNeighbors();
}
