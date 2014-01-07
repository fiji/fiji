/* Copyright 2013 Tiago Ferreira, 2005 Tom Maddock
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
//import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.Recorder;
import ij.process.*;
import ij.text.*;
import ij.util.Tools;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

/**
 * Performs Sholl Analysis on segmented arbors. Several analysis methods are
 * available: Linear (N), Linear (N/S), Semi-log and Log-log as described in
 * Milosevic and Ristanovic, J Theor Biol (2007) 245(1)130-40.
 * The original method is described in Sholl, DA. J Anat (1953) 87(4)387-406.
 *
 * NB: For binary images, background is always considered to be 0, independently
 * of Prefs.blackBackground.
 *
 * @author Tiago Ferreira v2.0 Feb 2012, v3.0 Oct 2012, v3.1 June 2013
 * @author Tom Maddock v1.0, Oct 2005
 */
public class Sholl_Analysis implements PlugIn, DialogListener {

    /* Plugin Information */
    public static final String VERSION = "3.2";
    private static final String URL = "http://fiji.sc/Sholl_Analysis";

    /* Sholl Type Definitions */
    private static final String[] SHOLL_TYPES = { "Linear", "Linear-norm", "Semi-log", "Log-log" };
    private static final int SHOLL_N    = 0;
    private static final int SHOLL_NS   = 1;
    private static final int SHOLL_SLOG = 2;
    private static final int SHOLL_LOG  = 3;
    private static boolean shollN    = true;
    private static boolean shollNS   = false;
    private static boolean shollSLOG = true;
    private static boolean shollLOG  = false;

    /* Data Normalization */
    private static final String[] NORMS2D = { "Area", "Perimeter", "Annulus" };
    private static final String[] NORMS3D = { "Volume", "Surface", "Spherical shell" };
    private static final int AREA_NORM      = 0;
    private static final int PERIMETER_NORM = 1;
    private static final int ANNULUS_NORM   = 2;
    private static int normChoice;

    /* Ramification Indices */
    private static int primaryBranches = 4;
    private static boolean inferPrimary;

    /* Curve Fitting, Results and Descriptors */
    private static boolean fitCurve = true;
    private static final String[] DEGREES = { "4th degree", "5th degree", "6th degree", "7th degree", "8th degree", "Best fitting degree" };
    private static final int SMALLEST_DATASET = 6;
    private static final String SHOLLTABLE = "Sholl Results";
    private static double[] centroid;
    private static int enclosingCutOff = 1;

	/* Image path and Output Options */
    private static boolean validPath;
	private static boolean hideSaved;
    private static String imgPath;
    private static String imgTitle;

    /* Default parameters and input values */
    private static double startRadius = 10.0;
    private static double endRadius   = 100.0;
    private static double stepRadius  = 1;
    private static double incStep     = 0;
    private static int polyChoice     = 3;
    private static boolean verbose;
    private static boolean mask;
    public static int maskBackground = 228;
    private static boolean save;
    private static boolean isCSV = false;

    /* Common variables */
    private static String unit = "pixels";
    private static double vxSize = 1;
    private static double vxWH = 1;
    private static double vxD  = 1;
    private static boolean is3D;
    private static int lowerT;
    private static int upperT;

    /* Boundaries of analysis */
    private static boolean orthoChord = false;
    private static boolean trimBounds;
    private static String[] quads = new String[2];
    private static final String QUAD_NORTH = "Above line";
    private static final String QUAD_SOUTH = "Below line";
    private static final String QUAD_EAST  = "Left of line";
    private static final String QUAD_WEST  = "Right of line";
    private static String quadString = "None";
    private static int quadChoice;
    private static int minX;
    private static int maxX;
    private static int minY;
    private static int maxY;
    private static int minZ;
    private static int maxZ;

    /* Parameters for 3D analysis */
    private static boolean skipSingleVoxels = false;

    /* Parameters for 2D analysis */
    private static final String[] BIN_TYPES = { "Mean", "Median" };
    private static final int BIN_AVERAGE = 0;
    private static final int BIN_MEDIAN  = 1;
    private static int binChoice = BIN_AVERAGE;
    private static int nSpans = 1;

    // If the edge of a group of pixels lies tangent to the sampling circle, multiple
    // intersections with that circle will be counted. With this flag on, we will try to
    // find these "false positives" and throw them out. A way to attempt this (we will be
    // missing some of them) is to throw out 1-pixel groups that exist solely on the edge
    // of a "stair" of target pixels (see countSinglePixels)
    private static boolean doSpikeSupression = true;

	private static double[] radii;
	private static double[] counts;

