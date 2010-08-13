package ij3d;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Alpha;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

public abstract class DefaultAnimatableUniverse extends DefaultUniverse {

	/** The axis of rotation */
	private Vector3d rotationAxis = new Vector3d(0, 1, 0);

	/* Temporary Transform3D objects which are re-used in the methods below */
	private Transform3D centerXform = new Transform3D();
	private Transform3D animationXform = new Transform3D();
	private Transform3D rotationXform = new Transform3D();
	private Transform3D rotate = new Transform3D();
	private Transform3D centerXformInv = new Transform3D();

	private float rotationInterval = 2f; // degree

	/**
	 * A reference to the RotationInterpolator used for animation.
	 */
	private RotationInterpolator rotpol;

	/**
	 * The Alpha object used for interpolation.
	 */
	private Alpha animation;

	/**
	 * A reference to the TransformGroup of the universe's viewing platform
	 * which is responsible for animation.
	 */
	private TransformGroup animationTG;

	/**
	 * A reference to the TransformGroup of the universe's viewing platform
	 * which is responsible for rotation.
	 */
	private TransformGroup rotationTG;

	/**
	 * ImageStack holding the image series after recording an animation.
	 */
	private ImageStack freehandStack;

	/**
	 * Constructor
	 * @param width of the universe
	 * @param height of the universe
	 */
	public DefaultAnimatableUniverse(int width, int height) {
		super(width, height);

		animationTG = getAnimationTG();
		rotationTG = getRotationTG();

		// Set up the default rotation axis and origin
		updateRotationAxisAndCenter();

		// Animation
		animation = new Alpha(-1, 4000);
		animation.pause();
		animation.setStartTime(System.currentTimeMillis());
		BranchGroup bg = new BranchGroup();
		rotpol = new RotationInterpolator(animation, animationTG) {
			@Override
			public void processStimulus(java.util.Enumeration e) {
				super.processStimulus(e);
				if(!animation.isPaused()) {
					fireTransformationUpdated();
				} else {
					// this is the point where we actually know that
					// the animation has stopped
					animationPaused();
				}
			}
		};
		rotpol.setTransformAxis(centerXform);
		rotpol.setSchedulingBounds(bounds);
		// set disabled; it's enabled at startAnimation
		rotpol.setEnable(false);
		bg.addChild(rotpol);
		animationTG.addChild(bg);

		addUniverseListener(new UniverseListener() {
				public void transformationStarted(View view) {}
				public void transformationFinished(View view) {}
				public void contentAdded(Content c) {}
				public void contentRemoved(Content c) {}
				public void canvasResized() {}
				public void universeClosed() {}
				public void contentSelected(Content c) {}

				public void transformationUpdated(View view) {
					addFreehandRecordingFrame();
				}

				public void contentChanged(Content c) {
					addFreehandRecordingFrame();
				}
		});
	}

	/**
	 * Set the rotation interval (in degree)
	 */
	public void setRotationInterval(float f) {
		this.rotationInterval = f;
	}

	/**
	 * Returns the rotation interval (in degree)
	 */
	public float getRotationInterval() {
		return rotationInterval;
	}

	/**
	 * Add a new frame to the freehand recording stack.
	 */
	private void addFreehandRecordingFrame() {
		if(freehandStack == null)
			return;

		win.updateImagePlus();
		ImageProcessor ip = win.getImagePlus().getProcessor();
		freehandStack.addSlice("", ip);
	}

	/**
	 * Start freehand recording.
	 */
	public void startFreehandRecording() {
		// check if is's already running.
		if(freehandStack != null)
			return;

		// create a new stack
		win.updateImagePlus();
		ImageProcessor ip = win.getImagePlus().getProcessor();
		freehandStack = new ImageStack(ip.getWidth(), ip.getHeight());
		freehandStack.addSlice("", ip);
	}

	/**
	 * Stop freehand recording.
	 * Returns an ImagePlus whose stack contains the frames of the movie.
	 */
	public ImagePlus stopFreehandRecording() {
		if(freehandStack == null || freehandStack.getSize() == 1)
			return null;

		ImagePlus imp = new ImagePlus("Movie", freehandStack);
		freehandStack = null;
		return imp;
	}

