package fiji.updater;

/**
 * Access the command-line interface of the ImageJ Updater
 *
 * @author Johannes Schindelin
 * @deprecated use ij-updater-core directly
 */
public class Main {
	public static void main(String[] args) {
		new Adapter(false).runCommandLineUpdater(args);
	}
}
