/*====================================================================
| Version: December 19, 2008
\===================================================================*/

/*====================================================================
| EPFL/STI/BIO-E/LIB
| Philippe Thevenaz
| Bldg. BM-Ecublens 4.137
| CH-1015 Lausanne VD
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/thevenaz/pointpicker/
\===================================================================*/

import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.WindowManager;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

/*====================================================================
|	PointPicker_
\===================================================================*/

/*********************************************************************
 This class is the only one that is accessed directly by imageJ;
 it attaches listeners and dies. Note that it implements
 <code>PlugIn</code> rather than <code>PlugInFilter</code>.
 ********************************************************************/
public class PointPicker_
	implements
		PlugIn

{ /* begin class PointPicker_ */

/*..................................................................*/
/* Public methods													*/
/*..................................................................*/

/*------------------------------------------------------------------*/
public void run (
	final String arg
) {
	final ImagePlus imp = WindowManager.getCurrentImage();
	if (imp == null) {
		IJ.noImage();
		return;
	}
	final ImageCanvas ic = imp.getWindow().getCanvas();
	final pointToolbar tb = new pointToolbar(Toolbar.getInstance());
	final int stackSize = imp.getStackSize();
	final pointHandler[] ph = new pointHandler[stackSize];
	for (int s = 0; (s < stackSize); s++) {
		ph[s] = new pointHandler(imp, tb);
	}
	final pointAction pa = new pointAction(imp, ph, tb);
	for (int s = 0; (s < stackSize); s++) {
		ph[s].setPointAction(pa);
	}
} /* end run */

} /* end class PointPicker_ */

/*====================================================================
|	pointAction
\===================================================================*/

/*********************************************************************
 This class is responsible for dealing with the mouse events relative
 to the image window.
 ********************************************************************/
class pointAction
	extends
		ImageCanvas
	implements
		FocusListener,
		KeyListener,
		MouseListener,
		MouseMotionListener

{ /* begin class pointAction */

/*....................................................................
	Public variables
....................................................................*/
public static final int ADD_CROSS = 0;
public static final int MOVE_CROSS = 1;
public static final int REMOVE_CROSS = 2;
public static final int MONOSLICE = 4;
public static final int MULTISLICE = 5;
public static final int FILE = 7;
public static final int TERMINATE = 8;
public static final int MAGNIFIER = 11;

/*....................................................................
	Private variables
....................................................................*/
private ImagePlus imp;
private pointHandler[] ph;
private pointToolbar tb;
private boolean active = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Listen to <code>focusGained</code> events.
 @param e Ignored.
 ********************************************************************/
public void focusGained (
	final FocusEvent e
) {
	active = true;
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end focusGained */

/*********************************************************************
 Listen to <code>focusGained</code> events.
 @param e Ignored.
 ********************************************************************/
public void focusLost (
	final FocusEvent e
) {
	active = false;
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end focusLost */

/*********************************************************************
 Return true if the window is active.
 ********************************************************************/
public boolean isActive (
) {
	return(active);
} /* end isActive */

/*********************************************************************
 Listen to <code>keyPressed</code> events.
 @param e The expected key codes are as follows:
 <ul><li><code>KeyEvent.VK_DELETE</code>: remove the current landmark;</li>
 <li><code>KeyEvent.VK_BACK_SPACE</code>: remove the current landmark;</li>
 <li><code>KeyEvent.VK_COMMA</code>: display the previous slice, if any;</li>
 <li><code>KeyEvent.VK_DOWN</code>: move down the current landmark;</li>
 <li><code>KeyEvent.VK_LEFT</code>: move the current landmark to the left;</li>
 <li><code>KeyEvent.VK_PERIOD</code>: display the next slice, if any;</li>
 <li><code>KeyEvent.VK_RIGHT</code>: move the current landmark to the
 right;</li>
 <li><code>KeyEvent.VK_SPACE</code>: select the current landmark;</li>
 <li><code>KeyEvent.VK_UP</code>: move up the current landmark.</li></ul>
 ********************************************************************/
public void keyPressed (
	final KeyEvent e
) {
	active = true;
	switch (e.getKeyCode()) {
		case KeyEvent.VK_COMMA:
			if (1 < imp.getCurrentSlice()) {
				imp.setSlice(imp.getCurrentSlice() - 1);
				imp.setRoi(ph[imp.getCurrentSlice() - 1]);
				updateStatus();
			}
			return;
		case KeyEvent.VK_PERIOD:
			if (imp.getCurrentSlice() < imp.getStackSize()) {
				imp.setSlice(imp.getCurrentSlice() + 1);
				imp.setRoi(ph[imp.getCurrentSlice() - 1]);
				updateStatus();
			}
			return;
	}
	final Point p = ph[imp.getCurrentSlice() - 1].getPoint();
	if (p == null) {
		return;
	}
	final int x = p.x;
	final int y = p.y;
	int scaledX;
	int scaledY;
	int scaledShiftedX;
	int scaledShiftedY;
	switch (e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].removePoint();
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].removePoint(commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_DOWN:
			scaledX = imp.getWindow().getCanvas().screenX(x);
			scaledShiftedY = imp.getWindow().getCanvas().screenY(y
				+ (int)Math.ceil(1.0
				/ imp.getWindow().getCanvas().getMagnification()));
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledX,
						scaledShiftedY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledX, scaledShiftedY, commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_LEFT:
			scaledShiftedX = imp.getWindow().getCanvas().screenX(x
				- (int)Math.ceil(1.0
				/ imp.getWindow().getCanvas().getMagnification()));
			scaledY = imp.getWindow().getCanvas().screenY(y);
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledShiftedX,
					scaledY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledShiftedX, scaledY, commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_RIGHT:
			scaledShiftedX = imp.getWindow().getCanvas().screenX(x
				+ (int)Math.ceil(1.0
				/ imp.getWindow().getCanvas().getMagnification()));
			scaledY = imp.getWindow().getCanvas().screenY(y);
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledShiftedX,
					scaledY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledShiftedX, scaledY, commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_SPACE:
			break;
		case KeyEvent.VK_UP:
			scaledX = imp.getWindow().getCanvas().screenX(x);
			scaledShiftedY = imp.getWindow().getCanvas().screenY(y
				- (int)Math.ceil(1.0
				/ imp.getWindow().getCanvas().getMagnification()));
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledX,
					scaledShiftedY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledX, scaledShiftedY, commonColor);
					}
					break;
			}
			break;
	}
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	updateStatus();
} /* end keyPressed */

