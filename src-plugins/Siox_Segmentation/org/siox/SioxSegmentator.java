/*
   Copyright 2005, 2006 by Gerald Friedland, Kristian Jantz and Lars Knipping

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.siox;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.*;

import org.siox.util.*;

/**
 * Image segmentator based on
 *<em>SIOX: Simple Interactive Object Extraction</em>.
 * <P>
 * To segmentate an image one has to perform the following steps.
 * <OL><LI>Construct an instance of <code>SioxSegmentator</code>.
 * </LI><LI>Create a confidence matrix, where each entry marks its
 *      corresponding image pixel to belong to the foreground, to the
 *      background, or being of unknown type.
 * </LI><LI>Call <code>segmentate</code> on the image with the confidence
 *       matrix. This stores the result as new foreground confidence into
 *       the confidence matrix, with each entry being either
 *       zero (<code>CERTAIN_BACKGROUND_CONFIDENCE</code>) or one
 *       (<code>CERTAIN_FOREGROUND_CONFIDENCE</code>).
 * </LI><LI>Optionally call <code>subpixelRefine</code> to areas
 *      where pixels contain both foreground and background (e.g.
 *      object borders or highly detailed features like flowing hairs).
 *      The pixel are then assigned confidence values between zero and
 *      one to give them a measure of "foregroundness".
 *      This step may be repeated as often as needed.
 * </LI></OL>
 * <P>
 * For algorithm documentation refer to
 * G. Friedland, K. Jantz, L. Knipping, R. Rojas:<i>
 * Image Segmentation by Uniform Color Clustering
 *  -- Approach and Benchmark Results</i>,
 * <A HREF="http://www.inf.fu-berlin.de/inst/pubs/tr-b-05-07.pdf">Technical Report B-05-07</A>,
 * Department of Computer Science, Freie Universitaet Berlin, June 2005.<br>
 * <P>
 * See <A HREF="http://www.siox.org/" target="_new">http://www.siox.org</A> for more information.<br>
 * <P>
 * Algorithm idea by Gerald Friedland.
 *
 * @author Gerald Friedland, Kristian Jantz, Lars Knipping
 * @version 1.13
 */
public class SioxSegmentator
{

	// CHANGELOG
	// 2006-26-04 1.13 added method segmentatevideo_firstframe() and segmentatevideo_nextframe()
	//                 for quick video segmentation.
	// 2006-01-16 1.12 fixed bug in subpixelrefine that handled the sure regions improperly
	// 2005-12-05 1.11 minor updates to JavaDoc comments
	// 2005-12-02 1.10 fixed misclassification accidentally introduced in prev. rev
	// 2005-12-02 1.09 more updates to JavaDoc comments
	// 2005-11-15 1.08 minor updates to JavaDoc comments
	// 2005-11-15 1.07 performance improvements to depthFirstSearch and
	//                 fillColorRegions
	// 2005-11-15 1.06 changed ArrayList use to custom IntArrayList for
	//                 improved performance, minor var and method renamings,
	//                 update of javadoc, some fixes to depth-first-searches
	// 2005-11-14 1.05 added method to apply detail refinement to generic areas
	// 2005-11-02 1.04 fixed a bug with multiple foreground objects
	// 2005-11-02 1.03 cleaned up a bit
	// 2005-11-02 1.02 added a few comments
	// 2005-11-01 1.01 fixed redundant code reported by Benjamin N Rhew
	// 2005-10-25 1.00 initial release

	/** Add mode for the subpixel refinement. */
	public static final String ADD_EDGE="add";

	/** Subtract mode for the subpixel refinement. */
	public static final String SUB_EDGE="subtract";

	// The following constants are defined for compatibility with video siox.

	/** Confidence corresponding to a certain foreground region (equals one). */
	public static final float CERTAIN_FOREGROUND_CONFIDENCE=1.0f;

	/** Confidence for a region likely being foreground.*/
	public static final float FOREGROUND_CONFIDENCE=0.8f;

	/** Confidence for foreground or background type being equally likely.*/
	public static final float UNKNOWN_REGION_CONFIDENCE=0.5f;

	/** Confidence for a region likely being background.*/
	public static final float BACKGROUND_CONFIDENCE=0.1f;

	/** Confidence corresponding to a certain background region (equals zero). */
	public static final float CERTAIN_BACKGROUND_CONFIDENCE=0.0f;

	// instance fields:

	/** Horizontal resolution of the image to be segmented. */
	private final int imgWidth;

	/** Vertical resolution of the image to be segmented. */
	private final int imgHeight;

	/** Stores component label (index) by pixel it belongs to. */
	private final int[] labelField;

	/**
	 * LAB color values of pixels that are definitely known background.
	 * Entries are of form {l,a,b}.
	 */
	private final float[][] knownBg;

	/**
	 * LAB color values of pixels that are definitely known foreground.
	 * Entries are of form {l,a,b}.
	 */
	private final float[][] knownFg;

	/** Holds background signature (a characteristic subset of the bg.) */
	private float[][] bgSignature;

	/** Holds foreground signature (a characteristic subset of the fg).*/
	private float[][] fgSignature;

	/** Size of cluster on lab axis. */
	private final float[] limits;

	/** Maximum distance of two lab values. */
	private final float clusterSize;

	/**
	 * Stores Tupels for fast access to nearest background/foreground pixels.
	 */
	private final IntHashMap hs=new IntHashMap();

	/** Size of the biggest blob.*/
	private int regionCount;

