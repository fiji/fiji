package voltex;

import ij3d.AxisConstants;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector4f;


/**
 * This class is a helper class whose main task is to create Appearance
 * objects for a specified axis and direction.
 * Each time when ask for an Appearance object, a new Appearance is created.
 * This is necessary, since each slice has another texture. However, all
 * the Appearance objects created by one instance of this class share their
 * Appearance attributes. In this way, it is easy and fast to change color,
 * transparency, etc.
 * 
 * @author Benjamin Schmid
 */
public class AppearanceCreator implements AxisConstants {

	/** The volume from which the textures are created */
	private VoltexVolume volume;

	/** Texture mode, e.g. Texture.RGB or so */
	private int textureMode;

	/** Indicates if transparent or opaque texture modes should be used */
	private boolean opaque = false;

	/** TexCoordGeneration object for x direction */
	private TexCoordGeneration xTg;
	/** TexCoordGeneration object for y direction */
	private TexCoordGeneration yTg;
	/** TexCoordGeneration object for z direction */
	private TexCoordGeneration zTg;

	/** texture attributes */
	private TextureAttributes texAttr;
	/** transparency attributes */
	private TransparencyAttributes transAttr;
	/** polygon attributes */
	private PolygonAttributes polyAttr;
	/** material */
	private Material material;
	/** color attributes */
	private ColoringAttributes colAttr;
	/** rendering attributes */
	private RenderingAttributes rendAttr;

	/**
	 * Constructor.
	 * Initializes this AppearanceCreator with the given image data.
	 * @param volume
	 */
	public AppearanceCreator(VoltexVolume volume) {
		this(volume, null, 0.1f);
	}

	/**
	 * Initializes this AppearanceCreator with the given image data,
	 * color and transparency values.
	 * @param volume
	 * @param color
	 * @param transparency
	 */
	public AppearanceCreator(VoltexVolume volume,
			Color3f color, float transparency) {
		initAttributes(color, transparency);
		setVolume(volume);
	}

	/**
	 * Release all stored data.
	 */
	public void release() {
		xTg = null; yTg = null; zTg = null;
		volume = null;
	}

	/**
	 * Change the image data of this AppearanceCreator
	 * @param v
	 */
	public void setVolume(VoltexVolume v) {
		this.volume = v;
		zTg = new TexCoordGeneration();
		zTg.setPlaneS(new Vector4f(v.xTexGenScale, 0f, 0f,
				// move it to pixel center
				(float)(0.5f * v.pw * v.xTexGenScale)
				// translate it to the origin
				-(float)(v.xTexGenScale * v.minCoord.x)));
		zTg.setPlaneT(new Vector4f(0f, v.yTexGenScale, 0f,
				(float)(0.5f * v.ph * v.yTexGenScale)
				-(float)(v.yTexGenScale * v.minCoord.y)));
		yTg = new TexCoordGeneration();
		yTg.setPlaneS(new Vector4f(v.xTexGenScale, 0f, 0f,
				(float)(0.5f * v.pw * v.xTexGenScale)
				-(float)(v.xTexGenScale * v.minCoord.x)));
		yTg.setPlaneT(new Vector4f(0f, 0f, v.zTexGenScale,
				(float)(0.5f * v.pd * v.zTexGenScale)
				-(float)(v.zTexGenScale * v.minCoord.z)));
		xTg = new TexCoordGeneration();
		xTg.setPlaneS(new Vector4f(0f, v.yTexGenScale, 0f,
				(float)(0.5f * v.ph * v.yTexGenScale)
				-(float)(v.yTexGenScale * v.minCoord.y)));
		xTg.setPlaneT(new Vector4f(0f, 0f, v.zTexGenScale,
				(float)(0.5f * v.pd * v.zTexGenScale)
				-(float)(v.zTexGenScale * v.minCoord.z)));
		updateTextureMode();
	}

	/**
	 * This flag indicates whether textures are opaque or not.
	 * This changes effectively the texture mode, (depending on whether
	 * RGB or 8-bit textures are used) between RGB <-> RGBA, or
	 * LUMINANCE <-> INTENSITY.
	 * This only effects newly loaded textures.
	 * Opaque textures are for example needed for the orthoslices.
	 */
	public void setOpaqueTextures(boolean opaque) {
		if(this.opaque != opaque) {
			this.opaque = opaque;
			updateTextureMode();
		}
	}

	/**
	 * Update the texture mode, after the volume has changed.
	 */
	public void updateTextureMode() {
		boolean rgb = volume.getDataType() == VoltexVolume.INT_DATA;

		if(rgb) {
			textureMode = opaque ? Texture.RGB : Texture.RGBA;
		} else {
			textureMode = opaque ? Texture.LUMINANCE : Texture.INTENSITY;
		}
	}