/*********************************************************************
 Listen to <code>keyReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void keyReleased (
	final KeyEvent e
) {
	active = true;
} /* end keyReleased */

/*********************************************************************
 Listen to <code>keyTyped</code> events.
 @param e Ignored.
 ********************************************************************/
public void keyTyped (
	final KeyEvent e
) {
	active = true;
} /* end keyTyped */

/*********************************************************************
 Listen to <code>mouseClicked</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
	active = true;
} /* end mouseClicked */

/*********************************************************************
 Listen to <code>mouseDragged</code> events. Move the current point
 and refresh the image window.
 @param e Event.
 ********************************************************************/
public void mouseDragged (
	final MouseEvent e
) {
	active = true;
	final int x = e.getX();
	final int y = e.getY();
	if (tb.getCurrentTool() == MOVE_CROSS) {
		switch (tb.getCurrentMode()) {
			case MONOSLICE:
				ph[imp.getCurrentSlice() - 1].movePoint(x, y);
				break;
			case MULTISLICE:
				final Integer commonColor =
					ph[imp.getCurrentSlice() - 1].getCurrentColor();
				for (int s = 0; (s < ph.length); s++) {
					ph[s].movePoint(x, y, commonColor);
				}
				break;
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	}
	mouseMoved(e);
} /* end mouseDragged */

/*********************************************************************
 Listen to <code>mouseEntered</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
	active = true;
	WindowManager.setCurrentWindow(imp.getWindow());
	imp.getWindow().toFront();
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end mouseEntered */

/*********************************************************************
 Listen to <code>mouseExited</code> events. Clear the ImageJ status
 bar.
 @param e Event.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
	active = false;
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	IJ.showStatus("");
} /* end mouseExited */

/*********************************************************************
 Listen to <code>mouseMoved</code> events. Update the ImageJ status
 bar.
 @param e Event.
 ********************************************************************/
public void mouseMoved (
	final MouseEvent e
) {
	active = true;
	setControl();
	final int x = imp.getWindow().getCanvas().offScreenX(e.getX());
	final int y = imp.getWindow().getCanvas().offScreenY(e.getY());
	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end mouseMoved */

/*********************************************************************
 Listen to <code>mousePressed</code> events. Perform the relevant
 action.
 @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	active = true;
	final int x = e.getX();
	final int y = e.getY();
	int currentPoint;
	switch (tb.getCurrentTool()) {
		case ADD_CROSS:
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].addPoint(
						imp.getWindow().getCanvas().offScreenX(x),
						imp.getWindow().getCanvas().offScreenY(y));
					break;
				case MULTISLICE:
					final int commonFreeColor = sieveColors();
					if (0 <= commonFreeColor) {
						for (int s = 0; (s < ph.length); s++) {
							ph[s].addPoint(
								imp.getWindow().getCanvas().offScreenX(x),
								imp.getWindow().getCanvas().offScreenY(y),
								commonFreeColor);
						}
					}
					break;
			}
			break;
		case MAGNIFIER:
			final int flags = e.getModifiers();
			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK))
				!= 0) {
				imp.getWindow().getCanvas().zoomOut(x, y);
			}
			else {
				imp.getWindow().getCanvas().zoomIn(x, y);
			}
			break;
		case MOVE_CROSS:
			ph[imp.getCurrentSlice() - 1].findClosest(x, y);
			break;
		case REMOVE_CROSS:
			ph[imp.getCurrentSlice() - 1].findClosest(x, y);
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].removePoint();
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].removePoint(commonColor);
					}
					break;
			}
			break;
	}
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end mousePressed */

/*********************************************************************
 Listen to <code>mouseReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
	active = true;
} /* end mouseReleased */

/*********************************************************************
 This constructor stores a local copy of its parameters and initializes
 the current control.
 @param imp <code>ImagePlus</code> object where points are being picked.
 @param ph <code>pointHandler</code> object that handles operations.
 @param tb <code>pointToolbar</code> object that handles the toolbar.
 ********************************************************************/
public pointAction (
	final ImagePlus imp,
	final pointHandler[] ph,
	final pointToolbar tb
) {
	super(imp);
	this.imp = imp;
	this.ph = ph;
	this.tb = tb;
	tb.setWindow(ph, imp);
	tb.installListeners(this);
} /* end pointAction */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private String getValueAsString (
	final int x,
	final int y
) {
	final Calibration cal = imp.getCalibration();
	final int[] v = imp.getPixel(x, y);
	switch (imp.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
			final double cValue = cal.getCValue(v[0]);
			if (cValue==v[0]) {
				return(", value=" + v[0]);
			}
			else {
				return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
			}
		case ImagePlus.GRAY32:
			return(", value=" + Float.intBitsToFloat(v[0]));
		case ImagePlus.COLOR_256:
			return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + ","
				+ v[2]);
		case ImagePlus.COLOR_RGB:
			return(", value=" + v[0] + "," + v[1] + "," + v[2]);
		default:
			return("");
	}
} /* end getValueAsString */

/*------------------------------------------------------------------*/
private void setControl (
) {
	switch (tb.getCurrentTool()) {
		case ADD_CROSS:
			imp.getWindow().getCanvas().setCursor(crosshairCursor);
			break;
		case FILE:
		case MAGNIFIER:
		case MOVE_CROSS:
		case REMOVE_CROSS:
			imp.getWindow().getCanvas().setCursor(defaultCursor);
			break;
	}
} /* end setControl */

/*------------------------------------------------------------------*/
private int sieveColors (
) {
	int attempt = 0;
	boolean found;
	do {
		found = true;
		for (int s = 0; (s < ph.length); s++) {
			if (ph[s].isUsedColor(attempt)) {
				found = false;
				attempt++;
				break;
			}
		}
	} while((attempt < pointHandler.GAMUT) && !found);
	if (!found) {
		attempt = -1;
		IJ.error("No color could be found that would fit all slices");
	}
	return(attempt);
} /* end sieveColors */

/*------------------------------------------------------------------*/
private void updateStatus (
) {
	final Point p = ph[imp.getCurrentSlice() - 1].getPoint();
	if (p == null) {
		IJ.showStatus("");
		return;
	}
	final int x = p.x;
	final int y = p.y;
	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end updateStatus */

} /* end class pointAction */

/*====================================================================
|	pointHandler
\===================================================================*/

/*********************************************************************
 This class is responsible for dealing with the list of point
 coordinates and for their visual appearance.
 ********************************************************************/
class pointHandler
	extends
		Roi

{ /* begin class pointHandler */

/*....................................................................
	Public variables
....................................................................*/
public static final int RAINBOW = 1;
public static final int MONOCHROME = 2;
public static final int GAMUT = 1024;

/*....................................................................
	Private variables
....................................................................*/
private static final int CROSS_HALFSIZE = 5;
private static int ID = 1;
private final Color spectrum[] = new Color[GAMUT];
private final boolean usedColor[] = new boolean[GAMUT];
private final Vector<Integer> listColors = new Vector<Integer>(0, 16);
private final Vector<Integer> listIDs = new Vector<Integer>(0, 16);
private final Vector<Point> listPoints = new Vector<Point>(0, 16);
private ImagePlus imp;
private pointAction pa;
private pointToolbar tb;
private int nextColor = 0;
private int currentPoint = -1;
private int numPoints = 0;
private boolean started = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method adds a new point to the list, with a color that is as
 different as possible from all those that are already in use. The
 points are stored in pixel units rather than canvas units to cope
 for different zooming factors.
 @param x Horizontal coordinate, in canvas units.
 @param y Vertical coordinate, in canvas units.
 ********************************************************************/
public void addPoint (
	final int x,
	final int y
) {
	if (numPoints < GAMUT) {
		if (usedColor[nextColor]) {
			int k;
			boolean found = false;
			for (k = 0; (k < GAMUT); k++) {
				nextColor++;
				nextColor &= GAMUT - 1;
				if (!usedColor[nextColor]) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new IllegalStateException(
					"Unexpected lack of available colors");
			}
		}
		final Point p = new Point(x, y);
		listIDs.addElement(ID++);
		listPoints.addElement(p);
		listColors.addElement(nextColor);
		usedColor[nextColor] = true;
		nextColor++;
		nextColor &= GAMUT - 1;
		currentPoint = numPoints;
		numPoints++;
	}
	else {
		IJ.error("Maximum number of points reached for this slice");
	}
} /* end addPoint */

/*********************************************************************
 This method adds a new point to the list, with a specific color.
 The points are stored in pixel units rather than canvas units to
 cope for different zooming factors.
 @param x Horizontal coordinate, in canvas units.
 @param y Vertical coordinate, in canvas units.
 @param color Specific color.
 ********************************************************************/
public void addPoint (
	final int x,
	final int y,
	final int color
) {
	if (usedColor[color]) {
		throw new IllegalStateException("Illegal color request");
	}
	final Point p = new Point(x, y);
	listIDs.addElement(ID++);
	listPoints.addElement(p);
	listColors.addElement(color);
	usedColor[color] = true;
	currentPoint = numPoints;
	numPoints++;
} /* end addPoint */

/*********************************************************************
 Draw the landmarks and outline the current point if there is one.
 @param g Graphics environment.
 ********************************************************************/
public void draw (
	final Graphics g
) {
	if (started) {
		final float mag = (float)ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		for (int k = 0; (k < numPoints); k++) {
			final Point p = listPoints.elementAt(k);
			g.setColor(spectrum[listColors.elementAt(k)]);
			if (k == currentPoint) {
				if (pa.isActive()) {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					if (1.0 < ic.getMagnification()) {
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy);
						g.drawLine(ic.screenX(p.x) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE) + dy,
							ic.screenX(p.x) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					}
				}
				else {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
				}
			}
			else {
				g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
					ic.screenY(p.y) + dy,
					ic.screenX(p.x + CROSS_HALFSIZE) + dx,
					ic.screenY(p.y) + dy);
				g.drawLine(ic.screenX(p.x) + dx,
					ic.screenY(p.y - CROSS_HALFSIZE) + dy,
					ic.screenX(p.x) + dx,
					ic.screenY(p.y + CROSS_HALFSIZE) + dy);
			}
		}
		if (updateFullWindow) {
			updateFullWindow = false;
			imp.draw();
		}
	}
} /* end draw */

