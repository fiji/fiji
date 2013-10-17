package edu.utexas.archipelago.segmentation;

import edu.utexas.archipelago.image.ImageBlockDeblock;
import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.archipelago.data.FileChunk;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.io.FileSaver;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

import java.awt.image.ColorModel;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

public class BatchWekaSegmentation
{
    public static class WekaSegmentationCallable
            implements Callable<Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>>>, Serializable
    {

        private final FileChunk file;
        private final FileChunk classifier;
        private final int index;

        public WekaSegmentationCallable(FileChunk inFile, FileChunk inClassifier, int index)
        {
            this.index = index;

            file = inFile;
            classifier = inClassifier;
        }

        public Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>> call() throws Exception
        {
            try
            {
                final String inPath = file.getData();
                final int idot = inPath.lastIndexOf('.');
                final String formatString = inPath.substring(0, idot) + "_seg_%02d.png";
                
                final ArrayList<FileChunk> outputFiles =
                        segment(file.getData(), classifier.getData(), formatString);
                return new Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>>(index,
                        new Duplex<FileChunk, ArrayList<FileChunk>>(file, outputFiles));
            }
            catch (Exception e)
            {
                FijiArchipelago.debug("Caught an exception:", e);
                throw e;
            }
        }

        public ArrayList<FileChunk> segment(final String file,
                                            final String classifier,
                                            final String outputFormat) throws Exception
        {
            ImagePlus impSeg, impSave;

            FijiArchipelago.log("Loading image from " + file);

            final ImagePlus imp = IJ.openImage(file);

            if (imp == null)
            {
                throw new IOException("Could not read file: " + file);
            }

            FijiArchipelago.log("Creating weka segmentation object");

            final WekaSegmentation seg = new WekaSegmentation(imp);
            final File f = new File(classifier);
            final InputStream is = new FileInputStream( f );
            final ObjectInputStream objectInputStream = new ObjectInputStream(is);
            final ArrayList<FileChunk> outputFiles = new ArrayList<FileChunk>();

            AbstractClassifier abstractClassifier;
            Instances header;

            FijiArchipelago.log("Loading classifier from " + classifier);

            abstractClassifier = (AbstractClassifier) objectInputStream.readObject();
            header = (Instances) objectInputStream.readObject();

            objectInputStream.close();

            FijiArchipelago.log("Calling seg.setClassifier");

            seg.setClassifier(abstractClassifier);
            seg.setTrainHeader(header);

            FijiArchipelago.log("Applying classifier to image");

            impSeg = seg.applyClassifier(imp, 1, true);
            
            
            for (int i = 1; i <= impSeg.getImageStackSize(); ++i)
            {
                FileChunk outChunk = new FileChunk(String.format(outputFormat, i));
                impSave = new ImagePlus(impSeg.getTitle(), impSeg.getImageStack().getProcessor(i));
                
                
                FijiArchipelago.log("Saving classified image to " + outChunk.getData());

                new FileSaver(impSave).saveAsTiff(outChunk.getData());
                
                outputFiles.add(outChunk);
            }

            FijiArchipelago.log("Done.");
            
            return outputFiles;
        }
    }


    private final FileChunk classifier;
    private final int[] blockSize, ovlpPx;
    private final int nClasses;
    private final ExecutorService ibdService;
    private final ExecutorService segService;

