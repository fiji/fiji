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

import static stitching.CommonFunctions.*;

import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.process.*;
import ij.gui.Roi;
import ij.gui.MultiLineLabel;
import ij.ImageStack;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Component;
import java.awt.Checkbox;
import java.awt.Choice;

import stitching.CrossCorrelationResult3D;
import stitching.FloatArray3D;
import stitching.ImageInformation;
import stitching.Point3D;

public class Stitching_3D implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	public String imgStack1, imgStack2, fusedImageName;
	
	public static String methodStatic = methodList[1];
	public static String handleRGB1Static = colorList[colorList.length - 1];
	public static String handleRGB2Static = colorList[colorList.length - 1];
	public static boolean fuseImagesStatic = true, windowingStatic = true, coregisterStatic = false, computeOverlapStatic = true;
	public static int checkPeaksStatic = 5, numberOfChannelsStatic = 1;
	public static double alphaStatic = 1.5;
	public static int xOffsetStatic = 0, yOffsetStatic = 0, zOffsetStatic = 0;

	public String method;
	public String handleRGB1;
	public String handleRGB2;
	public boolean fuseImages, windowing, coregister, computeOverlap = true;
	public int checkPeaks, numberOfChannels;
	public double alpha;
	public int xOffset = 0, yOffset = 0, zOffset = 0;

	public ImagePlus imp1 = null, imp2 = null;
	public boolean wasIndexed, doLogging = true;
	public double minOverlap = 0.01;
	private ArrayList<String[]> coregStacks;
	private ArrayList<ImagePlus[]> coregStackIMPs;
	private ArrayList<Boolean> coregWasIndexed;

	private Point3D shift = null;
	private CrossCorrelationResult3D[] result = null;
	
	public Point3D getTranslation() { return shift.clone(); }
	public CrossCorrelationResult3D[] getCrossCorrelationResults() { return result; }
	public CrossCorrelationResult3D getCrossCorrelationResult() { return result[0].clone(); }

		
	// a macro can call to only fuse two images given a certain shift
	private Point3D translation = null;

	public void run(String args)
	{
		// get list of image stacks
		int[] idList = WindowManager.getIDList();

		if (idList == null)
		{
			IJ.error("You need two open image stacks.");
			return;
		}

		int stacks = 0;
		for (int i = 0; i < idList.length; i++)
			if (WindowManager.getImage(idList[i]).getStackSize() > 1) stacks++;

		if (stacks < 2)
		{
			IJ.error("You need two open image stacks.");
			return;
		}

		String[] stackList = new String[stacks];
		int[] stackIDs = new int[stacks];
		stacks = 0;

		for (int i = 0; i < idList.length; i++)
		{
			if (WindowManager.getImage(idList[i]).getStackSize() > 1)
			{
				stackList[stacks] = WindowManager.getImage(idList[i]).getTitle();
				stackIDs[stacks] = idList[i];
				++stacks;
			}
		}

		// create generic dialog
		GenericDialog gd = new GenericDialog("Stitching of 3D Images");
		gd.addChoice("First_Stack (reference)", stackList, stackList[0]);
		gd.addChoice("Use_Channel_for_First", colorList, handleRGB1Static);
		enableChannelChoice((Choice) gd.getChoices().get(0), (Choice) gd.getChoices().get(1), stackIDs);

		gd.addChoice("Second_Stack (to register)", stackList, stackList[1]);
		gd.addChoice("Use_Channel_for_Second", colorList, handleRGB2Static);
		enableChannelChoice((Choice) gd.getChoices().get(2), (Choice) gd.getChoices().get(3), stackIDs);

		gd.addCheckbox("Use_Windowing", windowingStatic);
		gd.addNumericField("Peaks", checkPeaksStatic, 0);
		gd.addCheckbox("Create_Fused_Image", fuseImagesStatic);
		gd.addChoice("Fusion_Method", methodList, methodStatic);
		gd.addNumericField("Fusion alpha", alphaStatic, 2);		
		gd.addStringField("Fused_Image_Name: ", "Fused_" + stackList[0] + "_" + stackList[1]);
		gd.addCheckbox("Apply_to_other_Channels", coregisterStatic);
		gd.addNumericField("Number_of_other_Channels", numberOfChannelsStatic, 0);
		gd.addCheckbox("compute_overlap", computeOverlapStatic);
		gd.addNumericField("x", xOffset, xOffsetStatic);
		gd.addNumericField("y", yOffset, yOffsetStatic);
		gd.addNumericField("z", zOffset, zOffsetStatic);
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		// get Checkboxes
		Component[] c1 = new Component[] { (Component) gd.getChoices().get(4), 
										   (Component) gd.getStringFields().get(0), 
										   (Component) gd.getCheckboxes().get(2), 
										   (Component) gd.getNumericFields().get(1), 
										   (Component) gd.getNumericFields().get(2) };
		addEnablerListener((Checkbox) gd.getCheckboxes().get(1), c1, null);
		
		gd.showDialog();

		if (gd.wasCanceled()) return;

		this.imgStack1 = gd.getNextChoice();
		handleRGB1Static = gd.getNextChoice();
		this.imgStack2 = gd.getNextChoice();
		handleRGB2Static = gd.getNextChoice();
		this.imp1 = WindowManager.getImage(stackIDs[((Choice) gd.getChoices().get(0)).getSelectedIndex()]);
		this.imp2 = WindowManager.getImage(stackIDs[((Choice) gd.getChoices().get(2)).getSelectedIndex()]);
		windowingStatic = gd.getNextBoolean();
		checkPeaksStatic = (int) gd.getNextNumber();
		fuseImagesStatic = gd.getNextBoolean();
		methodStatic = gd.getNextChoice();
		alphaStatic = gd.getNextNumber();
		this.fusedImageName = gd.getNextString();
		coregisterStatic = gd.getNextBoolean();
		numberOfChannelsStatic = (int) gd.getNextNumber();
		computeOverlap = gd.getNextBoolean();
		xOffset = (int)Math.round( gd.getNextNumber() );
		yOffset = (int)Math.round( gd.getNextNumber() );
		zOffset = (int)Math.round( gd.getNextNumber() );

		method = methodStatic;
		handleRGB1 = handleRGB1Static;
		handleRGB2 = handleRGB2Static;
		fuseImages = fuseImagesStatic;
		windowing = windowingStatic;
		coregister = coregisterStatic;
		checkPeaks = checkPeaksStatic;
		numberOfChannels = numberOfChannelsStatic;
		alpha = alphaStatic;
		
		if ( !computeOverlap )
			this.translation = new Point3D( xOffset, yOffset, zOffset );
		else
			this.translation = null;

		//
		// determine wheater a macro called it which limits in determining name
		// clashes
		//
		boolean calledFromMacro = false;

		if (!(imp1.getTitle().equals(imgStack1) && imp2.getTitle().equals(imgStack2)))
		{
			calledFromMacro = true;
			imp1 = WindowManager.getImage(imgStack1);
			imp2 = WindowManager.getImage(imgStack2);
		}

		if (!calledFromMacro && stackIDs[((Choice) gd.getChoices().get(0)).getSelectedIndex()] == stackIDs[((Choice) gd.getChoices().get(2)).getSelectedIndex()])
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

		if (!fuseImages) coregister = false;

		if (imp1.getType() == ImagePlus.COLOR_256)
		{
			// convert to RGB
			new StackConverter(imp1).convertToRGB();
			new StackConverter(imp2).convertToRGB();

			wasIndexed = true;
		}
		else
		{
			wasIndexed = false;
		}

		if (coregister)
		{
			if (fuseImages == false)
			{
				YesNoCancelDialog error = new YesNoCancelDialog(null, "Error", "Co-Registration makes only sense if you want to fuse the image stacks, actually you only get the numbers. Do you want to continue?");

				if (!error.yesPressed()) return;
				else coregister = false;

			}
			else if (stackList.length < 3)
			{
				YesNoCancelDialog error = new YesNoCancelDialog(null, "Error", "You have only two stacks open, there is nothing to co-register. Do you want to continue?");

				if (!error.yesPressed()) return;
				else coregister = false;
			}
			else if (numberOfChannels < 1)
			{

				YesNoCancelDialog error = new YesNoCancelDialog(null, "Error", "You have selected less than 1 stack to co-register...that makes no sense to me. Do you want to continue?");

				if (!error.yesPressed()) return;
				else coregister = false;
			}
			else
			{
				GenericDialog coreg = new GenericDialog("Co-Registration");
				for (int i = 0; i < numberOfChannels; i++)
				{
					coreg.addMessage("Co-Register Stack #" + (i + 1));
					coreg.addChoice("First_Image_Stack_" + (i + 1) + " (not moved)", stackList, stackList[2]);
					coreg.addChoice("Second_Image_Stack_" + (i + 1) + " (moved)", stackList, stackList[3]);
					coreg.addStringField("Fused_Image_Name_" + (i + 1), "Fused Channel " + (i + 2));
					if (i + 1 != numberOfChannels) coreg.addMessage("");
				}

				coreg.showDialog();

				if (coreg.wasCanceled()) return;

				this.coregStacks = new ArrayList<String[]>();
				this.coregStackIMPs = new ArrayList<ImagePlus[]>();
				this.coregWasIndexed = new ArrayList<Boolean>();

				for (int i = 0; i < numberOfChannels; i++)
				{
					String[] entry = new String[3];
					entry[0] = coreg.getNextChoice();
					entry[1] = coreg.getNextChoice();
					entry[2] = coreg.getNextString();

					ImagePlus[] entryIMPs = new ImagePlus[2];
					entryIMPs[0] = WindowManager.getImage(stackIDs[((Choice) coreg.getChoices().get(i * 2)).getSelectedIndex()]);
					entryIMPs[1] = WindowManager.getImage(stackIDs[((Choice) coreg.getChoices().get(i * 2 + 1)).getSelectedIndex()]);

					if (entryIMPs[0].getType() != entryIMPs[1].getType())
					{
						IJ.error("The Image Stacks (Coreg #" + (i + 1) + ") are of a different type, it is unclear how to fuse them. Stopping.");
						return;
					}

					if (stackIDs[((Choice) coreg.getChoices().get(i * 2)).getSelectedIndex()] == stackIDs[((Choice) coreg.getChoices().get(i * 2 + 1)).getSelectedIndex()])
					{
						IJ.error("You selected the same stack twice (Coreg #" + (i + 1) + "). Stopping.");
						return;
					}

					if (entryIMPs[0].getType() == ImagePlus.COLOR_256)
					{
						// convert to RGB
						new StackConverter(entryIMPs[0]).convertToRGB();
						new StackConverter(entryIMPs[1]).convertToRGB();
						this.coregWasIndexed.add(true);
					}
					else
					{
						this.coregWasIndexed.add(false);
					}

					this.coregStacks.add(entry);
					this.coregStackIMPs.add(entryIMPs);
				}
			}
		}
		
		if (doLogging)
			IJ.log("(" + new Date(System.currentTimeMillis()) + "):Starting");
		work();
		if (doLogging)
			IJ.log("(" + new Date(System.currentTimeMillis()) + "):Finished");
	}

	private final void enableChannelChoice(final Choice controller, final Choice target, final int[] stackIDs)
	{
		setRGB(stackIDs[controller.getSelectedIndex()], target);

		controller.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				setRGB(stackIDs[controller.getSelectedIndex()], target);
			}
		});
	}

	private final void setRGB(int imagejID, final Choice target)
	{
		if (WindowManager.getImage(imagejID).getType() == ImagePlus.COLOR_RGB || WindowManager.getImage(imagejID).getType() == ImagePlus.COLOR_256) target.setEnabled(true);
		else target.setEnabled(false);
	}

	public static final void addEnablerListener(/* final GenericDialog gd, */final Checkbox master, final Component[] enable, final Component[] disable)
	{
		master.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				if (ie.getStateChange() == ItemEvent.SELECTED)
				{
					process(enable, true);
					process(disable, false);
				}
				else
				{
					process(enable, false);
					process(disable, true);
				}
			}

			private void process(final Component[] c, final boolean state)
			{
				if (null == c) return;
				for (int i = 0; i < c.length; i++)
				{
					c[i].setEnabled(state);
					// c[i].setVisible(state);
				}
				// gd.pack();
			}
		});
	}

	public static final void addInverseEnablerListener(/* final GenericDialog gd, */final Checkbox master, final Component[] enable, final Component[] disable)
	{
		master.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				if (ie.getStateChange() == ItemEvent.SELECTED)
				{
					process(enable, false);
					process(disable, true);
				}
				else
				{
					process(enable, true);
					process(disable, false);
				}
			}

			private void process(final Component[] c, final boolean state)
			{
				if (null == c) return;
				for (int i = 0; i < c.length; i++)
				{
					c[i].setEnabled(state);
					// c[i].setVisible(state);
				}
				// gd.pack();
			}
		});
	}
	
	private ImagePlus getImage(String imgStack)
	{
		ImagePlus imp = null;

		try
		{
			imp = WindowManager.getImage(imgStack);
		}
		catch (Exception e)
		{
			IJ.error("Could not find image stack: " + e);
		}
		;

		return imp;
	}

	public Stitching_3D()
	{
	}

	public Stitching_3D(boolean windowing1, int checkPeaks1, boolean fuseImages1, String method1, String fusedImageName1)
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

	public void work(FloatArray3D inputImage1, FloatArray3D inputImage2)
	{
		ImagePlus imp1, imp2;
		FloatArray3D img1 = null, img2 = null, fft1, fft2;
		
		Point3D img1Dim = new Point3D(0, 0, 0), img2Dim = new Point3D(0, 0, 0), // (incl.
																				// possible
																				// ext!!!)
		ext1Dim = new Point3D(0, 0, 0), ext2Dim = new Point3D(0, 0, 0), maxDim;

		// make it also executable as non-plugin/macro
		if (inputImage1 == null || inputImage2 == null)
		{
			// get images
			if (this.imp1 == null) imp1 = getImage(imgStack1);
			else imp1 = this.imp1;

			if (this.imp2 == null) imp2 = getImage(imgStack2);
			else imp2 = this.imp2;

			if (imp1 == null || imp2 == null)
			{
				IJ.error("Could not get the image stacks for some unknown reason.");
				return;
			}

			// check for ROIs in images and whether they are valid
			if (!checkRoi(imp1, imp2)) return;

			// apply ROIs if they are there and save dimensions of original
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

			imp1 = FloatArrayToStack(img1, "Image1", 0, 0);
			imp2 = FloatArrayToStack(img2, "Image2", 0, 0);

			img1Dim.x = img1.width;
			img1Dim.y = img1.height;
			img1Dim.z = img1.depth;
			img2Dim.x = img2.width;
			img2Dim.y = img2.height;
			img2Dim.z = img2.depth;

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
			FloatArray3D[] zeropadded = zeroPadImages(img1, img2);
			img1 = zeropadded[0];
			img2 = zeropadded[1];

			// save dimensions of zeropadded image
			maxDim = new Point3D(img1.width, img1.height, img1.depth);

			// compute FFT's
			fft1 = computeFFT(img1);
			fft2 = computeFFT(img2);

			// do the phase correlation
			FloatArray3D invPCM = computePhaseCorrelationMatrix(fft1, fft2, maxDim.x);

			// find the peaks
			ArrayList<Point3D> peaks = findPeaks(invPCM, img1Dim, img2Dim, ext1Dim, ext2Dim);

			// get the original images
			img1 = applyROI(imp1, img1Dim, ext1Dim, handleRGB1, false /* no windowing of course*/);
			img2 = applyROI(imp2, img2Dim, ext2Dim, handleRGB2, false /* no windowing of course*/);

			// test peaks
			result = testCrossCorrelation(invPCM, peaks, img1, img2);

			// delete images from memory
			img1.data = img2.data = null;
			img1 = img2 = null;

			// get shift of images relative to each other
			shift = getImageOffset(result[0], imp1, imp2);
		}
		else
		{
			shift = this.translation;
		}

		if (fuseImages)
		{
			// merge if wanted
			ImagePlus fused = fuseImages(imp1, imp2, shift, method, fusedImageName);

			if (fused != null)
			{
				if (this.wasIndexed) new StackConverter(fused).convertToIndexedColor(256);

				fused.show();
			}

			// coregister other channels
			if (coregister) for (int i = 0; i < numberOfChannels; i++)
			{
				String[] stacks = this.coregStacks.get(i);
				ImagePlus[] imps = this.coregStackIMPs.get(i);

				// get images
				imp1 = imps[0];
				imp2 = imps[1];

				ImagePlus coregistered = fuseImages(imp1, imp2, shift, method, stacks[2]);

				if (coregistered != null)
				{
					if (this.coregWasIndexed.get(i)) new StackConverter(fused).convertToIndexedColor(256);

					coregistered.show();
				}
			}

		}

		if (doLogging)
		{
			IJ.log("Translation Parameters:");
			IJ.log("(second stack relative to first stack)");
			
			if ( this.translation == null )
				IJ.log("x=" + shift.x + " y=" + shift.y + " z=" + shift.z + " R=" + result[0].R);
			else
				IJ.log("x=" + shift.x + " y=" + shift.y + " z=" + shift.z );				
		}
	}

	private Point3D getImageOffset(CrossCorrelationResult3D result, ImagePlus imp1, ImagePlus imp2)
	{
		// see "ROI shift to Image Shift.ppt" for details of nomenclature
		Point3D r1, r2, sr, sir, si;

		// relative shift between rois (all shifts relative to upper left front
		// corner)
		sr = result.shift;

		if (imp1.getRoi() == null || imp2.getRoi() == null)
		{
			// there are no rois....so we already have the relative shift
			// between the images
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

			r1 = new Point3D(x1, y1, 0);
			r2 = new Point3D(x2, y2, 0);

			sir = add(r1, sr);
			si = subtract(sir, r2);
		}

		return si;
	}

	private Point3D subtract(Point3D a, Point3D b)
	{
		return new Point3D(a.x - b.x, a.y - b.y, a.z - b.z);
	}

	private Point3D add(Point3D a, Point3D b)
	{
		return new Point3D(a.x + b.x, a.y + b.y, a.z + b.z);
	}

	private ImagePlus fuseImages(final ImagePlus imp1, final ImagePlus imp2, Point3D shift, final String fusionMethod, final String name)
	{		
		final int dim = 3;
		
		ImageInformation i1 = new ImageInformation(dim, 1, null);
		i1.closeAtEnd = false;
		i1.imp = imp1;
		i1.size[0] = imp1.getWidth();
		i1.size[1] = imp1.getHeight();
		i1.size[2] = imp1.getStack().getSize();
		i1.position = new float[dim];
		i1.position[0] = i1.position[1] = i1.position[2] = 0;
		i1.imageName = imp1.getTitle();
		i1.invalid = false;
		i1.imageType = imp1.getType();

		ImageInformation i2 = new ImageInformation(dim, 2, null);
		i2.closeAtEnd = false;
		i2.imp = imp2;
		i2.size[0] = imp2.getWidth();
		i2.size[1] = imp2.getHeight();
		i2.size[2] = imp2.getStack().getSize();
		i2.position = new float[dim];
		i2.position[0] = shift.x;
		i2.position[1] = shift.y;
		i2.position[2] = shift.z;
		i2.imageName = imp2.getTitle();
		i2.invalid = false;
		i2.imageType = imp2.getType();

		ArrayList<ImageInformation> imageInformationList = new ArrayList<ImageInformation>();
		imageInformationList.add(i1);
		imageInformationList.add(i2);
		
		final float[] max = Stitch_Image_Collection.getAndApplyMinMax(imageInformationList, dim);
		return Stitch_Image_Collection.fuseImages(imageInformationList, max, name, fusionMethod, "rgb", dim, alpha, true );
	}
	
	private CrossCorrelationResult3D[] testCrossCorrelation(FloatArray3D invPCM, ArrayList<Point3D> peaks, final FloatArray3D img1, final FloatArray3D img2)
	{
		final int numBestHits = peaks.size();
		final int numCases = 8;
		final CrossCorrelationResult3D result[] = new CrossCorrelationResult3D[numBestHits * numCases];

		int count = 0;
		int w = invPCM.width;
		int h = invPCM.height;
		int d = invPCM.depth;

		final Point3D[] points = new Point3D[numCases];

		for (int hit = 0; count < numBestHits * numCases; hit++)
		{
			points[0] = peaks.get(hit);

			if (points[0].x < 0) points[1] = new Point3D(points[0].x + w, points[0].y, points[0].z);
			else points[1] = new Point3D(points[0].x - w, points[0].y, points[0].z);

			if (points[0].y < 0) points[2] = new Point3D(points[0].x, points[0].y + h, points[0].z);
			else points[2] = new Point3D(points[0].x, points[0].y - h, points[0].z);

			if (points[0].z < 0) points[3] = new Point3D(points[0].x, points[0].y, points[0].z + d);
			else points[3] = new Point3D(points[0].x, points[0].y, points[0].z - d);

			points[4] = new Point3D(points[1].x, points[2].y, points[0].z);
			points[5] = new Point3D(points[0].x, points[2].y, points[3].z);
			points[6] = new Point3D(points[1].x, points[0].y, points[3].z);
			points[7] = new Point3D(points[1].x, points[2].y, points[3].z);

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
					}
					catch (Exception e)
					{
						e.printStackTrace();
						IJ.error(e.getMessage());
					}
				}
			};

			startTask(task, numCases);

			count += numCases;
		}

		quicksort(result, 0, result.length - 1);

		if (doLogging)
			for (int i = 0; i < result.length; i++)
				IJ.log("x:" + result[i].shift.x + " y:" + result[i].shift.y + " z:" + result[i].shift.z + " overlap:" + result[i].overlappingPixels + " R:" + result[i].R + " Peak:" + result[i].PCMValue);

		return result;
	}

	private CrossCorrelationResult3D testCrossCorrelation(Point3D shift, FloatArray3D img1, FloatArray3D img2)
	{
		// init Result Datastructure
		CrossCorrelationResult3D result = new CrossCorrelationResult3D();

		// compute values that will not change during testing
		result.PCMValue = shift.value;
		result.shift = shift;

		int w1 = img1.width;
		int h1 = img1.height;
		int d1 = img1.depth;

		int w2 = img2.width;
		int h2 = img2.height;
		int d2 = img2.depth;

		int sx = shift.x;
		int sy = shift.y;
		int sz = shift.z;

		int offsetImg1X = Math.max(0, -sx); // + max(0, max(0, -sx) - max(0, (w1
											// - w2)/2));
		int offsetImg1Y = Math.max(0, -sy); // + max(0, max(0, -sy) - max(0, (h1
											// - h2)/2));
		int offsetImg1Z = Math.max(0, -sz); // + max(0, max(0, -sy) - max(0, (h1
											// - h2)/2));
		int offsetImg2X = Math.max(0, sx); // + max(0, max(0, sx) - max(0, (w2
											// - w1)/2));
		int offsetImg2Y = Math.max(0, sy); // + max(0, max(0, sy) - max(0, (h2
											// - h1)/2));
		int offsetImg2Z = Math.max(0, sz); // + max(0, max(0, sy) - max(0, (h2
											// - h1)/2));

		int count = 0;
		float pixel1, pixel2;

		// iterate over overlapping region
		// first the average

		double avg1 = 0, avg2 = 0;

		int startX = Math.max(offsetImg1X, offsetImg2X);
		int startY = Math.max(offsetImg1Y, offsetImg2Y);
		int startZ = Math.max(offsetImg1Z, offsetImg2Z);
		int endX = Math.min(offsetImg1X + w1, offsetImg2X + w2);
		int endY = Math.min(offsetImg1Y + h1, offsetImg2Y + h2);
		int endZ = Math.min(offsetImg1Z + d1, offsetImg2Z + d2);

		// for direct array addressing
		int arrayOffsetY1 = img1.getPos(0, 1, 0);
		int arrayOffsetY2 = img2.getPos(0, 1, 0);
		int arrayOffsetZ1 = img1.getPos(0, 0, 1);
		int arrayOffsetZ2 = img2.getPos(0, 0, 1);

		int off1 = img1.getPos(startX - offsetImg1X, startY - offsetImg1Y, startZ - offsetImg1Z);
		int off2 = img2.getPos(startX - offsetImg2X, startY - offsetImg2Y, startZ - offsetImg2Z);
		int oldZ1, oldZ2, oldY1, oldY2;

		for (int z = startZ; z < endZ; z++)
		{
			oldZ1 = off1;
			oldZ2 = off2;

			for (int y = startY; y < endY; y++)
			{
				oldY1 = off1;
				oldY2 = off2;

				for (int x = startX; x < endX; x++)
				{
					// pixel1 = img1.getZero(x - offsetImg1X, y - offsetImg1Y, z
					// - offsetImg1Z);
					// pixel2 = img2.getZero(x - offsetImg2X, y - offsetImg2Y, z
					// - offsetImg2Z);
					pixel1 = img1.data[off1++];
					pixel2 = img2.data[off2++];

					avg1 += pixel1;
					avg2 += pixel2;
					count++;
				}

				off1 = oldY1 + arrayOffsetY1;
				off2 = oldY2 + arrayOffsetY2;
			}

			off1 = oldZ1 + arrayOffsetZ1;
			off2 = oldZ2 + arrayOffsetZ2;
		}

		// if less than 1% is overlapping
		if (count <= (Math.min(w1 * h1 * d1, w2 * h2 * d2)) * minOverlap)
		{
			// IJ.log("lower than 1%");
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
		off1 = img1.getPos(startX - offsetImg1X, startY - offsetImg1Y, startZ - offsetImg1Z);
		off2 = img2.getPos(startX - offsetImg2X, startY - offsetImg2Y, startZ - offsetImg2Z);

		for (int z = startZ; z < endZ; z++)
		{
			oldZ1 = off1;
			oldZ2 = off2;

			for (int y = startY; y < endY; y++)
			{
				oldY1 = off1;
				oldY2 = off2;

				for (int x = startX; x < endX; x++)
				{
					// pixel1 = img1.getZero(x - offsetImg1X, y - offsetImg1Y, z
					// - offsetImg1Z);
					// pixel2 = img2.getZero(x - offsetImg2X, y - offsetImg2Y, z
					// - offsetImg2Z);
					pixel1 = img1.data[off1++];
					pixel2 = img2.data[off2++];

					pixelSSQ = Math.pow(pixel1 - pixel2, 2);
					SSQ += pixelSSQ;
					count++;

					dist1 = pixel1 - avg1;
					dist2 = pixel2 - avg2;

					coVar += dist1 * dist2;
					var1 += dist1 * dist1;
					var2 += dist2 * dist2;
				}

				off1 = oldY1 + arrayOffsetY1;
				off2 = oldY2 + arrayOffsetY2;
			}

			off1 = oldZ1 + arrayOffsetZ1;
			off2 = oldZ2 + arrayOffsetZ2;
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
		result.shift = shift;

		// IJ.log("returning result");

		return result;
	}

	private ArrayList<Point3D> findPeaks(FloatArray3D invPCM, Point3D img1, Point3D img2, Point3D ext1, Point3D ext2)
	{
		int w = invPCM.width;
		int h = invPCM.height;
		int d = invPCM.depth;

		int xs, ys, zs, xt, yt, zt;
		float value;

		ArrayList<Point3D> peaks = new ArrayList<Point3D>();

		for (int j = 0; j < checkPeaks; j++)
			peaks.add(new Point3D(0, 0, 0, Float.MIN_VALUE));

		for (int z = 0; z < d; z++)
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					if (isLocalMaxima(invPCM, x, y, z))
					{
						value = invPCM.get(x, y, z);
						Point3D insert = null;
						int insertPos = -1;

						Iterator<Point3D> i = peaks.iterator();
						boolean wasBigger = true;

						while (i.hasNext() && wasBigger)
						{
							if (value > i.next().value)
							{
								if (insert == null) insert = new Point3D(0, 0, 0, value);

								insertPos++;
							}
							else wasBigger = false;
						}

						if (insertPos >= 0) peaks.add(insertPos + 1, insert);

						// remove lowest peak
						if (peaks.size() > checkPeaks) peaks.remove(0);

						if (insert != null)
						{

							// find relative to the left upper front corners of
							// both images
							xt = x + (img1.x - img2.x) / 2 - (ext1.x - ext2.x) / 2;

							if (xt >= w / 2) xs = xt - w;
							else xs = xt;

							yt = y + (img1.y - img2.y) / 2 - (ext1.y - ext2.y) / 2;

							if (yt >= h / 2) ys = yt - h;
							else ys = yt;

							zt = z + (img1.z - img2.z) / 2 - (ext1.z - ext2.z) / 2;

							if (zt >= d / 2) zs = zt - d;
							else zs = zt;

							insert.x = xs;
							insert.y = ys;
							insert.z = zs;
						}
					}

		return peaks;
	}

	private boolean isLocalMaxima(FloatArray3D invPCM, int x, int y, int z)
	{
		int width = invPCM.width;
		int height = invPCM.height;
		int depth = invPCM.depth;

		boolean isMax = true;
		float value = invPCM.get(x, y, z);

		if (x > 0 && y > 0 && z > 0 && x < width - 1 && y < height - 1 && z < depth - 1)
		{
			for (int xs = x - 1; xs <= x + 1 && isMax; xs++)
				for (int ys = y - 1; ys <= y + 1 && isMax; ys++)
					for (int zs = z - 1; zs <= z + 1 && isMax; zs++)
						if (!(x == xs && y == ys && z == zs)) if (invPCM.get(xs, ys, zs) > value) isMax = false;
		}
		else
		{
			int xt, yt, zt;

			for (int xs = x - 1; xs <= x + 1 && isMax; xs++)
				for (int ys = y - 1; ys <= y + 1 && isMax; ys++)
					for (int zs = z - 1; zs <= z + 1 && isMax; zs++)
						if (!(x == xs && y == ys && z == zs))
						{
							xt = xs;
							yt = ys;
							zt = zs;

							if (xt == -1) xt = width - 1;
							if (yt == -1) yt = height - 1;
							if (zt == -1) zt = depth - 1;

							if (xt == width) xt = 0;
							if (yt == height) yt = 0;
							if (zt == depth) zt = 0;

							if (invPCM.get(xt, yt, zt) > value) isMax = false;
						}
		}

		return isMax;
	}

	private FloatArray3D applyROI(ImagePlus imp, Point3D imgDim, Point3D extDim, String handleRGB, boolean windowing)
	{
		FloatArray3D stack;

		if (imp.getRoi() == null)
		{
			// there are no rois....
			stack = StackToFloatArray(imp.getStack(), handleRGB);
		}
		else
		{
			Roi r = imp.getRoi();

			int x = r.getBoundingRect().x;
			int y = r.getBoundingRect().y;
			int w = r.getBoundingRect().width;
			int h = r.getBoundingRect().height;

			stack = StackToFloatArray(imp.getStack(), handleRGB, x, y, w, h);
		}

		imgDim.x = stack.width;
		imgDim.y = stack.height;
		imgDim.z = stack.depth;

		extDim.x = extDim.y = extDim.z = 0;

		if (windowing)
		{
			int imgW = stack.width;
			int imgH = stack.height;
			int imgD = stack.depth;
			int extW = imgW / 4;
			int extH = imgH / 4;
			int extD = imgD / 4;

			// add an even number so that both sides extend equally
			if (extW % 2 != 0) extW++;
			if (extH % 2 != 0) extH++;
			if (extD % 2 != 0) extD++;

			extDim.x = extW;
			extDim.y = extH;
			extDim.z = extD;

			imgDim.x += extDim.x;
			imgDim.y += extDim.y;
			imgDim.z += extDim.z;

			// extend images
			stack = extendImageMirror(stack, imgW + extW, imgH + extH, imgD + extD);
		}
		return stack;
	}

	private boolean checkRoi(ImagePlus imp1, ImagePlus imp2)
	{
		boolean img1HasROI, img2HasROI;

		if (imp1.getRoi() != null) img1HasROI = true;
		else img1HasROI = false;

		if (imp2.getRoi() != null) img2HasROI = true;
		else img2HasROI = false;

		if (img1HasROI && !img2HasROI || img2HasROI && !img1HasROI)
		{
			IJ.error("Either both images should have a ROI or none of them.");
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
	 * -----------------------------------------------------------------------------------------
	 * Here are the general static methods needed by the plugin Just to pronouce
	 * that they are not directly related, I put them in a seperate child class
	 * -----------------------------------------------------------------------------------------
	 */

	private void exponentialWindow(FloatArray3D img)
	{
		double a = 1000;

		// create lookup table
		double weightsX[] = new double[img.width];
		double weightsY[] = new double[img.height];
		double weightsZ[] = new double[img.depth];

		for (int x = 0; x < img.width; x++)
		{
			double relPos = (double) x / (double) (img.width - 1);

			if (relPos <= 0.5) weightsX[x] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else weightsX[x] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
		{
			double relPos = (double) y / (double) (img.height - 1);

			if (relPos <= 0.5) weightsY[y] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else weightsY[y] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int z = 0; z < img.depth; z++)
		{
			double relPos = (double) z / (double) (img.depth - 1);

			if (relPos <= 0.5) weightsZ[z] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else weightsZ[z] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int z = 0; z < img.depth; z++)
			for (int y = 0; y < img.height; y++)
				for (int x = 0; x < img.width; x++)
					img.set((float) (img.get(x, y, z) * weightsX[x] * weightsY[y] * weightsZ[z]), x, y, z);
	}

	private FloatArray3D extendImageMirror(FloatArray3D ip, int width, int height, int depth)
	{
		FloatArray3D image = new FloatArray3D(width, height, depth);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;
		int offsetZ = (depth - ip.depth) / 2;

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

		if (offsetZ < 0)
		{
			IJ.error("Stitching_3D.extendImageMirror(): Extended size in Z smaller than image! " + depth + " < " + ip.depth);
			return null;
		}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++)
					image.set(ip.getMirror(x - offsetX, y - offsetY, z - offsetZ), x, y, z);

		return image;
	}

	private FloatArray3D StackToFloatArray(ImageStack stack, String handleRGB)
	{
		Object[] imageStack = stack.getImageArray();
		int width = stack.getWidth();
		int height = stack.getHeight();
		int nstacks = stack.getSize();

		int rgbType = -1;

		if (imageStack == null || imageStack.length == 0)
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (imageStack[0] instanceof int[])
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

		FloatArray3D pixels = new FloatArray3D(width, height, nstacks);
		int count;

		if (imageStack[0] instanceof byte[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			byte[] pixelTmp = (byte[]) imageStack[countSlice];
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y, countSlice)] = (float) (pixelTmp[count++] & 0xff);
		}
		else if (imageStack[0] instanceof short[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			short[] pixelTmp = (short[]) imageStack[countSlice];
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y, countSlice)] = (float) (pixelTmp[count++] & 0xffff);
		}
		else if (imageStack[0] instanceof float[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			float[] pixelTmp = (float[]) imageStack[countSlice];
			count = 0;

			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y, countSlice)] = pixelTmp[count++];
		}
		else if (imageStack[0] instanceof int[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			int[] pixelTmp = (int[]) imageStack[countSlice];
			count = 0;
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
					pixels.data[pixels.getPos(x, y, countSlice)] = getPixelValueRGB(pixelTmp[count++], rgbType);
		}
		else
		{
			IJ.error("StackToFloatArray: Unknown image type.");
			return null;
		}

		return pixels;
	}

	private FloatArray3D StackToFloatArray(ImageStack stack, String handleRGB, int xCrop, int yCrop, int wCrop, int hCrop)
	{
		Object[] imageStack = stack.getImageArray();
		int width = stack.getWidth();
		int nstacks = stack.getSize();

		int rgbType = -1;

		if (imageStack == null || imageStack.length == 0)
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (imageStack[0] instanceof int[])
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

		FloatArray3D pixels = new FloatArray3D(wCrop, hCrop, nstacks);

		if (imageStack[0] instanceof byte[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			byte[] pixelTmp = (byte[]) imageStack[countSlice];

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = (float) (pixelTmp[x + y * width] & 0xff);
		}
		else if (imageStack[0] instanceof short[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			short[] pixelTmp = (short[]) imageStack[countSlice];

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = (float) (pixelTmp[x + y * width] & 0xffff);
		}
		else if (imageStack[0] instanceof float[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			float[] pixelTmp = (float[]) imageStack[countSlice];

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = pixelTmp[x + y * width];
		}
		else if (imageStack[0] instanceof int[]) for (int countSlice = 0; countSlice < nstacks; countSlice++)
		{
			int[] pixelTmp = (int[]) imageStack[countSlice];

			for (int y = yCrop; y < yCrop + hCrop; y++)
				for (int x = xCrop; x < xCrop + wCrop; x++)
					pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = getPixelValueRGB(pixelTmp[x + y * width], rgbType);
		}
		else
		{
			IJ.error("StackToFloatArray(Crop): Unknown image type.");
			return null;
		}

		return pixels;
	}

	private ImagePlus FloatArrayToStack(FloatArray3D image, String name, float min, float max)
	{
		int width = image.width;
		int height = image.height;
		int nstacks = image.depth;

		ImageStack stack = new ImageStack(width, height);

		for (int slice = 0; slice < nstacks; slice++)
		{
			ImagePlus impResult = IJ.createImage("Result", "32-Bit Black", width, height, 1);
			ImageProcessor ipResult = impResult.getProcessor();
			float[] sliceImg = new float[width * height];

			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					sliceImg[y * width + x] = image.get(x, y, slice);

			ipResult.setPixels(sliceImg);

			if (min == max) ipResult.resetMinAndMax();
			else ipResult.setMinAndMax(min, max);

			stack.addSlice("Slice " + slice, ipResult);
		}

		return new ImagePlus(name, stack);
	}
}
