package fiji.util;

import ij.IJ;
import ij.plugin.PlugIn;

import imagej.util.LineOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This class installs a redirection for {@link System.err} and {@link
 * System.out} to show the information in ImageJ 1.x' Log window, too.
 * 
 * @author Johannes Schindelin
 */
public class RedirectErrAndOut implements PlugIn {
	private OutputStream ijLog;

	/**
	 * Installs the redirection (unless one was installed already).
	 */
	public void run(final String arg) {
		if (isRedirected(System.err)) {
			IJ.log("System.err is already redirected");
		} else {
			System.setErr(redirect(System.err));
		}

		if (isRedirected(System.out)) {
			IJ.log("System.out is already redirected");
		} else {
			System.setOut(redirect(System.out));
		}
	}

	/**
	 * Detects whether a PrintStream is already redirected.
	 */
	private boolean isRedirected(final PrintStream stream) {
		if (stream == null) {
			return false;
		}
		final String name = stream.getClass().getName();
		return name.startsWith("fiji.") || name.startsWith("ij.");
	}

	/**
	 * Redirects a {@link PrintStream} to ImageJ 1.x' Log window.
	 */
	private PrintStream redirect(final PrintStream stream) {
		return new PrintStream(new ShadowOutputStream(stream));
	}

	/**
	 * Makes ImageJ 1.x' Log window available as an output stream.
	 * 
	 * We need to make sure that we do not call {@link IJ#log()} if there
	 * is no instance of {@link ij.ImageJ} since ImageJ will fall back to
	 * stderr otherwise, causing an infinite loop.
	 */
	private boolean checkIJLog() {
		if (ijLog != null) {
			return IJ.debugMode;
		}
		if (IJ.getInstance() == null) {
			return false;
		}
		ijLog = new IJLogOutputStream();
		return IJ.debugMode;
	}

	/**
	 * A simple class wrapping ImageJ 1.x' Log window in an {@link
	 * OutputStream}.
	 */
	private class ShadowOutputStream extends OutputStream {
		private final OutputStream originalStream;

		public ShadowOutputStream(final OutputStream stream) {
			originalStream = stream;
		}

		@Override
		public void close() throws IOException {
			originalStream.close();
		}

		@Override
		public void flush() throws IOException {
			originalStream.flush();
		}

		@Override
		public void write(byte[] b) throws IOException {
			originalStream.write(b);
			if (checkIJLog()) {
				ijLog.write(b);
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			originalStream.write(b, off, len);
			if (checkIJLog()) {
				ijLog.write(b, off, len);
			}
		}

		@Override
		public void write(int b) throws IOException {
			originalStream.write(b);
			if (checkIJLog()) {
				ijLog.write(b);
			}
		}
	}

	private static class IJLogOutputStream extends LineOutputStream {
		@Override
		public void println(String line) {
			IJ.log(line);
		}
	}
}
