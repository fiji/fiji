package customnode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class FullInfoMesh {

	ArrayList<Integer> faces;
	ArrayList<Vertex> vertices;
	HashMap<Point3f, Integer> vertexToIndex;
	HashMap<Edge, Edge> edges;
	HashSet<Triangle> triangles;

	public FullInfoMesh() {
		faces = new ArrayList<Integer>();
		vertices = new ArrayList<Vertex>();
		vertexToIndex = new HashMap<Point3f, Integer>();
		triangles = new HashSet<Triangle>();
		edges = new HashMap<Edge, Edge>();
	}

	public FullInfoMesh(List<Point3f> mesh) {
		this();
		for(int i = 0; i < mesh.size(); i += 3) {
			int f1 = addVertex(mesh.get(i));
			int f2 = addVertex(mesh.get(i + 1));
			int f3 = addVertex(mesh.get(i + 2));

			addFace(f1, f2, f3);
		}
	}

	public List<Point3f> getMesh() {
		List<Point3f> ret = new ArrayList<Point3f>();
		for(int i = 0; i < faces.size(); i++) {
			int f = getFace(i);
			if(f != -1)
				ret.add(new Point3f(getVertex(f)));
		}
		return ret;
	}

	public Set<Point3f> getVertices() {
		return vertexToIndex.keySet();
	}

	public void moveVertex(int vIdx, Vector3f displacement) {
		Point3f p = vertices.get(vIdx);
		vertexToIndex.remove(p);
		p.add(displacement);
		vertexToIndex.put(p, vIdx);

	}

	public int getIndex(Point3f v) {
		if(vertexToIndex.containsKey(v))
			return vertexToIndex.get(v);
		return -1;
	}

	public int getVertexCount() {
		return vertexToIndex.size();
	}

	public Vertex getVertex(int i) {
		return vertices.get(i);
	}

	public int getFaceCount() {
		return faces.size();
	}

	public int getFace(int i) {
		return faces.get(i);
	}

	public int addVertex(Point3f p) {
		if(vertexToIndex.containsKey(p))
			return vertexToIndex.get(p);

		Vertex v = new Vertex(p);
		vertices.add(v);
		int idx = vertices.size() - 1;
		vertexToIndex.put(v, idx);
		return idx;
	}

	public void removeVertex(Point3f p) {
		removeVertex(vertexToIndex.get(p));
	}

	public void removeVertex(int vIdx) {
		Vertex v = getVertex(vIdx);
		ArrayList<Integer> toRemove =
			new ArrayList<Integer>(v.triangles);
		for(int f : toRemove)
			removeFace(f);

		v = vertices.get(vIdx);
	}

	public void removeFace(int fIdx) {
		int f1 = getFace(3 * fIdx);
		int f2 = getFace(3 * fIdx + 1);
		int f3 = getFace(3 * fIdx + 2);

		if(f1 == -1 && f2 == -1 && f3 == -1)
			return;

		boolean b = triangles.remove(new Triangle(f1, f2, f3));
		assert b;

		faces.set(3 * fIdx,     -1);
		faces.set(3 * fIdx + 1, -1);
		faces.set(3 * fIdx + 2, -1);

		Vertex v1 = getVertex(f1);
		Vertex v2 = getVertex(f2);
		Vertex v3 = getVertex(f3);

		Edge etmp = new Edge(f1, f2);
		etmp = edges.get(etmp);
		etmp.removeTriangle(fIdx);
		if(etmp.nTriangles() == 0) {
			v1.removeEdge(etmp);
			v2.removeEdge(etmp);
			edges.remove(etmp);
		}

		etmp = new Edge(f2, f3);
		etmp = edges.get(etmp);
		etmp.removeTriangle(fIdx);
		if(etmp.nTriangles() == 0) {
			v2.removeEdge(etmp);
			v3.removeEdge(etmp);
			edges.remove(etmp);
		}

		etmp = new Edge(f3, f1);
		etmp = edges.get(etmp);
		etmp.removeTriangle(fIdx);
		if(etmp.nTriangles() == 0) {
			v3.removeEdge(etmp);
			v1.removeEdge(etmp);
			edges.remove(etmp);
		}

		v1.removeTriangle(fIdx);
		v2.removeTriangle(fIdx);
		v3.removeTriangle(fIdx);

		if(v1.triangles.size() == 0) {
			vertexToIndex.remove(v1);
			vertices.set(f1, null);
		}
		if(v2.triangles.size() == 0) {
			vertexToIndex.remove(v2);
			vertices.set(f2, null);
		}
		if(v3.triangles.size() == 0) {
			vertexToIndex.remove(v3);
			vertices.set(f3, null);
		}
	}

	public void addFace(int f1, int f2, int f3) {
		Triangle tri = new Triangle(f1, f2, f3);
		if(triangles.contains(tri))
			return;

		triangles.add(tri);

		Vertex v1 = getVertex(f1);
		Vertex v2 = getVertex(f2);
		Vertex v3 = getVertex(f3);

		Edge e1 = new Edge(f2, f3);
		Edge e2 = new Edge(f3, f1);
		Edge e3 = new Edge(f1, f2);

		Edge etmp = edges.get(e1);
		if(etmp != null)
			e1 = etmp;
		else
			edges.put(e1, e1);

		etmp = edges.get(e2);
		if(etmp != null)
			e2 = etmp;
		else
			edges.put(e2, e2);

		etmp = edges.get(e3);
		if(etmp != null)
			e3 = etmp;
		else
			edges.put(e3, e3);


		v1.addEdges(e2, e3);
		v2.addEdges(e1, e3);
		v3.addEdges(e1, e2);

		int fIdx = faces.size() / 3;

		faces.add(f1);
		faces.add(f2);
		faces.add(f3);

		e1.addTriangle(fIdx);
		e2.addTriangle(fIdx);
		e3.addTriangle(fIdx);

		v1.addTriangle(fIdx);
		v2.addTriangle(fIdx);
		v3.addTriangle(fIdx);
	}

	private Vector3f tmpv1 = new Vector3f();
	private Vector3f tmpv2 = new Vector3f();
	public void getFaceNormal(int fIdx, Vector3f ret) {
		int f1 = getFace(3 * fIdx);
		int f2 = getFace(3 * fIdx + 1);
		int f3 = getFace(3 * fIdx + 2);

		Vertex v1 = getVertex(f1);
		Vertex v2 = getVertex(f2);
		Vertex v3 = getVertex(f3);

		tmpv1.sub(v2, v1);
		tmpv2.sub(v3, v1);

		ret.cross(tmpv1, tmpv2);
	}

	public void getVertexNormal(Vertex v, Vector3f ret) {
		ret.set(0, 0, 0);
		Vector3f tn = new Vector3f();
		for(int fIdx : v.triangles) {
			getFaceNormal(fIdx, tn);
			ret.add(tn);
		}
		ret.normalize();
	}

	public void getVertexNormal(int vIdx, Vector3f ret) {
		Vertex v = getVertex(vIdx);
		getVertexNormal(v, ret);
	}

	public int contractEdge(Edge e, Point3f p) {
		if(!edges.containsKey(e))
			throw new IllegalArgumentException("no edge " + e);

		Vertex v1 = getVertex(e.p1);

		HashSet<Integer> remainingTri = new HashSet<Integer>();
		remainingTri.addAll(v1.triangles);
		remainingTri.removeAll(e.triangles);

		// need to store this because it's destroyed
		// in removeVertex()
		ArrayList<Integer> remainingFaces =
			new ArrayList<Integer>();
		for(int fIdx : remainingTri) {
			remainingFaces.add(getFace(3 * fIdx));
			remainingFaces.add(getFace(3 * fIdx + 1));
			remainingFaces.add(getFace(3 * fIdx + 2));
		}

		removeVertex(e.p1);

		// create the new vertex
		int vIdx = -1;
		if(vertexToIndex.containsKey(p)) {
			vIdx = vertexToIndex.get(p);
		} else {
			Vertex v = new Vertex(p);
			vIdx = e.p1;
			vertices.set(vIdx, v);
			vertexToIndex.put(v, vIdx);
		}

		// add the remaining triangles, where the edge points
		// are replaced by the midpoint
		for(int i = 0; i < remainingFaces.size(); i += 3) {
			int f1 = remainingFaces.get(i);
			int f2 = remainingFaces.get(i + 1);
			int f3 = remainingFaces.get(i + 2);
			if(f1 == e.p1) addFace(vIdx, f2, f3);
			if(f2 == e.p1) addFace(f1, vIdx, f3);
			if(f3 == e.p1) addFace(f1, f2, vIdx);
		}


		Vertex v2 = getVertex(e.p2);
		remainingTri = new HashSet<Integer>();
		remainingTri.addAll(v2.triangles);
		remainingTri.removeAll(e.triangles);

		// need to store this because it's destroyed
		// in removeVertex()
		remainingFaces = new ArrayList<Integer>();
		for(int fIdx : remainingTri) {
			remainingFaces.add(getFace(3 * fIdx));
			remainingFaces.add(getFace(3 * fIdx + 1));
			remainingFaces.add(getFace(3 * fIdx + 2));
		}

		removeVertex(e.p2);

		// add the remaining triangles, where the edge points
		// are replaced by the midpoint
		for(int i = 0; i < remainingFaces.size(); i += 3) {
			int f1 = remainingFaces.get(i);
			int f2 = remainingFaces.get(i + 1);
			int f3 = remainingFaces.get(i + 2);
			if(f1 == e.p2) addFace(vIdx, f2, f3);
			if(f2 == e.p2) addFace(f1, vIdx, f3);
			if(f3 == e.p2) addFace(f1, f2, vIdx);
		}
		return vIdx;
	}

	public ArrayList<ArrayList<Point3f>> getSubmeshes() {
		HashSet<Vertex> open = new HashSet<Vertex>();
		open.addAll(vertices);
		open.remove(null);
		ArrayList<ArrayList<Point3f>> ret =
			new ArrayList<ArrayList<Point3f>>();
		while(!open.isEmpty()) {
			HashSet<Integer> meshSet = new HashSet<Integer>();
			LinkedList<Integer> queue = new LinkedList<Integer>();

			Vertex start = open.iterator().next();
			open.remove(start);
			queue.add(vertexToIndex.get(start));

			while(!queue.isEmpty()) {
				Integer vIdx = queue.poll();
				meshSet.add(vIdx);

				Vertex v = getVertex(vIdx);
				for(Edge e : v.edges) {
					int nIdx = e.p1 == vIdx ? e.p2 : e.p1;
					Vertex n = getVertex(nIdx);
					if(open.contains(n)) {
						open.remove(n);
						queue.offer(nIdx);
					}
				}
			}

			ArrayList<Point3f> tris = new ArrayList<Point3f>();
			for(int f : faces) {
				if(f != -1 && meshSet.contains(f))
					tris.add(getVertex(f));
			}
			ret.add(tris);
		}
		return ret;
	}

	@SuppressWarnings("serial")
	protected static final class Vertex extends Point3f {

		final HashSet<Edge> edges;
		final HashSet<Integer> triangles;

		public Set<Edge> getEdges() {
			return edges;
		}

		public Set<Integer> getTriangles() {
			return triangles;
		}

		private Vertex(Point3f p) {
			super(p);
			edges = new HashSet<Edge>();
			triangles = new HashSet<Integer>();
		}

		private void addEdge(Edge e) {
			edges.add(e);
		}

		private void addEdges(Edge e1, Edge e2) {
			addEdge(e1);
			addEdge(e2);
		}

		private void removeEdge(Edge e) {
			edges.remove(e);
		}

		private void addTriangle(int i) {
			triangles.add(i);
		}

		private void removeTriangle(int i) {
			triangles.remove(i);
		}
	}

	protected static final class Triangle {

		public final int f1, f2, f3;

		private Triangle(int f1, int f2, int f3) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
		}

		@Override
		public int hashCode() {
			return f1 * f2 * f3;
		}

		@Override
		public boolean equals(Object o) {
			Triangle r = (Triangle)o;
			int tmp;
			int tf1 = f1, tf2 = f2, tf3 = f3;
			if(tf2 < tf1) {tmp = tf1; tf1 = tf2; tf2 = tmp;}
			if(tf3 < tf2) {tmp = tf2; tf2 = tf3; tf3 = tmp;}
			if(tf2 < tf1) {tmp = tf1; tf1 = tf2; tf2 = tmp;}

			int rf1 = r.f1, rf2 = r.f2, rf3 = r.f3;
			if(rf2 < rf1) {tmp = rf1; rf1 = rf2; rf2 = tmp;}
			if(rf3 < rf2) {tmp = rf2; rf2 = rf3; rf3 = tmp;}
			if(rf2 < rf1) {tmp = rf1; rf1 = rf2; rf2 = tmp;}

			return tf1 == rf1 && tf2 == rf2 && tf3 == rf3;
		}
	}

	protected static final class Edge {
		public final int p1, p2;
		final HashSet<Integer> triangles;

		Edge(int p1, int p2) {
			this.p1 = p1;
			this.p2 = p2;
			triangles = new HashSet<Integer>();
		}

		@Override
		public boolean equals(Object o) {
			Edge e = (Edge)o;
			return (p1 == e.p1 && p2 == e.p2) ||
				(p1 == e.p2 && p2 == e.p1);
		}

		@Override
		public int hashCode() {
			return p1 * p2;
		}

		private void addTriangle(int i) {
			triangles.add(i);
		}

		private void removeTriangle(int i) {
			triangles.remove(i);
		}

		public int nTriangles() {
			return triangles.size();
		}
	}
}

