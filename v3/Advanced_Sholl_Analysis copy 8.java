import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.Prefs;
import ij.process.*;
import ij.ImageStack;
import ij.util.Tools;
import ij.WindowManager;
import ij.text.*;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.*;
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
    public static final String VERSION = "3.0a";
    public static final String URL = "http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:asa:start";

    /* Bin Function Type Definitions */
    private static final String[] BIN_TYPES = { "Mean", "Median" };
    private static final int BIN_AVERAGE = 0;
    private static final int BIN_MEDIAN  = 1;

    /* Sholl Type Definitions */
    private static final String[] SHOLL_TYPES = { "Intersections",
            "Inters./", "Semi-Log", "Log-Log" };
    private static final int SHOLL_N    = 0;
    private static final int SHOLL_NS   = 1;
    private static final int SHOLL_SLOG = 2;
    private static final int SHOLL_LOG  = 3;
    private static final String[] DEGREES = { "4th degree", "5th degree",
            "6th degree", "7th degree", "8th degree" };

    /* Default parameters and input values */
    private static double startRadius = 10.0;
    private static double endRadius   = 100.0;
    private static double stepRadius  = 1;
    private static double incStep     = 0;
    private static int    nSpans      = 1;
    private static int binChoice      = BIN_AVERAGE;
    private static int shollChoice    = SHOLL_N;
    private static int polyChoice     = 1;
    private static boolean fitCurve;
    private static boolean verbose;
    private static boolean mask;
    private static boolean saveValues;

    /* Common variables */
    private static boolean IS_3D;
    private static double x_spacing = 1;
    private static double y_spacing = 1;
    private static double z_spacing = 1;
    private static double pxSize    = 1;
    private static String Unit      = "pixels";

    /* Boundaries of analysis */
    private static boolean restrict;
    private static double chordAngle;
    private static int belowOrLeft = 0;
    private static String[] QUADS = new String[2];
    private static int minX;
    private static int maxX;
    private static int minY;
    private static int maxY;
    private static int minZ;
    private static int maxZ;

    public void run(String arg) {

        if (IJ.versionLessThan("1.46h")) return;

        // Initialize center coordinates
        int x, y, z;

        // Get current image, the ImageProcessor for the image and its title
        ImagePlus img = IJ.getImage();
        ImageProcessor ip = img.getProcessor();
        String title = img.getTitle();

        IS_3D = img.getStackSize() > 1;

        // Make sure image is of the right type
        if ( !ip.isBinary() ) { // (img.getType() != ImagePlus.GRAY8)
            error("8-bit binary image (Arbor: non-zero value) required.\n"
                + "Use \"Image>Adjust>Threshold...\" to binarize image.");
            return;
        }

        // Retrieve image path and check if it is valid
        String imgPath = IJ.getDirectory("image");
        boolean validPath = false;
        if (imgPath!=null) {
            File dir = new File(imgPath);
            validPath = dir.exists() && dir.isDirectory();
        }

        // Get image calibration. Stacks are likely to have anisotropic voxels.
        // In this case, it does not make sense to use steps smaller than the
        // largest dimension, typically depth
        Calibration cal = img.getCalibration();
        if( cal != null ) {
            x_spacing = cal.pixelWidth;
            y_spacing = cal.pixelHeight;
            z_spacing = cal.pixelDepth;
            Unit = cal.getUnits();
            pxSize = (x_spacing + y_spacing) / 2;
        }

        // Get parameters from current ROI. Prompt for one if no exists
        Roi roi = img.getRoi();

        if (!IJ.macroRunning() && !((roi != null && roi.getType() == Roi.LINE) ||
                                    (roi != null && roi.getType() == Roi.POINT))) {

            WaitForUserDialog wd = new WaitForUserDialog("Advanced Sholl Analysis v"
                + VERSION,"Please define the center of analysis using\n"
                        + "the Point Selection Tool or, alternatively, by\n"
                        + "creating a straight line starting at the center.");
            wd.show();

            if (wd.escPressed()) return;

            // Get new ROI in case it has changed
            roi = img.getRoi();

        }

        if (roi != null && roi.getType() == Roi.LINE) {

            // Get center coordinates, length and angle of chord
            Line chord = (Line)roi;
            x = chord.x1;
            y = chord.y1;
            endRadius= chord.getLength(); // calibrated units

            chordAngle= Math.abs(chord.getAngle(x, y, chord.x2, chord.y2));
            if (chordAngle%90==0) {
                restrict = true;
                if (chordAngle!=90.0)
                    { QUADS[0] = "Above line";  QUADS[1] ="Below line"; }
                else
                    { QUADS[0] = "Right of line";  QUADS[1] = "Left of line"; }
            } else
                restrict = false;


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

        z =  img.getCurrentSlice();

        // Create the plugin dialog
        GenericDialog gd = new GenericDialog("Advanced Sholl Analysis v" + VERSION);
        gd.addNumericField("Starting radius:", startRadius, 2, 9, Unit);
        gd.addNumericField("Ending radius:", endRadius, 2, 9, Unit);
        gd.addNumericField("Radius_step size:", incStep, 2, 9, Unit);

        // If 2D, allow multiple samples per radius
        if (!IS_3D) {
            //gd.addSlider("Samples per radius:", 1, 6, 1);
			gd.addNumericField("Samples per radius:", nSpans, 0, 3, "(1-10)");
			gd.setInsets(0,0,0);
            gd.addChoice("Samples_integration:", BIN_TYPES, BIN_TYPES[binChoice]);
        }

        // Personalize Sholl choices to reflect 2D/3D Sholl
        String nsChoice = SHOLL_TYPES[SHOLL_NS].replaceFirst("[^/]+$", "");
        SHOLL_TYPES[SHOLL_NS] = IS_3D ? nsChoice+"Volume" : nsChoice+"Area";

        gd.setInsets(12, 0, 0);
        gd.addChoice("Sholl method:", SHOLL_TYPES, SHOLL_TYPES[shollChoice]);

        // If an orthogonal chord exists, retrieve choice of quadrants
        if (restrict) {
            gd.setInsets(12, 6, 3);
            gd.addCheckbox("Restrict analysis to circular segment", false);
            gd.addChoice("_", QUADS, QUADS[0]);
            gd.setInsets(6, 6, 0);
        } else
            gd.setInsets(12, 6, 0);

        gd.addCheckbox("Fit profile and compute descriptors", fitCurve);
        int chckbxpstn = IS_3D ? 33 : 56;
        gd.setInsets(3, chckbxpstn, 3);
        gd.addCheckbox("Show parameters", verbose);
        gd.addChoice("Polynomial:", DEGREES, DEGREES[polyChoice]);
        gd.setInsets(6, 6, 0);
        gd.addCheckbox("Create intersections mask", mask);

        if (validPath) {
            gd.setInsets(5, 6, 0);
            gd.addCheckbox("Save plot values on image folder", saveValues);
        }

		//gd.setOKLabel("Run");
        gd.setHelpLabel("Online Help");
        gd.addHelp(URL);
        gd.showDialog();

        // Exit if user pressed cancel
        if (gd.wasCanceled()) return;

        // Get values from dialog
        startRadius = Math.max(pxSize, gd.getNextNumber());
        endRadius   = Math.max(pxSize, gd.getNextNumber());
        incStep     = Math.max(0, gd.getNextNumber());

        if (!IS_3D) {
            nSpans    = (int)Math.max(1, gd.getNextNumber());
			nSpans    = Math.min(10, nSpans);
            binChoice = gd.getNextChoiceIndex();
        }

        shollChoice = gd.getNextChoiceIndex();

        if (restrict) {
            restrict    = gd.getNextBoolean();
            belowOrLeft = gd.getNextChoiceIndex();
        }

        fitCurve   = gd.getNextBoolean();
        verbose    = gd.getNextBoolean();
        polyChoice = gd.getNextChoiceIndex();
        mask       = gd.getNextBoolean();

        if (validPath)
            saveValues = gd.getNextBoolean();

        // Impose valid parameters
        startRadius = (startRadius > endRadius) ? pxSize : startRadius;
        stepRadius =  Math.max(pxSize, incStep);
        if (IS_3D)
            stepRadius =  Math.max(z_spacing, stepRadius);

        // Calculate how many samples will be taken
        int size = (int) ((endRadius - startRadius) / stepRadius) + 1;

        // Exit if there are no samples
        if (size == 1) {
            error(" Invalid Parameters: Ending Radius cannot be larger than\n"+
                  "Starting radius and Radius step size must be within range!");
            return;
        }

        System.gc();
        long start = System.currentTimeMillis();
        IJ.resetEscape();

        // Create arrays for x-values and radii
        int[] radii = new int[size];
        double[] xvalues = new double[size];
        double[] yvalues = new double[size];

        // Populate arrays
        for (int i = 0; i < size; i++) {
            xvalues[i] = startRadius + i * stepRadius;
            radii[i] = (int) Math.round(xvalues[i] / pxSize);
        }

        // Define boundaries of analysis according to orthogonal chords (if any).
        // chord and chordAngle are already defined if restrict is true
        int maxradius = radii[size-1] + 5;
        int depth = img.getNSlices();

        minX = Math.max(x-maxradius, 0);
        maxX = Math.min(maxradius+x, ip.getWidth());
        minY = Math.max(y-maxradius, 0);
        maxY = Math.min(maxradius+y, ip.getHeight());
        minZ = Math.max(z-maxradius, 1);
        maxZ = Math.min(maxradius+z, depth);

        if (restrict) {

            if (chordAngle==0 || chordAngle==180) { // Horizontal chord

                minY = (belowOrLeft==1) ? Math.max(y-maxradius, y) : minY;
                maxY = (belowOrLeft==1) ? maxY : Math.min(maxradius+y, y);

            } else if (chordAngle==90) { // Vertical chord

                minX = (belowOrLeft==1) ? minX : Math.max(x-maxradius, x);
                maxX = (belowOrLeft==1) ? Math.min(maxradius+x, x) : maxX;

            }

        }

        // Perform 2D analysis with nSpans per radius
        if (!IS_3D) {

            // Analyze the data and return raw Sholl intersections
            yvalues = analyze(x, y, radii, nSpans, binChoice, ip);

        } else {

            // Perform 3D Sholl
            yvalues= analyze3D(x, y, z, xvalues, img);

        }

        // Display the plot and return transformed data if valid counts exist
        double[] grays = plotValues(title, shollChoice, xvalues, yvalues,
            x, y, z, saveValues, imgPath);

        String finalmsg = "Done.";

        // Create intersections mask, but check first if worth proceeding
        if (grays.length==0) {
            IJ.beep();
            finalmsg = "Error: All intersection counts were zero!";

        } else if (mask) {

            String metadata = fitCurve ? "Fitted data" : "Raw data";
            ImagePlus maskimg = makeMask(img, title, grays, xvalues, x, y, z,
                cal, metadata);
            if (maskimg==null)
                { IJ.beep(); finalmsg = "Error: Mask could not be created!"; }
            else
                maskimg.show();

        }

        IJ.showProgress(0, 0);
        IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)
            +"s. "+ finalmsg);

    }


    static public double[] analyze3D(int xcenter, int ycenter, int zcenter,
        double[] xvalues, ImagePlus img) {

        IJ.showStatus("3D Sholl: Preparing analysis...");

        double dx, dy, dz, distanceToRadius;
        int nspheres, count;

        // Create an array to hold the results
        double[] data = new double[nspheres = xvalues.length];

        int voxels;
        int[][] points;
		//short[] pixels;

		//make a cube containg the largest sphere volume:
		voxels = (int)Math.round( (maxX-minX+1) * (maxY-minY+1) * (maxZ-minZ) );
		points = new int[voxels][3];

        // Get Image Stack
        ImageStack stack = img.getStack();
        ImageProcessor ip;

        int i=0;
        for (int s = 0; s < nspheres; s++) {

            IJ.showStatus("Sampling sphere "+ (s+1) +"/"+ nspheres
                + ". Press 'Esc' to abort...");
            IJ.showProgress(s, nspheres);
            if (IJ.escapePressed()) { IJ.beep(); return data; }

			// Create an array to hold all the possible voxels of this sphere
            //voxels = (int)Math.round(Math.PI * ( (xvalues[s]/x_spacing +1)*(xvalues[s]/y_spacing +1)*(xvalues[s]/z_spacing +1) ) * 4/3);
			//points = new int[voxels][3];

            count = 0;
            for( int z = minZ; z <= maxZ; ++z ) {
                dz = (z-zcenter) * z_spacing * (z-zcenter) * z_spacing;
                for( int y = minY; y < maxY; ++y ) {
                    dy = (y-ycenter) * y_spacing * (y-ycenter) * y_spacing;
                    for( int x= minX; x < maxX; ++x ) {
                        dx = (x-xcenter) * x_spacing * (x-xcenter) * x_spacing;
                        distanceToRadius = Math.sqrt(dx + dy + dz) - xvalues[s];
                        if ( Math.abs(distanceToRadius) <1 && count < voxels) {

                            if (stack.getVoxel(x, y, z)!=0 ) {
                                points[count][0] = x;
                                points[count][1] = y;
                                points[count++][2] = z;;
                            }

                        }
                    }
                }
            }

            // now we have all the 3d coordinates of all the points that intercepted
            // Sholl spheres. Lets check if they belong to the same goup.
            data[s] = count3Dgroups(points, count, 1.5);

        }

        return data;
    }

    static public int count3Dgroups(int[][] points, int lastIndex, double threshold) {

        double distance;
        int i, j, k, target, source, dx, dy, dz, groups, len;

        // Create an array to hold the point grouping data
        int[] grouping = new int[len = lastIndex];

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
                dz = points[i][2] - points[j][2];
                distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

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

        return groups;
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

            IJ.showStatus("Radius "+ i +"/"+ size +", "+ binsize
                        + " measurement(s) per span. Press 'Esc' to abort...");

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
                binsamples[j] = countTargetGroups(pixels, points, ip);

            }

            IJ.showProgress(i, size * binsize);
            if (IJ.escapePressed()) { IJ.beep(); return data; }

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
    static public int countTargetGroups(int[] pixels, int[][] rawpoints,
           ImageProcessor ip) {

        int i, j;
        int[][] points;

        // Count how many target pixels (i.e., foreground, non-zero) we have
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0) j++;

        // Create an array to hold target pixels
        points = new int[j][2];

        // Copy all target pixels into the array
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0)
                points[j++] = rawpoints[i];

        return countGroups(points, 1.5, ip);

    }

    /*
     * For a set of points in 2D space, counts how many groups of value v there
     * are such that for every point in each group, there exists another point
     * in the same group that is less than threshold units of distance away. If
     * a point is greater than threshold units away from all other points, it is
     * in its own group. For threshold=1.5, this is equivalent to 8-connected
     * clusters
     */
    static public int countGroups(int[][] points, double threshold,
        ImageProcessor ip) {

        double distance;
        int i, j, k, target, source, dx, dy, groups, len;

        // Create an array to hold the point grouping data
        int[] grouping = new int[len =  points.length];

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
                if ( (px[0]!=0 && px[1]!=0 && px[3]!=0  &&
                      px[4]==0 && px[6]==0 && px[7]==0) ||
                     (px[1]!=0 && px[2]!=0 && px[4]!=0  &&
                      px[3]==0 && px[5]==0 && px[6]==0) ||
                     (px[4]!=0 && px[6]!=0 && px[7]!=0  &&
                      px[0]==0 && px[1]==0 && px[3]==0) ||
                     (px[3]!=0 && px[5]!=0 && px[6]!=0  &&
                      px[1]==0 && px[2]==0 && px[4]==0) )

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

            // We already filtered out of bounds coordinates in
            // getCircumferencePoints so we just need to retrieve pixel values
            // using get(x, y), faster than getPixel()
            pixels[i] = ip.get(points[i][0], points[i][1]);

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

        // Count how many points are out of bounds, while eliminating duplicates.
        // Duplicates are always at multiples of r (8 points)
        int pxX, pxY, count = 0, j= 0;
        for (i = 0; i < points.length; i++) {

            // Pull the coordinates out of the array
            pxX = points[i][0];
            pxY = points[i][1];

            if ( (i+1)%r!= 0 && pxX>= minX && pxX< maxX && pxY>= minY && pxY< maxY )
                count++;
        }

        // Create the final array containing only unique points within bounds
        int[][] refined = new int[count][2];

        for (i = 0; i < points.length; i++) {

            pxX = points[i][0];
            pxY = points[i][1];

            if ( (i+1)%r!= 0 && pxX>= minX && pxX< maxX && pxY>= minY && pxY< maxY ) {

                refined[j][0]= pxX;
                refined[j++][1]= pxY;

            }

        }

        // Return the array
        return refined;
    }

    /* Creates Results table, Sholl plot and curve fitting */
    static public double[] plotValues(String ttl, int mthd, double[] xpoints,
            double[] ypoints, int xcenter, int ycenter, int zcenter,
			boolean saveplot, String savepath) {

        IJ.showStatus("Preparing Results...");

        int size = ypoints.length;
        int i, j, nsize = 0;

        // Remove points with zero intersections avoiding log(0)
        for (i = 0; i < size; i++)
            if (ypoints[i] != 0) nsize++;

        // Do not proceed if there are no counts or mismatch between values
        if (nsize==0 || size!=xpoints.length) return new double[0];

        // get non-zero values & calculate "log intersections". The latter will
        // be used for non-traditional Sholls and to calculate the Sholl decay
        double[] x = new double[nsize];
        double[] y = new double[nsize];
        double[] logY = new double[nsize];
        double[] logY_3D = new double[nsize];
        double sumY = 0.0;
        for (i = 0, j = 0; i < size; i++) {

            if (ypoints[i] != 0.0) {
                x[j] = xpoints[i];
                y[j] = ypoints[i];
                logY[j]    = Math.log( y[j] / (Math.PI * x[j]*x[j]) );
                logY_3D[j] = Math.log( y[j] / (Math.PI * (x[j]*x[j]*x[j]) * 4/3) );
                sumY += y[j++];
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
        rt.addLabel("Image", ttl +" ("+ Unit +")");
        rt.addValue("Method #", mthd+1);
        rt.addValue("X center (px)", xcenter);
        rt.addValue("Y center (px)", ycenter);
        rt.addValue("Z center (slice)", zcenter);
        rt.addValue("Starting radius", startRadius);
        rt.addValue("Ending radius", endRadius);
        rt.addValue("Radius step", stepRadius);
        rt.addValue("Samples per radius", nSpans);
        rt.addValue("Sampled radii", size);
        rt.addValue("Sum Inters.", sumY);
        rt.addValue("Avg Inters.", sumY / nsize);
        rt.show(shollTable);

        // Define default plot axes
        double[] xScale = Tools.getMinMax(x);
        double[] yScale = Tools.getMinMax(y);
        String yTitle = "N. of Intersections";
        String xTitle = IS_3D ? "3D distance (" + Unit + ")" : "2D distance (" + Unit + ")";

        // Adjust axes for log Methods
        if (mthd == SHOLL_SLOG || mthd == SHOLL_LOG) {

            yScale = Tools.getMinMax(logY);
            yTitle = IS_3D ? "log(N. Inters./Sphere volume)" : "log(N. Inters./Circle area)";

            if (mthd == SHOLL_LOG) {
                for (i = 0; i < nsize; i++)
                    x[i] = Math.log(x[i]);
                xScale = Tools.getMinMax(x);
                xTitle = IS_3D ? "log(3D distance)" : "log(2D distance)";
            }

        } else if (mthd == SHOLL_NS) {

            if (IS_3D) {
                yTitle = "N. Inters./Sphere volume (" + Unit + "\u00B3)";
                for (i = 0; i < nsize; i++)
                     y[i] = y[i] / ( 4/3 * Math.PI * (x[i] * x[i]* x[i]) );
            } else {
                yTitle = "N. Inters./Circle area (" + Unit + "\u00B2)";
                for (i = 0; i < nsize; i++)
                     y[i] = y[i] / (Math.PI * x[i] * x[i]);
            }

            yScale = Tools.getMinMax(y);

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
                     + ":\nCurve fitting not performed: Not enough data points");
                plot.show();
                return y;
            }

            // Initialize plot label listing fitting details
            String label = "";

            // By default, calculate the Sholl decay, i.e., the slope of the
            // fitted regression on Semi-log Sholl
            CurveFitter cf;
            if (IS_3D)
                cf = new CurveFitter(x, logY_3D);
            else
                cf = new CurveFitter(x, logY);

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

            // Show the plot window, save plot values (if requested), update
            // results and return fitted data
            PlotWindow pw = plot.show();

            if (saveplot) {

                ResultsTable rtp = pw.getResultsTable();
                try {
                    rtp.saveAs(savepath +File.separator+ ttl.replaceFirst("[.][^.]+$", "")
                            + "_Sholl-M"+ ( mthd + 1 ) + Prefs.get("options.ext", ".csv"));
                } catch (IOException e) {
                    IJ.log(">>>> Sholl Analysis [" + SHOLL_TYPES[mthd] +
                           "] for " + ttl +":\n"+ e);
                }
            }

            rt.show(shollTable);
            return fy;
        }
    }


	public void drawSphere(double radius, int xc, int yc, int zc) {
		int diameter = (int)Math.round(radius*2);
	    double r = radius;
		int xmin=(int)(xc-r+0.5), ymin=(int)(yc-r+0.5), zmin=(int)(zc-r+0.5);
		int xmax=xmin+diameter, ymax=ymin+diameter, zmax=zmin+diameter;
		double r2 = r*r;
		r -= 0.5;
		double xoffset=xmin+r, yoffset=ymin+r, zoffset=zmin+r;
		double xx, yy, zz;
		for (int x=xmin; x<=xmax; x++) {
			for (int y=ymin; y<=ymax; y++) {
				for (int z=zmin; z<=zmax; z++) {
					xx = x-xoffset; yy = y-yoffset;  zz = z-zoffset;
					//if (xx*xx+yy*yy+zz*zz<=r2)
						//setVoxel(x, y, z, 255);
				}
			}
		}
	}

    /* Creates Sholl mask by applying values to foreground pixels of img*/
    public ImagePlus makeMask(ImagePlus img, String ttl, double[] values,
            double[] xvalues, int xcenter, int ycenter, int zcenter,
            Calibration cal, String label) {

        int[][] points;
        int drawRadius;
        int drawSteps = values.length;
        int drawWidth = (int)Math.round((xvalues[drawSteps-1]-startRadius)/(pxSize*drawSteps));

        // Check if analyzed image remains available, checking arrays mismatch
        ImageWindow imgw = img.getWindow();
        if ( imgw==null || (drawSteps > xvalues.length) ) return null;
        IJ.showStatus("Preparing intersections mask...");

        // Prepare mask image: A 32-bit img, so that it can hold any real number.
        // The mask is just for illustration purposes, so work on the stack
        // projection when dealing with a volume in 3D Sholl
        if (IS_3D) {
            ZProjector zp = new ZProjector(img);
            zp.setMethod(ZProjector.MAX_METHOD);
            zp.setStartSlice(minZ);
            zp.setStopSlice(maxZ);
            zp.doProjection();
            img = zp.getProjection();
        }

        ImageProcessor ip = img.getProcessor();
        ImageProcessor mp = new FloatProcessor(img.getWidth(), img.getHeight());

        for (int i = 0; i < drawSteps; i++) {

            IJ.showProgress(i, drawSteps);
            drawRadius = (int)Math.round( (startRadius/pxSize)+(i*drawWidth) );

            for (int j = 0; j < drawWidth; j++) {

                // getCircumferencePoints will already exclude out-of-bound pixels
                points = getCircumferencePoints(xcenter, ycenter, drawRadius++);
                for (int k = 0; k < points.length; k++)
                    for (int l = 0; l < points[k].length; l++)
                        if (ip.get(points[k][0], points[k][1]) != 0)
                            mp.putPixelValue(points[k][0], points[k][1], values[i]);

            }
        }

        mp.setMinAndMax(values[0], values[drawSteps-1]);

        ImagePlus img2 = new ImagePlus("Sholl mask ["+ SHOLL_TYPES[shollChoice]
                                      +"] :: "+ ttl, mp);

        // Apply calibration, set mask label and mark center of analysis
        img2.setCalibration(cal);
        img2.setProperty("Label", label);
        img2.setRoi(new PointRoi(xcenter, ycenter));

        // Return mask image
        IJ.run(img2, "Fire", ""); // "Fire", "Ice", "Spectrum", "Redgreen"
        return img2;
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

