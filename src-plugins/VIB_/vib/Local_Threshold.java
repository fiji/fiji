package vib;

import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.filter.PlugInFilter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;
import ij.gui.PointRoi;
import ij.gui.ShapeRoi;
import ij.gui.Roi;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.Blitter;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

public class Local_Threshold implements PlugInFilter {

	private ImagePlus image;
	private static ImageProcessor copy;
	private static int lastMinThreshold = 10;
	private static int lastMaxThreshold = 255;

	public void run(final ImageProcessor ip) {
		if(image.getRoi() == null) {
			IJ.error("Selection required");
			return;
		}
		Roi roiCopy = (Roi)image.getRoi().clone();
		copy = ip.duplicate();
		final GenericDialog gd = 
				new GenericDialog("Adjust local threshold");
		gd.addSlider("min value", 0, 255, lastMinThreshold);
		gd.addSlider("max value", 0, 255, lastMaxThreshold);

		final Scrollbar minSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar maxSlider = (Scrollbar)gd.getSliders().get(1);

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				applyThreshold(ip, image.getRoi(), 
						minSlider.getValue(),
						maxSlider.getValue());
				lastMinThreshold = minSlider.getValue();
				lastMaxThreshold = maxSlider.getValue();
				image.updateAndDraw();
			}
		};
		minSlider.addAdjustmentListener(listener);
		maxSlider.addAdjustmentListener(listener);

		applyThreshold(ip, image.getRoi(), 
				lastMinThreshold, lastMaxThreshold);
		image.updateAndDraw();
		gd.showDialog();

		// Convert area to selection
		ip.setRoi(image.getRoi());
		ImageProcessor newip = ip.crop();
		newip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		ImagePlus tmp = new ImagePlus("", newip);
		ThresholdToSelection ts = new ThresholdToSelection();
		ts.setup("", tmp);
		ts.run(newip);
		newip.resetThreshold();
		ip.insert(copy, 0, 0);
		Rectangle roiCopyR = roiCopy.getBounds();
		if(tmp.getRoi() != null) {
			Rectangle roiTempR = tmp.getRoi().getBounds();
			int xl = roiCopyR.x > 0 ? roiCopyR.x : 0;
			if(roiTempR.x > 0) xl += roiTempR.x;
			int yl = roiCopyR.y > 0 ? roiCopyR.y : 0;
			if(roiTempR.y > 0) yl += roiTempR.y;
			tmp.getRoi().setLocation(xl, yl);
			image.setRoi(tmp.getRoi());
		}
	}

	public static void applyThreshold(ImageProcessor ip, 
						Roi roi, int min, int max) {
		if(roi == null) {
			IJ.error("Selection required");
			return;
		}
		boolean mustCleanUp = copy == null;
		if(copy == null) {
			 copy = ip.duplicate();
		}

		byte[] p = (byte[])ip.getPixels();
		byte[] c = (byte[])copy.getPixels();

		int w = ip.getWidth(), h = ip.getHeight();

		Rectangle bounds = roi.getBoundingRect();
		int x1 = bounds.x > 0 ? bounds.x : 0;
		int y1 = bounds.y > 0 ? bounds.y : 0;
		int x2 = x1 + bounds.width <= w ? x1 + bounds.width : w;
		int y2 = y1 + bounds.height <= h ? y1 + bounds.height : h;


		for(int y = y1; y < y2; y++) {
			for(int x = x1; x < x2; x++) {
				if(!roi.contains(x, y))
					continue;
				int index = y*ip.getWidth() + x;
				if(((int)c[index]&0xff) >= min &&
						((int)c[index]&0xff) <= max) {
					p[index] = (byte)255;
				} else {
					p[index] = c[index];
				}
			}
		}
		if(mustCleanUp) copy = null;
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_8G;
	}
}
