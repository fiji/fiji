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

public class Test3DTracing {

	ImagePlus image;

	double startX = 56.524; double startY = 43.258; double startZ = 18;
	double endX = 0; double endY = 17.015; double endZ = 22.8;

	@Before public void setUp() {
		image = BatchOpener.openFirstChannel("test-images/c061AG-small-section.tif" );
		assertNotNull("Couldn't open the 3D test image",image);
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

		boolean doNormal = false;

		int pointsExploredNormal = 0;
		// This is very slow without the preprocessing, so don't do that bit by default:
		if( doNormal ) {
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
							       false, // singleSlice
							       null,
							       1, // multiplier
							       null,
							       false);

			tracer.run();
			Path result = tracer.getResult();
			assertNotNull("Not path found",result);

			double foundPathLength = result.getRealLength();
			assertTrue( "Path length must be greater than 95 micrometres",
				    foundPathLength > 95 );

			assertTrue( "Path length must be less than 100 micrometres",
				    foundPathLength < 100 );

			pointsExploredNormal = tracer.pointsConsideredInSearch();
		}

		int pointsExploredHessian = 0;
		{
			ComputeCurvatures hessian = new ComputeCurvatures(image, 0.721, null, calibration != null);
                        hessian.run();

			TracerThread tracer = new TracerThread(image,
							       0,
							       255,
							       -1, // timeoutSeconds
							       100, // reportEveryMilliseconds
							       (int)( startX / pixelWidth ),
							       (int)( startY / pixelHeight ),
							       (int)( startZ / pixelDepth ),
							       (int)( endX / pixelWidth ),
							       (int)( endY / pixelHeight ),
							       (int)( endZ / pixelDepth ),
							       true, // reciprocal
							       false, // singleSlice
							       hessian,
							       19.69, // multiplier
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

			assertTrue( "Hessian-based analysis should explore less than 20000 points",
				    pointsExploredHessian < 20000 );

			if( doNormal ) {
				assertTrue( "Hessian-based analysis should reduce the points explored " +
					    "by at least a third; in fact went from " +
					    pointsExploredNormal + " to " +pointsExploredHessian,
					    pointsExploredHessian * 10 < pointsExploredNormal );
			}
		}
	}
}
