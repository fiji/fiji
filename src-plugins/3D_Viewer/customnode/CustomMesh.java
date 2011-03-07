package customnode;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.process.StackConverter;

import vib.InterpolatedImage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryStripArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;

public abstract class CustomMesh extends Shape3D {

	public static final Color3f DEFAULT_COLOR = new Color3f(0, 1, 0);

	protected Color3f color = DEFAULT_COLOR;
	protected List<Point3f> mesh = null;
	protected float transparency = 0;
	protected boolean shaded = true;

	protected String loadedFromName = null;
	protected String loadedFromFile = null;
	protected boolean changed = false;

	protected CustomMesh() {}

	protected CustomMesh(List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	protected CustomMesh(List<Point3f> mesh, Color3f color, float transp) {
		this.mesh = mesh;
		if(color != null)
			this.color = color;
		this.transparency = transp;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	public String getFile() {
		return loadedFromFile;
	}

	public String getName() {
		return loadedFromName;
	}

	public boolean hasChanged() {
		return changed;
	}

	public void update() {
		this.setGeometry(createGeometry());
		this.setAppearance(createAppearance());
		changed = true;
	}

	public List getMesh() {
		return mesh;
	}

	public Color3f getColor() {
		return color;
	}

	public float getTransparency() {
		return transparency;
	}

	public boolean isShaded() {
		return shaded;
	}

	public void setShaded(boolean b) {
		this.shaded = b;
		PolygonAttributes pa = getAppearance().getPolygonAttributes();
		if(b)
			pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	public void calculateMinMaxCenterPoint(Point3f min,
				Point3f max, Point3f center) {

		if(mesh == null || mesh.size() == 0) {
			min.set(0, 0, 0);
			max.set(0, 0, 0);
			center.set(0, 0, 0);
			return;
		}

		min.x = min.y = min.z = Float.MAX_VALUE;
		max.x = max.y = max.z = Float.MIN_VALUE;
		for(int i = 0; i < mesh.size(); i++) {
			Point3f p = (Point3f)mesh.get(i);
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

	public abstract float getVolume();

	private int[] valid = new int[1];
	protected void addVerticesToGeometryStripArray(Point3f[] v) {
		changed = true;
		mesh.addAll(Arrays.asList(v));

		// check maximum vertex count
		GeometryStripArray ga = (GeometryStripArray)getGeometry();
		int max = ga.getVertexCount();
		ga.getStripVertexCounts(valid);
		int idx = valid[0];
		if(idx + v.length > max) {
			// enlarge arrays
			setGeometry(createGeometry());
			return;
		}


		valid[0] = idx + v.length;
		ga.setStripVertexCounts(valid);

		ga.setCoordinates(idx, v);

		// update colors
		Color3f[] colors = new Color3f[v.length];
		Arrays.fill(colors, this.color);
		ga.setColors(idx, colors);

		recalculateNormals(ga);
	}

	protected void addVerticesToGeometryArray(Point3f[] v) {
		changed = true;
		mesh.addAll(Arrays.asList(v));

		// check maximum vertex count
		GeometryArray ga = (GeometryArray)getGeometry();
		int max = ga.getVertexCount();
		int idx = ga.getValidVertexCount();
		if(idx + v.length > max) {
			// enlarge arrays
			setGeometry(createGeometry());
			return;
		}

		ga.setValidVertexCount(idx + v.length);
		ga.setCoordinates(idx, v);

		// update colors
		Color3f[] colors = new Color3f[v.length];
		Arrays.fill(colors, this.color);
		ga.setColors(idx, colors);

		recalculateNormals(ga);
	}

	public int[] vertexIndicesOfPoint(Point3f p) {
		int N = mesh.size();

		int[] indices = new int[N];
		int i = 0;
		for(int v = 0; v < N; v++)
			if(mesh.get(v) != null && mesh.get(v).equals(p))
				indices[i++] = v;

		int[] ret = new int[i];
		System.arraycopy(indices, 0, ret, 0, i);
		return ret;
	}

	public void setCoordinate(int i, Point3f p) {
		changed = true;
		((GeometryArray)getGeometry()).setCoordinate(i, p);
		mesh.get(i).set(p);
	}

	public void setCoordinates(int[] indices, Point3f p) {
		changed = true;
		GeometryArray ga = (GeometryArray)getGeometry();
		for(int i = 0; i < indices.length; i++) {
			ga.setCoordinate(indices[i], p);
			mesh.get(indices[i]).set(p);
		}
	}

	public void recalculateNormals(GeometryArray ga) {
		if(ga == null)
			return;
		if((ga.getVertexFormat() & GeometryArray.NORMALS) == 0)
			return;
		changed = true;
		GeometryInfo gi = new GeometryInfo(ga);
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		GeometryArray tmp = gi.getGeometryArray();
		int v = ga.getValidVertexCount();
		float[] normals = new float[3 * v];
		tmp.getNormals(0, normals);
		ga.setNormals(0, normals);
	}

	protected void addVertices(Point3f[] v) {
		if(mesh == null)
			return;
		changed = true;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null) {
			mesh.addAll(Arrays.asList(v));
			setGeometry(createGeometry());
			return;
		}

		if(ga instanceof GeometryStripArray)
			addVerticesToGeometryStripArray(v);
		else
			addVerticesToGeometryArray(v);
	}

	protected void removeVertices(int[] indices) {
		if(mesh == null)
			return;

		changed = true;
		for(int i = indices.length - 1; i >= 0; i--) {
			if(indices[i] < 0 || indices[i] >= mesh.size())
				continue;
			mesh.remove(indices[i]);
		}
		setGeometry(createGeometry());
	}

	public void setColor(Color3f color) {
		this.color = color != null ? color : DEFAULT_COLOR;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		int N = ga.getVertexCount();
		Color3f colors[] = new Color3f[N];
		for(int i=0; i<N; i++){
			colors[i] = this.color;
		}
		ga.setColors(0, colors);
		changed = true;
	}

	public void setColor(List<Color3f> color) {
		this.color = null;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		int N = ga.getValidVertexCount();
		if(color.size() != N)
			throw new IllegalArgumentException(
				"list of size " + N + " expected");
		Color3f[] colors = new Color3f[N];
		color.toArray(colors);
		ga.setColors(0, colors);
		changed = true;
	}

	public void setColor(int vtxIndex, Color3f color) {
		this.color = null;
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;
		ga.setColor(vtxIndex, color);
		changed = true;
	}

	public void loadSurfaceColorsFromImage(ImagePlus imp) {
		GeometryArray ga = (GeometryArray)getGeometry();
		if(ga == null)
			return;

		if(imp.getType() != ImagePlus.COLOR_RGB) {
			imp = new Duplicator().run(imp);
			new StackConverter(imp).convertToRGB();
		}
		InterpolatedImage ii = new InterpolatedImage(imp);

		int N = ga.getValidVertexCount();
		Color3f[] colors = new Color3f[N];
		Calibration cal = imp.getCalibration();
		double pw = cal.pixelWidth;
		double ph = cal.pixelHeight;
		double pd = cal.pixelDepth;
		Point3f coord = new Point3f();
		for(int i = 0; i < N; i++) {
			ga.getCoordinate(i, coord);
			int v = (int)Math.round(ii.interpol.get(
				coord.x / pw,
				coord.y / ph,
				coord.z / pd));
			colors[i] = new Color3f(
				((v & 0xff0000) >> 16) / 255f,
				((v & 0xff00) >> 8) / 255f,
				(v & 0xff) / 255f);
		}
		ga.setColors(0, colors);
		changed = true;
	}

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

	public void restoreDisplayedData(String path, String name) {
		HashMap<String, CustomMesh> contents = null;
		try {
			contents = WavefrontLoader.load(path);
		} catch(IOException e) {
			e.printStackTrace();
		}
		if(contents.containsKey(name)) {
			this.mesh = contents.get(name).getMesh();
			update();
		}
	}

	public void swapDisplayedData(String path, String name) {
		HashMap<String, CustomMesh> contents =
			new HashMap<String, CustomMesh>();
		contents.put(name, this);
		try {
			WavefrontExporter.save(
				contents,
				path + ".obj");
			this.mesh = null;
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void clearDisplayedData() {
		this.mesh = null;
	}

	protected abstract GeometryArray createGeometry();
}
