import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.frame.Recorder;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.Tools;
import java.util.Arrays;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;


/**
 * @author Tiago Ferreira v2.0  Feb 13, 2012
 * @author Tom Maddock    v1.0c Oct 26, 2005
 *
 * This plugin presents an automated way of conducting Sholl Analysis on a neuron's
 * dendritic structure. It's most native mode of operation is to analyze a neuron that has
 * already been traced, yet it can also analyze a direct image of a neuron as long as that
 * image has been thresholded to ensure that every pixel defining the neuron has the same
 * intensity value.
 * Several advanced analysis methods are available: Linear (N), Linear (N/S), Semi-log and
 * Log-log as described in Milosevic and Ristanovic, J Theor Biol (2007) 245(1)130-40
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation
 * (http://www.gnu.org/licenses/gpl.txt).
 *
 */

public class Advanced_Sholl_Analysis implements PlugIn {

    /* Version Information */
    public static final String VERSION = "2.2b";

    /* Bin Function Type Definitions */
    private static final String[] BIN_TYPES = {"Mean", "Median"};
    private static final int BIN_AVERAGE = 0;
    private static final int BIN_MEDIAN  = 1;

    /* Sholl Type Definitions */
    private static final String[]
        SHOLL_TYPES = {"Intersections (N)", "Inters./Area (N/S)", "Semi-Log", "Log-Log"};
    private static final int SHOLL_N    = 0;
    private static final int SHOLL_NS   = 1;
    private static final int SHOLL_SLOG = 2;
    private static final int SHOLL_LOG  = 3;
    private static final String[]
        DEGREES= {"4th degree", "5th degree", "6th degree", "7th degree", "8th degree"};

    /* Default Dialog Options */
    private static double  UnitStart   = 10.0;
    private static double  UnitEnd     = 100.0;
    private static double  UnitStep    = 10.0;
    private static double  UnitWidth   = 0.0;
    private static int     BinChoice   = 1;
    private static int     ShollChoice = 0;
    private static boolean FitCurve    = true;
    private static int     PolyChoice  = 1;
    private static boolean Verbose     = false;
    private static boolean MakeMask    = true;
    private static String  ScaleUnit   = "Pixels";

    /* Reference to the plugin's dialogs */
    private static GenericDialog gd = null;

    /* Common variables */
    private static double[] yscale;
    private static String yTitle;

