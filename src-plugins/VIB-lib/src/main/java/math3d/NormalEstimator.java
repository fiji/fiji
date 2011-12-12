package math3d;

/*
Use this class to estimate a plane from a number of points.

Least squares fitting:

given a set of points $a_i$, we want to find $\vec n$ and $d$, such that

$$\sum_i (\vec a_i\cdot\vec n - d)^2$$

is minimal. The derivative with respect to $n_x$ is

$$2\sum_i x_i(\vec a_i\cdot\vec n - d)$$

and the derivative with respect to $d$ is

$$-\sum_i (\vec a_i\cdot\vec n - d)$$

Therefore, a linear equation has to be solved:

 / x_i*x_i x_i*y_i x_i*z_i -x_i \  / n_x \
|  y_i*x_i y_i*y_i y_i*z_i -y_i  ||  n_y  |
|  z_i*x_i z_i*y_i z_i*z_i -z_i  ||  n_z  | = 0 
 \   x_i     y_i     z_i    -i  /  \  d  /

This equation is degenerated for the simple reason that the length of $\vec n$
cannot be limited in this way. Let's assume that $d$ is known, then this
equation becomes

 / x_i*x_i x_i*y_i x_i*z_i \  / n_x \     / x_i*d \
|  y_i*x_i y_i*y_i y_i*z_i  ||  n_y  | = |  y_i*d  |
 \ z_i*x_i z_i*y_i z_i*z_i /  \ n_z /     \ z_i*d /

Let's set $d = 1$. Then, $n$ is not normalized, but has the true $d$ as
length.

Of course, this approach must fail if the best choice for $d$ is 0. In that
case, the determinant of the matrix is 0, and one has to choose $n_x$
accordingly.
 */
public class NormalEstimator {
	double x, y, z, xx, yy, zz, xy, yz, xz, xyz;
	long total;

	public void reset() {
		x = y = z = xx = yy = zz = xy = yz = xz = xyz = 0;
		total = 0;
	}

	public void add(Point3d p) {
		x += p.x;
		y += p.y;
		z += p.z;
		xx += p.x * p.x;
		yy += p.y * p.y;
		zz += p.z * p.z;
		xy += p.x * p.y;
		yz += p.y * p.z;
		xz += p.x * p.z;
		xyz += p.x * p.y * p.z;
		total++;
	}

	// does not check if the point was added
	public void remove(Point3d p) {
		x -= p.x;
		y -= p.y;
		z -= p.z;
		xx -= p.x * p.x;
		yy -= p.y * p.y;
		zz -= p.z * p.z;
		xy -= p.x * p.y;
		yz -= p.y * p.z;
		xz -= p.x * p.z;
		xyz -= p.x * p.y * p.z;
		total--;
	}

	public Point3d normal;
	public double distance;

	public Point3d getNormal() {
		double det = xx * yy * zz + xy * yz * xz + xz * xy * yz
			- xx * yz * yz - xy * xy * zz - xz * yy * xz;
		if (det != 0.0) {
			double a11 = (yy * zz - yz * yz) / det;
			double a12 = (xz * yz - xy * zz) / det;
			double a13 = (xy * yz - yy * xz) / det;
			double a22 = (xx * zz - xz * xz) / det;
			double a23 = (xy * xz - xx * yz) / det;
			double a33 = (xx * yy - xy * xy) / det;
			double x1 = a11 * x + a12 * y + a13 * z;
			double y1 = a12 * x + a22 * y + a23 * z;
			double z1 = a13 * x + a23 * y + a33 * z;
			normal = new Point3d(x1, y1, z1);
			distance = normal.length();
		} else {
			distance = 0;
			// try n_x = 1
			double det1 = yy * zz - yz * yz;
			if (det1 != 0.0) {
				double y1 = (zz * -xy - yz * -xz) / det1;
				double z1 = (-yz * -xy + yy * -xz) / det1;
				normal = new Point3d(1, y1, z1);
			} else {
				// n_x = 0
				double a, b;
				distance = 0;
				if (xy != 0 && xz != 0)
					normal = new Point3d(0, 1, -xy / xz);
				else if (yy != 0 && yz != 0)
					normal = new Point3d(0, 1, -yy / yz);
				else if (yz != 0 && zz != 0)
					normal = new Point3d(0, 1, -yz / zz);
				else if (y != 0 && z != 0)
					normal = new Point3d(0, 1, -y / z);
				else if (xy != 0 || yy != 0 || yz != 0 || y != 0)
					normal = new Point3d(0, 0, 1);
				else if (xz != 0 || yz != 0 || zz != 0 || z != 0)
					normal = new Point3d(0, 1, 0);
				else 
					throw new RuntimeException("amiguous plane");
			}
		}
		normal = normal.times(1.0 / normal.length());
		return normal;
	}

	public long getTotal() {
		return total;
	}

	public Point3d getMean() {
		return new Point3d(x / (double)total,
				y / (double)total,
				z / (double)total);
	}

	public double distanceTo(Point3d p) {
		return Math.abs(distance - normal.scalar(p));
	}
	
	public static void main(String[] args) {
		NormalEstimator est = new NormalEstimator();
		Point3d n = new Point3d(1, +3, 1);
		n = n.times(1.0 / n.length());
		Point3d a = Point3d.random();
		for (int i = 0; i < 1000; i++) {
			Point3d b = Point3d.random();
			b = b.minus(n.times(n.scalar(b.minus(a))));
			est.add(b);
		}
		System.err.println("estimate "+est.getNormal());
		System.err.println("expect "+n);
		est.reset();
		est.add(new Point3d(-2, 1, 1));
		est.add(new Point3d(1, 1, -2));
		est.add(new Point3d(1, -2, 1));
		System.err.println("estimate "+est.getNormal());
		System.err.println("expect 1 1 1");
		est.reset();
		est.add(new Point3d(1, 0, 0));
		est.add(new Point3d(1, 1, 0));
		est.add(new Point3d(1, -2, 0));
		System.err.println("estimate "+est.getNormal());
		System.err.println("expect 0 0 1");
		est.reset();
		est.add(new Point3d(0, 0, 1));
		est.add(new Point3d(1, -1, 0));
		est.add(new Point3d(-1, 1, 0));
		System.err.println("estimate "+est.getNormal());
		System.err.println("expect 1 1 0");
	}
}



