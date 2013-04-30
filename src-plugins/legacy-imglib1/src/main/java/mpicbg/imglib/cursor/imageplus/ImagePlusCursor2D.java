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

package mpicbg.imglib.cursor.imageplus;

import mpicbg.imglib.container.imageplus.ImagePlusContainer;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * Basic Iterator for 2d {@link ImagePlusContainer ImagePlusContainers}
 * @param <T>
 *
 * @author Stephan Saalfeld
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class ImagePlusCursor2D< T extends Type< T > > extends ImagePlusCursor< T >
{
	final protected int maxIndex;
	
	public ImagePlusCursor2D( final ImagePlusContainer< T, ? > container, final Image< T > image, final T type )
	{
		super( container, image, type );
		
		maxIndex = container.getNumPixels() - 1;
	}
	
	@Override
	public boolean hasNext()
	{
		return type.getIndex() < maxIndex;
	}

	@Override
	public void fwd()
	{
		type.incIndex();
	}
}
