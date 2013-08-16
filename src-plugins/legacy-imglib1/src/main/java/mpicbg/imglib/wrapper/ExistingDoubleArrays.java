package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingDoubleArrays extends DoubleArray
{
	final List< double[] > arrays;
	
	int i = 0;
	
	public ExistingDoubleArrays( final List< double[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public DoubleArray createArray( final int numEntities ) { return new DoubleArray( arrays.get( i++ ) ); }
}
