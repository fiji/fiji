package edu.utexas.clm.archipelago.data;

import java.io.Serializable;

public class Duplex<A, B> implements Serializable
{
    public final A a;
    public final B b;
    
    public Duplex(final A a, final B b)
    {
        this.a = a;
        this.b = b;
    }
    
}