    public BatchWekaSegmentation(final File classifier, final int[] blockSize, final int[] ovlpPx)
            throws InvalidAlgorithmParameterException
    {
        this(classifier, blockSize, ovlpPx, Executors.newFixedThreadPool(1),
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }
    
    public BatchWekaSegmentation(final File classifier, final int[] blockSize, final int[] ovlpPx,
                                 final Cluster cluster)
            throws InvalidAlgorithmParameterException
    {
        this(classifier, blockSize, ovlpPx, cluster.getService(1.0f), cluster.getService(1));
    }


    private BatchWekaSegmentation(final File classifier, final int[] blockSize, final int[] ovlpPx,
                                 final ExecutorService ibdService, final ExecutorService segService)
            throws InvalidAlgorithmParameterException
    {
        this.classifier = new FileChunk(classifier.getAbsolutePath());
        this.blockSize = blockSize;
        this.ovlpPx = ovlpPx;
        this.ibdService = ibdService;
        this.segService = segService; 

        try
        {
            this.nClasses = countClasses(classifier);
        }
        catch (Exception e)
        {
            throw new InvalidAlgorithmParameterException(
                    "Could not count classes in the classifier model file " +
                            classifier.getPath() +".", e);
        }
        
        if (blockSize.length != 2)
        {
            throw new InvalidAlgorithmParameterException("blockSize must have exactly 2" +
                    " elements, had " + blockSize.length);
        }
        else if (ovlpPx.length != 2)
        {
            throw new InvalidAlgorithmParameterException("ovlpPx must have exactly 2" +
                    " elements, had " + ovlpPx.length);
        }
    }

    public ArrayList<VirtualStack> segmentImages(final Collection<File> imageFiles)
    {
        final ArrayList<Future<ImageBlockDeblock>> ibdFutures =
                new ArrayList<Future<ImageBlockDeblock>>(imageFiles.size());
        /*
         A little hard to read. Sigh. Five levels of generics...
         Future <--- Duplex <-- Integer - the index of the corresponding ImageBlockDeblock in
                                          ibdList
                          ^---  Duplex <-- FileChunk - represents a block out of the original image
                                     ^---  ArrayList <-- FileChunk - represents an image of the
                                                                     probability map corresponding
                                                                     to one of the classes in the
                                                                     classifier model.
        */
        final ArrayList<Future<Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>>>>
                segFutures =
                new ArrayList<Future<Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>>>>();
        final ArrayList<Future<ImageBlockDeblock>> outputFutures =
                new ArrayList<Future<ImageBlockDeblock>>();
        final ArrayList<ImageBlockDeblock> ibdList = new ArrayList<ImageBlockDeblock>();
        final ArrayList<VirtualStack> stacks = new ArrayList<VirtualStack>();
        int j = 0;


        // Split the images into blocks
        FijiArchipelago.log("Submitting images for splitting into blocks");

        for (final File f : imageFiles)
        {
            final String path = f.getAbsolutePath();
            final int lastDot = path.lastIndexOf('.');
            final String outPath = path.substring(0, lastDot) + "_seg_%d" + path.substring(lastDot);
            final ImageBlockDeblock ibd =
                    new ImageBlockDeblock(f, new File(outPath), blockSize, ovlpPx, nClasses);
            ibd.setSubfolder("blocks/" + j + "/");
            ibdFutures.add(ibdService.submit(ibd.imageBlockCallable()));
            ++j;
        }

        FijiArchipelago.log("Submitting image blocks for segmentation");
        // Run the blocks through the segmenter.
        try
        {
            for (int i = 0; i < ibdFutures.size(); ++i)
            {
                final Future<ImageBlockDeblock> future = ibdFutures.get(i);
                final ImageBlockDeblock ibd = future.get();

                ibdList.add(ibd);

                for (FileChunk fc : ibd.imageBlockFiles())
                {
                    final WekaSegmentationCallable callable =
                            new WekaSegmentationCallable(fc, classifier, i);
                    segFutures.add(segService.submit(callable));
                }
            }
        }
        catch (ExecutionException ee)
        {
            FijiArchipelago.err("A problem occured while splitting images: " + ee);
            FijiArchipelago.debug("A problem occured while splitting images: ", ee);
            return stacks;
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.err("Interrupted while splitting images: " + ie);
            FijiArchipelago.debug("Interrupted while splitting images: ", ie);
            return stacks;
        }

        FijiArchipelago.log("Submitting segmented blocks for recombination");
        // Recombine the blocks
        try
        {
            for (Future<Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>>> future : segFutures)
            {
                final Duplex<Integer, Duplex<FileChunk, ArrayList<FileChunk>>> segDup = future.get();
                final int index = segDup.a;
                //final int n = segDup.b.b.size();

                final ImageBlockDeblock ibd = ibdList.get(index);

                for (int i = 0; i < nClasses; ++i)
                {
                    ibd.mapChunks(segDup.b.a, segDup.b.b.get(i), i);
                }

                if (ibd.numUnmappedBlocks(nClasses-1) <= 0)
                {
                    outputFutures.add(ibdService.submit(ibd.imageDeblockCallable()));
                }
            }
        }
        catch (ExecutionException ee)
        {
            FijiArchipelago.err("A problem occured while segmenting images: " + ee);
            FijiArchipelago.debug("A problem occured while segmenting images: ", ee);
            return stacks;
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.err("Interrupted while segmenting images: " + ie);
            FijiArchipelago.debug("Interrupted while segmenting images: ", ie);
            return stacks;
        }

        FijiArchipelago.log("Creating virtual stack");
        // Create a virtual stack
        try
        {
            
            for (Future<ImageBlockDeblock> future : outputFutures)
            {
                ImageBlockDeblock ibd = future.get();
                
                if (stacks.isEmpty())
                {
                    for (int i = 0; i < nClasses; i++)
                    {
                        stacks.add(new VirtualStack(ibd.getWidth(), ibd.getHeight(),
                                ColorModel.getRGBdefault(), ""));
                    }
                }
                
                for (int i = 0; i < nClasses; ++i)
                {
                    File segFile = new File(ibd.getOutputFile(i).getData());

                    FijiArchipelago.log("Adding file " + segFile.getName() + " to VS");
                    stacks.get(i).addSlice(segFile.getAbsolutePath());
                }
            }
            FijiArchipelago.log("Done Creating VirtualStack");
        }
        catch (ExecutionException ee)
        {
            FijiArchipelago.err("A problem occured while recombining segmented image blocks: " +
                    ee);
            FijiArchipelago.debug("A problem occured while recombining segmented image blocks: ",
                    ee);
            return stacks;
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.err("Interrupted while recombining segmented image blocks: " + ie);
            FijiArchipelago.debug("Interrupted while recombining segmented image blocks: ", ie);
            return stacks;
        }

        return stacks;
    }
    
    private static int countClasses(final File classifier) throws IOException,
            ClassNotFoundException
    {
        final InputStream is = new FileInputStream( classifier );
        final ObjectInputStream objectInputStream = new ObjectInputStream(is);
        final AbstractClassifier abstractClassifier =
                (AbstractClassifier) objectInputStream.readObject();
        final Instances header = (Instances) objectInputStream.readObject();
        
        objectInputStream.close();
        
        return header.numClasses();
    }
    
}
