package isosurface;

import ij3d.Image3DUniverse;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.ContentNode;
import customnode.CustomMesh;
import customnode.CustomTriangleMesh;
import customnode.CustomMultiMesh;
import customnode.CustomMeshNode;

import ij.IJ;
import ij.gui.GenericDialog;

import java.awt.Choice;
import java.awt.Scrollbar;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import javax.vecmath.Point3f;
import javax.swing.SwingUtilities;

public class SmoothControl {

	static private final void apply(final CustomTriangleMesh m, final List<Point3f> triangles) {
		final List<Point3f> current = m.getMesh(); // the triangles, specified as triplets of vertices
		for (int i=0; i<current.size(); ++i) {
			current.get(i).set(triangles.get(i));
		}
		m.update();
	}

	static private final class Originals {

		final private Map<CustomTriangleMesh,List<Point3f>> data = new HashMap<CustomTriangleMesh,List<Point3f>>();

		private Originals() {}

		/** Restore the coordinates of the points in all meshes. */
		private void restore(final Content except) {
			final Set<CustomTriangleMesh> avoid = findMeshes(except);
			for (final Map.Entry<CustomTriangleMesh,List<Point3f>> e : data.entrySet()) {
				if (avoid.contains(e.getKey())) continue;
				apply(e.getKey(), e.getValue());
			}
			IJ.showStatus("Restored meshes");
		}

		/** Add meshes to the internal list of copies when appropriate. */
		private void copy(final Image3DUniverse univ, final boolean all) {
			if (all)
				for (final Content c : (Collection<Content>)(Collection)univ.getContents())
					add(c);
	 		else
				add(univ.getSelected());
		}

		private final List<Point3f> getCopyOfOriginals(final CustomTriangleMesh tm) {
			return deepCopy(data.get(tm));
		}

		/** Add any CustomTriangleMesh contained in {@param content} only if not there already. */
		private void add(final Content content) {
			for (final CustomTriangleMesh tm : findMeshes(content)) {
				if (data.containsKey(tm)) continue; // already stored
				data.put(tm, deepCopy(tm.getMesh()));
			}
		}

		private final List<Point3f> deepCopy(final List<Point3f> t) {
			final ArrayList<Point3f> list = new ArrayList<Point3f>(t.size());
			for (final Point3f p : t)
				list.add(new Point3f(p));
			return list;
		}
	}

	static public final Set<CustomTriangleMesh> findMeshes(final Content content) {
		final HashSet<CustomTriangleMesh> meshes = new HashSet<CustomTriangleMesh>();
		if (null == content) return meshes;
		ContentInstant ci = content.getCurrent();
		if (null == ci) return meshes;
		ContentNode node = ci.getContent();
		if (null == node) return meshes;
		// Must check first for multi, since it is also a CustomMeshNode
		if (node instanceof CustomMultiMesh) {
			CustomMultiMesh multi = (CustomMultiMesh)node;
			for (int i=0; i<multi.size(); ++i) {
				CustomMesh m = multi.getMesh(i);
				if (m instanceof CustomTriangleMesh) {
					meshes.add((CustomTriangleMesh)m);
				}
			}
		} else if (node instanceof CustomMeshNode) {
			CustomMesh m = ((CustomMeshNode)node).getMesh();
			if (m instanceof CustomTriangleMesh) {
				meshes.add((CustomTriangleMesh)m);
			}
		} else if (node instanceof MeshGroup) {
			CustomMesh m = ((MeshGroup)node).getMesh();
			if (m instanceof CustomTriangleMesh) {
				meshes.add((CustomTriangleMesh)m);
			}
		}
		return meshes;
	}

	static private final void smooth(final CustomTriangleMesh tm, final int iterations, final Originals originals) {
		// Start always from the original mesh
		final List<Point3f> triangles = originals.getCopyOfOriginals(tm);
		MeshEditor.smooth2(triangles, iterations);
		apply(tm, triangles);
	}

	static private final void smooth(final Content c, final int iterations, final Originals originals) {
		if (null == c) return;
		final ContentNode cn = c.getContent();
		final Set<CustomTriangleMesh> meshes = findMeshes(c);
		if (meshes.isEmpty()) {
			IJ.log("Cannot smooth content of class " + cn.getClass());
			return;
		}
		originals.add(c); // ensure it's there
		for (CustomTriangleMesh tm : meshes) {
			smooth(tm, iterations, originals);
		}
	}

