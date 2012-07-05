/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 */

package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;
import pal.math.ConjugateDirectionSearch;
import pal.math.MultivariateFunction;
import util.BatchOpener;
import util.Quantile_Based_Normalization;
import vib.app.FileGroup;

/**
 *
 * @author mark
 */
public class Fit_Sphere implements PlugIn {

	public void run(String ignored) {

		String averagedNC82Path = "/Users/mark/central-complex-complete-vib-protocol/output_1/71yAAeastmost.tif";
		String normalizedRootDirectory = "/Users/mark/central-complex-complete-vib-protocol/normalized/";

		int[][][] pointsInShells =
{{
{ 226, 450, 50 },
{ 232, 526, 50 },
{ 250, 364, 50 },
{ 248, 564, 50 },
{ 254, 360, 47 },
{ 226, 420, 47 },
{ 228, 506, 47 },
{ 244, 544, 47 },
{ 246, 376, 44 },
{ 222, 456, 44 },
{ 240, 530, 44 },
},
{
{ 270, 456, 50 },
{ 298, 360, 50 },
{ 290, 540, 50 },
{ 292, 382, 47 },
{ 268, 456, 47 },
{ 284, 522, 47 },
{ 280, 524, 44 },
{ 266, 456, 44 },
{ 290, 386, 44 },
}};


/*
		int[][][] pointsInShells = { { {249, 365, 43},
					       {216, 454, 43},
					       {244, 552, 43},
					       {262, 337, 48},
					       {225, 446, 48},
					       {254, 576, 48} },
					     { {298, 362, 49},
					       {272, 456, 49},
					       {296, 553, 49},
					       {300, 358, 50},
					       {272, 454, 50},
					       {292, 554, 50},
					       {302, 349, 56},
					       {264, 450, 56},
					       {297, 566, 56} }
		};
*/

		/*
		   These next two points are those that define the angle
		   swepping through the fan-shaped body that we consider */

		int [] pointInferiorLowZ = { 294 ,466, 43 };
		int [] pointInferiorHighZ = { 294 ,466, 49 };

		int [] pointSuperiorLowZ = { 234, 484, 42 };
		int [] pointSuperiorHighZ = { 234, 484, 50 };

		/* We ignore the y co-ordinates of these and
		   exclude those outside the angle in the XZ
		   plane. */

		ImagePlus[] channels = BatchOpener.open(averagedNC82Path);
		if (channels == null) {
			IJ.error("Couldn't open: " + averagedNC82Path);
		}

		ImagePlus imp = channels[0];

		Calibration c = channels[0].getCalibration();

		double x_spacing = c.pixelWidth;
		double y_spacing = c.pixelHeight;
		double z_spacing = c.pixelDepth;

		int width = imp.getWidth();
		int height = imp.getHeight();
		int depth = imp.getStackSize();

		// Scale the limit points with the calibration...

		double [] pointMaxZDefiningAngle = new double[3];
		double [] pointMinZDefiningAngle = new double[3];

		pointMaxZDefiningAngle[0] = pointInferiorHighZ[0] * x_spacing;
		pointMaxZDefiningAngle[1] = pointInferiorHighZ[1] * y_spacing;
		pointMaxZDefiningAngle[2] = pointInferiorHighZ[2] * z_spacing;

		pointMinZDefiningAngle[0] = pointInferiorLowZ[0] * x_spacing;
		pointMinZDefiningAngle[1] = pointInferiorLowZ[1] * y_spacing;
		pointMinZDefiningAngle[2] = pointInferiorLowZ[2] * z_spacing;

		ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();

		// optimizer.prin = 2; // Show some debugging information...
		optimizer.step = 0.5;

		// The values we're optimizing are the radius of the sphere and its centre.
		double[] startValues = new double[3];

		double my_guess_x = 356 * x_spacing;
		double my_guess_y = 459 * y_spacing;
		double my_guess_z = 45 * z_spacing;

		startValues[0] = (width * x_spacing) / 2;
		startValues[1] = (height * y_spacing) / 2;
		startValues[2] = (depth * z_spacing) / 2;

/*
		startValues[0] = my_guess_x;
		startValues[1] = my_guess_y;
		startValues[2] = my_guess_z;
		// Goes to an incorrect local minimum...
		startValues[0] = 0;
		startValues[1] = 0;
		startValues[2] = 0;
		 */

		SphereAttempt attempt = new SphereAttempt(2);

		for (int shell = 0; shell < 2; ++shell) {
			int[][] pointsThisTime = pointsInShells[shell];
			for (int i = 0; i < pointsThisTime.length; ++i) {
				attempt.addPointInShell(shell, pointsThisTime[i][0] * x_spacing, pointsThisTime[i][1] * y_spacing, pointsThisTime[i][2] * z_spacing);
			}
		}

		optimizer.optimize(attempt, startValues, 2, 2);

		double resultScaled_x = startValues[0];
		double resultScaled_y = startValues[1];
		double resultScaled_z = startValues[2];

		// "max" is towards high Z...
		double maxAngle = Math.atan( (pointMaxZDefiningAngle[2] - resultScaled_z) / (resultScaled_x - pointMaxZDefiningAngle[0]) );
		double minAngle = Math.atan( (pointMinZDefiningAngle[2] - resultScaled_z) / (resultScaled_x - pointMinZDefiningAngle[0]) );

		System.out.println("maxAngle is "+maxAngle+" (in degrees: "+((maxAngle*180)/Math.PI)+")");
		System.out.println("minAngle is "+minAngle+" (in degrees: "+((minAngle*180)/Math.PI)+")");

		// Now work out the radii from this:
		double[] radii = new double[pointsInShells.length];

		for (int shell = 0; shell < pointsInShells.length; ++shell) {

			double distanceSum = 0;
			int[][] points = pointsInShells[shell];
			for (int i = 0; i < points.length; ++i) {

				double x = points[i][0] * x_spacing;
				double y = points[i][1] * y_spacing;
				double z = points[i][2] * z_spacing;

				double x_diff = x - resultScaled_x;
				double y_diff = y - resultScaled_y;
				double z_diff = z - resultScaled_z;

				double d = Math.sqrt(x_diff * x_diff + y_diff * y_diff + z_diff * z_diff);

				distanceSum += d;
			}
			radii[shell] = distanceSum / points.length;
		}

		double resultUnscaled_x = resultScaled_x / x_spacing;
		double resultUnscaled_y = resultScaled_y / y_spacing;
		double resultUnscaled_z = resultScaled_z / z_spacing;

		double result_x = (int) resultUnscaled_x;
		double result_y = (int) resultUnscaled_y;
		double result_z = (int) resultUnscaled_z;

		ShellsCanvas testCanvas = new ShellsCanvas(imp, resultScaled_x, resultScaled_y, resultScaled_z);
		for (int i = 0; i < radii.length; ++i) {
			testCanvas.addRadius(radii[i]);
		}

		new StackWindow(imp, testCanvas);
		imp.show();

		// Now load the heatmap showing overlap of all the labels...
		String heatmapPath = "/Users/mark/central-complex-complete-vib-protocol/fb-average.tif";
		ImagePlus[] heatmapChannels = BatchOpener.open(heatmapPath);
		if (heatmapChannels == null) {
			IJ.error("Couldn't open: " + heatmapPath);
			return;
		}

		boolean [][] include = new boolean[depth][];
		byte [][] excludedByAngle = new byte[depth][width*height];

		double maxDistanceSquared = 0;
		double minDistanceSquared = Double.MAX_VALUE;

		ImagePlus heatmap = heatmapChannels[0];
		ImageStack heatmapStack = heatmap.getStack();
		int includeOverAndIncluding = 127;
		for (int z = 0; z < depth; ++z) {
			byte[] pixels = (byte[]) heatmapStack.getPixels(z+1);
			boolean[] includeSlice = new boolean[width * height];
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {

					double x_real = x * x_spacing;
					double y_real = y * y_spacing;
					double z_real = z * z_spacing;
					double x_diff = x_real - resultScaled_x;
					double y_diff = y_real - resultScaled_y;
					double z_diff = z_real - resultScaled_z;

					double angle = Math.atan( z_diff / -x_diff );
					/*
					if( z == 45 )
						System.out.println("in z slice: "+z+" got angle: "+angle);
					 */
					if( angle > maxAngle || x >= resultUnscaled_x ) {
						excludedByAngle[z][y*width+x] = (byte)255;
						continue;
					}
					if( angle < minAngle || x >= resultUnscaled_x ) {
						excludedByAngle[z][y*width+x] = (byte)255;
						continue;
					}

					int value = pixels[y * width + x] & 0xFF;
					boolean includeThisOne = (value >= includeOverAndIncluding);
					includeSlice[y * width + x] = includeThisOne;

					if( ! includeThisOne ) {
						excludedByAngle[z][y*width+x] = (byte)128;
					    continue;
					}

					double distanceSquared = x_diff * x_diff + y_diff * y_diff + z_diff * z_diff;

					if(distanceSquared > maxDistanceSquared)
					    maxDistanceSquared = distanceSquared;
					else if(distanceSquared < minDistanceSquared)
					    minDistanceSquared = distanceSquared;
				}
			}
			include[z] = includeSlice;
		}

