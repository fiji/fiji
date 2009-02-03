
package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005,2006,2007,2008 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
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

/*====================================================================
|   bUnwarpJDialog
\===================================================================*/

/*------------------------------------------------------------------*/

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Choice;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

/**
 * Class to create the dialog for bUnwarpJ.
 */
public class bUnwarpJDialog extends GenericDialog
{ /* begin class bUnwarpJDialog */

	/*....................................................................
       Private variables
    ....................................................................*/

	/** List of available images in ImageJ */
	private ImagePlus[] imageList;

	// Image representations (canvas and ImagePlus)
	/** Canvas of the source image */
	private ImageCanvas sourceIc;
	/** Canvas of the target image */
	private ImageCanvas targetIc;
	/** Image representation for source image */
	private ImagePlus sourceImp;
	/** Image representation for target image */
	private ImagePlus targetImp;

	// Original image processors
	/** initial source image processor */
	private ImageProcessor originalSourceIP;
	/** initial target image processor */
	private ImageProcessor originalTargetIP;


	// Image models
	/** Model for source image */
	private bUnwarpJImageModel source;
	/** Model for target image */
	private bUnwarpJImageModel target;

	// Image Masks
	/** Mask for source image */
	private bUnwarpJMask       sourceMsk;
	/** Mask for target image */
	private bUnwarpJMask       targetMsk;

	// Initial Affine Matrices
	/** Initial affine matrix for the source image */
	private double[][] sourceAffineMatrix = null;
	/** Initial affine matrix for the target image */
	private double[][] targetAffineMatrix = null;

	// Point handlers for the landmarks
	/** Point handlers for the landmarks in the source image */
	private bUnwarpJPointHandler sourcePh;
	/** Point handlers for the landmarks in the target image */
	private bUnwarpJPointHandler targetPh;


	/** Boolean for clearing mask */
	private boolean clearMask = false;
	/** Toolbar handler */
	private bUnwarpJPointToolbar tb
	= new bUnwarpJPointToolbar(Toolbar.getInstance(),this);

	// Final action
	/** flag to see if the finalAction was launched */
	private boolean finalActionLaunched=false;
	/** flag to stop the registration */
	private boolean stopRegistration=false;

	/** index of the source choice */
	private int sourceChoiceIndex = 0;
	/** index of the target choice */
	private int targetChoiceIndex = 1;

	/** minimum scale deformation */
	private int min_scale_deformation = 0;
	/** maximum scale deformation */
	private int max_scale_deformation = 2;
	/** mode ("Accurate" by default) */
	private int mode = 1;

	// Transformation parameters
	/** divergence weight */
	private double  divWeight                  = 0;
	/** curl weight */
	private double  curlWeight                 = 0;
	/** landmarks weight */
	private double  landmarkWeight             = 0;
	/** image similarity weight */
	private double  imageWeight                = 1;
	/** consistency weight */
	private double  consistencyWeight          = 10;
	/** flag for rich output (verbose option) */
	private boolean richOutput                 = false;
	/** flag for save transformation option */
	private boolean saveTransformation         = false;
	/** minimum image scale */
	private int     min_scale_image            = 0;
	/** maximum depth for the image pyramid */
	private int     imagePyramidDepth          = 3;
	/** stopping threshold */
	private double  stopThreshold      = 1e-2;

	/** consistency flag */
	private boolean bIsReverse = true;

	/** macro flag */
	private boolean bMacro = false;

	/** region of interest of the source image before calling the plugin */
	private Roi previousSourceRoi;
	/** region of interest of the target image before calling the plugin */
	private Roi previousTargetRoi;

	/*....................................................................
       Public methods
    ....................................................................*/

