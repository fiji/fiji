package octree;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Shape3D;

public class ShapeGroup implements Comparable {

	float pos;
	Shape3D shape;
	BranchGroup group;
	BranchGroup child;

	/* This is only used for creating the sorting indices for
	 * the parent OrderedGroup. Not very nice...
	 */
	int indexInParent;

	public ShapeGroup() {
		group = new BranchGroup();
		group.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		child = new BranchGroup();
		child.setCapability(BranchGroup.ALLOW_DETACH);
	}

	public void prepareForAxis(float pos) {
		this.pos = pos;
	}

	public void show(CubeData cdata, int index) {
		shape = new Shape3D(createGeometry(cdata, index),
			createAppearance(cdata, index));
		child.addChild(shape);
		group.addChild(child);
	}

	public void hide() {
		child.detach();
		child.removeAllChildren();
		shape = null;
	}

	private static GeometryArray createGeometry(CubeData cdata, int index) {
		GeometryArray arr = GeometryCreator.instance().getQuad(cdata, index);
		return arr;
	}

	private static Appearance createAppearance(CubeData cdata, int index) {
		return AppearanceCreator.instance().getAppearance(cdata, index);
	}

	public int compareTo(Object o) {
		ShapeGroup sg = (ShapeGroup)o;
		if(pos < sg.pos) return -1;
		if(pos > sg.pos) return +1;
		return 0;
	}

	/*
	 * Used in displayInitial.
	 */
	public ShapeGroup duplicate() {
		ShapeGroup ret = new ShapeGroup();
		if(shape != null) {
			ret.shape = new Shape3D(
				shape.getGeometry(),
				shape.getAppearance());
			ret.group.addChild(ret.shape);
		}
		ret.pos = pos;
		return ret;
	}
}

