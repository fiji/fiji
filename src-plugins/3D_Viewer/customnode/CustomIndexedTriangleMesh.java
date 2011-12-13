package customnode;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.process.StackConverter;

import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import vib.InterpolatedImage;

public class CustomIndexedTriangleMesh extends CustomMesh {

	protected Point3f[] vertices;
	protected Color3f[] colors;
	protected int[] faces;
	protected int nFaces;
	protected int nVertices;

	public CustomIndexedTriangleMesh(List<Point3f> mesh) {
		// TODO
	}

	public CustomIndexedTriangleMesh(Point3f[] vertices, int[] faces) {
		this(vertices, faces, DEFAULT_COLOR, 0);
	}

	public CustomIndexedTriangleMesh(Point3f[] vertices, int[] faces, Color3f color, float transp) {
		this.nVertices = vertices.length;
		this.nFaces = faces.length;
		this.vertices = vertices;
		this.faces = faces;
		if(color != null)
			setColor(color);
		this.transparency = transp;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		update();
	}

	@Override
	public String getFile() {
		return loadedFromFile;
	}

	@Override
	public String getName() {
		return loadedFromName;
	}

	@Override
	public boolean hasChanged() {
		return changed;
	}

	@Override
	public void update() {
		this.setGeometry(createGeometry());
		this.setAppearance(createAppearance());
		changed = true;
	}

	@Override
	public List getMesh() {
		return mesh;
	}

	@Override
	public Color3f getColor() {
		return color;
	}

	@Override
	public float getTransparency() {
		return transparency;
	}

	@Override
	public boolean isShaded() {
		return shaded;
	}

