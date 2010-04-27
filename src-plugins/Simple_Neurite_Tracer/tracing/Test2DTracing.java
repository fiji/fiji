/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for tracing through a simple image */

package tracing;

import ij.ImagePlus;
import ij.measure.Calibration;

import util.BatchOpener;

import features.ComputeCurvatures;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Test2DTracing {

	ImagePlus image;

	double startX = 73.539; double startY = 48.449;
	double endX = 1.730; double endY = 13.554;

	@Before public void setUp() {
		image = BatchOpener.openFirstChannel("test-images/c061AG-small-section-z-max.tif" );
		assertNotNull("Couldn't open the 2D test image",image);
	}

	@After
	public void tearDown() {
		image.close();
	}

	@Test
	public void testTracing() {

		double pixelWidth = 1;
		double pixelHeight = 1;
		double pixelDepth = 1;
		double minimumSeparation = 1;
		Calibration calibration = image.getCalibration();
		if( calibration != null ) {
			minimumSeparation = Math.min(Math.abs(calibration.pixelWidth),
						     Math.min(Math.abs(calibration.pixelHeight),
							      Math.abs(calibration.pixelDepth)));
			pixelWidth = calibration.pixelWidth;
			pixelHeight = calibration.pixelHeight;
			pixelDepth = calibration.pixelDepth;
		}

		int pointsExploredNormal = 0;
		{
			TracerThread tracer = new TracerThread(image,
							       0,
							       255,
							       -1, // timeoutSeconds
							       100, // reportEveryMilliseconds
							       (int)( startX / pixelWidth ),
							       (int)( startY / pixelHeight ),
							       0,
							       (int)( endX / pixelWidth ),
							       (int)( endY / pixelHeight ),
							       0,
							       true, // reciprocal
							       true, // singleSlice
							       null,
							       1, // multiplier
							       null,
							       false);

			tracer.run();
			Path result = tracer.getResult();
			assertNotNull("Not path found",result);

			double foundPathLength = result.getRealLength();
			assertTrue( "Path length must be greater than 100 micrometres",
				    foundPathLength > 100 );

			assertTrue( "Path length must be less than 105 micrometres",
				    foundPathLength < 105 );

			pointsExploredNormal = tracer.pointsConsideredInSearch();
		}

		int pointsExploredHessian = 0;
		{
			ComputeCurvatures hessian = new ComputeCurvatures(image, minimumSeparation, null, calibration != null);
                        hessian.run();

			TracerThread tracer = new TracerThread(image,
							       0,
							       255,
							       -1, // timeoutSeconds
							       100, // reportEveryMilliseconds
							       (int)( startX / pixelWidth ),
							       (int)( startY / pixelHeight ),
							       0,
							       (int)( endX / pixelWidth ),
							       (int)( endY / pixelHeight ),
							       0,
							       true, // reciprocal
							       true, // singleSlice
							       hessian,
							       50, // multiplier
							       null,
							       true);

			tracer.run();
			Path result = tracer.getResult();
			assertNotNull("Not path found",result);

			double foundPathLength = result.getRealLength();

			assertTrue( "Path length must be greater than 92 micrometres",
				    foundPathLength > 92 );

			assertTrue( "Path length must be less than 96 micrometres",
				    foundPathLength < 96 );

			pointsExploredHessian = tracer.pointsConsideredInSearch();

			assertTrue( "Hessian-based analysis should reduce the points explored " +
				    "by at least a third; in fact went from " +
				    pointsExploredNormal + " to " +pointsExploredHessian,
				    pointsExploredHessian * 1.5 < pointsExploredNormal );
		}
	}
}
