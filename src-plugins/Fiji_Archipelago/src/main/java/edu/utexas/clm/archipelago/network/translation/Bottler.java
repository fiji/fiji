package edu.utexas.clm.archipelago.network.translation;

import edu.utexas.clm.archipelago.network.MessageXC;

import java.io.Serializable;

/**
 *
 */
public interface Bottler<A> extends Serializable
{
    /**
     * Return true if this Bottler accepts the given Object.
     * @param o the Object, which will be accepted or not.
     * @return true if this Bottler accepts the given Object.
     */
    public boolean accepts(final Object o);

    /**
     * Return a Bottle that will return an equivalent to the argument when unBottle is called.
     * @param o an Object to bottle
     * @return a Bottle that will return an equivalent to the argument when unBottle is called.
     *
     * The Bottle must return an Object with the same signature as the Object argument here.

     */
    public Bottle<A> bottle(final Object o, final MessageXC xc);

    /**
     * Determines whether this Bottler is transferred to the remote node.
     * @return true to transfer, false otherwise.
     */
    public boolean transfer();

}