	/** Copy of the original image, needed for detail refinement. */
	private int[] origImage;

	/** A flag that stores if the segmentation algorithm has already ran.*/
	private boolean segmentated;

	/**
	 * Constructs a SioxSegmentator Object to be used for image segmentation.
	 *
	 * @param w X resolution of the image to be segmented.
	 * @param h Y resolution of the image to be segmented.
	 * @param limits Size of the cluster on LAB axises.
	 *        If <code>null</code>, the default value {0.64f,1.28f,2.56f}
	 *        is used.
	 */
	public SioxSegmentator(int w, int h, float[] limits)
	{

		imgWidth=w;
		imgHeight=h;
		labelField=new int[imgWidth*imgHeight];
		knownBg=new float[imgWidth*imgHeight][3];
		knownFg=new float[imgWidth*imgHeight][3];

		if (limits==null) {
			this.limits=new float[] {0.64f, 1.28f, 2.56f};
		} else {
			this.limits=limits;
		}
		clusterSize=Utils.sqrEuclidianDist(this.limits, new float[] {-this.limits[0], -this.limits[1], -this.limits[2]});
		segmentated=false;
	}

	/**
	 * Constructs a SioxSegmentator Object to be used for image segmentation.
	 *
	 * @param w X resolution of the image to be segmented.
	 * @param h Y resolution of the image to be segmented.
	 * @param limits Size of the cluster on LAB axises.
	 *        If <code>null</code>, the default value {0.64f,1.28f,2.56f}
	 *        is used.
	 * @param bgSignature background color signature
	 * @param fgSignature foreground color signature
	 * 
	 * @author Ignacio Arganda-Carreras (iarganda at mit.edu)
	 */
	public SioxSegmentator(
			int w, 
			int h, 
			float[] limits,
			float[][] bgSignature,
			float[][] fgSignature)
	{

		imgWidth=w;
		imgHeight=h;
		labelField=new int[imgWidth*imgHeight];
		knownBg=new float[imgWidth*imgHeight][3];
		knownFg=new float[imgWidth*imgHeight][3];

		if (limits==null) {
			this.limits=new float[] {0.64f, 1.28f, 2.56f};
		} else {
			this.limits=limits;
		}
		clusterSize=Utils.sqrEuclidianDist(this.limits, new float[] {-this.limits[0], -this.limits[1], -this.limits[2]});
		segmentated=false;
		
		this.bgSignature = bgSignature;
		this.fgSignature = fgSignature;
	}
	
	
	/**
	 * Segments the given image with information from the confidence
	 * matrix. For faster segmentation in videos, use the methods
	 * <tt>segmentatevideo_firstframe()</tt> and <tt>segmentatevideo_nextframe()</tt>.
	 * <P>
	 * The confidence entries  of <code>BACKGROUND_CONFIDENCE</code> or less
	 * are mark known background pixel for the segmentation, those
	 * of at least <code>FOREGROUND_CONFIDENCE</code> mark known
	 * foreground pixel for the segmentation. Any other entry is treated
	 * as region of unknown affiliation.
	 * <P>
	 * As result, each pixel is classified either as foreground or
	 * background, stored back into its <code>cm</code> entry as confidence
	 * <code>CERTAIN_FOREGROUND_CONFIDENCE</code> or
	 * <code>CERTAIN_BACKGROUND_CONFIDENCE</code>.
	 *
	 * @param image Pixel data of the image to be segmented.
	 *        Every integer represents one ARGB-value.
	 * @param cm Confidence matrix specifying the probability of an image
	 *        belonging to the foreground before and after the segmentation.
	 * @param smoothness Number of smoothing steps in the post processing.
	 * @param sizeFactorToKeep Segmentation retains the largest connected
	 *        foreground component plus any component with size at least
	 *        <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>.
	 * @return <CODE>true</CODE> if the segmentation algorithm succeeded,
	 *         <CODE>false</CODE> if segmentation is impossible
	 * @exception IllegalStateException if the confidence matrix defines no
	 *         image foreground.
	 */
	public boolean segmentate(int[] image, float[] cm, int smoothness, double sizeFactorToKeep)
	{
		segmentated = false;
		hs.clear();

		// save image for drb
		origImage = new int[image.length];
		System.arraycopy(image, 0, origImage, 0, image.length);

		// user predefined foreground pixels
		IntArrayList predefinedFgPixels = new IntArrayList();
		
		// create color signatures
		int knownBgCount=0, knownFgCount=0;
		for (int i=0; i<cm.length; i++) 
		{
			if (cm[i] <= BACKGROUND_CONFIDENCE) 
			{
				knownBg[knownBgCount++]=Utils.rgbToClab(image[i]);
			} 
			else if (cm[i]>=FOREGROUND_CONFIDENCE) 
			{
				knownFg[knownFgCount++]=Utils.rgbToClab(image[i]);
				if(cm[i] == CERTAIN_FOREGROUND_CONFIDENCE)
				{
					predefinedFgPixels.add(i);
					//System.out.println("added " + i);
				}
			}
		}
		bgSignature = ColorSignature.createSignature(knownBg, knownBgCount, limits, BACKGROUND_CONFIDENCE);
		fgSignature = ColorSignature.createSignature(knownFg, knownFgCount, limits, BACKGROUND_CONFIDENCE);
		if (bgSignature.length<1) {
			// segmentation impossible
			return false;
		}

		// classify using color signatures,
		// classification cached in hashmap for drb and speedup purposes
		for (int i=0; i<cm.length; i++) 
		{
			if (cm[i]>=FOREGROUND_CONFIDENCE) 
			{
				cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				continue;
			}
			else if (cm[i]>BACKGROUND_CONFIDENCE) 
			{
				Tupel tupel=(Tupel)hs.get(image[i]);
				boolean isBackground=true;
				if (tupel == null) 
				{
					tupel=new Tupel(0f, 0, 0f, 0);
					final float[] lab=Utils.rgbToClab(image[i]);
					float minBg=Utils.sqrEuclidianDist(lab, bgSignature[0]);
					int minIndex=0;
					for (int j=1; j<bgSignature.length; j++) 
					{
						final float d=Utils.sqrEuclidianDist(lab, bgSignature[j]);
						if (d<minBg) 
						{
							minBg=d;
							minIndex=j;
						}
					}
					tupel.minBgDist=minBg;
					tupel.indexMinBg=minIndex;
					float minFg=Float.MAX_VALUE;
					minIndex=-1;
					for (int j=0; j<fgSignature.length; j++) {
						final float d=Utils.sqrEuclidianDist(lab, fgSignature[j]);
						if (d<minFg) {
							minFg=d;
							minIndex=j;
						}
					}
					tupel.minFgDist=minFg;
					tupel.indexMinFg=minIndex;
					if (fgSignature.length==0) {
						isBackground=(minBg<=clusterSize);
						// remove next line to force behavior of old algorithm
						throw new IllegalStateException("foreground signature does not exist");
					} else {
						isBackground=minBg<minFg;
					}
					hs.put(image[i], tupel);
				} 
				else 
				{
					isBackground=tupel.minBgDist<=tupel.minFgDist;
				}
				if (isBackground) 
				{
					cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
				} 
				else 
				{
					cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				}
			} 
			else 
			{
				cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
			}
		}

		// postprocessing
		Utils.smoothcm(cm, imgWidth, imgHeight, 0.33f, 0.33f, 0.33f); // average
		Utils.normalizeMatrix(cm);
		Utils.erode(cm, imgWidth, imgHeight);
		keepOnlyLargeComponents(cm, UNKNOWN_REGION_CONFIDENCE, sizeFactorToKeep, predefinedFgPixels);
		for (int i=0; i<smoothness; i++) 
		{
			Utils.smoothcm(cm, imgWidth, imgHeight, 0.33f, 0.33f, 0.33f); // average
		}
		Utils.normalizeMatrix(cm);
		for (int i=0; i<cm.length; i++) 
		{
			if (cm[i]>=UNKNOWN_REGION_CONFIDENCE) // || predefinedFgPixels.contains(i)) 
			{
				cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
			} else 
			{
				cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
			}
		}
		
		//for(int i=0 ; i < predefinedFgPixels.size(); i++)
		//	System.out.println("cm["+predefinedFgPixels.get(i)+"] = " + cm[predefinedFgPixels.get(i)]);
		
		keepOnlyLargeComponents(cm, UNKNOWN_REGION_CONFIDENCE, sizeFactorToKeep, predefinedFgPixels);
		fillColorRegions(cm, image);
		Utils.dilate(cm, imgWidth, imgHeight);

		segmentated=true;
		return true;
	}

