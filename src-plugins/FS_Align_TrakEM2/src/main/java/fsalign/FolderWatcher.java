package fsalign;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * @author Larry Lindsey
 */
public class FolderWatcher extends TimerTask{

    /**
     * A FileFilter that accepts all folders that are not a trakem2 cache folder.
     * In other words, all folders are accepted as long as File.getName().startsWith("trakem2.")
     * return false.
     */
    public static class FolderFilter implements FileFilter
    {

        public boolean accept(File file)
        {
            return file.isDirectory() && !file.getName().startsWith("trakem2.");
        }
    }

    /**
     * Adapts javax.swing.filechooser.FileFilter's to be used as java.io.FileFilter's
     */
    private static class FileFilterAdapter implements FileFilter
    {
        final javax.swing.filechooser.FileFilter javaxFileFilter;

        public FileFilterAdapter(final javax.swing.filechooser.FileFilter filter)
        {
            javaxFileFilter = filter;
        }

        public boolean accept(File file) {
            return javaxFileFilter.accept(file);
        }
    }


    private final Vector<File> folders;
    private final Timer timer;
    private final long interval;
    private boolean running;
    private final Vector<File> allFileList;
    private final Vector<File> freshFileList;
    private final Vector<FileListener> listenerList;
    private final FileFilter fileFilter;
    private final FileFilter dirFilter;

    
    public FolderWatcher(final String folderName, final long inInterval)
    {
        this(folderName, inInterval,
                new FileFilter() {
                    public boolean accept(File file) {
                        return true;
                    }
                });
    }

    public FolderWatcher(final String folderName, final long inInterval, String... extension)
    {
        this(folderName, inInterval, new FileNameExtensionFilter("File extension", extension));
    }

    public FolderWatcher(final String folderName, final long inInterval,
                         final javax.swing.filechooser.FileFilter filter)
    {
        this(folderName, inInterval, new FileFilterAdapter(filter));
    }

    /**
     * Watch the folder at the given location for changes, polling at a given interval, ignoring
     * all files but those accepted by the given filter. Does not start() automatically.
     * @param folderName the String path to the folder in question
     * @param inInterval the poll interval in milliseconds
     * @param filter a filter to accept the files that we're interested in.
     */
    public FolderWatcher(final String folderName, final long inInterval, final FileFilter filter)
    {
        folders = new Vector<File>();
        timer = new Timer();
        running = false;
        interval = inInterval;
        allFileList = new Vector<File>();
        listenerList = new Vector<FileListener>();
        freshFileList = new Vector<File>();
        fileFilter = filter;
        dirFilter = new FolderFilter();
        System.out.println("Folder: " + folderName);
        folders.add(new File(folderName));
    }
    
    public void addListener(FileListener fl)
    {
        listenerList.add(fl);
    }
        

    public void start() throws IOException
    {
        if (!running)
        {
            if (!folders.get(0).exists())
            {
                throw new IOException(folders.get(0).getName() + " does not exist");
            }
            else if (!folders.get(0).isDirectory())
            {
                throw new IOException(folders.get(0).getName() + " is not a directory");
            }
            else
            {
                timer.schedule(this, (long)0, interval);
                running = true;
            }
        }
    }

    /**
     * Returns a list of all files seen.
     * @return a list of all files seen.
     */
    public Vector<File> getFileList()
    {
        return new Vector<File>(allFileList);
    }

    /**
     * Returns a list of file newly seen since the last poll epoch.
     * @return a list of file newly seen since the last poll epoch.
     */
    public Vector<File> getFreshFileList()
    {
        return new Vector<File>(freshFileList);
    }

    /**
     * Stop this FolderWatcher/TimerTask, cancel()'ing all FileListeners as well.
     * @return super.cancel()
     */
    public boolean cancel()
    {
        for (FileListener fl : listenerList)
        {
            fl.stop();
        }
        
        return super.cancel();
    }

    public void run()
    {
        freshFileList.clear();

        int i = 0;
        while (i < folders.size())
        {
            File[] subdirArray = folders.get(i).listFiles(dirFilter);
            for (File subdir : subdirArray)
            {
                if (!folders.contains(subdir))
                {
                    folders.add(subdir);
                }
            }
            
            ++i;
        }

        for (File folder : folders)
        {
            File[] fileArray = folder.listFiles(fileFilter);
            for (File f : fileArray)
            {
                if (!allFileList.contains(f))
                {
                    allFileList.add(f);
                    freshFileList.add(f);
                }
            }
        }

        for (FileListener fl : listenerList)
        {
            fl.handle(this);
        }
    }

    /**
     * Because who doesn't like a main method?
     * @param args looks at args[0] for a folder to watch.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.err.println("Need a directory name");
        }
        else
        {
            FolderWatcher fw = new FolderWatcher(args[0], 1100);
            fw.addListener(
                    new FileListener() {
                        public void handle(FolderWatcher fw) {
                            Vector<File> fresh = fw.getFreshFileList();
                            if (fresh.size() > 0)
                            {
                                for (File f : fresh)
                                {
                                    System.out.println(f);
                                }                                
                            }
                            else
                            {
                                System.out.println("No new files");
                            }
                            System.out.println();
                        }

                        public void stop(){}
                    }
            );
            fw.start();
        }
    }
    
}
