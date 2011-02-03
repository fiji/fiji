import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class FJ_Options implements PlugIn, WindowListener {

	static boolean isotropic = Prefs.get("fj.isotropic",false);
	static boolean close = Prefs.get("fj.close",false);
	static boolean save = Prefs.get("fj.save",false);
	static boolean pgs = Prefs.get("fj.pgs",true);
	static boolean log = Prefs.get("fj.log",false);

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!FJ.libcheck()) return;

		FJ.log(FJ.name()+" "+FJ.version()+": Options");

		final String space = "     ";
		GenericDialog gd = new GenericDialog(FJ.name()+": Options");
		gd.addCheckbox(" Isotropic Gaussian image smoothing"+space,isotropic);
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

		isotropic = gd.getNextBoolean();
		close = gd.getNextBoolean();
		save = gd.getNextBoolean();
		pgs = gd.getNextBoolean();
		log = gd.getNextBoolean();

		Prefs.set("fj.isotropic",isotropic);
		Prefs.set("fj.close",close);
		Prefs.set("fj.save",save);
		Prefs.set("fj.pgs",pgs);
		Prefs.set("fj.log",log);

		if (isotropic) FJ.log("Dividing smoothing scales by aspect-ratio values");
		else FJ.log("Assuming pixel units for smoothing scales");

		if (close) FJ.log("Closing input images after processing");
		else FJ.log("Keeping input images after processing");

		if (save) FJ.log("Asking to save result images before closing");
		else FJ.log("Closing result images without asking to save");

		if (pgs) FJ.log("Enabling progress indication");
		else FJ.log("Disabling progress indication");

		if (log) FJ.log("Enabling log messaging");
		else FJ.log("Disabling log messaging");
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
