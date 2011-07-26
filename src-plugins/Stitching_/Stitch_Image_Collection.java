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

import static stitching.CommonFunctions.AVG;
import static stitching.CommonFunctions.LIN_BLEND;
import static stitching.CommonFunctions.MAX;
import static stitching.CommonFunctions.MIN;
import static stitching.CommonFunctions.RED_CYAN;
import static stitching.CommonFunctions.addHyperLinkListener;
import static stitching.CommonFunctions.getPixelMin;
import static stitching.CommonFunctions.getPixelMinRGB;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.util.gui.GenericDialogPlus;

import ij.gui.MultiLineLabel;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.ImageStack;

import stitching.CommonFunctions;
import stitching.GridLayout;
import stitching.ImageInformation;
import stitching.OverlapProperties;
import stitching.Point2D;
import stitching.Point3D;
import stitching.model.*;

import static stitching.CommonFunctions.colorList;
import static stitching.CommonFunctions.methodListCollection;
import static stitching.CommonFunctions.rgbTypes;

public class Stitch_Image_Collection implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	public int dim = -1;
	
	public double alpha, thresholdR, thresholdDisplacementRelative, thresholdDisplacementAbsolute;
	public String rgbOrder;
	
	public static String fileNameStatic = "TileConfiguration.txt";
	public static boolean computeOverlapStatic = true;
	public static String handleRGBStatic = colorList[colorList.length - 1];
	public static String rgbOrderStatic = rgbTypes[0];	
	public static String fusionMethodStatic = methodListCollection[LIN_BLEND];
	public static double alphaStatic = 1.5;
	public static double thresholdRStatic = 0.3;
	public static double thresholdDisplacementRelativeStatic = 2.5;
	public static double thresholdDisplacementAbsoluteStatic = 3.5;
	public static boolean previewOnlyStatic = false;

	
	public void run(String arg0)
	{
		GenericDialogPlus gd = new GenericDialogPlus("Stitch Image Collection");
		
		//gd.addStringField("Layout file", fileNameStatic, 50);
		gd.addFileField("Layout file", fileNameStatic, 50);		
		gd.addCheckbox("compute_overlap (otherwise use the coordinates given in the layout file)", computeOverlapStatic );
		gd.addChoice("Channels_for_Registration", colorList, handleRGBStatic);
		gd.addChoice("rgb_order", rgbTypes, rgbOrderStatic);
		gd.addChoice("Fusion_Method", methodListCollection, methodListCollection[LIN_BLEND]);
		gd.addNumericField("Fusion alpha", alphaStatic, 2);
		gd.addNumericField("Regression Threshold", thresholdRStatic, 2);
		gd.addNumericField("Max/Avg Displacement Threshold", thresholdDisplacementRelativeStatic, 2);		
		gd.addNumericField("Absolute Avg Displacement Threshold", thresholdDisplacementAbsoluteStatic, 2);		
		gd.addCheckbox("Create_only_Preview", previewOnlyStatic);
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		String fileName = gd.getNextString();
		fileNameStatic = fileName;

		boolean computeOverlap = gd.getNextBoolean();
		computeOverlapStatic = computeOverlap;

		String handleRGB = gd.getNextChoice();
		handleRGBStatic = handleRGB;
		
		this.rgbOrder = gd.getNextChoice();
		rgbOrderStatic = rgbOrder;
		
		String fusionMethod = gd.getNextChoice();
		fusionMethodStatic = fusionMethod;
		
		this.alpha = gd.getNextNumber();
		alphaStatic = alpha;
		
		this.thresholdR = gd.getNextNumber();
		thresholdRStatic = thresholdR;
		
		this.thresholdDisplacementRelative = gd.getNextNumber();
		thresholdDisplacementRelativeStatic = thresholdDisplacementRelative;
		
		this.thresholdDisplacementAbsolute = gd.getNextNumber();
		thresholdDisplacementAbsoluteStatic = thresholdDisplacementAbsolute;
		
		boolean previewOnly = gd.getNextBoolean();
		previewOnlyStatic = previewOnly;
		
		work(fileName, previewOnly, computeOverlap, fusionMethod, handleRGB, true);		
	}
	
	public ImagePlus work(String fileName, boolean createPreview, boolean computeOverlap, String fusionMethod, String handleRGB, boolean showImage)
	{
		// read the layout file
		ArrayList<ImageInformation> imageInformationList = readLayoutFile(fileName);		
		return work(imageInformationList, createPreview, computeOverlap, fusionMethod, handleRGB, fileName, showImage);
	}
	
	public ImagePlus work( GridLayout gridLayout, boolean createPreview, boolean computeOverlap, String fileName, boolean showImage )
	{
		this.alpha = gridLayout.alpha;
		this.thresholdR = gridLayout.thresholdR;
		this.thresholdDisplacementRelative = gridLayout.thresholdDisplacementRelative;
		this.thresholdDisplacementAbsolute = gridLayout.thresholdDisplacementAbsolute;
		this.dim = gridLayout.dim;
		this.rgbOrder = gridLayout.rgbOrder;
		
		return work(gridLayout.imageInformationList, createPreview, computeOverlap, gridLayout.fusionMethod, gridLayout.handleRGB, fileName, showImage);
	}
	
	public ImagePlus work(ArrayList<ImageInformation> imageInformationList, boolean createPreview, boolean computeOverlap, String fusionMethod, String handleRGB, String fileName, boolean showImage)
	{	
		IJ.log("(" + new Date(System.currentTimeMillis()) + "): Stitching the following files:");
		for (ImageInformation iI : imageInformationList)
			IJ.log("" + iI);	
		
		// make max intensity projection and find overlapping tiles
		ArrayList<OverlapProperties> overlappingTiles = findOverlappingTiles(imageInformationList, createPreview, fusionMethod);
				
		// ask if we should start like this
		if (createPreview)
			return null;
				
		// this will store the final output configuration
		ArrayList<ImageInformation> newImageInformationList;
		
		if ( computeOverlap )
		{
			// compute all phase correlations
			computePhaseCorrelations(overlappingTiles, handleRGB);
			
			// compute the model
			newImageInformationList = optimize( overlappingTiles, imageInformationList.get( 0 ) );
			
			if(newImageInformationList == null)
				return null;
		}
		else
		{
			newImageInformationList = imageInformationList;
		}
		
		// output the final positions
		IJ.log("(" + new Date(System.currentTimeMillis()) + "): Final image positions in the fused image:");
		for (ImageInformation i: newImageInformationList)
		{
			if (dim == 3)
				IJ.log("Tile " + i.id + " (" + i.imageName + "): " + i.position[0] + ", " + i.position[1] + ", " + i.position[2]);
			else
				IJ.log("Tile " + i.id + " (" + i.imageName + "): " + i.position[0] + ", " + i.position[1]);
		}
		
		// write the new tile configuration
		writeOutputConfiguration( fileName, newImageInformationList );
		
		// getMax and set minimum coordinates to 0,0,0
		float[] max = getAndApplyMinMax(newImageInformationList, dim);		
		if (dim == 3)
			IJ.log("(" + new Date(System.currentTimeMillis()) + "): Size of bounding box for output image: " + max[0] + ", " + max[1] + ", " + max[2]);
		else
			IJ.log("(" + new Date(System.currentTimeMillis()) + "): Size of bounding box for output image: " + max[0] + ", " + max[1]);
		
		// fuse the images
		ImagePlus fused = fuseImages(newImageInformationList, max, "Stitched Image", fusionMethod, rgbOrder, dim, alpha, showImage );
		if ( showImage )
			fused.show();
		IJ.log("(" + new Date(System.currentTimeMillis()) + "): Finished Stitching.");

		return fused;
	}
	
	protected void writeOutputConfiguration( String fileName, ArrayList<ImageInformation> imageInformationList )
	{
		try
		{
			String outputfileName;
			
			if ( fileName.endsWith( ".registered"))
				outputfileName = fileName;
			else
				outputfileName = fileName + ".registered";
			
			PrintWriter out = Stitch_Image_Grid.openFileWrite( outputfileName );

			out.println("# Define the number of dimensions we are working on");
	        out.println("dim = " + dim);
	        out.println("");
	        out.println("# Define the image coordinates");

	        for ( ImageInformation iI : imageInformationList )
	        {
	        	String name = iI.imageName;
	        	
	        	// if it is a multiseries file add a funny string at the end that marks it
	        	if ( iI.seriesNumber >= 0 )
	        		name = name + "(((" + iI.seriesNumber + ")))";
	        	
		        if (dim == 3)
	    			out.println( name + "; ; (" + iI.position[0] + ", " + iI.position[1] + ", " + iI.position[2] + ")");
	    		else
	    			out.println( name + "; ; (" + iI.position[0] + ", " + iI.position[1] + ")");
	        }

			out.close();
		}
		catch (Exception e)
		{
			IJ.log("Cannot write registered configuration file: " + e );
		}
	}
	
	public static ImagePlus fuseImages(final ArrayList<ImageInformation> imageInformationList, final float[] max, final String name, final String fusionMethod, 
									   final String rgbOrder, final int dim, final double alpha, final boolean showOutput )
	{
		final int type;

		if (fusionMethod.equals("Min. Intensity")) type = MIN;
		else if (fusionMethod.equals("Linear Blending")) type = LIN_BLEND;
		else if (fusionMethod.equals("Max. Intensity")) type = MAX;
		else if (fusionMethod.equals("Red-Cyan Overlay") && imageInformationList.size() == 2) type = RED_CYAN;
		else if (fusionMethod.equals("None")) type = CommonFunctions.NONE;
		else type = AVG; // fusionMethod.equals("Average")
		
		if (type == AVG)
			IJ.log("Average Fusion started.");
		else if (type == MAX)
			IJ.log("Maximum Fusion started.");
		else if (type == MIN)
			IJ.log("Minimum Fusion started.");
		else if (type == LIN_BLEND)
			IJ.log("Linear Blending Fusion started.");
		else if (type == RED_CYAN)
			IJ.log("Red-Cyan Fusion started.");
		else if (type == CommonFunctions.NONE)
			IJ.log("Overlapping (non fusion) started.");

		final int imageType = imageInformationList.get(0).imageType; 
		final int imgW = Math.round(max[0]);
		final int imgH = Math.round(max[1]);
		final int imgD;
		if (dim == 3)
			imgD = Math.round(max[2]);
		else 
			imgD = 1;
		
		final ImagePlus fusedImp;
		final float[] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = Float.MIN_VALUE;
		
		if (type == RED_CYAN) fusedImp = IJ.createImage(name + " - 0%", "rgb black", imgW, imgH, imgD);			
		else if (imageType == ImagePlus.GRAY8) fusedImp = IJ.createImage(name + " - 0%", "8-bit black", imgW, imgH, imgD);
		else if (imageType == ImagePlus.GRAY16) fusedImp = IJ.createImage(name + " - 0%", "16-bit black", imgW, imgH, imgD);
		else if (imageType == ImagePlus.GRAY32) fusedImp = IJ.createImage(name + " - 0%", "32-bit black", imgW, imgH, imgD);
		else if (imageType == ImagePlus.COLOR_RGB) fusedImp = IJ.createImage(name + " - 0%", "rgb black", imgW, imgH, imgD);
		else
		{
			IJ.error("Unsupported/Unknown Image Type: " + imageType);
			return null;
		}

		Calibration cal = null;
		
		final ImageStack fusedStack = fusedImp.getStack();	
		
		if ( showOutput )
			fusedImp.show();

		try
		{
			fusedImp.setSlice(imgD/2 + 1);
		}
		catch (Exception e){};
	
		if (type == CommonFunctions.NONE || type == MAX || type == RED_CYAN)
		{
			int count = 0;
			final int dimension = dim;

			for (ImageInformation iI : imageInformationList)
			{
				// load the image to paint
				final ImagePlus imp;
				
				if (iI.imp == null)
					imp = CommonFunctions.loadImage("", iI.imageName, iI.seriesNumber, rgbOrder);
				else
					imp = iI.imp;

				cal = updateCalibration( cal, iI.imp.getCalibration() );
				
				final Object[] imageStack1 = imp.getStack().getImageArray();
				final int w1 = imp.getStack().getWidth();
				final int h1 = imp.getStack().getHeight();
				final int d1 = imp.getStack().getSize();
				
				if (type == RED_CYAN && imageType == ImagePlus.GRAY32)
				{
					for (int z = 0; z < d1; z++)
						for (int y = 0; y < h1; y++)
							for (int x = 0; x < w1; x++)
							{
								final float pixel = getPixelMin(imageType, imageStack1, w1, h1, d1, x, y, z, 0);
								
								if (pixel > minmax[1])
									minmax[1] = pixel;
							
								if (pixel < minmax[0])
									minmax[0] = pixel;								
							}
				}
				
				for (int z = 0; z < d1; z++)
				{
					final ImageProcessor fusedIp;
					if (dim == 3)
						fusedIp = fusedStack.getProcessor(z + 1 + Math.round(iI.position[2]));
					else
						fusedIp = fusedStack.getProcessor(z + 1);
						
					final int[] pixelrgbimg = new int[3];
	
					for (int y = 0; y < h1; y++)
						for (int x = 0; x < w1; x++)
						{
							if (imageType == ImagePlus.COLOR_RGB)
							{
								// get pixel value from source image
								final int[] pixelrgbSRC;
								if (dimension == 3)
									pixelrgbSRC = getPixelMinRGB(imageStack1, w1, h1, d1, x, y, z, 0);
								else
									pixelrgbSRC = getPixelMinRGB(imageStack1, w1, h1, d1, x, y, 0, 0);

								// get pixel value from target image
								fusedIp.getPixel(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), pixelrgbimg);
								
								if (type == MAX)
								{									
									for (int i = 0; i < pixelrgbSRC.length; i++)
										pixelrgbimg[i] = Math.max(pixelrgbSRC[i], pixelrgbimg[i]);										
								}
								else if(type == RED_CYAN)
								{
									if (count == 0)
										pixelrgbimg[0] = (pixelrgbSRC[0] + pixelrgbSRC[1] + pixelrgbSRC[2])/3;
									else if (count == 1)
										pixelrgbimg[1] = pixelrgbimg[2] = (pixelrgbSRC[0] + pixelrgbSRC[1] + pixelrgbSRC[2])/3;
								}
								else if(type == CommonFunctions.NONE)
								{
									for (int i = 0; i < pixelrgbSRC.length; i++)
										pixelrgbimg[i] = pixelrgbSRC[i];	
								}
								
								fusedIp.putPixel(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), pixelrgbimg);																
							}
							else
							{
								// get pixel value from source image
								float pixel;
								if (dimension == 3)
									pixel = getPixelMin(imageType, imageStack1, w1, h1, d1, x, y, z, 0);
								else
									pixel = getPixelMin(imageType, imageStack1, w1, h1, d1, x, y, 0, 0);
									
								if (type == MAX)
								{
									// get pixel value from target image
									pixel = Math.max(pixel, fusedIp.getPixelValue(x + Math.round(iI.position[0]), y + Math.round(iI.position[1])));
									
									if (imageType == ImagePlus.GRAY8 || imageType == ImagePlus.GRAY16)
									{										
										fusedIp.putPixel(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), (int) (pixel + 0.5));
									}
									else 
									{
										fusedIp.putPixelValue(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), pixel);
										if (pixel > minmax[1])
											minmax[1] = pixel;
									
										if (pixel < minmax[0])
											minmax[0] = pixel;
									}
								}
								else if(type == RED_CYAN)
								{
									// get pixel value from target image
									fusedIp.getPixel(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), pixelrgbimg);
																		
									if (imageType == ImagePlus.GRAY8)
									{
										if (count == 0)
											pixelrgbimg[0] = Math.round(pixel);
										else if (count == 1)
											pixelrgbimg[1] = pixelrgbimg[2] = Math.round(pixel);
										
									}
									else if (imageType == ImagePlus.GRAY16)
									{
										if (count == 0)
											pixelrgbimg[0] = Math.round(pixel/256);
										else if (count == 1)
											pixelrgbimg[1] = pixelrgbimg[2] = Math.round(pixel/256);
										
									}
									else
									{
										if (count == 0)
											pixelrgbimg[0] = Math.round(((pixel-minmax[0])/(minmax[1] - minmax[0]))*255);
										else if (count == 1)
											pixelrgbimg[1] = pixelrgbimg[2] = Math.round(((pixel-minmax[0])/(minmax[1] - minmax[0]))*255);
									}
									
									fusedIp.putPixel(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), pixelrgbimg);
								}
								else if(type == CommonFunctions.NONE)								 
									fusedIp.putPixelValue(x + Math.round(iI.position[0]), y + Math.round(iI.position[1]), pixel);
									
								
							}
							
						}
				}
				
				if (iI.closeAtEnd)
					imp.close();

				if (type == MAX && imageType == ImagePlus.GRAY32)
					fusedImp.getProcessor().setMinAndMax(minmax[0], minmax[1]);
				
				if ( showOutput )
					fusedImp.updateAndDraw();
				count++;
				
				fusedImp.setTitle(name + " - " + (count/imageInformationList.size()) + "%");
			}
		}
		else
		{
			final AtomicInteger ai = new AtomicInteger(0);
			final AtomicInteger progress = new AtomicInteger(1);
			
	        final Thread[] threads = CommonFunctions.newThreads();
	        final int numThreads = threads.length;

	        // load the images
	        for (ImageInformation iI : imageInformationList)
	        {
				if (iI.imp == null)
					iI.tmp = CommonFunctions.loadImage("", iI.imageName, iI.seriesNumber, rgbOrder);
				else
					iI.tmp = iI.imp;		
				
				cal = updateCalibration( cal, iI.imp.getCalibration() );
				
				iI.imageStack = iI.tmp.getStack().getImageArray();
				iI.w = iI.tmp.getStack().getWidth();
				iI.h = iI.tmp.getStack().getHeight();
				iI.d = iI.tmp.getStack().getSize();
	        }
	        
	        for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	final int dimension = dim;
	                	
	                    int myNumber = ai.getAndIncrement();

	                    final ImageInformation[] indices = new ImageInformation[imageInformationList.size()];
	        			final float[] pixels = new float[imageInformationList.size()];
	        			final int[][] rgbPixels = new int[imageInformationList.size()][3];
	        			final int[] pos = new int[3];
	        			final float[] weights = new float[imageInformationList.size()];
	        			final int[] tmp = new int[imageInformationList.size()];
	        			
	        			float finalPixel = 0;
	        			final int[] finalRGBPixel = new int[3];
	        						
	        			final ImageProcessor[] ip = new ImageProcessor[imgD]; 
	        			for (int i = 0; i < imgD; i++)
	        				ip[i] = fusedStack.getProcessor(i + 1);

	        			for (pos[0] = 0; pos[0]< imgW; pos[0]++)
	        				if (pos[0]%numThreads == myNumber)
		        			{
	        				for (pos[1]= 0; pos[1] < imgH; pos[1]++)
	        					for (pos[2] = 0; pos[2] < imgD; pos[2]++)
	        					{						
	        						// check which images are needed for this coordinate
	        						final int num = getImagesAtCoordinate(imageInformationList, indices, pos);
	        						
	        						if (num > 0)
	        						{
	        							// get the pixel values of all images that contribute
	        							for (int i = 0; i < num; i++)
	        							{
	        								if (imageType == ImagePlus.COLOR_RGB)
	        								{
	        									if (dimension == 3)
	        										rgbPixels[i] = getPixelMinRGB(indices[i].imageStack, indices[i].w, indices[i].h, indices[i].d, 
	        											       				  pos[0] - Math.round(indices[i].position[0]), 
	        											       				  pos[1] - Math.round(indices[i].position[1]), 
	        											       				  pos[2] - Math.round(indices[i].position[2]), 0);
	        									else
	        										rgbPixels[i] = getPixelMinRGB(indices[i].imageStack, indices[i].w, indices[i].h, indices[i].d, 
										       				  pos[0] - Math.round(indices[i].position[0]), 
										       				  pos[1] - Math.round(indices[i].position[1]), 
										       				  0, 0);
	        										
	        								}
	        								else
	        								{
	        									if (dimension == 3)
	        										pixels[i] = getPixelMin(imageType, indices[i].imageStack, indices[i].w, indices[i].h, indices[i].d, 
	        						       				  				pos[0] - Math.round(indices[i].position[0]), 
	        						       				  				pos[1] - Math.round(indices[i].position[1]), 
	        						       				  				pos[2] - Math.round(indices[i].position[2]), 0);
	        									else
	        										pixels[i] = getPixelMin(imageType, indices[i].imageStack, indices[i].w, indices[i].h, indices[i].d, 
						       				  				pos[0] - Math.round(indices[i].position[0]), 
						       				  				pos[1] - Math.round(indices[i].position[1]), 
						       				  				0, 0);
	        									//TODO: Remove test
	        									//pixels[i] = (float)Math.pow(2, indices[i].id);
	        								}
	        							}
	        							
	        							// compute the final value for the pixel
	        							if (type == MIN)
	        							{
	        								if (imageType == ImagePlus.COLOR_RGB)
	        								{
	        									finalRGBPixel[0] = getMin(rgbPixels, 0, num);
	        									finalRGBPixel[1] = getMin(rgbPixels, 1, num);
	        									finalRGBPixel[2] = getMin(rgbPixels, 2, num);
	        								}
	        								else
	        								{
	        									finalPixel = getMin(pixels, num);
	        								}								
	        							}
	        							else if (type == AVG)
	        							{
	        								if (imageType == ImagePlus.COLOR_RGB)
	        								{
	        									finalRGBPixel[0] = avg(rgbPixels, 0, num);
	        									finalRGBPixel[1] = avg(rgbPixels, 1, num);
	        									finalRGBPixel[2] = avg(rgbPixels, 2, num);
	        								}
	        								else
	        								{
	        									finalPixel = avg(pixels, num);
	        								}															
	        							}
	        							else // Linear Blending
	        							{
	        								computeLinearWeights(indices, num, pos, weights, tmp, alpha);
	        								if (imageType == ImagePlus.COLOR_RGB)
	        								{
	        									finalRGBPixel[0] = avg(rgbPixels, 0, weights, num);
	        									finalRGBPixel[1] = avg(rgbPixels, 1, weights, num);
	        									finalRGBPixel[2] = avg(rgbPixels, 2, weights, num);
	        								}
	        								else
	        								{
	        									finalPixel = avg(pixels, weights, num);
	        								}
	        							}
	        							
	        							// set the pixel into the volume
	        							if (imageType == ImagePlus.COLOR_RGB)
	        							{
	        								ip[pos[2]].putPixel(pos[0], pos[1], finalRGBPixel);														
	        							}
	        							else
	        							{						
	        								if (imageType == ImagePlus.GRAY8 || imageType == ImagePlus.GRAY16)
	        								{
	        									ip[pos[2]].putPixel(pos[0] , pos[1] , (int) (finalPixel + 0.5));
	        								}
	        								else
	        								{
	        									ip[pos[2]].putPixelValue(pos[0], pos[1], finalPixel);
	        									
	        									synchronized (minmax)
	        									{
		        									if (finalPixel < minmax[0])
		        										minmax[0] = finalPixel;
	        										
		        									if (finalPixel > minmax[1])
		        										minmax[1] = finalPixel;		        									
	        									}
	        								}
	        							}
	        						}
	        					}
	        				
	        				int line = progress.incrementAndGet();
	        				
	        				// only the first Thread redraws
	        				if (myNumber == 0)
	        				{		        				
	        					if ( showOutput )
	        						fusedImp.setTitle(name + " " + line + " of " + imgW );		        				
		        			}
	        				
	        				if (pos[0] % 100 == 0)
	        				{
		        				if (imageType == ImagePlus.GRAY32)
		        					fusedImp.getProcessor().setMinAndMax(minmax[0], minmax[1]);
	        					
		        				if ( showOutput )
		        					fusedImp.updateAndDraw();
	        				}
	        			}
	        			
	                }
	            });
	        
	        CommonFunctions.startAndJoin(threads);	
	        
	        // close all images
	        for (ImageInformation iI : imageInformationList)
	        {
				if (iI.closeAtEnd)
					iI.tmp.close();
				
				iI.tmp = null;
				iI.imageStack = null;	        	
	        }
		}
		
		if (imageType == ImagePlus.GRAY32)
			fusedImp.getProcessor().setMinAndMax(minmax[0], minmax[1]);
		
		fusedImp.setTitle(name);
		fusedImp.updateAndDraw();
		
		if ( cal != null)
			fusedImp.setCalibration(cal);
		
		return fusedImp;
	}
	
	
	protected static Calibration updateCalibration( Calibration cal, Calibration newOne )
	{
		if ( cal == null )
			cal = newOne.copy();
		else
			if ( !cal.equals(newOne) )
			{
				IJ.log( "Calibration mismatch: " );
				IJ.log( "  First   image: " + cal.toString() );
				IJ.log( "  Current image: " + newOne.toString() );						
			}
		
		return cal;
	}
	
	final private static void computeLinearWeights(final ImageInformation[] indices, final int num, final int[] pos, final float[] weights, final int[] minDistance, final double alpha)
	{
		if (num == 1)
		{
			weights[0] = 1;
			return;
		}
		
		// compute the minimal distance to the border for each image
		float sumInverseWeights = 0;
		for (int i = 0; i < num; i++)
		{
			final ImageInformation iI = indices[i];
			
			minDistance[i] = 1;
			for (int dim = 0; dim < iI.dim; dim++)
			{
				final int localImgPos = pos[dim] - Math.round(iI.position[dim]);
				final int value = Math.min(localImgPos, Math.round(iI.size[dim]) - localImgPos - 1) + 1;
				minDistance[i] *= value;
			}
			
			// the distance to the image, so always +1
			minDistance[i]++;
			
			weights[i] = (float)Math.pow(minDistance[i], alpha);
			sumInverseWeights += weights[i];
		}

		// norm them so that the integral is 1
		for (int i = 0; i < num; i++)
			weights[i] /= sumInverseWeights;
	}

	/*
	final private void computeLinearWeights(final ImageInformation[] indices, final int num, final int[] pos, final float[] weights, final int[] minDistance)
	{
		if (num == 1)
		{
			weights[0] = 1;
			return;
		}
		
		// compute the minimal distance to the border for each image
		float sumWeights = 0;
		for (int i = 0; i < num; i++)
		{
			final ImageInformation iI = indices[i];
			
			minDistance[i] = 1;
			for (int dim = 0; dim < iI.dim; dim++)
			{
				final int localImgPos = pos[dim] - Math.round(iI.position[dim]);
				final int value = Math.min(localImgPos, Math.round(iI.size[dim]) - localImgPos - 1) + 1;
				minDistance[i] *= value;
			}
			
			// the distance to the image, so always +1
			minDistance[i]++;

			//if (minDistance[i] < 1)
				//IJ.error("multdistance = " + minDistance[i] + " This is < 1, cannot be true!" + pos[0] + " " + pos[1] + " " + pos[2]);			

			sumWeights += minDistance[i];
		}
	
		// now compute the weights for each image
		for (int i = 0; i < num; i++)
		{			
			// get the sum of distances from all other images
			int sumDistance = 0;
			for (int j = 0; j < num; j++)
				if (j != i)
					sumDistance += minDistance[j];
			weights[i] = sumDistance;
		}		
		
		// compute inverse weight
		float sumInverseWeights = 0;
		for (int i = 0; i < num; i++)
		{
			weights[i] = (float)Math.pow(sumWeights - weights[i], alpha);
			sumInverseWeights += weights[i];
		}

		// norm them so that the integral is 1
		for (int i = 0; i < num; i++)
			weights[i] /= sumInverseWeights;
	}
	 */
	final private static int avg(final int[][] values, final int channel, final float[] weights, final int num)
	{
		if (num == 0)
			return 0;

		double avg = 0.0;		

		// the integral of the weights is 1
		for (int i = 0; i < num; i++)
			avg += values[i][channel]*weights[i];
		
		return (int)(avg + 0.5);
	}

	final private static float avg(final float[] values, final float[] weights, final int num)
	{
		if (num == 0)
			return 0;

		double avg = 0.0;		
		
		// the integral of the weights is 1
		for (int i = 0; i < num; i++)
			avg += values[i]*weights[i];

		
		return (float)(avg);
	}

	final private static int avg(final int[][] values, final int channel, final int num)
	{
		if (num == 0)
			return 0;

		double avg = 0.0;		
		
		for (int i = 0; i < num; i++)
			avg += values[i][channel];
		
		return (int)((avg/(double)num) + 0.5);
	}
	
	final private static float avg(final float[] values, final int num)
	{
		if (num == 0)
			return 0;

		double avg = 0.0;		

		for (int i = 0; i < num; i++)
			avg += values[i];
		
		return (float)(avg/(double)num);
	}

	final private static int getMin(final int[][] values, final int channel, final int num)
	{
		if (num == 0)
			return 0;

		int min = Integer.MAX_VALUE;		
		
		for (int i = 0; i < num; i++)
			if (values[i][channel] < min)
				min = values[i][channel];
		
		return min;
	}

	final private static float getMin(final float[] values, final int num)
	{
		if (num == 0)
			return 0;

		float min = values[0];
		
		for (int i = 0; i < num; i++)
			if (values[i] < min)
				min = values[i];
		
		return min;
	}

	final private static int round( final float value )
	{
		return (int)( value + (0.5f * Math.signum( value ) ) );
	}	

	final private static int getImagesAtCoordinate(final ArrayList<ImageInformation> imageInformationList, final ImageInformation indices[], final int[] pos)
	{
		int num = 0;
		
		for (final ImageInformation iI : imageInformationList)
		{
			// check if pixel is inside the image
			boolean isInside = true;
			for (int dim = 0; dim < iI.dim && isInside; dim++)
				if ( !(pos[dim] >= round(iI.position[dim]) && pos[dim] < round(iI.position[dim] + iI.size[dim]) ) )
					isInside = false;
			
			if (isInside)
			{
				//TODO: Thread safe loading
				// is the image loaded?
				/*if (iI.tmp == null)
				{
					if (iI.imp == null)
						iI.tmp = new Opener().openImage(iI.imageName);
					else
						iI.tmp = iI.imp;		
					
					iI.imageStack = iI.tmp.getStack().getImageArray();
					iI.w = iI.tmp.getStack().getWidth();
					iI.h = iI.tmp.getStack().getHeight();
					iI.d = iI.tmp.getStack().getSize();
				}*/
				
				indices[num++] = iI;
			}
			else // check if we can unload the image
			{
				//TODO: Thread safe unloading
				
				// if loaded
				/*if (iI.tmp != null)
				{
					// check if it will not be needed again (x, y bigger)
					boolean needed = true;
					
					for (int dim = 0; dim < 2 && needed; dim++)
						if ( pos[dim] < Math.round(iI.position[dim] + iI.size[dim]) )
							needed = false;
					
					if (needed)
					{
						if (iI.imp == null)
							iI.tmp.close();
						
						iI.tmp = null;
						iI.imageStack = null;
					}
					
					
				}*/
			}
			
		}
		return num;
	}
	
	protected static float[] getAndApplyMinMax(final ArrayList<ImageInformation> imageInformationList, final int dim)
	{
		float min[] = new float[dim];
		float max[] = new float[dim];
		for (int i = 0; i < min.length; i++)
		{
			min[i] = Float.MAX_VALUE;
			max[i] = Float.MIN_VALUE;
		}
		
		for (ImageInformation iI : imageInformationList)
			for (int i = 0; i < min.length; i++)
			{
				if (iI.position[i] < min[i])
					min[i] = iI.position[i];
			
				if (iI.position[i] + iI.size[i] > max[i])
					max[i] = iI.position[i] + iI.size[i]; 
			}
		
		for (ImageInformation iI : imageInformationList)
			for (int i = 0; i < min.length; i++)
				iI.position[i] -= min[i]; 

		for (int i = 0; i < min.length; i++)
		{
			max[i] -= min[i];
			min[i] = 0;
		}

		return max;
	}
	
	private ArrayList<ImageInformation> optimize(final ArrayList<OverlapProperties> overlappingTiles, final ImageInformation firstImage)
	{
		boolean redo;
		TileConfiguration tc;
		do
		{
			redo = false;
			ArrayList< Tile > tiles = new ArrayList< Tile >();
			for (final OverlapProperties o : overlappingTiles)
			{
				if (o.R < thresholdR)
					o.validOverlap = false;
	
				if (o.validOverlap)
				{
					Tile t1 = o.i1;
					Tile t2 = o.i2;
					
					Point p1, p2;
					
					if (dim == 3)
					{
						p1 = new Point(new float[]{0,0,0});
						p2 = new Point(new float[]{o.translation3D.x, o.translation3D.y, o.translation3D.z});
					}
					else 
					{
						p1 = new Point(new float[]{0,0});
						p2 = new Point(new float[]{o.translation2D.x, o.translation2D.y});						
					}
					
					t1.addMatch( new PointMatch( p2, p1, (float)o.R, o ) );
					t2.addMatch( new PointMatch( p1, p2, (float)o.R, o ) );
					t1.addConnectedTile( t2 );
					t2.addConnectedTile( t1 );
					
					if (!tiles.contains(t1))
						tiles.add( t1 );
	
					if (!tiles.contains(t2))
						tiles.add( t2 );				
				}
			}
			IJ.log("Tile size: " + tiles.size());
			
			if( tiles.size() == 0 )
			{
				IJ.log("Error: No correlated tiles found, setting the first tile to (0,0,0).");
				
				for ( int d = 0; d < firstImage.position.length; ++d )
					firstImage.position[ d ] = 0;
				
				ArrayList<ImageInformation> imageInformationList = new ArrayList<ImageInformation>();
				imageInformationList.add( firstImage );
				
				IJ.log(" image information list size =" + imageInformationList.size());
				
				return imageInformationList;
			}						
			
			// trash everything but the largest graph			
			final ArrayList< ArrayList< Tile > > graphs = Tile.identifyConnectedGraphs( tiles );
			IJ.log("Number of tile graphs = " + graphs.size());
			
			/*
			for ( final ArrayList< Tile > graph : graphs )
			{
				IJ.log("graph size = " + graph.size());
				if ( graph.size() < 30 )
				{
					tiles.removeAll( graph );
					IJ.log("-removed");
				}
				else
				{
					IJ.log("-kept");
					tc.fixTile( graph.get( 0 ) );
				}
			}
			
			*/
			ArrayList< Tile > largestGraph = new ArrayList< Tile >();
			for ( final ArrayList< Tile > graph : graphs )
			{
				
				if ( graph.size() > largestGraph.size() )
				{
					largestGraph = graph;
				}
				
				//IJ.log("graph size = " + graph.size());
			}
			
			for ( final ArrayList< Tile > graph : graphs )
				if ( graph != largestGraph )
				{
					tiles.removeAll( graph );
					//IJ.log(" Removed graph of size " + graph.size());
				}
			/*
			for ( final ArrayList< Tile > graph : graphs )
				   tc.fixTile( graph.get( 0 ) );
			*/
			
			
			tc = new TileConfiguration();
			tc.addTiles( tiles );
			tc.fixTile( tiles.get( 0 ) );						
			
			//IJ.log(" tiles size =" + tiles.size());
			//IJ.log(" tc.getTiles() size =" + tc.getTiles().size());
			
			try
			{
				tc.optimize( 10, 10000, 200 );
	
				double avgError = tc.getAvgError();
				double maxError = tc.getMaxError();				
				
				if ( (avgError*thresholdDisplacementRelative < maxError && maxError > 0.75) || avgError > thresholdDisplacementAbsolute)
				{
					IJ.log("maxError more than " + thresholdDisplacementRelative + " times bigger than avgerror.");
					Tile worstTile = tc.getWorstError();
					ArrayList< PointMatch > matches = worstTile.getMatches();
					
					float longestDisplacement = Float.MIN_VALUE;
					PointMatch worstMatch = null;
					
					for (PointMatch p : matches)
						if (p.getDistance() > longestDisplacement)
						{
							longestDisplacement = p.getDistance();
							worstMatch = p;
						}
					
					IJ.log("Identified link between " + worstMatch.o.i1.imageName + " and " + worstMatch.o.i2.imageName + " (R=" + worstMatch.o.R +") to be bad. Reoptimizing.");
					worstMatch.o.validOverlap = false;
					redo = true;
					
					for (Tile t: tiles)
						t.resetTile();
				}
			}
			catch ( NotEnoughDataPointsException e ){ e.printStackTrace(); }
		}
		while(redo);
		
		
		// create a list of image informations containing their positions			
		ArrayList<ImageInformation> imageInformationList = new ArrayList<ImageInformation>();
		for ( Tile t : tc.getTiles() )
		{
			if (dim == 3)
				((ImageInformation)t).position = (( TranslationModel3D ) t.getModel() ).getTranslation().clone();
			else
				((ImageInformation)t).position = (( TranslationModel2D ) t.getModel() ).getTranslation().clone();
			
			imageInformationList.add((ImageInformation)t);
		}		
		Collections.sort(imageInformationList);
		
		
		IJ.log(" image information list size =" + imageInformationList.size());
		
		return imageInformationList;
	}
	
	/**
	 * Compute phase correlation between overlapping tiles
	 * 
	 * @param overlappingTiles list of overlapping tiles
	 * @param handleRGB RGB mode (@see stitching.CommonFunctions.colorList)
	 */
	private void computePhaseCorrelations(final ArrayList<OverlapProperties> overlappingTiles, String handleRGB)
	{
		for (final OverlapProperties o : overlappingTiles)
		{
			final ImagePlus imp1, imp2;

			if (o.i1.imp == null)
			{
				imp1 = CommonFunctions.loadImage("", o.i1.imageName, o.i1.seriesNumber, rgbOrder);
				o.i1.closeAtEnd = true;
			}
			else
				imp1 = o.i1.imp;

			if (o.i2.imp == null)
			{
				imp2 = CommonFunctions.loadImage("", o.i2.imageName, o.i2.seriesNumber, rgbOrder);
				o.i2.closeAtEnd = true;
			}
			else
			{
				imp2 = o.i2.imp;
			}
			
			// where do we overlap?
			setROI(imp1, o.i1, o.i2);
			setROI(imp2, o.i2, o.i1);
			
			//imp1.show();
			//imp2.show();
			
			if (dim == 3)
			{
				final Stitching_3D stitch = new Stitching_3D();
				stitch.checkPeaks = 5;
				stitch.coregister = false;
				stitch.fusedImageName = "Fused " + imp1.getTitle() + " " + imp2.getTitle();
				stitch.fuseImages = false;
				stitch.handleRGB1 = handleRGB;
				stitch.handleRGB2 = handleRGB;				
				stitch.imgStack1 = imp1.getTitle();				
				stitch.imgStack2 = imp2.getTitle();
				stitch.imp1 = imp1;
				stitch.imp2 = imp2;
				stitch.doLogging = false;
				stitch.computeOverlap = true;
				
				try
				{
					stitch.work();
					
					o.R = stitch.getCrossCorrelationResult().R;
					o.translation3D = stitch.getTranslation();
				}
				catch (Exception e)
				{
					o.R = -1;
					o.translation3D = new Point3D(0,0,0);
				}
								
				IJ.log(o.i1.id + " overlaps " + o.i2.id + ": " + o.R + " translation: " + o.translation3D);
			}
			else if (dim == 2)
			{
				final Stitching_2D stitch = new Stitching_2D();
				stitch.checkPeaks = 5;
				stitch.fusedImageName = "Fused " + imp1.getTitle() + " " + imp2.getTitle();
				stitch.fuseImages = false;
				stitch.handleRGB1 = handleRGB;
				stitch.handleRGB2 = handleRGB;
				stitch.image1 = imp1.getTitle();
				stitch.image2 = imp2.getTitle();
				
				//final ImagePlus imp1b =  new ImagePlus("Imp1 B", imp1.getProcessor().duplicate());
				final ImageProcessor ip1 = imp1.getProcessor().duplicate();
				IJ.run(imp1, "Enhance Contrast", "saturated=0.1 normalize");				
				stitch.imp1 = imp1;
				
				//final ImagePlus imp2b = new ImagePlus("Imp2 B", imp2.getProcessor().duplicate());
				
				final ImageProcessor ip2 = imp2.getProcessor().duplicate();
				IJ.run(imp2, "Enhance Contrast", "saturated=0.1 normalize");				
				stitch.imp2 = imp2;
				
				stitch.doLogging = false;
				stitch.computeOverlap = true;
				
				try
				{
					stitch.work();
					
					o.R = stitch.getCrossCorrelationResult().R;
					o.translation2D = stitch.getTranslation();
				}
				catch (Exception e)
				{
					o.R = -1;
					o.translation2D = new Point2D(0, 0);
				}
				
				imp1.setProcessor(imp1.getTitle(), ip1);
				imp2.setProcessor(imp2.getTitle(), ip2);
								
				IJ.log(o.i1.id + " overlaps " + o.i2.id + ": " + o.R + " translation: " + o.translation2D);
			}
			else 
			{
				IJ.error("Dimensionality of images: " + dim  + " is not supported yet.");
				return;
			}
		}
	}
	
	private void setROI(final ImagePlus imp, final ImageInformation i1, final ImageInformation i2)
	{
		final int start[] = new int[2], end[] = new int[2];
		
		for (int dim = 0; dim < 2; dim++)
		{			
			// begin of 2 lies inside 1
			if (i2.offset[dim] >= i1.offset[dim] && i2.offset[dim] <= i1.offset[dim] + i1.size[dim])
			{
				start[dim] = Math.round(i2.offset[dim] - i1.offset[dim]);
				
				// end of 2 lies inside 1
				if (i2.offset[dim] + i2.size[dim] <= i1.offset[dim] + i1.size[dim])
					end[dim] = Math.round(i2.offset[dim] + i2.size[dim] - i1.offset[dim]);
				else
					end[dim] = Math.round(i1.size[dim]);
			}
			else if (i2.offset[dim] + i2.size[dim] <= i1.offset[dim] + i1.size[dim]) // end of 2 lies inside 1
			{
				start[dim] = 0;
				end[dim] = Math.round(i2.offset[dim] + i2.size[dim] - i1.offset[dim]);
			}
			else // if both outside then the whole image 
			{
				start[dim] = -1;
				end[dim] = -1;
			}
		}
					
		imp.setRoi(new Rectangle(start[0], start[1], end[0] - start[0], end[1] - start[1]));		
	}
	
	private ArrayList<OverlapProperties> findOverlappingTiles(final ArrayList<ImageInformation> imageInformationList, final boolean createPreview, final String fusionMethod)
	{
		final ZProjector zp = new ZProjector();

		int endX = 0, endY = 0, startX = 0, startY = 0;
		int count = 0;
		for (final ImageInformation iI : imageInformationList)
		{
			if (iI.imp == null)
			{
				iI.imp = CommonFunctions.loadImage("", iI.imageName, iI.seriesNumber, rgbOrder);
				iI.closeAtEnd = true;
			}
			else
			{
				iI.closeAtEnd = false;
			}
			
			if (iI.imp == null)
			{
				IJ.log("Cannot load " + iI.imageName + " ignoring.");
				iI.invalid = true;
				continue;
			}
			
			// 3D
			if (iI.imp.getStackSize() > 1)
			{
				iI.size[0] = iI.imp.getWidth();
				iI.size[1] = iI.imp.getHeight();
				iI.size[2] = iI.imp.getStackSize();
			}
			else // 2D
			{
				iI.size[0] = iI.imp.getWidth();
				iI.size[1] = iI.imp.getHeight();				
			}
			
			iI.imageType = iI.imp.getType();
			
			if (createPreview)
			{
				if (iI.imp.getStackSize() > 1)
				{
					zp.setMethod(ZProjector.MAX_METHOD);
					zp.setImage(iI.imp);
					if (iI.imp.getType() == ImagePlus.COLOR_RGB || iI.imp.getType() == ImagePlus.COLOR_256)
					{
						zp.doRGBProjection();
						iI.maxIntensity = zp.getProjection();
						iI.maxIntensity.setProcessor(iI.maxIntensity.getTitle(), iI.maxIntensity.getProcessor().convertToFloat());
					}
					else
					{
						zp.doProjection();
						iI.maxIntensity = zp.getProjection();
						iI.maxIntensity.setProcessor(iI.maxIntensity.getTitle(), iI.maxIntensity.getProcessor().convertToFloat());
					}
				}
				else
				{
					iI.maxIntensity = new ImagePlus(iI.imp.getTitle(), iI.imp.getProcessor().convertToFloat());
				}
				
				if (count++ == 0)
				{
					startX = Math.round(iI.offset[0]);
					startY = Math.round(iI.offset[1]);
					endX = startX + iI.maxIntensity.getWidth();
					endY = startY + iI.maxIntensity.getHeight();
				}
				else
				{
					if (Math.round(iI.offset[0]) < startX)
						startX = Math.round(iI.offset[0]);
					
					if (Math.round(iI.offset[1]) < startY)
						startY = Math.round(iI.offset[1]);
					
					if (Math.round(iI.offset[0]) + iI.maxIntensity.getWidth() > endX)
						endX = Math.round(iI.offset[0]) + iI.maxIntensity.getWidth();
					
					if (Math.round(iI.offset[1]) + iI.maxIntensity.getHeight() > endY)
						endY = Math.round(iI.offset[1]) + iI.maxIntensity.getHeight();
				}
			}
			
			if (fusionMethod.equals(methodListCollection[MAX]) && iI.closeAtEnd)
				iI.imp.close();
		}
		
		for (int i = 0; i < imageInformationList.size();)
		{
			ImageInformation iI = imageInformationList.get(i);
			if (iI.invalid)
			{
				imageInformationList.remove(i);
				IJ.log("Removed: " + iI.imageName);
			}
			else
				i++;
		}
		
		// get the connecting tiles
		ArrayList<OverlapProperties> overlappingTiles = new ArrayList<OverlapProperties>();

		for (int i = 0; i < imageInformationList.size() - 1; i++)
			for (int j = i + 1; j < imageInformationList.size(); j++)
			{
				ImageInformation i1 = imageInformationList.get(i);
				ImageInformation i2 = imageInformationList.get(j);
				
				boolean overlapping = true;
				
				for (int dim = 0; dim < i1.dim; dim++)
					if ( !((i2.offset[dim] >= i1.offset[dim] && i2.offset[dim] <= i1.offset[dim] + i1.size[dim]) || 
						   (i2.offset[dim] + i2.size[dim] >= i1.offset[dim] && i2.offset[dim] + i2.size[dim] <= i1.offset[dim] + i1.size[dim]) ||
						   (i2.offset[dim] <= i1.offset[dim] && i2.offset[dim] >= i1.offset[dim] + i1.size[dim])) )
						overlapping = false;
				
				if (overlapping)
				{
					OverlapProperties o = new OverlapProperties();
					o.i1 = i1; 
					o.i2 = i2;
					overlappingTiles.add(o);
					i1.overlaps = true;
					i2.overlaps = true;
				}
			}
		
		// create the preview
		if (createPreview)
		{
			System.out.println("startX: " + startX + " startY: " + startY);
			System.out.println("endX: " + endX + " endY: " + endY);
			
			final FloatProcessor out = new FloatProcessor(endX - startX, endY - startY);
			
			for (final ImageInformation iI : imageInformationList)
			{
				final FloatProcessor fp = (FloatProcessor)iI.maxIntensity.getProcessor();
				
				for (int y = 0; y < fp.getHeight(); y++)
					for (int x = 0; x < fp.getWidth(); x++)
					{
						final float newValue = fp.getPixelValue(x, y);
						final float oldValue = out.getPixelValue(x - startX + (int)Math.round(iI.offset[0]), y - startY + (int)Math.round(iI.offset[1]));
						out.putPixelValue(x - startX + (int)Math.round(iI.offset[0]), y - startY + (int)Math.round(iI.offset[1]), Math.max(newValue, oldValue));
					}	
			}
			
			final ImagePlus preview = new ImagePlus("Preview", out);
			preview.getProcessor().resetMinAndMax();
			preview.show();
		}
		
		return overlappingTiles;
	}
		
	private ArrayList<ImageInformation> readLayoutFile(String fileName)
	{
		ArrayList<ImageInformation> imageInformationList = new ArrayList<ImageInformation>();

		try
		{
			BufferedReader in = openFileRead(fileName);
			int lineNo = 0;
			
			while (in.ready())
			{
				String line = in.readLine().trim();
				lineNo++;
				if (!line.startsWith("#") && line.length() > 3)
				{
					if (line.startsWith("dim"))
					{
						String entries[] = line.split("=");
						if (entries.length != 2)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + " does not look like [ dim = n ]: " + line);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + " does not look like [ dim = n ]: " + line);
							return null;						
						}
						
						try
						{
							dim = Integer.parseInt(entries[1].trim());
						}
						catch (NumberFormatException e)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Cannot parse dimensionality: " + entries[1].trim());
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Cannot parse dimensionality: " + entries[1].trim());
							return null;														
						}
					}
					else
					{
						if (dim < 0)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Header missing, should look like [dim = n], but first line is: " + line);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Header missing, should look like [dim = n], but first line is: " + line);
							return null;							
						}
						
						if (dim < 2 || dim > 3)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": only dimensions of 2 and 3 are supported: " + line);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": only dimensions of 2 and 3 are supported: " + line);
							return null;							
						}
						
						// read image tiles
						String entries[] = line.split(";");
						if (entries.length != 3)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + " does not have 3 entries! [fileName; ImagePlus; (x,y,...)]");
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + " does not have 3 entries! [fileName; ImagePlus; (x,y,...)]");
							return null;						
						}
						String imageName = entries[0].trim();
						String imp = entries[1].trim();
						
						if (imageName.length() == 0 && imp.length() == 0)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": You have to give a filename or a ImagePlus [fileName; ImagePlus; (x,y,...)]: " + line);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": You have to give a filename or a ImagePlus [fileName; ImagePlus; (x,y,...)]: " + line);
							return null;						
						}
						
						String point = entries[2].trim();
						if (!point.startsWith("(") || !point.endsWith(")"))
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,...): " + point);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,...): " + point);
							return null;
						}
						
						point = point.substring(1, point.length() - 1);
						String points[] = point.split(",");
						if (points.length != dim)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,z,..), dim = " + dim + ": " + point);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,z,...), dim = " + dim + ": " + point);
							return null;
						}
						
						ImageInformation imageInformation;
								
						if (dim == 3)
							imageInformation = new ImageInformation(dim, imageInformationList.size(), new TranslationModel3D());
						else
							imageInformation = new ImageInformation(dim, imageInformationList.size(), new TranslationModel2D());
						
						imageInformation.imageName = imageName;
						
						if ( imageInformation.imageName.contains( "(((" ) && imageInformation.imageName.contains( ")))" ) )
						{
							// it is a multiseries file
							int index1 = imageInformation.imageName.indexOf( "(((" );
							int index2 = imageInformation.imageName.indexOf( ")))" );
							
							String seriesString = imageInformation.imageName.substring( index1 + 3, index2 );
							imageInformation.seriesNumber = Integer.parseInt( seriesString );
							imageInformation.imageName = imageInformation.imageName.substring( 0, index1 );
						}
						
						if (imp.length() > 0)
							imageInformation.imp = WindowManager.getImage(imp);
						else
							imageInformation.imp = null;
						
						if (imageInformation.imp == null && imp.length() > 0)
						{
							System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Cannot find ImagePlus, is not opened: " + imp);
							IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Cannot find ImagePlus, is not opened: " + imp);
							return null;						
						}
						
						for (int i = 0; i < dim; i++)
						{
							try
							{
								imageInformation.offset[i] = Float.parseFloat(points[i].trim()); 
								imageInformation.position[i] = imageInformation.offset[i]; 
							}
							catch (NumberFormatException e)
							{
								System.err.println("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Cannot parse number: " + points[i].trim());
								IJ.error("Stitch_Many_Cubes.readLayoutFile: Line " + lineNo + ": Cannot parse number: " + points[i].trim());
								return null;							
							}
						}
						imageInformationList.add(imageInformation);
					}
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Stitch_Many_Cubes.readLayoutFile: " + e);
			IJ.error("Stitch_Many_Cubes.readLayoutFile: " + e);
			return null;
		};
		
		return imageInformationList;
	}

	public static BufferedReader openFileRead(String fileName)
	{
	  BufferedReader inputFile;
	  try
	  {
		inputFile = new BufferedReader(new FileReader(fileName));
	  }
	  catch (IOException e)
	  {
		System.err.println("Stitch_Many_Cubes.openFileRead(): " + e);
		inputFile = null;
	  }
	  return(inputFile);
	}
}
