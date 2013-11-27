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
import edu.utexas.clm.archipelago.ijsupport.bottle.LayerBottler;
import edu.utexas.clm.archipelago.ijsupport.bottle.PatchBottler;
import edu.utexas.clm.archipelago.ijsupport.bottle.PointBottler;
import edu.utexas.clm.archipelago.ijsupport.bottle.SIFTParamBottler;
import edu.utexas.clm.archipelago.listen.ClusterStateListener;
import edu.utexas.clm.archipelago.network.client.ArchipelagoClient;
import ini.trakem2.ControlWindow;
import ini.trakem2.Project;
import ini.trakem2.persistence.FSLoader;
import ini.trakem2.plugin.TPlugIn;
import ini.trakem2.parallel.ExecutorProvider;
import ini.trakem2.parallel.DefaultExecutorProvider;
import ini.trakem2.utils.Utils;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


public class TrakEM2Archipelago implements TPlugIn
{
    private static class LogStream extends PrintStream
    {
        private final ArchipelagoClient client;

        public LogStream(OutputStream stream, ArchipelagoClient client)
        {
            super(stream);
            this.client = client;
        }

        public void println(String str)
        {
            super.println(str);
            if (client != null)
            {
                client.log(str);
            }
        }
    }

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

                        if (ExecutorProvider.getProvider() instanceof ClusterProvider)
                        {
                            ExecutorProvider.setProvider(new DefaultExecutorProvider());
                        }
                    }
            }
        }
    }
    
    private class ClusterProvider extends ExecutorProvider
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

        cluster.addBottler(new PointBottler());
        cluster.addBottler(new LayerBottler());
        cluster.addBottler(new PatchBottler());
        cluster.addBottler(new SIFTParamBottler());

        cluster.addStateListener(new ProviderListener());
        ExecutorProvider.setProvider(new ClusterProvider(cluster));

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

    public static File getFile(final Project p)
    {
        FSLoader loader = (FSLoader)p.getLoader();
        return new File(loader.getProjectXMLPath());
    }

    public static synchronized Project getProject(final File projectFile)
    {
        for (final Project p : Project.getProjects())
        {
            //FSLoader loader = (FSLoader)p.getLoader();
            if (projectFile.equals(getFile(p)))
            {
                return p;
            }
        }
        ControlWindow.setGUIEnabled(false);

        if (!(Utils.getLogStream() instanceof LogStream ))
        {
            Utils.setLogStream(new LogStream(System.out, ArchipelagoClient.getFirstClient()));
        }

        return Project.openFSProject(projectFile.getAbsolutePath(), false);
    }
}
