package ij3d.shapes;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class BoundingBox extends BranchGroup {

	private Point3f min, max;

	public BoundingBox(Point3d min, Point3d max) {
		this(new Point3f(min), new Point3f(max));
	}

	public BoundingBox(Point3f min, Point3f max) {
		this(min, max, new Color3f(1, 0, 0));
	}

	public BoundingBox(Point3d minp, Point3d maxp, Color3f color) {
		this(new Point3f(minp), new Point3f(maxp), color);
	}
	
	public BoundingBox(Point3f minp, Point3f maxp, Color3f color) {
		setCapability(BranchGroup.ALLOW_DETACH);
//		setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		min = minp;
		max = maxp;
		
		min.x -= 0; min.y -= 0; min.z -= 0;
		max.x += 0; max.y += 0; max.z += 0;
		Point3f[] p = new Point3f[8];
		p[0] = new Point3f(min.x, min.y, max.z);
		p[1] = new Point3f(max.x, min.y, max.z);
		p[2] = new Point3f(max.x, max.y, max.z);
		p[3] = new Point3f(min.x, max.y, max.z);
		p[4] = new Point3f(min.x, min.y, min.z);
		p[5] = new Point3f(max.x, min.y, min.z);
		p[6] = new Point3f(max.x, max.y, min.z);
		p[7] = new Point3f(min.x, max.y, min.z);

		Shape3D shape = new Shape3D();
		shape.setName("BB");

		Point3f[] coords = new Point3f[2];

		coords[0] = p[0];
		coords[1] = p[1];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[1];
		coords[1] = p[2];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[2];
		coords[1] = p[3];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[3];
		coords[1] = p[0];
		shape.addGeometry(makeLine(coords, color));

		coords[0] = p[4];
		coords[1] = p[5];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[5];
		coords[1] = p[6];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[6];
		coords[1] = p[7];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[7];
		coords[1] = p[4];
		shape.addGeometry(makeLine(coords, color));
		
		coords[0] = p[0];
		coords[1] = p[4];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[1];
		coords[1] = p[5];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[2];
		coords[1] = p[6];
		shape.addGeometry(makeLine(coords, color));
		coords[0] = p[3];
		coords[1] = p[7];
		shape.addGeometry(makeLine(coords, color));
		
		Appearance a = new Appearance();
		PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		a.setPolygonAttributes(pa);

		ColoringAttributes ca = new ColoringAttributes();
		ca.setColor(color);
		a.setColoringAttributes(ca);

		shape.setAppearance(a);

		addChild(shape);
	}

	private Geometry makeLine(Point3f[] coords, Color3f color) {
		LineArray ga = new LineArray(2, 
				GeometryArray.COORDINATES |
				GeometryArray.COLOR_3 |
				GeometryArray.NORMALS);
//		ga.setCapability(GeometryArray.ALLOW_INTERSECT);
		ga.setCoordinates(0, coords);
		Color3f[] col = new Color3f[2];
		for(int i = 0; i < 2; i++) 
			col[i] = color;
		ga.setColors(0, col);
		return ga;
	}

	public String toString() {
		return "[BoundingBox (" 
			+ min.x + ", " + min.y + ", " + min.z + ") - ("
			+ max.x + ", " + max.y + ", " + max.z + ")]";
	}
} 

