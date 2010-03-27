/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2010 Mark Longair */

/*
  This file is part of the ImageJ plugin "Curvatures_".

  The ImageJ plugin "Curvatures_" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  The ImageJ plugin "Curvatures_" is distributed in the hope that it
  will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fiji.features;

import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;
import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.outside.OutsideStrategyMirrorFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

public class Curvatures_<T extends NumericType<T>> implements PlugIn {

	protected Image<T> image;

	/* This comparator is useful for sorting a collection of Float
	   objects by their absolute value, largest first */

	public class ReverseAbsoluteFloatComparator implements Comparator {
		public int compare(Object d1, Object d2) {
			return Double.compare( Math.abs(((Float)d2).floatValue()),
					       Math.abs(((Float)d1).floatValue()) );
		}
	}

	/** Generate an ArrayList of images, each of which contains a
	    particular eigenvalue of the Hessian matrix at each point
	    in the image. */

	public ArrayList< Image<FloatType> > hessianEigenvalueImages( Image<T> input, float [] spacing ) {

		/* Various cursors may go outside the image, in which
		   case we supply mirror values: */

		OutsideStrategyMirrorFactory osmf = new OutsideStrategyMirrorFactory<T>();

		LocalizableByDimCursor<T> cursor = input.createLocalizableByDimCursor( osmf );

		ImageFactory<FloatType> floatFactory = new ImageFactory<FloatType>(
			new FloatType(),
			new ArrayContainerFactory() );

		ArrayList< Image<FloatType> > eigenvalueImages = new ArrayList< Image<FloatType> >();
		ArrayList< LocalizableByDimCursor<FloatType> > eCursors = new ArrayList< LocalizableByDimCursor<FloatType> >();

		int numberOfDimensions = input.getDimensions().length;

		/* Create an eigenvalue images and a cursor for each */

		for( int i = 0; i < numberOfDimensions; ++i ) {
			Image<FloatType> eigenvalueImage = floatFactory.createImage( input.getDimensions() );
			eigenvalueImages.add( eigenvalueImage );
			eCursors.add( eigenvalueImage.createLocalizableByDimCursor() );
		}

		Matrix hessian = new Matrix( numberOfDimensions, numberOfDimensions );

		/* Two cursors for finding points around the point of
		   interest, used for calculating the second
		   derivatives at that point: */
		LocalizableByDimCursor<T> ahead = input.createLocalizableByDimCursor( osmf );
		LocalizableByDimCursor<T> behind = input.createLocalizableByDimCursor( osmf );

		ReverseAbsoluteFloatComparator c = new ReverseAbsoluteFloatComparator();

		while( cursor.hasNext() ) {

			cursor.fwd();

			for( int m = 0; m < numberOfDimensions; ++m )
				for( int n = 0; n < numberOfDimensions; ++n ) {

					ahead.moveTo( cursor );
					behind.moveTo( cursor );

					ahead.fwd(m);
					behind.bck(m);

					ahead.fwd(n);
					behind.fwd(n);

					float firstDerivativeA = (ahead.getType().getReal() - behind.getType().getReal()) / (2 * spacing[m]);

					ahead.bck(n); ahead.bck(n);
					behind.bck(n); behind.bck(n);

					float firstDerivativeB = (ahead.getType().getReal() - behind.getType().getReal()) / (2 * spacing[m]);

					double value = (firstDerivativeA - firstDerivativeB) / (2 * spacing[n]);
					hessian.set(m,n,value);
				}

			// Now find the eigenvalues and eigenvalues of the Hessian matrix:
			EigenvalueDecomposition e = hessian.eig();

			/* Nonsense involved in sorting this array of
			   eigenvalues by their absolute values: */

			double [] eigenvaluesArray = e.getRealEigenvalues();
			ArrayList<Float> eigenvaluesArrayList = new ArrayList<Float>(eigenvaluesArray.length);
			for( double ev : eigenvaluesArray )
				eigenvaluesArrayList.add(new Float(ev));

			Collections.sort( eigenvaluesArrayList, c );

			// Set the eigenvalues at the point of interest in each image:

			for( int i = 0; i < numberOfDimensions; ++i ) {
				LocalizableByDimCursor<FloatType> eCursor = eCursors.get(i);
				eCursor.moveTo(cursor);
				eCursor.getType().set( eigenvaluesArrayList.get(i).floatValue() );
			}
		}

		// Remember to close all the cursors:

		cursor.close();

		ahead.close();
		behind.close();

		for( LocalizableByDimCursor<FloatType> eCursor : eCursors )
			eCursor.close();

		return eigenvalueImages;
	}

	public void run( String ignored ) {

		ImagePlus imagePlus = IJ.getImage();
		if( imagePlus == null ) {
			IJ.error("There's no image open to work on.");
			return;
		}

		float realSigma = 2;

		image = ImagePlusAdapter.wrap(imagePlus);

		float [] spacing = image.getCalibration();

		double [] sigmas = new double[spacing.length];
		for( int i = 0; i < spacing.length; ++i )
			sigmas[i] = 1.0 / (double)spacing[i];

		GaussianConvolution<T> gauss = new GaussianConvolution<T>( image, new OutsideStrategyMirrorFactory<T>(), sigmas );

		gauss.setNumThreads( Runtime.getRuntime().availableProcessors() );

		if ( !gauss.checkInput() || !gauss.process() )
		{
			IJ.error( "Gaussian Convolution failed: " + gauss.getErrorMessage() );
			return;
		}

		final Image<T> result = gauss.getResult();

		ImageJFunctions.copyToImagePlus( result ).show();

		ArrayList< Image<FloatType> > eigenvalueImages = hessianEigenvalueImages(result,spacing);

		for( Image<FloatType> resultImage : eigenvalueImages ) {
			ImageJFunctions.copyToImagePlus( resultImage ).show();
		}


	}

}