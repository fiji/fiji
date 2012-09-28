import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.Tools;
import ij.WindowManager;
import ij.text.*;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * @author Tiago Ferreira v2.0 Feb 13, 2012
 * @author Tom Maddock    v1.0 Oct 26, 2005
 * 
 *         Performs Sholl Analysis on binary images of previously segmented or
 *         traced arbors. Several analysis methods are available: Linear (N),
 *         Linear (N/S), Semi-log and Log-log as described in Milosevic and
 *         Ristanovic, J Theor Biol (2007) 245(1)130-40. 
 *         Background is always considered to be 0, independently of the 
 *         Prefs.blackBackground flag.
 * 
 *         This program is free software; you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt).
 * 
 */

public class Advanced_Sholl_Analysis implements PlugIn {

    /* Plugin Information */
    public static final String VERSION = "2.3a";
    public static final String URL = "http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:asa:start";

    /* Bin Function Type Definitions */
    private static final String[] BIN_TYPES = { "Mean", "Median" };
    private static final int BIN_AVERAGE = 0;
    private static final int BIN_MEDIAN = 1;

    /* Sholl Type Definitions */
    private static final String[] SHOLL_TYPES = { "Intersections",
            "Inters./Area", "Semi-Log", "Log-Log" };
    private static final int SHOLL_N = 0;
    private static final int SHOLL_NS = 1;
    private static final int SHOLL_SLOG = 2;
    private static final int SHOLL_LOG = 3;
    private static final String[] DEGREES = { "4th degree", "5th degree",
            "6th degree", "7th degree", "8th degree" };

    /* Default parameters and input values */
    private static double startRadius = 10.0;
    private static double endRadius = 100.0;
    private static double incStep = 10;
    private static double spanWidth = 0;
    private static int binChoice = BIN_AVERAGE;
    private static int shollChoice = SHOLL_N;
    private static boolean fitCurve = true;
    private static int polyChoice = 1;
    private static boolean verbose = false;
    private static boolean makeMask = true;
    private static String pxUnit = "Pixels";
    private static double pxSize = 1;
    private static int size;


