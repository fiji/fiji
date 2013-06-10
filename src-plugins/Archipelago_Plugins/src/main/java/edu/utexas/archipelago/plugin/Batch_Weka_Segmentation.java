package edu.utexas.archipelago.plugin;

import edu.utexas.archipelago.segmentation.BatchWekaSegmentation;
import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import ij.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Collection;

public class Batch_Weka_Segmentation implements PlugIn
{

    private String getCacheDirectory()
    {
        return IJ.getDirectory("Select a Shared Directory for the File Cache");
    }

    
    private boolean getFileList(final Collection<File> files, final ImagePlus imp)
    {
        if (imp.getImageStackSize() > 1)
        {
            ImageStack is = imp.getImageStack();
            if (is.isVirtual())
            {
                VirtualStack vs = (VirtualStack)is;
                for (int i = 1; i <= vs.getSize(); ++i)
                {
                    files.add(new File(vs.getDirectory() + vs.getFileName(i)).getAbsoluteFile());
                }
            }
            else
            {
                String dir = getCacheDirectory();

                if (dir == null)
                {
                    return false;
                }
                
                for (int i = 1; i <= is.getSize(); ++i)
                {
                    final String nid = String.format("%05d", i);
                    final ImagePlus impSlice = new ImagePlus(imp.getTitle() + " " + nid,
                            is.getProcessor(i));
                    final File f = new File(dir + "image" + nid + ".tif").getAbsoluteFile();
                    
                    IJ.save(impSlice, f.getAbsolutePath());
                    files.add(f);
                }
            }
        }
        else
        {
            File f;
            
            try
            {
                f = new File(imp.getOriginalFileInfo().directory +
                        imp.getOriginalFileInfo().fileName).getAbsoluteFile();
            }
            catch (NullPointerException npe)
            {
                f = null;
            }
            
            if (f == null || !f.exists())
            {
                final String dir = getCacheDirectory();
                
                if (dir == null)
                {
                    return false;
                }
                
                f = new File(dir + "image.tif").getAbsoluteFile();
                
                IJ.save(imp, f.getAbsolutePath());    
            }
            
            files.add(f);            
        }
        
        return true;
    }
    
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

            if (imp != null && getFileList(fileList, imp))
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

    }
}

