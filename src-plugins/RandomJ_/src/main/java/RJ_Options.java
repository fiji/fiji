import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class RJ_Options implements PlugIn, WindowListener {

	static boolean floatout = Prefs.get("rj.floatout",true);
	static boolean adopt = Prefs.get("rj.adopt",true);
	static boolean close = Prefs.get("rj.close",false);
	static boolean save = Prefs.get("rj.save",false);
	static boolean pgs = Prefs.get("rj.pgs",true);
	static boolean log = Prefs.get("rj.log",false);

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!RJ.libcheck()) return;

		RJ.log(RJ.name()+" "+RJ.version()+": Options");

		final String space = "     ";
		GenericDialog gd = new GenericDialog(RJ.name()+": Options");
		gd.addCheckbox(" Generate floating-point result images"+space,floatout);
		gd.addCheckbox(" Adopt brightness/contrast from input images"+space,adopt);
		gd.addCheckbox(" Close input images after processing"+space,close);
		gd.addCheckbox(" Save result images before closing"+space,save);
		gd.addCheckbox(" Progress indication"+space,pgs);
		gd.addCheckbox(" Log messaging"+space,log);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));

		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();

		if (gd.wasCanceled()) return;

		floatout = gd.getNextBoolean();
		adopt = gd.getNextBoolean();
		close = gd.getNextBoolean();
		save = gd.getNextBoolean();
		pgs = gd.getNextBoolean();
		log = gd.getNextBoolean();

		Prefs.set("rj.floatout",floatout);
		Prefs.set("rj.adopt",adopt);
		Prefs.set("rj.close",close);
		Prefs.set("rj.save",save);
		Prefs.set("rj.pgs",pgs);
		Prefs.set("rj.log",log);

		if (floatout) RJ.log("Generating 32-bit floating-point images");
		else RJ.log("Generating images of the same type as the input");

		if (adopt) RJ.log("Adopting brightness/contrast from input images");
		else RJ.log("Setting brightness/contrast based on full output range");

		if (close) RJ.log("Closing input images after processing");
		else RJ.log("Keeping input images after processing");

		if (save) RJ.log("Asking to save result images before closing");
		else RJ.log("Closing result images without asking to save");

		if (pgs) RJ.log("Enabling progress indication");
		else RJ.log("Disabling progress indication");

		if (log) RJ.log("Enabling log messaging");
		else RJ.log("Disabling log messaging");
	}

	public void windowActivated(final WindowEvent e) { }

	public void windowClosed(final WindowEvent e) {

		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}

	public void windowClosing(final WindowEvent e) { }

	public void windowDeactivated(final WindowEvent e) { }

	public void windowDeiconified(final WindowEvent e) { }

	public void windowIconified(final WindowEvent e) { }

	public void windowOpened(final WindowEvent e) { }

}
