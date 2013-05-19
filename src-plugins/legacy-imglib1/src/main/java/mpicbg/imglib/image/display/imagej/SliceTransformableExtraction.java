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
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.multithreading.Stopable;
import mpicbg.imglib.type.Type;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.models.RigidModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public abstract class SliceTransformableExtraction<T extends Type<T>> extends Thread implements Stopable
{
	final InverseTransformDescription<T> desc;
	final InvertibleCoordinateTransform transform;
	final Interpolator<T> it;
	final float[] offset;
	final ImagePlus parent;
	
	final Display<T> display;
	final T type;
	final int[] dimensionPositions;
	final int dimX, dimY, dimZ, slice, sizeX, sizeY, numDimensions;
	final float numImages;
	
	final boolean isAffine;
	
	protected boolean stopThread = false;

	public SliceTransformableExtraction( final int numImages, final InverseTransformDescription<T> desc, 
										 final ImagePlus parent, final int[] dimensionPositions, 
										 final int dimX, final int dimY, final int dimZ, 
										 final int sizeX, final int sizeY, final int slice)
	{
		this.desc = desc;
		this.transform = desc.getTransform();
		this.it = desc.getImage().createInterpolator( desc.getInterpolatorFactory() );
		this.offset = desc.getOffset();
		this.numDimensions = desc.getImage().getNumDimensions();
		
		if ( AffineModel2D.class.isInstance( desc.getTransform() ) ||
			 AffineModel3D.class.isInstance( desc.getTransform() ) ||
			 RigidModel2D.class.isInstance( desc.getTransform() ) ||
			 TranslationModel2D.class.isInstance( desc.getTransform() ) || 
			 TranslationModel3D.class.isInstance( desc.getTransform() ) )
			isAffine = true;
		else
			isAffine = false;
		
		this.parent = parent;
    	this.display = it.getImage().getDisplay();
    	this.type = this.it.getType();
		
		this.dimX = dimX;
		this.dimY = dimY;
		this.dimZ = dimZ;
		this.slice = slice;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.numImages = numImages;
		
		this.dimensionPositions = dimensionPositions.clone();
		
		if ( dimensionPositions.length != numDimensions )
			throw new RuntimeException("SliceTransformableExtraction.constructor(): dimensions of image and dimensionPositions not equal.");
	}
	
	protected abstract void setIntensity( final int index );
	
    public void run()
    {    
    	// store the current position
    	final float[] initialPosition = new float[ numDimensions ];
    
    	if ( dimX >= initialPosition.length )
    		throw new RuntimeException("SliceTransformableExtraction.run(): x-Dimension for display higher than available dimensions: " + dimX + " >= " +  initialPosition.length );
    	
    	if ( dimY >= initialPosition.length )
    		throw new RuntimeException("SliceTransformableExtraction.run(): y-Dimension for display higher than available dimensions: " + dimY + " >= " +  initialPosition.length );
    	
    	if ( dimZ >= initialPosition.length )
    		throw new RuntimeException("SliceTransformableExtraction.run(): z-Dimension for display higher than available dimensions: " + dimZ + " >= " +  initialPosition.length );
    	
    	for ( int d = 0; d < initialPosition.length; ++d )
    		initialPosition[ d ] = dimensionPositions[ d ];

		initialPosition[ dimZ ] = slice;

		final float[] position1 = initialPosition.clone();
		final float[] position2 = initialPosition.clone();
    	
		try
		{
	    	int i = 0;
	    	
	    	if ( isAffine )
	    	{
	    		// if it is an affine transform, lines stay lines
	    		// so we can get the vectors we have to move for each location in the output image
	    		// based on the edges of the image; by that we do not have to apply the inverse transformation
	    		// every time we move on a vector in the output image ( we move on the corresponding line in the input image) 	    		
	    		final float[] vectorX = new float[ initialPosition.length ];
	    		final float[] vectorY = new float[ initialPosition.length ];
	    		
	    		// get the vector for x movement	    		
	    		if ( sizeX > 1 )
	    		{
		    		position1[ dimX ] = position1[ dimY ] = 0;
		    		position2[ dimX ] = sizeX - 1;
		    		position2[ dimY ] = 0;
	
		    		for ( int d = 0; d < numDimensions; ++d )
		    		{
		    			position1[ d ] += offset[ d ];
		    			position2[ d ] += offset[ d ];
		    		}
		    		
		    		transform.applyInverseInPlace( position1 );
		    		transform.applyInverseInPlace( position2 );
		    		
		    		it.moveTo( position1 );
		    		it.moveTo( position2 );
		    		
		    		for ( int d = 0; d < initialPosition.length; ++d )
		    		{
		    			vectorX[ d ] = ( position2[ d ] - position1[ d ] ) / (float)sizeX;
		    			position1[ d ] = initialPosition[ d ];
		    			position2[ d ] = initialPosition[ d ];
		    		}
	    		}
	    		
	    		// get the vector for y movement
	    		if ( sizeY > 1 )
	    		{
		    		position1[ dimX ] = position1[ dimY ] = 0;
		    		position2[ dimX ] = 0;
		    		position2[ dimY ] = sizeY - 1;

		    		for ( int d = 0; d < numDimensions; ++d )
		    		{
		    			position1[ d ] += offset[ d ];
		    			position2[ d ] += offset[ d ];
		    		}
		    		
		    		transform.applyInverseInPlace( position1 );
		    		transform.applyInverseInPlace( position2 );
		    		
		    		it.moveTo( position1 );
		    		it.moveTo( position2 );
		    		
		    		for ( int d = 0; d < initialPosition.length; ++d )
		    		{
		    			vectorY[ d ] = ( position2[ d ] - position1[ d ] ) / (float)sizeY;
		    			position1[ d ] = initialPosition[ d ];
		    			position2[ d ] = initialPosition[ d ];
		    		}
	    		}
	    		
	    		// reset the locations
	    		position1[ dimX ] = position2[ dimX ] = 0;
	    		position1[ dimY ] = position2[ dimY ] = 0;

	    		for ( int d = 0; d < numDimensions; ++d )
	    			position1[ d ] += offset[ d ];
	    		
	    		transform.applyInverseInPlace( position1 );
	    		it.moveTo( position1 );
	    	
		    	for ( int y = 0; y < sizeY; y++ )
		    	{		    		
		        	for ( int x = 0; x < sizeX; x++ )
		        	{	        		
		        		if ( stopThread )
		        		{
		        			it.close();
		        			return;
		        		}
		        			
		        		setIntensity( i );
		        		//sliceImg[ i ] += display.get32Bit(type);
		        		
		        		// we move one step on the x axis in the output image 
		        		++i;
		        		
		        		// we move one step on the corresponding vector in the input image 
		        		it.moveRel( vectorX );

		        	}
		        	
		        	
		    		for ( int d = 0; d < initialPosition.length; ++d )
		    			position1[ d ] += vectorY[ d ];
		    		
		    		// typically this is big jump of hundreds of pixels
		    		it.setPosition( position1 );
		    		
					if ( parent != null && y%10 == 0 )
							parent.updateAndDraw();
		    	}
	    	}
	    	else
	    	{
		    	for ( int y = 0; y < sizeY; y++ )
		    	{        		
            		initialPosition[ dimY ] = y;
	
		        	for ( int x = 0; x < sizeX; x++ )
		        	{	        		
		        		if ( stopThread )
		        		{
		        			it.close();
		        			return;
		        		}
	
		        		for ( int d = 0; d < initialPosition.length; ++d )
			        		position1[ d ] = initialPosition[ d ] + offset[ d ];
	
			        	position1[ dimX ] = x + offset[ dimX ];
	
			        	transform.applyInPlace( position1 );
			        	it.moveTo( position1 );
			        	
			        	setIntensity( i );
		        		//sliceImg[ i ] += display.get32Bit(type);
		        		++i;
		        	}
		        	
					if ( parent != null && y%50 == 0)
						parent.updateAndDraw();
		    	}
	    	}
		}
		catch ( NoninvertibleModelException e )
		{
			System.out.println( it + " has a not invertible model: " + e );
		}
		
		it.close();
		
		if ( parent != null)
			parent.updateAndDraw();
	}

	@Override
	public void stopThread() 
	{
		stopThread = true;
	}
}
