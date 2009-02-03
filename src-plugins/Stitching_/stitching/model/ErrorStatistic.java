/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html
 *   
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package stitching.model;

import java.util.ArrayList;
import java.util.Collections;
import java.lang.IndexOutOfBoundsException;
import java.util.ListIterator;

public class ErrorStatistic
{
	final public ArrayList< Double > values = new ArrayList< Double >();
	final public ArrayList< Double > slope = new ArrayList< Double >();
	final public ArrayList< Double > sortedValues = new ArrayList< Double >();
	
	public double var0 = 0;		// variance relative to 0
	public double var = 0;		// variance relative to mean
	public double std0 = 0;		// standard-deviation relative to 0
	public double std = 0;		// standard-deviation
	public double mean = 0;
	public double median = 0;
	public double min = Double.MAX_VALUE;
	public double max = 0;
	
	final public void add( double new_value )
	{
		if (values.size() > 1 ) slope.add( new_value - values.get( values.size() - 1 ) );
		else slope.add( 0.0 );
		mean = ( mean * values.size() + new_value );
		values.add( new_value );
		mean /= values.size();
		
		var0 += new_value * new_value / ( double )( values.size() - 1 );
		std0 = Math.sqrt( var0 );
		
		double tmp = new_value - mean;
		var += tmp * tmp / ( double )( values.size() - 1 );
		std = Math.sqrt( var );
		
		sortedValues.add( new_value );
		Collections.sort( sortedValues );
		
		if ( sortedValues.size() % 2 == 0 )
		{
			int m = sortedValues.size() / 2;
			median = ( sortedValues.get( m - 1 ) + sortedValues.get( m ) ) / 2.0;
		}
		else
			median = sortedValues.get( sortedValues.size() / 2 );
		
		if ( new_value < min ) min = new_value;
		if ( new_value > max ) max = new_value;
	}
	
	final public double getWideSlope( int width ) throws IndexOutOfBoundsException
	{
		if ( width > slope.size() ) throw new IndexOutOfBoundsException( "Cannot estimate a wide slope for width larger than than the number of sample." );
		ListIterator< Double > li = slope.listIterator( slope.size() - 1 );
		int i = 0;
		double s = 0.0;
		while ( i < width && li.hasPrevious() )
		{
			s += li.previous();
			++i;
		}
		s /= ( double )width;
		return s;
	}
}
