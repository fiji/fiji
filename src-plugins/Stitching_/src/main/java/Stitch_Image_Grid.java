/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 *
 * @author Stephan Preibisch
 */

import static stitching.CommonFunctions.LIN_BLEND;
import static stitching.CommonFunctions.addHyperLinkListener;
import static stitching.CommonFunctions.colorList;
import static stitching.CommonFunctions.methodListCollection;
import static stitching.CommonFunctions.rgbTypes;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import fiji.util.gui.GenericDialogPlus;

import stitching.CommonFunctions;
import stitching.GridLayout;
import stitching.ImageInformation;
import stitching.model.TranslationModel3D;
import stitching.model.TranslationModel2D;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.*;
import ij.gui.*;

/* from Curtis
 * 
import loci.common.Location;
import loci.formats.FilePattern;

public class ChooseFile {
  public static void main(String[] args) {
    String s = args[0];
    FilePattern fp = new FilePattern(new Location(s));
    System.out.println("pattern = " + fp.getPattern());

    String[] prefixes = fp.getPrefixes();
    int[] count = fp.getCount();

    StringBuilder sb = new StringBuilder();
    char letter = 'i';
    for (String prefix : prefixes) {
      sb.append(prefix + "{" + letter + "}");
      letter++;
    }
    sb.append(fp.getSuffix());
    System.out.println("Steffi's pattern = " + sb.toString());
  }
}
[8:04:38 PM] Curtis Rueden: That's how you generate a pattern for the Stitcher like you need.
[8:04:53 PM] Curtis Rueden: E.g.:
[8:04:54 PM | Edited 8:05:08 PM] Curtis Rueden: j ChooseFile data/jayne/032610\ h2b\ low\ mag_C1_TP1.tiff
pattern = /Users/curtis/data/jayne/032610 h2b low mag_C<1-2>_TP<1-237>.tiff
Steffi's pattern = 032610 h2b low mag_C{i}_TP{j}.tiff
[8:05:20 PM] Curtis Rueden: Hopefully I did that right, and it makes sense. * 
 */

/**
 * @author Stephan
 *
 */
