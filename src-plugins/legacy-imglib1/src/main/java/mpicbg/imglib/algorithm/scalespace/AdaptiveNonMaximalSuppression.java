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

package mpicbg.imglib.algorithm.scalespace;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.kdtree.KDTree;
import mpicbg.imglib.algorithm.kdtree.RadiusNeighborSearch;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian.SpecialPoint;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class AdaptiveNonMaximalSuppression<T extends RealType<T>> implements Algorithm, Benchmark
{
	final List<DifferenceOfGaussianPeak<T>> detections;
	double radius;
	
	long processingTime;
	String errorMessage = "";

	/**
	 * Performs adaptive non maximal suppression in the local neighborhood of each detection, seperately 
	 * for minima and maxima. It sets all extrema to invalid if their value is absolutely smaller than any other
	 * in the local neighborhood of a point.
	 * The method getClearedList() can also return an {@link ArrayList} that only contains valid remaining
	 * {@link DifferenceOfGaussianPeak}s.
	 * 
	 * @param detections - the {@link List} of {@link DifferenceOfGaussianPeak}s
	 * @param radius - the radius of the local neighborhood
	 */
	public AdaptiveNonMaximalSuppression( final List<DifferenceOfGaussianPeak<T>> detections, final double radius )
	{
		this.detections = detections;
		this.radius = radius;
		
		processingTime = -1;
	}
	
	/**
	 * Creates a new {@link ArrayList} that only contains all {@link DifferenceOfGaussianPeak}s that are valid.
	 * 
	 * @return - {@link ArrayList} of {@link DifferenceOfGaussianPeak}s
	 */
	public ArrayList<DifferenceOfGaussianPeak<T>> getClearedList()
	{
		final ArrayList<DifferenceOfGaussianPeak<T>> clearedList = new ArrayList<DifferenceOfGaussianPeak<T>>();
		
		for ( final DifferenceOfGaussianPeak<T> peak : detections )
			if ( peak.isValid() )
				clearedList.add( peak );
		
		return clearedList;
	}
	
	@Override
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
		
		final KDTree<DifferenceOfGaussianPeak<T>> tree = new KDTree<DifferenceOfGaussianPeak<T>>( detections );
		final RadiusNeighborSearch<DifferenceOfGaussianPeak<T>> search = new RadiusNeighborSearch<DifferenceOfGaussianPeak<T>>( tree );

		for ( final DifferenceOfGaussianPeak<T> det : detections )
		{
			// if it has not been invalidated before we look if it is the highest detection
			if ( det.isValid() )
			{
				final ArrayList<DifferenceOfGaussianPeak<T>> neighbors = search.findNeighborsUnsorted( det, radius );				
				final ArrayList<DifferenceOfGaussianPeak<T>> extrema = new ArrayList<DifferenceOfGaussianPeak<T>>();
				
				for ( final DifferenceOfGaussianPeak<T> peak : neighbors )
					if ( det.isMax() && peak.isMax() ||  det.isMin() && peak.isMin() )
						extrema.add( peak );

				invalidateLowerEntries( extrema, det );
			}
		}
		
		processingTime = System.currentTimeMillis() - startTime;
		
		return true;
	}
	
	protected void invalidateLowerEntries( final ArrayList<DifferenceOfGaussianPeak<T>> extrema, final DifferenceOfGaussianPeak<T> centralPeak )
	{
		final double centralValue = Math.abs( centralPeak.getValue().getRealDouble() );
		
		for ( final DifferenceOfGaussianPeak<T> peak : extrema )
			if ( Math.abs( peak.getValue().getRealDouble() ) < centralValue )
				peak.setPeakType( SpecialPoint.INVALID );		
	}
	
	@Override
	public boolean checkInput()
	{
		if ( detections == null )
		{
			errorMessage = "List<DifferenceOfGaussianPeak<T>> detections is null.";
			return false;
		}
		else if ( detections.size() == 0 )
		{
			errorMessage = "List<DifferenceOfGaussianPeak<T>> detections is empty.";
			return false;			
		}
		else if ( Double.isNaN( radius ) )
		{
			errorMessage = "Radius is NaN.";
			return false;						
		}
		else
			return true;
	}
	
	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public long getProcessingTime() { return processingTime; }
}
