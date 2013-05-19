package VolumeJ;

/**
 * This class is the superclass for all classifiers.
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public abstract class VJClassifier
{
	/**
	 * Classifier can return a RGB VJAlphaColor.
	 */
	public static final int     	RGB = 4;
	/**
	 * Classifier can return a RGB VJAlphaColor.
	 */
	public static final int     	GRAYSCALE = 2;
	/**
	 * Contains a secondary classifier if needed in alphacolor.
	 */
	protected VJClassifier      	secondaryClassifier;
	/**
	 * A short description of the classifier.
	 */
	protected String		description = "Classifier";
	/**
	 *  Get the classifier threshold.
	*/
	public abstract double            getThreshold();
	/**
	 * The classification method, defining how a value/gradient combination will be classified
	 * as an VJAlphaColor (argb).
	 */
	public abstract VJAlphaColor    alphacolor(VJValue value, VJGradient g);
	/**
	 * Returns the type of VJAlphaColor this classifier generates: RGB or GRAYSCALE.
	 */
	public abstract int   	        does();
	/**
	 * Tell renderer whether this voxel will be visible. Used for optimizations.
	 */
	public abstract boolean 	visible(VJValue value);
	/** Tell renderer whether this classifier can process indices in the voxel value. */
	public abstract boolean         doesIndex();
	/** Return whether or not this classifier supports a user loadable LUT. */
	public abstract boolean         hasLUT();
	/** Return a String with information of this classifier. */
	public String                   toString() { return description; }
	/** Set up a user-defined LUT. Overload if hasLUT returns true.*/
	public boolean                  setLUT(byte [] reds, byte [] greens, byte [] blues) { return false; }
	/**
	* Sets an alternative classifier.
	*/
	public void                 setSecondaryClassifier(VJClassifier classifier)
	{ secondaryClassifier = classifier; }
	public VJClassifier         getSecondaryClassifier(){ return secondaryClassifier; }
	/**
	* This method can be overloaded as necessary
	*/
	public String      		toLongString() { return toString(); }
	/**
	* This method can be overloaded as necessary.
	* Tell renderer whether this classifier can process cutout (slice faces).
	*/
	public boolean              	doesCutouts() { return false; }
	/**
	* This method can be overloaded as necessary.
	* If cutouts can be processed, overload this method.
	*/
	public VJAlphaColor          	alphacolor(VJValue value) { return null; }
	/**
	 * For debugging: print value, index and gradient.
	*/
	public String trace(VJValue v, VJGradient g)
	{
		return " value "+v.intvalue+((v.index > 0) ? " ("+v.index+")":"")+" gradient "+g.toString();
	}
}

