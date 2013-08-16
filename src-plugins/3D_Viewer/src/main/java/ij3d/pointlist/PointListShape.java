package ij3d.pointlist;

import java.awt.Font;

import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.OrientedShape3D;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.ScaleInterpolator;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import vib.BenesNamedPoint;
import vib.PointList;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;

/**
 * This class extends BranchGroup to represent a PointList as a number
 * of spheres in the universe.
 *
 * @author Benjamin Schmid
 *
 */
public class PointListShape extends BranchGroup
			implements PointList.PointListListener{

	/** The PointList which is represented by this PointListShape */
	private PointList points;

	/** The color of the points */
	private Color3f color = new Color3f(1, 1, 0);

	/** The default appearance which is used for new points */
	private Appearance appearance;

	/** The radius of the points */
	private float radius = 1;

	/**
	 * Constructor.
	 * Creates a new BranchGroup, and adds all points from the specified
	 * PointList as points to it.
	 * @param points
	 */
	public PointListShape(PointList points) {
		setCapability(ALLOW_CHILDREN_EXTEND);
		setCapability(ALLOW_CHILDREN_WRITE);
		setCapability(ALLOW_DETACH);
		this.points = points;
		points.addPointListListener(this);
		initAppearance(color);
		recreate();
	}

	/**
	 * Set the PointList which is represented by this PointListShape.
	 * @param pl
	 */
	public void setPointList(PointList pl) {
		points.removePointListListener(this);
		points = pl;
		points.addPointListListener(this);
		recreate();
	}

	/**
	 * Returns the radius of the points.
	 * @return
	 */
	public float getRadius() {
		return radius;
	}

	/**
	 * Set the radius of the points.
	 * @param r
	 */
	public void setRadius(float r) {
		this.radius = r;
		Transform3D t3d = new Transform3D();
		for(int i = 0; i < numChildren(); i++) {
			BranchGroup bg = (BranchGroup)getChild(i);
			TransformGroup tg = (TransformGroup)bg.getChild(0);
			ScaleInterpolator si = (ScaleInterpolator)tg.getChild(1);
			if (si != null) {
				si.setMaximumScale(5 * radius);
				si.setMinimumScale(radius);
			}
			TransformGroup sig = (TransformGroup)tg.getChild(0);
			sig.getTransform(t3d);
			t3d.setScale(radius);
			sig.setTransform(t3d);
		}
	}

	/**
	 * Set the color of the points.
	 * @param c
	 */
	public void setColor(Color3f c) {
		color = c == null ? new Color3f(1, 1, 0) : c;
		initAppearance(color);
		for(int i = 0; i < numChildren(); i++) {
			BranchGroup bg = (BranchGroup)getChild(i);
			TransformGroup tg = (TransformGroup)((Group)bg
						.getChild(0)).getChild(0);
			Sphere s = (Sphere)tg.getChild(0);
			s.setAppearance(appearance);

			// update text
			Group gr = (Group)bg.getChild(0);
			gr = (Group)gr.getChild(2);
			OrientedShape3D os = (OrientedShape3D)gr.getChild(0);
			Text2D t2d = new Text2D(points.get(i).getName(), color, "Helvetica", 24, Font.PLAIN);
			t2d.setRectangleScaleFactor(0.03f);
			Geometry g = t2d.getGeometry();
			Appearance a = t2d.getAppearance();
			RenderingAttributes ra = new RenderingAttributes();
			ra.setDepthTestFunction(RenderingAttributes.ALWAYS);
			a.setRenderingAttributes(ra);
			g.setCapability(Geometry.ALLOW_INTERSECT);
			os.setGeometry(g);
			os.setAppearance(a);
		}
	}

	public Color3f getColor() {
		return color;
	}

	/* *************************************************************
	 * PointList.PointListListener interface
	 * *************************************************************/
	/**
	 * @see PointList.PointListListener#added(BenesNamedPoint)
	 */
	@Override
	public void added(BenesNamedPoint p) {
		Point3f p3f = new Point3f((float)p.x, (float)p.y, (float)p.z);
		addPointToGeometry(p3f, p.getName());
	}

	/**
	 * @see PointList.PointListListener#removed(BenesNamedPoint)
	 */
	@Override
	public void removed(BenesNamedPoint p) {
		deletePointFromGeometry(p.getName());
	}

	/**
	 * @see PointList.PointListListener#renamed(BenesNamedPoint)
	 */
	@Override
	public void renamed(BenesNamedPoint p) {
		int i = points.indexOf(p);
		getChild(i).setName(points.get(i).getName());
		Group bg = (Group)getChild(i);
		bg = (Group)bg.getChild(0);
		bg = (Group)bg.getChild(2);
		OrientedShape3D os = (OrientedShape3D)bg.getChild(0);
		Text2D t2d = new Text2D(p.getName(), color, "Helvetica", 24, Font.PLAIN);
		t2d.setRectangleScaleFactor(0.03f);
		Geometry g = t2d.getGeometry();
		Appearance a = t2d.getAppearance();
		RenderingAttributes ra = new RenderingAttributes();
		ra.setDepthTestFunction(RenderingAttributes.ALWAYS);
		a.setRenderingAttributes(ra);
		g.setCapability(Geometry.ALLOW_INTERSECT);
		os.setGeometry(g);
		os.setAppearance(a);
	}

	/**
	 * @see PointList.PointListListener#moved(BenesNamedPoint)
	 */
	@Override
	public void moved(BenesNamedPoint p) {
		int i = points.indexOf(p);
		if(i >= 0 && i < points.size())
			updatePositionInGeometry(i, new Point3d(p.x, p.y, p.z));
	}

	/**
	 * @see PointList.PointListListener#reordered(BenesNamedPoint)
	 */
	@Override
	public void reordered() {
		recreate();
	}

	/**
	 * @see PointList.PointListListener#highlighted(BenesNamedPoint)
	 */
	@Override
	public void highlighted(final BenesNamedPoint p) {
		final int i = points.indexOf(p);
		BranchGroup bg = (BranchGroup)getChild(i);
		TransformGroup tg = (TransformGroup)bg.getChild(0);
		ScaleInterpolator si = (ScaleInterpolator)tg.getChild(1);
		final Alpha a = si.getAlpha();
		si.setEnable(true);
		a.resume();
		try {
			Thread.sleep(600);
		} catch(Exception e) { }
		a.pause();
		si.setEnable(false);
	}

	/* *************************************************************
	 * Private methods for updating the scenegraph
	 * *************************************************************/
	private Transform3D t3d = new Transform3D();
	private Vector3f v3f = new Vector3f();

	/**
	 * Delete the child with the specified name from the scenegraph.
	 * @param name
	 */
	private void deletePointFromGeometry(String name) {
		for(int i = 0; i < numChildren(); i++) {
			BranchGroup bg = (BranchGroup)getChild(i);
			if(bg.getName().equals(name)) {
				bg.detach();
				return;
			}
		}
	}

	/**
	 * Add a new Point with the specified name to the scenegraph
	 * @param p
	 * @param name
	 */
	private void addPointToGeometry(Point3f p, String name) {
		v3f.x = p.x; v3f.y = p.y; v3f.z = p.z;

		BranchGroup bg = new BranchGroup();
		bg.setName(name);
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		t3d.set(v3f);
		TransformGroup tg = new TransformGroup(t3d);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		bg.addChild(tg);

		TransformGroup sig = new TransformGroup();
		sig.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.addChild(sig);

		Alpha alpha = new Alpha();
		alpha.setStartTime(System.currentTimeMillis());
		alpha.setMode(Alpha.DECREASING_ENABLE|Alpha.INCREASING_ENABLE);
		alpha.setIncreasingAlphaDuration(300);
		alpha.setDecreasingAlphaDuration(300);
		alpha.pause();
		ScaleInterpolator si = new ScaleInterpolator(alpha, sig);
		si.setMaximumScale(5 * radius);
		si.setMinimumScale(radius);
		BoundingSphere bs = new BoundingSphere(new Point3d(0, 0, 0), 100000);
		si.setSchedulingBounds(bs);

		tg.addChild(si);

		Sphere sphere = new Sphere();
		sphere.setPickable(false);
		sphere.getShape().setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);
		sphere.setAppearance(appearance);
		sig.addChild(sphere);
		addText(tg, name, p, color);

		addChild(bg);

		si.setEnable(false);

		Transform3D scaleTransform = new Transform3D();
		sig.getTransform(scaleTransform);
		scaleTransform.setScale(radius);
		sig.setTransform(scaleTransform);
	}

	private void addText(Group bg, String s, Point3f pos, Color3f c) {
		Transform3D translation = new Transform3D();
		translation.setTranslation(new Vector3f(-radius, -radius, 0));
		TransformGroup tg = new TransformGroup(translation);
		Text2D t2d = new Text2D(s, c, "Helvetica", 24, Font.PLAIN);
		t2d.setRectangleScaleFactor(0.03f);
		Geometry g = t2d.getGeometry();
		Appearance a = t2d.getAppearance();
		RenderingAttributes ra = new RenderingAttributes();
		ra.setDepthTestFunction(RenderingAttributes.ALWAYS);
		a.setRenderingAttributes(ra);
		g.setCapability(Geometry.ALLOW_INTERSECT);
		OrientedShape3D textShape = new OrientedShape3D();
		textShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		textShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		textShape.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);
		textShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);
		textShape.setRotationPoint(new Point3f());
		textShape.setConstantScaleEnable(true);
		textShape.setGeometry(g);
		textShape.setAppearance(a);
		tg.setCapability(ENABLE_PICK_REPORTING);
		tg.setPickable(true);

		tg.addChild(textShape);
		bg.addChild(tg);
	}

	/**
	 * Update the position of the child at the specified index.
	 * @param i
	 * @param pos
	 */
	private void updatePositionInGeometry(int i, Point3d pos) {
		BranchGroup bg = (BranchGroup)getChild(i);
		TransformGroup tg = (TransformGroup)bg.getChild(0);
		v3f.x = (float)pos.x;
		v3f.y = (float)pos.y;
		v3f.z = (float)pos.z;
		t3d.set(v3f);
		tg.setTransform(t3d);
	}

	/**
	 * Removes all children from this BranchGroup and adds all
	 * the points from the underlying PointList (again).
	 */
	private void recreate() {
		removeAllChildren();
		for(int i = 0; i < points.size(); i++) {
			BenesNamedPoint po = points.get(i);
			Point3f p3f = new Point3f(
				(float)po.x,(float)po.y,(float)po.z);
			addPointToGeometry(p3f, po.getName());
		}
	}

	/**
	 * Create a default Appearance object using the specified color.
	 * @param color
	 */
	private void initAppearance(Color3f color) {
		appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		colorAttrib.setColor(color);
		colorAttrib.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		appearance.setColoringAttributes(colorAttrib);

		Material material = new Material();
 		material.setDiffuseColor(color);
		appearance.setMaterial(material);

	}

	/**
	 * Returns a string representation of the underlying PointList.
	 */
	@Override
	public String toString() {
		return points.toString();
	}
}

