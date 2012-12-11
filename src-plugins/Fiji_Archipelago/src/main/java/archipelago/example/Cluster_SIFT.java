package archipelago.example;

import archipelago.Cluster;
import archipelago.FijiArchipelago;
import archipelago.compute.SerializableCallable;
import archipelago.data.FileChunk;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import mpicbg.ij.SIFT;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Larry Lindsey
 */

public class Cluster_SIFT implements PlugIn
{


    
    public static class SIFTCall implements SerializableCallable<ArrayList<Feature>>
    {
        private final FloatArray2DSIFT.Param param;
        private final FileChunk fileChunk;
        
        public SIFTCall(FloatArray2DSIFT.Param p, String filename)
        {
            param = p;
            fileChunk = new FileChunk(filename);
        }
        
        public ArrayList<Feature> call() throws Exception {
            ImagePlus im = IJ.openImage(fileChunk.getData());
            System.out.println("attempting to open file " + fileChunk.getData());
            ImageProcessor ip = im.getProcessor();
            ArrayList<Feature> feat = new ArrayList<Feature>();
            SIFT sift = new SIFT(new FloatArray2DSIFT(param));
            sift.extractFeatures(ip, feat);
            return feat;
        }
    }


    /**
     * Extracts SIFT features from images using a FijiArchipelago Cluster
     * @param fileNames a Collection of file names
     *                  This method makes no check of the validity of the filenames. For this
     *                  to work properly, they must exist within the Cluster file root, which
     *                  is indicated by FijiArchipelago.getFileRoot().
     * @param param The SIFT parameters
     * @return An ArrayList of ArrayLists of Features, such that the ArrayList<Feature>
     *     corresponding to the ith file in fileNames is returned in the ith position here.
     */
    public static ArrayList<ArrayList<Feature>> clusterSIFTExtraction(Collection<String> fileNames,
                                                                      FloatArray2DSIFT.Param param)
    {
        if (Cluster.activeCluster())
        {
            boolean executionErrorOccurred = false;

            // The return list
            ArrayList<ArrayList<Feature>> featuresList = new ArrayList<ArrayList<Feature>>();
            // A List of Futures, used a little later.
            ArrayList<Future<ArrayList<Feature>>> futures =
                    new ArrayList<Future<ArrayList<Feature>>>();
            
            FijiArchipelago.debug("Submitting futures");
            
            // For each file name, create a SIFTCall, submit it, and collect the Future returned by
            // the Cluster
            for (String fileName : fileNames)
            {                
                futures.add(Cluster.getCluster().submit(new SIFTCall(param.clone(), fileName)));
            }


            FijiArchipelago.debug("Waiting on futures");
            // Get the result from each Future. this will block until the results return from the
            // ClusterNode that the computation is actually running on.
            for (Future<ArrayList<Feature>> future: futures)
            {
                try
                {
                    featuresList.add(future.get());
                }
                catch (InterruptedException ie)
                {
                    // This happens if we're interrupted while blocking in Future.get
                    FijiArchipelago.err(
                            "Cluster SIFT Extraction: Interrupted while waiting for results.");
                    return featuresList;
                }
                catch (ExecutionException ee)
                {
                    // If the Callable throws an error on the remote node, it will propagate back
                    // over the network and end up here.
                    // This would be called as a .err, but it when it rains, it pours.
                    FijiArchipelago.log("Remote exception: " + ee);
                    executionErrorOccurred = true;
                }
            }
            
            if (executionErrorOccurred)
            {
                FijiArchipelago.err("Caught at least one ExecutionError. See the log");
            }
            
            return featuresList;
        }
        else
        {
            FijiArchipelago.err("Cluster SIFT Extraction: Cluster is not active.");
            return new ArrayList<ArrayList<Feature>>();
        }
    }
    
    
    public float runOnCluster()
    {
        ImagePlus ip = IJ.getImage();

        if (ip == null)
        {
            IJ.showMessage("First, open an image");
            return 0;
        }
        else if(!(ip.getStack().isVirtual() 
                && FijiArchipelago.fileIsInRoot(((VirtualStack)ip.getStack()).getFileName(1))))
        {
            IJ.showMessage("Stack must be virtual and within the cluster file root "
                    + FijiArchipelago.getFileRoot());
            return 0;
        }

        if (!Cluster.activeCluster())
        {            
            if (FijiArchipelago.runClusterGUI())
            {
                Cluster.getCluster().waitUntilReady();
            }
        }

        if (Cluster.activeCluster())
        {
            long sTime = System.currentTimeMillis();
            ImageStack stack = ip.getStack();

            FijiArchipelago.log("Cluster is active.");
            
            if (stack.isVirtual() 
                    && FijiArchipelago.fileIsInRoot(((VirtualStack) stack).getFileName(1)))
            {
                // In theory, this check shouldn't be necessary, but I included it anyway
                ArrayList<String> fileNames = new ArrayList<String>();
                VirtualStack vstack = (VirtualStack)stack;

                for (int i = 1; i <= vstack.getSize(); ++i)
                {                    
                    fileNames.add(vstack.getDirectory() + vstack.getFileName(i));                    
                }
                
                FijiArchipelago.debug("Running clusterSIFTExtraction on " + fileNames.size() + " files");
                
                clusterSIFTExtraction(fileNames, new FloatArray2DSIFT.Param());

                return System.currentTimeMillis() - sTime;
            }
            else
            {
                IJ.showMessage("Stack wasn't virtual and in file root");
                return 0;
            }
        }
        else
        {
            FijiArchipelago.debug("Cluster was not ready");
        }
        return 0;
    }
    
