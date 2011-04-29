package fiji.updater.logic;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import fiji.updater.util.Canceled;
import fiji.updater.util.IJLogOutputStream;
import fiji.updater.util.InputStream2IJLog;

import ij.IJ;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;

public class SSHFileUploader extends FileUploader {
	private Session session;
	private Channel channel;
	private OutputStream out;
	protected OutputStream err;
	private InputStream in;

	public SSHFileUploader(String username, String sshHost, String uploadDirectory,
			UserInfo userInfo) throws JSchException {
		super(uploadDirectory);

		err = new IJLogOutputStream();

        session = SSHSessionCreator.connect(username, sshHost, userInfo);
	}

	//Steps to accomplish entire upload task
	public synchronized void upload(List<SourceFile> sources,
			List<String> locks) throws IOException {
		setCommand("date +%Y%m%d%H%M%S");
		timestamp = readNumber(in);
		setTitle("Uploading");

		String uploadFilesCommand = "scp -p -t -r " + uploadDir;
		setCommand(uploadFilesCommand);
		if (checkAck(in) != 0) {
			throw new IOException("Failed to set command " + uploadFilesCommand);
		}

		try {
			uploadFiles(sources);
		} catch (Canceled cancel) {
			for (String lock : locks)
				setCommand("rm " + uploadDir + lock + ".lock");
			out.close();
			channel.disconnect();
			throw cancel;
		}

		//Unlock process
		for (String lock : locks)
			setCommand("mv " + uploadDir + lock + ".lock " +
				uploadDir + lock);

		out.close();
		disconnectSession();
	}

	private void uploadFiles(List<SourceFile> sources) throws IOException {
		calculateTotalSize(sources);
		int count = 0;

		String prefix = "";
		byte[] buf = new byte[16384];
		for (SourceFile source : sources) {
			String target = source.getFilename();
			while (!target.startsWith(prefix))
				prefix = cdUp(prefix);

			// maybe need to enter directory
			int slash = target.lastIndexOf('/');
			String directory = target.substring(0, slash + 1);
			cdInto(directory.substring(prefix.length()));
			prefix = directory;

			// notification that file is about to be written
			String command = source.getPermissions() + " "
				+ source.getFilesize() + " "
				+ target.substring(slash + 1) + "\n";
			out.write(command.getBytes());
			out.flush();
			checkAckUploadError(target);

			/*
			 * Make sure that the file is there; this is critical
			 * to get the server timestamp from db.xml.gz.lock.
			 */
			addItem(source);

			// send contents of file
			InputStream input = source.getInputStream();
			int currentCount = 0;
			int currentTotal = (int)source.getFilesize();
			for (;;) {
				int len = input.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len);
				currentCount += len;
				setItemCount(currentCount, currentTotal);
				setCount(count + currentCount, total);
			}
			input.close();
			count += currentCount;

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			checkAckUploadError(target);
			itemDone(source);
		}

		while (!prefix.equals("")) {
			prefix = cdUp(prefix);
		}

		done();
	}

	private String cdUp(String directory) throws IOException {
		out.write("E\n".getBytes());
		out.flush();
		checkAckUploadError(directory);
		int slash = directory.lastIndexOf('/', directory.length() - 2);
		return directory.substring(0, slash + 1);
	}

	private void cdInto(String directory) throws IOException {
		while (!directory.equals("")) {
			int slash = directory.indexOf('/');
			String name = (slash < 0 ?  directory :
					directory.substring(0, slash));
			String command = "D2775 0 " + name + "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0)
				throw new IOException("Cannot enter directory " + name);
			if (slash < 0)
				return;
			directory = directory.substring(slash + 1);
		}
	}

	private void setCommand(String command) throws IOException {
		if (out != null) {
			out.close();
			channel.disconnect();
		}
		try {
			if (IJ.debugMode)
				IJ.log("launching command " + command);
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			channel.setInputStream(null);
			((ChannelExec)channel).setErrStream(err);

			// get I/O streams for remote scp
			out = channel.getOutputStream();
			in = channel.getInputStream();
			channel.connect();
		} catch (JSchException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}

	private void checkAckUploadError(String target) throws IOException {
		if (checkAck(in) != 0)
			throw new IOException("Failed to upload " + target);
	}

	public void disconnectSession() throws IOException {
		new InputStream2IJLog(in);
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			/* ignore */
		}
		out.close();
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			/* ignore */
		}
		int exitStatus = channel.getExitStatus();
		if (IJ.debugMode)
			IJ.log("disconnect session; exit status is " + exitStatus);
		channel.disconnect();
		session.disconnect();
		err.close();
		if (exitStatus != 0)
			throw new IOException("Command failed with status " + exitStatus + " (see Log)!");
	}

	protected long readNumber(InputStream in) throws IOException {
		long result = 0;
		for (;;) {
			int b = in.read();
			if (b >= '0' && b <= '9')
				result = 10 * result + b - '0';
			else if (b == '\n')
				return result;
		}
	}

	private int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if (b == 0)
			return b;
		IJ.handleException(new Exception("checkAck returns " + b));
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char)c);
			} while (c != '\n');
			IJ.log("checkAck returned '" + sb.toString() + "'");
			IJ.error(sb.toString());
		}
		return b;
	}
}