package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.LongArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingLongArrays extends LongArray
{
	final List< long[] > arrays;
	
	int i = 0;
	
	public ExistingLongArrays( final List< long[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public LongArray createArray( final int numEntities ) { return new LongArray( arrays.get( i++ ) ); }
}
