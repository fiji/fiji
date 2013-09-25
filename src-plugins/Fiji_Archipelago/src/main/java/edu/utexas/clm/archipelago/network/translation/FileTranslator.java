package edu.utexas.clm.archipelago.network.translation;

/**
 *
 */
public interface FileTranslator
{
    public String getLocalPath(final String remotePath);

    public String getRemotePath(final String localPath);

}
