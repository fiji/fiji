package ij3d.behaviors;

import ij3d.UniverseSettings;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Tuple3d;

/**
 * This class is a helper class which implements some functions for
 * transforming the view part of the scene graph.
 * The view transformation consists of 5 single transformations:
 *
 * A center transformation, which is responsible for shifting the view
 * to a position so that the content of the universe is centered.
 *
 * A translate transformation, which is adjusted manually, either explicitly
 * or interactively, when the user translates the view with the mouse.
 *
 * A rotation transformation, which is adjusted manually, either explicitly
 * or interactively, when the user rotates the view with the mouse.
 *
 * An animation transformation, which is changed when the universe is animated.
 *
 * A zoom transformation, which translates the view backward and forward
 *
 * The functions in this class mainly aim to facilitate transformations
 * related to the image plate.
 *
 * @author bene
 */
public class ViewPlatformTransformer {

	protected DefaultUniverse univ;
	protected ImageCanvas3D canvas;

	protected final Point3d rotCenter = new Point3d();

	private BehaviorCallback callback;

	private TransformGroup centerTG;
	private TransformGroup rotationTG;
	private TransformGroup zoomTG;
	private TransformGroup translateTG;

	private Transform3D centerXform = new Transform3D();
	private Transform3D rotationXform = new Transform3D();
	private Transform3D zoomXform = new Transform3D();
	private Transform3D translateXform = new Transform3D();

	private Point3d origin = new Point3d(0, 0, 0);
	private Point3d eyePos = new Point3d();

	private final Point3d oneInX = new Point3d(1, 0, 0);
	private final Point3d oneInY = new Point3d(0, 1, 0);
	private final Point3d oneInZ = new Point3d(0, 0, 1);

	private Vector3d zDir = new Vector3d();
	private Vector3d xDir = new Vector3d();
	private Vector3d yDir = new Vector3d();

	private Vector3d centerV = new Vector3d();

	private Transform3D ipToVWorld = new Transform3D();

