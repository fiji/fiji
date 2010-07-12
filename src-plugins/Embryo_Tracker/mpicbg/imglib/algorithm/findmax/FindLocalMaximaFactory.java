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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

public class FindLocalMaximaFactory<T extends RealType<T>> {	
    public LocalMaximaFinder createLocalMaximaFinder(final Image<T> image, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory, boolean allowEdgeMax) {
        if (image.getNumDimensions() == 2) {
        	return new FindMaxima2D<T>(image, outOfBoundsFactory, allowEdgeMax);
        } else if (image.getNumDimensions() == 3) {
        	return new FindMaxima3D<T>(image, outOfBoundsFactory, allowEdgeMax);
        } else {
        	throw new IllegalArgumentException("Image is not 2 or 3 dimensional!");
        }
    }
}
