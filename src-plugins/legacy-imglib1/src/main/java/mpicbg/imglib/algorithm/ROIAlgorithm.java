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
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.algorithm;

import java.util.Arrays;

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * ROIAlgorithm implements a framework against which to build operations of one image against
 * another, like convolution, cross-correlation, or morphological operations.  It operates by
 * creating a {@link RegionOfInterestCursor} from the input image, and an output image, which is
 * assumed to be of the same size as the input.  The process() method curses over the output image,
 * and at each step calls the abstract method patchOperation(), giving it the current location in 
 * the output and input images, as well as the RegionOfInterestCursor, which will curse over a 
 * patch of a given size in the input image.  patchOperation() is responsible for setting the
 * value of the pixel at the given position in the output image.
 * 
 * @param <T>
 * @param <S>
 *
 * @author Larry Lindsey
 */
public abstract class ROIAlgorithm <T extends RealType<T>, S extends RealType<S>>
	implements OutputAlgorithm<S>, Benchmark
{

	private final RegionOfInterestCursor<T> roiCursor;
	private final int[] patchSize;
	private final int[] originOffset;
	private final Image<T> inputImage;
	private final OutOfBoundsStrategyFactory<T> outsideFactory;
	private Image<S> outputImage;	
	private ImageFactory<S> imageFactory;		
	private String errorMsg;
	private String name;
	private final S typeS;
	private long pTime;

	/**
	 * Creates an ROIAlgorithm with default {@link OutOfBoundsStrategyValueFactory}, with value
	 * Zero.
	 * @param type a representative type for the output image.
	 * @param imageIn the image over which to iterate.
	 * @param patchSize the size of the patch that will be examined at each iteration.
	 */
	protected ROIAlgorithm(final S type, final Image<T> imageIn, final int[] patchSize)
	{
		this(type, imageIn, patchSize, null);
	}
	
	/**
	 * 
	 * @param type a representative type for the output image.
	 * @param imageIn the image over which to iterate.
	 * @param patchSize the size of the patch that will be examined at each iteration.
	 * @param inOutFactory an {@link OutOfBoundsStrategyFactory} to handle the border phenomenon.
	 */
	protected ROIAlgorithm(final S type, final Image<T> imageIn, final int[] patchSize,
			final OutOfBoundsStrategyFactory<T> inOutFactory)
	{		
		int nd = imageIn.getNumDimensions();
		
		final int[] initPos = new int[nd];
		
		pTime = 0;
		originOffset = new int[nd];
		inputImage = imageIn;
		this.patchSize = patchSize.clone();
		outputImage = null;
		imageFactory = null;
		errorMsg = "";
		name = null;
		typeS = type.copy();
		Arrays.fill(initPos, 0);
		
		if (inOutFactory == null)
		{
			T typeT = imageIn.createType();
			typeT.setZero();
			outsideFactory = new OutOfBoundsStrategyValueFactory<T>(typeT); 
		}
		else
		{
			outsideFactory = inOutFactory;
		}
		
		roiCursor = imageIn.createLocalizableByDimCursor(outsideFactory)
			.createRegionOfInterestCursor(initPos, patchSize);
		
		for (int i = 0; i < nd; ++i)
		{
			//Dividing an int by 2 automatically takes the floor, which is what we want.
			originOffset[i] = patchSize[i] / 2;
		}		
	}

	/**
	 * Performs this algorithm's operation on the patch represented by a
	 * {@link RegionOfInterestCursor} 
	 * @param position the position in the input image at the center of the patch represented by
	 * the RegionOfInterestCursor
	 * @param cursor a {@link RegionOfInterestCursor} that iterates over the given
	 * patch-of-interest
	 * @return true if the operation on the given patch was successful, false otherwise.  If false
	 * is returned by this method, process() will abort. 
	 */
	protected abstract boolean patchOperation(final int[] position,
			final RegionOfInterestCursor<T> cursor);
	
	/**
	 * Set the {@link ImageFactory} used to create the output {@link Image} 
	 * @param factory the {@link ImageFactory} used to create the output {@link Image}
	 */
	public void setImageFactory(final ImageFactory<S> factory)
	{
		imageFactory = factory;
	}
	
	/**
	 * Set the name given to the output image.
	 * @param inName the name to give to the output image.
	 */
	public void setName(final String inName)
	{
		name = inName;
	}
		
	public String getName()
	{
		return name;
	}
	
	public int[] getPatchSize()
	{
		return patchSize.clone();
	}

	/**
	 * Returns the {@link Image} that will eventually become the result of this
	 * {@link OutputAlgorithm}, and creates it if it has not yet been created.
	 * @return the {@link Image} that will eventually become the result of this
	 * {@link OutputAlgorithm}.
	 */
	protected Image<S> getOutputImage()
	{		
		if (outputImage == null)
		{
			if (imageFactory == null)
			{			
				imageFactory = new ImageFactory<S>(typeS, inputImage.getContainerFactory());
			}
					
			if (name == null)
			{
				outputImage = imageFactory.createImage(inputImage.getDimensions());
			}
			else
			{
				outputImage = imageFactory.createImage(inputImage.getDimensions(), name);				
			}
		}
		return outputImage;
	}
	
	@Override
	public Image<S> getResult()
	{		
		return outputImage;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMsg;
	}
	
	protected void setErrorMessage(String message)
	{
		errorMsg = message;
	}

	/**
	 * Offsets the given position to reflect the origin of the patch being in its center, rather
	 * than at the top-left corner as is usually the case.
	 * @param position the position to be offset
	 * @param offsetPosition an int array to contain the newly offset position coordinates
	 * @return offsetPosition, for convenience.
	 */
	protected int[] positionOffset(final int[] position, final int[] offsetPosition)
	{
		if (offsetPosition.length < position.length)
		{
			throw new RuntimeException("Cannot copy " + position.length 
					+ " values into array of length " + offsetPosition.length);
		}
		else if (position.length < originOffset.length){
			throw new RuntimeException("Position vector has less cardinality than " +
					"the input image's dimensionality.");
		}
			
		for (int i = 0; i < position.length; ++i)
		{
			offsetPosition[i] = position[i] - originOffset[i];
		}
		return offsetPosition;
	}
	
	@Override
	public boolean process()
	{
		final LocalizableCursor<S> outputCursor = getOutputImage().createLocalizableCursor();
		final int[] pos = new int[inputImage.getNumDimensions()];
		final int[] offsetPos = new int[inputImage.getNumDimensions()];
		final long sTime = System.currentTimeMillis();
		
		while (outputCursor.hasNext())
		{
			outputCursor.fwd();			
			outputCursor.getPosition(pos);
			roiCursor.reset(positionOffset(pos, offsetPos));
						
			if (!patchOperation(pos, roiCursor))
			{
				outputCursor.close();
				
				return false;
			}
		}
			
		outputCursor.close();
		
		pTime = System.currentTimeMillis() - sTime;
		return true;		
	}
	
	@Override
	public boolean checkInput()
	{
		return roiCursor.isActive();
	}
	
	public void close()
	{
		roiCursor.close();
	}

	@Override
	public long getProcessingTime()
	{
		return pTime;
	}
	
}