	/**
	 * Initialize this ViewPlatformTransformer.
	 * @param univ
	 * @param callback
	 */
	public ViewPlatformTransformer(DefaultUniverse univ, BehaviorCallback callback) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
		this.callback = callback;
		this.centerTG = univ.getCenterTG();
		this.rotationTG = univ.getRotationTG();
		this.zoomTG = univ.getZoomTG();
		this.translateTG = univ.getTranslateTG();
		((Image3DUniverse)univ).getGlobalCenterPoint(rotCenter);
	}

	/**
	 * Copies the rotation center into the given Tuple3d.
	 */
	public void getRotationCenter(Tuple3d ret) {
		ret.set(rotCenter);
	}

	/**
	 * Moves the view back (i.e. in the z-direction of the image plate)
	 * to the specified distance.
	 * @param distance
	 */
	public void zoomTo(double distance) {
		zDir.set(0, 0, 1);
		zDir.scale(distance);
		zoomXform.set(zDir);
		zoomTG.setTransform(zoomXform);
		univ.getViewer().getView().setBackClipDistance(5 * distance);
		univ.getViewer().getView().setFrontClipDistance(5 * distance / 100);
		transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
	}

	public void updateFrontBackClip() {
		zoomTG.getTransform(zoomXform);
		zoomXform.get(zDir);
		double d = zDir.length();
		univ.getViewer().getView().setBackClipDistance(5 * d);
		univ.getViewer().getView().setFrontClipDistance(5 * d / 100);
	}

	private Transform3D tmp = new Transform3D();
	/**
	 * Zoom by the specified amounts of units.
	 * @param units
	 */
	public void zoom(int units) {
		origin.set(0, 0, 0);
		canvas.getCenterEyeInImagePlate(eyePos);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorld.transform(eyePos);
		float dD = (float)eyePos.distance(origin);

		originInCanvas(originInCanvas);
		canvas.getPixelLocationInImagePlate(originInCanvas, originOnIp);
		ipToVWorld.transform(originOnIp);
		float dd = (float)eyePos.distance(originOnIp);

		canvas.getPixelLocationInImagePlate(
			(int)Math.round(originInCanvas.x+5),
			(int)Math.round(originInCanvas.y), currentPtOnIp);
		ipToVWorld.transform(currentPtOnIp);
		float dx = (float)originOnIp.distance(currentPtOnIp);

		zDir.set(0, 0, -1);
		float factor = dx * dD / dd;
		zDir.scale(units * factor);

		zoomTG.getTransform(zoomXform);
		tmp.set(zDir);
		zoomXform.mul(tmp, zoomXform);

		zoomTG.setTransform(zoomXform);
		zoomXform.get(centerV);
		double distance = centerV.length();
		univ.getViewer().getView().setBackClipDistance(5 * distance);
		univ.getViewer().getView().setFrontClipDistance(5 * distance / 100);
		transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
	}

	/**
	 * Center the view at the given point.
	 * @param center
	 */
	public void centerAt(Point3d center) {
		// set the center transformation to the translation given by
		// the specified point
		centerV.set(center.x, center.y, center.z);
		centerXform.set(centerV);
		centerTG.setTransform(centerXform);
		// set the global translation to identity
		centerXform.setIdentity();
		translateTG.setTransform(centerXform);
		transformChanged(BehaviorCallback.TRANSLATE, centerXform);
		// update rotation center
		rotCenter.set(center);
	}

	private Point2d originInCanvas = new Point2d();
	private Point3d originOnIp = new Point3d();
	private Point3d currentPtOnIp = new Point3d();
	/**
	 * Translates the view suitable to a mouse movement by dxPix and dyPix
	 * on the canvas.
	 * @param dxPix
	 * @param dyPix
	 */
	public void translateXY(int dxPix, int dyPix) {
		origin.set(0, 0, 0);
		canvas.getCenterEyeInImagePlate(eyePos);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorld.transform(eyePos);
		float dD = (float)eyePos.distance(origin);

		originInCanvas(originInCanvas);
		canvas.getPixelLocationInImagePlate(originInCanvas, originOnIp);
		ipToVWorld.transform(originOnIp);
		float dd = (float)eyePos.distance(originOnIp);

		canvas.getPixelLocationInImagePlate(
			(int)Math.round(originInCanvas.x+1),
			(int)Math.round(originInCanvas.y), currentPtOnIp);
		ipToVWorld.transform(currentPtOnIp);
		float dx = (float)originOnIp.distance(currentPtOnIp);

		canvas.getPixelLocationInImagePlate(
			(int)Math.round(originInCanvas.x),
			(int)Math.round(originInCanvas.y+1), currentPtOnIp);
		ipToVWorld.transform(currentPtOnIp);
		float dy = (float)originOnIp.distance(currentPtOnIp);

		float dX = dx * dxPix * dD / dd;
		float dY = dy * dyPix * dD / dd;

		translateXY(dX, dY);
	}

	/**
	 * Translates the view by the specified distances along the x, y
	 * and z direction (of the vworld).
	 * @param v The distances in x, y and z direction, given in vworld
	 * dimensions.
	 */
	public void translate(Vector3d v) {
		getTranslateTranslation(tmpV);
		tmpV.sub(v);
		translateXform.set(tmpV);
		translateTG.setTransform(translateXform);
		transformChanged(BehaviorCallback.TRANSLATE, translateXform);
	}

	/**
	 * Translates the view by the specified distances along the x and y
	 * direction (of the image plate).
	 * @param dx The distance in x direction, given in vworld dimensions.
	 * @param dy The distance in y direction, given in vworld dimensions.
	 */
	public void translateXY(double dx, double dy) {
		getXDir(xDir);
		getYDir(yDir);
		xDir.scale(dx);
		yDir.scale(dy);
		xDir.add(yDir);
		translate(xDir);
	}

	private AxisAngle4d aa = new AxisAngle4d();
	private Vector3d tmpV = new Vector3d();
	/**
	 * Rotates the view around the global rotation center by the specified
	 * angle around the x axis (of the image plate).
	 * @param angle The angle (in rad) around the x-axis
	 */
	public void rotateX(double angle){
		xDir.set(1, 0, 0);
		rotate(xDir, angle);
	}

	/**
	 * Rotates the view around the global rotation center by the specified
	 * angle around the y axis (of the image plate).
	 * @param angle The angle (in rad) around the y-axis
	 */
	public void rotateY(double angle){
		yDir.set(0, 1, 0);
		rotate(yDir, angle);
	}

	/**
	 * Rotates the view around the global rotation center by the specified
	 * angle around the z axis (of the image plate).
	 * @param angle The angle (in rad) around the z-axis
	 */
	public void rotateZ(double angle){
		zDir.set(0, 0, 1);
		rotate(zDir, angle);
	}

	/**
	 * Rotates the view around the center of view by the specified
	 * angle around the given axis (of the image plate).
	 * @param axis The axis of rotation (in image plate coordinate system)
	 * @param angle The angle (in rad) around the given axis
	 */
	public void rotate(Vector3d axis, double angle) {
		Vector3d axisVW = new Vector3d();
		getAxisVworld(axis, axisVW);
		aa.set(axisVW, angle);
		tmp.set(aa);

		// first apply the old transform
		rotationTG.getTransform(rotationXform);
		// rotate
		rotationXform.mul(tmp, rotationXform);

		rotationTG.setTransform(rotationXform);
		transformChanged(BehaviorCallback.ROTATE, rotationXform);
	}

	private AxisAngle4d aa2 = new AxisAngle4d();
	private Transform3D tmp2 = new Transform3D();
	/**
	 * Rotates the view around the center of view by the specified
	 * angles around the x and y axis (of the image plate).
	 * @param angleX The angle (in rad) around the x-axis
	 * @param angleY The angle (in rad) around the y-axis
	 */
	public void rotateXY(double angleX, double angleY) {
		getXDir(xDir);
		aa.set(xDir, angleX);
		tmp.set(aa);

		getYDir(yDir);
		aa2.set(yDir, angleY);
		tmp2.set(aa2);

		// first apply the old transform
		rotationTG.getTransform(rotationXform);
		// rotate x
		rotationXform.mul(tmp, rotationXform);
		// rotate y
		rotationXform.mul(tmp2, rotationXform);

		rotationTG.setTransform(rotationXform);
		transformChanged(BehaviorCallback.ROTATE, rotationXform);
	}

	/**
	 * Retrieves the manual translation vector of the view.
	 * @param v
	 */
	public void getTranslateTranslation(Vector3d v) {
		translateTG.getTransform(tmp);
		tmp.get(v);
	}

	/**
	 * Retrieves the translation vector which is responsible for
	 * centering the view.
	 * @param v
	 */
	public void getCenterTranslation(Vector3d v) {
		centerTG.getTransform(tmp);
		tmp.get(v);
	}

	/**
	 * Retrieves the translation vector which is responsible for the current
	 * zooming and stores it in the given Vector3d.
	 * @param v
	 */
	public void getZoomTranslation(Vector3d v) {
		zoomTG.getTransform(tmp);
		tmp.get(v);
	}

	/**
	 * Stores the canvas position of the origin of the vworld in
	 * the specified Point2d.
	 * @param out
	 */
	public void originInCanvas(Point2d out) {
		origin.set(0, 0, 0);
		pointInCanvas(origin, out);
	}

	private Point3d tmpP = new Point3d();
	private Transform3D ipToVWorldInverse = new Transform3D();
	/**
	 * Calculates where the specified point in the vworld space is
	 * placed on the canvas and stores the result in the specified Point2d.
	 * @param in
	 * @param out
	 */
	public void pointInCanvas(Point3d in, Point2d out) {
		tmpP.set(in);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorldInverse.invert(ipToVWorld);
		ipToVWorldInverse.transform(in);
		canvas.getPixelLocationFromImagePlate(in, out);
	}

	/**
	 * Calculates the distance between the viewer's eye and an
	 * arbitrary point in the vworld space.
	 * @return
	 */
	public double distanceEyeTo(Point3d p) {
		canvas.getCenterEyeInImagePlate(eyePos);
		canvas.getImagePlateToVworld(ipToVWorld);
		ipToVWorld.transform(eyePos);
		return eyePos.distance(p);
	}

	/**
	 * Calculates the distance between the viewer's eye and the
	 * origin in the vworld space.
	 * @return
	 */
	public double distanceEyeOrigin() {
		origin.set(0, 0, 0);
		return distanceEyeTo(origin);
	}

	/**
	 * Calculates from the specified axis in image plate coordinate
	 * system the corresponding vector in the vworld coordinate system.
	 */
	public void getAxisVworld(Vector3d axis, Vector3d axisVW) {
		canvas.getImagePlateToVworld(ipToVWorld);
		origin.set(0, 0, 0);
		oneInX.set(axis);
		ipToVWorld.transform(oneInX);
		ipToVWorld.transform(origin);
		axisVW.sub(oneInX, origin);
		axisVW.normalize();
	}

	/**
	 * Transforms the x-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 */
	public void getXDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getXDir(v, ipToVWorld);
	}

	/**
	 * Transforms the x-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 * @param ipToVWorld the image plate to vworld transformation.
	 */
	public void getXDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInX.set(1, 0, 0);
		ipToVWorld.transform(oneInX);
		ipToVWorld.transform(origin);
		v.sub(oneInX, origin);
		v.normalize();
	}

	/**
	 * Stores the y-direction in the image plate coordinate system,
	 * i.e. the direction towards the user, in the given Vector3d.
	 * @param v Vector3d in which the result in stored.
	 */
	public void getYDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getYDir(v, ipToVWorld);
	}

	/**
	 * Transforms the y-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 * @param ipToVWorld the image plate to vworld transformation.
	 */
	public void getYDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInY.set(0, 1, 0);
		ipToVWorld.transform(oneInY);
		ipToVWorld.transform(origin);
		v.sub(oneInY, origin);
		v.normalize();
	}

	/**
	 * Transforms the z-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 */
	public void getZDir(Vector3d v) {
		canvas.getImagePlateToVworld(ipToVWorld);
		getZDir(v, ipToVWorld);
	}

	/**
	 * Transforms the z-direction of the image plate to a normalized
	 * vector representing this direction in the vworld space.
	 *
	 * @param v Vector3d in which the result in stored.
	 * @param ipToVWorld the image plate to vworld transformation.
	 */
	public void getZDir(Vector3d v, Transform3D ipToVWorld) {
		origin.set(0, 0, 0);
		oneInZ.set(0, 0, 1);
		ipToVWorld.transform(oneInZ);
		ipToVWorld.transform(origin);
		v.sub(origin, oneInZ);
		v.normalize();
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}
}
