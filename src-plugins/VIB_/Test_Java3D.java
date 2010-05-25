import java.awt.BorderLayout;
import java.awt.Panel;
import java.awt.event.*;
import java.awt.Dimension;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import ij.plugin.PlugIn;
import ij.gui.GenericDialog;


public class Test_Java3D implements PlugIn {

	public void run(String args) {
		GenericDialog gd = new GenericDialog("3D Test");
		Panel p = createPanel();
		gd.addPanel(p);
		gd.showDialog();
	}

	public Panel createPanel() {
		Panel p = new Panel();
		p.setPreferredSize(new Dimension(512, 512));
		p.setLayout(new BorderLayout());
		Canvas3D canvas3D = new Canvas3D(
			SimpleUniverse.getPreferredConfiguration());
		p.add("Center", canvas3D);

		BranchGroup scene = createSceneGraph();
		scene.compile();

		SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
		simpleU.getViewingPlatform().setNominalViewingTransform();

		simpleU.addBranchGraph(scene);
		return p;
	} // end of HelloJava3Dd (constructor)

	public BranchGroup createSceneGraph() {
		BranchGroup objRoot = new BranchGroup();

		// rotate object has composited transformation matrix
		Transform3D rotate = new Transform3D();
		Transform3D tempRotate = new Transform3D();

		rotate.rotX(Math.PI/4.0d);
		tempRotate.rotY(Math.PI/5.0d);
		rotate.mul(tempRotate);

		TransformGroup objRotate = new TransformGroup(rotate);

		// Create the transform group node and initialize it to the
		// identity.  Enable the TRANSFORM_WRITE capability so that
		// our behavior code can modify it at runtime.  Add it to the
		// root of the subgraph.
		TransformGroup objSpin = new TransformGroup();
		objSpin.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		objRoot.addChild(objRotate);
		objRotate.addChild(objSpin);

		// Create a simple shape leaf node, add it to the scene graph.
		// ColorCube is a Convenience Utility class
		objSpin.addChild(new ColorCube(0.4));

		// Create a new Behavior object that will perform the desired
		// operation on the specified transform object and add it into
		// the scene graph.
		Transform3D yAxis = new Transform3D();
		Alpha rotationAlpha = new Alpha(-1, 4000);

		RotationInterpolator rotator = new RotationInterpolator(
					rotationAlpha, objSpin, yAxis,
					0.0f, (float) Math.PI*2.0f);

		// a bounding sphere specifies a region a behavior is active
		// create a sphere centered at the origin with radius of 1
		BoundingSphere bounds = new BoundingSphere();
		rotator.setSchedulingBounds(bounds);
		objSpin.addChild(rotator);

		return objRoot;
	} // end of CreateSceneGraph method of HelloJava3Dd

} // end of class HelloJava3Dd
