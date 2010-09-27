import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Image;
import imagescience.transform.Rotate;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Rotate implements PlugIn, WindowListener {
	
	private static String zangle = "0.0";
	private static String yangle = "0.0";
	private static String xangle = "0.0";
	
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
	
	private static boolean adjust = true;
	private static boolean antialias = false;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Rotate");
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Rotate");
		gd.addStringField("z-angle (degrees):",zangle);
		gd.addStringField("y-angle (degrees):",yangle);
		gd.addStringField("x-angle (degrees):",xangle);
		gd.addPanel(new Panel(),GridBagConstraints.WEST,new Insets(0,0,0,0));
		gd.addChoice("Interpolation scheme:",schemes,schemes[scheme]);
		gd.addStringField("Background value:",bgvalue);
		gd.addCheckbox(" Adjust size to fit result",adjust);
		gd.addCheckbox(" Anti-alias borders",antialias);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		zangle = gd.getNextString();
		yangle = gd.getNextString();
		xangle = gd.getNextString();
		scheme = gd.getNextChoiceIndex();
		bgvalue = gd.getNextString();
		adjust = gd.getNextBoolean();
		antialias = gd.getNextBoolean();
		
		(new TJRotate()).run(imp,zangle,yangle,xangle,scheme,bgvalue,adjust,antialias);
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

class TJRotate {
	
	void run(
		final ImagePlus imp,
		final String zangle,
		final String yangle,
		final String xangle,
		final int scheme,
		final String bgvalue,
		final boolean adjust,
		final boolean antialias
	) {
		
		try {
			final Image img = Image.wrap(imp);
			final Rotate rotator = new Rotate();
			rotator.messenger.log(TJ_Options.log);
			rotator.messenger.status(TJ_Options.pgs);
			rotator.progressor.display(TJ_Options.pgs);
			double za, ya, xa, bg;
			try { za = Double.parseDouble(zangle); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid z-angle for rotation"); }
			try { ya = Double.parseDouble(yangle); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid y-angle for rotation"); }
			try { xa = Double.parseDouble(xangle); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid x-angle for rotation"); }
			try { bg = Double.parseDouble(bgvalue); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid background value"); }
			rotator.background = bg;
			int ischeme = Rotate.NEAREST;
			switch (scheme) {
				case 0: ischeme = Rotate.NEAREST; break;
				case 1: ischeme = Rotate.LINEAR; break;
				case 2: ischeme = Rotate.CUBIC; break;
				case 3: ischeme = Rotate.BSPLINE3; break;
				case 4: ischeme = Rotate.OMOMS3; break;
				case 5: ischeme = Rotate.BSPLINE5; break;
			}
			final Image newimg = rotator.run(img,za,ya,xa,ischeme,adjust,antialias);
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
