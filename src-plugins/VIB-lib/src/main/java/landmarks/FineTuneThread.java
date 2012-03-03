/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.IJ;
import ij.ImagePlus;
import pal.math.ConjugateDirectionSearch;
import vib.FastMatrix;

/* This can all get very confusing, so to make my convention clear:

     green channel == fixed image == current image
     magenta channel == transformed image == cropped template image

   So we're transforming the template onto the current image.

     templatePoint is in the template
     guessedPoint is in the current image

*/

public class FineTuneThread extends Thread {

	boolean keepResults = true;

	int method;
	double cubeSide;
	ImagePlus croppedTemplate;
	ImagePlus template;
	NamedPointWorld templatePoint;
	ImagePlus newImage;
	NamedPointWorld guessedPoint;
	double [] initialTransformation;
	double [] guessedTransformation;
	ProgressWindow progressWindow;
	FineTuneProgressListener listener;

	public void setInitialTransformation( double [] initialTransformation ) {
		if( initialTransformation.length != 6 )
			throw new RuntimeException( "initialTransformation passed to FineTuneThread must be 6 in length" );
		this.initialTransformation = initialTransformation;
	}

	public FineTuneThread(
		int method,
		double cubeSide,
		ImagePlus croppedTemplate, // The cropped template image.
		ImagePlus template, // The full template image.
		NamedPointWorld templatePoint,
		ImagePlus newImage, // The full current image.
		NamedPointWorld guessedPoint,
		double [] initialTransformation,
		double [] guessedTransformation,
		ProgressWindow progressWindow, // May be null if there's no GUI
		FineTuneProgressListener listener ) {

		this.method = method;
		this.cubeSide = cubeSide;
		this.croppedTemplate = croppedTemplate;
		this.template = template;
		this.templatePoint = templatePoint;
		this.newImage = newImage;
		this.guessedPoint = guessedPoint;
		if( initialTransformation != null && initialTransformation.length != 6 )
			throw new RuntimeException( "initialTransformation passed to FineTuneThread must be 6 in length" );
		this.initialTransformation = initialTransformation;
		if( guessedTransformation != null && guessedTransformation.length != 6 )
			throw new RuntimeException( "guessedTransformation passed to FineTuneThread must be 6 in length, if non-null" );
		this.guessedTransformation = guessedTransformation;
		this.progressWindow = progressWindow;
		this.listener = listener;
	}

	ConjugateDirectionSearch optimizer;

	@Override
	public void run() {

		double [] startValues = initialTransformation.clone();

		optimizer = new ConjugateDirectionSearch();

		optimizer.step = 1;
		optimizer.scbd = 10.0;
		optimizer.illc = true;

		TransformationAttempt attempt = new TransformationAttempt(
			cubeSide,
			croppedTemplate,
			templatePoint,
			newImage,
			guessedPoint,
			method,
			listener,
			progressWindow );

		optimizer.optimize(attempt, startValues, 2, 2);

		if( pleaseStop ) {
			listener.fineTuneThreadFinished( FineTuneProgressListener.CANCELLED, null, this );
			return;
		}

		// Now it should be optimized such that our result
		// is in startValues.

		/*
		System.out.println("startValues now: ");
		NamePoints.printParameters(startValues);
		*/

		if( pleaseStop ) {
			listener.fineTuneThreadFinished( FineTuneProgressListener.CANCELLED, null, this );
			return;
		}

		// Now reproduce those results; they might be good...

		RegistrationResult r = NamePoints.mapImageWith(
			croppedTemplate,
			newImage,
			templatePoint,
			guessedPoint,
			startValues,
			cubeSide,
			method,
			"score: ");

		listener.fineTuneThreadFinished( FineTuneProgressListener.COMPLETED, r, this );
	}

	volatile boolean pleaseStop = false;

	public void askToFinish() {
		pleaseStop = true;
		if( optimizer != null )
			optimizer.interrupt = true;
	}
}
