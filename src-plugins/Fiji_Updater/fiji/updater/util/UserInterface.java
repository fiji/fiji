package fiji.updater.util;

import java.awt.Frame;

import java.io.IOException;
import java.io.OutputStream;

public abstract class UserInterface {
	// The methods
	public abstract void error(String message);
	public abstract void info(String message, String title);
	public abstract void log(String message);
	public abstract void debug(String message);
	public abstract OutputStream getOutputStream();
	public abstract void showStatus(String message);
	public abstract void handleException(Throwable exception);
	public abstract boolean isBatchMode();
	public abstract int optionDialog(String message, String title, Object[] options, int def);
	public abstract String getPref(String key);
	public abstract void setPref(String key, String value);
	public abstract void savePreferences();
	public abstract void openURL(String url) throws IOException;
	public abstract String getString(String title);
	public abstract String getPassword(String title);
	public abstract void addWindow(Frame window);
	public abstract void removeWindow(Frame window);

	// The singleton
	protected static UserInterface ui = new StderrInterface();

	public static void set(UserInterface ui) {
		UserInterface.ui = ui;
	}

	public final static UserInterface get() {
		return ui;
	}

	// The default implementation
	protected static class StderrInterface extends UserInterface {
		private boolean debug = false;

		@Override
		public void error(String message) {
			System.err.println(message);
		}

		@Override
		public void info(String message, String title) {
			System.err.println(title + ": " + message);
		}

		@Override
		public void log(String message) {
			System.err.println(message);
		}

		@Override
		public void debug(String message) {
			if (debug)
				System.err.println(message);
		}

		@Override
		public OutputStream getOutputStream() {
			return System.err;
		}

		@Override
		public void showStatus(String message) {
			System.err.println(message);
		}

		@Override
		public void handleException(Throwable exception) {
			exception.printStackTrace();
		}

		@Override
		public boolean isBatchMode() {
			return true;
		}

		@Override
		public int optionDialog(String message, String title, Object[] options, int def) {
			throw new RuntimeException("TODO");
		}

		@Override
		public String getPref(String key) {
			return null;
		}

		@Override
		public void setPref(String key, String value) {
			/* ignore */
		}

		@Override
		public void savePreferences() {
			throw new RuntimeException("TODO");
		}

		@Override
		public void openURL(String url) {
			System.err.println("Open URL " + url);
		}

		@Override
		public String getString(String title) {
			System.err.print(title + " ");
			return new String(System.console().readLine());
		}

		@Override
		public String getPassword(String title) {
			System.err.print(title + " ");
			return new String(System.console().readPassword());
		}

		@Override
		public void addWindow(Frame window) {
			// do nothing
		}

		@Override
		public void removeWindow(Frame window) {
			// do nothing
		}
	}
}