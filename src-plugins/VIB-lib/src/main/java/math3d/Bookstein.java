/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * This implements the Bookstein transform in 3d
 */

package math3d;

public class Bookstein {
	protected Point3d[] points;
	protected Bookstein1d bx, by, bz;
	
	public Bookstein(Point3d[] orig, Point3d[] trans) {
		if (orig.length != trans.length)
			throw new RuntimeException("orig has " + orig.length
						   + " points, but trans has " + trans.length
						   + "???");
		points = orig;
		int N = orig.length + 4;
		double[][] matrix = new double[N][N];
		for (int i = 0; i < orig.length; i++) {
			for (int j = i + 1; j < orig.length; j++)
				matrix[i][j] = U(orig[i].distanceTo(orig[j]));
			matrix[i][orig.length] = 1;
			matrix[i][orig.length + 1] = orig[i].x;
			matrix[i][orig.length + 2] = orig[i].y;
			matrix[i][orig.length + 3] = orig[i].z;
			for (int j = i + 1; j < N; j++)
				matrix[j][i] = matrix[i][j];
		}

		FastMatrixN.invert(matrix);

		bx = new Bookstein1d();
		by = new Bookstein1d();
		bz = new Bookstein1d();
		bx.w = new double[orig.length];
		by.w = new double[orig.length];
		bz.w = new double[orig.length];
		for (int i = 0; i < orig.length; i++)
			for (int j = 0; j < orig.length; j++) {
				bx.w[i] += trans[j].x * matrix[i][j];
				by.w[i] += trans[j].y * matrix[i][j];
				bz.w[i] += trans[j].z * matrix[i][j];
			}
		for (int j = 0; j < orig.length; j++) {
			bx.a1 += trans[j].x * matrix[orig.length][j];
			bx.ax += trans[j].x * matrix[orig.length + 1][j];
			bx.ay += trans[j].x * matrix[orig.length + 2][j];
			bx.az += trans[j].x * matrix[orig.length + 3][j];
			by.a1 += trans[j].y * matrix[orig.length][j];
			by.ax += trans[j].y * matrix[orig.length + 1][j];
			by.ay += trans[j].y * matrix[orig.length + 2][j];
			by.az += trans[j].y * matrix[orig.length + 3][j];
			bz.a1 += trans[j].z * matrix[orig.length][j];
			bz.ax += trans[j].z * matrix[orig.length + 1][j];
			bz.ay += trans[j].z * matrix[orig.length + 2][j];
			bz.az += trans[j].z * matrix[orig.length + 3][j];
		}
	}
	
	public double x, y, z;
	public void apply(Point3d p) {
		x = bx.evalInit(p);
		y = by.evalInit(p);
		z = bz.evalInit(p);
		for (int i = 0; i < points.length; i++) {
			double u = U(p.distanceTo(points[i]));
			x += bx.w[i] * u;
			y += by.w[i] * u;
			z += bz.w[i] * u;
		}
	}
	
	public class Bookstein1d {
		double a1, ax, ay, az;
		public double[] w;
		
		public double evalInit(Point3d p) {
			return a1 + ax * p.x + ay * p.y + az * p.z;
		}
		
		public double eval(Point3d p) {
			double res = evalInit(p);
			for (int i = 0; i < points.length; i++)
				res += w[i] * U(p.distanceTo(points[i]));
			return res;
		}
	}
	
	public static double U(double r) {
		if (r <= 0) return 0;
		return r * r * Math.log(r);
	}
}
