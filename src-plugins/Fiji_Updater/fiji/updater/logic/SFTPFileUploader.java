package fiji.updater.logic;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import fiji.updater.util.Canceled;
import ij.IJ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // TODO handle situation when *.lock file already exists on the server
    // TODO preserve modification times of uploaded files
    // TODO Use SFTP progress monitor to have more detailed progress notifications (?)
    // TODO Share ConfigInfo and getIdentity with SSHFileUploader (through a separate class SSHConfigInfo?)

    final SFTPOperations sftp;


    public SFTPFileUploader(final String username, final String sshHost, final String uploadDirectory,
                            final UserInfo userInfo) throws JSchException {
        super(uploadDirectory);

        sftp = new SFTPOperations(username, sshHost, userInfo);
    }


    //Steps to accomplish entire upload task
    public synchronized void upload(final List<SourceFile> sources, final List<String> locks) throws IOException {

        setTitle("Uploading");

        timestamp = remoteTimeStamp();

        try {
            uploadFiles(sources);
        } catch (final Canceled cancel) {
            // Delete locks
            for (final String lock : locks) {
                final String path = uploadDir + lock + ".lock";
                try {
                    sftp.rm(path);
                } catch (final IOException ex) {
                    // Do not re-throw, since 'cancel' exception will be thrown.
                }
            }
            throw cancel;
        }

        // Unlock process
        for (final String lock : locks) {
            final String src = uploadDir + lock + ".lock";
            final String dest = uploadDir + lock;
            sftp.rename(src, dest);
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
                log("Upload '" + source.getFilename() + "', size " + source.getFilesize());
                sftp.put(input, dest);
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
        sftp.disconnect();
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
        final long timestamp;
        sftp.put(in, destFile);
        timestamp = sftp.timestamp(destFile);
        sftp.rm(destFile);

        return timestamp;
    }


    private void log(final String message) {
        IJ.log(message);
    }


}