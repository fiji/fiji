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
 */
package mpicbg.panorama;

import ij.process.ImageProcessor;
import mpicbg.ij.InverseTransformMapping;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class HomogeneousMapping< T extends InverseCoordinateTransform > extends InverseTransformMapping< T >
{
	public HomogeneousMapping( final T t )
	{
		super( t );
	}
	
	//@Override
	public void map(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final float[] t = new float[ 3 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			final int row = tw * y;
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				t[ 2 ] = 1f;
				try
				{
					transform.applyInverseInPlace( t );
					final float h = t[ 2 ];
					final int tx = ( int )( t[ 0 ] + 0.5f );
					final int ty = ( int )( t[ 1 ] + 0.5f );
					if (
							h >= 0 &&
							tx >= 0 &&
							tx <= sw &&
							ty >= 0 &&
							ty <= sh )
						target.set( row + x, source.getPixel( tx, ty ) );
				}
				catch ( NoninvertibleModelException e ){}
			}
		}
	}
	
	//@Override
	public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final float[] t = new float[ 3 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				t[ 2 ] = 1;
				try
				{
					transform.applyInverseInPlace( t );
					final float h = t[ 2 ];
					final float tx = t[ 0 ];
					final float ty = t[ 1 ];
					if (
							h >= 0 &&
							tx >= 0 &&
							tx <= sw &&
							ty >= 0 &&
							ty <= sh )
						target.putPixel( x, y, source.getPixelInterpolated( tx, ty ) );
				}
				catch ( NoninvertibleModelException e ){}
			}
		}
	}
}
