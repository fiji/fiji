/*====================================================================
| Version: April 29, 2009
\===================================================================*/

/*====================================================================
| EPFL/STI/IMT/LIB
| Philippe Thevenaz
| Bldg. BM-Ecublens 4.137
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

/*===================================================================
@ARTICLE(SheppAndLoganPhantom
AUTHOR="Shepp, L.A. and Logan, B.F.",
TITLE="The Fourier Reconstruction of a Head Section",
JOURNAL="{IEEE} Transactions on Nuclear Science",
YEAR="1974",
volume="21",
number="3",
pages="21--43",
month="June",
note="")
\===================================================================*/

import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.Recorder;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

/*====================================================================
|	SheppLogan_
\===================================================================*/

/*------------------------------------------------------------------*/
public class SheppLogan_
	implements
		ExtendedPlugInFilter,
		KeyListener

{ /* begin class SheppLogan_ */

/*....................................................................
	private variables
....................................................................*/

private static boolean doCorrectForTypo = false;
private static int linearDimension = 512;
private static double xOrigin = (double)(linearDimension - 1) / 2.0;
private static double yOrigin = (double)(linearDimension - 1) / 2.0;
private final int CAPABILITIES = NO_IMAGE_REQUIRED;
private final int ELLIPSES = 10;
private final GenericDialog dialog = new GenericDialog("Shepp-Logan Phantom");
private final SheppLoganEllipse[] ellipses = new SheppLoganEllipse[ELLIPSES];
private final String DO_CORRECT_FOR_TYPO = "Do_Correct_for_Typo";
private final String LINEAR_DIMENSION = "Linear_Dimension_(pixel)";
private final String X_ORIGIN = "Horizontal_Origin_(pixel)";
private final String Y_ORIGIN = "Vertical_Origin_(pixel)";

/*....................................................................
	ExtendedPlugInFilter methods
....................................................................*/

/*------------------------------------------------------------------*/
public void run (
	final ImageProcessor ip
) {
	final Vector<TextField> numbers = dialog.getNumericFields();
	try {
		linearDimension = new Integer((numbers.elementAt(0)).getText());
		xOrigin = new Double((numbers.elementAt(1)).getText());
		yOrigin = new Double((numbers.elementAt(2)).getText());
	} catch (NumberFormatException e) {
		return;
	}
	if (linearDimension < 1) {
		return;
	}
	final Vector<Checkbox> booleans = dialog.getCheckboxes();
	doCorrectForTypo = (booleans.elementAt(0)).getState();
	Recorder.setCommand("SheppLogan ");
	Recorder.recordOption(LINEAR_DIMENSION, "" + linearDimension);
	Recorder.recordOption(X_ORIGIN, "" + xOrigin);
	Recorder.recordOption(Y_ORIGIN, "" + yOrigin);
	Recorder.recordOption(DO_CORRECT_FOR_TYPO, "" + doCorrectForTypo);
	Recorder.saveCommand();
	ellipses[0] = new SheppLoganEllipse(
		00.00, 00.0000, 0.6900, 0.920, 000.0, 02.00);
	ellipses[1] = new SheppLoganEllipse(
		00.00, -0.0184, 0.6624, 0.874, 000.0, -0.98);
	ellipses[2] = new SheppLoganEllipse(
		00.22, 00.0000, 0.1100, 0.310, -18.0, -0.02);
	ellipses[3] = new SheppLoganEllipse(
		-0.22, 00.0000, 0.1600, 0.410, 018.0, -0.02);
	ellipses[4] = new SheppLoganEllipse(
		00.00, 00.3500, 0.2100, 0.250, 000.0, 00.01);
	ellipses[5] = new SheppLoganEllipse(
		00.00, 00.1000, 0.0460, 0.046, 000.0, 00.01);
	if (doCorrectForTypo) {
		ellipses[6] = new SheppLoganEllipse(
			00.00, -0.1000, 0.0460, 0.046, 000.0, 00.02);
	}
	else {
		ellipses[6] = new SheppLoganEllipse(
			00.00, -0.1000, 0.0460, 0.046, 000.0, 00.01);
	}
	ellipses[7] = new SheppLoganEllipse(
		-0.08, -0.6050, 0.0460, 0.023, 000.0, 00.01);
	ellipses[8] = new SheppLoganEllipse(
		00.00, -0.6050, 0.0230, 0.023, 000.0, 00.01);
	ellipses[9] = new SheppLoganEllipse(
		00.06, -0.6050, 0.0230, 0.046, 000.0, 00.01);
	SheppLoganEllipse.xOrigin = xOrigin;
	SheppLoganEllipse.yOrigin = yOrigin;
	SheppLoganEllipse.linearDimension = linearDimension;
	final float[][] data = new float[linearDimension][linearDimension];
	for (int y = 0; (y < linearDimension); y++) {
		for (int x = 0; (x < linearDimension); x++) {
			for (int e = 0; (e < ELLIPSES); e++) {
				if (ellipses[e].contains(x, y)) {
					data[x][y] += ellipses[e].graylevel;
				}
			}
		}
	}
	final FloatProcessor phantom = new FloatProcessor(data);
	final ImagePlus imp = new ImagePlus("Shepp-Logan Phantom", phantom);
	imp.show();
} /* end run */

/*------------------------------------------------------------------*/
public void setNPasses (
	final int nPasses
) {
} /* end setNPasses */

/*------------------------------------------------------------------*/
public int setup (
	final String arg,
	final ImagePlus imp
) {
	return(CAPABILITIES);
} /* end setup */

/*------------------------------------------------------------------*/
public int showDialog (
	final ImagePlus imp,
	final String command,
	final PlugInFilterRunner pfr
) {
	dialog.addNumericField(LINEAR_DIMENSION, linearDimension, 0);
	dialog.addNumericField(X_ORIGIN, xOrigin, 2);
	dialog.addNumericField(Y_ORIGIN, yOrigin, 2);
	dialog.addCheckbox(DO_CORRECT_FOR_TYPO, doCorrectForTypo);
	final Vector<TextField> numbers = dialog.getNumericFields();
	(numbers.elementAt(0)).addKeyListener(this);
	if (Macro.getOptions() != null) {
		activateMacro();
		return(CAPABILITIES);
	}
	dialog.showDialog();
	if (dialog.wasCanceled()) {
		return(DONE);
	}
	if (dialog.wasOKed()) {
		return(CAPABILITIES);
	}
	else {
		return(DONE);
	}
} /* end showDialog */

/*....................................................................
	KeyListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void keyPressed (
	final KeyEvent e
) {
} /* end keyPressed */

/*------------------------------------------------------------------*/
public void keyReleased (
	final KeyEvent e
) {
} /* end keyReleased */

/*------------------------------------------------------------------*/
public void keyTyped (
	final KeyEvent e
) {
	final Vector<TextField> numbers = dialog.getNumericFields();
	try {
		linearDimension = new Integer((numbers.elementAt(0)).getText());
		xOrigin = (double)(linearDimension - 1) / 2.0;
		yOrigin = (double)(linearDimension - 1) / 2.0;
		(numbers.elementAt(1)).setText(""+xOrigin);
		(numbers.elementAt(2)).setText(""+yOrigin);
	} catch (NumberFormatException n) {
		return;
	}
} /* end keyTyped */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void activateMacro (
) {
	final Vector<TextField> numbers = dialog.getNumericFields();
	final TextField linearDimensionField = numbers.elementAt(0);
	final TextField xOriginField = numbers.elementAt(1);
	final TextField yOriginField = numbers.elementAt(2);
	final Vector<Checkbox> booleans = dialog.getCheckboxes();
	final Checkbox checkbox = booleans.elementAt(0);
	final String options = Macro.getOptions();
	linearDimensionField.setText(Macro.getValue(options, LINEAR_DIMENSION,
		"" + linearDimension));
	xOriginField.setText(Macro.getValue(options, X_ORIGIN,
		"" + xOrigin));
	yOriginField.setText(Macro.getValue(options, Y_ORIGIN,
		"" + yOrigin));
	checkbox.setState(new Boolean(Macro.getValue(options, DO_CORRECT_FOR_TYPO,
		"" + doCorrectForTypo)));
} /* end activateMacro */

} /* end class SheppLogan_ */

