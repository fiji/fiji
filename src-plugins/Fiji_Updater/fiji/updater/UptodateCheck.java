package fiji.updater;

import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginCollection.UpdateSite;

import fiji.updater.util.Util;

import ij.IJ;
import ij.Prefs;

import ij.macro.Interpreter;

import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileNotFoundException;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Date;
import java.util.Enumeration;

import javax.swing.JOptionPane;

public class UptodateCheck implements PlugIn {
	final static String latestReminderKey = "fiji.updater.latestNag";
	final static long reminderInterval = 86400 * 7; // one week

	public void run(String arg) {
		Util.useSystemProxies();
		if ("quick".equals(arg)) {
			// "quick" is used on startup; don't produce an error in the Debian packaged version
			if (Updater.isDebian())
				return;
			checkOrShowDialog();
		} else {
			if (Updater.errorIfDebian())
				return;
			if ("verbose".equals(arg)) {
				String result = checkOrShowDialog();
				if (result != null)
					JOptionPane.showMessageDialog(IJ.getInstance(),
						result, "Up-to-date check",
						JOptionPane.INFORMATION_MESSAGE);
			}
			else if ("config".equals(arg) && !isBatchMode())
				config();
		}
	}

	public String checkOrShowDialog() {
		String result = check();
		if (result == null && !isBatchMode())
			showDialog();
		return result;
	}

	public String check() {
		if (neverRemind())
			return "You wanted never to be reminded.";
		if (shouldRemindLater())
			return "You wanted to be reminded later.";
		if (isBatchMode())
			return "No check will be performed in batch mode";
		if (!canWrite())
			return "Your Fiji is read-only!";
		if (!haveNetworkConnection())
			return "No network connection available!";
		PluginCollection plugins = new PluginCollection();
		try {
			try {
				plugins.read();
			}
			catch (FileNotFoundException e) { /* ignore */ }
			for (String name : plugins.getUpdateSiteNames()) {
				UpdateSite updateSite = plugins.getUpdateSite(name);
				long lastModified = getLastModified(updateSite.url + Updater.XML_COMPRESSED);
				if (lastModified < 0)
					continue; // assume network is down
				if (!updateSite.isLastModified(lastModified))
					return null;
			}
		}
		catch (FileNotFoundException e) { /* ignore */ }
		catch (Exception e) {
			IJ.handleException(e);
			return null;
		}
		setLatestNag(-1);
		return "Up-to-date";
	}

	public static long now() {
		return new Date().getTime() / 1000;
	}

	public boolean neverRemind() {
		String latestNag = Prefs.get(latestReminderKey, null);
		if (latestNag == null || latestNag.equals(""))
			return false;
		long time = Long.parseLong(latestNag);
		return time == Long.MAX_VALUE;
	}

	public boolean shouldRemindLater() {
		String latestNag = Prefs.get(latestReminderKey, null);
		if (latestNag == null || latestNag.equals(""))
			return false;
		return now() - Long.parseLong(latestNag) < reminderInterval;
	}

	public boolean isBatchMode() {
		if (Updater.hidden)
			return false;
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

	public static long getLastModified(String url) {
		try {
			URLConnection connection = new URL(url).openConnection();
			if (connection instanceof HttpURLConnection)
				((HttpURLConnection)connection)
					.setRequestMethod("HEAD");
			connection.setUseCaches(false);
			long lastModified = connection.getLastModified();
			connection.getInputStream().close();
			return lastModified;
		} catch (Exception e) {
			// assume no network; so let's pretend everything's ok.
			return -1;
		}
	}

	public void showDialog() {
		Object[] options = {
			"Yes, please",
			"Never",
			"Remind me later"
		};
		switch (JOptionPane.showOptionDialog(IJ.getInstance(),
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
			setLatestNag(Long.MAX_VALUE);
			break;
		case 2:
			setLatestNag(now());
			break;
		case JOptionPane.CLOSED_OPTION:
			// do nothing
		}
	}

	public static void config() {
		if (!"true".equals(System.getProperty("fiji.main"
				+ ".checksUpdaterAtStartup"))) {
			JOptionPane.showMessageDialog(IJ.getInstance(),
				"You need to update misc/Fiji_.jar first!",
				"Fiji error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String latestNag = Prefs.get(latestReminderKey, null);
		boolean enabled = latestNag == null || latestNag.equals("") ?
			true : (Long.parseLong(latestNag) != Long.MAX_VALUE);
		Object[] options = {
			"Check at startup",
			"Do not check at startup"
		};
		switch (JOptionPane.showOptionDialog(IJ.getInstance(),
				"Fiji Updater startup options:",
				"Configure Fiji Updater",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null, options, options[enabled ? 0 : 1])) {
		case 0:
			setLatestNag(-1);
			break;
		case 1:
			setLatestNag(Long.MAX_VALUE);
			break;
		}
	}

	public static void setLatestNag(long ticks) {
		Prefs.set(latestReminderKey, ticks < 0 ? "" : ("" + ticks));
		Prefs.savePreferences();
	}
}
