package vib;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.measure.Calibration;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import ij3d.Image3DUniverse;

import fiji.util.IntArray;
import fiji.util.DoubleArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import math3d.NormalEstimator;
import math3d.Point3d;

public class Extract_Surface implements PlugInFilter {
	ImagePlus image;
	InterpolatedImage ii;
	Calibration calib;

	public void run(ImageProcessor ip) {
		calib = image.getCalibration();
		GenericDialog gd = new GenericDialog("Transform Parameters");
		gd.addNumericField("LowerThreshold", 1, 0);
		gd.addNumericField("UpperThreshold", 255, 0);
		gd.addNumericField("CullRadius", 6, 0);
		gd.addStringField("outputFileName", "");
		gd.addStringField("outputVRMLFileName", "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		lowerThreshold = (int)gd.getNextNumber();
		upperThreshold = (int)gd.getNextNumber();
		double cullRadius = gd.getNextNumber();
		String fileName = gd.getNextString();
		String wrlFileName = gd.getNextString();

		maxDev2 = cullRadius * cullRadius;
		ii = new InterpolatedImage(image);

		IJ.showProgress(0, 6);
		IJ.showStatus("find vertices");
		getAllVertices();
		IJ.showProgress(1, 6);
		IJ.showStatus("reassociate vertices");
		reassociateVertices();
		//IJ.showProgress(2, 6);
		//IJ.showStatus("get normals");
		//getNormals();
		IJ.showProgress(3, 6);
		IJ.showStatus("get edges");
		//showAllPoints();
		getEdges();
		IJ.showProgress(4, 6);
		IJ.showStatus("get triangles");
		//showEdges(0xff);
		getTriangles();
		IJ.showProgress(5, 6);
		IJ.showStatus("show triangles");
		showTriangles(0xff0000);
		IJ.showProgress(6, 6);

		if (!wrlFileName.equals(""))
			saveWRL(wrlFileName);
		if (!fileName.equals(""))
			saveAsText(fileName);
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C | NO_CHANGES;
	}

	/*
	 * A SurfaceVoxel contains more information than just its coordinate.
	 * Therefore, use a Map instead of a Set. Thus, a voxel can be
	 * retrieved by constructing a new SurfaceVoxel and using it as a key.
	 */
	Map surfaceVoxels = new TreeMap();
	Vector vertices = new Vector();

	class SurfaceVoxel implements Comparable {
		int i, j, k;

		int vertexIndex; // the next vertex index
		double distance; // squared distance to vertex
		Vector neighbours;

		public SurfaceVoxel(int i, int j, int k) {
			this.i = i; this.j = j; this.k = k;
			vertexIndex = -1;
		}

		public int hashCodeOld() {
			return i | (j << 11) | (k << 22);
		}

		public boolean equals(Object o) {
			SurfaceVoxel other = (SurfaceVoxel)o;
			return i == other.i && j == other.j && k == other.k;
		}

		// lexicographic order
		public int compareTo(Object o) {
			SurfaceVoxel other = (SurfaceVoxel)o;
			int diff = i - other.i;
			if (diff == 0) {
				diff = j - other.j;
				if (diff == 0)
					diff = k - other.k;
			}
			return diff;
		}

		public String toString() {
			return "" + i + " " + j + " " + k;
		}

		public Point3d getPoint3d() {
			return new Point3d(calib.xOrigin + i * calib.pixelWidth,
				calib.yOrigin + j * calib.pixelHeight,
				calib.zOrigin + k * calib.pixelDepth);
		}

		public Point3f getPoint3f() {
			return new Point3f(
				(float)(calib.xOrigin + i * calib.pixelWidth),
				(float)(calib.yOrigin + j * calib.pixelHeight),
				(float)(calib.zOrigin + k * calib.pixelDepth));
		}

		public double distance2(SurfaceVoxel other) {
			return getPoint3d().distance2(other.getPoint3d());
		}

		public SurfaceVoxel getVertex() {
			if (vertexIndex < 0)
				return null;
			return (SurfaceVoxel)vertices.get(vertexIndex);
		}

		public void setVertex(int index) {
			vertexIndex = index;
			distance = distance2(getVertex());
		}

		public class NeighbourIterator implements Iterator {
			Iterator iter;

			NeighbourIterator() {
				if (neighbours == null)
					getNeighbours();
				iter = neighbours.iterator();
			}

			public boolean hasNext() {
				return iter.hasNext();
			}

			public Object next() {
				return iter.next();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}

			final private void getNeighbours() {
				neighbours = new Vector();
				for (int z = k - 1; z <= k + 1; z++)
					for (int y = j - 1; y <= j + 1; y++)
						for (int x = i - 1; x <= i + 1;
								x++)
							getNeighbour(x, y, z);
			}

			final private void getNeighbour(int x, int y, int z) {
				if (z == k && y == j && x == i)
					return;
				if (!testVoxel(x, y, z))
					return;
				neighbours.add(getVoxel(x, y, z));
			}

		}

		public NeighbourIterator iterator() {
			return new NeighbourIterator();
		}
	}

	public SurfaceVoxel getVertex(int index) {
		return (SurfaceVoxel)vertices.get(index);
	}

	public SurfaceVoxel getVoxel(int i, int j, int k) {
		SurfaceVoxel voxel, voxel2;
		voxel = new SurfaceVoxel(i, j, k);
		voxel2 = (SurfaceVoxel)surfaceVoxels.get(voxel);
		if (voxel2 != null)
			return voxel2;
		surfaceVoxels.put(voxel, voxel);
		return voxel;
	}

	public SurfaceVoxel getFirstPoint() {
		InterpolatedImage.Iterator iter = ii.iterator();
		while (iter.next() != null) {
			if (testVoxel(iter.i, iter.j, iter.k)) {
				SurfaceVoxel voxel = getVoxel(iter.i, iter.j,
					iter.k);
				surfaceVoxels.put(voxel, voxel);
				return voxel;
			}
		}
		return null;
	}

	/**
	 * This class implements a queue where points are popped by distance
	 * to a center (nearer points first).
	 */
	static class NeighbourQueue {
		Point3d center;
		Vector queue;
		Set done;
		DoubleArray distances;
		boolean pushNeighboursAutomatically;

		public NeighbourQueue(SurfaceVoxel center) {
			this(center, true);
		}

		public NeighbourQueue(SurfaceVoxel center, boolean autoGrow) {
			pushNeighboursAutomatically = autoGrow;
			this.center = center.getPoint3d();
			queue = new Vector();
			distances = new DoubleArray();
			done = new TreeSet();
			push(center);
		}

		public SurfaceVoxel pop() {
			int newSize = queue.size() - 1;
			SurfaceVoxel result = (SurfaceVoxel)queue.get(newSize);
			queue.setSize(newSize);
			distances.setSize(newSize);

			if (pushNeighboursAutomatically) {
				// handle neighbours
				Iterator iter = result.iterator();
				while (iter.hasNext())
					push((SurfaceVoxel)iter.next());
			}

			return result;
		}

		public void push(SurfaceVoxel voxel) {
			if (done.contains(voxel))
				return;

			double distance = center.distance2(voxel.getPoint3d());
			int i;
			for (i = 0; i < distances.size() &&
					distance < distances.get(i);
					i++);
			distances.insert(i, distance);
			queue.add(i, voxel);
			done.add(voxel);
		}

		public boolean contains(SurfaceVoxel v) {
			return queue.contains(v);
		}

		public Iterator iterator() {
			return queue.iterator();
		}

		public int size() {
			return queue.size();
		}

		public double getDistance() {
			int index = size() - 1;
			if (index < 0)
				return Double.MAX_VALUE;
			return distances.get(index);
		}
	}

	Set vertexCandidates;

	double maxDev2;

	/*
	 * Get as many surface voxels in the vicinity as possible,
	 * provided that they are no further away than a certain
	 * distance (maxDev).
	 * Whenever a voxel is hit that belongs to another vertex,
	 * add that vertex as neighbour.
	 */
	public void addVertex(SurfaceVoxel vertex) {
		final int vertexIndex = vertices.size();
		vertices.addElement(vertex);

		NeighbourQueue queue = new NeighbourQueue(vertex);

		while (queue.getDistance() < maxDev2) {
			SurfaceVoxel voxel = (SurfaceVoxel)queue.pop();

			if (voxel.vertexIndex >= 0)
				continue;

			voxel.setVertex(vertexIndex);
		}

		Iterator iter = queue.iterator();
		while (iter.hasNext())
			vertexCandidates.add(iter.next());
	}

	/*
	 * Find all vertices
	 *
	 * 1. start with a vertex
	 * 2. iterate over the neighbouring surface voxels sorted by distance
	 *    to the vertex, until a certain distance is reached
	 * 3. all neighbouring surface voxels are vertex candidates
	 * 4. pick the next vertex candidate which does not yet have a vertex
	 *    as the new vertex
	 * 5. loop from step 2 until no more vertex candidates are left
	 */
	public void getAllVertices() {
		vertexCandidates = new TreeSet();
		SurfaceVoxel first = getFirstPoint();
		if (first == null)
			throw new RuntimeException("Empty material!");
		vertexCandidates.add(first);
		while (vertexCandidates.size() > 0) {
			Iterator iter = vertexCandidates.iterator();
			while (iter.hasNext()) {
				SurfaceVoxel voxel = (SurfaceVoxel)iter.next();
				if (voxel.vertexIndex < 0) {
					addVertex(voxel);
					break;
				}
				iter.remove();
			}
		}
	}

	void showAllPoints() {
		showAllPoints(false);
	}


	Color3f makeColor(int v) {
		return new Color3f(((v >> 16) & 0xff) / 255.0f,
				((v >> 8) & 0xff) / 255.0f,
				(v & 0xff) / 255.0f);
	}

	void showAllPoints(boolean clearObjects) {
		showPoints(surfaceVoxels.keySet().iterator(), 0xff0000);
	/*
		getUniverse();

		if (clearObjects)
			ic.objects.clear();

		Color3f[] colors = new int[vertices.size()];
		for (int i = 0; i < colors.length; i++)
			colors[i] = makeColor(vertices.get(i).hashCode());

		Iterator iter = surfaceVoxels.keySet().iterator();
		while (iter.hasNext()) {
			SurfaceVoxel v = (SurfaceVoxel)iter.next();
			Color3f color = (v.vertexIndex < 0 ? makeColor(0xff0000)
					: colors[v.vertexIndex]);
			if (v == v.getVertex())
				color = makeColor(0xffffff);
			addPointToRenderObject(ro, v.getPoint3d(), color);
		}

		if (clearObjects) {
			Matrix m2 = new Matrix();
			m2.setRotationXyzProgressive(0, 0.01f, 0);
			ic.rotate(m2);
		}
		ic.repaint();
	*/
	}

	/*
	 * Reassociate the surface voxels with their vertices:
	 *
	 * 1. for all vertices
	 *    1.1 iterate over the neighbouring surface voxels sorted by
	 *        distance to the vertex:
	 *        1.1.1 if that voxel has a different vertex assigned, and
	 *              if the distance to that vertex is bigger than
	 *              to the current one, assign the current vertex
	 *              and iterate over the neighbours
	 */
	void reassociateVertices() {
		for (int i = 0; i < vertices.size(); i++) {
			SurfaceVoxel vertex = (SurfaceVoxel)vertices.get(i);

			NeighbourQueue queue = new NeighbourQueue(vertex,
					false);

			while (queue.size() > 0) {
				SurfaceVoxel voxel = queue.pop();

				if (voxel.vertexIndex != i) {
					double d = voxel.distance2(vertex);
					if (d > voxel.distance)
						continue;
					voxel.setVertex(i);
				}

				Iterator iter = voxel.iterator();
				while (iter.hasNext()) {
					SurfaceVoxel neighbour;
					neighbour = (SurfaceVoxel)iter.next();
					queue.push(neighbour);
				}
			}
		}
	}

	static Point3f Point3d2Point3f(Point3d p) {
		return new Point3f((float)p.x, (float)p.y, (float)p.z);
	}

	Point3f[] normals;

	void getNormals() {
		normals = new Point3f[vertices.size()];
		for (int i = 0; i < normals.length; i++) {
			SurfaceVoxel vertex = getVertex(i);
			NeighbourQueue queue = new NeighbourQueue(vertex);
			NormalEstimator est = new NormalEstimator();

			while (queue.getDistance() < maxDev2) {
				SurfaceVoxel voxel = queue.pop();
				est.add(voxel.getPoint3d());
			}
			normals[i] = Point3d2Point3f(est.getNormal());
		}
	}

	class Edge implements Comparable {
		int a, b;

		public Edge(int a, int b) {
			if (a < b) {
				this.a = a; this.b = b;
			} else if (a == b) {
				throw new RuntimeException("illegal edge " + a + " " + b + "!");
			} else {
				this.a = b; this.b = a;
			}
		}

		public boolean equals(Object o) {
			Edge e = (Edge)o;
			return a == e.a && b == e.b;
		}

		public int compareTo(Object o) {
			Edge e = (Edge)o;
			int diff = a - e.a;
			if (diff == 0)
				diff = b - e.b;
			return diff;
		}

		public String toString() {
			return "" + a + " " + b;
		}

		public void addLineTo(List out) {
			out.add(getVertex(a).getPoint3f());
			out.add(getVertex(b).getPoint3f());
		}
	}

	/*
	 * Get the edges
	 *
	 * 1. for all surface voxels:
	 *    1.1 iterate over its direct neighbours
	 *        1.1.1 if the voxel and the neighbour have different
	 *              vertices, add an edge
	 */
	Map edges;

	void getEdges() {
		edges = new TreeMap();
		Iterator iter = surfaceVoxels.keySet().iterator();
		while (iter.hasNext()) {
			SurfaceVoxel voxel = (SurfaceVoxel)iter.next();
			Iterator iter2 = voxel.iterator();
			while (iter2.hasNext()) {
				SurfaceVoxel voxel2 =
					(SurfaceVoxel)iter2.next();
				if (voxel.vertexIndex != voxel2.vertexIndex) {
					Edge e = new Edge(voxel.vertexIndex,
							voxel2.vertexIndex);
					edges.put(e, e);
				}
			}
		}
	}

	static int lineMeshCount = 0;

	void showEdges(int color) {
		getUniverse();

		List lineMesh = new ArrayList();
		Iterator iter = edges.keySet().iterator();
		while(iter.hasNext()) {
			Edge e = (Edge)iter.next();
			e.addLineTo(lineMesh);
		}
		universe.addLineMesh(lineMesh, makeColor(color),
				"line" + lineMeshCount++, false);
	}


	class Triangle implements Comparable {
		int a, b, c;

		public Triangle(int a, int b, int c) {
			if (a > b) {
				int d = a; a = b; b = d;
			}
			if (b > c) {
				int d = b; b = c; c = d;
			}
			if (a > b) {
				int d = a; a = b; b = d;
			}
			this.a = a; this.b = b; this.c = c;
		}

		public boolean equals(Object other) {
			Triangle o = (Triangle)other;
			return a == o.a && b == o.b && c == o.c;
		}

		public int compareTo(Object other) {
			Triangle o = (Triangle)other;
			int diff = a - o.a;
			if (diff == 0) {
				diff = b - o.b;
				if (diff == 0)
					diff = c - o.c;
			}
			return diff;
		}

		public String toString() {
			return "" + a + " " + b + " " + c;
		}

		public void addTriangleTo(List out) {
			out.add(getVertex(a).getPoint3f());
			out.add(getVertex(b).getPoint3f());
			out.add(getVertex(c).getPoint3f());
			out.add(getVertex(c).getPoint3f());
			out.add(getVertex(b).getPoint3f());
			out.add(getVertex(a).getPoint3f());
		}
	}

	Vector connected;
	Map triangles;

	void getTriangles() {
		connected = new Vector();
		connected.setSize(vertices.size());
		Iterator iter = edges.keySet().iterator();
		while (iter.hasNext()) {
			Edge e = (Edge)iter.next();
			IntArray a = (IntArray)connected.get(e.a);
			if (a == null) {
				a = new IntArray();
				connected.set(e.a, a);
			}
			if (!a.contains(e.b))
				a.add(e.b);
			a = (IntArray)connected.get(e.b);
			if (a == null) {
				a = new IntArray();
				connected.set(e.b, a);
			}
			if (!a.contains(e.a))
				a.add(e.a);
		}

		triangles = new TreeMap();
		iter = edges.keySet().iterator();
		while (iter.hasNext()) {
			Edge e = (Edge)iter.next();
			
			IntArray a = (IntArray)connected.get(e.a);
			IntArray b = (IntArray)connected.get(e.b);
			for (int j = 0; j < a.size(); j++) {
				int c = a.get(j);
				if (b.contains(c)) {
					Triangle t = new Triangle(e.a, e.b, c);
					triangles.put(t, t);
				}
			}
		}
	}

	int triangleCount = 0;

	void showTriangles(int color) {
		getUniverse();

		List mesh = new ArrayList();
		Iterator iter = triangles.keySet().iterator();
		while(iter.hasNext()) {
			Triangle t = (Triangle)iter.next();
			t.addTriangleTo(mesh);
		}
		universe.addMesh(mesh, makeColor(color),
				"mesh" + triangleCount++, 0);
	}

	Image3DUniverse universe;

	void getUniverse() {
		if (universe != null)
			return;
		universe = new Image3DUniverse();
		universe.show();
	}

	int pointCount = 0;

	void showPoints(Iterator iter, int color) {
		getUniverse();

		List mesh = new ArrayList();
		while(iter.hasNext()) {
			Object next = iter.next();
			Point3d p = null;
			if (next instanceof SurfaceVoxel)
				p = ((SurfaceVoxel)next).getPoint3d();
			else
				p = (Point3d)next;

			float radius = (ii.w + ii.h + ii.d) / 6;
			Point3f p1 = new Point3f((float)p.x, (float)p.y,
					(float)p.z - radius);
			Point3f p2 = new Point3f((float)p.x - 0.5f * radius,
					(float)p.y + 0.8f * radius,
					(float)p.z + 0.3f * radius);
			Point3f p3 = new Point3f((float)p.x - 0.5f * radius,
					(float)p.y - 0.8f * radius,
					(float)p.z + 0.3f * radius);
			Point3f p4 = new Point3f((float)p.x + 0.8f * radius,
					(float)p.y,
					(float)p.z + 0.3f * radius);
			mesh.add(p1);
			mesh.add(p2);
			mesh.add(p3);

			mesh.add(p4);
			mesh.add(p2);
			mesh.add(p3);

			mesh.add(p1);
			mesh.add(p2);
			mesh.add(p4);

			mesh.add(p1);
			mesh.add(p3);
			mesh.add(p4);
		}
		universe.addMesh(mesh, makeColor(color),
				"point" + pointCount++, 0);
	}

	public void showSurfaceVoxels() {
		getUniverse();
		showPoints(surfaceVoxels.values().iterator(), 0xffffffff);
	}

	/* here comes the meat */

	boolean verbose = true;
	int lowerThreshold, upperThreshold;

	boolean isWithinThresholds(int i, int j, int k) {
		int v = ii.getNoInterpol(i, j, k);
		return (v < lowerThreshold || v > upperThreshold);
	}

	boolean testVoxel(int i, int j, int k) {
		if (!isWithinThresholds(i, j, k))
			return false;
		for (int z = -1; z <= +1; z++)
			for (int y = -1; y <= +1; y++)
				for (int x = -1; x <= +1; x++)
					if (!isWithinThresholds(i + x, j + y,
								k + z))
						return true;
		return false;
	}

	void saveWRL(String filename) {
		try {
			FileWriter w = new FileWriter(new File(filename));
			PrintWriter pw = new PrintWriter(new BufferedWriter(w));
			pw.println("#VRML V2.0 utf8\n"
					+ "\n"
					+ "Transform { children [ Shape {\n"
					+ "   appearance Appearance { material "
						+ "Material { diffuseColor "
						+ "0.57 0.57 0.57 } }\n"
					+ "   geometry IndexedFaceSet { "
						+ "coord Coordinate { point [");
			Iterator iter = vertices.iterator();
			while (iter.hasNext()) {
				SurfaceVoxel v = (SurfaceVoxel)iter.next();
				pw.println("    " + v.getPoint3d());
			}
			pw.println("  ] } coordIndex [");
			iter = triangles.keySet().iterator();
			while (iter.hasNext()) {
				Triangle t = (Triangle)iter.next();
				pw.println("    " + t.a + ", " + t.b
						+ ", " + t.c + ", -1,");
				pw.println("    " + t.c + ", " + t.b
						+ ", " + t.a + ", -1,");
			}
			if (normals != null) {
				pw.println("  ],\n"
						+ "  normal Normal {\n"
						+ "  vector [\n");
				for (int i = 0; i < normals.length; i++)
					pw.println("    " + normals[i]);
				pw.println("  ] } normalIndex [");
				iter = triangles.keySet().iterator();
				while (iter.hasNext()) {
					Triangle t = (Triangle)iter.next();
					pw.println("    " + t.a + ", " + t.b
							+ ", " + t.c + ", -1,");
					pw.println("    " + t.c + ", " + t.b
							+ ", " + t.a + ", -1,");
				}
			}
			pw.println("  ]\n"
					+ "  } }\n"
					+ "] }");
			pw.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	void saveAsText(String filename) {
		try {
			DecimalFormat f = new DecimalFormat("0.###");
			FileWriter w = new FileWriter(new File(filename));
			PrintWriter pw = new PrintWriter(new BufferedWriter(w));
			pw.println("# vertices");
			pw.println("" + vertices.size());
			for (int i = 0; i < vertices.size(); i++) {
				SurfaceVoxel v = (SurfaceVoxel)vertices.get(i);
				pw.println(v.getPoint3d());
			}
			pw.println("# normals");
			pw.println("" + normals.length);
			for (int i = 0; i < normals.length; i++) {
				Point3f v = normals[i];
				pw.println(f.format(v.x) + " "
						+ f.format(v.y) + " "
						+ f.format(v.z));
			}
			pw.println("# triangles");
			pw.println("" + triangles.size());
			Iterator iter = triangles.keySet().iterator();
			while (iter.hasNext())
				pw.println(iter.next());
			pw.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}

