package fiji.plugin.trackmate;

import ij.IJ;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * This class is used to log messages occurring during plugin execution. 
 */
public abstract class Logger extends PrintWriter {
	
	public Logger() {
		// Call super with a dummy writer
		super(new Writer() {
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {}			
			@Override
			public void flush() throws IOException {}			
			@Override
			public void close() throws IOException {}
		});
		// Replace by a useful writer
		this.out = new Writer() {
			@Override
			public void close() throws IOException {}
			@Override
			public void flush() throws IOException {}
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				String str = ""; 
				for (int i = off; i < len; i++)
					str += cbuf[i];
				log(str);
			}			
		};
	}

	public static final Color NORMAL_COLOR = Color.BLACK;
	public static final Color ERROR_COLOR = new Color(0.8f, 0, 0);
	public static final Color GREEN_COLOR = new Color(0, 0.6f, 0);
	public static final Color BLUE_COLOR = new Color(0, 0, 0.7f);
	
	/**
	 * Append the message to the logger, with the specified color.
	 */
	public abstract void log(String message, Color color);
	
	/**
	 * Send the message to the error channel of this logger.
	 */
	public abstract void error(String message);
	
	/**
	 * Append the message to the logger with default black color.
	 */
	public void log(String message)  { log(message, NORMAL_COLOR);	}
	
	/**
	 * Set the progress value of the process logged by this logger. 
	 * Values should be between 0 and 1, 1 meaning the process if finished.
	 */
	public abstract void setProgress(double val);
	
	/**
	 * Set the status to be displayed by this logger.
	 */
	public abstract void setStatus(String status);
	
	
	/**
	 * This logger discard any message.
	 */
	public static final Logger VOID_LOGGER = new Logger() {
		
		@Override
		public void setStatus(String status) { }
		@Override
		public void setProgress(double val) { }
		@Override
		public void log(String message, Color color) { }		
		@Override
		public void error(String message) { }
	};
	
	/**
	 * This {@link Logger} simply outputs to the standard output and standard error.
	 * The {@link #setProgress(float)} method is ignored, the {@link #setStatus(String)} is 
	 * sent to the console.
	 */
	public static Logger DEFAULT_LOGGER = new Logger() {

		@Override
		public void log(String message, Color color) {
			System.out.print(message);
		}
		@Override
		public void error(String message) {
			System.err.print(message);
		}
		@Override
		public void setProgress(double val) {}
		
		@Override
		public void setStatus(String status) {
			System.out.println(status);
		}
	};
	
	/**
	 * This {@link Logger} outputs to the ImageJ log window, and to the ImageJ toolbar
	 * to report progress. Colors are ignored.
	 */
	public static Logger IJ_LOGGER = new Logger() {

		@Override
		public void log(String message, Color color) {
			IJ.log(message);
		}

		@Override
		public void error(String message) {
			IJ.log(message);
		}

		@Override
		public void setProgress(double val) {
			IJ.showProgress(val);
		}

		@Override
		public void setStatus(String status) {
			IJ.showStatus(status);
		}
		
	};
	
	/**
	 * This {@link Logger} outputs to a StringBuilder given at construction.
	 * Report progress and colors are ignored.
	 */
	public static class StringBuilderLogger extends Logger {
		
		private final StringBuilder sb;

		public StringBuilderLogger(final StringBuilder sb) {
			this.sb = sb;
		}

		public StringBuilderLogger() {
			this(new StringBuilder());
		}
		
		/*
		 * METHODS
		 */
		
		@Override
		public void log(String message, Color color) {
			sb.append(message);
		}

		@Override
		public void error(String message) {
			sb.append(message);
		}

		@Override
		public void setProgress(double val) { }

		@Override
		public void setStatus(String status) {
			sb.append(status);
		}
		
		@Override
		public String toString() {
			return sb.toString();
		}
		
	};
	
}