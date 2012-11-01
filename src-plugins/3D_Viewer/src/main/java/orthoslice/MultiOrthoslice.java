package orthoslice;

import java.util.Arrays;
import java.util.BitSet;
import ij.ImagePlus;
import voltex.VolumeRenderer;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.Appearance;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Switch;
import javax.media.j3d.View;
import javax.vecmath.Color3f;

public class MultiOrthoslice extends VolumeRenderer {

	/**
	 * For each axis, a boolean array indicating if the slice should
	 * be visible
	 */
	private boolean[][] slices = new boolean[3][];

	/** The dimensions in x-, y- and z- direction */
	private int[] dimensions = new int[3];

	/** The visible children of the axis Switch in VolumeRenderer */
	private BitSet whichChild = new BitSet(6);

	/**
	 * @param img The image stack
	 * @param color The color this Orthoslice should use
	 * @param tr The transparency of this Orthoslice
	 * @param channels A boolean[] array which indicates which color channels
	 * to use (only affects RGB images). The length of the array must be 3.
	 */
	public MultiOrthoslice(ImagePlus img, Color3f color, 
					float tr, boolean[] channels) {
		super(img, color, tr, channels);
		getVolume().setAlphaLUTFullyOpaque();
		dimensions[0] = img.getWidth();
		dimensions[1] = img.getHeight();
		dimensions[2] = img.getStackSize();

		for(int i = 0; i < 3; i++) {
			// by default, show only the middle slice
			slices[i] = new boolean[dimensions[i]];
			slices[i][dimensions[i] / 2] = true;;
			whichChild.set(i, true);
			whichChild.set(i + 3, true);
		}
	}

	/**
	 * Returns whether the textures are transparent or not.
	 */
	public boolean getTexturesOpaque() {
		return appCreator.getOpaqueTextures();
	}

	/**
	 * Makes the textures transparent or not.
	 */
	public void setTexturesOpaque(boolean opaque) {
		boolean before = appCreator.getOpaqueTextures();
		if(before != opaque) {
			appCreator.setOpaqueTextures(opaque);
			fullReload();
		}
	}

	/**
	 * Overwrites loadAxis() in VolumeRenderer to skip the slices
	 * for which the visibility flag is not set.
	 * @param axis Must be one of X_AXIS, Y_AXIS or Z_AXIS in
	 * VolumeRendConstants.
	 * @param index The index within the axis
	 * @param front the front group
	 * @param back the back group
	 */
	@Override
	protected void loadAxis(int axis, int index, Group front, Group back) {
		if(slices[axis][index])
			super.loadAxis(axis, index, front, back);
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

	public int getSliceCount(int axis) {
		return dimensions[axis];
	}

	public void setVisible(int axis, boolean[] b) {
		// cache existing children in the front group
		BranchGroup[] cachedFrontGroups =
			new BranchGroup[dimensions[axis]];
		int axisFront = axisIndex[axis][FRONT];
		Group frontGroup = (Group)axisSwitch.getChild(axisFront);
		int groupIndex = 0;
		for(int i = 0; i < slices[axis].length; i++)
			if(slices[axis][i])
				cachedFrontGroups[i] = (BranchGroup)
					frontGroup.getChild(groupIndex++);
		frontGroup.removeAllChildren();

		// cache existing children in the back group
		BranchGroup[] cachedBackGroups  =
			new BranchGroup[dimensions[axis]];
		int axisBack = axisIndex[axis][BACK];
		Group backGroup = (Group)axisSwitch.getChild(axisBack);
		groupIndex = backGroup.numChildren() - 1;
		for(int i = 0; i < slices[axis].length; i++)
			if(slices[axis][i])
				cachedBackGroups[i] = (BranchGroup)
					backGroup.getChild(groupIndex--);
		backGroup.removeAllChildren();

		for(int i = 0; i < slices[axis].length; i++) {
			slices[axis][i] = b[i];

			if(!slices[axis][i])
				continue;

			// see if we have something in the cache
			BranchGroup frontShapeGroup = cachedFrontGroups[i];
			BranchGroup backShapeGroup = cachedBackGroups[i];

			// if not cached, create it
			if(frontShapeGroup == null || backShapeGroup == null) {
				GeometryArray quadArray = geomCreator.getQuad(axis, i);
				Appearance a = appCreator.getAppearance(axis, i);

				Shape3D frontShape = new Shape3D(quadArray, a);
				frontShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
				frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

				frontShapeGroup = new BranchGroup();
				frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
				frontShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
				frontShapeGroup.addChild(frontShape);

				Shape3D backShape = new Shape3D(quadArray, a);
				backShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
				backShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				backShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

				backShapeGroup = new BranchGroup();
				backShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
				backShapeGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
				backShapeGroup.addChild(backShape);
			}

			// add the groups to the appropriate axis
			frontGroup.addChild(frontShapeGroup);
			backGroup.insertChild(backShapeGroup, 0);
		}
	}

	/** Hide/show the whole set of slices in the given axis. */
	public void setVisible(int axis, boolean b) {
		boolean[] bs = new boolean[dimensions[axis]];
		Arrays.fill(bs, b);
		setVisible(axis, bs);
	}

	/** Show a slice every {@param interval} slices, and hide the rest.
	 * Starts by showing slice at {@param offset}, and counts slices up to {@param range}.
	 */
	public void setVisible(int axis, int interval, int offset, int range) {
		boolean[] bs = new boolean[dimensions[axis]];
		for (int i = offset, k = 0; k < (dimensions[axis] - offset) && k < range; ++k, i += interval)
			bs[i] = true;
		setVisible(axis, bs);
	}

	/**
	 * Translate the visibility state along the given axis.
	 * @param axis
	 */
	public void translateVisibilityState(int axis, int shift) {
		boolean[] bs = new boolean[dimensions[axis]];
		int first = 0;
		int len = dimensions[axis];
		for (int i = 0; i < dimensions[axis]; ++i) {
			int target = i + shift;
			if (target < 0 || target > dimensions[axis])
				continue;
			bs[target] = slices[axis][i];
		}
		setVisible(axis, bs);
	}
}
