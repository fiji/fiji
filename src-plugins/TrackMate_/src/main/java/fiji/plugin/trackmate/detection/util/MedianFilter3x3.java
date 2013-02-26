package fiji.plugin.trackmate.detection.util;

import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.img.Img;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.Type;

public class MedianFilter3x3<T extends Type<T> & Comparable<T>> extends BenchmarkAlgorithm implements OutputAlgorithm<Img<T>> {

	private final Img<T> source;
	private final SquareNeighborhood3x3<T> domain;
	private Img<T> output;
	
	public MedianFilter3x3(Img<T> source) {
		this.source = source;
		long[] dim = new long[source.numDimensions()];
		source.dimensions(dim);
		this.output = source.factory().create(dim , source.firstElement().copy());
		this.domain = new SquareNeighborhood3x3<T>(source, 
				new OutOfBoundsConstantValueFactory<T, RandomAccessibleInterval<T>>(source.firstElement().createVariable()));
	}
	
	
	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		final Cursor<T> cursor = source.localizingCursor();
		final Cursor<T> outCursor = output.cursor();
		final SquareNeighborhoodCursor3x3<T> neighborhoodCursor = domain.localizingCursor();
		final ArrayList<T> values = new ArrayList<T>(9);
		
		while (cursor.hasNext()) {
			cursor.fwd();
			outCursor.fwd();
			domain.setPosition(cursor);
			values.clear();
			
			while (neighborhoodCursor.hasNext()) {
				neighborhoodCursor.fwd();
				if (neighborhoodCursor.isOutOfBounds())
					continue;
				
				values.add(neighborhoodCursor.get());
			}

			Collections.sort(values);
			int n = values.size() / 2;
			outCursor.get().set(values.get(n));
		}
		
		this.processingTime = System.currentTimeMillis() - start;
		return true;
	}

	@Override
	public Img<T> getResult() {
		return output;
	} 

}
