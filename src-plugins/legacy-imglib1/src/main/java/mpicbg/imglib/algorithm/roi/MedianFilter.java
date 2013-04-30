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
 * Median filter / morphological operation.
 * 
 * @param <T> {@link Image} type.
 * @author Larry Lindsey
 */
public class MedianFilter<T extends RealType<T>> extends StatisticalOperation<T> {

	public MedianFilter(final Image<T> imageIn, final StructuringElement inStrel,
			final OutOfBoundsStrategyFactory<T> inOutFactory) {
		super(imageIn, inStrel, inOutFactory);
		setName(imageIn.getName() + " Median Filter");
	}
	
	public MedianFilter(final Image<T> imageIn, final StructuringElement inStrel) {
		super(imageIn, inStrel);
		setName(imageIn.getName() + " Median Filter");
	}

	@Override
	protected void statsOp(LocalizableByDimCursor<T> cursor) {		
		int n = super.getList().size();
		cursor.getType().set(super.getList().get(n / 2));
	}

}
