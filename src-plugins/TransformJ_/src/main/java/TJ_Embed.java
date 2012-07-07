import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.transform.Embed;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Embed implements PlugIn, WindowListener {
	
	private static int newxdim = 1024;
	private static int newydim = 1024;
	private static int newzdim = 1;
	private static int newtdim = 1;
	private static int newcdim = 1;
	
	private static int xpos = 0;
	private static int ypos = 0;
	private static int zpos = 1;
	private static int tpos = 1;
	private static int cpos = 1;
	
	private static final String[] fillings = { "zero", "minimum", "maximum", "repeat", "mirror", "clamp" };
	private static int filling = 0;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Embed");
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Embed");
		gd.addNumericField("x-size of new image:",newxdim,0);
		gd.addNumericField("y-size of new image:",newydim,0);
		gd.addNumericField("z-size of new image:",newzdim,0);
		gd.addNumericField("t-size of new image:",newtdim,0);
		gd.addNumericField("c-size of new image:",newcdim,0);
		gd.addPanel(new Panel(),GridBagConstraints.WEST,new Insets(6,0,0,0));
		gd.addNumericField("x-position of input image:",xpos,0);
		gd.addNumericField("y-position of input image:",ypos,0);
		gd.addNumericField("z-position of input image:",zpos,0);
		gd.addNumericField("t-position of input image:",tpos,0);
		gd.addNumericField("c-position of input image:",cpos,0);
		gd.addPanel(new Panel(),GridBagConstraints.WEST,new Insets(0,0,0,0));
		gd.addChoice("Background filling:",fillings,fillings[filling]);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		newxdim = (int)gd.getNextNumber();
		newydim = (int)gd.getNextNumber();
		newzdim = (int)gd.getNextNumber();
		newtdim = (int)gd.getNextNumber();
		newcdim = (int)gd.getNextNumber();
		xpos = (int)gd.getNextNumber();
		ypos = (int)gd.getNextNumber();
		zpos = (int)gd.getNextNumber();
		tpos = (int)gd.getNextNumber();
		cpos = (int)gd.getNextNumber();
		filling = gd.getNextChoiceIndex();
		
		(new TJEmbed()).run(imp,newxdim,newydim,newzdim,newtdim,newcdim,xpos,ypos,zpos,tpos,cpos,filling);
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

class TJEmbed {
	
	void run(
		final ImagePlus imp,
		final int newxdim,
		final int newydim,
		final int newzdim,
		final int newtdim,
		final int newcdim,
		final int xpos,
		final int ypos,
		final int zpos,
		final int tpos,
		final int cpos,
		final int filling
	) {
		
		try {
			final Image img = Image.wrap(imp);
			final Embed embedder = new Embed();
			embedder.messenger.log(TJ_Options.log);
			embedder.messenger.status(TJ_Options.pgs);
			embedder.progressor.display(TJ_Options.pgs);
			int filltype = Embed.ZERO;
			switch (filling) {
				case 0: filltype = Embed.ZERO; break;
				case 1: filltype = Embed.MINIMUM; break;
				case 2: filltype = Embed.MAXIMUM; break;
				case 3: filltype = Embed.REPEAT; break;
				case 4: filltype = Embed.MIRROR; break;
				case 5: filltype = Embed.CLAMP; break;
			}
			if (newxdim < 1) throw new IllegalArgumentException("Zero or negative x-size for new image");
			if (newydim < 1) throw new IllegalArgumentException("Zero or negative y-size for new image");
			if (newzdim < 1) throw new IllegalArgumentException("Zero or negative z-size for new image");
			if (newtdim < 1) throw new IllegalArgumentException("Zero or negative t-size for new image");
			if (newcdim < 1) throw new IllegalArgumentException("Zero or negative c-size for new image");
			final Dimensions newdims = new Dimensions(newxdim,newydim,newzdim,newtdim,newcdim);
			final Coordinates inpos = new Coordinates(xpos,ypos,zpos-1,tpos-1,cpos-1);
			final Image newimg = embedder.run(img,newdims,inpos,filltype);
			TJ.show(newimg,imp,mapchan(img.dimensions(),newdims,inpos,filling));
			
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
	
	private int[][] mapchan(final Dimensions indims, final Dimensions newdims, final Coordinates inpos, final int filling) {
		
		final int[][] idx = new int[2][];
		
		switch (filling) {
			case 0: case 1: case 2:
				idx[0] = new int[indims.c]; idx[1] = new int[indims.c];
				for (int i=0; i<indims.c; ++i) { idx[0][i] = i + 1; idx[1][i] = inpos.c + i + 1; }
				break;
			case 3:
				idx[0] = new int[newdims.c]; idx[1] = new int[newdims.c];
				for (int i=0; i<indims.c; ++i) idx[0][inpos.c+i] = i + 1;
				for (int i=0; i<newdims.c; ++i) idx[1][i] = i + 1;
				for (int i=inpos.c-1, i0=inpos.c+indims.c-1; i>=0; --i, --i0) idx[0][i] = idx[0][i0];
				for (int i=inpos.c+indims.c, i0=inpos.c; i<newdims.c; ++i, ++i0) idx[0][i] = idx[0][i0];
				break;
			case 4:
				idx[0] = new int[newdims.c]; idx[1] = new int[newdims.c];
				for (int i=0; i<indims.c; ++i) idx[0][inpos.c+i] = i + 1;
				for (int i=0; i<newdims.c; ++i) idx[1][i] = i + 1;
				int ifs = 2; int indimssm1 = indims.c - 1;
				if (indims.c == 1) { ++indimssm1; ifs = 1; }
				for (int i=inpos.c-1; i>=0; --i) {
				final int idiff = i - inpos.c;
				int i0 = idiff / indimssm1; i0 += i0 % ifs;
				idx[0][i] = idx[0][inpos.c + Math.abs(idiff - i0*indimssm1)]; }
				for (int i=inpos.c+indims.c; i<newdims.c; ++i) {
					final int idiff = i - inpos.c;
					int i0 = idiff / indimssm1; i0 += i0 % ifs;
					idx[0][i] = idx[0][inpos.c + Math.abs(idiff - i0*indimssm1)];
				}
				break;
			case 5:
				idx[0] = new int[newdims.c]; idx[1] = new int[newdims.c];
				for (int i=0; i<indims.c; ++i) idx[0][inpos.c+i] = i + 1;
				for (int i=0; i<newdims.c; ++i) idx[1][i] = i + 1;
				final int b = idx[0][inpos.c];
				final int e = idx[0][inpos.c + indims.c - 1];
				for (int i=inpos.c-1; i>=0; --i) idx[0][i] = b;
				for (int i=inpos.c+indims.c; i<newdims.c; ++i) idx[0][i] = e;
				break;
		}
		
		return idx;
	}
	
}
