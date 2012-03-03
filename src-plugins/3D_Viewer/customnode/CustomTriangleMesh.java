package customnode;

import isosurface.MeshProperties;

import java.util.List;
import java.util.Arrays;

import java.awt.Polygon;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.Transform3D;
import javax.media.j3d.Canvas3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3f;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

import java.util.ArrayList;


public class CustomTriangleMesh extends CustomMesh {

	private double volume = 0.0;

	public CustomTriangleMesh(List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	public CustomTriangleMesh(List<Point3f> mesh, Color3f col, float trans) {
		super(mesh, col, trans);
		if(mesh != null) {
			Point3d center = new Point3d();
			double[][] inertia = new double[3][3];
			volume = MeshProperties.compute(mesh, center, inertia);
		}
	}

	public void setMesh(List<Point3f> mesh) {
		this.mesh = mesh;
		update();
	}

	public void addTriangles(Point3f[] v) {
		if(v.length % 3 != 0)
			throw new IllegalArgumentException(
				"Number must be a multiple of 3");
		addVertices(v);
	}

	private Point3f[] threePoints = new Point3f[3];
	public void addTriangle(Point3f p1, Point3f p2, Point3f p3) {
		threePoints[0] = p1;
		threePoints[1] = p2;
		threePoints[2] = p3;
		addVertices(threePoints);
	}

	private int[] threeIndices = new int[3];
	public void removeTriangle(int index) {
		int offs = 3 * index;
		threeIndices[0] = offs;
		threeIndices[1] = offs + 1;
		threeIndices[2] = offs + 2;
		removeVertices(threeIndices);
	}

	public void removeTriangles(int[] indices) {
		Arrays.sort(indices);
		int[] vIndices = new int[indices.length * 3];
		for(int i = 0, j = 0; i < indices.length; i++) {
			int index = indices[i];
			int offs = 3 * index;
			vIndices[j++] = offs;
			vIndices[j++] = offs + 1;
			vIndices[j++] = offs + 2;
		}
		removeVertices(vIndices);
	}

	@Override
	protected GeometryArray createGeometry() {
		if(mesh == null || mesh.size() < 3)
			return null;
		List<Point3f> tri = mesh;
		int nValid = tri.size();
		int nAll = 2 * nValid;

		Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		Color3f colors[] = new Color3f[nValid];
		if (null == color) {
			// Vertex-wise colors are not stored
			// so they have to be retrieved from the geometry:
			for (int i=0; i<colors.length; ++i) {
				colors[i] = new Color3f(DEFAULT_COLOR);
			}
			GeometryArray gaOld = (GeometryArray) getGeometry();
			if (null != gaOld)
				gaOld.getColors(0, colors);
		} else {
			Arrays.fill(colors, color);
		}

		GeometryArray ta = new TriangleArray(nAll,
						TriangleArray.COORDINATES |
						TriangleArray.COLOR_3 |
						TriangleArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		GeometryInfo gi = new GeometryInfo(ta);
// 		gi.recomputeIndices();
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
// 		Stripifier st = new Stripifier();
// 		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		result.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		result.setCapability(GeometryArray.ALLOW_INTERSECT);
		result.setValidVertexCount(nValid);

		return result;
	}

	private Point2d p2d = new Point2d();
	private boolean roiContains(Point3f p, Transform3D volToIP, Canvas3D canvas, Polygon polygon) {
		Point3d locInImagePlate = new Point3d(p);
		volToIP.transform(locInImagePlate);
		canvas.getPixelLocationFromImagePlate(locInImagePlate, p2d);
		return polygon.contains(p2d.x, p2d.y);
	}

	public void retain(Canvas3D canvas, Polygon polygon) {
		Transform3D volToIP = new Transform3D();
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();

		Transform3D toVWorld = new Transform3D();
		this.getLocalToVworld(toVWorld);
		volToIP.mul(toVWorld);

		ArrayList<Point3f> f = new ArrayList<Point3f>();
		for(int i = 0; i < mesh.size(); i += 3) {
			Point3f p1 = mesh.get(i);
			Point3f p2 = mesh.get(i + 1);
			Point3f p3 = mesh.get(i + 2);
			if(roiContains(p1, volToIP, canvas, polygon) ||
					roiContains(p2, volToIP, canvas, polygon) ||
					roiContains(p3, volToIP, canvas, polygon)) {
				f.add(p1);
				f.add(p2);
				f.add(p3);
			}
		}
		mesh.clear();
		mesh.addAll(f);
		update();
	}

	@Override
	public float getVolume() {
		return (float)volume;
	}
}
