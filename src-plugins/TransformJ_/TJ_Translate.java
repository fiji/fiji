import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Image;
import imagescience.transform.Translate;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Translate implements PlugIn, WindowListener {
	
	private static String xtrans = "0.0";
	private static String ytrans = "0.0";
	private static String ztrans = "0.0";
	
	private static final String[] schemes = {
		"nearest neighbor",
		"linear",
		"cubic convolution",
		"cubic B-spline",
		"cubic O-MOMS",
		"quintic B-spline"
	};
	private static int scheme = 1;
	
	private static String bgvalue = "0.0";
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Translate");
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Translate");
		gd.addStringField("x-translation (pixels):",xtrans);
		gd.addStringField("y-translation (pixels):",ytrans);
		gd.addStringField("z-translation (slices):",ztrans);
		gd.addPanel(new Panel(),GridBagConstraints.WEST,new Insets(0,0,0,0));
		gd.addChoice("Interpolation scheme:",schemes,schemes[scheme]);
		gd.addStringField("Background value:",bgvalue);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		xtrans = gd.getNextString();
		ytrans = gd.getNextString();
		ztrans = gd.getNextString();
		scheme = gd.getNextChoiceIndex();
		bgvalue = gd.getNextString();
		
		(new TJTranslate()).run(imp,xtrans,ytrans,ztrans,scheme,bgvalue);
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

class TJTranslate {
	
	void run(
		final ImagePlus imp,
		final String xtrans,
		final String ytrans,
		final String ztrans,
		final int scheme,
		final String bgvalue
	) {
		
		try {
			final Image img = Image.wrap(imp);
			final Translate translator = new Translate();
			translator.messenger.log(TJ_Options.log);
			translator.messenger.status(TJ_Options.pgs);
			translator.progressor.display(TJ_Options.pgs);
			double xs, ys, zs, bg;
			try { xs = Double.parseDouble(xtrans); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid x-translation value"); }
			try { ys = Double.parseDouble(ytrans); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid y-translation value"); }
			try { zs = Double.parseDouble(ztrans); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid z-translation value"); }
			try { bg = Double.parseDouble(bgvalue); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid background value"); }
			translator.background = bg;
			int ischeme = Translate.NEAREST;
			switch (scheme) {
				case 0: ischeme = Translate.NEAREST; break;
				case 1: ischeme = Translate.LINEAR; break;
				case 2: ischeme = Translate.CUBIC; break;
				case 3: ischeme = Translate.BSPLINE3; break;
				case 4: ischeme = Translate.OMOMS3; break;
				case 5: ischeme = Translate.BSPLINE5; break;
			}
			final Image newimg = translator.run(img,xs,ys,zs,ischeme);
			TJ.show(newimg,imp);
			
		} catch (OutOfMemoryError e) {
			TJ.error("Not enough memory for this operation");
			
		} catch (UnknownError e) {
			TJ.error("Could not create output image for some reason.\nPossibly there is not enough free memory");
			
		} catch (IllegalArgumentException e) {
			TJ.error(e.getMessage());
			
		} catch (Throwable e) {
			TJ.error("An unidentified error occurred while running the plugin");
			
		}
	}
	
}