		heatmap.close();

		ImageStack newStack=new ImageStack(width,height);
		for( int z = 0; z < depth; ++z ) {
			ByteProcessor bp = new ByteProcessor(width,height);
			bp.setPixels(excludedByAngle[z]);
			newStack.addSlice("", bp);
		}
		ImagePlus newImagePlus=new ImagePlus("points excluded by angle",newStack );
		newImagePlus.show();

		if(false)
			return;

		boolean rescaleValues = false;

		double maxDistance = Math.sqrt(maxDistanceSquared);
		double minDistance = Math.sqrt(minDistanceSquared);

		System.out.println("All distances between: "+maxDistance+" and "+minDistance);

		/* Load the annotation file with scores for each one... */

		String annotationScoresFilename = "/Users/mark/thesis/annotations-with-scores.csv";

		Hashtable<String,Integer> scoresHash = new Hashtable<String,Integer>();

		System.out.println("Trying to load: "+annotationScoresFilename);

		try {

			BufferedReader f = new BufferedReader(
				new FileReader(annotationScoresFilename));
			String line;
			int lineNumber = 0;
			while ((line=f.readLine())!=null) {
				++ lineNumber;
				if( lineNumber == 1 )
					continue;
				// There are some commas in the fields after
				// the ones we care about here, so this  isn't
				// good enough in general, just good enough here.
				String [] fieldsSortOf = line.split(",");
				String fileName = fieldsSortOf[2];
				String scoreAsString = fieldsSortOf[3];
				try {
					int scoreInt = Integer.parseInt(scoreAsString);
					int lastDotIndex = fileName.lastIndexOf(".");
					if( lastDotIndex > 0 ) {
						fileName = fileName.substring(0, lastDotIndex);
					}
					System.out.println("got score "+scoreAsString+" for file "+fileName);
					scoresHash.put(fileName,new Integer(scoreInt));

				} catch( NumberFormatException e ) {
					System.out.println("The score '"+scoreAsString+"' wasn't an integer - skipping that one.");
				}

			}

		} catch( IOException e ) {
			IJ.error("Error parsing the file "+annotationScoresFilename+": "+e);
			return;
		}

