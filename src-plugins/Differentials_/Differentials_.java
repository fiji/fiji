/*********************************************************************
 * Version: May 21, 2001
 ********************************************************************/

/*********************************************************************
 * Philippe Thevenaz
 * BM-Ecublens 4.137
 * Biomedical Imaging Group/IOA/DMT
 * Swiss Federal Institute of Technology Lausanne
 * CH-1015 Lausanne EPFL
 * Switzerland
 *
 * phone (CET): +41(21)693.51.61
 * fax: +41(21)693.37.01
 * RFC-822: philippe.thevenaz@epfl.ch
 * X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
 * URL: http://bigwww.epfl.ch/
 ********************************************************************/

/*********************************************************************
 * This work is based on the following paper:
 *
 * Michael Unser
 * Splines: A Perfect Fit for Signal and Image Processing
 * IEEE Signal Processing Magazine
 * vol. 16, no. 6, pp. 22-38, November 1999
 *
 * This paper is available on-line at
 * http://bigwww.epfl.ch/publications/unser9902.html
 *
 * Other relevant on-line publications are available at
 * http://bigwww.epfl.ch/publications/
 ********************************************************************/

/*********************************************************************
 * See the companion file "Differentials_.html" for additional help
 *
 * You'll be free to use this software for research purposes, but you
 * should not redistribute it without our consent. In addition, we expect
 * you to include a citation or acknowlegment whenever you present or
 * publish results that are based on it.
 ********************************************************************/

import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;

/********************************************************************/
/* Differentials_													*/
/********************************************************************/
/*------------------------------------------------------------------*/
public class Differentials_
	implements
		PlugIn

