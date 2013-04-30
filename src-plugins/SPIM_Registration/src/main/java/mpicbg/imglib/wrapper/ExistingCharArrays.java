package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.CharArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingCharArrays extends CharArray
{
	final List< char[] > arrays;
	
	int i = 0;
	
	public ExistingCharArrays( final List< char[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public CharArray createArray( final int numEntities ) { return new CharArray( arrays.get( i++ ) ); }
}
