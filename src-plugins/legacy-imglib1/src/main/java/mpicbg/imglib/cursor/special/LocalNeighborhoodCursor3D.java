/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LocalNeighborhoodCursor3D<T extends Type<T>> extends LocalNeighborhoodCursor<T>
{
	int i = -1;
	
	public LocalNeighborhoodCursor3D( final LocalizableByDimCursor<T> cursor ) 
	{ 
		super( cursor );
		
		if ( numDimensions != 3 )
		{
			System.out.println( "LocalNeighborhoodCursor3D.constructor(): Error, dimensionality is not 3 but " + numDimensions + ", I have to close." );
			close();
		}
	}
	
	@Override
	public void reset()
	{
		if (i == 25)
			cursor.bck( 2 );
		else
			cursor.setPosition( position );
		
		i = -1;
	}
	
	@Override
	public void update()
	{
		cursor.getPosition( position );		
		i = -1;
	}
	
	@Override
	public boolean hasNext()
	{
		if (i < 25)
			return true;
		else 
			return false;		
	}
	
	@Override
	public void fwd()
	{
		// acces plan for neighborhood, starting at the 
		// center position (x)
		//
		// upper z plane (z-1)
		// -------------
		// | 2 | 1 | 8 |
		// |------------
		// | 3 | 0 | 7 |
		// |------------
		// | 4 | 5 | 6 |
		// -------------
		//
		// mid z plane (z=0)
		// -------------
		// | 11| 10| 9 |
		// |------------
		// | 12| x | 16|
		// |------------
		// | 13| 14| 15|
		// -------------
		//
		// lower z plane(z+1)
		// -------------
		// | 20| 19| 18|
		// |------------
		// | 21| 25| 17|
		// |------------
		// | 22| 23| 24|
		// -------------
		//
		
		switch( i )		
		{
			case -1: cursor.bck( 2 ); break;
			case  0: cursor.bck( 1 ); break;
			case  1: cursor.bck( 0 ); break;
			case  2: cursor.fwd( 1 ); break;
			case  3: cursor.fwd( 1 ); break;
			case  4: cursor.fwd( 0 ); break;
			case  5: cursor.fwd( 0 ); break;
			case  6: cursor.bck( 1 ); break;
			case  7: cursor.bck( 1 ); break;			
			case  8: cursor.fwd( 2 ); break;
			
			case  9: cursor.bck( 0 ); break;
			case 10: cursor.bck( 0 ); break;
			case 11: cursor.fwd( 1 ); break;
			case 12: cursor.fwd( 1 ); break;
			case 13: cursor.fwd( 0 ); break;
			case 14: cursor.fwd( 0 ); break;
			case 15: cursor.bck( 1 ); break;
			case 16: cursor.fwd( 2 ); break;

			case 17: cursor.bck( 1 ); break;
			case 18: cursor.bck( 0 ); break;
			case 19: cursor.bck( 0 ); break;
			case 20: cursor.fwd( 1 ); break;
			case 21: cursor.fwd( 1 ); break;
			case 22: cursor.fwd( 0 ); break;
			case 23: cursor.fwd( 0 ); break;
			case 24: cursor.bck( 0 ); cursor.bck( 1 ); break;
		}
		
		++i;
	}
	
	public int getRelativePosition( final int d ) { return cursor.getPosition( d ) - position[ d ]; }	
}
