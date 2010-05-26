package ij3d.shapes;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class BoundingSphere extends BranchGroup {

	private Point3f center;
	private float radius;
	private Color3f color;
	private float transparency;

	private TransformGroup scaleTG;
	private Transform3D scale = new Transform3D();

	private TransformGroup translateTG;
	private Transform3D translate = new Transform3D();

	private TransparencyAttributes ta;
	private PolygonAttributes pa;
	private ColoringAttributes ca;
	private Appearance appearance;

	public BoundingSphere(Point3f center, float radius) {
		this(center, radius, new Color3f(1, 0, 0));
	}

	public BoundingSphere(Point3f center, float radius, Color3f color) {
		this(center, radius, color, 0f);
	}

	public BoundingSphere(Point3f center, float radius, Color3f color, float transparency) {
		setCapability(BranchGroup.ALLOW_DETACH);
//		setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		this.center = new Point3f(center);
		this.radius = radius;
		this.color = color;
		this.transparency = transparency;

		appearance = new Appearance();
		pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		appearance.setPolygonAttributes(pa);
		ta = new TransparencyAttributes();
		ta.setTransparency(transparency);
		ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		appearance.setTransparencyAttributes(ta);
		ca = new ColoringAttributes();
		ca.setColor(color);
		appearance.setColoringAttributes(ca);

		Sphere sphere = new Sphere(1, /*Primitive.ENABLE_GEOMETRY_PICKING,*/ appearance);
		sphere.setName("BS");

		Vector3f translateV = new Vector3f(center);
		translate.set(translateV);
		translateTG = new TransformGroup(translate);
		translateTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		scale.set(radius);
		scaleTG = new TransformGroup(scale);
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		
		scaleTG.addChild(sphere);
		translateTG.addChild(scaleTG);
		addChild(translateTG);
	}

	public void setRadius(float radius) {
		this.radius = radius;
		scale.set(radius);
		scaleTG.setTransform(scale);
	}

	public void setCenter(Point3f center) {
		this.center.set(center);
		Vector3f translateV = new Vector3f(center);
		translate.set(translateV);
		translateTG.setTransform(translate);
	}

	public void getTransform(Transform3D transform) {
		transform.set(new Vector3f(center));
		transform.mul(scale);
	}

	public void getTransformInverse(Transform3D transform) {
		getTransform(transform);
		transform.invert();
	}

	public String toString() {
		return "[BoundingSphere center: " + center + " radius: " + radius + "] " + hashCode(); 
	}
} 