    public void run( final String arg) {

        if (IJ.versionLessThan("1.46h"))
            return;

        final ImagePlus img = WindowManager.getCurrentImage();

		// Initialize center coordinates (in pixel units) and N. of samples
		int x, y, z;
		final Calibration cal;

		isCSV = IJ.altKeyDown();

		if (isCSV) {

			ResultsTable rt = ResultsTable.getResultsTable();
			if (rt==null || rt.getCounter()==0) {
				try {
					rt = ResultsTable.open("");
					if (rt!=null) {
						rt.show("Results"); // could be omitted
						validPath = true;
						imgPath = OpenDialog.getLastDirectory();
						imgTitle = trimExtension(OpenDialog.getLastName());
					} else	//user pressed "Cancel" in file prompt
						return;
				} catch(final IOException e) {
					sError(e.getMessage());
					return;
				}
			} else {
				imgPath = null;
				validPath = false;
				imgTitle = "Imported data";
			}

			if (!csvPrompt(rt))
				return;

			fitCurve = true;
			x = (int)Double.NaN;
			y = (int)Double.NaN;
			z = (int)Double.NaN;
			cal = null; unit = "N.A.";

		} else {

			// Make sure image is of the right type, reminding the user
			// that the analysis is performed on segmented cells
			final ImageProcessor ip = getValidProcessor(img);
			if (ip==null)
				return;

			// Set the 2D/3D Sholl flag
			final int depth = img.getNSlices();
			is3D = depth > 1;

			imgTitle = img.getShortTitle();

			// Get image calibration. Stacks are likely to have anisotropic voxels
			// with large z-steps. It is unlikely that lateral dimensions will differ
			cal = img.getCalibration();
			if (cal.scaled()) {
				vxWH = Math.sqrt(cal.pixelWidth * cal.pixelHeight);
				vxD  = cal.pixelDepth;
				unit = cal.getUnits();
			} else {
				vxWH = vxD = 1; unit = "pixels";
			}
			vxSize = (is3D) ? Math.cbrt(vxWH*vxWH*vxD) : vxWH;

			z = img.getCurrentSlice();

			// Get parameters from current ROI. Prompt for one if none exists
			Roi roi = img.getRoi();
			final boolean validRoi = roi!=null && (roi.getType()==Roi.LINE || roi.getType()==Roi.POINT);

			if (!IJ.macroRunning() && !validRoi) {
				img.killRoi();
				Toolbar.getInstance().setTool("line");
				final WaitForUserDialog wd = new WaitForUserDialog(
								"Please define the largest Sholl radius by creating\n"
								+ "a straight line starting at the center of analysis.\n"
								+ "(Hold down \"Shift\" to draw an orthogonal radius)\n \n"
								+ "Alternatively, define the focus of the arbor using\n"
								+ "the Point Selection Tool.");
				wd.show();
				if (wd.escPressed())
					return;
				roi = img.getRoi();
			}

			// Initialize angle of the line roi (if any). It will become positive
			// if a line (chord) exists.
			double chordAngle = -1.0;

			// Line: Get center coordinates, length and angle of chord
			if (roi!=null && roi.getType()==Roi.LINE) {

				final Line chord = (Line) roi;
				x = chord.x1;
				y = chord.y1;
				endRadius = vxSize * chord.getRawLength();
				chordAngle = Math.abs(chord.getAngle(x, y, chord.x2, chord.y2));

			// Point: Get center coordinates (x,y)
			} else if (roi != null && roi.getType() == Roi.POINT) {

				final PointRoi point = (PointRoi) roi;
				final Rectangle rect = point.getBounds();
				x = rect.x;
				y = rect.y;

			// Not a proper ROI type
			} else {
				sError("Straight Line or Point selection required.");
				return;
			}

			// Show the plugin dialog: Update parameters with user input and
			// retrieve if analysis will be restricted to a hemicircle/hemisphere
			if(!bitmapPrompt(chordAngle, is3D))
				return;

			// Impose valid parameters
			final int wdth = ip.getWidth();
			final int hght = ip.getHeight();
			final double dx, dy, dz, maxEndRadius;

			dx = ((orthoChord && trimBounds && quadString.equals(QUAD_WEST)) || x<=wdth/2)
				? (x-wdth)*vxWH : x*vxWH;

			dy = ((orthoChord && trimBounds && quadString.equals(QUAD_SOUTH)) || y<=hght/2)
				? (y-hght)*vxWH : y*vxWH;

			dz = (z<=depth/2) ? (z-depth)*vxD : z*vxD;

			maxEndRadius = Math.sqrt(dx*dx + dy*dy + dz*dz);
			endRadius = Double.isNaN(endRadius) ? maxEndRadius : Math.min(endRadius, maxEndRadius);
			stepRadius = Math.max(vxSize, Double.isNaN(incStep) ? 0 : incStep);

			// Calculate how many samples will be taken
			final int size = (int) ((endRadius-startRadius)/stepRadius)+1;

			// Exit if there are no samples
			if (size<=1) {
				sError(" Invalid Parameters: Starting radius must be smaller than\n"
					+ "Ending radius and Radius step size must be within range!");
				return;
			}

			img.startTiming();
			IJ.resetEscape();

			// Create arrays for radii (in physical units) and intersection counts
			radii = new double[size];
			counts = new double[size];

			for (int i = 0; i < size; i++) {
				radii[i] = startRadius + i*stepRadius;
			}

			// Define boundaries of analysis according to orthogonal chords (if any)
			final int xymaxradius = (int) Math.round(radii[size-1]/vxWH);
			final int zmaxradius  = (int) Math.round(radii[size-1]/vxD);

			minX = Math.max(x-xymaxradius, 0);
			maxX = Math.min(x+xymaxradius, wdth);
			minY = Math.max(y-xymaxradius, 0);
			maxY = Math.min(y+xymaxradius, hght);
			minZ = Math.max(z-zmaxradius, 1);
			maxZ = Math.min(z+zmaxradius, depth);

			if (orthoChord && trimBounds) {
				if (quadString.equals(QUAD_NORTH))
					maxY = (int) Math.min(y + xymaxradius, y);
				else if (quadString.equals(QUAD_SOUTH))
					minY = (int) Math.max(y - xymaxradius, y);
				else if (quadString.equals(QUAD_WEST))
					minX = x;
				else if (quadString.equals(QUAD_EAST))
					maxX = x;
				}

			// 2D: Analyze the data and return intersection counts with nSpans
			// per radius. 3D: Analysis without nSpans
			if (is3D) {
				counts = analyze3D(x, y, z, radii, img);
			} else {
				counts = analyze2D(x, y, radii, vxSize, nSpans, binChoice, ip);
			}

		}

        IJ.showStatus("Preparing Results...");

        // Retrieve pairs of radii, counts for intersecting radii
        final double[][] valuesN = getNonZeroValues(radii, counts);
		final int trimmedCounts = valuesN.length;
        if (trimmedCounts==0) {
            IJ.beep();
            IJ.showProgress(0, 0);
            IJ.showTime(img, img.getStartTime(), "Error: All intersection counts were zero! ");
            return;
        }

        // Retrieve stats on sampled data
        final ResultsTable statsTable = createStatsTable(imgTitle, x, y, z, valuesN);

        // Transform and fit data
        final double[][] valuesNS   = transformValues(valuesN, true, false, false);
        final double[][] valuesSLOG = transformValues(valuesNS, false, true, false);
        final double[][] valuesLOG  = transformValues(valuesSLOG, false, false, true);
        double[] fvaluesN   = null;
        double[] fvaluesNS  = null;
        //double[] fvaluesLOG = null;

        // Create plots
        if (shollN) {

            final Plot plotN = plotValues("Sholl profile ("+ SHOLL_TYPES[SHOLL_N] +") for "+ imgTitle,
                    is3D ? "3D distance ("+ unit +")" : "2D distance ("+ unit +")",
                    "N. of Intersections",
                    valuesN, SHOLL_N);
			markPlotPoint(plotN, centroid, Color.RED);
            if (fitCurve)
                fvaluesN = getFittedProfile(valuesN, SHOLL_N, statsTable, plotN);
            savePlot(plotN, SHOLL_N);

        }

        final String normalizerString = is3D ? NORMS3D[normChoice] : NORMS2D[normChoice];
        final String distanceString = is3D ? "3D distance" : "2D distance";

        if (shollNS) {

			final Plot plotNS = plotValues("Sholl profile ("+ SHOLL_TYPES[SHOLL_NS] +") for "+ imgTitle,
					distanceString +" ("+ unit +")", "Inters./"+ normalizerString,
                    valuesNS, SHOLL_NS);
            if (fitCurve)
                fvaluesNS = getFittedProfile(valuesNS, SHOLL_NS, statsTable, plotNS);
            savePlot(plotNS, SHOLL_NS);

        }
		if (shollSLOG) {

			final Plot plotSLOG = plotValues("Sholl profile ("+ SHOLL_TYPES[SHOLL_SLOG] +") for "+ imgTitle,
					distanceString +" ("+ unit +")", "log(Inters./"+ normalizerString +")",
                    valuesSLOG, SHOLL_SLOG);
            if (fitCurve)
                plotRegression(valuesSLOG, plotSLOG, statsTable, SHOLL_TYPES[SHOLL_SLOG]);
            savePlot(plotSLOG, SHOLL_SLOG);

        }
		if (shollLOG) {

			final Plot plotLOG = plotValues("Sholl profile ("+ SHOLL_TYPES[SHOLL_LOG] +") for "+ imgTitle,
                    "log("+ distanceString +")", "log(Inters./"+ normalizerString +")",
                    valuesLOG, SHOLL_LOG);
            if (fitCurve)
                //fvaluesLOG = getFittedProfile(valuesLOG, SHOLL_LOG, statsTable, plotLOG);
                plotRegression(valuesLOG, plotLOG, statsTable, SHOLL_TYPES[SHOLL_LOG]);
            savePlot(plotLOG, SHOLL_LOG);

        }

        ResultsTable rt;
        final String profileTable = imgTitle + "_Sholl-Profiles";
        final Frame window = WindowManager.getFrame(profileTable);
        if (window == null)
            rt = new ResultsTable();
        else
            rt = ((TextWindow) window).getTextPanel().getResultsTable();

        rt.setPrecision(getPrecision());
        for (int i=0; i <valuesN.length; i++) {
            rt.incrementCounter();
            rt.addValue("Radius", valuesN[i][0]);
            rt.addValue("Inters.", valuesN[i][1]);
            rt.addValue("Norm Inters. ("+ normalizerString +")", valuesNS[i][1]);
            rt.addValue("log(Radius)", valuesLOG[i][0]);
            rt.addValue("log(Norm Inters.)", valuesLOG[i][0]);

            if (fvaluesN!=null) {
                rt.addValue("Linear Sholl: Polynomial fx (X)", valuesN[i][0]);
                rt.addValue("Linear Sholl: Polynomial fx (Y)", fvaluesN[i]);
            }
            if (fvaluesNS!=null) {
                rt.addValue("Linear (norm.): Power fx (X)", valuesNS[i][0]);
                rt.addValue("Linear (norm.): Power fx (Y)", fvaluesNS[i]);
            }
            //if (fvaluesLOG!=null) {
            //   rt.addValue("Log-log: Exponential (X)", valuesLOG[i][0]);
            //    rt.addValue("Log-log: Exponential (Y)", fvaluesLOG[i]);
            //}
        }

		if (validPath && save) {
			try {
				final String path = imgPath + profileTable;
				rt.saveAs(path + Prefs.get("options.ext", ".csv"));
			} catch (final IOException e) {
				IJ.log(">>>> An error occurred when saving "+ imgTitle +"'s profile(s):\n"+ e);
			}
		}
		if (!validPath || (validPath && !hideSaved))
            rt.show(profileTable);

		String exitmsg = "Done. ";

		if (isCSV)
			{ IJ.showStatus(exitmsg); return; }

        // Create intersections mask, but check first if it is worth proceeding
        if (mask) {
			final ImagePlus maskimg = makeMask(img, imgTitle, (fvaluesN==null ? counts : fvaluesN), x, y, cal);

            if (maskimg == null) {
				IJ.beep(); exitmsg = "Error: Mask could not be created! ";
			} else {
				if (!validPath || (validPath && !hideSaved))
					maskimg.show(); maskimg.updateAndDraw();
			}
		}

        IJ.showProgress(0, 0);
        IJ.showTime(img, img.getStartTime(), exitmsg);

    }

