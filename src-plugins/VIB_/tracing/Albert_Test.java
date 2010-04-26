/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * Albert_Test.java
 *
 * Created on 07-Oct-2007, 15:17:53
 *
 */

package tracing;

import features.ComputeCurvatures;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.measure.Calibration;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Albert_Test implements PlugIn {

	public void run(String ignored) {

		// This is an example of tracing between two random
		// points in an image synchronously.  For an
		// example of how to use these classes in a asynchronous
		// way, see the Simple_Neurite_Tracer plugin.
		ImagePlus imagePlus = WindowManager.getCurrentImage();
		if (imagePlus == null) {
			IJ.error("No current image to use.");
			return;
		}

		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();
		int depth = imagePlus.getStackSize();

		if( ! (imagePlus.getType() == ImagePlus.GRAY8 ||
		       imagePlus.getType() == ImagePlus.COLOR_256) ) {
			IJ.error("This test only works on 8 bit images");
			return;
		}

		// Just pick a random start and goal point for the moment.
		Random rng = new Random();

		int start_x = rng.nextInt(width);
		int start_y = rng.nextInt(height);
		int start_z = rng.nextInt(depth);

		int goal_x = rng.nextInt(width);
		int goal_y = rng.nextInt(height);
		int goal_z = rng.nextInt(depth);

		// For testing, force these to be sensible values for
		// c061AG-cropped.tif:
		start_x = 319;
		start_y = 263;
		start_z = 39;

		goal_x = 186;
		goal_y = 48;
		goal_z = 29;

		// Use the reciprocal of the value at the new point as the cost
		// in moving to it (scaled by the distance between the points.
		boolean reciprocal = true;

		Calibration calibration = imagePlus.getCalibration();
		double minimumSeparation = 1;
		if( calibration != null )
			minimumSeparation = Math.min(Math.abs(calibration.pixelWidth),
						     Math.min(Math.abs(calibration.pixelHeight),
							      Math.abs(calibration.pixelDepth)));

                ComputeCurvatures hessian=null;
                if( true )  {

                        System.out.println("Calculating Gaussian...");

                        // In most cases you'll get better results by using the Hessian
                        // based measure of curvatures at each point, so calculate that
                        // in advance.
                        hessian = new ComputeCurvatures(imagePlus, minimumSeparation, null, calibration != null);
                        hessian.run();
                }

		System.out.println("Finished calculating Gaussian.");

		// Give up after 3 minutes.
		// int timeoutSeconds = 3 * 60;
                int timeoutSeconds = 5 * 60;

		// This doesn't matter in this case, since there's no
		// interface that'll need updating.  However, it'll only
                // check whether the timeout has expired every time this
                // interval is up, so don't set it too high.
		long reportEveryMilliseconds = 3000;

		TracerThread tracer = new TracerThread(imagePlus, 0, 255, timeoutSeconds, reportEveryMilliseconds, start_x, start_y, start_z, goal_x, goal_y, goal_z, reciprocal, depth == 1, hessian, ((hessian == null) ? 1 : 4), null, hessian != null );

		System.out.println("Running tracer...");
		tracer.run();
		System.out.println("Finished running tracer...");

		Path result = tracer.getResult();

		if (result == null) {
			IJ.error("Finding a path failed: "+
                                 SearchThread.exitReasonStrings[tracer.getExitReason()]);
			return;
		}

		IJ.error("Found a path: " + result);

		// We can just use the Path object directly, or write
		// it out using the PathAndFillManger.
		PathAndFillManager manager = new PathAndFillManager(imagePlus);
		manager.addPath(result);

		File tmpFile;

		try {
			tmpFile = File.createTempFile("albert-test-", ".xml");
			manager.writeXML(tmpFile.getAbsolutePath(), false);
		} catch (IOException e) {
			IJ.error("IOException while trying to write the path to a temporary file: " + e);
			return;
		}

		IJ.open(tmpFile.getAbsolutePath());
	}
}
