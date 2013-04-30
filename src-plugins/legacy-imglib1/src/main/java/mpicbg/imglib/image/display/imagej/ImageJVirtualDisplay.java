/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.image.display.imagej;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collection;

import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.type.Type;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ImageJVirtualDisplay<T extends Type<T>> extends ImageStack
{
	final Collection<InverseTransformDescription<T>> transformDescription;
	final int type;
	final int[] dimensionPositions;

	final int dimX, dimY, dimZ;
	final int sizeX, sizeY, sizeZ;
	
	final ArrayList<SliceTransformableExtraction<T>> threadList = new ArrayList<SliceTransformableExtraction<T>>();

	double min, max;
	
	ImagePlus parent = null;
	
	public ImageJVirtualDisplay( final Collection<InverseTransformDescription<T>> interpolators, final int[] dimensions, 
			                     final int type, final int[] dim, final int[] dimensionPositions )
	{
		super( dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ] );
		
		this.transformDescription = interpolators;
		this.dimX = dim[ 0 ];
		this.dimY = dim[ 1 ];
		this.dimZ = dim[ 2 ];
		this.dimensionPositions = dimensionPositions.clone();
		
		this.type = type;
		
		this.sizeX = dimensions[ 0 ];
		this.sizeY = dimensions[ 1 ];
		this.sizeZ = dimensions[ 2 ];
		
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
		
		for ( InverseTransformDescription<T> it : interpolators )
		{
			if ( it.getImage().getDisplay().getMax() > max )
				max = it.getImage().getDisplay().getMax();
			
			if ( it.getImage().getDisplay().getMin() < min )
				min = it.getImage().getDisplay().getMin();
		}
	}
	
	public void setParent( ImagePlus parent ) { this.parent = parent; }
	public Collection<InverseTransformDescription<T>> getTransformDescription() { return transformDescription; }	
    
    /** Returns an ImageProcessor for the specified slice,
    were 1<=n<=nslices. Returns null if the stack is empty.
     */
	public ImageProcessor getProcessor( final int n ) 
	{
	    final ImageProcessor ip;
	    
	    if (n<1 || n>sizeZ)
	        throw new IllegalArgumentException("no slice " + n);
	    
	    if (sizeZ==0)
	        return null;
	    
	    switch(type) 
	    {
	    	/*case ImagePlus.GRAY8:
	    		ip = new ByteProcessor(size[0], size[1], extractSliceByte( img, display, n-1, dim, size, dimensionPositions, true ), null); break;
	     	case ImagePlus.COLOR_RGB:
	    		ip = new ColorProcessor( sizeX, sizeY, extractSliceRGBA( n-1 ) ); break;*/
	    	default:
	    		ip = new FloatProcessor( sizeX, sizeY, extractSliceFloat( n-1 ), null ); 
	    		ip.setMinAndMax( min, max );
	    		break;
	    }
	    
	    return ip;
	}

    public float[] extractSliceFloat( final int slice )
    {
    	// store the slice image
    	final float[] sliceImg = new float[ sizeX * sizeY ];
    	
    	if ( parent == null )
    		return sliceImg;

		for  ( SliceTransformableExtraction<T> thread : threadList )
			thread.stopThread();
    	
    	threadList.clear();
    	    	      
        for ( InverseTransformDescription<T> it : transformDescription )
        {
        	SliceTransformableExtractionFloat<T> thread = new SliceTransformableExtractionFloat<T>( transformDescription.size(), it, sliceImg, parent, dimensionPositions, dimX, dimY, dimZ, sizeX, sizeY, slice); 
			threadList.add( thread );
        }
        
		for( SliceTransformableExtraction<T> thread : threadList )
		{				
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();				
		}			

        return sliceImg;
    }
    
    public float[] extractSliceFloatSingleThreaded( final int slice )
    {    	
    	// store the slice image
    	final float[] sliceImg = new float[ sizeX * sizeY ];
    	
    	// store the current position
    	final float[] initialPosition = new float[ dimensionPositions.length ];
    	    	
    	for ( int d = 0; d < initialPosition.length; ++d )
    		initialPosition[ d ] = dimensionPositions[ d ];

    	if ( dimZ < initialPosition.length )
    		initialPosition[ dimZ ] = slice;

    	final float[] position = initialPosition.clone();
    	
		for ( final InverseTransformDescription<T> desc : transformDescription )
		{
			final Interpolator<T> it = desc.getImage().createInterpolator( desc.getInterpolatorFactory() );
			final InvertibleCoordinateTransform transform = desc.getTransform();
	    	final float[] offset = desc.getOffset();
	
			try
			{
		    	final T type = it.getType();
		    	final Display<T> display = it.getImage().getDisplay();
	
		    	int i = 0;
		    	
		    	for ( int y = 0; y < sizeY; y++ )
		    	{
		        	if ( dimY < initialPosition.length )
		        		initialPosition[ dimY ] = y;

		        	for ( int x = 0; x < sizeX; x++ )
		        	{
			        	for ( int d = 0; d < initialPosition.length; ++d )
			        		position[ d ] = initialPosition[ d ] + offset[ d ];

			        	position[ dimX ] = x + offset[ dimX ];

			        	transform.applyInverseInPlace( position );
			        	it.moveTo( position );
			        	
		        		sliceImg[ i ] += display.get32Bit(type);
			        	++i;
		        	}
		    	}
			}
			catch ( NoninvertibleModelException e )
			{
				System.out.println( it + " has a no invertible model: " + e );
			}
			
			it.close();
		}
     	
    	return sliceImg;
    }
    
 	/** Obsolete. Short images are always unsigned. */
    public void addUnsignedShortSlice(String sliceLabel, Object pixels) {}
   
	/** Adds the image in 'ip' to the end of the stack. */
	public void addSlice(String sliceLabel, ImageProcessor ip) {}
   
    /** Adds the image in 'ip' to the stack following slice 'n'. Adds
       the slice to the beginning of the stack if 'n' is zero. */
    public void addSlice(String sliceLabel, ImageProcessor ip, int n) {}
   
    /** Deletes the specified slice, were 1<=n<=nslices. */
    public void deleteSlice(int n) {}
   
    /** Deletes the last slice in the stack. */
    public void deleteLastSlice() {}
       
    /** Updates this stack so its attributes, such as min, max,
        calibration table and color model, are the same as 'ip'. */
    public void update(ImageProcessor ip) {}
   
    /** Returns the pixel array for the specified slice, were 1<=n<=nslices. */
    public Object getPixels(int n) { return getProcessor(n).getPixels(); }
   
    /** Assigns a pixel array to the specified slice,
        were 1<=n<=nslices. */
    public void setPixels(Object pixels, int n) {}
   
    /** Returns the stack as an array of 1D pixel arrays. Note
        that the size of the returned array may be greater than
        the number of slices currently in the stack, with
        unused elements set to null. */
    public Object[] getImageArray() { return null; }
   
    /** Returns the slice labels as an array of Strings. Note
        that the size of the returned array may be greater than
        the number of slices currently in the stack. Returns null
        if the stack is empty or the label of the first slice is null.  */
    public String[] getSliceLabels() { return null; }
   
    /** Returns the label of the specified slice, were 1<=n<=nslices.
        Returns null if the slice does not have a label. For DICOM
        and FITS stacks, labels may contain header information. */
    public String getSliceLabel(int n) { return "" + n; }
   
    /** Returns a shortened version (up to the first 60 characters or first newline and 
        suffix removed) of the label of the specified slice.
        Returns null if the slice does not have a label. */
    public String getShortSliceLabel(int n) { return getSliceLabel(n); }

    /** Sets the label of the specified slice, were 1<=n<=nslices. */
    public void setSliceLabel(String label, int n) {}

    /** Returns true if this is a 3-slice RGB stack. */
    public boolean isRGB() { return false; }
   
    /** Returns true if this is a 3-slice HSB stack. */
    public boolean isHSB() { return false; }

    /** Returns true if this is a virtual (disk resident) stack. 
        This method is overridden by the VirtualStack subclass. */
    public boolean isVirtual() { return true; }

    /** Frees memory by deleting a few slices from the end of the stack. */
    public void trim() {}

    public String toString() { return ("Virtual Display of " + transformDescription.size() + " Interpolators"); }	
}
