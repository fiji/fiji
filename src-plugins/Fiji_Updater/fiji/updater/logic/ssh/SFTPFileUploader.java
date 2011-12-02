package fiji.updater.logic.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import fiji.updater.logic.FileUploader;

import fiji.updater.util.Canceled;

import ij.IJ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;

/**
 * Uploads files to an update server using only SFTP protocol.
 * In contrast to SSHFileUploader it does not execute any remote commands using SSH.
 * This important when setting up update site on Source Forge its restricted Shell
 * does not allow execution of any remote commands.
 *
 * @author Jarek Sacha
 */
final public class SFTPFileUploader extends FileUploader {

	private final SFTPOperations sftp;

	/**
	 * Create new instance of operations and initialize connection to remote site.
	 *
	 * @param username		username used for the connection.
	 * @param sshHost		 remote host to connect to.
	 * @param uploadDirectory root directory on the remote server
	 * @param userInfo		authentication information.
	 * @throws JSchException if the connection fails.
	 */
	public SFTPFileUploader(final String username, final String sshHost, final String uploadDirectory,
							final UserInfo userInfo) throws JSchException {
		super(uploadDirectory);

		sftp = new SFTPOperations(username, sshHost, userInfo);
	}


	public synchronized void upload(final List<SourceFile> sources, final List<String> locks) throws IOException {

		timestamp = remoteTimeStamp();
		// 'timestamp' has to be set before calling setTitle("Uploading")
		// setTitle("Uploading") has a side effect of adding timestamp to file names of 'sources'
		// If timestamp is not set file names will end with '*-0'.
		// See fiji.updater.logic.PluginUploader.VerifyTimestamp.setTitle(String)
		setTitle("Uploading");


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

		int sizeOfFilesUploadedSoFar = 0;

		for (final SourceFile source : sources) {
			final String target = source.getFilename();

			/*
			 * Make sure that the file is there; this is critical
			 * to get the server timestamp from db.xml.gz.lock.
			 */
			addItem(source);

			// send contents of file
			final InputStream input = source.getInputStream();
			final int currentFileSize = (int) source.getFilesize();
			final String dest = this.uploadDir + target;
			try {
				log("Upload '" + source.getFilename() + "', size " + source.getFilesize());

				// Setup progress monitoring for current file
				final int uploadedBytesCount = sizeOfFilesUploadedSoFar;
				final SFTPOperations.ProgressListener listener = new SFTPOperations.ProgressListener() {
					@Override
					public void progress(final long currentCount) {
						setItemCount((int) currentCount, currentFileSize);
						setCount(uploadedBytesCount + (int) currentCount, total);
					}
				};

				// Upload file
				sftp.put(input, dest, listener);
			} finally {
				input.close();
			}

			// Update progress notifications
			sizeOfFilesUploadedSoFar += currentFileSize;
			setItemCount(currentFileSize, currentFileSize);
			setCount(sizeOfFilesUploadedSoFar, total);

			itemDone(source);
		}

		// Complete progress notification
		done();
	}

	void disconnectSession() throws IOException {
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

	private static void log(final String message) {
		if (IJ.debugMode) {
			IJ.log(message);
		}
	}
}