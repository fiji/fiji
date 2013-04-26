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

package edu.utexas.clm.archipelago.compute;

import edu.utexas.clm.archipelago.network.node.ClusterNode;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 *
 * @author Larry Lindsey
 */
public class ProcessManager<T> implements Runnable, Serializable
{
    private static final long serialVersionUID = -5729538939085054236L;

    private Callable<T> callable;
    private T output;
    private final long id;
    private Exception remoteException;
    private long runningOn;
    private final float numCores;
    private final boolean isFractional;
     
    
    //public <S extends Callable<T> & Serializable> ProcessManager(final S c, final ProcessListener pl, long idArg)
    
    public ProcessManager(final Callable<T> c, final long idArg, final float nc, final boolean f)
    {
        callable = c;
        output = null;
        id = idArg;
        remoteException = null;
        runningOn = -1;
        numCores = nc;
        isFractional = f;
    }

    /**
     * Runs this ProcessManager. This will typically be called on a remote node.
     */
    public void run()
    {
        try
        {
            output = callable.call();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            remoteException = e;
        }
        // Nullify the callable so we don't have to transfer extra data back home.
        callable = null;
    }

    public synchronized void setRunningOn(final ClusterNode node)
    {
        runningOn = node == null ? -1 : node.getID();
    }

    public long getRunningOn()
    {
        return runningOn;
    }
    
    public T getOutput()
    {
        return output;
    }
    
    public Callable<T> getCallable()
    {
        return callable;
    }

    public long getID()
    {
        return id;
    }
    
    public Exception getRemoteException()
    {
        return remoteException;
    }
    
    public int requestedCores(int totalCores)
    {
        int c;
        if (isFractional)
        {
            c = (int)(numCores * (float)totalCores);
        }
        else
        {
            c = (int)numCores;
        }

        return c > 0 ? c : 1;
    }
    
    public int requestedCores(ClusterNode node)
    {
        return requestedCores(node.getThreadLimit());
    }
    
}
