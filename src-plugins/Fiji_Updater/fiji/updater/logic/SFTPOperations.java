package fiji.updater.logic;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.InputStream;
import java.util.Vector;
import java.util.logging.Logger;


/**
 * @author Jarek Sacha
 * @since 4/21/11 2:15 PM
 */
final class SFTPOperations {

    private static final Logger LOGGER = Logger.getLogger(SFTPOperations.class.getName());


    private SFTPOperations() {
    }


    public static void put(final ChannelSftp sftp, final InputStream in, final String dest) throws SftpException {
        LOGGER.fine("put(...,...," + dest + ")");
        mkParentDirs(sftp, dest);
        sftp.put(in, dest);
    }


    public static void rename(final ChannelSftp sftp, final String src, final String dest) throws SftpException {

        rm(sftp, dest);
        mkParentDirs(sftp, dest);

        // Rename to final
        sftp.rename(src, dest);

    }


    public static void rm(final ChannelSftp sftp, final String path) throws SftpException {

        if (SFTPOperations.fileExists(sftp, path)) {
            sftp.rm(path);
        }
    }


    /**
     * Test if specified path exists on the remote server.
     *
     * @param sftp sftp channel handle.
     * @param path path to test
     * @return {@code true} if the path exists
     * @throws com.jcraft.jsch.SftpException in case of sftp error.
     */
    public static boolean fileExists(final ChannelSftp sftp, final String path) throws SftpException {
        LOGGER.fine("fileExists(" + path + ")");

        final String filePath = removeTrailingSlash(path);
        final String parent;
        final String fileName;
        if (filePath.contains("/")) {
            final int index = filePath.lastIndexOf("/") + 1;
            parent = filePath.substring(0, index);
            fileName = filePath.substring(index);
        } else {
            parent = "";
            fileName = path;
        }

        final Vector files = sftp.ls(parent);
        for (int i = 0; i < files.size(); i++) {
            final ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) files.elementAt(i);
            if (fileName.equals(e.getFilename())) {
                return true;
            }
        }

        return false;
    }


    /**
     * Creates the directory named by this path, including any necessary but nonexistent parent directories.
     *
     * @param sftp sftp channel handle.
     * @param path path for which to create parent directories
     * @throws com.jcraft.jsch.SftpException in case of sftp error.
     */
    public static void mkParentDirs(final ChannelSftp sftp, final String path) throws SftpException {
        LOGGER.fine("mkParentDirs(" + path + ")");
        mkParentDirs(sftp, "", path);
    }


    private static void mkParentDirs(final ChannelSftp sftp, final String root, final String path) throws SftpException {
        if (path.contains("/")) {
            final int index = path.indexOf("/");
            final String newRoot = root + path.substring(0, index + 1);
            final String newPath = path.substring(index + 1);
            if (!"/".equals(newRoot) && !fileExists(sftp, newRoot)) {
                sftp.mkdir(newRoot);
            }
            mkParentDirs(sftp, newRoot, newPath);
        }
    }


    private static String removeTrailingSlash(final String path) {
        return path.endsWith("/")
                ? removeTrailingSlash(path.substring(0, path.length() - 1))
                : path;
    }

}
