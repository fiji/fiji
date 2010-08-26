package ij3d;

import ij3d.shapes.Scalebar;
import ij.util.Java2;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.text.TextWindow;
import ij.plugin.frame.Recorder;
import ij.process.StackConverter;
import ij.process.ImageConverter;

import ij3d.gui.ContentCreatorDialog;

import math3d.TransformIO;

import javax.swing.JFileChooser;
import javax.swing.filechooser.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.Map;

import java.io.File;

import vib.InterpolatedImage;
import vib.FastMatrix;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import voltex.VolumeRenderer;
import isosurface.MeshExporter;
import isosurface.MeshEditor;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4d;
import javax.media.j3d.Transform3D;
import javax.media.j3d.Background;

import customnode.CustomMesh;
import customnode.CustomMeshNode;
import customnode.CustomTriangleMesh;

import java.util.concurrent.atomic.AtomicInteger;
import javax.vecmath.Point3d;
import octree.OctreeDialog;
import octree.VolumeOctree;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Executer {

	// These strings are the names of the static methods in
	// ImageJ3DViewer.
	public static final String START_ANIMATE = "startAnimate";
	public static final String STOP_ANIMATE = "stopAnimate";
	public static final String START_FREEHAND_RECORDING
		= "startFreehandRecording";
	public static final String STOP_FREEHAND_RECORDING
		= "stopFreehandRecording";
	public static final String RECORD_360 = "record360";
	public static final String RESET_VIEW = "resetView";
	public static final String SCALEBAR = "scalebar";
	public static final String CLOSE = "close";
	public static final String WINDOW_SIZE = "windowSize";

	public static final String SET_COLOR = "setColor"; 
	public static final String SET_TRANSPARENCY = "setTransparency";
	public static final String SET_CHANNELS = "setChannels";
	public static final String FILL_SELECTION = "fillSelection";
	public static final String SET_SLICES = "setSlices";
	public static final String LOCK = "lock";
	public static final String UNLOCK = "unlock";
	public static final String SET_THRESHOLD = "setThreshold";
	public static final String SET_CS = "setCoordinateSystem";
	public static final String SET_TRANSFORM = "setTransform";
	public static final String APPLY_TRANSFORM = "applyTransform";
	public static final String SAVE_TRANSFORM = "saveTransform";
	public static final String RESET_TRANSFORM = "resetTransform";

	// TODO
	public static final String ADD = "add";
	public static final String DELETE = "delete";

	public static final String SMOOTH = "smooth";

	private Image3DUniverse univ;


	public Executer(Image3DUniverse univ) {
		this.univ = univ;
	}


	/* **********************************************************
	 * File menu
	 * *********************************************************/

	public void addContentFromFile() {
		OpenDialog od = new OpenDialog("Open from file", null);
		String folder = od.getDirectory();
		String name = od.getFileName();
		if(folder == null || name == null)
			return;
		File f = new File(folder, name);
		if(f.exists())
			addContent(null, f);
		else
			IJ.error("Can not load " + f.getAbsolutePath());
	}

	public void addContentFromImage(ImagePlus image) {
		addContent(image, null);
	}

	public void addTimelapseFromFile() {
		addContentFromFile();
	}

	public void addTimelapseFromFolder() {
		DirectoryChooser dc = new DirectoryChooser("Open from folder");
		String dir = dc.getDirectory();
		if(dir == null)
			return;
		File d = new File(dir);
		if(d.exists())
			addContent(null, d);
		else
			IJ.error("Cannot load " + d.getAbsolutePath());
	}

	public void addTimelapseFromHyperstack(ImagePlus image) {
		addContentFromImage(image);
	}

	public void addContent(final ImagePlus image, final File file) {
		new Thread() {
			@Override
			public void run() {
				addC(image, file);
			}
		}.start();
	}

	private Content addC(ImagePlus image, File file) {
		ContentCreatorDialog gui = new ContentCreatorDialog();
		Content c = gui.showDialog(univ, image, file);
		if(c == null)
			return null;

		univ.addContent(c);

		// record
		String title = gui.getFile() != null ?
			gui.getFile().getAbsolutePath() :
			gui.getImage().getTitle();
		boolean[] channels = gui.getChannels();
		String[] arg = new String[] {
			title,
			ColorTable.getColorName(gui.getColor()),
			gui.getName(),
			Integer.toString(gui.getThreshold()),
			Boolean.toString(channels[0]),
			Boolean.toString(channels[1]),
			Boolean.toString(channels[2]),
			Integer.toString(gui.getResamplingFactor()),
			Integer.toString(gui.getType())};
		record(ADD, arg);

		return c;
	}

	public void delete(Content c) {
		if(!checkSel(c))
			return;
		univ.removeContent(c.getName());
		record(DELETE);
	}

// 	public void loadOctree() {
// 		OctreeDialog od = new OctreeDialog();
// 		od.showDialog();
// 		if(!od.checkUserInput())
// 			return;
// 		String dir = od.getImageDir();
// 		String name = od.getName();
// 		String path = od.getImagePath();
// 		if(od.shouldCreateData()) {
// 			try {
// 				new FilePreparer(path, VolumeOctree.SIZE, dir).createFiles();
// 			} catch(Exception e) {
// 				IJ.error(e.getMessage());
// 				e.printStackTrace();
// 				return;
// 			}
// 		}
// 		univ.addOctree(dir, name);
// 	}
// 
// 	public void removeOctree() {
// 		univ.removeOctree();
// 	}

	public void importWaveFront() {
		OpenDialog od = new OpenDialog("Select .obj file", OpenDialog.getDefaultDirectory(), null);
		String filename = od.getFileName();
		if (null == filename) return;
		if (!filename.toLowerCase().endsWith(".obj")) {
			IJ.showMessage("Must select a wavefront .obj file!");
			return;
		}
		String path = new StringBuilder(od.getDirectory()).append(filename).toString();
		IJ.log("path: " + path);
		Object ob;
		try {
			ob = univ.addContentLater(path);
		} catch (Exception e) {
			e.printStackTrace();
			ob = null;
		}
		if (null == ob)
			IJ.showMessage("Could not load the file:\n" + path);
	}

	public void saveAsDXF() {
		MeshExporter.saveAsDXF(univ.getContents());
	}

	public void saveAsWaveFront() {
		MeshExporter.saveAsWaveFront(univ.getContents());
	}

	public void loadView() {
		OpenDialog sd = new OpenDialog(
			"Open view...", "", ".view");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if(dir == null || name == null)
			return;
		try {
			univ.loadView(dir + name);
		} catch(Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void saveView() {
		SaveDialog sd = new SaveDialog(
			"Save view...", "", ".view");
		String dir = sd.getDirectory();
		String name = sd.getFileName();
		if(dir == null || name == null)
			return;
		try {
			univ.saveView(dir + name);
		} catch(Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void loadSession() {
		OpenDialog sd = new OpenDialog(
			"Open session...", "session", ".scene");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if(dir == null || name == null)
			return;
		new Thread() {
			public void run() {
				try {
					univ.loadSession(dir + name);
				} catch(Exception e) {
					IJ.error(e.getMessage());
				}
			}
		}.start();
	}

	public void saveSession() {
		SaveDialog sd = new SaveDialog(
			"Save session...", "session", ".scene");
		String dir = sd.getDirectory();
		String name = sd.getFileName();
		if(dir == null || name == null)
			return;
		try {
			univ.saveSession(dir + name);
		} catch(Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void close() {
		univ.close();
		record(CLOSE);
	}



	/* **********************************************************
	 * Edit menu
	 * *********************************************************/
	public void updateVolume(final Content c) {
		if(!checkSel(c))
			return;
		if(c.getType() != Content.VOLUME &&
			c.getType() != Content.ORTHO)
			return;
		if(c.getResamplingFactor() != 1) {
			IJ.error("Object must be loaded " +
				"with resamplingfactor 1");
			return;
		}
		((VoltexGroup)c.getContent()).update();
	}

	public void changeSlices(final Content c) {
		if(!checkSel(c))
			return;
		final GenericDialog gd = new GenericDialog(
			"Adjust slices...", univ.getWindow());
		final OrthoGroup os = (OrthoGroup)c.getContent();
		final int ind1 = os.getSlice(VolumeRenderer.X_AXIS);
		final int ind2 = os.getSlice(VolumeRenderer.Y_AXIS);
		final int ind3 = os.getSlice(VolumeRenderer.Z_AXIS);
		final boolean vis1 = os.isVisible(VolumeRenderer.X_AXIS);
		final boolean vis2 = os.isVisible(VolumeRenderer.Y_AXIS);
		final boolean vis3 = os.isVisible(VolumeRenderer.Z_AXIS);
		ImagePlus imp = c.getImage();
		int w = imp.getWidth() / c.getResamplingFactor();
		int h = imp.getHeight() / c.getResamplingFactor();
		int d = imp.getStackSize() / c.getResamplingFactor();

		gd.addCheckbox("Show_yz plane", vis1);
		gd.addSlider("x coordinate", 0, w-1, ind1);
		gd.addCheckbox("Show_xz plane", vis2);
		gd.addSlider("y coordinate", 0, h-1, ind2);
		gd.addCheckbox("Show_xy plane", vis3);
		gd.addSlider("z coordinate", 0, d-1, ind3);

		gd.addMessage(  "You can use the x, y and z key plus\n" +
				"the arrow keys to adjust slices in\n" +
				"x, y and z direction respectively.\n \n" +
				"x, y, z + SPACE switches planes on\n" +
				"and off");

		final int[] dirs = new int[] {VolumeRenderer.X_AXIS, 
				VolumeRenderer.Y_AXIS, VolumeRenderer.Z_AXIS};
		final Scrollbar[] sl = new Scrollbar[3]; 
		final Checkbox[] cb = new Checkbox[3];

		for(int k = 0; k < 3; k++) {
			final int i = k;
			sl[i] = (Scrollbar)gd.getSliders().get(i);
			sl[i].addAdjustmentListener(new AdjustmentListener() {
				public void adjustmentValueChanged(
							AdjustmentEvent e) {
					os.setSlice(dirs[i], sl[i].getValue());
					univ.fireContentChanged(c);
				}
			});

			cb[i] = (Checkbox)gd.getCheckboxes().get(i);
			cb[i].addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					os.setVisible(dirs[i],cb[i].getState());
				}
			});
		}

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					os.setSlice(VolumeRenderer.X_AXIS, ind1);
					os.setSlice(VolumeRenderer.Y_AXIS, ind2);
					os.setSlice(VolumeRenderer.Z_AXIS, ind3);
					os.setVisible(VolumeRenderer.X_AXIS, vis1);
					os.setVisible(VolumeRenderer.Y_AXIS, vis2);
					os.setVisible(VolumeRenderer.Z_AXIS, vis3);
					univ.fireContentChanged(c);
					return;
				} else {
					record(SET_SLICES,
					Integer.toString(sl[0].getValue()), 
					Integer.toString(sl[1].getValue()),
					Integer.toString(sl[2].getValue()));
					return;
				}
			}
		});
		gd.showDialog();
	}

	public void fill(final Content c) {
		if(!checkSel(c))
			return;
		int type = c.getType();
		if(type != Content.VOLUME && type != Content.ORTHO) 
			return;
		new Thread() {
			@Override
			public void run() {
				ImageCanvas3D canvas = (ImageCanvas3D)univ.getCanvas();
				((VoltexGroup)c.getContent()).
					fillRoi(canvas, canvas.getRoi(), (byte)0);
				univ.fireContentChanged(c);
				record(FILL_SELECTION);
			}
		}.start();
	}

	public void smoothMesh(Content c) {
		if(!checkSel(c))
			return;
		if(c.getType() == Content.SURFACE || c.getType() == Content.CUSTOM) {
			ContentNode cn = c.getContent();
			if(cn instanceof CustomMeshNode) {
				CustomMesh mesh = ((CustomMeshNode)cn).getMesh();
				if(mesh instanceof CustomTriangleMesh)
					MeshEditor.smooth((CustomTriangleMesh)mesh, 0.25f);
			}
		}
	}

	public void smoothAllMeshes() {
		// process each Mesh in a separate thread
		final Collection all = univ.getContents();
		final Content[] c = new Content[all.size()];
		all.toArray(c);
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] thread = new Thread[
			Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i<thread.length; i++) {
			thread[i] = new Thread() {
				@Override
				public void run() {
					try {
						for (int k=ai.getAndIncrement();
						k < c.length;
						k = ai.getAndIncrement()) {
							smoothMesh(c[k]);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			thread[i].start();
		}
	}


	/* ----------------------------------------------------------
	 * Display As submenu
	 * --------------------------------------------------------*/
	public void displayAs(Content c, int type) {
		if(!checkSel(c))
			return;
		c.displayAs(type);
	}


	/* ----------------------------------------------------------
	 * Attributes submenu
	 * --------------------------------------------------------*/
	public void changeColor(final Content c) {
		if(!checkSel(c))
			return;
		final ContentInstant ci = c.getCurrent();
		final GenericDialog gd = 
			new GenericDialog("Adjust color ...", univ.getWindow());
		final Color3f oldC = ci.getColor();

		gd.addCheckbox("Use default color", oldC == null);
		gd.addSlider("Red",0,255,oldC == null ? 255 : oldC.x*255);
		gd.addSlider("Green",0,255,oldC == null ? 0 : oldC.y*255);
		gd.addSlider("Blue",0,255,oldC == null ? 0 : oldC.z*255);

		gd.addCheckbox("Apply to all timepoints", true);

		final Scrollbar rSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar gSlider = (Scrollbar)gd.getSliders().get(1);
		final Scrollbar bSlider = (Scrollbar)gd.getSliders().get(2);
		final Checkbox cBox = (Checkbox)gd.getCheckboxes().get(0);
		final Checkbox aBox = (Checkbox)gd.getCheckboxes().get(1);

		rSlider.setEnabled(oldC != null);
		gSlider.setEnabled(oldC != null);
		bSlider.setEnabled(oldC != null);

		cBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				gd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				rSlider.setEnabled(!cBox.getState());
				gSlider.setEnabled(!cBox.getState());
				bSlider.setEnabled(!cBox.getState());
				ci.setColor(cBox.getState() ? null :
					new Color3f(
						rSlider.getValue() / 255f,
						gSlider.getValue() / 255f,
						bSlider.getValue() / 255f));
				gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				univ.fireContentChanged(c);
			}
		});

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				ci.setColor(new Color3f(
						rSlider.getValue() / 255f,
						gSlider.getValue() / 255f,
						bSlider.getValue() / 255f));
				univ.fireContentChanged(c);
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					ci.setColor(oldC);
					univ.fireContentChanged(c);
					return;
				}
				// gd.wasOKed: apply to all time points
				if(aBox.getState()) {
					c.setColor(ci.getColor());
				}
				univ.fireContentChanged(c);
				if(cBox.getState()){
					record(SET_COLOR,
					"null", "null", "null");
				} else {
					record(SET_COLOR,
					Integer.toString(rSlider.getValue()),
					Integer.toString(gSlider.getValue()),
					Integer.toString(bSlider.getValue()));
				}
			}
		});
		gd.showDialog();
	}

	/** Adjust the background color in place. */
	public void changeBackgroundColor() {
		final GenericDialog gd = 
			new GenericDialog("Adjust background color ...", univ.getWindow());

		final Background background = ((ImageCanvas3D)univ.getCanvas()).getBG();
		final Label status = univ.getWindow().getStatusLabel();
		final Color3f oldC = new Color3f();
		background.getColor(oldC);

		gd.addSlider("Red",0,255,oldC == null ? 255 : oldC.x*255);
		gd.addSlider("Green",0,255,oldC == null ? 0 : oldC.y*255);
		gd.addSlider("Blue",0,255,oldC == null ? 0 : oldC.z*255);

		final Scrollbar rSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar gSlider = (Scrollbar)gd.getSliders().get(1);
		final Scrollbar bSlider = (Scrollbar)gd.getSliders().get(2);

		rSlider.setEnabled(oldC != null);
		gSlider.setEnabled(oldC != null);
		bSlider.setEnabled(oldC != null);

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				float r = rSlider.getValue() / 255f;
				float g = gSlider.getValue() / 255f;
				float b = bSlider.getValue() / 255f;
				background.setColor(r, g, b);
				status.setBackground(new Color(r, g, b));
				((ImageCanvas3D)univ.getCanvas()).render();
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					background.setColor(oldC);
					status.setBackground(new Color(oldC.x, oldC.y, oldC.z));
					((ImageCanvas3D)univ.getCanvas()).render();
					return;
				} else {
					// TODO macro record
					// Apply:
					float r = rSlider.getValue() / 255f;
					float g = gSlider.getValue() / 255f;
					float b = bSlider.getValue() / 255f;
					background.setColor(r, g, b);
					status.setBackground(new Color(r, g, b));
					((ImageCanvas3D)univ.getCanvas()).render();
				}
			}
		});
		gd.showDialog();
	}

	public void changeChannels(Content c) {
		if(!checkSel(c))
			return;
		final ContentInstant ci = c.getCurrent();
		GenericDialog gd = new GenericDialog("Adjust channels ...",
							univ.getWindow());
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
				new String[] {"red", "green", "blue"}, 
				ci.getChannels());
		gd.addCheckbox("Apply to all timepoints", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		if(gd.getNextBoolean())
			c.setChannels(channels);
		else
			ci.setChannels(channels);
		univ.fireContentChanged(c);
		record(SET_CHANNELS, Boolean.toString(channels[0]),
			Boolean.toString(channels[1]),
			Boolean.toString(channels[2]));
	}

	public void changeTransparency(final Content c) {
		if(!checkSel(c))
			return;
		final ContentInstant ci = c.getCurrent();
		final SliderAdjuster transp_adjuster = new SliderAdjuster() {
			public synchronized final void setValue(ContentInstant ci, int v) {
				ci.setTransparency(v / 100f);
				univ.fireContentChanged(c);
			}
		};
		final GenericDialog gd = new GenericDialog(
			"Adjust transparency ...", univ.getWindow());
		final int oldTr = (int)(ci.getTransparency() * 100);
		gd.addSlider("Transparency", 0, 100, oldTr);
		gd.addCheckbox("Apply to all timepoints", true);

		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if(!transp_adjuster.go)
					transp_adjuster.start();
				transp_adjuster.exec((int)e.getValue(), ci, univ);
			}
		});
		((TextField)gd.getNumericFields().get(0)).
			addTextListener(new TextListener() {
			public void textValueChanged(TextEvent e) {
				if(!transp_adjuster.go)
					transp_adjuster.start();
				TextField input = (TextField)e.getSource();
				String text = input.getText();
				try {
					int value = Integer.parseInt(text);
					transp_adjuster.exec(value, ci, univ);
				} catch (Exception exception) {
					// ignore intermediately invalid number
				}
			}
		});
		final Checkbox aBox = (Checkbox)(gd.getCheckboxes().get(0));
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (null != transp_adjuster)
					transp_adjuster.quit();
				if(gd.wasCanceled()) {
					float newTr = (float)oldTr / 100f;
					ci.setTransparency(newTr);
					univ.fireContentChanged(c);
					return;
				}
				// apply to all instants of the content
				if(aBox.getState())
					c.setTransparency(ci.getTransparency());

				record(SET_TRANSPARENCY, Float.
					toString(((Scrollbar)gd.getSliders().
					get(0)).getValue() / 100f));
			}
		});
		gd.showDialog();
	}

	public void changeThreshold(final Content c) {
		if(!checkSel(c))
			return;
		if(c.getImage() == null) {
			IJ.error("The selected object contains no image data,\n" +
					"therefore the threshold can't be changed");
			return;
		}
		final ContentInstant ci = c.getCurrent();
		final SliderAdjuster thresh_adjuster = new SliderAdjuster() {
			public synchronized final void setValue(ContentInstant ci, int v) {
				ci.setThreshold(v);
				univ.fireContentChanged(c);
			}
		};
		final int oldTr = (int)(ci.getThreshold());
		if(c.getType() == Content.SURFACE) {
			final GenericDialog gd = new GenericDialog(
				"Adjust threshold ...", univ.getWindow());
			final int old = (int)ci.getThreshold();
			gd.addNumericField("Threshold", old, 0);
			gd.addCheckbox("Apply to all timepoints", true);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			int th = (int)gd.getNextNumber();
			th = Math.max(0, th);
			th = Math.min(th, 255);
			if(gd.getNextBoolean())
				c.setThreshold(th);
			else
				ci.setThreshold(th);
			univ.fireContentChanged(c);
			record(SET_THRESHOLD, Integer.toString(th));
			return;
		}
		// in case we've not a mesh, change it interactively
		final GenericDialog gd = 
				new GenericDialog("Adjust threshold...");
		gd.addSlider("Threshold", 0, 255, oldTr);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				// start adjuster and request an action
				if(!thresh_adjuster.go)
					thresh_adjuster.start();
				thresh_adjuster.exec((int)e.getValue(), ci, univ);
			}
		});
		gd.addCheckbox("Apply to all timepoints", true);
		final Checkbox aBox = (Checkbox)gd.getCheckboxes().get(0);
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				try {
					if(gd.wasCanceled()) {
						ci.setThreshold(oldTr);
						univ.fireContentChanged(c);
						return;
					}
					// apply to other time points
					if(aBox.getState())
						c.setThreshold(ci.getThreshold());

					record(SET_THRESHOLD,
						Integer.toString(
						c.getThreshold()));
				} finally {
					// [ This code block executes even when
					//   calling return above ]
					//
					// clean up
					if (null != thresh_adjuster)
						thresh_adjuster.quit();
				}
			}
		});
		gd.showDialog();
	}

	public void setShaded(Content c, boolean b) {
		if(!checkSel(c))
			return;
		int t = c.getType();
		if(t != Content.SURFACE &&
				t != Content.SURFACE_PLOT2D && t != Content.CUSTOM)
			return;

		if(c.getNumberOfInstants() == 1) {
			c.setShaded(b);
			return;
		}

		ContentInstant ci = c.getCurrent();
		GenericDialog gd = new GenericDialog("Set shaded");
		gd.addCheckbox("Apply to all timepoints", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		if(gd.getNextBoolean())
			c.setShaded(b);
		else
			ci.setShaded(b);
	}


	/* ----------------------------------------------------------
	 * Hide/Show submenu
	 * --------------------------------------------------------*/
	public void showCoordinateSystem(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.showCoordinateSystem(b);
		record(SET_CS, Boolean.toString(b));
	}

	public void showBoundingBox(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.showBoundingBox(b);
	}

	public void showContent(Content c, boolean b) {
		if(!checkSel(c))
			return;
		univ.getSelected().setVisible(b);
		if(!b)
			univ.clearSelection();
	}

	public void showAllCoordinateSystems(boolean b) {
		for (Iterator it = univ.contents(); it.hasNext(); )
			((Content)it.next()).showCoordinateSystem(b);
	}


	/* ----------------------------------------------------------
	 * Point list submenu
	 * --------------------------------------------------------*/
	public void loadPointList(Content c) {
		if(!checkSel(c))
			return;
		c.loadPointList();
	}

	public void savePointList(Content c) {
		if(!checkSel(c))
			return;
		c.savePointList();
	}

	public void changePointSize(final Content c) {
		if(!checkSel(c))
			return;
		final GenericDialog gd = 
			new GenericDialog("Point size", univ.getWindow());
		final float oldS = (float)(c.getLandmarkPointSize());
		gd.addSlider("Size", 0, 20, oldS);
		((Scrollbar)gd.getSliders().get(0)).
			addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				float newS = (float)e.getValue();
				c.setLandmarkPointSize(newS);
			}
		});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if(gd.wasCanceled()) {
					c.setLandmarkPointSize(oldS);
					return;
				}
			}
		});
		gd.showDialog();
	}

	public void showPointList(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.showPointList(b);
	}

	public void register() {
		// Select the contents used for registration
		Collection contents = univ.getContents();
		if(contents.size() < 2) {
			IJ.error("At least two bodies are " +
				"required for registration");
			return;
		}
		RegistrationMenubar rm = univ.getRegistrationMenuBar();
		univ.setMenubar(rm);
		rm.register();
	}

	public void contentProperties(Content c) {
		if(!checkSel(c))
			return;
		Point3d min = new Point3d();
		Point3d max = new Point3d();
		Point3d center = new Point3d();

		c.getContent().getMin(min);
		c.getContent().getMax(max);
		c.getContent().getCenter(center);

		TextWindow tw = new TextWindow(c.getName(), 
			" \tx\ty\tz",
			"min\t" + (float)min.x + "\t"
				+ (float)min.y + "\t"
				+ (float)min.z + "\n" +
			"max\t" + (float)max.x + "\t"
				+ (float)max.y + "\t"
				+ (float)max.z + "\n" +
			"cog\t" + (float)center.x + "\t"
				+ (float)center.y + "\t"
				+ (float)center.z + "\n\n" +
			"volume\t" + c.getContent().getVolume(),
			512, 512);
	}



	/* **********************************************************
	 * Select menu
	 * *********************************************************/
	public void select(String name) {
		if(name == null) {
			univ.select(null);
			return;
		}
		Content c = univ.getContent(name);
		univ.select(c);
	}

	/* **********************************************************
	 * Transformation menu
	 * *********************************************************/
	public void setLocked(Content c, boolean b) {
		if(!checkSel(c))
			return;
		c.setLocked(b);
		if(b) record(LOCK);
		else record(UNLOCK);
	}

	public void resetTransform(Content c) {
		if(!checkSel(c))
			return;
		if(c.isLocked()) {
			IJ.error(c.getName() + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		c.setTransform(new Transform3D());
		univ.fireTransformationFinished();
		record(RESET_TRANSFORM);
	}

	public void setTransform(Content c) {
		if(!checkSel(c))
			return;
		if(c.isLocked()) {
			IJ.error(c.getName() + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		float[] t = readTransform(c);
		if(t != null) {
			c.setTransform(new Transform3D(t));
			univ.fireTransformationFinished();
			record(SET_TRANSFORM, affine2string(t));
		}
	}

	public void applyTransform(Content c) {
		if(!checkSel(c))
			return;
		if(c.isLocked()) {
			IJ.error(c.getName() + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		float[] t = readTransform(c);
		if(t != null) {
			c.applyTransform(new Transform3D(t));
			univ.fireTransformationFinished();
			record(APPLY_TRANSFORM, affine2string(t));
		}
	}

	public void saveTransform(Content c) {
		if(!checkSel(c))
			return;
		Transform3D t1 = new Transform3D();
		c.getLocalTranslate().getTransform(t1);
		Transform3D t2 = new Transform3D();
		c.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		float[] matrix = new float[16];
		t1.get(matrix);
		if(new TransformIO().saveAffineTransform(matrix))
			record(SAVE_TRANSFORM, affine2string(matrix));
	}

	public void exportTransformed(final Content c) {
		if(!checkSel(c))
			return;
		new Thread(new Runnable() {
			public void run() {
				exportTr(c);
			}
		}).start();
	}

	private void exportTr(Content c) {
		ImagePlus orig = c.getImage();
		if(orig == null) {
			IJ.error("No greyscale image exists for "
				+ c.getName());
			return;
		}
		Transform3D t1 = new Transform3D();
		c.getLocalTranslate().getTransform(t1);
		Transform3D t2 = new Transform3D();
		c.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		FastMatrix fc = FastMatrix.fromCalibration(orig);
		FastMatrix fm = fc.inverse().times(toFastMatrix(t1).inverse()).
			times(fc);
		InterpolatedImage in = new InterpolatedImage(orig);
		InterpolatedImage out = in.cloneDimensionsOnly();
		int w = orig.getWidth(), h = orig.getHeight();
		int d = orig.getStackSize();

		for (int k = 0; k < d; k++) {
			for (int j = 0; j < h; j++) {
				for(int i = 0; i < w; i++) {
					fm.apply(i, j, k);
					out.set(i, j, k, (byte)in.interpol.get(
							fm.x, fm.y, fm.z));
				}
				IJ.showProgress(k + 1, d);
			}
		}
		out.getImage().setTitle(orig.getTitle() + "_transformed");
		out.getImage().show();
	}




	/* **********************************************************
	 * View menu
	 * *********************************************************/
	public void resetView() {
		univ.resetView();
		record(RESET_VIEW);
	}

	public void centerSelected(Content c) {
		if(!checkSel(c))
			return;

		univ.centerSelected(c);
	}

	public void centerUniverse() {
		Point3d c = new Point3d();
		univ.getGlobalCenterPoint(c);
		univ.centerAt(c);
	}

	public void centerOrigin() {
		univ.centerAt(new Point3d());
	}

	public void fitViewToUniverse() {
		univ.adjustView();
	}

	public void fitViewToContent(Content c) {
		if(!checkSel(c))
			return;
		univ.adjustView(c);
	}

	public void record360() {
		ImagePlus movie = univ.record360();
		if(movie != null)
			movie.show();
		record(RECORD_360);
	}

	public void startFreehandRecording() {
		univ.startFreehandRecording();
		record(START_FREEHAND_RECORDING);
	}

	public void stopFreehandRecording() {
		ImagePlus movie = univ.stopFreehandRecording();
		if(movie != null)
			movie.show();
		record(STOP_FREEHAND_RECORDING);
	}

	public void startAnimation() {
		univ.startAnimation();
		record(START_ANIMATE);
	}

	public void stopAnimation() {
		univ.pauseAnimation();
		record(STOP_ANIMATE);
	}

	public void changeAnimationOptions() {
		GenericDialog gd = new GenericDialog("Change animation axis");
		String[] choices = new String[] {"x axis", "y axis", "z axis"};

		Vector3f axis = new Vector3f();
		univ.getRotationAxis(axis);
		axis.normalize();
		float interval = univ.getRotationInterval();

		int idx = 0;
		if(axis.x == 0 && axis.y == 1 && axis.z == 0)
			idx = 1;
		if(axis.x == 0 && axis.y == 0 && axis.z == 1)
			idx = 2;
		gd.addChoice("Rotate around", choices, choices[idx]);
		gd.addNumericField("Rotation interval", interval, 2, 6, "degree");

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		idx = gd.getNextChoiceIndex();
		switch(idx) {
			case 0: axis.x = 1; axis.y = 0; axis.z = 0; break;
			case 1: axis.x = 0; axis.y = 1; axis.z = 0; break;
			case 2: axis.x = 0; axis.y = 0; axis.z = 1; break;
		}
		interval = (float)gd.getNextNumber();
		univ.setRotationAxis(axis);
		univ.setRotationInterval(interval);
	}

	public void viewPreferences() {
		UniverseSettings.initFromDialog(univ);
	}

	public void editScalebar() {
		Scalebar sc = univ.getScalebar();
		final GenericDialog gd = new GenericDialog(
				"Edit scalebar...", univ.getWindow());
		gd.addNumericField("x position", sc.getX(), 2);
		gd.addNumericField("y position", sc.getY(), 2);
		gd.addNumericField("length", sc.getLength(), 2);
		gd.addStringField("Units", sc.getUnit(), 5);
		gd.addChoice("Color", ColorTable.colorNames, 
				ColorTable.getColorName(sc.getColor()));
		gd.addCheckbox("show", univ.isAttributeVisible(
				Image3DUniverse.ATTRIBUTE_SCALEBAR));
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		sc.setPosition((float)gd.getNextNumber(), 
				(float)gd.getNextNumber());
		sc.setLength((float)gd.getNextNumber());
		sc.setUnit(gd.getNextString());
		sc.setColor(ColorTable.getColor(gd.getNextChoice()));
		boolean vis = gd.getNextBoolean();
		univ.showAttribute(Image3DUniverse.ATTRIBUTE_SCALEBAR, vis);
	}



	/* **********************************************************
	 * Help menu
	 * *********************************************************/
	public void j3dproperties() {
		TextWindow tw = new TextWindow("Java 3D Properties", 
			"Key\tValue", "", 512, 512);
		Map props = Image3DUniverse.getProperties();
		tw.append("Java 3D properties\n \n");
		for(Iterator it = props.entrySet().iterator();
						it.hasNext();) {
			Map.Entry me = (Map.Entry)it.next();
			tw.append(me.getKey() + "\t" + me.getValue());
		}
		props = univ.getCanvas().queryProperties();
		tw.append(" \nRendering properties\n \n");
		for(Iterator it = props.entrySet().iterator();
						it.hasNext();) {
			Map.Entry me = (Map.Entry)it.next();
			tw.append(me.getKey() + "\t" + me.getValue());
		}
	}

	private float[] readTransform(Content selected) {
		final GenericDialog gd = new GenericDialog(
				"Read transformation", univ.getWindow());
		Transform3D t1 = new Transform3D();
		selected.getLocalTranslate().getTransform(t1);
		Transform3D t2 = new Transform3D();
		selected.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		float[] matrix = new float[16];
		t1.get(matrix);
		String transform = affine2string(matrix);
		gd.addStringField("Transformation", transform, 25);
		Panel p = new Panel(new FlowLayout());
		Button b = new Button("Open from file");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float[] m = new TransformIO().
						openAffineTransform();
				if(m != null) {
					TextField tf = (TextField)gd.
						getStringFields().get(0);
					tf.setText(affine2string(m));
					tf.repaint();
				}
			}
		});
		p.add(b);
		gd.addPanel(p);
		gd.showDialog();
		if(gd.wasCanceled())
			return null;

		transform = gd.getNextString();
		float[] m = string2affine(transform);
		return m;
	}

	private FastMatrix toFastMatrix(Transform3D t3d) {
		Matrix4d m = new Matrix4d();
		t3d.get(m);
		return new FastMatrix(new double[][] {
			{m.m00, m.m01, m.m02, m.m03}, 
			{m.m10, m.m11, m.m12, m.m13},
			{m.m20, m.m21, m.m22, m.m23}});
	}

	private String affine2string(float[] matrix) {
		String transform = "";
		for(int i = 0; i < matrix.length; i++) {
			transform += matrix[i] + " ";
		}
		return transform;
	}

	private float[] string2affine(String transform){
		String[] s = ij.util.Tools.split(transform);
		float[] m = new float[s.length];
		for(int i = 0; i < s.length; i++) {
			m[i] = Float.parseFloat(s[i]);
		}
		return m;
	}

	private static final int getAutoThreshold(ImagePlus imp) {
		int[] histo = new int[256];
		int d = imp.getStackSize();
		for(int z = 0; z < d; z++) {
			byte[] p = (byte[])imp.getStack().getPixels(z+1);
			for(int i = 0; i < p.length; i++) {
				histo[(int)(p[i]&0xff)]++;
			}
		}
		return imp.getProcessor().getAutoThreshold(histo);
	}

	private final boolean checkSel(Content c) {
		if(c == null) {
			IJ.error("Selection required");
			return false;
		}
		return true;
	}



	/* **********************************************************
	 * Recording methods
	 * *********************************************************/
	public static void record(String command, String... args) {
		command = "call(\"ij3d.ImageJ3DViewer." + command;
		for(int i = 0; i < args.length; i++)
			command += "\", \"" + args[i];
		command += "\");\n";
		if(Recorder.record)
			Recorder.recordString(command);
	}



	/* **********************************************************
	 * Thread which handles the updates of sliders
	 * *********************************************************/
	private abstract class SliderAdjuster extends Thread {
		boolean go = false;
		int newV;
		ContentInstant content;
		Image3DUniverse univ;
		final Object lock = new Object();

		SliderAdjuster() {
			super("VIB-SliderAdjuster");
			setPriority(Thread.NORM_PRIORITY);
			setDaemon(true);
		}

		/*
		 * Set a new event, overwritting previous if any.
		 */
		void exec(final int newV, final ContentInstant content, final Image3DUniverse univ) {
			synchronized (lock) {
				this.newV = newV;
				this.content = content;
				this.univ = univ;
			}
			synchronized (this) { notify(); }
		}

		public void quit() {
			this.go = false;
			synchronized (this) { notify(); }
		}
		
		/*
		 * This class has to be implemented by subclasses, to define
		 * the specific updating function.
		 */
		protected abstract void setValue(final ContentInstant c, final int v);

		@Override
		public void run() {
			go = true;
			while (go) {
				try {
					if (null == content) {
						synchronized (this) { wait(); }
					}
					if (!go) return;
					// 1 - cache vars, to free the lock very quickly
					ContentInstant c;
					int transp = 0;
					Image3DUniverse u;
					synchronized (lock) {
						c = this.content;
						transp = this.newV;
						u = this.univ;
					}
					// 2 - exec cached vars
					if (null != c) {
						setValue(c, transp);
					}
					// 3 - done: reset only if no new request was put
					synchronized (lock) {
						if (c == this.content) {
							this.content = null;
							this.univ = null;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	ExecutorService exec = Executors.newSingleThreadExecutor();

	/** Destroy the ExecutorService that runs Runnable tasks. */
	public void flush() {
		if (null != exec) {
			synchronized (exec) {
				exec.shutdownNow();
				exec = null;
			}
		}
	}

	/** Submit a task for execution, to the single-threaded executor. */
	public void execute(Runnable task) {
		if (null == exec) {
			IJ.log("The executer service has been shut down!");
			return;
		}
		exec.submit(task);
	}
}
