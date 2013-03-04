package edu.utexas.clm.archipelago.data;

import java.io.Serializable;

/**
 *
 */
public class HeartBeat implements Serializable
{
    public static final int MB = 1048576;
    
    public final int ramMBAvailable;
    public final int ramMBTotal;
    public final int ramMBMax;
    
    public HeartBeat(final long availableBytes, final long totalBytes, final long maxBytes)
    {
        ramMBAvailable = (int)(availableBytes/ MB);
        ramMBTotal = (int)(totalBytes/ MB);
        ramMBMax = (int)(maxBytes/ MB);
    }
}
