package edu.utexas.clm.archipelago.data;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.network.node.ClusterNode;

import java.io.Serializable;
/**
 *
 * @author Larry Lindsey
 */

public abstract class DataChunk<T> implements Serializable
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
    
    /**
     * Returns the data contained in this DataChunk, or null if that is
     * infeasible
     * @return the data contained in this DataChunk
     */
    public abstract T getData();


}
