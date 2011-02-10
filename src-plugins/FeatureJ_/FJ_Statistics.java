import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import imagescience.feature.Statistics;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.Formatter;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

public class FJ_Statistics implements PlugIn, ItemListener, WindowListener {

	private static final String[] labels = {
		" Minimum",
		" Maximum",
		" Mean",
		" Median",
		" Elements",
		" Mass",
		" Variance",
		" Mode",
		" S-Deviation",
		" A-Deviation",
		" L1-Norm",
		" L2-Norm",
		" Skewness",
		" Kurtosis"
	};

	private static final boolean[] values = {
		true,
		true,
		true,
		false,
		true,
		false,
		false,
		false,
		true,
		false,
		false,
		false,
		false,
		false
	};

	private static boolean clear = false;
	private static boolean name = true;
	private static boolean channel = false;
	private static boolean time = false;
	private static boolean slice = false;

	private static int decimals = 3;

	private Checkbox channelbox, timebox, slicebox;

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!FJ.libcheck()) return;
		final ImagePlus imp = FJ.imageplus();
		if (imp == null) return;

		FJ.log(FJ.name()+" "+FJ.version()+": Statistics");

		GenericDialog gd = new GenericDialog(FJ.name()+": Statistics");
		gd.addCheckboxGroup(7,2,labels,values);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(5,0,0,0));
		gd.addCheckbox(" Clear previous results",clear);
		gd.addCheckbox(" Image name displaying",name);
		gd.addCheckbox(" Channel numbering",channel);
		gd.addCheckbox(" Time frame numbering",time);
		gd.addCheckbox(" Slice numbering",slice);
		final Vector checkboxes = gd.getCheckboxes();
		final int veclen = checkboxes.size();
		slicebox = (Checkbox)checkboxes.get(veclen-1); slicebox.addItemListener(this);
		timebox = (Checkbox)checkboxes.get(veclen-2); timebox.addItemListener(this);
		channelbox = (Checkbox)checkboxes.get(veclen-3); channelbox.addItemListener(this);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		final String[] decslist = new String[11];
		for (int i=0; i<11; ++i) decslist[i] = String.valueOf(i);
		gd.addChoice("        Decimal places:",decslist,String.valueOf(decimals));

		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();

		if (gd.wasCanceled()) return;

		for (int i=0; i<values.length; ++i) values[i] = gd.getNextBoolean();
		clear = gd.getNextBoolean();
		name = gd.getNextBoolean();
		channel = gd.getNextBoolean();
		time = gd.getNextBoolean();
		slice = gd.getNextBoolean();
		decimals = gd.getNextChoiceIndex();

		(new FJStatistics()).run(imp,values,clear,name,channel,time,slice,decimals);
	}

	public void itemStateChanged(final ItemEvent e) {

		if (e.getSource() == slicebox) {
			if (slicebox.getState()) {
				timebox.setState(true);
				channelbox.setState(true);
			}
		} else if (e.getSource() == timebox) {
			if (timebox.getState()) {
				channelbox.setState(true);
			} else {
				slicebox.setState(false);
			}
		} else if (e.getSource() == channelbox) {
			if (!channelbox.getState()) {
				timebox.setState(false);
				slicebox.setState(false);
			}
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

class FJStatistics {

	private final Statistics stats = new Statistics();
	private final Formatter fmt = new Formatter();

	private boolean[] values = null;

	private boolean clear = false;
	private boolean name = true;
	private boolean channel = false;
	private boolean time = false;
	private boolean slice = false;

	private static int number = 0;

	private int decimals = 3;

	void run(
		final ImagePlus imp,
		final boolean[] values,
		final boolean clear,
		final boolean name,
		final boolean channel,
		final boolean time,
		final boolean slice,
		final int decimals
	) {

		this.values = new boolean[values.length];
		for (int i=0; i<values.length; ++i) this.values[i] = values[i];
		this.clear = clear;
		this.name = name;
		this.channel = channel;
		this.time = time;
		this.slice = slice;
		this.decimals = decimals;

		try {
			// Initialize:
			if (decimals < 0 || decimals > 10) {
				throw new IllegalArgumentException("Invalid number of decimals");
			} else {
				fmt.decs(decimals);
				fmt.chop(0);
			}

			// Determine region of interest:
			final Image img = Image.wrap(imp);
			final Dimensions dims = img.dimensions();
			final Coordinates cmin = new Coordinates();
			final Coordinates cmax = new Coordinates();
			Roi roi = imp.getRoi();
			if (roi == null) roi = new Roi(0,0,dims.x,dims.y);
			ImageProcessor maskip = null;
			switch (roi.getType()) {
				case Roi.COMPOSITE:
				case Roi.FREEROI:
				case Roi.OVAL:
				case Roi.POINT:
				case Roi.POLYGON:
				case Roi.TRACED_ROI:
					maskip = roi.getMask();
					break;
				case Roi.RECTANGLE:
					maskip = new ByteProcessor(1,1);
					maskip.set(0,0,255);
					break;
			}
			if (maskip == null) throw new IllegalArgumentException("Region of interest not supported");
			final ImagePlus maskimp = new ImagePlus("Mask",maskip); // maskimp.show();
			final Image mask = Image.wrap(maskimp);
			final Rectangle bounds = roi.getBounds();
			cmin.x = bounds.x;
			cmin.y = bounds.y;
			cmax.x = bounds.x + bounds.width - 1;
			cmax.y = bounds.y + bounds.height - 1;

			// Compute and show statistics:
			final String nameprelude = name ? ("\t"+imp.getTitle()) : "";
			final TextPanel textpanel = IJ.getTextPanel();
			final String headings = headings();
			if (clear || !headings.equals(textpanel.getColumnHeadings())) {
				textpanel.setColumnHeadings(headings);
				number = 0;
			}

			final Progressor pgs = new Progressor();
			final Messenger msg = new Messenger();
			pgs.display(FJ_Options.pgs);
			msg.status(FJ_Options.pgs);

			if (slice) {
				msg.status("Computing statistics per slice / time / channel...");
				pgs.steps(dims.c*dims.t*dims.z);
				pgs.start();
				for (cmin.c=cmax.c=0; cmin.c<dims.c; ++cmin.c, ++cmax.c) {
					for (cmin.t=cmax.t=0; cmin.t<dims.t; ++cmin.t, ++cmax.t) {
						for (cmin.z=cmax.z=0; cmin.z<dims.z; ++cmin.z, ++cmax.z) {
							stats.run(img,cmin,cmax,mask);
							final String prelude = (++number) + nameprelude + "\t" + (cmin.c+1) + "\t" + (cmin.t+1) + "\t" + (cmin.z+1);
							textpanel.append(prelude + results());
							pgs.step();
						}
					}
				}
				pgs.stop();
				msg.status("");

			} else if (time) {
				msg.status("Computing statistics per time / channel...");
				pgs.steps(dims.c*dims.t);
				pgs.start();
				cmax.z = dims.z - 1;
				for (cmin.c=cmax.c=0; cmin.c<dims.c; ++cmin.c, ++cmax.c) {
					for (cmin.t=cmax.t=0; cmin.t<dims.t; ++cmin.t, ++cmax.t) {
						stats.run(img,cmin,cmax,mask);
						final String prelude = (++number) + nameprelude + "\t" + (cmin.c+1) + "\t" + (cmin.t+1);
						textpanel.append(prelude + results());
						pgs.step();
					}
				}
				pgs.stop();
				msg.status("");

			} else if (channel) {
				msg.status("Computing statistics per channel...");
				pgs.steps(dims.c);
				pgs.start();
				cmax.z = dims.z - 1;
				cmax.t = dims.t - 1;
				for (cmin.c=cmax.c=0; cmin.c<dims.c; ++cmin.c, ++cmax.c) {
					stats.run(img,cmin,cmax,mask);
					final String prelude = (++number) + nameprelude + "\t" + (cmin.c+1);
					textpanel.append(prelude + results());
					pgs.step();
				}
				pgs.stop();
				msg.status("");

			} else {
				cmax.z = dims.z - 1;
				cmax.t = dims.t - 1;
				cmax.c = dims.c - 1;
				stats.messenger.status(FJ_Options.pgs);
				stats.progressor.display(FJ_Options.pgs);
				stats.run(img,cmin,cmax,mask);
				final String prelude = (++number) + nameprelude;
				textpanel.append(prelude + results());
			}

		} catch (OutOfMemoryError e) {
			FJ.error("Not enough memory for this operation");

		} catch (IllegalArgumentException e) {
			FJ.error(e.getMessage());

		} catch (Throwable e) {
			FJ.error("An unidentified error occurred while running the plugin");

		}
	}

	private String headings() {

		final StringBuffer cols = new StringBuffer();

		cols.append("Nr");
		if (name) cols.append("\tImage");
		if (slice) cols.append("\tChan\tTime\tSlice");
		else if (time) cols.append("\tChan\tTime");
		else if (channel) cols.append("\tChan");
		if (values[4]) cols.append("\tElements");
		if (values[0]) cols.append("\tMin");
		if (values[1]) cols.append("\tMax");
		if (values[2]) cols.append("\tMean");
		if (values[6]) cols.append("\tVar");
		if (values[8]) cols.append("\tS-Dev");
		if (values[9]) cols.append("\tA-Dev");
		if (values[3]) cols.append("\tMedian");
		if (values[7]) cols.append("\tMode");
		if (values[5]) cols.append("\tMass");
		if (values[10]) cols.append("\tL1");
		if (values[11]) cols.append("\tL2");
		if (values[12]) cols.append("\tSkew");
		if (values[13]) cols.append("\tKurt");

		return cols.toString();
	}

	private String results() {

		final StringBuffer res = new StringBuffer();

		if (values[4]) res.append("\t"+fmt.d2s(stats.get(stats.ELEMENTS)));
		if (values[0]) res.append("\t"+fmt.d2s(stats.get(stats.MINIMUM)));
		if (values[1]) res.append("\t"+fmt.d2s(stats.get(stats.MAXIMUM)));
		if (values[2]) res.append("\t"+fmt.d2s(stats.get(stats.MEAN)));
		if (values[6]) res.append("\t"+fmt.d2s(stats.get(stats.VARIANCE)));
		if (values[8]) res.append("\t"+fmt.d2s(stats.get(stats.SDEVIATION)));
		if (values[9]) res.append("\t"+fmt.d2s(stats.get(stats.ADEVIATION)));
		if (values[3]) res.append("\t"+fmt.d2s(stats.get(stats.MEDIAN)));
		if (values[7]) res.append("\t"+fmt.d2s(stats.get(stats.MODE)));
		if (values[5]) res.append("\t"+fmt.d2s(stats.get(stats.MASS)));
		if (values[10]) res.append("\t"+fmt.d2s(stats.get(stats.L1NORM)));
		if (values[11]) res.append("\t"+fmt.d2s(stats.get(stats.L2NORM)));
		if (values[12]) res.append("\t"+fmt.d2s(stats.get(stats.SKEWNESS)));
		if (values[13]) res.append("\t"+fmt.d2s(stats.get(stats.KURTOSIS)));

		return res.toString();
	}

}
