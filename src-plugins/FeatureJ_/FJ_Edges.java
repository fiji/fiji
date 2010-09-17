import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.feature.Edges;
import imagescience.image.Aspects;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.segment.Thresholder;
import imagescience.utility.Progressor;
import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class FJ_Edges implements PlugIn, ItemListener, WindowListener {

	private static boolean compute = true;
	private static boolean suppress = false;

	private static String scale = "1.0";
	private static String lower = "";
	private static String higher = "";

	private Checkbox computebox, suppressbox;

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!FJ.libcheck()) return;
		final ImagePlus imp = FJ.imageplus();
		if (imp == null) return;

		FJ.log(FJ.name()+" "+FJ.version()+": Edges");

		GenericDialog gd = new GenericDialog(FJ.name()+": Edges");
		gd.addCheckbox(" Compute gradient-magnitude image     ",compute);
		gd.addStringField("                Smoothing scale:",scale);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		gd.addCheckbox(" Suppress non-maximum gradients     ",suppress);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		gd.addStringField("                Lower threshold value:",lower);
		gd.addStringField("                Higher threshold value:",higher);
		computebox = (Checkbox)gd.getCheckboxes().get(0); computebox.addItemListener(this);
		suppressbox = (Checkbox)gd.getCheckboxes().get(1); suppressbox.addItemListener(this);

		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();

		if (gd.wasCanceled()) return;

		compute = gd.getNextBoolean();
		scale = gd.getNextString();
		suppress = gd.getNextBoolean();
		lower = gd.getNextString();
		higher = gd.getNextString();

		(new FJEdges()).run(imp,compute,scale,suppress,lower,higher);
	}

	public void itemStateChanged(final ItemEvent e) {

		if (e.getSource() == computebox) {
			if (!computebox.getState()) suppressbox.setState(false);
		} else if (e.getSource() == suppressbox) {
			if (suppressbox.getState()) computebox.setState(true);
		}
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

class FJEdges {

	void run(
		final ImagePlus imp,
		final boolean compute,
		final String scale,
		final boolean suppress,
		final String lower,
		final String higher
	) {

		try {
			double scaleval, lowval=0, highval=0;
			boolean lowthres = true, highthres = true;
			try { scaleval = Double.parseDouble(scale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }
			try { if (lower.equals("")) lowthres = false; else lowval = Double.parseDouble(lower); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid lower threshold value"); }
			try { if (higher.equals("")) highthres = false; else highval = Double.parseDouble(higher); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid higher threshold value"); }
			final int thresmode = (lowthres ? 10 : 0) + (highthres ? 1 : 0);

			final Image img = Image.wrap(imp);
			Image newimg = new FloatImage(img);

			double[] pls = {0, 1}; int pl = 0;
			if ((compute || suppress) && thresmode > 0)
				pls = new double[] {0, 0.9, 1};
			final Progressor progressor = new Progressor();
			progressor.display(FJ_Options.pgs);

			if (compute || suppress) {
				final Aspects aspects = newimg.aspects();
				if (!FJ_Options.isotropic) newimg.aspects(new Aspects());
				final Edges edges = new Edges();
				progressor.range(pls[pl],pls[++pl]);
				edges.progressor.parent(progressor);
				edges.messenger.log(FJ_Options.log);
				edges.messenger.status(FJ_Options.pgs);
				newimg = edges.run(newimg,scaleval,suppress);
				newimg.aspects(aspects);
			}

			if (thresmode > 0) {
				final Thresholder thres = new Thresholder();
				progressor.range(pls[pl],pls[++pl]);
				thres.progressor.parent(progressor);
				thres.messenger.log(FJ_Options.log);
				thres.messenger.status(FJ_Options.pgs);
				switch (thresmode) {
					case 1: { thres.hard(newimg,highval); break; }
					case 10: { thres.hard(newimg,lowval); break; }
					case 11: { thres.hysteresis(newimg,lowval,highval); break; }
				}
			}

			FJ.show(newimg,imp);
			FJ.close(imp);

		} catch (OutOfMemoryError e) {
			FJ.error("Not enough memory for this operation");

		} catch (IllegalArgumentException e) {
			FJ.error(e.getMessage());

		} catch (IllegalStateException e) {
			FJ.error(e.getMessage());

		} catch (Throwable e) {
			FJ.error("An unidentified error occurred while running the plugin");

		}
	}

}
