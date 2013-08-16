import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import imagescience.image.Coordinates;
import imagescience.image.Image;
import imagescience.transform.Crop;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Crop implements PlugIn, WindowListener {
	
	private static String xrange = "0,0";
	private static String yrange = "0,0";
	private static String zrange = "1,1";
	private static String trange = "1,1";
	private static String crange = "1,1";
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Crop");
		
		final Roi roi = imp.getRoi();
		if (roi != null) {
			final Rectangle rect = roi.getBounds();
			xrange = rect.x + "," + (rect.x + rect.width - 1);
			yrange = rect.y + "," + (rect.y + rect.height - 1);
			zrange = "1," + imp.getNSlices();
			trange = "1," + imp.getNFrames();
			crange = "1," + imp.getNChannels();
		}
		
		boolean dox = true; if (imp.getWidth() == 1) dox = false;
		boolean doy = true; if (imp.getHeight() == 1) doy = false;
		boolean doz = true; if (imp.getNSlices() == 1) doz = false;
		boolean dot = true; if (imp.getNFrames() == 1) dot = false;
		boolean doc = true; if (imp.getNChannels() == 1) doc = false;
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Crop");
		if (dox) gd.addStringField("x-range for cropping:",xrange,10);
		if (doy) gd.addStringField("y-range for cropping:",yrange,10);
		if (doz) gd.addStringField("z-range for cropping:",zrange,10);
		if (dot) gd.addStringField("t-range for cropping:",trange,10);
		if (doc) gd.addStringField("c-range for cropping:",crange,10);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		xrange = dox ? gd.getNextString() : "0,0";
		yrange = doy ? gd.getNextString() : "0,0";
		zrange = doz ? gd.getNextString() : "1,1";
		trange = dot ? gd.getNextString() : "1,1";
		crange = doc ? gd.getNextString() : "1,1";
		
		(new TJCrop()).run(imp,xrange,yrange,zrange,trange,crange);
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

class TJCrop {
	
	void run(
		final ImagePlus imp,
		final String xrange,
		final String yrange,
		final String zrange,
		final String trange,
		final String crange
	) {
		
		try {
			int startx, starty, startz, startt, startc;
			int stopx, stopy, stopz, stopt, stopc;
			try {
				startx = Integer.parseInt(xrange.substring(0,xrange.indexOf(',')));
				stopx = Integer.parseInt(xrange.substring(xrange.indexOf(',')+1));
			} catch (Exception e) { throw new IllegalArgumentException("Invalid x-range for cropping"); }
			try {
				starty = Integer.parseInt(yrange.substring(0,yrange.indexOf(',')));
				stopy = Integer.parseInt(yrange.substring(yrange.indexOf(',')+1));
			} catch (Exception e) { throw new IllegalArgumentException("Invalid y-range for cropping"); }
			try {
				startz = Integer.parseInt(zrange.substring(0,zrange.indexOf(','))) - 1;
				stopz = Integer.parseInt(zrange.substring(zrange.indexOf(',')+1)) - 1;
			} catch (Exception e) { throw new IllegalArgumentException("Invalid z-range for cropping"); }
			try {
				startt = Integer.parseInt(trange.substring(0,trange.indexOf(','))) - 1;
				stopt = Integer.parseInt(trange.substring(trange.indexOf(',')+1)) - 1;
			} catch (Exception e) { throw new IllegalArgumentException("Invalid t-range for cropping"); }
			try {
				startc = Integer.parseInt(crange.substring(0,crange.indexOf(','))) - 1;
				stopc = Integer.parseInt(crange.substring(crange.indexOf(',')+1)) - 1;
			} catch (Exception e) { throw new IllegalArgumentException("Invalid c-range for cropping"); }
			
			final Image img = Image.wrap(imp);
			final Crop cropper = new Crop();
			cropper.messenger.log(TJ_Options.log);
			cropper.messenger.status(TJ_Options.pgs);
			cropper.progressor.display(TJ_Options.pgs);
			final Coordinates startpos = new Coordinates(startx,starty,startz,startt,startc);
			final Coordinates stoppos = new Coordinates(stopx,stopy,stopz,stopt,stopc);
			final Image newimg = cropper.run(img,startpos,stoppos);
			TJ.show(newimg,imp,mapchan(startc,stopc));
			
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
	
	private int[][] mapchan(final int start, final int stop) {
		
		final int len = stop - start + 1;
		final int[][] idx = new int[2][len];
		for (int i=0; i<len; ++i) {
			idx[0][i] = start + i + 1;
			idx[1][i] = i + 1;
		}
		return idx;
	}
	
}
