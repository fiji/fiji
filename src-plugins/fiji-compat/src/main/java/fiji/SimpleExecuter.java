package fiji;

import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @deprecated use {@link org.scijava.util.ProcessUtils#exec(File, PrintStream, PrintStream, String...)} instead
 * 
 * @author Johannes Schindelin
 */
public class SimpleExecuter {
	protected StreamDumper stdout, stderr;
	protected int exitCode;

	public static interface LineHandler {
		public void handleLine(String line);
	}

	public SimpleExecuter(String... cmdarray) throws IOException {
		this(cmdarray, null, null, null);
	}

	public SimpleExecuter(File workingDirectory, String... cmdarray) throws IOException {
		this(cmdarray, null, null, workingDirectory);
	}

	public SimpleExecuter(File workingDirectory, OutputStream out, OutputStream err, String... cmdarray) throws IOException {
		this(cmdarray, null, null, null, out, err, workingDirectory);
	}

	public SimpleExecuter(String[] cmdarray, File workingDirectory) throws IOException {
		this(cmdarray, null, null, workingDirectory);
	}

	public SimpleExecuter(String[] cmdarray, LineHandler out, LineHandler err) throws IOException {
		this(cmdarray, out, err, null);
	}

	public SimpleExecuter(String[] cmdarray, LineHandler out, LineHandler err, File workingDirectory) throws IOException {
		this(cmdarray, null, out, err, workingDirectory);
	}

	public SimpleExecuter(String[] cmdarray, InputStream in, LineHandler out, LineHandler err, File workingDirectory) throws IOException {
		this(cmdarray, in, out, err, null, null, workingDirectory);
	}

	public SimpleExecuter(String[] cmdarray, InputStream in, LineHandler out, LineHandler err, OutputStream out2, OutputStream err2, File workingDirectory) throws IOException {
		if (out != null && out2 != null)
			throw new RuntimeException("Cannot handle two outputs");
		if (err != null && err2 != null)
			throw new RuntimeException("Cannot handle two error outputs");
		if (IJ.isWindows()) {
			String interpreter = getInterpreter(cmdarray[0]);
			if (interpreter != null) {
				String[] newArray = new String[cmdarray.length + 1];
				newArray[0] = interpreter;
				System.arraycopy(cmdarray, 0, newArray, 1, cmdarray.length);
				cmdarray = newArray;
			}
		}
		Process process = Runtime.getRuntime().exec(cmdarray, null, workingDirectory);
		stderr = err != null ? getDumper(err, process.getErrorStream()) : getDumper(process.getErrorStream(), err2, true);
		stdout = out != null ? getDumper(out, process.getInputStream()) : getDumper(process.getInputStream(), out2, out2 != err2);
		new StreamCopy(in, process.getOutputStream(), true);
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			process.destroy();
			stdout.stop();
			stderr.stop();
			exitCode = -1;
			return;
		}
		try {
			stdout.join();
		} catch (InterruptedException e) {
			stdout.stop();
		}
		try {
			stderr.join();
		} catch (InterruptedException e) {
			stderr.stop();
		}
	}

	public static void exec(String... args) {
		exec(null, args);
	}

	public static void exec(File workingDirectory, String... args) {
		LineHandler ijLogHandler = new LineHandler() {
			public void handleLine(String line) {
				IJ.log(line);
			}
		};
		try {
			SimpleExecuter executer = new SimpleExecuter(args, ijLogHandler, ijLogHandler, workingDirectory);
			if (executer.getExitCode() != 0)
				throw new RuntimeException("exit status: " + executer.getExitCode());
		}
		catch (IOException e) {
			throw new RuntimeException("Could not execute", e);
		}
	}

	public static void exec(File workingDirectory, OutputStream out, String... args) {
		final PrintStream print = new PrintStream(out);
		exec(workingDirectory, new LineHandler() {
			@Override
			final public void handleLine(String line) {
				print.println(line);
			}
		}, args);
		print.flush();
	}

	public static void exec(File workingDirectory, LineHandler out, String... args) {
		try {
			SimpleExecuter executer = new SimpleExecuter(args, out, out, workingDirectory);
			if (executer.getExitCode() != 0)
				throw new RuntimeException("exit status: " + executer.getExitCode());
		}
		catch (IOException e) {
			throw new RuntimeException("Could not execute", e);
		}
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

		@Override
		public void run() {
			byte[] buffer = new byte[16384];
			try {
				for (;;) {
					int count = in.read(buffer);
					if (count < 0)
						break;
					handle(buffer, 0, count);
				}
				in.close();
			} catch (IOException e) {
				stderr.out.append(e.toString());
			}
		}

		protected void handle(byte[] buffer, int offset, int length) throws IOException {
			out.append(new String(buffer, offset, length));
		}
	}

	protected class LineDumper extends StreamDumper {
		protected LineHandler handler;

		public LineDumper(LineHandler handler, InputStream in) {
			super(in);
			this.handler = handler;
		}

		@Override
		public void run() {
			super.run();
			if (out.length() > 0)
				handler.handleLine(out.toString());
		}

		protected void handle(byte[] buffer, int offset, int length) {
			for (int i = 0; i < length; i++)
				if (buffer[offset + i] == '\n') {
					out.append(new String(buffer, offset, i));
					handler.handleLine(out.toString());
					out.setLength(0);

					offset += i + 1;
					length -= i + 1;
					i = -1;
				}
			out.append(new String(buffer, offset, length));
		}
	}

	protected class StreamCopy extends StreamDumper {
		protected OutputStream out;
		protected boolean closeAfterRun;

		public StreamCopy(InputStream in, OutputStream out) {
			this(in, out, true);
		}

		public StreamCopy(InputStream in, OutputStream out, boolean closeAfterRun) {
			super(in);
			this.out = out;
			this.closeAfterRun = closeAfterRun;
		}

		@Override
		public void run() {
			if (in != null) try {
				super.run();
				out.flush();
				if (closeAfterRun)
					out.close();
			} catch (IOException e) {
				IJ.handleException(e);
			}
		}

		@Override
		public void handle(byte[] buffer, int offset, int length) throws IOException {
			out.write(buffer, offset, length);
		}
	}

	public StreamDumper getDumper(LineHandler handler, InputStream in) {
		return handler != null ? new LineDumper(handler, in) : new StreamDumper(in);
	}

	public StreamDumper getDumper(InputStream in, OutputStream out, boolean closeAfterRun) {
		return out != null ? new StreamCopy(in, out, closeAfterRun) : new StreamDumper(in);
	}

	protected String getInterpreter(String path) {
		String lower = path.toLowerCase();
		if (lower.endsWith(".exe"))
			return null;
		if (lower.endsWith(".bsh") || lower.endsWith(".bs") || lower.endsWith(".py") || lower.endsWith(".rb") || lower.endsWith(".clj"))
			return System.getProperty("ij.executable");
		// TODO: handle #! lines (needs command line splitting)
		return null;
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