	/*------------------------------------------------------------------*/
	/**
	 * Create a new instance of bUnwarpJDialog.
	 *
	 * @param parentWindow pointer to the parent window
	 * @param imageList list of images from ImageJ
	 * @param mode default registration mode (0 = Fast, 1 = Accurate)
	 * @param min_scale_deformation default minimum scale deformation value
	 * @param max_scale_deformation default maximum scale deformation value
	 * @param divWeight default divergence weight
	 * @param curlWeight default curl weight
	 * @param landmarkWeight default landmarks weight
	 * @param imageWeight default image similarity weight
	 * @param consistencyWeight default consistency weight
	 * @param stopThreshold default stopping threshold
	 * @param richOutput default verbose flag
	 * @param saveTransformation default save transformations flag
	 */
	public bUnwarpJDialog (
			final Frame parentWindow,
			final ImagePlus[] imageList,
			final int mode,
			final int min_scale_deformation,
			final int max_scale_deformation,
			final double divWeight,
			final double curlWeight,
			final double landmarkWeight,
			final double imageWeight,
			final double consistencyWeight,
			final double stopThreshold,
			final boolean richOutput,
			final boolean saveTransformation)
	{
		super("bUnwarpJ", null);
		setModal(false);

		this.imageList 				= imageList;
		this.mode					= mode;
		this.min_scale_deformation 	= min_scale_deformation;
		this.max_scale_deformation 	= max_scale_deformation;
		this.divWeight 			   	= divWeight;
		this.curlWeight 			= curlWeight;
		this.landmarkWeight        	= landmarkWeight;
		this.imageWeight           	= imageWeight;
		this.consistencyWeight     	= consistencyWeight;
		this.stopThreshold         	= stopThreshold;
		this.richOutput 			= richOutput;
		this.saveTransformation		= saveTransformation;


		// We create a list of image titles to be used as source or target images
		String[] titles = new String[imageList.length];
		for ( int i = 0; i < titles.length; ++i )
			titles[i] = imageList[i].getTitle();

		// Source and target choices
		addChoice( "Source_Image", titles, titles[sourceChoiceIndex]);
		addChoice( "Target_Image", titles, titles[targetChoiceIndex]);

		// Registration Mode
		String[] sRegistrationModes = { "Fast", "Accurate" };
		addChoice("Registration Mode", sRegistrationModes, sRegistrationModes[this.mode]);

		// Advanced Options
		addMessage("----- Advanced Options -----");
		String[] sMinScaleDeformationChoices = { "Very Coarse", "Coarse", "Fine", "Very Fine" };
		addChoice("Initial_Deformation :", sMinScaleDeformationChoices, sMinScaleDeformationChoices[this.min_scale_deformation]);	   
		String[] sMaxScaleDeformationChoices = { "Very Coarse", "Coarse", "Fine", "Very Fine", "Super Fine" };
		addChoice("Final_Deformation :", sMaxScaleDeformationChoices, sMaxScaleDeformationChoices[this.max_scale_deformation]);
		addNumericField("Divergence_Weight :", this.divWeight, 1);
		addNumericField("Curl_Weight :", this.curlWeight, 1);
		addNumericField("Landmark_Weight :", this.landmarkWeight, 1);
		addNumericField("Image_Weight :", this.imageWeight, 1);
		addNumericField("Consistency_Weight :", this.consistencyWeight, 1);
		addNumericField("Stop_Threshold :", this.stopThreshold, 2);
		addCheckbox(" Verbose ", this.richOutput);
		addCheckbox(" Save_Transformations ", this.saveTransformation);       	  

		// Check if it is a macro call
		this.bMacro = Macro.getOptions() != null;

		// Start concurrent image processing threads       
		createSourceImage(bIsReverse);
		createTargetImage();
		loadPointRoiAsLandmarks();
		setSecondaryPointHandlers();	   

	} /* end bUnwarpJDialog (constructor) */



	/*------------------------------------------------------------------*/
	/**
	 * Set source Mask.
	 *
	 * @param sFileName source mask file name
	 */
	public void setSourceMask (String sFileName)
	{
		this.sourceMsk.readFile(sFileName);
	} /* end setSourceMask */

	/*------------------------------------------------------------------*/
	/**
	 * Set source intial affine matrix.
	 *
	 * @param affineMatrix initial affine matrix
	 */
	public void setSourceAffineMatrix (double[][] affineMatrix)
	{
		this.sourceAffineMatrix = affineMatrix;
	} /* end setSourceInitialMatrix */

	/*------------------------------------------------------------------*/
	/**
	 * Get source Mask.
	 */
	public bUnwarpJMask getSourceMask ()
	{
		return this.sourceMsk;
	} /* end getSourceMask */

