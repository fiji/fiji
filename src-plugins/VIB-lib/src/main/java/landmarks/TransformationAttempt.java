/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.ImagePlus;
import ij.measure.Calibration;
import pal.math.MultivariateFunction;

public class TransformationAttempt implements MultivariateFunction {

	double cubeSide;
	ImagePlus croppedTemplate;
	NamedPointWorld templatePoint;
	ImagePlus newImage;
	NamedPointWorld guessedPoint;

	ProgressWindow progressWindow;

	double minTranslation;
	double maxTranslation;

	int similarityMeasure;

	FineTuneProgressListener listener;

	public TransformationAttempt( double cubeSide,
				      ImagePlus croppedTemplate,
				      NamedPointWorld templatePoint,
				      ImagePlus newImage,
				      NamedPointWorld guessedPoint,
				      int similarityMeasure,
				      FineTuneProgressListener listener,
				      ProgressWindow progressWindow ) {

		this.cubeSide = cubeSide;
		this.croppedTemplate = croppedTemplate;
		this.templatePoint = templatePoint;
		this.newImage = newImage;
		this.guessedPoint = guessedPoint;
		this.listener = listener;
		this.progressWindow = progressWindow;

		this.similarityMeasure = similarityMeasure;

		minTranslation = -cubeSide;
		// Find what the maximum translation could be:
		Calibration c=newImage.getCalibration();
		maxTranslation = 0;
		double max_x = newImage.getWidth() * c.pixelWidth;
		double max_y = newImage.getHeight() * c.pixelHeight;
		double max_z = newImage.getStackSize() * c.pixelDepth;
		if( max_x > maxTranslation ) maxTranslation = max_x;
		if( max_y > maxTranslation ) maxTranslation = max_y;
		if( max_z > maxTranslation ) maxTranslation = max_z;
		maxTranslation += cubeSide;
	}

	public double evaluate( double[] argument ) {

		RegistrationResult r = NamePoints.mapImageWith( croppedTemplate, newImage, templatePoint, guessedPoint, argument, cubeSide, similarityMeasure, "");

		if( listener != null )
			listener.fineTuneNewBestResult( r );

		return r.score;
	}

	public int getNumArguments() {
		return 6;
	}

	public double getLowerBound(int n) {
		if( (n >= 0) && (n <= 2) ) {
			// i.e. it's an angle...
			return -4 * Math.PI;
		} else /* if( (n >= 3) && (n <= 5) ) */ {
			// i.e. it's a translation...
			return minTranslation;
		}
	}

	public double getUpperBound(int n) {
		if( (n >= 0) && (n <= 2) ) {
			// i.e. it's an angle...
			return 4 * Math.PI;
		} else /* if( (n >= 3) && (n <= 5) ) */ {
			// i.e. it's a translation...
			return maxTranslation;
		}
	}
}