		File filesDirectory = new File("/Users/mark/central-complex-complete-vib-protocol/warped_2");
		File nc82FilesDirectory = new File("/Users/mark/central-complex-complete-vib-protocol/warped_1");

		Pattern[] linesPatterns = new Pattern[6];
		linesPatterns[0] = Pattern.compile("71y");
		linesPatterns[1] = Pattern.compile("210y");
		linesPatterns[2] = Pattern.compile("c0*5");
		linesPatterns[3] = Pattern.compile("c0*61");
		linesPatterns[4] = Pattern.compile("760");
		linesPatterns[5] = Pattern.compile("(876|924)");

		int bins = 100;

		/* Not implemented yet... */
		FileGroup [] fileGroupArray = new FileGroup[linesPatterns.length+1];
		for( int i = 0; i < fileGroupArray.length; ++i )
			fileGroupArray[i] = new FileGroup("group "+i);

		// Make the last time through be the nc82 channels on their own...
		Bins[] lineBins = new Bins[linesPatterns.length+1];

		int totalGoodBrains = 0;

		// for (int i = 0; i <= linesPatterns.length; ++i) {
		for (int i = 0; i <= linesPatterns.length; ++i) {

			lineBins[i] = new Bins(bins, minDistance, maxDistance);

			Pattern pattern;
			if( i == linesPatterns.length )
				pattern = Pattern.compile("^[a-zA-Z0-9][^/]+$");
			else
				pattern = linesPatterns[i];

			System.out.println("Finding files matching: /" + pattern+"/");

			File[] files;

			if( i == linesPatterns.length )
				files = nc82FilesDirectory.listFiles();
			else
				files = filesDirectory.listFiles();

			for (int j = 0; j < files.length; ++j) {

				File f = files[j];
				if (!pattern.matcher(f.getName()).find()) {
					continue;
				}

				// Check the score to see if we keep it.

				String nameWithoutExtension = f.getName();
				int lastDotIndex = nameWithoutExtension.lastIndexOf(".");
				if( lastDotIndex >= 0 )
					nameWithoutExtension = nameWithoutExtension.substring(0,lastDotIndex);
				Integer scoreInteger=scoresHash.get(nameWithoutExtension);
				if( scoreInteger != null ) {
					int score = scoreInteger.intValue();
					System.out.println("... that had score "+score);
					if( score < 6 )
						continue;
				}

				fileGroupArray[i].add(f);
				++ totalGoodBrains;


			}
		}

