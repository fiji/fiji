/**
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
 * @author Nick Perry
 */

package mpicbg.imglib.algorithm.findmax;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

public interface LocalMaximaFinder<T extends RealType<T>> extends Algorithm {
	
	/**
	 * Returns the ArrayList containing the coordinates of the local extrema found. Each 
	 * element of the ArrayList is a double array, representing the coordinate of the found
	 * extrema, in the same order that of the source {@link Image}.
	 * 
	 * @return  the ArrayList containing the extrema coordinates
	 */
	public ArrayList< double[] > getLocalMaxima();
	
	/**
	 * If set to true before the {@link #process()} method is called, then extrema locations
	 * will be interpolated using intensity interpolation by a paraboloid. 
	 * @param flag
	 */
	public void doInterpolate(boolean flag);
	
	/**
	 * If set to true before the {@link #process()} method is called, then extrema found 
	 * at the edges of the image bounds (including time edges) will not be priuned, and will
	 * be included in the result array.
	 * @param flag
	 */
	public void allowEdgeExtrema(boolean flag);
	
	/**
	 * Set the strategy used by this extrema finder to deal with edge pixels.
	 * By default, it is an {@link OutOfBoundsStrategyValueFactory} set with the value
	 * 0 to avoid nasty edge effects.
	 * @param strategy  the strategy to set
	 */
	void setOutOfBoundsStrategyFactory(OutOfBoundsStrategyFactory<T> strategy);
}
