//=====================================================================================
// File :       MIJ.java
// Project:     MIJ: Matlab to ImageJ interface
// URL:			http://bigwww.epfl.ch/sage/soft/mij/
//
// Fiji version (Jean-Yves Tinevez, July 2011)
// - Removed some Eclipse minor warnings
// - The createColor methods bahve like createImage: they have a boolean flag to
//   specify whether we display the image or not, and they return the ImagePlus they
//   created.
// - Fixed a bug in  createColor(String title, byte[][][][] is, boolean showImage) preventing
//   a stack of color images to be properly converted to ImagePlus. 
//
// Changes in version 1.3.6 (Daniel Sage, 21 December 2010)
// - Updated the documentation and the web page
//
// Changes in version 1.3.5 (Carlos Ortiz, 19 December 2010)
// - Added the functionality to specify both plugins and macro folder by passing a 
//   command-line argument to the ImageJ constructor.
// - Updated the use of deprecated getValue functions from the results table to use 
//   getValueAsDouble
// - Cleaned up the jar file and included a manifest file
// - Increased the vebosity of the verbose mode
// - To make ImageJ be able to compile and run plugins, added ImageJ/jre/lib/ext 
//   to the java path during the start function. This folder should contain either 
//   javac.jar or tools.jar as provided by ImageJ.
// - To increase the memory available to ImageJ, go to File/Preferences/JavaHeapSize 
//   and increase it. This works for R2010a but it's a new feature. 
//   With older matlabs, google for a way to set java.lang.Runtime.getRuntime.maxMemory 
//   by creating a java.opts file in your startup directory to set this value with 
//   a JVM flag.
// - Made MIJ.start and MIJ.exit work such that you can open and close MIJ and REOPEN it
//   without null exceptions by removing the use of instanceof (imagej remains an 
//   instance of ImageJ after the first pass even though the quit() function is called).
//   Instead, we use a boolean that gets set to true/false for each start/stop instance.
//   The only thing that doesn't work still is that when opening the second instance, 
//   the pluginsfolder commandline argument does not get passed correctly.
//  - For niceness, added ij.jar and mij.jar to
//    /usr/local/Mathworks/R2010a/toolbox/local/classpath.txt, this way, they are 
//   loaded with matlab.  Also, I could not get some plugins working without doing 
//   this. If you want to make changes that do not affect anyone else, copy 
//   classpath.txt to your own startup directory and edit the file there. 
//   When MATLAB starts up, it looks for classpath.txt first in your startup 
//   directory, and then in the default location. It uses the first file it finds.
//
// Changes in version 1.3.4 (Daniel Sage, 10 December 2010)
// - Added a createImage method with the possibility to show or show the image
//
//=====================================================================================

import java.awt.Rectangle;
import java.awt.Polygon;
import java.util.StringTokenizer;
import ij.IJ;
import ij.ImageJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;
import ij.plugin.filter.Analyzer;

/**
 * Matlab to ImageJ interface.
 *
 * @version 1.3.6 (21 December 2010).
 *
 * @author <ul type="square">
 *	<li>Daniel Sage, Biomedical Imaging Group (BIG), Ecole Polytechnique Federale de Lausanne (EPFL), Switzerland.</li>
 *  <li>Dimiter Prodanov, Catholic University of Louvain, Belgium.</li>
 *  <li>Carlos Ortiz, North Carolina State University, USA.</li>
 *  </ul>
 * 
 *
 * @see
 * <p>
 * <b>More information:</b> <a href="http://bigwww.epfl.ch/sage/soft/mij/">http://bigwww.epfl.ch/sage/soft/mij/</a>
 * </p>
 * <b>Important note for the installation:</b><br>
 * <p>To use this class, copy mij.jar to your ImageJ folder and tell matlab where the files are by:<br>
 * javaaddpath '<IJpath>\ij.jar'<br>
 * javaaddpath '<IJpath>\mij.jar'<br>
 * </p>
 * <p>
 * <b>This class was tested:</b>
 * <ul type="square">
 * <li>Matlab 7.10.0 (R2010a) on Ubuntu.</li>
 * <li>Matlab 7.11.0 (R2010b) on Mac OX 10.6.5.</li>
 * </ul>
 * <p>
 * <b>Conditions of use:</b><br>
 * You'll be free to use this software for research purposes, but you
 * should not redistribute it without our consent. In addition, we 
 * expect you to include a citation or acknowledgement whenever 
 * you present or publish results that are based on it.
 * </p>
 */

public class MIJ {

	public static ImageJ imagej;
	private static final  String version = "1.3.6-fiji";
	private static final int CAL	=	1;
	private static final int NOCAL	=	0;
	private static boolean verbose = true;

	/**
	 * Class constructor.
	 */
	public MIJ() {

	}

	/**
	 * Get the version of the MIJ class.
	 *
	 * @return	Returns the version number.
	 */
	public static String version() {
		return version;
	}

