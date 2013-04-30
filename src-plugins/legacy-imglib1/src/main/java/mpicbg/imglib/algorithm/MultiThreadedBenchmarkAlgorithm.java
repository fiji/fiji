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

package mpicbg.imglib.algorithm;

/**
 * This is a convenience implementation of an algorithm that implements {@link MultiThreaded},
 * {@link Benchmark} and {@link Algorithm} so that less code has to be re-implemented.
 * 
 * IMPORTANT: It is not meant to be used for any other purpose than that, it should not be 
 * demanded by any other method or generic construct, use the interfaces instead.
 *   
 * @author Stephan Preibisch
 */
public abstract class MultiThreadedBenchmarkAlgorithm extends MultiThreadedAlgorithm implements Benchmark
{
	protected long processingTime = -1;

	public MultiThreadedBenchmarkAlgorithm() { super(); }
	
	@Override
	public long getProcessingTime() { return processingTime; }
}
