package fiji.plugin.trackmate.util;

import net.imglib2.Cursor;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.RealType;

public class SpotNeighborhoodCursor<T extends RealType<T>> implements Cursor<T> {

	/*
	 * FIELDs
	 */
	
	protected final Cursor<T> cursor;
	protected final double[] calibration;
	protected final long[] center;
	/** A utility holder to store position everytime required. */
	private final long[] pos;

	/*
	 * CONSTRUCTOR
	 */
	
	public SpotNeighborhoodCursor(SpotNeighborhood<T> sn) {
		this.cursor = sn.neighborhood.cursor();
		this.calibration = sn.calibration;
		this.center = sn.center;
		this.pos = new long[cursor.numDimensions()];
		reset();
	}
	
	/*
	 * METHODS
	 * These methods are specific and are mainly focused on the use of calibrated units.
	 */

	/**
	 * Store the relative <b>calibrated</b> position with respect to the neighborhood center.
	 */
	public void getRelativePosition(double[] position) {
		cursor.localize(pos);
		for (int d = 0; d < center.length; d++) {
			position[d] = calibration[d] * (pos[d] - center[d]);
		}
	}
	
	/**
	 * Return the square distance measured from the center of the domain to the current
	 * cursor position, in <b>calibrated</b> units.
	 */
	public double getDistanceSquared() {
		cursor.localize(pos);
		double sum = 0;
		double dx = 0;
		for (int d = 0; d < pos.length; d++) {
			dx = calibration[d] * ( pos[d] - center[d] );
			sum += (dx * dx);
		}
		return sum;
	}
	
	/**
	 * Return the current inclination with respect to this spot center. Will be in
	 * the range [0, π]. 
	 * <p>
	 * In spherical coordinates, the inclination is the angle 
	 * between the Z axis and the line OM where O is the sphere center and M is 
	 * the point location.
	 */
	public double getTheta() {
		if (numDimensions() < 2)
			return 0;
		double dx = calibration[2] * ( cursor.getDoublePosition(2) - center[2]);
		return Math.acos( dx / Math.sqrt( getDistanceSquared() ) );
	}
	
	/**
	 * Return the azimuth of the spherical coordinates of this cursor, with respect 
	 * to its center. Will be in the range ]-π, π].
	 * <p>
	 * In spherical coordinates, the azimuth is the angle measured in the plane XY between 
	 * the X axis and the line OH where O is the sphere center and H is the orthogonal 
	 * projection of the point M on the XY plane.
	 */
	public double getPhi() {
		double dx = calibration[0] * ( cursor.getDoublePosition(0) - center[0]);
		double dy = calibration[1] * ( cursor.getDoublePosition(1) - center[1]);
		return Math.atan2(dy, dx);
	}
	
	/*
	 * CURSOR METHODS
	 * We delegate to the wrapped cursor
	 */

	@Override
	public void localize(float[] position) {
		cursor.localize(position);
	}

	@Override
	public void localize(double[] position) {
		cursor.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return cursor.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return cursor.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return cursor.numDimensions();
	}

	@Override
	public T get() {
		return cursor.get();
	}

	@Override
	public Sampler<T> copy() {
		return cursor.copy();
	}

	@Override
	public void jumpFwd(long steps) {
		cursor.jumpFwd(steps);
	}

	@Override
	public void fwd() {
		cursor.fwd();
	}

	@Override
	public void reset() {
		cursor.reset();
	}

	@Override
	public boolean hasNext() {
		return cursor.hasNext();
	}

	@Override
	public T next() {
		return cursor.next();
	}

	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public void localize(int[] position) {
		cursor.localize(position);
	}

	@Override
	public void localize(long[] position) {
		cursor.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return cursor.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return cursor.getLongPosition(d);
	}

	@Override
	public Cursor<T> copyCursor() {
		return cursor.copyCursor();
	}

	
}
