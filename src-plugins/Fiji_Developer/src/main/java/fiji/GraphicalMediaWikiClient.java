package fiji;

import ij.Prefs;

import ij.gui.GenericDialog;

import java.awt.TextField;

public class GraphicalMediaWikiClient extends MediaWikiClient {
	protected String login, password;

	public GraphicalMediaWikiClient() {
		super();
	}

	public GraphicalMediaWikiClient(String wikiBaseURI) {
		super(wikiBaseURI);
	}

	public boolean login() {
		return login("Wiki login");
	}

	public boolean login(String title) {
		if (login != null && password != null)
			logIn(login, password);
		while (!isLoggedIn()) {
			GenericDialog gd = new GenericDialog(title);
			if (login == null)
				login = Prefs.get("fiji.wiki.user", "");
			gd.addStringField("Login", login, 20);
			gd.addStringField("Password", "", 20);
			((TextField)gd.getStringFields().lastElement())
				.setEchoChar('*');
			gd.showDialog();
			if (gd.wasCanceled())
				return false;

			login = gd.getNextString();
			Prefs.set("fiji.wiki.user", login);
			password = gd.getNextString();
			logIn(login, password);
		}
		return true;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Need 1 arg: the wiki URL");
			System.exit(1);
		}

		GraphicalMediaWikiClient client =
			new GraphicalMediaWikiClient(args[0]);
		System.err.println("login: " + client.login("Test"));
	}
}