{ /* begin class Differentials_ */

/*..................................................................*/
/* Global-scope variables											*/
/*..................................................................*/
final static int GRADIENT_MAGNITUDE = 1;
final static int GRADIENT_DIRECTION = 2;
final static int LAPLACIAN = 3;
final static int LARGEST_HESSIAN = 4;
final static int SMALLEST_HESSIAN = 5;
final static int HESSIAN_ORIENTATION = 6;

private final double FLT_EPSILON = (double)Float.intBitsToFloat((int)0x33FFFFFF);
private ImagePlus imp = null;
private ProgressBar progressBar = IJ.getInstance().getProgressBar();
private long lastTime = System.currentTimeMillis();
private int completed = 1;
private int processDuration = 1;
private static int operation = LAPLACIAN;
private int stackSize = 1;
private boolean cancel = false;

/*..................................................................*/
/* Public methods													*/
/*..................................................................*/
/*------------------------------------------------------------------*/
public void getCrossHessian (
	ImageProcessor ip,
	double tolerance
) {
	getHorizontalGradient(ip, tolerance);
	getVerticalGradient(ip, tolerance);
} /* end getCrossHessian */

/*------------------------------------------------------------------*/
public void getHorizontalGradient (
	ImageProcessor ip,
	double tolerance
) {
	if (!(ip.getPixels() instanceof float[])) {
		throw new IllegalArgumentException("Float image required");
	}

	float[] floatPixels = (float[])ip.getPixels();
	int width = ip.getWidth();
	int height = ip.getHeight();
	double line[] = new double[width];

	for (int y = 0; (y < height); y++) {
		getRow(ip, y, line);
		getSplineInterpolationCoefficients(line, tolerance);
		getGradient(line);
		putRow(ip, y, line);
		stepProgressBar();
	}
} /* end getHorizontalGradient */

/*------------------------------------------------------------------*/
public void getHorizontalHessian (
	ImageProcessor ip,
	double tolerance
) {
	if (!(ip.getPixels() instanceof float[])) {
		throw new IllegalArgumentException("Float image required");
	}

	float[] floatPixels = (float[])ip.getPixels();
	int width = ip.getWidth();
	int height = ip.getHeight();
	double line[] = new double[width];

	for (int y = 0; (y < height); y++) {
		getRow(ip, y, line);
		getSplineInterpolationCoefficients(line, tolerance);
		getHessian(line);
		putRow(ip, y, line);
		stepProgressBar();
	}
} /* end getHorizontalHessian */

/*------------------------------------------------------------------*/
public void getVerticalGradient (
	ImageProcessor ip,
	double tolerance
) {
	if (!(ip.getPixels() instanceof float[])) {
		throw new IllegalArgumentException("Float image required");
	}

	float[] floatPixels = (float[])ip.getPixels();
	int width = ip.getWidth();
	int height = ip.getHeight();
	double line[] = new double[height];

	for (int x = 0; (x < width); x++) {
		getColumn(ip, x, line);
		getSplineInterpolationCoefficients(line, tolerance);
		getGradient(line);
		putColumn(ip, x, line);
		stepProgressBar();
	}
} /* end getVerticalGradient */

/*------------------------------------------------------------------*/
public void getVerticalHessian (
	ImageProcessor ip,
	double tolerance
) {
	if (!(ip.getPixels() instanceof float[])) {
		throw new IllegalArgumentException("Float image required");
	}

	float[] floatPixels = (float[])ip.getPixels();
	int width = ip.getWidth();
	int height = ip.getHeight();
	double line[] = new double[height];

	for (int x = 0; (x < width); x++) {
		getColumn(ip, x, line);
		getSplineInterpolationCoefficients(line, tolerance);
		getHessian(line);
		putColumn(ip, x, line);
		stepProgressBar();
	}
} /* end getVerticalHessian */

/*------------------------------------------------------------------*/
public void run (
	String arg
) {
	ImagePlus imp = WindowManager.getCurrentImage();
	this.imp = imp;
	if (imp == null) {
		IJ.noImage();
		return;
	}
	if ((1 < imp.getStackSize()) && (imp.getType() == imp.COLOR_256)) {
		IJ.error("Stack of color images not supported (use grayscale)");
		return;
	}
	if (1 < imp.getStackSize()) {
		if (imp.getStack().isRGB()) {
			IJ.error("RGB color images not supported (use grayscale)");
			return;
		}
	}
	if (1 < imp.getStackSize()) {
		if (imp.getStack().isHSB()) {
			IJ.error("HSB color images not supported (use grayscale)");
			return;
		}
	}
	if (imp.getType() == imp.COLOR_256) {
		IJ.error("Indexed color images not supported (use grayscale)");
		return;
	}
	if (imp.getType() == imp.COLOR_RGB) {
		IJ.error("Color images not supported (use grayscale)");
		return;
	}

	differentialsDialog dialog = new differentialsDialog(IJ.getInstance(),
		"Differentials", true, operation);
	GUI.center(dialog);
	dialog.setVisible(true);
	cancel = dialog.getCancel();
	operation = dialog.getOperation();
	dialog.dispose();
	if (cancel) {
		return;
	}

	imp.startTiming();
	if (1 < imp.getStackSize()) {
		if (!(imp.getProcessor().getPixels() instanceof float[])) {
			new StackConverter(imp).convertToGray32();
		}
	}
	else {
		if (!(imp.getProcessor().getPixels() instanceof float[])) {
			new ImageConverter(imp).convertToGray32();
		}
	}
	ImageStack stack = imp.getStack();
	stackSize = stack.getSize();
	Undo.reset();

	setupProgressBar();
	resetProgressBar();

	for (int i = 1; (i <= stackSize); i++) {
		ImageProcessor ip = stack.getProcessor(i);
		doIt(ip);
		imp.getProcessor().resetMinAndMax();
		imp.setSlice(i);
		imp.updateAndRepaintWindow();
	}
	imp.getProcessor().resetMinAndMax();
	imp.setSlice(1);
	imp.updateAndRepaintWindow();
	cleanUpProgressBar();
	IJ.showTime(imp, imp.getStartTime(), "Differentials: ");
	ImageWindow win = imp.getWindow();
	if (win != null) {
		win.running = false;
	}
} /* end run */

/*..................................................................*/
/* Private methods													*/
/*..................................................................*/
/*------------------------------------------------------------------*/
void antiSymmetricFirMirrorOnBounds (
	double[] h,
	double[] c,
	double[] s
) {
	if (h.length != 2) {
		throw new IndexOutOfBoundsException("The half-length filter size should be 2");
	}
	if (h[0] != 0.0) {
		throw new IllegalArgumentException("Antisymmetry violation (should have h[0]=0.0)");
	}
	if (c.length != s.length) {
		throw new IndexOutOfBoundsException("Incompatible size");
	}
	if (2 <= c.length) {
		s[0] = 0.0;
		for (int i = 1; (i < (s.length - 1)); i++) {
			s[i] = h[1] * (c[i + 1] - c[i - 1]);
		}
		s[s.length - 1] = 0.0;
	}
	else {
		if (c.length == 1) {
			s[0] = 0.0;
		}
		else {
			throw new NegativeArraySizeException("Invalid length of data");
		}
	}
} /* end antiSymmetricFirMirrorOnBounds */

/*------------------------------------------------------------------*/
void cleanUpProgressBar (
) {
	completed = 0;
	progressBar.show(2.0);
} /* end cleanUpProgressBar */

/*------------------------------------------------------------------*/
void doIt (
	ImageProcessor ip
) {
	int width = ip.getWidth();
	int height = ip.getHeight();
	double hLine[] = new double[width];
	double vLine[] = new double[height];

	if (!(ip.getPixels() instanceof float[])) {
		throw new IllegalArgumentException("Float image required");
	}
	switch (operation) {
		case GRADIENT_MAGNITUDE:
			{
				ImageProcessor h = ip.duplicate();
				ImageProcessor v = ip.duplicate();
				float[] floatPixels = (float[])ip.getPixels();
				float[] floatPixelsH = (float[])h.getPixels();
				float[] floatPixelsV = (float[])v.getPixels();

				getHorizontalGradient(h, FLT_EPSILON);
				getVerticalGradient(v, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++) {
					for (int x = 0; (x < width); x++, k++) {
						floatPixels[k] = (float)Math.sqrt(floatPixelsH[k] * floatPixelsH[k]
							+ floatPixelsV[k] * floatPixelsV[k]);
					}
					stepProgressBar();
				}
			}
			break;
		case GRADIENT_DIRECTION:
			{
				ImageProcessor h = ip.duplicate();
				ImageProcessor v = ip.duplicate();
				float[] floatPixels = (float[])ip.getPixels();
				float[] floatPixelsH = (float[])h.getPixels();
				float[] floatPixelsV = (float[])v.getPixels();

				getHorizontalGradient(h, FLT_EPSILON);
				getVerticalGradient(v, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++) {
					for (int x = 0; (x < width); x++, k++) {
						floatPixels[k] = (float)Math.atan2(floatPixelsH[k], floatPixelsV[k]);
					}
					stepProgressBar();
				}
			}
			break;
		case LAPLACIAN:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				float[] floatPixels = (float[])ip.getPixels();
				float[] floatPixelsHH = (float[])hh.getPixels();
				float[] floatPixelsVV = (float[])vv.getPixels();

				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++) {
					for (int x = 0; (x < width); x++, k++) {
						floatPixels[k] = (float)(floatPixelsHH[k] + floatPixelsVV[k]);
					}
					stepProgressBar();
				}
			}
			break;
		case LARGEST_HESSIAN:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				ImageProcessor hv = ip.duplicate();
				float[] floatPixels = (float[])ip.getPixels();
				float[] floatPixelsHH = (float[])hh.getPixels();
				float[] floatPixelsVV = (float[])vv.getPixels();
				float[] floatPixelsHV = (float[])hv.getPixels();

				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				getCrossHessian(hv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++) {
					for (int x = 0; (x < width); x++, k++) {
						floatPixels[k] = (float)(0.5 * (floatPixelsHH[k] + floatPixelsVV[k]
							+ Math.sqrt(4.0 * floatPixelsHV[k] * floatPixelsHV[k]
							+ (floatPixelsHH[k] - floatPixelsVV[k])
							* (floatPixelsHH[k] - floatPixelsVV[k]))));
					}
					stepProgressBar();
				}
			}
			break;
		case SMALLEST_HESSIAN:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				ImageProcessor hv = ip.duplicate();
				float[] floatPixels = (float[])ip.getPixels();
				float[] floatPixelsHH = (float[])hh.getPixels();
				float[] floatPixelsVV = (float[])vv.getPixels();
				float[] floatPixelsHV = (float[])hv.getPixels();

				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				getCrossHessian(hv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++) {
					for (int x = 0; (x < width); x++, k++) {
						floatPixels[k] = (float)(0.5 * (floatPixelsHH[k] + floatPixelsVV[k]
							- Math.sqrt(4.0 * floatPixelsHV[k] * floatPixelsHV[k]
							+ (floatPixelsHH[k] - floatPixelsVV[k])
							* (floatPixelsHH[k] - floatPixelsVV[k]))));
					}
					stepProgressBar();
				}
			}
			break;
		case HESSIAN_ORIENTATION:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				ImageProcessor hv = ip.duplicate();
				float[] floatPixels = (float[])ip.getPixels();
				float[] floatPixelsHH = (float[])hh.getPixels();
				float[] floatPixelsVV = (float[])vv.getPixels();
				float[] floatPixelsHV = (float[])hv.getPixels();

				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				getCrossHessian(hv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++) {
					for (int x = 0; (x < width); x++, k++) {
						if (floatPixelsHV[k] < 0.0) {
							floatPixels[k] = (float)(-0.5 * Math.acos((floatPixelsHH[k]
								- floatPixelsVV[k]) / Math.sqrt(4.0 * floatPixelsHV[k]
								* floatPixelsHV[k] + (floatPixelsHH[k] - floatPixelsVV[k])
								* (floatPixelsHH[k] - floatPixelsVV[k]))));
						}
						else {
							floatPixels[k] = (float)(0.5 * Math.acos((floatPixelsHH[k]
								- floatPixelsVV[k]) / Math.sqrt(4.0 * floatPixelsHV[k]
								* floatPixelsHV[k] + (floatPixelsHH[k] - floatPixelsVV[k])
								* (floatPixelsHH[k] - floatPixelsVV[k]))));
						}
					}
					stepProgressBar();
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid operation");
	}
	ip.resetMinAndMax();
	imp.updateAndDraw();
} /* end doIt */

