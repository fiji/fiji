package archipelago.ijsupport;

import archipelago.data.DataChunk;
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
