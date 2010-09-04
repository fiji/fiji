package fiji.plugin.nperry.visualization;

import ij3d.ContentNode;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Switch;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3d;

import customnode.CustomTriangleMesh;
import customnode.MeshMaker;

public class SpotGroupContentInstant <K extends Comparable<K>> extends ContentNode {

	private static final int DEFAULT_MERIDIAN_NUMBER = 12;
	private static final int DEFAULT_PARALLEL_NUMBER = 12;
	
	private Map<K, Point4f>  centers;
	private Map<K, Color4f> colors;
	
	private HashMap<Integer, CustomTriangleMesh> meshes;
;
	private Switch ballSwitch;
	private BitSet switchMask;
	private HashMap<Integer, K> indices;


	public SpotGroupContentInstant(Map<K, Point4f>  centers, Map<K, Color4f> colors) {
		this.centers = centers;
		this.colors = colors;
		this.ballSwitch = new Switch();
		this.switchMask = new BitSet();
		makeMeshes();
	}

	/*
	 * PRIVATE METHODS
	 */
	
	@SuppressWarnings("unchecked")
	private void makeMeshes() {
		List<Point3f> points;
		CustomTriangleMesh node;
		Color4f color;
		Point4f center;
		meshes = new HashMap<Integer, CustomTriangleMesh>(centers.size());
		indices = new HashMap<Integer, K>(centers.size());
		ballSwitch.removeAllChildren();
		int index = 0;
		for (K key : centers.keySet()) {
			center = centers.get(key);
			color = colors.get(key);
			// Create mesh
			points = MeshMaker.createSphere(center.x, center.y, center.z, center.w, DEFAULT_MERIDIAN_NUMBER, DEFAULT_PARALLEL_NUMBER);
			node = new CustomTriangleMesh(points, new Color3f(color.x, color.y, color.z), color.w);
			// Add it to the switch. We keep an index of the position it is added to for later retrieval by key
			meshes.put(index, node);
			ballSwitch.addChild(node); // at index
			indices.put(index, key); // store index for key
			index++;
		}
		switchMask = new BitSet(centers.size());
		switchMask.set(0, switchMask.size()-1, true);
		ballSwitch.setChildMask(switchMask);
		addChild(ballSwitch);
	}
	
	
	/*
	 * CONTENTNODE METHODS
	 */

	@Override
	public void colorUpdated(Color3f color) { 
		for(CustomTriangleMesh mesh : meshes.values()) 
			mesh.setColor(color);
	}
	
	@Override
	public void transparencyUpdated(float transparency) {
		for(CustomTriangleMesh mesh : meshes.values()) 
			mesh.setTransparency(transparency);
	}
	
	@Override
	public void shadeUpdated(boolean shaded) {
		for (CustomTriangleMesh mesh : meshes.values())
			 mesh.setShaded(shaded);
		
	}
	
	@Override
	public void getCenter(Tuple3d center) {
		double x = 0, y = 0, z = 0;
		for (Point4f c : centers.values()) {
			x += c.x;
			y += c.y;
			z += c.z;
		}
		x /= centers.size();
		y /= centers.size();
		z /= centers.size();		
	}


	@Override
	public void getMax(Tuple3d max) {
		float xmax = Float.NEGATIVE_INFINITY;
		float ymax = Float.NEGATIVE_INFINITY;
		float zmax = Float.NEGATIVE_INFINITY;
		for (Point4f center : centers.values()) {
			if (xmax < center.x - center.w)
				xmax =  center.x - center.w;
			if (ymax < center.y - center.w)
				ymax =  center.y - center.w;
			if (zmax < center.z + center.w)
				zmax =  center.z + center.w;
		}
		max.x = xmax;
		max.y = ymax;
		max.z = zmax;
	}

	@Override
	public void getMin(Tuple3d min) {
		float xmin = Float.POSITIVE_INFINITY;
		float ymin = Float.POSITIVE_INFINITY;
		float zmin = Float.POSITIVE_INFINITY;
		for (Point4f center : centers.values()) {
			if (xmin > center.x - center.w)
				xmin =  center.x - center.w;
			if (ymin > center.y - center.w)
				ymin =  center.y - center.w;
			if (zmin > center.z - center.w)
				zmin =  center.z - center.w;
		}
		min.x = xmin;
		min.y = ymin;
		min.z = zmin;
	}


	@Override
	public float getVolume() {
		float volume = 0;
		for (CustomTriangleMesh mesh : meshes.values()) 
			volume += mesh.getVolume();
		return volume;
	}


	@Override
	public void channelsUpdated(boolean[] channels) {}
	@Override
	public void thresholdUpdated(int threshold) {}
	@Override
	public void eyePtChanged(View view) {}
	




	
	
}
