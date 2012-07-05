package fiji.stacks;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.Slicer;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * <h2>Dynamic reslice of a stack.</h2>
 * 
 * This plugin is simply a dynamic version of the Reslice command. It draws an
 * orthogonal slice through the volume represented by the stack it is applied on
 * along its ROI, and update dynamically this slice as the ROI is displaced or
 * deformed. It is based on the Reslice command, as it is in ImageJ version
 * 1.42l, by Patrick Kelly, Harvey Karten, Wayne Rasband, Julian Cooper and
 * Adrian Deerr
 * <p>
 * 
 * <h3>Version history</h3>
 * 
 * <ul>
 * <li>1.0 - 22 April 2009 - First working version.
 * <li>1.1 - 22 April 2009 - Albert Cardona added the separate thread for
 * updating
 * <li>1.2 - 23 April 2009 -
 * <ul>
 * <li>Window size automatically changes when Roi length changes
 * <li>Can now be called and managed from scripts
 * <li>Major refactoring
 * </ul>
 * </ul>
 * 
 * <h3>License: GPL</h3>
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License 2 as published by the Free
 * Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * <p>
 * 
 * @author Jean-Yves Tinevez (tinevez at mpi-cbg dot de) & Albert Cardona
 * @see Slicer
 * @version 1.2
 * @category Image > Stacks
 */