/*------------------------------------------------------------------*/
void getColumn (
	ImageProcessor ip,
	int x,
	double[] column
) {
	int width = ip.getWidth();

	if (ip.getHeight() != column.length) {
		throw new IndexOutOfBoundsException("Incoherent array sizes");
	}
	if (ip.getPixels() instanceof float[]) {
		float[] floatPixels = (float[])ip.getPixels();
		for (int i = 0; (i < column.length); i++) {
			column[i] = (double)floatPixels[x];
			x += width;
		}
	}
	else {
		throw new IllegalArgumentException("Float image required");
	}
} /* end getColumn */

/*------------------------------------------------------------------*/
void getGradient (
	double[] c
) {
	double h[] = {0.0, -1.0 / 2.0};
	double s[] = new double[c.length];

	antiSymmetricFirMirrorOnBounds(h, c, s);
	System.arraycopy(s, 0, c, 0, s.length);
} /* end getGradient */

/*------------------------------------------------------------------*/
void getHessian (
	double[] c
) {
	double h[] = {-2.0, 1.0};
	double s[] = new double[c.length];

	symmetricFirMirrorOnBounds(h, c, s);
	System.arraycopy(s, 0, c, 0, s.length);
} /* end getHessian */

/*------------------------------------------------------------------*/
double getInitialAntiCausalCoefficientMirrorOnBounds (
	double[] c,
	double z,
	double tolerance
) {
	return((z * c[c.length - 2] + c[c.length - 1]) * z / (z * z - 1.0));
} /* end getInitialAntiCausalCoefficientMirrorOnBounds */

