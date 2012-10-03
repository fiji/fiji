package orthoslice;

import java.awt.*;
import java.awt.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import ij.ImagePlus;
import java.util.BitSet;

import voltex.VolumeRenderer;

/**
 * Orthoslice extends VolumeRenderer and modifies it in a way so that
 * not the whole volume is displayed, but only three orthogonal slices
 * through the volume.
 * 
 * @author Benjamin Schmid
 */
public class Orthoslice extends VolumeRenderer {

	/** The indices of the currently displayed slices */
	private int[] slices = new int[3];

	/** The dimensions in x-, y- and z- direction */
	private int[] dimensions = new int[3];

	/** Flag indicating which planes are visible */
	private boolean[] visible = new boolean[3];

	/** The visible children of the axis Switch in VolumeRenderer */
	private BitSet whichChild = new BitSet(6);

	/**
	 * Initializes a new Orthoslice with the given image, color, transparency
	 * and channels. By default, the slice indices go through the center
	 * of the image stack.
	 * 
	 * @param img The image stack
	 * @param color The color this Orthoslice should use
	 * @param tr The transparency of this Orthoslice
	 * @param channels A boolean[] array which indicates which color channels
	 * to use (only affects RGB images). The length of the array must be 3.
	 */
	public Orthoslice(ImagePlus img, Color3f color, 
					float tr, boolean[] channels) {
		super(img, color, tr, channels);
		getVolume().setAlphaLUTFullyOpaque();
		dimensions[0] = img.getWidth();
		dimensions[1] = img.getHeight();
		dimensions[2] = img.getStackSize();
		for(int i = 0; i < 3; i++) {
			slices[i] = dimensions[i] / 2;
			visible[i] = true;
			whichChild.set(i, true);
			whichChild.set(i+3, true);
		}
	}

	/**
	 * Overwrites loadAxis() in VolumeRenderer to show only one plane
	 * in each direction.
	 * @param axis Must be one of X_AXIS, Y_AXIS or Z_AXIS in
	 * VolumeRendConstants.
	 */
	@Override
	protected void loadAxis(int axis) {

		Group front = (Group)axisSwitch.getChild(axisIndex[axis][FRONT]);
		Group back  = (Group)axisSwitch.getChild(axisIndex[axis][BACK]);
		int i = slices[axis];
		loadAxis(axis, i, front, back);
	} 

	/**
	 * Override eyePtChanged() in VolumeRenderer to always show all
	 * slices.
	 * @param view
	 */
	@Override
	public void eyePtChanged(View view) {
		axisSwitch.setWhichChild(Switch.CHILD_MASK);
		axisSwitch.setChildMask(whichChild);
	}

	/**
	 * Returns the current index of the specified plane
	 * @param axis
	 * @return
	 */
	public int getSlice(int axis) {
		return slices[axis];
	}

	/**
	 * Returns whether the specified plane is visible at the moment
	 * @param axis
	 * @return
	 */
	public boolean isVisible(int axis) {
		return visible[axis];
	}

	/**
	 * Sets the specified plane visible.
	 * @param axis
	 * @param b
	 */
	public void setVisible(int axis, boolean b) {
		if(visible[axis] != b) {
			visible[axis] = b;
			whichChild.set(axisIndex[axis][FRONT], b);
			whichChild.set(axisIndex[axis][BACK], b);
			axisSwitch.setChildMask(whichChild);
		}
	}

	/**
	 * Decreases the index of the specified plane by one.
	 * @param axis
	 */
	public void decrease(int axis) {
		setSlice(axis, slices[axis]-1);
	}

	/**
	 * Increases the index of the specified plane by one.
	 * @param axis
	 */
	public void increase(int axis) {
		setSlice(axis, slices[axis]+1);
	}

	/**
	 * Sets the slice index of the specified plane to the given value.
	 * @param axis
	 * @param v
	 */
	public void setSlice(int axis, int v) {
		if(v >= dimensions[axis] || v < 0)
			return;
		slices[axis] = v;
		Group g = (Group)axisSwitch.getChild(axisIndex[axis][FRONT]);
		int num = g.numChildren();
		if(num > 1) 
			System.out.println(num + " children, expected only 1");
		Shape3D shape = (Shape3D)
			((Group)g.getChild(num-1)).getChild(0);

		double[] quadCoords = geomCreator.getQuadCoords(axis, v);
		((QuadArray)shape.getGeometry()).setCoordinates(0, quadCoords);

		Texture2D tex = appCreator.getTexture(axis, v);
		shape.getAppearance().setTexture(tex);
		TexCoordGeneration tg = appCreator.getTg(axis);
		shape.getAppearance().setTexCoordGeneration(tg);
	}
}