		File [] normalizedDirectories = new File[linesPatterns.length+1];

		for( int i = 0; i <= linesPatterns.length; ++i ) {

			String normalizedDirectory = normalizedRootDirectory +
				( i == linesPatterns.length ? "nc82" : linesPatterns[i].toString() ) +
				File.separator;
			normalizedDirectories[i] = new File(normalizedDirectory);
			System.out.println("Going to create: "+normalizedDirectory);
			boolean result = normalizedDirectories[i].mkdir();
			System.out.println("   result was: "+result);

			// Now actually normalize into those directories:

			Quantile_Based_Normalization qbn = new Quantile_Based_Normalization();
			qbn.processToDirectory(fileGroupArray[i],
					       normalizedDirectories[i].getAbsolutePath(),
					       false,
					       heatmapPath,
					       0,
					       256,
					       false,
					       false);
		}

		int brainCounter = 0;
		Bins [] brainBins = new Bins[totalGoodBrains];
		String [] brainLines = new String[totalGoodBrains];
		String [] brainNames = new String[totalGoodBrains];

		for (int i = 0; i <= linesPatterns.length; ++i) {

			File directory=normalizedDirectories[i];

			File[] files=directory.listFiles();

			for (int j = 0; j < files.length; ++j) {

				File f = files[j];

				System.out.println("looking at values in: "+f);

				ImagePlus[] gal4Channels = BatchOpener.open(f.getAbsolutePath());
				if (gal4Channels == null) {
					IJ.error("Couldn't open file: " + f);
					return;
				}

				ImagePlus gal4Channel = gal4Channels[0];
				ImageStack gal4Stack = gal4Channel.getStack();

				int maxValue = -1;
				if( rescaleValues ) {
				    maxValue = maxValueInImage(gal4Channel, include);
				    System.out.println("Rescaling with old maxValue: "+maxValue);
				}

				for (int z = 0; z < depth; ++z) {
					byte[] pixels = (byte[]) gal4Stack.getPixels(z+1);
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							if (!include[z][y * width + x]) {
								continue;
							}
							double x_real = x * x_spacing;
							double y_real = y * y_spacing;
							double z_real = z * z_spacing;
							double x_diff = x_real - resultScaled_x;
							double y_diff = y_real - resultScaled_y;
							double z_diff = z_real - resultScaled_z;
							double distance = Math.sqrt(x_diff * x_diff + y_diff * y_diff + z_diff * z_diff);
							int value = pixels[y * width + x] & 255;
							if( rescaleValues ) {
							    value = (255 * value) / maxValue;
							}
							lineBins[i].add(value,distance);

							brainBins[brainCounter].add(value,distance);
							brainLines[brainCounter] = directory.getName();
							brainNames[brainCounter] = f.getName();
						}
					}
				}
				gal4Channel.close();

