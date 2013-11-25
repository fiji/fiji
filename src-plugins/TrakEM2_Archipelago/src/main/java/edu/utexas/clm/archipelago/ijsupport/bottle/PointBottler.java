package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import edu.utexas.clm.archipelago.network.translation.Bottler;
import mpicbg.models.Point;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 */
public class PointBottler implements Bottler<Point>
{
    private boolean isOrigin = true;

    public boolean accepts(final Object o)
    {
        return o instanceof Point;
    }

    public synchronized Bottle<Point> bottle(final Object o, final MessageXC xc)
    {
        return new PointBottle((Point)o, isOrigin);
    }

    public boolean transfer()
    {
        return true;
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();

        isOrigin = false;
    }
}
