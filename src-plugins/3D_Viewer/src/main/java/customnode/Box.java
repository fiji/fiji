package customnode;

import javax.vecmath.Point3f;

public class Box extends Primitive {

	public Box(Point3f min, Point3f max) {
		super(makeVertices(min, max), makeFaces());
	}

	private static Point3f[] makeVertices(Point3f min, Point3f max) {
		Point3f[] p = new Point3f[8];
		p[0] = new Point3f(min.x, min.y, max.z);
		p[1] = new Point3f(max.x, min.y, max.z);
		p[2] = new Point3f(max.x, max.y, max.z);
		p[3] = new Point3f(min.x, max.y, max.z);
		p[4] = new Point3f(min.x, min.y, min.z);
		p[5] = new Point3f(max.x, min.y, min.z);
		p[6] = new Point3f(max.x, max.y, min.z);
		p[7] = new Point3f(min.x, max.y, min.z);
		return p;
	}

	private static int[] makeFaces() {
		return new int[] {
			0, 1, 2, 0, 2, 3, // back
			4, 7, 6, 4, 6, 5, // front
			1, 5, 6, 1, 6, 2, // right
			0, 3, 7, 0, 7, 4, // left
			0, 4, 5, 0, 5, 1, // top
			7, 3, 2, 7, 2, 6  // bottom
		};
	}
}