/*------------------------------------------------------------------*/
double getInitialCausalCoefficientMirrorOnBounds (
	double[] c,
	double z,
	double tolerance
) {
	double z1 = z, zn = Math.pow(z, c.length - 1);
	double sum = c[0] + zn * c[c.length - 1];
	int horizon = c.length;

	if (0.0 < tolerance) {
		horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
		horizon = (horizon < c.length) ? (horizon) : (c.length);
	}
	zn = zn * zn;
	for (int n = 1; (n < (horizon - 1)); n++) {
		zn = zn / z;
		sum = sum + (z1 + zn) * c[n];
		z1 = z1 * z;
	}
	return(sum / (1.0 - Math.pow(z, 2 * c.length - 2)));
} /* end getInitialCausalCoefficientMirrorOnBounds */

/*------------------------------------------------------------------*/
void getRow (
	ImageProcessor ip,
	int y,
	double[] row
) {
	int rowLength = ip.getWidth();

	if (rowLength != row.length) {
		throw new IndexOutOfBoundsException("Incoherent array sizes");
	}
	y *= rowLength;
	if (ip.getPixels() instanceof float[]) {
		float[] floatPixels = (float[])ip.getPixels();
		for (int i = 0; (i < rowLength); i++) {
			row[i] = (double)floatPixels[y++];
		}
	}
	else {
		throw new IllegalArgumentException("Float image required");
	}
} /* end getRow */

