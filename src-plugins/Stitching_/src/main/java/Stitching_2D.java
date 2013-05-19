import static stitching.CommonFunctions.*;

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

import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.process.*;
import ij.gui.Roi;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Iterator;

import stitching.CrossCorrelationResult2D;
import stitching.FloatArray2D;
import stitching.ImageInformation;
import stitching.Point2D;

public class Stitching_2D implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	public String image1, image2, fusedImageName;
	
	public static String methodStatic = methodList[1];
	public static String handleRGB1Static = colorList[colorList.length - 1];
	public static String handleRGB2Static = colorList[colorList.length - 1];
	public static boolean fuseImagesStatic = true, windowingStatic = true, computeOverlapStatic = true;
	public static int checkPeaksStatic = 5;
	public static double alphaStatic = 1.5;
	public static int xOffsetStatic = 0, yOffsetStatic = 0;
	
	public String method = methodList[1];
	public String handleRGB1= colorList[colorList.length - 1];
	public String handleRGB2= colorList[colorList.length - 1];
	public boolean fuseImages= true, windowing = true, computeOverlap = true;
	public int checkPeaks= 5;
	public double alpha= 1.5;
	public int xOffset = 0, yOffset = 0;
	
	public boolean doLogging = true;
	public ImagePlus imp1 = null, imp2 = null;
	
	public Point2D shift = null;	
	private CrossCorrelationResult2D[] result = null;
	
	public Point2D getTranslation() { return shift.clone(); }
	public CrossCorrelationResult2D[] getCrossCorrelationResults() { return result; }
	public CrossCorrelationResult2D getCrossCorrelationResult() { return result[0].clone(); }
	

	// a macro can call to only fuse two images given a certain shift
	private Point2D translation = null;

	public void run(String args)
	{
		// get list of image stacks
		int[] idList = WindowManager.getIDList();

		if (idList == null)
		{
			IJ.error("You need two open images.");
			return;
		}

		int nostacks = 0;
		for (int i = 0; i < idList.length; i++)
			if (WindowManager.getImage(idList[i]).getStackSize() == 1)
				nostacks++;

		if (nostacks < 2)
		{
			IJ.error("You need two open images (no stacks).");
			return;
		}

		String[] nostackList = new String[nostacks];
		int[] nostackIDs = new int[nostacks];
		nostacks = 0;

		for (int i = 0; i < idList.length; i++)
		{
			if (WindowManager.getImage(idList[i]).getStackSize() == 1)
			{
				nostackList[nostacks] = WindowManager.getImage(idList[i]).getTitle();
				nostackIDs[nostacks] = idList[i];
				++nostacks;
			}
		}
		
		// create generic dialog
		GenericDialog gd = new GenericDialog("Stitching of 2D Images");
		gd.addChoice("First_image (reference)", nostackList, nostackList[0]);
		gd.addChoice("Use_Channel_for_First", colorList, handleRGB1Static);
		enableChannelChoice((Choice) gd.getChoices().get(0), (Choice) gd.getChoices().get(1), nostackIDs);

		gd.addChoice("Second_image (to register)", nostackList, nostackList[1]);
		gd.addChoice("Use_Channel_for_Second", colorList, handleRGB2Static);
		enableChannelChoice((Choice) gd.getChoices().get(2), (Choice) gd.getChoices().get(3), nostackIDs);

		gd.addCheckbox("Use_windowing", windowingStatic);
		gd.addNumericField("How_many_peaks should be checked", checkPeaksStatic, 0);
		gd.addMessage("");
		gd.addCheckbox("Create_merged_image (fusion)", fuseImagesStatic);
		gd.addChoice("Fusion_method", methodList, methodStatic);
		gd.addNumericField("Fusion_alpha", alphaStatic, 2);		
		gd.addStringField("Fused_image name: ", "Fused_" + nostackList[0] + "_" + nostackList[1]);
		gd.addCheckbox("compute_overlap", computeOverlapStatic );
		gd.addNumericField("x", xOffset, xOffsetStatic);
		gd.addNumericField("y", yOffset, yOffsetStatic);
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);
		
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		this.image1 = gd.getNextChoice();
		handleRGB1Static = gd.getNextChoice();
		this.image2 = gd.getNextChoice();
		handleRGB2Static = gd.getNextChoice();
		this.imp1 = WindowManager.getImage(nostackIDs[((Choice) gd.getChoices().get(0)).getSelectedIndex()]);
		this.imp2 = WindowManager.getImage(nostackIDs[((Choice) gd.getChoices().get(2)).getSelectedIndex()]);
		windowingStatic = gd.getNextBoolean();
		checkPeaksStatic = (int) gd.getNextNumber();
		fuseImagesStatic = gd.getNextBoolean();
		methodStatic = gd.getNextChoice();
		alphaStatic = gd.getNextNumber();
		this.fusedImageName = gd.getNextString();
		computeOverlapStatic = gd.getNextBoolean();
		xOffsetStatic = (int)Math.round( gd.getNextNumber() );
		yOffsetStatic = (int)Math.round( gd.getNextNumber() );

		method = methodStatic;
		handleRGB1= handleRGB1Static;
		handleRGB2= handleRGB2Static;
		fuseImages = fuseImagesStatic;
		windowing = windowingStatic;
		checkPeaks = checkPeaksStatic;
		alpha = alphaStatic;
		computeOverlap = computeOverlapStatic;
		xOffset = xOffsetStatic;
		yOffset = yOffsetStatic;
		
		
		if ( !computeOverlap )
			this.translation = new Point2D( xOffset, yOffset );
		else
			this.translation = null;
		
		//
		// determine wheater a macro called it which limits in determining name
		// clashes
		//
		boolean calledFromMacro = false;

		if (!(imp1.getTitle().equals(image1) && imp2.getTitle().equals(image2)))
		{
			calledFromMacro = true;
			imp1 = WindowManager.getImage(image1);
			imp2 = WindowManager.getImage(image2);
		}

		if (!calledFromMacro && nostackIDs[((Choice) gd.getChoices().get(0)).getSelectedIndex()] == nostackIDs[((Choice) gd.getChoices().get(2)).getSelectedIndex()])
		{
			IJ.error("You selected the same stack twice. Stopping.");
			return;
		}

		if (fuseImages)
		{
			if (imp1.getType() != imp2.getType())
			{
				IJ.error("The Image Stacks are of a different type, it is unclear how to fuse them. Stopping.");
				return;
			}

			if ((imp1.getType() == ImagePlus.COLOR_RGB || imp1.getType() == ImagePlus.COLOR_256) && method.equals(methodList[RED_CYAN]))
			{
				IJ.log("Warning: Red-Cyan Overlay is not possible for RGB images, reducing images to Single Channel data.");
			}
		}
		
		work();
	}

	private final void enableChannelChoice(final Choice controller, final Choice target, final int[] nostackIDs)
	{
		setRGB(nostackIDs[controller.getSelectedIndex()], target);

		controller.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				setRGB(nostackIDs[controller.getSelectedIndex()], target);
			}
		});
	}

	private final void setRGB(final int imagejID, final Choice target)
	{
		if (WindowManager.getImage(imagejID).getType() == ImagePlus.COLOR_RGB || WindowManager.getImage(imagejID).getType() == ImagePlus.COLOR_256)
			target.setEnabled(true);
		else		
			target.setEnabled(false);
	}
	
	private ImagePlus getImage(String img)
	{
		ImagePlus imp = null;

		try
		{
			imp = WindowManager.getImage(img);
		} catch (Exception e)
		{
			IJ.error("Could not find image: " + e);
		};

		return imp;
	}

	public Stitching_2D()
	{
	}

	public Stitching_2D(boolean windowing1, int checkPeaks1, boolean fuseImages1, String method1, String fusedImageName1)
	{
		windowing = windowing1;
		fuseImages = fuseImages1;
		method = method1;
		fusedImageName = fusedImageName1;
		checkPeaks = checkPeaks1;
	}

	public void work()
	{
		work(null, null);
	}

	public void work(FloatArray2D inputImage1, FloatArray2D inputImage2)
	{
		FloatArray2D img1 = null, img2 = null, fft1, fft2;
		ImagePlus imp1, imp2;

		Point2D img1Dim = new Point2D(0, 0), img2Dim = new Point2D(0, 0), // (incl. possible ext!!!)
				ext1Dim = new Point2D(0, 0), ext2Dim = new Point2D(0, 0),
				maxDim;

		// make it also executable as non-plugin/macro
		if (inputImage1 == null || inputImage2 == null)
		{
			// get images
			if (this.imp1 == null) imp1 = getImage(image1);
			else imp1 = this.imp1;

			if (this.imp2 == null) imp2 = getImage(image2);
			else imp2 = this.imp2;

			if (imp1 == null || imp2 == null)
				return;

			// check for ROIs in images and whether they are valid
			if (!checkRoi(imp1, imp2))
				return;

			// apply ROIs if they are there and save dimensions of original images and size increases
			// images and size increases
			if (this.translation == null)
			{
				img1 = applyROI(imp1, img1Dim, ext1Dim, handleRGB1, windowing);
				img2 = applyROI(imp2, img2Dim, ext2Dim, handleRGB2, windowing);
			}
		}
		else
		{
			img1 = inputImage1;
			img2 = inputImage2;

			imp1 = FloatArrayToImagePlus(img1, "Image1", 0, 0);
			imp2 = FloatArrayToImagePlus(img2, "Image2", 0, 0);

			img1Dim.x = img1.width;
			img1Dim.y = img1.height;
			img2Dim.x = img2.width;
			img2Dim.y = img2.height;
		}

		if (this.translation == null)
		{

			// apply windowing
			if (windowing)
			{
				exponentialWindow(img1);
				exponentialWindow(img2);
			}
	
			// zero pad images to fft-able size
			FloatArray2D[] zeropadded = zeroPadImages(img1, img2);
			img1 = zeropadded[0];
			img2 = zeropadded[1];
	
			/*
			   FloatArrayToImagePlus(img1, "1", 0, 0).show();
			   FloatArrayToImagePlus(img2, "2", 0, 0).show();
			 */
	
			// save dimensions of zeropadded image
			maxDim = new Point2D(img1.width, img1.height);
	
			// compute FFT's
			fft1 = computeFFT(img1);
			fft2 = computeFFT(img2);
	
			// do the phase correlation
			FloatArray2D invPCM = computePhaseCorrelationMatrix(fft1, fft2, maxDim.x);
			//FloatArrayToImagePlus(invPCM, "invpcm", 0, 0).show();
	
			// find the peaks
			ArrayList<Point2D> peaks = findPeaks(invPCM, img1Dim, img2Dim, ext1Dim, ext2Dim, checkPeaks);
	
			// get the original images
			img1 = applyROI(imp1, img1Dim, ext1Dim, handleRGB1, false /*no windowing of course*/);
			img2 = applyROI(imp2, img2Dim, ext2Dim, handleRGB2, false /*no windowing of course*/);
	
			// test peaks
			result = testCrossCorrelation(invPCM, peaks, img1, img2);
	
			// get shift of images relative to each other
			shift = getImageOffset(result[0], imp1, imp2);
		}
		else
		{
			shift = translation;
		}
		
		if (fuseImages)
		{
			// merge if wanted
			ImagePlus fused = fuseImages(imp1, imp2, shift, method, fusedImageName);
			fused.show();
		}

		if (doLogging)
		{
			IJ.log("Translation Parameters:");
			IJ.log("(second stack relative to first stack)");
			
			if ( this.translation == null )
				IJ.log("x= " + shift.x + " y= " + shift.y + " R= " + result[0].R);
			else
				IJ.log("x= " + shift.x + " y= " + shift.y);
		}
	}

	private Point2D getImageOffset(CrossCorrelationResult2D result, ImagePlus imp1, ImagePlus imp2)
	{
		// see "ROI shift to Image Shift.ppt" for details of nomenclature
		Point2D r1, r2, sr, sir, si;

		// relative shift between rois (all shifts relative to upper left front corner)
		sr = result.shift;

		if (imp1.getRoi() == null || imp2.getRoi() == null)
		{
			// there are no rois....so we already have the relative shift between the images
			si = sr;
		}
		else
		{
			Roi roi1 = imp1.getRoi();
			Roi roi2 = imp2.getRoi();

			int x1 = roi1.getBoundingRect().x;
			int y1 = roi1.getBoundingRect().y;
			int x2 = roi2.getBoundingRect().x;
			int y2 = roi2.getBoundingRect().y;

			r1 = new Point2D(x1, y1);
			r2 = new Point2D(x2, y2);

			sir = add(r1, sr);
			si = subtract(sir, r2);
		}

		return si;
	}

	private Point2D subtract(Point2D a, Point2D b)
	{
		return new Point2D(a.x - b.x, a.y - b.y);
	}

	private Point2D add(Point2D a, Point2D b)
	{
		return new Point2D(a.x + b.x, a.y + b.y);
	}

	/*private float getPixelMin(ImageProcessor ip, int width, int height, int x, int y, double min)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			return (float) min;

		if (ip.getPixels() instanceof byte[])
		{
			byte[] pixelTmp = (byte[]) ip.getPixels();
			return (float) (pixelTmp[x + y * width] & 0xff);
		}
		else if (ip.getPixels() instanceof short[])
		{
			short[] pixelTmp = (short[]) ip.getPixels();
			return (float) (pixelTmp[x + y * width] & 0xffff);
		}
		else // instance of float[]
		{
			float[] pixelTmp = (float[]) ip.getPixels();
			return pixelTmp[x + y * width];
		}

	}*/

	private ImagePlus fuseImages(final ImagePlus imp1, final ImagePlus imp2, Point2D shift, final String fusionMethod, final String name)
	{		
		final int dim = 2;
		
		ImageInformation i1 = new ImageInformation(dim, 1, null);
		i1.closeAtEnd = false;
		i1.imp = imp1;
		i1.size[0] = imp1.getWidth();
		i1.size[1] = imp1.getHeight();
		i1.position = new float[2];
		i1.position[0] = 0;
		i1.position[1] = 0;
		i1.imageName = imp1.getTitle();
		i1.invalid = false;
		i1.imageType = imp1.getType();

		ImageInformation i2 = new ImageInformation(dim, 2, null);
		i2.closeAtEnd = false;
		i2.imp = imp2;
		i2.size[0] = imp2.getWidth();
		i2.size[1] = imp2.getHeight();
		i2.position = new float[2];
		i2.position[0] = shift.x;
		i2.position[1] = shift.y;
		i2.imageName = imp2.getTitle();
		i2.invalid = false;
		i2.imageType = imp2.getType();

		ArrayList<ImageInformation> imageInformationList = new ArrayList<ImageInformation>();
		imageInformationList.add(i1);
		imageInformationList.add(i2);
		
		final float[] max = Stitch_Image_Collection.getAndApplyMinMax(imageInformationList, dim);
		return Stitch_Image_Collection.fuseImages(imageInformationList, max, name, fusionMethod, "rgb", dim, alpha, true );
	}

	/*private ImagePlus fuseImages(ImagePlus imp1, ImagePlus imp2, Point2D shift, String method, String name)
	{
		int type = 0;

		if (method.equals("Average"))
			type = AVG;
		else if (method.equals("Linear Blending"))
			type = LIN_BLEND;
		else if (method.equals("Max. Intensity"))
			type = MAX;
		else if (method.equals("Min. Intensity"))
			type = MIN;
		else if (method.equals("Red-Cyan Overlay"))
			type = RED_CYAN;
		
		ImageProcessor ip1 = imp1.getProcessor();
		int w1 = ip1.getWidth();
		int h1 = ip1.getHeight();

		ImageProcessor ip2 = imp2.getProcessor();
		int w2 = ip2.getWidth();
		int h2 = ip2.getHeight();

		int sx = shift.x;
		int sy = shift.y;

		int imgW, imgH;

		if (sx >= 0)
			imgW = Math.max(w1, w2 + sx);
		else
			imgW = Math.max(w1 - sx, w2); // equals max(w1 + Math.abs(sx), w2);

		if (sy >= 0)
			imgH = Math.max(h1, h2 + sy);
		else
			imgH = Math.max(h1 - sy, h2);

		int offsetImg1X = Math.max(0, -sx); // + max(0, max(0, -sx) - max(0, (w1 - w2)/2));
		int offsetImg1Y = Math.max(0, -sy); // + max(0, max(0, -sy) - max(0, (h1 - h2)/2));
		int offsetImg2X = Math.max(0, sx); // + max(0, max(0, sx) - max(0, (w2 - w1)/2));
		int offsetImg2Y = Math.max(0, sy); // + max(0, max(0, sy) - max(0, (h2 - h1)/2));

		// get size of min enclosing rectangle
		Point2D[] result = getCommonRectangle(new ImagePlus[]{imp1, imp2}, new Point2D[]{shift}, true);
		Point2D size = result[1];

		// now compute the beginning of the enclosing rectangle relative to the big resulting image
		Point2D offset = new Point2D(Math.max(offsetImg2X, offsetImg1X), Math.max(offsetImg2Y, offsetImg1Y));
		
		final int up = 0;
		final int dn = 1;
		final int img1 = 0;
		final int img2 = 1;		
		
		// get the weightening directions
		double xw[][] = new double[2][2]; // [img1, img2][up, down]
		double yw[][] = new double[2][2]; // [img1, img2][up, down]
			
		if (type == LIN_BLEND)
		{
			if (offsetImg1X == offsetImg2X)
			{
				xw[img1][up] = xw[img2][up] = 0;
			}
			else if (offsetImg1X < offsetImg2X)
			{
				xw[img1][up] = 1;
				xw[img2][up] = 0;
			}
			else
			{
				xw[img1][up] = 0;
				xw[img2][up] = 1;
			}
	
			if (offsetImg1Y == offsetImg2Y)
			{
				yw[img1][up] = yw[img2][up] = 0;
			}
			else if (offsetImg1Y < offsetImg2Y)
			{
				yw[img1][up] = 1;
				yw[img2][up] = 0;
			}
			else
			{
				yw[img1][up] = 0;
				yw[img2][up] = 1;
			}
	
			if (offsetImg1X + w1 == offsetImg2X + w2)
			{
				xw[img1][dn] = xw[img2][dn] = 0;
			}
			else if (offsetImg1X + w1 > offsetImg2X + w2)
			{
				xw[img1][dn] = 1;
				xw[img2][dn] = 0;
			}
			else
			{
				xw[img1][dn] = 0;
				xw[img2][dn] = 1;
			}
	
			if (offsetImg1Y + h1 == offsetImg2Y + h2)
			{
				yw[img1][dn] = yw[img2][dn] = 0;
			}
			else if (offsetImg1Y + h1 > offsetImg2Y + h2)
			{
				yw[img1][dn] = 1;
				yw[img2][dn] = 0;
			}
			else
			{
				yw[img1][dn] = 0;
				yw[img2][dn] = 1;
			}
		}
				
		FloatArray2D fused = null;
		ImagePlus fusedImp = null;
		ImageProcessor fusedIp = null;
		float pixel1, pixel2;

		double min1 = ip1.getMin();
		double max1 = ip1.getMax();
		double min2 = ip2.getMin();
		double max2 = ip2.getMax();

		if (type == RED_CYAN)
		{
			fusedImp = IJ.createImage(name, "RGB", imgW, imgH, 1);
			fusedIp = fusedImp.getProcessor();
		}
		else
		{
			fused = new FloatArray2D(imgW, imgH);
		}

		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		float pixel3 = 0;
		int[] iArray = new int[3];

		int count = 0;

		for (int y = 0; y < imgH; y++)
			for (int x = 0; x < imgW; x++)
			{
				pixel1 = getPixelMin(ip1, w1, h1, x - offsetImg1X, y - offsetImg1Y, min1);
				pixel2 = getPixelMin(ip2, w2, h2, x - offsetImg2X, y - offsetImg2Y, min2);

				if (type != RED_CYAN)
				{
					// combine images if overlapping
					if (x >= offsetImg1X && x >= offsetImg2X &&
						x < offsetImg1X + w1 && x < offsetImg2X + w2 &&
						y >= offsetImg1Y && y >= offsetImg2Y &&
						y < offsetImg1Y + h1 && y < offsetImg2Y + h2)
					{
						// compute w1(weight Image1) and w2(weight image2)
						
						// first we need the relative positions in the enclosing rectangle
						double weight1 = 0.5;
						double weight2 = 0.5; 
						
						if (type == LIN_BLEND)
						{
							final double px = (x - offset.x)/(double)(size.x-1);
							final double py = (y - offset.y)/(double)(size.y-1);
							
							final double wx1, wx2, wy1, wy2;
							
							if (px < 0.5)
							{
								wx1 = 0.5 * 2*px + xw[img1][up] * (1 - 2*px);
								wx2 = 0.5 * 2*px + xw[img2][up] * (1 - 2*px);
							}
							else if (px > 0.5)
							{
								wx1 = 0.5 * (1 - 2*(px-0.5)) + xw[img1][dn] * 2*(px-0.5);
								wx2 = 0.5 * (1 - 2*(px-0.5)) + xw[img2][dn] * 2*(px-0.5);
							}
							else
							{
								wx1 = wx2 = 0.5;
							}
	
							if (py < 0.5)
							{
								wy1 = 0.5 * 2*py + yw[img1][up] * (1 - 2*py);
								wy2 = 0.5 * 2*py + yw[img2][up] * (1 - 2*py);
							}
							else if (py > 0.5)
							{
								wy1 = 0.5 * (1 - 2*(py-0.5)) + yw[img1][dn] * 2*(py-0.5);
								wy2 = 0.5 * (1 - 2*(py-0.5)) + yw[img2][dn] * 2*(py-0.5);
							}
							else
							{
								wy1 = wy2 = 0.5;
							}
														
							weight1 = wx1 * wy1;
							weight2 = wx2 * wy2;
							
							if (weight1 == 0 && weight2 == 0)
							{
								weight1 = weight2 = 0.5;
							}
							else
							{
								double norm = weight1 + weight2; 
								weight1 /= norm;
								weight2 /= norm;
							}
						}
												
						pixel3 = 0;
						if (type == AVG)
							pixel3 = (pixel1 + pixel2) / 2f;
						else if (type == LIN_BLEND)
							pixel3 = (float)(pixel1*weight1 + pixel2*weight2);							
						else if (type == MAX)
							pixel3 = Math.max(pixel1, pixel2);
						else if (type == MIN)
							pixel3 = Math.min(pixel1, pixel2);
					}
					else
					{
						pixel3 = Math.max(pixel1, pixel2);

					}

					fused.data[count++] = pixel3;

					if (pixel3 < min)
						min = pixel3;
					else if (pixel3 > max)
						max = pixel3;
				}
				else
				{
					iArray[0] = (int) (((pixel1 - min1) / (max1 - min1)) * 255D);
					iArray[1] = iArray[2] = (int) (((pixel2 - min2) / (max2 - min2)) * 255D);

					fusedIp.putPixel(x, y, iArray);
				}
			}

		if (type != RED_CYAN)
		{
			fusedImp = FloatArrayToImagePlus(fused, name, min, max);
			fused.data = null;
			fused = null;
		}

		return fusedImp;
	}*/
	
	public static Point2D[] getCommonRectangle(ImagePlus img[], Point2D t[], boolean min)
	{
		Point2D size = new Point2D(0, 0);

		// defines where the output image starts relative to the first image
		Point2D startOffset = new Point2D(0, 0);

		if (false)
		{
		    // maximum size (bounding box)
		    Point2D start = new Point2D(0, 0);
		    Point2D minStart = new Point2D(0, 0);
		    Point2D end = new Point2D(img[0].getWidth(), img[0].getWidth());
		    
		    for (int i = 1; i < img.length; i++)
		    {
				start.x += t[i - 1].x;
				start.y += t[i - 1].y;
				
				minStart.x = Math.min(start.x, minStart.x);
				minStart.y = Math.min(start.y, minStart.y);
				
				end.x = Math.max(end.x, start.x + img[i].getWidth());
				end.y = Math.max(end.y, start.y + img[i].getWidth());
		    }

		    size.x = end.x - minStart.x;
		    size.y = end.y - minStart.y;
		    
		    startOffset.x = minStart.x;
		    startOffset.y = minStart.y;		    
		}
		else
		 {
		    // minimum size
		    Point2D start = new Point2D(0, 0, 0);
	
		    Point2D minEnd = new Point2D(img[0].getWidth(), img[0].getHeight());
		    Point2D maxStart = new Point2D(0, 0, 0);
	
		    for (int i = 1; i < img.length; i++)
		    {
				start.x += t[i - 1].x;
				start.y += t[i - 1].y;
		
				maxStart.x = Math.max(start.x, maxStart.x);
				maxStart.y = Math.max(start.y, maxStart.y);
		
				minEnd.x = Math.min(minEnd.x, start.x + img[i].getWidth());
				minEnd.y = Math.min(minEnd.y, start.y + img[i].getHeight());
		    }
	
		    size.x = minEnd.x - maxStart.x;
		    size.y = minEnd.y - maxStart.y;
	
		    startOffset.x = maxStart.x;
		    startOffset.y = maxStart.y;
		}
		 
		 Point2D[] result = new Point2D[2];
		 
		 result[0] = startOffset;
		 result[1] = size;
		 
		 return result;
	}
	

	private CrossCorrelationResult2D[] testCrossCorrelation(FloatArray2D invPCM, ArrayList<Point2D> peaks,
			final FloatArray2D img1, final FloatArray2D img2)
	{
		final int numBestHits = peaks.size();
		final int numCases = 4;
		final CrossCorrelationResult2D result[] = new CrossCorrelationResult2D[numBestHits * numCases];

		int count = 0;
		int w = invPCM.width;
		int h = invPCM.height;

		final Point2D[] points = new Point2D[numCases];

		for (int hit = 0; count < numBestHits * numCases; hit++)
		{
			points[0] = peaks.get(hit);

			if (points[0].x < 0)
				points[1] = new Point2D(points[0].x + w, points[0].y, points[0].value);
			else
				points[1] = new Point2D(points[0].x - w, points[0].y, points[0].value);

			if (points[0].y < 0)
				points[2] = new Point2D(points[0].x, points[0].y + h, points[0].value);
			else
				points[2] = new Point2D(points[0].x, points[0].y - h, points[0].value);

			points[3] = new Point2D(points[1].x, points[2].y, points[0].value);

			final AtomicInteger entry = new AtomicInteger(count);
			final AtomicInteger shift = new AtomicInteger(0);

			Runnable task = new Runnable()
			{
				public void run()
				{
					try
					{
						int myEntry = entry.getAndIncrement();
						int myShift = shift.getAndIncrement();
						result[myEntry] = testCrossCorrelation(points[myShift], img1, img2);
					} catch (Exception e)
					{
						e.printStackTrace();
						IJ.log(e.getMessage());
					}
				}
			};

			startTask(task, numCases);

			count += numCases;
		}

		quicksort(result, 0, result.length - 1);

		if (doLogging)
			for (int i = 0; i < result.length; i++)
				IJ.log("x:" + result[i].shift.x + " y:" + result[i].shift.y + " overlap:" + result[i].overlappingPixels + " R:" + result[i].R + " Peak:" + result[i].PCMValue);

		return result;
	}

	private CrossCorrelationResult2D testCrossCorrelation(Point2D shift, FloatArray2D img1, FloatArray2D img2)
	{
		// init Result Datastructure
		CrossCorrelationResult2D result = new CrossCorrelationResult2D();

		// compute values that will not change during testing
		result.PCMValue = shift.value;
		result.shift = shift;

		int w1 = img1.width;
		int h1 = img1.height;

		int w2 = img2.width;
		int h2 = img2.height;

		int sx = shift.x;
		int sy = shift.y;

		int imgW, imgH;

		if (sx >= 0)
			imgW = Math.max(w1, w2 + sx);
		else
			imgW = Math.max(w1 - sx, w2); // equals max(w1 + Math.abs(sx), w2);

		if (sy >= 0)
			imgH = Math.max(h1, h2 + sy);
		else
			imgH = Math.max(h1 - sy, h2);

		int offsetImg1X = Math.max(0, -sx); // + max(0, max(0, -sx) - max(0, (w1 - w2)/2));
		int offsetImg1Y = Math.max(0, -sy); // + max(0, max(0, -sy) - max(0, (h1 - h2)/2));
		int offsetImg2X = Math.max(0, sx); // + max(0, max(0, sx) - max(0, (w2 - w1)/2));
		int offsetImg2Y = Math.max(0, sy); // + max(0, max(0, sy) - max(0, (h2 - h1)/2));

		int count = 0;
		float pixel1, pixel2;

		double avg1 = 0, avg2 = 0;

		for (int y = 0; y < imgH; y++)
			for (int x = 0; x < imgW; x++)
			{
				// compute errors only if the images overlap
				if (x >= offsetImg1X && x >= offsetImg2X &&
					x < offsetImg1X + w1 && x < offsetImg2X + w2 &&
					y >= offsetImg1Y && y >= offsetImg2Y &&
					y < offsetImg1Y + h1 && y < offsetImg2Y + h2)
				{
					pixel1 = img1.getZero(x - offsetImg1X, y - offsetImg1Y);
					pixel2 = img2.getZero(x - offsetImg2X, y - offsetImg2Y);

					avg1 += pixel1;
					avg2 += pixel2;
					count++;

				}
			}

		// if less than 1% is overlapping
		if (count <= (Math.min(w1, w2) * Math.min(h1, h2)) * 0.01)
		{
			//IJ.log("lower than 1%");
			result.R = 0;
			result.SSQ = Float.MAX_VALUE;
			result.overlappingPixels = count;
			return result;
		}

		avg1 /= (double) count;
		avg2 /= (double) count;

		double var1 = 0, var2 = 0;
		double coVar = 0;

		double dist1, dist2;

		double SSQ = 0;
		double pixelSSQ;
		count = 0;

		for (int y = 0; y < imgH; y++)
			for (int x = 0; x < imgW; x++)
			{
				// compute errors only if the images overlap
				if (x >= offsetImg1X && x >= offsetImg2X &&
					x < offsetImg1X + w1 && x < offsetImg2X + w2 &&
					y >= offsetImg1Y && y >= offsetImg2Y &&
					y < offsetImg1Y + h1 && y < offsetImg2Y + h2)
				{
					pixel1 = img1.getZero(x - offsetImg1X, y - offsetImg1Y);
					pixel2 = img2.getZero(x - offsetImg2X, y - offsetImg2Y);

					pixelSSQ = Math.pow(pixel1 - pixel2, 2);
					SSQ += pixelSSQ;
					count++;

					dist1 = pixel1 - avg1;
					dist2 = pixel2 - avg2;

					coVar += dist1 * dist2;
					var1 += Math.pow(dist1, 2);
					var2 += Math.pow(dist2, 2);

				}
			}

		SSQ /= (double) count;
		var1 /= (double) count;
		var2 /= (double) count;
		coVar /= (double) count;

		double stDev1 = Math.sqrt(var1);
		double stDev2 = Math.sqrt(var2);

		// all pixels had the same color....
		if (stDev1 == 0 || stDev2 == 0)
		{
			result.R = 0;
			result.SSQ = Float.MAX_VALUE;
			result.overlappingPixels = count;
			return result;
		}

		// compute correlation coeffienct
		result.R = coVar / (stDev1 * stDev2);
		result.SSQ = SSQ;
		result.overlappingPixels = count;

		return result;
	}

	private ArrayList<Point2D> findPeaks(FloatArray2D invPCM, Point2D img1, Point2D img2, Point2D ext1, Point2D ext2, int checkPeaks)
	{
		int w = invPCM.width;
		int h = invPCM.height;

		int xs, ys, xt, yt;
		float value;

		ArrayList<Point2D>peaks = new ArrayList<Point2D>();

		for (int j = 0; j < checkPeaks; j++)
			peaks.add(new Point2D(0, 0, Float.MIN_VALUE));

		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				if (isLocalMaxima(invPCM, x, y))
				{
					value = invPCM.get(x, y);
					Point2D insert = null;
					int insertPos = -1;

					Iterator<Point2D> i = peaks.iterator();
					boolean wasBigger = true;

					while (i.hasNext() && wasBigger)
					{
						if (value > i.next().value)
						{
							if (insert == null)
								insert = new Point2D(0, 0, value);

							insertPos++;
						}
						else
							wasBigger = false;
					}

					if (insertPos >= 0)
						peaks.add(insertPos + 1, insert);

					// remove lowest peak
					if (peaks.size() > checkPeaks)
						peaks.remove(0);

					if (insert != null)
					{
						// find relative to the left upper front corners of both images
						xt = x + (img1.x - img2.x) / 2 - (ext1.x - ext2.x) / 2;

						if (xt >= w / 2)
						{
							xs = xt - w;
						}
						else
							xs = xt;

						yt = y + (img1.y - img2.y) / 2 - (ext1.y - ext2.y) / 2;

						if (yt >= h / 2)
						{
							ys = yt - h;
						}
						else
							ys = yt;

						insert.x = xs;
						insert.y = ys;
					}
				}

		return peaks;
	}

	private boolean isLocalMaxima(FloatArray2D invPCM, int x, int y)
	{
		int width = invPCM.width;
		int height = invPCM.height;

		boolean isMax = true;
		float value = invPCM.get(x, y);

		if (x > 0 && y > 0 && x < width - 1 && y < height - 1)
		{
			for (int xs = x - 1; xs <= x + 1 && isMax; xs++)
				for (int ys = y - 1; ys <= y + 1 && isMax; ys++)
					if (!(x == xs && y == ys))
						if (invPCM.get(xs, ys) > value)
							isMax = false;
		}
		else
		{
			int xt, yt;

			for (int xs = x - 1; xs <= x + 1 && isMax; xs++)
				for (int ys = y - 1; ys <= y + 1 && isMax; ys++)
					if (!(x == xs && y == ys))
					{
						xt = xs;
						yt = ys;

						if (xt == -1) xt = width - 1;
						if (yt == -1) yt = height - 1;

						if (xt == width) xt = 0;
						if (yt == height) yt = 0;

						if (invPCM.get(xt, yt) > value)
							isMax = false;
					}
		}

		return isMax;
	}
	
	private FloatArray2D applyROI(ImagePlus imp, Point2D imgDim, Point2D extDim, String handleRGB, boolean windowing)
	{
		FloatArray2D img;

		if (imp.getRoi() == null)
		{
			// there are no rois....
			img = ImageToFloatArray(imp.getProcessor(), handleRGB);
		}
		else
		{
			Roi r = imp.getRoi();

			int x = r.getBoundingRect().x;
			int y = r.getBoundingRect().y;
			int w = r.getBoundingRect().width;
			int h = r.getBoundingRect().height;

			img = ImageToFloatArray(imp.getProcessor(), handleRGB, x, y, w, h);
		}

		imgDim.x = img.width;
		imgDim.y = img.height;

		extDim.x = extDim.y = 0;

		if (windowing)
		{
			int imgW = img.width;
			int imgH = img.height;
			int extW = imgW / 4;
			int extH = imgH / 4;

			// add an even number so that both sides extend equally
			if (extW % 2 != 0) extW++;
			if (extH % 2 != 0) extH++;

			extDim.x = extW;
			extDim.y = extH;

			imgDim.x += extDim.x;
			imgDim.y += extDim.y;

			// extend images
			img = extendImageMirror(img, imgW + extW, imgH + extH);
		}

		return img;
	}

	private boolean checkRoi(ImagePlus imp1, ImagePlus imp2)
	{
		boolean img1HasROI, img2HasROI;

		if (imp1.getRoi() != null)
			img1HasROI = true;
		else
			img1HasROI = false;

		if (imp2.getRoi() != null)
			img2HasROI = true;
		else
			img2HasROI = false;

		if (img1HasROI && !img2HasROI || img2HasROI && !img1HasROI)
		{
			IJ.error("Eigher both images should have a ROI or none of them.");

			return false;
		}

		if (img1HasROI)
		{
			int type1 = imp1.getRoi().getType();
			int type2 = imp2.getRoi().getType();

			if (type1 != Roi.RECTANGLE)
			{
				IJ.error(imp1.getTitle() + " has a ROI which is no rectangle.");
			}

			if (type2 != Roi.RECTANGLE)
			{
				IJ.error(imp2.getTitle() + " has a ROI which is no rectangle.");
			}
		}

		return true;
	}


	/*
	   -----------------------------------------------------------------------------------------
	   Here are the general static methods needed by the plugin
	   Just to pronouce that they are not directly related, I put them in a seperate child class
	   -----------------------------------------------------------------------------------------
	 */

	private void exponentialWindow(FloatArray2D img)
	{
		double a = 1000;

		// create lookup table
		double weightsX[] = new double[img.width];
		double weightsY[] = new double[img.height];

		for (int x = 0; x < img.width; x++)
		{
			double relPos = (double) x / (double) (img.width - 1);

			if (relPos <= 0.5)
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
		{
			double relPos = (double) y / (double) (img.height - 1);

			if (relPos <= 0.5)
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
			for (int x = 0; x < img.width; x++)
				img.set((float) (img.get(x, y) * weightsX[x] * weightsY[y]), x, y);
	}

	private FloatArray2D extendImageMirror(FloatArray2D ip, int width, int height)
	{
		FloatArray2D image = new FloatArray2D(width, height);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;

		if (offsetX < 0)
		{
			IJ.error("Stitching_3D.extendImageMirror(): Extended size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Stitching_3D.extendImageMirror(): Extended size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				image.set(ip.getMirror(x - offsetX, y - offsetY), x, y);

		return image;
	}

	public FloatArray2D ImageToFloatArray(ImageProcessor ip, String handleRGB)
	{
		int rgbType = -1;

		if (ip == null )
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (ip.getPixels() instanceof int[])
		{
			if (handleRGB == null || handleRGB.trim().length() == 0) handleRGB = colorList[colorList.length - 1];

			for (int i = 0; i < colorList.length; i++)
			{
				if (handleRGB.toLowerCase().trim().equals(colorList[i].toLowerCase())) rgbType = i;
			}

			if (rgbType == -1)
			{
				System.err.println("Unrecognized command to handle RGB: " + handleRGB + ". Assuming Average of Red, Green and Blue.");
				rgbType = colorList.length - 1;
			}
		}
		
		int width = ip.getWidth();
		int height = ip.getHeight();

		if (width * height == 0)
		{
			System.out.println("Image is empty.");
			return null;
		}

		FloatArray2D pixels = new FloatArray2D(width, height);
		int count;

		if (ip.getPixels() instanceof byte[])
		{
			byte[] pixelTmp = (byte[]) ip.getPixels();
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y)] = (float) (pixelTmp[count++] & 0xff);
		}
		else if (ip.getPixels() instanceof short[])
		{
			short[] pixelTmp = (short[]) ip.getPixels();
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y)] = (float) (pixelTmp[count++] & 0xffff);
		}
		else if (ip.getPixels() instanceof int[]) 
		{
			int[] pixelTmp = (int[]) ip.getPixels();
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y)] = getPixelValueRGB(pixelTmp[count++], rgbType);
		}
		else// instance of float[]
		{
			float[] pixelTmp = (float[]) ip.getPixels();
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y)] = pixelTmp[count++];
		}

		return pixels;
	}

	private FloatArray2D ImageToFloatArray(ImageProcessor ip, String handleRGB, int xCrop, int yCrop, int wCrop, int hCrop)
	{
		int rgbType = -1;

		if (ip == null )
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (ip.getPixels() instanceof int[])
		{
			if (handleRGB == null || handleRGB.trim().length() == 0) handleRGB = colorList[colorList.length - 1];

			for (int i = 0; i < colorList.length; i++)
			{
				if (handleRGB.toLowerCase().trim().equals(colorList[i].toLowerCase())) rgbType = i;
			}

			if (rgbType == -1)
			{
				System.err.println("Unrecognized command to handle RGB: " + handleRGB + ". Assuming Average of Red, Green and Blue.");
				rgbType = colorList.length - 1;
			}
		}

		int width = ip.getWidth();
		int height = ip.getHeight();

		if (width * height == 0)
		{
			System.out.println("Image is empty.");
			return null;
		}

		FloatArray2D pixels = new FloatArray2D(wCrop, hCrop);

		if (ip.getPixels() instanceof byte[])
		{
			byte[] pixelTmp = (byte[]) ip.getPixels();

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop)] = (float) (pixelTmp[x + y * width] & 0xff);
		}
		else if (ip.getPixels() instanceof short[])
		{
			short[] pixelTmp = (short[]) ip.getPixels();

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop)] = (float) (pixelTmp[x + y * width] & 0xffff);
		}
		else if (ip.getPixels() instanceof int[])
		{
			int[] pixelTmp = (int[]) ip.getPixels();

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop)] = getPixelValueRGB(pixelTmp[x + y * width], rgbType);
		}
		else // instance of float[]
		{
			float[] pixelTmp = (float[]) ip.getPixels();

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop)] = pixelTmp[x + y * width];
		}

		return pixels;
	}

	private ImagePlus FloatArrayToImagePlus(FloatArray2D image, String name, float min, float max)
	{
		int width = image.width;
		int height = image.height;

		ImagePlus impResult = IJ.createImage(name, "32-Bit Black", width, height, 1);
		ImageProcessor ipResult = impResult.getProcessor();
		float[] sliceImg = new float[width * height];

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				sliceImg[y * width + x] = image.get(x, y);

		ipResult.setPixels(sliceImg);

		if (min == max)
			ipResult.resetMinAndMax();
		else
			ipResult.setMinAndMax(min, max);

		impResult.updateAndDraw();

		return impResult;
	}
}





