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

/*
 * Created on Oct 26, 2005
 */

/**
 * @author Tom Maddock
 *
 * This plugin presents an automated way of conducting Sholl Analysis on a neuron's
 * dendritic structure.  It's most native mode of operation is to analyze a neuron
 * that has already been traced, yet it can also analyze a direct image of a neuron
 * as long as that image has been thresholded to ensure that every pixel constituting
 * part of the neuron has the same intensity value.
 */

public class ShollAnalysis_ implements PlugIn, ActionListener {

    /* Version Information */
    public static final String VERSION = "1.0";

    /* Analysis Constants */

    /* Sometimes, if the edge of a dendritic branch lies roughly tangent to
     * a sampling circle, it is counted as having multiple intersections with
     * that circle, thus causing the resulting graph to appear "spikey".  This
     * is caused by the interplay between the pixels in the circle and the edge.
     * This variable enables an extra level of searching to try and find these
     * "false positives" and throw them out, thereby making the analysis more
     * accurate, but just a tad bit slower. */
    private static final boolean DoSpikeSupression = true;

    /* This variable controls the width in pixels of circle circumference that
     * is used to count intersections.  Testing reveals this is best left at 1. */
    private static final int LineWidth = 1;

    /* Bin Function Type Definitions */
    private static final int BIN_AVERAGE = 0;
    private static final int BIN_MEDIAN  = 1;
    private static final String[] BIN_TYPES = {"Mean", "Median"};

    /* Default Dialog Options */
    private static double UnitStart = 10.0;
    private static double UnitEnd   = 100.0;
    private static double UnitStep  = 10.0;
    private static double UnitWidth = 0.0;
    private static int    BinChoice = 1;

    /* Reference to the plugin's dialog */
    private static GenericDialog PluginDialog = null;

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