				++ brainCounter;
			}
		}


		String outputPathStem = "/Users/mark/central-complex-complete-vib-protocol/mhl-output/";

		try {

			for( int i=0; i <= linesPatterns.length; ++i ) {

				String outputPath;
				if( i == linesPatterns.length )
					outputPath = outputPathStem+"nc82.tsv";
				else
					outputPath = outputPathStem+linesPatterns[i]+".tsv";

				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputPath))));

				pw.println("Pattern\t" +
					   "Bin\t" +
					   "MinimumInBin\t" +
					   "MaximumInBin\t" +
					   "MidPointOfBin\t" +
					   "MeanValueInBin\t" +
					   "SDOfValuesInBin\t +" +
					   "N");

				String lineName;
				if( i == linesPatterns.length )
					lineName = "nc82";
				else
					lineName = linesPatterns[i].toString();

				Bins lineBin = lineBins[i];

				for( int b = 0; b < bins; ++b ) {
					pw.println("\"" + lineName +"\"\t" +
						   b + "\t" +
						   lineBin.getMinimumInBin(b) + "\t" +
						   lineBin.getMaximumInBin(b) + "\t" +
						   lineBin.getMidPointOfBin(b) + "\t" +
						   lineBin.getMean(b) + "\t" +
						   lineBin.getStandardDeviation(b) + "\t" +
						   lineBin.getN(b) );
				}


				pw.close();

			}

			File perBrainSubdirectory = new File(outputPathStem + "per-brain");
			perBrainSubdirectory.mkdir();

			for( int i=0; i <= brainBins.length; ++i ) {

				String outputPath = perBrainSubdirectory.getAbsolutePath() + File.separator + brainNames[i] + ".tsv";

				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputPath))));

				pw.println("Pattern\t" +
					   "Bin\t" +
					   "MinimumInBin\t" +
					   "MaximumInBin\t" +
					   "MidPointOfBin\t" +
					   "MeanValueInBin\t" +
					   "SDOfValuesInBin\t +" +
					   "N");

				Bins brainBin = brainBins[i];

				for( int b = 0; b < bins; ++b ) {
					pw.println("\"" + brainLines[i] +"\"\t" +
						   b + "\t" +
						   brainBin.getMinimumInBin(b) + "\t" +
						   brainBin.getMaximumInBin(b) + "\t" +
						   brainBin.getMidPointOfBin(b) + "\t" +
						   brainBin.getMean(b) + "\t" +
						   brainBin.getStandardDeviation(b) + "\t" +
						   brainBin.getN(b) );
				}
				pw.close();
			}

		} catch (IOException e) {
			IJ.error("There was an exception while writing the data: " + e);
			return;
		}

		// FIXME: also generate averaged images...




	}

	private int maxValueInImage( ImagePlus imp, boolean [][] includedVoxels ) {

		int maxValue = 0;

		ImageStack stack = imp.getStack();
		int width=imp.getWidth();
		int height=imp.getHeight();
		int depth=imp.getStackSize();
		for (int z = 0; z < depth; ++z) {
			byte[] pixels = (byte[]) stack.getPixels(z+1);
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if (!includedVoxels[z][y * width + x]) {
						continue;
					}
					int value = pixels[y*width+x] & 0xFF;
					if( value > maxValue )
					    maxValue = value;
				}
			}
		}
		return maxValue;
	}


	class SphereAttempt implements MultivariateFunction {

		ArrayList<ArrayList<double[]>> pointsInShells;

		SphereAttempt(int shells) {
			pointsInShells = new ArrayList<ArrayList<double[]>>();
			for (int i = 0; i < shells; ++i) {
				pointsInShells.add(new ArrayList<double[]>());
			}
		}

		public double evaluate(double[] argument) {

			double variancesSummed = 0;

			for (int shell = 0; shell < pointsInShells.size(); ++shell) {

				ArrayList<double[]> points = pointsInShells.get(shell);

				double sumDSquared = 0;
				double sumD = 0;
				int n = points.size();

				double centre_x = argument[0];
				double centre_y = argument[1];
				double centre_z = argument[2];

				for (Iterator<double[]> i = points.iterator(); i.hasNext();) {

					double[] p = i.next();

					double xdiff = p[0] - centre_x;
					double ydiff = p[1] - centre_y;
					double zdiff = p[2] - centre_z;

					double distanceSquared = xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;

					double distance = Math.sqrt(distanceSquared);

					sumDSquared += distanceSquared;
					sumD += distance;
				}

				double variance = (sumDSquared / n) - ((sumD * sumD) / (n * n));

				variancesSummed += variance;
			}

			return variancesSummed;
		}

		public int getNumArguments() {
			return 3;
		}

		public double getLowerBound(int n) {

			// FIXME: for argument n, what's the lower bound?
			// Should actually be related to the dimensions
			// of the image.
			return 0;
		}

		public double getUpperBound(int n) {

			// FIXME: ditto above
			return 10000;
		}

		public void addPointInShell(int shell, double x, double y, double z) {
			if (shell < 0 || shell >= pointsInShells.size()) {
				IJ.error("There are only " + shell + " (trying to insert into shell " + shell + ")");
				return;
			}
			double[] newPoint = new double[3];
			newPoint[0] = x;
			newPoint[1] = y;
			newPoint[2] = z;
			pointsInShells.get(shell).add(newPoint);
		}

	}
}

