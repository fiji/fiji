/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.peak;

import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A simple class fit a gaussian to the peaks in a n-dim, 0-background
 * image.
 * <p>
 * The gaussian can have a different sigma in any direction, but the ellipse main axes still need
 * to be aligned with the image axes. Note that there is no constant term in this equation, 
 * thus the need for a 0-background image.
 * <p>
 * The fitting uses a plain Levenberg-Marquardt least-square curve fitting algorithm, with 
 * various small tweaks for robustness and speed, mainly a first step to derive 
 * a crude estimate, based on maximum-likelihood analytic formulae.
 * <p>
 * A fitter is instantiated for an image, but can be easily processed in parallel for
 * multiple spots on this image.
 *  
 * @param <T>  the type of the given image, must extend {@link RealType}, for
 * we operate on real values.
 *
 * @author Jean-Yves Tinevez (tinevez@pasteur.fr)
 * @author 2011-2012
 */
public class GaussianPeakFitterND <T extends RealType<T>> {

	private static final String BASE_ERROR_MESSAGE = "GaussianPeakFitterND: ";
	
	private final Image<T> image;
	private final int ndims;
	private String errorMessage;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Instantiate a 2D gaussian peak fitter that will operate on the given image. 
	 * It is important that the image has a 0 background for this class to 
	 * operate properly. 
	 */
	public GaussianPeakFitterND(final Image<T> image) {
		this.image = image;
		this.ndims = image.getNumDimensions();
	}

	/*
	 * METHODS
	 */

	/** 
	 * Ensure the image is not null.
	 */
	public boolean checkInput() {
		if (null == image) {
			errorMessage = BASE_ERROR_MESSAGE + "Image is null.";
			return false;
		}
		return true;
	}

	/**
	 * Fit an elliptical gaussian to a peak in the image, using the formula:
	 * <pre>
	 * 	g(xᵢ) = A * exp ( - ∑ cᵢ × (xᵢ - x₀ᵢ)² )
	 * </pre>
	 * <p>
	 * First observation arrays are built by collecting pixel positions and intensities
	 * around the given peak location. These arrays are then used to guess a starting
	 * set of parameters, that is then fed to least-square optimization procedure, 
	 * using the Levenberg-Marquardt curve fitter.
	 * <p>
	 * Calls to this function does not generate any class field, and can therefore
	 * by readily parallelized with multiple peaks on the same image.
	 * 
	 * @param point  the approximate coordinates of the peak
	 * @param typical_sigma  the typical sigma of the peak (in pixel unit, array of one
	 * element per dimension), that will be used to derive the size of a block that will
	 * be inspected around the peak by the fit (the actual block size will of of size 
	 * <code>2 * 2 * ceil(typical_sigma) + 1)</code>.
	 * @return  a <code>2*ndims+1</code> elements double array containing fit estimates, 
	 * in the following indices: 
	 * <pre> 0.			A
	 * 1 → ndims		x₀ᵢ
	 * ndims+1 → 2 × ndims	cᵢ = 1 / σᵢ² </pre>
	 */
	public double[] process(final Localizable point, final double[] typical_sigma) {

		// Determine the size of the data to gather
		int[] pad_size = new int[ndims];
		for (int i = 0; i < ndims; i++) {
			pad_size[i] = (int) Math.ceil( 2 * typical_sigma[i]);
		}	

		// Gather data around peak
		final Observation data = gatherObservationData(point, pad_size); 
		final double[][] X = data.X;
		final double[] I = data.I;

		// Make best guess
		double[] start_param = makeBestGuess(X, I);
		
		// Correct for too large sigmas: we drop estimate and replace it by user input
		for (int j = 0; j < ndims; j++) {
			if ( start_param [ j + ndims + 1 ] <  1 / ( typical_sigma[j] * typical_sigma[j] ) ) {
				start_param [ j + ndims + 1 ] = 1 / ( typical_sigma[j] * typical_sigma[j] );
			}
		}
		
		// Prepare optimizer
		int maxiter = 300;
		double lambda = 1e-3;
		double termepsilon = 1e-1;
		
		final double[] a = start_param.clone();
		try {
			LevenbergMarquardtSolver.solve(X, a, I, new GaussianMultiDLM(), lambda , termepsilon, maxiter);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		// NaN protection: we prefer returning the crude estimate that NaN
		for (int j = 0; j < a.length; j++) {
			if (Double.isNaN(a[j]))
				a[j] = start_param[j];
		}
		return a;
	}

	/**
	 * Try to build a best guess parameter set to serve as a starting point for optimization.
	 * <p>
	 * The principle of this guess relies on supposing the intensity distribution is 
	 * actually a multivariate normal distribution from which numbers are 
	 * drawn, with a frequency I. We try to retrieve parameters from the distribution
	 * using a maximum likelihood approach. For the normal distribution, parameter
	 * estimates can be derived analytically, using the covariance matrix.  
	 * 
	 * @param X  the coordinates of observations
	 * @param I  the pixel value of observations
	 * @return  a double array containing crude estimates for parameters in this order: 
	 * <pre> 0.			A
	 * 1 → ndims		x₀ᵢ
	 * ndims+1 → 2 × ndims	cᵢ = 1 / σᵢ² </pre>
	 */
	private final double[] makeBestGuess(final double[][] X, final double[] I) {

		double[] start_param = new double[2*ndims+1];

		double[] X_sum = new double[ndims];
		for (int j = 0; j < ndims; j++) {
			X_sum[j] = 0;
			for (int i = 0; i < X.length; i++) {
				X_sum[j] += X[i][j] * I[i];
			}
		}

		double I_sum = 0;
		double max_I = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < X.length; i++) {
			I_sum += I[i];
			if (I[i] > max_I) {
				max_I = I[i];
			}

		}

		start_param[0] = max_I;

		for (int j = 0; j < ndims; j++) {
			start_param[j+1] = X_sum[j] / I_sum;
		}

		for (int j = 0; j < ndims; j++) {
			double C = 0;
			double dx;
			for (int i = 0; i < X.length; i++) {
				dx = X[i][j] - start_param[j+1];
				C += I[i] * dx * dx;
			}
			C /= I_sum;
			start_param[ndims + j + 1] = 1 / C;
		}
		
		return start_param;		
	}