	@Override
	public void setShaded(boolean b) {
		this.shaded = b;
		PolygonAttributes pa = getAppearance().getPolygonAttributes();
		if(b)
			pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	@Override
	public void calculateMinMaxCenterPoint(Point3f min,
				Point3f max, Point3f center) {

		if(vertices == null || nVertices == 0) {
			min.set(0, 0, 0);
			max.set(0, 0, 0);
			center.set(0, 0, 0);
			return;
		}

		min.x = min.y = min.z = Float.MAX_VALUE;
		max.x = max.y = max.z = Float.MIN_VALUE;
		for(int i = 0; i < nVertices; i++) {
			Point3f p = vertices[i];
			if(p.x < min.x) min.x = p.x;
			if(p.y < min.y) min.y = p.y;
			if(p.z < min.z) min.z = p.z;
			if(p.x > max.x) max.x = p.x;
			if(p.y > max.y) max.y = p.y;
			if(p.z > max.z) max.z = p.z;
		}
		center.x = (max.x + min.x) / 2;
		center.y = (max.y + min.y) / 2;
		center.z = (max.z + min.z) / 2;
	}

	@Override
	public float getVolume() {
		throw new IllegalArgumentException("Not supported yet");
	}

	// private int[] valid = new int[1];
	@Override
	protected void addVerticesToGeometryStripArray(Point3f[] v) {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	protected void addVerticesToGeometryArray(Point3f[] v) {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	public int[] vertexIndicesOfPoint(Point3f p) {
		int i = vertexIndexOfPoint(p);
		if(i == -1)
			return new int[] {};
		else
			return new int[] {i};
	}

	public int vertexIndexOfPoint(Point3f p) {
		for(int i = 0; i < nVertices; i++) {
			Point3f v = vertices[i];
			if(p.equals(v))
				return i;
		}
		return -1;
	}

	@Override
	public void setCoordinate(int i, Point3f p) {
		changed = true;
		vertices[i].set(p);
		((GeometryArray)getGeometry()).setCoordinate(i, p);
	}

	@Override
	public void setCoordinates(int[] indices, Point3f p) {
		changed = true;
		GeometryArray ga = (GeometryArray)getGeometry();
		for(int i = 0; i < indices.length; i++) {
			ga.setCoordinate(indices[i], p);
			vertices[indices[i]].set(p);
		}
	}

	@Override
	public void recalculateNormals(GeometryArray ga) {
	}

	@Override
	protected void addVertices(Point3f[] v) {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	protected void removeVertices(int[] indices) {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	public void setColor(Color3f color) {
		if(this.colors == null || this.colors.length != this.vertices.length)
			this.colors = new Color3f[this.vertices.length];
		this.color = color != null ? color : DEFAULT_COLOR;
		for(int i = 0; i < nVertices; i++)
			colors[i] = this.color;

		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;

		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setColor(List<Color3f> color) {
		if(color.size() != colors.length)
			throw new IllegalArgumentException("Number of colors must equal number of vertices");

		this.color = null;
		color.toArray(this.colors);

		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setColor(int vtxIndex, Color3f color) {
		this.color = null;
		this.colors[vtxIndex] = color;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		ga.setColor(vtxIndex, color);
		changed = true;
	}

	@Override
	public void loadSurfaceColorsFromImage(ImagePlus imp) {
		if(imp.getType() != ImagePlus.COLOR_RGB) {
			imp = new Duplicator().run(imp);
			new StackConverter(imp).convertToRGB();
		}
		InterpolatedImage ii = new InterpolatedImage(imp);

		Calibration cal = imp.getCalibration();
		double pw = cal.pixelWidth;
		double ph = cal.pixelHeight;
		double pd = cal.pixelDepth;
		for(int i = 0; i < nVertices; i++) {
			Point3f coord = vertices[i];
			int v = (int)Math.round(ii.interpol.get(
				coord.x / pw,
				coord.y / ph,
				coord.z / pd));
			colors[i] = new Color3f(
				((v & 0xff0000) >> 16) / 255f,
				((v & 0xff00) >> 8) / 255f,
				(v & 0xff) / 255f);
		}
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setTransparency(float transparency) {
		TransparencyAttributes  ta = getAppearance().
						getTransparencyAttributes();
		if(transparency <= .01f) {
			this.transparency = 0.0f;
			ta.setTransparencyMode(TransparencyAttributes.NONE);
		} else {
			this.transparency = transparency;
			ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		}
		ta.setTransparency(this.transparency);
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
		int mode = transparency == 0f ? TransparencyAttributes.NONE
					: TransparencyAttributes.FASTEST;
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

	@Override
	public void restoreDisplayedData(String path, String name) {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support swapping data.");
	}

	@Override
	public void swapDisplayedData(String path, String name) {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support swapping data.");
	}

	@Override
	public void clearDisplayedData() {
		throw new RuntimeException("CustomIndexedTriangleMesh does not support swapping data.");
	}

	@Override
	protected GeometryArray createGeometry() {
		if(nVertices == 0)
			return null;
		IndexedTriangleArray ta = new IndexedTriangleArray(
			vertices.length,
				TriangleArray.COORDINATES |
				TriangleArray.COLOR_3 |
				TriangleArray.NORMALS,
			faces.length);

		ta.setValidIndexCount(nFaces);

		ta.setCoordinates(0, vertices);
		ta.setColors(0, colors);

		ta.setCoordinateIndices(0, faces);
		ta.setColorIndices(0, faces);

		ta.setNormals(0, getNormals());
		ta.setNormalIndices(0, faces);

		ta.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		ta.setCapability(GeometryArray.ALLOW_INTERSECT);

		return ta;
	}

	public Vector3f[] getNormals() {
		Vector3f[] normals = new Vector3f[nVertices];
		for (int i = 0; i < nVertices; i++)
			normals[i] = new Vector3f();

		Vector3f v1 = new Vector3f(), v2 = new Vector3f();
		for (int i = 0; i < nFaces; i += 3) {
			int f1 = faces[i];
			int f2 = faces[i + 1];
			int f3 = faces[i + 2];

			v1.sub(vertices[f2], vertices[f1]);
			v2.sub(vertices[f3], vertices[f1]);
			v1.cross(v1, v2);

			normals[f1].add(v1);
			normals[f2].add(v1);
			normals[f3].add(v1);
		}
		for (int i = 0; i < nVertices; i++)
			normals[i].normalize();

		return normals;
	}

}