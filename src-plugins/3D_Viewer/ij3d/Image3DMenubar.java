package ij3d;

import ij.ImagePlus;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Iterator;

import javax.media.j3d.View;


public class Image3DMenubar extends JMenuBar implements ActionListener,
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;
	private Executer executer;

	private JMenuItem addContentFromFile;
	private JMenuItem addContentFromImage;
	private JMenuItem add4DFromFile;
	private JMenuItem add4DFromImage;
	private JMenuItem add4DFromFolder;
	private JMenuItem saveView;
	private JMenuItem loadView;
	private JMenuItem saveSession;
	private JMenuItem loadSession;
	private JMenuItem importObj;
	private JMenuItem importStl;
	private JMenuItem color;
	private JMenuItem bgColor;
	private JMenuItem channels;
	private JMenuItem luts;
	private JMenuItem transparency;
	private JMenuItem threshold;
	private JMenuItem fill;
	private JMenuItem slices;
	private JMenuItem updateVol;
	private JMenuItem delete;
	private JMenuItem properties;
	private JMenuItem resetView;
	private JMenuItem snapshot;
	private JMenuItem record360;
	private JMenuItem startRecord;
	private JMenuItem stopRecord;
	private JMenuItem startAnimation;
	private JMenuItem stopAnimation;
	private JMenuItem animationOptions;
	private JMenuItem viewPreferences;
	private JMenuItem close;
	private JMenuItem setTransform;
	private JMenuItem resetTransform;
	private JMenuItem applyTransform;
	private JMenuItem saveTransform;
	private JMenuItem exportTransformed;
	private JMenuItem exportObj;
	private JMenuItem exportDXF;
	private JMenuItem exportAsciiSTL;
	private JMenuItem exportBinarySTL;
	private JMenuItem smoothMesh;
	private JMenuItem scalebar;
	private JMenuItem smoothAllMeshes;
	private JMenuItem displayAsVolume;
	private JMenuItem displayAsOrtho;
	private JMenuItem displayAsSurface;
	private JMenuItem displayAsSurfacePlot;
	private JMenuItem centerSelected;
	private JMenuItem centerOrigin;
	private JMenuItem centerUniverse;
	private JCheckBoxMenuItem sync;
	private JMenuItem fitViewToUniverse;
	private JMenuItem fitViewToContent;
	private JMenuItem regist;
	private JCheckBoxMenuItem shaded;
	private JMenuItem pl_load;
	private JMenuItem pl_save;
	private JMenuItem pl_size;
	private JCheckBoxMenuItem pl_show;
	private JMenuItem j3dproperties;
	private JCheckBoxMenuItem coordinateSystem;
	private JCheckBoxMenuItem boundingBox;
	private JCheckBoxMenuItem allCoordinateSystems;
	private JCheckBoxMenuItem lock;
	private JCheckBoxMenuItem show;
	private JMenuItem viewposXY, viewposXZ, viewposYZ, viewnegXY, viewnegXZ, viewnegYZ;

	private JMenu transformMenu;
	private JMenu editMenu;
	private JMenu selectMenu;
	private JMenu viewMenu;
	private JMenu fileMenu;
	private JMenu helpMenu;

	public Image3DMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;
		this.executer = univ.getExecuter();

		univ.addUniverseListener(this);

		fileMenu = createFileMenu();
		this.add(fileMenu);

		editMenu = createEditMenu();
		this.add(editMenu);

		selectMenu = createSelectMenu();
		this.add(selectMenu);

		transformMenu = createTransformMenu();
		this.add(transformMenu);

		viewMenu = createViewMenu();
		this.add(viewMenu);

		helpMenu = createHelpMenu();
		this.add(helpMenu);

		contentSelected(null);
	}

	public JMenu createFileMenu() {
		JMenu file = new JMenu("File");

		JMenu addContent = new JMenu("Add content");
		addContentFromFile = new JMenuItem("from file");
		addContentFromFile.addActionListener(this);
		addContent.add(addContentFromFile);
		addContentFromImage = new JMenuItem("from open image");
		addContentFromImage.addActionListener(this);
		addContent.add(addContentFromImage);
		file.add(addContent);

		JMenu add4D = new JMenu("Add timelapse");
		add4DFromFile = new JMenuItem("from hyperstack file");
		add4DFromFile.addActionListener(this);
		add4D.add(add4DFromFile);
		add4DFromImage = new JMenuItem("from open hyperstack");
		add4DFromImage.addActionListener(this);
		add4D.add(add4DFromImage);
		add4DFromFolder = new JMenuItem("from folder with stacks");
		add4DFromFolder.addActionListener(this);
		add4D.add(add4DFromFolder);
		file.add(add4D);


		importObj = new JMenuItem("Import WaveFront");
		importObj.addActionListener(this);
		file.add(importObj);

		importStl = new JMenuItem("Import STL");
		importStl.addActionListener(this);
		file.add(importStl);

		delete = new JMenuItem("Delete");
		delete.setEnabled(false);
		delete.addActionListener(this);
		file.add(delete);

		file.addSeparator();

		saveView = new JMenuItem("Save View");
		saveView.addActionListener(this);
		file.add(saveView);

		loadView = new JMenuItem("Load View");
		loadView.addActionListener(this);
		file.add(loadView);

		file.addSeparator();

		saveSession = new JMenuItem("Save Session");
		saveSession.addActionListener(this);
		file.add(saveSession);

		loadSession = new JMenuItem("Load Session");
		loadSession.addActionListener(this);
		file.add(loadSession);

		file.addSeparator();

		JMenu subMenu = new JMenu("Export surfaces as");
		file.add(subMenu);
		exportObj = new JMenuItem("WaveFront");
		exportObj.addActionListener(this);
		subMenu.add(exportObj);

		exportDXF = new JMenuItem("DXF");
		exportDXF.addActionListener(this);
		subMenu.add(exportDXF);

		exportAsciiSTL = new JMenuItem("STL (ASCII)");
		exportAsciiSTL.addActionListener(this);
		subMenu.add(exportAsciiSTL);

		exportBinarySTL = new JMenuItem("STL (binary)");
		exportBinarySTL.addActionListener(this);
		subMenu.add(exportBinarySTL);

		file.addSeparator();

		close = new JMenuItem("Quit");
		close.addActionListener(this);
		file.add(close);

		return file;
	}

	public JMenu createEditMenu() {
		JMenu edit = new JMenu("Edit");

		slices = new JMenuItem("Adjust slices");
		slices.addActionListener(this);
		edit.add(slices);

		updateVol = new JMenuItem("Upate Volume");
		updateVol.addActionListener(this);
		edit.add(updateVol);

		fill = new JMenuItem("Fill selection");
		fill.addActionListener(this);
		edit.add(fill);

		smoothMesh = new JMenuItem("Smooth mesh");
		smoothMesh.addActionListener(this);
		edit.add(smoothMesh);

		smoothAllMeshes = new JMenuItem("Smooth all meshes");
		smoothAllMeshes.addActionListener(this);
		edit.add(smoothAllMeshes);

		edit.addSeparator();

		edit.add(createDisplayAsSubMenu());
		edit.add(createAttributesSubMenu());
		edit.add(createHideSubMenu());
		edit.add(createPLSubMenu());

		edit.addSeparator();

		regist = new JMenuItem("Register");
		regist.addActionListener(this);
		edit.add(regist);

		edit.addSeparator();

		properties = new JMenuItem("Object Properties");
		properties.addActionListener(this);
		edit.add(properties);

		return edit;
	}

	public JMenu createSelectMenu() {
		return new JMenu("Select");
	}

	public JMenu createTransformMenu() {
		JMenu transform = new JMenu("Transformation");

		lock = new JCheckBoxMenuItem("Lock");
		lock.addItemListener(this);
		transform.add(lock);

		setTransform = new JMenuItem("Set Transform");
		setTransform.addActionListener(this);
		transform.add(setTransform);

		resetTransform = new JMenuItem("Reset Transform");
		resetTransform.addActionListener(this);
		transform.add(resetTransform);

		applyTransform = new JMenuItem("Apply Transform");
		applyTransform.addActionListener(this);
		transform.add(applyTransform);

		saveTransform = new JMenuItem("Save Transform");
		saveTransform.addActionListener(this);
		transform.add(saveTransform);

		transform.addSeparator();

		exportTransformed= new JMenuItem("Export transformed image");
		exportTransformed.addActionListener(this);
		transform.add(exportTransformed);

		return transform;
	}
	public JMenu createViewMenu() {
		JMenu view = new JMenu("View");

		resetView = new JMenuItem("Reset view");
		resetView.addActionListener(this);
		view.add(resetView);

		// center submenu
		JMenu menu = new JMenu("Center");
		centerSelected = new JMenuItem("Selected content");
		centerSelected.addActionListener(this);
		menu.add(centerSelected);

		centerOrigin = new JMenuItem("Origin");
		centerOrigin.addActionListener(this);
		menu.add(centerOrigin);

		centerUniverse = new JMenuItem("Universe");
		centerUniverse.addActionListener(this);
		menu.add(centerUniverse);
		view.add(menu);

		// fit view submenu
		menu = new JMenu("Fit view to");
		fitViewToUniverse = new JMenuItem("Universe");
		fitViewToUniverse.addActionListener(this);
		menu.add(fitViewToUniverse);

		fitViewToContent = new JMenuItem("Selected content");
		fitViewToContent.addActionListener(this);
		menu.add(fitViewToContent);
		view.add(menu);

		menu = new JMenu("Set view");
		viewposXY = new JMenuItem("+ XY"); viewposXY.addActionListener(this); menu.add(viewposXY);
		viewposXZ = new JMenuItem("+ XZ"); viewposXZ.addActionListener(this); menu.add(viewposXZ);
		viewposYZ = new JMenuItem("+ YZ"); viewposYZ.addActionListener(this); menu.add(viewposYZ);
		viewnegXY = new JMenuItem("- XY"); viewnegXY.addActionListener(this); menu.add(viewnegXY);
		viewnegXZ = new JMenuItem("- XZ"); viewnegXZ.addActionListener(this); menu.add(viewnegXZ);
		viewnegYZ = new JMenuItem("- YZ"); viewnegYZ.addActionListener(this); menu.add(viewnegYZ);
		view.add(menu);

		view.addSeparator();

		snapshot = new JMenuItem("Take snapshot");
		snapshot.addActionListener(this);
		view.add(snapshot);

		view.addSeparator();

		record360 = new JMenuItem("Record 360 deg rotation");
		record360.addActionListener(this);
		view.add(record360);

		startRecord = new JMenuItem("Start freehand recording");
		startRecord.addActionListener(this);
		view.add(startRecord);

		stopRecord = new JMenuItem("Stop freehand recording");
		stopRecord.addActionListener(this);
		view.add(stopRecord);

		view.addSeparator();

		startAnimation = new JMenuItem("Start animation");
		startAnimation.addActionListener(this);
		view.add(startAnimation);

		stopAnimation = new JMenuItem("Stop animation");
		stopAnimation.addActionListener(this);
		view.add(stopAnimation);

		animationOptions = new JMenuItem("Change animation options");
		animationOptions.addActionListener(this);
		view.add(animationOptions);

		view.addSeparator();

		sync = new JCheckBoxMenuItem("Sync view");
		sync.addItemListener(this);
		view.add(sync);

		view.addSeparator();

		scalebar = new JMenuItem("Edit Scalebar");
		scalebar.addActionListener(this);
		view.add(scalebar);

		view.addSeparator();

		viewPreferences = new JMenuItem("View Preferences");
		viewPreferences.addActionListener(this);
		view.add(viewPreferences);

		bgColor = new JMenuItem("Change background color");
		bgColor.addActionListener(this);
		view.add(bgColor);

		return view;
	}

	public JMenu createHelpMenu() {
		JMenu help = new JMenu("Help");
		j3dproperties = new JMenuItem("Java 3D Properties");
		j3dproperties.addActionListener(this);
		help.add(j3dproperties);
		return help;
	}


	public JMenu createPLSubMenu() {
		JMenu pl = new JMenu("Point list");
		if(univ == null)
			return pl;
		pl_load = new JMenuItem("Load Point List");
		pl_load.addActionListener(this);
		pl.add(pl_load);

		pl_save = new JMenuItem("Save Point List");
		pl_save.addActionListener(this);
		pl.add(pl_save);

		pl_show = new JCheckBoxMenuItem("Show Point List");
		pl_show.addItemListener(this);
		pl.add(pl_show);

		pl.addSeparator();

		pl_size = new JMenuItem("Point size");
		pl_size.addActionListener(this);
		pl.add(pl_size);

		return pl;
	}


	public JMenu createHideSubMenu() {
		JMenu hide = new JMenu("Hide/Show");

		show = new JCheckBoxMenuItem("Show content");
		show.setState(true);
		show.addItemListener(this);
		hide.add(show);

		coordinateSystem = new JCheckBoxMenuItem(
					"Show coordinate system", true);
		coordinateSystem.addItemListener(this);
		hide.add(coordinateSystem);

		boundingBox = new JCheckBoxMenuItem(
					"Show bounding box", false);
		boundingBox.addItemListener(this);
		hide.add(boundingBox);

		allCoordinateSystems = new JCheckBoxMenuItem(
				"Show all coordinate systems", true);
		allCoordinateSystems.addItemListener(this);
		hide.add(allCoordinateSystems);

		return hide;
	}

	public JMenu createAttributesSubMenu() {
		JMenu attributes = new JMenu("Attributes");

		luts = new JMenuItem("Transfer function");
		luts.addActionListener(this);
		attributes.add(luts);

		channels = new JMenuItem("Change channels");
		channels.addActionListener(this);
		attributes.add(channels);

		color = new JMenuItem("Change color");
		color.addActionListener(this);
		attributes.add(color);

		transparency = new JMenuItem("Change transparency");
		transparency.addActionListener(this);
		attributes.add(transparency);

		threshold = new JMenuItem("Adjust threshold");
		threshold.addActionListener(this);
		attributes.add(threshold);

		shaded = new JCheckBoxMenuItem("Shade surface");
		shaded.setState(true);
		shaded.addItemListener(this);
		attributes.add(shaded);

		return attributes;
	}

	public JMenu createDisplayAsSubMenu() {
		JMenu display = new JMenu("Display as");

		displayAsVolume = new JMenuItem("Volume");
		displayAsVolume.addActionListener(this);
		display.add(displayAsVolume);

		displayAsOrtho = new JMenuItem("Orthoslice");
		displayAsOrtho.addActionListener(this);
		display.add(displayAsOrtho);

		displayAsSurface = new JMenuItem("Surface");
		displayAsSurface.addActionListener(this);
		display.add(displayAsSurface);

		displayAsSurfacePlot = new JMenuItem("Surface Plot 2D");
		displayAsSurfacePlot.addActionListener(this);
		display.add(displayAsSurfacePlot);

		return display;
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if(src == color)
			executer.changeColor(getSelected());
		else if (src == bgColor)
			executer.changeBackgroundColor();
		else if(src == scalebar)
			executer.editScalebar();
		else if(src == luts)
			executer.adjustLUTs(getSelected());
		else if(src == channels)
			executer.changeChannels(getSelected());
		else if(src == transparency)
			executer.changeTransparency(getSelected());
		else if(src == addContentFromFile)
			executer.addContentFromFile();
		else if(src == addContentFromImage)
			executer.addContentFromImage(null);
		else if(src == add4DFromFile)
			executer.addTimelapseFromFile();
		else if(src == add4DFromImage)
			executer.addTimelapseFromHyperstack(null);
		else if(src == add4DFromFolder)
			executer.addTimelapseFromFolder();
		else if(src == regist)
			executer.register();
		else if(src == delete)
			executer.delete(getSelected());
		else if(src == resetView)
			executer.resetView();
		else if(src == centerSelected)
			executer.centerSelected(getSelected());
		else if(src == centerOrigin)
			executer.centerOrigin();
		else if(src == centerUniverse)
			executer.centerUniverse();
		else if(src == fitViewToUniverse)
			executer.fitViewToUniverse();
		else if(src == fitViewToContent)
			executer.fitViewToContent(getSelected());
		else if(src == snapshot)
			executer.snapshot();
		else if(src == record360)
			executer.record360();
		else if(src == startRecord)
			executer.startFreehandRecording();
		else if(src == stopRecord)
			executer.stopFreehandRecording();
		else if(src == startAnimation)
			executer.startAnimation();
		else if(src == stopAnimation)
			executer.stopAnimation();
		else if(src == animationOptions)
			executer.changeAnimationOptions();
		else if(src == threshold)
			executer.changeThreshold(getSelected());
		else if(src == displayAsVolume) {
			executer.displayAs(getSelected(), Content.VOLUME);
			updateMenus();
		} else if(src == displayAsOrtho) {
			executer.displayAs(getSelected(), Content.ORTHO);
			updateMenus();
		} else if(src == displayAsSurface) {
			executer.displayAs(getSelected(), Content.SURFACE);
			updateMenus();
		} else if(src == displayAsSurfacePlot) {
			executer.displayAs(
				getSelected(), Content.SURFACE_PLOT2D);
			updateMenus();
		} else if(src == updateVol)
			executer.updateVolume(getSelected());
		else if(src == slices)
			executer.changeSlices(getSelected());
		else if(src == fill)
			executer.fill(getSelected());
		else if(src == close)
			executer.close();
		else if(src == resetTransform)
			executer.resetTransform(getSelected());
		else if(src == setTransform)
			executer.setTransform(getSelected());
		else if(src == properties)
			executer.contentProperties(getSelected());
		else if(src == applyTransform)
			executer.applyTransform(getSelected());
		else if(src == saveTransform)
			executer.saveTransform(getSelected());
		else if(src == exportTransformed)
			executer.exportTransformed(getSelected());
		else if (src == pl_load)
			executer.loadPointList(getSelected());
		else if (src == pl_save)
			executer.savePointList(getSelected());
		else if (src == pl_size)
			executer.changePointSize(getSelected());
		else if (src == saveView)
			executer.saveView();
		else if (src == loadView)
			executer.loadView();
		else if (src == saveSession)
			executer.saveSession();
		else if (src == loadSession)
			executer.loadSession();
		else if (src == importObj)
			executer.importWaveFront();
		else if (src == importStl)
			executer.importSTL();
		else if (src == exportDXF)
			executer.saveAsDXF();
		else if (src == exportObj)
			executer.saveAsWaveFront();
		else if (src == exportAsciiSTL)
			executer.saveAsAsciiSTL();
		else if (src == exportBinarySTL)
			executer.saveAsBinarySTL();
		else if (src == smoothMesh)
			executer.smoothMesh(getSelected());
		else if (src == smoothAllMeshes)
			executer.smoothAllMeshes();
		else if (src == viewPreferences)
			executer.viewPreferences();
		else if(src == j3dproperties)
			executer.j3dproperties();
		else if (viewposXY == src)
			executer.execute(new Runnable() { public void run() { univ.rotateToPositiveXY(); }});
		else if (viewposXZ == src)
			executer.execute(new Runnable() { public void run() { univ.rotateToPositiveXZ(); }});
		else if (viewposYZ == src)
			executer.execute(new Runnable() { public void run() { univ.rotateToPositiveYZ(); }});
		else if (viewnegXY == src)
			executer.execute(new Runnable() { public void run() { univ.rotateToNegativeXY(); }});
		else if (viewnegXZ == src)
			executer.execute(new Runnable() { public void run() { univ.rotateToNegativeXZ(); }});
		else if (viewnegYZ == src)
			executer.execute(new Runnable() { public void run() { univ.rotateToNegativeYZ(); }});
	}

	public void itemStateChanged(ItemEvent e) {
		Object src = e.getSource();
		Content c = getSelected();
		if(src == coordinateSystem)
			executer.showCoordinateSystem(
				c, coordinateSystem.getState());
		else if (src == allCoordinateSystems)
			executer.showAllCoordinateSystems(
				allCoordinateSystems.getState());
		else if(src == boundingBox)
			executer.showBoundingBox(
				c, boundingBox.getState());
		else if(src == show)
			executer.showContent(c, show.getState());
		else if(src == lock)
			executer.setLocked(c, lock.getState());
		else if(src == shaded)
			executer.setShaded(c, shaded.getState());
		else if (src == pl_show)
			executer.showPointList(c, pl_show.getState());
		else if (src == sync)
			executer.sync(sync.getState());
	}

	private Content getSelected() {
		Content c = univ.getSelected();
		if(c != null)
			return c;
		if(univ.getContents().size() == 1)
			return (Content)univ.contents().next();
		return null;
	}







	// Universe Listener interface
	public void transformationStarted(View view) {}
	public void transformationFinished(View view) {}
	public void canvasResized() {}
	public void transformationUpdated(View view) {}
	public void contentChanged(Content c) {}
	public void universeClosed() {}

	public void contentAdded(Content c) {
		updateMenus();
		if(c == null)
			return;
		final String name = c.getName();
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
		item.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(item.getState())
					executer.select(name);
				else
					executer.select(null);
			}
		});
		selectMenu.add(item);
	}

	public void contentRemoved(Content c) {
		updateMenus();
		if(c == null)
			return;
		for(int i = 0; i < selectMenu.getItemCount(); i++) {
			JMenuItem item = selectMenu.getItem(i);
			if(item.getLabel().equals(c.getName())) {
				selectMenu.remove(i);
				return;
			}
		}
	}


	public void contentSelected(Content c) {
		updateMenus();
	}

	public void updateMenus() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				doUpdateMenus();
			}
		});
	}

	private void doUpdateMenus() {

		Content c = getSelected();

		delete.setEnabled(c != null);
		centerSelected.setEnabled(c != null);
		updateVol.setEnabled(c != null);
		fill.setEnabled(c != null);
		smoothMesh.setEnabled(c != null);

		displayAsVolume.setEnabled(c != null);
		displayAsSurface.setEnabled(c != null);
		displayAsSurfacePlot.setEnabled(c != null);
		displayAsOrtho.setEnabled(c != null);
		properties.setEnabled(c != null);

		color.setEnabled(c != null);
		transparency.setEnabled(c != null);
		threshold.setEnabled(c != null);
		channels.setEnabled(c != null);
		shaded.setEnabled(c != null);

		show.setEnabled(c != null);
		coordinateSystem.setEnabled(c != null);

		pl_load.setEnabled(c != null);
		pl_save.setEnabled(c != null);
		pl_show.setEnabled(c != null);
		pl_size.setEnabled(c != null);

		lock.setEnabled(c != null);
		setTransform.setEnabled(c != null);
		applyTransform.setEnabled(c != null);
		resetTransform.setEnabled(c != null);
		saveTransform.setEnabled(c != null);
		exportTransformed.setEnabled(c != null);

		// update select menu
		Content sel = univ.getSelected();
		for(int i = 0; i < selectMenu.getItemCount(); i++) {
			JMenuItem item = selectMenu.getItem(i);
			((JCheckBoxMenuItem)item).setState(sel != null &&
				sel.getName().equals(item.getLabel()));
		}


		if(c == null)
			return;

		int t = c.getType();

		slices.setEnabled(t == Content.ORTHO);
		updateVol.setEnabled(t == Content.VOLUME ||
			t == Content.ORTHO);
		fill.setEnabled(t == Content.VOLUME);
		shaded.setEnabled(t == Content.SURFACE_PLOT2D ||
			t == Content.SURFACE || t == Content.CUSTOM);
		smoothMesh.setEnabled(t == Content.SURFACE || t == Content.CUSTOM);

		coordinateSystem.setState(c.hasCoord());
		lock.setState(c.isLocked());
		show.setState(c.isVisible());
		pl_show.setState(c.isPLVisible());
		shaded.setState(c.isShaded());

		ImagePlus i = c.getImage();
		displayAsVolume.setEnabled(t != Content.VOLUME && i != null);
		displayAsOrtho.setEnabled(t != Content.ORTHO && i != null);
		displayAsSurface.setEnabled(t != Content.SURFACE && i != null);
		displayAsSurfacePlot.setEnabled(
				t != Content.SURFACE_PLOT2D && i != null);
	}
}

