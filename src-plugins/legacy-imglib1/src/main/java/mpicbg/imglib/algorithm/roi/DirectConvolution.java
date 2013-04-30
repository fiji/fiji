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

import mpicbg.imglib.algorithm.ROIAlgorithm;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.ShortType;

/**
 * DirectConvolution is an ROIAlgorithm designed to do both convolution and cross-correlation 
 * by operating on the image and kernel directly, rather than by using such time-saving tricks as
 * FFT.
 * @param <T> input image type
 * @param <R> kernel type
 * @param <S> output image type
 *
 * @author Larry Lindsey
 */
public class DirectConvolution
	<T extends RealType<T>, R extends RealType<R>, S extends RealType<S>>
		extends ROIAlgorithm<T, S>
{

	protected static void quickKernel2D(short[][] vals, Image<ShortType> kern)
	{
		final LocalizableByDimCursor<ShortType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i)
		{
			for (int j = 0; j < vals[i].length; ++j)
			{
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				cursor.getType().set(vals[i][j]);
			}
		}
		cursor.close();		
	}
	
	public static Image<ShortType> sobelVertical()
	{
		final ImageFactory<ShortType> factory = new ImageFactory<ShortType>(new ShortType(),
				new ArrayContainerFactory());
		final Image<ShortType> sobel = factory.createImage(new int[]{3, 3}, "Vertical Sobel");
		final short[][] vals = {{-1, -2, -1},
				{0, 0, 0},
				{1, 2, 1}};
		
		quickKernel2D(vals, sobel);		
		
		return sobel;
	}
	
	public static Image<ShortType> sobelHorizontal()
	{
		final ImageFactory<ShortType> factory = new ImageFactory<ShortType>(new ShortType(),
				new ArrayContainerFactory());
		final Image<ShortType> sobel = factory.createImage(new int[]{3, 3}, "Horizontal Sobel");
		final short[][] vals = {{1, 0, -1},
				{2, 0, -2},
				{1, 0, -1}};
		
		quickKernel2D(vals, sobel);		
		
		return sobel;
	}
	
	
	private final Image<R> kernel;
	private final int[] kernelSize;
	private LocalizableByDimCursor<S> outputImageCursor;
	private final LocalizableByDimCursor<R> kernelCursor;
	private final boolean doInvert;
	
	public DirectConvolution(final S type, final Image<T> inputImage, final Image<R> kernel)
	{
		this(type, inputImage, kernel, null);
	}
	
	
	public DirectConvolution(final S type, final Image<T> inputImage, final Image<R> kernel,
			final OutOfBoundsStrategyFactory<T> outsideFactory) {
		this(type, inputImage, kernel, outsideFactory, true);
	}
	
	protected DirectConvolution(final S type, final Image<T> inputImage, final Image<R> kernel,
			final OutOfBoundsStrategyFactory<T> outsideFactory, final boolean isconv)
	{
		super(type, inputImage, kernel.getDimensions(), outsideFactory);
		
		this.kernel = kernel;
		outputImageCursor = null;
		kernelSize = kernel.getDimensions();
		kernelCursor = kernel.createLocalizableByDimCursor();
		
		setName(inputImage.getName() + " * " + kernel.getName());
		
		doInvert = isconv;
	}
	
	private LocalizableByDimCursor<S> getOutputCursor()
	{
		if (outputImageCursor == null)
		{
			outputImageCursor = getOutputImage().createLocalizableByDimCursor();
		}		
		return outputImageCursor;
	}
	
	private void invertPosition(final int[] pos, final int[] invPos)
	{
	    for (int i = 0; i < kernel.getNumDimensions(); ++i)
		{
			invPos[i] = kernelSize[i] - pos[i] - 1;
		}
	}
	
	@Override
	protected boolean patchOperation(final int[] position, final RegionOfInterestCursor<T> roiCursor) {
		final LocalizableByDimCursor<S> outCursor = getOutputCursor();
		final int[] pos = new int[outCursor.getNumDimensions()];
		final int[] invPos = new int[outCursor.getNumDimensions()];
		S accum = outCursor.getImage().createType();
		S mul = outCursor.getImage().createType();
		S temp = outCursor.getImage().createType();
		
		accum.setZero();
		
		outCursor.setPosition(position);
		
		while(roiCursor.hasNext())
		{
			mul.setOne();
			roiCursor.fwd();
			roiCursor.getPosition(pos);
			
			if (doInvert)
			{
				invertPosition(pos, invPos);			
				kernelCursor.setPosition(invPos);
			}
			else
			{
				kernelCursor.setPosition(pos);
			}

			temp.setReal(kernelCursor.getType().getRealDouble());
			temp.setComplex(-kernelCursor.getType().getComplexDouble());			
			mul.mul(temp);
			
			temp.setReal(roiCursor.getType().getRealDouble());
			temp.setComplex(roiCursor.getType().getComplexDouble());
			mul.mul(temp);
			
			accum.add(mul);			
		}
				
		outCursor.getType().set(accum);
		return true;
	}

	@Override
	public boolean checkInput() {
		if (super.checkInput())
		{
			if (kernel.getNumDimensions() == getOutputImage().getNumActiveCursors())
			{
				setErrorMessage("Kernel has different dimensionality than the Image");
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}

}
