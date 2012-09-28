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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.util.Arrays;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Rectangle;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Insets;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;


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

public class Advanced_Sholl_Analysis implements PlugIn, ActionListener {

    /* Plugin Information */
    public static final String VERSION = "2.3";
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
    private static double circWidth = 0;
    private static int binChoice = BIN_AVERAGE;
    private static int shollChoice = SHOLL_N;
    private static boolean fitCurve = true;
    private static int polyChoice = 1;
    private static boolean verbose = false;
    private static boolean makeMask = true;
    private static String pxUnit = "Pixels";
    private static double pxSize = 1;
    private static int size;

	/* Reference to the plugin's dialog */
	private static GenericDialog gd = null;
	/* Help dialog title for this plugin */
    private static final String HelpTitle = "Help For Sholl Analysis Plugin v" + VERSION;
	
    /* Help dialog message for this plugin */
    private static final String HelpMessage =
	
	"<html>"                                                                                    +
	
	"This plugin performs automated Sholl analysis on a picture of a neuron.<p>"                +
	"The requirements for this plugin are twofold: that the image be  8-bit<p>"                 +
	"grayscale, and that the pixels constituting part of the neuron are all the<p>"             +
	"same gray value, while the rest of the pixels are some other gray value.<p><p>"            +
	"To use the plugin, first select a point on the image to be the center of<p>"               +
	"analysis, then run the plugin.  After setting the parameters and clicking OK,<p>"          +
	"the plugin will produce a graph of # of intersections vs. distance, and the<p>"            +
	"results can then be exported to a spreadsheet program.<p><p>"                              +
	
	"This plugin uses the scale information embedded in the image to perform its<p>"            +
	"analysis.  If the scale information needs to be set, this may be done by going to the<p>"  +
	"\"Analyze\" menu and selecting the \"Set Scale...\" item while the image is open.<p><p>"   +
	
	"<b>Starting Radius</b>: The smallest radius of the analysis circles, i.e., the<p>"         +
	"radius of the first circle on which the # of intersections are measured.<p><p>"            +
	
	"<b>Ending Radius</b>: The cutoff limit for how large the radii of the analysis<p>"         +
	"circles can get.  There is no guarantee that there will be a measurement<p>"               +
	"for this radius value, unless the difference between this and the starting<p>"             +
	"radius is a multiple of the step size (below).<p><p>"                                      +
	
	"<b>Radius Step Size</b>: The interval between radii of analysis circles. Can be<p>"        +
	"set to zero to enable continuous Sholl analysis.<p><p>"                                    +
	
	"<b>Radius Span:</b> The margin around each radius in which continuous intersection<p>"     +
	"measurements are made, which are then combined to become the value for the<p>"             +
	"# of intersections at that radius length.  Can be set to zero to disable span.<p><p>"      +
	
	"<b>Span Type</b>: The statistical function used to combine measurements in a span.<p>"     +
	"Current options are mean and median."                                                      +
	
	"</html>";
	