/*====================================================================
|	SheppLoganEllipse
\===================================================================*/

/*------------------------------------------------------------------*/
class SheppLoganEllipse

{ /* begin class SheppLoganEllipse */

/*....................................................................
	protected variables
....................................................................*/

protected double graylevel = 0.0;
protected double xCenter = 0.0;
protected double yCenter = 0.0;
protected static double xOrigin = 0.0;
protected static double yOrigin = 0.0;
protected static int linearDimension = 0;

/*....................................................................
	private variables
....................................................................*/

private double e0 = 0.0;
private double e11 = 0.0;
private double e12 = 0.0;
private double e22 = 0.0;

/*....................................................................
	creator methods
....................................................................*/

/*------------------------------------------------------------------*/
protected SheppLoganEllipse (
	final double xCenter,
	final double yCenter,
	final double majorAxis,
	final double minorAxis,
	final double theta,
	final double graylevel
) {
	this.graylevel = graylevel;
	this.xCenter = xCenter;
	this.yCenter = yCenter;
	final double c = Math.cos(theta * Math.PI / 180.0);
	final double s = Math.sin(theta * Math.PI / 180.0);
	e0 = minorAxis * minorAxis * majorAxis * majorAxis;
	e11 = majorAxis * majorAxis * s * s + minorAxis * minorAxis * c * c;
	e12 = (minorAxis * minorAxis - majorAxis * majorAxis)
		* Math.sin(2.0 * theta * Math.PI / 180.0);
	e22 = majorAxis * majorAxis * c * c + minorAxis * minorAxis * s * s;
} /* end SheppLoganEllipse */

/*....................................................................
	protected methods
....................................................................*/

/*------------------------------------------------------------------*/
protected boolean contains (
	final int x,
	final int y
) {
	final double u = 2.0 * ((double)x - xOrigin) / (double)linearDimension
		- xCenter;
	final double v = -2.0 * ((double)y - yOrigin) / (double)linearDimension
		- yCenter;
	return((e11 * u * u + e12 * u * v + e22 * v * v) < e0);
} /* end contains */

} /* end class SheppLoganEllipse */