package fiji.updater.logic;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import fiji.updater.Updater;

import fiji.updater.util.Canceled;

import ij.IJ;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.List;

public class SSHFileUploader extends FileUploader {
	private final String host = "pacific.mpi-cbg.de";
	private final int hostKeyType = HostKey.SSHRSA;
	private final String hostKey =
		" 00 00 00 07 73 73 68 2d 72 73 61 00 00 00 01 23" +
		" 00 00 01 01 00 c5 8b 21 2f f2 59 8e d1 b9 de 7f" +
		" 57 e7 3c c9 d8 d8 0b d7 d2 f7 0e 67 62 3e f6 95" +
		" 79 09 ec d9 5a 17 3c 9f 31 1c 2a 33 75 d7 a2 1f" +
		" c2 15 74 ba b7 53 ef f4 94 e3 d9 5c 03 d8 7b bf" +
		" 23 43 0f 0e f7 87 14 e4 67 0f 64 04 91 f0 9b 24" +
		" 2a 31 59 7f 86 7f 50 77 6c 35 24 5a 78 9c a9 9a" +
		" fd a6 39 26 6f bf b0 8d 09 f9 0d fa 64 74 ec f5" +
		" dc 29 0d 07 e8 7b c5 ac 41 55 27 1c ba b1 d9 8b" +
		" 2a 56 a6 f7 d8 ad ce 44 7c fd ee d6 91 00 1f 8c" +
		" a3 ea 0c 68 39 1f c5 65 2f 95 b9 40 28 38 cd bf" +
		" 01 bf d1 ad e6 c6 34 d7 95 56 ae 2f f1 17 29 e9" +
		" a5 4e 4c 93 b2 6f e7 7f b2 5d 5c 9b b6 09 27 83" +
		" aa 87 33 aa 2b de 2a a0 c2 7a 9d 96 6c 0e 32 b3" +
		" 15 12 f2 8f 3f 9c 03 6f 9a 3b f5 8d 57 c0 9a 17" +
		" 0a 46 44 72 c4 83 5d 4d 23 1b d9 92 7b 02 98 e4" +
		" 9a 55 db 33 82 a0 c7 96 86 78 bf 31 fd b4 6c 62" +
		" bf 42 3a 05 63";

	private Session session;
	private Channel channel;
	private SourceFile currentUpload;
	private long uploadedBytes;
	private long uploadSize;
	private OutputStream out;
	private InputStream in;

	public SSHFileUploader(String username, String password,
			String uploadDirectory) throws JSchException {
		super(uploadDirectory);

		JSch jsch = new JSch();
		HostKey hostKeyObject = new HostKey(host, hostKeyType,
			getHostKeyBytes());
		jsch.getHostKeyRepository().add(hostKeyObject, null);

		session = jsch.getSession(username, host, 22);
		session.setPassword(password);
		session.connect();
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
			setCommand("rm " + uploadDir + Updater.XML_LOCK);
			out.close();
			channel.disconnect();
			throw cancel;
		}

		//Unlock process
		for (String lock : locks)
			setCommand("mv " + uploadDir + lock + ".lock " +
				uploadDir + lock);

		out.close();
		channel.disconnect();
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
			checkAckUploadError();

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
			checkAckUploadError();
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
		checkAckUploadError();
		int slash = directory.lastIndexOf('/', directory.length() - 2);
		return directory.substring(0, slash + 1);
	}

	private void cdInto(String directory) throws IOException {
		while (!directory.equals("")) {
			int slash = directory.indexOf('/');
			String name = (slash < 0 ?  directory :
					directory.substring(0, slash));
			String command = "D0755 0 " + name + "\n";
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
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			channel.setInputStream(null);

			// get I/O streams for remote scp
			out = channel.getOutputStream();
			in = channel.getInputStream();
			channel.connect();
		} catch (JSchException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}

	private void checkAckUploadError() throws IOException {
		if (checkAck(in) != 0)
			throw new IOException("Failed to upload " +
				currentUpload.getFilename());
	}

	public void disconnectSession() throws IOException {
		out.close();
		channel.disconnect();
		session.disconnect();
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
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char)c);
			} while (c != '\n');
			IJ.error(sb.toString());
		}
		return b;
	}

	private byte[] getHostKeyBytes() {
		byte[] result = new byte[hostKey.length() / 3];
		for (int i = 0; i < result.length; i++)
			result[i] = (byte)(fromHex(hostKey.charAt(i * 3 + 2))
				| (fromHex(hostKey.charAt(i * 3 + 1)) << 4));
		return result;
	}

	private int fromHex(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		throw new RuntimeException("Illegal hex character: " + c);
	}
}