	/**
	 * Give a brief description of the methods.
	 *
	 * @return	Returns a brief description of the methods. 
	 */
	public static String help() {
		String str = "Supported methods for image manipulation in version " + version + "\n"+
				"createColor - exports RGB image \n"+
				"createImage - exports other images \n"+
				"exit -exists MIJ \n"+
				"getColumn - returns a specifying column the current instance of ResultsTable \n" +
				"getCurrentImage - returns a 2D array representing the current image \n"+
				"getCurrentTitle - imports the title of the current image \n"+
				"getHistogram - imports the histogram of the current image \n"+
				"getImage - returns a 2D array representing the image specified by the title\n"+
				"getListColumns - returns the list of columns currently used in the ResultsTable \n"+
				"getListImages - returns the list of opened images \n"+
				"getResultsTable - imports the ResultsTable \n"+
				"getRoi - imports the current ROI \n"+
				"help - give a brief description of the MIJ methods \n"+
				"run - runs command or macro \n" +
				"setColumn - exports contents to a column in the ResultsTable\n"+
				"setRoi - exports the current ROI \n"+
				"setThreshold - sets the threshods of the image \n"+
				"start - starts MIJ \n"+
				"version - return the MIJ version\n";
		return str;
	}

	/**
	 * Starts new instance of ImageJ from Matlab.
	 */
	public static void start() {
		start(true);
	}

	/**
	 * Starts new instance of ImageJ from Matlab with or without verbose mode.
	 *
	 * @param v indicate the verbose mode
	 */
	public static void start(boolean v) {
		verbose = v;
		launch(null);
	}

	/**
	 * Starts new instance of ImageJ specifying the plugins directory and macros directory.  
	 *
	 * @param IJpath	String that points to the folder containing ij.jar and plugins and macros folder
	 * @param verbose	indicate the verbose mode
	 */
	public static void start(String IJpath) {
		System.setProperty("plugins.dir", IJpath);
		verbose = true;
		setupExt(IJpath);
		launch(null);
	}

	/**
	 * Starts new instance of ImageJ specifying the plugins directory and macros directory.  
	 * For this to work correctly, DO NOT end homeDir with plugins and DO NOT 
	 * leave homeDir null. See setupPluginsAndMacrosPaths() in Menus.java and 
	 * note that Menus.updateImageJMenus() is the only public function that has 
	 * access to setupPluginsAndMacrosPaths().
	 *
	 * @param homeDir	Location of the user's homeDir E.g. /home/user (ImageJ preferences will be saved here. 
	 *                  It also means that if you already have preferences, opening ImageJ via matlab will use those preferences)
	 * @param IJpath	String that points to the folder containing ij.jar and plugins and macros folder
	 * @param verbose	indicate the verbose mode
	 */
	public static void start(String homeDir, String IJpath, boolean v) {
		System.setProperty("plugins.dir", IJpath);
		System.setProperty("user.dir", homeDir);
		System.setProperty("user.home", homeDir);
		verbose = v;
		setupExt(IJpath);
		launch(null);
	}

	/**
	 * Setup the IJPath.
	 * 
	 * @param IJpath String that points to the folder containing ij.jar and plugins and macros folder 
	 */
	public static void setupExt(String IJpath) {
		if(System.getProperty("java.imagej")==null) { //these need to be added for compile to work
			System.setProperty("java.ext.dirs", System.getProperty("java.ext.dirs")+":"+IJpath+"/jre/lib/ext");
			System.setProperty("java.imagej","set");
		}
	}

	/**
	 * Starts new instance of ImageJ specifying the command-line options .
	 *
	 * @param args	String of the same format as ImageJ commandline e.g. "-debug -ijpath /opt/ImageJ/plugins"
	 * @param IJpath	String that points to the folder containing ij.jar and plugins and macros folder
	 */
	public static void start(String args, String IJpath) {
		setupExt(IJpath);
		verbose = true;
		launch(args.split("\\s"));
	}

