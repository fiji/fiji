package fsalign;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * @author Larry Lindsey
 */
public abstract class ImageListener implements FileListener, Runnable
{
    private static class FileModifiedComparator implements Comparator<File>
    {
        public int compare(File file1, File file2)
        {
            return (int)(file1.lastModified() - file2.lastModified());
        }
    }

    private static final int QUEUE_CAP = 256;
    private final ArrayBlockingQueue<File> imageQueue;
    private final Vector<File> holdList;
    private final FileModifiedComparator fmc;
    private final Hashtable<String, ImagePlus> fileTable;
    private final Thread thread;
    private boolean enabled;


    public static FolderWatcher imageFolderWatcher(final String folderName, final int interval, final ImageListener imageListener, final String regexp)
    {
        FolderWatcher fw = new FolderWatcher(folderName, interval, 
                new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().matches(regexp);
                    }

                    @Override
                    public String getDescription() {
                        return "File Filter, matching against regexp " + regexp;
                    }
                });
        fw.addListener(imageListener);        
        return fw;
    }
    
    public ImageListener()
    {
        imageQueue = new ArrayBlockingQueue<File>(QUEUE_CAP);
        holdList = new Vector<File>();
        fmc = new FileModifiedComparator();
        enabled = true;
        fileTable = new Hashtable<String, ImagePlus>();
        thread = new Thread(this);
        thread.start();
    }

    public void handle(FolderWatcher fw)
    {
        ArrayList<File> readyList;
        holdList.addAll(fw.getFreshFileList());
        readyList = new ArrayList<File>();

        for (File f : holdList)
        {
            if (fileIsReady(f))
            {
                readyList.add(f);                
            }
        }

        Collections.sort(readyList, fmc);

        for (File f : readyList)
        {
            if(imageQueue.offer(f))
            {
                holdList.remove(f);
            }
        }
    }

    public void setEnabled(boolean go)
    {
        enabled = go;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void run()
    {
        while (enabled)
        {
            try
            {
                //imageQueue is supposed to block while its empty                
                processImage(imageQueue.take());
            }
            catch (InterruptedException ie)
            {
                setEnabled(false);
            }
        }
    }

    public void stop()
    {
        // Do this first, in case interrupting the imageQueue doesn't work.
        setEnabled(false);
        thread.interrupt();
    }
    
    public ImagePlus getImageFromPath(final File file)
    {
        return getImageFromPath(file.getAbsolutePath());
    }
    
    public void dropImage(final File file)
    {
        dropImage(file.getAbsolutePath());
    }
    
    public void dropImage(final String path)
    {
        fileTable.remove(path);
    }
    
    public ImagePlus getImageFromPath(final String path)
    {
        return fileTable.get(path);
    }
    
    protected  boolean fileIsReady(File imageFile)
    {
        if (imageFile.isDirectory())
        {
            return false;
        }
        else
        {
            ImagePlus im = IJ.openImage(imageFile.getAbsolutePath());            
            if (im == null)
            {
                return false;
            }
            else
            {
                fileTable.put(imageFile.getAbsolutePath(), im);
                return true;
            }
            
        }
    }
    
    protected abstract void processImage(File imageFile);

}
