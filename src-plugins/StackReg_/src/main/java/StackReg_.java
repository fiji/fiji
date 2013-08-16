/*====================================================================	
| Version: March 2, 2005
\===================================================================*/

/*====================================================================
| EPFL/STI/IOA/LIB
| Philippe Thevenaz
| Bldg. BM-Ecublens 4.137
| Station 17
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

/*====================================================================
| This work is based on the following paper:
|
| P. Thevenaz, U.E. Ruttimann, M. Unser
| A Pyramid Approach to Subpixel Registration Based on Intensity
| IEEE Transactions on Image Processing
| vol. 7, no. 1, pp. 27-41, January 1998.
|
| This paper is available on-line at
| http://bigwww.epfl.ch/publications/thevenaz9801.html
|
| Other relevant on-line publications are available at
| http://bigwww.epfl.ch/publications/
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/thevenaz/stackreg/
| Ancillary TurboReg_ plugin available at: http://bigwww.epfl.ch/thevenaz/turboreg/
|
| You'll be free to use this software for research purposes, but you
| should not redistribute it without our consent. In addition, we expect
| you to include a citation or acknowledgment whenever you present or
| publish results that are based on it.
\===================================================================*/

// ImageJ
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ShortProcessor;

// Java 1.1
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.IndexColorModel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*====================================================================
|	StackReg_
\===================================================================*/

/********************************************************************/
public class StackReg_
	implements
		PlugIn