	/**
	 * Starts new instance of ImageJ from Matlab using command-line arguments
	 */
	private static void launch(String myargs[]) {
		if (verbose) {
			System.out.println("--------------------------------------------------------------");
			System.out.println("MIJ " + version + ": Matlab to ImageJ Interface");
			System.out.println("--------------------------------------------------------------");
			System.out.println("More Info: http://bigwww.epfl.ch/sage/soft/mij/");
			System.out.println("Help: MIJ.help");
			Runtime runtime = Runtime.getRuntime();
			System.out.println("JVM> " + version);
			System.out.println("JVM> Version: " + System.getProperty("java.version"));
			System.out.println("JVM> Total amount of memory: " + Math.round(runtime.totalMemory()/1024) + " Kb");
			System.out.println("JVM> Amount of free memory: " + Math.round(runtime.freeMemory()/1024) + " Kb");
		}

		if (imagej instanceof ImageJ) {
			if (verbose) {
				System.out.println("--------------------------------------------------------------");
				System.out.println("Status> ImageJ is already started.");
				System.out.println("--------------------------------------------------------------");
			}
			return;
		}

		/////////////////////////////////
		///////These are the important lines
		////////////////////////////////////
		imagej = new ImageJ();
		if( myargs!=null) {
			if (verbose) {
				System.out.println("ImageJ> Arguments:");
				for(int i =0; i < myargs.length ; i++)
					System.out.println(myargs[i]);
			}
			ImageJ.main(myargs);
		}
		///////////////////////////////////

		if (imagej instanceof ImageJ) {
			if (verbose) {
				System.out.println("ImageJ> Version:" + IJ.getVersion());
				System.out.println("ImageJ> Memory:" + IJ.freeMemory() );
				System.out.println("ImageJ> Directory plugins: " + (IJ.getDirectory("plugins")==null?"Not specified":IJ.getDirectory("plugins")));
				System.out.println("ImageJ> Directory macros: " + (IJ.getDirectory("macros")==null?"Not specified":IJ.getDirectory("macros")));
				System.out.println("ImageJ> Directory luts: " + (IJ.getDirectory("luts")==null?"Not specified":IJ.getDirectory("luts")));
				System.out.println("ImageJ> Directory image: " + (IJ.getDirectory("image")==null?"Not specified":IJ.getDirectory("image")));
				System.out.println("ImageJ> Directory imagej: " + (IJ.getDirectory("imagej")==null?"Not specified":IJ.getDirectory("imagej")));
				System.out.println("ImageJ> Directory startup: " + (IJ.getDirectory("startup")==null?"Not specified":IJ.getDirectory("startup")));
				System.out.println("ImageJ> Directory home: " + (IJ.getDirectory("home")==null?"Not specified":IJ.getDirectory("home")));
				System.out.println("--------------------------------------------------------------");
				System.out.println("Status> ImageJ is running.");
				System.out.println("--------------------------------------------------------------");
			}
			return;
		}
		if (verbose) {
			System.out.println("--------------------------------------------------------------");
			System.out.println("Status> ImageJ can not be started.");
			System.out.println("--------------------------------------------------------------");
		}
	}

	/**
	 * Starts new instance of ImageJ.
	 *
	 * @param args	Start up arguments for MIJ
	 */
	public static void main(String [] args) {
		try {
			MIJ.start(args[0],args[1]);
		} 
		catch (ArrayIndexOutOfBoundsException e) {	
			IJ.log(MIJ.help());
		}
	}

	/**
	 * Exits ImageJ.imagej instanceimagej instance
	 */
	public static void exit() {
		IJ.getInstance().quit();
		imagej = null;
		if (verbose && !(imagej instanceof ImageJ)) {
			System.out.println("ImageJ instance ended cleanly");
		}
	}

	/**
	 * Gives the list of the open images in the ImageJ instance.
	 *
	 * @return	List of open images
	 */
	public static String[] getListImages() {
		int[] is = WindowManager.getIDList();
		String[] strings = new String[is.length];
		for (int i = 0; i < is.length; i++) {
			ImagePlus imageplus = WindowManager.getImage(is[i]);
			strings[i] = imageplus.getTitle();
		}
		return strings;
	}

	/**
	 * Returns the title of the current image window.
	 *
	 * @return	Title of the current image window
	 */
	public static String getCurrentTitle() {
		ImagePlus imageplus = WindowManager.getCurrentImage();
		return imageplus.getTitle();
	}