	/**
	 * Segments the given image with the previously calculated color signatures.
	 *
	 * @param image Pixel data of the image to be segmented.
	 *        Every integer represents one ARGB-value.
	 * @param cm Confidence matrix specifying the probability of an image
	 *        belonging to the foreground before and after the segmentation.
	 * @param smoothness Number of smoothing steps in the post processing.
	 * @param sizeFactorToKeep Segmentation retains the largest connected
	 *        foreground component plus any component with size at least
	 *        <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>.
	 * @return <CODE>true</CODE> if the segmentation algorithm succeeded,
	 *         <CODE>false</CODE> if segmentation is impossible
	 * @exception IllegalStateException if the confidence matrix defines no
	 *         image foreground.
	 *         
	 * @author Ignacio Arganda-Carreras (iarganda at mit.edu)
	 */
	public boolean applyPrecomputedSignatures(
			int[] image, 
			float[] cm, 
			int smoothness, 
			double sizeFactorToKeep)
	{
		segmentated = false;
		hs.clear();

		// save image for drb
		origImage = new int[image.length];
		System.arraycopy(image, 0, origImage, 0, image.length);

		// user predefined foreground pixels
		IntArrayList predefinedFgPixels = new IntArrayList();
		
		// create color signatures
		int knownBgCount=0, knownFgCount=0;
		for (int i=0; i<cm.length; i++) 
		{
			if (cm[i] <= BACKGROUND_CONFIDENCE) 
			{
				knownBg[knownBgCount++]=Utils.rgbToClab(image[i]);
			} 
			else if (cm[i]>=FOREGROUND_CONFIDENCE) 
			{
				knownFg[knownFgCount++]=Utils.rgbToClab(image[i]);
				if(cm[i] == CERTAIN_FOREGROUND_CONFIDENCE)
				{
					predefinedFgPixels.add(i);
					//System.out.println("added " + i);
				}
			}
		}
		
		if (bgSignature.length<1) {
			// segmentation impossible
			return false;
		}

		// classify using color signatures,
		// classification cached in hashmap for drb and speedup purposes
		for (int i=0; i<cm.length; i++) 
		{
			if (cm[i]>=FOREGROUND_CONFIDENCE) 
			{
				cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				continue;
			}
			else if (cm[i]>BACKGROUND_CONFIDENCE) 
			{
				Tupel tupel=(Tupel)hs.get(image[i]);
				boolean isBackground=true;
				if (tupel == null) 
				{
					tupel=new Tupel(0f, 0, 0f, 0);
					final float[] lab=Utils.rgbToClab(image[i]);
					float minBg=Utils.sqrEuclidianDist(lab, bgSignature[0]);
					int minIndex=0;
					for (int j=1; j<bgSignature.length; j++) 
					{
						final float d=Utils.sqrEuclidianDist(lab, bgSignature[j]);
						if (d<minBg) 
						{
							minBg=d;
							minIndex=j;
						}
					}
					tupel.minBgDist=minBg;
					tupel.indexMinBg=minIndex;
					float minFg=Float.MAX_VALUE;
					minIndex=-1;
					for (int j=0; j<fgSignature.length; j++) {
						final float d=Utils.sqrEuclidianDist(lab, fgSignature[j]);
						if (d<minFg) {
							minFg=d;
							minIndex=j;
						}
					}
					tupel.minFgDist=minFg;
					tupel.indexMinFg=minIndex;
					if (fgSignature.length==0) {
						isBackground=(minBg<=clusterSize);
						// remove next line to force behavior of old algorithm
						throw new IllegalStateException("foreground signature does not exist");
					} else {
						isBackground=minBg<minFg;
					}
					hs.put(image[i], tupel);
				} 
				else 
				{
					isBackground=tupel.minBgDist<=tupel.minFgDist;
				}
				if (isBackground) 
				{
					cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
				} 
				else 
				{
					cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				}
			} 
			else 
			{
				cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
			}
		}

		// postprocessing
		Utils.smoothcm(cm, imgWidth, imgHeight, 0.33f, 0.33f, 0.33f); // average
		Utils.normalizeMatrix(cm);
		Utils.erode(cm, imgWidth, imgHeight);
		//keepOnlyLargeComponents(cm, UNKNOWN_REGION_CONFIDENCE, sizeFactorToKeep, predefinedFgPixels);
		for (int i=0; i<smoothness; i++) 
		{
			Utils.smoothcm(cm, imgWidth, imgHeight, 0.33f, 0.33f, 0.33f); // average
		}
		Utils.normalizeMatrix(cm);
		for (int i=0; i<cm.length; i++) 
		{
			if (cm[i]>=UNKNOWN_REGION_CONFIDENCE) // || predefinedFgPixels.contains(i)) 
			{
				cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
			} else 
			{
				cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
			}
		}
		
		//for(int i=0 ; i < predefinedFgPixels.size(); i++)
		//	System.out.println("cm["+predefinedFgPixels.get(i)+"] = " + cm[predefinedFgPixels.get(i)]);
		
		//keepOnlyLargeComponents(cm, UNKNOWN_REGION_CONFIDENCE, sizeFactorToKeep, predefinedFgPixels);
		fillColorRegions(cm, image);
		Utils.dilate(cm, imgWidth, imgHeight);

		segmentated=true;
		return true;
	}
	

