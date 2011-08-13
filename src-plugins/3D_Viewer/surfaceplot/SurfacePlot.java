package surfaceplot;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

import ij.IJ;

import ij3d.Volume;

import java.awt.Color;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;

import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

/**
 * This class displays an image stack as a surface plot.
 * 
 * @author Benjamin Schmid
 */
public final class SurfacePlot extends Shape3D {

	/** The image data */
	private Volume volume;

	/** The currently displayed slice */
	private int slice = 1;

	/** Pixel width in real world dimensions */
	private float pw = 1;
	/** Pixel height in real world dimensions */
	private float ph = 1;

	/** The maximum intensity value */
	private int maxVal = -1;
	
	/** The maximum z value */
	private float maxZ = -1;

	/** The factor by which the intensity values are multiplied */ 
	private float zFactor = 1;

	/** The color of this surface plot */
	private Color3f color = null;

	/** The geometry array */
	private IndexedQuadArray[] geometry;
	/** The appearance */
	private Appearance appearance;

	/**
	 * Constructs a SurfacePlot from the given image data, color and
	 * transparency of the specified slice.
	 * @param vol
	 * @param color
	 * @param transp
	 * @param slice
	 */
	public SurfacePlot(Volume vol, final Color3f color, 
					float transp, final int slice) {
		this.volume = vol;
		this.slice = slice;
		this.color = color;
		pw = (float)volume.pw;
		ph = (float)volume.ph;

		calculateMax();
		calculateZFactor();

		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);

