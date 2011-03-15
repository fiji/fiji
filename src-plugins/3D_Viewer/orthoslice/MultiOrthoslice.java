package orthoslice;

import java.util.Arrays;
import java.util.BitSet;
import ij.ImagePlus;
import voltex.VolumeRenderer;
import javax.media.j3d.Group;
import javax.media.j3d.Switch;
import javax.media.j3d.View;
import javax.vecmath.Color3f;

public class MultiOrthoslice extends VolumeRenderer {

	/** The dimensions in x-, y- and z- direction */
	private int[] dimensions = new int[3];

	/** The visible children of the axis Switch in VolumeRenderer */
	private BitSet whichChild;

	/** Half the whichChild.size(), or the sum of the number of slices
	 * in all three dimensions. */
	private int sliceSum;

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
		appCreator.setOpaqueTextures(true);
		dimensions[0] = img.getWidth();
		dimensions[1] = img.getHeight();
		dimensions[2] = img.getStackSize();

		this.sliceSum = dimensions[0] + dimensions[1] + dimensions[2];

		// double, there are FRONT and BACK slices
		whichChild = new BitSet(2 * sliceSum);
		whichChild.set(0, 2 * sliceSum);
	}

	/**
	 * Overwrites loadAxis() in VolumeRenderer to show only a few planes
	 * in each direction.
	 * @param axis Must be one of X_AXIS, Y_AXIS or Z_AXIS in
	 * VolumeRendConstants.
	 */
	@Override
	protected void loadAxis(int axis) {
		Group front = (Group)axisSwitch.getChild(axisIndex[axis][FRONT]);
		Group back  = (Group)axisSwitch.getChild(axisIndex[axis][BACK]);
		for (int i=0; i<dimensions[axis]; i++) {
			loadAxis(axis, i, front, back);
		}
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

	private int offset(int axis) {
		int offset = 0;
		switch (axis) {
			case 0: break;
			case 1: offset = dimensions[0]; break;
			case 2: offset = dimensions[1] + dimensions[1]; break;
		}
		return offset;
	}

	/** Hide/show the whole set of slices in the given axis. */
	public void setVisible(int axis, boolean b) {
		int offset = offset(axis);
		// front:
		whichChild.set(offset, offset + dimensions[axis]);
		// back:
		whichChild.set(sliceSum + offset, sliceSum + offset + dimensions[axis]);
		axisSwitch.setChildMask(whichChild);
	}

	/** Show a slice every {@param interval} slices, and hide the rest.
	 * Starts by showing slice at {@param offset}, and counts slices up to {@param range}.
	 */
	public void setVisible(int axis, int interval, int offset, int range) {
		for (int i=offset(axis) + offset, k=0; k < (dimensions[axis] - offset) && k<range; ++k, ++i) {
			boolean b = 0 == k % interval;
			whichChild.set(i, b);
			whichChild.set(sliceSum + i, b);
		}
		axisSwitch.setChildMask(whichChild);
	}

	public void setVisible(int axis, boolean[] b) {
		int end = Math.min(b.length, dimensions[axis]);
		for (int i=offset(axis), k=0; k<end; ++i, ++k) {
			whichChild.set(i, b[k]);
			whichChild.set(sliceSum + i, b[k]);
		}
		axisSwitch.setChildMask(whichChild);
	}

	/**
	 * Translate the visibility state along the given axis.
	 * @param axis
	 */
	public void translateVisibilityState(int axis, int shift) {
		int first = offset(axis);
		int last = first + dimensions[axis] - 1;
		BitSet c = whichChild.get(first, first + dimensions[axis]);
		whichChild.clear(first, first + dimensions[axis]);
		for (int i=first; i<last; ++i) {
			int target = i + shift;
			if (target < first || target > last) continue;
			whichChild.set(i);
			whichChild.set(sliceSum + i);
		}
		axisSwitch.setChildMask(whichChild);
	}
}
