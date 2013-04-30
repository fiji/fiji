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

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.util.RealSum;

/**
 * Normalizes a given {@link Image} so that the sum of all of its pixels is equal to one
 * (or approximately so). 
 * @param <T> Image type
 * @author Larry Lindsey
 */
public class NormalizeImageFloat <T extends RealType<T>> implements OutputAlgorithm<FloatType>, Benchmark
{
	private final Image<T> image;
	private Image<FloatType> outputImage;
	private String errorMsg;
	private long pTime;
	
	
	public static <T extends RealType<T>> double sumImage( final Image<T> image )
	{
		final RealSum sum = new RealSum();
		final Cursor<T> cursor = image.createCursor();
		
		while (cursor.hasNext())
		{
			cursor.fwd();
			sum.add(cursor.getType().getRealFloat());
		}
		
		cursor.close();
		
		return sum.getSum();
	}
	
	public NormalizeImageFloat(final Image<T> imageInput)
	{
		errorMsg = "";
		outputImage = null;
		pTime = 0;
		image = imageInput;
	}
	
	@Override
	public boolean process()
	{
		long startTime = System.currentTimeMillis();
		final double norm = sumImage(image);
		final int[] dims = image.getDimensions();
		final ImageFactory<FloatType> factory =
			new ImageFactory<FloatType>(new FloatType(), image.getContainerFactory());  		
		
		final LocalizableCursor<T> pullCursor;
		final LocalizableByDimCursor<FloatType> pushCursor;
		
		if (norm == 0)
		{
			errorMsg = "Zero Sum Image";
			return false;
		}		
		
		outputImage = factory.createImage(dims);
		pushCursor = outputImage.createLocalizableByDimCursor();
		pullCursor = image.createLocalizableCursor();
			
		while(pullCursor.hasNext())
		{			
			pullCursor.fwd();
			pushCursor.setPosition(pullCursor);
			pushCursor.getType().set((float)(pullCursor.getType().getRealFloat() / norm));
		}
		
		pTime = System.currentTimeMillis() - startTime;
		
		pullCursor.close();
	    pushCursor.close();
	    
		return true;
	}

	@Override
	public Image<FloatType> getResult() {		
		return outputImage;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMsg;
	}

	@Override
	public long getProcessingTime() {		
		return pTime;
	}
}