/*********************************************************************
 Let the point that is closest to the given coordinates become the
 current landmark.
 @param x Horizontal coordinate, in canvas units.
 @param y Vertical coordinate, in canvas units.
 ********************************************************************/
public void findClosest (
	int x,
	int y
) {
	if (numPoints == 0) {
		return;
	}
	x = ic.offScreenX(x);
	y = ic.offScreenY(y);
	Point p = new Point(listPoints.elementAt(currentPoint));
	float distance = (float)(x - p.x) * (float)(x - p.x)
		+ (float)(y - p.y) * (float)(y - p.y);
	for (int k = 0; (k < numPoints); k++) {
		p = listPoints.elementAt(k);
		final float candidate = (float)(x - p.x) * (float)(x - p.x)
			+ (float)(y - p.y) * (float)(y - p.y);
		if (candidate < distance) {
			distance = candidate;
			currentPoint = k;
		}
	}
} /* end findClosest */

/*********************************************************************
 Return the list of colors.
 ********************************************************************/
public Vector<Integer> getColors (
) {
	return(listColors);
} /* end getColors */

/*********************************************************************
 Return the color of the current object. Return -1 if there is none.
 ********************************************************************/
public Integer getCurrentColor (
) {
	return((0 <= currentPoint) ? (listColors.elementAt(currentPoint))
		: (new Integer(-1)));
} /* end getCurrentColor */

/*********************************************************************
 Return the list of IDs.
 ********************************************************************/
public Vector<Integer> getIDs (
) {
	return(listIDs);
} /* end getIDs */

/*********************************************************************
 Return the current point as a <code>Point</code> object.
 ********************************************************************/
public Point getPoint (
) {
	return((0 <= currentPoint) ? (listPoints.elementAt(currentPoint)) : (null));
} /* end getPoint */

/*********************************************************************
 Return the list of points.
 ********************************************************************/
public Vector<Point> getPoints (
) {
	return(listPoints);
} /* end getPoints */

/*********************************************************************
 Return <code>true</code> if color is free.
 Return <code>false</code> if color is in use.
 ********************************************************************/
public boolean isUsedColor (
	final int color
) {
	return(usedColor[color]);
} /* end isUsedColor */

/*********************************************************************
 Modify the location of the current point. Clip the admissible range
 to the image size.
 @param x Desired new horizontal coordinate in canvas units.
 @param y Desired new vertical coordinate in canvas units.
 ********************************************************************/
public void movePoint (
	int x,
	int y
) {
	if (0 <= currentPoint) {
		x = ic.offScreenX(x);
		y = ic.offScreenY(y);
		x = (x < 0) ? (0) : (x);
		x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
		y = (y < 0) ? (0) : (y);
		y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
		listPoints.removeElementAt(currentPoint);
		final Point p = new Point(x, y);
		listPoints.insertElementAt(p, currentPoint);
	}
} /* end movePoint */

