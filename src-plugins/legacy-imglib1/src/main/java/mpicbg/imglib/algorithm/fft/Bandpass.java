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

package mpicbg.imglib.algorithm.fft;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 */
public class Bandpass<T extends NumericType<T>> implements OutputAlgorithm<T>, Benchmark
{
	String errorMessage = "";
	boolean inPlace, bandPass;
	
	Image<T> img, output;
	
	int beginRadius, endRadius;
	long processingTime;
	int[] origin;
	
	public Bandpass( final Image<T> img, final int beginRadius, final int endRadius )
	{
		this.img = img;		

		this.inPlace = false;
		this.bandPass = true;
		this.beginRadius = beginRadius;
		this.endRadius = endRadius;
		
		this.origin = img.createPositionArray();
		
		this.origin[ 0 ] = img.getDimension( 0 ) - 1;
		for ( int d = 1; d < this.origin.length; ++d )
			origin[ d ] = img.getDimension( d ) / 2;
	}
	
	public void setImage( final Image<T> img ) { this.img = img; }
	public void setInPlace( final boolean inPlace ) { this.inPlace = inPlace; }
	public void setBandPass( final boolean bandPass ) { this.bandPass = bandPass; }
	public void setOrigin( final int[] position ) { this.origin = position.clone(); }
	public void setBandPassRadius( final int beginRadius, final int endRadius )
	{
		this.beginRadius = beginRadius;
		this.endRadius = endRadius;
	}

	public Image<T> getImage() { return img; }
	public boolean getInPlace() { return inPlace; }
	public int getBeginBandPassRadius() { return beginRadius; }
	public int getEndBandPassRadius() { return endRadius; }
	public int[] getOrigin() { return origin; }
	
	@Override
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
		final Image<T> img;

		if ( inPlace )
		{
			img = this.img;
		}
		else
		{
			this.output = this.img.clone(); 
			img = this.output;
		}
		
		final LocalizableCursor<T> cursor = img.createLocalizableCursor();
		final int[] pos = img.createPositionArray();
		
		final boolean actAsBandPass = bandPass;
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.getPosition( pos );
			
			final float dist = Util.computeDistance( origin, pos );
			
			if ( actAsBandPass )
			{
				if ( dist < beginRadius || dist > endRadius )
					cursor.getType().setZero();
			}
			else
			{
				if ( dist >= beginRadius && dist <= endRadius )
					cursor.getType().setZero();				
			}
		}
		
		processingTime = System.currentTimeMillis() - startTime;
		
		// finished applying bandpass
		return true;
	}	
	
	@Override
	public Image<T> getResult()
	{
		if ( inPlace )
			return img;
		else
			return output;
	}

	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public boolean checkInput() { return true; }

	@Override
	public String getErrorMessage() { return errorMessage; }	
}
