package ij3d;

import ij3d.shapes.CoordinateSystem;
import ij3d.shapes.BoundingBox;
import ij3d.pointlist.PointListPanel;
import ij3d.pointlist.PointListShape;
import ij3d.pointlist.PointListDialog;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.OpenDialog;

import vib.PointList;
import isosurface.MeshGroup;
import voltex.VoltexGroup;
import orthoslice.OrthoGroup;
import surfaceplot.SurfacePlotGroup;

import java.util.BitSet;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class ContentInstant extends BranchGroup implements UniverseListener, ContentConstants {

	// time point for this ContentInstant
	int timepoint = 0;

	// attributes
	protected String name;
	protected Color3f color = null;
	protected ImagePlus image;
	protected boolean[] channels = new boolean[] {true, true, true};
	protected float transparency = 0f;
	protected int resamplingF = 1;
	protected int threshold = 0;
	protected boolean shaded = true;
	protected int type = VOLUME;

	// visibility flags
	private boolean locked = false;
	private boolean visible = true;
	private boolean bbVisible = false;
	private boolean coordVisible = UniverseSettings.
					showLocalCoordinateSystemsByDefault;
	private boolean showPL = false;
	protected boolean selected = false;

	// entries
	private ContentNode contentNode = null;

	// point list
	private PointListShape plShape   = null;
	private PointListDialog plDialog = null;
	private PointListPanel plPanel   = null;
	private PointList points;

	// scene graph entries
	private Switch bbSwitch;
	private BitSet whichChild = new BitSet(5);

	protected TransformGroup localRotate;
	protected TransformGroup localTranslate;

	public ContentInstant(String name) {
		// create BranchGroup for this image
		this.name = name;
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);

		// create transformation for pickeing
		localTranslate = new TransformGroup();
		localTranslate.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		localTranslate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		addChild(localTranslate);
		localRotate = new TransformGroup();
		localRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		localRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		localTranslate.addChild(localRotate);

		bbSwitch = new Switch();
		bbSwitch.setWhichChild(Switch.CHILD_MASK);
		bbSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		bbSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		bbSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		bbSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		localRotate.addChild(bbSwitch);

		// create the point list
		points = new PointList();
		plShape = new PointListShape(points);
		plShape.setPickable(false);
		plPanel = new PointListPanel(name, points);
	}

	public void displayAs(int type) {
		if(image == null)
			return;
		// create content node and add it to the switch
		switch(type) {
			case VOLUME: contentNode = new VoltexGroup(this); break;
			case ORTHO: contentNode = new OrthoGroup(this); break;
			case SURFACE: contentNode = new MeshGroup(this); break;
			case SURFACE_PLOT2D: contentNode =
				new SurfacePlotGroup(this); break;
			default: throw new IllegalArgumentException(
					"Specified type is neither VOLUME, ORTHO," +
					"SURFACE or SURFACEPLOT2D");
		}
		display(contentNode);
		// update type
		this.type = type;
	}

	public static int getDefaultThreshold(ImagePlus imp, int type) {
		if(type != SURFACE)
			return 0;
		ImageStack stack = imp.getStack();
		int d = imp.getStackSize();
		// compute stack histogram
		int[] h = stack.getProcessor(1).getHistogram();
		for(int z = 1; z < d; z++) {
			int[] tmp = stack.getProcessor(z+1).getHistogram();
			for(int i = 0; i < h.length; i++)
				h[i] += tmp[i];

		}
		return imp.getProcessor().getAutoThreshold(h);
	}

	public static int getDefaultResamplingFactor(ImagePlus imp, int type) {
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int max = Math.max(w, Math.max(h, d));
		switch(type) {
			case SURFACE: return (int)Math.ceil(max / 128f);
			case VOLUME:  return (int)Math.ceil(max / 256f);
			case ORTHO:   return (int)Math.ceil(max / 256f);
			case SURFACE_PLOT2D: return (int)Math.ceil(max / 128f);
		}
		return 1;
	}

	public void display(ContentNode node) {
		// remove everything if possible
		bbSwitch.removeAllChildren();

		// create content node and add it to the switch
		contentNode = node;
		bbSwitch.addChild(contentNode);

		// create the bounding box and add it to the switch
		Point3d min = new Point3d(); contentNode.getMin(min);
		Point3d max = new Point3d(); contentNode.getMax(max);
		BoundingBox bb = new BoundingBox(min, max);
		bb.setPickable(false);
		bbSwitch.addChild(bb);
		bb = new BoundingBox(min, max, new Color3f(0, 1, 0));
		bb.setPickable(false);
		bbSwitch.addChild(bb);

		// create coordinate system and add it to the switch
		float cl = (float)Math.abs(max.x - min.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(
						cl, new Color3f(0, 1, 0));
		cs.setPickable(false);
		bbSwitch.addChild(cs);

		// create point list and add it to the switch
		bbSwitch.addChild(plShape);

		// initialize child mask of the switch
		whichChild.set(BS, selected);
		whichChild.set(CS, coordVisible);
		whichChild.set(CO, visible);
		whichChild.set(PL, showPL);
		bbSwitch.setChildMask(whichChild);

		// update type
		this.type = CUSTOM;
	}

	/* ************************************************************
	 * setters - visibility flags
	 *
	 * ***********************************************************/

	public void setVisible(boolean b) {
		visible = b;
		whichChild.set(CO, b);
		whichChild.set(CS, b && coordVisible);
// 		whichChild.set(BB, b && bbVisible);
		// only if hiding, hide the point list
		if(!b) {
			showPointList(false);
		}
		bbSwitch.setChildMask(whichChild);
	}

	public void showBoundingBox(boolean b) {
		bbVisible = b;
		whichChild.set(BB, b);
		bbSwitch.setChildMask(whichChild);
	}


	public void showCoordinateSystem(boolean b) {
		coordVisible = b;
		whichChild.set(CS, b);
		bbSwitch.setChildMask(whichChild);
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		whichChild.set(BS, selected);
		bbSwitch.setChildMask(whichChild);
	}

	/* ************************************************************
	 * point list
	 *
	 * ***********************************************************/

	public void setPointListDialog(PointListDialog p) {
		this.plDialog = p;
	}

	public void showPointList(boolean b) {
		if(plShape == null)
			return;

		whichChild.set(PL, b);
		showPL = b;
		bbSwitch.setChildMask(whichChild);
		if(b && plDialog != null)
			plDialog.addPointList(name, plPanel);
		else if(!b && plDialog != null)
			plDialog.removePointList(plPanel);
	}

	public void loadPointList() {
		points = PointList.load(image);
		plPanel.setPointList(points);
		plShape.setPointList(points);
	}

	public void savePointList() {
		String dir = OpenDialog.getDefaultDirectory();
		String n = this.name;
		if(image != null) {
			FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			n = fi.fileName;
		}
		points.save(dir, n);
	}

	/**
	 * @deprecated
	 * @param p
	 */
	public void addPointListPoint(Point3d p) {
		points.add(p.x, p.y, p.z);
		if(plDialog != null)
			plDialog.update();
	}

	/**
	 * @deprecated
	 * @param i
	 * @param pos
	 */
	public void setListPointPos(int i, Point3d pos) {
		points.placePoint(points.get(i), pos.x, pos.y, pos.z);
	}

	public float getLandmarkPointSize() {
		return plShape.getRadius();
	}

	public void setLandmarkPointSize(float r) {
		plShape.setRadius(r);
	}

	public PointList getPointList() {
		return points;
	}

	/**
	 * @deprecated
	 * @param i
	 */
	public void deletePointListPoint(int i) {
		points.remove(i);
		if(plDialog != null)
			plDialog.update();
	}

	/* ************************************************************
	 * setters - transform
	 *
	 **************************************************************/
	public void toggleLock() {
		locked = !locked;
	}

	public void setLocked(boolean b) {
		locked = b;
	}

	public void applyTransform(double[] matrix) {
		applyTransform(new Transform3D(matrix));
	}

	public void applyTransform(Transform3D transform) {
		Transform3D t1 = new Transform3D();
		localTranslate.getTransform(t1);
		Transform3D t2 = new Transform3D();
		localRotate.getTransform(t2);
		t1.mul(t2);

		t1.mul(transform, t1);
		setTransform(t1);
	}

	public void setTransform(double[] matrix) {
		if(contentNode == null)
			return;
		setTransform(new Transform3D(matrix));
	}

	public void setTransform(Transform3D transform) {
		if(contentNode == null)
			return;
		Transform3D t = new Transform3D();
		Point3d c = new Point3d(); contentNode.getCenter(c);

		Matrix3f m = new Matrix3f();
		transform.getRotationScale(m);
		t.setRotationScale(m);
		// One might thing a rotation matrix has no translational
		// component, however, if the rotation is composed of
		// translation - rotation - backtranslation, it has indeed.
		Vector3d v = new Vector3d();
		v.x = -m.m00*c.x - m.m01*c.y - m.m02*c.z + c.x;
		v.y = -m.m10*c.x - m.m11*c.y - m.m12*c.z + c.y;
		v.z = -m.m20*c.x - m.m21*c.y - m.m22*c.z + c.z;
		t.setTranslation(v);
		localRotate.setTransform(t);

		Vector3d v2 = new Vector3d();
		transform.get(v2);
		v2.sub(v);
		t.set(v2);
		localTranslate.setTransform(t);
	}

	/* ************************************************************
	 * setters - attributes
	 *
	 * ***********************************************************/

	public void setChannels(boolean[] channels) {
		boolean channelsChanged = channels[0] != this.channels[0] ||
				channels[1] != this.channels[1] ||
				channels[2] != this.channels[2];
		if(!channelsChanged)
			return;
		this.channels = channels;
		if(contentNode != null)
			contentNode.channelsUpdated(channels);
	}

	public void setThreshold(int th) {
		if(th != threshold) {
			this.threshold = th;
			if(contentNode != null)
				contentNode.thresholdUpdated(threshold);
		}
	}

	public void setShaded(boolean b) {
		if(b != shaded) {
			this.shaded = b;
			if(contentNode != null)
				contentNode.shadeUpdated(shaded);
		}
	}

	public boolean isShaded() {
		return shaded;
	}

	public void setColor(Color3f color) {
		if ((this.color == null && color == null) ||
				(this.color != null && color != null &&
				 this.color.equals(color)))
			return;
		this.color = color;
 		plShape.setColor(color);
		if(contentNode != null)
			contentNode.colorUpdated(this.color);
	}

	public synchronized void setTransparency(float transparency) {
		transparency = transparency < 0 ? 0 : transparency;
		transparency = transparency > 1 ? 1 : transparency;
		if(Math.abs(transparency - this.transparency) < 0.01)
			return;
		this.transparency = transparency;
		if(contentNode != null)
			contentNode.transparencyUpdated(this.transparency);
	}

	/* ************************************************************
	 * UniverseListener interface
	 *
	 *************************************************************/
	public void transformationStarted(View view) {}
	public void contentAdded(Content c) {}
	public void contentRemoved(Content c) {
		if(plDialog != null)
			plDialog.removePointList(plPanel);
	}
	public void canvasResized() {}
	public void contentSelected(Content c) {}
	public void contentChanged(Content c) {}

	public void universeClosed() {
		if(plDialog != null)
			plDialog.removePointList(plPanel);
	}

	public void transformationUpdated(View view) {
		eyePtChanged(view);
	}

	public void transformationFinished(View view) {
		eyePtChanged(view);
	}

	public void eyePtChanged(View view) {
		if(contentNode != null)
			contentNode.eyePtChanged(view);
	}

	/* *************************************************************
	 * getters
	 *
	 **************************************************************/
	@Override
	public String getName() {
		return name;
	}

	public int getTimepoint() {
		return timepoint;
	}

	public int getType() {
		return type;
	}

	public ContentNode getContent() {
		return contentNode;
	}

	public ImagePlus getImage() {
		return image;
	}

	public boolean[] getChannels() {
		return channels;
	}

	public Color3f getColor() {
		return color;
	}

	public int getThreshold() {
		return threshold;
	}

	public float getTransparency() {
		return transparency;
	}

	public int getResamplingFactor() {
		return resamplingF;
	}

	public TransformGroup getLocalRotate() {
		return localRotate;
	}

	public TransformGroup getLocalTranslate() {
		return localTranslate;
	}

	public void getLocalRotate(Transform3D t) {
		localRotate.getTransform(t);
	}

	public void getLocalTranslate(Transform3D t) {
		localTranslate.getTransform(t);
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean hasCoord() {
		return coordVisible;
	}

	public boolean hasBoundingBox() {
		return bbVisible;
	}

	public boolean isPLVisible() {
		return showPL;
	}
}

