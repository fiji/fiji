package ij3d.shapes;

import java.awt.Font;
import java.text.DecimalFormat;

import javax.vecmath.Point3f;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import javax.media.j3d.LineArray;
import javax.media.j3d.Geometry;
import javax.media.j3d.OrientedShape3D;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.Text3D;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;


public class Scalebar extends BranchGroup {

	private DecimalFormat df = new DecimalFormat("###0.00");

	private float length = 1f;
	private Color3f color = new Color3f(1.0f, 1.0f, 1.0f);
	private float x = 2, y = 2;
	private String unit = "";

	private TransformGroup positionTG, textTG;
	private Shape3D lineShape;
	private OrientedShape3D textShape;

	public Scalebar() {
		this(1f);
	}
	
	public Scalebar(float length) {
		Transform3D position = new Transform3D();
		positionTG = new TransformGroup(position);
		positionTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		positionTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		addChild(positionTG);

		lineShape = new Shape3D();
		lineShape.setGeometry(createLineGeometry());
		lineShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		positionTG.addChild(lineShape);

		Transform3D texttranslate = new Transform3D();
		texttranslate.setTranslation(
			new Vector3f(length/2, -length/2, 0.0f));

		textTG = new TransformGroup(texttranslate);
		textTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		positionTG.addChild(textTG);

		textShape = new OrientedShape3D();
		textShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		textShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		textShape.setGeometry(createTextGeometry());
		textShape.setAppearance(createTextAppearance());
		textShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);
		textTG.addChild(textShape);
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getLength() {
		return length;
	}

	public String getUnit() {
		return unit;
	}

	public Color3f getColor() {
		return color;
	}

	public void setUnit(String unit) {
		this.unit = unit;
		textShape.setGeometry(createTextGeometry());
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		Transform3D p = new Transform3D();
		p.setTranslation(new Vector3f(x, y, 0.0f));
		positionTG.setTransform(p);
	}

	public void setLength(float l) {
		this.length = l;
		lineShape.setGeometry(createLineGeometry());
		textShape.setGeometry(createTextGeometry());
		Transform3D d = new Transform3D();
		d.setTranslation(new Vector3f(length/2, -length/2, 0.0f));
		textTG.setTransform(d);
	}

	public void setColor(Color3f c) {
		this.color = c;
		lineShape.setGeometry(createLineGeometry());
		textShape.setAppearance(createTextAppearance());
	}

	public Appearance createTextAppearance() {
		Appearance textAppear = new Appearance();
		ColoringAttributes textColor = new ColoringAttributes();
		textColor.setColor(color);
		textAppear.setColoringAttributes(textColor);
 
		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setBackFaceNormalFlip(true);
		textAppear.setPolygonAttributes(pa);

		return textAppear;
	}

	public Geometry createTextGeometry() {
		String text = df.format(length) + " " + unit;
		int fontSize = (int)length/3;
		Font3D font3D = new Font3D(
			new Font("Helvetica", Font.PLAIN, fontSize > 0 ? fontSize : 1),
			new FontExtrusion());
		try {
			Text3D textGeom = new Text3D(font3D, text);
			textGeom.setAlignment(Text3D.ALIGN_CENTER);
			return textGeom;
		} catch(Exception e) {}
		return null;
	}

	public Geometry createLineGeometry() {
		Point3f origin = new Point3f();
		Point3f onX = new Point3f(length < 1 ? 1f : length, 0, 0);

		Point3f[] coords = {origin, onX};
		int N = coords.length;
		
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++){
			colors[i] = color;
		}
		LineArray ta = new LineArray (N, 
					LineArray.COORDINATES | 
					LineArray.COLOR_3);
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);
		
		return ta;
	}
}
