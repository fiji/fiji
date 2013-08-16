package ij3d.behaviors;

import javax.media.j3d.Transform3D;

/**
 * @author Benjamin Schmid
 */
public interface BehaviorCallback {

	public static final int ROTATE = 0;
	public static final int TRANSLATE = 0;

	/**
	 * Called when the transformation of a Content or the view
	 * has changed.
	 * @param type
	 * @param t
	 */
	public void transformChanged(int type, Transform3D t);

}
