package fiji.tool;

import fiji.tool.AbstractTool;
import fiji.tool.SliceListener;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

import ij.io.OpenDialog;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.io.SaveDialog;

import ij.plugin.frame.RoiManager;

import ij.process.ImageProcessor;

import java.awt.Cursor;
import java.awt.List;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class AbstractTrackingTool extends AbstractTool implements MouseListener, MouseMotionListener, SliceListener {
	protected Map<ImagePlus, Roi[]> map = new HashMap<ImagePlus, Roi[]>();
	protected Map<ImagePlus, Integer> latestCurrentSlice = new HashMap<ImagePlus, Integer>();
	protected Cursor originalCursor = IJ.getInstance().getCursor();
	protected Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	protected int activeHandle;

	@Override
	public String getToolIcon() {
		return "C00aT0509TT4509rT7509aTb509kT0e09TT3e09oT8e09oTde09l";
	}

	@Override
	public void mousePressed(MouseEvent e) {
		ImagePlus image = getImagePlus(e);
		activeHandle = getHandle(image.getRoi(), e.getX(), e.getY());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (activeHandle >= 0) {
			ImagePlus image = getImagePlus(e);
			// make sure that the ROI has a correct bounding box
			Roi roi = image.getRoi();
			if (roi != null)
				setRoi(WindowManager.getCurrentImage(), (Roi)roi.clone());
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {
		ImageWindow window = getImageWindow(e);
		if (window == null)
			return;

		ImagePlus image = getImagePlus(e);
		if (image == null)
			return;
		ImageCanvas canvas = window.getCanvas();
		if (getHandle(image.getRoi(), e.getX(), e.getY()) < 0)
			canvas.setCursor(originalCursor);
		else
			canvas.setCursor(handCursor);
		window.mouseMoved(getOffscreenX(e), getOffscreenY(e));
		e.consume();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (activeHandle >= 0) {
			ImagePlus image = getImagePlus(e);
			Roi roi = image.getRoi();
			if (roi != null) {
				if (roi instanceof PolygonRoi) {
					PolygonRoi polygonROI = (PolygonRoi)roi;
					Rectangle bounds = polygonROI.getBounds();
					polygonROI.getXCoordinates()[activeHandle] = getOffscreenX(e) - bounds.x;
					polygonROI.getYCoordinates()[activeHandle] = getOffscreenY(e) - bounds.y;
					setRoi(image, polygonROI);
				}
			}
		}
		e.consume();
	}

	public abstract Roi optimizeRoi(Roi roi, ImageProcessor ip);

	@Override
	public void sliceChanged(ImagePlus image) {
		// image.setRoi(Cell_Finder.makeShapeRoi(Cell_Finder.getRois(image.getProcessor(), 20, 7, 6)));
		Roi[] rois = getRois(image);

		int previousCurrentSlice = latestCurrentSlice.get(image).intValue();
		int currentSlice = image.getCurrentSlice();
		latestCurrentSlice.put(image, new Integer(currentSlice));

		rois[previousCurrentSlice - 1] = image.getRoi();
		if (rois[previousCurrentSlice - 1] != null && rois[currentSlice - 1] == null) {
			int step = currentSlice > previousCurrentSlice ? 1 : -1;
			for (int i = previousCurrentSlice; i != currentSlice + step; i += step)
				if (rois[i - 1] == null)
					rois[i - 1] = optimizeRoi(rois[i - 1 - step], image.getProcessor());
		}
		setRoi(image, rois[currentSlice - 1]);
	}

	protected int getHandle(Roi roi, int x, int y) {
		if (roi == null)
			return -1;
		return roi.isHandle(x, y);
	}

	protected Roi[] getRois(ImagePlus image) {
		Roi[] result = map.get(image);
		if (result != null)
			return result; // TODO: grow/shrink as necessary
		int currentSlice = image.getCurrentSlice();
		latestCurrentSlice.put(image, new Integer(currentSlice));
		result = new Roi[image.getStackSize()];
		result[currentSlice - 1] = image.getRoi();
		map.put(image, result);
		return result;
	}

	protected static void setRoi(ImagePlus image, Roi roi) {
		if (image == null)
			return;
		if (roi == null)
			image.killRoi();
		else
			image.setRoi(roi);
	}

	protected void exportToROIManager() {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image != null)
			exportToROIManager(image);
	}

	protected void exportToROIManager(ImagePlus image) {
		Roi[] rois = map.get(image);
		if (rois == null)
			return;
		int currentSlice = image.getCurrentSlice();
		RoiManager manager = RoiManager.getInstance();
		if (manager == null)
			manager = new RoiManager();
		for (int i = 0; i < rois.length; i++)
			if (rois[i] != null) {
				image.setSliceWithoutUpdate(i + 1);
				manager.add(image, rois[i], i + 1);
			}
		image.setSlice(currentSlice);
	}

	protected void importFromROIManager() {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null)
			return;

		Roi[] rois = getRois(image);
		RoiManager manager = RoiManager.getInstance();
		if (manager == null)
			return;
		List labels = manager.getList();
		@SuppressWarnings("unchecked")
		Hashtable<String, Roi> table = (Hashtable<String, Roi>)manager.getROIs();
		for (int i = 0; i < labels.getItemCount(); i++) {
			String label = labels.getItem(i);
			int index = manager.getSliceNumber(label) - 1;
			if (index >= 0 && index < rois.length)
				rois[index] = table.get(label);
		}
		setRoi(image, rois[image.getCurrentSlice() - 1]);
	}

	protected void saveROIs() {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image != null)
			saveROIs(image);
	}

	protected void saveROIs(ImagePlus image) {
		Roi[] rois = map.get(image);
		if (rois == null)
			return;

		SaveDialog dialog = new SaveDialog("Save ROIs", "RoiSet", ".zip");
		String name = dialog.getFileName();
		if (name == null)
			return;
		saveROIs(rois, dialog.getDirectory() + name);
	}

	protected void saveROIs(Roi[] rois, String path) {
		try {
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(path));
			DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
			RoiEncoder roiEncoder = new RoiEncoder(dataOut);
			for (int i = 0; i < rois.length; i++) {
				if (rois[i] == null)
					continue;
				out.putNextEntry(new ZipEntry(getROILabel(i + 1, rois.length, rois[i]) + ".roi"));
				roiEncoder.write(rois[i]);
				dataOut.flush();
			}
			dataOut.close();
		}
		catch (IOException e) {
			IJ.handleException(e);
		}
	}

	protected String getROILabel(int slice, int sliceCount, Roi roi) {
		int digits = (int)Math.ceil(Math.log(Math.max(1, sliceCount)) / Math.log(10)) + 1;
		String format = "%0" + digits + "d";
		Rectangle bounds = roi.getBounds();
		return String.format(format + "-" + format + "-" + format, slice, bounds.x, bounds.y);
	}

	protected void loadROIs() {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.error("Need an image");
			return;
		}
		loadROIs(image);
	}

	protected void loadROIs(ImagePlus image) {
		OpenDialog dialog = new OpenDialog("Load ROIs", "");
		String name = dialog.getFileName();
		if (name == null)
			return;
		loadROIs(image, dialog.getDirectory() + name);
	}

	protected void loadROIs(ImagePlus image, String path) {
		try {
			Roi[] rois = new Roi[image.getStackSize()];
			byte[] buf = new byte[16384];
			ZipInputStream in = new ZipInputStream(new FileInputStream(path));
			for (;;) {
				ZipEntry entry = in.getNextEntry();
				if (entry == null)
					break;
				String name = entry.getName();
				if (!entry.getName().endsWith(".roi"))
					continue;
				int slice, minus = name.indexOf('-');
				try {
					slice = Integer.parseInt(minus < 0 ? name : name.substring(0, minus));
				} catch (NumberFormatException e) {
					IJ.log("Skipping ROI with invalid name: " + name);
					continue;
				}
				if (slice < 1 || slice > rois.length) {
					IJ.log("Skipping ROI for invalid slice: " + slice);
					continue;
				}
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				for (;;) {
					int count = in.read(buf);
					if (count < 0)
						break;
					buffer.write(buf, 0, count);
				}
				RoiDecoder roiDecoder = new RoiDecoder(buffer.toByteArray(), entry.getName());
				Roi roi = roiDecoder.getRoi();
				if (roi != null)
					rois[slice - 1] = roi;
			}
			in.close();
			map.put(image, rois);
			setRoi(image, rois[image.getCurrentSlice() - 1]);
		}
		catch (IOException e) {
			IJ.handleException(e);
		}
	}

	protected void clearROIs() {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.error("Need an image");
			return;
		}
		map.remove(image);
		image.killRoi();
	}

	public void addIOButtons(final GenericDialogPlus gd) {
		gd.addButton("Export to ROI Manager", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportToROIManager();
				gd.dispose();
			}
		});
		gd.addButton("Import from ROI Manager", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importFromROIManager();
				gd.dispose();
			}
		});
		gd.addButton("Save ROIs", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveROIs();
				gd.dispose();
			}
		});
		gd.addButton("Load ROIs", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadROIs();
				gd.dispose();
			}
		});
		gd.addButton("Delete current ROIs", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearROIs();
				gd.dispose();
			}
		});
	}
}