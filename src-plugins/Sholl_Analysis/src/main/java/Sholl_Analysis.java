/* Copyright 2012 Tiago Ferreira, 2005 Tom Maddock
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
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.process.*;
import ij.text.*;
import ij.util.Tools;

import java.awt.*;
import java.awt.event.*;
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
 * @author Tiago Ferreira v2.0, Feb 2012, v3.0 Oct, 2012
 * @author Tom Maddock v1.0, Oct 2005
 */
public class Sholl_Analysis implements PlugIn, TextListener, ItemListener {

    /* Plugin Information */
    public static final String VERSION = "3.0";
    private static final String URL = "http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:asa:start";

    /* Sholl Type Definitions */
    public static final String[] SHOLL_TYPES = { "Intersections", "Norm. Intersections", "Semi-Log", "Log-Log" };
    public static final int SHOLL_N    = 0;
    public static final int SHOLL_NS   = 1;
    public static final int SHOLL_SLOG = 2;
    public static final int SHOLL_LOG  = 3;
    private static final String[] DEGREES = { "4th degree", "5th degree", "6th degree", "7th degree", "8th degree" };

    /* Will image directory be accessible? */
    private static boolean validPath;
    private static String imgPath;

    /* Default parameters and input values */
    private static double startRadius = 10.0;
    private static double endRadius   = 100.0;
    private static double stepRadius  = 1;
    private static double incStep     = 0;
    private static int shollChoice    = SHOLL_N;
    private static int polyChoice     = 1;
    private static boolean fitCurve;
    private static boolean verbose;
    private static boolean mask;
    public static int maskBackground = 228;
    private static boolean save;

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
    private static int quadChoice;
    private static int minX;
    private static int maxX;
    private static int minY;
    private static int maxY;
    private static int minZ;
    private static int maxZ;

    /* Dialog listeners*/
    private static Choice iebinChoice;
    private static Choice ieshollChoice;
    private static Choice iepolyChoice;
    private static Choice iequadChoice;
    private static Checkbox ietrimBounds;
    private static Checkbox iefitCurve;
    private static Checkbox ieverbose;
    private static Checkbox iemask;
    private static TextField ienSpans;
    private static TextField iemaskBackground;

    /* Default parameters for 3D analysis */
    private static boolean secludeSingleVoxels = false;

    /* Default parameters for 2D analysis */
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

    public void run( final String arg) {

        if (IJ.versionLessThan("1.46h"))
            return;

        // Get current image and its ImageProcessor. Make sure image is the right
        // type, reminding the user that the analysis is performed on segmented cells
        final ImagePlus img = WindowManager.getCurrentImage();
        final ImageProcessor ip = getValidProcessor(img);
        if (ip==null)
            return;

        // Set the 2D/3D Sholl flag
        final int depth = img.getNSlices();
        is3D = depth > 1;

        final String title = img.getShortTitle();

        // Get image calibration. Stacks are likely to have anisotropic voxels
        // with large z-steps. It is unlikely that lateral dimensions will differ
        final Calibration cal = img.getCalibration();
        if (cal.scaled()) {
            vxWH = Math.sqrt(cal.pixelWidth * cal.pixelHeight);
            vxD  = cal.pixelDepth;
            unit = cal.getUnits();
        } else {
            vxWH = vxD = 1; unit = "pixels";
        }
        vxSize = (is3D) ? Math.cbrt(vxWH*vxWH*vxD) : vxWH;

        // Initialize center coordinates (in pixel units)
        int x, y;
        final int z = img.getCurrentSlice();

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
        final String trim = showDialog(chordAngle, is3D);
        if (trim==null)
            return;

        // Impose valid parameters
        final int wdth = ip.getWidth();
        final int hght = ip.getHeight();
        final double dx, dy, dz, maxEndRadius;

        dx = ((orthoChord && trimBounds && trim.equalsIgnoreCase("right")) || x<=wdth/2)
             ? (x-wdth)*vxWH : x*vxWH;

        dy = ((orthoChord && trimBounds && trim.equalsIgnoreCase("below")) || y<=hght/2)
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
        final double[] radii = new double[size];
        double[] counts = new double[size];

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
            if (trim.equalsIgnoreCase("above"))
                maxY = (int) Math.min(y + xymaxradius, y);
            else if (trim.equalsIgnoreCase("below"))
                minY = (int) Math.max(y - xymaxradius, y);
            else if (trim.equalsIgnoreCase("right"))
                minX = x;
            else if (trim.equalsIgnoreCase("left"))
                maxX = x;
        }

