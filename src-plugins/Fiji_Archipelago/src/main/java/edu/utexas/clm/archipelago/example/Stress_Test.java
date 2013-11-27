package edu.utexas.clm.archipelago.example;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.compute.ArchipelagoFuture;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 *
 */
public class Stress_Test implements PlugIn
{
    public static class Stressor implements Callable<Double>, Serializable
    {
        private final int stresses;

        public Stressor()
        {
            this.stresses = 1024 * 1024;
        }

        public Double call() throws Exception
        {
            Random r = new Random(System.currentTimeMillis());
            double rval = r.nextDouble();
            for (int i = 0; i < stresses; ++i)
            {
                double rval2 = r.nextDouble();
                rval2 = rval2 * rval2;
                rval2 = Math.pow(rval2, rval);
                rval2 = rval2 / i;
                rval = rval2;
            }

            return rval;
        }
    }


    public void run(String arg)
    {
        Cluster cluster = Cluster.getCluster();
        int startNum = 1024;

        if (!cluster.isReady())
        {
            IJ.error("Cluster is not ready");
        }
        else
        {
            ExecutorService service = cluster.getService(1);
            ArrayList<Future<Double>> futures = new ArrayList<Future<Double>>(startNum);

            for (int i = 0; i < startNum; ++i)
            {
                futures.add(service.submit(new Stressor()));
            }

            while (!futures.isEmpty())
            {
                try
                {
                    Future<Double> future = futures.get(0);
                    if (future instanceof ArchipelagoFuture)
                    {
                        ArchipelagoFuture archf = (ArchipelagoFuture)future;
                        IJ.log("Waiting for future " + archf.getID() + "...");
                    }
                    else
                    {
                        IJ.log("Waiting for the future...");
                    }
                    Double d = future.get();
                    IJ.log("The future came!");
                    futures.remove(0);
                    futures.add(service.submit(new Stressor()));
                }
                catch (ExecutionException ee)
                {
                    IJ.log("The future had a problem.");
                    ee.printStackTrace();
                    IJ.log("Caught Exception: " + ee);
                }
                catch (InterruptedException ie)
                {
                    IJ.log("Interrupted while running.");
                    return;
                }
            }

            IJ.log("Done!");
        }
    }
}
