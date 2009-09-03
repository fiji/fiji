package fiji.pluginManager.ui;

import ij.IJ;
import ij.Prefs;

import ij.gui.GenericDialog;

import java.awt.Component;
import java.awt.TextField;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import fiji.pluginManager.logic.PluginManager;
import fiji.pluginManager.logic.UpdateSource;
import fiji.pluginManager.logic.Updater;

import fiji.pluginManager.logic.FileUploader.SourceFile;
import fiji.pluginManager.logic.FileUploader.UploadListener;

/*
 * The "interface" for uploading plugins (Consists of IJ progress bar & IJ GenericDialog).
 */
public class Uploader implements UploadListener, Runnable {
	private volatile MainUserInterface mainUserInterface;
	private volatile PluginManager pluginManager;
	private volatile Updater updater;
	private Thread uploadThread;

	public Uploader(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
	}

	//Ask for login details and then began upload process
	public synchronized void setUploadInformationAndStart(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		uploadThread = new Thread(this);
		uploadThread.start();
	}

	public synchronized void update(SourceFile source, long bytesSoFar, long bytesTotal) {
		if (source != null)
			IJ.showStatus("Uploading "
					+ source.getFilename() + "...");
		IJ.showProgress((int)bytesSoFar, (int)bytesTotal);
	}

	public synchronized void uploadFileComplete(SourceFile source) {
	}

	public synchronized void uploadProcessComplete() {
	}

	public void run() {
		String error_message = null;
		//boolean loginSuccess = false;
		try {
			String username = "";
			String password = "";
			updater = new Updater(pluginManager);
			do {
				//Dialog to enter username and password
				GenericDialog gd = new GenericDialog("Login");
				String user = Prefs.get(PluginManager.PREFS_USER, "");
				gd.addStringField("Username", user, 20);
				gd.addStringField("Password", "", 20);
				final TextField pwd = ((TextField)gd.getStringFields().lastElement());
				pwd.setEchoChar('*');
				if (!user.equals(""))
					((TextField)gd.getStringFields().firstElement())
							.addFocusListener(new FocusAdapter() {
						public void focusGained(FocusEvent e) {
							pwd.requestFocus();
							((Component)e.getSource()).removeFocusListener(this);
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
			mainUserInterface.setVisible(false);
			updater.upload(this);

		} catch (Throwable e) {
			e.printStackTrace();
			error_message = e.getLocalizedMessage();
		}

		//If there is an error message, show it
		if (error_message != null) {
			mainUserInterface.exitWithRestartMessage("Error",
					"Failed to upload changes to server: " + error_message + "\n\n" +
					"You need to restart Plugin Manager again."); //exit if failure
		} else {
			IJ.showStatus(""); //exit if successful
			mainUserInterface.exitWithRestartMessage("Updated",
					"Files successfully uploaded to server!\n\n"
					+ "You need to restart Plugin Manager for changes to take effect.");
		}
	}

}
