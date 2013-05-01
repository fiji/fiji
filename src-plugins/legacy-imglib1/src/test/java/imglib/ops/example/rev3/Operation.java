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

package imglib.ops.example.rev3;

import imglib.ops.example.rev3.constraints.Constraints;
import imglib.ops.example.rev3.function.IntegerIndexedScalarFunction;
import imglib.ops.observer.IterationStatus;
import imglib.ops.observer.IterationStatus.Message;

import java.util.Observable;
import java.util.Observer;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

// NOTICE
//   You can copy an image from another image by creating an ImageFunction on the other image and passing it into an Operation.
//   You should be able to setup an ImageFunction with an OutOfBoundsCursor.

// This class will assign to an output image the evaluation of a function across a given domain.

// note that it is possible to modify an image in place by passing it as the output image and also having it be an ImageFunction
// as part of the passed in function. Doing this is safe if the function's relative domain is one pixel in size. These checks are
// not in place at the moment but should be fairly straightforward to do. so right now its possible to try and convolve an image
// in place but realize the input pixels are being modified by the operation. again this points out need for domain information
// for a function. 

/**
 * TODO
 *
 */
public class Operation
{
	private final Image<? extends RealType<?>> outputImage;
	private final int[] origin;
	private final IntegerIndexedScalarFunction function;
	private final RegionOfInterestCursor<? extends RealType<?>> cursor;
	private boolean wasInterrupted;
	private boolean isDone;
	private Observable notifier;
	private Constraints constraints;
	
	@SuppressWarnings({"rawtypes","unchecked"})
	public Operation(Image<? extends RealType<?>> outputImage, int[] origin, int[] span, IntegerIndexedScalarFunction function)
	{
		this.outputImage = outputImage;
		this.origin = origin;
		this.function = function;
		LocalizableByDimCursor<? extends RealType<?>> tmpCursor = outputImage.createLocalizableByDimCursor();
		this.cursor = new RegionOfInterestCursor(tmpCursor, origin, span);  // nongeneric instantiation. SAFE?
		this.wasInterrupted = false;
		this.isDone = false;
		this.notifier = null;
		this.constraints = new Constraints();
	}
	
	// Note some inefficiency is execute() for in place transformations (basically because we're maintaining multiple cursors):
	//   we create a cursor on output image (image 1)
	//   we then iterate that cursor and use getPosition() to copy position
	//   we then evaluate the function which eventually points to our input image (again image 1)
	//   it sets its cursor position to position and returns its value

	public void execute()  // TODO - make this run in its own thread. multithread it too?
	{
		int[] position = new int[outputImage.getNumDimensions()];

		Status status = new Status();
		
		if (notifier != null)
		{
			status.message = Message.INITIALIZE;
			notifier.notifyObservers(status);
		}
		
		while (cursor.hasNext())
		{
			if (wasInterrupted)
				break;
			cursor.fwd();
			cursor.getPosition(position);
			for (int i = 0; i < position.length; i++)  // TODO - slowing HACK because RoiCursor returns relative position rather than absolute position
				position[i] += origin[i];
			boolean constraintsSatisfied = constraints.areSatisfied(position);
			if (constraintsSatisfied)
			{
				double newValue = function.evaluate(position);
				cursor.getType().setReal(newValue);
			}
			if (notifier != null)
			{
				status.message = Message.UPDATE;
				status.position = position;
				status.value = cursor.getType().getRealDouble();    // not sure what is best to pass as value if constraints
				status.conditionsSatisfied = constraintsSatisfied;  // violated but I think if I pass original value it might be
				notifier.notifyObservers(status);                   // useful info to caller. its incurs a small performance hit.
			}
		}
		
		if (notifier != null)
		{
			status.message = Message.DONE;
			status.wasInterrupted = wasInterrupted;
			notifier.notifyObservers(status);
		}
		
		isDone = true;
	}
	
	public void interrupt()  // this assumes execute() running in a different thread
	{
		wasInterrupted = true;
	}

	public boolean wasInterrupted()  // this assumes execute() running in a different thread
	{
		return wasInterrupted;
	}
	
	public boolean isDone()  // this assumes execute() running in a different thread
	{
		return isDone;
	}
	
	public void addObserver(Observer ob)
	{
		if (notifier == null)
			notifier = new Observable();
		
		notifier.addObserver(ob);
	}
	
	public void deleteObserver(Observer ob)
	{
		if (notifier != null)
		{
			notifier.deleteObserver(ob);
			// although this could improve performance in execute() it could cause problems there in a multithreaded context
			//if (notifier.countObservers() == 0)
			//	notifier = null;
		}
	}
	
	public void setConstraints(Constraints c)
	{
		constraints = c;
	}
	
	private class Status implements IterationStatus
	{
		Message message;
		int[] position;
		double value;
		boolean conditionsSatisfied;
		boolean wasInterrupted;

		@Override
		public Message getMessage()
		{
			return message;
		}

		@Override
		public int[] getPosition()
		{
			return position;
		}

		@Override
		public double getValue()
		{
			return value;
		}

		@Override
		public boolean getConditionsSatisfied()
		{
			return conditionsSatisfied;
		}

		@Override
		public boolean wasInterrupted()
		{
			return wasInterrupted;
		}
		
	}
}