	/**
	 * Segments the first frame of a scene in a video. Only a reduced postprocessing
	 * is applied. Still image segmentation should be performed with <tt>segmentate()</tt>.
	 * This methods also rebuilds the color signatures.
	 * <P>
	 * The confidence entries  of <code>BACKGROUND_CONFIDENCE</code> or less
	 * are mark known background pixel for the segmentation, those
	 * of at least <code>FOREGROUND_CONFIDENCE</code> mark known
	 * foreground pixel for the segmentation. Any other entry is treated
	 * as region of unknown affiliation.
	 * <P>
	 * As result, each pixel is classified either as foreground or
	 * background, stored back into its <code>cm</code> entry as confidence
	 * <code>CERTAIN_FOREGROUND_CONFIDENCE</code> or
	 * <code>CERTAIN_BACKGROUND_CONFIDENCE</code>.
	 *
	 * @param image Pixel data of the image to be segmented.
	 *        Every integer represents one ARGB-value.
	 * @param cm Confidence matrix specifying the probability of an image
	 *        belonging to the foreground before and after the segmentation.
	 * @param sizeFactorToKeep Segmentation retains the largest connected
	 *        foreground component plus any component with size at least
	 *        <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>.
	 * @return <CODE>true</CODE> if the segmentation algorithm succeeded,
	 *         <CODE>false</CODE> if segmentation is impossible
	 * @exception IllegalStateException if the confidence matrix defines no
	 *         image foreground.
	 */
	public boolean segmentatevideo_firstframe(int[] image, float[] cm, double sizeFactorToKeep)
	{
		segmentated=false;
		hs.clear();

		// save image for drb
		origImage=new int[image.length];
		System.arraycopy(image, 0, origImage, 0, image.length);

		// create color signatures
		int knownBgCount=0, knownFgCount=0;
		for (int i=0; i<cm.length; i++) {
			if (cm[i]<=BACKGROUND_CONFIDENCE) {
				knownBg[knownBgCount++]=Utils.rgbToClab(image[i]);
			} else if (cm[i]>=FOREGROUND_CONFIDENCE) {
				knownFg[knownFgCount++]=Utils.rgbToClab(image[i]);
			}
		}
		bgSignature=ColorSignature.createSignature(knownBg, knownBgCount, limits, BACKGROUND_CONFIDENCE);
		fgSignature=ColorSignature.createSignature(knownFg, knownFgCount, limits, BACKGROUND_CONFIDENCE);
		if (bgSignature.length<1) {
			// segmentation impossible
			return false;
		}

		// classify using color signatures,
		// classification cached in hashmap for drb and speedup purposes
		for (int i=0; i<cm.length; i++) {
			if (cm[i]>=FOREGROUND_CONFIDENCE) {
				cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				continue;
			}
			if (cm[i]>BACKGROUND_CONFIDENCE) {
				Tupel tupel=(Tupel)hs.get(image[i]);
				boolean isBackground=true;
				if (tupel==null) {
					tupel=new Tupel(0f, 0, 0f, 0);
					final float[] lab=Utils.rgbToClab(image[i]);
					float minBg=Utils.sqrEuclidianDist(lab, bgSignature[0]);
					int minIndex=0;
					for (int j=1; j<bgSignature.length; j++) {
						final float d=Utils.sqrEuclidianDist(lab, bgSignature[j]);
						if (d<minBg) {
							minBg=d;
							minIndex=j;
						}
					}
					tupel.minBgDist=minBg;
					tupel.indexMinBg=minIndex;
					float minFg=Float.MAX_VALUE;
					minIndex=-1;
					for (int j=0; j<fgSignature.length; j++) {
						final float d=Utils.sqrEuclidianDist(lab, fgSignature[j]);
						if (d<minFg) {
							minFg=d;
							minIndex=j;
						}
					}
					tupel.minFgDist=minFg;
					tupel.indexMinFg=minIndex;
					if (fgSignature.length==0) {
						isBackground=(minBg<=clusterSize);
						// remove next line to force behavior of old algorithm
						throw new IllegalStateException("foreground signature does not exist");
					} else {
						isBackground=minBg<minFg;
					}
					hs.put(image[i], tupel);
				} else {
					isBackground=tupel.minBgDist<=tupel.minFgDist;
				}
				if (isBackground) {
					cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
				} else {
					cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				}
			} else {
				cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
			}
		}
		// postprocessing
		Utils.smoothcm(cm, imgWidth, imgHeight, 0.33f, 0.33f, 0.33f); // average
		Utils.normalizeMatrix(cm);				
		keepOnlyLargeComponents(cm, UNKNOWN_REGION_CONFIDENCE, sizeFactorToKeep);
		segmentated=true;
		return true;
	}


