package ij3d;

import ij3d.pointlist.PointListDialog;
import ij.ImagePlus;
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.MenuBar;
import java.awt.event.*;

import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;

import customnode.MeshLoader;
import customnode.CustomLineMesh;
import customnode.CustomPointMesh;
import customnode.CustomQuadMesh;
import customnode.CustomMesh;
import customnode.CustomMultiMesh;
import customnode.CustomMeshNode;
import customnode.CustomTriangleMesh;

import view4d.TimelineGUI;
import view4d.Timeline;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.io.IOException;
import java.io.File;
import octree.FilePreparer;
import octree.VolumeOctree;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class Image3DUniverse extends DefaultAnimatableUniverse {

	public static ArrayList<Image3DUniverse> universes =
				new ArrayList<Image3DUniverse>();

	/** The current time point */
	private int currentTimepoint = 0;

	/** The global start time */
	private int startTime = 0;

	/** The global end time */
	private int endTime = 0;

	/** The timeline object for animation accross 4D */
	private final Timeline timeline;

	/** The GUI controlling the timeline */
	private TimelineGUI timelineGUI;

	/** The selected Content */
	private Content selected;

	/**
	 * A Hashtable which stores the Contents of this universe. Keys in
	 * the table are the names of the Contents.
	 */
	private Hashtable<String, Content> contents =
				new Hashtable<String, Content>();

	/** A reference to the Image3DMenubar */
	private Image3DMenubar menubar;

	/** A reference to the RegistrationMenubar */
	private RegistrationMenubar registrationMenubar;

	/** A reference to the ImageCanvas3D */
	private ImageCanvas3D canvas;

	/** A reference to the Executer */
	private Executer executer;

	/**
	 * A flag indicating whether the view is adjusted each time a
	 * Content is added
	 */
	private boolean autoAdjustView = true;

	private PointListDialog plDialog;

	/**
	 * An object used for synchronizing.
	 * Synchronized methods in a subclass of SimpleUniverse should
	 * be avoided, since Java3D uses it obviously internally for
	 * locking.
	 */
	private final Object lock = new Object();

	static{
		UniverseSettings.load();
	}

	/**
	 * Default constructor.
	 * Creates a new universe using the Universe settings - either default
	 * settings or stored settings.
	 */
	public Image3DUniverse() {
		this(UniverseSettings.startupWidth, UniverseSettings.startupHeight);
	}

	/**
	 * Constructs a new universe with the specified width and height.
	 * @param width
	 * @param height
	 */
	public Image3DUniverse(int width, int height) {
		super(width, height);
		canvas = (ImageCanvas3D)getCanvas();
		executer = new Executer(this);
		this.timeline = new Timeline(this);
		this.timelineGUI = new TimelineGUI(timeline);

		BranchGroup bg = new BranchGroup();
		scene.addChild(bg);

		resetView();

		// add mouse listeners
		canvas.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Content c = picker.getPickedContent(
						e.getX(), e.getY());
				if(c != null)
					IJ.showStatus(c.getName());
				else
					IJ.showStatus("");
			}
		});
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Content c = picker.getPickedContent(
						e.getX(), e.getY());
				select(c);
			}
		});

		universes.add(this);
	}

	/**
	 * Display this universe in a window (an ImageWindow3D).
	 */
	@Override
	public void show() {
		super.show();
		plDialog = new PointListDialog(win);
		plDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				hideAllLandmarks();
			}
		});
		menubar = new Image3DMenubar(this);
		registrationMenubar = new RegistrationMenubar(this);
		setMenubar(menubar);
	}

	/**
	 * Close this universe. Remove all Contents and release all resources.
	 */
	@Override
	public void close() {
		timeline.pause();
		removeAllContents();
		contents = null;
		universes.remove(this);
		adder.shutdownNow();
		WindowListener[] ls = plDialog.getWindowListeners();
		for(WindowListener l : ls)
			plDialog.removeWindowListener(l);
		plDialog.dispose();
		super.close();
	}

	/**
	 * Shows the specified status string at the bottom of the viewer window.
	 * @param text
	 */
	public void setStatus(String text) {
// 		if(win != null)
// 			win.getStatusLabel().setText("  " + text);
	}

	/**
	 * Set a custom menu bar to the viewer
	 * @param mb
	 */
	public void setMenubar(MenuBar mb) {
		if(win != null)
			win.setMenuBar(mb);
	}

	/**
	 * Returns a reference to the menu bar used by this universe.
	 * @return
	 */
	public MenuBar getMenuBar() {
		return menubar;
	}

	/**
	 * Returns a reference to the registration menu bar.
	 */
	public RegistrationMenubar getRegistrationMenuBar() {
		return registrationMenubar;
	}

	/**
	 * Returns a reference to the Executer used by this universe.
	 * @return
	 */
	public Executer getExecuter() {
		return executer;
	}

	/**
	 * Returns a reference to the PointListDialog used by this universe
	 */
	public PointListDialog getPointListDialog() {
		return plDialog;
	}

	/**
	 * Hide point list dialog and all landmark points.
	 */
	public void hideAllLandmarks() {
		for(Content c : contents.values()) {
			c.showPointList(false);
		}
		// just for now, to update the menu bar
		fireContentSelected(selected);
	}

	/* *************************************************************
	 * Session methods
	 * *************************************************************/
	public void saveSession(String file) throws IOException {
		SaveSession.saveScene(this, file);
	}

	public void loadSession(final String file) throws IOException {
		removeAllContents();
		SaveSession.loadScene(this, file);
	}

	/* *************************************************************
	 * Timeline stuff
	 * *************************************************************/
	public Timeline getTimeline() {
		return timeline;
	}

	public void showTimepoint(int tp) {
		this.currentTimepoint = tp;
		for(Content c : contents.values())
			c.showTimepoint(tp);
		if(timelineGUIVisible)
			timelineGUI.updateTimepoint(tp);
	}

	public int getCurrentTimepoint() {
		return currentTimepoint;
	}

	public int getStartTime() {
		return startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public void updateStartAndEndTime(int st, int e) {
		this.startTime = st;
		this.endTime = e;
		updateTimelineGUI();
	}

	public void updateTimeline() {
		if(contents.size() == 0)
			startTime = endTime = 0;
		else {
			startTime = Integer.MAX_VALUE;
			endTime = Integer.MIN_VALUE;
			for(Content c : contents.values()) {
				if(c.getStartTime() < startTime)
					startTime = c.getStartTime();
				if(c.getEndTime() > endTime)
					endTime = c.getEndTime();
			}
		}
		if(currentTimepoint > endTime)
			showTimepoint(endTime);
		else if(currentTimepoint < startTime)
			showTimepoint(startTime);
		updateTimelineGUI();
	}

	boolean timelineGUIVisible = false;
	public void updateTimelineGUI() {
		if(win == null)
			return;
		if(endTime != startTime && !timelineGUIVisible) {
			win.add(timelineGUI.getPanel(), BorderLayout.SOUTH, -1);
			timelineGUIVisible = true;
			win.pack();
		} else if(endTime == startTime && timelineGUIVisible) {
			win.remove(timelineGUI.getPanel());
			timelineGUIVisible = false;
			win.pack();
		}
		if(timelineGUIVisible) {
			timelineGUI.updateStartAndEnd(startTime, endTime);
		}
	}


	/* *************************************************************
	 * Selection methods
	 * *************************************************************/
	/**
	 * Select the specified Content. If another Content is already selected,
	 * it will be deselected. fireContentSelected() is thrown.
	 * @param c
	 */
	public void select(Content c) {
		if(selected != null) {
			selected.setSelected(false);
			selected = null;
		}
		if(c != null && c.isVisibleAt(currentTimepoint)) {
			c.setSelected(true);
			selected = c;
		}
		String st = c != null ? c.getName() : "none";
		IJ.showStatus("selected: " + st);

		fireContentSelected(selected);

		if(c != null && ij.plugin.frame.Recorder.record)
			Executer.record("select", c.getName());
	}

	/**
	 * Returns the selected Content, or null if none is selected.
	 */
	public Content getSelected() {
		return selected;
	}

	/**
	 * If any Content is selected, deselects it.
	 */
	public void clearSelection() {
		if(selected != null)
			selected.setSelected(false);
		selected = null;
		fireContentSelected(null);
	}

	/* *************************************************************
	 * Dimensions
	 * *************************************************************/
	/**
	 * autoAdjustView indicates, whether the view is adjusted to
	 * fit the whole universe each time a Content is added.
	 */
	public void setAutoAdjustView(boolean b) {
		autoAdjustView = b;
	}

	/**
	 * autoAdjustView indicates, whether the view is adjusted to
	 * fit the whole universe each time a Content is added.
	 */
	public boolean getAutoAdjustView() {
		return autoAdjustView;
	}

	/**
	 * Calculates the global minimum, maximum and center point depending
	 * on all the available contents.
	 */
	public void recalculateGlobalMinMax() {
		if(contents.isEmpty())
			return;
		Point3d min = new Point3d();
		Point3d max = new Point3d();

		Iterator it = contents();
		Content c = (Content)it.next();
		c.getMin(min);
		c.getMax(max);
		globalMin.set(min);
		globalMax.set(max);
		while(it.hasNext()) {
			c = (Content)it.next();
			c.getMin(min);
			c.getMax(max);
			if(min.x < globalMin.x) globalMin.x = min.x;
			if(min.y < globalMin.y) globalMin.y = min.y;
			if(min.z < globalMin.z) globalMin.z = min.z;
			if(max.x > globalMax.x) globalMax.x = max.x;
			if(max.y > globalMax.y) globalMax.y = max.y;
			if(max.z > globalMax.z) globalMax.z = max.z;
		}
		globalCenter.x = globalMin.x + (globalMax.x - globalMin.x) / 2;
		globalCenter.y = globalMin.y + (globalMax.y - globalMin.y) / 2;
		globalCenter.z = globalMin.z + (globalMax.z - globalMin.z) / 2;
	}

	/**
	 * If the specified Content is the only content in the universe, global
	 * minimum, maximum and center point are set accordingly to this Content.
	 * If not, the extrema of the specified Content are compared with the
	 * current global extrema, and these are set accordingly.
	 * @param c
	 */
	public void recalculateGlobalMinMax(Content c) {
		Point3d cmin = new Point3d(); c.getMin(cmin);
		Point3d cmax = new Point3d(); c.getMax(cmax);
		if(contents.size() == 1) {
			globalMin.set(cmin);
			globalMax.set(cmax);
		} else {
			if(cmin.x < globalMin.x) globalMin.x = cmin.x;
			if(cmin.y < globalMin.y) globalMin.y = cmin.y;
			if(cmin.z < globalMin.z) globalMin.z = cmin.z;
			if(cmax.x > globalMax.x) globalMax.x = cmax.x;
			if(cmax.y > globalMax.y) globalMax.y = cmax.y;
			if(cmax.z > globalMax.z) globalMax.z = cmax.z;
		}
		globalCenter.x = globalMin.x + (globalMax.x - globalMin.x) / 2;
		globalCenter.y = globalMin.y + (globalMax.y - globalMin.y) / 2;
		globalCenter.z = globalMin.z + (globalMax.z - globalMin.z) / 2;
	}

	/**
	 * Get a reference to the global center point.
	 * Attention: Changing the returned point results in unspecified
	 * behavior.
	 */
	public Point3d getGlobalCenterPoint() {
		return globalCenter;
	}

	/**
	 * Copies the global center point into the specified Point3d.
	 * @param p
	 */
	public void getGlobalCenterPoint(Point3d p) {
		p.set(globalCenter);
	}

	/**
	 * Copies the global minimum point into the specified Point3d.
	 * @param p
	 */
	public void getGlobalMinPoint(Point3d p) {
		p.set(globalMin);
	}

	/**
	 * Copies the global maximum point into the specified Point3d.
	 * @param p
	 */
	public void getGlobalMaxPoint(Point3d p) {
		p.set(globalMax);
	}

	/* *************************************************************
	 * Octree methods - deprecated
	 * *************************************************************/
	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
	public void updateOctree() {
// 		if(octree != null)
// 			octree.update();
	}

	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
	public void cancelOctree() {
// 		if(octree != null)
// 			octree.cancel();
	}

	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
	private VolumeOctree octree = null;

	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
	public void removeOctree() {
		if(octree != null) {
			this.removeUniverseListener(octree);
			scene.removeChild(octree.getRootBranchGroup());
			octree = null;
		}
	}

	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
	public VolumeOctree addOctree(String imageDir, String name) {
		if(octree != null) {
			IJ.error("Only one large volume can be displayed a time.\n" +
				"Please remove previously displayed volumes first.");
			return null;
		}
		if(contents.containsKey(name)) {
			IJ.error("Name exists already");
			return null;
		}
		try {
			octree = new VolumeOctree(imageDir, canvas);
			octree.displayInitial();
			octree.getRootBranchGroup().compile();
			scene.addChild(octree.getRootBranchGroup());
			ensureScale(octree.realWorldXDim());
			this.addUniverseListener(octree);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return octree;
	}

	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
	/*
	 * Requires an empty directory.
	 */
// 	public VolumeOctree createAndAddOctree(
// 			String imagePath, String dir, String name) {
// 		File outdir = new File(dir);
// 		if(!outdir.exists())
// 			outdir.mkdir();
// 		if(!outdir.isDirectory()) {
// 			throw new RuntimeException("Not a directory");
// 		}
// 		try {
// 			new FilePreparer(imagePath, VolumeOctree.SIZE, dir).createFiles();
// 			return addOctree(dir, name);
// 		} catch(Exception e) {
// 			e.printStackTrace();
// 			throw new RuntimeException(e);
// 		}
// 	}

	/**
	 * @deprecated The octree methods will be outsourced into a different
	 * plugin.
	 */
// 	public octree.VolumeOctree createAndAddOctree(
// 			ImagePlus image, String dir, String name) {
// 		File outdir = new File(dir);
// 		if(!outdir.exists())
// 			outdir.mkdir();
// 		if(!outdir.isDirectory()) {
// 			throw new RuntimeException("Not a directory");
// 		}
// 		try {
// 			new FilePreparer(image, VolumeOctree.SIZE, dir).createFiles();
// 			return addOctree(dir, name);
// 		} catch(Exception e) {
// 			e.printStackTrace();
// 			throw new RuntimeException(e);
// 		}
// 	}

	/* *************************************************************
	 * Adding and removing Contents
	 * *************************************************************/
	/**
	 * Add the specified image as a new Content to the universe. The specified
	 * type is one of the constants defined in Content, e.g. VOLUME, SURFACE
	 * etc. For meaning about color, threshold, channels, ... see the
	 * documentation for Content.
	 *
	 * @param image the image to display
	 * @param color the color in which the Content is displayed
	 * @param name a name for the Content to be added
	 * @param thresh a threshold
	 * @param channels the color channels to display.
	 * @param resf a resampling factor.
	 * @param type the type which determines how the image is displayed.
	 * @return The Content which is added, null if any error occurred.
	 */
	public Content addContent(ImagePlus image, Color3f color, String name,
		int thresh, boolean[] channels, int resf, int type) {
		if(contents.containsKey(name)) {
			IJ.error("Content named '" + name + "' exists already");
			return null;
		}
		Content c = ContentCreator.createContent(name, image, type,
			resf, -1, color, thresh, channels);
		return addContent(c);
	}

	/**
	 * Add the specified image as a new Content to the universe. The specified
	 * type is one of the constants defined in Content, e.g. VOLUME, SURFACE
	 * etc. For meaning about color, threshold, channels, ... see the
	 * documentation for Content.
	 * Default parameters are used for its attributes:
	 * <ul><li>color: null
	 * <li>name: title of the image
	 * <li>threshold: the default threshold, as returned by
	 *                Content.getDefaultTreshold()
	 * <li>channels: all color channels r, g, b
	 * <li>resampling factor: the default resampling factor, as returned
	 *                by Content.getDefaultResamplingFactor() </li>
	 * </ul>
	 *
	 * @param image the image to display
	 * @param type the type which determines how the image is displayed.
	 * @return The Content which is added, null if any error occurred.
	 */
	public Content addContent(ImagePlus image, int type) {
		int res = Content.getDefaultResamplingFactor(image, type);
		int thr = Content.getDefaultThreshold(image, type);
		return addContent(image, null, image.getTitle(), thr,
			new boolean[] {true, true, true}, res, type);
	}

	/**
	 * Add a new image as a content, displaying it as a volume rendering.
	 * Default parameters are used for its attributes:
	 * <ul><li>color: null
	 * <li>name: title of the image
	 * <li>threshold: the default threshold, as returned by
	 *                Content.getDefaultTreshold()
	 * <li>channels: all color channels r, g, b
	 * <li>resampling factor: the default resampling factor, as returned
	 *                by Content.getDefaultResamplingFactor() </li>
	 * </ul>
	 *
	 * @param image the image to display
	 * @return the Content which was added, null if any error occurred.
	 */
	public Content addVoltex(ImagePlus image) {
		return addContent(image, Content.VOLUME);
	}

	/**
	 * Add a new image as a content, displaying it as a volume rendering.
	 * For the meaning of color, threshold, channels, resampling factor etc
	 * see the documentation of Content.
	 *
	 * @param image the image to display
	 * @param color the color in which this volume rendering is displayed.
	 * @param name the name of the displayed Content.
	 * @param threshold the threshold used for the displayed volume rendering
	 * @param channels the displayed color channels,
	 *        must be a boolean array of length 3
	 * @param resamplingF a resampling factor.
	 * @return the added Content, null if any error occurred
	 */
	public Content addVoltex(ImagePlus image, Color3f color,
		String name, int thresh, boolean[] channels, int resamplingF) {
		return addContent(image, color, name, thresh, channels,
			resamplingF, Content.VOLUME);
	}

	/**
	 * Add a new image as a content, displaying it as orthoslices.
	 * Default parameters are used for its attributes:
	 * <ul><li>color: null
	 * <li>name: title of the image
	 * <li>threshold: the default threshold, as returned by
	 *                Content.getDefaultTreshold()
	 * <li>channels: all color channels r, g, b
	 * <li>resampling factor: the default resampling factor, as returned
	 *                by Content.getDefaultResamplingFactor() </li>
	 * </ul>
	 *
	 * @param image the image to display
	 * @return the Content which was added, null if any error occurred.
	 */
	public Content addOrthoslice(ImagePlus image) {
		return addContent(image, Content.ORTHO);
	}

	/**
	 * Add a new image as a content, displaying it as orthoslices.
	 * For the meaning of color, threshold, channels, resampling factor etc
	 * see the documentation of Content.
	 *
	 * @param image the image to display
	 * @param color the color in which these orthoslices are displayed.
	 * @param name the name of the displayed Content.
	 * @param threshold the threshold used for the displayed orthoslices
	 * @param channels the displayed color channels,
	 *        must be a boolean array of length 3
	 * @param resamplingF a resampling factor.
	 * @return the added Content, null if any error occurred
	 */
	public Content addOrthoslice(ImagePlus image, Color3f color,
		String name, int thresh, boolean[] channels, int resamplingF) {
		return addContent(image, color, name, thresh, channels,
			resamplingF, Content.ORTHO);
	}

	/**
	 * Add a new image as a content, displaying it as a 2D surface plot.
	 * Default parameters are used for its attributes:
	 * <ul><li>color: null
	 * <li>name: title of the image
	 * <li>threshold: the default threshold, as returned by
	 *                Content.getDefaultTreshold()
	 * <li>channels: all color channels r, g, b
	 * <li>resampling factor: the default resampling factor, as returned
	 *                by Content.getDefaultResamplingFactor() </li>
	 * </ul>
	 *
	 * @param image the image to display
	 * @return the Content which was added, null if any error occurred.
	 */
	public Content addSurfacePlot(ImagePlus image) {
		return addContent(image, Content.SURFACE_PLOT2D);
	}

	/**
	 * Add a new image as a content, displaying it as a 2D surface plot.
	 * For the meaning of color, threshold, channels, resampling factor etc
	 * see the documentation of Content.
	 *
	 * @param image the image to display
	 * @param color the color in which this surface plot is displayed.
	 * @param name the name of the displayed Content.
	 * @param threshold the threshold used for the displayed surface plot
	 * @param channels the displayed color channels,
	 *        must be a boolean array of length 3
	 * @param resamplingF a resampling factor.
	 * @return the added Content, null if any error occurred
	 */
	public Content addSurfacePlot(ImagePlus image, Color3f color,
		String name, int thresh, boolean[] channels, int resamplingF) {
		return addContent(image, color, name, thresh, channels,
			resamplingF, Content.SURFACE_PLOT2D);
	}

	/**
	 * Add a new image as a content, displaying it as an isosurface.
	 * Default parameters are used for its attributes:
	 * <ul><li>color: null
	 * <li>name: title of the image
	 * <li>threshold: the default threshold, as returned by
	 *                Content.getDefaultTreshold()
	 * <li>channels: all color channels r, g, b
	 * <li>resampling factor: the default resampling factor, as returned
	 *                by Content.getDefaultResamplingFactor() </li>
	 * </ul>
	 *
	 * @param image the image to display
	 * @return the Content which was added, null if any error occurred.
	 */
	public Content addMesh(ImagePlus img) {
		return addContent(img, Content.SURFACE);
	}

	/**
	 * Add a new image as a content, displaying it as an iso-surface.
	 * For the meaning of color, threshold, channels, resampling factor etc
	 * see the documentation of Content.
	 *
	 * @param image the image to display
	 * @param color the color in which this surface is displayed.
	 * @param name the name of the displayed Content.
	 * @param threshold the threshold used for generating the surface
	 * @param channels the displayed color channels,
	 *        must be a boolean array of length 3
	 * @param resamplingF a resampling factor.
	 * @return the added Content, null if any error occurred
	 */
	public Content addMesh(ImagePlus image, Color3f color, String name,
		int threshold, boolean[] channels, int resamplingF) {

		return addContent(image, color, name, threshold, channels,
				resamplingF, Content.SURFACE);
	}

	/**
	 * Add a custom mesh to the universe.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 * @param mesh the CustomMesh to display
	 * @param name the name for the added Content
	 * @return the added Content
	 */
	public Content addCustomMesh(CustomMesh mesh, String name) {
		if(contents.containsKey(name)) {
			IJ.error("Mesh named '"+name+"' exists already");
			return null;
		}
		Content content = createContent(mesh, name);
		return addContent(content);
	}

	/**
	 * Add a CustomMultiMesh to the universe.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 * @param mesh the CustomMultiMesh to display
	 * @param name the name for the added Content
	 * @return the added Content
	 */
	public Content addCustomMesh(CustomMultiMesh mesh, String name) {
		if(contents.containsKey(name)) {
			IJ.error("Mesh named '"+name+"' exists already");
			return null;
		}
		Content content = createContent(mesh, name);
		return addContent(content);
	}

	/**
	 * Create a Content object from the mesh.
	 *
	 * Does not add the Content to the view; it merely creates it
	 * with all the appropriate default parameters.
	 * Does not check if the view already contains a Content object
	 * with the same name, which is not allowed.
	 *
	 * @param mesh the CustomMesh to display
	 * @param name the name for the added Content
	 * @return the created Content
	 */
	public Content createContent(CustomMesh mesh, String name) {
		return ContentCreator.createContent(mesh, name);
	}

	public Content createContent(CustomMultiMesh node, String name) {
		return ContentCreator.createContent(node, name);
	}

	/**
	 * Add a custom mesh, in particular a line mesh (a set of lines in 3D)
	 * to the universe.
	 * For the line parameters, default values are used: The width of
	 * the line is 1.0 and the pattern is solid.
	 *
	 * There exist two styles of line meshes:
	 * <ul><li>a normal line mesh. In this case, the specified strips flag
	 * should be false. <code>mesh</code> is a list of points which are
	 * connected pairwise, i.e. the 1st point is connected to the 2nd, the
	 * 3rd to the 4th, and so on.</li>
	 * <li>a strip line. In this case, the specified strip flag should be
	 * true. <code>mesh</code> is a list of points which are connected
	 * continuously, i.e.the 1st point is connected to the 2nd, the 2nd to
	 * the 2rd, the 3rd to the 4th, and so on.
	 * </li></ul>
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh
	 * @param color the color in which the line is displayed
	 * @param name a name for the added Content
	 * @param strips flag specifying wheter the line is continuously or
	 * pairwise connected
	 * @return the connected Content.
	 */
	public Content addLineMesh(List<Point3f> mesh, Color3f color, String name,
			    boolean strips) {
		  int mode = strips ? CustomLineMesh.CONTINUOUS
				  	: CustomLineMesh.PAIRWISE;
		  CustomLineMesh lmesh = new CustomLineMesh(mesh, mode, color, 0);
		  return addCustomMesh(lmesh, name);
	}

	/**
	 * Add a custom mesh, in particular a point mesh, to the universe.
	 * For the size of the points, a default value is used which is 1.0.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh.
	 * @param color the color in which the points is displayed
	 * @param name a name for the added Content
	 * @return the connected Content.
	 */
	public Content addPointMesh(List<Point3f> mesh,
			    Color3f color, String name) {
		  CustomPointMesh tmesh = new CustomPointMesh(mesh, color, 0);
		  return addCustomMesh(tmesh, name);
	}

	/**
	 * Add a custom mesh, in particular a point mesh, to the universe.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh.
	 * @param color the color in which the points is displayed
	 * @param name a name for the added Content
	 * @param ptsize the size of the points.
	 * @return the connected Content.
	 */
	public Content addPointMesh(List<Point3f> mesh, Color3f color,
						float ptsize, String name) {
		  CustomPointMesh tmesh = new CustomPointMesh(mesh, color, 0);
		  tmesh.setPointSize(ptsize);
		  return addCustomMesh(tmesh, name);
	}

	/**
	 * Add a custom mesh, in particular a mesh consisting of quads, to the
	 * universe.
	 * The number of points in the specified list must be devidable by 4.
	 * 4 consecutive points represent one quad.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh.
	 * @param color the color in which the points is displayed
	 * @param name a name for the added Content
	 * @return the connected Content.
	 */
	public Content addQuadMesh(List<Point3f> mesh,
			    Color3f color, String name) {
		  CustomQuadMesh tmesh = new CustomQuadMesh(mesh, color, 0);
		  return addCustomMesh(tmesh, name);
	}

	/**
	 * Add a custom mesh, in particular a triangle mesh, to the universe.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh. The number of
	 *        points must be devidable by 3. 3 successive points make up one
	 *        triangle.
	 * @param color the color in which the line is displayed
	 * @param name a name for the added Content
	 * @return the connected Content.
	 */
	public Content addTriangleMesh(List<Point3f> mesh,
			    Color3f color, String name) {
		CustomTriangleMesh tmesh = new CustomTriangleMesh(mesh, color, 0);
		return addCustomMesh(tmesh, name);
	}

	/**
	 * Add a custom mesh, in particular a triangle mesh, to the universe.
	 * With this method, the color of each vertex may be set to produce
	 * multi-colored meshes.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh. The number of
	 *        points must be devidable by 3. 3 successive points make up one
	 *        triangle.
	 * @param colors a List of the colors at each vertex in the mesh
	 * @param name a name for the added Content
	 * @return the connected Content.
	 */
	public Content addTriangleMesh(List<Point3f> mesh,
			    List<Color3f> colors, String name) {
		CustomTriangleMesh tmesh = new CustomTriangleMesh(mesh);
		tmesh.setColor(colors);
		return addCustomMesh(tmesh, name);
	}

	/**
	 * @deprecated This method will not be supported in the future.
	 * The specified 'scale' will be ignored, and the applied scale
	 * will be calculated automatically from the minimum and maximum
	 * coordinates of the specified mesh.
	 * Use addTriangleMesh instead.
	 */
	public Content addMesh(List mesh, Color3f color, String name,
			    float scale, int threshold) {
		  Content c = addMesh(mesh, color, name, threshold);
		  return c;
	}

	/**
	 * @deprecated This method will not be supported in the future.
	 * Use addTriangleMesh instead.
	 */
	public Content addMesh(List<Point3f> mesh, Color3f color, String name,
			    int threshold) {
		  return addTriangleMesh(mesh, color, name);
	}

	/**
	 * Remove all the contents of this universe.
	 */
	public void removeAllContents() {
		  String[] names = new String[contents.size()];
		  contents.keySet().toArray(names);
		  for (int i=0; i<names.length; i++)
			    removeContent(names[i]);
	}

	/**
	 * Remove the Content with the specified name from the universe.
	 * @param name
	 */
	public void removeContent(String name) {
		synchronized(lock) {
			Content content = (Content)contents.get(name);
			if(content == null)
				return;
			scene.removeChild(content);
			contents.remove(name);
			if(selected == content)
				clearSelection();
			fireContentRemoved(content);
			this.removeUniverseListener(content);
			updateTimeline();
		}
	}

	/* *************************************************************
	 * Content retrieval
	 * *************************************************************/
	/**
	 * Return an Iterator which iterates through all the contents of this
	 * universe.
	 */
	public Iterator contents() {
		return contents.values().iterator();
	}

	/**
	 * Returns a Collection containing the references to all the
	 * contents of this universe.
	 * @return
	 */
	public Collection getContents() {
		return contents.values();
	}

	/**
	 * Returns true if a Content with the specified name is present in
	 * this universe.
	 * @param name
	 * @return
	 */
	public boolean contains(String name) {
		return contents.containsKey(name);
	}

	/**
	 * Returns the Content with the specified name. Null if no Content with
	 * the specified name is present.
	 * @param name
	 * @return
	 */
	public Content getContent(String name) {
		if (null == name) return null;
		return (Content)contents.get(name);
	}

	/* *************************************************************
	 * Methods changing the view of this universe
	 * *************************************************************/
	/**
	 * Save the current view transformations to a file
	 */
	public void saveView(String file) throws IOException {
		SaveSession.saveView(this, file);
	}

	/**
	 * Load view transformations from file
	 */
	public void loadView(final String file) throws IOException {
		SaveSession.loadView(this, file);
	}

	/**
	 * Reset the transformations of the view side of the scene graph
	 * as if the Contents of this universe were just displayed.
	 */
	public void resetView() {
		fireTransformationStarted();

		// rotate so that y shows downwards
		Transform3D t = new Transform3D();
		AxisAngle4d aa = new AxisAngle4d(1, 0, 0, Math.PI);
		t.set(aa);
		getRotationTG().setTransform(t);

		t.setIdentity();
		getTranslateTG().setTransform(t);
		getZoomTG().setTransform(t);
		getZoomTG().setTransform(t);
		recalculateGlobalMinMax();
		getViewPlatformTransformer().centerAt(globalCenter);
		// reset zoom
		double d = oldRange / Math.tan(Math.PI/8);
		getViewPlatformTransformer().zoomTo(d);
		fireTransformationUpdated();
		fireTransformationFinished();
	}

	/**
	 * Rotate the universe, using the given axis of rotation and angle;
	 * The center of rotation is the global center.
	 * @param axis The axis of rotation (in the image plate coordinate system)
	 * @param angle The angle in radians.
	 */
	public void rotateUniverse(Vector3d axis, double angle) {
		viewTransformer.rotate(axis, angle);
	}

	/**
	 * Rotate the univere so that the user looks in the negative direction
	 * of the z-axis.
	 */
	public void rotateToNegativeXY() {
		fireTransformationStarted();
		getRotationTG().setTransform(new Transform3D());
		fireTransformationFinished();
	}

	/**
	 * Rotate the univere so that the user looks in the positive direction
	 * of the z-axis.
	 */
	public void rotateToPositiveXY() {
		fireTransformationStarted();
		getRotationTG().setTransform(new Transform3D());
		waitForNextFrame();
		rotateUniverse(new Vector3d(0, 1, 0), Math.PI);
	}

	/**
	 * Rotate the univere so that the user looks in the negative direction
	 * of the y-axis.
	 */
	public void rotateToNegativeXZ() {
		fireTransformationStarted();
		getRotationTG().setTransform(new Transform3D());
		waitForNextFrame();
		rotateUniverse(new Vector3d(1, 0, 0), Math.PI / 2);
	}

	/**
	 * Rotate the univere so that the user looks in the positive direction
	 * of the y-axis.
	 */
	public void rotateToPositiveXZ() {
		fireTransformationStarted();
		getRotationTG().setTransform(new Transform3D());
		waitForNextFrame();
		rotateUniverse(new Vector3d(0, 1, 0), -Math.PI / 2);
	}

	/**
	 * Rotate the univere so that the user looks in the negative direction
	 * of the x-axis.
	 */
	public void rotateToNegativeYZ() {
		fireTransformationStarted();
		getRotationTG().setTransform(new Transform3D());
		waitForNextFrame();
		rotateUniverse(new Vector3d(0, 1, 0), Math.PI / 2);
	}

	/**
	 * Rotate the univere so that the user looks in the positive direction
	 * of the x-axis.
	 */
	public void rotateToPositiveYZ() {
		fireTransformationStarted();
		getRotationTG().setTransform(new Transform3D());
		waitForNextFrame();
		rotateUniverse(new Vector3d(1, 0, 0), -Math.PI / 2);
	}

	/**
	 * Select the view at the selected Content.
	 */
	public void centerSelected(Content c) {
		Point3d center = new Point3d();
		c.getContent().getCenter(center);

		Transform3D localToVWorld = new Transform3D();
		c.getContent().getLocalToVworld(localToVWorld);
		localToVWorld.transform(center);

		getViewPlatformTransformer().centerAt(center);
	}

	/**
	 * Center the universe at the given point.
	 */
	public void centerAt(Point3d p) {
		getViewPlatformTransformer().centerAt(p);
	}

	/**
	 * Fit all contents optimally into the canvas.
	 */
	public void adjustView() {
		adjustView(ViewAdjuster.ADJUST_BOTH);
	}

	/**
	 * Fit all contents optimally into the canvas.
	 * @param dir One of ViewAdjuster.ADJUST_HORIZONTAL,
	 *            ViewAdjuster.ADJUST_VERTICAL or ViewAdjuster.ADJUST_BOTH
	 */
	public void adjustView(int dir) {
		ViewAdjuster adj = new ViewAdjuster(this, dir);
		adj.addCenterOf(contents.values());
		for(Content c : contents.values())
			adj.add(c);
		adj.apply();
	}

	/**
	 * Fit the specified contents optimally into the canvas.
	 */
	public void adjustView(Iterable<Content> contents) {
		adjustView(contents, ViewAdjuster.ADJUST_BOTH);
	}

	/**
	 * Fit the specified contents optimally into the canvas.
	 * @param dir One of ViewAdjuster.ADJUST_HORIZONTAL,
	 *            ViewAdjuster.ADJUST_VERTICAL or ViewAdjuster.ADJUST_BOTH
	 */
	public void adjustView(Iterable<Content> contents, int dir) {
		ViewAdjuster adj = new ViewAdjuster(this, dir);
		adj.addCenterOf(contents);
		for(Content c : contents)
			adj.add(c);
		adj.apply();
	}

	/**
	 * Fit the specified content optimally into the canvas.
	 * @param dir One of ViewAdjuster.ADJUST_HORIZONTAL,
	 *            ViewAdjuster.ADJUST_VERTICAL or ViewAdjuster.ADJUST_BOTH
	 */
	public void adjustView(Content c, int dir) {
		ViewAdjuster adj = new ViewAdjuster(this, dir);
		adj.add(c);
		adj.apply();
	}

	/**
	 * Fit the specified content optimally into the canvas.
	 */
	public void adjustView(Content c) {
		adjustView(c, ViewAdjuster.ADJUST_BOTH);
	}

	/* *************************************************************
	 * Private methods
	 * *************************************************************/
	private float oldRange = 2f;

	private void ensureScale(float range) {
		oldRange = range;
		double d = (range) / Math.tan(Math.PI/8);
		getViewPlatformTransformer().zoomTo(d);
	}

	public String allContentsString() {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for(String s : contents.keySet()) {
			if(first)
				first = false;
			else
				sb.append(", ");
			sb.append("\"");
			sb.append(s);
			sb.append("\"");
		}
		return sb.toString();
	}

	public String getSafeContentName( String suggested ) {
		String originalName = suggested;
		String attempt = suggested;
		int tryNumber = 2;
		while( contains(attempt) ) {
			attempt = originalName + " (" + tryNumber + ")";
			++ tryNumber;
		}
		return attempt;
	}

	/** Returns true on success. */
	private boolean addContentToScene(Content c) {
		synchronized (lock) {
			String name = c.getName();
			if(contents.containsKey(name)) {
				IJ.log("Mesh named '" + name + "' exists already");
				return false;
			}
			TreeMap<Integer, ContentInstant> instants =
				c.getInstants();
			// in case the time point is not set, do so now
			if(instants.firstKey() == -1)
				c.startAt(currentTimepoint);
			// update start and end time
			int st = startTime;
			int e = endTime;
			if(c.getStartTime() < startTime)
				st = c.getStartTime();
			if(c.getEndTime() > endTime)
				e = c.getEndTime();
			updateStartAndEndTime(st, e);
			c.showTimepoint(currentTimepoint);

			this.scene.addChild(c);
			this.contents.put(name, c);
			this.recalculateGlobalMinMax(c);
			c.showTimepoint(currentTimepoint);

			c.setPointListDialog(plDialog);
		}
		return true;
	}

	/**
	 * Add the specified Content to the universe. It is assumed that the
	 * specified Content is constructed correctly.
	 * Will wait until the Content is fully added; for asynchronous
	 * additions of Content, use the @addContentLater method.
	 * @param c
	 * @return the added Content, or null if an error occurred.
	 */
	public Content addContent(Content c) {
		try {
			return addContentLater(c).get();
		} catch (InterruptedException ie) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private ExecutorService adder = Executors.newFixedThreadPool(1);

	/**
	 * Add the specified Content to the universe. It is assumed that the
	 * specified Content is constructed correctly.
	 * The Content is added asynchronously, and this method returns immediately.
	 * @param c The Content to add
	 * @return a Future holding the added Content, or null if an error occurred.
	 */
	public Future<Content> addContentLater(final Content c) {
		final Image3DUniverse univ = this;
		return adder.submit(new Callable<Content>() {
			public Content call() {
				synchronized (lock) {
					if (!addContentToScene(c)) return null;
					if (univ.autoAdjustView) {
						univ.getViewPlatformTransformer()
							.centerAt(univ.globalCenter);
						float range = (float)(univ.globalMax.x
							- univ.globalMin.x);
						univ.ensureScale(range);
					}
				}
				univ.fireContentAdded(c);
				univ.addUniverseListener(c);
				univ.waitForNextFrame();
				univ.fireTransformationUpdated();
				return c;
			}
		});
	}

	public Collection<Future<Content>> addContentLater(String file) {
		Map<String,CustomMesh> meshes = MeshLoader.load(file);
		if(meshes == null)
			return null;

		List<Content>contents = new ArrayList<Content>();
		for(Map.Entry<String,CustomMesh> entry : meshes.entrySet()) {
			String name = entry.getKey();
			name = getSafeContentName(name);
			CustomMesh mesh = entry.getValue();

			Content content = createContent(mesh, name);
			contents.add(content);
		}
		return addContentLater(contents);
	}


	/**
	 * Add the specified collection of Content to the universe. It is
	 * assumed that the specified Content is constructed correctly.
	 * The Content is added asynchronously, and this method returns immediately.
	 * @param c The Collection of Content to add
	 * @return a Collection of Future objects, each holding an added Content.
	 *         The returned Collection is never null, but its Future objects
	 *         may return null on calling get() on them if an error ocurred
	 *         when adding a specific Content object.
	 */
	public Collection<Future<Content>> addContentLater(Collection<Content> cc) {
		final Image3DUniverse univ = this;
		final ArrayList<Future<Content>> all = new ArrayList<Future<Content>>();
		for (final Content c : cc) {
			all.add(adder.submit(new Callable<Content>() {
				public Content call() {
					if (!addContentToScene(c)) return null;
					univ.fireContentAdded(c);
					univ.addUniverseListener(c);
					return c;
				}
			}));
		}
		// Post actions: submit a new task to the single-threaded
		// executor service, so it will be executed after all other
		// tasks.
		adder.submit(new Callable<Boolean>() {
			public Boolean call() {
				// Now adjust universe view
				if (univ.autoAdjustView) {
					univ.getViewPlatformTransformer()
						.centerAt(univ.globalCenter);
					float range = (float)(univ.globalMax.x
						- univ.globalMin.x);
					univ.ensureScale(range);
				}
				// Notify listeners
				univ.waitForNextFrame();
				univ.fireTransformationUpdated();
				return true;
			}
		});
		return all;
	}
}

