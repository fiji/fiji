package edu.utexas.clm.archipelago.network.translation;

import edu.utexas.clm.archipelago.network.MessageXC;

import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
public interface Bottle<A> extends Serializable
{

    public A unBottle(final MessageXC xc) throws IOException;

}
