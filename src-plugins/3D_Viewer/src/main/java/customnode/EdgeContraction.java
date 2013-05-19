package customnode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import customnode.FullInfoMesh.Edge;
import customnode.FullInfoMesh.Vertex;

public class EdgeContraction {

	private final boolean LENGTH_ONLY;
	private ArrayList<FullInfoMesh> mesh;
	private SortedSet<CEdge> queue;
	private Map<CEdge, Float> edgeCosts = new HashMap<CEdge, Float>();

	public final void removeUntil(float maxCost) {
		while(!queue.isEmpty()) {
			CEdge e = queue.first();
			if(edgeCosts.get(e) > maxCost)
				break;
			queue.remove(e);
			fuse(e);
		}
	}

	public final int removeNext(int n) {
		int curr = getRemainingVertexCount();
		int goal = curr - n;

		while(curr > goal && !queue.isEmpty()) {
			CEdge e = queue.first();
			queue.remove(e);
			fuse(e);
			curr = getRemainingVertexCount();
		}
		int v = 0;
		for(int i = 0; i < mesh.size(); i++)
			v += mesh.get(i).getVertexCount();

		return v;
	}

	public int getRemainingVertexCount() {
		int v = 0;
		for(int i = 0; i < mesh.size(); i++)
			v += mesh.get(i).getVertexCount();

		return v;
	}

	public final Edge nextToRemove() {
		return queue.first().edge;
	}

	public final int getVertexCount() {
		int v = 0;
		for(FullInfoMesh m : mesh)
			v += m.getVertexCount();
		return v;
	}

	private static final ArrayList<FullInfoMesh> makeList(FullInfoMesh m) {
		ArrayList<FullInfoMesh> l = new ArrayList<FullInfoMesh>();
		l.add(m);
		return l;
	}

	public EdgeContraction(FullInfoMesh mesh) {
		this(mesh, false);
	}

	public EdgeContraction(FullInfoMesh mesh, boolean edgeLengthOnly) {
		this(makeList(mesh), edgeLengthOnly);
	}

	public EdgeContraction(ArrayList<FullInfoMesh> meshes,
					boolean edgeLengthOnly) {

		this.LENGTH_ONLY = edgeLengthOnly;
		queue = new TreeSet<CEdge>(new EdgeComparator());
		mesh = meshes;

		int meshIdx = 0;
		for(FullInfoMesh fim : mesh) {
			for(Edge e : fim.edges.keySet()) {
				CEdge ce = new CEdge(e, meshIdx);
				edgeCosts.put(ce, computeCost(ce));
				queue.add(ce);
			}
			meshIdx++;
		}
	}

	public ArrayList<FullInfoMesh> getMeshes() {
		return mesh;
	}

	protected float computeCost(CEdge ce) {
		float l = getLength(ce);
		if(LENGTH_ONLY)
			return l;

		FullInfoMesh fim = mesh.get(ce.meshIdx);
		Edge e = ce.edge;

		HashSet<Integer> triangles = new HashSet<Integer>();
		triangles.addAll(fim.getVertex(e.p1).triangles);
		triangles.addAll(fim.getVertex(e.p2).triangles);
		for(int i : e.triangles)
			triangles.remove(i);

		float angle = 0;
		Vector3f oldN = new Vector3f();
		Vector3f newN = new Vector3f();
		Point3f midp = getMidpoint(ce);

		for(int fIdx : triangles) {

			int f1 = fim.faces.get(fIdx * 3);
			int f2 = fim.faces.get(fIdx * 3 + 1);
			int f3 = fim.faces.get(fIdx * 3 + 2);

			Point3f v1 = fim.getVertex(f1);
			Point3f v2 = fim.getVertex(f2);
			Point3f v3 = fim.getVertex(f3);

			getNormal(v1, v2, v3, oldN);
			if(f1 == e.p1 || f1 == e.p2)
				getNormal(midp, v2, v3, newN);
			else if(f2 == e.p1 || f2 == e.p2)
				getNormal(v1, midp, v3, newN);
			else if(f3 == e.p1 || f3 == e.p2)
				getNormal(v1, v2, midp, newN);
			oldN.normalize();
			newN.normalize();
			float dAngle = oldN.angle(newN);
			if(!Float.isNaN(dAngle))
				angle += oldN.angle(newN);
		}
		return l * angle;
	}

	private final boolean shouldFuse(CEdge ce) {
		// only allow to fuse if it's a well-behaved mesh region.
		// In particular, don't fuse if is kind of a fold-back,
		// which is recognized by checking the neighbor triangles:
		// if there are more than 4 vertices in common (the vertices
		// of the 2 triangles of e, which have 2 points in common).
		FullInfoMesh fim = mesh.get(ce.meshIdx);
		Edge e = ce.edge;
		Set<Vertex> neighborVertices = new HashSet<Vertex>();
		int n1 = fim.getVertex(e.p1).edges.size() + 1;
		if(n1 < 5)
			return false;
		for(Edge e1 : fim.getVertex(e.p1).edges) {
			neighborVertices.add(fim.getVertex(e1.p1));
			neighborVertices.add(fim.getVertex(e1.p2));
		}
		int n2 = fim.getVertex(e.p2).edges.size() + 1;
		if(n2 < 5)
			return false;
		for(Edge e2 : fim.getVertex(e.p2).edges) {
			neighborVertices.add(fim.getVertex(e2.p1));
			neighborVertices.add(fim.getVertex(e2.p2));
		}
		return neighborVertices.size() == n1 + n2 - 4;
	}

