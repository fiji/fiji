package ij3d;

import ij.IJ;

import ij3d.pointlist.PointListDialog;
import ij.ImagePlus;

import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import vib.PointList;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.vecmath.Vector3f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import java.util.TreeMap;
import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Content extends BranchGroup implements UniverseListener, ContentConstants, AxisConstants {

	private HashMap<Integer, Integer> timepointToSwitchIndex;
	private TreeMap<Integer, ContentInstant> contents;
	private int currentTimePoint;
	private Switch contentSwitch;
	private boolean showAllTimepoints = false;
	private final String name;
	private boolean showPointList = false;

	private final boolean swapTimelapseData;

	public Content(String name) {
		this(name, 0);
	}

	public Content(String name, int tp) {
		this.name = name;
		this.swapTimelapseData = false;
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		timepointToSwitchIndex = new HashMap<Integer, Integer>();
		contents = new TreeMap<Integer, ContentInstant>();
		ContentInstant ci = new ContentInstant(name + "_#" + tp);
		ci.timepoint = tp;
		contents.put(tp, ci);
		timepointToSwitchIndex.put(tp, 0);
		contentSwitch = new Switch();
		contentSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		contentSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		contentSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		contentSwitch.addChild(ci);
		addChild(contentSwitch);
	}

	public Content(String name, TreeMap<Integer, ContentInstant> contents) {
		this(name, contents, false);
	}

	public Content(String name, TreeMap<Integer, ContentInstant> contents, boolean swapTimelapseData) {
		this.name = name;
		this.swapTimelapseData = swapTimelapseData;
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		this.contents = contents;
		timepointToSwitchIndex = new HashMap<Integer, Integer>();
		contentSwitch = new Switch();
		contentSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		contentSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		contentSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		for(int i : contents.keySet()) {
			ContentInstant c = contents.get(i);
			c.timepoint = i;
			timepointToSwitchIndex.put(i, contentSwitch.numChildren());
			contentSwitch.addChild(c);
		}
		addChild(contentSwitch);
	}

	// replace if timepoint is already present
	public void addInstant(ContentInstant ci) {
		int timepoint = ci.timepoint;
		contents.put(timepoint, ci);
		if(!contents.containsKey(timepoint)) {
			timepointToSwitchIndex.put(timepoint, contentSwitch.numChildren());
			contentSwitch.addChild(ci);
		} else {
			int switchIdx = timepointToSwitchIndex.get(timepoint);
			contentSwitch.setChild(ci, switchIdx);
		}
	}

	public void removeInstant(int timepoint) {
		if(!contents.containsKey(timepoint))
			return;
		int sIdx = timepointToSwitchIndex.get(timepoint);
		contentSwitch.removeChild(sIdx);
		contents.remove(timepoint);
		timepointToSwitchIndex.remove(timepoint);
		// update the following switch indices.
		for(int i = sIdx; i < contentSwitch.numChildren(); i++) {
			ContentInstant ci = (ContentInstant)contentSwitch.getChild(i);
			int tp = ci.getTimepoint();
			timepointToSwitchIndex.put(tp, i);
		}
	}

	public ContentInstant getCurrent() {
		return contents.get(currentTimePoint);
	}

	public ContentInstant getInstant(int i) {
		return contents.get(i);
	}

	public TreeMap<Integer, ContentInstant> getInstants() {
		return contents;
	}

	public void showTimepoint(int tp) {
		showTimepoint(tp, false);
	}

	public void showTimepoint(int tp, boolean force) {
		if(tp == currentTimePoint && !force)
			return;
		ContentInstant old = getCurrent();
		if(old != null && !showAllTimepoints) {
			if(swapTimelapseData)
				old.swapDisplayedData();
			if (!showAllTimepoints) {
				ContentInstant next = contents.get(tp);
				if (next != null)
					next.showPointList(showPointList);
			}
			getCurrent().showPointList(false);
		}
		currentTimePoint = tp;
		if(showAllTimepoints)
			return;
		ContentInstant next = getCurrent();
		if(next != null && swapTimelapseData)
				next.restoreDisplayedData();

		Integer idx = timepointToSwitchIndex.get(tp);
		if(idx == null)
			contentSwitch.setWhichChild(Switch.CHILD_NONE);
		else
			contentSwitch.setWhichChild(idx);
	}

	public void setShowAllTimepoints(boolean b) {
		this.showAllTimepoints = b;
		if(b) {
			contentSwitch.setWhichChild(Switch.CHILD_ALL);
			return;
		}
		Integer idx = timepointToSwitchIndex.get(currentTimePoint);
		if(idx == null)
			contentSwitch.setWhichChild(Switch.CHILD_NONE);
		else
			contentSwitch.setWhichChild(idx);
	}

	public boolean getShowAllTimepoints() {
		return showAllTimepoints;
	}

	public int getNumberOfInstants() {
		return contents.size();
	}

	public boolean isVisibleAt(int tp) {
		return contents.containsKey(tp);
	}

	public int getStartTime() {
		return contents.firstKey();
	}

	public int getEndTime() {
		return contents.lastKey();
	}


	// ==========================================================
	// From here begins the 'Content Instant interface', i.e.
	// methods which are delegated to the individual
	// ContentInstants.
	//
	public void displayAs(int type) {
		for(ContentInstant c : contents.values())
			c.displayAs(type);
	}

	public static int getDefaultThreshold(ImagePlus imp, int type) {
		return ContentInstant.getDefaultThreshold(imp, type);
	}

	public static int getDefaultResamplingFactor(ImagePlus imp, int type) {
		return ContentInstant.getDefaultResamplingFactor(imp, type);
	}

	public void display(ContentNode node) {
		for(ContentInstant c : contents.values())
			c.display(node);
	}

	public ImagePlus exportTransformed() {
		return getCurrent().exportTransformed();
	}

	/* ************************************************************
	 * setters - visibility flags
	 *
	 * ***********************************************************/

	public void setVisible(boolean b) {
		for(ContentInstant c : contents.values())
			c.setVisible(b);
	}

	public void showBoundingBox(boolean b) {
		for(ContentInstant c : contents.values())
			c.showBoundingBox(b);
	}


	public void showCoordinateSystem(boolean b) {
		for(ContentInstant c : contents.values())
			c.showCoordinateSystem(b);
	}

	public void setSelected(boolean selected) {
		// TODO really all?
		for(ContentInstant c : contents.values())
			c.setSelected(selected);
	}

	/* ************************************************************
	 * point list
	 *
	 * ***********************************************************/
	public void setPointListDialog(PointListDialog p) {
		for(ContentInstant c : contents.values())
			c.setPointListDialog(p);
	}

	public void showPointList(boolean b) {
		getCurrent().showPointList(b);
		this.showPointList = b;
	}

	protected final static Pattern startFramePattern =
		Pattern.compile("(?s)(?m).*?^(# frame:? (\\d+)\n).*");

	public void loadPointList() {
		String dir = null, fileName = null;
		ImagePlus image = contents.firstEntry().getValue().image;
		if (image != null) {
			FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			fileName = fi.fileName + ".points";
		}
		OpenDialog od = new OpenDialog("Open points annotation file", dir, fileName);
		if (od.getFileName() == null)
			return;

		File file = new File(od.getDirectory(), od.getFileName());
		try {
			String fileContents = readFile(new FileInputStream(file));
			Matcher matcher = startFramePattern.matcher(fileContents);
			if (matcher.matches()) {
				// empty point lists
				for (Integer frame : contents.keySet())
					contents.get(frame).setPointList(new PointList());
				while (matcher.matches()) {
					int frame = Integer.parseInt(matcher.group(2));
					fileContents = fileContents.substring(matcher.end(1));
					matcher = startFramePattern.matcher(fileContents);
					ContentInstant ci = contents.get(frame);
					if (ci == null)
						continue;
					String pointsForFrame = matcher.matches() ?
						fileContents.substring(0, matcher.start(1)) : fileContents;
					PointList points = PointList.parseString(pointsForFrame);
					if (points != null)
						ci.setPointList(points);
				}
			}
			else {
				// fall back to old-style one-per-frame point lists
				PointList points = PointList.parseString(fileContents);
				if (points != null)
					getCurrent().setPointList(points);
			}
			showPointList(true);
		}
		catch (IOException e) {
			IJ.error("Could not read point list from " + file);
		}
	}

	String readFile(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
		}
		in.close();
		out.close();
		return out.toString("UTF-8");
	}

	public void savePointList() {
		String dir = OpenDialog.getDefaultDirectory();
		String fileName = getName();
		ImagePlus image = contents.firstEntry().getValue().image;
		if (image != null) {
			FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			fileName = fi.fileName;
		}
		SaveDialog sd = new SaveDialog("Save points annotation file as...",
			dir, fileName, ".points");
		if (sd.getFileName() == null)
			return;

		File file = new File(sd.getDirectory(), sd.getFileName());
		if (file.exists() && !IJ.showMessageWithCancel("File exists", "Overwrite " + file + "?"))
			return;
		try {
			PrintStream out = new PrintStream(file);
			for (Integer frame : contents.keySet()) {
				ContentInstant ci = contents.get(frame);
				if (ci.getPointList().size() != 0) {
					out.println("# frame " + frame);
					ci.savePointList(out);
				}
			}
			out.close();
		}
		catch (IOException e) {
			IJ.error("Could not save points to " + file);
		}
	}

	/**
	 * @deprecated
	 * @param p
	 */
	public void addPointListPoint(Point3d p) {
		getCurrent().addPointListPoint(p);
	}

	/**
	 * @deprecated
	 * @param i
	 * @param pos
	 */
	public void setListPointPos(int i, Point3d pos) {
		getCurrent().setListPointPos(i, pos);
	}

	public float getLandmarkPointSize() {
		return getCurrent().getLandmarkPointSize();
	}

	public void setLandmarkPointSize(float r) {
		for(ContentInstant c : contents.values())
			c.setLandmarkPointSize(r);
	}

	public Color3f getLandmarkColor() {
		return getCurrent().getLandmarkColor();
	}

	public void setLandmarkColor(Color3f color) {
		for(ContentInstant c : contents.values())
			c.setLandmarkColor(color);
	}

	public PointList getPointList() {
		return getCurrent().getPointList();
	}

	/**
	 * @deprecated
	 * @param i
	 */
	public void deletePointListPoint(int i) {
		getCurrent().deletePointListPoint(i);
	}

	/* ************************************************************
	 * setters - transform
	 *
	 **************************************************************/
	public void toggleLock() {
		for(ContentInstant c : contents.values())
			c.toggleLock();
	}

	public void setLocked(boolean b) {
		for(ContentInstant c : contents.values())
			c.setLocked(b);
	}

	public void applyTransform(double[] matrix) {
		applyTransform(new Transform3D(matrix));
	}

	public void applyTransform(Transform3D transform) {
		for(ContentInstant c : contents.values())
			c.applyTransform(transform);
	}

	public void applyRotation(int axis, double degree) {
		Transform3D t = new Transform3D();
		switch(axis) {
			case X_AXIS: t.rotX(deg2rad(degree)); break;
			case Y_AXIS: t.rotY(deg2rad(degree)); break;
			case Z_AXIS: t.rotZ(deg2rad(degree)); break;
		}
		applyTransform(t);
	}

	public void applyTranslation(float dx, float dy, float dz) {
		Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(dx, dy, dz));
		applyTransform(t);
	}

	public void setTransform(double[] matrix) {
		setTransform(new Transform3D(matrix));
	}

	public void setTransform(Transform3D transform) {
		for(ContentInstant c : contents.values())
			c.setTransform(transform);
	}

	public void setRotation(int axis, double degree) {
		Transform3D t = new Transform3D();
		switch(axis) {
			case X_AXIS: t.rotX(deg2rad(degree)); break;
			case Y_AXIS: t.rotY(deg2rad(degree)); break;
			case Z_AXIS: t.rotZ(deg2rad(degree)); break;
		}
		setTransform(t);
	}

	public void setTranslation(float dx, float dy, float dz) {
		Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(dx, dy, dz));
		setTransform(t);
	}

	private double deg2rad(double deg) {
		return deg * Math.PI / 180.0;
	}

	/* ************************************************************
	 * setters - attributes
	 *
	 * ***********************************************************/

	public void setChannels(boolean[] channels) {
		for(ContentInstant c : contents.values())
			c.setChannels(channels);
	}

	public void setLUT(int[] r, int[] g, int[] b, int[] a) {
		for(ContentInstant c : contents.values())
			c.setLUT(r, g, b, a);
	}

	public void setThreshold(int th) {
		for(ContentInstant c : contents.values())
			c.setThreshold(th);
	}

	public void setShaded(boolean b) {
		for(ContentInstant c : contents.values())
			c.setShaded(b);
	}

	public void setSaturatedVolumeRendering(boolean b) {
		for(ContentInstant c : contents.values())
			c.setSaturatedVolumeRendering(b);
	}

	public void applySurfaceColors(ImagePlus img) {
		for(ContentInstant c : contents.values())
			c.applySurfaceColors(img);
	}

	public void setColor(Color3f color) {
		for(ContentInstant c : contents.values())
			c.setColor(color);
	}

	public synchronized void setTransparency(float transparency) {
		for(ContentInstant c : contents.values())
			c.setTransparency(transparency);
	}

	/* ************************************************************
	 * UniverseListener interface
	 *
	 *************************************************************/
	public void transformationStarted(View view) {}
	public void contentAdded(Content c) {}
	public void contentRemoved(Content c) {
		for(ContentInstant co : contents.values()) {
			co.contentRemoved(c);
		}
	}
	public void canvasResized() {}
	public void contentSelected(Content c) {}
	public void contentChanged(Content c) {}

	public void universeClosed() {
		for(ContentInstant c : contents.values()) {
			c.universeClosed();
		}
	}

	public void transformationUpdated(View view) {
		eyePtChanged(view);
	}

	public void transformationFinished(View view) {
		eyePtChanged(view);
		// apply same transformation to all other time points
		// in case this content was transformed
		ContentInstant curr = getCurrent();
		if(curr == null || !curr.selected)
			return;
		Transform3D t = new Transform3D();
		Transform3D r = new Transform3D();
		curr.getLocalTranslate(t);
		curr.getLocalRotate(r);

		for(ContentInstant c : contents.values()) {
			if(c == getCurrent())
				continue;
			c.getLocalRotate().setTransform(r);
			c.getLocalTranslate().setTransform(t);
			c.transformationFinished(view);
		}
	}

	public void eyePtChanged(View view) {
		for(ContentInstant c : contents.values())
			c.eyePtChanged(view);
	}

	/* *************************************************************
	 * getters
	 *
	 **************************************************************/
	@Override
	public String getName() {
		return name;
	}

	public int getType() {
		return getCurrent().getType();
	}

	public ContentNode getContent() {
		return getCurrent().getContent();
	}

	public void getMin(Point3d min) {
		min.set(Double.MAX_VALUE,
			Double.MAX_VALUE,
			Double.MAX_VALUE);
		Point3d tmp = new Point3d();
		for(ContentInstant c : contents.values()) {
			c.getContent().getMin(tmp);
			if(tmp.x < min.x) min.x = tmp.x;
			if(tmp.y < min.y) min.y = tmp.y;
			if(tmp.z < min.z) min.z = tmp.z;
		}
	}

	public void getMax(Point3d max) {
		max.set(Double.MIN_VALUE,
			Double.MIN_VALUE,
			Double.MIN_VALUE);
		Point3d tmp = new Point3d();
		for(ContentInstant c : contents.values()) {
			c.getContent().getMax(tmp);
			if(tmp.x > max.x) max.x = tmp.x;
			if(tmp.y > max.y) max.y = tmp.y;
			if(tmp.z > max.z) max.z = tmp.z;
		}
	}

	public ImagePlus getImage() {
		return getCurrent().getImage();
	}

	public boolean[] getChannels() {
		return getCurrent().getChannels();
	}

	public void getRedLUT(int[] l) {
		getCurrent().getRedLUT(l);
	}

	public void getGreenLUT(int[] l) {
		getCurrent().getGreenLUT(l);
	}

	public void getBlueLUT(int[] l) {
		getCurrent().getBlueLUT(l);
	}

	public void getAlphaLUT(int[] l) {
		getCurrent().getAlphaLUT(l);
	}

	public Color3f getColor() {
		return getCurrent().getColor();
	}

	public boolean isShaded() {
		return getCurrent().isShaded();
	}

	public boolean isSaturatedVolumeRendering() {
		return getCurrent().isSaturatedVolumeRendering();
	}

	public int getThreshold() {
		return getCurrent().getThreshold();
	}

	public float getTransparency() {
		return getCurrent().getTransparency();
	}

	public int getResamplingFactor() {
		return getCurrent().getResamplingFactor();
	}

	public TransformGroup getLocalRotate() {
		return getCurrent().getLocalRotate();
	}

	public TransformGroup getLocalTranslate() {
		return getCurrent().getLocalTranslate();
	}

	public void getLocalRotate(Transform3D t) {
		getCurrent().getLocalRotate(t);
	}

	public void getLocalTranslate(Transform3D t) {
		getCurrent().getLocalTranslate(t);
	}

	public boolean isLocked() {
		return getCurrent().isLocked();
	}

	public boolean isVisible() {
		return getCurrent().isVisible();
	}

	public boolean hasCoord() {
		return getCurrent().hasCoord();
	}

	public boolean hasBoundingBox() {
		return getCurrent().hasBoundingBox();
	}

	public boolean isPLVisible() {
		return getCurrent().isPLVisible();
	}
}

