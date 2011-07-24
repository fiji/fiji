package fiji.updater.logic;

import com.jcraft.jsch.*;
import ij.IJ;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;


/**
 * @author Jarek Sacha
 * @since 4/21/11 2:15 PM
 */
final class SFTPOperations {

    private static final Logger LOGGER = Logger.getLogger(SFTPOperations.class.getName());

    final private Session session;
    final private ChannelSftp sftp;


    public SFTPOperations(final String username, final String sshHost, final UserInfo userInfo) throws JSchException {

        session = SSHSessionCreator.connect(username, sshHost, userInfo);

        sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
    }


    public void disconnect() throws IOException {
        int exitStatus = sftp.getExitStatus();
        sftp.disconnect();
        session.disconnect();
        if (exitStatus != -1) {
            throw new IOException("Command failed (see Log)!");
        }
    }


    public void put(final InputStream in, final String dest) throws IOException {
        LOGGER.fine("put(...,...," + dest + ")");
        mkParentDirs(dest);

        try {
            sftp.put(in, dest);
        } catch (final SftpException ex) {
            throw wrapException("Failed to upload file '" + dest + "'.", ex);
        }
    }


    public void rename(final String src, final String dest) throws IOException {

        rm(dest);
        mkParentDirs(dest);

        // Rename to final
        try {
            sftp.rename(src, dest);
        } catch (final SftpException ex) {
            throw wrapException("Failed to rename remote file '" + src + "' to '" + dest + "'.", ex);
        }

    }


    public void rm(final String path) throws IOException {

        if (fileExists(path)) {
            try {
                sftp.rm(path);
            } catch (final SftpException ex) {
                throw wrapException("Failed to remove remote file '" + path + "'.", ex);
            }
        }
    }


    /**
     * Test if specified path exists on the remote server.
     *
     * @param path path to test
     * @return {@code true} if the path exists
     * @throws IOException in case of sftp error.
     */
    public boolean fileExists(final String path) throws IOException {
        LOGGER.fine("fileExists2(" + path + ")");

        // Traversing the path may hit directories without read access.
        // Rather than listing content to see if directory exists just test the path directly (using nasty exception).
        try {
            sftp.stat(path);
            return true;
        } catch (final SftpException e) {
            return false;
        }
    }


    /**
     * Creates the directory named by this path, including any necessary but nonexistent parent directories.
     *
     * @param path path for which to create parent directories
     * @throws IOException in case of sftp error.
     */
    public void mkParentDirs(final String path) throws IOException {
        LOGGER.fine("mkParentDirs(" + path + ")");
        mkParentDirs("", path);
    }


    public long timestamp(final String timestampFile) throws IOException {
        final int mTime;
        try {
            final SftpATTRS stats = sftp.stat(timestampFile);
            mTime = stats.getMTime();
        } catch (final SftpException ex) {
            throw new IOException("Failed to extract remote timestamp from file '" + timestampFile + "'.", ex);
        }

        final Date date = new Date(((long) mTime) * 1000);
        return Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmss").format(date));
    }


    private void mkParentDirs(final String root, final String path) throws IOException {
        if (path.contains("/")) {
            final int index = path.indexOf("/");
            final String newRoot = root + path.substring(0, index + 1);
            final String newPath = path.substring(index + 1);
            if (!"/".equals(newRoot) && !fileExists(newRoot)) {
                try {
                    sftp.mkdir(newRoot);
                } catch (final SftpException ex) {
                    throw wrapException("Failed to create directory '" + newRoot + "'", ex);
                }
            }
            mkParentDirs(newRoot, newPath);
        }
    }


    private static String removeTrailingSlash(final String path) {
        return path.endsWith("/")
                ? removeTrailingSlash(path.substring(0, path.length() - 1))
                : path;
    }


    private IOException wrapException(final String message, final SftpException ex) {
        final String m = message + " SFTP error id=" + ex.id + ": " + ex.getMessage();
        IJ.log(m);
        return new IOException(m);
    }

}
