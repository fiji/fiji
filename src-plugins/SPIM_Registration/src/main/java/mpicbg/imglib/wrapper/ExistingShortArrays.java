package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.ShortArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingShortArrays extends ShortArray
{
	final List< short[] > arrays;
	
	int i = 0;
	
	public ExistingShortArrays( final List< short[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public ShortArray createArray( final int numEntities ) { return new ShortArray( arrays.get( i++ ) ); }
}
