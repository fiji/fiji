package ij.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * Wrapper to extract the java.awt.Shape from a ShapeRoi
 *  
 * @author Ignacio Arganda-Carreras and Johannes Schindelin
 *
 */
public class ShapeRoiHelper extends ShapeRoi {
	/** Generated serial version uid */
	private static final long serialVersionUID = 3683518238872064558L;

	private ShapeRoiHelper() { super((Roi)null); }

	// unfortunately, this method is not public in (old) ImageJ
	public static Shape getShape(ShapeRoi roi) { return roi.getShape(); }

	public static Shape getShape(ShapeRoi roi, Graphics g, int x, int y, double magnification) {
		final AffineTransform transform = (((Graphics2D)g).getDeviceConfiguration()).getDefaultTransform();
		final Rectangle r = roi.getBounds();
		transform.setTransform(magnification, 0.0, 0.0, magnification, x - r.x * magnification, y - r.y * magnification);
		return transform.createTransformedShape(roi.getShape());
	}
}