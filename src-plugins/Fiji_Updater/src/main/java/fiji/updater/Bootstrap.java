package fiji.updater;

import java.io.File;

/**
 * Bootstrap the ImageJ Updater
 *
 * @author Johannes Schindelin
 */
public class Bootstrap {
	public static void main(String[] args) {
		System.setProperty("ij.dir", new File(".").getAbsolutePath());
		String[] newArgs = new String[args.length + 1];
		newArgs[0] = "update-force-pristine";
		System.arraycopy(args, 0, newArgs, 1, args.length);
		Main.main(newArgs);
	}
}