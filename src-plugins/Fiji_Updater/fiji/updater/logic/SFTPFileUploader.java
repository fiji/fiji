package fiji.updater.logic;

import com.jcraft.jsch.*;
import fiji.updater.util.Canceled;
import ij.IJ;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Uploads files to an update server using only SFTP protocol.
 * In contrast to SSHFileUploader it does not execute any remote commands.
 * This important when setting up update site on Source Forge its restricted Shell
 * does not allow execution of any remote commands only copying of files.
 *
 * @author Jarek Sacha
 */
final public class SFTPFileUploader extends FileUploader {

    private Session session;
    private ChannelSftp sftp;

    // TODO handle situation when *.lock file already exists on the server
    // TODO preserve modification times of uploaded files
    // TODO Use SFTP progress monitor to have more detailed progress notifications (?)
    // TODO Share ConfigInfo and getIdentity with SSHFileUploader (through a separate class SSHConfigInfo?)


    public SFTPFileUploader(String username, String sshHost, final String uploadDirectory,
                            final UserInfo userInfo) throws JSchException {
        super(uploadDirectory);

        int port = 22, colon = sshHost.indexOf(':');
        if (colon > 0) {
            port = Integer.parseInt(sshHost.substring(colon + 1));
            sshHost = sshHost.substring(0, colon);
        }

        final JSch jsch = new JSch();

        // Reuse ~/.ssh/known_hosts file
        final File knownHosts = new File(new File(System.getProperty("user.home"), ".ssh"), "known_hosts");
        jsch.setKnownHosts(knownHosts.getAbsolutePath());

        final ConfigInfo configInfo = getIdentity(username, sshHost);
        if (configInfo != null) {
            if (configInfo.username != null) {
                username = configInfo.username;
            }
            if (configInfo.sshHost != null) {
                sshHost = configInfo.sshHost;
            }
            if (configInfo.identity != null) {
                jsch.addIdentity(configInfo.identity);
            }
        }

        session = jsch.getSession(username, sshHost, port);
        session.setUserInfo(userInfo);
        session.connect();

        sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
    }


    //Steps to accomplish entire upload task
    public synchronized void upload(final List<SourceFile> sources, final List<String> locks) throws IOException {
        timestamp = remoteTimeStamp();
        setTitle("Uploading");

        try {
            uploadFiles(sources);
        } catch (final Canceled cancel) {
            // Delete locks
            for (final String lock : locks) {
                final String path = uploadDir + lock + ".lock";
                try {
                    SFTPUtils.rm(sftp, path);
                } catch (final SftpException ex) {
                    // Handle so it ges to log, but do not re-throw, since 'cancel' exception will be thrown.
                    //noinspection ThrowableResultOfMethodCallIgnored
                    wrapException("Failed to remove remote file '" + path + "'.", ex);
                }
            }
            throw cancel;
        }

        // Unlock process
        for (final String lock : locks) {
            final String src = uploadDir + lock + ".lock";
            final String dest = uploadDir + lock;
            try {
                SFTPUtils.rename(sftp, src, dest);
            } catch (final SftpException ex) {
                throw wrapException("Failed to rename remote file '" + src + "' to '" + dest + "'.", ex);
            }
        }

        disconnectSession();
    }


    private void uploadFiles(final List<SourceFile> sources) throws IOException {
        calculateTotalSize(sources);

        int count = 0;

        for (final SourceFile source : sources) {
            final String target = source.getFilename();

            /*
             * Make sure that the file is there; this is critical
             * to get the server timestamp from db.xml.gz.lock.
             */
            addItem(source);

            // send contents of file
            final InputStream input = source.getInputStream();
            final String dest = this.uploadDir + target;
            try {
                log("Upload '" + source.getFilename() + ", size " + source.getFilesize());
                SFTPUtils.put(sftp, input, dest);
            } catch (final SftpException ex) {
                throw wrapException("Failed to upload file '" + target + "'.", ex);
            } finally {
                input.close();
            }

            // Update progress notifications
            final int fileSize = (int) source.getFilesize();
            count += fileSize;
            setItemCount(fileSize, fileSize);
            setCount(count, total);

            itemDone(source);
        }

        // Complete progress notification
        done();
    }


    public void disconnectSession() throws IOException {
        // TODO Are those sleep commands needed with SFTP?
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            /* ignore */
        }
        int exitStatus = sftp.getExitStatus();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            /* ignore */
        }
        sftp.disconnect();
        session.disconnect();
        if (exitStatus != -1)
            throw new IOException("Command failed (see Log)!");
    }


    /**
     * Extract current time at remote server
     *
     * @return time stamp
     * @throws IOException when execution of remote date command fails.
     */
    private long remoteTimeStamp() throws IOException {
        // Normally time stamp would be created using shell command: date +%Y%m%d%H%M%S
        // Shell commands cannot be executed on restricted shell accounts like SourceForge
        // Need to simulate date command by creating a temporary file and reading its modification time.

        final InputStream in = new ByteArrayInputStream("".getBytes());
        final String destFile = uploadDir + "timestamp";
        final int mTime;
        try {
            SFTPUtils.put(sftp, in, destFile);
            final SftpATTRS stats = sftp.stat(destFile);
            SFTPUtils.rm(sftp, destFile);
            mTime = stats.getMTime();
        } catch (final SftpException ex) {
            throw wrapException("Failed to extract remote timestamp.", ex);
        }

        final Date date = new Date(((long) mTime) * 1000);
        return Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmss").format(date));
    }


    private IOException wrapException(final String message, final SftpException ex) {
        final String m = message + " SFTP error id=" + ex.id + ": " + ex.getMessage();
        IJ.log(m);
        return new IOException(m);
    }


    private void log(final String message) {
        IJ.log(message);
    }


    protected static class ConfigInfo {

        String username, sshHost, identity;
    }


    protected ConfigInfo getIdentity(final String username, final String sshHost) {
        final File config = new File(new File(System.getProperty("user.home"), ".ssh"), "config");
        if (!config.exists()) {
            return null;
        }

        try {
            final ConfigInfo result = new ConfigInfo();
            final BufferedReader reader = new BufferedReader(new FileReader(config));
            boolean hostMatches = false;
            for (; ;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                int space = line.indexOf(' ');
                if (space < 0) {
                    continue;
                }
                final String key = line.substring(0, space).toLowerCase();
                if (key.equals("host")) {
                    hostMatches = line.substring(5).trim().equals(sshHost);
                } else if (hostMatches) {
                    if (key.equals("user")) {
                        if (username == null || username.equals("")) {
                            result.username = line.substring(5).trim();
                        }
                    } else if (key.equals("hostname")) {
                        result.sshHost = line.substring(9).trim();
                    } else if (key.equals("identityfile")) {
                        result.identity = line.substring(13).trim();
                    }
                    // TODO what if condition do match any here?
                }
            }
            reader.close();
            return result;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}