	/**
	 * Set a region of interest (ROI) in the current image.
	 *
	 * @param	roiarray	give coordinates or positions of the ROI depending of the ROI type
	 * @param	type	supported types: Roi.LINE, Roi.RECTANGLE, Roi.POINT, Roi.OVAL, Roi.POLYLINE, Roi.POLYGON, Roi.ANGLE
	 */
	public static void setRoi(double[][] roiarray, int type) {
		ImagePlus imageplus = WindowManager.getCurrentImage();
		switch (type) {
		case Roi.LINE:
			Line roi=new Line((int) roiarray[0][0], (int) roiarray[1][0], 
					(int) roiarray[0][1], (int) roiarray[1][1]);
			imageplus.setRoi((Roi)roi);
			break;
		case Roi.RECTANGLE:
			int width= (int) roiarray[0][0]-(int) roiarray[0][1];
			int height= (int) roiarray[1][1]-(int) roiarray[1][2];
			IJ.log("w: "+width+ " h: "+height);
			Roi rect=new Roi((int) roiarray[0][0], (int) roiarray[1][0], Math.abs(width), Math.abs(height));
			imageplus.setRoi(rect);
			break;
		case Roi.POINT:
			PointRoi pnt=new PointRoi((int) roiarray[0][0], (int) roiarray[1][0], imageplus);
			imageplus.setRoi(pnt);
			break;
		case Roi.OVAL:
			width= (int) roiarray[0][0]-(int) roiarray[0][1];
			height= (int) roiarray[1][1]-(int) roiarray[1][2];
			int xc= (int) roiarray[0][0] + width/2;
			int yc= (int) roiarray[1][1] + height/2;
			IJ.log("w: "+width+ " h: "+height+ " xc: "+xc+" yc "+yc);
			OvalRoi oval=new OvalRoi( xc,  yc,  width,  height);
			imageplus.setRoi(oval);
			break;
		case Roi.POLYLINE:
			int nPoints=roiarray[0].length;
			int[] xarr=new int[nPoints];
			int[] yarr=new int[nPoints];
			for (int i=0;i<nPoints;i++) {
				xarr[i]=(int)roiarray[0][i];
				yarr[i]=(int)roiarray[1][i];
			}
			PolygonRoi poly=new PolygonRoi(xarr, yarr, nPoints, Roi.POLYLINE);
			imageplus.setRoi(poly);
			break;
		case Roi.POLYGON:
			nPoints=roiarray[0].length;
			xarr=new int[nPoints];
			yarr=new int[nPoints];
			for (int i=0;i<nPoints;i++) {
				xarr[i]=(int)roiarray[0][i];
				yarr[i]=(int)roiarray[1][i];
			}
			poly=new PolygonRoi(xarr, yarr, nPoints, Roi.POLYGON);
			imageplus.setRoi(poly);
			break;
		case Roi.ANGLE:
			break;
		default:
		}
	}

	/**
	 * Get a region of interest (ROI) of the current image with or without calibration.
	 *
	 * @param option	CAL for using calibration or NOCAL for no calibration
	 * @return Object
	 */
	public static Object getRoi(int option) {
		ImagePlus imageplus = WindowManager.getCurrentImage();
		Roi roi=imageplus.getRoi();
		Calibration cal=imageplus.getCalibration();
		double fh=cal.pixelHeight;
		double fw=cal.pixelWidth;
		Object ret=null;
		if (roi.isLine()) {
			Rectangle rect=roi.getBounds();
			double x=rect.getX();
			double y=rect.getY();
			double w=rect.getWidth();
			double h=rect.getHeight();
			if (option==NOCAL){
				double [][] pnts= {{x,x+w,x+w,x},{y,y,y+h,y+h}};
				ret=(Object)pnts;
			}
			if (option==CAL){
				double [][] pnts= {{x*fw,(x+w)*fw,(x+w),x*fw},
						{y*fh,y*fh,(y+h)*fh,(y+h)*fh}};
				ret=(Object)pnts; 
			}
		}
		else {
			Polygon  polygon=roi.getPolygon();
			if (option==NOCAL){
				int[][] pnts=new int[2][polygon.npoints];
				pnts[0]=polygon.xpoints;
				pnts[1]=polygon.ypoints;
				ret=(Object)pnts;
			}
			if (option==CAL){
				double [][] pnts=new double[2][polygon.npoints];
				for (int i=0;i<polygon.npoints; i++){
					pnts[0][i]=polygon.xpoints[i]*fw;
					pnts[1][i]=polygon.ypoints[i]*fh;
				}
				ret=(Object)pnts;
			}
		}
		return ret;
	}

	/**
	 * Returns the current (selected) image from ImageJ.
	 *
	 * @return	Current image
	 */
	public static Object getCurrentImage() {
		ImagePlus imageplus = WindowManager.getCurrentImage();
		if (imageplus == null)
			return null;
		return get(imageplus);
	}

	/**
	 * Returns the specifying image from ImageJ.
	 *
	 * @param title	title of image
	 * @return Object
	 */
	public static Object getImage(String title) {
		String[] strings = getListImages();
		int[] is = WindowManager.getIDList();
		for (int i = 0; i < is.length; i++) {
			if (strings[i].equals(title)) {
				ImagePlus imageplus = WindowManager.getImage(is[i]);
				return get(imageplus);
			}
		}
		System.out.println("MIJ Error message: the requested image (" + title + ") does not exist.");
		return null;
	}


	/**
	 * Returns the histogram of the current image.
	 *
	 * @return	histogram
	 */
	public static Object getHistogram() {
		ImagePlus imageplus = WindowManager.getCurrentImage();
		if (imageplus == null)
			return null;
		return imageplus.getProcessor().getHistogram();
	}

	/**
	 * Returns the list of columns currently used in the ResultsTable.
	 *
	 * @return list of columns
	 */
	public static String[] getListColumns() {
		ResultsTable rt = Analyzer.getResultsTable();
		StringTokenizer st = new StringTokenizer(rt.getColumnHeadings());
		int n = st.countTokens();
		String[] strings = new String[n];
		for (int i = 0; i < n; i++) {
			strings[i] =st.nextToken();
		}
		return strings;
	}

