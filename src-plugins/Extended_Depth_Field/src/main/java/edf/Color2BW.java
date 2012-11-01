//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
//==============================================================================

package edf;

import ij.IJ;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.image.IndexColorModel;

public class Color2BW {

	public static final double WHITE = 255.0;
	public static final double TINY = (double)Float.intBitsToFloat((int)0x33FFFFFF);

	/**
	 */
	public static ImageStack C2BPrincipalComponents(ImageStack stack){

		int n = stack.getSize();
		int nx = stack.getWidth();
		int ny = stack.getHeight();

		ImageStack output = new ImageStack(nx, ny);

		for (int k=0; k<n; k++){
			output.addSlice(null,C2BPrincipalComponents(stack.getProcessor(k+1)));
		}
		return output;

	}

	/**
	 *
	 */
	public static ImageProcessor C2BPrincipalComponents(ImageProcessor ip){
		ByteProcessor fp = new ByteProcessor(ip.getWidth(), ip.getHeight());
		fp.setPixels(getLuminanceFromPrincipalComponents(ip));
		fp.resetMinAndMax();
		return fp;
	}

	/**
	 */
	public static ImageProcessor C2BMean(ImageProcessor ip){
		ByteProcessor fp = new ByteProcessor(ip.getWidth(), ip.getHeight());
		fp.setPixels(getLuminanceFromMean(ip));
		fp.resetMinAndMax();
		return fp;
	}

	/**
	 */
	public static ImageStack C2BMean( ImageStack stack ){
		int n = stack.getSize();
		int nx = stack.getWidth();
		int ny = stack.getHeight();

		ImageStack output = new ImageStack(nx, ny);

		for (int k=0; k<n; k++){
			output.addSlice(null,C2BMean(stack.getProcessor(k+1)));
		}
		return output;
	}

	/**
	 */
	private static byte[] getLuminanceFromMean (final ImageProcessor ip){

		final int length = ip.getWidth() * ip.getHeight();
		double meanR = 0, meanG = 0, meanB = 0;

		if (ip.getPixels() instanceof byte[]) {

			final byte[] pixels = (byte[])ip.getPixels();
			final IndexColorModel icm = (IndexColorModel)ip.getColorModel();

			final int mapSize = icm.getMapSize();

			final byte[] reds = new byte[mapSize];
			final byte[] greens = new byte[mapSize];
			final byte[] blues = new byte[mapSize];

			icm.getReds(reds);
			icm.getGreens(greens);
			icm.getBlues(blues);

			int index;

			meanR = 0;
			meanG = 0;
			meanB = 0;

			for (int k = 0; (k < length); k++) {
				index = (int)(pixels[k] & 0xFF);
				meanR += (float)(reds[index] & 0xFF);
				meanG += (float)(greens[index] & 0xFF);
				meanB += (float)(blues[index] & 0xFF);
			}

			meanR /= length;
			meanG /= length;
			meanB /= length;
		}
		else if (ip.getPixels() instanceof int[]) {

			final int[] pixels = (int[])ip.getPixels();

			meanR = 0;
			meanG = 0;
			meanB = 0;

			for (int k = 0; (k < length); k++) {
				meanR += (double)((pixels[k] & 0x00FF0000) >>> 16);
				meanG += (double)((pixels[k] & 0x0000FF00) >>> 8);
				meanB += (double)(pixels[k] & 0x000000FF);
			}

			meanR /= length;
			meanG /= length;
			meanB /= length;
		}

		return getLuminanceFromFixedWeights(ip, new double[]{meanR,meanG,meanB});
	}

	/**
	 * Color to Gray Scale Image Conversion by Fixed Weights provided
	 * in the Image Converter Class of ImageJ Weights: r*0.299 + g*0.587 + b*0.114
	 */

	public static ImageStack C2BFixedWeights(ImageStack stack, boolean weighted){
		double[] weights = new double[3];
		if (weighted){
			weights[0] = 0.299;
			weights[1] = 0.587;
			weights[2] = 0.114;
		}
		else{
			weights[0] = 1.0/3.0;
			weights[1] = 1.0/3.0;
			weights[2] = 1.0/3.0;
		}
		return C2BFixedWeights(stack, weights);
	}

	/**
	 */
	public static ImageStack C2BFixedWeights(ImageStack stack, double[] weights){
		int n = stack.getSize();
		int nx = stack.getWidth();
		int ny = stack.getHeight();

		ImageStack output = new ImageStack(nx, ny);

		for (int k=0; k<n; k++){
			output.addSlice(null,C2BFixedWeights(stack.getProcessor(k+1),weights));
		}
		return output;

	}

	/**
	 *
	 */
	public static ImageProcessor C2BFixedWeights(ImageProcessor ip, boolean weighted){
		double[] weights = new double[3];
		if (weighted){
			weights[0] = 0.299;
			weights[1] = 0.587;
			weights[2] = 0.114;
		}
		else{
			weights[0] = 1.0/3.0;
			weights[1] = 1.0/3.0;
			weights[2] = 1.0/3.0;
		}
		return C2BFixedWeights(ip, weights);
	}