	private final void fuse(CEdge ce) {
		if(!shouldFuse(ce))
			return;

		FullInfoMesh fim = mesh.get(ce.meshIdx);
		Edge e = ce.edge;

		// remove all edges of e.p1 and e.p2 from the queue
		for(Edge ed : fim.getVertex(e.p1).edges)
			queue.remove(new CEdge(ed, ce.meshIdx));
		for(Edge ed : fim.getVertex(e.p2).edges)
			queue.remove(new CEdge(ed, ce.meshIdx));

		Point3f midp = getMidpoint(ce);

		int mIdx = fim.contractEdge(e, midp);

		// re-add the affected edges to the priority queue
		Set<Edge> newEdges = fim.getVertex(mIdx).edges;
		for(Edge edge : newEdges) {
			CEdge cEdge = new CEdge(edge, ce.meshIdx);
			edgeCosts.put(cEdge, computeCost(cEdge));
			queue.add(cEdge);
		}

		// get the neighbor points of midp
		List<Integer> neighbors = new ArrayList<Integer>();
		for(Edge edge : newEdges) {
			if(edge.p1 != mIdx)
				neighbors.add(edge.p1);
			if(edge.p2 != mIdx)
				neighbors.add(edge.p2);
		}

		// collect all the edges of the neighborpoints
		// these are all the edges whose cost must be updated.
		Set<Edge> neighborEdges = new HashSet<Edge>();
		for(int n : neighbors)
			neighborEdges.addAll(fim.getVertex(n).edges);
		neighborEdges.removeAll(newEdges);

		// update costs
		for(Edge ed : neighborEdges)
			queue.remove(new CEdge(ed, ce.meshIdx));

		for(Edge edge : neighborEdges) {
			CEdge cEdge = new CEdge(edge, ce.meshIdx);
			edgeCosts.put(cEdge, computeCost(cEdge));
			queue.add(cEdge);
		}
	}

	private Vector3f v1 = new Vector3f();
	private Vector3f v2 = new Vector3f();
	void getNormal(Point3f p1, Point3f p2, Point3f p3, Vector3f ret) {
		v1.sub(p2, p1);
		v2.sub(p3, p1);
		ret.cross(v1, v2);
	}

	void getMidpoint(CEdge e, Point3f ret) {
		Point3f p1 = mesh.get(e.meshIdx).getVertex(e.edge.p1);
		Point3f p2 = mesh.get(e.meshIdx).getVertex(e.edge.p2);
		ret.add(p1, p2);
		ret.scale(0.5f);
	}

	Point3f getMidpoint(CEdge e) {
		Point3f ret = new Point3f();
		getMidpoint(e, ret);
		return ret;
	}

	float getLength(CEdge e) {
		return mesh.get(e.meshIdx).getVertex(e.edge.p1).distance(
			mesh.get(e.meshIdx).getVertex(e.edge.p2));
	}

	private final class CEdge {
		final Edge edge;
		final int meshIdx;

		CEdge(Edge edge, int mIdx) {
			this.edge = edge;
			this.meshIdx = mIdx;
		}

		@Override
		public boolean equals(Object o) {
			CEdge e = (CEdge)o;
			return meshIdx == e.meshIdx && edge.equals(e.edge);
		}

		@Override
		public int hashCode() {
			long bits = 1L;
			bits = 31L * bits + edge.p1;
			bits = 31L * bits + edge.p2;
			bits = 31L * bits + meshIdx;
			return (int) (bits ^ (bits >> 32));
		}
	}

	private final class EdgeComparator implements Comparator<CEdge> {
		private Point3f mp1 = new Point3f();
		private Point3f mp2 = new Point3f();

		@Override
		public int compare(CEdge e1, CEdge e2) {
			if(e1.equals(e2))
				return 0;
			float l1 = edgeCosts.get(e1);
			float l2 = edgeCosts.get(e2);
			if(l1 < l2) return -1;
			if(l2 < l1) return 1;

			if(e1.meshIdx < e2.meshIdx) return -1;
			if(e1.meshIdx > e2.meshIdx) return +1;

			getMidpoint(e1, mp1);
			getMidpoint(e2, mp2);

			if(mp1.z < mp2.z) return -1;
			if(mp1.z > mp2.z) return +1;
			if(mp1.y < mp2.y) return -1;
			if(mp1.y > mp2.y) return +1;
			if(mp1.x < mp2.x) return -1;
			if(mp1.x > mp2.x) return +1;

			return 0;
		}
	}
}