	/**
     * Performs curve fitting, Appends the fitted curve to the Sholl plot, respective
	 * descriptors to the summary table. Returns fitted values or null if values.length
	 * is less than SMALLEST_DATASET
     */
	private static double[] getFittedProfile(final double[][] values, final int method,
			final ResultsTable rt, final Plot plot) {

		final int size = values.length;
		final double[] x = new double[size];
		final double[] y = new double[size];
		for (int i=0; i <size; i++) {
			x[i] = values[i][0];
			y[i] = values[i][1];
		}

		// Define a global analysis title
		final String longtitle = "Sholl Profile ("+ SHOLL_TYPES[method] +") for "+ imgTitle;

		// Abort curve fitting when dealing with small datasets that are prone to
		// inflated coefficients of determination
		if (fitCurve && size <= SMALLEST_DATASET) {
			IJ.log(longtitle +":\nCurve fitting not performed: Not enough data points");
			return null;
		}

		 // Perform fitting
		 final CurveFitter cf = new CurveFitter(x, y);
		 //cf.setRestarts(4); // default: 2;
		 //cf.setMaxIterations(50000); //default: 25000

		if (method == SHOLL_N) {
			if (DEGREES[polyChoice].startsWith("4")) {
				cf.doFit(CurveFitter.POLY4, false);
			} else if (DEGREES[polyChoice].startsWith("5")) {
				cf.doFit(CurveFitter.POLY5, false);
			} else if (DEGREES[polyChoice].startsWith("6")) {
				cf.doFit(CurveFitter.POLY6, false);
			} else if (DEGREES[polyChoice].startsWith("7")) {
				cf.doFit(CurveFitter.POLY7, false);
			} else if (DEGREES[polyChoice].startsWith("8")) {
				cf.doFit(CurveFitter.POLY8, false);
			} else {
				IJ.showStatus("Choosing polynomial of best fit...");
	    		if (verbose)
	    			IJ.log("*** Choosing polynomial of best fit for "+ imgTitle +"...");
				cf.doFit(getBestPolyFit(x,y), false);
			}
		} else if (method == SHOLL_NS) {
			cf.doFit(CurveFitter.POWER, false);
		} else if (method == SHOLL_LOG) {
			cf.doFit(CurveFitter.EXP_WITH_OFFSET, false);
		}

		//IJ.showStatus("Curve fitter status: " + cf.getStatusString());
		final double[] parameters = cf.getParams();

		// Get fitted data
		final double[] fy = new double[size];
		for (int i = 0; i < size; i++)
			fy[i] = cf.f(parameters, x[i]);

		// Initialize plotLabel
		final StringBuffer plotLabel = new StringBuffer();

		// Register quality of fit
		plotLabel.append("\nR\u00B2= "+ IJ.d2s(cf.getRSquared(), 3));

		// Plot fitted curve
		plot.setColor(Color.BLUE);
		plot.setLineWidth(2);
		plot.addPoints(x, fy, PlotWindow.LINE);
		plot.setLineWidth(1);

		if (verbose) {
			IJ.log("\n*** "+ longtitle +", fitting details:"+ cf.getResultString());
		}

		if (method==SHOLL_N) {

			double cv  = 0;	// Polyn. regression: ordinate of maximum
			double cr  = 0; // Polyn. regression: abscissa of maximum
			double mv  = 0; // Polyn. regression: Average value
			double rif = 0; // Polyn. regression: Ramification index

			// Get coordinates of cv, the local maximum of polynomial. We'll
			// iterate around the index of highest fitted value to retrive values
			// empirically. This is probably the most ineficient way of doing it
			final int maxIdx = CurveFitter.getMax(fy);
			final int iterations = 1000;
			final double crLeft  = (x[Math.max(maxIdx-1, 0)] + x[maxIdx]) / 2;
			final double crRight = (x[Math.min(maxIdx+1, size-1)] + x[maxIdx]) / 2;
			final double step = (crRight-crLeft) / iterations;
			double crTmp, cvTmp;
			for (int i = 0; i < iterations; i++) {
				crTmp = crLeft + (i*step);
				cvTmp = cf.f(parameters, crTmp);
				if (cvTmp > cv)
					{ cv = cvTmp; cr = crTmp; }
			}

			// Calculate mv, the mean value of the fitted Sholl function.
			// This can be done assuming that the mean value is the height
			// of a rectangle that has the width of (NonZeroEndRadius -
			// NonZeroStartRadius) and the same area of the area under the
			// fitted curve on that discrete interval
			final double[] xRange = Tools.getMinMax(x);
			for (int i = 0; i < parameters.length-1; i++) //-1?
				mv += (parameters[i]/(i+1)) * Math.pow(xRange[1]-xRange[0], i);

			// Highlight the mean value on the plot
			plot.setLineWidth(1);
			plot.setColor(Color.LIGHT_GRAY);
			plot.drawLine(xRange[0], mv, xRange[1], mv);

			// Calculate the "fitted" ramification index
			rif = (inferPrimary || primaryBranches==0) ? cv/y[0] : cv/primaryBranches;

			// Register parameters
			plotLabel.append("\nCV= "+ IJ.d2s(cv, 2));
			plotLabel.append("\nCR= "+ IJ.d2s(cr, 2));
			plotLabel.append("\nMV= "+ IJ.d2s(mv, 2));
			plotLabel.append("\n"+ (parameters.length-2) + "th degree");

			rt.addValue("Critical value", cv);
			rt.addValue("Critical radius", cr);
			rt.addValue("Mean value", mv);
			rt.addValue("Ramification index (CV)", rif);
			rt.addValue("Polyn. degree", parameters.length-2);
			rt.addValue("Polyn. R^2", cf.getRSquared());

		}

		makePlotLabel(plot, plotLabel.toString(), Color.BLACK);
		rt.show(SHOLLTABLE);
		return fy;

	}

	/**
     * Remove zeros from data. Zero intersections are problematic for logs and polynomials.
	 * Long stretches of zeros (e.g., caused by discontinuous arbors) often cause sharp
	 * "bumps" on the fitted curve. Setting zeros to NaN is not option as it would impact
	 * the CurveFitter.
     */
    public double[][] getNonZeroValues(final double[] xpoints, final  double[] ypoints) {

        final int size = Math.min(xpoints.length, ypoints.length);
        int i, j, nsize = 0;

        for (i=0; i<size; i++)
            { if (xpoints[i]>0.0 && ypoints[i]>0.0) nsize++; }

        final double[][] values = new double[nsize][2];
        for (i=0, j=0; i<size; i++) {
            if (xpoints[i]>0.0 && ypoints[i]>0.0) {
                values[j][0] = xpoints[i];
                values[j++][1] = ypoints[i];
            }
        }

        return values;

    }


    /** Guesses the polynomial of best fit by comparison of coefficient of determination */
    static private int getBestPolyFit(final double x[], final double[] y) {

    	final int[] polyList = { CurveFitter.POLY4, CurveFitter.POLY5, CurveFitter.POLY6,
    							 CurveFitter.POLY7, CurveFitter.POLY8 };

    	int bestFit = 0;
    	double bestRSquared = 0.0;
    	final double[] listRSquared = new double[polyList.length];

    	for (int i=0; i<polyList.length; i++) {

    		final CurveFitter cf = new CurveFitter(x, y);
    		cf.doFit(polyList[i], false);
    		listRSquared[i] = cf.getRSquared();
    		if (listRSquared[i] > bestRSquared) {
    			bestRSquared = listRSquared[i];
    			bestFit = i;
    		}
    		if (verbose)
    			IJ.log( CurveFitter.fitList[polyList[i]] +": R^2= "+ IJ.d2s(listRSquared[i], 5) );

    	}

    	return polyList[bestFit];
    }


    /**
     * Creates the plugin dialog. Returns the region of the image (relative to the center)
     * to be trimmed from the analysis "None", "Above","Below", "Right" or "Left".
     * Returns null if dialog was canceled
     */
    private boolean bitmapPrompt(final double chordAngle, final boolean is3D) {

        final GenericDialog gd = new GenericDialog("Sholl Analysis v"+ VERSION);

        final Font headerFont = new Font("SansSerif", Font.BOLD, 12);
        final int xIndent = 44;

        // Part I: Definition of Shells
        gd.setInsets(-2, 0, 0);
        gd.addMessage("I. Definition of Shells:", headerFont);
        gd.addNumericField("Starting radius", startRadius, 2, 9, unit);
        gd.addNumericField("Ending radius", endRadius, 2, 9, unit);
        gd.addNumericField("Radius_step size", incStep, 2, 9, unit);

        // If an orthogonal chord exists, prompt for hemicircle/hemisphere analysis
        orthoChord = (chordAngle > -1 && chordAngle % 90 == 0);
        if (orthoChord) {
            if (chordAngle == 90.0) {
                quads[0] = QUAD_EAST;
                quads[1] = QUAD_WEST;
            } else {
                quads[0] = QUAD_NORTH;
                quads[1] = QUAD_SOUTH;
            }
            gd.setInsets(0, xIndent, 0);
            gd.addCheckbox("Restrict analysis to hemi"+ (is3D ? "sphere:" : "circle:"), trimBounds);
            gd.setInsets(0, 0, 0);
            gd.addChoice("_", quads, quads[quadChoice]);
        }

        // Part II: Multiple samples (2D) and noise filtering (3D)
        if (is3D) {
            gd.setInsets(2, 0, 2);
            gd.addMessage("II. Noise Reduction:", headerFont);
            gd.setInsets(0, xIndent, 0);
            gd.addCheckbox("Ignore isolated (6-connected) voxels", skipSingleVoxels);
        } else {
            gd.setInsets(10, 0, 2);
            gd.addMessage("II. Multiple Samples per Radius:", headerFont);
            gd.addSlider("#_Samples", 1, 10, nSpans);
            gd.setInsets(0, 0, 0);
            gd.addChoice("Integration", BIN_TYPES, BIN_TYPES[binChoice]);
        }

        // Part III: Indices and Curve Fitting
        gd.setInsets(10, 0, 2);
        gd.addMessage("III. Descriptors and Curve Fitting:", headerFont);
        gd.addNumericField("Enclosing radius cutoff", enclosingCutOff, 0, 6, "intersection(s)");
        gd.addNumericField("#_Primary branches", primaryBranches, 0);
        gd.setInsets(0, 2*xIndent, 0);
        gd.addCheckbox("Infer from Starting radius", inferPrimary);
        gd.setInsets(6, xIndent, 0);
        gd.addCheckbox("Fit profile and compute descriptors", fitCurve);
        gd.setInsets(3, 2*xIndent, 0);
        gd.addCheckbox("Show fitting details", verbose);

        // Part IV: Sholl Methods
        gd.setInsets(10, 0, 2);
        gd.addMessage("IV. Sholl Methods:", headerFont);
        gd.setInsets(0, xIndent/2, 2);
        gd.addMessage("Profiles Without Normalization:");
        gd.setInsets(0, xIndent, 0);
        gd.addCheckbox("Linear", shollN);
        gd.setInsets(0, 0, 0);
        gd.addChoice("Polynomial", DEGREES, DEGREES[polyChoice]);

        gd.setInsets(8, xIndent/2, 2);
        gd.addMessage("Normalized Profiles:");
        gd.setInsets(0, xIndent, 0);
        gd.addCheckboxGroup(1, 3,
        		new String[]{SHOLL_TYPES[SHOLL_NS], SHOLL_TYPES[SHOLL_SLOG],SHOLL_TYPES[SHOLL_LOG]},
        		new boolean[]{shollNS, shollSLOG, shollLOG});

        gd.setInsets(0, 0, 0);
        if (is3D) {
            gd.addChoice("Normalizer", NORMS3D, NORMS3D[normChoice]);
        } else {
            gd.addChoice("Normalizer", NORMS2D, NORMS2D[normChoice]);
        }

        // Part V: Mask and outputs
        gd.setInsets(10, 0, 2);
        gd.addMessage("V. Output Options:", headerFont);
        gd.setInsets(0, xIndent, 0);
        gd.addCheckbox("Create intersections mask", mask);
        gd.addSlider("Background", 0, 255, maskBackground);

        // Offer to save results if local image
        if (validPath) {
            gd.setInsets(2, xIndent, 0);
            gd.addCheckbox("Save results on image directory", save);
			gd.setInsets(0, 2*xIndent, 0);
            gd.addCheckbox("Do not display saved files", hideSaved);
        }

        gd.setHelpLabel("Online Help");
        gd.addHelp(URL);

		// Add listener and update prompt before displaying it
        gd.addDialogListener(this);
		dialogItemChanged(gd, null);
		//gd.setResizable(true);
		gd.showDialog();

        if (gd.wasCanceled())
            return false;
        else if (dialogItemChanged(gd, null)) { // Read dialog result
            return true;
        } else {
            sError("No method(s) chosen.\nAt least one analysis method must be chosen.");
            return false;
        }
    }

