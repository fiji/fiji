package customnode;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

public class Cone extends Primitive {

	public static final int DEFAULT_PARALLELS = 12;

	public Cone(Point3f from, Point3f to, float r) {
		this(from, to, r, DEFAULT_PARALLELS);
	}

	public Cone(Point3f from, Point3f to, float r, int parallels) {
		super(makeVertices(from, to, r, parallels), makeFaces(parallels));
	}

	private static Point3f[] makeVertices(Point3f from, Point3f to, float r, int parallels) {
		Point3f[] p = new Point3f[parallels + 2];
		p[0] = new Point3f(from);
		p[1] = new Point3f(to);

		for(int i = 0; i < parallels; i++) {
			double a = (i - 6) * (2 * Math.PI) / 12;
			double c = r * Math.cos(a);
			double s = r * Math.sin(a);
			p[i + 2] = new Point3f((float)c, (float)s, 0);
		}
		Matrix4f ry = new Matrix4f();
		float ay = (float)Math.atan2((to.x - from.x), (to.z - from.z));
		ry.rotY(ay);

		Matrix4f rx = new Matrix4f();
		float ax = -(float)Math.asin((to.y - from.y) / from.distance(to));
		rx.rotX(ax);

		rx.mul(ry, rx);
		for(int i = 2; i < p.length; i++) {
			Point3f pi = p[i];
			rx.transform(pi);
			pi.add(from);
		}
		return p;
	}

	private static int[] makeFaces(int parallels) {
		int idx = 0;
		int[] faces = new int[2 * 3 * parallels];
		for(int i = 0; i < parallels; i++) {
			faces[idx++] = 2 + i;
			faces[idx++] = 2 + (i + 1) % parallels;
			faces[idx++] = 0;
			faces[idx++] = 2 + i;
			faces[idx++] = 1;
			faces[idx++] = 2 + (i + 1) % parallels;
		}
		return faces;
	}
}


