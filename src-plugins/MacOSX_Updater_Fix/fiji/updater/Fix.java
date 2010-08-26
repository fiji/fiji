package fiji.updater;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import javax.swing.filechooser.FileFilter;

public class Fix {
	protected static File validate(String appDirectory) {
		File result = new File(appDirectory, "Contents/MacOS");
		return result.isDirectory() ? result : null;
	}

	protected static File getFijiPath() {
		File result = null;
		try {
			BinaryPList dock = BinaryPList.readDock();
			result = validate(dock.getPersistentAppURL("Fiji"));
		} catch (IOException e) {
			/* ignore */
		}

		if (result == null)
			result = validate(System.getenv("HOME") + "/Desktop/Fiji.app");
		if (result == null)
			result = validate(System.getenv("HOME") + "/Applications/Fiji.app");
		if (result == null)
			result = validate("/Applications/Fiji.app");
		if (result == null) {
			String[] list = new File("/Applications/").list();
			for (String item : list != null ? list : new String[0])
				if ((result = validate("/Applications/" + item)) != null)
					break;
		}
		while (result == null) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Choose Fiji.app directory");
			chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);
			chooser.setDialogType(chooser.OPEN_DIALOG);
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) {
					return f.getName().endsWith(".app");
				}

				public String getDescription() {
					return "Applications";
				}
			});
			if (chooser.showOpenDialog(null) != chooser.APPROVE_OPTION)
				return null;
			result = validate(chooser.getSelectedFile().getAbsolutePath());
		}

		return result;
	}

	protected static boolean quietExec(File workingDirectory, String... cmdarray) {
		try {
			Process process = Runtime.getRuntime().exec(cmdarray, null, workingDirectory);
			process.getOutputStream().close();
			InputStreamBuffer err = new InputStreamBuffer(process.getErrorStream());
			err.start();
			InputStreamBuffer out = new InputStreamBuffer(process.getInputStream());
			out.start();
			for (;;) try {
				int exitCode = process.waitFor();
				if (exitCode == 0)
					return true;
				try {
					err.join();
				} catch (InterruptedException e) { /* ignore */ }
				try {
					out.join();
				} catch (InterruptedException e) { /* ignore */ }
				JOptionPane.showMessageDialog(null,
					"Failed command (" + exitCode + "): '" + join(cmdarray, "' '") + "'\n"
					+ "Error output: " + err + "\n"
					+ "Other output: " + out,
					"Error", JOptionPane.ERROR_MESSAGE);
				break;
			} catch (InterruptedException e) { /* ignore */ }
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected static class InputStreamBuffer extends Thread {
		StringBuffer buffer = new StringBuffer();
		InputStream in;

		protected InputStreamBuffer(InputStream in) {
			this.in = in;
		}

		public void run() {
			byte[] buf = new byte[16384];
			try {
				for (;;) {
					int count = in.read(buf);
					if (count < 0)
						break;
					buffer.append(new String(buf, 0, count));
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace(new PrintWriter(new Writer() {
					public void close() {}
					public void flush() {}
					public void write(char[] buf, int off, int len) {
						buffer.append(new String(buf, off, len));
					}
				}));
			}
		}

		public String toString() {
			return buffer.toString();
		}
	}

	protected static String join(String[] array, String delimiter) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			if (i > 0)
				result.append(delimiter);
			result.append(array[i]);
		}
		return result.toString();
	}

	protected static boolean makeExecutable(File workingDirectory, String fileName1, String fileName2) {
		return quietExec(workingDirectory, "chmod", "a+x", fileName1, fileName2);
	}

	public static void main(String[] args) {
		File fijiPath = getFijiPath();
		if (fijiPath == null) {
			JOptionPane.showMessageDialog(null, "Could not find Fiji.app", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		File appDirectory = fijiPath;
		if (appDirectory.getName().equals("MacOS"))
			appDirectory = appDirectory.getParentFile();
		if (appDirectory.getName().equals("Contents"))
			appDirectory = appDirectory.getParentFile();
		if (makeExecutable(fijiPath, "fiji-tiger", "fiji-macosx"))
			JOptionPane.showMessageDialog(null, "Fixed " + appDirectory, "Success", JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(null, "Could not make " + appDirectory + " executable", "Error", JOptionPane.INFORMATION_MESSAGE);
	}
}
