package archipelago.example;


import archipelago.Cluster;
import archipelago.FijiArchipelago;
import archipelago.compute.SerializableCallable;
import archipelago.network.node.ClusterNode;
import ij.plugin.PlugIn;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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

    Cluster cluster;
    ExecutorService[] executors;
    final int[] coreCount= {4, 128};
    int numCalls;
    
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

    private ArrayList<NullCall> createCallables (final int coreCount,
                                                 final int numCalls)
    {
        final ArrayList<NullCall> callables = new ArrayList<NullCall>(numCalls);

        for (int i = 0; i < numCalls; ++i)
        {
            callables.add(new NullCall(5000, coreCount));
        }

        return callables;
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

    private void basicTest()
    {
        FijiArchipelago.log("Basic Test");
        logResults(submitJobs(executors, coreCount, numCalls));
        FijiArchipelago.log("Basic Test Complete");
    }
    
    private void midCancelTest()
    {
        FijiArchipelago.log("Mid Cancel Test");
        List<Future<NullCall>> futures = submitJobs(executors, coreCount, numCalls);
        boolean ok = true;
        int clusterMaxThreads = cluster.getMaxThreads();

        for (ClusterNode node : cluster.getNodes())
        {
            if (ok && node.getThreadLimit() != clusterMaxThreads)
            {
                FijiArchipelago.log("Waiting to close " + node);
                while (node.numRunningThreads() == 0)
                {
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException ie)
                    {
                        //
                    }
                }
                FijiArchipelago.log("Closing " + node);
                node.close();
                ok = false;
            }
        }

        logResults(futures);
        FijiArchipelago.log("Mid Cancel Test Complete");
    }
    
    private void shutdownTest()
    {
        FijiArchipelago.log("Shutdown Test");
        final List<Future<NullCall>> finalFutures =
                submitJobs(executors, coreCount, numCalls);

        Thread t = new Thread()
        {
            public void run()
            {
                logResults(finalFutures);
            }
        };

        t.start();

        FijiArchipelago.log("Shutting down cluster");
        executors[0].shutdown();

        try
        {
            t.join();
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.log("Test: Interrupted!");
        }
        FijiArchipelago.log("Shutdown Test Complete");
    }
    

    private void invokeAllTest()
    {
        FijiArchipelago.log("Invoke All Test");
        try
        {
            logResults(executors[0].invokeAll(createCallables(coreCount[0], numCalls)));
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.err(ie.toString());
        }
        FijiArchipelago.log("Invoke All Test Complete");
    }

    private void invokeAllTimeoutTest()
    {
        FijiArchipelago.log("Invoke All Timeout Test");
        try
        {
            List<Future<NullCall>> results =
                    executors[0].invokeAll(createCallables(coreCount[0], numCalls * 2), 7500, TimeUnit.MILLISECONDS);
            logResults(results);
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.err(ie.toString());
        }
        FijiArchipelago.log("Invoke All Timeout Test Complete");
    }

    private void invokeAnyTest()
    {

    }

    private void invokeAnyTimeoutTest()
    {

    }



    public synchronized void run(String arg)
    {
        if (Cluster.activeCluster() || FijiArchipelago.runClusterGUI())
        {            
            cluster = Cluster.getCluster();
            
            
            executors = new ExecutorService[coreCount.length];                        
            
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

            for (int i = 0; i < coreCount.length; ++i)
            {
                executors[i] = cluster.getService(coreCount[i]);
            }

            numCalls = cluster.getNodes().size() * 4;

            basicTest();

            invokeAllTest();
            invokeAllTimeoutTest();
            invokeAnyTimeoutTest();
            
            midCancelTest();
            
            shutdownTest();
        }
        
        
    }
}
