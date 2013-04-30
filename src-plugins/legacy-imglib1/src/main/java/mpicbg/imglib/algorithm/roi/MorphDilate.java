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

package mpicbg.imglib.algorithm.roi;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Dilation morphological operation.
 * 
 * @param <T> {@link Image} type.
 * @author Larry Lindsey
 */
public class MorphDilate<T extends RealType<T>> extends StatisticalOperation<T> {

	
	public MorphDilate(final Image<T> imageIn, final StructuringElement strel,
			final OutOfBoundsStrategyFactory<T> inOutsideFactory)
	{
		super(imageIn, strel, inOutsideFactory);
		setName(imageIn.getName() + " dilated");
	}
	
	
	public MorphDilate(final Image<T> imageIn, final StructuringElement strel) {
		super(imageIn, strel);
		setName(imageIn.getName() + " dilated");
	}

	@Override
	protected void statsOp(final LocalizableByDimCursor<T> cursor) {
		cursor.getType().set(super.getList().getLast());
	}

}
