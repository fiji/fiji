package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.IntArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingIntArrays extends IntArray
{
	final List< int[] > arrays;
	
	int i = 0;
	
	public ExistingIntArrays( final List< int[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public IntArray createArray( final int numEntities ) { return new IntArray( arrays.get( i++) ); }

}