    public void run(String arg0) {

        ImagePlus image = IJ.getImage();          // Get current image
        ImageProcessor ip = image.getProcessor(); // Get the ImageProcessor for the image

        // Make sure the image is the right type
        if (!ip.isBinary()) {
            error("8-bit binary image (black and white only) is required. Binary\n"
                 +"  images can be created using Image>Adjust>Threshold...");
            return;
        }

        Calibration cal = image.getCalibration(); // Get the image calibration
        double scale = 1.0;                       // Set default dimensions info

        // Get image dimension info from the calibration
        if (cal != null && cal.pixelHeight == cal.pixelWidth)  {
            ScaleUnit = cal.getUnits();
            scale = cal.pixelHeight;
        }

        Roi roi = image.getRoi(); // Get current ROI

        if (!IJ.macroRunning() && !((roi != null && roi.getType() == Roi.LINE) ||
                                    (roi != null && roi.getType() == Roi.POINT))) {

                WaitForUserDialog wd = new WaitForUserDialog("Advanced Sholl Analysis v"
                                 + VERSION, "Please define the center of analysis using\n"
                                 + "the Point Selection Tool or, alternatively, by\n"
                                 + "creating a straight line starting at the center.");
            wd.show();
            if (wd.escPressed()) return;
        }


        roi = image.getRoi(); // Re-get ROI in case it has changed

        int x, y;
        if (roi != null && roi.getType() == Roi.LINE) {

            Line line = (Line)roi;                  // Recast the ROI into its true self
            x = line.x1; y = line.y1;               // Get the center coordinates
            UnitEnd = line.getRawLength()*scale;    // Get the length of the line

        } else if (roi != null && roi.getType() == Roi.POINT) {

            PointRoi point = (PointRoi)roi;         // Recast the ROI into its true self
            Rectangle rect = point.getBounds();     // Get the center coordinates
            x = rect.x; y = rect.y;

        } else { // Not a proper ROI type

            error("Line or Point selection required.");
            return;

        }

        // Set the initial values
        UnitStart = (UnitStart > UnitEnd) ? 0 : UnitStart;
        UnitStep  = (UnitStep > UnitEnd - UnitStart) ? 0 : UnitStep;

        do {

            // Create the plugin dialog box
            gd = new GenericDialog("Advanced Sholl Analysis v" + VERSION);
            gd.addNumericField("Starting radius:", UnitStart, 2, 9, ScaleUnit);
            gd.addNumericField("Ending radius:", UnitEnd, 2, 9, ScaleUnit);
            gd.addNumericField("Radius step size:", UnitStep, 2, 9, ScaleUnit);
            gd.addNumericField("Radius_span:", UnitWidth, 2, 9, ScaleUnit);
            gd.addChoice("Span_type:", BIN_TYPES, BIN_TYPES[BinChoice]);
            gd.addChoice("Sholl method:", SHOLL_TYPES, SHOLL_TYPES[ShollChoice]);
            gd.setInsets(10, 6, 0);
            gd.addCheckbox("Fit profile and compute descriptors", FitCurve);
            gd.addChoice("Polynomial:", DEGREES, DEGREES[PolyChoice]);
            gd.setInsets(-5, 35, 0);
            gd.addCheckbox("Show parameters", Verbose);
            gd.setInsets(10, 6, 0);
            gd.addCheckbox("Create intersections mask", MakeMask);
            gd.addHelp("http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:asa:start");
            gd.showDialog();

            // Stop if the user pressed cancel
            if (gd.wasCanceled()) { gd = null; return; }

            // Get the values from the PluginDialog
            UnitStart = Math.max(0, gd.getNextNumber());
            UnitEnd = Math.max(0, gd.getNextNumber());
            UnitStep = Math.max(0, gd.getNextNumber());
            UnitWidth = Math.max(0, gd.getNextNumber());
            BinChoice = gd.getNextChoiceIndex();
            ShollChoice = gd.getNextChoiceIndex();
            FitCurve = gd.getNextBoolean();
            PolyChoice = gd.getNextChoiceIndex();
            Verbose = gd.getNextBoolean();
            MakeMask = gd.getNextBoolean();

            // Check Start and End Points
            if (UnitStart >= UnitEnd) {
                error("Invalid Parameters!");
                if (Recorder.getInstance()!=null) return;
            } else
                break;

            // Keep presenting dialog until the user gets it right
        } while (true);

        // Set lower bounds of parameters
        double unitstep  = Math.max(scale, UnitStep);
        double unitwidth = Math.max(scale, UnitWidth);
        double unitstart = UnitStart; //Math.max(scale, UnitStart); //

        // Calculate how many samples we're taking
        int size = (int)((UnitEnd - unitstart) / unitstep) + 1;

        // Create an x-values and pixel radii array
        int[] radii = new int[size];
        double[] xvalues = new double[size];

        // Populate the arrays
        for (int i = 0; i < size; i++) {
            xvalues[i] = unitstart + i*unitstep;
            radii[i] = (int)Math.round(xvalues[i]/scale);
        }

        IJ.showStatus("Counting intersections...");

        // Analyze the data and return raw Sholl intersections
        double[] yvalues = analyze(x, y, radii, (int)Math.round(unitwidth/scale), BinChoice, ip);

        IJ.showStatus("Making plot...");

        // Display the analysis and return transformed data
        double[] grays = plotValues(image.getTitle(), ShollChoice, radii, xvalues, yvalues, x, y);

        // Create intersections mask
        if (MakeMask) {
            IJ.showStatus("Creating intersections mask...");

            ImagePlus img2 = IJ.createImage(SHOLL_TYPES[ShollChoice]+" mask for "+ image.getTitle(),
                                            "16-bit black", image.getWidth(), image.getHeight(), 1);

            ImageProcessor ip2 = img2.getProcessor();

            int[][] points; int i, j, k; double arbor;
            int drawRadius; int drawSteps = grays.length;
            int drawWidth = (int)( ((UnitEnd-unitstart)/scale)/drawSteps ) ;

            for (i=0; i<drawSteps; i++) {
                IJ.showProgress(i, drawSteps);
                drawRadius = (int)Math.round( (unitstart/scale) + (i*drawWidth) );
                points = getCircumferencePoints(x, y, drawRadius); //drawWidth

                for (j = 0; j < points.length; j++) {
                    for (k = 0; k < points[j].length; k++) {
                        if (ip.getPixel(points[j][0], points[j][1])!=0)
                            ip2.putPixelValue(points[j][0], points[j][1], grays[i]);
                    }
                }
            }

            IJ.showProgress(0, 0);
			
		    // mention type of mask in image label
			String metadata = "Raw data";
			if (FitCurve) metadata = "Fitted data";
			img2.setProperty("Label", metadata);

            // Adjust levels, apply calibration of measured image and display mask
            double[] levels = Tools.getMinMax(grays);
            IJ.run(img2, "Fire", ""); //"Fire", "Ice", "Spectrum", "Redgreen"
            ip2.setMinAndMax(levels[0], levels[1]);
            img2.setCalibration(cal);
            img2.show();
        }

        gd = null;
    }


/* Does the actual analysis. Accepts an array of radius values and takes the measurements
 * for each */
    static public double[] analyze(int x, int y, int[] radii, int binsize, int bintype,
                                   ImageProcessor ip) {

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
                points = getCircumferencePoints(x, y, rbin);
                pixels = getPixels(ip, points);

                // Count the number of intersections
                binsamples[j] = countTargetGroups(pixels, points, ip);

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


/* Counts how many groups of non-zero pixels are present in the given data. A group consists
 * of adjacent pixels, where adjacency is true for all eight neighboring positions around
 * a given pixel. */
    static public int countTargetGroups(int[] pixels, int[][] rawpoints, ImageProcessor ip) {

        int i, j;
        int[][] points;

        // Count how many non-zero foreground we have
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0) j++;

        // Create an array to hold the targetpixels
        points = new int[j][2];

        // Copy all targetpixels into the array
        for (i = 0, j = 0; i < pixels.length; i++)
            if (pixels[i] != 0)
                points[j++] = rawpoints[i];

        return countGroups(points, 1.5, ip);

    }


/* For a set of points in 2d space, counts how many groups there are such that for every
 * point in each group, there exists another point in the same group that is less than
 * threshold units of distance away. If a point is greater than threshold units away from
 * all other points, it is in its own group. */
    static public int countGroups(int[][] points, double threshold, ImageProcessor ip) {

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

                    // Change all targets to sources
                    for (k = 0; k < len; k++)
                        if (grouping[k] == target)
                            grouping[k] = source;

                    // Update the number of groups
                    groups--;

                }
            }

       /* If the edge of the group lies tangent to the sampling circle, multiple
     	* intersections with that circle will be counted. We will try to find
        * these "false positives" and throw them out. A way to attempt this (we
        * will be missing some of them) is to throw out 1-pixel groups that
        * exist solely on the edge of a "stair" of target pixels */         

        boolean multigroup;
        int[] px;
        int[][] testpoints = new int[8][2];

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

