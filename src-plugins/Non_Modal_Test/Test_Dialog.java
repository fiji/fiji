import ij.IJ;
import ij.plugin.PlugIn;

public class Test_Dialog implements PlugIn {
	public void run(String arg) {
		NonblockingGenericDialog gd =
			new NonblockingGenericDialog("Hello, Ignacio!");
		gd.addCheckbox("A toggle", false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.write("was canceled");
			return;
		}

		boolean toggle = gd.getNextBoolean();
		IJ.write("Toggle is " + (toggle ? "" : "not ") + "on!");
	}
}
