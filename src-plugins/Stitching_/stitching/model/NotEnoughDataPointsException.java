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

import java.lang.Exception;

/**
 * Signalizes that there were not enough data points available to estimate the
 * {@link Model}.
 * 
 */
public class NotEnoughDataPointsException extends Exception
{
	public NotEnoughDataPointsException()
	{
		super( "Not enough data points to solve the Model." );
	}
	
	
	public NotEnoughDataPointsException( String message )
	{
		super( message );
	}
	
	
	public NotEnoughDataPointsException( Throwable cause )
	{
		super( cause );
	}
	
	
	public NotEnoughDataPointsException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