        // 2D: Analyze the data and return intersection counts with nSpans
        // per radius. 3D: Analysis without nSpans
        if (is3D) {
            counts = analyze3D(x, y, z, radii, img);
        } else {
            counts = analyze2D(x, y, radii, vxSize, nSpans, binChoice, ip);
        }

        // Display the plot and return transformed data
        final double[] grays = plotValues(title, shollChoice, radii, counts, x, y, z);

        String exitmsg = "Done. ";

        // Create intersections mask, but check first if it is worth proceeding
        if (grays.length == 0) {

            IJ.beep();
            exitmsg = "Error: All intersection counts were zero! ";

        } else if (mask) {

            final ImagePlus maskimg = makeMask(img, title, grays, xymaxradius, x, y, cal);
            if (maskimg == null)
                { IJ.beep(); exitmsg = "Error: Mask could not be created! "; }
            else
                { maskimg.show(); maskimg.updateAndDraw(); }

        }

        IJ.showProgress(0, 0);
        IJ.showTime(img, img.getStartTime(), exitmsg);

    }

    /**
     * Creates the plugin dialog. Returns the region of the image (relative to the center)
     * to be trimmed from the analysis "None", "Above","Below", "Right" or "Left".
     * Returns null if dialog was canceled
     */
    private String showDialog(final double chordAngle, final boolean is3D) {

        String trim = "None"; // Default return value

        final GenericDialog gd = new GenericDialog("Sholl Analysis v"+ VERSION);
        gd.addNumericField("Starting radius:", startRadius, 2, 9, unit);
        gd.addNumericField("Ending radius:", endRadius, 2, 9, unit);
        gd.addNumericField("Radius_step size:", incStep, 2, 9, unit);

        // 2D Analysis: allow multiple samples per radius,
        if (!is3D) {
            gd.addSlider("Samples per radius:", 1, 10, nSpans);
            gd.setInsets(0, 0, 0);
            gd.addChoice("Samples_integration:", BIN_TYPES, BIN_TYPES[binChoice]);
        }

        gd.setInsets(12, 0, 12);
        gd.addChoice("Sholl method:", SHOLL_TYPES, SHOLL_TYPES[shollChoice]);

        // If an orthogonal chord exists, prompt for hemicircle/hemisphere analysis
        orthoChord = (chordAngle > -1 && chordAngle % 90 == 0);
        final String[] quads = new String[2];
        if (orthoChord) {
            if (chordAngle == 90.0) {
                quads[0] = "Right of line";
                quads[1] = "Left of line";
            } else {
                quads[0] = "Above line";
                quads[1] = "Below line";
            }
            gd.setInsets(0, 6, 3);
            gd.addCheckbox("Restrict analysis to hemi"+ (is3D ? "sphere:" : "circle:"), trimBounds);
            gd.addChoice("_", quads, quads[quadChoice]);
        }

        if (is3D) {
            gd.setInsets( orthoChord ? 6 : 0, 6, 6);
            gd.addCheckbox("Ignore isolated (6-connected) voxels", secludeSingleVoxels);
        }

        // Prompt for curve fitting related options
        gd.setInsets(6, 6, 0);
        gd.addCheckbox("Fit profile and compute descriptors:", fitCurve);
        gd.setInsets(3, is3D ? 33 : 56, 3);
        gd.addCheckbox("Show parameters", verbose);
        gd.addChoice("Polynomial:", DEGREES, DEGREES[polyChoice]);

        // Prompt for mask related options
        gd.setInsets(6, 6, 0);
        gd.addCheckbox("Create intersections mask:", mask);
        gd.addSlider("Background:", 0, 255, maskBackground);

        // Offer to save results if local image
        if (validPath) {
            gd.setInsets(6, 6, 0);
            gd.addCheckbox("Save results on image directory", save);
        }

        gd.setHelpLabel("Online Help");
        gd.addHelp(URL);

        // Add listeners and set initial states
        final Vector<?> numericfields = gd.getNumericFields();
        final Vector<?> choices = gd.getChoices();
        final Vector<?> checkboxes = gd.getCheckboxes();
        int currentCheckbox = 0;
        int currentChoice = 0;
        int currentField = 3;

        if (!is3D) {
            ienSpans = (TextField)numericfields.elementAt(currentField++);
            ienSpans.addTextListener(this);
            iebinChoice = (Choice)choices.elementAt(currentChoice++);
            iebinChoice.addItemListener(this);
            iebinChoice.setEnabled(nSpans>1);
        }

        ieshollChoice = (Choice)choices.elementAt(currentChoice++);
        ieshollChoice.addItemListener(this);

        if (orthoChord) {
            ietrimBounds = (Checkbox)checkboxes.elementAt(currentCheckbox++);
            ietrimBounds.addItemListener(this);
            iequadChoice = (Choice)choices.elementAt(currentChoice++);
            iequadChoice.addItemListener(this);
            iequadChoice.setEnabled(trimBounds);
        }

        if (is3D) currentCheckbox++;
        iefitCurve = (Checkbox)checkboxes.elementAt(currentCheckbox++);
        iefitCurve.addItemListener(this);

        ieverbose = (Checkbox)checkboxes.elementAt(currentCheckbox++);
        ieverbose.addItemListener(this);
        ieverbose.setEnabled(fitCurve);

        iepolyChoice = (Choice)choices.elementAt(currentChoice++);
        iepolyChoice.addItemListener(this);
        iepolyChoice.setEnabled(shollChoice==SHOLL_N && fitCurve);

        iemask = (Checkbox)checkboxes.elementAt(currentCheckbox++);
        iemask.addItemListener(this);

        iemaskBackground = (TextField)numericfields.elementAt(currentField++);
        iemaskBackground.setEnabled(mask);

        gd.showDialog();

        // Exit if user pressed cancel
        if (gd.wasCanceled())
            return null;

        // Get values from dialog
        startRadius = Math.max(0, gd.getNextNumber());
        endRadius = gd.getNextNumber();
        incStep = Math.max(0, gd.getNextNumber());

        if (!is3D) {
            nSpans = Math.min(Math.max((int)gd.getNextNumber(), 1), 10);
            binChoice = gd.getNextChoiceIndex();
        }

        shollChoice = gd.getNextChoiceIndex();

        // Get trim choice
        if (orthoChord) {
            trimBounds = gd.getNextBoolean();
            final String choice = quads[quadChoice = gd.getNextChoiceIndex()];
            trim = choice.substring(0, choice.indexOf(" "));
        }

        if (is3D)
            secludeSingleVoxels = gd.getNextBoolean();

        fitCurve = gd.getNextBoolean();
        verbose = gd.getNextBoolean();
        polyChoice = gd.getNextChoiceIndex();
        mask = gd.getNextBoolean();
        maskBackground = Math.min(Math.max((int)gd.getNextNumber(), 0), 255);
        if (validPath)
            save = gd.getNextBoolean();

        // Return trim choice
        return trim;
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

        if (!secludeSingleVoxels)
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
     * Counts how many groups of value v are present in the given data. A group
     * consists of a formation of adjacent pixels, where adjacency is true for
     * all eight neighboring positions around a given pixel.
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
    static public int[][] getCircumferencePoints(final int cx, final int cy, int r) {

        // Initialize algorithm variables
        int i = 0, x = 0, y = r, err = 0, errR, errD;

        // Array to store first 1/8 of points relative to center
        final int[][] data = new int[++r][2];

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
        final int[][] points = new int[r * 8][2];

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

    /** Creates Results table and Sholl plot, performing curve fitting */
    static public double[] plotValues(final String title, final int mthd, final double[] xpoints,
            final double[] ypoints, final int xc, final int yc, final int zc) {

        final int size = ypoints.length;
        int i, j, nsize = 0;
        final StringBuffer plotLabel = new StringBuffer();

        IJ.showStatus("Preparing Results...");

        // Zero intersections are problematic for logs and polynomials. Long
        // stretches of zeros often cause sharp "bumps" on the fitted curve.
        // Setting zeros to NaN is not option as it would impact the CurveFitter
        for (i = 0; i < size; i++)
            if (ypoints[i] != 0)
                nsize++;

        // Do not proceed if there are no counts or mismatch between values
        if (nsize == 0 || size > xpoints.length)
            return new double[0];

        final double[] x = new double[nsize];
        final double[] logY = new double[nsize];
        double[] y = new double[nsize];
        double sumY = 0, maxIntersect = 0, maxR = 0;

        for (i = 0, j = 0; i < size; i++) {

            if (ypoints[i] != 0.0) {
                x[j] = xpoints[i];
                y[j] = ypoints[i];

                // Normalize log values to area of circle/volume of sphere
                if (is3D)
                    logY[j] = Math.log(y[j] / (Math.PI * x[j]*x[j]*x[j] * 4/3));
                else
                    logY[j] = Math.log(y[j] / (Math.PI * x[j]*x[j]));

                // Retrieve raw statistics
                if( y[j] > maxIntersect )
                    { maxIntersect = y[j]; maxR = x[j]; }
                sumY += y[j++];
            }

        }

        // Calculate the smallest circle/sphere enclosing the arbor
        final double lastR = x[nsize-1];
        final double field = is3D ? Math.PI*4/3*lastR*lastR*lastR : Math.PI*lastR*lastR;

        // Calculate ramification index, the maximum of intersection divided by the n.
        // of primary branches, assumed to be the n. intersections at starting radius
        final double ri = maxIntersect / y[0];

        // Place parameters on a dedicated table
        ResultsTable rt;
        final String shollTable = "Sholl Results";
        final Frame window = WindowManager.getFrame(shollTable);
        if (window == null)
            rt = new ResultsTable();
        else
            rt = ((TextWindow) window).getTextPanel().getResultsTable();

        rt.incrementCounter();
        rt.setPrecision(getPrecision());
        rt.addLabel("Image", title + " (" + unit + ")");
        rt.addValue("Lower Thold", lowerT);
        rt.addValue("Upper Thold", upperT);
        rt.addValue("Method #", mthd + 1);
        rt.addValue("X center (px)", xc);
        rt.addValue("Y center (px)", yc);
        rt.addValue("Z center (slice)", zc);
        rt.addValue("Starting radius", startRadius);
        rt.addValue("Ending radius", endRadius);
        rt.addValue("Radius step", stepRadius);
        rt.addValue("Samples per radius", is3D ? 1 : nSpans);
        rt.addValue("Intersecting radii", nsize);
        rt.addValue("Sum Inters.", sumY);
        rt.addValue("Avg Inters.", sumY/nsize);
        rt.addValue("Max Inters.", maxIntersect);
        rt.addValue("Max Inters. radius", maxR);
        rt.addValue("Enclosing radius", lastR);
        rt.addValue("Enclosed field", field);
        rt.addValue("Ramification index", ri);

        // Calculate Sholl decay: the slope of fitted regression on Semi-log Sholl
        CurveFitter cf = new CurveFitter(x, logY);
        cf.doFit(CurveFitter.STRAIGHT_LINE, false);

        double[] parameters = cf.getParams();
        plotLabel.append("k= " + IJ.d2s(-parameters[1], -2));
        rt.addValue("Sholl Regression Coefficient", -parameters[1]); // Slope of regression
        rt.addValue("Regression Intercept", parameters[0]);
        rt.addValue("Regression R^2", cf.getRSquared());
        rt.show(shollTable);

        // Define a global analysis title
        final String longtitle = "Sholl Profile ("+ SHOLL_TYPES[mthd] +") for "+ title;

        // Abort curve fitting when dealing with small datasets that are prone to
        // inflated coefficients of determination
        if (fitCurve && nsize <= 6) {
            fitCurve = false;
            IJ.log(longtitle +":\nCurve fitting not performed: Not enough data points");
        }

        // Define plot axes
        String xTitle, yTitle;
        final boolean xAxislog  = mthd == SHOLL_LOG;
        final boolean yAxislog  = mthd == SHOLL_LOG || mthd == SHOLL_SLOG;
        final boolean yAxisnorm = mthd == SHOLL_NS;

        if (xAxislog) {

            xTitle = is3D ? "log(3D distance)" : "log(2D distance)";
            for (i = 0; i < nsize; i++)
                x[i] = Math.log(x[i]);

        } else {
            xTitle = is3D ? "3D distance ("+ unit +")" : "2D distance ("+ unit +")";
        }

        if (yAxislog) {

            yTitle = is3D ? "log(N. Inters./Sphere volume)" : "log(N. Inters./Circle area)";
            y = (double[])logY.clone();

        } else if (yAxisnorm) {

                yTitle = is3D ? "N. Inters./Sphere volume ("+ unit +"\u00B3)" :
                                "N. Inters./Circle area ("+ unit +"\u00B2)";
                for (i=0; i<nsize; i++)
                    y[i] = Math.exp(logY[i]);

        } else {
            yTitle = "N. of Intersections";
        }

        // Create an empty plot
        final Plot plot = createEmptyPlot(longtitle, xTitle, yTitle);

        // Set limits
        final double[] xScale = Tools.getMinMax(x);
        final double[] yScale = Tools.getMinMax(y);
        setPlotLimits(plot, xScale, yScale);

        // Add original data (default color is black)
        plot.setColor(Color.GRAY);
        plot.addPoints(x, y, Plot.CROSS);

        // Exit and return raw data if no fitting is done
        if (!fitCurve) {
            savePlot(plot, title, plotLabel.toString(), xpoints, ypoints, logY, null, null);
            return y;
        }

        // Perform a new fit if data is not Semi-log Sholl
        if (mthd!=SHOLL_SLOG )
            cf = new CurveFitter(x, y);

        //cf.setRestarts(4); // default: 2;
        //cf.setMaxIterations(50000); //default: 25000

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

        //IJ.showStatus("Curve fitter status: " + cf.getStatusString());

        // Get parameters of fitted function
        if (mthd != SHOLL_SLOG)
            parameters = cf.getParams();

        // Get fitted data
        final double[] fy = new double[nsize];
        for (i = 0; i < nsize; i++)
            fy[i] = cf.f(parameters, x[i]);

        // Initialize morphometric descriptors
        double cv = 0, cr = 0, mv = 0, rif = 0;

        // Linear Sholl: Calculate Critical value (cv), Critical radius (cr),
        // Mean Sholl value (mv) and "fitted" Ramification (Schoenen) index (rif)
        if (mthd == SHOLL_N) {

            // Get coordinates of cv, the local maximum of polynomial. We'll
            // iterate around the index of highest fitted value to retrive values
            // empirically. This is probably the most ineficient way of doing it
            final int maxIdx = CurveFitter.getMax(fy);
            final int iterations = 1000;
            final double crLeft  = (x[Math.max(maxIdx-1, 0)] + x[maxIdx]) / 2;
            final double crRight = (x[Math.min(maxIdx+1, nsize-1)] + x[maxIdx]) / 2;
            final double step = (crRight-crLeft) / iterations;
            double crTmp, cvTmp;
            for (i = 0; i < iterations; i++) {
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
            for (i = 0; i < parameters.length-1; i++) //-1?
                mv += (parameters[i]/(i+1)) * Math.pow(xScale[1]-xScale[0], i);

            // Highlight the mean Sholl value on the plot
            plot.setLineWidth(1);
            plot.setColor(Color.lightGray);
            plot.drawLine(xScale[0], mv, xScale[1], mv);

            // Calculate the "fitted" ramification index
            rif = cv / y[0];

            // Append calculated parameters to plot label
            plotLabel.append("\nCv= "+ IJ.d2s(cv, 2));
            plotLabel.append("\nCr= "+ IJ.d2s(cr, 2));
            plotLabel.append("\nMv= "+ IJ.d2s(mv, 2));
            plotLabel.append("\n" + DEGREES[polyChoice]);

        } else {
            cv = cr = mv = rif = Double.NaN;
        }

        rt.addValue("Critical value", cv);
        rt.addValue("Critical radius", cr);
        rt.addValue("Mean value", mv);
        rt.addValue("Ramification index (cv)", rif);
        rt.addValue("Polyn. degree", mthd==SHOLL_N ? parameters.length-2 : Double.NaN);

        // Register quality of fit
        plotLabel.append("\nR\u00B2= "+ IJ.d2s(cf.getRSquared(), 3));
        rt.addValue("R^2 (fit)", cf.getRSquared());

        // Plot fitted curve
        plot.setColor(Color.BLUE);
        plot.setLineWidth(2);
        plot.addPoints(x, fy, PlotWindow.LINE);
        plot.setLineWidth(1);

        if (verbose) {
            IJ.log("\n*** "+ longtitle +", fitting details:"+ cf.getResultString());
        }

        // Show the plot window, save profile, update results and return fitted data
        savePlot(plot, title, plotLabel.toString(), xpoints, ypoints, logY, x, fy);
        rt.show(shollTable);
        return fy;
    }

    /**
     * Creates a 2D Sholl heatmap by applying measured values to the foregroud pixels
     * of a copy of the analyzed image
     */
    private ImagePlus makeMask(final ImagePlus img, final String ttl, final double[] values,
            final int lastRadius, final int xc, final int yc, final Calibration cal) {

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

        // Heatmap will be a 32-bit image so that it can hold any real number
        final ImageProcessor mp = new FloatProcessor(ip.getWidth(), ip.getHeight());

        final int drawSteps = values.length;
        final int firstRadius = (int) Math.round(startRadius/vxWH);
        final int drawWidth = (int) Math.round((lastRadius-startRadius)/drawSteps);

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
        mp.setColorModel(matlabJetColorMap(maskBackground, shollChoice==SHOLL_SLOG || shollChoice==SHOLL_LOG ? 255 : 0));

        if ( shollChoice==SHOLL_N ) {
            final double[] range = Tools.getMinMax(values);
            mp.setMinAndMax(0, range[1]);
        } else
            (new ContrastEnhancer()).stretchHistogram(mp, 0.35);

        final String title = ttl + "_ShollMask-M"+ (shollChoice+1) + ".tif";
        final ImagePlus img2 = new ImagePlus(title, mp);

        // Apply calibration, set mask label and mark center of analysis
        img2.setCalibration(cal);
        img2.setProperty("Label", fitCurve ? "Fitted data" : "Raw data");
        img2.setRoi(new PointRoi(xc, yc));

        if (validPath && save) {
            IJ.save(img2, imgPath + File.separator + title);
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
    public static IndexColorModel matlabJetColorMap(final int grayvalue, final int idx) {

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
        reds[idx] = greens[idx] = blues[idx] = (byte)grayvalue;

        return new IndexColorModel(8, 256, reds, greens, blues);
    }

    /** Creates an empty plot (the default constructor using "flags" requires data arrays) */
    private static Plot createEmptyPlot(final String title, final String xTitle, final String yTitle) {
        final double[] empty = null;
        final int flags = Plot.X_FORCE2GRID + Plot.X_TICKS + Plot.X_NUMBERS
                        + Plot.Y_FORCE2GRID + Plot.Y_TICKS + Plot.Y_NUMBERS;
        final Plot plot = new Plot(title, xTitle, yTitle, empty, empty, flags);

        return plot;
    }

    /** Sets plot limits imposing grid lines  */
    private static void setPlotLimits(final Plot plot, final double[] xScale, final double[] yScale) {
        final boolean gridState = PlotWindow.noGridLines;
        PlotWindow.noGridLines = false;
        plot.setLimits(xScale[0], xScale[1], yScale[0], yScale[1]);
        PlotWindow.noGridLines = gridState;
    }

    /** Shows the plot window and saves the plot table on the image directory */
    private static void savePlot(final Plot plot, final String title, final String label,
            final double[] x0, final double[] y0, final double[] logy0, final double[] x1,
            final double[] y1) {
        makePlotLabel(plot, label);
        plot.show();
        if (validPath && save) {
            final String path = imgPath + File.separator + title + "_Sholl-M" + (shollChoice+1);
            final ResultsTable rt = getProfileTable(x0, y0, logy0, x1, y1);
            try {
                rt.saveAs(path + Prefs.get("options.ext", ".csv"));
                IJ.saveAs(plot.getImagePlus(), "png", path + ".png");
            } catch (final IOException e) {
                IJ.log(">>>> An error occured when saving "+ title +"'s profile:\n"+ e);
            }
        }
    }

    /** Draws a label at the less "crowded" upper corner of the plot canvas */
    private static void makePlotLabel(final Plot plot, final String label) {
        final int margin = 4; // Axes internal margin, 1+Plot.TICK_LENGTH
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
		final int xRight = Plot.LEFT_MARGIN + PlotWindow.plotWidth - margin - textWidth;
		final int xLeft  = Plot.LEFT_MARGIN + margin;

        ip.setRoi(xLeft, yTop, textWidth, textHeight);
        final double meanLeft = ImageStatistics.getStatistics(ip, Measurements.MEAN, null).mean;
        ip.setRoi(xRight, yTop, textWidth, textHeight);
        final double meanRight = ImageStatistics.getStatistics(ip, Measurements.MEAN, null).mean;

        ip.drawString(label, meanLeft>meanRight ? xLeft : xRight, yTop);
    }

    /** Retrieves precision according to Analyze>Set Measurements...*/
    private static int getPrecision() {
		final boolean sNotation = (Analyzer.getMeasurements()&Measurements.SCIENTIFIC_NOTATION)!=0;
        int precision = Analyzer.getPrecision();
		if (sNotation)
		    precision = -precision;
		return precision;
    }

    /** Returns a Results Table with profile data */
    private static ResultsTable getProfileTable(final double[] rawX, final double[] rawY,
            final double[] lograwY, final double[] fitX, final double[] fitY) {
        final ResultsTable rt = new ResultsTable();
        rt.setPrecision(getPrecision());
        for (int i=0, j=0; i<rawX.length; i++) {
            rt.setValue("Radius", i, rawX[i]);
            rt.setValue("Crossings", i, rawY[i]);
            rt.setValue("log(Norm crossings)", i, rawY[i]!=0 ? lograwY[j++] : Double.NaN);
            if (fitCurve && i<fitX.length) {
                rt.setValue("Fitted X", i, fitX[i]);
                rt.setValue("Fitted Y", i, fitY[i]);
            }
        }
        return rt;
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
    private void sError(final String msg) {
        error(msg, false);
    }
    private void lError(final String msg) {
        error(msg, true);
    }

    /**  Disables invalid options every time the dialog changes */
    public void itemStateChanged(final ItemEvent ie) {
        if (ie.getSource() == iemask)
            iemaskBackground.setEnabled(iemask.getState());
        else if (ie.getSource() == ietrimBounds)
            iequadChoice.setEnabled(ietrimBounds.getState());
        else {
            final boolean fState = iefitCurve.getState();
            final boolean pState = ieshollChoice.getSelectedItem().equals(SHOLL_TYPES[SHOLL_N]) && fState;
            ieverbose.setEnabled(fState);
            iepolyChoice.setEnabled(pState);
        }
    }

    /**  Disables BIN_TYPES choice when not required */
    public void textValueChanged(final TextEvent e) {
        iebinChoice.setEnabled( (int)Tools.parseDouble(ienSpans.getText(), 0.0) > 1 );
    }

}
