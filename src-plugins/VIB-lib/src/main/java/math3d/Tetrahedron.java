package math3d;

public class Tetrahedron {
	Point3d a, b, c, d;

	Point3d center;
	double radius2;

	public Point3d calculateCircumcenter() {
		Point3d x = b.minus(a);
		Point3d y = c.minus(a);
		Point3d z = d.minus(a);
		double xx = x.scalar(x);
		double xy = x.scalar(y);
		double xz = x.scalar(z);
		double yy = y.scalar(y);
		double yz = y.scalar(z);
		double zz = z.scalar(z);

		double det = xx * yy * zz + xy * yz * xz
			+ xz * xy * yz
			- xy * xy * zz - xz * yy * xz - xx * yz * yz;
		// inverse of a symmetric is symmetric again
		double a11 = (yy * zz - yz * yz) / det;
		double a12 = (yz * xz - xy * zz) / det;
		double a13 = (xy * yz - yy * xz) / det;
		double a22 = (xx * zz - xz * xz) / det;
		double a23 = (xy * xz - xx * yz) / det;
		double a33 = (xx * yy - xy * xy) / det;
		double alpha = (a11 * xx + a12 * yy + a13 * zz) / 2;
		double beta = (a12 * xx + a22 * yy + a23 * zz) / 2;
		double gamma = (a13 * xx + a23 * yy + a33 * zz) / 2;

		center = new Point3d(a.x + alpha * x.x + beta * y.x + gamma * z.x,
				a.y + alpha * x.y + beta * y.y +gamma * z.y,
				a.z + alpha * x.z + beta * y.z + gamma * z.z);
		radius2 = center.distance2(a);

		return center;
	}

	public static void test() {
		Tetrahedron t = new Tetrahedron();
		t.a = Point3d.random();
		t.b = Point3d.random();
		t.c = Point3d.random();
		t.d = Point3d.random();
		Point3d c = t.calculateCircumcenter();
	}
}