	/**
	 * Segment the further frames of a scene in a video. Only a reduced postprocessing
	 * is applied. The methods DOES NOT rebuild the color signatures.
	 * This method can only be used after a call to <tt>segmentate()</tt> or
	 * <tt>segmentatevideo_firstframe()</tt>.
	 * <P>
	 * The confidence entries  of <code>BACKGROUND_CONFIDENCE</code> or less
	 * are mark known background pixel for the segmentation, those
	 * of at least <code>FOREGROUND_CONFIDENCE</code> mark known
	 * foreground pixel for the segmentation. Any other entry is treated
	 * as region of unknown affiliation. This method does not require any known
	 * confidences in a frame.
	 * <P>
	 * As result, each pixel is classified either as foreground or
	 * background, stored back into its <code>cm</code> entry as confidence
	 * <code>CERTAIN_FOREGROUND_CONFIDENCE</code> or
	 * <code>CERTAIN_BACKGROUND_CONFIDENCE</code>.
	 *
	 * @param image Pixel data of the image to be segmented.
	 *        Every integer represents one ARGB-value.
	 * @param cm Confidence matrix specifying the probability of an image
	 *        belonging to the foreground before and after the segmentation.
	 * @param sizeFactorToKeep Segmentation retains the largest connected
	 *        foreground component plus any component with size at least
	 *        <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>.
	 * @return <CODE>true</CODE> if the segmentation algorithm succeeded,
	 *         <CODE>false</CODE> if segmentation is impossible
	 * @exception IllegalStateException if no color signature has been defined
	 * before calling this method.
	 */
	public boolean segmentatevideo_nextframe(int[] image, float[] cm, double sizeFactorToKeep)
	{
		if (!segmentated) throw new IllegalStateException("This method cannot be called before color signatures have been created.");

		// save image for drb
		origImage=new int[image.length];
		System.arraycopy(image, 0, origImage, 0, image.length);

		// classify using color signatures,
		// classification cached in hashmap for drb and speedup purposes
		for (int i=0; i<cm.length; i++) {
			if (cm[i]>=FOREGROUND_CONFIDENCE) {
				cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				continue;
			}
			if (cm[i]>BACKGROUND_CONFIDENCE) {
				Tupel tupel=(Tupel)hs.get(image[i]);
				boolean isBackground=true;
				if (tupel==null) {
					tupel=new Tupel(0f, 0, 0f, 0);
					final float[] lab=Utils.rgbToClab(image[i]);
					float minBg=Utils.sqrEuclidianDist(lab, bgSignature[0]);
					int minIndex=0;
					for (int j=1; j<bgSignature.length; j++) {
						final float d=Utils.sqrEuclidianDist(lab, bgSignature[j]);
						if (d<minBg) {
							minBg=d;
							minIndex=j;
						}
					}
					tupel.minBgDist=minBg;
					tupel.indexMinBg=minIndex;
					float minFg=Float.MAX_VALUE;
					minIndex=-1;
					for (int j=0; j<fgSignature.length; j++) {
						final float d=Utils.sqrEuclidianDist(lab, fgSignature[j]);
						if (d<minFg) {
							minFg=d;
							minIndex=j;
						}
					}
					tupel.minFgDist=minFg;
					tupel.indexMinFg=minIndex;
					if (fgSignature.length==0) {
						isBackground=(minBg<=clusterSize);
						// remove next line to force behaviour of old algorithm
						throw new IllegalStateException("foreground signature does not exist");
					} else {
						isBackground=minBg<minFg;
					}
					hs.put(image[i], tupel);
				} else {
					isBackground=tupel.minBgDist<=tupel.minFgDist;
				}
				if (isBackground) {
					cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
				} else {
					cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				}
			} else {
				cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
			}
		}
		// postprocessing
		Utils.smoothcm(cm, imgWidth, imgHeight, 0.33f, 0.33f, 0.33f); // average
		Utils.normalizeMatrix(cm);
		keepOnlyLargeComponents(cm, UNKNOWN_REGION_CONFIDENCE, sizeFactorToKeep);
		segmentated=true;
		return true;
	}


