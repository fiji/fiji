import java.io.*;

import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.process.ColorProcessor;


public class PIVAnalyserTestDrive {
	
	private static final String TESTPIV = "testpiv-1.tif"; 
	private static boolean LOG = false;
	private static boolean DISPLAY_COLOR_WHEEL = true;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Set params
		int[][] pairs = new int[][] { new int[] {1,2} };
		PIV_analyser.WINDOW_SIZE blocksize = PIV_analyser.WINDOW_SIZE._64x64;  
		// Load the test image
		System.out.println("Loading the image...");
		FileInfo fi = new FileInfo();
		fi.width = 256;
		fi.height = 64;
		fi.offset = 177;
		fi.nImages = 2;		
		fi.fileName = TESTPIV;
		fi.directory = ".";
		new FileOpener(fi).open();
		// Get a reference to it
		ImagePlus imp = WindowManager.getCurrentImage();
		// Initialize the plugin
		System.out.println("Initializing the plugin...");
		PIV_analyser piv = new PIV_analyser();
		// give it a ref to the ImageProcessor
		piv.setup("", imp);
		// setup image pair
		piv.setImagePairs(pairs);
		// setup window size
		piv.setWinsize(blocksize);
		// run it and time it
		System.out.println("Running the plugin...");
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();
		piv.exec(false);
		stopwatch.stop();
		System.out.println("Done in: " + stopwatch);
		System.out.println("Exiting.");
		if (LOG) {
			File log = new File("log.txt");
			Writer output;
			try {
				output = new BufferedWriter(new FileWriter(log, true));
				output.append("[+] Test - "+DateUtils.now()+"\n");
				output.append("\t Image: "+TESTPIV+"\n");
				output.append("\t Image pairs: "+ pairs.length + " pair(s).\n");
				output.append("\t Image size: "+imp.getWidth()+" x "+imp.getHeight()+".\n");
				output.append("\t Block size: "+blocksize.toString()+".\n");
				output.append("\t Exec time: "+ stopwatch+"\n\n");
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		imp.changes = false;
		imp.close();
		
		
		if (DISPLAY_COLOR_WHEEL) {		
			ColorProcessor cp = new ColorProcessor(256, 256);
			PIV_analyser.colorCircle(cp);
			new ImagePlus("Color coded orientation", cp).show();
		}
	}

}
