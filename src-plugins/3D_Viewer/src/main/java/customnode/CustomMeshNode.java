package customnode;

import javax.media.j3d.View;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3d;
import javax.vecmath.Color3f;

import ij3d.Content;
import ij3d.ContentNode;

public class CustomMeshNode extends ContentNode {

	private CustomMesh mesh;

	protected Point3f min, max, center;
	protected CustomMeshNode() {}

	public CustomMeshNode(CustomMesh mesh) {
		this.mesh = mesh;
		calculateMinMaxCenterPoint();
		addChild(mesh);
	}

	public CustomMesh getMesh() {
		return mesh;
	}

	@Override
	public void getMin(Tuple3d min) {
		min.set(this.min);
	}

	@Override
	public void getMax(Tuple3d max) {
		max.set(this.max);
	}

	@Override
	public void getCenter(Tuple3d center) {
		center.set(this.center);
	}

	@Override
	public void channelsUpdated(boolean[] channels) {
		// do nothing
	}

	@Override
	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {
		// do nothing
	}

	@Override
	public void colorUpdated(Color3f color) {
		mesh.setColor(color);
	}

	@Override
	public void eyePtChanged(View view) {
		// do nothing
	}

	@Override
	public float getVolume() {
		return mesh.getVolume();
	}

	@Override
	public void shadeUpdated(boolean shaded) {
		mesh.setShaded(shaded);
	}

	@Override
	public void thresholdUpdated(int threshold) {
		// do nothing
	}

	@Override
	public void transparencyUpdated(float transparency) {
		mesh.setTransparency(transparency);
	}

	private void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		mesh.calculateMinMaxCenterPoint(min, max, center);
	}

	@Override
	public void restoreDisplayedData(String path, String name) {
		mesh.restoreDisplayedData(path, name);
	}

	@Override
	public void swapDisplayedData(String path, String name) {
		mesh.swapDisplayedData(path, name);
	}

	@Override
	public void clearDisplayedData() {
		mesh.clearDisplayedData();
	}
}
