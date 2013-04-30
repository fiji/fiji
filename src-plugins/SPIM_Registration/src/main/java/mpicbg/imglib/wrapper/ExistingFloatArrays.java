package mpicbg.imglib.wrapper;

import java.util.List;

import mpicbg.imglib.container.basictypecontainer.array.FloatArray;

/**
 * Instead of creating new cell arrays, it return the corresponding array from a predefined list
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class ExistingFloatArrays extends FloatArray
{
	final List< float[] > arrays;
	
	int i = 0;
	
	public ExistingFloatArrays( final List< float[] > arrays )
	{
		super( 1 );
		
		this.arrays = arrays;
	}
	
	@Override
	public FloatArray createArray( final int numEntities ) { return new FloatArray( arrays.get( i++ ) ); }
}