    /** Retrieves values from the dialog, disabling dialog components that are not applicable.
        Returns false if user no Analysis method was chosen
     */
    public boolean dialogItemChanged(final GenericDialog gd, final AWTEvent e) {

        final Vector<?> numericfields = gd.getNumericFields();
        final Vector<?> choices = gd.getChoices();
        final Vector<?> checkboxes = gd.getCheckboxes();

        int checkboxCounter = 0;
        int choiceCounter = 0;
        int fieldCounter = 0;

        // Part I: Definition of Shells
        startRadius = Math.max(0, gd.getNextNumber());
        fieldCounter++;
        endRadius = gd.getNextNumber();
        fieldCounter++;
        incStep = Math.max(0, gd.getNextNumber());
        fieldCounter++;

        // Orthogonal chord options
        if (orthoChord) {
            trimBounds = gd.getNextBoolean();
            quadChoice = gd.getNextChoiceIndex();
            quadString = quads[quadChoice];
            checkboxCounter++;
            //final Checkbox ietrimBounds = (Checkbox)checkboxes.elementAt(checkboxCounter++);
            final Choice iequadChoice = (Choice)choices.elementAt(choiceCounter++);
            iequadChoice.setEnabled(trimBounds);
        }

        // Part II: Multiple samples (2D) and noise filtering (3D)
        if (is3D) {
            skipSingleVoxels = gd.getNextBoolean();
            checkboxCounter++;
        } else {
            nSpans = Math.min(Math.max((int)gd.getNextNumber(), 1), 10);
            fieldCounter++;
            binChoice = gd.getNextChoiceIndex();
            final Choice iebinChoice = (Choice)choices.elementAt(choiceCounter++);
            iebinChoice.setEnabled(nSpans>1);
        }

        // Part III: Indices and Curve Fitting
        enclosingCutOff = (int)Math.max(1, gd.getNextNumber());
        fieldCounter++;
        primaryBranches = (int)Math.max(1, gd.getNextNumber());
        final TextField ieprimaryBranches = (TextField)numericfields.elementAt(fieldCounter++);
        inferPrimary = gd.getNextBoolean();
        checkboxCounter++;
        ieprimaryBranches.setEnabled(!inferPrimary);

        fitCurve = gd.getNextBoolean();
        checkboxCounter++;
        verbose = gd.getNextBoolean();
        final Checkbox ieverbose = (Checkbox)checkboxes.elementAt(checkboxCounter++);
        ieverbose.setEnabled(fitCurve);

        // Part IV: Sholl Methods
        shollN = gd.getNextBoolean();
        checkboxCounter++;

        polyChoice = gd.getNextChoiceIndex();
        final Choice iepolyChoice = (Choice)choices.elementAt(choiceCounter++);
        iepolyChoice.setEnabled( fitCurve && shollN );

        shollNS = gd.getNextBoolean();
        checkboxCounter++;
        shollSLOG = gd.getNextBoolean();
        checkboxCounter++;
        shollLOG = gd.getNextBoolean();
        checkboxCounter++;

        normChoice = gd.getNextChoiceIndex();
        final Choice ienormChoice = (Choice)choices.elementAt(choiceCounter++);
        ienormChoice.setEnabled( shollNS || shollSLOG || shollLOG);

        // Part V: Mask and outputs
        mask = gd.getNextBoolean();
        checkboxCounter++;
        //final Checkbox iemask = (Checkbox)checkboxes.elementAt(checkboxCounter++);
        maskBackground = Math.min(Math.max((int)gd.getNextNumber(), 0), 255);
        final TextField iemaskBackground = (TextField)numericfields.elementAt(fieldCounter++);
        iemaskBackground.setEnabled(mask);

        if (validPath) {
            save = gd.getNextBoolean();
			checkboxCounter++;
			hideSaved = gd.getNextBoolean();
			final Checkbox iehideSaved = (Checkbox)checkboxes.elementAt(checkboxCounter++);
			iehideSaved.setEnabled(save);
		}

        // Disable the OK button if no method is chosen
        return (shollN || shollNS || shollSLOG || shollLOG) ? true : false;

    }


    private boolean csvPrompt(final ResultsTable rt) {

        final String[] headings = rt.getHeadings();
        if (headings.length<2 || rt.getCounter()<=SMALLEST_DATASET ) {
            sError("Failed to import profile from "+ headings.length +"x"+ rt.getCounter() +" Results Table.\n"
                    +"At least "+ (SMALLEST_DATASET+1) +" pairs of values are required for curve fitting.");
            return false;
        }

        final GenericDialog gd = new GenericDialog("Sholl Analysis v"+ VERSION +" (Tabular Data)");

        final Font headerFont = new Font("SansSerif", Font.BOLD, 12);
        final int xIndent = 40;

        // Part I: Importing data
        gd.setInsets(0, 0, 0);
        gd.addMessage("I. Results Table Import Options:", headerFont);
        gd.addStringField("Name of dataset", imgTitle, 20);
        gd.addChoice("Distance column", headings, headings[0]);
        gd.addChoice("Intersections column", headings, headings[1]);
        gd.setInsets(0, xIndent, 0);
        gd.addCheckbox("3D data? (uncheck if 2D profile)", is3D);

        // Part II: Indices and Curve Fitting
        gd.setInsets(15, 0, 2);
        gd.addMessage("II. Descriptors and Ramification Indices:", headerFont);
        gd.addNumericField("Enclosing radius cutoff", enclosingCutOff, 0, 6, "intersection(s)");
        gd.addNumericField(" #_Primary branches", primaryBranches, 0);
        gd.setInsets(0, 2*xIndent, 0);
        gd.addCheckbox("Infer from values in first row", inferPrimary);

        // Part III: Sholl Methods
        gd.setInsets(15, 0, 2);
        gd.addMessage("III. Sholl Methods:", headerFont);
        gd.setInsets(0, xIndent/2, 2);
        gd.addMessage("Profiles Without Normalization:");
        gd.setInsets(0, xIndent, 0);
        gd.addCheckbox("Linear", shollN);
        gd.setInsets(0, 0, 0);
        gd.addChoice("Polynomial", DEGREES, DEGREES[polyChoice]);

        gd.setInsets(8, xIndent/2, 2);
        gd.addMessage("Normalized Profiles:");
        gd.setInsets(0, xIndent, 0);
        gd.addCheckboxGroup(1, 3,
                            new String[]{SHOLL_TYPES[SHOLL_NS], SHOLL_TYPES[SHOLL_SLOG],SHOLL_TYPES[SHOLL_LOG]},
                            new boolean[]{shollNS, shollSLOG, shollLOG});

        final String[] norms = new String[NORMS3D.length];
        for(int i=0; i<norms.length; i++) {
            norms[i] = NORMS2D[i] +"/"+ NORMS3D[i];
        }
        gd.setInsets(2, 0, 0);
        gd.addChoice("Normalizer", norms, norms[normChoice]);

        gd.setInsets(15, 0, 2);
        gd.addMessage("IV. Output Options:", headerFont);
        gd.setInsets(0, xIndent, 0);
        gd.addCheckbox("Show fitting details", verbose);
        if (validPath) {
            gd.setInsets(0, xIndent, 0);
            gd.addCheckbox("Save results on directory of profile", save);
            gd.setInsets(0, xIndent, 0);
            gd.addCheckbox("Do not display saved files", hideSaved);
        }

        gd.setHelpLabel("Online Help");
        gd.addHelp(URL + "#Importing");
        gd.showDialog();

        if (gd.wasCanceled())
            return false;

        imgTitle = gd.getNextString();
        final int rColumn = gd.getNextChoiceIndex();
        final int cColumn = gd.getNextChoiceIndex();
        is3D = gd.getNextBoolean();

        enclosingCutOff = (int)Math.max(1, gd.getNextNumber());
        primaryBranches = (int)Math.max(1, gd.getNextNumber());
        inferPrimary = gd.getNextBoolean();

        shollN = gd.getNextBoolean();
        polyChoice = gd.getNextChoiceIndex();
        shollNS = gd.getNextBoolean();
        shollSLOG = gd.getNextBoolean();
        shollLOG = gd.getNextBoolean();
        normChoice = gd.getNextChoiceIndex();

        verbose = gd.getNextBoolean();
        if (validPath) {
        	save = gd.getNextBoolean();
        	hideSaved = gd.getNextBoolean();
        }

        if ( rColumn==cColumn ) {
        	sError("\"Distance\" and \"Intersections\" columns cannot be the same.");
        	return false;
        }

        counts = rt.getColumnAsDoubles(cColumn);
        radii = rt.getColumnAsDoubles(rColumn);
        stepRadius = rt.getValueAsDouble(rColumn, 1) - rt.getValueAsDouble(rColumn, 0);
        if ( Double.isNaN(stepRadius) || stepRadius<=0 ) {
			IJ.log("*** Warning: Could not determine radius step size for "+ imgTitle
					+ "\n*** Normalizations involving "+ norms[norms.length-1] +" will not be relevant");
        }

        if ( !(shollN || shollNS || shollSLOG || shollLOG) ) {
            sError("No method(s) chosen.\nAt least one analysis method must be chosen.");
            return false;
        }

        return true;

    }


