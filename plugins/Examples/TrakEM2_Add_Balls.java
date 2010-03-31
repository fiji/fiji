/** Albert Cardona 20081110
 *
 * An example plugin for TrakEM2 that inserts two Ball objects into an existing
 * and opened TrakEM2 project, where each Ball object contains three balls each
 * (i.e. three sets of x,y,z,r coordinates).
 *
 * Note that the new Ball objects need be added to BOTH the LayerSet and the ProjectTree,
 * the latter is done in a convenient way using ProjectTree.insertSegmentations(...) method.
 *
 * Then a set of optional calls are made:
 *  - to show the contents in 3D
 *  - to export the contents of the 3D window to wavefront format
 *
 * This code is under the public domain.
 *
 * This code requires TrakEM2_.jar and all its dependencies, including VIB_.jar.
 *
 * Have fun.
 */


import ij.plugin.PlugIn;

import ij3d.Image3DUniverse;
import ij3d.Content;
import isosurface.MeshExporter;

import ini.trakem2.ControlWindow;
import ini.trakem2.Project;
import ini.trakem2.display.Ball;
import ini.trakem2.display.Display;
import ini.trakem2.display.Display3D;
import ini.trakem2.display.Layer;
import ini.trakem2.display.LayerSet;
import ini.trakem2.tree.ProjectThing;
import ini.trakem2.utils.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

public class TrakEM2_Add_Balls implements PlugIn {

	public void run(String arg) {

		// 1 - Obtain an open TrakEM2 project: the one selected in the ControlWindow
		final Project project = ControlWindow.getActive();

		if (null == project) {
			Utils.log("Open or create a TrakEM2 project first!");
			return;
		}
		// Else, you could create a project like:
		// Project project = Project.newFSProject("blank", null, "/path/to/storage_folder/");


		// 2 - Define two sets of x,y,z,r coordinates to import as 3D-positioned balls of radius r

					//   X    Y    Z   R

		final double[][] set1 = {{ 100, 100,   0, 25 },
			                 { 130, 120,   1, 30 },
					 { 110, 150,   0, 15 }};

		final double[][] set2 = {{ 200, 200,   1, 45 },
			                 { 240, 190,   0, 35 },
					 { 220, 250,   1, 55 }};

		// 3 - Insert each set as a Ball object, each containing 3 x,y,z,r spheres:
		Ball b1 = addBallObject(project, set1, "Set 1");
		Ball b2 = addBallObject(project, set2, "Set 2");

		// 4 - Change colors and settings:
		b1.setColor(Color.green);
		b2.setColor(Color.magenta); // yellow is default color
		// NOTICE that the color cues will paint each individual ball red in the previous layer,
		// and blue in the next one -- the color is used to paint in the actual layer where it lives.
		// (Scroll through the stack to see the effect -- the color cues help in noticing whether
		// a ball already exists at the location in the previous or next slice/Layer).
		// You can TURN OFF color cues with the 'p' keyboard shortcut on a Display,
		// or programmatically by:
		//      project.setProperty("no_color_cues", true);


		// 5 - Add nodes automatically to the Template and Project trees (REQUIRED):
		// (can be rearranged manually later, or could be added with way more precision programmatically)
		ArrayList al = new ArrayList();
		al.add(b1);
		al.add(b2);
		project.getProjectTree().insertSegmentations(project, al);

		// 6 - Automatically scroll LayerSet to the first layer of the first ball (OPTIONAL):
		Display.getFront().setLayer(b1.getFirstLayer());

		// 7 - Show the balls in 3D (OPTIONAL):
		ProjectThing p1 = project.findProjectThing(b1); // a Project tree node
		ProjectThing p2 = project.findProjectThing(b2); // a Project tree node
		Future<List<Content>> fu1 = Display3D.show(p1, true, 1); // wait, and resample 1 ( but resample only affects image volumes and AreaList meshes!)
		Future<List<Content>> fu2 = Display3D.show(p2, true, 1); //  If not waiting, then since its threaded, the saveAsWaveFront below would find nothing to save.

		// WAIT until added, so we know the Display3D has been created
		try {
			fu1.get();
			fu2.get();
		} catch (InterruptedException ie) {
			//
		} catch (ExecutionException ee) {
			ee.printStackTrace();
		}

		// 8 - Export the balls meshes to a .obj wavefront file (OPTIONAL):
		// (This will export all the current contents of the Display3D that can be exported as meshes.
		// One could refine what to export with:
		//    Content c1 = univ.getContent(Display3D.makeTitle(b1)); // by title
		//    Content c2 = ...
		// ... and then adding them to a Collection.
		//
		Image3DUniverse univ = Display3D.getDisplay(project.getRootLayerSet()).getUniverse();
		Collection all = univ.getContents();
		MeshExporter.saveAsWaveFront(all); // pops up file dialog to save
	}

	private Ball addBallObject(Project project, double[][] data, String title) {

		LayerSet layerset = project.getRootLayerSet();
		Ball ball = new Ball(project, title, 0, 0);

		// Ball is a ZDisplayable object, so gets added to a LayerSet:
		layerset.add(ball); 

		for (int i=0; i<data.length; i++) {
			// Get or create a layer for the Z
			double z = data[i][2];
			double thickness = 1;
			Layer la = layerset.getLayer(z, thickness, true); // created new if not there already
			// Insert the individual ball
			double x = data[i][0];
			double y = data[i][1];
			double r = data[i][3];
			ball.addBall(x, y, r, la.getId()); // no Z, but Layer id!
		}

		// After a repaint, Ball balls will be made local to the bounding box of all of them
		ball.repaint(true);
		return ball;
	}
}