{ /* begin class StackReg_ */

/*....................................................................
	Private global variables
....................................................................*/
private static final double TINY = (double)Float.intBitsToFloat((int)0x33FFFFFF);

/*....................................................................
	Public methods
....................................................................*/

/********************************************************************/
public void run (
	final String arg
) {
	Runtime.getRuntime().gc();
	final ImagePlus imp = WindowManager.getCurrentImage();
	if (imp == null) {
		IJ.error("No image available");
		return;
	}
	if (imp.getStack().isRGB() || imp.getStack().isHSB()) {
		IJ.error("Unable to process either RGB or HSB stacks");
		return;
	}
	GenericDialog gd = new GenericDialog("StackReg");
	final String[] transformationItem = {
		"Translation",
		"Rigid Body",
		"Scaled Rotation",
		"Affine"
	};
	gd.addChoice("Transformation:", transformationItem, "Rigid Body");
	gd.addCheckbox("Credits", false);
	gd.showDialog();
	if (gd.wasCanceled()) {
		return;
	}
	final int transformation = gd.getNextChoiceIndex();
	if (gd.getNextBoolean()) {
		final stackRegCredits dialog = new stackRegCredits(IJ.getInstance());
		GUI.center(dialog);
		dialog.setVisible(true);
		return;
	}
	final int width = imp.getWidth();
	final int height = imp.getHeight();
	final int targetSlice = imp.getCurrentSlice();
	double[][] globalTransform = {
		{1.0, 0.0, 0.0},
		{0.0, 1.0, 0.0},
		{0.0, 0.0, 1.0}
	};
	double[][] anchorPoints = null;
	switch (transformation) {
		case 0: {
			anchorPoints = new double[1][3];
			anchorPoints[0][0] = (double)(width / 2);
			anchorPoints[0][1] = (double)(height / 2);
			anchorPoints[0][2] = 1.0;
			break;
		}
		case 1: {
			anchorPoints = new double[3][3];
			anchorPoints[0][0] = (double)(width / 2);
			anchorPoints[0][1] = (double)(height / 2);
			anchorPoints[0][2] = 1.0;
			anchorPoints[1][0] = (double)(width / 2);
			anchorPoints[1][1] = (double)(height / 4);
			anchorPoints[1][2] = 1.0;
			anchorPoints[2][0] = (double)(width / 2);
			anchorPoints[2][1] = (double)((3 * height) / 4);
			anchorPoints[2][2] = 1.0;
			break;
		}
		case 2: {
			anchorPoints = new double[2][3];
			anchorPoints[0][0] = (double)(width / 4);
			anchorPoints[0][1] = (double)(height / 2);
			anchorPoints[0][2] = 1.0;
			anchorPoints[1][0] = (double)((3 * width) / 4);
			anchorPoints[1][1] = (double)(height / 2);
			anchorPoints[1][2] = 1.0;
			break;
		}
		case 3: {
			anchorPoints = new double[3][3];
			anchorPoints[0][0] = (double)(width / 2);
			anchorPoints[0][1] = (double)(height / 4);
			anchorPoints[0][2] = 1.0;
			anchorPoints[1][0] = (double)(width / 4);
			anchorPoints[1][1] = (double)((3 * height) / 4);
			anchorPoints[1][2] = 1.0;
			anchorPoints[2][0] = (double)((3 * width) / 4);
			anchorPoints[2][1] = (double)((3 * height) / 4);
			anchorPoints[2][2] = 1.0;
			break;
		}
		default: {
			IJ.error("Unexpected transformation");
			return;
		}
	}
	ImagePlus source = null;
	ImagePlus target = null;
	double[] colorWeights = null;
	switch (imp.getType()) {
		case ImagePlus.COLOR_256:
		case ImagePlus.COLOR_RGB: {
			colorWeights = getColorWeightsFromPrincipalComponents(imp);
			imp.setSlice(targetSlice);
			target = getGray32("StackRegTarget", imp, colorWeights);
			break;
		}
		case ImagePlus.GRAY8: {
			target = new ImagePlus("StackRegTarget",
				new ByteProcessor(width, height, new byte[width * height],
				imp.getProcessor().getColorModel()));
			target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
			break;
		}
		case ImagePlus.GRAY16: {
			target = new ImagePlus("StackRegTarget",
				new ShortProcessor(width, height, new short[width * height],
				imp.getProcessor().getColorModel()));
			target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
			break;
		}
		case ImagePlus.GRAY32: {
			target = new ImagePlus("StackRegTarget",
				new FloatProcessor(width, height, new float[width * height],
				imp.getProcessor().getColorModel()));
			target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
			break;
		}
		default: {
			IJ.error("Unexpected image type");
			return;
		}
	}
	for (int s = targetSlice - 1; (0 < s); s--) {
		source = registerSlice(source, target, imp, width, height,
			transformation, globalTransform, anchorPoints, colorWeights, s);
		if (source == null) {
			imp.setSlice(targetSlice);
			return;
		}
	}
	if ((1 < targetSlice) && (targetSlice < imp.getStackSize())) {
		globalTransform[0][0] = 1.0;
		globalTransform[0][1] = 0.0;
		globalTransform[0][2] = 0.0;
		globalTransform[1][0] = 0.0;
		globalTransform[1][1] = 1.0;
		globalTransform[1][2] = 0.0;
		globalTransform[2][0] = 0.0;
		globalTransform[2][1] = 0.0;
		globalTransform[2][2] = 1.0;
		imp.setSlice(targetSlice);
		switch (imp.getType()) {
			case ImagePlus.COLOR_256:
			case ImagePlus.COLOR_RGB: {
				target = getGray32("StackRegTarget", imp, colorWeights);
				break;
			}
			case ImagePlus.GRAY8:
			case ImagePlus.GRAY16:
			case ImagePlus.GRAY32: {
				target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
				break;
			}
			default: {
				IJ.error("Unexpected image type");
				return;
			}
		}
	}
	for (int s = targetSlice + 1; (s <= imp.getStackSize()); s++) {
		source = registerSlice(source, target, imp, width, height,
			transformation, globalTransform, anchorPoints, colorWeights, s);
		if (source == null) {
			imp.setSlice(targetSlice);
			return;
		}
	}
	imp.setSlice(targetSlice);
	imp.updateAndDraw();
} /* end run */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void computeStatistics (
	final ImagePlus imp,
	final double[] average,
	final double[][] scatterMatrix
) {
	int length = imp.getWidth() * imp.getHeight();
	double r;
	double g;
	double b;
	if (imp.getProcessor().getPixels() instanceof byte[]) {
		final IndexColorModel icm = (IndexColorModel)imp.getProcessor().getColorModel();
		final int mapSize = icm.getMapSize();
		final byte[] reds = new byte[mapSize];
		final byte[] greens = new byte[mapSize];
		final byte[] blues = new byte[mapSize];	
		icm.getReds(reds); 
		icm.getGreens(greens); 
		icm.getBlues(blues);
		final double[] histogram = new double[mapSize];
		for (int k = 0; (k < mapSize); k++) {
			histogram[k] = 0.0;
		}
		for (int s = 1; (s <= imp.getStackSize()); s++) {
			imp.setSlice(s);
			final byte[] pixels = (byte[])imp.getProcessor().getPixels();
			for (int k = 0; (k < length); k++) {
				histogram[pixels[k] & 0xFF]++;
			}
		}
		for (int k = 0; (k < mapSize); k++) {
			r = (double)(reds[k] & 0xFF);
			g = (double)(greens[k] & 0xFF);
			b = (double)(blues[k] & 0xFF);
			average[0] += histogram[k] * r;
			average[1] += histogram[k] * g;
			average[2] += histogram[k] * b;
			scatterMatrix[0][0] += histogram[k] * r * r;
			scatterMatrix[0][1] += histogram[k] * r * g;
			scatterMatrix[0][2] += histogram[k] * r * b;
			scatterMatrix[1][1] += histogram[k] * g * g;
			scatterMatrix[1][2] += histogram[k] * g * b;
			scatterMatrix[2][2] += histogram[k] * b * b;
		}
	}
	else if (imp.getProcessor().getPixels() instanceof int[]) {
		for (int s = 1; (s <= imp.getStackSize()); s++) {
			imp.setSlice(s);
			final int[] pixels = (int[])imp.getProcessor().getPixels();
			for (int k = 0; (k < length); k++) {
				r = (double)((pixels[k] & 0x00FF0000) >>> 16);
				g = (double)((pixels[k] & 0x0000FF00) >>> 8);
				b = (double)(pixels[k] & 0x000000FF);
				average[0] += r;
				average[1] += g;
				average[2] += b;
				scatterMatrix[0][0] += r * r;
				scatterMatrix[0][1] += r * g;
				scatterMatrix[0][2] += r * b;
				scatterMatrix[1][1] += g * g;
				scatterMatrix[1][2] += g * b;
				scatterMatrix[2][2] += b * b;
			}
		}
	}
	else {
		IJ.error("Internal type mismatch");
	}
	length *= imp.getStackSize();
	average[0] /= (double)length;
	average[1] /= (double)length;
	average[2] /= (double)length;
	scatterMatrix[0][0] /= (double)length;
	scatterMatrix[0][1] /= (double)length;
	scatterMatrix[0][2] /= (double)length;
	scatterMatrix[1][1] /= (double)length;
	scatterMatrix[1][2] /= (double)length;
	scatterMatrix[2][2] /= (double)length;
	scatterMatrix[0][0] -= average[0] * average[0];
	scatterMatrix[0][1] -= average[0] * average[1];
	scatterMatrix[0][2] -= average[0] * average[2];
	scatterMatrix[1][1] -= average[1] * average[1];
	scatterMatrix[1][2] -= average[1] * average[2];
	scatterMatrix[2][2] -= average[2] * average[2];
	scatterMatrix[2][1] = scatterMatrix[1][2];
	scatterMatrix[2][0] = scatterMatrix[0][2];
	scatterMatrix[1][0] = scatterMatrix[0][1];
} /* computeStatistics */

/*------------------------------------------------------------------*/
private double[] getColorWeightsFromPrincipalComponents (
	final ImagePlus imp
) {
	final double[] average = {0.0, 0.0, 0.0};
	final double[][] scatterMatrix = {{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}};
	computeStatistics(imp, average, scatterMatrix);
	double[] eigenvalue = getEigenvalues(scatterMatrix);
	if ((eigenvalue[0] * eigenvalue[0] + eigenvalue[1] * eigenvalue[1]
		+ eigenvalue[2] * eigenvalue[2]) <= TINY) {
		return(getLuminanceFromCCIR601());
	}
	double bestEigenvalue = getLargestAbsoluteEigenvalue(eigenvalue);
	double eigenvector[] = getEigenvector(scatterMatrix, bestEigenvalue);
	final double weight = eigenvector[0] + eigenvector[1] + eigenvector[2];
	if (TINY < Math.abs(weight)) {
		eigenvector[0] /= weight;
		eigenvector[1] /= weight;
		eigenvector[2] /= weight;
	}
	return(eigenvector);
} /* getColorWeightsFromPrincipalComponents */

/*------------------------------------------------------------------*/
private double[] getEigenvalues (
	final double[][] scatterMatrix
) {
	final double[] a = {
		scatterMatrix[0][0] * scatterMatrix[1][1] * scatterMatrix[2][2]
			+ 2.0 * scatterMatrix[0][1] * scatterMatrix[1][2] * scatterMatrix[2][0]
			- scatterMatrix[0][1] * scatterMatrix[0][1] * scatterMatrix[2][2]
			- scatterMatrix[1][2] * scatterMatrix[1][2] * scatterMatrix[0][0]
			- scatterMatrix[2][0] * scatterMatrix[2][0] * scatterMatrix[1][1],
		scatterMatrix[0][1] * scatterMatrix[0][1]
			+ scatterMatrix[1][2] * scatterMatrix[1][2]
			+ scatterMatrix[2][0] * scatterMatrix[2][0]
			- scatterMatrix[0][0] * scatterMatrix[1][1]
			- scatterMatrix[1][1] * scatterMatrix[2][2]
			- scatterMatrix[2][2] * scatterMatrix[0][0],
		scatterMatrix[0][0] + scatterMatrix[1][1] + scatterMatrix[2][2],
		-1.0
	};
	double[] RealRoot = new double[3];
	double Q = (3.0 * a[1] - a[2] * a[2] / a[3]) / (9.0 * a[3]);
	double R = (a[1] * a[2] - 3.0 * a[0] * a[3] - (2.0 / 9.0) * a[2] * a[2] * a[2] / a[3])
		/ (6.0 * a[3] * a[3]);
	double Det = Q * Q * Q + R * R;
	if (Det < 0.0) {
		Det = 2.0 * Math.sqrt(-Q);
		R /= Math.sqrt(-Q * Q * Q);
		R = (1.0 / 3.0) * Math.acos(R);
		Q = (1.0 / 3.0) * a[2] / a[3];
		RealRoot[0] = Det * Math.cos(R) - Q;
		RealRoot[1] = Det * Math.cos(R + (2.0 / 3.0) * Math.PI) - Q;
		RealRoot[2] = Det * Math.cos(R + (4.0 / 3.0) * Math.PI) - Q;
		if (RealRoot[0] < RealRoot[1]) {
			if (RealRoot[2] < RealRoot[1]) {
				double Swap = RealRoot[1];
				RealRoot[1] = RealRoot[2];
				RealRoot[2] = Swap;
				if (RealRoot[1] < RealRoot[0]) {
					Swap = RealRoot[0];
					RealRoot[0] = RealRoot[1];
					RealRoot[1] = Swap;
				}
			}
		}
		else {
			double Swap = RealRoot[0];
			RealRoot[0] = RealRoot[1];
			RealRoot[1] = Swap;
			if (RealRoot[2] < RealRoot[1]) {
				Swap = RealRoot[1];
				RealRoot[1] = RealRoot[2];
				RealRoot[2] = Swap;
				if (RealRoot[1] < RealRoot[0]) {
					Swap = RealRoot[0];
					RealRoot[0] = RealRoot[1];
					RealRoot[1] = Swap;
				}
			}
		}
	}
	else if (Det == 0.0) {
		final double P = 2.0 * ((R < 0.0) ? (Math.pow(-R, 1.0 / 3.0)) : (Math.pow(R, 1.0 / 3.0)));
		Q = (1.0 / 3.0) * a[2] / a[3];
		if (P < 0) {
			RealRoot[0] = P - Q;
			RealRoot[1] = -0.5 * P - Q;
			RealRoot[2] = RealRoot[1];
		}
		else {
			RealRoot[0] = -0.5 * P - Q;
			RealRoot[1] = RealRoot[0];
			RealRoot[2] = P - Q;
		}
	}
	else {
		IJ.error("Warning: complex eigenvalue found; ignoring imaginary part.");
		Det = Math.sqrt(Det);
		Q = ((R + Det) < 0.0) ? (-Math.exp((1.0 / 3.0) * Math.log(-R - Det)))
			: (Math.exp((1.0 / 3.0) * Math.log(R + Det)));
		R = Q + ((R < Det) ? (-Math.exp((1.0 / 3.0) * Math.log(Det - R)))
			: (Math.exp((1.0 / 3.0) * Math.log(R - Det))));
		Q = (-1.0 / 3.0) * a[2] / a[3];
		Det = Q + R;
		RealRoot[0] = Q - R / 2.0;
		RealRoot[1] = RealRoot[0];
		RealRoot[2] = RealRoot[1];
		if (Det < RealRoot[0]) {
			RealRoot[0] = Det;
		}
		else {
			RealRoot[2] = Det;
		}
	}
	return(RealRoot);
} /* end getEigenvalues */

/*------------------------------------------------------------------*/
private double[] getEigenvector (
	final double[][] scatterMatrix,
	final double eigenvalue
) {
	final int n = scatterMatrix.length;
	final double[][] matrix = new double[n][n];
	for (int i = 0; (i < n); i++) {
		System.arraycopy(scatterMatrix[i], 0, matrix[i], 0, n);
		matrix[i][i] -= eigenvalue;
	}
	final double[] eigenvector = new double[n];
	double absMax;
	double max;
	double norm;
	for (int i = 0; (i < n); i++) {
		norm = 0.0;
		for (int j = 0; (j < n); j++) {
			norm += matrix[i][j] * matrix[i][j];
		}
		norm = Math.sqrt(norm);
		if (TINY < norm) {
			for (int j = 0; (j < n); j++) {
				matrix[i][j] /= norm;
			}
		}
	}
	for (int j = 0; (j < n); j++) {
		max = matrix[j][j];
		absMax = Math.abs(max);
		int k = j;
		for (int i = j + 1; (i < n); i++) {
			if (absMax < Math.abs(matrix[i][j])) {
				max = matrix[i][j];
				absMax = Math.abs(max);
				k = i;
			}
		}
		if (k != j) {
			final double[] partialLine = new double[n - j];
			System.arraycopy(matrix[j], j, partialLine, 0, n - j);
			System.arraycopy(matrix[k], j, matrix[j], j, n - j);
			System.arraycopy(partialLine, 0, matrix[k], j, n - j);
		}
		if (TINY < absMax) {
			for (k = 0; (k < n); k++) {
				matrix[j][k] /= max;
			}
		}
		for (int i = j + 1; (i < n); i++) {
			max = matrix[i][j];
			for (k = 0; (k < n); k++) {
				matrix[i][k] -= max * matrix[j][k];
			}
		}
	}
	final boolean[] ignore = new boolean[n];
	int valid = n;
	for (int i = 0; (i < n); i++) {
		ignore[i] = false;
		if (Math.abs(matrix[i][i]) < TINY) {
			ignore[i] = true;
			valid--;
			eigenvector[i] = 1.0;
			continue;
		}
		if (TINY < Math.abs(matrix[i][i] - 1.0)) {
			IJ.error("Insufficient accuracy.");
			eigenvector[0] = 0.212671;
			eigenvector[1] = 0.71516;
			eigenvector[2] = 0.072169;
			return(eigenvector);
		}
		norm = 0.0;
		for (int j = 0; (j < i); j++) {
			norm += matrix[i][j] * matrix[i][j];
		}
		for (int j = i + 1; (j < n); j++) {
			norm += matrix[i][j] * matrix[i][j];
		}
		if (Math.sqrt(norm) < TINY) {
			ignore[i] = true;
			valid--;
			eigenvector[i] = 0.0;
			continue;
		}
	}
	if (0 < valid) {
		double[][] reducedMatrix = new double[valid][valid];
		for (int i = 0, u = 0; (i < n); i++) {
			if (!ignore[i]) {
				for (int j = 0, v = 0; (j < n); j++) {
					if (!ignore[j]) {
						reducedMatrix[u][v] = matrix[i][j];
						v++;
					}
				}
				u++;
			}
		}
		double[] reducedEigenvector = new double[valid];
		for (int i = 0, u = 0; (i < n); i++) {
			if (!ignore[i]) {
				for (int j = 0; (j < n); j++) {
					if (ignore[j]) {
						reducedEigenvector[u] -= matrix[i][j] * eigenvector[j];
					}
				}
				u++;
			}
		}
		reducedEigenvector = linearLeastSquares(reducedMatrix, reducedEigenvector);
		for (int i = 0, u = 0; (i < n); i++) {
			if (!ignore[i]) {
				eigenvector[i] = reducedEigenvector[u];
				u++;
			}
		}
	}
	norm = 0.0;
	for (int i = 0; (i < n); i++) {
		norm += eigenvector[i] * eigenvector[i];
	}
	norm = Math.sqrt(norm);
	if (Math.sqrt(norm) < TINY) {
		IJ.error("Insufficient accuracy.");
		eigenvector[0] = 0.212671;
		eigenvector[1] = 0.71516;
		eigenvector[2] = 0.072169;
		return(eigenvector);
	}
	absMax = Math.abs(eigenvector[0]);
	valid = 0;
	for (int i = 1; (i < n); i++) {
		max = Math.abs(eigenvector[i]);
		if (absMax < max) {
			absMax = max;
			valid = i;
		}
	}
	norm = (eigenvector[valid] < 0.0) ? (-norm) : (norm);
	for (int i = 0; (i < n); i++) {
		eigenvector[i] /= norm;
	}
	return(eigenvector);
} /* getEigenvector */

/*------------------------------------------------------------------*/
private ImagePlus getGray32 (
	final String title,
	final ImagePlus imp,
	final double[] colorWeights
) {
	final int length = imp.getWidth() * imp.getHeight();
	final ImagePlus gray32 = new ImagePlus(title,
		new FloatProcessor(imp.getWidth(), imp.getHeight()));
	final float[] gray = (float[])gray32.getProcessor().getPixels();
	double r;
	double g;
	double b;
	if (imp.getProcessor().getPixels() instanceof byte[]) {
		final byte[] pixels = (byte[])imp.getProcessor().getPixels();
		final IndexColorModel icm = (IndexColorModel)imp.getProcessor().getColorModel();
		final int mapSize = icm.getMapSize();
		final byte[] reds = new byte[mapSize];
		final byte[] greens = new byte[mapSize];
		final byte[] blues = new byte[mapSize];	
		icm.getReds(reds); 
		icm.getGreens(greens); 
		icm.getBlues(blues);
		int index;
		for (int k = 0; (k < length); k++) {
			index = (int)(pixels[k] & 0xFF);
			r = (double)(reds[index] & 0xFF);
			g = (double)(greens[index] & 0xFF);
			b = (double)(blues[index] & 0xFF);
			gray[k] = (float)(colorWeights[0] * r + colorWeights[1] * g + colorWeights[2] * b);
		}
	}
	else if (imp.getProcessor().getPixels() instanceof int[]) {
		final int[] pixels = (int[])imp.getProcessor().getPixels();
		for (int k = 0; (k < length); k++) {
			r = (double)((pixels[k] & 0x00FF0000) >>> 16);
			g = (double)((pixels[k] & 0x0000FF00) >>> 8);
			b = (double)(pixels[k] & 0x000000FF);
			gray[k] = (float)(colorWeights[0] * r + colorWeights[1] * g + colorWeights[2] * b);
		}
	}
	return(gray32);
} /* getGray32 */

/*------------------------------------------------------------------*/
private double getLargestAbsoluteEigenvalue (
	final double[] eigenvalue
) {
	double best = eigenvalue[0];
	for (int k = 1; (k < eigenvalue.length); k++) {
		if (Math.abs(best) < Math.abs(eigenvalue[k])) {
			best = eigenvalue[k];
		}
		if (Math.abs(best) == Math.abs(eigenvalue[k])) {
			if (best < eigenvalue[k]) {
				best = eigenvalue[k];
			}
		}
	}
	return(best);
} /* getLargestAbsoluteEigenvalue */

/*------------------------------------------------------------------*/
private double[] getLuminanceFromCCIR601 (
) {
	double[] weights = {0.299, 0.587, 0.114};
	return(weights);
} /* getLuminanceFromCCIR601 */

/*------------------------------------------------------------------*/
private double[][] getTransformationMatrix (
	final double[][] fromCoord,
	final double[][] toCoord,
	final int transformation
) {
	double[][] matrix = new double[3][3];
	switch (transformation) {
		case 0: {
			matrix[0][0] = 1.0;
			matrix[0][1] = 0.0;
			matrix[0][2] = toCoord[0][0] - fromCoord[0][0];
			matrix[1][0] = 0.0;
			matrix[1][1] = 1.0;
			matrix[1][2] = toCoord[0][1] - fromCoord[0][1];
			break;
		}
		case 1: {
			final double angle = Math.atan2(fromCoord[2][0] - fromCoord[1][0],
				fromCoord[2][1] - fromCoord[1][1]) - Math.atan2(toCoord[2][0] - toCoord[1][0],
				toCoord[2][1] - toCoord[1][1]);
			final double c = Math.cos(angle);
			final double s = Math.sin(angle);
			matrix[0][0] = c;
			matrix[0][1] = -s;
			matrix[0][2] = toCoord[0][0] - c * fromCoord[0][0] + s * fromCoord[0][1];
			matrix[1][0] = s;
			matrix[1][1] = c;
			matrix[1][2] = toCoord[0][1] - s * fromCoord[0][0] - c * fromCoord[0][1];
			break;
		}
		case 2: {
			double[][] a = new double[3][3];
			double[] v = new double[3];
			a[0][0] = fromCoord[0][0];
			a[0][1] = fromCoord[0][1];
			a[0][2] = 1.0;
			a[1][0] = fromCoord[1][0];
			a[1][1] = fromCoord[1][1];
			a[1][2] = 1.0;
			a[2][0] = fromCoord[0][1] - fromCoord[1][1] + fromCoord[1][0];
			a[2][1] = fromCoord[1][0] + fromCoord[1][1] - fromCoord[0][0];
			a[2][2] = 1.0;
			invertGauss(a);
			v[0] = toCoord[0][0];
			v[1] = toCoord[1][0];
			v[2] = toCoord[0][1] - toCoord[1][1] + toCoord[1][0];
			for (int i = 0; (i < 3); i++) {
				matrix[0][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[0][i] += a[i][j] * v[j];
				}
			}
			v[0] = toCoord[0][1];
			v[1] = toCoord[1][1];
			v[2] = toCoord[1][0] + toCoord[1][1] - toCoord[0][0];
			for (int i = 0; (i < 3); i++) {
				matrix[1][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[1][i] += a[i][j] * v[j];
				}
			}
			break;
		}
		case 3: {
			double[][] a = new double[3][3];
			double[] v = new double[3];
			a[0][0] = fromCoord[0][0];
			a[0][1] = fromCoord[0][1];
			a[0][2] = 1.0;
			a[1][0] = fromCoord[1][0];
			a[1][1] = fromCoord[1][1];
			a[1][2] = 1.0;
			a[2][0] = fromCoord[2][0];
			a[2][1] = fromCoord[2][1];
			a[2][2] = 1.0;
			invertGauss(a);
			v[0] = toCoord[0][0];
			v[1] = toCoord[1][0];
			v[2] = toCoord[2][0];
			for (int i = 0; (i < 3); i++) {
				matrix[0][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[0][i] += a[i][j] * v[j];
				}
			}
			v[0] = toCoord[0][1];
			v[1] = toCoord[1][1];
			v[2] = toCoord[2][1];
			for (int i = 0; (i < 3); i++) {
				matrix[1][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[1][i] += a[i][j] * v[j];
				}
			}
			break;
		}
		default: {
			IJ.error("Unexpected transformation");
		}
	}
	matrix[2][0] = 0.0;
	matrix[2][1] = 0.0;
	matrix[2][2] = 1.0;
	return(matrix);
} /* end getTransformationMatrix */

/*------------------------------------------------------------------*/
private void invertGauss (
	final double[][] matrix
) {
	final int n = matrix.length;
	final double[][] inverse = new double[n][n];
	for (int i = 0; (i < n); i++) {
		double max = matrix[i][0];
		double absMax = Math.abs(max);
		for (int j = 0; (j < n); j++) {
			inverse[i][j] = 0.0;
			if (absMax < Math.abs(matrix[i][j])) {
				max = matrix[i][j];
				absMax = Math.abs(max);
			}
		}
		inverse[i][i] = 1.0 / max;
		for (int j = 0; (j < n); j++) {
			matrix[i][j] /= max;
		}
	}
	for (int j = 0; (j < n); j++) {
		double max = matrix[j][j];
		double absMax = Math.abs(max);
		int k = j;
		for (int i = j + 1; (i < n); i++) {
			if (absMax < Math.abs(matrix[i][j])) {
				max = matrix[i][j];
				absMax = Math.abs(max);
				k = i;
			}
		}
		if (k != j) {
			final double[] partialLine = new double[n - j];
			final double[] fullLine = new double[n];
			System.arraycopy(matrix[j], j, partialLine, 0, n - j);
			System.arraycopy(matrix[k], j, matrix[j], j, n - j);
			System.arraycopy(partialLine, 0, matrix[k], j, n - j);
			System.arraycopy(inverse[j], 0, fullLine, 0, n);
			System.arraycopy(inverse[k], 0, inverse[j], 0, n);
			System.arraycopy(fullLine, 0, inverse[k], 0, n);
		}
		for (k = 0; (k <= j); k++) {
			inverse[j][k] /= max;
		}
		for (k = j + 1; (k < n); k++) {
			matrix[j][k] /= max;
			inverse[j][k] /= max;
		}
		for (int i = j + 1; (i < n); i++) {
			for (k = 0; (k <= j); k++) {
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
			for (k = j + 1; (k < n); k++) {
				matrix[i][k] -= matrix[i][j] * matrix[j][k];
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
		}
	}
	for (int j = n - 1; (1 <= j); j--) {
		for (int i = j - 1; (0 <= i); i--) {
			for (int k = 0; (k <= j); k++) {
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
			for (int k = j + 1; (k < n); k++) {
				matrix[i][k] -= matrix[i][j] * matrix[j][k];
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
		}
	}
	for (int i = 0; (i < n); i++) {
		System.arraycopy(inverse[i], 0, matrix[i], 0, n);
	}
} /* end invertGauss */

/*------------------------------------------------------------------*/
private double[] linearLeastSquares (
	final double[][] A,
	final double[] b
) {
	final int lines = A.length;
	final int columns = A[0].length;
	final double[][] Q = new double[lines][columns];
	final double[][] R = new double[columns][columns];
	final double[] x = new double[columns];
	double s;
	for (int i = 0; (i < lines); i++) {
		for (int j = 0; (j < columns); j++) {
			Q[i][j] = A[i][j];
		}
	}
	QRdecomposition(Q, R);
	for (int i = 0; (i < columns); i++) {
		s = 0.0;
		for (int j = 0; (j < lines); j++) {
			s += Q[j][i] * b[j];
		}
		x[i] = s;
	}
	for (int i = columns - 1; (0 <= i); i--) {
		s = R[i][i];
		if ((s * s) == 0.0) {
			x[i] = 0.0;
		}
		else {
			x[i] /= s;
		}
		for (int j = i - 1; (0 <= j); j--) {
			x[j] -= R[j][i] * x[i];
		}
	}
	return(x);
} /* end linearLeastSquares */

/*------------------------------------------------------------------*/
private void QRdecomposition (
	final double[][] Q,
	final double[][] R
) {
	final int lines = Q.length;
	final int columns = Q[0].length;
	final double[][] A = new double[lines][columns];
	double s;
	for (int j = 0; (j < columns); j++) {
		for (int i = 0; (i < lines); i++) {
			A[i][j] = Q[i][j];
		}
		for (int k = 0; (k < j); k++) {
			s = 0.0;
			for (int i = 0; (i < lines); i++) {
				s += A[i][j] * Q[i][k];
			}
			for (int i = 0; (i < lines); i++) {
				Q[i][j] -= s * Q[i][k];
			}
		}
		s = 0.0;
		for (int i = 0; (i < lines); i++) {
			s += Q[i][j] * Q[i][j];
		}
		if ((s * s) == 0.0) {
			s = 0.0;
		}
		else {
			s = 1.0 / Math.sqrt(s);
		}
		for (int i = 0; (i < lines); i++) {
			Q[i][j] *= s;
		}
	}
	for (int i = 0; (i < columns); i++) {
		for (int j = 0; (j < i); j++) {
			R[i][j] = 0.0;
		}
		for (int j = i; (j < columns); j++) {
			R[i][j] = 0.0;
			for (int k = 0; (k < lines); k++) {
				R[i][j] += Q[k][i] * A[k][j];
			}
		}
	}
} /* end QRdecomposition */

/*------------------------------------------------------------------*/
private ImagePlus registerSlice (
	ImagePlus source,
	final ImagePlus target,
	final ImagePlus imp,
	final int width,
	final int height,
	final int transformation,
	final double[][] globalTransform,
	final double[][] anchorPoints,
	final double[] colorWeights,
	final int s
) {
	imp.setSlice(s);
	try {
		Object turboReg = null;
		Method method = null;
		double[][] sourcePoints = null;
		double[][] targetPoints = null;
		double[][] localTransform = null;
		switch (imp.getType()) {
			case ImagePlus.COLOR_256:
			case ImagePlus.COLOR_RGB: {
				source = getGray32("StackRegSource", imp, colorWeights);
				break;
			}
			case ImagePlus.GRAY8: {
				source = new ImagePlus("StackRegSource", new ByteProcessor(
					width, height, (byte[])imp.getProcessor().getPixels(),
					imp.getProcessor().getColorModel()));
				break;
			}
			case ImagePlus.GRAY16: {
				source = new ImagePlus("StackRegSource", new ShortProcessor(
					width, height, (short[])imp.getProcessor().getPixels(),
					imp.getProcessor().getColorModel()));
				break;
			}
			case ImagePlus.GRAY32: {
				source = new ImagePlus("StackRegSource", new FloatProcessor(
					width, height, (float[])imp.getProcessor().getPixels(),
					imp.getProcessor().getColorModel()));
				break;
			}
			default: {
				IJ.error("Unexpected image type");
				return(null);
			}
		}
		final FileSaver sourceFile = new FileSaver(source);
		final String sourcePathAndFileName = IJ.getDirectory("temp") + source.getTitle();
		sourceFile.saveAsTiff(sourcePathAndFileName);
		final FileSaver targetFile = new FileSaver(target);
		final String targetPathAndFileName = IJ.getDirectory("temp") + target.getTitle();
		targetFile.saveAsTiff(targetPathAndFileName);
		switch (transformation) {
			case 0: {
				turboReg = IJ.runPlugIn("TurboReg_", "-align"
					+ " -file " + sourcePathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -file " + targetPathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -translation"
					+ " " + (width / 2) + " " + (height / 2)
					+ " " + (width / 2) + " " + (height / 2)
					+ " -hideOutput"
				);
				break;
			}
			case 1: {
				turboReg = IJ.runPlugIn("TurboReg_", "-align"
					+ " -file " + sourcePathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -file " + targetPathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -rigidBody"
					+ " " + (width / 2) + " " + (height / 2)
					+ " " + (width / 2) + " " + (height / 2)
					+ " " + (width / 2) + " " + (height / 4)
					+ " " + (width / 2) + " " + (height / 4)
					+ " " + (width / 2) + " " + ((3 * height) / 4)
					+ " " + (width / 2) + " " + ((3 * height) / 4)
					+ " -hideOutput"
				);
				break;
			}
			case 2: {
				turboReg = IJ.runPlugIn("TurboReg_", "-align"
					+ " -file " + sourcePathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -file " + targetPathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -scaledRotation"
					+ " " + (width / 4) + " " + (height / 2)
					+ " " + (width / 4) + " " + (height / 2)
					+ " " + ((3 * width) / 4) + " " + (height / 2)
					+ " " + ((3 * width) / 4) + " " + (height / 2)
					+ " -hideOutput"
				);
				break;
			}
			case 3: {
				turboReg = IJ.runPlugIn("TurboReg_", "-align"
					+ " -file " + sourcePathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -file " + targetPathAndFileName
					+ " 0 0 " + (width - 1) + " " + (height - 1)
					+ " -affine"
					+ " " + (width / 2) + " " + (height / 4)
					+ " " + (width / 2) + " " + (height / 4)
					+ " " + (width / 4) + " " + ((3 * height) / 4)
					+ " " + (width / 4) + " " + ((3 * height) / 4)
					+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
					+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
					+ " -hideOutput"
				);
				break;
			}
			default: {
				IJ.error("Unexpected transformation");
				return(null);
			}
		}
		if (turboReg == null) {
			throw(new ClassNotFoundException());
		}
		target.setProcessor(null, source.getProcessor());
		method = turboReg.getClass().getMethod("getSourcePoints", null);
		sourcePoints = ((double[][])method.invoke(turboReg, null));
		method = turboReg.getClass().getMethod("getTargetPoints", null);
		targetPoints = ((double[][])method.invoke(turboReg, null));
		localTransform = getTransformationMatrix(targetPoints, sourcePoints,
			transformation);
		double[][] rescued = {
			{globalTransform[0][0], globalTransform[0][1], globalTransform[0][2]},
			{globalTransform[1][0], globalTransform[1][1], globalTransform[1][2]},
			{globalTransform[2][0], globalTransform[2][1], globalTransform[2][2]}
		};
		for (int i = 0; (i < 3); i++) {
			for (int j = 0; (j < 3); j++) {
				globalTransform[i][j] = 0.0;
				for (int k = 0; (k < 3); k++) {
					globalTransform[i][j] += localTransform[i][k] * rescued[k][j];
				}
			}
		}
		switch (imp.getType()) {
			case ImagePlus.COLOR_256: {
				source = new ImagePlus("StackRegSource", new ByteProcessor(
					width, height, (byte[])imp.getProcessor().getPixels(),
					imp.getProcessor().getColorModel()));
				ImageConverter converter = new ImageConverter(source);
				converter.convertToRGB();
				Object turboRegR = null;
				Object turboRegG = null;
				Object turboRegB = null;
				byte[] r = new byte[width * height];
				byte[] g = new byte[width * height];
				byte[] b = new byte[width * height];
				((ColorProcessor)source.getProcessor()).getRGB(r, g, b);
				final ImagePlus sourceR = new ImagePlus("StackRegSourceR",
					new ByteProcessor(width, height));
				final ImagePlus sourceG = new ImagePlus("StackRegSourceG",
					new ByteProcessor(width, height));
				final ImagePlus sourceB = new ImagePlus("StackRegSourceB",
					new ByteProcessor(width, height));
				sourceR.getProcessor().setPixels(r);
				sourceG.getProcessor().setPixels(g);
				sourceB.getProcessor().setPixels(b);
				ImagePlus transformedSourceR = null;
				ImagePlus transformedSourceG = null;
				ImagePlus transformedSourceB = null;
				final FileSaver sourceFileR = new FileSaver(sourceR);
				final String sourcePathAndFileNameR = IJ.getDirectory("temp") + sourceR.getTitle();
				sourceFileR.saveAsTiff(sourcePathAndFileNameR);
				final FileSaver sourceFileG = new FileSaver(sourceG);
				final String sourcePathAndFileNameG = IJ.getDirectory("temp") + sourceG.getTitle();
				sourceFileG.saveAsTiff(sourcePathAndFileNameG);
				final FileSaver sourceFileB = new FileSaver(sourceB);
				final String sourcePathAndFileNameB = IJ.getDirectory("temp") + sourceB.getTitle();
				sourceFileB.saveAsTiff(sourcePathAndFileNameB);
				switch (transformation) {
					case 0: {
						sourcePoints = new double[1][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						break;
					}
					case 1: {
						sourcePoints = new double[3][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							sourcePoints[2][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
								sourcePoints[2][i] += globalTransform[i][j]
									* anchorPoints[2][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						break;
					}
					case 2: {
						sourcePoints = new double[2][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						break;
					}
					case 3: {
						sourcePoints = new double[3][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							sourcePoints[2][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
								sourcePoints[2][i] += globalTransform[i][j]
									* anchorPoints[2][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						break;
					}
					default: {
						IJ.error("Unexpected transformation");
						return(null);
					}
				}
				method = turboRegR.getClass().getMethod("getTransformedImage", null);
				transformedSourceR = (ImagePlus)method.invoke(turboRegR, null);
				method = turboRegG.getClass().getMethod("getTransformedImage", null);
				transformedSourceG = (ImagePlus)method.invoke(turboRegG, null);
				method = turboRegB.getClass().getMethod("getTransformedImage", null);
				transformedSourceB = (ImagePlus)method.invoke(turboRegB, null);
				transformedSourceR.getStack().deleteLastSlice();
				transformedSourceG.getStack().deleteLastSlice();
				transformedSourceB.getStack().deleteLastSlice();
				transformedSourceR.getProcessor().setMinAndMax(0.0, 255.0);
				transformedSourceG.getProcessor().setMinAndMax(0.0, 255.0);
				transformedSourceB.getProcessor().setMinAndMax(0.0, 255.0);
				ImageConverter converterR = new ImageConverter(transformedSourceR);
				ImageConverter converterG = new ImageConverter(transformedSourceG);
				ImageConverter converterB = new ImageConverter(transformedSourceB);
				converterR.convertToGray8();
				converterG.convertToGray8();
				converterB.convertToGray8();
				final IndexColorModel icm = (IndexColorModel)imp.getProcessor().getColorModel();
				final byte[] pixels = (byte[])imp.getProcessor().getPixels();
				r = (byte[])transformedSourceR.getProcessor().getPixels();
				g = (byte[])transformedSourceG.getProcessor().getPixels();
				b = (byte[])transformedSourceB.getProcessor().getPixels();
				final int[] color = new int[4];
				color[3] = 255;
				for (int k = 0; (k < pixels.length); k++) {
					color[0] = (int)(r[k] & 0xFF);
					color[1] = (int)(g[k] & 0xFF);
					color[2] = (int)(b[k] & 0xFF);
					pixels[k] = (byte)icm.getDataElement(color, 0);
				}
				break;
			}
			case ImagePlus.COLOR_RGB: {
				Object turboRegR = null;
				Object turboRegG = null;
				Object turboRegB = null;
				final byte[] r = new byte[width * height];
				final byte[] g = new byte[width * height];
				final byte[] b = new byte[width * height];
				((ColorProcessor)imp.getProcessor()).getRGB(r, g, b);
				final ImagePlus sourceR = new ImagePlus("StackRegSourceR",
					new ByteProcessor(width, height));
				final ImagePlus sourceG = new ImagePlus("StackRegSourceG",
					new ByteProcessor(width, height));
				final ImagePlus sourceB = new ImagePlus("StackRegSourceB",
					new ByteProcessor(width, height));
				sourceR.getProcessor().setPixels(r);
				sourceG.getProcessor().setPixels(g);
				sourceB.getProcessor().setPixels(b);
				ImagePlus transformedSourceR = null;
				ImagePlus transformedSourceG = null;
				ImagePlus transformedSourceB = null;
				final FileSaver sourceFileR = new FileSaver(sourceR);
				final String sourcePathAndFileNameR = IJ.getDirectory("temp") + sourceR.getTitle();
				sourceFileR.saveAsTiff(sourcePathAndFileNameR);
				final FileSaver sourceFileG = new FileSaver(sourceG);
				final String sourcePathAndFileNameG = IJ.getDirectory("temp") + sourceG.getTitle();
				sourceFileG.saveAsTiff(sourcePathAndFileNameG);
				final FileSaver sourceFileB = new FileSaver(sourceB);
				final String sourcePathAndFileNameB = IJ.getDirectory("temp") + sourceB.getTitle();
				sourceFileB.saveAsTiff(sourcePathAndFileNameB);
				switch (transformation) {
					case 0: {
						sourcePoints = new double[1][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						break;
					}
					case 1: {
						sourcePoints = new double[3][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							sourcePoints[2][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
								sourcePoints[2][i] += globalTransform[i][j]
									* anchorPoints[2][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						break;
					}
					case 2: {
						sourcePoints = new double[2][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						break;
					}
					case 3: {
						sourcePoints = new double[3][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							sourcePoints[2][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
								sourcePoints[2][i] += globalTransform[i][j]
									* anchorPoints[2][j];
							}
						}
						turboRegR = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameR
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						if (turboRegR == null) {
							throw(new ClassNotFoundException());
						}
						turboRegG = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameG
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						turboRegB = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileNameB
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						break;
					}
					default: {
						IJ.error("Unexpected transformation");
						return(null);
					}
				}
				method = turboRegR.getClass().getMethod("getTransformedImage", null);
				transformedSourceR = (ImagePlus)method.invoke(turboRegR, null);
				method = turboRegG.getClass().getMethod("getTransformedImage", null);
				transformedSourceG = (ImagePlus)method.invoke(turboRegG, null);
				method = turboRegB.getClass().getMethod("getTransformedImage", null);
				transformedSourceB = (ImagePlus)method.invoke(turboRegB, null);
				transformedSourceR.getStack().deleteLastSlice();
				transformedSourceG.getStack().deleteLastSlice();
				transformedSourceB.getStack().deleteLastSlice();
				transformedSourceR.getProcessor().setMinAndMax(0.0, 255.0);
				transformedSourceG.getProcessor().setMinAndMax(0.0, 255.0);
				transformedSourceB.getProcessor().setMinAndMax(0.0, 255.0);
				ImageConverter converterR = new ImageConverter(transformedSourceR);
				ImageConverter converterG = new ImageConverter(transformedSourceG);
				ImageConverter converterB = new ImageConverter(transformedSourceB);
				converterR.convertToGray8();
				converterG.convertToGray8();
				converterB.convertToGray8();
				((ColorProcessor)imp.getProcessor()).setRGB(
					(byte[])transformedSourceR.getProcessor().getPixels(),
					(byte[])transformedSourceG.getProcessor().getPixels(),
					(byte[])transformedSourceB.getProcessor().getPixels());
				break;
			}
			case ImagePlus.GRAY8:
			case ImagePlus.GRAY16:
			case ImagePlus.GRAY32: {
				switch (transformation) {
					case 0: {
						sourcePoints = new double[1][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
							}
						}
						turboReg = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileName
							+ " " + width + " " + height
							+ " -translation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " -hideOutput"
						);
						break;
					}
					case 1: {
						sourcePoints = new double[3][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							sourcePoints[2][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
								sourcePoints[2][i] += globalTransform[i][j]
									* anchorPoints[2][j];
							}
						}
						turboReg = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileName
							+ " " + width + " " + height
							+ " -rigidBody"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + (width / 2) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						break;
					}
					case 2: {
						sourcePoints = new double[2][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
							}
						}
						turboReg = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileName
							+ " " + width + " " + height
							+ " -scaledRotation"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 4) + " " + (height / 2)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + ((3 * width) / 4) + " " + (height / 2)
							+ " -hideOutput"
						);
						break;
					}
					case 3: {
						sourcePoints = new double[3][3];
						for (int i = 0; (i < 3); i++) {
							sourcePoints[0][i] = 0.0;
							sourcePoints[1][i] = 0.0;
							sourcePoints[2][i] = 0.0;
							for (int j = 0; (j < 3); j++) {
								sourcePoints[0][i] += globalTransform[i][j]
									* anchorPoints[0][j];
								sourcePoints[1][i] += globalTransform[i][j]
									* anchorPoints[1][j];
								sourcePoints[2][i] += globalTransform[i][j]
									* anchorPoints[2][j];
							}
						}
						turboReg = IJ.runPlugIn("TurboReg_", "-transform"
							+ " -file " + sourcePathAndFileName
							+ " " + width + " " + height
							+ " -affine"
							+ " " + sourcePoints[0][0] + " " + sourcePoints[0][1]
							+ " " + (width / 2) + " " + (height / 4)
							+ " " + sourcePoints[1][0] + " " + sourcePoints[1][1]
							+ " " + (width / 4) + " " + ((3 * height) / 4)
							+ " " + sourcePoints[2][0] + " " + sourcePoints[2][1]
							+ " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
							+ " -hideOutput"
						);
						break;
					}
					default: {
						IJ.error("Unexpected transformation");
						return(null);
					}
				}
				if (turboReg == null) {
					throw(new ClassNotFoundException());
				}
				method = turboReg.getClass().getMethod("getTransformedImage", null);
				ImagePlus transformedSource = (ImagePlus)method.invoke(turboReg, null);
				transformedSource.getStack().deleteLastSlice();
				switch (imp.getType()) {
					case ImagePlus.GRAY8: {
						transformedSource.getProcessor().setMinAndMax(0.0, 255.0);
						final ImageConverter converter = new ImageConverter(transformedSource);
						converter.convertToGray8();
						break;
					}
					case ImagePlus.GRAY16: {
						transformedSource.getProcessor().setMinAndMax(0.0, 65535.0);
						final ImageConverter converter = new ImageConverter(transformedSource);
						converter.convertToGray16();
						break;
					}
					case ImagePlus.GRAY32: {
						break;
					}
					default: {
						IJ.error("Unexpected image type");
						return(null);
					}
				}
				imp.setProcessor(null, transformedSource.getProcessor());
				break;
			}
			default: {
				IJ.error("Unexpected image type");
				return(null);
			}
		}
	} catch (NoSuchMethodException e) {
		IJ.error("Unexpected NoSuchMethodException " + e);
		return(null);
	} catch (IllegalAccessException e) {
		IJ.error("Unexpected IllegalAccessException " + e);
		return(null);
	} catch (InvocationTargetException e) {
		IJ.error("Unexpected InvocationTargetException " + e);
		return(null);
	} catch (ClassNotFoundException e) {
		IJ.error("Please download TurboReg_ from\nhttp://bigwww.epfl.ch/thevenaz/turboreg/");
		return(null);
	}
	return(source);
} /* end registerSlice */

} /* end class StackReg_ */

/*====================================================================
|	stackRegCredits
\===================================================================*/

/********************************************************************/
class stackRegCredits
	extends
		Dialog

{ /* begin class stackRegCredits */

/*....................................................................
	Public methods
....................................................................*/

/********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/********************************************************************/
public stackRegCredits (
	final Frame parentWindow
) {
	super(parentWindow, "StackReg", true);
	setLayout(new BorderLayout(0, 20));
	final Label separation = new Label("");
	final Panel buttonPanel = new Panel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final Button doneButton = new Button("Done");
	doneButton.addActionListener(
		new ActionListener (
		) {
			public void actionPerformed (
				final ActionEvent ae
			) {
				if (ae.getActionCommand().equals("Done")) {
					dispose();
				}
			}
		}
	);
	buttonPanel.add(doneButton);
	final TextArea text = new TextArea(25, 56);
	text.setEditable(false);
	text.append("\n");
	text.append(" This work is based on the following paper:\n");
	text.append("\n");
	text.append(" P. Th" + (char)233 + "venaz, U.E. Ruttimann, M. Unser\n");
	text.append(" A Pyramid Approach to Subpixel Registration Based on Intensity\n");
	text.append(" IEEE Transactions on Image Processing\n");
	text.append(" vol. 7, no. 1, pp. 27-41, January 1998.\n");
	text.append("\n");
	text.append(" This paper is available on-line at\n");
	text.append(" http://bigwww.epfl.ch/publications/thevenaz9801.html\n");
	text.append("\n");
	text.append(" Other relevant on-line publications are available at\n");
	text.append(" http://bigwww.epfl.ch/publications/\n");
	text.append("\n");
	text.append(" Additional help available at\n");
	text.append(" http://bigwww.epfl.ch/thevenaz/stackreg/\n");
	text.append("\n");
	text.append(" Ancillary TurboReg_ plugin available at\n");
	text.append(" http://bigwww.epfl.ch/thevenaz/turboreg/\n");
	text.append("\n");
	text.append(" You'll be free to use this software for research purposes, but\n");
	text.append(" you should not redistribute it without our consent. In addition,\n");
	text.append(" we expect you to include a citation or acknowledgment whenever\n");
	text.append(" you present or publish results that are based on it.\n");
	add("North", separation);
	add("Center", text);
	add("South", buttonPanel);
	pack();
} /* end stackRegCredits */

} /* end class stackRegCredits */