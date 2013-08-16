package orthoslice;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentInstant;

import vib.NaiveResampler;
import voltex.*;

/**
 * This class extends VoltexGroup. Instead of a whole volume, it shows
 * only three orthogonal slices, one in xy-, xz-, and yz-direction.
 * Each of the slices can be shown or hidden. Additionally, this class
 * offers methods to alter the position of each of the three slices.
 * 
 * @author Benjamin Schmid
 */
public class OrthoGroup extends VoltexGroup {

	/**
	 * Construct a OrthoGroup from the given Content.
	 * @param c
	 */
	public OrthoGroup(Content c) {
		this(c.getCurrent());
	}

	/**
	 * Construct a OrthoGroup from the given ContentInstant.
	 * @param c
	 */
	public OrthoGroup(ContentInstant c) {
		super();
		this.c = c;
		ImagePlus imp = c.getResamplingFactor() == 1 ? c.getImage() 
			: NaiveResampler.resample(c.getImage(),
				c.getResamplingFactor());
		renderer = new Orthoslice(imp, c.getColor(), 
				c.getTransparency(), c.getChannels());
		renderer.fullReload();
		calculateMinMaxCenterPoint();
		addChild(renderer.getVolumeNode());
	}

	/**
	 * Alter the slice index of the given direction.
	 * @param axis
	 * @param v
	 */
	public void setSlice(int axis, int v) {
		((Orthoslice)renderer).setSlice(axis, v);
	}

	/**
	 * Get the slice index of the specified axis
	 * @param axis
	 * @return
	 */
	public int getSlice(int axis) {
		return ((Orthoslice)renderer).getSlice(axis);
	}

	/**
	 * Alter the slice index of the given direction.
	 * @param axis
	 */
	public void decrease(int axis) {
		((Orthoslice)renderer).decrease(axis);
	}

	/**
	 * Alter the slice index of the given direction.
	 * @param axis
	 */
	public void increase(int axis) {
		((Orthoslice)renderer).increase(axis);
	}

	/**
	 * Returns true if the slice in the given direction is visible.
	 */
	public boolean isVisible(int i) {
		return ((Orthoslice)renderer).isVisible(i);
	}

	/**
	 * Set the specified slice visible
	 * @param axis
	 * @param b
	 */
	public void setVisible(int axis, boolean b) {
		((Orthoslice)renderer).setVisible(axis, b);
	}
}

