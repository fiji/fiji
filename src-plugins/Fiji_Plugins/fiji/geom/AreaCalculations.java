package fiji.geom;

/*
 * This class provides methods to calculate circumference, area and center
 * of gravity of polygons.
 *
 * It is part of Fiji (http://fiji.sc/), and is licensed under
 * the GPLv2.  If you do not know what the GPLv2 is, you have no license
 * to use it at all.
 */
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

public class AreaCalculations {
	protected abstract static class Calculation {
		protected double[] start = new double[2],
			  previous = new double[2], current = new double[2];

		public double result; // for convenience

		public abstract void handleSegment();

		public void apply(PathIterator path) {
			double[] swap;

			while (!path.isDone()) {
				switch (path.currentSegment(current)) {
				case PathIterator.SEG_MOVETO:
					System.arraycopy(current, 0,
							start, 0, 2);
					break;
				case PathIterator.SEG_CLOSE:
					System.arraycopy(start, 0,
							current, 0, 2);
					/* fallthru */
				case PathIterator.SEG_LINETO:
					handleSegment();
					break;
				default:
					throw new RuntimeException("invalid "
						+ "polygon");
				}
				swap = current;
				current = previous;
				previous = swap;
				path.next();
			}
		}

		public double calculate(PathIterator path) {
			result = 0;
			apply(path);
			return result;
		}
	}

	protected static class Circumference extends Calculation {
		public void handleSegment() {
			double x = current[0] - previous[0];
			double y = current[1] - previous[1];
			result += Math.sqrt(x * x + y * y);
		}
	}

	/** Compute the perimeter of the path or multiple paths in @param path. */
	public static double circumference(PathIterator path) {
		return new Circumference().calculate(path);
	}

	public static double triangleArea(double[] a, double[] b, double[] c) {
		/* half the scalar product between (b - a)^T and (c - a) */
		return ((a[1] - b[1]) * (c[0] - a[0]) +
			(b[0] - a[0]) * (c[1] - a[1])) / 2;
	}

	/* This assumes even/odd winding rule, and it has a sign */
	protected static class Area extends Calculation {
		public void handleSegment() {
			result += triangleArea(start, previous, current);
		}
	}

	/** Compute the surface area of the path or multuple paths in @param path; returns a positive value for counter-clockwise paths, and negative for clockwise paths. Considers holes as holes; i.e. will do the right operation. */
	public static double area(PathIterator path) {
		return new Area().calculate(path);
	}

	protected static class Centroid extends Calculation {
		double totalArea, x, y;

		public void handleSegment() {
			double area = triangleArea(start, previous, current);
			totalArea += area;
			x += (start[0] + previous[0] + current[0]) / 3 * area;
			y += (start[1] + previous[1] + current[1]) / 3 * area;
		}

		public double[] getResult() {
			return new double[] { x / totalArea, y / totalArea };
		}
	}

	public static double[] centroid(PathIterator path) {
		Centroid centroid = new Centroid();
		centroid.apply(path);
		return centroid.getResult();
	}

	public static void main(String[] args) {
		GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
		path.moveTo(100, 100);
		path.lineTo(200, 110);
		path.lineTo(190, 213);
		path.lineTo(105, 205);
		path.closePath();

		double result = circumference(path.getPathIterator(null));
		double expect = Math.sqrt(100 * 100 + 10 * 10) +
			Math.sqrt(10 * 10 + 103 * 103) +
			Math.sqrt(85 * 85 + 8 * 8) +
			Math.sqrt(5 * 5 + 105 * 105);
		System.err.println("result: " + result + ", expect: " + expect +
				", diff: " + (result - expect));

		result = triangleArea(new double[] { 100, 100 },
			new double[] { 200, 110 }, new double[] { 190, 213 });
		expect = 100 * 113 - 100 * 10 / 2 - 10 * 103 / 2 - 90 * 113 / 2;
		System.err.println("triangleArea: " + result
				+ ", expect: " + expect
				+ ", diff: " + (result - expect));

		result = triangleArea(new double[] { 100, 100 },
			new double[] { 190, 213 }, new double[] { 105, 205 });
		expect = 90 * 113 - 90 * 113 / 2 - 85 * 8 / 2 - 5 * 105.0 / 2
			- 5 * 8;
		System.err.println("triangleArea: " + result
				+ ", expect: " + expect
				+ ", diff: " + (result - expect));

		result = area(path.getPathIterator(null));
		expect = 100 * 113 - 100 * 10 / 2 - 10 * 103 / 2
			- 85 * 8 / 2 - 5 * 105.0 / 2.0 - 5 * 8;
		System.err.println("result: " + result + ", expect: " + expect +
				", diff: " + (result - expect));

		double[] result2 = centroid(path.getPathIterator(null));
		double area1 = triangleArea(new double[] { 100, 100 },
			new double[] { 200, 110 }, new double[] { 190, 213 });
		double area2 = triangleArea(new double[] { 100, 100 },
			new double[] { 190, 213 }, new double[] { 105, 205 });
		double[] expect2 = new double[2];
		expect2[0] = (area1 * (100 + 200 + 190) / 3
				+ area2 * (100 + 190 + 105) / 3)
			/ (area1 + area2);
		expect2[1] = (area1 * (100 + 110 + 213) / 3
				+ area2 * (100 + 213 + 205) / 3)
			/ (area1 + area2);
		System.err.println("result: " + result2[0] + ", " + result2[1]
			+ ", expect: " + expect2[0] + ", " + expect2[1] +
			", diff: " + Math.sqrt((result2[0] - expect2[0])
				* (result2[0] - expect2[0]) +
				(result2[1] - expect2[1]) *
				(result2[1] - expect2[1])));
	}
}