public class Stitch_Image_Grid implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	/*public static String[] arrangement = {"First X forward, then Y forward", "First Y forward, then X forward", 
		                                  "First X backward then Y forward", "First Y forward, then X backward",
		                                  "First X forward then Y backward", "First Y backward, then X forward",
		                                  "First X backward then Y backward", "First Y backward, then X backward"}; 
	*/
	public static int gridSizeXStatic = 3, gridSizeYStatic = 3;
	//public static String arrangmentStatic = arrangement[0];
	public static double overlapStatic = 20;
	public static String directoryStatic = "";
	public static String fileNamesStatic = "TiledConfocal_{ii}.lsm";
	public static String rgbOrderStatic = rgbTypes[0];
	public static String tileConfStatic = "TileConfiguration.txt";
	public static boolean writeOnlyTileConfStatic = false;
	public static int startXStatic = 1;
	public static int startYStatic = 1;
	public static int startIStatic = 1;
	public static String handleRGBStatic = colorList[colorList.length - 1];
	public static String fusionMethodStatic = methodListCollection[LIN_BLEND];
	public static double alphaStatic = 1.5;
	public static double thresholdRStatic = 0.3;
	public static double thresholdDisplacementRelativeStatic = 2.5;
	public static double thresholdDisplacementAbsoluteStatic = 3.5;
	public static boolean previewOnlyStatic = false;
	public static boolean computeOverlapStatic = true;
	
	public void run(String arg0)
	{
		GenericDialogPlus gd = new GenericDialogPlus("Stitch Image Grid");
		GridLayout gridLayout = new GridLayout();
		
		gd.addNumericField("grid_size_x", gridSizeXStatic, 0);
		gd.addNumericField("grid_size_y", gridSizeYStatic, 0);
		//gd.addChoice("order_of_storage", arrangement, arrangmentStatic );
		
		gd.addSlider("overlap [%]", 0, 100, overlapStatic);
		gd.addDirectoryField("directory", directoryStatic, 50);
		gd.addStringField("file_names", fileNamesStatic, 50);
		gd.addChoice("rgb_order", rgbTypes, rgbOrderStatic);
		gd.addStringField("Output_file_name", tileConfStatic, 50);
		gd.addCheckbox("Save_Only_Tile_Configuration", writeOnlyTileConfStatic);
		gd.addNumericField("start_x", startXStatic, 0);
		gd.addNumericField("start_y", startYStatic, 0);
		gd.addNumericField("start_i", startIStatic, 0);
		gd.addChoice("channels_for_registration", colorList, handleRGBStatic);
		gd.addChoice("fusion_method", methodListCollection, fusionMethodStatic);
		gd.addNumericField("fusion_alpha", alphaStatic, 2);
		gd.addNumericField("regression_threshold", thresholdRStatic, 2);
		gd.addNumericField("max/avg_displacement_threshold", thresholdDisplacementRelativeStatic, 2);		
		gd.addNumericField("absolute_displacement_threshold", thresholdDisplacementAbsoluteStatic, 2);		
		gd.addCheckbox("create_only_preview", previewOnlyStatic);
		gd.addCheckbox("compute_overlap (otherwise use the coordinates given in the layout file)", computeOverlapStatic );
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		gridLayout.sizeX = (int)Math.round(gd.getNextNumber());
		gridLayout.sizeY = (int)Math.round(gd.getNextNumber());
		gridSizeXStatic = gridLayout.sizeX;
		gridSizeYStatic = gridLayout.sizeY;

		//gridLayout.arrangement = gd.getNextChoice();
		//arrangmentStatic = gridLayout.arrangement;
		
		double overlap = gd.getNextNumber()/100;
		overlapStatic = overlap*100;
		
		String directory = gd.getNextString();
		directoryStatic = directory;
		
		String filenames = gd.getNextString();
		fileNamesStatic = filenames;
		
		gridLayout.rgbOrder = gd.getNextChoice();
		rgbOrderStatic = gridLayout.rgbOrder;
		
		String output = gd.getNextString();
		tileConfStatic = output;
		
		boolean writeOnlyOutput = gd.getNextBoolean();
		writeOnlyTileConfStatic = writeOnlyOutput;
		
		int startX = (int)Math.round(gd.getNextNumber());
		startXStatic = startX;
		
		int startY = (int)Math.round(gd.getNextNumber());
		startYStatic = startY;
		
		int startI = (int)Math.round(gd.getNextNumber());
		startIStatic = startI;
		
		String handleRGB = gd.getNextChoice();
		handleRGBStatic = handleRGB;
		
		String fusionMethod = gd.getNextChoice();
		fusionMethodStatic = fusionMethod;
		
		gridLayout.alpha = gd.getNextNumber();
		alphaStatic = gridLayout.alpha;
		
		gridLayout.thresholdR = gd.getNextNumber();
		thresholdRStatic = gridLayout.thresholdR;
		
		gridLayout.thresholdDisplacementRelative = gd.getNextNumber();
		thresholdDisplacementRelativeStatic = gridLayout.thresholdDisplacementRelative;
		
		gridLayout.thresholdDisplacementAbsolute = gd.getNextNumber();
		thresholdDisplacementAbsoluteStatic = gridLayout.thresholdDisplacementAbsolute;
		
		boolean previewOnly = gd.getNextBoolean();
		previewOnlyStatic = previewOnly;
		
		boolean computeOverlap = gd.getNextBoolean();
		computeOverlapStatic = computeOverlap;
		
		stitchImageGrid(filenames, directory, gridLayout, handleRGB, 
				fusionMethod, output, overlap, startX, startY, startI, 
				writeOnlyOutput, previewOnly, computeOverlap);

	}
	
	/**
	 * Stitch grid of 2D images
	 *  
	 * @param filenames file name format (for example: "TiledConfocal_{ii}.lsm")
	 * @param inputDirectory input directory
	 * @param gridLayout grid layout information
	 * @param handleRGB RGB mode (@see stitching.CommonFunctions.colorList)
	 * @param fusionMethod fusion method (@see stitching.CommonFunctions.methodListCollection)
	 * @param outputFileName output file name (for example: "TileConfiguration.txt")
	 * @param overlap percentage of overlap
	 * @param startX starting X value
	 * @param startY starting Y value
	 * @param startI starting I value
	 * @param writeOnlyOutput "Save Only Tile Configuration" option
	 * @param previewOnly "create only preview" option
	 * @param computeOverlap "compute overlap" option
	 */
	public static ImagePlus stitchImageGrid(
			String filenames, 
			String inputDirectory, 
			GridLayout gridLayout, 
			String handleRGB, 
			String fusionMethod, 
			String outputFileName, 
			double overlap, 
			int startX, 
			int startY, 
			int startI, 
			boolean writeOnlyOutput, 
			boolean previewOnly,
			boolean computeOverlap)
	{
		// find how to parse
		String replaceX = "{", replaceY = "{", replaceI = "{";
		int numXValues = 0, numYValues = 0, numIValues = 0;
		
		int x1 = filenames.indexOf("{x");
		int x2 = filenames.indexOf("x}");
		if (x1 >= 0 && x2 > 0)
		{
			numXValues = x2 - x1;
			for (int i = 0; i < numXValues; i++)
				replaceX += "x";
			replaceX += "}";
		}
		else
		{
			replaceX = "\\\\\\\\";
		}

		int y1 = filenames.indexOf("{y");
		int y2 = filenames.indexOf("y}");
		if (y1 >= 0 && y2 > 0)
		{
			numYValues = y2 - y1;
			for (int i = 0; i < numYValues; i++)
				replaceY += "y";
			replaceY += "}";
		}
		else
		{
			replaceY = "\\\\\\\\";
		}

		int i1 = filenames.indexOf("{i");
		int i2 = filenames.indexOf("i}");
		if (i1 >= 0 && i2 > 0)
		{
			numIValues = i2 - i1;
			for (int i = 0; i < numIValues; i++)
				replaceI += "i";
			replaceI += "}";
		}
		else
		{
			replaceI = "\\\\\\\\";
		}
		
		// write the output file
		inputDirectory = inputDirectory.replace('\\', '/');
		inputDirectory = inputDirectory.trim();
		if (inputDirectory.length() > 0 && !inputDirectory.endsWith("/"))
			inputDirectory = inputDirectory + "/";
		
		gridLayout.fusionMethod = fusionMethod;
		gridLayout.handleRGB = handleRGB;
		gridLayout.imageInformationList = new ArrayList<ImageInformation>();
		
		String fileName = inputDirectory + outputFileName;
		PrintWriter out = openFileWrite( fileName );
				
        int imgX = 0, imgY = 0;
        int i = 0;
    	int xoffset = 0, yoffset = 0, zoffset = 0;
    	int dim = 0;

    	for (int y = 0; y < gridLayout.sizeY; y++)
    	{
        	if (y == 0)
        		yoffset = 0;
        	else 
        		yoffset += (int)(imgY * (1 - overlap));

        	for (int x = 0; x < gridLayout.sizeX; x++)
            {
            	int xs = x + startX;
            	int ys = y + startY;
            	int is = i + startI;       	
            	
            	String file = filenames.replace(replaceX, getLeadingZeros(numXValues, xs));
            	file = file.replace(replaceY, getLeadingZeros(numYValues, ys));
            	file = file.replace(replaceI, getLeadingZeros(numIValues, is));
            	
            	if (i == 0)
            	{
            		ImagePlus imp = CommonFunctions.loadImage(inputDirectory, file, -1, gridLayout.rgbOrder);
            		if (imp == null)
            		{
            			IJ.error("Cannot open first file: '" + inputDirectory + file + "' - Quitting.");
            			return null;
            		}
            		if (imp.getStackSize() > 1)
            			gridLayout.dim = 3;
            		else
            			gridLayout.dim = 2;
            		imgX = imp.getWidth();
            		imgY = imp.getHeight();
            		
            		if (imp.getStackSize() > 1)
            			dim = 3;
            		else
            			dim = 2;
            		
            		if (out != null)
            		{
            			out.println("# Define the number of dimensions we are working on");
            	        out.println("dim = " + dim);
            	        out.println("");
            	        out.println("# Define the image coordinates");
            		}
            		
            		imp.close();
            	}
            	
            	if (x == 0 && y == 0)
            		xoffset = yoffset = zoffset = 0;
            	
            	if (x == 0)
            		xoffset = 0;
            	else 
            		xoffset += (int)(imgX * (1 - overlap));
            	
            	if (out != null)
            	{
            		if (dim == 3)
            			out.println(inputDirectory + file + "; ; (" + xoffset + ", " + yoffset + ", " + zoffset + ")");
            		else
            			out.println(inputDirectory + file + "; ; (" + xoffset + ", " + yoffset + ")");
            	}            	
            	
            	ImageInformation iI;
            	
            	if (dim == 3)
            		iI = new ImageInformation(3, i, new TranslationModel3D());
            	else
            		iI = new ImageInformation(2, i, new TranslationModel2D());
            	
            	iI.imageName = inputDirectory + file;
            	iI.imp = null;
            	iI.offset[0] = xoffset;
            	iI.offset[1] = yoffset;
            	iI.position[0] = xoffset;
            	iI.position[1] = yoffset;
            	if (dim == 3)
            	{
            		iI.offset[2] = zoffset;
            		iI.position[2] = zoffset;
            	}
            	gridLayout.imageInformationList.add(iI);
            	
            	i++;
            }
    	}
    	if (out != null)
    		out.close();
    	
    	if (writeOnlyOutput)
    		return null;

    	Stitch_Image_Collection smc = new Stitch_Image_Collection();
    	return smc.work(gridLayout, previewOnly, computeOverlap, fileName,true);
	}
	
	public static String getLeadingZeros(int zeros, int number)
	{
		String output = "" + number;
		
		while (output.length() < zeros)
			output = "0" + output;
		
		return output;
	}
	
	public static PrintWriter openFileWrite(String fileName)
	{
	  PrintWriter outputFile;
	  try
	  {
		outputFile = new PrintWriter(new FileWriter(fileName));
	  }
	  catch (IOException e)
	  {
		System.err.println("CreateGridLayout.openFileWrite(): " + e);
		outputFile = null;
	  }
	  return(outputFile);
	}	

}