/*------------------------------------------------------------------*/
void getSplineInterpolationCoefficients (
	double[] c,
	double tolerance
) {
	double z[] = {Math.sqrt(3.0) - 2.0};
	double lambda = 1.0;

	if (c.length == 1) {
		return;
	}
	for (int k = 0; (k < z.length); k++) {
		lambda = lambda * (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
	}
	for (int n = 0; (n < c.length); n++) {
		c[n] = c[n] * lambda;
	}
	for (int k = 0; (k < z.length); k++) {
		c[0] = getInitialCausalCoefficientMirrorOnBounds(c, z[k], tolerance);
		for (int n = 1; (n < c.length); n++) {
			c[n] = c[n] + z[k] * c[n - 1];
		}
		c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOnBounds(c, z[k],
			tolerance);
		for (int n = c.length - 2; (0 <= n); n--) {
			c[n] = z[k] * (c[n+1] - c[n]);
		}
	}
} /* end getSplineInterpolationCoefficients */

/*------------------------------------------------------------------*/
void putColumn (
	ImageProcessor ip,
	int x,
	double[] column
) {
	int width = ip.getWidth();

	if (ip.getHeight() != column.length) {
		throw new IndexOutOfBoundsException("Incoherent array sizes");
	}
	if (ip.getPixels() instanceof float[]) {
		float[] floatPixels = (float[])ip.getPixels();
		for (int i = 0; (i < column.length); i++) {
			floatPixels[x] = (float)column[i];
			x += width;
		}
	}
	else {
		throw new IllegalArgumentException("Float image required");
	}
} /* end putColumn */

/*------------------------------------------------------------------*/
void putRow (
	ImageProcessor ip,
	int y,
	double[] row
) {
	int rowLength = ip.getWidth();

	if (rowLength != row.length) {
		throw new IndexOutOfBoundsException("Incoherent array sizes");
	}
	y *= rowLength;
	if (ip.getPixels() instanceof float[]) {
		float[] floatPixels = (float[])ip.getPixels();
		for (int i = 0; (i < rowLength); i++) {
			floatPixels[y++] = (float)row[i];
		}
	}
	else {
		throw new IllegalArgumentException("Float image required");
	}
} /* end putRow */

/*------------------------------------------------------------------*/
void resetProgressBar (
) {
	completed = 0;
	lastTime = System.currentTimeMillis();
	progressBar.show(2.0);
} /* end resetProgressBar */

/*------------------------------------------------------------------*/
void setupProgressBar (
) {
	int height = imp.getHeight();
	int width = imp.getWidth();

	completed = 0;
	lastTime = System.currentTimeMillis();
	switch (operation) {
		case GRADIENT_MAGNITUDE:
			processDuration = stackSize * (width + 2 * height);
			break;
		case GRADIENT_DIRECTION:
			processDuration = stackSize * (width + 2 * height);
			break;
		case LAPLACIAN:
			processDuration = stackSize * (width + 2 * height);
			break;
		case LARGEST_HESSIAN:
			processDuration = stackSize * (2 * width + 3 * height);
			break;
		case SMALLEST_HESSIAN:
			processDuration = stackSize * (2 * width + 3 * height);
			break;
		case HESSIAN_ORIENTATION:
			processDuration = stackSize * (2 * width + 3 * height);
			break;
		default:
			throw new IllegalArgumentException("Invalid operation");
	}
} /* end setupProgressBar */

/*------------------------------------------------------------------*/
void stepProgressBar (
) {
	long timeStamp = System.currentTimeMillis();

	completed = completed + 1;
	if (50L < (timeStamp - lastTime)) {
		lastTime = timeStamp;
		progressBar.show((double)completed / (double)processDuration);
	}
} /* end stepProgressBar */

/*------------------------------------------------------------------*/
void symmetricFirMirrorOnBounds (
	double[] h,
	double[] c,
	double[] s
) {
	if (h.length != 2) {
		throw new IndexOutOfBoundsException("The half-length filter size should be 2");
	}
	if (c.length != s.length) {
		throw new IndexOutOfBoundsException("Incompatible size");
	}
	if (2 <= c.length) {
		s[0] = h[0] * c[0] + 2.0 * h[1] * c[1];
		for (int i = 1; (i < (s.length - 1)); i++) {
			s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
		}
		s[s.length - 1] = h[0] * c[c.length - 1] + 2.0 * h[1] * c[c.length - 2];
	}
	else {
		switch (c.length) {
			case 1:
				s[0] = (h[0] + 2.0 * h[1]) * c[0];
				break;
			default:
				throw new NegativeArraySizeException("Invalid length of data");
		}
	}
} /* end symmetricFirMirrorOnBounds */

} /* end class Differentials_ */

