package ij3d;

import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.text.TextWindow;

import java.text.DecimalFormat;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import vib.PointList;
import vib.BenesNamedPoint;
import vib.FastMatrix;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import isosurface.MeshGroup;
import isosurface.MeshExporter;
import isosurface.MeshEditor;

import javax.vecmath.Color3f;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;

public class RegistrationMenubar extends JMenuBar implements ActionListener,
							UniverseListener {

	private Image3DUniverse univ;

	private JMenu register;
	private JMenuItem exit;
	private JMenuItem adjustSlices;

	private List openDialogs = new ArrayList();

	private Content templ, model;


	public RegistrationMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;

		univ.addUniverseListener(this);

		register = new JMenu("Register");

		exit = new JMenuItem("Exit registration");
		exit.addActionListener(this);
		register.add(exit);

		adjustSlices = new JMenuItem("Adjust slices");
		adjustSlices.addActionListener(this);
		register.add(adjustSlices);

		this.add(register);

	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == exit) {
			exitRegistration();
		} else if(e.getSource() == adjustSlices) {
			Content c = univ.getSelected();
			if(c != null)
				univ.getExecuter().changeSlices(c);
		} else if(e.getActionCommand().equals("LS_TEMPLATE")) {
			// select landmarks of the template
			selectLandmarkSet(templ, "LS_MODEL");
		} else if(e.getActionCommand().equals("LS_MODEL")) {
			// select the landmarks of the model
			selectLandmarkSet(model, "REGISTER");
		} else if(e.getActionCommand().equals("REGISTER")) {
			// do registration
			doRegistration(templ, model);
		}
	}


	// usually called from the main menu bar.
	public void register() {
		new Thread(new Runnable() {
			public void run() {
				initRegistration();
			}
		}).start();
	}

	public void exitRegistration() {
		templ.showPointList(false);
		model.showPointList(false);
		JMenuBar mb = univ.getMenuBar();
		univ.setMenubar(mb);
		univ.clearSelection();
		univ.setStatus("");
		univ.getPointListDialog().removeExtraPanel();
		univ.ui.setHandTool();
	}

	private void hideAll() {
		for(Iterator it = univ.contents(); it.hasNext();)
			((Content)it.next()).setVisible(false);
	}


	private void selectLandmarkSet(final Content content,
						String actionCommand) {
		hideAll();
		content.setVisible(true);
		content.displayAs(Content.ORTHO);
		content.showPointList(true);
		univ.ui.setPointTool();
		univ.select(content);

		univ.setStatus("Select landmarks in " + content.getName() +
				" and click OK");

		Panel p = new Panel(new FlowLayout());
		Button b = new Button("OK");
		b.setActionCommand(actionCommand);
		b.addActionListener(this);
		p.add(b);

		if(actionCommand.equals("REGISTER")) {
			b = new Button("Back to template");
			b.setActionCommand("LS_TEMPLATE");
			b.addActionListener(this);
			p.add(b);
		}

		univ.getPointListDialog().addPanel(p);
	}

	public void initRegistration() {
		// Select the contents used for registration
		Collection contents = univ.getContents();
		if(contents.size() < 2) {
			IJ.error("At least two bodies are required for " +
				" registration");
			return;
		}
		String[] conts = new String[contents.size()];
		int i = 0;
		for(Iterator it = contents.iterator(); it.hasNext();)
			conts[i++] = ((Content)it.next()).getName();
		GenericDialog gd = new GenericDialog("Registration");
		gd.addChoice("template", conts, conts[0]);
		gd.addChoice("model", conts, conts[1]);
		gd.addCheckbox("allow scaling", true);
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return;
		templ = univ.getContent(gd.getNextChoice());
		model = univ.getContent(gd.getNextChoice());
		boolean scaling = gd.getNextBoolean();

		// Select the landmarks of the template
		selectLandmarkSet(templ, "LS_MODEL");
	}




	public void doRegistration(Content templ, Content model) {

		univ.setStatus("");
		// select the landmarks common to template and model
		PointList tpoints = templ.getPointList();
		PointList mpoints = model.getPointList();
		if(tpoints.size() < 2 || mpoints.size() < 2) {
			IJ.error("At least two points are required in each "
				+ "of the point lists");
		}
		List sett = new ArrayList();
		List setm = new ArrayList();
		for(int i = 0; i < tpoints.size(); i++) {
			BenesNamedPoint pt = tpoints.get(i);
			BenesNamedPoint pm = mpoints.get(pt.getName());
			if(pm != null) {
				sett.add(pt);
				setm.add(pm);
			}
		}
		if(sett.size() < 2) {
			IJ.error("At least two points with the same name "
				+ "must exist in both bodies");
			univ.setStatus("");
			return;
		}

		// Display common landmarks
		DecimalFormat df = new DecimalFormat("00.000");
		String message = "Points used for registration\n \n";
		for(int i = 0; i < sett.size(); i++) {
			BenesNamedPoint bnp = (BenesNamedPoint)sett.get(i);
			message += (bnp.getName() + "    "
				+ df.format(bnp.x) + "    "
				+ df.format(bnp.y) + "    "
				+ df.format(bnp.z) + "\n");
		}
		boolean cont = IJ.showMessageWithCancel(
			"Points used for registration", message);
		if(!cont) return;

		// calculate best rigid
		BenesNamedPoint[] sm = new BenesNamedPoint[setm.size()];
		BenesNamedPoint[] st = new BenesNamedPoint[sett.size()];
		FastMatrix fm = FastMatrix.bestRigid(
			(BenesNamedPoint[])setm.toArray(sm),
			(BenesNamedPoint[])sett.toArray(st));

		// reset the transformation of the template
		// and set the transformation of the model.
		Transform3D t3d = new Transform3D(fm.rowwise16());
		templ.setTransform(new Transform3D());
		model.setTransform(t3d);

		templ.setVisible(true);
		templ.setLocked(true);
		model.setVisible(true);
		model.setLocked(true);

		univ.clearSelection();
		univ.ui.setHandTool();

		IJ.showMessage("Contents are locked to prevent\n" +
			"accidental transformations");
		exitRegistration();
	}

	public void closeAllDialogs() {
		while(openDialogs.size() > 0) {
			GenericDialog gd = (GenericDialog)openDialogs.get(0);
			gd.dispose();
			openDialogs.remove(gd);
		}
	}




	// Universe Listener interface
	public void transformationStarted(View view) {}
	public void transformationFinished(View view) {}
	public void canvasResized() {}
	public void transformationUpdated(View view) {}
	public void contentChanged(Content c) {}
	public void universeClosed() {}
	public void contentAdded(Content c) {}

	public void contentRemoved(Content c) {}
	public void contentSelected(Content c) {}
}

