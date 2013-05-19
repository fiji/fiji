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
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.interpolation;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public abstract class InterpolatorImpl<T extends Type<T>> implements Interpolator<T>
{
	final protected InterpolatorFactory<T> interpolatorFactory;
	final protected OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory;
	final protected Image<T> img;

	// the location of the interpolator in the image
	final protected float[] position, tmp;

	/**
	 * the number of dimensions 
	 */
	final protected int numDimensions;
	
	protected InterpolatorImpl( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		this.interpolatorFactory = interpolatorFactory;
		this.outOfBoundsStrategyFactory = outOfBoundsStrategyFactory;
		this.img = img;
		this.numDimensions = img.getNumDimensions();
	
		tmp = new float[ numDimensions ];
		position = new float[ numDimensions ];
	}

	/**
	 * Returns the typed interpolator factory the Interpolator has been instantiated with.
	 * 
	 * @return - the interpolator factory
	 */
	@Override
	public InterpolatorFactory<T> getInterpolatorFactory(){ return interpolatorFactory; }
	
	/**
	 * Returns the {@link OutOfBoundsStrategyFactory} used for interpolation
	 * 
	 * @return - the {@link OutOfBoundsStrategyFactory}
	 */
	@Override
	public OutOfBoundsStrategyFactory<T> getOutOfBoundsStrategyFactory() { return outOfBoundsStrategyFactory; }
	
	/**
	 * Returns the typed image the interpolator is working on
	 * 
	 * @return - the image
	 */
	@Override
	public Image<T> getImage() { return img; }		
	
	@Override
	public void getPosition( final float[] position )
	{
		for ( int d = 0; d < numDimensions; d++ )
			position[ d ] = this.position[ d ];
	}

	@Override
	public float[] getPosition() { return position.clone();	}	

	@Override
	public void moveRel( final float[] vector )
	{		
		for ( int d = 0; d < numDimensions; ++d )
			tmp[ d ] = position[ d ] + vector[ d ];
		
		moveTo( tmp );
	}
	
}