	/**
	 *
	 */
	public static ImageProcessor C2BFixedWeights(ImageProcessor ip, double[] weight){
		ByteProcessor fp = new ByteProcessor(ip.getWidth(), ip.getHeight());
		fp.setPixels(getLuminanceFromFixedWeights(ip,weight));
		fp.resetMinAndMax();
		return fp;
	}

	/**
	 *
	 */
	private static void computeStatistics(final ImageProcessor ip,	final double[] average,
			final double[][] scatterMatrix){
		final int length = ip.getWidth() * ip.getHeight();
		double r;
		double g;
		double b;
		if (ip.getPixels() instanceof byte[]) {
			final byte[] pixels = (byte[])ip.getPixels();
			final IndexColorModel icm = (IndexColorModel)ip.getColorModel();
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
			for (int k = 0; (k < length); k++) {
				histogram[pixels[k] & 0xFF]++;
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
		else if (ip.getPixels() instanceof int[]) {
			final int[] pixels = (int[])ip.getPixels();
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
	}

	/**
	 *
	 */
	private static double[] getEigenvalues (final double[][] scatterMatrix){

		double[] a = new double[4];

		a[0] = scatterMatrix[0][0] * scatterMatrix[1][1] *	scatterMatrix[2][2];
		a[0]+= 2.0 * scatterMatrix[0][1] * scatterMatrix[1][2] * scatterMatrix[2][0];
		a[0]-= scatterMatrix[0][1] * scatterMatrix[0][1] * scatterMatrix[2][2];
		a[0]-= scatterMatrix[1][2] * scatterMatrix[1][2] * scatterMatrix[0][0];
		a[0]-= scatterMatrix[2][0] * scatterMatrix[2][0] * scatterMatrix[1][1];

		a[1] = scatterMatrix[0][1] * scatterMatrix[0][1];
		a[1]+= scatterMatrix[1][2] * scatterMatrix[1][2];
		a[1]+= scatterMatrix[2][0] * scatterMatrix[2][0];
		a[1]-= scatterMatrix[0][0] * scatterMatrix[1][1];
		a[1]-= scatterMatrix[1][1] * scatterMatrix[2][2];
		a[1]-= scatterMatrix[2][2] * scatterMatrix[0][0];

		a[2]= scatterMatrix[0][0] + scatterMatrix[1][1] + scatterMatrix[2][2];

		a[3]=	 -1.0;

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
	}

	/**
	 *
	 */
	private static double[] getEigenvector(final double[][] scatterMatrix,
			final double eigenvalue){

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

	/**
	 *
	 */
	private static double getLargestAbsoluteEigenvalue (final double[] eigenvalue) {
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
	}

	/**
	 *
	 */
	private static byte[] getLuminanceFromPrincipalComponents (final ImageProcessor ip){

		final double[] average = {0.0, 0.0, 0.0};
		final double[][] scatterMatrix =
		{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}};

		computeStatistics(ip, average, scatterMatrix);

		double[] eigenvalue = getEigenvalues(scatterMatrix);

		if ((eigenvalue[0] * eigenvalue[0] + eigenvalue[1] *
				eigenvalue[1] + eigenvalue[2] * eigenvalue[2]) <= TINY) {
			IJ.error("Warning: eigenvalues too small.");
			return(getLuminanceFromCCIR709(ip));
		}

		double bestEigenvalue = getLargestAbsoluteEigenvalue(eigenvalue);
		double eigenvector[] = getEigenvector(scatterMatrix, bestEigenvalue);

		final double weight = eigenvector[0] + eigenvector[1] + eigenvector[2];

		if (TINY < Math.abs(weight)) {
			eigenvector[0] /= weight;
			eigenvector[1] /= weight;
			eigenvector[2] /= weight;
		}

		return getLuminanceFromFixedWeights(ip,eigenvector);
	}

	/**
	 *
	 * @param ip
	 * @return
	 */
	private static byte[] getLuminanceFromCCIR709 (final ImageProcessor ip){
		double[] weight = {0.212671, 0.71516, 0.072169};
		return getLuminanceFromFixedWeights(ip,weight);
	}

	/**
	 *
	 */
	private static byte[] getLuminanceFromFixedWeights (final ImageProcessor ip, double[] weight){

		final int length = ip.getWidth() * ip.getHeight();
		final byte[] gray = new byte[length];
		double r;
		double g;
		double b;

		double wr = weight[0];
		double wg = weight[1];
		double wb = weight[2];

		if (ip.getPixels() instanceof byte[]) {
			final byte[] pixels = (byte[])ip.getPixels();
			final IndexColorModel icm = (IndexColorModel)ip.getColorModel();
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
				gray[k] = (byte)(wr * r + wg * g + wb * b);
			}
		}
		else if (ip.getPixels() instanceof int[]) {
			final int[] pixels = (int[])ip.getPixels();
			for (int k = 0; (k < length); k++) {
				r = (double)((pixels[k] & 0x00FF0000) >>> 16);
				g = (double)((pixels[k] & 0x0000FF00) >>> 8);
				b = (double)(pixels[k] & 0x000000FF);
				gray[k] = (byte)( wr * r + wg * g + wb * b);
			}
		}
		return(gray);
	}

	/**
	 *
	 */
	private static double[] linearLeastSquares(final double[][] A, final double[] b){
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
	}


	/**
	 *
	 */
	private static void QRdecomposition(final double[][] Q,final double[][] R){
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
	}

}
