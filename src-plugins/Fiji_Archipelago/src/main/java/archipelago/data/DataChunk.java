package archipelago.data;

import archipelago.FijiArchipelago;
import archipelago.network.ClusterNode;

import java.io.Serializable;
import java.net.InetAddress;

public abstract class DataChunk<T> implements Serializable, Iterable<DataChunk<T>>
{
    
    private long lastOn;
    private long lastTime = -1;
    private long id;
    private final int mark;

    public DataChunk()
    {
        lastOn = -1;
        id = FijiArchipelago.getUniqueID();
        mark = 0;
    }
    
    public DataChunk(DataChunk chunk)
    {
        lastOn = -1;
        id = chunk.id;
        mark = chunk.mark + 1;
    }
    
    public int getMark()
    {
        return mark;
    }
    
    public long getID()
    {
        return id;
    }
    
    public void setProcessingOn(ClusterNode node)
    {
        lastOn = node.getID();
        lastTime = System.currentTimeMillis();
    }
    
    public long lastProcessedOn()
    {
        return lastOn;
    }
    
    public boolean isWhole()
    {
        return true;
    }
    
    public boolean placeSubChunk(DataChunk<T> chunk)
    {
        return false;
    }
    
    public boolean canPlaceChunk(DataChunk<T> chunk)
    {
        return false;
    }


    /**
     * Returns the number of sub-chunks this DataChunk may contain,
     * returns 0 if this DataChunk is indivisible.
     * @return the number of sub-chunks this DataChunk may contain.
     */
    public int getSize()
    {
        return 0;
    }

    /**
     * Returns the data contained in this DataChunk, or null if that is
     * infeasible
     * @return the data contained in this DataChunk
     */
    public abstract T getData();


}
