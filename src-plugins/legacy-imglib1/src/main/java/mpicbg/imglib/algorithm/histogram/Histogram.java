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

package mpicbg.imglib.algorithm.histogram;

import java.util.ArrayList;
import java.util.Hashtable;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.Type;

/**
 * Implements a Histogram over an Image.
 *
 * @author Larry Lindsey
 */
public class Histogram <T extends Type<T>> implements Algorithm, Benchmark
{
	/**
	 * Processing time, milliseconds.
	 */
	private long pTime = 0;
	
	/**
	 * Hold the histogram itself.
	 */
	private Hashtable<HistogramKey<T>, HistogramBin<T>>  hashTable;
	
	/**
	 * The Cursor from which the histogram is to be calculated.
	 */
	private Cursor<T> cursor;
	
	/**
	 * The HistogramBinFactory to use for generating HistogramBin's and
	 * HistogramKey's.
	 */
	private final HistogramBinFactory<T> binFactory;	

	/**
	 * Create a Histogram using the given factory, calculating from the given
	 * Cursor.
	 * @param factory the HistogramBinFactory used to generate  
	 * {@link HistogramKey}s and {@link HistogramBin}s 
	 * @param c a Cursor corresponding to the Image from which the Histogram
	 * will be calculated
	 * 
	 */
	public Histogram(HistogramBinFactory<T> factory, Cursor<T> c)
	{
		cursor = c;
		hashTable = new Hashtable<HistogramKey<T>, HistogramBin<T>>();
		binFactory = factory;		
	}
	
	/**
	 * Returns an ArrayList containing the {@link HistogramKey}s generated
	 * when calculating this Histogram.
	 * @return an ArrayList containing the {@link HistogramKey}s generated
	 * when calculating this Histogram.
	 */
	public ArrayList<HistogramKey<T>> getKeys()
	{
		return new ArrayList<HistogramKey<T>>(hashTable.keySet());
	}

	/**
	 * Returns the center {@link Type} corresponding to each
	 * {@link HistogramKey} generated when calculating this Histogram.
	 * @return
	 */
	public ArrayList<T> getKeyTypes()
	{
		ArrayList<HistogramKey<T>> keys = getKeys();
		ArrayList<T> types = new ArrayList<T>(keys.size());
		for (HistogramKey<T> hk : keys)
		{
			types.add(hk.getType());			
		}
		
		return types;
	}
	
	/**
	 * Returns the bin corresponding to a given {@link Type}.
	 * @param t the Type corresponding to the requested 
	 * {@link HistogramBin}
	 * @return The requested HistogramBin.
	 */
	public HistogramBin<T> getBin(T t)
	{
		return getBin(binFactory.createKey(t));
	}
	
	/**
	 * Returns the bin corresponding to a given {@link HistogramKey}.
	 * @param key the HistogramKey corresponding to the requested 
	 * {@link HistogramBin}
	 * @return The requested HistogramBin.
	 */
	public HistogramBin<T> getBin(HistogramKey<T> key)
	{
		if (hashTable.containsKey(key))
		{
			return hashTable.get(key);
		}
		else
		{
			/*
			 * If the hash table doesn't contain the key in question, create a 
			 * zero bin and return that.
			 */
			HistogramBin<T> zeroBin = binFactory.createBin(key.getType());
			return zeroBin;
		}
	}
	
	@Override
	public boolean checkInput() {		
		return true;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public boolean process() {
		long startTime = System.currentTimeMillis();
		
		while (cursor.hasNext())
		{			
			cursor.fwd();
			//Create a key for the given type
			HistogramKey<T> key = binFactory.createKey(cursor.getType());
			//Grab the HistogramBin corresponding to that key, if it exists.
			HistogramBin<T> bin = hashTable.get(key);
			
			if (bin == null)
			{
				//If there wasn't a bin already, create one and add it to the 
				//hash table.
				bin = binFactory.createBin(key.getType());
				hashTable.put(key, bin);
			}
			
			//Increment the count of the bin.
			bin.inc();
		}
		
		pTime = System.currentTimeMillis() - startTime;
		return true;
	}

	@Override
	public long getProcessingTime() {		
		return pTime;
	}
	
}
