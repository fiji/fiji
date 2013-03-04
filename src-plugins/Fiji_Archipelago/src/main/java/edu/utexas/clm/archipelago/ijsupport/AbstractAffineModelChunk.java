/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.ijsupport;

import edu.utexas.clm.archipelago.data.DataChunk;
import mpicbg.models.*;


public class AbstractAffineModelChunk extends DataChunk<AbstractAffineModel2D<?>>
{

    
/*
TranslationModel2D();
RigidModel2D();
SimilarityModel2D();
AffineModel2D();
HomographyModel2D();
*/
    
    private static enum ModelType
    {
        Translation,
        Rigid,
        Similarity,
        Affine,
        Homography
    }
    
    //private final ModelType type;
    
    public AbstractAffineModelChunk(AbstractAffineModel2D<?> model2D)
    {
      /*  if (model2D instanceof TranslationModel2D)
        {
            type = ModelType.Translation;
        }
        else if (model2D instanceof RigidModel2D)
        {
            type = ModelType.Rigid;
        }
        else if (model2D instanceof SimilarityModel2D)
        {
            type = ModelType.Similarity;
        }
        else if ()*/
    }

    @Override
    public AbstractAffineModel2D<?> getData() {
        return null;
    }
}