	/**
	 * Returns the instance of the ResultsTable.
	 *
	 * @return	Instance of the ResultsTable
	 */
	public static Object getResultsTable(){
		ResultsTable rt=Analyzer.getResultsTable();
		int col=0;
		int[] index=new int[ResultsTable.MAX_COLUMNS];
		for  (int cnt=0;cnt<ResultsTable.MAX_COLUMNS; cnt++) {
			if (rt.columnExists(cnt)){
				index[col]=cnt;
				col++;
			}
		}
		int counter=rt.getCounter();
		double [][] results=new double[counter][col];
		for( int i=0;i<col;i++) {
			for( int j=0;j<counter;j++) {
				results[j][i]=rt.getValueAsDouble(index[i],j);
			}
		}
		return results;
	}

	/**
	 * Returns a specifying column the current instance of ResultsTable.
	 *
	 * @param heading	heading of a column
	 * @return column specified by its heading
	 */
	public static Object getColumn(String heading){
		ResultsTable rt=Analyzer.getResultsTable();
		int col= rt.getColumnIndex(heading);
		int counter=rt.getCounter();
		double []results=new double[counter];

		results=rt.getColumnAsDoubles(col);
		return results;
	}

	/**
	 * Set a specifying column into the current instance ResultsTable.
	 *
	 * @param heading	heading of a column	
	 * @param object
	 */
	public static void setColumn(String heading, Object object){
		ResultsTable rt=Analyzer.getResultsTable();

		int col= rt.getColumnIndex(heading);
		if (col==ResultsTable.COLUMN_NOT_FOUND) 
			col=rt.getFreeColumn(heading);
		int cc=rt.getCounter();
		if (object instanceof double[]) {
			double[] values = (double[]) object;
			for (int i=0; i<values.length; i++){
				if (cc<=i) rt.incrementCounter();
				rt.setValue(col, i, values[i]);
			}
		}
	}

