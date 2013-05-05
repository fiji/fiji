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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.type.Type;
import mpicbg.models.InvertibleBoundable;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class InverseTransformDescription<T extends Type<T>> 
{
	final InvertibleBoundable transform;
	final InterpolatorFactory<T> factory;
	final Image<T> image;
	final float[] offset;
	final int numDimensions;
	
	public InverseTransformDescription( final InvertibleBoundable transform, final InterpolatorFactory<T> factory, final Image<T> image )
	{
		this.transform = transform;
		this.factory = factory;
		this.image = image;
		this.numDimensions = image.getNumDimensions();
		this.offset = new float[ numDimensions ];
	}
	
	public InvertibleBoundable getTransform() { return transform; }
	public InterpolatorFactory<T> getInterpolatorFactory() { return factory; }
	public Image<T> getImage() { return image; }
	
	public void setOffset( final float[] offset )
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.offset[ d ] = offset[ d ];
	}
	
	public float[] getOffset() { return offset.clone(); }
	public void getOffset( final float[] offset )
	{
		for ( int d = 0; d < numDimensions; ++d )
			offset[ d ] = this.offset[ d ];		
	}
}
