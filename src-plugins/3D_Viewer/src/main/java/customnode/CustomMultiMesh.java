package customnode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.vecmath.Tuple3d;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.View;

public class CustomMultiMesh extends CustomMeshNode {

	private List<CustomMesh> customMeshes;

	public CustomMultiMesh() {
		setCapabilities(this);
		customMeshes = new ArrayList<CustomMesh>();
		calculateMinMaxCenterPoint();
	}

	public CustomMultiMesh(CustomMesh customMesh) {
		setCapabilities(this);
		customMeshes = new ArrayList<CustomMesh>();
		customMeshes.add(customMesh);
		calculateMinMaxCenterPoint();
		BranchGroup bg = new BranchGroup();
		setCapabilities(bg);

		bg.addChild(customMesh);
		addChild(bg);
	}

	public CustomMultiMesh(List<CustomMesh> meshes) {
		customMeshes = meshes;
		calculateMinMaxCenterPoint();
		for(CustomMesh m : customMeshes) {
			BranchGroup bg = new BranchGroup();
			setCapabilities(bg);
			bg.addChild(m);
			addChild(bg);
		}
	}

	private static void setCapabilities(BranchGroup bg) {
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	}

	public void remove(int i) {
		customMeshes.remove(i);
		calculateMinMaxCenterPoint();
		BranchGroup bg = (BranchGroup)getChild(i);
		removeChild(i);
		bg.removeAllChildren();
	}

	public void remove(CustomMesh mesh) {
		customMeshes.remove(mesh);
		calculateMinMaxCenterPoint();
		BranchGroup bg = (BranchGroup)mesh.getParent();
		removeChild(bg);
		bg.removeAllChildren();
	}

	public void add(CustomMesh mesh) {
		customMeshes.add(mesh);
		calculateMinMaxCenterPoint();
		BranchGroup bg = new BranchGroup();
		setCapabilities(bg);
		bg.addChild(mesh);
		addChild(bg);
	}

	public int size() {
		return customMeshes.size();
	}

	public CustomMesh getMesh(int i) {
		return customMeshes.get(i);
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
	public void colorUpdated(Color3f color) {
		for(CustomMesh mesh : customMeshes)
			mesh.setColor(color);
	}

	@Override
	public void eyePtChanged(View view) {
		// do nothing
	}

	@Override
	public float getVolume() {
		float vol = 0f;
		for(CustomMesh mesh : customMeshes)
			vol += mesh.getVolume();
		return vol;
	}

	@Override
	public void shadeUpdated(boolean shaded) {
		for(CustomMesh mesh : customMeshes)
			mesh.setShaded(shaded);
	}

	@Override
	public void thresholdUpdated(int threshold) {
		// do nothing
	}

	@Override
	public void transparencyUpdated(float transparency) {
		for(CustomMesh mesh : customMeshes)
			mesh.setTransparency(transparency);
	}

	private static void adjustMinMax(Point3f p, Point3f min, Point3f max) {
		if(p.x < min.x) min.x = p.x;
		if(p.y < min.y) min.y = p.y;
		if(p.z < min.z) min.z = p.z;

		if(p.x > max.x) max.x = p.x;
		if(p.y > max.y) max.y = p.y;
		if(p.z > max.z) max.z = p.z;
	}

	private void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		if(customMeshes.isEmpty())
			return;

		customMeshes.get(0).calculateMinMaxCenterPoint(
				min, max, center);
		Point3f mint = new Point3f();
		Point3f maxt = new Point3f();
		int n = customMeshes.size();
		if(n == 1)
			return;
		for(int i = 1; i < n; i++) {
			CustomMesh mesh = customMeshes.get(i);
			mesh.calculateMinMaxCenterPoint(mint, maxt, center);
			adjustMinMax(mint, min, max);
			adjustMinMax(maxt, min, max);
		}
		center.sub(max, min);
		center.scale(0.5f);
	}

	public void restoreDisplayedData(String path, String name) {
		HashMap<String, CustomMesh> contents = null;
		customMeshes = null;
		try {
			contents = WavefrontLoader.load(path);
			for(int i = 0; i < contents.size(); i++) {
				CustomMesh cm = contents.get(name + "###" + i);
				customMeshes.add(cm);
				cm.update();
			}

		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void swapDisplayedData(String path, String name) {
		HashMap<String, CustomMesh> contents =
			new HashMap<String, CustomMesh>();
		for(int i = 0; i < customMeshes.size(); i++)
			contents.put(name + "###" + i, customMeshes.get(i));

		try {
			WavefrontExporter.save(
				contents,
				path + ".obj");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

