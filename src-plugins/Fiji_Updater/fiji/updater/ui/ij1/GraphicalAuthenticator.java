package fiji.updater.ui.ij1;

import ij.Macro;

import ij.gui.GenericDialog;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class GraphicalAuthenticator extends Authenticator {
	protected PasswordAuthentication getPasswordAuthentication() {
		GenericDialog gd = new GenericDialog("Proxy Authentication");
		gd.addStringField("User", "");
		gd.setEchoChar('*');
		gd.addStringField("Password", "");
		gd.showDialog();
		if (gd.wasCanceled())
			throw new RuntimeException(Macro.MACRO_CANCELED);
		String user = gd.getNextString();
		String password = gd.getNextString();
		return new PasswordAuthentication(user, password.toCharArray());
	}
}