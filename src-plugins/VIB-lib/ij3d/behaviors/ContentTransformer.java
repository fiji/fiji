package ij3d.behaviors;

import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;
import java.awt.event.MouseEvent;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * This class is a helper class which transforms MouseEvents
 * to an appropriate transformation of the selected Content.
 * 
 * @author Benjamin Schmid
 */
public class ContentTransformer {

	private Initializer initializer;

	private DefaultUniverse univ;
	private ImageCanvas3D canvas;
	private BehaviorCallback callback;
	private Content content;

	private Vector3d axisPerDx = new Vector3d();
	private Vector3d axisPerDy = new Vector3d();
	private double anglePerPix;
	

	private AxisAngle4d aaX = new AxisAngle4d();
	private AxisAngle4d aaY = new AxisAngle4d();
	private Transform3D transX = new Transform3D();
	private Transform3D transY = new Transform3D();

	private Transform3D transl = new Transform3D();
	private Transform3D transl_inv = new Transform3D();

	private Vector3d translationPerDx = new Vector3d();
	private Vector3d translationPerDy = new Vector3d();

	private TransformGroup translateTG, rotateTG;
		
	private int xLast, yLast;

	/**
	 * Constructs a new ContentTransformer.
	 * @param univ
	 * @param callback
	 */
	public ContentTransformer(DefaultUniverse univ, BehaviorCallback callback) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
		this.callback = callback;
		this.initializer = new Initializer();
	}

	/**
	 * This method should be called to initiate a new transformation, e.g.
	 * when the mouse is pressed before rotation or translation.
	 * @param c
	 * @param x
	 * @param y
	 */
	public void init(Content c, int x, int y) {
		initializer.init(c, x, y);
	}

	/**
	 * Translate the selected Content suitably to the specified MouseEvent.
	 * @param e
	 */
	public void translate(MouseEvent e) {
		translate(e.getX(), e.getY());
	}

	/**
	 * Rotate the selected Content suitably to the specified MouseEvent.
	 * @param e
	 */
	public void rotate(MouseEvent e) {
		rotate(e.getX(), e.getY());
	}

	private Transform3D translateNew = new Transform3D();
	private Transform3D translateOld = new Transform3D();
	private Vector3d translation = new Vector3d();
	private Point3d v1 = new Point3d();
	private Point3d v2 = new Point3d();

	void translate(int xNew, int yNew) {
		if(content == null || content.isLocked())
			return;
		int dx = xNew - xLast;
		int dy = yNew - yLast;
		translateTG.getTransform(translateOld);
		v1.scale(dx, translationPerDx);
		v2.scale(-dy, translationPerDy);
		translation.add(v1, v2);
		translateNew.set(translation);
		translateNew.mul(translateOld);

		translateTG.setTransform(translateNew);
		transformChanged(BehaviorCallback.TRANSLATE, translateNew);	

		xLast = xNew;
		yLast = yNew;
	}

	private Transform3D rotateNew = new Transform3D();
	private Transform3D rotateOld = new Transform3D();
	void rotate(int xNew, int yNew) {
		if(content == null || content.isLocked())
			return;

		int dx = xNew - xLast;
		int dy = yNew - yLast;

		aaX.set(axisPerDx, dx * anglePerPix);
		aaY.set(axisPerDy, dy * anglePerPix);

		transX.set(aaX);
		transY.set(aaY);

		rotateTG.getTransform(rotateOld);

		rotateNew.set(transl_inv);
		rotateNew.mul(transY);
		rotateNew.mul(transX);
		rotateNew.mul(transl);
		rotateNew.mul(rotateOld);

		rotateTG.setTransform(rotateNew);
		xLast = xNew;
		yLast = yNew;

		transformChanged(BehaviorCallback.ROTATE, rotateNew);	
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}

	private class Initializer {
		private Point3d centerInVWorld = new Point3d();
		private Point3d centerInIp = new Point3d();

		private Transform3D ipToVWorld           = new Transform3D();
		private Transform3D ipToVWorldInverse    = new Transform3D();
		private Transform3D localToVWorld        = new Transform3D();
		private Transform3D localToVWorldInverse = new Transform3D();

		private Point3d eyePtInVWorld  = new Point3d();
		private Point3d pickPtInVWorld = new Point3d();

		private Point3d p1 = new Point3d();
		private Point3d p2 = new Point3d();
		private Point3d p3 = new Point3d();

		private Vector3d vec = new Vector3d();

		private void init(Content c, int x, int y) {
			xLast = x;
			yLast = y;

			content = c;

			// some transforms
			c.getLocalToVworld(localToVWorld);
			localToVWorldInverse.invert(localToVWorld);
			canvas.getImagePlateToVworld(ipToVWorld);
			ipToVWorldInverse.invert(ipToVWorld);

			// calculate the canvas position in world coords
			c.getContent().getCenter(centerInVWorld);
			localToVWorld.transform(centerInVWorld);
			ipToVWorldInverse.transform(centerInVWorld, centerInIp);

			// get the eye point in world coordinates
			canvas.getCenterEyeInImagePlate(eyePtInVWorld);
			ipToVWorld.transform(eyePtInVWorld);

			// use picking to infer the radius of the virtual sphere which is rotated
			Point3d p = univ.getPicker().getPickPointGeometry(c, x, y);
			float r = 0, dD = 0;
			if(p != null) {
				pickPtInVWorld.set(p);
				localToVWorld.transform(pickPtInVWorld);
				r = (float)pickPtInVWorld.distance(centerInVWorld);
			} else {
				c.getContent().getMin(p1);
				localToVWorld.transform(p1);
				r = (float)p1.distance(centerInVWorld);
				vec.sub(centerInVWorld, eyePtInVWorld);
				vec.normalize();
				vec.scale(-r);
				pickPtInVWorld.add(centerInVWorld, vec);
			}
			dD = (float)pickPtInVWorld.distance(eyePtInVWorld);

			// calculate distance between eye and canvas point
			canvas.getPixelLocationInImagePlate(x, y, p1);
			ipToVWorld.transform(p1);
			float dd = (float)p1.distance(eyePtInVWorld);

			// calculate the virtual distance between two neighboring pixels
			canvas.getPixelLocationInImagePlate(x+1, y, p2);
			ipToVWorld.transform(p2);
			float dx = (float)p1.distance(p2);

			// calculate the virtual distance between two neighboring pixels
			canvas.getPixelLocationInImagePlate(x, y+1, p3);
			ipToVWorld.transform(p3);
			float dy = (float)p1.distance(p3);

			float dX = dD / dd * dx;
			float dY = dD / dd * dy;

			anglePerPix = Math.atan2(dX, r);
			
			univ.getViewPlatformTransformer().getYDir(axisPerDx, ipToVWorld);
			univ.getViewPlatformTransformer().getXDir(axisPerDy, ipToVWorld);

			translationPerDx.set(axisPerDy);
			translationPerDx.scale(dX);

			translationPerDy.set(axisPerDx);
			translationPerDy.scale(dY);

			rotateTG = c.getLocalRotate();
			translateTG = c.getLocalTranslate();
			c.getContent().getCenter(vec);
			transl_inv.set(vec);
			vec.set(-vec.x, -vec.y, -vec.z);
			transl.set(vec);
		}
	}
}
