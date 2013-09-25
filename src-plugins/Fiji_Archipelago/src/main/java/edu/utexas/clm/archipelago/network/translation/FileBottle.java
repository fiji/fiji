package edu.utexas.clm.archipelago.network.translation;

import edu.utexas.clm.archipelago.listen.MessageType;
import edu.utexas.clm.archipelago.network.MessageXC;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class FileBottle implements Bottle<File>
{
    private final String path;
    private final boolean exists;

    public FileBottle(final File file, final MessageXC xc)
    {
        path = xc.getRemotePath(file.getAbsolutePath());
        exists = file.exists();
    }

    public File unBottle(final MessageXC xc) throws IOException
    {
        final File file = xc.getLocalFile(new File(path));
        if (exists && !file.exists())
        {
            xc.queueMessage(MessageType.ERROR, new IOException("File " + file +
                    " should exist, but it doesn't"));
        }
        return file;
    }
}
