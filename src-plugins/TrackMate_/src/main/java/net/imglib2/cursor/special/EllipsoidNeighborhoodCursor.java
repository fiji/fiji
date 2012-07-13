package net.imglib2.cursor.special;

import net.imglib2.Bounded;
import net.imglib2.Cursor;
import net.imglib2.Sampler;

public class EllipsoidNeighborhoodCursor<T> implements Cursor<T>, Bounded  {

	protected EllipsoidNeighborhood<T> ellipsoid;

	public EllipsoidNeighborhoodCursor(final EllipsoidNeighborhood<T> ellipsoid) {
		this.ellipsoid = ellipsoid;
	}

	@Override
	public void localize(float[] position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void localize(double[] position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getFloatPosition(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDoublePosition(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int numDimensions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Sampler<T> copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void jumpFwd(long steps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fwd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void localize(int[] position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void localize(long[] position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getIntPosition(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLongPosition(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isOutOfBounds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor<T> copyCursor() {
		// TODO Auto-generated method stub
		return null;
	}

}
