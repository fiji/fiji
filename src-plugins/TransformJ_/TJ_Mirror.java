import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Axes;
import imagescience.image.Image;
import imagescience.transform.Mirror;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Mirror implements PlugIn, WindowListener {
	
	private static boolean x = false;
	private static boolean y = false;
	private static boolean z = false;
	private static boolean t = false;
	private static boolean c = false;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Mirror");
		
		boolean dox = true; if (imp.getWidth() == 1) dox = false;
		boolean doy = true; if (imp.getHeight() == 1) doy = false;
		boolean doz = true; if (imp.getNSlices() == 1) doz = false;
		boolean dot = true; if (imp.getNFrames() == 1) dot = false;
		boolean doc = true; if (imp.getNChannels() == 1) doc = false;
		
		final String space = "     ";
		GenericDialog gd = new GenericDialog(TJ.name()+": Mirror");
		if (dox) gd.addCheckbox(" x-mirror input image"+space,x);
		if (doy) gd.addCheckbox(" y-mirror input image"+space,y);
		if (doz) gd.addCheckbox(" z-mirror input image"+space,z);
		if (dot) gd.addCheckbox(" t-mirror input image"+space,t);
		if (doc) gd.addCheckbox(" c-mirror input image"+space,c);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		x = dox ? gd.getNextBoolean() : false;
		y = doy ? gd.getNextBoolean() : false;
		z = doz ? gd.getNextBoolean() : false;
		t = dot ? gd.getNextBoolean() : false;
		c = doc ? gd.getNextBoolean() : false;
		
		(new TJMirror()).run(imp,x,y,z,t,c);
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

class TJMirror {
	
	void run(
		final ImagePlus imp,
		final boolean x,
		final boolean y,
		final boolean z,
		final boolean t,
		final boolean c
	) {
		
		try {
			final Image img = Image.wrap(imp);
			final Image newimg = img.duplicate();
			final Mirror mrr = new Mirror();
			mrr.messenger.log(TJ_Options.log);
			mrr.messenger.status(TJ_Options.pgs);
			mrr.progressor.display(TJ_Options.pgs);
			mrr.run(newimg,new Axes(x,y,z,t,c));
			TJ.show(newimg,imp,mapchan(c,imp.getNChannels()));
			
		} catch (OutOfMemoryError e) {
			TJ.error("Not enough memory for this operation");
			
		} catch (Throwable e) {
			TJ.error("An unidentified error occurred while running the plugin");
			
		}
	}
	
	private int[][] mapchan(final boolean c, final int nc) {
		
		final int[][] idx = new int[2][nc];
		if (c) for (int i=0; i<nc; ++i) {
			idx[0][i] = i + 1;
			idx[1][i] = nc - i;
		} else for (int i=0; i<nc; ++i) {
			idx[0][i] = idx[1][i] = i + 1;
		}
		return idx;
	}
	
}
