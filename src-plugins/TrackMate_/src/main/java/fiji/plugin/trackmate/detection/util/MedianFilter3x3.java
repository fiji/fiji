package fiji.plugin.trackmate.detection.util;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.img.Img;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.numeric.RealType;

public class MedianFilter3x3<T extends RealType<T>> extends BenchmarkAlgorithm implements OutputAlgorithm<Img<T>> {

	private final Img<T> source;
	private final SquareNeighborhood3x3<T> domain;
	private Img<T> output;
	
	public MedianFilter3x3(Img<T> source) {
		this.source = source;
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
		this.output = source.factory().create(source , source.firstElement().copy());
		final Cursor<T> outCursor = output.cursor();
		final float[] values = new float[9];
		
		while (cursor.hasNext()) {
			cursor.fwd();
			outCursor.fwd();
			
			domain.setPosition(cursor);
			SquareNeighborhoodCursor3x3<T> neighborhoodCursor = domain.localizingCursor();
			int index = 0;
			while (neighborhoodCursor.hasNext()) {
				neighborhoodCursor.fwd();
				if (neighborhoodCursor.isOutOfBounds())
					continue;
				
				values[index++] = neighborhoodCursor.get().getRealFloat();
			}

			Arrays.sort(values, 0, index);
			outCursor.get().setReal(values[(index-1)/2]);
		}
		
		this.processingTime = System.currentTimeMillis() - start;
		return true;
	}

	@Override
	public Img<T> getResult() {
		return output;
	}
}
