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
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class SliceTransformableExtractionFloat<T extends Type<T>> extends SliceTransformableExtraction<T>
{
	final float[] sliceImg;

	public SliceTransformableExtractionFloat( final int numImages, final InverseTransformDescription<T> it, final float[] sliceImg, 
											  final ImagePlus parent, final int[] dimensionPositions, 
											  final int dimX, final int dimY, final int dimZ, 
											  final int sizeX, final int sizeY, final int slice)
	{
		super( numImages, it, parent, dimensionPositions, dimX, dimY, dimZ, sizeX, sizeY, slice );

		this.sliceImg = sliceImg;
	}
	
	@Override
	final protected void setIntensity( final int index )
	{
		sliceImg[ index ] += display.get32Bit(type);		
	}
}