    /** Measures intersections for each sphere surface */
    static public double[] analyze3D(final int xc, final int yc, final int zc,
            final double[] radii, final ImagePlus img) {

        int nspheres, xmin, ymin, zmin, xmax, ymax, zmax;
        double dx, value;

        // Create an array to hold results
        final double[] data = new double[nspheres = radii.length];

        // Get Image Stack
        final ImageStack stack = img.getStack();

        for (int s = 0; s < nspheres; s++) {

            IJ.showProgress(s, nspheres);
            IJ.showStatus("Sampling sphere "+ (s+1) +"/"+ nspheres +". Press 'Esc' to abort...");
            if (IJ.escapePressed())
                { IJ.beep(); mask = false; return data; }

            // Initialize ArrayLists to hold surface points
            final ArrayList<int[]> points = new ArrayList<int[]>();

            // Restrain analysis to the smallest volume for this sphere
            xmin = Math.max(xc - (int)Math.round(radii[s]/vxWH), minX);
            ymin = Math.max(yc - (int)Math.round(radii[s]/vxWH), minY);
            zmin = Math.max(zc - (int)Math.round(radii[s]/vxD), minZ);
            xmax = Math.min(xc + (int)Math.round(radii[s]/vxWH), maxX);
            ymax = Math.min(yc + (int)Math.round(radii[s]/vxWH), maxY);
            zmax = Math.min(zc + (int)Math.round(radii[s]/vxD), maxZ);

            for (int z=zmin; z<=zmax; z++) {
                for (int y=ymin; y<ymax; y++) {
                    for (int x=xmin; x<xmax; x++) {
                        dx = Math.sqrt((x-xc) * vxWH * (x-xc) * vxWH
                                     + (y-yc) * vxWH * (y-yc) * vxWH
                                     + (z-zc) * vxD  * (z-zc) * vxD);
                        if (Math.abs(dx-radii[s])<0.5) {
                            value = stack.getVoxel(x,y,z);
                            if (value >= lowerT && value <= upperT) {
                                if (hasNeighbors(x,y,z,stack))
                                    points.add( new int[]{x,y,z} );
                            }
                        }
                    }
                }
            }

            // We now have the the points intercepting the surface of this Sholl sphere.
            // Lets check if their respective pixels are clustered
            data[s] = count3Dgroups(points);

            // Exit as soon as a sphere has no interceptions
                //if (points.size()==0) break;
        }
        return data;

    }

    /** Returns true if at least one of the 6-neighboring voxels of this position is thresholded */
    static private boolean hasNeighbors(final int x, final int y, final int z, final ImageStack stack) {

        if (!skipSingleVoxels)
            return true;  // Do not proceed if secludeSingleVoxels is not set

        final int[][] neighboors = new int[6][3];

        // Out of bounds positions will have a value of zero
        neighboors[0] = new int[]{x-1, y, z};
        neighboors[1] = new int[]{x+1, y, z};
        neighboors[2] = new int[]{x, y-1, z};
        neighboors[3] = new int[]{x, y+1, z};
        neighboors[4] = new int[]{x, y, z+1};
        neighboors[5] = new int[]{x, y, z-1};

        boolean clustered = false;

        for (int i=0; i<neighboors.length; i++) {
            final double value = stack.getVoxel(neighboors[i][0], neighboors[i][1], neighboors[i][2] );
            if (value >= lowerT && value <= upperT) {
                clustered = true;
                break;
            }
        }

        return clustered;

    }

    /**
     * Analogous to countGroups(), counts clusters of 26-connected voxels from an ArrayList
     * of 3D coordinates. SpikeSupression is not performed
     */
    static public int count3Dgroups(final ArrayList<int[]> points) {

        int target, source, groups, len;

        final int[] grouping = new int[len = groups = points.size()];

        for (int i = 0; i < groups; i++)
            grouping[i] = i + 1;

        for (int i = 0; i < len; i++) {
            //IJ.showProgress(i, len+1);
            for (int j = 0; j < len; j++) {
                if (i == j)
                    continue;

                // Compute the chessboard (Chebyshev) distance for this point. A chessboard
                // distance of 1 in xy (lateral) underlies 8-connectivity within the plane.
                // A distance of 1 in z (axial) underlies 26-connectivity in 3D
                final int lDist = Math.max(Math.abs(points.get(i)[0] - points.get(j)[0]),
                                           Math.abs(points.get(i)[1] - points.get(j)[1]));
                final int aDist = Math.max(Math.abs(points.get(i)[2] - points.get(j)[2]),lDist);
                if ( (lDist*aDist<=1) && (grouping[i] != grouping[j]) ) {
                    source = grouping[i];
                    target = grouping[j];
                    for (int k = 0; k < len; k++)
                        if (grouping[k] == target)
                            grouping[k] = source;
                    groups--;
                }
            }
        }
        return groups;

    }

    /**
     * Does the actual 2D analysis. Accepts an array of radius values and takes
     * the measurements for each
     */
    static public double[] analyze2D(final int xc, final int yc,
            final double[] radii, final double pixelSize, final int binsize,
            final int bintype, final ImageProcessor ip) {

        int i, j, k, rbin, sum, size;
        int[] binsamples, pixels;
        int[][] points;
        double[] data;

        // Create an array to hold the results
        data = new double[size = radii.length];

        // Create array for bin samples. Passed value of binsize must be at least 1
        binsamples = new int[binsize];

        IJ.showStatus("Sampling "+ size +" radii, "+ binsize
                    + " measurement(s) per radius. Press 'Esc' to abort...");

        // Outer loop to control the analysis bins
        for (i = 0; i < size; i++) {

            // Retrieve the radius in pixel coordinates and set the largest
            // radius of this bin span
            rbin = (int) Math.round(radii[i]/pixelSize + binsize/2);

            // Inner loop to gather samples for each bin
            for (j = 0; j < binsize; j++) {

                // Get the circumference pixels for this radius
                points = getCircumferencePoints(xc, yc, rbin--);
                pixels = getPixels(ip, points);

                // Count the number of intersections
                binsamples[j] = countTargetGroups(pixels, points, ip);

            }

            IJ.showProgress(i, size * binsize);
            if (IJ.escapePressed()) {
                IJ.beep(); return data;
            }

            // Statistically combine bin data
            if (binsize > 1) {
                if (bintype == BIN_MEDIAN) {

                    // Sort the bin data
                    Arrays.sort(binsamples);

                    // Pull out the median value: average the two middle values if no
                    // center exists otherwise pull out the center value
                    if (binsize % 2 == 0)
                        data[i] = ((double) (binsamples[binsize/2] + binsamples[binsize/2 -1])) /2.0;
                    else
                        data[i] = (double) binsamples[binsize/2];

                } else if (bintype == BIN_AVERAGE) {

                    // Mean: Find the samples sum and divide by n. of samples
                    for (sum = 0, k = 0; k < binsize; k++)
                        sum += binsamples[k];
                    data[i] = ((double) sum) / ((double) binsize);

                }

                // There was only one sample
            } else
                data[i] = binsamples[0];

        }

        return data;
    }

    /**
     * Counts how many groups of non-zero pixels are present in the given data. A
     * group consists of a formation of adjacent pixels, where adjacency is true
     * for all eight neighboring positions around a given pixel.
     */
    static public int countTargetGroups(final int[] pixels, final int[][] rawpoints,
            final ImageProcessor ip) {

        int i, j;
        int[][] points;

        // Count how many target pixels (i.e., foreground, non-zero) we have
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0.0)
                j++;

        // Create an array to hold target pixels
        points = new int[j][2];

