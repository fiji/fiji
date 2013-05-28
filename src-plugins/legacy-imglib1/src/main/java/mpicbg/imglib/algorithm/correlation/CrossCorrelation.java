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

package mpicbg.imglib.algorithm.correlation;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 */
public class CrossCorrelation< S extends RealType< S >, T extends RealType< T > > implements Algorithm, MultiThreaded
{
	public static < S extends RealType< S >, T extends RealType< T > > double correlate( final Image< S > img1, final Image< T > img2 )
	{
		final CrossCorrelation< S, T > cc = new CrossCorrelation< S, T >( img1, img2 );
		cc.process();
		return cc.getR();
	}

	final Image< S > image1;
	final Image< T > image2;
	
	double R = 0;
	
	int numThreads;
	
	public CrossCorrelation( final Image< S > image1, final Image< T > image2 )
	{
		this.image1 = image1;
		this.image2 = image2;
		
		setNumThreads();
	}
	
	public double getR() { return R; }
	
	@Override
	public boolean checkInput() { return true; }

	@Override
	public boolean process() 
	{
		final double avg1 = computeAvg( image1 );
		final double avg2 = computeAvg( image2 );

        final boolean isCompatible = image1.getContainer().compareStorageContainerCompatibility( image2.getContainer() ); 
       
        if ( isCompatible )
        	R = computeSimple( avg1, avg2 );
        else
        	R = computeAdvancedSum( avg1, avg2 );
        
		return true;
	}

	protected < R extends RealType< R > > double computeAvg( final Image< R > image )
	{
		double avg = 0;

		for ( final R type : image )
			avg += type.getRealDouble();
		
        return avg / (double)image.getNumPixels();		
	}

	protected double computeSimple( final double avg1, final double avg2 )
	{
		final Cursor<S> cursor1 = image1.createCursor();
		final Cursor<T> cursor2 = image2.createCursor();
		
		double var1 = 0, var2 = 0;
		double coVar = 0;

        // do as many pixels as wanted by this thread
		while ( cursor1.hasNext() )
        {
			cursor1.fwd();
			cursor2.fwd();
			
			final double pixel1 = cursor1.getType().getRealDouble();
			final double pixel2 = cursor2.getType().getRealDouble();
			
			final double dist1 = pixel1 - avg1;
			final double dist2 = pixel2 - avg2;

			coVar += dist1 * dist2;
			var1 += dist1 * dist1;
			var2 += dist2 * dist2;
		}		
		
		cursor1.close();
		cursor2.close();					

		var1 /= (double) image1.getNumPixels();
		var2 /= (double) image1.getNumPixels();
		coVar /= (double) image1.getNumPixels();
		
		double stDev1 = Math.sqrt(var1);
		double stDev2 = Math.sqrt(var2);

		// all pixels had the same color....
 		if ( stDev1 == 0 || stDev2 == 0 )
		{
			if ( stDev1 == stDev2 && avg1 == avg2 )
				return 1;
			else
				return 0;
		}

		// compute correlation coeffienct
		return coVar / (stDev1 * stDev2);
	}
	
	protected double computeAdvancedSum( final double avg1, final double avg2 )
	{
		final LocalizableByDimCursor<S> cursor1 = image1.createLocalizableByDimCursor();
		final LocalizableCursor<T> cursor2 = image2.createLocalizableCursor();

		double var1 = 0, var2 = 0;
		double coVar = 0;

		while ( cursor2.hasNext() )
        {
			cursor2.fwd();
			cursor1.setPosition( cursor2 );
			
			final double pixel1 = cursor1.getType().getRealDouble();
			final double pixel2 = cursor2.getType().getRealDouble();
			
			final double dist1 = pixel1 - avg1;
			final double dist2 = pixel2 - avg2;

			coVar += dist1 * dist2;
			var1 += dist1 * dist1;
			var2 += dist2 * dist2;
		}		
		
		cursor1.close();
		cursor2.close();					

		var1 /= (double) image1.getNumPixels();
		var2 /= (double) image1.getNumPixels();
		coVar /= (double) image1.getNumPixels();
		
		double stDev1 = Math.sqrt(var1);
		double stDev2 = Math.sqrt(var2);

		// all pixels had the same color....
 		if ( stDev1 == 0 || stDev2 == 0 )
		{
			if ( stDev1 == stDev2 && avg1 == avg2 )
				return 1;
			else
				return 0;
		}

		// compute correlation coeffienct
		return coVar / (stDev1 * stDev2);
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads(int numThreads) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }

}
