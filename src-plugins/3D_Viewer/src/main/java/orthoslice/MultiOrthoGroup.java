package orthoslice;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentInstant;

import vib.NaiveResampler;
import voltex.VoltexGroup;
import javax.media.j3d.View;

/**
 * This class extends VoltexGroup. Instead of a whole volume, it shows
 * only a subset of orthogonal slices.
 * Each of the slices can be shown or hidden.
 */
public class MultiOrthoGroup extends VoltexGroup {

	/**
	 * Construct a MultiOrthoGroup from the given Content.
	 * @param c
	 */
	public MultiOrthoGroup(Content c) {
		this(c.getCurrent());
	}

	/**
	 * Construct a MultiOrthoGroup from the given ContentInstant.
	 * @param c
	 */
	public MultiOrthoGroup(ContentInstant c) {
		super();
		this.c = c;
		ImagePlus imp = c.getResamplingFactor() == 1 ? c.getImage() 
			: NaiveResampler.resample(c.getImage(),
				c.getResamplingFactor());
		renderer = new MultiOrthoslice(imp, c.getColor(), 
				c.getTransparency(), c.getChannels());
		renderer.fullReload();
		calculateMinMaxCenterPoint();
		addChild(renderer.getVolumeNode());
	}

	/**
	 * Returns whether the textures are transparent or not.
	 */
	public boolean getTexturesOpaque() {
		return ((MultiOrthoslice)renderer).getTexturesOpaque();
	}

	/**
	 * Makes the textures transparent or not.
	 */
	public void setTexturesOpaque(boolean opaque) {
		((MultiOrthoslice)renderer).setTexturesOpaque(opaque);
	}

	/** Show/hide all slices in the given axis. */
	public void setVisible(int axis, boolean b) {
		((MultiOrthoslice)renderer).setVisible(axis, b);
	}

	/**
	 * Show a slice every {@param interval} slices, and hide the rest.
	 * Starts from the first slice.
	 * @param axis
	 * @param interval
	 */
	public void setVisible(int axis, int interval, int offset, int range) {
		((MultiOrthoslice)renderer).setVisible(axis, interval, offset, range);
	}

	public int getSliceCount(int axis) {
		return ((MultiOrthoslice)renderer).getSliceCount(axis);
	}

	/**
	 * Set the visibility of each slice in the given axis.
	 * @param axis
	 * @param b
	 */
	public void setVisible(int axis, boolean[] b) {
		((MultiOrthoslice)renderer).setVisible(axis, b);
	}

	/**
	 * Translate the visibility state along the given axis.
	 * @param axis
	 */
	public void translateVisibilityState(int axis, int shift) {
		((MultiOrthoslice)renderer).translateVisibilityState(axis, shift);
	}

	/**
	 * Override eyePtChanged() in VolumeRenderer to show a subset of slices only.
	 * @param view
	 */
	@Override
	public void eyePtChanged(View view) {
		((MultiOrthoslice)renderer).eyePtChanged(view);
	}
}