	/**
	 * Clears given confidence matrix except entries for the largest connected
	 * component and every component with
	 * <CODE>size*sizeFactorToKeep >= sizeOfLargestComponent</CODE>.
	 *
	 * @param cm  Confidence matrix to be analyzed
	 * @param threshold Pixel visibility threshold.
	 *        Exactly those cm entries larger than threshold are considered
	 *        to be a "visible" foreground pixel.
	 * @param sizeFactorToKeep This method keeps the largest connected
	 *        component plus any component with size at least
	 *        <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>.
	 */
	protected void keepOnlyLargeComponents(float[] cm, float threshold, double sizeFactorToKeep)
	{
		Arrays.fill(labelField, -1);
		int curlabel = 0;
		int maxregion = 0;
		int maxblob = 0;

		// slow but easy to understand:
		final IntArrayList labelSizes = new IntArrayList();
		for (int i=0; i<cm.length; i++) 
		{
			regionCount=0;
			if (labelField[i] == -1 && cm[i] >= threshold) {
				regionCount = depthFirstSearch(cm, i, threshold, curlabel++);
				labelSizes.add(regionCount);
			}

			if (regionCount > maxregion) {
				maxregion=regionCount;
				maxblob=curlabel-1;
			}
		}

		for (int i=0; i<cm.length; i++) 
		{
			if (labelField[i] != -1) 
			{
				// remove if the component is to small
				if (labelSizes.get(labelField[i])*sizeFactorToKeep < maxregion) {
					cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
				}
				// add maxblob always to foreground
				if (labelField[i] == maxblob) {
					cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
				}
			}
		}
	}

	
	/**
	 * Clears given confidence matrix except entries for the user-defined connected
	 * component and every component with
	 * <CODE>size*sizeFactorToKeep >= sizeOfLargestComponent</CODE>.
	 *
	 * @param cm  Confidence matrix to be analyzed
	 * @param threshold Pixel visibility threshold.
	 *        Exactly those cm entries larger than threshold are considered
	 *        to be a "visible" foreground pixel.
	 * @param sizeFactorToKeep This method keeps the largest connected
	 *        component plus any component with size at least
	 *        <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>.
	 * @param predefinedFgPixels list of pixels defined for the user as foreground.
	 */
	protected void keepOnlyLargeComponents(
			final float[] cm, 
			final float threshold, 
			final double sizeFactorToKeep, 
			final IntArrayList predefinedFgPixels)
	{
		Arrays.fill(labelField, -1);
		int curlabel = 0;
		int maxregion = 0;
		int maxblob = 0;

		// slow but easy to understand:
		final IntArrayList labelSizes = new IntArrayList();
		
		final IntArrayList predefinedLabels = new IntArrayList();
		
		for (int i=0; i<cm.length; i++) 
		{
			regionCount=0;
			if (labelField[i] == -1 && cm[i] >= threshold) {
				regionCount = depthFirstSearch(cm, i, threshold, curlabel++);
				labelSizes.add(regionCount);				
			}
			
			if(labelField[i] != -1 && !predefinedLabels.contains(labelField[i])
					&& predefinedFgPixels.contains(i))
			{
				predefinedLabels.add(labelField[i]);
				//System.out.println("added label " + labelField[i]);
			}

			if (regionCount > maxregion) {
				maxregion=regionCount;
				maxblob=curlabel-1;
			}
		}

		// Uni-label
		if(sizeFactorToKeep == 0)
		{
			for (int i=0; i<cm.length; i++) 
			{
				if (labelField[i] != -1) 
				{
					if ( predefinedLabels.contains(labelField[i]) ) 
						cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
					else
						cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
				}
					
			}
		}
		else // multi-label
		{
			for (int i=0; i<cm.length; i++) 
				if (labelField[i] != -1) 
				{
					// remove if the component is to small
					if (labelSizes.get(labelField[i])*sizeFactorToKeep < maxregion 
							&& !predefinedLabels.contains(labelField[i]) ) 
					{
						cm[i]=CERTAIN_BACKGROUND_CONFIDENCE;
					}
					// add maxblob always to foreground
					else if (labelField[i] == maxblob) {
						cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
					}
				}
		}
	}	
	
