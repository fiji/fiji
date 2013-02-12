package archipelago.data;

import java.io.Serializable;

/**
 *
 */
public class HeartBeat implements Serializable
{
    public static final int MB = 1048576;
    
    public final int ramMBAvailable;
    public final int ramMBTotal;
    
    public HeartBeat(final long availableBytes, final long totalBytes)
    {
        ramMBAvailable = (int)(availableBytes/ MB);
        ramMBTotal = (int)(totalBytes/ MB);
    }
}
