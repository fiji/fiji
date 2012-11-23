package mpicbg.spim.postprocessing.deconvolution2;

import java.util.ArrayList;
import java.util.Collections;

public class CUDATools 
{
	public static int getSuggestedSizes( final int size, final int dim )
	{
		if ( dim == 0 )
		{
			// for dim 0 sizes which are multiplicative of 32 are good
			int cudaSize = size / 32;
			
			if ( size % 32 != 0 )
				cudaSize += 32;
			
			return cudaSize;
		}
		else
		{
			return 0;
		}
	}
	
	public static int[] getSizes( final int min, final int max )
	{
		ArrayList< Integer > list1 = new ArrayList<Integer>();
		
		for ( int exp = 2; exp < 14; ++exp )
		{
			long value = (int)Math.round( Math.pow( 2, exp ) );
			
			if ( value < 4096 && value > 0 )
				list1.add( (int)value );

			value = (int)Math.round( Math.pow( 3, exp ) );
			
			if ( value < 4096 && value > 0 )
				list1.add( (int)value );
			
			value = (int)Math.round( Math.pow( 5, exp ) );
			
			if ( value < 4096 && value > 0 )
				list1.add( (int)value );
			
			value = (int)Math.round( Math.pow( 7, exp ) );
			
			if ( value < 4096 && value > 0 )
				list1.add( (int)value );
		}
		
		Collections.sort( list1 );
		
		System.out.println();
		for ( final int i : list1 )
			System.out.println( i );
		
		System.exit( 0 );
		
		return null;
	}
	
}
