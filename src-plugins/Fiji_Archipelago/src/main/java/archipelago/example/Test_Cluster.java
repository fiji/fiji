package archipelago.example;


import archipelago.Cluster;
import archipelago.FijiArchipelago;
import archipelago.compute.SerializableCallable;
import archipelago.network.node.ClusterNode;
import ij.plugin.PlugIn;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class Test_Cluster implements PlugIn
{
    public static class NullCall implements SerializableCallable<NullCall>
    {
        private final long winks;
        private final int nc;
        private String string;

        public NullCall(final long sleepTime, final int n)
        {
            winks = sleepTime;
            nc = n;
        }

        public NullCall call() throws Exception
        {
            try
            {
                Thread.sleep(winks);
                string = java.net.InetAddress.getLocalHost().getHostName();
            }
            catch (InterruptedException ie)
            {
                string = "interrupted";
            }
            catch (UnknownHostException uhe)
            {
                string = "unknown";
            }

            return this;
        }
    }
    
    private ArrayList<Future<NullCall>> submitJobs(final ExecutorService[] executors,
                                                   final int[] coreCount,
                                                   final int numCalls)
    {
        final ArrayList<Future<NullCall>> futures = new ArrayList<Future<NullCall>>(numCalls);
        int eid = 0;

        for (int i = 0; i < numCalls; ++i)
        {
            int n = coreCount[eid];
            futures.add(executors[eid].submit(new NullCall(5000, n)));
            ++eid;

            if (eid >= executors.length)
            {
                eid = 0;
            }
        }

        return futures;
    }


    private void logResults(final List<Future<NullCall>> futures)
    {
        for (Future<NullCall> future : futures)
        {
            try
            {
                NullCall nullCall = future.get();
                if (nullCall != null)
                {
                    FijiArchipelago.log("Callable requested " + nullCall.nc + ", ran on " +
                            nullCall.string);
                }
                else
                {
                    FijiArchipelago.log("Callable returned null");
                }
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.err("Test: Interrupted!");
                return;
            }
            catch (ExecutionException ee)
            {
                FijiArchipelago.err("Test: " + ee);
                return;
            }

        }
    }

    public void run(String arg)
    {
        if (Cluster.activeCluster() || FijiArchipelago.runClusterGUI())
        {
            final int[] coreCount= {4, 128};
            int numCalls;
            final Cluster cluster = Cluster.getCluster();
            int clusterMaxThreads;

            boolean ok = true;
            final ExecutorService[] executors = new ExecutorService[coreCount.length];
            List<Future<NullCall>> futures;
            Thread t;
            
            cluster.waitUntilReady();
            try
            {
                cluster.waitForAllNodes(3000000);
            }
            catch (TimeoutException toe)
            {
                FijiArchipelago.log("Test: Timed out waiting for nodes, continuing anyway");
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.log("Test: Interrupted while waiting, quitting.");
                return;
            }
            
            clusterMaxThreads = cluster.getMaxThreads();

            for (int i = 0; i < coreCount.length; ++i)
            {
                executors[i] = cluster.getService(coreCount[i]);
            }

            numCalls = cluster.getNodes().size() * 4;

            logResults(submitJobs(executors, coreCount, numCalls));
            
            futures = submitJobs(executors, coreCount, numCalls);

            for (ClusterNode node : cluster.getNodes())
            {
                if (ok && node.getThreadLimit() == clusterMaxThreads)
                {
                    ok = false;
                }
                else
                {
                    node.close();
                }
            }
            
            logResults(futures);

            final List<Future<NullCall>> finalFutures = 
                    submitJobs(executors, coreCount, numCalls);
            
            t = new Thread()
            {
                public void run()
                {
                    logResults(finalFutures);
                }
            };
            
            t.start();
            
            executors[0].shutdown();
            
            try
            {
                t.join();
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.log("Test: Interrupted!");
            }
        }
        
        
    }
}