	/**
	 * Returns the Appearance object for the specified direction and index.
	 * This is composed of the shared Appearance attributes plus the
	 * individual textures.
	 * @param direction
	 * @param index
	 * @return
	 */
	public Appearance getAppearance(int direction, int index) {
		Appearance a = new Appearance();
		a.setCapability(Appearance.ALLOW_TEXTURE_UNIT_STATE_WRITE);
		a.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
		a.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
		a.setMaterial(material);
		a.setTransparencyAttributes(transAttr);
		a.setPolygonAttributes(polyAttr);
		a.setColoringAttributes(colAttr);
		a.setRenderingAttributes(rendAttr);

		TextureUnitState[] tus = new TextureUnitState[2];
		tus[0] = new TextureUnitState(
			getTexture(direction, index, volume),
			texAttr,
			getTg(direction));
		tus[1] = null;
		a.setTextureUnitState(tus);
		return a;
	}

	/**
	 * Set the transparency for all the textures that were loaded by this
	 * AppearanceCreator.
	 * @param f
	 */
	public void setTransparency(float f) {
		transAttr.setTransparency(f);
	}

	/**
	 * Set the threshold for all the textures that were loaded by this
	 * AppearanceCreator.
	 * Pixel values below the threshold are not rendered.
	 * @param f
	 */
	public void setThreshold(float f) {
		rendAttr.setAlphaTestValue(f);
	}

	/**
	 * Set the color for all the textures that were loaded by this
	 * AppearanceCreator.
	 * Pixel values below the threshold are not rendered.
	 * @param f
	 */
	public void setColor(Color3f c) {
		colAttr.setColor(c);
	}

	/**
	 * Returns the texture for the specified axis and slice
	 * @param axis
	 * @param index
	 * @return
	 */
	public Texture2D getTexture(int axis, int index) {
		return getTexture(axis, index, volume);
	}

	/**
	 * Returns the texture for the specified axis and slice
	 * @param axis
	 * @param index
	 * @param vol
	 * @return
	 */
	public Texture2D getTexture(int axis, int index, VoltexVolume vol) {
		int sSize = 0, tSize = 0;
		ImageComponent2D pArray = null;
		switch (axis) {
			case Z_AXIS:
				sSize = vol.xTexSize;
				tSize = vol.yTexSize;
				pArray = vol.getImageComponentZ(index);
				break;
			case Y_AXIS:
				sSize = vol.xTexSize;
				tSize = vol.zTexSize;
				pArray = vol.getImageComponentY(index);
				break;
			case X_AXIS:
				sSize = vol.yTexSize;
				tSize = vol.zTexSize;
				pArray = vol.getImageComponentX(index);
				break;
		}
		Texture2D tex = new Texture2D(Texture.BASE_LEVEL,
			textureMode, sSize, tSize);

		tex.setImage(0, pArray);
		tex.setEnable(true);
		tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
		tex.setMagFilter(Texture.BASE_LEVEL_LINEAR);

		tex.setBoundaryModeS(Texture.CLAMP);
		tex.setBoundaryModeT(Texture.CLAMP);
		return tex;
	}

	/**
	 * Get the TextureGeneration of the specified direction
	 * @param direction
	 * @param index
	 * @return
	 */
	public TexCoordGeneration getTg(int direction) {
		switch(direction) {
			case X_AXIS: return xTg;
			case Y_AXIS: return yTg;
			case Z_AXIS: return zTg;
		}
		return null;
	}

	/**
	 * Initialize the Appearance attributes.
	 * @param color
	 * @param transparency
	 */
	private void initAttributes(Color3f color, float transparency) {

		texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.MODULATE);
		texAttr.setPerspectiveCorrectionMode(TextureAttributes.NICEST);

		transAttr = new TransparencyAttributes();
		transAttr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transAttr.setTransparencyMode(TransparencyAttributes.BLENDED);
		transAttr.setSrcBlendFunction(TransparencyAttributes.BLEND_SRC_ALPHA);
		transAttr.setDstBlendFunction(TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA);
		transAttr.setTransparency(transparency);

		polyAttr = new PolygonAttributes();
		polyAttr.setCullFace(PolygonAttributes.CULL_NONE);

		material = new Material();
		material.setLightingEnable(true);

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
		rendAttr.setCapability(
			RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
 		rendAttr.setAlphaTestValue(0.1f);
		rendAttr.setAlphaTestFunction(RenderingAttributes.GREATER);
	}
}
