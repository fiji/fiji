/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.data;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.network.node.ClusterNode;

import java.io.Serializable;
/**
 * An abstract class to help support objects that might not otherwise be Serializable
 *
 * This class should be considered volatile and subject to change. Use at your own risk.
 *
 * @author Larry Lindsey
 */

public abstract class DataChunk<T> implements Serializable
{
    private static final long serialVersionUID = 3307652134796685516L;

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
