package customnode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;


public class CustomTransparentTriangleMesh extends CustomTriangleMesh {

	private double volume = 0.0;

	public CustomTransparentTriangleMesh(List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	public CustomTransparentTriangleMesh(List<Point3f> mesh, Color3f col, float trans) {
		super(mesh, col, trans);
	}

	@Override
	public float getVolume() {
		return (float)volume;
	}

	@Override
	public void setColor(Color3f color) {
		this.color = color != null ? color : DEFAULT_COLOR;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		int N = ga.getVertexCount();
		Color4f colors[] = new Color4f[N];
		for(int i=0; i<N; i++){
			colors[i] = new Color4f(
				this.color.x,
				this.color.y,
				this.color.z,
				1);
		}
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setTransparency(float t) {
		// do nothing
	}

	public void setTransparentColor(List<Color4f> color) {
		this.color = null;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		int N = ga.getValidVertexCount();
		if(color.size() != N)
			throw new IllegalArgumentException(
				"list of size " + N + " expected");
		Color4f[] colors = new Color4f[N];
		color.toArray(colors);
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	protected GeometryArray createGeometry() {
		if(mesh == null || mesh.size() < 3)
			return null;
		List<Point3f> tri = mesh;
		int nValid = tri.size();
		int nAll = 2 * nValid;

		Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		Color4f colors[] = new Color4f[nValid];
		if (null == color) {
			// Vertex-wise colors are not stored
			// so they have to be retrieved from the geometry:
			for (int i=0; i<colors.length; ++i) {
				colors[i] = new Color4f(DEFAULT_COLOR.x, DEFAULT_COLOR.y, DEFAULT_COLOR.z, 1);
			}
			GeometryArray gaOld = (GeometryArray) getGeometry();
			if (null != gaOld)
				gaOld.getColors(0, colors);
		} else {
			Arrays.fill(colors, new Color4f(
				color.x,
				color.y,
				color.z,
				1));
		}

		GeometryArray ta = new TriangleArray(nAll,
						TriangleArray.COORDINATES |
						TriangleArray.COLOR_4 |
						TriangleArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		GeometryInfo gi = new GeometryInfo(ta);
// 		gi.recomputeIndices();
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
// 		Stripifier st = new Stripifier();
// 		st.stripify(gi);
		GeometryArray result = gi.getGeometryArray();
		result.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		result.setCapability(GeometryArray.ALLOW_INTERSECT);
		result.setValidVertexCount(nValid);

		return result;
	}

	@Override
	protected Appearance createAppearance () {
		Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.
					ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		if(this.shaded)
			polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else
			polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		if (null != color) // is null when colors are vertex-wise
			colorAttrib.setColor(color);
 		appearance.setColoringAttributes(colorAttrib);

		TransparencyAttributes tr = new TransparencyAttributes();
		int mode = TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		appearance.setTransparencyAttributes(tr);

		Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f,0.1f,0.1f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		appearance.setMaterial(material);
		return appearance;
	}
	public static void main(String[] args) {
		ij3d.Image3DUniverse u = new ij3d.Image3DUniverse();
		u.show();

		List<Point3f> pts = new ArrayList<Point3f>();
		pts.add(new Point3f(0, 0, 0));
		pts.add(new Point3f(1, 0, 0));
		pts.add(new Point3f(1, 1, 0));

		CustomTransparentTriangleMesh m = new CustomTransparentTriangleMesh(pts);
		List<Color4f> cols = new ArrayList<Color4f>();
		cols.add(new Color4f(0, 1, 1, 0));
		cols.add(new Color4f(1, 0, 1, 0.5f));
		cols.add(new Color4f(1, 1, 0, 1));

		m.setTransparentColor(cols);

		u.addCustomMesh(m, "lkjl");
	}
}
