package mpicbg.pointdescriptor.model;

import mpicbg.models.AbstractModel;
import mpicbg.models.Model;
import mpicbg.pointdescriptor.AbstractPointDescriptor;

/**
 * This class is a subtle hint that {@link Model}s which are used to fit {@link AbstractPointDescriptor}s should be translation invariant. 
 * 
 * @author Stephan Preibisch (preibisch@mpi-cbg.de)
 *
 * @param <M> something that extends {@link Model}
 */
public abstract class TranslationInvariantModel< M extends TranslationInvariantModel< M > > extends AbstractModel< M >
{
	/**
	 * The {@link TranslationInvariantModel} can tell which dimensions it supports.
	 * 
	 * @param numDimensions - The dimensionality (e.g. 3 means 3-dimensional) 
	 * @return - If the {@link TranslationInvariantModel} supports that dimensionality
	 */
	public abstract boolean canDoNumDimension( int numDimensions );
}
