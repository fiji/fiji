package edu.utexas.archipelago.plugin;

import edu.utexas.archipelago.ArchipelagoUtils;
import edu.utexas.archipelago.segmentation.BatchWekaSegmentation;
import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.ui.ClusterXML;
import edu.utexas.clm.archipelago.util.PrintStreamLogger;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.VirtualStack;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Batch_Weka_Segmentation implements PlugIn
{
    private static final int DEFAULT_BLOCK_SIZE = 512;
    private static final int DEFAULT_OVLP = 32;

    private File getBWSParams(final String arg, final int[] blockSize, final int[] overlap)
    {
        final GenericDialog gd = new GenericDialog("Block Segmentation Parameters");
        final Panel buttonPanel = new Panel();
        final Button selectFileButton = new Button("Select File...");                
        
        gd.addStringField("Classifier File", !arg.isEmpty() && new File(arg).exists() ? arg : "",
                64);
        buttonPanel.add(selectFileButton);
        gd.addPanel(buttonPanel);
        gd.addNumericField("Block Size (px)", DEFAULT_BLOCK_SIZE, 0);
        gd.addNumericField("Overlap (px)", DEFAULT_OVLP, 0);

        selectFileButton.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                final OpenDialog od = new OpenDialog("Select a classifier file", null);
                final String dirName = od.getDirectory();
                final String fileName = od.getFileName();
                if (fileName != null)
                {
                    ((TextField)gd.getStringFields().get(0)).setText(dirName + fileName);
                    gd.validate();
                }
            }
        });
        
        gd.showDialog();
        
        if(gd.wasOKed())            
        {
            String file = gd.getNextString();
            int bs = (int)gd.getNextNumber();
            int ovlp = (int)gd.getNextNumber();

            blockSize[0] = bs;
            blockSize[1] = bs;
            overlap[0] = ovlp;
            overlap[1] = ovlp;

            return new File(file).getAbsoluteFile();
        }
        else
        {
            return null;
        }        
    }

    public void runWithUI(final String arg)
    {
        final ImagePlus imp = IJ.getImage();
        final ArrayList<File> fileList = new ArrayList<File>();


        try
        {

            if (imp != null && ArchipelagoUtils.getFileList(fileList, imp))
            {
                final Cluster cluster = Cluster.getClusterWithUI();
                final int[] blockSize = new int[2];
                final int[] overlap = new int[2];
                final File classifierFile = getBWSParams(arg, blockSize, overlap);

                cluster.waitUntilReady();

                if (classifierFile != null)
                {
                    final BatchWekaSegmentation bws = new BatchWekaSegmentation(
                            classifierFile, blockSize, overlap, cluster);
                    int i = 0;

                    for (VirtualStack vs : bws.segmentImages(fileList))
                    {
                        new ImagePlus(imp.getTitle() + " probability map " + ++i, vs).show();
                    }
                }
            }
        }
        catch (InvalidAlgorithmParameterException iape)
        {
            FijiArchipelago.err("" + iape);
        }
        catch (IOException ioe)
        {
            FijiArchipelago.err("" + ioe);
        }
    }

    public void runHeadless(final String options)
    {
        final ArrayList<String> args = new ArrayList<String>();
        final ArrayList<File> fileList = new ArrayList<File>();
        final StringTokenizer tokenizer = new StringTokenizer(options);
        final String imageListPath, classifierPath, clusterPath;
        final int blockSize, ovlp;
        final Cluster cluster;
        PrintStream debugLogStream;
        final BatchWekaSegmentation bws;


        final BufferedReader br;
        String line;

        while (tokenizer.hasMoreTokens())
        {
            args.add(tokenizer.nextToken(" "));
        }

        if (args.size() < 3)
        {
            System.err.println("Batch Weka Segmentation needs at least three arguments:\n" +
                               "\tfiji --run \"Weka Segmentation 2D\" \"<image_list> " +
                    "<classifier_file> <cluster_file> [<block_size> [<overlap_size>]]\n" +
                               "\timage list - a text file with one image path per line\n" +
                               "\tclassifier file - a weka .model file\n" +
                               "\tcluster file - a cluster .arc file\n" +
                               "\tblock size - image block size, defaults to " +
                    DEFAULT_BLOCK_SIZE +"\n" +
                               "\toverlap size - image block overlap, defaults to " + DEFAULT_OVLP);
            return;
        }

        imageListPath = args.get(0);
        classifierPath = args.get(1);
        clusterPath = args.get(2);
        blockSize = args.size() >= 4 ? Integer.parseInt(args.get(3)) : DEFAULT_BLOCK_SIZE;
        ovlp = args.size() >= 5 ? Integer.parseInt(args.get(4)) : DEFAULT_OVLP;



        FijiArchipelago.setDebugLogger(new PrintStreamLogger());

        try
        {
            br = new BufferedReader(new FileReader(imageListPath));
            while ((line = br.readLine()) != null)
            {
                fileList.add(new File(line));
            }
        }
        catch (IOException ioe)
        {
            System.out.println("Error while reading " + imageListPath + ": " + ioe);
            return;
        }

        cluster = Cluster.getCluster();
        try
        {
            ClusterXML.loadClusterFile(new File(clusterPath), cluster, null);
        }
        catch (Exception e)
        {
            System.err.println("Could not load cluster file " + clusterPath + ": " + e);
            return;
        }

        try
        {
            debugLogStream = new PrintStream(
                    new FileOutputStream(
                            "archipelago debug " + System.currentTimeMillis() + ".log"));
        }
        catch (IOException ioe)
        {
            System.err.println("Could not open log file for writing, debug stream to std out");
            debugLogStream = System.out;
        }

        FijiArchipelago.setDebugLogger(new PrintStreamLogger(debugLogStream));
        FijiArchipelago.setErrorLogger(new PrintStreamLogger(System.err));
        FijiArchipelago.setInfoLogger(new PrintStreamLogger(System.out));

        cluster.start();
        cluster.waitUntilReady();

        try
        {
            bws = new BatchWekaSegmentation(
                    new File(classifierPath),
                    new int[]{blockSize, blockSize},
                    new int[]{ovlp, ovlp},
                    cluster);
            bws.segmentImages(fileList);
        }
        catch (InvalidAlgorithmParameterException iape)
        {
            System.err.println("" + iape);
        }
    }

    public void run(final String arg)
    {
        final String options = Macro.getOptions();

        System.out.println("Got options " + options);

        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance())
        {
            runHeadless(options);
        }
        else
        {
            runWithUI(arg);
        }
    }
}