	/**
	 * Records a full 360 degree rotation and returns an ImagePlus
	 * containing the frames of the animation.
	 */
	public ImagePlus record360() {
		// check if freehand recording is running
		if(freehandStack != null) {
			IJ.error("Freehand recording is active. Stop first.");
			return null;
		}
		// stop the animation
		if(!animation.isPaused())
			pauseAnimation();
		// create a new stack
		ImageProcessor ip = win.getImagePlus().getProcessor();
		ImageStack stack = new ImageStack(ip.getWidth(), ip.getHeight());
		// prepare everything
		updateRotationAxisAndCenter();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {e.printStackTrace();}
		centerXformInv.invert(centerXform);
		double deg2 = rotationInterval * Math.PI / 180;
		int steps = (int)Math.round(2 * Math.PI / deg2);

		getCanvas().getView().stopView();

		double alpha = 0;

		// update transformation and record
		for(int i = 0; i < steps; i++) {
			alpha = i * deg2;
			rotationXform.rotY(alpha);
			rotate.mul(centerXform, rotationXform);
			rotate.mul(rotate, centerXformInv);
			animationTG.setTransform(rotate);
			fireTransformationUpdated();
			getCanvas().getView().renderOnce();
			win.updateImagePlusAndWait();
			ip = win.getImagePlus().getProcessor();
			int w = ip.getWidth(), h = ip.getHeight();
			if(stack == null)
				stack = new ImageStack(w, h);
			stack.addSlice("", ip);
		}

		// restart the view and animation
		getCanvas().getView().startView();

		// cleanup
		incorporateAnimationInRotation();

		if(stack.getSize() == 0)
			return null;
		ImagePlus imp = new ImagePlus("Movie", stack);
		return imp;
	}

	/**
	 * Copy the current rotation axis into the given Vector3f.
	 */
	public void getRotationAxis(Vector3f ret) {
		ret.set(rotationAxis);
	}

	/**
	 * Set the rotation axis to the specified Vector3f.
	 */
	public void setRotationAxis(Vector3f a) {
		rotationAxis.set(a);
	}

	/**
	 * Convenience method which rotates the universe around the
	 * x-axis (regarding the view, not the vworld) the specified amount
	 * of degrees (in rad).
	 */
	public void rotateX(double rad) {
		viewTransformer.rotateX(rad);
	}

	/**
	 * Convenience method which rotates the universe around the
	 * y-axis (regarding the view, not the vworld) the specified amount
	 * of degrees (in rad).
	 */
	public void rotateY(double rad) {
		viewTransformer.rotateY(rad);
		fireTransformationUpdated();
	}

	/**
	 * Convenience method which rotates the universe around the
	 * z-axis (regarding the view, not the vworld) the specified amount
	 * of degrees (in rad).
	 */
	public void rotateZ(double rad) {
		viewTransformer.rotateZ(rad);
	}

	/**
	 * Starts animating the universe.
	 */
	public void startAnimation() {
		animationTG.getTransform(animationXform);
		updateRotationAxisAndCenter();
		rotpol.setTransformAxis(centerXform);
		rotpol.setEnable(true);
		animation.resume();
		fireTransformationStarted();
	}

	/**
	 * Pauses the animation.
	 */
	public void pauseAnimation() {
		animation.pause();
	}

	/**
	 * Called from the RotationInterpolator, indicating that the
	 * animation was paused.
	 */
	private void animationPaused() {
		rotpol.setEnable(false);
		incorporateAnimationInRotation();
		animation.setStartTime(System.currentTimeMillis());
		fireTransformationUpdated();
		fireTransformationFinished();
	}

	/**
	 * After animation was stopped, the transformation of the animation
	 * TransformGroup is incorporated in the rotation TransformGroup and
	 * the animation TransformGroup is set to identity.
	 *
	 * This is necessary, because otherwise a following rotation by the
	 * mouse would not take place around the expected axis.
	 */
	private void incorporateAnimationInRotation() {
		rotationTG.getTransform(rotationXform);
		animationTG.getTransform(animationXform);
		rotationXform.mul(rotationXform, animationXform);

		animationXform.setIdentity();
		animationTG.setTransform(animationXform);
		rotationTG.setTransform(rotationXform);
	}

	private Vector3d v1 = new Vector3d();
	private Vector3d v2 = new Vector3d();
	private AxisAngle4d aa = new AxisAngle4d();

	private void updateRotationAxisAndCenter() {
		v1.set(0, 1, 0);
		v2.cross(v1, rotationAxis);
		double angle = Math.acos(v1.dot(rotationAxis));
		aa.set(v2, angle);
		centerXform.set(aa);
	}
}