    public void run(String arg) {

        if (IJ.versionLessThan("1.46h")) return;

        // Get current image and the ImageProcessor for the image
        ImagePlus img = IJ.getImage();
        ImageProcessor ip = img.getProcessor();

        // Make sure image is of the right type
        if (!ip.isBinary()) {
            error("8-bit binary image (Arbor: 255; Background: 0) required.\n"
                + "   Use \"Image>Adjust>Threshold...\" to binarize image.");
            return;
        }

        // Get image calibration and define Radius span accordingly
        Calibration cal = img.getCalibration();
        if (cal != null && cal.pixelHeight == cal.pixelWidth) {
            pxUnit = cal.getUnits();
            pxSize = cal.pixelHeight;
        }

        // Get current ROI
        Roi roi = img.getRoi();

        if (!IJ.macroRunning() && !((roi != null && roi.getType() == Roi.LINE) || 
                                    (roi != null && roi.getType() == Roi.POINT))) {

            WaitForUserDialog wd = new WaitForUserDialog("Advanced Sholl Analysis v"
                + VERSION,"Please define the center of analysis using\n"
                        + "the Point Selection Tool or, alternatively, by\n"
                        + "creating a straight line starting at the center.");
            wd.show();
            if (wd.escPressed())
                return;

            // Get new ROI, in case it has changed
            roi = img.getRoi();

        }

        // Initialize center coordinates
        int x, y;

        if (roi != null && roi.getType() == Roi.LINE) {

            // Get center coordinates and length of line
            Line line = (Line) roi;
            x = line.x1;
            y = line.y1; 
            endRadius = line.getRawLength() * pxSize;

        } else if (roi != null && roi.getType() == Roi.POINT) {

            // Get center coordinates
            PointRoi point = (PointRoi) roi;
            Rectangle rect = point.getBounds();
            x = rect.x;
            y = rect.y;

        } else {

            // Not a proper ROI type
            error("Line or Point selection required.");
            return;
        }

        // Create the plugin dialog
        GenericDialog gd = new GenericDialog("Advanced Sholl Analysis v" + VERSION);
        gd.addNumericField("Starting radius:", startRadius, 2, 9, pxUnit);
        gd.addNumericField("Ending radius:", endRadius, 2, 9, pxUnit);
        gd.addNumericField("Radius_step size:", incStep, 2, 9, pxUnit);
        gd.addNumericField("Radius_span:", spanWidth, 2, 9, pxUnit);
        gd.addChoice("Span_type:", BIN_TYPES, BIN_TYPES[binChoice]);
        gd.addChoice("Sholl method:", SHOLL_TYPES, SHOLL_TYPES[shollChoice]);
        gd.setInsets(10, 6, 0);
        gd.addCheckbox("Fit profile and compute descriptors", fitCurve);
        gd.setInsets(3, 34, 3);
        gd.addCheckbox("Show parameters", verbose);
        gd.addChoice("Polynomial:", DEGREES, DEGREES[polyChoice]);
        gd.setInsets(5, 6, 0);
        gd.addCheckbox("Create intersections mask", makeMask);
        gd.setHelpLabel("Online Help");
        gd.addHelp(URL);
        gd.showDialog();

        // Exit if user pressed cancel
        if (gd.wasCanceled())
            return;

        // Get values from dialog
        startRadius = Math.max(pxSize, gd.getNextNumber());
        endRadius   = Math.max(pxSize, gd.getNextNumber());
        incStep     = Math.max(pxSize, gd.getNextNumber());
        spanWidth   = Math.max(pxSize, gd.getNextNumber());
        binChoice   = gd.getNextChoiceIndex();
        shollChoice = gd.getNextChoiceIndex();
        fitCurve    = gd.getNextBoolean();
        verbose     = gd.getNextBoolean();
        polyChoice  = gd.getNextChoiceIndex();
        makeMask    = gd.getNextBoolean();

        // Impose valid parameters & restrict Radius span to an acceptable value
        startRadius = (startRadius > endRadius) ? pxSize : startRadius;
        incStep = (incStep > (endRadius - startRadius)) ? pxSize : incStep;
        int nSpans = Math.min(10, (int)(spanWidth/pxSize));

        // Calculate how many samples will be taken
        int size = (int) ((endRadius - startRadius) / incStep) + 1;

        // Exit if there are no samples
        if (size == 1) {
            error("Invalid Parameters: Ending Radius \u2264 Starting radius!");
            return;
        }

        long start = System.currentTimeMillis();

        // Create arrays for x-values and radii
        int[] radii = new int[size];
        double[] xvalues = new double[size];

        // Populate arrays
        for (int i = 0; i < size; i++) {
            xvalues[i] = startRadius + i * incStep;
            radii[i] = (int) Math.round(xvalues[i] / pxSize);
        }

        // Analyze the data and return raw Sholl intersections
        double[] yvalues = analyze(x, y, radii, nSpans, binChoice, ip);

        IJ.showStatus("Preparing Results...");

        // Display the plot and return transformed data if valid counts exist
        double[] grays = plotValues(img.getTitle(), shollChoice, radii,
                xvalues, yvalues, nSpans, x, y);

        // Exit if no valid data was gathered
        if (grays.length==0) {
           error("All intersection counts were zero!");
           return;
        }

        // Create intersections mask
        if (makeMask) {

            IJ.showStatus("Preparing intersections mask...");

            ImagePlus img2 = IJ.createImage("Sholl mask [" +
                SHOLL_TYPES[shollChoice] + "] for " + img.getTitle(),
                "32-bit black", img.getWidth(), img.getHeight(), 1);

            ImageProcessor ip2 = img2.getProcessor();

            int[][] points;
            int i, j, k, l, drawRadius;
            int drawSteps = grays.length;
            int drawWidth = (int) (((endRadius - startRadius) / pxSize) / drawSteps);

            for (i = 0; i < drawSteps; i++) {

                IJ.showProgress(i, drawSteps);
                drawRadius = (int) ((startRadius / pxSize) + (i * drawWidth));

                for (j = 0; j < drawWidth; j++) {
                    points = getCircumferencePoints(x, y, drawRadius++);

                    for (k = 0; k < points.length; k++) {
                        for (l = 0; l < points[k].length; l++) {

                            if (ip.getPixel(points[k][0], points[k][1]) != 0)
                                ip2.putPixelValue(points[k][0], points[k][1], grays[i]);

                        }
                    }
                }
            }

            // Store type of data in mask label and mark center of analysis
            String metadata = "Raw data";
            if (fitCurve)
                metadata = "Fitted data";
            img2.setProperty("Label", metadata);
            img2.setRoi(new PointRoi(x, y));

            // Adjust levels, apply original calibration and display mask
            double[] levels = Tools.getMinMax(grays);
            IJ.run(img2, "Fire", ""); // "Fire", "Ice", "Spectrum", "Redgreen"
            ip2.setMinAndMax(levels[0], levels[1]);
            img2.setCalibration(cal);
            img2.show();

        }

        IJ.showProgress(0, 0);
        IJ.showStatus("Finished. "
                + IJ.d2s((System.currentTimeMillis() - start) / 1000.0, 2)
                + " seconds");

    }

