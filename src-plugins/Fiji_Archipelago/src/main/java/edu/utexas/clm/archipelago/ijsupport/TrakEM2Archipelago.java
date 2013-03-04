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

package edu.utexas.clm.archipelago.ijsupport;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.listen.ClusterStateListener;
import ini.trakem2.plugin.TPlugIn;
import mpicbg.trakem2.concurrent.DefaultExecutorProvider;
import mpicbg.trakem2.concurrent.ExecutorProvider;
import mpicbg.trakem2.concurrent.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


public class TrakEM2Archipelago implements TPlugIn
{
    private class ProviderListener implements ClusterStateListener        
    {
        private final AtomicBoolean doneSwitched;
        
        public ProviderListener()
        {
            doneSwitched = new AtomicBoolean(false);
        }
        
        public synchronized void stateChanged(Cluster cluster)
        {
            switch(cluster.getState())
            {
                case STOPPED:
                case STOPPING:
                    if (!doneSwitched.getAndSet(true))
                    {
                        FijiArchipelago.log("TrakEM2 now using the " +
                                "Default ExecutorService Provider");

                        if (ThreadPool.getProvider() instanceof ClusterProvider)
                        {
                            ThreadPool.setProvider(new DefaultExecutorProvider());
                        }
                    }
            }
        }
    }
    
    private class ClusterProvider implements ExecutorProvider
    {
        private final Cluster cluster;
        
        public ClusterProvider(final Cluster c)
        {
            cluster = c;
        }
        
        public ExecutorService getService(int nThreads) {
            return cluster.getService(nThreads);
        }

        public ExecutorService getService(float fractionThreads) {
            return cluster.getService(fractionThreads);
        }
    }
    
    public boolean setup(Object... params)
    {
        Cluster cluster;        
        if (Cluster.initializedCluster())
        {
            cluster = Cluster.getCluster();
            if (cluster.numRegisteredUIs() <= 0)
            {
                if (!FijiArchipelago.runClusterGUI())
                {
                    return false;
                }
            }
        }
        else
        {
            if (!FijiArchipelago.runClusterGUI())
            {
                return false;
            }
            cluster = Cluster.getCluster();
        }

        cluster.addStateListener(new ProviderListener());
        ThreadPool.setProvider(new ClusterProvider(cluster));

        FijiArchipelago.log("TrakEM2 now using the Cluster");
        
        return true;
    }

    public Object invoke(Object... params)
    {
        return null;
    }

    public boolean applies(Object ob)
    {
        return false;
    }
}
