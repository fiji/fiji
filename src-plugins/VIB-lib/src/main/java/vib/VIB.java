/*
 * convenience class
 */

package vib;

import ij.IJ;

import ij.macro.Interpreter;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VIB {
	static TextArea area;

	// log file
	static String logFile;
	static DateFormat dateFormat;
	static String hostName;

	public static boolean alwaysShowWindow = true;
	public static void showStatus(String message) {
		if (IJ.getInstance() == null)
			println(message);
		else
			IJ.showStatus(message);
	}

	public static void showProgress(int step, int count) {
		if (IJ.getInstance() == null)
			;
		else
			IJ.showProgress(step, count);
	}

	public static void println(String message) {
		if (message.startsWith("logfile:")) {
			logFile = message.substring(8).trim();
			dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			try {
				InetAddress l = InetAddress.getLocalHost();
				// work around "localhost"
				l = InetAddress.getByName(l.getHostAddress());
				hostName = l.getHostName();
			} catch (Exception e) {
				hostName = "<unnamed>";
			}
			return;
		}

		if (alwaysShowWindow || Interpreter.isBatchMode()) {
			if (area == null) {
				area = new TextArea(25, 80);
				area.setText(message);
				area.setEditable(false);
				area.setVisible(true);
				final Frame frame = new Frame("Log");
				frame.addWindowListener(new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						area = null;
						frame.dispose();
					}
				});
				frame.add(area);
				frame.setSize(new Dimension(400, 300));
				frame.doLayout();
				frame.setVisible(true);
				area.setCaretPosition(Integer.MAX_VALUE);
			} else
				area.append(message);
		} else
			IJ.showStatus(message);

		if (logFile != null) {
			try {
				FileWriter out = new FileWriter(logFile, true);
				message = dateFormat.format(new Date())
					+ " (" + hostName + "): " + message;
				out.write(message);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}

