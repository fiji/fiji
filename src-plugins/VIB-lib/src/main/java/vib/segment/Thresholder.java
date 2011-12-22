package vib.segment;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.Roi;
import ij.gui.GenericDialog;

import ij.plugin.filter.ThresholdToSelection;

import ij.process.ImageProcessor;
import ij.process.ByteProcessor;

import java.awt.Checkbox;
import java.awt.Rectangle;
import java.awt.Scrollbar;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

import java.util.Arrays;
import java.util.Vector;

public class Thresholder {
	protected CustomStackWindow stackWindow;
	protected ImagePlus image;
	protected ImageProcessor copy;

	protected int minThreshold = 127;
	protected int maxThreshold = 255;
	protected int erodeDilateIterations = 0;
	protected boolean showBinary = false;

	public Thresholder(final CustomStackWindow stackWindow) {
		this.stackWindow = stackWindow;
		image = stackWindow.getCustomCanvas().getImage();
	}

	public void run() {
		if (stackWindow.areAllRoisEmpty()) {
			int w = image.getWidth(), h = image.getHeight();
			for (int i = 1; i <= image.getStackSize(); i++)
				stackWindow.setRoi(i, new Roi(0, 0, w, h));
		}

		initializeSlice();

		final GenericDialog gd = new GenericDialog("Adjust threshold");
		gd.addSlider("min value", 0, 255, minThreshold);
		gd.addSlider("max value", 0, 255, maxThreshold);
		gd.addSlider("erode/dilate iterations", 0, 10,
				erodeDilateIterations);
		gd.addCheckbox("show binary", false);
		gd.addSlider("slice", 1, image.getStackSize(),
				image.getCurrentSlice());
		Vector<Scrollbar> sliders = (Vector<Scrollbar>)gd.getSliders();
		final Scrollbar minSlider = sliders.get(0);
		final Scrollbar maxSlider = sliders.get(1);
		final Scrollbar iterationSlider = sliders.get(2);
		final Checkbox binary =
			((Vector<Checkbox>)gd.getCheckboxes()).get(0);
		final Scrollbar sliceSlider = sliders.get(3);

		final AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				minThreshold = minSlider.getValue();
				maxThreshold = maxSlider.getValue();
				erodeDilateIterations =
					iterationSlider.getValue();
				showBinary = binary.getState();
				apply();
				image.updateAndDraw();
			}
		};
		minSlider.addAdjustmentListener(listener);
		maxSlider.addAdjustmentListener(listener);
		iterationSlider.addAdjustmentListener(listener);
		binary.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				listener.adjustmentValueChanged(null);
			}
		});
		sliceSlider.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				restoreSlice();
				stackWindow.setCurrentSlice(e.getValue());
				initializeSlice();
			}
		});

		image.updateAndDraw();
		gd.showDialog();
		restoreSlice();
		image.updateAndDraw();
		if (gd.wasCanceled())
			return;

		minThreshold = (int)gd.getNextNumber();
		maxThreshold = (int)gd.getNextNumber();
		erodeDilateIterations = (int)gd.getNextNumber();
		int dummy = (int)gd.getNextNumber();
		select();
	}

	void select() {
		ImageProcessor ip = copy.duplicate();
		ImagePlus dummy = new ImagePlus("dummy", ip);
		byte[] p = (byte[])ip.getPixels();
		ThresholdToSelection ts = new ThresholdToSelection();
		for (int i = 1; i <= image.getStackSize(); i++) {
			Roi roi = stackWindow.getRoi(i);
			if (roi == null)
				continue;
			copy = image.getStack().getProcessor(i);
			Arrays.fill(p, (byte)0);
			apply(ip, roi, minThreshold,
				maxThreshold, erodeDilateIterations, true);
			ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
			ts.setup("", dummy);
			ts.run(ip);
			stackWindow.setRoi(i, dummy.getRoi());
		}
	}

	protected void initializeSlice() {
		copy = image.getProcessor().duplicate();
		apply();
	}

	protected void restoreSlice() {
		image.setProcessor(null, copy);
	}

	protected void apply() {
		Roi roi = image.getRoi();
		if (roi == null)
			return;
		ImageProcessor ip = image.getProcessor();
		apply(ip, roi, minThreshold, maxThreshold,
				erodeDilateIterations, showBinary);
	}

	protected void apply(ImageProcessor ip, Roi roi,
			int min, int max, int erodeDilateIterations,
			boolean makeBinary) {
		byte[] p = (byte[])ip.getPixels();
		byte[] c = (byte[])copy.getPixels();

		int w = ip.getWidth(), h = ip.getHeight();
		Rectangle bounds = roi.getBoundingRect();
		int x1 = Math.min(0, bounds.x);
		int y1 = Math.min(0, bounds.y);
		int x2 = Math.max(w, x1 + bounds.width);
		int y2 = Math.max(h, y1 + bounds.height);

		if (erodeDilateIterations > 0) {
			ByteProcessor bp = new ByteProcessor(w, h);
			byte[] p1 = (byte[])bp.getPixels();
			for (int y = y1; y < y2; y++)
				System.arraycopy(p, y * w + x1,
						p1, y * w + x1, x2 - x1);

			apply(bp, roi, min, max, 0, true);

			for (int i = 0; i < erodeDilateIterations; i++)
				bp.erode(1, 0);
			for (int i = 0; i < erodeDilateIterations; i++)
				bp.dilate(1, 0);

			for (int y = y1; y < y2; y++)
				for (int x = x1; x < x2; x++) {
					int index = y * w + x;
					if (p1[index] != 0)
						p[index] = (byte)255;
					else if (makeBinary)
						p[index] = 0;
					else
						p[index] = c[index];
				}

			return;
		}

		for(int y = y1; y < y2; y++)
			for(int x = x1; x < x2; x++) {
				int index = y * w + x;
				if(roi.contains(x, y) &&
						(c[index]&0xff) >= min &&
						(c[index]&0xff) <= max)
					p[index] = (byte)255;
				else if (makeBinary)
					p[index] = 0;
				else
					p[index] = c[index];
			}
	}
}
