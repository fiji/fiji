package fiji;

import ij.IJ;

import java.io.IOException;
import java.io.InputStream;

public class SimpleExecuter {
	protected StreamDumper stdout, stderr;
	protected int exitCode;

	public SimpleExecuter(String[] cmdarray) throws IOException {
		Process process = Runtime.getRuntime().exec(cmdarray);
		process.getOutputStream().close();
		stderr = new StreamDumper(process.getErrorStream());
		stdout = new StreamDumper(process.getInputStream());
		for (;;) try {
			exitCode = process.waitFor();
			break;
		} catch (InterruptedException e) { /* ignore */ }
		for (;;) try {
			stdout.join();
			break;
		} catch (InterruptedException e) { /* ignore */ }
		for (;;) try {
			stderr.join();
			break;
		} catch (InterruptedException e) { /* ignore */ }
	}

	public int getExitCode() {
		return exitCode;
	}

	public String getOutput() {
		return stdout.out.toString();
	}

	public String getError() {
		return stderr.out.toString();
	}

	protected class StreamDumper extends Thread {
		protected InputStream in;
		public StringBuffer out;

		public StreamDumper(InputStream in) {
			this.in = in;
			out = new StringBuffer();
			start();
		}

		public void run() {
			byte[] buffer = new byte[16384];
			try {
				for (;;) {
					int count = in.read(buffer);
					if (count < 0)
						break;
					out.append(new String(buffer, 0, count));
				}
				in.close();
			} catch (IOException e) {
				stderr.out.append(e.toString());
			}
		}
	}

	public static void main(String[] args) {
		try {
			SimpleExecuter executer = new SimpleExecuter(args);
			IJ.log("status: " + executer.getExitCode());
			IJ.log("output: " + executer.getOutput());
			IJ.log("error: " + executer.getError());
		} catch (IOException e) {
			IJ.handleException(e);
		}
	}
}