    public void run(String arg0) {

        // Should we display the help message?
        if (IJ.shiftKeyDown() || (arg0 != null && arg0.equalsIgnoreCase("HELP"))) {

            // Show the help message
            showHelp();

            // Unset the shift key if it is set
            if (IJ.shiftKeyDown())
                IJ.setKeyUp(java.awt.event.KeyEvent.VK_SHIFT);

            return;

        }

        // Get the current image
        ImagePlus image = IJ.getImage();

        // Make sure the image is the right type
        if (image.getType() != ImagePlus.GRAY8) {

            IJ.showMessage("Error", "8-Bit Grayscale Image Required");
            return;

        }

        // Get the image calibration
        Calibration cal = image.getCalibration();

        // Set default dimensions info
        String scalestring = "Pixels";
        double scale = 1.0;

        // Get image dimension info from the calibration
        if (cal != null && cal.pixelHeight == cal.pixelWidth)  {
            scalestring = cal.getUnits();
            scale = cal.pixelHeight;
        }

        // Get the ImageProcessor for the image
        ImageProcessor ip = image.getProcessor();

        // Get the current ROI
        Roi roi = image.getRoi();

        int x, y;

        if (roi != null && roi.getType() == Roi.LINE) {

            //Recast the ROI into its true self
            Line line = (Line)roi;

            // Get the center coordinates
            x = line.x1; y = line.y1;

            // Get the length of the line
            UnitEnd = line.getRawLength()*scale;

        } else if (roi != null && roi.getType() == Roi.POINT) {

            //Recast the ROI into its true self
            PointRoi point = (PointRoi)roi;

            // Get the center coordinates
            Rectangle rect = point.getBoundingRect();
            x = rect.x; y = rect.y;

        } else {

            // Not a proper ROI type
            IJ.showMessage("Error", "Point or Line Selection Required");
            return;

        }

        // Get the target color
        int color = ip.getPixel(x, y);

        // Set the initial values
        UnitStart = (UnitStart > UnitEnd) ? 0 : UnitStart;
        UnitStep  = (UnitStep > UnitEnd - UnitStart) ? 0 : UnitStep;

        // Setup the help button
        Button helpbutton = new Button("Click For Additional Help");
        helpbutton.setActionCommand("OPENHELP");
        helpbutton.addActionListener(this);

        // Setup the help panel
        Panel helppanel = new Panel();
        helppanel.add(helpbutton);

        do {

        // Create the plugin PluginDialog box
        PluginDialog = new GenericDialog("Sholl Analysis Options");

        // Add the parameter fields
        PluginDialog.addMessage("Unit Scale: 1 Pixel = " + scale + " " + scalestring);
        PluginDialog.addMessage("Analysis Parameters (in " + scalestring + ")");
        PluginDialog.addNumericField("Starting Radius", UnitStart, 2);
        PluginDialog.addNumericField("Ending Radius", UnitEnd, 2);
        PluginDialog.addNumericField("Radius Step Size", UnitStep, 2);
        PluginDialog.addNumericField("Radius Span", UnitWidth, 2);
        PluginDialog.addChoice("Span Type", BIN_TYPES, BIN_TYPES[BinChoice]);

        // Add the help button
        PluginDialog.addPanel(helppanel, GridBagConstraints.SOUTH, new Insets(5,0,0,0));

        // Display the PluginDialog
        PluginDialog.showDialog();

        // Stop if the user pressed cancel
        if (PluginDialog.wasCanceled()) {
            PluginDialog = null;
            return;
        }

        // Get the values from the PluginDialog
        UnitStart = Math.max(0, PluginDialog.getNextNumber());
        UnitEnd = Math.max(0, PluginDialog.getNextNumber());
        UnitStep = Math.max(0, PluginDialog.getNextNumber());
        UnitWidth = Math.max(0, PluginDialog.getNextNumber());
        BinChoice = PluginDialog.getNextChoiceIndex();

        // Check the Start and End Points
        if (UnitStart > UnitEnd)
            IJ.showMessage("Error", "Invalid Start and End Parameters");
        else
            break;

        // Keep presenting the dialog until the user gets it right
        } while (true);

        // Set the lower bound for these variables
        double unitstep = Math.max(scale, UnitStep);
        double unitwidth = Math.max(scale, UnitWidth);

        // Calculate how many samples we're taking
        int size = (int)((UnitEnd - UnitStart) / unitstep) + 1;

        // Create an x-values and pixel radii array
        int[] radii = new int[size];
        double[] xvalues = new double[size];

        // Populate the arrays
        for (int i = 0; i < size; i++) {
            xvalues[i] = UnitStart + i*unitstep;
            radii[i] = (int)Math.round(xvalues[i]/scale);
        }

        // Analyze the data
        double[] yvalues = analyze(x, y, radii, (int)Math.round(unitwidth/scale), BinChoice, color, ip);

        // Create a plot of the results
        Plot plot = new Plot("Sholl Analysis for " + image.getTitle(),
                "Distance (" + scalestring + ")", "# of Intersections", xvalues, yvalues);

        double max = 0;

        // Figure out the maximum y value
        for (int i = 0; i < yvalues.length; i++)
            if (yvalues[i] > max) max = yvalues[i];

        // Fix the plot range and add tick marks
        prettyPlot(plot, xvalues[xvalues.length-1], (int)Math.round(max));

        // Show the plot window
        plot.show();

        PluginDialog = null;

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
        if (PluginDialog != null)
            helpdialog = new JDialog(PluginDialog, HelpTitle, false);
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

    /* Does the work of the analysis. Accepts an array of radius values, and takes the appropriate measurements for each */
    static public double[] analyze(int x, int y, int[] radii, int binsize, int bintype, int targetColor, ImageProcessor ip) {

       int i, j, k, r, rbin, sum, size;
       int[] binsamples, pixels;
       int[][] points;
       double[] data;

       // Create an array to hold the results
       data = new double[size = radii.length];

       // Create an array for the bin samples
       binsamples = new int[Math.max(binsize, 0)];

       // Outer loop to control the analysis bins
       for (i = 0; i < size; i++) {

           // Set the progress bar
           IJ.showProgress(i, size);

           // Get the radius we are sampling
           r = radii[i];

           // Set the first radius for this bin
           rbin = r - (int)Math.round((double)binsize / 2.0);

           j = 0;

           // Cut any analysis below a 0 radius
           if (rbin < 0) {j = -rbin; rbin = 0;}

           // Inner loop to gather samples for each bin
           for (; j < binsize; j++, rbin++) {

               // Get the circle pixels for this radius
               points = getCircumferencePoints(x, y, rbin, LineWidth);
               pixels = getPixels(ip, points);

               // Count the number of intersections
               binsamples[j] = countTargetGroups(pixels, points, targetColor, ip);

           }

           // Now perform the proper function on the bin data
           if (bintype == BIN_MEDIAN && binsize > 0) {

               // Sort the bin data
               Arrays.sort(binsamples);

               // Pull out the median value
               data[i] = (binsize % 2 == 0)

                       // Average the two middle values if no center exists
                       ? ((double)(binsamples[binsize/2]+binsamples[binsize/2-1]))/2.0

                       // Pull out the center value
                       : (double)binsamples[binsize/2];

           } else if (binsize > 0) {

               /* Average bin samples by default */

               // Find the sum of the samples
               for (sum = 0, k = 0; k < binsize; k++)
                   sum += binsamples[k];

               // Divide the sum by the number of samples
               data[i] = ((double)sum) / ((double)binsize);

           } else data[i] = 0;

       }

       // Set the progress bar
       IJ.showProgress(0, 0);

       return data;

    }

    /* Counts how many groups of targetColor are present in the given data.
     * A group consists of a formation of adjacent pixels, where adjacency
     * is true for all eight neighboring positions around a given pixel. */
    static public int countTargetGroups(int[] pixels, int[][] rawpoints, int targetColor, ImageProcessor ip) {

        int i, j;
        int[][] points;

        // Count how many targetcolors we have

        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] == targetColor) j++;