	/**
	 * Get an image.
	 *
	 * @param imageplus	image
	 * @return an N x M array representing the input image
	 */
	private static Object get(ImagePlus imageplus) {
		if (imageplus == null)
			return null;
		int width = imageplus.getWidth();
		int height = imageplus.getHeight();
		int stackSize = imageplus.getStackSize();
		int counter = 0;
		ImageStack imagestack = imageplus.getStack();
		switch (imageplus.getType()) {

		case ImagePlus.COLOR_256: {
			;
		}
		case ImagePlus.GRAY8: {
			short[][][] is = new short[height][width][stackSize];
			for (int sz = 0; sz < stackSize; sz++) {
				ByteProcessor byteprocessor = (ByteProcessor) imagestack.getProcessor(sz + 1);
				byte[] pixels = (byte[]) byteprocessor.getPixels();
				counter = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						is[h][w][sz] = (short)(pixels[counter]&0xff);
						w++;
						counter++;
					}
					counter = ++h * width;
				}
			}
			return is;
		}
		case ImagePlus.GRAY16: {
			int[][][] is = new int[height][width][stackSize];
			for (int sz = 0; sz < stackSize; sz++) {
				counter = 0;
				ShortProcessor shortprocessor = (ShortProcessor) imagestack.getProcessor(sz + 1);
				short[] spixels = (short[]) shortprocessor.getPixels();
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						is[h][w][sz] = (int)(spixels[counter]&0xffff);
						w++;
						counter++;
					}
					counter = ++h * width;
				}
			}
			return is;
		}
		case ImagePlus.GRAY32: {
			double[][][] fs = new double[height][width][stackSize];
			for (int sz = 0; sz < stackSize; sz++) {
				FloatProcessor floatprocessor = (FloatProcessor) imagestack.getProcessor(sz + 1);
				float[] fpixels = (float[]) floatprocessor.getPixels();
				counter = 0;
				int i = 0;
				while (i < height) {
					int j = 0;
					while (j < width) {
						fs[i][j][sz] = (double) fpixels[counter];
						j++;
						counter++;
					}
					counter = ++i * width;
				}
			}
			return fs;
		}
		case ImagePlus.COLOR_RGB: {
			if (stackSize == 1) {
				short[][][] is = new short[height][width][3];
				ColorProcessor colorprocessor = (ColorProcessor) imagestack.getProcessor(1);
				byte[] red = new byte[width * height];
				byte[] green = new byte[width * height];
				byte[] blue = new byte[width * height];
				colorprocessor.getRGB(red, green, blue);
				counter = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						is[h][w][0] = (short)(red[counter]&0xff);
						is[h][w][1] = (short)(green[counter]&0xff);
						is[h][w][2] = (short)(blue[counter]&0xff);
						w++;
						counter++;
					}
					counter = ++h * width;
				}
				return is;
			}
			short[][][][] is = new short[height][width][stackSize][3];
			for (int sz = 0; sz < stackSize; sz++) {
				ColorProcessor colorprocessor  = (ColorProcessor) imagestack.getProcessor(sz + 1);
				byte[] red = new byte[width * height];
				byte[] green = new byte[width * height];
				byte[] blue = new byte[width * height];
				colorprocessor.getRGB(red, green, blue);
				counter = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						is[h][w][sz][0] = (short)red[counter];
						is[h][w][sz][1] = (short)green[counter];
						is[h][w][sz][2] = (short)blue[counter];
						w++;
						counter++;
					}
					counter = ++h * width;
				}
			}
			return is;
		}
		default:
			System.out.println("MIJ Error message: Unknow type of volumes.");
			return null;
		}
	}

	/**
	 * Create a new image in ImageJ from a Matlab variable.
	 *
	 * This method try to create a image (ImagePlus of ImageJ) from a Matlab's variable
	 * which should be an 2D or 3D array
	 * The recognize type are byte, short, int, float and double. The dimensionality of
	 * the 2 (image) or 3 (stack of images)
	 *
	 * @param object	Matlab variable
	 */
	public static void createImage(Object object) {
		createImage("Import from Matlab", object, true);
	}

	/**
	 * Create a new image in ImageJ from a Matlab variable with a specified title.
	 *
	 * This method try to create a image (ImagePlus of ImageJ) from a Matlab's variable
	 * which should be an 2D or 3D array
	 * The recognize type are byte, short, int, float and double. The dimensionality of
	 * the 2 (image) or 3 (stack of images)
	 *
	 * @param title			title of the new image	
	 * @param object		Matlab variable
	 * @param showImage     Whether to display the newly created image or not
	 * @return the resulting ImagePlus instance
	 */
	public static ImagePlus createImage(String title, Object object, boolean showImage) {
		ImagePlus imp = null;
		int i = 0;
		if (object instanceof byte[][]) {
			byte[][] is = (byte[][]) object;
			int height = is.length;
			int width = is[0].length;
			ByteProcessor byteprocessor = new ByteProcessor(width, height);
			byte[] bp = (byte[]) byteprocessor.getPixels();
			int h = 0;
			while (h < height) {
				int w = 0;
				while (w < width) {
					bp[i] = is[h][w];
					w++;
					i++;
				}
				i = ++h * width;
			}
			imp = new ImagePlus(title, byteprocessor);

		} 
		else if (object instanceof short[][]) {
			short[][] is = (short[][]) object;
			int height = is.length;
			int width = is[0].length;
			ShortProcessor shortprocessor = new ShortProcessor(width, height);
			short[] sp = (short[]) shortprocessor.getPixels();
			int h = 0;
			while (h < height) {
				int w = 0;
				while (w < width) {
					sp[i] = is[h][w];
					w++;
					i++;
				}
				i = ++h * width;
			}
			imp = new ImagePlus(title, shortprocessor);

		} 
		else if (object instanceof int[][]) {
			if (verbose)
				System.out.println("MIJ warning message: Loss of precision: convert int 32-bit to short 16-bit");
			int[][] is = (int[][]) object;
			int height = is.length;
			int width = is[0].length;
			ShortProcessor shortprocessor = new ShortProcessor(width, height);
			short[] sp = (short[]) shortprocessor.getPixels();
			int h = 0;
			while (h < height) {
				int w = 0;
				while (w < width) {
					sp[i] = (short)is[h][w];
					w++;
					i++;
				}
				i = ++h * width;
			}
			imp = new ImagePlus(title, shortprocessor);
		} 
		else if (object instanceof float[][]) {
			float[][] fs = (float[][]) object;
			int height = fs.length;
			int width = fs[0].length;
			FloatProcessor floatprocessor = new FloatProcessor(width, height);
			float[] fp = (float[])floatprocessor.getPixels();
			int h = 0;
			while (h < height) {
				int w = 0;
				while (w < width) {
					fp[i] = fs[h][w];
					w++;
					i++;
				}
				i = ++h * width;
			}
			floatprocessor.resetMinAndMax();
			imp = new ImagePlus(title, floatprocessor);

		} 
		else if (object instanceof double[][]) {
			if (verbose)
				System.out.println("MIJ warning message: Loss of precision: convert double 32-bit to float 32-bit");
			double[][] ds = (double[][]) object;
			int height = ds.length;
			int width = ds[0].length;
			FloatProcessor floatprocessor = new FloatProcessor(width, height);
			float[] fp = (float[]) floatprocessor.getPixels();
			int h = 0;
			while (h < height) {
				int w = 0;
				while (w < width) {
					fp[i] = (float) ds[h][w];
					w++;
					i++;
				}
				i = ++h * width;
			}
			floatprocessor.resetMinAndMax();
			imp = new ImagePlus(title, floatprocessor);

		} 
		else if (object instanceof byte[][][]) {
			byte[][][] is = (byte[][][]) object;
			int height = is.length;
			int width = is[0].length;
			int stackSize = is[0][0].length;
			ImageStack imagestack = new ImageStack(width, height);
			for (int sz = 0; sz < stackSize; sz++) {
				ByteProcessor byteprocessor = new ByteProcessor(width, height);
				byte[] bp = (byte[]) byteprocessor.getPixels();
				i = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						bp[i] = is[h][w][sz];
						w++;
						i++;
					}
					i = ++h * width;
				}
				imagestack.addSlice("", byteprocessor);
			}
			imp = new ImagePlus(title, imagestack);

		} 
		else if (object instanceof short[][][]) {
			short[][][] is = (short[][][]) object;
			int height = is.length;
			int width = is[0].length;
			int stackSize = is[0][0].length;
			ImageStack imagestack = new ImageStack(width, height);
			for (int sz = 0; sz < stackSize; sz++) {
				ShortProcessor shortprocessor  = new ShortProcessor(width, height);
				short[] sp = (short[]) shortprocessor.getPixels();
				i = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						sp[i] = is[h][w][sz];
						w++;
						i++;
					}
					i = ++h * width;
				}
				imagestack.addSlice("", shortprocessor);
			}
			imp = new ImagePlus(title, imagestack);

		} 
		else if (object instanceof int[][][]) {
			if (verbose)
				System.out.println("MIJ warning message: Loss of precision: convert int 32 bits to short 16 bits");
			int[][][] is = (int[][][]) object;
			int height = is.length;
			int width = is[0].length;
			int stackSize = is[0][0].length;
			ImageStack imagestack = new ImageStack(width, height);
			for (int sz = 0; sz < stackSize; sz++) {
				ShortProcessor shortprocessor  = new ShortProcessor(width, height);
				short[] sp = (short[]) shortprocessor.getPixels();
				i = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						sp[i] = (short) is[h][w][sz];
						w++;
						i++;
					}
					i = ++h * width;
				}
				if (sz == 0)
					shortprocessor.resetMinAndMax();
				imagestack.addSlice("", shortprocessor);

			}
			imp = new ImagePlus(title, imagestack);

		} 
		else if (object instanceof float[][][]) {
			float[][][] fs = (float[][][]) object;
			int height = fs.length;
			int width = fs[0].length;
			int stackSize = fs[0][0].length;
			ImageStack imagestack = new ImageStack(width, height);
			for (int sz = 0; sz < stackSize; sz++) {
				FloatProcessor floatprocessor = new FloatProcessor(width, height);
				float[] fp = (float[]) floatprocessor.getPixels();
				i = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						fp[i] = fs[h][w][sz];
						w++;
						i++;
					}
					i = ++h * width;
				}
				if (sz == 0)
					floatprocessor.resetMinAndMax();
				imagestack.addSlice("", floatprocessor);
			}
			imp=new ImagePlus(title, imagestack);

		} 
		else if (object instanceof double[][][]) {
			if (verbose)
				System.out.println("MIJ warning message: Loss of precision: convert double 32-bit to float 32-bit");
			double[][][] ds = (double[][][]) object;
			int height = ds.length;
			int width = ds[0].length;
			int stackSize = ds[0][0].length;
			ImageStack imagestack = new ImageStack(width, height);
			for (int sz = 0; sz < stackSize; sz++) {
				FloatProcessor floatprocessor = new FloatProcessor(width, height);
				float[] fp = (float[]) floatprocessor.getPixels();
				i = 0;
				int h = 0;
				while (h < height) {
					int w = 0;
					while (w < width) {
						fp[i] = (float) ds[h][w][sz];
						w++;
						i++;
					}
					i = ++h * width;
				}
				if (sz == 0)
					floatprocessor.resetMinAndMax();
				imagestack.addSlice("", floatprocessor);
			}
			imp=new ImagePlus(title, imagestack);

		}
		else {
			System.out.println("MIJ Error message: Unknow type of images or volumes.");
			return null;
		}

		if (showImage) {
			imp.show();
			imp.updateAndDraw();
		}
		return imp;
	}

	/**
	 * Create a new color image in ImageJ from a Matlab variable.
	 *
	 * The last index of the array is the color channel index (3 channels) in
	 * the follwing order Red-Green-Blue.
	 *
	 * @param is	Matlab variable	
	 */
	public static ImagePlus createColor(final byte[][][] is, boolean showImage) {
		return createColor("Imported from Matlab", is, showImage);
	}

	/**
	 * Create a new color image in ImageJ from a Matlab variable with a specified title.
	 *
	 * @param title	title of the new image
	 * @param is	Matlab variable
	 */
	public static ImagePlus createColor(String title, final byte[][][] is, boolean showImage) {
		int height = is.length;
		int width = is[0].length;
		int stackSize = is[0][0].length;
		ColorProcessor colorprocessor = new ColorProcessor(width, height);
		byte[] R_pixels = new byte[width * height];
		byte[] G_pixels = new byte[width * height];
		byte[] B_pixels = new byte[width * height];
		if (stackSize >= 3) {
			for (int h = 0; h < height; h++) {
				int index = h * width;
				int w = 0;
				while (w < width) {
					R_pixels[index] = is[h][w][0];
					G_pixels[index] = is[h][w][1];
					B_pixels[index] = is[h][w][2];
					w++;
					index++;
				}
			}
		} 
		else if (stackSize >= 2) {
			for (int j = 0; j < height; j++) {
				int index = j * width;
				int i = 0;
				while (i < width) {
					R_pixels[index] = is[j][i][0];
					G_pixels[index] = is[j][i][1];
					i++;
					index++;
				}
			}
		} 
		else if (stackSize >= 1) {
			for (int j = 0; j < height; j++) {
				int index = j * width;
				int i = 0;
				while (i < width) {
					R_pixels[index] = is[j][i][0];
					i++;
					index++;
				}
			}
		}
		colorprocessor.setRGB(R_pixels, G_pixels, B_pixels);
		ImagePlus imp = new ImagePlus(title, colorprocessor);
		if (showImage) {
			imp.show();
		}
		return imp;
	}

	/**
	 * Create a new 3D color image in ImageJ from a Matlab variable.
	 *
	 * @param is	Matlab variable
	 */
	public static ImagePlus createColor(byte[][][][] is, boolean showImage) {
		return createColor("Import from Matlab", is, showImage);
	}

	/**
	 * Create a new 3D color image in ImageJ from a Matlab variable with a specified title.
	 *
	 * @param title	title of the new image
	 * @param is	Matlab variable
	 */
	public static ImagePlus createColor(String title, byte[][][][] is, boolean showImage) {
		int height = is.length;
		int width = is[0].length;
		int stackSize = is[0][0].length;
		int dim = is[0][0][0].length;
		ImageStack imagestack = new ImageStack(width, height);
		if (dim >= 3) {
			for (int k = 0; k < stackSize; k++) {
				ColorProcessor colorprocessor = new ColorProcessor(width, height);
				byte[] red = new byte[width * height];
				byte[] green = new byte[width * height];
				byte[] blue = new byte[width * height];
				for (int j = 0; j < height; j++) {
					int index = j * width;
					int i = 0;
					while (i < width) {
						red[index] = is[j][i][k][0];
						green[index] = is[j][i][k][1];
						blue[index] = is[j][i][k][2];
						i++;
						index++;
					}
				}
				colorprocessor.setRGB(red, green, blue);
				imagestack.addSlice("", colorprocessor);
			}
		} 
		else if (dim >= 2) {
			for (int k = 0; k < stackSize; k++) {
				ColorProcessor colorprocessor = new ColorProcessor(width, height);
				byte[] red = new byte[width * height];
				byte[] green = new byte[width * height];
				byte[] blue = new byte[width * height];

				for (int j = 0; j < height; j++) {
					int index = j * width;
					int i = 0;
					while (i < width) {
						red[index] = is[j][i][k][0];
						green[index] = is[j][i][k][1];
						i++;
						index++;
					}
				}
				colorprocessor.setRGB(red, green, blue);
				imagestack.addSlice("", colorprocessor);
			}
		} 
		else if (dim >= 1) {
			for (int k = 0; k < stackSize; k++) {
				ColorProcessor colorprocessor = new ColorProcessor(width, height);
				byte[] red = new byte[width * height];
				byte[] green = new byte[width * height];
				byte[] blue = new byte[width * height];

				for (int j = 0; j < height; j++) {
					int index = j * width;
					int i = 0;
					while (i < width) {
						red[index] = is[j][i][k][0];
						i++;
						index++;
					}
				}
				colorprocessor.setRGB(red, green, blue);
				imagestack.addSlice("", colorprocessor);
			}
		}
		ImagePlus imp = new ImagePlus(title, imagestack);
		if (showImage) {
			imp.show();
		}
		return imp;
	}

	/**
	 * Run a ImageJ command without arguments.
	 *
	 * This method call the run method of ImageJ without any options.
	 *
	 * @param command	command to run
	 */
	public static void run(String command) {
		IJ.run(command);
	}

	/**
	 * Run a ImageJ command with specified arguments.
	 *
	 * This method call the run method of ImageJ with specified options.
	 *
	 * @param command	command in ImageJ
	 * @param options	options for the command
	 */
	public static void run(String command, String options) {
		IJ.run(command, options);

	}

	/**
	 * Set the threshold values of the current image.
	 *
	 * @param lowerThreshold
	 * @param upperThresold
	 */
	public static void setTreshold(double lowerThreshold, double upperThresold){
		IJ.setThreshold( lowerThreshold,  upperThresold);
	}

}