    public float runLocally()
    {
        ImagePlus ip = IJ.getImage();

        if (ip == null)
        {
            IJ.showMessage("First, open a virtual stack");
            return 0;
        }
        else if(!(ip.getStack().isVirtual()))
        {
            IJ.showMessage("Stack must be virtual");
            return 0;
        }

        long sTime = System.currentTimeMillis();
        ImageStack stack = ip.getStack();
        ArrayList<Thread> threads = new ArrayList<Thread>();
        final VirtualStack vstack = (VirtualStack)stack;
        final int numCore = Runtime.getRuntime().availableProcessors();

        for (int p = 0; p < numCore; ++p)
        {
            final int pnum = p;
            Thread t = new Thread(){
                public void run()
                {
                    for (int i = pnum + 1; i <= vstack.getSize(); i+=numCore)
                    {
                        ImagePlus im = IJ.openImage(vstack.getDirectory() + vstack.getFileName(i));
                        ImageProcessor ip1 = im.getProcessor();
                        ArrayList<Feature> feat = new ArrayList<Feature>();
                        SIFT sift = new SIFT(new FloatArray2DSIFT(new FloatArray2DSIFT.Param()));
                        sift.extractFeatures(ip1, feat);
                    }
                }
            };
            threads.add(t);
            t.start();
        }
        
        try
        {
            for (Thread t : threads)
            {
                t.join();
            }
        }
        catch (InterruptedException ie)
        {
            IJ.showMessage("Interrupted while join()ing");
            return 0;
        }

        return System.currentTimeMillis() - sTime;
    }
    
    public void run(String arg)
    {
        if (arg.equals("standalone"))
        {
            IJ.showMessage("Finished after " + (runLocally() / 1000) + " seconds");
            
        }
        else if (arg.equals("cluster"))
        {
            IJ.showMessage("Finished after " + (runOnCluster() / 1000) + " seconds");
        }
        else if (arg.equals("test"))
        {
            float clusterTime = runOnCluster();
            FijiArchipelago.debug("Cluster run finished");
            float aloneTime = runLocally();
            IJ.showMessage("Cluster: " + (clusterTime / 1000f )+ "s\nStand Alone: "
                    + (aloneTime / 1000f)+ "s\nSpeedup: " + (aloneTime / clusterTime));
        }            
        else
        {
            IJ.showMessage("Must choose to run either standalone or on cluster");
        }
    }
}
