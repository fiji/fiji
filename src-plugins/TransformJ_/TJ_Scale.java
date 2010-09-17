import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Image;
import imagescience.transform.Scale;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Scale implements PlugIn, WindowListener {
	
	private static String xfactor = "1.0";
	private static String yfactor = "1.0";
	private static String zfactor = "1.0";
	
	private static final String[] schemes = {
		"nearest neighbor",
		"linear",
		"cubic convolution",
		"cubic B-spline",
		"cubic O-MOMS",
		"quintic B-spline"
	};
	private static int scheme = 1;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Scale");
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Scale");
		gd.addStringField("x-factor for scaling:",xfactor);
		gd.addStringField("y-factor for scaling:",yfactor);
		gd.addStringField("z-factor for scaling:",zfactor);
		gd.addPanel(new Panel(),GridBagConstraints.WEST,new Insets(0,0,0,0));
		gd.addChoice("Interpolation scheme:",schemes,schemes[scheme]);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		xfactor = gd.getNextString();
		yfactor = gd.getNextString();
		zfactor = gd.getNextString();
		scheme = gd.getNextChoiceIndex();
		
		(new TJScale()).run(imp,xfactor,yfactor,zfactor,scheme);
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

class TJScale {
	
	void run(
		final ImagePlus imp,
		final String xfactor,
		final String yfactor,
		final String zfactor,
		final int scheme
	) {
		
		try {
			final Image img = Image.wrap(imp);
			final Scale scaler = new Scale();
			scaler.messenger.log(TJ_Options.log);
			scaler.messenger.status(TJ_Options.pgs);
			scaler.progressor.display(TJ_Options.pgs);
			double xf=1, yf=1, zf=1, tf=1, cf=1;
			try { xf = Double.parseDouble(xfactor); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid x-factor for scaling"); }
			try { yf = Double.parseDouble(yfactor); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid y-factor for scaling"); }
			try { zf = Double.parseDouble(zfactor); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid z-factor for scaling"); }
			int ischeme = Scale.NEAREST;
			switch (scheme) {
				case 0: ischeme = Scale.NEAREST; break;
				case 1: ischeme = Scale.LINEAR; break;
				case 2: ischeme = Scale.CUBIC; break;
				case 3: ischeme = Scale.BSPLINE3; break;
				case 4: ischeme = Scale.OMOMS3; break;
				case 5: ischeme = Scale.BSPLINE5; break;
			}
			final Image newimg = scaler.run(img,xf,yf,zf,tf,cf,ischeme);
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
