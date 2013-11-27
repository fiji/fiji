package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import edu.utexas.clm.archipelago.network.translation.Bottler;
import mpicbg.trakem2.align.RegularizedAffineLayerAlignment;

/**
 *
 */
public class SIFTParamBottler implements Bottler
{
    public boolean accepts(Object o)
    {
        return o instanceof RegularizedAffineLayerAlignment.Param;
    }

    public Bottle bottle(Object o, MessageXC xc)
    {
        return new SIFTParamBottle((RegularizedAffineLayerAlignment.Param)o);
    }

    public boolean transfer()
    {
        return false;
    }
}
