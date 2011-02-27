package fiji.plugin.trackmate.visualization;

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

public class SpotGroupNode<K> extends ContentNode {

	private static final int DEFAULT_MERIDIAN_NUMBER = 12;
	private static final int DEFAULT_PARALLEL_NUMBER = 12;
	
	/**
	 * Hold the center and radius position of all spots.
	 */
	protected Map<K, Point4f>  centers;
	/**
	 * Hold the color and transparency of all spots.
	 */
	protected Map<K, Color4f> colors;
	/**
	 * Hold the mesh of each spot.
	 */
	protected HashMap<K, CustomTriangleMesh> meshes;

	/**
	 * Switch used for display. Is the only child of this {@link ContentNode}.
	 */
	protected Switch spotSwitch;
	/**
	 * Boolean set that controls the visibility of each spot.
	 */
	protected BitSet switchMask;
	/**
	 * Map that links the spot keys to their indices in the Switch. 
	 * @see #spotSwitch
	 */
	protected HashMap<K, Integer> indices;


	/**
	 * Create a new {@link SpotGroupNode} with spots at position and with color given in argument.
	 * <p>
	 * The positions are given by a {@link Point4f} map. The <code>x</code>,  <code>y</code>,  <code>z</code>
	 * are used to specify the spot center, and the <code>w</code> field its radius.
	 * Colors are specified by a {@link Color4f} map. The <code>x</code>,  <code>y</code>,  <code>z</code>
	 * are used to specify the R, G and B component, and the <code>w</code> field the spot transparency.
	 * <p>
	 * The arguments are copied on creation, ensuring that are unmodified by this class, and vice-versa. 
	 * @param centers
	 * @param colors
	 */
	public SpotGroupNode(final Map<K, Point4f>  centers, final Map<K, Color4f> colors) {
		this.centers = new HashMap<K, Point4f>(centers);
		this.colors = new HashMap<K, Color4f>(colors);
		this.spotSwitch = new Switch(Switch.CHILD_MASK);
		spotSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		spotSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		spotSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		this.switchMask = new BitSet();
		makeMeshes();
	}

	/**
	 * Create a new {@link SpotGroupNode} with spots at position and with color given in argument.
	 * <p>
	 * The positions are given by a {@link Point4f} map. The <code>x</code>,  <code>y</code>,  <code>z</code>
	 * are used to specify the spot center, and the <code>w</code> field its radius.
	 * The same color is used for all the spots, with a transparency of 0.
	 * <p>
	 * The arguments are copied on creation, ensuring that are unmodified by this class, and vice-versa. 
	 * @param centers
	 * @param colors
	 */
	public SpotGroupNode(HashMap<K, Point4f> centers, Color3f color) {
		this.centers = new HashMap<K, Point4f>(centers);
		this.colors = new HashMap<K, Color4f>(centers.size());
		for(K key : centers.keySet()) {
			colors.put(key, new Color4f(color.x, color.y, color.z, 0));
		}
		this.spotSwitch = new Switch(Switch.CHILD_MASK);
		spotSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		spotSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		spotSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		this.switchMask = new BitSet();
		makeMeshes();
	}
	/*
	 * PRIVATE METHODS
	 */
	

	/**
	 * Re-create the {@link #meshes} field with spot meshes, from the fields {@link #centers} and {@link #colors}.
	 * <p>
	 * This resets the {@link #spotSwitch} and the {@link #switchMask} fields with new values.
	 */
	protected void makeMeshes() {
		List<Point3f> points;
		CustomTriangleMesh node;
		Color4f color;
		Point4f center;
		meshes = new HashMap<K, CustomTriangleMesh>(centers.size());
		indices = new HashMap<K, Integer>(centers.size());
		spotSwitch.removeAllChildren();
		int index = 0;
		for (K key : centers.keySet()) {
			center = centers.get(key);
			color = colors.get(key);
			// Create mesh
			points = MeshMaker.createSphere(center.x, center.y, center.z, center.w, DEFAULT_MERIDIAN_NUMBER, DEFAULT_PARALLEL_NUMBER);
			node = new CustomTriangleMesh(points, new Color3f(color.x, color.y, color.z), color.w);
			// Add it to the switch. We keep an index of the position it is added to for later retrieval by key
			meshes.put(key, node);
			spotSwitch.addChild(node); // at index
			indices.put(key, index); // store index for key
			index++;
		}
		switchMask = new BitSet(centers.size());
		switchMask.set(0, centers.size(), true);
		spotSwitch.setChildMask(switchMask);
		removeAllChildren();
		addChild(spotSwitch);
	}
	
	/*
	 * SINGLE ELEMENT GETTERS/SETTERS
	 */
	
