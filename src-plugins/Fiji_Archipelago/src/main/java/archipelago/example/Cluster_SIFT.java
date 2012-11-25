package archipelago.example;

import archipelago.FijiArchipelago;
import archipelago.compute.ChunkProcessor;
import archipelago.compute.ProcessListener;
import archipelago.compute.ProcessManager;
import archipelago.data.DataChunk;
import archipelago.data.FileChunk;
import archipelago.data.SimpleChunk;
import archipelago.network.Cluster;
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

/**
 *
 * @author Larry Lindsey
 */

public class Cluster_SIFT implements PlugIn
{


    
    public static class SIFTProcessor implements ChunkProcessor<ArrayList<Feature>, String>
    {
        private final FloatArray2DSIFT.Param param;
        
        public SIFTProcessor(FloatArray2DSIFT.Param p)
        {
            param = p;
        }
        
        public DataChunk<ArrayList<Feature>> process(DataChunk<String> chunk)
        {
            try
            {
                ImagePlus im = IJ.openImage(chunk.getData());
                ImageProcessor ip = im.getProcessor();
                ArrayList<Feature> feat = new ArrayList<Feature>();
                SIFT sift = new SIFT(new FloatArray2DSIFT(param));
                sift.extractFeatures(ip, feat);
                return new SimpleChunk<ArrayList<Feature>>(feat);
            }
            catch (Exception e)
            {
                return new SimpleChunk<ArrayList<Feature>>(new ArrayList<Feature>());
            }
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
            int index = 0;
            // A map of index to ID. This will be used later to keep the order of features correct
            final long[] idMap = new long[fileNames.size()];
            // Used to map ProcessManager ID to features
            final Hashtable<Long, ArrayList<Feature>> idFeatureMap
                    = new Hashtable<Long, ArrayList<Feature>>();
            // Before queueing, this will be filled with the IDs of the PMs. The ProcessListener
            // will remove those IDs as the results come in. When it's empty, we know we're done.
            // Use a Vector because they're synchronized.
            final Vector<Long> remainingIDS = new Vector<Long>();
            // The current thread will be told to sleep forever (well, 2^63 -1 ms). When
            // remainingIDs is empty, we'll interrupt the current thread.
            final Thread currentThread = Thread.currentThread();
            // The return list
            ArrayList<ArrayList<Feature>> featuresList;
            // A list of PMs where we collect them before submitting to the cluster.
            ArrayList<ProcessManager> pms = new ArrayList<ProcessManager>();
            
            /*
            For each filename passed in, create the appropriate FileChunk, a ProcessListener,
            and SIFTProcessor.
            
            The FileChunk and SIFTProcessor get sent to the client over an ObjectStream through
            a Socket, so they must be Serializable. For internal classes, they must also be
            static.
             */
            for (String fileName : fileNames)
            {                
                FileChunk imFileChunk = new FileChunk(fileName);
                ProcessListener listener = new ProcessListener() {
                    public boolean processFinished(ProcessManager<?, ?> process)
                    {
                        // Try to cast. This should *always* work, but if it fails, its important
                        // to let someone know.
                        try
                        {
                            ArrayList<Feature> feat = 
                                    (ArrayList<Feature>)process.getOutput().getData();
                            idFeatureMap.put(process.getID(), feat);
                        }
                        catch (ClassCastException cce)
                        {
                            FijiArchipelago.log("Cluster SIFT: Couldn't cast features correctly");
                            idFeatureMap.put(process.getID(), new ArrayList<Feature>());
                        }
                        // Remove the id from remaining IDs after we populate the map.
                        // It's possible that multiple listeners are running concurrently, so
                        // even if we remove the last ID, the interrupt might be called from a
                        // different thread.
                        remainingIDS.remove(process.getID());
                        if (remainingIDS.isEmpty())
                        {
                            currentThread.interrupt();
                        }
                        
                        return true;
                    }
                };
                ProcessManager<ArrayList<Feature>, String> pm =
                        new ProcessManager<ArrayList<Feature>, String>(
                                imFileChunk, new SIFTProcessor(param.clone()), listener);

                idMap[index] = pm.getID();
                remainingIDS.add(pm.getID());
                pms.add(pm);
                
                
            }

            Cluster.getCluster().queueProcesses(pms);

            try
            {
                // Sleep until we're kissed by Prince(ss) Charming
                Thread.sleep(Long.MAX_VALUE);
            }
            catch (InterruptedException ie)
            {
                // We got a kiss!
            }
            
            featuresList = new ArrayList<ArrayList<Feature>>();
            for (long id : idMap)
            {
                featuresList.add(idFeatureMap.get(id));
            }
            return featuresList;
        }
        else
        {
            FijiArchipelago.err("Cluster SIFT Extraction: Cluster is not active.");
            return new ArrayList<ArrayList<Feature>>();
        }
    }
    
    
    public long runOnCluster()
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
            FijiArchipelago.runClusterGUI();
            if (Cluster.activeCluster())
            {
                Cluster.getCluster().waitUntilReady();
            }
        }

        if (Cluster.activeCluster())
        {
            long sTime = System.currentTimeMillis();
            ImageStack stack = ip.getStack();

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
                
                clusterSIFTExtraction(fileNames, new FloatArray2DSIFT.Param());

                return System.currentTimeMillis() - sTime;
            }
            else
            {
                IJ.showMessage("Stack wasn't virtual and in file root");
                return 0;
            }
        }
        return 0;
    }
    
    public long runLocally()
    {
        ImagePlus ip = IJ.getImage();

        if (ip == null)
        {
            IJ.showMessage("First, open an image");
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
            long clusterTime = runOnCluster();
            long aloneTime = runLocally();
            IJ.showMessage("Cluster: " + (clusterTime / 1000 )+ "s\nStand Alone: "
                    + (aloneTime / 1000)+ "s\nSpeedup: " + (aloneTime / clusterTime));
        }            
        else
        {
            IJ.showMessage("Must choose to run either standalone or on cluster");
        }
    }
}