        // Create an array to hold the targetpixels
        points = new int[j][2];

        // Copy all targetpixels into the array
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] == targetColor)
                points[j++] = rawpoints[i];

        return countGroups(points, 1.5, targetColor, ip);

    }

    /* For a set of points in 2d space, counts how many groups there are
     * such that for every point in each group, there exists another
     * point in the same group that is less than threshold units of
     * distance away.  If a point is greater than threshold units of
     * distance away from all other points, it is in its own group. */
    static public int countGroups(int[][] points, double threshold, int tc, ImageProcessor ip) {

        double distance;
        int i, j, k, target, source, dx, dy, groups, len;

        // Create an array to hold the point grouping data
        int[] grouping = new int[len = points.length];

        // Initialize each point to be in a unique group
        for (i = 0, groups = len; i < groups; i++)
            grouping[i] = i+1;

        for (i = 0; i < len; i++)
            for (j = 0; j < len; j++) {

                // Don't compare the same point with itself
                if (i == j) continue;

                // Compute the distance between the two points
                dx = points[i][0] - points[j][0];
                dy = points[i][1] - points[j][1];
                distance = Math.sqrt(dx*dx + dy*dy);

                // Should these two points be in the same group?
                if ( (distance <= threshold) && (grouping[i] != grouping[j]) ) {

                    // Record which numbers we're changing
                    source = grouping[i];
                    target = grouping[j];

                    // Chance all targets to sources
                    for (k = 0; k < len; k++)
                        if (grouping[k] == target)
                            grouping[k] = source;

                    // Update the number of groups
                    groups--;

                }

            }

        /* Our next task is to throw out 1-pixel groups satisfying the condition that they
         * exist solely on the edge of a "stair" of target pixels. These such groups seem
         * to significantly contribute to the number of "spikes" in the Sholl Analysis
         * results, and although we cannot get all of them, this should suppress a great
         * deal of the spikes.
         */

        if (DoSpikeSupression) {

        boolean multigroup;
        int[] pixels;
        int[][] testpoints = new int[8][2];
        byte pixelmask;

        for (i = 0; i < len; i++) {

           // Check for other members of this group
           for (multigroup = false, j = 0; j < len; j++) {
               if (i == j) continue;
               if (grouping[i] == grouping[j]) {
                   multigroup = true;
                   break;
               }
           }

           // If not a single-pixel group, try again
           if (multigroup) continue;

           // Save the coordinates of this point
           dx = points[i][0]; dy = points[i][1];

           // Calculate the points surrounding this point
           testpoints[0][0] = dx-1; testpoints[0][1] = dy+1;
           testpoints[1][0] = dx  ; testpoints[1][1] = dy+1;
           testpoints[2][0] = dx+1; testpoints[2][1] = dy+1;
           testpoints[3][0] = dx-1; testpoints[3][1] = dy  ;
           testpoints[4][0] = dx+1; testpoints[4][1] = dy  ;
           testpoints[5][0] = dx-1; testpoints[5][1] = dy-1;
           testpoints[6][0] = dx  ; testpoints[6][1] = dy-1;
           testpoints[7][0] = dx+1; testpoints[7][1] = dy-1;

           // Pull out the pixel values for these points
           pixels = getPixels(ip, testpoints);

           // Now perform the stair checks
           if ( (pixels[0]==tc && pixels[1]==tc && pixels[3]==tc &&
                 pixels[4]!=tc && pixels[6]!=tc && pixels[7]!=tc) ||
                (pixels[1]==tc && pixels[2]==tc && pixels[4]==tc &&
                 pixels[3]!=tc && pixels[5]!=tc && pixels[6]!=tc) ||
                (pixels[4]==tc && pixels[6]==tc && pixels[7]==tc &&
                 pixels[0]!=tc && pixels[1]!=tc && pixels[3]!=tc) ||
                (pixels[3]==tc && pixels[5]==tc && pixels[6]==tc &&
                 pixels[1]!=tc && pixels[2]!=tc && pixels[4]!=tc) )

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
            x = points[i][0]; y = points[i][1];

            // Check if the coordinates are valid for the image
            pixels[i] = (x < 0 || x >= ip.getWidth() || y < 0 || y >= ip.getHeight())

                // Use -1 to indicate an invalid pixel location
                ? -1

                // Get the pixel value in the image
                : ip.getPixel(points[i][0], points[i][1]);

        }

        return pixels;

    }

    /* Return the location of along a circumference of a given width */
    static public int[][] getCircumferencePoints(int cx, int cy, int r, int width) {

        int i, j, x;

        // Shortcut for unit width
        if (width == 1) return getCircumferencePoints(cx, cy, r);

        // Create an array to store all the points
        int[][][] points = new int[width][][];

        // Choose which radius to start the circumference at
        x = Math.max(0, r - (int)Math.round((double)width/2.0));

        // Get the points for each individual circumference
        for (i = 0; i < width; i++, x++)
            points[i] = getCircumferencePoints(cx, cy, x);

        // Figure out how many points there are in total
        for (i = 0, x = 0; i < width; i++)
            x += points[i].length;

        // Create a new array to hold all the points
        int[][] result = new int[x][];

        // Copy all the points into the combined array
        for (i = 0, x = 0; i < width; i++)
            for (j = 0; j < points[i].length; j++)
                result[x++] = points[i][j];

        return result;

    }

    /* Return the location of pixels clockwise along the circumference of the given circle */
    static public int[][] getCircumferencePoints(int cx, int cy, int r) {

        /* Implementation of Bresenham's Circle Algorithm */

        // Initialize Algorithm Variables
        int i = 0, x = 0, y = r, err = 0, errR, errD;

        // Array to store first 1/8 of points relative to center
        int[][] data = new int[++r][2];

        do {

            // Add this point as part of the circumference
            data[i][0] = x; data[i++][1] = y;

            // Calculate the errors for going right and down
            errR = err + 2*x + 1; errD = err - 2*y + 1;

            // Choose which direction to go
            if (Math.abs(errD) < Math.abs(errR)) {

                // Go down
                y--; err = errD;

            } else {

                // Go right
                x++; err = errR;

            }

        } while (x <= y);

        // Create an array to hold the absolute coordinates
        int[][] points = new int[r*8][2];

        // Loop through the relative circumference points
        for (i = 0; i < r; i++) {

            // Pull out the point for quick access;
            x = data[i][0]; y = data[i][1];

            // Convert the relative point to an absolute point
            points[i][0] = x + cx; points[i][1] = y + cy;

            // Use geometry to calculate the other 7/8 of the circumference points
            points[r*4-i-1][0] =  x + cx; points[r*4-i-1][1] = -y + cy;
            points[r*8-i-1][0] = -x + cx; points[r*8-i-1][1] =  y + cy;
            points[r*4+i]  [0] = -x + cx; points[r*4+i]  [1] = -y + cy;
            points[r*2-i-1][0] =  y + cx; points[r*2-i-1][1] =  x + cy;
            points[r*2+i]  [0] =  y + cx; points[r*2+i]  [1] = -x + cy;
            points[r*6+i]  [0] = -y + cx; points[r*6+i]  [1] =  x + cy;
            points[r*6-i-1][0] = -y + cx; points[r*6-i-1][1] = -x + cy;

        }

        // Create a new array to hold points without duplicates
        int[][] refined = new int[Math.max(points.length-8, 1)][2];

        // Copy the first point manually
        refined[0] = points[0];

        // Loop through the rest of the points
        for (i = 1, x = 1; i < points.length; i++) {

            // Duplicates are always at multiples of r
            if ((i+1) % r == 0) continue;

            // Copy the non-duplicate
            refined[x++] = points[i];

        }

        // Return the array without duplicates
        return refined;

    }

    /* Sets the viewing range of the plot with 0 at the origin and xmax
     * and ymax as the maximum values for the respective axes, as well
     * as drawing tick marks on the y axis at no less than 1 unit intervals. */
    public static void prettyPlot(Plot plot, double xmax, double ymax) {

        // First set the plot viewing range
        plot.setLimits(0, xmax, 0, ymax);

        // Get the processor of the plot
        ImageProcessor ip = plot.getProcessor();

        /* Add tick marks to y axis */

        int skip = 1;

        // Figure out what interval ticks will be drawn at
        while ((ymax)/skip >= 50) skip++;

        // Figure out the length of one unit in pixels
        double unitlength = 200.0 / ymax;

        // Set drawing variables
        int value, bottom = ip.getHeight() - 30;

        // Draw tick marks
        for (int i = 0; i <= ymax; i += skip) {

            // Compute where we will draw the mark
            value = bottom - (int)(unitlength * i);

            // Draw the tick mark
            ip.putPixel( 49, value, 0 );
            ip.putPixel( 48, value, 0 );

        }

        /* Add tick marks to x axis */

        skip = 1;

        // Figure out what interval ticks will be drawn at
        while ((xmax)/skip >= 50) skip*=10;

        // Figure out the length of one unit in pixels
        unitlength = 450.0 / xmax;

        // Draw tick marks
        for (int i = 0; i <= xmax; i += skip) {

            // Compute where we will draw the mark
            value = 50 + (int)(unitlength * i);

            // Draw the tick mark
            ip.putPixel( value, bottom + 1, 0 );
            ip.putPixel( value, bottom + 2, 0 );

        }


    }

}