public class Dynamic_Reslice implements PlugIn, MouseMotionListener,
		WindowListener {

	/*
	 * FIELDS
	 */

	/**
	 * Is set to true, then rotate the destination by 90°.
	 */
	private boolean rotate;
	/**
	 * Is set to true, then the slices in the source stack will be processed
	 * from bottom to top
	 */
	private boolean flip;
	private double inputZSpacing = 1.0;
	private boolean rgb, notFloat;
	/**
	 * Stores the source ImagePlus for this plugin.
	 */
	private ImagePlus imp;
	/**
	 * Store the destination output for this plugin. This is where the 
	 * reslice is going to be drawn.
	 */
	private ImagePlus dest_imp;

	/**
	 * The Updater instance that will take care of redrawing dynamically the
	 * destination image.
	 */
	private Updater updater = new Updater();
	/**
	 * Number of discretized points in the line ROI
	 */
	private int n;
	/**
	 * Coordinates of the ROI (with respect to xbase, ybase)
	 */
	private double[] x, y;
	/**
	 * Coordinate of the bounding box of the ROI
	 */
	private int xbase, ybase;
	/**
	 * Total length of the line ROI
	 */
	private double length;
	/**
	 * Length of each segment of the line ROI. Has a length equal 
	 * to x.length-1.
	 */
	private double[] segmentLengths;
	/**
	 * Coordinate increment for each segment of the line ROI. Has a length equal
	 * to x.length -1.
	 */
	private double[] dx, dy;
	/**
	 * Flag to report if the dynamic reslice process was started. If set to true, will
	 * prevent new  call to the start method.
	 */
	private boolean hasStarted = false;

	/*
	 * NESTED CLASSES
	 */

	/**
	 * This is a helper class for the Dynamic_Reslice plugin, that delegates the
	 * repainting of the destination window to another thread.
	 * 
	 * @author Albert Cardona
	 */
	private class Updater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		Updater() {
			super("Dynamic Reslice updater");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call update from this thread
					if (r > 0)
						refresh();
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
				}
			}
		}
	}

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Empty constructor, needed by ImageJ. Does not set any fields. This
	 * constructor is meant to be called when the plugin is launched from ImageJ
	 * menus.
	 */
	public Dynamic_Reslice() {
		this.imp = WindowManager.getCurrentImage();
	}

	/**
	 * Create a new instance of this plugin, linked to the ImagePlus given in
	 * argument. This ImagePlus will be used as the source for dynamic
	 * reslicing. This constructor is meant to be called from a scripting
	 * context, and take care of setting all properties according the ImagePlus
	 * argument or defaults.
	 * 
	 * @param _imp            the source ImagePlus
	 */
	public Dynamic_Reslice(ImagePlus _imp) {
		this.imp = _imp;
		setup();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Main method, called when the plugin is launched from ImageJ menu.
	 */
	public void run(String arg) {

		if (imp == null) {
			IJ.noImage();
			return;
		}

		int stackSize = imp.getStackSize();
		Roi roi = imp.getRoi();
		int roiType = roi != null ? roi.getType() : 0;

		// stack required except for ROI = none or RECT
		if (stackSize < 2 && roi != null && roiType != Roi.RECTANGLE) {
			IJ.error("Dynamic Reslice...", "Stack required");
			return;
		}

		// permissible ROI types: none,*LINE
		if (roi == null	|| (roiType != Roi.LINE && roiType != Roi.POLYLINE && roiType != Roi.FREELINE)) {
			IJ.error("Dynamic Reslice...", "Line selection required");
			return;
		}

		// Show dialog (and quit if it was canceled).
		setup();
		if (!showDialog()) 	return;
		start();
	}
	
	/**
	 * Method to start the dynamic reslice process after all parameters
	 * have been set. Will not initiate as long as there is no ROI.
	 */
	public void start() {
		if (hasStarted || imp.getRoi() == null) return; 
		// Get type of source window
		rgb = imp.getType() == ImagePlus.COLOR_RGB;
		notFloat = !rgb && imp.getType() != ImagePlus.GRAY32;

		// Create the destination ImagePlus dest_imp by get a slice a first time.
		dest_imp = new ImagePlus("Dynamic Reslice of "+imp.getShortTitle(), getSlice(imp, imp.getRoi()));
		// Copy min & max to new result
		ImageProcessor ip = imp.getProcessor();
		double min = ip.getMin();
		double max = ip.getMax();
		if (!rgb) dest_imp.getProcessor().setMinAndMax(min, max);

		// Display window result
		dest_imp.show();

		// Add listeners
		imp.getCanvas().addMouseMotionListener(this);
		imp.getWindow().addWindowListener(this);
		dest_imp.getWindow().addWindowListener(this);
		
		// Set the started flag to true
		hasStarted = true;
	}
	
	/**
	 * Shut down the Dynamic reslice, that is, remove itself from the listener
	 * list.
	 */
	public void shutdown() {
		updater.quit();
		updater = null;
		imp.getCanvas().removeMouseMotionListener(this);
		imp.getWindow().removeWindowListener(this);
		dest_imp.getWindow().removeWindowListener(this);
		IJ.showStatus("Dynamic Reslice shut down.");
	}
	
	/**
	 * Refresh the output display with the current roi. This is done by sending a signal 
	 * to the Updater() thread. This method can be called from a script to force update
	 * after the Roi has been changed with other means than the mouse
	 */
	public void update() {
		updater.doUpdate();
	}
	


	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Create an ImageProcessor containing the reslice of the current stack in
	 * the given ImagePlus, for the Roi given as second argument (not necessary the 
	 * ImagePlus's Roi).
	 * Different sub-methods are called according to various Roi types.
	 * 
	 * @param imp  the ImagePlus to reslice
	 * @param roi  the Roi to use for reslice
	 * @return  an ImageProcessor with the resulting slice
	 */
	protected ImageProcessor getSlice(ImagePlus imp, Roi roi) {

		if (roi == null) return null;
		int roiType = roi.getType();
		ImageStack stack = imp.getStack();
		int stackSize = stack.getSize();
		ImageProcessor ip_out = null, ip;
		boolean ortho = false;
		float[] line = null;
		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;

		if (roiType == Roi.LINE) {
			Line lineRoi = (Line) roi;
			x1 = lineRoi.x1d;
			y1 = lineRoi.y1d;
			x2 = lineRoi.x2d;
			y2 = lineRoi.y2d;
			ortho = (x1 == x2 || y1 == y2);
		}

		// Build output ImageProcessor
		for (int i = 0; i < stackSize; i++) {
			ip = stack.getProcessor(flip ? stackSize - i : i + 1);

			if (roiType == Roi.POLYLINE || roiType == Roi.FREELINE) {
				line = getIrregularProfile(ip, roi);
			} else if (ortho) // orthogonal straight line
				line = getOrthoLine(ip, (int) x1, (int) y1, (int) x2, (int) y2);
			else
				// no orthogonal straight line
				line = getLine(ip, x1, y1, x2, y2);

			if (rotate) {
				if (i == 0)
					ip_out = ip.createProcessor(stackSize, line.length);
				putColumn(ip_out, i, 0, line, line.length);
			} else {
				if (i == 0)
					ip_out = ip.createProcessor(line.length, stackSize);
				putRow(ip_out, 0, i, line, line.length);
			}
		}

		// Deal with calibration
		Calibration cal = imp.getCalibration();
		double zSpacing = inputZSpacing / cal.pixelWidth;
		if (zSpacing != 1.0) {
			ip_out.setInterpolate(true);
			if (rotate)
				ip_out = ip_out.resize((int) (stackSize * zSpacing),
						line.length);
			else
				ip_out = ip_out.resize(line.length,
						(int) (stackSize * zSpacing));
		}

		return ip_out;
	}


	/**
	 * Main method that actually dispatch the work. Update the destination
	 * ImagePlus ({@link #dest_imp}) with the new content resulting from the
	 * reslice. Also deal with calibration.
	 */
	protected void reslice() {
	
		ImageProcessor ip_out;
		Roi roi = imp.getRoi();
		int roiType = roi != null ? roi.getType() : 0;

		/*
		 * Save calibration
		 */
		Calibration origCal = imp.getCalibration();
		double zSpacing = inputZSpacing / imp.getCalibration().pixelWidth;

		/*
		 * Do reslice and update dest_imp
		 */
		ip_out = getSlice(imp, roi);
		dest_imp.setProcessor(null, ip_out);

		/*
		 * Deal with calibration
		 */
		// create Calibration for new stack
		// start from previous cal and swap appropriate fields
		boolean horizontal = false;
		boolean vertical = false;
		if (roi != null && roiType == Roi.LINE) {
			Line l = (Line) roi;
			horizontal = (l.y2 - l.y1) == 0;
			vertical = (l.x2 - l.x1) == 0;
		}
		
		dest_imp.setCalibration(imp.getCalibration());
		Calibration cal = dest_imp.getCalibration();
		if (horizontal) {
			cal.pixelWidth = origCal.pixelWidth;
			cal.pixelHeight = origCal.pixelDepth / zSpacing;
		} else if (vertical) {
			cal.pixelWidth = origCal.pixelHeight;
			cal.pixelHeight = origCal.pixelDepth / zSpacing;
			;
		} else { // oblique line, polyLine or freeline
			if (origCal.pixelHeight == origCal.pixelWidth) {
				cal.pixelWidth = cal.pixelHeight = origCal.pixelDepth	/ zSpacing;
			} else {
				cal.pixelWidth = cal.pixelHeight = cal.pixelDepth = 1.0;
				cal.setUnit("pixel");
			}
		}
		double tmp;
		if (rotate) {// if rotated flip X and Y
			tmp = cal.pixelWidth;
			cal.pixelWidth = cal.pixelHeight;
			cal.pixelHeight = tmp;
		}
	}
	
	
	/*
	 * PRIVATE METHODS
	 */

	
	/**
	 * Generate the profile for this ImageProcessor along the Roi given in
	 * argument, in the case where the Roi is of type FREELINE or POLYLINE.
	 * 
	 * @param ip    the ImageProcessor to extract the profile from
	 * @param roi   the Roi to use as a profile guide
	 * @return the profile as a float array
	 * @see {@link #doIrregularSetup(Roi)},
	 *      {@link #getLine(ImageProcessor, double, double, double, double) getLine},
	 *      {@link #getOrthoLine(ImageProcessor, int, int, int, int) getOrthoLine}
	 */
	private float[] getIrregularProfile(ImageProcessor ip, Roi roi) {

		doIrregularSetup(roi);
		float[] values = new float[(int) length];
		double leftOver = 1.0;
		double distance = 0.0;
		int index;
		for (int i = 0; i < n; i++) {
			double len = segmentLengths[i];
			if (len == 0.0)
				continue;
			double xinc = dx[i] / len;
			double yinc = dy[i] / len;
			double start = 1.0 - leftOver;
			double rx = xbase + x[i] + start * xinc;
			double ry = ybase + y[i] + start * yinc;
			double len2 = len - start;
			int n2 = (int) len2;
			for (int j = 0; j <= n2; j++) {
				index = (int) distance + j;
				if (index < values.length) {
					if (notFloat)
						values[index] = (float) ip.getInterpolatedPixel(rx, ry);
					else if (rgb) {
						int rgbPixel = ((ColorProcessor) ip)
								.getInterpolatedRGBPixel(rx, ry);
						values[index] = Float
								.intBitsToFloat(rgbPixel & 0xffffff);
					} else
						values[index] = (float) ip.getInterpolatedValue(rx, ry);
				}
				rx += xinc;
				ry += yinc;
			}
			distance += len;
			leftOver = len2 - n2;
		}

		return values;

	}

	/**
	 * Generate the profile for this ImageProcessor along the Roi given in argument, in 
	 * the case where the Roi is a straight line in any direction.

	 * @param ip  the ImageProcessor to extract the profile from
	 * @param x1  coordinate of the Roi straight line
	 * @param y1  coordinate of the Roi straight line
	 * @param x2  coordinate of the Roi straight line
	 * @param y2  coordinate of the Roi straight line
	 * @return  the profile as a float array
	 * @see {@link #getIrregularProfile(ImageProcessor, Roi) getIrregularProfile getIrregularProfile},
	 * {@link #getIrregularProfile(ImageProcessor, Roi) getIrregularProfile}
	 */
	private float[] getLine(ImageProcessor ip, double x1, double y1, double x2,
			double y2) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		int n = (int) Math.round(Math.sqrt(dx * dx + dy * dy));
		float[] data = new float[n];
		double xinc = dx / n;
		double yinc = dy / n;
		double rx = x1;
		double ry = y1;
		for (int i = 0; i < n; i++) {
			if (notFloat)
				data[i] = (float) ip.getInterpolatedPixel(rx, ry);
			else if (rgb) {
				int rgbPixel = ((ColorProcessor) ip).getInterpolatedRGBPixel(
						rx, ry);
				data[i] = Float.intBitsToFloat(rgbPixel & 0xffffff);
			} else
				data[i] = (float) ip.getInterpolatedValue(rx, ry);
			rx += xinc;
			ry += yinc;
		}
		return data;
	}

	/**
	 * Generate the profile for this ImageProcessor along the Roi given in argument, in 
	 * the case where the Roi is a straight line, along the X or Y direction.

	 * @param ip  the ImageProcessor to extract the profile from
	 * @param x1  coordinate of the Roi straight line
	 * @param y1  coordinate of the Roi straight line
	 * @param x2  coordinate of the Roi straight line
	 * @param y2  coordinate of the Roi straight line
	 * @return  the profile as a float array
	 * @see {@link #getLine(ImageProcessor, double, double, double, double) getLine},
	 * {@link #getIrregularProfile(ImageProcessor, Roi) getIrregularProfile}
	 */
	private float[] getOrthoLine(ImageProcessor ip, int x1, int y1, int x2,
			int y2) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		int n = Math.max(Math.abs(dx), Math.abs(dy));
		float[] data = new float[n];
		int xinc = dx / n;
		int yinc = dy / n;
		int rx = x1;
		int ry = y1;
		for (int i = 0; i < n; i++) {
			if (notFloat)
				data[i] = (float) ip.getPixel(rx, ry);
			else if (rgb) {
				int rgbPixel = ((ColorProcessor) ip).getPixel(rx, ry);
				data[i] = Float.intBitsToFloat(rgbPixel & 0xffffff);
			} else
				data[i] = (float) ip.getPixelValue(rx, ry);
			rx += xinc;
			ry += yinc;
		}
		return data;
	}

	/**
	 * In the case of a POLYLINE or FREELINE Roi, generates the content of the
	 * fields {@link #x} and {@link #y} that will be used by the method
	 * {@link #getIrregularProfile(Roi, ImageProcessor) getIrregularProfile} to
	 * get a slice in the case of these Roi types.
	 * 
	 * @param roi
	 *            the POLYLINE or FREELINE Roi to work on
	 */
	private void doIrregularSetup(Roi roi) {
		n = ((PolygonRoi) roi).getNCoordinates();
		int[] ix = ((PolygonRoi) roi).getXCoordinates();
		int[] iy = ((PolygonRoi) roi).getYCoordinates();
		x = new double[n];
		y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = ix[i];
			y[i] = iy[i];
		}
		if (roi.getType() == Roi.FREELINE) {
			// smooth line
			for (int i = 1; i < n - 1; i++) {
				x[i] = (x[i - 1] + x[i] + x[i + 1]) / 3.0 + 0.5;
				y[i] = (y[i - 1] + y[i] + y[i + 1]) / 3.0 + 0.5;
			}
		}
		Rectangle r = roi.getBounds();
		xbase = r.x;
		ybase = r.y;
		length = 0.0;
		double segmentLength;
		double xdelta, ydelta;
		segmentLengths = new double[n];
		dx = new double[n];
		dy = new double[n];
		for (int i = 0; i < (n - 1); i++) {
			xdelta = x[i + 1] - x[i];
			ydelta = y[i + 1] - y[i];
			segmentLength = Math.sqrt(xdelta * xdelta + ydelta * ydelta);
			length += segmentLength;
			segmentLengths[i] = segmentLength;
			dx[i] = xdelta;
			dy[i] = ydelta;
		}
	}
	
	private void putRow(ImageProcessor ip, int x, int y, float[] data, int length) {
		if (rgb) {
			for (int i = 0; i < length; i++)
				ip.putPixel(x++, y, Float.floatToIntBits(data[i]));
		} else {
			for (int i = 0; i < length; i++)
				ip.putPixelValue(x++, y, data[i]);
		}
	}

	private void putColumn(ImageProcessor ip, int x, int y, float[] data, int length) {
		if (rgb) {
			for (int i = 0; i < length; i++)
				ip.putPixel(x, y++, Float.floatToIntBits(data[i]));
		} else {
			for (int i = 0; i < length; i++)
				ip.putPixelValue(x, y++, data[i]);
		}
	}
	
	/**
	 * Display the generic dialog for this plugin. Called by the
	 * {@link #run(String)} method when the plugin is launched from the ImageJ
	 * menu.
	 * 
	 * @return a boolean, which is false if the cancel button was pressed, true
	 *         otherwise
	 */
	private boolean showDialog() {

		/*
		 * Create and display generic dialog
		 */
		GenericDialog gd = new GenericDialog("Dynamic Reslice");
		gd.addCheckbox("Flip Vertically", flip);
		gd.addCheckbox("Rotate 90 Degrees", rotate);
		gd.showDialog();
		if (gd.wasCanceled())			return false;

		/*
		 * Collect from generic dialog
		 */
		flip = gd.getNextBoolean();
		rotate = gd.getNextBoolean();
		return true;
	}
	
	/**
	 * This methods setup this plugin's fields from the data contained in the
	 * source ImagePlus, {@link #imp}.
	 */
	private void setup() {
		Calibration cal = imp.getCalibration();
		if (cal.pixelDepth < 0.0)
			cal.pixelDepth = -cal.pixelDepth;
		if (cal.pixelWidth == 0.0)
			cal.pixelWidth = 1.0;
		inputZSpacing = cal.pixelDepth;
	}

	/**
	 * Update the Dynamic reslice window. Called by the
	 * Updater(), which lives in a separate thread.
	 */
	private void refresh() {
		// Only update if the roi is large enough, 
		if ( imp.getRoi().getLength() < 2.0)			return;
		// If not initialized, initialize
		if (!hasStarted) start();

		// Re-calculate slice
		reslice();
	}

	/*
	 * SETTERS AND GETTERS
	 */
	
	/**
	 * Get the flip state for this instance. If true, the slices in the source stack
	 * will be parsed from bottom to top.
	 */
	public boolean getFlip() {
		return flip;
	}
	
	/**
	 * Set the flip state for this instance. If sets to true, the slices in the source stack
	 * will be parsed from bottom to top. Cannot be changed after the start() method has been
	 * called on this plugin.
	 */
	public void setFlip(boolean _flip) {
		if (hasStarted) return;
		this.flip = _flip;
	}
	
	/**
	 * Get the rotate state for this instance. If true, the result image
	 * will be rotated 90°.
	 */
	public boolean getRotate() {
		return rotate;
	}
	
	/**
	 * Set the flip state for this instance. If sets to true, the result image
	 * will be be rotated 90°. Cannot be changed after the start() method has been
	 * called on this plugin.
	 */
	public void setRotate(boolean _rotate) {
		if (hasStarted) return;
		this.rotate = _rotate;
	}
	
	public ImagePlus getImagePlus() {
		return dest_imp;
	}
	
	/*
	 * EVENTS
	 */

	public void mouseDragged(MouseEvent e) {
		e.consume();
		updater.doUpdate();
	}

	public void windowClosing(WindowEvent e) {
		shutdown();
	}

	public void mouseMoved(MouseEvent e) {	}
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

}