        // Copy all target pixels into the array
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0.0)
                points[j++] = rawpoints[i];

        return countGroups(points, ip);

    }

    /**
     * For a set of points in 2D space, counts how many groups (clusters) of 8-connected
     * pixels exist.
     */
    static public int countGroups(final int[][] points, final ImageProcessor ip) {

        int i, j, k, target, source, groups, len, dx;

        // Create an array to hold the point grouping data
        final int[] grouping = new int[len = points.length];

        // Initialize each point to be in a unique group
        for (i = 0, groups = len; i < groups; i++)
            grouping[i] = i + 1;

        for (i = 0; i < len; i++)
            for (j = 0; j < len; j++) {

                // Don't compare the same point with itself
                if (i == j)
                    continue;

                // Compute the chessboard (Chebyshev) distance. A distance of 1
                // underlies 8-connectivity
                dx = Math.max( Math.abs(points[i][0] - points[j][0]),
                               Math.abs(points[i][1] - points[j][1]));

                // Should these two points be in the same group?
                if ((dx==1) && (grouping[i] != grouping[j])) {

                    // Record which numbers we're changing
                    source = grouping[i];
                    target = grouping[j];

                    // Change all targets to sources
                    for (k = 0; k < len; k++)
                        if (grouping[k] == target)
                            grouping[k] = source;

                    // Update the number of groups
                    groups--;

                }
            }

        if (doSpikeSupression)
            groups -= countSinglePixels(points, len, grouping, ip);

        return groups;
    }

    /** Counts 1-pixel groups that exist solely on the edge of a "stair" of target pixels */
    static public int countSinglePixels(final int[][] points, final int pointsLength,
            final int[] grouping, final ImageProcessor ip) {

        int counts = 0;

        for (int i = 0; i < pointsLength; i++) {

            // Check for other members of this group
            boolean multigroup = false;
            for (int j=0; j<pointsLength; j++) {
                if (i == j)
                    continue;
                if (grouping[i] == grouping[j]) {
                    multigroup = true;
                    break;
                }
            }

            // If not a single-pixel group, try again
            if (multigroup)
                continue;

            // Store the coordinates of this point
            final int dx = points[i][0];
            final int dy = points[i][1];

            // Calculate the 8 neighbors surrounding this point
            final int[][] testpoints = new int[8][2];
            testpoints[0][0] = dx-1;   testpoints[0][1] = dy+1;
            testpoints[1][0] = dx  ;   testpoints[1][1] = dy+1;
            testpoints[2][0] = dx+1;   testpoints[2][1] = dy+1;
            testpoints[3][0] = dx-1;   testpoints[3][1] = dy  ;
            testpoints[4][0] = dx+1;   testpoints[4][1] = dy  ;
            testpoints[5][0] = dx-1;   testpoints[5][1] = dy-1;
            testpoints[6][0] = dx  ;   testpoints[6][1] = dy-1;
            testpoints[7][0] = dx+1;   testpoints[7][1] = dy-1;

            // Pull out the pixel values for these points
            final int[] px = getPixels(ip, testpoints);

            // Now perform the stair checks
            if ((px[0]!=0 && px[1]!=0 && px[3]!=0 && px[4]==0 && px[6]==0 && px[7]==0) ||
                (px[1]!=0 && px[2]!=0 && px[4]!=0 && px[3]==0 && px[5]==0 && px[6]==0) ||
                (px[4]!=0 && px[6]!=0 && px[7]!=0 && px[0]==0 && px[1]==0 && px[3]==0) ||
                (px[3]!=0 && px[5]!=0 && px[6]!=0 && px[1]==0 && px[2]==0 && px[4]==0))

                counts++;

        }

        return counts;

    }

    /**
     * For a given set of points, returns values of 1 for pixel intensities
     * within the thresholded range, otherwise returns 0 values
     */
    static public int[] getPixels(final ImageProcessor ip, final int[][] points) {

        int value;

        // Initialize the array to hold the pixel values. Arrays of integral
        // types have a default value of 0
        final int[] pixels = new int[points.length];

        // Put the pixel value for each circumference point in the pixel array
        for (int i = 0; i < pixels.length; i++) {

            // We already filtered out of bounds coordinates in getCircumferencePoints
            value = ip.getPixel(points[i][0], points[i][1]);
            if (value >= lowerT && value <= upperT)
                pixels[i] = 1;
        }

        return pixels;

    }

    /**
     * Returns the location of pixels clockwise along a (1-pixel wide) circumference
     * using Bresenham's Circle Algorithm
     */
    static public int[][] getCircumferencePoints(final int cx, final int cy, final int radius) {

        // Initialize algorithm variables
        int i = 0, x = 0, y = radius;
        final int r = radius+1;
        int err = 0, errR, errD;

        // Array to store first 1/8 of points relative to center
        final int[][] data = new int[r][2];

        do {
            // Add this point as part of the circumference
            data[i][0] = x;
            data[i++][1] = y;

            // Calculate the errors for going right and down
            errR = err + 2 * x + 1;
            errD = err - 2 * y + 1;

            // Choose which direction to go
            if (Math.abs(errD) < Math.abs(errR)) {
                y--;
                err = errD; // Go down
            } else {
                x++;
                err = errR; // Go right
            }
        } while (x <= y);

        // Create an array to hold the absolute coordinates
        final int[][] points = new int[r*8][2];

        // Loop through the relative circumference points
        for (i = 0; i < r; i++) {

            // Pull out the point for quick access;
            x = data[i][0];
            y = data[i][1];

            // Convert the relative point to an absolute point
            points[i][0] = x + cx;
            points[i][1] = y + cy;

            // Use geometry to calculate remaining 7/8 of the circumference points
            points[r*4-i-1][0] =  x + cx;   points[r*4-i-1][1] = -y + cy;
            points[r*8-i-1][0] = -x + cx;   points[r*8-i-1][1] =  y + cy;
            points[r*4+i]  [0] = -x + cx;   points[r*4+i]  [1] = -y + cy;
            points[r*2-i-1][0] =  y + cx;   points[r*2-i-1][1] =  x + cy;
            points[r*2+i]  [0] =  y + cx;   points[r*2+i]  [1] = -x + cy;
            points[r*6+i]  [0] = -y + cx;   points[r*6+i]  [1] =  x + cy;
            points[r*6-i-1][0] = -y + cx;   points[r*6-i-1][1] = -x + cy;

        }

        // Count how many points are out of bounds, while eliminating
        // duplicates. Duplicates are always at multiples of r (8 points)
        int pxX, pxY, count = 0, j = 0;
        for (i = 0; i < points.length; i++) {

            // Pull the coordinates out of the array
            pxX = points[i][0];
            pxY = points[i][1];

            if ((i+1)%r!=0 && pxX>=minX && pxX<=maxX && pxY>=minY && pxY<=maxY)
                count++;
        }

        // Create the final array containing only unique points within bounds
        final int[][] refined = new int[count][2];

        for (i = 0; i < points.length; i++) {

            pxX = points[i][0];
            pxY = points[i][1];

            if ((i+1)%r!=0 && pxX>=minX && pxX<=maxX && pxY>=minY && pxY<=maxY) {
                refined[j][0] = pxX;
                refined[j++][1] = pxY;

            }

        }

        // Return the array
        return refined;

    }

    /**
     * Creates a 2D Sholl heatmap by applying measured values to the foregroud pixels
     * of a copy of the analyzed image
     */
    private ImagePlus makeMask(final ImagePlus img, final String ttl, final double[] values,
            final int xc, final int yc, final Calibration cal) {

        // Check if analyzed image remains available
        if ( img.getWindow()==null ) return null;

        IJ.showStatus("Preparing intersections mask...");

        ImageProcessor ip;

        // Work on a stack projection when dealing with a volume
        if (is3D) {
            final ZProjector zp = new ZProjector(img);
            zp.setMethod(ZProjector.MAX_METHOD);
            zp.setStartSlice(minZ);
            zp.setStopSlice(maxZ);
            zp.doProjection();
            ip = zp.getProjection().getProcessor();
        } else {
            ip = img.getProcessor();
        }

        // Sholl mask as a 16-bit image. Any negative values from polynomial will become 0
        final ImageProcessor mp = new ShortProcessor(ip.getWidth(), ip.getHeight());

        final int drawSteps = values.length;
        final int firstRadius = (int) Math.round( startRadius/vxWH );
        final int lastRadius = (int) Math.round( (startRadius + (drawSteps-1)*stepRadius)/vxWH) ;
        final int drawWidth = (int) Math.round( (lastRadius-startRadius)/drawSteps );

        for (int i = 0; i < drawSteps; i++) {

            IJ.showProgress(i, drawSteps);
            int drawRadius = firstRadius + (i*drawWidth);

            for (int j = 0; j < drawWidth; j++) {

                // this will already exclude pixels out of bounds
                final int[][] points = getCircumferencePoints(xc, yc, drawRadius++);
                for (int k = 0; k < points.length; k++)
                    for (int l = 0; l < points[k].length; l++) {
                        final double value = ip.getPixel(points[k][0], points[k][1]);
                        if (value >= lowerT && value <= upperT)
                            mp.putPixelValue(points[k][0], points[k][1], values[i]);
                    }
            }
        }

        // Apply LUT
        mp.setColorModel(matlabJetColorMap(maskBackground));
        //(new ContrastEnhancer()).stretchHistogram(mp, 0.35);
        final double[] range = Tools.getMinMax(values);
        mp.setMinAndMax(0, range[1]);

        final String title = ttl + "_ShollMask.tif";
        final ImagePlus img2 = new ImagePlus(title, mp);

        // Apply calibration, set mask label and mark center of analysis
        img2.setCalibration(cal);
        img2.setProperty("Label", (fitCurve && shollN) ? "Fitted data" : "Raw data");
        img2.setRoi(new PointRoi(xc, yc));

        if (validPath && save) {
            IJ.save(img2, imgPath + title);
        }
        return img2;

    }

    /**
      * Checks if image is valid (segmented grayscale), sets validPath and returns its
      * ImageProcessor
      */
    private ImageProcessor getValidProcessor(final ImagePlus img) {

        ImageProcessor ip = null;
        String exitmsg = "";

        if (img==null) {
            exitmsg = "There are no images open.";
        } else if (img.isComposite()) {
            exitmsg = "Composite images are not supported.";
        } else {
            ip = img.getProcessor();
            final int type = img.getBitDepth();
            if (type==24)
                exitmsg = "RGB color images are not supported.";
            else if (type==32)
                exitmsg = "32-bit grayscale images are not supported.";
            else {  // 8/16-bit grayscale image

                final double lower = ip.getMinThreshold();
                if (ip.isBinary() && lower==ImageProcessor.NO_THRESHOLD) {
                    lowerT = upperT = 255;
                    if (ip.isInvertedLut()) {
                        ip.setThreshold(lowerT, upperT, ImageProcessor.RED_LUT);
                        img.updateAndDraw();
                    }
                } else if (lower==ImageProcessor.NO_THRESHOLD)
                    exitmsg = "Image is not thresholded.";
                else {
                    lowerT = (int) lower;
                    upperT = (int) ip.getMaxThreshold();
                }

            }
        }

        if (!"".equals(exitmsg)) {
            lError(exitmsg + "\n \nThis plugin requires a segmented arbor. Either:\n"
                  + "    - A binary image (Arbor: non-zero value)\n"
                  + "    - A thresholded grayscale image (8/16-bit)");
            return null;
        }

        // Retrieve image path and check if it is valid
        imgPath = IJ.getDirectory("image");
        if (imgPath == null) {
            validPath = false;
        } else {
            final File dir = new File(imgPath);
            validPath = dir.exists() && dir.isDirectory();
        }

        return ip;
    }

    /**
     * Returns an IndexColorModel similar to MATLAB's jet color map. An 8-bit gray color
     * level specified by grayvalue is mapped to index idx.
     */
    public static IndexColorModel matlabJetColorMap(final int backgroundGray) {

        // Initialize colors arrays (zero-filled by default)
        final byte[] reds   = new byte[256];
        final byte[] greens = new byte[256];
        final byte[] blues  = new byte[256];

        // Set greens, index 0-32; 224-255: 0
        for( int i = 0; i < 256/4; i++ )         // index 32-96
            greens[i+256/8] = (byte)(i*255*4/256);
        for( int i = 256*3/8; i < 256*5/8; ++i ) // index 96-160
            greens[i] = (byte)255;
        for( int i = 0; i < 256/4; i++ )         // index 160-224
            greens[i+256*5/8] = (byte)(255-(i*255*4/256));

        // Set blues, index 224-255: 0
        for(int i = 0; i < 256*7/8; i++)         // index 0-224
            blues[i] = greens[(i+256/4) % 256];

        // Set reds, index 0-32: 0
        for(int i = 256/8; i < 256; i++)         // index 32-255
            reds[i] = greens[(i+256*6/8) % 256];

        // Set background color
        reds[0] = greens[0] = blues[0] = (byte)backgroundGray;

        return new IndexColorModel(8, 256, reds, greens, blues);

    }

    /**
     * Calculates the centroid of a non-self-intersecting closed polygon as
     * described in http://en.wikipedia.org/wiki/Centroid#Centroid_of_polygon
     * It is assumed that arrays of x,y points have the same size
     */
    public static double[] baryCenter(final double[] xpoints, final double[] ypoints) {

        double area = 0, sumx = 0, sumy = 0;
        for (int i=1; i<xpoints.length; i++) {
            final double cfactor = (xpoints[i-1]*ypoints[i]) - (xpoints[i]*ypoints[i-1]);
            sumx += (xpoints[i-1] + xpoints[i]) * cfactor;
            sumy += (ypoints[i-1] + ypoints[i]) * cfactor;
            area += cfactor/2;
        }
        return new double[] { sumx/(6*area), sumy/(6*area) };

    }

     /** Retrieves the median of an array */
    private static double getMedian(final double[] array) {

        final int size = array.length;
        final double[] sArray = array.clone();
        Arrays.sort(sArray);
        final double median;
        if (size % 2 == 0)
            median = (sArray[size/2] + sArray[size/2 -1])/2;
        else
            median = sArray[size/2];
        return median;

    }

    /** Returns the Sholl Summary Table populated with profile statistics */
	private static ResultsTable createStatsTable(final String rowLabel, final int xc,
			final int yc, final int zc, final double[][] values) {

		ResultsTable rt;
		final Frame window = WindowManager.getFrame(SHOLLTABLE);
		if (window == null)
			rt = new ResultsTable();
		else
			rt = ((TextWindow) window).getTextPanel().getResultsTable();

			double sumY = 0, maxIntersect = 0, maxR = 0, enclosingR = 0;

		// Retrieve simple statistics
		final int size = values.length;
		final double[] x = new double[size];
		final double[] y = new double[size];
		for (int i=0; i <size; i++) {
			x[i] = values[i][0];
			y[i] = values[i][1];
			if ( y[i] > maxIntersect )
				{ maxIntersect = y[i]; maxR = x[i]; }
			if (y[i] >= enclosingCutOff)
				enclosingR = x[i];
			sumY += y[i];
		}

		// Calculate the smallest circle/sphere enclosing the arbor
		//final double lastR = x[size-1];
		//final double field = is3D ? Math.PI*4/3*lastR*lastR*lastR : Math.PI*lastR*lastR;

		// Calculate ramification index, the maximum of intersection divided by the n.
		// of primary branches, assumed to be the n. intersections at starting radius
		final double ri = (inferPrimary || primaryBranches==0) ? maxIntersect / y[0] : maxIntersect / primaryBranches;

		rt.incrementCounter();
		rt.setPrecision(getPrecision());
		rt.addLabel("Image", rowLabel + " (" + unit + ")");
		rt.addValue("Lower threshold", isCSV ? Double.NaN : lowerT);
		rt.addValue("Upper threshold", isCSV ? Double.NaN : upperT);
		rt.addValue("X center (px)", isCSV ? Double.NaN : xc);
		rt.addValue("Y center (px)", isCSV ? Double.NaN : yc);
		rt.addValue("Z center (slice)", isCSV ? Double.NaN : zc);
		rt.addValue("Starting radius", isCSV ? Double.NaN : startRadius);
		rt.addValue("Ending radius", isCSV ? Double.NaN : endRadius);
		rt.addValue("Radius step", stepRadius);
		rt.addValue("Samples/radius", (isCSV || is3D) ? 1 : nSpans);
		rt.addValue("Enclosing radius cutoff", enclosingCutOff);
		rt.addValue("I branches (User)", (inferPrimary || primaryBranches==0) ? Double.NaN : primaryBranches);
		rt.addValue("I branches (Inferred)", (inferPrimary || primaryBranches==0) ? y[0] : Double.NaN);

		rt.addValue("Intersecting radii", size);
		rt.addValue("Sum inters.", sumY);
		rt.addValue("Mean inters.", sumY/size);
		rt.addValue("Median inters.", getMedian(y));
		rt.addValue("Max inters.", maxIntersect);
		rt.addValue("Max inters. radius", maxR);
		rt.addValue("Ramification index (sampled)", ri);

		// Calculate the 'center of mass' for the sampled curve (linear Sholl);
		centroid = baryCenter(x, y);
		rt.addValue("Centroid radius", centroid[0]);
		rt.addValue("Centroid inters.", centroid[1]);

		rt.addValue("Enclosing radius", enclosingR);

		//rt.addValue("Enclosed field", field);
		rt.show(SHOLLTABLE);
		return rt;

	}

    /** Transforms data */
    private static double[][] transformValues(final double[][] values, final boolean normY,
			final boolean logY, final boolean logX) {

        double x, y;
        final double[][] transValues = new double[values.length][2];

        for (int i=0; i<values.length; i++) {

            x = values[i][0]; y = values[i][1];

            if (normY) {
                if (normChoice==AREA_NORM) {
                    if (is3D)
                        transValues[i][1] = y / (Math.PI * x*x*x * 4/3); // Volume of sphere
                    else
                        transValues[i][1] = y / (Math.PI * x*x); // Area of circle
                } else if (normChoice==PERIMETER_NORM) {
                    if (is3D)
                        transValues[i][1] = y / (Math.PI * x*x * 4); // Surface area of sphere
                    else
                        transValues[i][1] = y / (Math.PI * x * 2); // Length of circumference
                } else if (normChoice==ANNULUS_NORM) {
                	final double r1 = x - stepRadius/2;
                	final double r2 = x + stepRadius/2;
                    if (is3D)
                        transValues[i][1] = y / ( Math.PI * 4/3 * (r2*r2*r2 - r1*r1*r1) ); // Volume of spherical shell
                    else
                        transValues[i][1] = y / ( Math.PI * (r2*r2 - r1*r1) ); // Area of annulus
                }
            } else
                transValues[i][1] = y;

            if (logY)
                transValues[i][1] = Math.log(y);

            if (logX)
                transValues[i][0] = Math.log(x);
            else
                transValues[i][0] = x;
        }

        return transValues;

    }

    /** Returns a plot with some axes customizations*/
    private static Plot plotValues(final String title, final String xLabel,
			final String yLabel, final double[][] xy, final int method) {

        // Extract values
        final int size = xy.length;
        final double[] x0 = new double[size];
        final double[] y0 = new double[size];
        for (int i=0; i <size; i++) {
            x0[i] = xy[i][0];
            y0[i] = xy[i][1];
        }

        // Create an empty plot
        final double[] empty = null;
        final int flags = Plot.X_FORCE2GRID + Plot.X_TICKS + Plot.X_NUMBERS
                        + Plot.Y_FORCE2GRID + Plot.Y_TICKS + Plot.Y_NUMBERS;
        final Plot plot = new Plot(title, xLabel, yLabel, empty, empty, flags);

        // Set limits
        final double[] xScale = Tools.getMinMax(x0);
        final double[] yScale = Tools.getMinMax(y0);
        setPlotLimits(plot, method, xScale, yScale);

        // Add data (default color is black)
        plot.setColor(Color.GRAY);
        plot.addPoints(x0, y0, Plot.CROSS);

        return plot;

    }

    /** Sets plot limits imposing grid lines */
    private static void setPlotLimits(final Plot plot, final int method,
    		final double[] xScale, final double[] yScale) {

        final boolean gridState = PlotWindow.noGridLines;
        PlotWindow.noGridLines = false;
		//if (method==SHOLL_NS) {
		//	plot.setLogScaleY();
		//}
        plot.setLimits(xScale[0], xScale[1], yScale[0], yScale[1]);
        PlotWindow.noGridLines = gridState;

    }

	/** Calls plotRegression for both regressions */
    private static void plotRegression(final double[][]values, final Plot plot,
			final ResultsTable rt, final String method) {

		final int size = values.length;
		final double[] x = new double[size];
		final double[] y = new double[size];
		for (int i=0; i <size; i++) {
			x[i] = values[i][0];
			y[i] = values[i][1];
		}
		plotRegression(x, y, false, plot, rt, method);
		plotRegression(x, y, true, plot, rt, method);

	}


    /** Performs linear regression using the full range data or percentiles 10-90 */
    private static void plotRegression(final double[] x, final double[] y, final boolean trim,
			final Plot plot, final ResultsTable rt, final String method) {

		final int size = x.length;
		int start = 0;
		int end = size-1;

		Color color = Color.BLUE;
		final StringBuffer label = new StringBuffer();
		String labelSufix = " ("+ method +")";

		final CurveFitter cf;
		if (trim) {

			color = Color.RED;
			labelSufix += " [P10-P90]"; //"[P\u2081\u2080 - P\u2089\u2080]";
			start = (int)(size*0.10);
			end   = (int)(end-start);

			// Do not proceed if there are not enough points
			if (end <= SMALLEST_DATASET)
				return;

			final double[] xtrimmed = Arrays.copyOfRange(x, start, end);
			final double[] ytrimmed = Arrays.copyOfRange(y, start, end);

			cf = new CurveFitter(xtrimmed, ytrimmed);

		} else {
			cf = new CurveFitter(x, y);
		}

		cf.doFit(CurveFitter.STRAIGHT_LINE, false);
		final double[] cfparam = cf.getParams();
		//IJ.log("Curve fitter status: " + cf.getStatusString());

		if (verbose) {
			IJ.log("\n*** "+ imgTitle +", regression details"
		        + labelSufix + cf.getResultString());
		}

		final double x1 = x[start];
		final double x2 = x[end];
		final double y1 = cf.f(cfparam, x1);
		final double y2 = cf.f(cfparam, x2);

		plot.setLineWidth(2);
		plot.setColor(color);
		plot.drawLine(x1, y1, x2, y2);

		final double k = -cfparam[1];	// slope
		final double kIntercept = cfparam[0];	// y-intercept
		final double kRSquared = cf.getRSquared();	// R^2

		rt.addValue("Regression coefficient"+ labelSufix, k);
		rt.addValue("Regression intercept"+ labelSufix, kIntercept);
		rt.addValue("Regression R^2"+ labelSufix, kRSquared);
		rt.show(SHOLLTABLE);

		label.append("R\u00B2= "+ IJ.d2s(kRSquared, 3));
		label.append("\nk= "+ IJ.d2s(k, -2));
		label.append("\nIntercept= "+ IJ.d2s(kIntercept,2));

		markPlotPoint(plot, new double[]{0, kIntercept}, color);
		makePlotLabel(plot, label.toString(), color);

	}

	/** Highlights a point on a plot without listing it on the plot table  */
    public static void markPlotPoint(final Plot plot, final double[] coordinates,
			final Color color) {

		plot.setLineWidth(6); // default markSize: 5;
		plot.setColor(color);
		plot.drawLine(coordinates[0], coordinates[1], coordinates[0], coordinates[1]);

		// restore defaults
		plot.setLineWidth(1);
		plot.setColor(Color.BLACK);

    }

    /** Draws a label at the less "crowded" corner of a plot canvas */
    public static void makePlotLabel(final Plot plot, final String label, final Color color) {

        final int margin = 7; // Axes internal margin, 1+Plot.TICK_LENGTH
        final ImageProcessor ip = plot.getProcessor();

        int maxLength = 0; String maxLine = "";
        final String[] lines = Tools.split(label, "\n");
        for (int i = 0; i<lines.length; i++) {
            final int length = lines[i].length();
            if (length>maxLength)
                { maxLength = length; maxLine = lines[i]; }
        }

        final FontMetrics metrics = ip.getFontMetrics();
        final int textWidth = metrics.stringWidth(maxLine+" ");
        final int lineHeight = metrics.getHeight();
        final int textHeight = lineHeight * lines.length;

        final int yTop = Plot.TOP_MARGIN + margin + lineHeight;
		final int yBottom = Plot.TOP_MARGIN + PlotWindow.plotHeight - textHeight;
        final int xLeft  = Plot.LEFT_MARGIN + margin;
        final int xRight = Plot.LEFT_MARGIN + PlotWindow.plotWidth - margin - textWidth;

        final double northEast = meanRoiValue(ip, xLeft, yTop, textWidth, textHeight);
        final double northWest = meanRoiValue(ip, xRight, yTop, textWidth, textHeight);
        final double southEast = meanRoiValue(ip, xLeft, yBottom, textWidth, textHeight);
        final double southWest = meanRoiValue(ip, xRight, yBottom, textWidth, textHeight);
        final double pos = Math.max(Math.max(northEast, northWest), Math.max(southEast,southWest));

		ip.setColor(color);
		if (pos==northEast) {
		    ip.drawString(label, xLeft, yTop);
		} else if (pos==northWest) {
		    ip.drawString(label, xRight, yTop);
		} else if (pos==southEast) {
		    ip.drawString(label, xLeft, yBottom);
		} else {
		    ip.drawString(label, xRight, yBottom);
		}

    }


    /** Returns the mean value of a rectangular ROI */
    private static double meanRoiValue(final ImageProcessor ip, final int x, final int y,
			final int width, final int height) {

		ip.setRoi(x, y, width, height);
		return ImageStatistics.getStatistics(ip, Measurements.MEAN, null).mean;

	}


    /** Saves plot according to imgPath */
    private static void savePlot(final Plot plot, final int shollChoice) {

    	if (!validPath || (validPath && !hideSaved))
            plot.show();
        if (validPath && save) {
        	Recorder.disablePathRecording();
            final String path = imgPath + imgTitle +"_ShollPlot"+ SHOLL_TYPES[shollChoice] + ".png";
            IJ.saveAs(plot.getImagePlus(), "png", path);
        }

    }

    /** Retrieves precision according to Analyze>Set Measurements... */
    private static int getPrecision() {

        final boolean sNotation = (Analyzer.getMeasurements()&Measurements.SCIENTIFIC_NOTATION)!=0;
        int precision = Analyzer.getPrecision();
        if (sNotation)
            precision = -precision;
        return precision;

    }

    /** Creates improved error messages with help button */
    private void error(final String msg, final boolean extended) {

		if (IJ.macroRunning())
            IJ.error("Sholl Analysis Error", msg);
        else {
            final GenericDialog gd = new GenericDialog("Sholl Analysis Error");
            gd.setInsets(0,0,0);
            gd.addMessage(msg);
            if (extended) {
                gd.setInsets(6,0,0);
                gd.addCheckbox("Open sample arbor (2D) of Drosophila neuron", false);
            }
            gd.addHelp(URL);
            gd.setHelpLabel("Online Help");
            gd.hideCancelButton();
            gd.showDialog();
            if (gd.getNextBoolean())
                IJ.runPlugIn("Sholl_Utils", "sample");
        }

    }

	/** Simple error message */
    private void sError(final String msg) {
        error(msg, false);
    }

	/** Extended error message */
    private void lError(final String msg) {
        error(msg, true);
    }

    /** Returns a filename that does not include extension */
    private String trimExtension(String filename) {
        final int index = filename.lastIndexOf(".");
        if (index>0)
            filename = filename.substring(0, index);
        return filename;
    }
    
    /**
     * Returns the skewness and kurtosis values for an array of univariate data. 
     * Skewness: Negative values indicate data that is skewed left (left tail is longer than
     *           right tail); positive values indicate data skewed right (longer right tail),
     *           defined according to http://en.wikipedia.org/wiki/Skewness#Sample_skewness
     * Kurtosis: Positive values indicate "peaked" distributions; a negative value indicates a
     *           "flat" distribution. A normal distribution has a kurtosis of 0. Here, defined
     *           according to http://en.wikipedia.org/wiki/Kurtosis#Sample_kurtosis
     */
    private final double[] getSkewnessKurtosis(final double values[], final double mean) {
    	final int npoints = values.length;
    	double numS = 0;
    	double numK = 0;
    	double denm = 0;
    	for (int i=0; i<npoints; i++) {
    		denm += (values[i]-mean) * (values[i]-mean);
    		numS += denm * (values[i]-mean);
    		numK += numS * (values[i]-mean);
    	}
    	final double skewness = (1/npoints * numS) / Math.pow((1/npoints * denm), 3.0/2.0);
    	final double kurtosis = ((1/npoints * numK) / Math.pow((1/npoints * denm), 2)) - 3;
    	return new double[] { skewness, kurtosis };
    }

}
