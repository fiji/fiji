package edu.utexas.clm.archipelago.network.translation;

import java.io.File;

/**
 *
 */
public class PathSubstitutingFileTranslator implements FileTranslator
{

    private final String localFileRoot, remoteFileRoot;

    public PathSubstitutingFileTranslator(final String local, final String remote)
    {
        if (local.endsWith("/") || local.endsWith("\\"))
        {
            localFileRoot = local.substring(0, local.length() - 1);
        }
        else
        {
            localFileRoot = local;
        }

        if (remote.endsWith("/") || remote.endsWith("\\"))
        {
            remoteFileRoot = remote.substring(0, remote.length() - 1);
        }
        else
        {
            remoteFileRoot = remote;
        }
    }

    public String getLocalPath(String remotePath) {

        if (remotePath.startsWith(remoteFileRoot))
        {
            return remotePath.replace(remoteFileRoot, localFileRoot);
        }
        else
        {
            return remotePath;
        }
    }

    public String getRemotePath(String localPath)
    {
        if (localPath.startsWith(localFileRoot))
        {
            return localPath.replace(remoteFileRoot, localFileRoot);
        }
        else
        {
            return localPath;
        }
    }
}