/*********************************************************************
 Modify the location of the current point. Clip the admissible range
 to the image size.
 @param x Desired new horizontal coordinate in canvas units.
 @param y Desired new vertical coordinate in canvas units.
 @param color Color index of the point to move.
 ********************************************************************/
public void movePoint (
	int x,
	int y,
	final Integer color
) {
	final int index = listColors.indexOf(color);
	if (index == -1) {
		return;
	}
	currentPoint = index;
	movePoint(x, y);
} /* end movePoint */

/*********************************************************************
 This constructor stores a local copy of its parameters and initializes
 the current spectrum. It also creates the object that takes care of
 the interactive work.
 @param imp <code>ImagePlus</code> object where points are being picked.
 @param tb <code>pointToolbar</code> object that handles the toolbar.
 ********************************************************************/
public pointHandler (
	final ImagePlus imp,
	final pointToolbar tb
) {
	super(0, 0, imp.getWidth(), imp.getHeight(), imp);
	this.imp = imp;
	this.tb = tb;
	setSpectrum(RAINBOW);
} /* end pointHandler */

/*********************************************************************
 Remove the current point. Make its color available again.
 ********************************************************************/
public void removePoint (
) {
	if (0 < numPoints) {
		listIDs.removeElementAt(currentPoint);
		listPoints.removeElementAt(currentPoint);
		usedColor[listColors.elementAt(currentPoint)] = false;
		listColors.removeElementAt(currentPoint);
		numPoints--;
	}
	currentPoint = numPoints - 1;
	if (currentPoint < 0) {
		tb.setTool(pointAction.ADD_CROSS);
	}
} /* end removePoint */

/*********************************************************************
 Remove a point of a given index. Make its color available again.
 @param color Color index of the point to remove.
 ********************************************************************/
public void removePoint (
	final Integer color
) {
	final int index = listColors.indexOf(color);
	if (index == -1) {
		return;
	}
	currentPoint = index;
	removePoint();
} /* end removePoint */

/*********************************************************************
 Remove all points and make every color available.
 ********************************************************************/
public void removePoints (
) {
	listIDs.removeAllElements();
	listPoints.removeAllElements();
	listColors.removeAllElements();
	for (int k = 0; (k < GAMUT); k++) {
		usedColor[k] = false;
	}
	nextColor = 0;
	numPoints = 0;
	currentPoint = -1;
	tb.setTool(pointAction.ADD_CROSS);
	imp.setRoi(this);
} /* end removePoints */

/*********************************************************************
 Stores a local copy of its parameter and allows the graphical
 operations to proceed. The present class is now fully initialized.
 @param pa <code>pointAction</code> object.
 ********************************************************************/
public void setPointAction (
	final pointAction pa
) {
	this.pa = pa;
	started = true;
} /* end setPointAction */

/*********************************************************************
 Setup the color scheme.
 @param colorization Colorization code. Admissible values are
 {<code>RAINBOW</code>, <code>MONOCHROME</code>}.
 ********************************************************************/
public void setSpectrum (
	final int colorization
) {
	int k = 0;
	switch (colorization) {
		case RAINBOW:
			final int bound1 = GAMUT / 6;
			final int bound2 = GAMUT / 3;
			final int bound3 = GAMUT / 2;
			final int bound4 = (2 * GAMUT) / 3;
			final int bound5 = (5 * GAMUT) / 6;
			final int bound6 = GAMUT;
			final float gamutChunk1 = (float)bound1;
			final float gamutChunk2 = (float)(bound2 - bound1);
			final float gamutChunk3 = (float)(bound3 - bound2);
			final float gamutChunk4 = (float)(bound4 - bound3);
			final float gamutChunk5 = (float)(bound5 - bound4);
			final float gamutChunk6 = (float)(bound6 - bound5);
			do {
				spectrum[stirColor(k)] = new Color(1.0F, (float)k
					/ gamutChunk1, 0.0F);
				usedColor[stirColor(k)] = false;
			} while (++k < bound1);
			do {
				spectrum[stirColor(k)] = new Color(1.0F - (float)(k - bound1)
					/ gamutChunk2, 1.0F, 0.0F);
				usedColor[stirColor(k)] = false;
			} while (++k < bound2);
			do {
				spectrum[stirColor(k)] = new Color(0.0F, 1.0F,
					(float)(k - bound2) / gamutChunk3);
				usedColor[stirColor(k)] = false;
			} while (++k < bound3);
			do {
				spectrum[stirColor(k)] = new Color(0.0F,
					1.0F - (float)(k - bound3) / gamutChunk4, 1.0F);
				usedColor[stirColor(k)] = false;
			} while (++k < bound4);
			do {
				spectrum[stirColor(k)] = new Color((float)(k - bound4)
					/ gamutChunk5, 0.0F, 1.0F);
				usedColor[stirColor(k)] = false;
			} while (++k < bound5);
			do {
				spectrum[stirColor(k)] = new Color(1.0F, 0.0F,
					1.0F - (float)(k - bound5) / gamutChunk6);
				usedColor[stirColor(k)] = false;
			} while (++k < bound6);
			break;
		case MONOCHROME:
			for (k = 0; (k < GAMUT); k++) {
				spectrum[k] = ROIColor;
				usedColor[k] = false;
			}
			break;
	}
	imp.setRoi(this);
} /* end setSpectrum */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private int stirColor (
	int color
) {
	if (color < 0) {
		return(-1);
	}
	int stirredColor = 0;
	for (int k = 0; (k < (int)Math.round(Math.log((double)GAMUT)
		/ Math.log(2.0))); k++) {
		stirredColor <<= 1;
		stirredColor |= (color & 1);
		color >>= 1;
	}
	return(stirredColor);
} /* end stirColor */

} /* end class pointHandler */

/*====================================================================
|	pointPickerClearAll
\===================================================================*/

/*********************************************************************
 This class creates a dialog to remove every point.
 ********************************************************************/
class pointPickerClearAll
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerClearAll */

/*....................................................................
	Private variables
....................................................................*/
private ImagePlus imp;
private pointHandler[] ph;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Clear All</code>: Remove everything;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	if (ae.getActionCommand().equals("Clear All")) {
		for (int s = 0; (s < ph.length); s++) {
			ph[s].removePoints();
		}
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Clear Slice")) {
		ph[imp.getCurrentSlice() - 1].removePoints();
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		setVisible(false);
	}
} /* end actionPerformed */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 This constructor stores a local copy of its parameters and prepares
 the layout of the dialog.
 @param parentWindow Parent window.
 @param ph <code>pointHandler</code> object that handles operations.
 @param imp <code>ImagePlus</code> object where points are being picked.
 ********************************************************************/
