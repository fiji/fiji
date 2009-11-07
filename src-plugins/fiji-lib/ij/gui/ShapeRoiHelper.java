package ij.gui;

import java.awt.Graphics;
import java.awt.Shape;


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
		return roi.getShape();
	}
}
