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

import java.io.File;
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

/**
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Stitch_Image_Directory implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	
	public static String imageDirectoryStatic = "";
	public static String rgbOrderStatic = rgbTypes[0];
	public static String tileConfStatic = "TileConfiguration.txt";
	public static String handleRGBStatic = colorList[colorList.length - 1];
	public static String fusionMethodStatic = methodListCollection[LIN_BLEND];
	public static double alphaStatic = 1.5;
	public static double thresholdRStatic = 0.3;
	public static double thresholdDisplacementRelativeStatic = 2.5;
	public static double thresholdDisplacementAbsoluteStatic = 3.5;
	
	public void run(String arg0)
	{
		GenericDialogPlus gd = new GenericDialogPlus("Stitch Directory with Images (unknown configuration)");
		GridLayout gridLayout = new GridLayout();
		
		//gd.addStringField("image_directory", imageDirectoryStatic, 50);
		gd.addDirectoryField("image_directory", imageDirectoryStatic, 50);
		
		gd.addStringField("output_file_name", tileConfStatic, 50);		
		gd.addChoice("rgb_order", rgbTypes, rgbOrderStatic);
		gd.addChoice("channels_for_registration", colorList, handleRGBStatic);
		gd.addChoice("fusion_method", methodListCollection, fusionMethodStatic);
		gd.addNumericField("fusion_alpha", alphaStatic, 2);
		gd.addNumericField("regression_threshold", thresholdRStatic, 2);
		gd.addNumericField("max/avg_displacement_threshold", thresholdDisplacementRelativeStatic, 2);		
		gd.addNumericField("absolute_displacement_threshold", thresholdDisplacementAbsoluteStatic, 2);		
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		String imageDir = gd.getNextString();
		imageDirectoryStatic = imageDir;

		String output = gd.getNextString();
		tileConfStatic = output;
		
		gridLayout.rgbOrder = gd.getNextChoice();
		rgbOrderStatic = gridLayout.rgbOrder;		
		
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
		
		// get all files from the directory
		File dir = new File(imageDir);
		if ( !dir.isDirectory() )
		{
			IJ.error("'" + imageDir + "' is not a directory. stop.");
			return;
		}
		
		String[] imageFiles = dir.list();
				
		ArrayList<String> files = new ArrayList<String>();
		for (String fileName : imageFiles )
		{
			File file = new File( dir, fileName );
			if ( file.isFile() )
			{
				IJ.log( file.getPath() );
				files.add( fileName );
			}
		}
		
		IJ.log("Found " + files.size() + " files.");
		
		if( files.size() < 2 )
		{
			IJ.error("Only " + files.size() + " files found in '" + dir.getPath() + "' stop.");
			return;
		}

		
		gridLayout.sizeX = files.size();
		gridLayout.sizeY = 1;
		
		gridLayout.fusionMethod = fusionMethod;
		gridLayout.handleRGB = handleRGB;
		gridLayout.imageInformationList = new ArrayList<ImageInformation>();
		
		String fileName = (new File(imageDir, output)).getPath();
		PrintWriter out = openFileWrite( fileName );

		boolean seenFirst = false;
        int i = 0;
    	int dim = 0;

    	for ( String file : files )
    	{
    		if ( file.contains("TileConfiguration.txt") )
    			continue;
    		
        	if (!seenFirst)
        	{        		
        		ImagePlus imp = CommonFunctions.loadImage( dir.getPath(), file, -1, gridLayout.rgbOrder );
        		
        		if (imp == null)
        		{
        			IJ.log("Cannot read: " + file + ", trying next one...");
        			continue;
        		}
        		
        		seenFirst = true;
        		
        		if (imp.getStackSize() > 1)
        			gridLayout.dim = 3;
        		else
        			gridLayout.dim = 2;
        		
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
        	        	
        	if (out != null)
        	{
        		if (dim == 3)
        			out.println((new File(imageDir, file)).getPath() + "; ; (" + 0 + ", " + 0 + ", " + 0 + ")");
        		else
        			out.println((new File(imageDir, file)).getPath() + "; ; (" + 0 + ", " + 0 + ")");
        	}            	
        	
        	ImageInformation iI;
        	
        	if (dim == 3)
        		iI = new ImageInformation(3, i, new TranslationModel3D());
        	else
        		iI = new ImageInformation(2, i, new TranslationModel2D());
        	
        	iI.imageName = (new File(imageDir, file)).getPath();
        	iI.imp = null;
        	iI.offset[0] = 0;
        	iI.offset[1] = 0;
        	iI.position[0] = 0;
        	iI.position[1] = 0;
        	if (dim == 3)
        	{
        		iI.offset[2] = 0;
        		iI.position[2] = 0;
        	}
        	gridLayout.imageInformationList.add(iI);
        	
        	i++;
            
    	}
    	if (out != null)
    		out.close();
    	
    	Stitch_Image_Collection smc = new Stitch_Image_Collection();
    	smc.work(gridLayout, false, true, fileName);
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