pointPickerClearAll (
	final Frame parentWindow,
	final pointHandler[] ph,
	final ImagePlus imp
) {
	super(parentWindow, "Removing Points", true);
	this.ph = ph;
	this.imp = imp;
	setLayout(new GridLayout(0, 1));
	final Button removeAllButton = new Button("Clear All");
	removeAllButton.addActionListener(this);
	final Button removeSliceButton = new Button("Clear Slice");
	removeSliceButton.addActionListener(this);
	final Button cancelButton = new Button("Cancel");
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(removeAllButton);
	add(removeSliceButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerClearAll */

} /* end class pointPickerClearAll */

/*====================================================================
|	pointPickerColorSettings
\===================================================================*/

/*********************************************************************
 This class creates a dialog to choose the color scheme.
 ********************************************************************/
class pointPickerColorSettings
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerColorSettings */

/*....................................................................
	Private variables
....................................................................*/
private final CheckboxGroup choice = new CheckboxGroup();
private ImagePlus imp;
private pointHandler[] ph;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Rainbow</code>: Display points in many colors;</li>
 <li><code>Monochrome</code>: Display points in ImageJ's highlight color;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	if (ae.getActionCommand().equals("Rainbow")) {
		for (int s = 0; (s < ph.length); s++) {
			ph[s].setSpectrum(pointHandler.RAINBOW);
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Monochrome")) {
		for (int s = 0; (s < ph.length); s++) {
			ph[s].setSpectrum(pointHandler.MONOCHROME);
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		setVisible(false);
	}
} /* end actionPerformed */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 This constructor stores a local copy of its parameters and prepares
 the layout of the dialog.
 @param parentWindow Parent window.
 @param imp <code>ImagePlus</code> object where points are being picked.
 @param ph <code>pointHandler</code> object that handles operations.
 ********************************************************************/
pointPickerColorSettings (
	final Frame parentWindow,
	final pointHandler[] ph,
	final ImagePlus imp
) {
	super(parentWindow, "Color Settings", true);
	this.ph = ph;
	this.imp = imp;
	setLayout(new GridLayout(0, 1));
	final Button rainbow = new Button("Rainbow");
	final Button monochrome = new Button("Monochrome");
	final Button cancelButton = new Button("Cancel");
	rainbow.addActionListener(this);
	monochrome.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(rainbow);
	add(monochrome);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerColorSettings */

} /* end class pointPickerColorSettings */

/*====================================================================
|	pointPickerFile
\===================================================================*/

/*********************************************************************
 This class creates a dialog to store and retrieve points into and
 from a text file, respectively.
 ********************************************************************/
class pointPickerFile
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerFile */

/*....................................................................
	Private variables
....................................................................*/
private final CheckboxGroup choice = new CheckboxGroup();
private ImagePlus imp;
private pointHandler[] ph;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Save as</code>: Save points into a text file;</li>
 <li><code>Show</code>: Display the coordinates in ImageJ's window;</li>
 <li><code>Open</code>: Retrieve points from a text file;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	this.setVisible(false);
	if (ae.getActionCommand().equals("Save as")) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Point list", FileDialog.SAVE);
		final String path;
		String filename = imp.getTitle();
		final int dot = filename.lastIndexOf('.');
		if (dot == -1) {
			fd.setFile(filename + ".txt");
		}
		else {
			filename = filename.substring(0, dot);
			fd.setFile(filename + ".txt");
		}
		fd.setVisible(true);
		path = fd.getDirectory();
		filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return;
		}
		try {
			final FileWriter fw = new FileWriter(path + filename);
			Point p;
			String n;
			String x;
			String y;
			String z;
			String c;
			String id;
			fw.write("point     x     y slice color    ID\n");
			for (int s = 0; (s < ph.length); s++) {
				Vector<Integer> listIDs = ph[s].getIDs();
				Vector<Point> listPoints = ph[s].getPoints();
				Vector<Integer> listColors = ph[s].getColors();
				for (int k = 0; (k < listPoints.size()); k++) {
					n = "" + k;
					while (n.length() < 5) {
						n = " " + n;
					}
					p = listPoints.elementAt(k);
					x = "" + p.x;
					while (x.length() < 5) {
						x = " " + x;
					}
					y = "" + p.y;
					while (y.length() < 5) {
						y = " " + y;
					}
					z = "" + (s + 1);
					while (z.length() < 5) {
						z = " " + z;
					}
					c = "" + listColors.elementAt(k);
					while (c.length() < 5) {
						c = " " + c;
					}
					id = "" + listIDs.elementAt(k);
					while (id.length() < 5) {
						id = " " + id;
					}
					fw.write(n + " " + x + " " + y + " " + z
						+ " " + c + " " + id + "\n");
				}
			}
			fw.close();
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (SecurityException e) {
			IJ.error("Security exception");
		}
	}
	else if (ae.getActionCommand().equals("Show")) {
		Point p;
		String n;
		String x;
		String y;
		String z;
		String c;
		String id;
		IJ.getTextPanel().setFont(new Font("Monospaced", Font.PLAIN, 12));
		IJ.setColumnHeadings(" point\t      x\t      y\t slice"
			+ "\t color\t     id");
		for (int s = 0; (s < ph.length); s++) {
			Vector<Integer> listIDs = ph[s].getIDs();
			Vector<Point> listPoints = ph[s].getPoints();
			Vector<Integer> listColors = ph[s].getColors();
			for (int k = 0; (k < listPoints.size()); k++) {
				n = "" + k;
				while (n.length() < 6) {
					n = " " + n;
				}
				p = listPoints.elementAt(k);
				x = "" + p.x;
				while (x.length() < 7) {
					x = " " + x;
				}
				y = "" + p.y;
				while (y.length() < 7) {
					y = " " + y;
				}
				z = "" + (s + 1);
				while (z.length() < 6) {
					z = " " + z;
				}
				c = "" + listColors.elementAt(k);
				while (c.length() < 6) {
					c = " " + c;
				}
				id = "" + listIDs.elementAt(k);
				while (id.length() < 7) {
					id = " " + id;
				}
				IJ.write(n + "\t" + x + "\t" + y + "\t" + z
					+ "\t" + c + "\t" + id);
			}
		}
	}
	else if (ae.getActionCommand().equals("Open")) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Point list", FileDialog.LOAD);
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return;
		}
		try {
			final FileReader fr = new FileReader(path + filename);
			final BufferedReader br = new BufferedReader(fr);
			for (int s = 0; (s < ph.length); s++) {
				ph[s].removePoints();
			}
			String line;
			String pString;
			String xString;
			String yString;
			String zString;
			String cString;
			int separatorIndex;
			int x;
			int y;
			int z;
			int c;
			if ((line = br.readLine()) == null) {
				fr.close();
				return;
			}
			while ((line = br.readLine()) != null) {
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					fr.close();
					IJ.error("Invalid file");
					return;
				}
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					fr.close();
					IJ.error("Invalid file");
					return;
				}
				xString = line.substring(0, separatorIndex);
				xString = xString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				yString = line.substring(0, separatorIndex);
				yString = yString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				zString = line.substring(0, separatorIndex);
				zString = zString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				cString = line.substring(0, separatorIndex);
				cString = cString.trim();
				x = Integer.parseInt(xString);
				y = Integer.parseInt(yString);
				z = Integer.parseInt(zString) - 1;
				c = Integer.parseInt(cString);
				if (z < ph.length) {
					ph[z].addPoint(x, y, c);
				}
			}
			fr.close();
		} catch (FileNotFoundException e) {
			IJ.error("File not found exception");
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (NumberFormatException e) {
			IJ.error("Number format exception");
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
	}
} /* end actionPerformed */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 This constructor stores a local copy of its parameters and prepares
 the layout of the dialog.
 @param parentWindow Parent window.
 @param ph <code>pointHandler</code> object that handles operations.
 @param imp <code>ImagePlus</code> object where points are being picked.
 ********************************************************************/
