package edu.utexas.archipelago.plugin;

import edu.utexas.archipelago.ArchipelagoUtils;
import edu.utexas.archipelago.segmentation.BatchWekaSegmentation;
import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;

public class Batch_Weka_Segmentation implements PlugIn
{

    private File getBWSParams(final String arg, final int[] blockSize, final int[] overlap)
    {
        final GenericDialog gd = new GenericDialog("Block Segmentation Parameters");
        final Panel buttonPanel = new Panel();
        final Button selectFileButton = new Button("Select File...");                
        
        gd.addStringField("Classifier File", !arg.isEmpty() && new File(arg).exists() ? arg : "",
                64);
        buttonPanel.add(selectFileButton);
        gd.addPanel(buttonPanel);
        gd.addNumericField("Block Size (px)", 512, 0);
        gd.addNumericField("Overlap (px)", 32, 0);

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
    
    public void run(final String arg)
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
}

