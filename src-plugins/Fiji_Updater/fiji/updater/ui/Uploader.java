package fiji.updater.ui;

import fiji.updater.util.Progress;

import ij.IJ;
import ij.Prefs;

import ij.gui.GenericDialog;

import java.awt.Component;
import java.awt.TextField;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import fiji.updater.logic.PluginManager;
import fiji.updater.logic.UpdateSource;
import fiji.updater.logic.Updater;

import fiji.updater.logic.FileUploader.SourceFile;

/*
 * The "interface" for uploading plugins (Consists of IJ progress bar & IJ
 * GenericDialog).
 */
// TODO: "Updater" should be named "Uploader", and this class should be partly
// merged into MainUserInterface (which should be renamed to "Main"), and
// partly refactored into "class SSHLogin".
public class Uploader {
	private volatile MainUserInterface mainUserInterface;

	public Uploader(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
	}

	protected void interactiveSshLogin(Updater updater) {
		String username = Prefs.get(PluginManager.PREFS_USER, "");
		String password = "";
		do {
			//Dialog to enter username and password
			GenericDialog gd = new GenericDialog("Login");
			gd.addStringField("Username", username, 20);
			gd.addStringField("Password", "", 20);

			final TextField user =
				(TextField)gd.getStringFields().firstElement();
			final TextField pwd =
				(TextField)gd.getStringFields().lastElement();
			pwd.setEchoChar('*');
			if (!username.equals(""))
				user.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent e) {
						pwd.requestFocus();
						user.removeFocusListener(this);
					}
				});

			gd.showDialog();
			if (gd.wasCanceled()) {
				mainUserInterface.backToPluginManager();
				return; //return back to user interface
			}

			//Get the required login information
			username = gd.getNextString();
			password = gd.getNextString();

		} while (!updater.setLogin(username, password));

		Prefs.set(PluginManager.PREFS_USER, username);
	}

	//Ask for login details and then began upload process
	public void start(long xmlLastModified, Progress progress) {
		Updater updater = new Updater(xmlLastModified);

		String error = null;
		//boolean loginSuccess = false;
		try {
			interactiveSshLogin(updater);
			mainUserInterface.setVisible(false);
			updater.upload(progress);

		} catch (Throwable e) {
			e.printStackTrace();
			error = e.getLocalizedMessage();
		}

		//If there is an error message, show it
		if (error != null) {
			mainUserInterface.exitWithRestartMessage("Error",
					"Failed to upload changes to server: " + error + "\n\n" +
					"You need to restart Plugin Manager again.");
		} else {
			IJ.showStatus("");
			IJ.showProgress(1, 1);
			mainUserInterface.exitWithRestartMessage("Updated",
					"Files successfully uploaded to server!\n\n"
					+ "You need to restart Plugin Manager for changes to take effect.");
		}
	}
}
