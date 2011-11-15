package fiji.updater.ui;

import fiji.updater.util.IJLogOutputStream;
import fiji.updater.util.UserInterface;

import ij.IJ;
import ij.Prefs;

import ij.macro.Interpreter;

import ij.plugin.BrowserLauncher;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JOptionPane;

public class IJ1UI extends UserInterface {
	@Override
	public void error(String message) {
		IJ.error(message);
	}

	@Override
	public void info (String message, String title) {
		JOptionPane.showMessageDialog(IJ.getInstance(),
			message, "Up-to-date check",
			JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void log(String message) {
		IJ.log(message);
	}

	@Override
	public void debug(String message) {
		if (IJ.debugMode)
			IJ.log(message);
	}

	@Override
	public OutputStream getOutputStream() {
		return new IJLogOutputStream();
	}

	@Override
	public void showStatus(String message) {
		IJ.showStatus(message);
	}

	@Override
	public void handleException(Throwable exception) {
		IJ.handleException(exception);
	}


	@Override
	public boolean isBatchMode() {
		return IJ.getInstance() == null || !IJ.getInstance().isVisible()
			|| Interpreter.isBatchMode();
	}


	@Override
	public int optionDialog(String message, String title, Object[] options, int def) {
		return JOptionPane.showOptionDialog(IJ.getInstance(),
			message, title,
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null, options, options[def]);
	}

	@Override
	public String getPref(String key) {
		return Prefs.get(key, null);
	}

	@Override
	public void setPref(String key, String value) {
		Prefs.set(key, value);
	}

	@Override
	public void savePreferences() {
		Prefs.savePreferences();
	}

	@Override
	public void openURL(String url) throws IOException {
		BrowserLauncher.openURL(url);
	}
}