	/*------------------------------------------------------------------*/
	/**
	 * Get target Mask.
	 */
	public bUnwarpJMask getTargetMask ()
	{
		return this.targetMsk;
	}   /* end getTargetMask */

	/*------------------------------------------------------------------*/
	/**
	 * Actions to be taken during the dialog.
	 */
	public synchronized void actionPerformed (final ActionEvent ae)
	{
		super.actionPerformed(ae);
		if (wasOKed() || wasCanceled())
			notify();
	} /* end actionPerformed */

	/*------------------------------------------------------------------*/
	/**
	 * Show main bUnwarpJ dialog
	 * 
	 */
	public synchronized void showDialog() {
		super.showDialog();
		if (Macro.getOptions() != null)
			return;
		try {
			wait();
		} catch (InterruptedException e) {
			IJ.error("Dialog " + getTitle() + " was interrupted.");
		}
	}

	/*------------------------------------------------------------------*/
	/**
	 * Action to be taken when choices change.
	 * 
	 * @param e item event
	 * 
	 */
	public void itemStateChanged(ItemEvent e) 
	{
		super.itemStateChanged(e);
		Object o = e.getSource();

		if(!(o instanceof Choice))
			return;

		Choice originChoice = (Choice) o;


		// Change in the source image choice (choice 0 = Source_Image)
		if(originChoice == (Choice)super.choice.get(0))
		{
			final int newChoiceIndex = originChoice.getSelectedIndex();
			if (sourceChoiceIndex != newChoiceIndex)
			{
				stopSourceThread();
				if (targetChoiceIndex != newChoiceIndex)
				{
					sourceChoiceIndex = newChoiceIndex;
					cancelSource();
					targetPh.removePoints();
					// Restore previous target roi
					targetImp.setRoi(this.previousTargetRoi);
					createSourceImage(bIsReverse);
					loadPointRoiAsLandmarks();
					setSecondaryPointHandlers();
				}
				else
				{
					stopTargetThread();
					targetChoiceIndex = sourceChoiceIndex;
					sourceChoiceIndex = newChoiceIndex;
					// Target choice is the second choice in the vector
					((Choice)super.choice.get(1)).select(targetChoiceIndex);
					permuteImages(bIsReverse);
				}

			}
		}
		// Change in the target image choice (Target_Image = choice 1)
		else if(originChoice == (Choice)super.choice.get(1))
		{
			final int newChoiceIndex = originChoice.getSelectedIndex();
			if (targetChoiceIndex != newChoiceIndex)
			{
				stopTargetThread();
				if (sourceChoiceIndex != newChoiceIndex)
				{
					targetChoiceIndex = newChoiceIndex;
					cancelTarget();
					sourcePh.removePoints();
					// Restore previous source roi
					sourceImp.setRoi(this.previousSourceRoi);
					createTargetImage();
					loadPointRoiAsLandmarks();
					setSecondaryPointHandlers();
				}
				else
				{
					stopSourceThread();
					sourceChoiceIndex = targetChoiceIndex;
					targetChoiceIndex = newChoiceIndex;
					// Source choice is the first choice in the vector
					((Choice)super.choice.get(0)).select(sourceChoiceIndex);
					permuteImages(bIsReverse);
				}
			}
		}
		// Change in the minimum scale deformation choice (= choice 3)
		else if(originChoice == (Choice)super.choice.get(3))
		{
			final int new_min_scale_deformation = originChoice.getSelectedIndex();
			int new_max_scale_deformation = max_scale_deformation;
			if (max_scale_deformation < new_min_scale_deformation)
				new_max_scale_deformation = new_min_scale_deformation;
			if (new_min_scale_deformation != min_scale_deformation ||
					new_max_scale_deformation != max_scale_deformation) 
			{
				min_scale_deformation = new_min_scale_deformation;
				max_scale_deformation = new_max_scale_deformation;
				computeImagePyramidDepth();
				restartModelThreads(bIsReverse);
			}
			// Update minimum scale deformation choice
			((Choice)super.choice.get(3)).select(min_scale_deformation);
			// Update maximum scale deformation choice
			((Choice)super.choice.get(4)).select(max_scale_deformation);
		}
		// Change in the maximum scale deformation choice (= choice 4)
		else if(originChoice == (Choice)super.choice.get(4))
		{
			final int new_max_scale_deformation = originChoice.getSelectedIndex();
			int new_min_scale_deformation = min_scale_deformation;
			if (new_max_scale_deformation < min_scale_deformation)
				new_min_scale_deformation=new_max_scale_deformation;
			if (new_max_scale_deformation != max_scale_deformation ||
					new_min_scale_deformation != min_scale_deformation) 
			{
				min_scale_deformation = new_min_scale_deformation;
				max_scale_deformation = new_max_scale_deformation;
				computeImagePyramidDepth();
				restartModelThreads(bIsReverse);
			}
			// Update maximum scale deformation choice
			((Choice)super.choice.get(4)).select(max_scale_deformation);
			// Update minimum scale deformation choice
			((Choice)super.choice.get(3)).select(min_scale_deformation);
		}


	} /* end itemStateChanged */
	/*------------------------------------------------------------------*/
	/**
	 * Apply the transformation defined by the spline coefficients to the source
	 * image.
	 *
	 * @param intervals intervals in the deformation
	 * @param cx b-spline X- coefficients
	 * @param cy b-spline Y- coefficients
	 */
	public void applyTransformationToSource(
			int intervals,
			double [][]cx,
			double [][]cy)
	{
		// Apply transformation
		bUnwarpJMiscTools.applyTransformationToSource(
				this.sourceImp, this.targetImp, this.source, intervals, cx, cy);

		// Restart the computation of the model
		cancelSource();
		this.targetPh.removePoints();

		createSourceImage(false);

		setSecondaryPointHandlers();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Apply a raw transformation to the source image.
	 *
	 * @param transformation_x X- mapping
	 * @param transformation_y Y- mapping
	 */
	public void applyRawTransformationToSource(
			double [][] transformation_x,
			double [][] transformation_y)
	{
		// Apply transformation
		bUnwarpJMiscTools.applyRawTransformationToSource(this.sourceImp, this.targetImp, this.source, transformation_x, transformation_y);

		// Restart the computation of the model
		cancelSource();
		this.targetPh.removePoints();

		createSourceImage(false);

		setSecondaryPointHandlers();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Free the memory used in the program.
	 */
	public void freeMemory()
	{
		imageList    = null;
		sourceIc     = null;
		targetIc     = null;
		sourceImp    = null;
		targetImp    = null;
		source       = null;
		target       = null;
		sourcePh     = null;
		targetPh     = null;
		tb           = null;
		Runtime.getRuntime().gc();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Method to color the area of the mask.
	 *
	 * @param ph image point handler
	 */
	public void grayImage(final bUnwarpJPointHandler ph)
	{
		if (ph==sourcePh)
		{
			int Xdim=source.getWidth();
			int Ydim=source.getHeight();
			FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
			int ij=0;
			double []source_data=source.getImage();
			for (int i=0; i<Ydim; i++)
				for (int j=0; j<Xdim; j++,ij++)
					if (sourceMsk.getValue(j,i))
						fp.putPixelValue(j,i,    source_data[ij]);
					else fp.putPixelValue(j,i,0.5*source_data[ij]);
			fp.resetMinAndMax();
			sourceImp.setProcessor(sourceImp.getTitle(),fp);
			sourceImp.updateImage();
		}
		else
		{
			int Xdim=target.getWidth();
			int Ydim=target.getHeight();
			FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
			double []target_data=target.getImage();
			int ij=0;
			for (int i=0; i<Ydim; i++)
				for (int j=0; j<Xdim; j++,ij++)
					if (targetMsk.getValue(j,i))
						fp.putPixelValue(j,i,    target_data[ij]);
					else fp.putPixelValue(j,i,0.5*target_data[ij]);
			fp.resetMinAndMax();
			targetImp.setProcessor(targetImp.getTitle(),fp);
			targetImp.updateImage();
		}
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get finalActionLaunched flag.
	 */
	public boolean isFinalActionLaunched () {return finalActionLaunched;}

	/*------------------------------------------------------------------*/
	/**
	 * Get clearMask flag.
	 */
	public boolean isClearMaskSet () {return clearMask;}

	/*------------------------------------------------------------------*/
	/**
	 * Get saveTransformation flag.
	 */
	public boolean isSaveTransformationSet () {return saveTransformation;}
	/*------------------------------------------------------------------*/
	/**
	 * Set saveTransformation flag.
	 */
	public void setSaveTransformation(boolean b) 
	{ 
		this.saveTransformation = b;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get stopRegistration flag.
	 */
	public boolean isStopRegistrationSet () {return stopRegistration;}

	/*------------------------------------------------------------------*/
	/**
	 * Join the threads for the source and target images.
	 */
	public void joinThreads ()
	{
		try
		{
			source.getThread().join();
			target.getThread().join();
		}
		catch (InterruptedException e)
		{
			IJ.error("Unexpected interruption exception" + e);
		}
	} /* end joinSourceThread */

	/*------------------------------------------------------------------*/
	/**
	 * Restore the initial conditions.
	 */
	public void restoreAll ()
	{
		ungrayImage(sourcePh.getPointAction());
		ungrayImage(targetPh.getPointAction());
		cancelSource();
		cancelTarget();
		tb.restorePreviousToolbar();
		Toolbar.getInstance().repaint();
		bUnwarpJProgressBar.resetProgressBar();
		Runtime.getRuntime().gc();
	} /* end restoreAll */

	/*------------------------------------------------------------------*/
	/**
	 * Set the clearMask flag.
	 */
	public void setClearMask (boolean val) {clearMask = val;}

	/*------------------------------------------------------------------*/
	/**
	 * Set the stopRegistration flag to true.
	 */
	public void setStopRegistration () {stopRegistration = true;}

	/*------------------------------------------------------------------*/
	/**
	 * Get the source initial affine matrix.
	 */
	public double[][] getSourceAffineMatrix () {return this.sourceAffineMatrix;}

	/*------------------------------------------------------------------*/
	/**
	 * Get the target initial affine matrix.
	 */
	public double[][] getTargetAffineMatrix () {return this.targetAffineMatrix;}


	/*------------------------------------------------------------------*/
	/**
	 * Ungray image. It restores the original version of the image (without mask).
	 *
	 * @param pa point action pointer
	 */
	public void ungrayImage(final bUnwarpJPointAction pa)
	{
		if (pa == sourcePh.getPointAction()) 
		{
			/*
          int Xdim=source.getWidth();
          int Ydim=source.getHeight();
          FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          int ij=0;
          double []source_data=source.getImage();
          for (int i=0; i<Ydim; i++)
             for (int j=0; j<Xdim; j++,ij++)
                 fp.putPixelValue(j,i,source_data[ij]);
          fp.resetMinAndMax();
          sourceImp.setProcessor(sourceImp.getTitle(),fp);
			 */
			this.sourceImp.setProcessor(sourceImp.getTitle(), this.originalSourceIP);
			sourceImp.updateImage();
		} 
		else 
		{
			/*
          int Xdim=target.getWidth();
          int Ydim=target.getHeight();
          FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          double []target_data=target.getImage();
          int ij=0;
          for (int i=0; i<Ydim; i++)
             for (int j=0; j<Xdim; j++,ij++)
                 fp.putPixelValue(j,i,target_data[ij]);
          fp.resetMinAndMax();

          targetImp.setProcessor(targetImp.getTitle(),fp);
			 */
			this.targetImp.setProcessor(this.targetImp.getTitle(), this.originalTargetIP);
			targetImp.updateImage();
		}
	}

	/*....................................................................
       Private methods
    ....................................................................*/

	/*------------------------------------------------------------------*/
	/**
	 * Add the image list to the list of choices.
	 *
	 * @param choice list of choices
	 */
	private void addImageList (final Choice choice)
	{
		for (int k = 0; (k < imageList.length); k++)
			choice.add(imageList[k].getTitle());
	} /* end addImageList */

	/*------------------------------------------------------------------*/
	/**
	 * Close all the variables related to the source image.
	 */
	private void cancelSource ()
	{
		sourcePh.killListeners();
		sourcePh  = null;
		sourceIc  = null;
		sourceImp.killRoi();
		// Restore previous roi
		sourceImp.setRoi(this.previousSourceRoi);

		sourceImp = null;
		source    = null;
		sourceMsk = null;
		Runtime.getRuntime().gc();
	} /* end cancelSource */

	/*------------------------------------------------------------------*/
	/**
	 * Close all the variables related to the target image.
	 */
	private void cancelTarget ()
	{
		targetPh.killListeners();
		targetPh  = null;
		targetIc  = null;
		targetImp.killRoi();
		// Restore previous roi
		targetImp.setRoi(this.previousTargetRoi);

		targetImp = null;
		target    = null;
		targetMsk = null;
		Runtime.getRuntime().gc();
	} /* end cancelTarget */

	/*------------------------------------------------------------------*/
	/**
	 * Compute the depth of the image resolution pyramid.
	 */
	private void computeImagePyramidDepth ()
	{
		this.imagePyramidDepth = max_scale_deformation - min_scale_deformation + 1;
	}


	/*------------------------------------------------------------------*/
	/**
	 * Create the source image.
	 *
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private void createSourceImage (boolean bIsReverse)
	{    	    
		if (this.bMacro) 
		{
			String macroOptions = Macro.getOptions();
			Choice thisChoice = (Choice)(choice.elementAt(0));
			String item = thisChoice.getSelectedItem();
			item = Macro.getValue(macroOptions, "Source_Image", item);
			for(int i = 0; i < this.imageList.length; i++)
				if((this.imageList[i].getTitle()).equals(item))
				{
					this.sourceChoiceIndex = i;
					break;
				}			
		}	

		sourceImp = imageList[sourceChoiceIndex];
		sourceImp.setSlice(1);

		// Save original image processor
		this.originalSourceIP = this.sourceImp.getProcessor();

		// Create image model to perform registration
		source    =
			new bUnwarpJImageModel(sourceImp.getProcessor(), bIsReverse);

		this.computeImagePyramidDepth();
		source.setPyramidDepth(imagePyramidDepth + min_scale_image);
		source.getThread().start();
		sourceIc  = sourceImp.getWindow().getCanvas();
		
		// If it is an stack, the second slice is considered a mask
		if (sourceImp.getStackSize() == 1) 
		{
			// Create an empty mask
			sourceMsk = new bUnwarpJMask(sourceImp.getProcessor(),false);
		} 
		else 
		{
			// Take the mask from the second slice			
			sourceImp.setSlice(2);
			sourceMsk = new bUnwarpJMask(sourceImp.getProcessor(), true);
			sourceImp.setSlice(1);
		}
		sourcePh  = new bUnwarpJPointHandler(sourceImp, tb, sourceMsk, this);

		tb.setSource(sourceImp, sourcePh);
	} /* end createSourceImage */

	/*------------------------------------------------------------------*/
	/**
	 * Create target image.
	 */
	private void createTargetImage ()
	{
		if (this.bMacro) 
		{
			String macroOptions = Macro.getOptions();
			Choice thisChoice = (Choice)(choice.elementAt(1));
			String item = thisChoice.getSelectedItem();
			item = Macro.getValue(macroOptions, "Target_Image", item);
			for(int i = 0; i < this.imageList.length; i++)
				if((this.imageList[i].getTitle()).equals(item))
				{
					this.targetChoiceIndex = i;
					break;
				}			
		}	

		targetImp = imageList[targetChoiceIndex];
		targetImp.setSlice(1);

		this.originalTargetIP = this.targetImp.getProcessor();

		target    =
			new bUnwarpJImageModel(targetImp.getProcessor(), true);
		
		this.computeImagePyramidDepth();
		target.setPyramidDepth(imagePyramidDepth + min_scale_image);
		target.getThread().start();
		targetIc  = targetImp.getWindow().getCanvas();

		// If it is an stack, the second slice is considered a mask
		if (targetImp.getStackSize()==1) 
		{
			// Create an empty mask
			targetMsk = new bUnwarpJMask(targetImp.getProcessor(), false);
		} 
		else 
		{
			// Take the mask from the second slice
			targetImp.setSlice(2);
			targetMsk = new bUnwarpJMask(targetImp.getProcessor(), true);
			targetImp.setSlice(1);
		}
		targetPh  = new bUnwarpJPointHandler(targetImp, tb, targetMsk, this);
		tb.setTarget(targetImp, targetPh);
	} /* end createTargetImage */


	/*------------------------------------------------------------------*/
	/**
	 * Load point rois in the source and target images as landmarks.
	 */
	private void loadPointRoiAsLandmarks()
	{

		Roi roiSource = this.previousSourceRoi =  sourceImp.getRoi();
		Roi roiTarget = this.previousTargetRoi = targetImp.getRoi();

		if(roiSource instanceof PointRoi && roiTarget instanceof PointRoi)
		{
			PointRoi prSource = (PointRoi) roiSource;
			int[] xSource = prSource.getXCoordinates();

			PointRoi prTarget = (PointRoi) roiTarget;
			int[] xTarget = prTarget.getXCoordinates();

			int numOfPoints = xSource.length;

			// If the number of points in both images is not the same,
			// we do nothing.
			if(numOfPoints != xTarget.length)
				return;

			// Otherwise we load the points in order as landmarks.
			int[] ySource = prSource.getYCoordinates();
			int[] yTarget = prTarget.getYCoordinates();

			// The coordinates from the point rois are relative to the
			// bounding box origin.
			Rectangle recSource = prSource.getBounds();
			int originXSource = recSource.x;
			int originYSource = recSource.y;

			Rectangle recTarget = prTarget.getBounds();
			int originXTarget = recTarget.x;
			int originYTarget = recTarget.y;

			for(int i = 0; i < numOfPoints; i++)
			{
				sourcePh.addPoint(xSource[i] + originXSource, ySource[i] + originYSource);
				targetPh.addPoint(xTarget[i] + originXTarget, yTarget[i] + originYTarget);
			}
		}

	}
	/* end loadPointRoiAsLandmarks */

	/*------------------------------------------------------------------*/
	/**
	 * Permute the pointer for the target and source images.
	 *
	 * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
	 */
	private void permuteImages (boolean bIsReverse)
	{
		// Swap image canvas
		final ImageCanvas swapIc = this.sourceIc;
		this.sourceIc = this.targetIc;
		this.targetIc = swapIc;

		// Swap ImagePlus
		final ImagePlus swapImp = this.sourceImp;
		this.sourceImp = this.targetImp;
		this.targetImp = swapImp;

		// Swap original ImageProcessors
		final ImageProcessor swapIP = this.originalSourceIP;
		this.originalSourceIP = this.originalTargetIP;
		this.originalTargetIP = swapIP;

		// Swap Mask
		final bUnwarpJMask swapMsk = this.sourceMsk;
		this.sourceMsk = this.targetMsk;
		this.targetMsk = swapMsk;

		// Swap Point Handlers
		final bUnwarpJPointHandler swapPh = this.sourcePh;
		this.sourcePh = this.targetPh;
		this.targetPh = swapPh;
		setSecondaryPointHandlers();

		// Swap affine matrices
		double[][] swapMatrix = null;


		if(this.sourceAffineMatrix != null)
			swapMatrix = new double[2][3];
		for(int i = 0; i < 2; i++)
			for(int j = 0; j < 3; j++)
			{
				if(this.sourceAffineMatrix != null)
					swapMatrix[i][j] = this.sourceAffineMatrix [i][j];
				if(this.targetAffineMatrix != null)
					this.sourceAffineMatrix[i][j] = this.targetAffineMatrix[i][j];
			}

		if(swapMatrix != null)
		{
			if (this.targetAffineMatrix == null)
				this.targetAffineMatrix = new double[2][3];
			for(int i = 0; i < 2; i++)
				for(int j = 0; j < 3; j++)
					this.targetAffineMatrix[i][j] = swapMatrix[i][j];
		}

		// Inform the Toolbar about the change
		tb.setSource(this.sourceImp, this.sourcePh);
		tb.setTarget(this.targetImp, this.targetPh);

		// Restart the computation with each image
		this.source = new bUnwarpJImageModel(this.sourceImp.getProcessor(), bIsReverse);
		this.source.setPyramidDepth(imagePyramidDepth + min_scale_image);
		this.source.getThread().start();

		this.target = new bUnwarpJImageModel(this.targetImp.getProcessor(), true);
		this.target.setPyramidDepth(imagePyramidDepth + min_scale_image);
		this.target.getThread().start();
	} /* end permuteImages */

	/*------------------------------------------------------------------*/
	/**
	 * Remove the points from the points handlers of the source and target image.
	 */
	private void removePoints ()
	{
		sourcePh.removePoints();
		targetPh.removePoints();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Re-launch the threads for the image models of the source and target.
	 *
	 * @param bIsReverse boolean variable to indicate the use of consistency
	 */
	private void restartModelThreads (boolean bIsReverse)
	{
		// Stop threads
		stopSourceThread();
		stopTargetThread();

		// Remove the current image models
		source = null;
		target = null;
		Runtime.getRuntime().gc();

		// Now restart the threads
		source    =
			new bUnwarpJImageModel(sourceImp.getProcessor(), bIsReverse);
		source.setPyramidDepth(imagePyramidDepth + min_scale_image);
		source.getThread().start();

		target =
			new bUnwarpJImageModel(targetImp.getProcessor(), true);
		target.setPyramidDepth(imagePyramidDepth + min_scale_image);
		target.getThread().start();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the secondary point handlers.
	 */
	private void setSecondaryPointHandlers ()
	{
		sourcePh.setSecondaryPointHandler(targetImp, targetPh);
		targetPh.setSecondaryPointHandler(sourceImp, sourcePh);
	} /* end setSecondaryPointHandler */

	/*------------------------------------------------------------------*/
	/**
	 * Stop the thread of the source image.
	 */
	private void stopSourceThread ()
	{
		// Stop the source image model
		while (source.getThread().isAlive()) {
			source.getThread().interrupt();
		}
		source.getThread().interrupted();
	} /* end stopSourceThread */

	/*------------------------------------------------------------------*/
	/**
	 * Stop the thread of the target image.
	 */
	private void stopTargetThread ()
	{
		// Stop the target image model
		while (target.getThread().isAlive()) {
			target.getThread().interrupt();
		}
		target.getThread().interrupted();
	} /* end stopTargetThread */

	/*------------------------------------------------------------------*/
	/**
	 * Get source point handler.
	 */
	public bUnwarpJPointHandler getSourcePh() 
	{
		return this.sourcePh;
	} /* end getSourcePh */

	/*------------------------------------------------------------------*/
	/**
	 * Get target point handler.
	 */
	public bUnwarpJPointHandler getTargetPh() 
	{
		return this.targetPh;
	} /* end getTargetPh */

	/*------------------------------------------------------------------*/
	/**
	 * Get source point handler.
	 */
	public bUnwarpJMask getSourceMsk() 
	{
		return this.sourceMsk;
	} /* end getSourceMsk */

	/*------------------------------------------------------------------*/
	/**
	 * Get target point handler.
	 */
	public bUnwarpJMask getTargetMsk() 
	{
		return this.targetMsk;
	} /* end getTargetMsk */

	/*------------------------------------------------------------------*/
	/**
	 * Set final action launched flag.
	 */
	public void setFinalActionLaunched(boolean b) 
	{
		this.finalActionLaunched = b;		
	} /* end setFinalActionLaunched */

	/*------------------------------------------------------------------*/
	/**
	 * Set toolbar tools all up.
	 */
	public void setToolbarAllUp() 
	{
		tb.setAllUp();		
	} /* setToolbarAllUp */

	/*------------------------------------------------------------------*/
	/**
	 * Repaint toolbar.
	 */
	public void repaintToolbar() 
	{
		tb.repaint();
	} /* end repaintToolbar */

	/*------------------------------------------------------------------*/
	/**
	 * Get target image model.
	 */
	public bUnwarpJImageModel getTarget() {
		return this.target;
	} /* end getTarget */

	/*------------------------------------------------------------------*/
	/**
	 * Get original source image process.
	 */
	public ImageProcessor getOriginalSourceIP() {
		return this.originalSourceIP;
	} /* end getOriginalSourceIP */

	/*------------------------------------------------------------------*/
	/**
	 * Get original target image process.
	 */
	public ImageProcessor getOriginalTargetIP() {
		return this.originalTargetIP;
	} /* end getOriginalTargetIP */

	/*------------------------------------------------------------------*/
	/**
	 * Get source image model.
	 */
	public bUnwarpJImageModel getSource() {
		return this.source;
	} /* end getSource */

	/**
	 * Get the macro flag
	 * 
	 * @return macro flag
	 */
	public boolean isMacroCall() {
		return bMacro;
	}

} /* end class bUnwarpJDialog */