pointPickerFile (
	final Frame parentWindow,
	final pointHandler[] ph,
	final ImagePlus imp
) {
	super(parentWindow, "Point List", true);
	this.ph = ph;
	this.imp = imp;
	setLayout(new GridLayout(0, 1));
	final Button saveAsButton = new Button("Save as");
	final Button showButton = new Button("Show");
	final Button openButton = new Button("Open");
	final Button cancelButton = new Button("Cancel");
	saveAsButton.addActionListener(this);
	showButton.addActionListener(this);
	openButton.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(saveAsButton);
	add(showButton);
	add(openButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerFile */

} /* end class pointPickerFile */

/*====================================================================
|	pointPickerTerminate
\===================================================================*/

/*********************************************************************
 This class creates a dialog to return to ImageJ.
 ********************************************************************/
class pointPickerTerminate
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerTerminate */

/*....................................................................
	Private variables
....................................................................*/
private final CheckboxGroup choice = new CheckboxGroup();
private boolean cancel = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Done</code>: Return to ImageJ;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	this.setVisible(false);
	if (ae.getActionCommand().equals("Done")) {
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		cancel = true;
	}
} /* end actionPerformed */

/*********************************************************************
 Return <code>true</code> only if the user chose <code>Cancel</code>.
 ********************************************************************/
public boolean choseCancel (
) {
	return(cancel);
} /* end choseCancel */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 40, 20, 40));
} /* end getInsets */

/*********************************************************************
 This constructor prepares the layout of the dialog.
 @param parentWindow Parent window.
 ********************************************************************/
