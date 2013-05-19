package fiji.updater;

/**
 * Access the command-line interface of the ImageJ Updater
 *
 * @author Johannes Schindelin
 */
public class Main {
	public static void main(String[] args) {
		new Adapter(false).runCommandLineUpdater(args);
	}
}
