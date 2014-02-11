
package fiji;

import ij.IJ;

public class MediaWikiTestDrive {
	private final static String URL = "http://my.wiki.org/wiki/index.php";
	private final static String USER = System.getProperty("wiki.user");
	private final static String PASSWORD = System.getProperty("wiki.password");

	public static void main(String[] args) {
		IJ.debugMode = true;
		final MediaWikiClient client = new MediaWikiClient(URL);
		if (!client.isLoggedIn()) {
			client.logIn(USER, PASSWORD);
		}
		//client.uploadPage("Hello_" + System.currentTimeMillis(), "Hello " + System.currentTimeMillis(), "comment");
		client.logOut();
	}
}
