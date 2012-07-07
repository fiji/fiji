package customnode;

import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Tube extends Primitive{

	public static final int DEFAULT_PARALLELS = 12;

	public Tube(List<Point3f> points, float r) {
		this(points, r, DEFAULT_PARALLELS);
	}
	public Tube(List<Point3f> points, float r, int parallels) {
		super(makeVertices(points, r, parallels), makeFaces(points.size(), parallels));
	}

	private static Point3f[] makeVertices(List<Point3f> points, float r, int parallels) {
		Point3f[] p = new Point3f[points.size() * parallels];

		// first set of parallels
		Point3f p0 = points.get(0);
		Point3f p1 = points.get(1);
		for(int i = 0; i < parallels; i++) {
			double a = (i - 6) * (2 * Math.PI) / 12;
			double c = r * Math.cos(a);
			double s = r * Math.sin(a);
			p[i] = new Point3f((float)c, (float)s, 0);
		}
		Matrix4f ry = new Matrix4f();
		float ay = (float)Math.atan2((p1.x - p0.x), (p1.z - p0.z));
		ry.rotY(ay);

		Matrix4f rx = new Matrix4f();
		float ax = -(float)Math.asin((p1.y - p0.y) / p1.distance(p0));
		rx.rotX(ax);

		rx.mul(ry, rx);
		for(int i = 0; i < parallels; i++) {
			Point3f pi = p[i];
			rx.transform(pi);
			pi.add(p0);
		}

		// between
		Point3f p2;
		for(int pi = 1; pi < points.size() - 1; pi++) {
			p0 = points.get(pi - 1);
			p1 = points.get(pi);
			p2 = points.get(pi + 1);


			Vector3f p0p1 = new Vector3f();
			p0p1.sub(p1, p0);
			Vector3f p1p2 = new Vector3f();
			p1p2.sub(p2, p1);
			p0p1.normalize();
			p1p2.normalize();
			Vector3f plane = new Vector3f();
			plane.add(p0p1);
			plane.add(p1p2);
			plane.normalize();

			Vector3f transl = new Vector3f();
			transl.sub(p1, p0);

			// project onto plane
			for(int i = 0; i < parallels; i++) {
				int idx0 = ((pi - 1) * parallels + i);
				int idx1 = (pi * parallels + i);
				p[idx1] = new Point3f(p[idx0]);
				p[idx1].add(transl);
				p[idx1] = intersect(p[idx0], p[idx1], plane, p1);
			}
		}

		// last set of parallels
		p0 = points.get(points.size() - 2);
		p1 = points.get(points.size() - 1);
		int offset = (points.size() - 1) * parallels;
		for(int i = 0; i < parallels; i++) {
			double a = (i - 6) * (2 * Math.PI) / 12;
			double c = r * Math.cos(a);
			double s = r * Math.sin(a);
			p[offset + i] = new Point3f((float)c, (float)s, 0);
		}
		ry = new Matrix4f();
		ay = (float)Math.atan2((p1.x - p0.x), (p1.z - p0.z));
		ry.rotY(ay);

		rx = new Matrix4f();
		ax = -(float)Math.asin((p1.y - p0.y) / p1.distance(p0));
		rx.rotX(ax);

		rx.mul(ry, rx);
		for(int i = 0; i < parallels; i++) {
			Point3f pi = p[offset + i];
			rx.transform(pi);
			pi.add(p1);
		}
		return p;
	}

	private static int[] makeFaces(int nPts, int parallels) {
		int idx = 0;
		int[] faces = new int[2 * 3 * parallels * (nPts - 1)];
		for(int pi = 0; pi < nPts - 1; pi++) {
			int offs0 = pi * parallels;
			int offs1 = (pi + 1) * parallels;
			for(int i = 0; i < parallels; i++) {
				faces[idx++] = offs0 + i;
				faces[idx++] = offs1 + i;
				faces[idx++] = offs1 + (i + 1) % parallels;
				faces[idx++] = offs0 + i;
				faces[idx++] = offs1 + (i + 1) % parallels;
				faces[idx++] = offs0 + (i + 1) % parallels;
			}
		}
		return faces;
	}

	private static Point3f intersect(Point3f p1, Point3f p2, Vector3f n, Point3f p3) {
		// http://paulbourke.net/geometry/planeline/
		Vector3f v1 = new Vector3f();
		v1.sub(p3, p1);
		Vector3f v2 = new Vector3f();
		v2.sub(p2, p1);
		float u = (n.dot(v1)) / (n.dot(v2));
		Point3f res = new Point3f();
		res.scaleAdd(u, v2, p1);
		return res;
	}
}