/********************************************************************/
/* differentialsDialog												*/
/********************************************************************/
/*------------------------------------------------------------------*/
class differentialsDialog
	extends
		Dialog
	implements
		ActionListener,
		ItemListener

{ /* begin class differentialsDialog */

/*..................................................................*/
/* Global-scope variables											*/
/*..................................................................*/
private CheckboxGroup choice = new CheckboxGroup();
private int operation = Differentials_.LAPLACIAN;
private boolean cancel = false;

/*..................................................................*/
/* Public methods													*/
/*..................................................................*/
/*------------------------------------------------------------------*/
public void actionPerformed (
	ActionEvent ae
) {
	if (ae.getActionCommand().equals("Cancel")) {
		cancel = true;
	}
	else if (ae.getActionCommand().equals("OK")) {
		cancel = false;
	}
	else {
		cancel = true;
		IJ.error("Unexpected button ID");
	}
	setVisible(false);
} /* end actionPerformed */

/*------------------------------------------------------------------*/
differentialsDialog (
	Frame parentWindow,
	String title,
	boolean isModal,
	int operation
) {
	super(parentWindow, title, isModal);
	setLayout(new GridLayout(0, 1));

	Checkbox gradientMagnitude = new Checkbox("Gradient Magnitude", choice, false);
	Checkbox gradientDirection = new Checkbox("Gradient Direction", choice, false);
	Checkbox laplacian = new Checkbox("Laplacian", choice, false);
	Checkbox largestHessian = new Checkbox("Largest Hessian", choice, false);
	Checkbox smallestHessian = new Checkbox("Smallest Hessian", choice, false);
	Checkbox hessianOrientation = new Checkbox("Hessian Orientation", choice, false);
	this.operation = operation;
	switch (operation) {
		case Differentials_.GRADIENT_MAGNITUDE:
			choice.setSelectedCheckbox(gradientMagnitude);
			break;
		case Differentials_.GRADIENT_DIRECTION:
			choice.setSelectedCheckbox(gradientDirection);
			break;
		case Differentials_.LAPLACIAN:
			choice.setSelectedCheckbox(laplacian);
			break;
		case Differentials_.LARGEST_HESSIAN:
			choice.setSelectedCheckbox(largestHessian);
			break;
		case Differentials_.SMALLEST_HESSIAN:
			choice.setSelectedCheckbox(smallestHessian);
			break;
		case Differentials_.HESSIAN_ORIENTATION:
			choice.setSelectedCheckbox(hessianOrientation);
			break;
		default:
			cancel = true;
			IJ.error("Unexpected operation ID");
			setVisible(false);
	}

	gradientMagnitude.addItemListener(this);
	gradientDirection.addItemListener(this);
	laplacian.addItemListener(this);
	largestHessian.addItemListener(this);
	smallestHessian.addItemListener(this);
	hessianOrientation.addItemListener(this);

	Label separation1 = new Label("");
	Label separation2 = new Label("");
	Label separation3 = new Label("");
	Label separation4 = new Label("");

	Panel buttonPanel = new Panel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	Button cancelButton = new Button("Cancel");
	Button okButton = new Button("OK");
	cancelButton.addActionListener(this);
	okButton.addActionListener(this);
	buttonPanel.add(cancelButton);
	buttonPanel.add(okButton);

	add(separation1);
	add(gradientMagnitude);
	add(gradientDirection);
	add(separation2);
	add(laplacian);
	add(separation3);
	add(largestHessian);
	add(smallestHessian);
	add(hessianOrientation);
	add(separation4);
	add(buttonPanel);

	pack();
} /* end differentialsDialog */

/*------------------------------------------------------------------*/
public boolean getCancel (
) {
	return(cancel);
} /* end getCancel */

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*------------------------------------------------------------------*/
public int getOperation (
) {
	return(operation);
} /* end getOperation */

/*------------------------------------------------------------------*/
public void itemStateChanged (
	ItemEvent ie
) {
	if (choice.getSelectedCheckbox().getLabel().equals("Gradient Magnitude")) {
		operation = Differentials_.GRADIENT_MAGNITUDE;
	}
	else if (choice.getSelectedCheckbox().getLabel().equals("Gradient Direction")) {
		operation = Differentials_.GRADIENT_DIRECTION;
	}
	else if (choice.getSelectedCheckbox().getLabel().equals("Laplacian")) {
		operation = Differentials_.LAPLACIAN;
	}
	else if (choice.getSelectedCheckbox().getLabel().equals("Largest Hessian")) {
		operation = Differentials_.LARGEST_HESSIAN;
	}
	else if (choice.getSelectedCheckbox().getLabel().equals("Smallest Hessian")) {
		operation = Differentials_.SMALLEST_HESSIAN;
	}
	else if (choice.getSelectedCheckbox().getLabel().equals("Hessian Orientation")) {
		operation = Differentials_.HESSIAN_ORIENTATION;
	}
	else {
		cancel = true;
		operation = Differentials_.LAPLACIAN;
		IJ.error("Unexpected checkbox ID");
		this.setVisible(false);
	}
	repaint();
} /* end itemStateChanged */

} /* end class differentialsDialog */