    /*
     * Does the actual analysis. Accepts an array of radius values and takes the
     * measurements for each
     */
    static public double[] analyze(int x, int y, int[] radii, int binsize,
            int bintype, ImageProcessor ip) {

        int i, j, k, r, rbin, sum, size;
        int[] binsamples, pixels;
        int[][] points;
        double[] data;

        // Create an array to hold the results
        data = new double[size = radii.length];

        // Create an array for the bin samples. We must ensure that the passed
        // binsize is at least 1
        binsamples = new int[binsize];

        // Outer loop to control the analysis bins
        for (i = 0; i < size; i++) {

            IJ.showStatus("Sampling radius "+ i +"/"+ size +". "+ binsize
                        + " measurement(s) per span...");

            // Get the radius we are sampling
            r = radii[i];

            // Set the last radius for this bin
            rbin = r + (int) Math.round((double) binsize / 2.0);

            // Inner loop to gather samples for each bin
            for (j = 0; j < binsize; j++) {

                // Get the circumference pixels for this radius
                points = getCircumferencePoints(x, y, rbin--);
                pixels = getPixels(ip, points);

                // Count the number of intersections
                binsamples[j] = countTargetGroups(pixels, points, 255, ip);

            }

            IJ.showProgress(i, size * binsize);

            // Statistically combine bin data
            if (binsize > 1) {
                if (bintype==BIN_MEDIAN) {

                    // Sort the bin data
                    Arrays.sort(binsamples);

                    // Pull out the median value
                    data[i] = (binsize % 2 == 0)

                    // Average the two middle values if no center exists
                    ? ((double) (binsamples[binsize / 2] + binsamples[binsize / 2 - 1])) / 2.0

                    // Pull out the center value
                    : (double) binsamples[binsize / 2];

                } else if (bintype==BIN_AVERAGE) {

                    // Mean: Find the sum of the samples and divide by n. of samples
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

    /* Counts how many groups of value v are present in the given data.
     * A group consists of a formation of adjacent pixels, where adjacency
     * is true for all eight neighboring positions around a given pixel. */
    static public int countTargetGroups(int[] pixels, int[][] rawpoints, int v,
            ImageProcessor ip) {

        int i, j;
        int[][] points;

        // Count how many target pixels (i.e., foreground, non-zero) we have
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] == v) j++;

        // Create an array to hold target pixels
        points = new int[j][2];

        // Copy all target pixels into the array
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] == v)
                points[j++] = rawpoints[i];

        return countGroups(points, 1.5, 255, ip);

    }

