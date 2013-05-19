/**
 * Stitching_ plugin for ImageJ and Fiji.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 */

import static stitching.CommonFunctions.LIN_BLEND;
import static stitching.CommonFunctions.addHyperLinkListener;
import static stitching.CommonFunctions.colorList;
import static stitching.CommonFunctions.methodListCollection;
import static stitching.CommonFunctions.rgbTypes;
import stitching.GridLayout;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.MultiLineLabel;
import ij.io.FileSaver;
import ij.plugin.PlugIn;

/**
 * Plugin class to stitch a sequence of grid of images (fixed X/Y configuration)
 * 
 * @author Ignacio Arganda-Carreras <iarganda@mit.edu>
 */
public class Stitch_Image_Grid_Sequence implements PlugIn
{
	private String myURL = "http://fiji.sc/Stitching_2D/3D";

	public static int gridSizeXStatic = 3, gridSizeYStatic = 3, gridSizeZStatic = 3;

	public static double overlapStatic = 20;
	public static String inputDirectoryStatic = "";
	public static String fileNamesStatic = "Tile_Z{zzz}_Y{yyy}_X{xxx}.lsm";
	public static String rgbOrderStatic = rgbTypes[0];
	public static String tileConfStatic = "TileConfiguration_{zzz}.txt";
	public static String outputDirectoryStatic = "";
	public static boolean writeOnlyTileConfStatic = false;
	public static int startXStatic = 1;
	public static int startYStatic = 1;
	public static int startZStatic = 1;
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
		GenericDialogPlus gd = new GenericDialogPlus("Stitch Image Grid Sequence");
		GridLayout gridLayout = new GridLayout();
		
		
		gd.addNumericField("grid_size_x", gridSizeXStatic, 0);
		gd.addNumericField("grid_size_y", gridSizeYStatic, 0);
		gd.addNumericField("grid_size_z", gridSizeZStatic, 0);
		
		
		gd.addSlider("overlap [%]", 0, 100, overlapStatic);
		gd.addDirectoryField("input directory", inputDirectoryStatic, 50);
		gd.addStringField("file_names", fileNamesStatic, 50);
		gd.addChoice("rgb_order", rgbTypes, rgbOrderStatic);
		gd.addStringField("Output_file_name", tileConfStatic, 50);
		gd.addDirectoryField("output directory", outputDirectoryStatic, 50);
		gd.addCheckbox("Save_Only_Tile_Configuration", writeOnlyTileConfStatic);
		gd.addNumericField("start_x", startXStatic, 0);
		gd.addNumericField("start_y", startYStatic, 0);
		gd.addNumericField("start_z", startZStatic, 0);
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
		gd.addMessage("This Plugin is maintained by Ignacio Arganda-Carreras\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		gridLayout.sizeX = (int)Math.round(gd.getNextNumber());
		gridLayout.sizeY = (int)Math.round(gd.getNextNumber());
		gridSizeXStatic = gridLayout.sizeX;
		gridSizeYStatic = gridLayout.sizeY;
		
		gridSizeZStatic = (int)Math.round(gd.getNextNumber());
		
		double overlap = gd.getNextNumber()/100;
		overlapStatic = overlap*100;
		
		String inputDirectory = gd.getNextString();
		inputDirectoryStatic = inputDirectory;
		
		String filenames = gd.getNextString();
		fileNamesStatic = filenames;
		
		gridLayout.rgbOrder = gd.getNextChoice();
		rgbOrderStatic = gridLayout.rgbOrder;
		
		String outputFileName = gd.getNextString();
		tileConfStatic = outputFileName;
		
		String outputDirectory = gd.getNextString();
		outputDirectoryStatic = outputDirectory;
		
		boolean writeOnlyOutput = gd.getNextBoolean();
		writeOnlyTileConfStatic = writeOnlyOutput;
		
		int startX = (int)Math.round(gd.getNextNumber());
		startXStatic = startX;
		
		int startY = (int)Math.round(gd.getNextNumber());
		startYStatic = startY;
		
		int startZ = (int)Math.round(gd.getNextNumber());
		startZStatic = startZ;
		
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
		
		stitchImageGridSequence(gridSizeZStatic, filenames, inputDirectory, gridLayout, handleRGB, 
				fusionMethod, outputFileName, outputDirectory, overlap, startX, startY, startZ, startI, 
				writeOnlyOutput, previewOnly, computeOverlap);
		
	}

	/**
	 * Stitch sequence of image grids (all grids must have the same X/Y configuration)
	 * 
	 * @param nSections
	 * @param filenames
	 * @param inputDirectory
	 * @param gridLayout
	 * @param handleRGB
	 * @param fusionMethod
	 * @param outputFileName
	 * @param outputDirectory
	 * @param overlap
	 * @param startX
	 * @param startY
	 * @param startZ
	 * @param startI
	 * @param writeOnlyOutput
	 * @param previewOnly
	 * @param computeOverlap
	 */
	public static void stitchImageGridSequence(
			int nSections, 
			String filenames, 
			String inputDirectory, 
			GridLayout gridLayout, 
			String handleRGB, 
			String fusionMethod, 
			String outputFileName,
			String outputDirectory,
			double overlap, 
			int startX, 
			int startY,
			int startZ,
			int startI, 
			boolean writeOnlyOutput, 
			boolean previewOnly, 
			boolean computeOverlap) 
	{
		// find how to parse
		String replaceZ = "{";
		int numZValues = 0;
		
		int z1 = filenames.indexOf("{z");
		int z2 = filenames.indexOf("z}");
		if (z1 >= 0 && z2 > 0)
		{
			numZValues = z2 - z1;
			for (int i = 0; i < numZValues; i++)
				replaceZ += "z";
			replaceZ += "}";
		}
		else
		{
			replaceZ = "\\\\\\\\";
		}
		
		final int gridSize = gridLayout.sizeX * gridLayout.sizeY;
		
		for(int z = 0; z < nSections; z++)
		{
			final int zs = z + startZ;
			final String file = filenames.replace(replaceZ, Stitch_Image_Grid.getLeadingZeros(numZValues, zs));
			final String outTileConfName = outputFileName.replace(replaceZ, Stitch_Image_Grid.getLeadingZeros(numZValues, zs)); 
			
			final ImagePlus fusedImage = 
				Stitch_Image_Grid.stitchImageGrid(file, inputDirectory, gridLayout, handleRGB, fusionMethod, 
					outTileConfName, overlap, startX, startY, startI, writeOnlyOutput, previewOnly, computeOverlap);
			
			if (fusedImage == null)
				return;
			
			final String outputFusedPath = outputDirectory + System.getProperty("file.separator") + fusedImage.getTitle() 
								+ "_" + Stitch_Image_Grid.getLeadingZeros(numZValues, zs) + ".tif"; 
			IJ.log("Saving " + outputFusedPath +  " ... ");
			try 
			{
				new FileSaver(fusedImage).saveAsTiff(outputFusedPath);
			} 
			catch (Exception e) 
			{
				IJ.log("Error while saving " + outputFusedPath);
				e.printStackTrace();
				return;
			}
			IJ.log("Saved");
			
			startI += gridSize;
			
			// Iteratively close fused images to avoid unnecessarily filling the screen 
			fusedImage.close();
		}
		
		IJ.showMessage("Image grid sequence stitching is done!");
		
	}

}