	/**
	 * Set the visibility of all spots given in argument to <code>true<code>, all the others
	 * are set to invisible.
	 */
	public void setVisible(Iterable<K> toShow) {
		switchMask = new BitSet(meshes.size());
		Integer index;
		for(K key : toShow) {
			index = indices.get(key);
			if (null == index)
				continue;
			switchMask.set(index);
		}
		spotSwitch.setChildMask(switchMask);
	}

	
	/**
	 * Set the visibility of all spots.
	 */
	public void setVisible(boolean visible) {
		switchMask.set(0, switchMask.size()-1, visible);
		spotSwitch.setChildMask(switchMask);
	}
	
	/**
	 * Set the visibility of the spot <code>key</code>.
	 */
	public void setVisible(final K key, final boolean visible) {
		Integer index = indices.get(key);
		if (null == index) 
			return;
		switchMask.set(index, visible);
		spotSwitch.setChildMask(switchMask);
	}
	
	/**
	 * Set the color of all spots.
	 */
	public void setColor(Color3f color) {
		for (CustomTriangleMesh mesh : meshes.values()) 
			mesh.setColor(color);
	}
	
	/**
	 * Set the color of the spot <code>key</code>. Its transparency is unchanged.
	 */
	public void setColor(final K key, final Color3f color) {
		CustomTriangleMesh mesh = meshes.get(key);
		if (null == mesh)
			return;
		mesh.setColor(color);
		colors.get(key).x = color.x;
		colors.get(key).y = color.y;
		colors.get(key).z = color.z;
	}
	
	public Color4f getColor(final K key) {
		return colors.get(key);
	}
	
	public Color3f getColor3f(final K key) {
		return new Color3f(colors.get(key).x, colors.get(key).y, colors.get(key).z);
	}
	
	/**
	 * Set the color of the spot <code>key</code>. Its transparency set by the <code>w</code>
	 * field of the {@link Color4f} argument.
	 */
	public void setColor(final K key, final Color4f color) {
		CustomTriangleMesh mesh = meshes.get(key);
		if (null == mesh)
			return;
		mesh.setColor(new Color3f(color.x, color.y, color.z));
		mesh.setTransparency(color.w);
		colors.put(key, new Color4f(color));
	}
	
	/**
	 * Set the transparency of the spot <code>key</code>. Its color is unchanged.
	 */
	public void setTransparency(final K key, final float transparency) {
		CustomTriangleMesh mesh = meshes.get(key);
		if (null == mesh)
			return;
		mesh.setTransparency(transparency);
		colors.get(key).w = transparency;
	}
	
	/**
	 * Move the spot <code>key</code> center to the position given by the {@link Point3f}.
	 * Its radius is unchanged.
	 */
	public void setCenter(final K key, final Point3f center) {
		CustomTriangleMesh mesh = meshes.get(key);
		if (null == mesh)
			return;
		float r = centers.get(key).w;
		mesh.setMesh(MeshMaker.createSphere(center.x, center.y, center.z, r, DEFAULT_MERIDIAN_NUMBER, DEFAULT_PARALLEL_NUMBER));
		centers.get(key).x = center.x;
		centers.get(key).y = center.y;
		centers.get(key).z = center.z;
	}
	
	/**
	 * Move the spot <code>key</code> center to the position given by the 
	 * <code>x</code>,  <code>y</code>,  <code>z</code> fields of the {@link Point4f}.
	 * Its radius is set by the <code>w</code> field.
	 */
	public void setCenter(final K key, final Point4f center) {
		CustomTriangleMesh mesh = meshes.get(key);
		if (null == mesh)
			return;
		mesh.setMesh(MeshMaker.createSphere(center.x, center.y, center.z, center.w, DEFAULT_MERIDIAN_NUMBER, DEFAULT_PARALLEL_NUMBER));
		centers.put(key, new Point4f(center));
	}
	
	/**
	 * Change the radius of the spot <code>key</code>. Its position is unchanged.
	 */
	public void setRadius(final K key, final float radius) {
		CustomTriangleMesh mesh = meshes.get(key);
		if (null == mesh)
			return;
		Point4f center = centers.get(key);
		mesh.setMesh(MeshMaker.createSphere(center.x, center.y, center.z, radius, DEFAULT_MERIDIAN_NUMBER, DEFAULT_PARALLEL_NUMBER));
		center.w = radius;
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
			if (xmax < center.x + center.w)
				xmax =  center.x + center.w;
			if (ymax < center.y + center.w)
				ymax =  center.y + center.w;
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

	@Override
	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {}

	@Override
	public void swapDisplayedData(String path, String name) {}

	@Override
	public void restoreDisplayedData(String path, String name) {}


}