	/**
	 * Depth first search pixels in a foreground component.
	 *
	 * @param cm confidence matrix to be searched.
	 * @param i starting position as index to confidence matrix.
	 * @param threshold defines the minimum value at which a pixel is
	 *        considered foreground.
	 * @param curlabel label no of component.
	 * @return size in pixel of the component found.
	 */
	private int depthFirstSearch(float[] cm, int i, float threshold, int curLabel)
	{
		// stores positions of labeled pixels, where the neighbors
		// should still be checked for processing:
		final IntArrayList pixelsToVisit=new IntArrayList();
		int componentSize=0;
		if (labelField[i]==-1 && cm[i]>=threshold) { // label #i
			labelField[i] = curLabel;
			++componentSize;
			pixelsToVisit.add(i);
		}
		while (!pixelsToVisit.isEmpty()) {
			final int pos=pixelsToVisit.remove(pixelsToVisit.size()-1);
			final int x=pos%imgWidth;
			final int y=pos/imgWidth;
			// check all four neighbours
			final int left = pos-1;
			if (x-1>=0 && labelField[left]==-1 && cm[left]>=threshold) {
				labelField[left]=curLabel;
				++componentSize;
				pixelsToVisit.add(left);
			}
			final int right = pos+1;
			if (x+1<imgWidth && labelField[right]==-1 && cm[right]>=threshold) {
				labelField[right]=curLabel;
				++componentSize;
				pixelsToVisit.add(right);
			}
			final int top = pos-imgWidth;
			if (y-1>=0 && labelField[top]==-1 && cm[top]>=threshold) {
				labelField[top]=curLabel;
				++componentSize;
				pixelsToVisit.add(top);
			}
			final int bottom = pos+imgWidth;
			if (y+1<imgHeight && labelField[bottom]==-1
				&& cm[bottom]>=threshold) {
				labelField[bottom]=curLabel;
				++componentSize;
				pixelsToVisit.add(bottom);
			}
		}
		return componentSize;
	}

	/**
	 * Refines the classification stored in the confidence matrix by modifying
	 * the confidences for regions which have characteristics to both
	 * foreground and background if they fall into the specified square.
	 * <P>
	 * The can be used in displaying the image by assigning the alpha values
	 * of the pixels according to the confidence entries.
	 * <P>
	 * In the algorithm descriptions and examples GUIs this step is referred
	 * to as <EM>Detail Refinement (Brush)</EM>.
	 *
	 * @param x Horizontal coordinate of the squares center.
	 * @param y Vertical coordinate of the squares center.
	 * @param brushmode Mode of the refinement applied, <CODE>ADD_EDGE</CODE>
	 *        or <CODE>SUB_EDGE</CODE>. Add mode only modifies pixels
	 *        formerly classified as background, sub mode only those
	 *        formerly classified as foreground.
	 * @param threshold Threshold for the add and sub refinement, deciding
	 *        at the confidence level to stop at.
	 * @param cf The confidence matrix to modify, generated by
	 *        <CODE>segmentate</CODE>, possibly already refined by previous
	 *        calls to <CODE>subpixelRefine</CODE>.
	 * @param brushsize Halfed diameter of the square shaped brush.
	 *
	 * @exception IllegalStateException if there is no segmentation calculated
	 *            yet to be refined
	 * @see #segmentate
	 */
	public void subpixelRefine(int x, int y, String brushmode, float threshold, float[] cf, int brushsize)
	{
		subpixelRefine(new Area(new Rectangle(x-brushsize, y-brushsize,
											  2*brushsize, 2*brushsize)),
					   brushmode, threshold, cf);
	}

	/**
	 * Refines the classification stored in the confidence matrix by modifying
	 * the confidences for regions which have characteristics to both
	 * foreground and background if they fall into the specified area.
	 * <P>
	 * The can be used in displaying the image by assigning the alpha values
	 * of the pixels according to the confidence entries.
	 * <P>
	 * In the algorithm descriptions and examples GUIs this step is referrered
	 * to as <EM>Detail Refinement (Brush)</EM>.
	 *
	 * @param area Area in which the reworking of the segmentation is
	 *        applied to.
	 * @param brushmode Mode of the refinement applied, <CODE>ADD_EDGE</CODE>
	 *        or <CODE>SUB_EDGE</CODE>. Add mode only modifies pixels
	 *        formerly classified as background, sub mode only those
	 *        formerly classified as foreground.
	 * @param threshold Threshold for the add and sub refinement, deciding
	 *        at the confidence level to stop at.
	 * @param cf The confidence matrix to modify, generated by
	 *        <CODE>segmentate</CODE>, possibly already refined by privious
	 *        calls to <CODE>subpixelRefine</CODE>.
	 *
	 * @exception IllegalStateException if there is no segmentation
	 *            calculated yet to be refined
	 * @see #segmentate
	 */
	public void subpixelRefine(Area area, String brushmode, float threshold, float[] cf)
	{
		if (!segmentated) {
			throw new IllegalStateException("no segmentation yet");
		}
		final Rectangle r = area.getBounds();
		final int x0 = Math.max(0, r.x);
		final int y0 = Math.max(0, r.y);
		final int xTo = Math.min(imgWidth-1, r.x+r.width);
		final int yTo = Math.min(imgHeight-1, r.y+r.height);
		for (int ey=y0; ey<yTo; ++ey) {
			for (int ex=x0; ex<xTo; ++ex) {
				if (!area.contains(ex, ey)) {
					continue;
				}
				int val=origImage[ey*imgWidth+ex];
				final int orig=val;
				final Tupel tupel=(Tupel)hs.get(val);
				final float minDistBg, minDistFg;
				if (tupel!=null) {
					minDistBg=(float)Math.sqrt(tupel.minBgDist);
					minDistFg=(float)Math.sqrt(tupel.minFgDist);
				} else {
					continue;
				}
				if (ADD_EDGE.equals(brushmode)) { // handle adder
					if (cf[ey*imgWidth+ex]<FOREGROUND_CONFIDENCE) { // postprocessing wins
					  float alpha;
					  if (minDistFg==0) {
						  alpha=CERTAIN_FOREGROUND_CONFIDENCE;
					  } else {
						  alpha=Math.min(minDistBg/minDistFg, CERTAIN_FOREGROUND_CONFIDENCE);
					  }
					  if (alpha<threshold) { // background with certain confidence decided by user.
						  alpha=CERTAIN_BACKGROUND_CONFIDENCE;
					  }
					  val=Utils.setAlpha(alpha, orig);
					  cf[ey*imgWidth+ex]=alpha;
					}
				} else if (SUB_EDGE.equals(brushmode)) { // handle substractor
					if (cf[ey*imgWidth+ex]>FOREGROUND_CONFIDENCE) {
						// foreground, we want to take something away
						float alpha;
						if (minDistBg==0) {
							alpha=CERTAIN_BACKGROUND_CONFIDENCE;
						} else {
							alpha=CERTAIN_FOREGROUND_CONFIDENCE-Math.min(minDistFg/minDistBg, CERTAIN_FOREGROUND_CONFIDENCE); // more background -> >1
							// bg = gf -> 1
							// more fg -> <1
						}
						if (alpha<threshold) { // background with certain confidence decided by user
							alpha=CERTAIN_BACKGROUND_CONFIDENCE;
						}
						val=Utils.setAlpha(alpha, orig);
						cf[ey*imgWidth+ex]=alpha;
					}
				} else {
					throw new IllegalArgumentException("unknown brush mode: "+brushmode);
				}
			}
		}
	}