/* Returns the location of pixels clockwise along a (1-pixel wide) circumference 
 * using  Bresenham's Circle Algorithm */
    static public int[][] getCircumferencePoints(int cx, int cy, int r) {

        // Initialize algorithm variables
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
                y--; err = errD; // Go down
            } else {
                x++; err = errR; // Go right
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

/* Create the Sholl plot */
    static public double[] plotValues(String ttl, int mthd, int[] r, double[] xpoints,
                                  double[] ypoints, int xcenter, int ycenter) {

        int i, j; String label = "", xTitle;

        int size = ypoints.length; int nsize = 0;

        // Remove points with zero intersections avoiding log(0)
        for (i = 0; i <size; i++)
            if (ypoints[i]!=0.0) nsize++;
        double[] x = new double [nsize];
        double[] y = new double [nsize];
        for (i = 0, j = 0; i <size; i++) {
            if (ypoints[i]!=0.0) {
                x[j] = xpoints[i];
                y[j++] = ypoints[i];
            }
        }

        // Get limits for plot axis
        double[] xscale = Tools.getMinMax(x); yscale = Tools.getMinMax(y);

        // Transform intersections for non-traditional Sholl
        if (mthd==SHOLL_SLOG || mthd==SHOLL_LOG) {

            for (i =0; i < y.length; i++)
                y[i] = Math.log(y[i]/(Math.PI*r[i]*r[i]));
            yscale = Tools.getMinMax(y);
            yTitle = "log(N/S)";

        } else if (mthd==SHOLL_NS) {

            for (i =0; i < y.length; i++)
                y[i] = y[i]/(Math.PI*r[i]*r[i]);
            yscale = Tools.getMinMax(y);
            yTitle = "# Inters./Area ("+ ScaleUnit +"\u00B2)";

        } else
            yTitle = "# Intersections";

        // Transform radii for Log-Log Sholl */
        if (mthd==SHOLL_LOG) {

            for (i =0; i < nsize; i++)
                x[i] = Math.log(x[i]);
            xscale = Tools.getMinMax(x);
            xTitle = "log(R)";

        } else
            xTitle = "Radius ("+ ScaleUnit +")";

        // Creat the plot
        int FLAGS = Plot.X_FORCE2GRID + Plot.X_TICKS + Plot.X_NUMBERS
                  + Plot.Y_FORCE2GRID + Plot.Y_TICKS + Plot.Y_NUMBERS
                  + PlotWindow.CIRCLE;
        PlotWindow.noGridLines = false; // draw grid lines
        float[] mock = null;            // start an empty plot

        Plot plot = new Plot("Sholl Analysis for " + ttl,xTitle,yTitle, mock, mock,FLAGS);
        plot.setLimits(xscale[0], xscale[1], yscale[0], yscale[1]);

        // Plot original data
        plot.addPoints(x, y, Plot.CIRCLE); //default color is black

        // Curve fitting
        if (!FitCurve) {

			plot.show();
			return y;

        } else {
			
			if (nsize<=6) {
				IJ.log("\nCurve fitting requires more than 6 sampled points"
					   +"\nPlease adjust parameters...");
				plot.show();
				return y;
				
			}

            CurveFitter cf = new CurveFitter(x, y);
            //cf.setRestarts(4); // default: 2;
            //cf.setMaxIterations(50000); //default: 25000
            double[] fy = new double[nsize];

            if (mthd==SHOLL_N) {

                if (DEGREES[PolyChoice].startsWith("4"))
                    cf.doFit(CurveFitter.POLY4, false);
                else if (DEGREES[PolyChoice].startsWith("5"))
                    cf.doFit(CurveFitter.POLY5, false);
                else if (DEGREES[PolyChoice].startsWith("6")) 
                    cf.doFit(CurveFitter.POLY6, false);
                else if (DEGREES[PolyChoice].startsWith("7")) 
                    cf.doFit(CurveFitter.POLY7, false);
                else if (DEGREES[PolyChoice].startsWith("8")) 
                    cf.doFit(CurveFitter.POLY8, false);

            } else if (mthd==SHOLL_NS) {

                cf.doFit(CurveFitter.POWER, false);

            } else if(mthd==SHOLL_SLOG) {

                cf.doFit(CurveFitter.STRAIGHT_LINE, false);

            } else if(mthd==SHOLL_LOG) {

                cf.doFit(CurveFitter.EXP_WITH_OFFSET, false);

            }

            // Get fitted values
            double[] parameters = cf.getParams();
            for (i=0; i<nsize; i++)
                fy[i] = cf.f(parameters, x[i]);


            // Linear Sholl: Calculate critical value (cv), critical radius (cr) & Nav
            if (mthd==SHOLL_N) {

                // Calculate N_AV, the average of intersections over the whole area occupied
                // by the dendritic arbor, i.e., the mean value of the fitted Sholl function.
                // This can be done assuming that the mean value is the height of a rectangle
                // that has the width of (NonZeroEndRadius - NonZeroStartRadius) and the same
                // area of the area under the fitted curve on that discrete interval
                double N_AV = 0.0;
                for (i=0; i<parameters.length-1; i++ ) //-1?
                    N_AV = N_AV + ( (parameters[i]/(i+1)) * (Math.pow(xscale[1]-xscale[0], i)) );

                // Get index of highest fitted value
                int maxidx = cf.getMax(fy);

                // Get the coordinates of the "critical value", i.e, local maximum of polynomial.
                // We'll iterate around maxidx and retrive the values empirically. This is
                // obviously imprecise
                int iterations  = 1000;
                double cvxleft  = (x[maxidx-1] + x[maxidx])/2;
                double cvxright = (x[maxidx+1] + x[maxidx])/2;
                double step     = (cvxright-cvxleft)/iterations;
                double cvx = 0.0, cvy = 0.0, cvxtmp = 0.0, cvytmp = 0.0;
                for (i=0; i<iterations; i++) {
                    cvxtmp = cvxleft + (i*step);
                    cvytmp = cf.f(parameters, cvxtmp);
                    if(cvytmp>cvy) { cvy = cvytmp; cvx = cvxtmp; }
                }

                // Adjust font size
                plot.changeFont(new Font("SansSerif", Font.PLAIN, 11));

                // Add mean value, Ramification (Schoenen) index, Cv, Cr and Nav to label
                label = "RI: " + IJ.d2s(cvy/y[0], 2);
                label = label +"\nNav= " + IJ.d2s(N_AV,2);
                label = label +"\nCv= "+ IJ.d2s(cvy, 2);
                label = label +"\nCr= "+ IJ.d2s(cvx, 2);
                label = label +"\nPR: "+ DEGREES[PolyChoice];

                // highlight mean value of function
                plot.setLineWidth(1);
                plot.setColor(Color.lightGray);
                plot.drawLine(xscale[0], N_AV, xscale[1], N_AV);

            }

            // Plot fitted data
            plot.setColor(Color.gray);
            plot.setLineWidth(2);
            plot.addPoints(x, fy, PlotWindow.LINE);

            // Semi-log Sholl: extract Sholl decay
            if (mthd==SHOLL_SLOG)
                label = "k= "+ IJ.d2s(parameters[0],-3);

            if (Verbose) {
                IJ.log("***\nSholl Analysis ["+ SHOLL_TYPES[mthd] +"] for "+ ttl +":");
                IJ.log("Center of analysis: "+ xcenter +", "+ ycenter);
                IJ.log("Input Starting radius: "+ UnitStart + ScaleUnit);
                IJ.log("Input Ending radius: "+ IJ.d2s(UnitEnd,2) + ScaleUnit);
                IJ.log("Input radius step: "+ UnitStep + ScaleUnit);
                IJ.log("Input radius span: "+ UnitWidth + ScaleUnit);
                IJ.log("Sample size: "+ size);
                IJ.log(label);
                IJ.log("Fitting details:"+ cf.getResultString());
            }

            label = label + "\nR\u00B2= " + IJ.d2s(cf.getRSquared(), 3);
            plot.setJustification(ImageProcessor.RIGHT_JUSTIFY); //2
            plot.setColor(Color.black);
            plot.addLabel(0.99, 0.08, label);
			
			// Show the plot window and return data
			plot.show();
			return fy;
        }
    }

/* Allow error messages to open an URL */
   void error(String error) {
        if (IJ.macroRunning())
            IJ.error("Advanced Sholl Analysis v" + VERSION, error);
        else {
            GenericDialog gd = new GenericDialog("Advanced Sholl Analysis v" + VERSION);
            Font font = new Font("SansSerif", Font.PLAIN, 13);
            gd.addMessage(error, font);
            gd.addHelp("http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:asa:start");
            gd.hideCancelButton();
            gd.showDialog();
        }
   }
}
