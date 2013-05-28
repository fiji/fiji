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

package mpicbg.imglib.algorithm.kdtree;

import java.util.Comparator;

import mpicbg.imglib.algorithm.kdtree.node.Leaf;

/**
 * Compares which {@link Leaf} is closer to another {@link Leaf}
 * 
 * @param <T>
 *
 * @author Johannes Schindelin
 * @author Stephan Preibisch
 */
public class DistanceComparator< T extends Leaf<T> > implements Comparator<T>
{
	final T point;
	
	public DistanceComparator( final T point )
	{
		this.point = point;
	}
	
	@Override
	public int compare( final T a, final T b ) 
	{
		final double distA = point.distanceTo( a );
		final double distB = point.distanceTo( b );
		return distA < distB ? -1 : distA > distB ? +1 : 0;
	}
}
