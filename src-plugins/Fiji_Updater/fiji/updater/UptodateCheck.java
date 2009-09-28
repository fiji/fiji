package fiji.updater;

import fiji.updater.util.Util;

import ij.IJ;
import ij.Prefs;

import ij.macro.Interpreter;

import ij.plugin.PlugIn;

import java.io.File;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Date;
import java.util.Enumeration;

import javax.swing.JOptionPane;

public class UptodateCheck implements PlugIn {
	long localLastModified;
	final static String latestReminderKey = "fiji.updater.latestNag";
	final static long reminderInterval = 86400 * 7; // one week

	public void run(String arg) {
		if ("quick".equals(arg))
			checkOrShowDialog();
		else if ("verbose".equals(arg)) {
			String result = checkOrShowDialog();
			if (result != null)
				JOptionPane.showMessageDialog(IJ.getInstance(),
					result, "Up-to-date check",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public String checkOrShowDialog() {
		String result = check();
		if (result == null && !isBatchMode())
			showDialog();
		return result;
	}

	public String check() {
		if (shouldRemindLater())
			return "You wanted to be reminded later.";
		if (isBatchMode())
			return "No check will be performed in batch mode";
		if (!canWrite())
			return "Your Fiji is read-onyl!";
		if (!haveNetworkConnection())
			return "No network connection available!";
		localLastModified = getLocalLastModified();
		if (isDbXmlGzUpToDate(localLastModified)) {
			Prefs.set(latestReminderKey, "");
			return "Up-to-date";
		}
		return null;
	}

	public static long now() {
		return new Date().getTime() / 1000;
	}

	public boolean shouldRemindLater() {
		String latestNag = Prefs.get(latestReminderKey, null);
		if (latestNag == null || latestNag.equals(""))
			return false;
		return now() - Long.parseLong(latestNag) < reminderInterval;
	}

	public boolean isBatchMode() {
		return IJ.getInstance() == null || !IJ.getInstance().isVisible()
			|| Interpreter.isBatchMode();
	}

	public boolean canWrite() {
		String url = getClass()
			.getResource("UptodateCheck.class").toString();
		if (url.startsWith("jar:file:"))
			url = url.substring(9);
		int bang = url.indexOf('!');
		if (bang > 0)
			url = url.substring(0, bang);
		return new File(url).canWrite();
	}

	public static long getLocalLastModified() {
		File dbXmlGz = new File(Util.prefix(Updater.XML_COMPRESSED));
		return dbXmlGz.exists() ? dbXmlGz.lastModified() : 0;
	}

	public static boolean haveNetworkConnection() {
		try {
			Enumeration<NetworkInterface> ifaces =
				NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				Enumeration<InetAddress> addresses =
					ifaces.nextElement().getInetAddresses();
				while (addresses.hasMoreElements())
					if (!addresses.nextElement()
							.isLoopbackAddress())
						return true;
			}
		} catch (SocketException e) { }
		return false;
	}

	public static boolean isDbXmlGzUpToDate(long local) {
		try {
			URLConnection connection = new URL(Updater.MAIN_URL
				+ Updater.XML_COMPRESSED).openConnection();
			connection.setUseCaches(false);
			long lastModified = connection.getLastModified();
			connection.getInputStream().close();
			return lastModified == local;
		} catch (Exception e) {
			// assume no network; so let's pretend everything's ok.
			return true;
		}
	}

	public void showDialog() {
		Object[] options = {
			"Yes, please",
			"Never",
			"Remind me later"
		};
		switch (JOptionPane.showOptionDialog(IJ.getInstance(),
				localLastModified == 0 ?
				"You have not checked for updates yet.\n"
				+ "Would you like to check now?" :
				"There are updates available.\n"
				+ "Do you want to start the Fiji Updater now?",
				"Up-to-date check",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null, options, options[0])) {
		case 0:
			new Updater().run("update");
			break;
		case 1:
			Prefs.set(latestReminderKey, "" + Long.MAX_VALUE);
			break;
		case 2:
			Prefs.set(latestReminderKey, "" + now());
			break;
		case JOptionPane.CLOSED_OPTION:
			// do nothing
		}
	}
}
