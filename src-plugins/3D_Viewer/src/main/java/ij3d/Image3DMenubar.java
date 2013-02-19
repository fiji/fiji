package ij3d;

import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.media.j3d.View;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;


@SuppressWarnings("serial")
public class Image3DMenubar extends JMenuBar implements ActionListener,
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;
	private Executer executer;

	private JMenuItem open;
	private JMenuItem addContentFromImage;
	private JMenuItem saveView;
	private JMenuItem loadView;
	private JMenuItem saveSession;
	private JMenuItem loadSession;
	private JMenuItem importObj;
	private JMenuItem importStl;
	private JMenuItem color;
	private JMenuItem bgColor;
	private JCheckBoxMenuItem fullscreen;
	private JMenuItem channels;
	private JMenuItem luts;
	private JMenuItem transparency;
	private JMenuItem threshold;
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
	private JMenuItem light;
	private JMenuItem viewPreferences;
	private JMenuItem shortcuts;
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
	private JMenuItem exportU3D;
	private JMenuItem scalebar;
	private JMenuItem displayAsVolume;
	private JMenuItem displayAsOrtho;
	private JMenuItem displayAsMultiOrtho;
	private JMenuItem displayAsSurface;
	private JMenuItem displayAsSurfacePlot;
	private JMenuItem centerSelected;
	private JMenuItem centerOrigin;
	private JMenuItem centerUniverse;
	private JCheckBoxMenuItem sync;
	private JMenuItem fitViewToUniverse;
	private JMenuItem fitViewToContent;
	private JMenuItem regist;
	private JMenuItem pl_load;
	private JMenuItem pl_save;
	private JMenuItem pl_size;
	private JMenuItem pl_color;
	private JCheckBoxMenuItem pl_show;
	private JMenuItem j3dproperties;
	private JCheckBoxMenuItem coordinateSystem;
	private JCheckBoxMenuItem boundingBox;
	private JCheckBoxMenuItem allCoordinateSystems;
	private JCheckBoxMenuItem lock;
	private JCheckBoxMenuItem show;
	private JMenuItem viewposXY, viewposXZ, viewposYZ, viewnegXY, viewnegXZ, viewnegYZ;
	private JMenuItem sphere, box, cone, tube;

	private JMenu transformMenu;
	private JMenu landmarksMenu;
	private JMenu editMenu;
	private JMenu selectMenu;
	private JMenu viewMenu;
	private JMenu fileMenu;
	private JMenu helpMenu;
	private JMenu addMenu;

	public Image3DMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;
		this.executer = univ.getExecuter();

		univ.addUniverseListener(this);

		fileMenu = createFileMenu();
		this.add(fileMenu);

		editMenu = createEditMenu();
		this.add(editMenu);

		viewMenu = createViewMenu();
		this.add(viewMenu);

		addMenu = createAddMenu();
		this.add(addMenu);

		landmarksMenu = createLandmarkMenu();
		this.add(landmarksMenu);

		helpMenu = createHelpMenu();
		this.add(helpMenu);

		contentSelected(null);
	}

	public JMenu createFileMenu() {
		JMenu file = new JMenu("File");

		open = new JMenuItem("Open...");
		open.addActionListener(this);
		file.add(open);

		JMenu importt = new JMenu("Import surfaces");

		importObj = new JMenuItem("WaveFront");
		importObj.addActionListener(this);
		importt.add(importObj);

		importStl = new JMenuItem("STL");
		importStl.addActionListener(this);
		importt.add(importStl);


		file.add(importt);

		JMenu subMenu = new JMenu("Export surfaces");
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

		exportU3D = new JMenuItem("U3D");
		exportU3D.addActionListener(this);
		subMenu.add(exportU3D);

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

		close = new JMenuItem("Quit");
		close.addActionListener(this);
		file.add(close);

		return file;
	}

	public JMenu createAddMenu() {
		JMenu add = new JMenu("Add");
		addContentFromImage = new JMenuItem("From image");
		addContentFromImage.addActionListener(this);
		add.add(addContentFromImage);

		add.addSeparator();

		sphere = new JMenuItem("Sphere");
		sphere.addActionListener(this);
		add.add(sphere);

		box = new JMenuItem("Box");
		box.addActionListener(this);
		add.add(box);

		cone = new JMenuItem("Cone");
		cone.addActionListener(this);
		add.add(cone);

		tube = new JMenuItem("Tube");
		tube.addActionListener(this);
		add.add(tube);

		return add;
	}

	public JMenu createEditMenu() {
		JMenu edit = new JMenu("Edit");

		edit.add(createDisplayAsSubMenu());
		// TODO:
		selectMenu = createSelectMenu();
		edit.add(selectMenu);

		edit.addSeparator();

		luts = new JMenuItem("Transfer function");
		luts.addActionListener(this);
		edit.add(luts);

		channels = new JMenuItem("Change channels");
		channels.addActionListener(this);
		edit.add(channels);

		color = new JMenuItem("Change color");
		color.addActionListener(this);
		edit.add(color);

		transparency = new JMenuItem("Change transparency");
		transparency.addActionListener(this);
		edit.add(transparency);

		threshold = new JMenuItem("Adjust threshold");
		threshold.addActionListener(this);
		edit.add(threshold);

		edit.addSeparator();

		show = new JCheckBoxMenuItem("Show content");
		show.setState(true);
		show.addItemListener(this);
		edit.add(show);

		coordinateSystem = new JCheckBoxMenuItem(
					"Show coordinate system", true);
		coordinateSystem.addItemListener(this);
		edit.add(coordinateSystem);

		boundingBox = new JCheckBoxMenuItem(
					"Show bounding box", false);
		boundingBox.addItemListener(this);
		edit.add(boundingBox);

		allCoordinateSystems = new JCheckBoxMenuItem(
				"Show all coordinate systems", true);
		allCoordinateSystems.addItemListener(this);
		edit.add(allCoordinateSystems);

		edit.addSeparator();

		delete = new JMenuItem("Delete");
		delete.setEnabled(false);
		delete.addActionListener(this);
		edit.add(delete);

		edit.addSeparator();

		properties = new JMenuItem("Object Properties");
		properties.addActionListener(this);
		edit.add(properties);

		edit.addSeparator();


		transformMenu = createTransformMenu();
		edit.add(transformMenu);

		edit.addSeparator();

		shortcuts = new JMenuItem("Keyboard shortcuts");
		shortcuts.addActionListener(this);
		edit.add(shortcuts);

		viewPreferences = new JMenuItem("View Preferences");
		viewPreferences.addActionListener(this);
		edit.add(viewPreferences);

		return edit;
	}

	private JMenu createLandmarkMenu() {
		JMenu pl = new JMenu("Landmarks");

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

		pl_color = new JMenuItem("Point color");
		pl_color.addActionListener(this);
		pl.add(pl_color);

		pl.addSeparator();

		regist = new JMenuItem("Register");
		regist.addActionListener(this);
		pl.add(regist);

		return pl;
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

		light = new JMenuItem("Adjust light");
		light.addActionListener(this);
		view.add(light);

		bgColor = new JMenuItem("Change background color");
		bgColor.addActionListener(this);
		view.add(bgColor);

		fullscreen = new JCheckBoxMenuItem("Fullscreen");
		fullscreen.setState(univ.isFullScreen());
		fullscreen.addItemListener(this);
		view.add(fullscreen);

		return view;
	}

	public JMenu createHelpMenu() {
		JMenu help = new JMenu("Help");
		j3dproperties = new JMenuItem("Java 3D Properties");
		j3dproperties.addActionListener(this);
		help.add(j3dproperties);
		return help;
	}

	public JMenu createAttributesSubMenu() {
		JMenu attributes = new JMenu("Attributes");




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

		displayAsMultiOrtho = new JMenuItem("Multi-orthoslice");
		displayAsMultiOrtho.addActionListener(this);
		display.add(displayAsMultiOrtho);

		displayAsSurface = new JMenuItem("Surface");
		displayAsSurface.addActionListener(this);
		display.add(displayAsSurface);

		displayAsSurfacePlot = new JMenuItem("Surface Plot 2D");
		displayAsSurfacePlot.addActionListener(this);
		display.add(displayAsSurfacePlot);

		return display;
	}

	@Override
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
		else if(src == open)
			executer.addContentFromFile();
		else if(src == addContentFromImage)
			executer.addContentFromImage(null);
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
		} else if(src == displayAsMultiOrtho) {
			executer.displayAs(getSelected(), Content.MULTIORTHO);
			updateMenus();
		} else if(src == displayAsSurface) {
			executer.displayAs(getSelected(), Content.SURFACE);
			updateMenus();
		} else if(src == displayAsSurfacePlot) {
			executer.displayAs(
				getSelected(), Content.SURFACE_PLOT2D);
			updateMenus();
		} else if(src == close)
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
		else if (src == pl_color)
			executer.changePointColor(getSelected());
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
		else if (src == exportU3D)
			executer.saveAsU3D();
		else if (src == exportAsciiSTL)
			executer.saveAsAsciiSTL();
		else if (src == exportBinarySTL)
			executer.saveAsBinarySTL();
		else if (src == light)
			executer.adjustLight();
		else if (src == viewPreferences)
			executer.viewPreferences();
		else if (src == shortcuts)
			executer.editShortcuts();
		else if(src == j3dproperties)
			executer.j3dproperties();
		else if (viewposXY == src)
			executer.execute(new Runnable() { @Override
			public void run() { univ.rotateToPositiveXY(); }});
		else if (viewposXZ == src)
			executer.execute(new Runnable() { @Override
			public void run() { univ.rotateToPositiveXZ(); }});
		else if (viewposYZ == src)
			executer.execute(new Runnable() { @Override
			public void run() { univ.rotateToPositiveYZ(); }});
		else if (viewnegXY == src)
			executer.execute(new Runnable() { @Override
			public void run() { univ.rotateToNegativeXY(); }});
		else if (viewnegXZ == src)
			executer.execute(new Runnable() { @Override
			public void run() { univ.rotateToNegativeXZ(); }});
		else if (viewnegYZ == src)
			executer.execute(new Runnable() { @Override
			public void run() { univ.rotateToNegativeYZ(); }});
		else if(src == sphere) {
			executer.addSphere();
		} else if(src == box) {
			executer.addBox();
		} else if(src == cone) {
			executer.addCone();
		} else if(src == tube) {
			executer.addTube();
		}
	}

	@Override
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
		else if (src == pl_show)
			executer.showPointList(c, pl_show.getState());
		else if (src == sync)
			executer.sync(sync.getState());
		else if (src == fullscreen)
			executer.setFullScreen(fullscreen.getState());
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
	@Override
	public void transformationStarted(View view) {}
	@Override
	public void transformationFinished(View view) {}
	@Override
	public void canvasResized() {}
	@Override
	public void transformationUpdated(View view) {}
	@Override
	public void contentChanged(Content c) {}
	@Override
	public void universeClosed() {}

	@Override
	public void contentAdded(Content c) {
		updateMenus();
		if(c == null)
			return;
		final String name = c.getName();
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
		item.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(item.getState())
					executer.select(name);
				else
					executer.select(null);
			}
		});
		selectMenu.add(item);
	}

	@Override
	public void contentRemoved(Content c) {
		updateMenus();
		if(c == null)
			return;
		for(int i = 0; i < selectMenu.getItemCount(); i++) {
			JMenuItem item = selectMenu.getItem(i);
			if(item.getText().equals(c.getName())) {
				selectMenu.remove(i);
				return;
			}
		}
	}


	@Override
	public void contentSelected(Content c) {
		updateMenus();
	}

	public void updateMenus() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doUpdateMenus();
			}
		});
	}

	private void doUpdateMenus() {

		fullscreen.setState(univ.isFullScreen());

		Content c = getSelected();

		delete.setEnabled(c != null);
		centerSelected.setEnabled(c != null);

		displayAsVolume.setEnabled(c != null);
		displayAsSurface.setEnabled(c != null);
		displayAsSurfacePlot.setEnabled(c != null);
		displayAsOrtho.setEnabled(c != null);
		properties.setEnabled(c != null);

		color.setEnabled(c != null);
		transparency.setEnabled(c != null);
		threshold.setEnabled(c != null);
		channels.setEnabled(c != null);

		show.setEnabled(c != null);
		coordinateSystem.setEnabled(c != null);

		pl_load.setEnabled(c != null);
		pl_save.setEnabled(c != null);
		pl_show.setEnabled(c != null);
		pl_size.setEnabled(c != null);
		pl_color.setEnabled(c != null);

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
				sel.getName().equals(item.getText()));
		}


		if(c == null)
			return;

		int t = c.getType();

		coordinateSystem.setState(c.hasCoord());
		lock.setState(c.isLocked());
		show.setState(c.isVisible());
		pl_show.setState(c.isPLVisible());

		ImagePlus i = c.getImage();
		displayAsVolume.setEnabled(t != Content.VOLUME && i != null);
		displayAsOrtho.setEnabled(t != Content.ORTHO && i != null);
		displayAsSurface.setEnabled(t != Content.SURFACE && i != null);
		displayAsSurfacePlot.setEnabled(
				t != Content.SURFACE_PLOT2D && i != null);
		displayAsMultiOrtho.setEnabled(t != Content.MULTIORTHO && i != null);
	}
}

