package fiji;

import java.awt.Image;

import sc.fiji.compat.FijiTools;

/**
 * Main entry point into Fiji.
 * 
 * @author Johannes Schindelin
 * @deprecated Use {@link net.imagej.Main} instead.
 */
@Deprecated
public class Main {
	protected Image icon;
	protected boolean debug;

	static {
		new IJ1Patcher().run();
	}

	public static void installRecentCommands() {
		FijiTools.runPlugInGently("fiji.util.Recent_Commands", "install");
	}
}