pointPickerTerminate (
	final Frame parentWindow
) {
	super(parentWindow, "Back to ImageJ", true);
	setLayout(new GridLayout(0, 1));
	final Button doneButton = new Button("Done");
	final Button cancelButton = new Button("Cancel");
	doneButton.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(doneButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerTerminate */

} /* end class pointPickerTerminate */

/*====================================================================
|	pointToolbar
\===================================================================*/

/*********************************************************************
 This class deals with the toolbar that gets substituted to that of
 ImageJ.
 ********************************************************************/
class pointToolbar
	extends
		Canvas
	implements
		AdjustmentListener,
		MouseListener

{ /* begin class pointToolbar */

/*....................................................................
	Private variables
....................................................................*/
private static final int NUM_TOOLS = 19;
private static final int SIZE = 22;
private static final int OFFSET = 3;
private static final Color gray = Color.lightGray;
private static final Color brighter = gray.brighter();
private static final Color darker = gray.darker();
private static final Color evenDarker = darker.darker();
private final boolean[] down = new boolean[NUM_TOOLS];
private Graphics g;
private	Scrollbar scrollbar;
private ImagePlus imp;
private Toolbar previousInstance;
private pointAction pa;
private pointHandler[] ph;
private pointToolbar instance;
private long mouseDownTime;
private int currentTool = pointAction.ADD_CROSS;
private int currentMode = pointAction.MONOSLICE;
private int x;
private int y;
private int xOffset;
private int yOffset;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Listen to <code>AdjustmentEvent</code> events.
 @param e Ignored.
 ********************************************************************/
public synchronized void adjustmentValueChanged (
	AdjustmentEvent e
) {
	imp.setRoi(ph[e.getValue() - 1]);
} /* adjustmentValueChanged */

/*********************************************************************
 Return the index of the mode that is currently activated.
 ********************************************************************/
public int getCurrentMode (
) {
	return(currentMode);
} /* getCurrentMode */

/*********************************************************************
 Return the index of the tool that is currently activated.
 ********************************************************************/
public int getCurrentTool (
) {
	return(currentTool);
} /* getCurrentTool */

/*********************************************************************
 Setup the various listeners.
 @param pa <code>pointAction</code> object.
 ********************************************************************/
public void installListeners (
	pointAction pa
) {
	this.pa = pa;
	final ImageWindow iw = imp.getWindow();
	final ImageCanvas ic = iw.getCanvas();
	iw.removeKeyListener(IJ.getInstance());
	ic.removeKeyListener(IJ.getInstance());
	ic.removeMouseListener(ic);
	ic.removeMouseMotionListener(ic);
	ic.addMouseMotionListener(pa);
	ic.addMouseListener(pa);
	ic.addKeyListener(pa);
	iw.addKeyListener(pa);
	if (imp.getWindow() instanceof StackWindow) {
		StackWindow sw = (StackWindow)imp.getWindow();
		final Component component[] = sw.getComponents();
		for (int i = 0; (i < component.length); i++) {
			if (component[i] instanceof Scrollbar) {
				scrollbar = (Scrollbar)component[i];
				scrollbar.addAdjustmentListener(this);
			}
		}
	}
	else {
		scrollbar = null;
	}
} /* end installListeners */

/*********************************************************************
 Listen to <code>mouseClicked</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
} /* end mouseClicked */

/*********************************************************************
 Listen to <code>mouseEntered</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
} /* end mouseEntered */

/*********************************************************************
 Listen to <code>mouseExited</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
} /* end mouseExited */

/*********************************************************************
 Listen to <code>mousePressed</code> events. Test for single or double
 clicks and perform the relevant action.
 @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	final int x = e.getX();
	final int y = e.getY();
	final int previousTool = currentTool;
	int newTool = 0;
	for (int i = 0; (i < NUM_TOOLS); i++) {
		if (((i * SIZE) < x) && (x < (i * SIZE + SIZE))) {
			newTool = i;
		}
	}
	final boolean doubleClick = ((newTool == getCurrentTool())
		&& ((System.currentTimeMillis() - mouseDownTime) <= 500L));
	mouseDownTime = System.currentTimeMillis();
	switch (newTool) {
		case pointAction.MONOSLICE:
		case pointAction.MULTISLICE:
			setMode(newTool);
			return;
		default:
			setTool(newTool);
	}
	if (doubleClick) {
		switch (newTool) {
			case pointAction.ADD_CROSS:
			case pointAction.MOVE_CROSS:
				pointPickerColorSettings colorDialog
					= new pointPickerColorSettings(IJ.getInstance(), ph, imp);
				GUI.center(colorDialog);
				colorDialog.setVisible(true);
				colorDialog.dispose();
				break;
			case pointAction.REMOVE_CROSS:
				pointPickerClearAll clearAllDialog
					= new pointPickerClearAll(IJ.getInstance(), ph, imp);
				GUI.center(clearAllDialog);
				clearAllDialog.setVisible(true);
				clearAllDialog.dispose();
				break;
		}
	}
	switch (newTool) {
		case pointAction.FILE:
			pointPickerFile fileDialog
				= new pointPickerFile(IJ.getInstance(), ph, imp);
			GUI.center(fileDialog);
			fileDialog.setVisible(true);
			setTool(previousTool);
			fileDialog.dispose();
			break;
		case pointAction.TERMINATE:
			pointPickerTerminate terminateDialog
				= new pointPickerTerminate(IJ.getInstance());
			GUI.center(terminateDialog);
			terminateDialog.setVisible(true);
			if (terminateDialog.choseCancel()) {
				setTool(previousTool);
			}
			else {
				for (int s = 0; (s < ph.length); s++) {
					ph[s].removePoints();
				}
				cleanUpListeners();
				restorePreviousToolbar();
				Toolbar.getInstance().repaint();
			}
			terminateDialog.dispose();
			break;
	}
} /* mousePressed */

/*********************************************************************
 Listen to <code>mouseReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*********************************************************************
 Draw the tools of the toolbar.
 @param g Graphics environment.
 ********************************************************************/
public void paint (
	final Graphics g
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		drawButton(g, i);
	}
} /* paint */

/*********************************************************************
 This constructor substitutes ImageJ's toolbar by that of PointPicker_.
 @param previousToolbar ImageJ's toolbar.
 ********************************************************************/
public pointToolbar (
	final Toolbar previousToolbar
) {
	previousInstance = previousToolbar;
	instance = this;
	final Container container = previousToolbar.getParent();
//	final Container container = IJ.getInstance(); // Proposed by Maxime Pinchon
	final Component component[] = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == previousToolbar) {
			container.remove(previousToolbar);
			container.add(this, i);
			break;
		}
	}
	resetButtons();
	down[currentTool] = true;
	down[currentMode] = true;
	setTool(currentTool);
	setMode(currentMode);
	setForeground(evenDarker);
	setBackground(gray);
	addMouseListener(this);
	container.validate();
} /* end pointToolbar */

/*********************************************************************
 Setup the current mode. The selection of non-functional modes is
 honored but leads to a no-op action.
 @param mode Admissible modes belong to [<code>0</code>,
 <code>NUM_TOOLS - 1</code>]
 ********************************************************************/
public void setMode (
	final int mode
) {
	if (mode == currentMode) {
		return;
	}
	down[mode] = true;
	down[currentMode] = false;
	final Graphics g = this.getGraphics();
	drawButton(g, currentMode);
	drawButton(g, mode);
	g.dispose();
	showMessage(mode);
	currentMode = mode;
} /* end setMode */

/*********************************************************************
 Setup the current tool. The selection of non-functional tools is
 honored but leads to a no-op action.
 @param tool Admissible tools belong to [<code>0</code>,
 <code>NUM_TOOLS - 1</code>]
 ********************************************************************/
public void setTool (
	final int tool
) {
	if (tool == currentTool) {
		return;
	}
	down[tool] = true;
	down[currentTool] = false;
	final Graphics g = this.getGraphics();
	drawButton(g, currentTool);
	drawButton(g, tool);
	g.dispose();
	showMessage(tool);
	currentTool = tool;
} /* end setTool */

/*********************************************************************
 Setup the point handler.
 @param ph <code>pointHandler</code> object that handles operations.
 @param imp <code>ImagePlus</code> object where points are being picked.
 ********************************************************************/
public void setWindow (
	final pointHandler[] ph,
	final ImagePlus imp
) {
	this.ph = ph;
	this.imp = imp;
} /* end setWindow */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void cleanUpListeners (
) {
	if (scrollbar != null) {
		scrollbar.removeAdjustmentListener(this);
	}
	final ImageWindow iw = imp.getWindow();
	final ImageCanvas ic = iw.getCanvas();
	iw.removeKeyListener(pa);
	ic.removeKeyListener(pa);
	ic.removeMouseListener(pa);
	ic.removeMouseMotionListener(pa);
	ic.addMouseMotionListener(ic);
	ic.addMouseListener(ic);
	ic.addKeyListener(IJ.getInstance());
	iw.addKeyListener(IJ.getInstance());
} /* end cleanUpListeners */

/*------------------------------------------------------------------*/
private void d (
	int x,
	int y
) {
	x += xOffset;
	y += yOffset;
	g.drawLine(this.x, this.y, x, y);
	this.x = x;
	this.y = y;
} /* end d */

