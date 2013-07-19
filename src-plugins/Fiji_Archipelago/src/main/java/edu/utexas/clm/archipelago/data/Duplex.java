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

    public int hashCode()
    {
        return 37 * a.hashCode() + b.hashCode();
    }

    public boolean equals(Object o)
    {
        if (o instanceof Duplex)
        {
            Duplex dup = (Duplex)o;
            return dup.a.equals(a) && dup.b.equals(b);
        }
        else
        {
            return false;
        }
    }

    public String toString()
    {
        return a.toString() + " " + b.toString();
    }
    
}
