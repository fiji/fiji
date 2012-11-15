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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class Cluster_SIFT implements PlugIn
{

    /*public class ClusterSIFTProcessListener implements ProcessListener
    {
        final Vector<Long> ids;
        final HashMap<Integer, ArrayList<Feature>> map;
        Thread waitThread = null;
        
        public ClusterSIFTProcessListener()
        {
            ids = new Vector<Long>();
            map = new HashMap<Integer, ArrayList<Feature>>();
        }

        public void addProcessManagers(final List<ProcessManager> pms)
        {
            for (ProcessManager pm : pms)
            {
                ids.add(pm.getID());
            }
        }

        public boolean processFinished(final ProcessManager<?, ?> process) {
            try
            {
                int index = ids.indexOf(process.getID());                
                ArrayList<Feature> result = (ArrayList<Feature>)process.getOutput().getData();
                ids.remove(process.getID());
                map.put(index, result);

                if (ids.isEmpty() && waitThread != null)
                {
                    waitThread.interrupt();
                }
                return true;
            }
            catch (ClassCastException cce)
            {
                FijiArchipelago.log("Got ClassCastException " + cce);
                return false;
            }
        }
        
        public ArrayList<ArrayList<Feature>> getFeatureListList()
        {
            ArrayList<ArrayList<Feature>> featureListList = new ArrayList<ArrayList<Feature>>();
            for (int i = 0; i < map.size(); ++i)
            {
                featureListList.add(map.get(i));
            }
            return featureListList;
        }
        
        public synchronized void waitUntilDone()
        {
            waitThread = Thread.currentThread();
            if (!ids.isEmpty())
            {
                try
                {
                    Thread.sleep(Long.MAX_VALUE);
                }
                catch (InterruptedException ie)
                {
                    //nothing to do
                }
            }
        }
        
    }

    public static class SIFTProcessor implements ChunkProcessor<ArrayList<Feature>, String>
    {

        final FloatArray2DSIFT.Param param;
        
        public SIFTProcessor(final FloatArray2DSIFT.Param p)
        {
            param = p;
        }
        
        public DataChunk<ArrayList<Feature>> process(DataChunk<String> dataChunk)
        {
            System.out.println("Woo");
            File file = new File(dataChunk.getData());
            System.out.println("Extracting Sift points from " + file.getAbsolutePath());
            ImagePlus im = IJ.openImage(file.getAbsolutePath());
//            ImageProcessor ip = im.getProcessor();
            ArrayList<Feature> feat = new ArrayList<Feature>();
//            SIFT sift = new SIFT(new FloatArray2DSIFT(param));
//
//            sift.extractFeatures(ip, feat);
//
//            System.out.println("done");
            
            return new SimpleChunk<ArrayList<Feature>>(feat, dataChunk);
        }

        
    }*/
    
    public static class SIFTProcessor implements ChunkProcessor<String, String>
    {
        public DataChunk<String> process(DataChunk<String> chunk)
        {
            System.out.println("Got chunk " + chunk.toString());
        
            try
            {
                System.out.println("Opening image");
                ImagePlus im = IJ.openImage(chunk.getData());
                System.out.println("Getting IP");
                ImageProcessor ip = im.getProcessor();
                System.out.println("Making features");
                ArrayList<Feature> feat = new ArrayList<Feature>();
                System.out.println("Making SIFT");
                SIFT sift = new SIFT(new FloatArray2DSIFT(new FloatArray2DSIFT.Param()));
                System.out.println("Doing SIFT");
                sift.extractFeatures(ip, feat);
                System.out.println("Done SIFT");
                return new SimpleChunk<String>("Successfully read " + chunk.toString() + " and got " + feat.size() + " features");
            }
            catch (Exception e)
            {
                System.out.println("Caught exception: " + e);
                return new SimpleChunk<String>("Had problems reading " + chunk.toString() + ": " + e.toString());
            }
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

        if (!Cluster.activeCluster())
        {
            FijiArchipelago.runClusterGUI();
        }

        if (Cluster.activeCluster())
        {
            long sTime = System.currentTimeMillis();
            Cluster cluster = Cluster.getCluster();
            ImageStack stack = ip.getStack();
            cluster.waitUntilReady();

            if (stack.isVirtual() 
                    && FijiArchipelago.fileIsInRoot(((VirtualStack) stack).getFileName(1)))
            {
//                System.out.println("Starting cluster job");
//                
//                FloatArray2DSIFT.Param param = new FloatArray2DSIFT.Param();
//                VirtualStack vstack = (VirtualStack)stack;
//                ArrayList<ProcessManager> pms
//                        = new ArrayList<ProcessManager>();
//                SIFTProcessor siftProcessor;
//                ClusterSIFTProcessListener processListener = new ClusterSIFTProcessListener();
//
//                param.maxOctaveSize = 2048;
//
//                siftProcessor = new SIFTProcessor(param);
//                
//                for (int i = 1; i <= vstack.getSize(); ++i)
//                {
//                    FileChunk imFileChunk = new FileChunk(vstack.getFileName(i));
//                    ProcessManager<ArrayList<Feature>, String> pm =
//                            new ProcessManager<ArrayList<Feature>, String>(
//                                    imFileChunk, siftProcessor, processListener);
//                    pms.add(pm);
//                }
//
//                processListener.addProcessManagers(pms);
//                
//                cluster.queueProcesses(pms);
//                
//                processListener.waitUntilDone();

                VirtualStack vstack = (VirtualStack)stack;
                for (int i = 1; i <= vstack.getSize(); ++i)
                {
                    FileChunk imFileChunk = new FileChunk(vstack.getDirectory() + vstack.getFileName(i));
                    FijiArchipelago.log("Got file " + imFileChunk);
                    ProcessListener listener = new ProcessListener() {
                        public boolean processFinished(ProcessManager<?, ?> process) {
                            FijiArchipelago.log("Got job back: " + process.getOutput());
                            return true;
                        }
                    };
                    ProcessManager<String, String> pm =
                            new ProcessManager<String, String>(imFileChunk, new SIFTProcessor(), listener);
//                    pm.run();
                    cluster.queueProcess(pm);
//                    pms.add(pm);
                }
                
                
                return System.currentTimeMillis() - sTime;
            }
            else
            {
                IJ.showMessage("Front image must be a Virtual Stack within the file root "
                        + FijiArchipelago.getFileRoot());
                return 0;
            }
        }
        return 0;
    }
    
    public long runLocally()
    {
        return 0;
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
