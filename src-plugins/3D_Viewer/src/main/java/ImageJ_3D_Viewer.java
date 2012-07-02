import ij.IJ;
import ij.plugin.PlugIn;
import ij3d.ImageJ3DViewer;

public class ImageJ_3D_Viewer implements PlugIn {

	public static void main(String[] args) {
		if(checkJava3D())
			new ImageJ3DViewer().run("");
	}

	public void run(String args) {
		if(checkJava3D())
			new ImageJ3DViewer().run(args);
	}

	/**
	 * Returns true if the viewer can run.
	 */
	public static boolean checkJava3D() {
		String version = ij3d.Install_J3D.getJava3DVersion();
		System.out.println("version = " + version);
		if(version != null && Float.parseFloat(version) >= 1.5)
			return true;

		boolean inst;
		if(version != null) {
			inst = IJ.showMessageWithCancel("Outdated Java 3D version",
				"Java 3D version " + version + " detected,\n" +
				"but version >= 1.5 is required.\n" +
				"Auto-install new version?");
		} else {
			inst = IJ.showMessageWithCancel(
				"Java 3D seems not to be installed\n",
				"Java 3D seems not to be installed\n" +
				"Auto-install?");
		}

		if(inst) {
			if(ij3d.Install_J3D.autoInstall())
				IJ.showMessage("Please restart ImageJ now");
		}
		return false;
	}
}