	static private class Task extends Thread {
		final Task[] state;
		final Object pivot;
		Task next = null;

		private Task(final Object pivot, final Task[] state) {
			super();
			this.pivot = pivot;
			this.state = state;
		}
		private void launchWhenDone(final Task next) {
			this.next = next;
			interrupt(); // will stop the MeshEditor.smooth2
		}
	}

	static private Task launchTask(final GenericDialog gd, final Scrollbar s, final Choice c, final Object pivot, final Task[] state, final Runnable r) {
		return new Task(pivot, state) {
			{ setPriority(Thread.NORM_PRIORITY); }
			public void run() {
				try {
					r.run();
				} catch (Throwable e) {
					e.printStackTrace();
				}
				// Reset state
				synchronized (pivot) {
					if (null == next) state[0] = null;
					else {
						state[0] = next;
						next.start();
					}
				}
			}
		};
	}

	private static interface Listener extends AdjustmentListener, ItemListener {}

	private final Originals originals;

	public SmoothControl(final Image3DUniverse univ) {
		// Start with a copy of the selected mesh, if any
		originals = new Originals();
		originals.copy(univ, false);
		final Task[] lastTask = new Task[1];
		final Object pivot = new Object();

		// Prepare the dialog
		final GenericDialog gd = new GenericDialog("Smooth meshes") {
			public void windowClosed(WindowEvent we) {
				GenericDialog gd = (GenericDialog) we.getSource();
				if (gd.wasCanceled()) {
					// ... or the window closed.
					synchronized (pivot) {
						if (null != lastTask[0]) {
							lastTask[0].interrupt();
							lastTask[0] = null;
						}
					}
					new Thread() {
						{ setPriority(Thread.NORM_PRIORITY); }
						public void run() {
							originals.restore(null);
						}
					}.start();
					return;
				}
				if (gd.wasOKed()) {
					return; // all stays smoothed as desired
				}
			}
		};
		gd.addSlider("Iterations", 0, 100, 0);
		final Scrollbar slider = (Scrollbar)gd.getSliders().get(0);

		final String[] c = new String[]{"Selected mesh", "All meshes"};
		gd.addChoice("Process", c, c[0]);
		final Choice choice = (Choice)gd.getChoices().get(0);

		final int[] all = new int[1]; // zero by default, meaning selected mesh only
		final int[] lastValue = new int[1];

		final Listener listener = new Listener() {
			public void itemStateChanged(ItemEvent ie) {
				run(slider.getValue());
			}
			public void adjustmentValueChanged(AdjustmentEvent ae) {
				if (ae.getValueIsAdjusting()) return; // wait until the slider stops
				if (!slider.isEnabled()) {
					IJ.log("slider not enabled!");
					return;
				}
				// Check that the value has really changed
				final int v = slider.getValue();
				if (v == lastValue[0]) return;
				lastValue[0] = v;
				run(v);
			}
			private final void run(final int iterations) {
				synchronized (pivot) {
					if (!gd.isVisible()) return;
					if (null != lastTask[0]) {
						lastTask[0].launchWhenDone(task(iterations));
					} else {
						lastTask[0] = task(iterations);
						lastTask[0].start();
					}
				}
			}
			private final Task task(final int iterations) {
				IJ.showStatus("Smoothing with " + iterations + " iterations");
				return launchTask(gd, slider, choice, pivot, lastTask, new Runnable() {
					public void run() {
						final int choiceValue = choice.getSelectedIndex();
						// Restore if nothing should be smoothed (zero iterations)
						if (0 == iterations) {
							originals.restore(null);
							return;
						} else if (1 == all[0] && 0 == choiceValue) {
							// Changing from smooth all to smooth only the selected mesh.
							// The selected mesh is already smoothed, so skip it:
							originals.restore(univ.getSelected());
							return;
						}
						// Smooth meshes
						if (1 == choiceValue) {
							// All meshes
							for (final Content c : (Collection<Content>)(Collection) univ.getContents()) {
								if (0 == all[0] && 1 == choiceValue && c == univ.getSelected()) continue; // skip selected when going from selected to all.
								smooth(c, iterations, originals);
							}
						} else {
							smooth(univ.getSelected(), iterations, originals);
						}
						all[0] = choiceValue;
					}
				});
			}
		};

		slider.addAdjustmentListener(listener);
		choice.addItemListener(listener);

		gd.setModal(false);

		gd.showDialog();
	}
}