/*------------------------------------------------------------------*/
private void drawButton (
	final Graphics g,
	final int tool
) {
	fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
	g.setColor(Color.black);
	int x = tool * SIZE + OFFSET;
	int y = OFFSET;
	if (down[tool]) {
		x++;
		y++;
	}
	this.g = g;
	switch (tool) {
		case pointAction.ADD_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			m(13, 11);
			d(13, 15);
			break;
		case pointAction.MOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(1, 1);
			d(1, 10);
			m(2, 2);
			d(2, 9);
			m(3, 3);
			d(3, 8);
			m(4, 4);
			d(4, 7);
			m(5, 5);
			d(5, 7);
			m(6, 6);
			d(6, 7);
			m(7, 7);
			d(7, 7);
			m(11, 5);
			d(11, 6);
			m(10, 7);
			d(10, 8);
			m(12, 7);
			d(12, 8);
			m(9, 9);
			d(9, 11);
			m(13, 9);
			d(13, 11);
			m(10, 12);
			d(10, 15);
			m(12, 12);
			d(12, 15);
			m(11, 9);
			d(11, 10);
			m(11, 13);
			d(11, 15);
			m(9, 13);
			d(13, 13);
			break;
		case pointAction.REMOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			break;
		case pointAction.MONOSLICE:
			xOffset = x;
			yOffset = y;
			m(2, 6);
			d(2, 6);
			m(3, 5);
			d(3, 5);
			m(5, 3);
			d(5, 3);
			m(6, 2);
			d(6, 2);
			m(9, 6);
			d(9, 6);
			m(10, 5);
			d(10, 5);
			m(12, 3);
			d(12, 3);
			m(13, 2);
			d(13, 2);
			m(9, 13);
			d(9, 13);
			m(10, 12);
			d(10, 12);
			m(12, 10);
			d(12, 10);
			m(13, 9);
			d(13, 9);
			m(2, 13);
			d(2, 13);
			m(3, 12);
			d(3, 12);
			m(4, 11);
			d(11, 11);
			d(11, 4);
			d(4, 4);
			d(4, 11);
			break;
		case pointAction.MULTISLICE:
			xOffset = x;
			yOffset = y;
			m(2, 13);
			d(9, 13);
			d(9, 6);
			d(2, 6);
			d(2, 13);
			m(3, 5);
			d(3, 5);
			m(4, 4);
			d(11, 4);
			d(11, 11);
			m(10, 12);
			d(10, 12);
			m(10, 5);
			d(10, 5);
			m(12, 3);
			d(12, 3);
			m(12, 10);
			d(12, 10);
			m(5, 3);
			d(5, 3);
			m(6, 2);
			d(13, 2);
			d(13, 9);
			break;
		case pointAction.FILE:
			xOffset = x;
			yOffset = y;
			m(3, 1);
			d(9, 1);
			d(9, 4);
			d(12, 4);
			d(12, 14);
			d(3, 14);
			d(3, 1);
			m(10, 2);
			d(11, 3);
			m(5, 4);
			d(7, 4);
			m(5, 6);
			d(10, 6);
			m(5, 8);
			d(10, 8);
			m(5, 10);
			d(10, 10);
			m(5, 12);
			d(10, 12);
			break;
		case pointAction.TERMINATE:
			xOffset = x;
			yOffset = y;
			m(5, 0);
			d(5, 8);
			m(4, 5);
			d(4, 7);
			m(3, 6);
			d(3, 7);
			m(2, 7);
			d(2, 9);
			m(1, 8);
			d(1, 9);
			m(2, 10);
			d(6, 10);
			m(3, 11);
			d(3, 13);
			m(1, 14);
			d(6, 14);
			m(0, 15);
			d(7, 15);
			m(2, 13);
			d(2, 13);
			m(5, 13);
			d(5, 13);
			m(7, 8);
			d(14, 8);
			m(8, 7);
			d(15, 7);
			m(8, 9);
			d(13, 9);
			m(9, 6);
			d(9, 10);
			m(15, 4);
			d(15, 6);
			d(14, 6);
			break;
		case pointAction.MAGNIFIER:
			xOffset = x + 2;
			yOffset = y + 2;
			m(3, 0);
			d(3, 0);
			d(5, 0);
			d(8, 3);
			d(8, 5);
			d(7, 6);
			d(7, 7);
			d(6, 7);
			d(5, 8);
			d(3, 8);
			d(0, 5);
			d(0, 3);
			d(3, 0);
			m(8, 8);
			d(9, 8);
			d(13, 12);
			d(13, 13);
			d(12, 13);
			d(8, 9);
			d(8, 8);
			break;
	}
} /* end drawButton */

/*------------------------------------------------------------------*/
private void fill3DRect (
	final Graphics g,
	final int x,
	final int y,
	final int width,
	final int height,
	final boolean raised
) {
	if (raised) {
		g.setColor(gray);
	}
	else {
		g.setColor(darker);
	}
	g.fillRect(x + 1, y + 1, width - 2, height - 2);
	g.setColor((raised) ? (brighter) : (evenDarker));
	g.drawLine(x, y, x, y + height - 1);
	g.drawLine(x + 1, y, x + width - 2, y);
	g.setColor((raised) ? (evenDarker) : (brighter));
	g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
	g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
} /* end fill3DRect */

/*------------------------------------------------------------------*/
private void m (
	final int x,
	final int y
) {
	this.x = xOffset + x;
	this.y = yOffset + y;
} /* end m */

/*------------------------------------------------------------------*/
private void resetButtons (
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		down[i] = false;
	}
} /* end resetButtons */

/*------------------------------------------------------------------*/
private void restorePreviousToolbar (
) {
	final Container container = instance.getParent();
	final Component component[] = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == instance) {
			container.remove(instance);
			container.add(previousInstance, i);
			container.validate();
			break;
		}
	}
} /* end restorePreviousToolbar */

/*------------------------------------------------------------------*/
private void showMessage (
	final int tool
) {
	switch (tool) {
		case pointAction.ADD_CROSS:
			IJ.showStatus("Add crosses");
			return;
		case pointAction.MOVE_CROSS:
			IJ.showStatus("Move crosses");
			return;
		case pointAction.REMOVE_CROSS:
			IJ.showStatus("Remove crosses");
			return;
		case pointAction.MONOSLICE:
			IJ.showStatus("Apply to the current slice");
			return;
		case pointAction.MULTISLICE:
			IJ.showStatus("Apply to all slices");
			return;
		case pointAction.FILE:
			IJ.showStatus("Export/Import list of points");
			return;
		case pointAction.TERMINATE:
			IJ.showStatus("Exit PointPicker");
			return;
		case pointAction.MAGNIFIER:
			IJ.showStatus("Magnifying glass");
			return;
		default:
			IJ.showStatus("Undefined operation");
			return;
	}
} /* end showMessage */

} /* end class pointToolbar */