    public void run(String arg) {
		
        if (IJ.versionLessThan("1.46h")) return;

        // Get current image and the ImageProcessor for the image
        ImagePlus img = IJ.getImage();
        ImageProcessor ip = img.getProcessor();

        // Make sure image is of the right type
        if (!ip.isBinary()) {
            error("8-bit binary image (black and white only) is required. Binary\n"
                + "  images can be created using Image>Adjust>Threshold...");
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
        gd.addNumericField("Radius_span:", circWidth, 2, 9, pxUnit);
        gd.addChoice("Span_type:", BIN_TYPES, BIN_TYPES[binChoice]);
        gd.addChoice("Sholl method:", SHOLL_TYPES, SHOLL_TYPES[shollChoice]);
        gd.setInsets(10, 6, 0);
        gd.addCheckbox("Fit profile and compute descriptors", fitCurve);
        gd.setInsets(3, 34, 3);
        gd.addCheckbox("Show parameters", verbose);
        gd.addChoice("Polynomial:", DEGREES, DEGREES[polyChoice]);
        gd.setInsets(5, 6, 0);
        gd.addCheckbox("Create intersections mask", makeMask);
		
		
		// Setup the help button
        Button helpbutton = new Button("Offline Help");
        helpbutton.setActionCommand("OPENHELP");
        helpbutton.addActionListener(this);
		
        // Setup the help panel
        Panel helppanel = new Panel();
        helppanel.add(helpbutton);
		
		// Add the help button
        gd.addPanel(helppanel, GridBagConstraints.SOUTH, new Insets(5,0,0,0));
		
		
		
        gd.setHelpLabel("Online Help");
        gd.addHelp(URL);
        gd.showDialog();

        // Exit if user pressed cancel
        if (gd.wasCanceled())
            return;

        // Get values from dialog
        startRadius = Math.max(pxSize, gd.getNextNumber());
        endRadius = Math.max(pxSize, gd.getNextNumber());
        incStep = Math.max(pxSize, gd.getNextNumber());
        circWidth = Math.max(pxSize, gd.getNextNumber());
        binChoice = gd.getNextChoiceIndex();
        shollChoice = gd.getNextChoiceIndex();
        fitCurve = gd.getNextBoolean();
        verbose = gd.getNextBoolean();
        polyChoice = gd.getNextChoiceIndex();
        makeMask = gd.getNextBoolean();

        // Impose valid parameters & restrict Radius span to an acceptable value
        startRadius = (startRadius > endRadius) ? pxSize : startRadius;
        incStep = (incStep > (endRadius - startRadius)) ? pxSize : incStep;
        circWidth = Math.min(2 * incStep, circWidth);

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
        double[] yvalues = analyze(x, y, radii, (int)(circWidth/pxSize),
                binChoice, ip);

        IJ.showStatus("Preparing Results...");

        // Display the analysis and return transformed data
        double[] grays = plotValues(img.getTitle(), shollChoice, radii,
                xvalues, yvalues, x, y);

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
                drawRadius = (int) Math.round((startRadius / pxSize)
                        + (i * drawWidth) - drawWidth);

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

            // Store type of data in mask label
            String metadata = "Raw data";
            if (fitCurve)
                metadata = "Fitted data";
            img2.setProperty("Label", metadata);

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

        gd = null;
		

    }

	
	/* This function processes all custom button clicks*/
    public void actionPerformed(ActionEvent e) {
		
        // The button to show the help dialog was clicked
        if (e.getActionCommand().equals("OPENHELP"))
            showHelp();
		
        // The "OK" button on the help dialog was clicked
        else if (e.getActionCommand().equals("CLOSEHELP")){
            JDialog helpdialog =
			(JDialog)((JButton)e.getSource()).getClientProperty("DIALOG");
            helpdialog.setVisible(false);
            helpdialog.dispose();
        }
		
    }
	
    /* Shows the help dialog box */
    public void showHelp() {
		
        JDialog helpdialog;
		
        // Create the help dialog box
        if (gd != null)
            helpdialog = new JDialog(gd, HelpTitle, false);
        else
            helpdialog = new JDialog(IJ.getInstance(), HelpTitle, false);
		
        // Set up the dialog's contents
        ij.util.Java2.setSystemLookAndFeel();
        Container container = helpdialog.getContentPane();
        container.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        JLabel label = new JLabel(HelpMessage);
        panel.add(label);
        container.add(panel, "Center");
        panel = new JPanel();
        JButton button = new JButton("OK");
        button.setActionCommand("CLOSEHELP");
        button.addActionListener(this);
        button.putClientProperty("DIALOG", helpdialog);
        panel.add(button);
        container.add(panel, "South");
        helpdialog.pack();
		
        // Show the help dialog
        ij.gui.GUI.center(helpdialog);
        helpdialog.setVisible(true);
		
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

        // Create an array for the bin samples (we have ensured previously that
        // binsize won't take negative values)
        binsamples = new int[binsize];

        // Outer loop to control the analysis bins
        for (i = 0; i < size; i++) {

            IJ.showStatus("Sampling radius "+ i +"/"+ size + ". Span: "
                          + binsize + " measurement(s)/radius...");

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

            // Statistically combine bin data
            if (bintype == BIN_MEDIAN && binsize > 0) {

                // Sort the bin data
                Arrays.sort(binsamples);

                // Pull out the median value
                data[i] = (binsize % 2 == 0)

                // Average the two middle values if no center exists
                ? ((double) (binsamples[binsize / 2] + binsamples[binsize / 2 - 1])) / 2.0

                // Pull out the center value
                : (double) binsamples[binsize / 2];

            } else if (binsize > 0) {

                // Mean: Find the sum of the samples and divide by n. of samples
                for (sum = 0, k = 0; k < binsize; k++)
                    sum += binsamples[k];
                data[i] = ((double) sum) / ((double) binsize);

            } else
                data[i] = 0;

        }

        return data;
    }

    /* Counts how many groups of non-zero pixels are present in the given data. */
    static public int countTargetGroups(int[] pixels, int[][] rawpoints,
            ImageProcessor ip) {

        int i, j;
        int[][] points;

        // Count how many non-zero (foreground) pixels we have
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0)
                j++;

        // Create an array to hold target pixels
        points = new int[j][2];

        // Copy all target pixels into the array
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0)
                points[j++] = rawpoints[i];