		geometry = new IndexedQuadArray[volume.zDim];
		geometry[slice] = createGeometry(slice, color);
		appearance = createAppearance(transp);
		setGeometry(geometry[slice]);
		setAppearance(appearance);
		new Thread() {
			@Override
			public void run() {
				for(int g = 0; g < volume.zDim; g++) {
					if(g != slice) {
						geometry[g] = createGeometry(g, color);
						IJ.showProgress(g + 1, volume.zDim);
					}
				}
			}
		}.start();
	}

	/**
	 * Sets the currently displayed slice.
	 * @param slice
	 */
	public void setSlice(int slice) {
		this.slice = slice;
		setGeometry(geometry[slice-1]);
	}

	/**
	 * Returns the currently displayed slice.
	 */
	public int getSlice() {
		return slice;
	}

	/**
	 * Change the transparency of this surface plot.
	 * @param t
	 */
	public void setTransparency(float t) {
		TransparencyAttributes tr = appearance
					.getTransparencyAttributes();
		int mode = t == 0f ? TransparencyAttributes.NONE
					: TransparencyAttributes.FASTEST;
		tr.setTransparencyMode(mode);
		tr.setTransparency(t);
	}

	/**
	 * Change the displayed channels of this surface plot. This has only
	 * an effect if color images are displayed.
	 * @param ch
	 */
	public void setChannels(boolean[] ch) {
		if(!volume.setChannels(ch))
			return;
		calculateMax();
		calculateZFactor();
		geometry[slice] = createGeometry(slice, color);
		setGeometry(geometry[slice]);
		new Thread() {
			@Override
			public void run() {
				for(int g = 0; g < volume.zDim; g++) {
					if(g != slice) {
						geometry[g] = createGeometry(g, color);
						IJ.showProgress(g + 1, volume.zDim);
					}
				}
			}
		}.start();
	}

	/**
	 * Changes the color of this surface plot. If null, the z value
	 * is color coded.
	 * @param color
	 */
	public void setColor(Color3f color) {
		for(int g = 0; g < geometry.length; g++) {
			int N = geometry[g].getVertexCount();
			Color3f colors[] = new Color3f[N];
			Point3f coord = new Point3f();
			for(int i = 0; i < N; i++) {
				geometry[g].getCoordinate(i, coord);
				colors[i] = color != null ? color
					: new Color3f(Color.getHSBColor(
						coord.z / maxZ, 1, 1));
			}
			geometry[g].setColors(0, colors);
		}
	}

	/**
	 * Shade the surface or not.
	 * @param b
	 */
	public void setShaded(boolean b) {
		PolygonAttributes pa = appearance.getPolygonAttributes();
		if(b)
			pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	/**
	 * Store the minimum, maximum and center coordinate in the given
	 * points.
	 * @param min
	 * @param max
	 * @param center
	 */
	void calculateMinMaxCenterPoint(Point3d min, 
				Point3d max, Point3d center) {

		min.x = 0; min.y = 0; min.z = 0;
		max.x = volume.xDim * pw; max.y = volume.yDim * ph; max.z = maxZ;
		center.x = max.x / 2;
		center.y = max.y / 2;
		center.z = max.z / 2;
	}

	/**
	 * Calculate the maximum intensity value in the image data.
	 */
	private void calculateMax() {
		maxVal = 0;
		for(int z = 0; z < volume.zDim; z++) {
			for(int y = 0; y < volume.yDim; y++) {
				for(int x = 0; x < volume.xDim; x++) {
					int v = (0xff & volume.getAverage(x, y, z));
					if(v > maxVal)
						maxVal = v;
				}
			}
		}
	}

	/**
	 * Automatically calculate a suitable z-factor.
	 * The intensity values are multiplied with this factor.
	 */
	private void calculateZFactor() {
		float realW = volume.xDim * pw;
		float realH = volume.yDim * ph;
		maxZ = realW > realH ? realW : realH;
		zFactor = maxZ / maxVal;
	}

	/**
	 * Create the appearance.
	 * @return
	 */
	private static Appearance createAppearance (float transparency) {
		Appearance app = new Appearance();
		app.setCapability(Appearance.
					ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		app.setPolygonAttributes(polyAttrib);

		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
// 		colorAttrib.setColor(color);
		app.setColoringAttributes(colorAttrib);

		TransparencyAttributes tr = new TransparencyAttributes();
		int mode = transparency == 0f ? TransparencyAttributes.NONE
					: TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		app.setTransparencyAttributes(tr);

		Material material = new Material();
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.5f,0.5f,0.5f);
		material.setDiffuseColor(0.1f,0.1f,0.1f);
		app.setMaterial(material);
		return app;
	}

	/**
	 * Create the geometry for the specified slice
	 * @param g
	 * @return
	 */
	private IndexedQuadArray createGeometry(int g, Color3f color) {

		int nQuads = (volume.xDim - 1) * (volume.yDim - 1);
		int nIndices = volume.xDim * volume.yDim;
		int nVertices = nQuads * 4;

		IndexedQuadArray ta = new IndexedQuadArray (nIndices,
					TriangleArray.COORDINATES |
					TriangleArray.COLOR_3 |
					TriangleArray.NORMALS,nVertices);

		Point3f[] coords = new Point3f[nIndices];
		Color3f colors[] = new Color3f[nIndices];
		for(int i = 0; i < nIndices; i++) {
			float y = ph * (i / volume.xDim);
			float x = pw * (i % volume.xDim);
			float v = zFactor * (0xff & volume.getAverage(
				i % volume.xDim, i / volume.xDim, g));
			coords[i] = new Point3f(x, y, v);
			int c = volume.loadWithLUT(i % volume.xDim, i / volume.xDim, g);
			colors[i] = new Color3f(((c >> 16) & 0xff) / 255f, ((c >> 8) & 0xff) / 255f, (c & 0xff) / 255f);
		}
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);


		int[] indices = new int[nVertices];
		int index = 0;
		for(int y = 0; y < volume.yDim - 1; y++) {
			for(int x = 0; x < volume.xDim - 1; x++) {
				indices[index++] = y * volume.xDim + x;
				indices[index++] = (y+1) * volume.xDim + x;
				indices[index++] = (y+1) * volume.xDim + x+1;
				indices[index++] = y * volume.xDim + x+1;
			}
		}
		ta.setCoordinateIndices(0, indices);
		ta.setColorIndices(0, indices);

		// initialize the geometry info here
		GeometryInfo gi = new GeometryInfo(ta);
		// generate normals
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		IndexedQuadArray result = (IndexedQuadArray)gi
					.getIndexedGeometryArray();
		result.setCapability(TriangleArray.ALLOW_COLOR_WRITE);
		result.setCapability(TriangleArray.ALLOW_COUNT_READ);
		result.setCapability(TriangleArray.ALLOW_INTERSECT);

		return result;
	}
}
