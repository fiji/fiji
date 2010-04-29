package octree;

import java.awt.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import ij3d.AxisConstants;

public class AppearanceCreator implements AxisConstants {

	private static final int TEX_MODE = Texture.INTENSITY;
	private static final int COMP_TYPE = ImageComponent.FORMAT_CHANNEL8;
	private static final boolean BY_REF = true;
	private static final boolean Y_UP = true;
	private static final int SIZE = VolumeOctree.SIZE;

	private TextureAttributes texAttr;
	private TransparencyAttributes transAttr;
	private PolygonAttributes polyAttr;
	private Material material;
	private ColoringAttributes colAttr;
	private RenderingAttributes rendAttr;

	private static AppearanceCreator instance;

	private AppearanceCreator() {
		initAttributes(null, 0.1f);
	}

	public static AppearanceCreator instance() {
		if(instance == null)
			instance = new AppearanceCreator();
		return instance;
	}

	public Appearance getAppearance(CubeData cdata, int index) {
		Appearance a = new Appearance();
		a.setMaterial(material);
		a.setTransparencyAttributes(transAttr);
		a.setPolygonAttributes(polyAttr);
		a.setColoringAttributes(colAttr);
		a.setRenderingAttributes(rendAttr);

		a.setTexture(getTexture(cdata, index));
		a.setTexCoordGeneration(cdata.tg);
		a.setTextureAttributes(texAttr);
		return a;
	}

	public void setTransparency(float f) {
		transAttr.setTransparency(f);
	}

	public void setThreshold(float f) {
		rendAttr.setAlphaTestValue(f);
	}

	public void setColor(Color3f c) {
		colAttr.setColor(c);
	}

	private Texture2D getTexture(CubeData cdata, int index) {
		Texture2D tex = new Texture2D(Texture.BASE_LEVEL, TEX_MODE, SIZE, SIZE);
		ImageComponent2D pArray = new ImageComponent2D(COMP_TYPE, SIZE, SIZE, BY_REF, Y_UP);
		pArray.set(cdata.images[index]);

		tex.setImage(0, pArray);
		tex.setEnable(true);
		tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
		tex.setMagFilter(Texture.BASE_LEVEL_LINEAR);

		tex.setBoundaryModeS(Texture.CLAMP);
		tex.setBoundaryModeT(Texture.CLAMP);
		return tex;
	}

	private void initAttributes(Color3f color, float transparency) {
		texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.COMBINE);
		texAttr.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
		texAttr.setPerspectiveCorrectionMode(TextureAttributes.NICEST);

		transAttr = new TransparencyAttributes();
		transAttr.setTransparency(0.1f);
		transAttr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transAttr.setTransparencyMode(TransparencyAttributes.BLENDED);
		transAttr.setTransparency(transparency);

		polyAttr = new PolygonAttributes();
		polyAttr.setCullFace(PolygonAttributes.CULL_NONE);

		material = new Material();
		material.setLightingEnable(false);

		colAttr = new ColoringAttributes();
		colAttr.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		colAttr.setShadeModel(ColoringAttributes.NICEST);
		if(color == null) {
			colAttr.setColor(1f, 1f, 1f);
		} else {
			colAttr.setColor(color);
		}

		// Avoid rendering of voxels having an alpha value of zero
		rendAttr = new RenderingAttributes();
		rendAttr.setDepthTestFunction(RenderingAttributes.ALWAYS);
		rendAttr.setCapability(
			RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
 		rendAttr.setAlphaTestValue(0f);
		rendAttr.setAlphaTestFunction(RenderingAttributes.GREATER);
	}
}
