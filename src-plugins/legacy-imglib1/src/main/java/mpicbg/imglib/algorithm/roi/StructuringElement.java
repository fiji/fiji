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

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.logic.BitType;

/**
 * TODO
 *
 */
public class StructuringElement extends Image<BitType> {
	
	private final int[] offset;
	
	public StructuringElement(final int[] dimensions, final String name)
	{
		this(new ImageFactory<BitType>(new BitType(), new ArrayContainerFactory()), dimensions, name);
	}
	
	public StructuringElement(final ImageFactory<BitType> factory, final int[] dimensions, final String name)
	{
		super(factory, dimensions, name);
		offset = new int[dimensions.length];
		
		for (int i = 0; i < dimensions.length; ++i)
		{
			offset[i] = dimensions[i] / 2;
		}
	}
	
	public int[] getOffset()
	{
		return offset;
	}
	
	public static StructuringElement createBall(final int nd, final double radius)
	{
		StructuringElement strel;
		LocalizableCursor<BitType> cursor;
		final int[] dims = new int[nd];
		final int[] pos = new int[nd];
		double dist;
		
		for (int i = 0; i < dims.length; ++i)
		{
			dims[i] = (int)(radius * 2 + 1);
		}
		strel = new StructuringElement(dims, "Ball Structure " + nd + "D, " + radius);
		
		cursor = strel.createLocalizableCursor();
		
		while (cursor.hasNext())
		{
			dist = 0;
			cursor.fwd();
			cursor.getPosition(pos);
			for (int i = 0; i < dims.length; ++i)
			{
				dist += Math.pow(pos[i] - strel.offset[i], 2);
			}
			dist = Math.sqrt(dist);
			
			if (dist <= radius)
			{
				cursor.getType().setOne();
			}
			else
			{
				cursor.getType().setZero();
			}
		}
		cursor.close();
		strel.removeCursor(cursor);
		
		return strel;
	}
	
	public static StructuringElement createCube(final int nd, final int length)
	{
		StructuringElement strel;
		Cursor<BitType> cursor;
		final int[] dims = new int[nd];
		for (int i = 0; i < nd; ++i)
		{
			dims[i] = length;
		}
		
		strel = new StructuringElement(dims, "Cube Structure " + length);
		cursor = strel.createCursor(); 
		
		while (cursor.hasNext())
		{
			cursor.fwd();
			cursor.getType().setOne();
		}
		
		cursor.close();
		strel.removeCursor(cursor);
		
		return strel;
	}
	
	public static StructuringElement createBar(int nd, int length, int lengthDim)
	{		
		if (lengthDim >= nd)
		{
			throw new RuntimeException("Invalid bar dimension " + lengthDim + ". Only have " + nd +
					" dimensions.");
		}
		final int [] dims = new int[nd];
		Cursor<BitType> cursor;
		StructuringElement strel;
		
		for (int i = 0; i < nd; ++i)
		{
			if (i == lengthDim)
			{
				dims[i] = length;
			}
			else
			{
				dims[i] = 1;
			}
		}
		
		strel = new StructuringElement(dims, "Bar " + lengthDim + " of " + nd + ", " + length);
		cursor = strel.createCursor();
		
		while(cursor.hasNext())
		{
			cursor.fwd();
			cursor.getType().setOne();
		}
		
		cursor.close();
		strel.removeCursor(cursor);
		
		return strel;	
	}

}
