package ij3d;

import ij.ImagePlus;

import java.awt.event.*;
import java.awt.*;
import java.util.Iterator;

import javax.media.j3d.View;


public class Image3DMenubar extends MenuBar implements ActionListener, 
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;
	private Executer executer;

	private MenuItem addContentFromFile;
	private MenuItem addContentFromImage;
	private MenuItem add4DFromFile;
	private MenuItem add4DFromImage;
	private MenuItem add4DFromFolder;
	private MenuItem saveView;
	private MenuItem loadView;
	private MenuItem saveSession;
	private MenuItem loadSession;
	private MenuItem importObj;
	private MenuItem color;
	private MenuItem bgColor;
	private MenuItem channels;
	private MenuItem transparency;
	private MenuItem threshold;
	private MenuItem fill;
	private MenuItem slices;
	private MenuItem updateVol;
	private MenuItem delete;
	private MenuItem properties;
	private MenuItem resetView;
	private MenuItem record360;
	private MenuItem startRecord;
	private MenuItem stopRecord;
	private MenuItem startAnimation;
	private MenuItem stopAnimation;
	private MenuItem animationOptions;
	private MenuItem viewPreferences;
	private MenuItem close;
	private MenuItem setTransform;
	private MenuItem resetTransform;
	private MenuItem applyTransform;
	private MenuItem saveTransform;
	private MenuItem exportTransformed;
	private MenuItem exportObj;
	private MenuItem exportDXF;
	private MenuItem smoothMesh;
	private MenuItem scalebar;
	private MenuItem smoothAllMeshes;
	private MenuItem displayAsVolume;
	private MenuItem displayAsOrtho;
	private MenuItem displayAsSurface;
	private MenuItem displayAsSurfacePlot;
	private MenuItem centerSelected;
	private MenuItem centerOrigin;
	private MenuItem centerUniverse;
	private MenuItem fitViewToUniverse;
	private MenuItem fitViewToContent;
	private MenuItem regist;
	private CheckboxMenuItem shaded;
	private MenuItem pl_load;
	private MenuItem pl_save;
	private MenuItem pl_size;
	private CheckboxMenuItem pl_show;
	private MenuItem j3dproperties;
	private CheckboxMenuItem coordinateSystem;
	private CheckboxMenuItem boundingBox;
	private CheckboxMenuItem allCoordinateSystems;
	private CheckboxMenuItem lock;
	private CheckboxMenuItem show;
	private MenuItem viewposXY, viewposXZ, viewposYZ, viewnegXY, viewnegXZ, viewnegYZ;

	private Menu transformMenu;
	private Menu editMenu;
	private Menu selectMenu;
	private Menu viewMenu;
	private Menu fileMenu;
	private Menu helpMenu;

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
		this.setHelpMenu(helpMenu);

		contentSelected(null);
	}

	public Menu createFileMenu() {
		Menu file = new Menu("File");

		Menu addContent = new Menu("Add content");
		addContentFromFile = new MenuItem("from file");
		addContentFromFile.addActionListener(this);
		addContent.add(addContentFromFile);
		addContentFromImage = new MenuItem("from open image");
		addContentFromImage.addActionListener(this);
		addContent.add(addContentFromImage);
		file.add(addContent);

		Menu add4D = new Menu("Add timelapse");
		add4DFromFile = new MenuItem("from hyperstack file");
		add4DFromFile.addActionListener(this);
		add4D.add(add4DFromFile);
		add4DFromImage = new MenuItem("from open hyperstack");
		add4DFromImage.addActionListener(this);
		add4D.add(add4DFromImage);
		add4DFromFolder = new MenuItem("from folder with stacks");
		add4DFromFolder.addActionListener(this);
		add4D.add(add4DFromFolder);
		file.add(add4D);


		importObj = new MenuItem("Import WaveFront");
		importObj.addActionListener(this);
		file.add(importObj);

		delete = new MenuItem("Delete");
		delete.setEnabled(false);
		delete.addActionListener(this);
		file.add(delete);

		file.addSeparator();

		saveView = new MenuItem("Save View");
		saveView.addActionListener(this);
		file.add(saveView);

		loadView = new MenuItem("Load View");
		loadView.addActionListener(this);
		file.add(loadView);

		file.addSeparator();

		saveSession = new MenuItem("Save Session");
		saveSession.addActionListener(this);
		file.add(saveSession);

		loadSession = new MenuItem("Load Session");
		loadSession.addActionListener(this);
		file.add(loadSession);

		file.addSeparator();

		Menu subMenu = new Menu("Export surfaces as");
		file.add(subMenu);
		exportObj = new MenuItem("WaveFront");
		exportObj.addActionListener(this);
		subMenu.add(exportObj);

		exportDXF = new MenuItem("DXF");
		exportDXF.addActionListener(this);
		subMenu.add(exportDXF);

		file.addSeparator();

		close = new MenuItem("Quit");
		close.addActionListener(this);
		file.add(close);

		return file;
	}

	public Menu createEditMenu() {
		Menu edit = new Menu("Edit");
		
		slices = new MenuItem("Adjust slices");
		slices.addActionListener(this);
		edit.add(slices);
		
		updateVol = new MenuItem("Upate Volume");
		updateVol.addActionListener(this);
		edit.add(updateVol);

		fill = new MenuItem("Fill selection");
		fill.addActionListener(this);
		edit.add(fill);

		smoothMesh = new MenuItem("Smooth mesh");
		smoothMesh.addActionListener(this);
		edit.add(smoothMesh);

		smoothAllMeshes = new MenuItem("Smooth all meshes");
		smoothAllMeshes.addActionListener(this);
		edit.add(smoothAllMeshes);

		edit.addSeparator();
		
		edit.add(createDisplayAsSubMenu());
		edit.add(createAttributesSubMenu());
		edit.add(createHideSubMenu());
		edit.add(createPLSubMenu());
	
		edit.addSeparator();

		regist = new MenuItem("Register");
		regist.addActionListener(this);
		edit.add(regist);

		edit.addSeparator();

		properties = new MenuItem("Object Properties");
		properties.addActionListener(this);
		edit.add(properties);

		return edit;
	}

	public Menu createSelectMenu() {
		return new Menu("Select");
	}

	public Menu createTransformMenu() {
		Menu transform = new Menu("Transformation");

		lock = new CheckboxMenuItem("Lock");
		lock.addItemListener(this);
		transform.add(lock);
		
		setTransform = new MenuItem("Set Transform");
		setTransform.addActionListener(this);
		transform.add(setTransform);

		resetTransform = new MenuItem("Reset Transform");
		resetTransform.addActionListener(this);
		transform.add(resetTransform);

		applyTransform = new MenuItem("Apply Transform");
		applyTransform.addActionListener(this);
		transform.add(applyTransform);

		saveTransform = new MenuItem("Save Transform");
		saveTransform.addActionListener(this);
		transform.add(saveTransform);

		transform.addSeparator();

		exportTransformed= new MenuItem("Export transformed image");
		exportTransformed.addActionListener(this);
		transform.add(exportTransformed);

		return transform;
	}
	public Menu createViewMenu() {
		Menu view = new Menu("View");

		resetView = new MenuItem("Reset view");
		resetView.addActionListener(this);
		view.add(resetView);

		// center submenu
		Menu menu = new Menu("Center");
		centerSelected = new MenuItem("Selected content");
		centerSelected.addActionListener(this);
		menu.add(centerSelected);

		centerOrigin = new MenuItem("Origin");
		centerOrigin.addActionListener(this);
		menu.add(centerOrigin);

		centerUniverse = new MenuItem("Universe");
		centerUniverse.addActionListener(this);
		menu.add(centerUniverse);
		view.add(menu);

		// fit view submenu
		menu = new Menu("Fit view to");
		fitViewToUniverse = new MenuItem("Universe");
		fitViewToUniverse.addActionListener(this);
		menu.add(fitViewToUniverse);

		fitViewToContent = new MenuItem("Selected content");
		fitViewToContent.addActionListener(this);
		menu.add(fitViewToContent);
		view.add(menu);

		menu = new Menu("Set view");
		viewposXY = new MenuItem("+ XY"); viewposXY.addActionListener(this); menu.add(viewposXY);
		viewposXZ = new MenuItem("+ XZ"); viewposXZ.addActionListener(this); menu.add(viewposXZ);
		viewposYZ = new MenuItem("+ YZ"); viewposYZ.addActionListener(this); menu.add(viewposYZ);
		viewnegXY = new MenuItem("- XY"); viewnegXY.addActionListener(this); menu.add(viewnegXY);
		viewnegXZ = new MenuItem("- XZ"); viewnegXZ.addActionListener(this); menu.add(viewnegXZ);
		viewnegYZ = new MenuItem("- YZ"); viewnegYZ.addActionListener(this); menu.add(viewnegYZ);
		view.add(menu);

		view.addSeparator();

		record360 = new MenuItem("Record 360 deg rotation");
		record360.addActionListener(this);
		view.add(record360);

		startRecord = new MenuItem("Start freehand recording");
		startRecord.addActionListener(this);
		view.add(startRecord);

		stopRecord = new MenuItem("Stop freehand recording");
		stopRecord.addActionListener(this);
		view.add(stopRecord);

		view.addSeparator();

		startAnimation = new MenuItem("Start animation");
		startAnimation.addActionListener(this);
		view.add(startAnimation);

		stopAnimation = new MenuItem("Stop animation");
		stopAnimation.addActionListener(this);
		view.add(stopAnimation);

		animationOptions = new MenuItem("Change animation options");
		animationOptions.addActionListener(this);
		view.add(animationOptions);

		view.addSeparator();

		scalebar = new MenuItem("Edit Scalebar");
		scalebar.addActionListener(this);
		view.add(scalebar);

		view.addSeparator();

		viewPreferences = new MenuItem("View Preferences");
		viewPreferences.addActionListener(this);
		view.add(viewPreferences);

		bgColor = new MenuItem("Change background color");
		bgColor.addActionListener(this);
		view.add(bgColor);

		return view;
	}

	public Menu createHelpMenu() {
		Menu help = new Menu("Help");
		j3dproperties = new MenuItem("Java 3D Properties");
		j3dproperties.addActionListener(this);
		help.add(j3dproperties);
		return help;
	}


	public Menu createPLSubMenu() {
		Menu pl = new Menu("Point list");
		if(univ == null)
			return pl;
		pl_load = new MenuItem("Load Point List");
		pl_load.addActionListener(this);
		pl.add(pl_load);

		pl_save = new MenuItem("Save Point List");
		pl_save.addActionListener(this);
		pl.add(pl_save);

		pl_show = new CheckboxMenuItem("Show Point List");
		pl_show.addItemListener(this);
		pl.add(pl_show);

		pl.addSeparator();

		pl_size = new MenuItem("Point size");
		pl_size.addActionListener(this);
		pl.add(pl_size);

		return pl;
	}


	public Menu createHideSubMenu() {
		Menu hide = new Menu("Hide/Show");

		show = new CheckboxMenuItem("Show content");
		show.setState(true);
		show.addItemListener(this);
		hide.add(show);

		coordinateSystem = new CheckboxMenuItem(
					"Show coordinate system", true);
		coordinateSystem.addItemListener(this);
		hide.add(coordinateSystem);

		boundingBox = new CheckboxMenuItem(
					"Show bounding box", false);
		boundingBox.addItemListener(this);
		hide.add(boundingBox);

		allCoordinateSystems = new CheckboxMenuItem(
				"Show all coordinate systems", true);
		allCoordinateSystems.addItemListener(this);
		hide.add(allCoordinateSystems);

		return hide;
	}

	public Menu createAttributesSubMenu() {
		Menu attributes = new Menu("Attributes");

		channels = new MenuItem("Change channels");
		channels.addActionListener(this);
		attributes.add(channels);

		color = new MenuItem("Change color");
		color.addActionListener(this);
		attributes.add(color);

		transparency = new MenuItem("Change transparency");
		transparency.addActionListener(this);
		attributes.add(transparency);

		threshold = new MenuItem("Adjust threshold");
		threshold.addActionListener(this);
		attributes.add(threshold);

		shaded = new CheckboxMenuItem("Shade surface");
		shaded.setState(true);
		shaded.addItemListener(this);
		attributes.add(shaded);

		return attributes;
	}

	public Menu createDisplayAsSubMenu() {
		Menu display = new Menu("Display as");

		displayAsVolume = new MenuItem("Volume");
		displayAsVolume.addActionListener(this);
		display.add(displayAsVolume);

		displayAsOrtho = new MenuItem("Orthoslice");
		displayAsOrtho.addActionListener(this);
		display.add(displayAsOrtho);

		displayAsSurface = new MenuItem("Surface");
		displayAsSurface.addActionListener(this);
		display.add(displayAsSurface);

		displayAsSurfacePlot = new MenuItem("Surface Plot 2D");
		displayAsSurfacePlot.addActionListener(this);
		display.add(displayAsSurfacePlot);

		return display;
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if(src == color)
			executer.changeColor(univ.getSelected());
		else if (src == bgColor)
			executer.changeBackgroundColor();
		else if(src == scalebar)
			executer.editScalebar();
		else if(src == channels)
			executer.changeChannels(univ.getSelected());
		else if(src == transparency)
			executer.changeTransparency(univ.getSelected());
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
			executer.delete(univ.getSelected());
		else if(src == resetView)
			executer.resetView();
		else if(src == centerSelected)
			executer.centerSelected(univ.getSelected());
		else if(src == centerOrigin)
			executer.centerOrigin();
		else if(src == centerUniverse)
			executer.centerUniverse();
		else if(src == fitViewToUniverse)
			executer.fitViewToUniverse();
		else if(src == fitViewToContent)
			executer.fitViewToContent(univ.getSelected());
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
			executer.changeThreshold(univ.getSelected());
		else if(src == displayAsVolume)
			executer.displayAs(univ.getSelected(), Content.VOLUME);
		else if(src == displayAsOrtho)
			executer.displayAs(univ.getSelected(), Content.ORTHO);
		else if(src == displayAsSurface)
			executer.displayAs(univ.getSelected(), Content.SURFACE);
		else if(src == displayAsSurfacePlot)
			executer.displayAs(
				univ.getSelected(), Content.SURFACE_PLOT2D);
		else if(src == updateVol)
			executer.updateVolume(univ.getSelected());
		else if(src == slices)
			executer.changeSlices(univ.getSelected());
		else if(src == fill)
			executer.fill(univ.getSelected());
		else if(src == close)
			executer.close();
		else if(src == resetTransform)
			executer.resetTransform(univ.getSelected());
		else if(src == setTransform)
			executer.setTransform(univ.getSelected());
		else if(src == properties)
			executer.contentProperties(univ.getSelected());
		else if(src == applyTransform)
			executer.applyTransform(univ.getSelected());
		else if(src == saveTransform)
			executer.saveTransform(univ.getSelected());
		else if(src == exportTransformed)
			executer.exportTransformed(univ.getSelected());
		else if (src == pl_load)
			executer.loadPointList(univ.getSelected());
		else if (src == pl_save)
			executer.savePointList(univ.getSelected());
		else if (src == pl_size)
			executer.changePointSize(univ.getSelected());
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
		else if (src == exportDXF)
			executer.saveAsDXF();
		else if (src == exportObj)
			executer.saveAsWaveFront();
		else if (src == smoothMesh)
			executer.smoothMesh(univ.getSelected());
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
		Content c = univ.getSelected();
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
	}







	// Universe Listener interface
	public void transformationStarted(View view) {}
	public void transformationFinished(View view) {}
	public void canvasResized() {}
	public void transformationUpdated(View view) {}
	public void contentChanged(Content c) {}
	public void universeClosed() {}

	public void contentAdded(Content c) {
		if(c == null)
			return;
		final String name = c.getName();
		final CheckboxMenuItem item = new CheckboxMenuItem(name);
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
		if(c == null)
			return;
		for(int i = 0; i < selectMenu.getItemCount(); i++) {
			MenuItem item = selectMenu.getItem(i);
			if(item.getLabel().equals(c.getName())) {
				selectMenu.remove(i);
				return;
			}
		}
	}

	public void contentSelected(Content c) {

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
		for(int i = 0; i < selectMenu.getItemCount(); i++) {
			MenuItem item = selectMenu.getItem(i);
			((CheckboxMenuItem)item).setState(c != null &&
				c.getName().equals(item.getLabel()));
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

