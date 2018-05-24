
package sc.fiji.compat;

import ij.IJ;
import ij.plugin.PlugIn;

public class Compile_and_Run implements PlugIn {

	protected static String directory, fileName;

	@Override
	public void run(String arg) {
		IJ.showMessage("The \"Compile and Run\" command is not currently supported."
			+ " We recommend using the Script Editor or an IDE such as Eclipse for "
			+ "plugin development.");
	}
}
