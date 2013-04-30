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

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Open morphological operation. Operates by creating a {@link MorphErode} and a
 * {@link MorphDilate}, taking the output from the first, and passing it to the second.
 * 
 * @param <T> {@link Image} type.
 * @author Larry Lindsey
 */
public class MorphOpen<T extends RealType<T>> implements OutputAlgorithm<T>, Benchmark
{
	
	private final Image<T> image;
	private Image<T> outputImage;
	private MorphDilate<T> dilater;
	private final MorphErode<T> eroder;
	private final StructuringElement strel;
	private final OutOfBoundsStrategyFactory<T> outsideFactory;
	private long pTime;
	
	public MorphOpen(Image<T> imageIn, StructuringElement strelIn)
	{
		this(imageIn, strelIn, null);
	}
	
	public MorphOpen(Image<T> imageIn, StructuringElement strelIn,
			final OutOfBoundsStrategyFactory<T> inOutsideFactory)
	{
		image = imageIn;		
		strel = strelIn;
		eroder = new MorphErode<T>(image, strel, inOutsideFactory);
		dilater = null;
		outputImage = null;
		outsideFactory = inOutsideFactory;
		pTime = 0;		
	}
	
	@Override
	public Image<T> getResult()
	{
		return outputImage;
	}

	@Override
	public boolean checkInput()
	{		
		return true;
	}

	@Override
	public String getErrorMessage() {
		String errorMsg = "";
		errorMsg += eroder.getErrorMessage();
		if (dilater != null)
		{
			errorMsg += dilater.getErrorMessage();
		}
		
		return errorMsg;
	}

	@Override
	public boolean process() {
		final long sTime = System.currentTimeMillis();
		pTime = 0;
		boolean rVal = false;
		
		if (eroder.process())
		{
			dilater = new MorphDilate<T>(eroder.getResult(), strel, outsideFactory);
			dilater.setName(image.getName() + " Opened");
			rVal = dilater.process();			
		}
		
		if (rVal)
		{
			outputImage = dilater.getResult();
		}
		else
		{
			outputImage = null;
		}
		
		pTime = System.currentTimeMillis() - sTime;
		
		return rVal;
	}

	@Override
	public long getProcessingTime() {
		return pTime;
	}

}