	/**
	 * Collect the points to build the observation array, by iterating in a hypercube
	 * around the given location. The size of the cube is calculated by  
	 * <code>2 * 2 * ceil(typical_sigma) + 1)</code>. Points found out of the image are
	 * not included.
	 */
	private final Observation gatherObservationData(final Localizable point, final int[] pad_size) {

		int n_pixels = 1;
		for (int i = 0; i < pad_size.length; i++) {
			n_pixels *= (2 * pad_size[i] + 1);
		}

		double[][] tmp_X 	= new double[n_pixels][ndims];
		double[] tmp_I 		= new double[n_pixels];
		int index = 0;

		// Initialize position
		int[] offset = new int[ndims];
		int[] size = new int[ndims];
		for (int i = 0; i < offset.length; i++) {
			offset[i] = point.getPosition(i) - pad_size[i];
			size[i] = 2 * pad_size[i] + 1;
		}
		RegionOfInterestCursor<T> cursor = image.createLocalizableByDimCursor().createRegionOfInterestCursor(offset, size);

		boolean outofbounds = false;
		int[] pos = cursor.createPositionArray();
		while (cursor.hasNext()) {
			
			cursor.fwd();
			cursor.getPosition(pos); // This is the RELATIVE roi position
			
			// Offset position array and check if we are not out of bounds
			for (int i = 0; i < ndims; i++) {
				pos[i] += offset[i];
				if (pos[i] < 0 || pos[i] >= image.getDimension(i)) {
					outofbounds = true;
					break;
				}
			}
			if (outofbounds) {
				outofbounds = false;
				continue;
			}

			for (int i = 0; i < ndims; i++) {
				tmp_X[index][i] = pos[i];
			}
			
			tmp_I[index] = cursor.getType().getRealDouble();
			index++;
		} 
		cursor.close();

		// Now we possibly resize the arrays, in case we have been too close to the 
		// image border.
		double[][] X = null;
		double[] I = null;
		if (index == n_pixels) {
			// Ok, we have gone through the whole square
			X 	= tmp_X;
			I 	= tmp_I;
		} else {
			// Re-dimension the arrays
			X 	= new double[index][ndims];
			I 	= new double[index];
			System.arraycopy(tmp_X, 0, X, 0, index);
			System.arraycopy(tmp_I, 0, I, 0, index);
		}

		Observation obs = new Observation();
		obs.I = I;
		obs.X = X;
		return obs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	/*
	 * INNER CLASSES
	 */

	/**
	 * A general-use class that can store the observations as a double array, and their 
	 * coordinates as a 2D double array. 
	 */
	private static class Observation {
		public double[] I;
		public double[][] X;
	} 
}