	/**
	 * A region growing algorithm used to fill up the confidence matrix
	 * with <CODE>CERTAIN_FOREGROUND_CONFIDENCE</CODE> for corresponding
	 * areas of equal colors.
	 * <P>
	 * Basically, the method works like the <EM>Magic Wand<EM> with a
	 * tolerance threshold of zero.
	 *
	 * @param cm confidence matrix to be searched
	 * @param image image to be searched
	 */
	private void fillColorRegions(float[] cm, int[] image)
	{
		Arrays.fill(labelField, -1);
		//int maxRegion=0; // unused now
		final IntArrayList pixelsToVisit=new IntArrayList();
		for (int i=0; i<cm.length; i++) { // for all pixels
			if (labelField[i]!=-1 || cm[i]<UNKNOWN_REGION_CONFIDENCE) {
				continue; // already visited or bg
			}
			final int origColor=image[i];
			final int curLabel=i+1;
			labelField[i]=curLabel;
			cm[i]=CERTAIN_FOREGROUND_CONFIDENCE;
			// int componentSize = 1;
			pixelsToVisit.add(i);
			// depth first search to fill region
			while (!pixelsToVisit.isEmpty()) {
				final int pos=pixelsToVisit.remove(pixelsToVisit.size()-1);
				final int x=pos%imgWidth;
				final int y=pos/imgWidth;
				// check all four neighbours
				final int left = pos-1;
				if (x-1>=0 && labelField[left]==-1
					&& Utils.labcolordiff(image[left], origColor)<1.0) {
					labelField[left]=curLabel;
					cm[left]=CERTAIN_FOREGROUND_CONFIDENCE;
					// ++componentSize;
					pixelsToVisit.add(left);
				}
				final int right = pos+1;
				if (x+1<imgWidth && labelField[right]==-1
					&& Utils.labcolordiff(image[right], origColor)<1.0) {
				  labelField[right]=curLabel;
				  cm[right]=CERTAIN_FOREGROUND_CONFIDENCE;
				  // ++componentSize;
				  pixelsToVisit.add(right);
				}
				final int top = pos-imgWidth;
				if (y-1>=0 && labelField[top]==-1
					&& Utils.labcolordiff(image[top], origColor)<1.0) {
					labelField[top]=curLabel;
					cm[top]=CERTAIN_FOREGROUND_CONFIDENCE;
					// ++componentSize;
					pixelsToVisit.add(top);
				}
				final int bottom = pos+imgWidth;
				if (y+1<imgHeight && labelField[bottom]==-1
					&& Utils.labcolordiff(image[bottom], origColor)<1.0) {
					labelField[bottom]=curLabel;
					cm[bottom]=CERTAIN_FOREGROUND_CONFIDENCE;
					// ++componentSize;
					pixelsToVisit.add(bottom);
				}
			}
			//if (componentSize>maxRegion) {
			//    maxRegion=componentSize;
			//}
		}
	}

	/**
	 * Helper class for storing the minimum distances to a cluster centroid
	 * in background and foreground and the index to the centroids in each
	 * signature for a given color.
	 */
	private final class Tupel{
		float minBgDist;
		int indexMinBg;
		float minFgDist;
		int indexMinFg;

		Tupel(float minBgDist, int indexMinBg, float minFgDist, int indexMinFg)
		{
			this.minBgDist=minBgDist;
			this.indexMinBg=indexMinBg;
			this.minFgDist=minFgDist;
			this.indexMinFg=indexMinFg;
		}
	}
	
	/** Get background signature */
	public float[][] getBgSignature(){ return this.bgSignature;}
	/** Get foreground signature */
	public float[][] getFgSignature(){ return this.fgSignature;}
	
	
}
