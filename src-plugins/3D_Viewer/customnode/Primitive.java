package customnode;

import javax.vecmath.Point3f;

public abstract class Primitive extends CustomIndexedTriangleMesh {
	public Primitive(Point3f[] vertices, int[] faces) {
		super(vertices, faces);
	}
}
