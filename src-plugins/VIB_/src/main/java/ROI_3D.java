import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class ROI_3D implements PlugIn {
	public void run(String arg) {
		ImagePlus image = IJ.getImage();
		ImageCanvas canvas = image.getCanvas();
		image.setWindow(new StackWindowWith3dRoi(image, canvas));
	}

	static class StackWindowWith3dRoi extends StackWindow {
		StackWindowWith3dRoi(ImagePlus image, ImageCanvas canvas) {
			super(image, canvas);
			int i = image.getCurrentSlice();
			sliceSelector.addAdjustmentListener(new Listener(i));
		}

		class Listener implements AdjustmentListener {
			Roi[] rois;
			int oldSlice;

			Listener(int currentSlice) {
				rois = new Roi[imp.getStack().getSize() + 1];
				oldSlice = currentSlice;
			}

			public void adjustmentValueChanged(AdjustmentEvent e) {
				rois[oldSlice] = imp.getRoi();
				oldSlice = e.getValue();
				if (rois[oldSlice] == null)
					imp.killRoi();
				else
					imp.setRoi(rois[oldSlice]);
			}
		}
	}
}