class ShellsCanvas extends ImageCanvas {

	private ImagePlus imagePlus;

	public ShellsCanvas(ImagePlus imagePlus, double centre_x, double centre_y, double centre_z) {
		super(imagePlus);
		this.imagePlus = imagePlus;
		this.centre_x = centre_x;
		this.centre_y = centre_y;
		this.centre_z = centre_z;
		System.out.println("Creating canvas with shells centred around: " + centre_x + "," + centre_y + "," + centre_z);
		radii = new ArrayList<Double>();
		Calibration c = imagePlus.getCalibration();
		x_spacing = c.pixelWidth;
		y_spacing = c.pixelHeight;
		z_spacing = c.pixelDepth;
		width = imagePlus.getWidth();
		height = imagePlus.getHeight();
		depth = imagePlus.getStackSize();
	}
	private double x_spacing;
	private double y_spacing;
	private double z_spacing;
	private double centre_x;
	private double centre_y;
	private double centre_z;
	private int width;
	private int height;
	private int depth;
	private ArrayList<Double> radii;

	public void addRadius(double radius) {
		System.out.println("Added radius: " + radius);
		radii.add(new Double(radius));
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		drawOverlay(g);
	}

	public void drawOverlay(Graphics g) {
		g.setColor(Color.RED);
		int real_side = (int) Math.ceil(getMagnification());
		int z = imp.getCurrentSlice() - 1;
		for (int shell = 0; shell < radii.size(); ++shell) {

			double radius = radii.get(shell);

			double real_z = z_spacing * z;
			double z_diff = real_z - centre_z;

			double a = radius * radius - z_diff * z_diff;
			double a_root = Math.sqrt(a);

			double x_min = (centre_x - a_root) / x_spacing;
			double x_max = (centre_x + a_root) / x_spacing;

			double y_min = (centre_y - a_root) / y_spacing;
			double y_max = (centre_y + a_root) / y_spacing;

			int x_min_screen = screenX((int) Math.round(x_min));
			int x_max_screen = screenX((int) Math.round(x_max));

			int y_min_screen = screenY((int) Math.round(y_min));
			int y_max_screen = screenY((int) Math.round(y_max));

			g.drawOval(x_min_screen, y_min_screen, x_max_screen - x_min_screen, y_max_screen - y_min_screen);
		}
	}
}

class Bins {

	int bins;
	double minDistance;
	double maxDistance;
	double distanceRange;
	double[] sum;
	double[] sumSquared;
	int[] n;

	public Bins(int bins, double minDistance, double maxDistance) {

		this.bins = bins;
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
		this.distanceRange = maxDistance - minDistance;

		sum = new double[bins];
		sumSquared = new double[bins];
		n = new int[bins];
	}

	public void add(int value, double distance) {

		if( value > 255 )
		    System.out.println("Warning, adding value > 255: "+value);

		double binDouble = (bins * (distance - minDistance)) / distanceRange;

		// We basically take the floor of this, cast it to an int and that
		// gives us the bin index.  The exception is that if distance is
		// maxDistance the we could index off the end, so check for that.
		int bin = (int) Math.floor(binDouble);

		if (bin >= bins) {
			bin = bins - 1;
		}
		n[bin] += 1;
		sum[bin] += value;
		sumSquared[bin] += value * value;
	}

	public int numberOfBins() {
		return bins;
	}

	public double getMean(int bin) {
		return sum[bin] / n[bin];
	}

	public double getVariance(int bin) {
		return (sumSquared[bin] / n[bin]) - (sum[bin] * sum[bin]) / (n[bin] * n[bin]);
	}

	public double getStandardDeviation(int bin) {
		return Math.sqrt(getVariance(bin));
	}

	public double getMinimumInBin(int bin) {
	    return (bin * distanceRange) / bins;
	}

	public double getMaximumInBin(int bin) {
	    return ((bin + 1) * distanceRange) / bins;
	}

	public double getMidPointOfBin(int bin) {
	    return (getMaximumInBin(bin) + getMinimumInBin(bin)) / 2;
	}

	public int getN(int b) {
	    return n[b];
	}
}
