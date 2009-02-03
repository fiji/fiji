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

import stitching.CommonFunctions;
import stitching.GridLayout;
import stitching.ImageInformation;
import stitching.model.TranslationModel3D;
import stitching.model.TranslationModel2D;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.*;
import ij.gui.*;

/**
 * @author Stephan
 *
 */
public class Stitch_Image_Grid implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	
	public static int gridSizeXStatic = 3, gridSizeYStatic = 3;
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
	
	public void run(String arg0)
	{
		GenericDialog gd = new GenericDialog("Stitch Image Grid");
		GridLayout gridLayout = new GridLayout();
		
		gd.addNumericField("grid_size_x", gridSizeXStatic, 0);
		gd.addNumericField("grid_size_y", gridSizeYStatic, 0);
		gd.addSlider("overlap [%]", 0, 100, overlapStatic);
		gd.addStringField("directory", directoryStatic, 50);
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
		
		double overlap = gd.getNextNumber()/100;
		overlapStatic = overlap;
		
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
		directory = directory.replace('\\', '/');
		directory = directory.trim();
		if (directory.length() > 0 && !directory.endsWith("/"))
			directory = directory + "/";
		
		gridLayout.fusionMethod = fusionMethod;
		gridLayout.handleRGB = handleRGB;
		gridLayout.imageInformationList = new ArrayList<ImageInformation>();
		
		PrintWriter out = openFileWrite(directory + output);
				
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
            		ImagePlus imp = CommonFunctions.loadImage(directory, file, gridLayout.rgbOrder);
            		if (imp == null)
            		{
            			IJ.error("Cannot open first file: '" + directory + file + "' - Quitting.");
            			return;
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
            			out.println(directory + file + "; ; (" + xoffset + ", " + yoffset + ", " + zoffset + ")");
            		else
            			out.println(directory + file + "; ; (" + xoffset + ", " + yoffset + ")");
            	}            	
            	
            	ImageInformation iI;
            	
            	if (dim == 3)
            		iI = new ImageInformation(3, i, new TranslationModel3D());
            	else
            		iI = new ImageInformation(2, i, new TranslationModel2D());
            	
            	iI.imageName = directory + file;
            	iI.imp = null;
            	iI.offset[0] = xoffset;
            	iI.offset[1] = yoffset;
            	if (dim == 3)
            		iI.offset[2] = zoffset;
            	gridLayout.imageInformationList.add(iI);
            	
            	i++;
            }
    	}
    	if (out != null)
    		out.close();
    	
    	if (writeOnlyOutput)
    		return;

    	Stitch_Image_Collection smc = new Stitch_Image_Collection();
    	smc.work(gridLayout, previewOnly);
	}
	
	private String getLeadingZeros(int zeros, int number)
	{
		String output = "" + number;
		
		while (output.length() < zeros)
			output = "0" + output;
		
		return output;
	}
	
	private static PrintWriter openFileWrite(String fileName)
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
