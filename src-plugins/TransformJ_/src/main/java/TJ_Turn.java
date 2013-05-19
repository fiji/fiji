import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Image;
import imagescience.transform.Turn;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Turn implements PlugIn, WindowListener {
	
	private static final String[] angles = {"0","90","180","270"};
	
	private static int zindex = 0;
	private static int yindex = 0;
	private static int xindex = 0;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Turn");
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Turn");
		gd.addChoice("z-angle (degrees):",angles,angles[zindex]);
		gd.addChoice("y-angle (degrees):",angles,angles[yindex]);
		gd.addChoice("x-angle (degrees):",angles,angles[xindex]);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		zindex = gd.getNextChoiceIndex();
		yindex = gd.getNextChoiceIndex();
		xindex = gd.getNextChoiceIndex();
		
		(new TJTurn()).run(imp,zindex,yindex,xindex);
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

class TJTurn {
	
	void run(
		final ImagePlus imp,
		final int zindex,
		final int yindex,
		final int xindex
	) {
		
		try {
			final Image img = Image.wrap(imp);
			final Turn turner = new Turn();
			turner.messenger.log(TJ_Options.log);
			turner.messenger.status(TJ_Options.pgs);
			turner.progressor.display(TJ_Options.pgs);
			final Image newimg = turner.run(img,zindex,yindex,xindex);
			TJ.show(newimg,imp);
			
		} catch (OutOfMemoryError e) {
			TJ.error("Not enough memory for this operation");
			
		} catch (UnknownError e) {
			TJ.error("Could not create output image for some reason.\nPossibly there is not enough free memory");
			
		} catch (Throwable e) {
			TJ.error("An unidentified error occurred while running the plugin");
			
		}
	}
	
}
