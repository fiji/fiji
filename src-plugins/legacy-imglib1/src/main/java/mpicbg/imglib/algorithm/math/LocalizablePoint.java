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

package mpicbg.imglib.algorithm.math;

import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 */
public class LocalizablePoint implements Localizable 
{
	final int[] position;
	final int numDimensions;
	
	public LocalizablePoint ( final int[] position )
	{
		this.position = position;
		this.numDimensions = position.length;
	}

	public LocalizablePoint ( final float[] position )
	{
		this( position.length );
		
		for ( int d = 0; d < numDimensions; ++d )
			this.position[ d ] = Util.round( position[ d ] );
	}

	public LocalizablePoint ( final int numDimensions )
	{
		this.numDimensions = numDimensions;
		this.position = new int[ numDimensions ];
	}
	
	@Override
	public void fwd(long steps) {}

	@Override
	public void fwd() {}

	@Override
	public void getPosition( final int[] position ) 
	{
		for ( int d = 0; d < numDimensions; ++d )
			position[ d ] = this.position[ d ];
	}

	@Override
	public int[] getPosition() { return position; }

	@Override
	public int getPosition( final int dim ) { return position[ dim ]; }

	@Override
	public String getPositionAsString() { return Util.printCoordinates( position ); }
}
