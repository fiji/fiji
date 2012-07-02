package mpicbg.stitching.fusion;

import java.util.ArrayList;
import java.util.Collections;

public class MedianPixelFusion implements PixelFusion
{
	final ArrayList< Float > list;
	
	public MedianPixelFusion() 
	{
		list = new ArrayList< Float >();
		clear(); 
	}
	
	@Override
	public void clear() { list.clear();	}

	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition ) 
	{
		list.add( value );
	}

	@Override
	public float getValue() 
	{ 
		if ( list.size() == 0 )
		{
			return 0;
		}
		else
		{
			Collections.sort( list );
			final int size = list.size();
			
			if ( size % 2 == 1 )
				return list.get( size/2 );
			else
				return (list.get( size/2 - 1) + list.get( size/2 ))/2.0f ;
		}
	}
	
	@Override
	public PixelFusion copy() { return new MedianPixelFusion(); }
}
