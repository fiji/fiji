package isosurface;

import ij.IJ;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.ContentNode;

import java.awt.Color;
import java.util.List;

import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3d;

import marchingcubes.MCTriangulator;
import customnode.CustomTriangleMesh;

public class MeshGroup extends ContentNode {

	private CustomTriangleMesh mesh;
	private Triangulator triangulator = new MCTriangulator();
	private ContentInstant c;
	private Point3f min, max, center;

	public MeshGroup (Content c) {
		this(c.getCurrent());
	}

	public MeshGroup (ContentInstant c) {
		super();
		this.c = c;
		Color3f color = c.getColor();
		List tri = triangulator.getTriangles(c.getImage(),
			c.getThreshold(), c.getChannels(),
			c.getResamplingFactor());
		if(color == null) {
			int value = c.getImage().getProcessor().
				getColorModel().getRGB(c.getThreshold());
			color = new Color3f(new Color(value));
		}
		mesh = new CustomTriangleMesh(tri, color, c.getTransparency());
		calculateMinMaxCenterPoint();
		addChild(mesh);
	}

	public CustomTriangleMesh getMesh() {
		return mesh;
	}

	public void getMin(Tuple3d min) {
		min.set(this.min);
	}

	public void getMax(Tuple3d max) {
		max.set(this.max);
	}

	public void getCenter(Tuple3d center) {
		center.set(this.center);
	}

	public void eyePtChanged(View view) {
		// do nothing
	}

	public void thresholdUpdated(int threshold) {
		if(c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale " +
				"image. Can't change threshold");
			return;
		}
		List tri = triangulator.getTriangles(c.getImage(),
				c.getThreshold(), c.getChannels(),
				c.getResamplingFactor());
		mesh.setMesh(tri);
	}

	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {
		// TODO
	}

	public void channelsUpdated(boolean[] channels) {
		if(c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale " +
				"image. Can't change channels");
			return;
		}
		List tri = triangulator.getTriangles(c.getImage(),
			c.getThreshold(), c.getChannels(),
			c.getResamplingFactor());
		mesh.setMesh(tri);
	}

	public void calculateMinMaxCenterPoint() {
		min = new Point3f(); max = new Point3f();
		center = new Point3f();
		if(mesh != null) {
			mesh.calculateMinMaxCenterPoint(min, max, center);
		}
	}

	public float getVolume() {
		if(mesh == null)
			return -1;
		return mesh.getVolume();
	}

	public void shadeUpdated(boolean shaded) {
		mesh.setShaded(shaded);
	}

	public void colorUpdated(Color3f newColor) {
		if(newColor == null){
			int val = c.getImage().getProcessor().
				getColorModel().getRGB(c.getThreshold());
			newColor = new Color3f(new Color(val));
		}
		mesh.setColor(newColor);
	}

	public void transparencyUpdated(float transparency) {
		mesh.setTransparency(transparency);
	}

	public void restoreDisplayedData(String path, String name) {
		mesh.restoreDisplayedData(path, name);
	}

	public void clearDisplayedData() {
		mesh.clearDisplayedData();
	}

	public void swapDisplayedData(String path, String name) {
		mesh.swapDisplayedData(path, name);
	}
}

