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
    /**
     * A Comparator used to sort files in ascending order of modified time.
     */
    private static class FileModifiedComparator implements Comparator<File>
    {
        public int compare(File file1, File file2)
        {
            return (int)(file1.lastModified() - file2.lastModified());
        }
    }

    /**
     * ArrayBlockingQueue size.
     */
    private static final int QUEUE_CAP = 256;
    private final ArrayBlockingQueue<File> imageQueue;
    private final Vector<File> holdList;
    private final FileModifiedComparator fmc;
    private final Hashtable<String, ImagePlus> fileTable;
    private final Thread thread;
    private boolean enabled;

    /**
     * Create a FolderWatcher to watch the given folder
     * @param folderName the folder to watch
     * @param interval the poll interval
     * @param imageListener the ImageListener to use
     * @param regexp the regular expression used to match image files.
     * @return a new FolderWatcher to watch the given folder
     */
    public static FolderWatcher imageFolderWatcher(final String folderName, final int interval,
                                                   final ImageListener imageListener,
                                                   final String regexp)
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

    /**
     * Handles a new file event passed by a FolderWatcher.
     * The FolderWatcher passes itself as a parameter.
     * @param fw the FolderWatcher calling this method
     */
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

    /**
     * Causes this ImageListener to quit
     */
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

    /**
     * Remove the ImagePlus at the given path from the HashTable mapping them together.
     * The purpose of this function is to help with garbage collection.
     * @param path the path keying the given ImagePlus
     */
    public void dropImage(final String path)
    {
        fileTable.remove(path);
    }

    /**
     * Returns an ImagePlus given a path String.
     * To check whether an image file is ready, an ImageListener attempts to open it as an
     * ImagePlus. If a non-null ImagePlus is returned by IJ.open, the image is determined to be
     * ready. In that case, in order to avoid duplication of effort, the ImagePlus is stored in
     * a HashTable (which is synchronized), and keyed to its path. This function returns that
     * already-opened ImagePlus for user further down the line.
     *
     * @param path the path of the given ImagePlus
     * @return the ImagePlus that was previously opened from the given path by this ImageListener.
     */
    public ImagePlus getImageFromPath(final String path)
    {
        return fileTable.get(path);
    }

    /**
     * Checks whether an image file is ready, in the sense that it has been completely written to
     * disk. This is determined by whether it can be opened as a non-null ImagePlus.
     * @param imageFile the File representing the image file to test.
     * @return true if the image is ready, false otherwise.
     */
    protected boolean fileIsReady(File imageFile)
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

    /**
     * An inheriting class should implement a method that processes each image file as it is
     * popped from the queue. Images are popped in order of modification time.
     * @param imageFile the image File to process
     */
    protected abstract void processImage(File imageFile);

}
