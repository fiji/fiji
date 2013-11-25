package edu.utexas.clm.archipelago.network.translation;

import edu.utexas.clm.archipelago.network.MessageXC;

import java.io.File;

/**
 *
 */
public class FileBottler implements Bottler<File>
{

    public boolean accepts(final Object o)
    {
        return o.getClass() == File.class;
    }

    public Bottle<File> bottle(final Object o, final MessageXC xc)
    {
        return new FileBottle((File)o, xc);
    }

    public boolean transfer() {
        return true;
    }
}