    /*
     * For a set of points in 2D space, counts how many groups of value v there
     * are such that for every point in each group, there exists another point
     * in the same group that is less than threshold units of distance away. If
     * a point is greater than threshold units away from all other points, it is
     * in its own group. For threshold=1.5, this is equivalent to 8-connected
     * clusters
     */
    static public int countGroups(int[][] points, double threshold, int v,
        ImageProcessor ip) {

        double distance;
        int i, j, k, target, source, dx, dy, groups, len;

        // Create an array to hold the point grouping data
        int[] grouping = new int[len = points.length];

        // Initialize each point to be in a unique group
        for (i = 0, groups = len; i < groups; i++)
            grouping[i] = i + 1;

        for (i = 0; i < len; i++)
            for (j = 0; j < len; j++) {

                // Don't compare the same point with itself
                if (i == j)
                    continue;

                // Compute the distance between the two points
                dx = points[i][0] - points[j][0];
                dy = points[i][1] - points[j][1];
                distance = Math.sqrt(dx * dx + dy * dy);

                // Should these two points be in the same group?
                if ((distance <= threshold) && (grouping[i] != grouping[j])) {

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

        // DoSpikeSupression: If the edge of the group lies tangent to the
        // sampling circle, multiple intersections with that circle will be
        // counted. We will try to find these "false positives" and throw them
        // out. A way to attempt this (we will be missing some of them) is to
        // throw out 1-pixel groups that exist solely on the edge of a "stair"
        // of target pixels. Testing reveals it is always best to 
        if (true) {
            boolean multigroup;
            int[] px;
            int[][] testpoints = new int[8][2];

            for (i = 0; i < len; i++) {

                // Check for other members of this group
                for (multigroup = false, j = 0; j < len; j++) {
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

                // Save the coordinates of this point
                dx = points[i][0];
                dy = points[i][1];

                // Calculate the 8 neighbors surrounding this point
                testpoints[0][0] = dx-1; testpoints[0][1] = dy+1;
                testpoints[1][0] = dx  ; testpoints[1][1] = dy+1;
                testpoints[2][0] = dx+1; testpoints[2][1] = dy+1;
                testpoints[3][0] = dx-1; testpoints[3][1] = dy  ;
                testpoints[4][0] = dx+1; testpoints[4][1] = dy  ;
                testpoints[5][0] = dx-1; testpoints[5][1] = dy-1;
                testpoints[6][0] = dx  ; testpoints[6][1] = dy-1;
                testpoints[7][0] = dx+1; testpoints[7][1] = dy-1;

                // Pull out the pixel values for these points
                px = getPixels(ip, testpoints);

                // Now perform the stair checks
                if ( (px[0]==v && px[1]==v && px[3]==v  &&
                      px[4]!=v && px[6]!=v && px[7]!=v) ||
                     (px[1]==v && px[2]==v && px[4]==v  &&
                      px[3]!=v && px[5]!=v && px[6]!=v) ||
                     (px[4]==v && px[6]==v && px[7]==v  &&
                      px[0]!=v && px[1]!=v && px[3]!=v) ||
                     (px[3]==v && px[5]==v && px[6]==v  &&
                      px[1]!=v && px[2]!=v && px[4]!=v) )

                    groups--;

                }
        }

        return groups;
    }

    /* Returns the pixel values for a given set of points */
    static public int[] getPixels(ImageProcessor ip, int[][] points) {

        int x, y;

        // Create the array to hold the pixel values
        int[] pixels = new int[points.length];

        // Put the pixel value for each circumference point in the pixel array
        for (int i = 0; i < pixels.length; i++) {

            // Pulls the coordinates out of the array
            x = points[i][0];
            y = points[i][1];

            // Check if the coordinates are valid for the image
            pixels[i] = (x< 0 || x>= ip.getWidth() || y< 0 || y>= ip.getHeight())

            // Use -1 to indicate an invalid pixel location, otherwise get the
            // pixel value in the image
            ? -1 : ip.getPixel(points[i][0], points[i][1]);

        }

        return pixels;
    }

    /*
     * Returns the location of pixels clockwise along a (1-pixel wide)
     * circumference using Bresenham's Circle Algorithm
     */
    static public int[][] getCircumferencePoints(int cx, int cy, int r) {

        // Initialize algorithm variables
        int i = 0, x = 0, y = r, err = 0, errR, errD;

        // Array to store first 1/8 of points relative to center
        int[][] data = new int[++r][2];

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
        int[][] points = new int[r * 8][2];

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

        // Create a new array to hold points without duplicates
        int[][] refined = new int[Math.max(points.length - 8, 1)][2];

        // Copy the first point manually
        refined[0] = points[0];

        // Loop through the rest of the points
        for (i = 1, x = 1; i < points.length; i++) {

            // Duplicates are always at multiples of r
            if ((i + 1) % r == 0) continue;

            // Copy the non-duplicate
            refined[x++] = points[i];

        }

        // Return the array without duplicates
        return refined;

    }

    /* Creates Results table, Sholl plot and curve fitting */
    static public double[] plotValues(String ttl, int mthd, int[] r,
            double[] xpoints, double[] ypoints, int spanSamples, int xcenter, int ycenter) {

        int size = ypoints.length;
        int i, j, nsize = 0;

        // Remove points with zero intersections avoiding log(0)
        for (i = 0; i < size; i++)
            if (ypoints[i] != 0) nsize++;

        // Do not proceed if there are no counts
        if (nsize==0) return new double[0];

        // get non-zero values & calculate "log intersections". The latter will
        // be used for non-traditional Sholls and to calculate the Sholl decay
        double[] x = new double[nsize];
        double[] y = new double[nsize];
        double[] logY = new double[nsize];
        double sumY = 0.0;
        for (i = 0, j = 0; i < size; i++) {

            if (ypoints[i] != 0.0) {
                x[j] = xpoints[i];
                y[j] = ypoints[i];
                sumY += ypoints[i];
                logY[j++] = Math.log(ypoints[i] / (Math.PI * r[i] * r[i]));
            }

        }

        // Place parameters on a dedicated table
        ResultsTable rt;
        String shollTable = "Sholl Results";
        Frame window = WindowManager.getFrame(shollTable);
        if (window == null)
            rt = new ResultsTable();
        else
            rt = ((TextWindow) window).getTextPanel().getResultsTable();

        rt.incrementCounter();
        rt.addLabel("Image", ttl + " (" + pxUnit + ")");
        rt.addValue("Method N.", mthd+1);
        rt.addValue("X center (px)", xcenter);
        rt.addValue("Y center (px)", ycenter);
        rt.addValue("Starting radius", startRadius);
        rt.addValue("Ending radius", endRadius);
        rt.addValue("Radius step", incStep);
        rt.addValue("Radius span", spanWidth);
        rt.addValue("Sampled radii", size);
        rt.addValue("Samples within span", spanSamples);
        rt.addValue("Sum Inters.", sumY);
        rt.addValue("Avg Inters.", sumY / nsize);
        rt.show(shollTable);

        // Define default plot axes
        double[] xScale = Tools.getMinMax(x);
        double[] yScale = Tools.getMinMax(y);
        String yTitle = "N. of Intersections";
        String xTitle = "Radius (" + pxUnit + ")";

        // Adjust axes for log Methods
        if (mthd == SHOLL_SLOG || mthd == SHOLL_LOG) {

            yScale = Tools.getMinMax(logY);
            yTitle = "log(N. Inters./Circle area)";

            if (mthd == SHOLL_LOG) {
                for (i = 0; i < nsize; i++)
                    x[i] = Math.log(x[i]);
                xScale = Tools.getMinMax(x);
                xTitle = "log(Radius)";
            }

        } else if (mthd == SHOLL_NS) {

            for (i = 0; i < nsize; i++)
                y[i] = y[i] / (Math.PI * r[i] * r[i]);
            yScale = Tools.getMinMax(y);
            yTitle = "N. Inters./Circle area (" + pxUnit + "\u00B2)";

        }

        // Creat the plot
        int FLAGS = Plot.X_FORCE2GRID + Plot.X_TICKS + Plot.X_NUMBERS
                + Plot.Y_FORCE2GRID + Plot.Y_TICKS + Plot.Y_NUMBERS
                + PlotWindow.CIRCLE;
        PlotWindow.noGridLines = false;
        float[] mock = null; // start an empty plot

        Plot plot = new Plot("Sholl Plot [" + SHOLL_TYPES[mthd] + "] for " + ttl,
                             xTitle, yTitle, mock, mock, FLAGS);
        plot.setLimits(xScale[0], xScale[1], yScale[0], yScale[1]);

        // Plot original data (default color is black)
        if (mthd == SHOLL_SLOG || mthd == SHOLL_LOG)
            plot.addPoints(x, logY, Plot.CIRCLE);
        else
            plot.addPoints(x, y, Plot.CIRCLE);

        // Plot fitted data and retrieve descriptors from the fit. Return raw
        // data if no fitting is done
        if (!fitCurve) {

            plot.show();
            return y;

        } else {

            // Fitting small data sets is prone to inflated coefficients of
            // determination. Testing suggests that a sample of at least 6 data
            // points is required for meaningful results
            if (nsize <= 6) {
                IJ.log("\nSholl Analysis for " + ttl
                     + ":\nCurve fitting not performed as it requires more"
                     + "than 6\nsampled points. Parameters must be adjusted");
                plot.show();
                return y;
            }

            // Initialize plot label listing fitting details
            String label = "";

            // By default, calculate the Sholl decay, i.e., the slope of the
            // fitted regression on Semi-log Sholl
            CurveFitter cf = new CurveFitter(x, logY);
            cf.doFit(CurveFitter.STRAIGHT_LINE, false);

            // Get parameters of fit
            double[] parameters = cf.getParams();
            label = "k= " + IJ.d2s(parameters[0], -3);
            rt.addValue("Sholl decay", parameters[0]);
            rt.addValue("R^2", cf.getRSquared());

            // Perform fits not involving log transformation of intersections
            if (mthd == SHOLL_N || mthd == SHOLL_NS)
                cf = new CurveFitter(x, y);

            // cf.setRestarts(2); // default: 2;
            // cf.setMaxIterations(25000); //default: 25000

            if (mthd == SHOLL_N) {
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
                }
            } else if (mthd == SHOLL_NS) {
                cf.doFit(CurveFitter.POWER, false);
            } else if (mthd == SHOLL_LOG) {
                cf.doFit(CurveFitter.EXP_WITH_OFFSET, false);
            }

            // Get parameters of fitted function
            if (mthd != SHOLL_SLOG)
                parameters = cf.getParams();

            // Get fitted data
            double[] fy = new double[nsize];
            for (i = 0; i < nsize; i++)
                fy[i] = cf.f(parameters, x[i]);

            // Linear Sholl: Calculate Critical value (cv), Critical radius (cr),
            // Mean Sholl value (mv) and Ramification (Schoenen) index (ri)
            if (mthd == SHOLL_N) {

                double cv = 0.0, cr = 0.0, mv = 0.0, ri;

                // Get coordinates of cv, the local maximum of polynomial. We'll
                // iterate around the index of highest fitted value to retrive
                // values empirically. This is obviously inelegant
                int maxIdx = cf.getMax(fy);
                int iterations = 1000;
                double crLeft = (x[maxIdx - 1] + x[maxIdx]) / 2;
                double crRight = (x[Math.min(maxIdx+1, nsize-1)] + x[maxIdx]) / 2;
                double step = (crRight - crLeft) / iterations;
                double crTmp, cvTmp;
                for (i = 0; i < iterations; i++) {
                    crTmp = crLeft + (i * step);
                    cvTmp = cf.f(parameters, crTmp);
                    if (cvTmp > cv)
                        { cv = cvTmp; cr = crTmp; }
                }
                rt.addValue("Critical value", cv);
                rt.addValue("Critical radius", cr);

                // Calculate mv, the mean value of the fitted Sholl function.
                // This can be done assuming that the mean value is the height
                // of a rectangle that has the width of (NonZeroEndRadius -
                // NonZeroStartRadius) and the same area of the area under the
                // fitted curve on that discrete interval
                for (i = 0; i < parameters.length - 1; i++) // -1?
                    mv += (parameters[i]/(i+1)) * Math.pow(xScale[1]-xScale[0], i);
                rt.addValue("Mean value", mv);

                // Highlight the mean Sholl value on the plot
                plot.setLineWidth(1);
                plot.setColor(Color.lightGray);
                plot.drawLine(xScale[0], mv, xScale[1], mv);

                // Calculate the ramification index: cv/N. of primary branches
                ri = cv / y[0];
                rt.addValue("Ramification index", ri);

                // Register polynomial order
                rt.addValue("R^2 (polyn.)", cf.getRSquared());
                rt.addValue("Polyn. degree", parameters.length-2);

                // Add calculated parameters to plot label
                label += "\nCv= " + IJ.d2s(cv, 2)
                       + "\nCr= " + IJ.d2s(cr, 2)
                       + "\nMv= " + IJ.d2s(mv, 2)
                       + "\nRI= " + IJ.d2s(ri, 2)
                       + "\n" + DEGREES[polyChoice];
            }

            // Append quality of fit to label
            label += "\nR\u00B2= " + IJ.d2s(cf.getRSquared(), 3);

            // Add label to plot
            plot.changeFont(new Font("SansSerif", Font.PLAIN, 11));
            plot.setColor(Color.black);
            plot.addLabel(0.8, 0.085, label);

            // Plot fitted curve
            plot.setColor(Color.gray);
            plot.setLineWidth(2);
            plot.addPoints(x, fy, PlotWindow.LINE);

            if (verbose) {
                IJ.log("\n*** Fitting details - Sholl Analysis [" + 
                    SHOLL_TYPES[mthd] + "] for " + ttl + cf.getResultString());
            }

            // Show the plot window, update results and return fitted data
            plot.show();
            rt.show(shollTable);
            return fy;
        }
    }

    /* Creates improved error messages */
    void error(String msg) {
        if (IJ.macroRunning())
            IJ.error("Advanced Sholl Analysis Error", msg);
        else {
            GenericDialog gd = new GenericDialog("Advanced Sholl Analysis Error");
            Font font = new Font("SansSerif", Font.PLAIN, 13);
            gd.addMessage(msg, font);
            gd.addHelp(URL);
            gd.setHelpLabel("Online Help");
            gd.hideCancelButton();
            gd.showDialog();
        }
    }
}
