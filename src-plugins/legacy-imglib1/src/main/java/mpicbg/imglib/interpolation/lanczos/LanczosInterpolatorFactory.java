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

package mpicbg.imglib.interpolation.lanczos;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LanczosInterpolatorFactory<T extends RealType<T>> extends InterpolatorFactory<T>
{
	int alpha;
	boolean clipping;
	
	/**
	 * Creates a new {@link LanczosInterpolatorFactory} using the Lanczos (sinc) interpolation in a certain window
	 * 
	 * @param outOfBoundsStrategyFactory - the {@link OutOfBoundsStrategyFactory} to use
	 * @param alpha - the rectangular radius of the window for perfoming the lanczos interpolation
	 * @param clipping - the lanczos-interpolation can create values that are bigger or smaller than the original values,
	 *        so they can be clipped to the range of the {@link Type} if wanted
	 */
	public LanczosInterpolatorFactory( final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory, final int alpha, final boolean clipping )
	{
		super( outOfBoundsStrategyFactory );
		
		this.alpha = alpha;
		this.clipping = clipping;
	}

	/**
	 * Creates a new {@link LanczosInterpolatorFactory} with standard parameters (do clipping, alpha=5)
	 * 
	 * @param outOfBoundsStrategyFactory - the {@link OutOfBoundsStrategyFactory} to use
	 */
	public LanczosInterpolatorFactory( final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		this( outOfBoundsStrategyFactory, 3, true );
	}
	
	/**
	 * Set the rectangular radius of the window for perfoming the lanczos interpolation
	 * @param alpha - radius
	 */
	public void setAlpha( final int alpha ) { this.alpha = alpha; }
	
	/**
	 * The lanczos-interpolation can create values that are bigger or smaller than the original values,
	 * so they can be clipped to the range of the {@link RealType} if wanted
	 * 
	 * @param clipping - perform clipping (true)
	 */
	public void setClipping( final boolean clipping ) { this.clipping = clipping; }
	
	/**
	 * @return - rectangular radius of the window for perfoming the lanczos interpolation 
	 */
	public int getAlpha() { return alpha; }
	
	/**
	 * @return - if clipping to the {@link RealType} range will be performed 
	 */
	public boolean getClipping() { return clipping; }

	@Override
	public LanczosInterpolator<T> createInterpolator( final Image<T> img )
	{
		return new LanczosInterpolator<T>( img, this, outOfBoundsStrategyFactory, alpha, clipping );
	}
}
