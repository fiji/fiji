package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.ByteArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingByteArrays extends ByteArray
{
	final List< byte[] > arrays;
	
	int i = 0;
	
	public ExistingByteArrays( final List< byte[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public ByteArray createArray( final int numEntities ) { return new ByteArray( arrays.get( i++ ) ); }
}
