package edu.utexas.archipelago;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.data.FileChunk;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;

import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
public final class ArchipelagoUtils
{
    private ArchipelagoUtils(){}

    private static String getCacheDirectory() throws IOException
    {
        String dir = FijiArchipelago.getFileRoot() + "/cache";
        final File dirFile = new File(dir);
        if (!dirFile.exists() && !dirFile.mkdirs())
        {
            throw new IOException("Could not create directory " + dirFile.getAbsolutePath());
        }

        return dir;
    }

    public static boolean getFileList(final Collection<File> files, final ImagePlus imp) throws IOException
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

    private static <T> T getFirst(Collection<T> ts)
    {
        final Iterator<T> it = ts.iterator();
        return it.hasNext() ? it.next() : null;
    }

    public static VirtualStack makeVirtualStack(final Collection<FileChunk> files,
                                                final int width, final int height)
    {
        final VirtualStack vs = new VirtualStack(width, height, ColorModel.getRGBdefault(),
                new File(getFirst(files).getData()).getParent());

        for (FileChunk fc : files)
        {
            FijiArchipelago.log("Appending " + fc.getData() + " to VS");
            vs.addSlice(new File(fc.getData()).getName());
        }

        return vs;
    }




}