        return countGroups(points, 1.5, ip);

    }

    /*
     * For a set of points in 2D space, counts how many groups there are such
     * that for every point in each group, there exists another point in the
     * same group that is less than threshold units of distance away. If a point
     * is greater than threshold units away from all other points, it is in its
     * own group. For threshold= 1.5 is this equivalent to 8-connected clusters?
     */
    static public int countGroups(int[][] points, double threshold, ImageProcessor ip) {

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

        // If the edge of the group lies tangent to the sampling circle,
        // multiple intersections with that circle will be counted. We will try
        // to find these "false positives" and throw them out. A way to attempt
        // this (we will be missing some of them) is to throw out 1-pixel groups
        // that exist solely on the edge of a "stair" of target pixels
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
            testpoints[0][0] = dx - 1; testpoints[0][1] = dy + 1;
            testpoints[1][0] = dx;     testpoints[1][1] = dy + 1;
            testpoints[2][0] = dx + 1; testpoints[2][1] = dy + 1;
            testpoints[3][0] = dx - 1; testpoints[3][1] = dy;
            testpoints[4][0] = dx + 1; testpoints[4][1] = dy;
            testpoints[5][0] = dx - 1; testpoints[5][1] = dy - 1;
            testpoints[6][0] = dx;     testpoints[6][1] = dy - 1;
            testpoints[7][0] = dx + 1; testpoints[7][1] = dy - 1;

            // Pull out the pixel values for these points
            px = getPixels(ip, testpoints);

            // Now perform the stair checks
            if ((px[0]!=0 && px[1]!=0 && px[3]!=0 && px[4]==0 && px[6]==0 && px[7]==0) ||
                (px[1]!=0 && px[2]!=0 && px[4]!=0 && px[3]==0 && px[5]==0 && px[6]==0) ||
                (px[4]!=0 && px[6]!=0 && px[7]!=0 && px[0]==0 && px[1]==0 && px[3]==0) ||
                (px[3]!=0 && px[5]!=0 && px[6]!=0 && px[1]==0 && px[2]==0 && px[4]==0))

                groups--;
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
            double[] xpoints, double[] ypoints, int xcenter, int ycenter) {

        int size = ypoints.length;
        int i, j, nsize = 0;

        // Remove points with zero intersections avoiding log(0)
        for (i = 0; i < size; i++)
            if (ypoints[i] != 0.0) nsize++;

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
        rt.addValue("Method", mthd+1);
        rt.addValue("X center (px)", xcenter);
        rt.addValue("Y center (px)", ycenter);
        rt.addValue("Starting radius", startRadius);
        rt.addValue("Ending radius", endRadius);
        rt.addValue("Radius step", incStep);
        rt.addValue("Radius span", circWidth);
        rt.addValue("Circ. within span", circWidth/pxSize);
        rt.addValue("Sampled radii", size);
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

            // Perform fits not involving log transformation of intersections
            if (mthd == SHOLL_N || mthd == SHOLL_NS)
                cf = new CurveFitter(x, y);
            // cf.setRestarts(2); // default: 2;
            // cf.setMaxIterations(25000); //default: 25000

            if (mthd == SHOLL_N) {
                if (DEGREES[polyChoice].startsWith("4")) {
                    cf.doFit(CurveFitter.POLY4, false);
                    rt.addValue("Polyn. degree", 4);
                } else if (DEGREES[polyChoice].startsWith("5")) {
                    cf.doFit(CurveFitter.POLY5, false);
                    rt.addValue("Polyn. degree", 5);
                } else if (DEGREES[polyChoice].startsWith("6")) {
                    cf.doFit(CurveFitter.POLY6, false);
                    rt.addValue("Polyn. degree", 6);
                } else if (DEGREES[polyChoice].startsWith("7")) {
                    cf.doFit(CurveFitter.POLY7, false);
                    rt.addValue("Polyn. degree", 7);
                } else if (DEGREES[polyChoice].startsWith("8")) {
                    cf.doFit(CurveFitter.POLY8, false);
                    rt.addValue("Polyn. degree", 8);
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

                // Add calculated parameters to plot label
                label += "\nCv= " + IJ.d2s(cv, 2)
                       + "\nCr= " + IJ.d2s(cr, 2)
                       + "\nMv= " + IJ.d2s(mv, 2)
                       + "\nRI= " + IJ.d2s(ri, 2)
                       + "\n" + DEGREES[polyChoice];
            }

            // Register quality of fit
            rt.addValue("R^2", cf.getRSquared());
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
            IJ.error("Advanced Sholl Analysis v" + VERSION, msg);
        else {
            GenericDialog gd = new GenericDialog("Advanced Sholl Analysis v" + VERSION);
            Font font = new Font("SansSerif", Font.PLAIN, 13);
            gd.addMessage(msg, font);
            gd.addHelp(URL);
            gd.setHelpLabel("Online Help");
            gd.hideCancelButton();
            gd.showDialog();
        }
    }
}
