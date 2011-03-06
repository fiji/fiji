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
		private void restore() {
			for (final Map.Entry<CustomTriangleMesh,List<Point3f>> e : data.entrySet()) {
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
			if (null == content) return;
			ContentInstant ci = content.getCurrent();
			if (null == ci) return;
			ContentNode node = ci.getContent();
			if (null == node) return;
			if (node instanceof CustomMultiMesh) {
				CustomMultiMesh multi = (CustomMultiMesh)node;
				for (int i=0; i<multi.size(); ++i) {
					CustomMesh m = multi.getMesh(i);
					if (m instanceof CustomTriangleMesh) {
						CustomTriangleMesh tm = (CustomTriangleMesh)m;
						if (data.containsKey(tm)) continue; // already stored
						data.put(tm, deepCopy(tm.getMesh()));
					}
				}
			} else if (node instanceof CustomMeshNode) {
				CustomMesh m = ((CustomMeshNode)node).getMesh();
				if (m instanceof CustomTriangleMesh) {
					CustomTriangleMesh tm = (CustomTriangleMesh)m;
					if (data.containsKey(tm)) return; // already stored
					data.put(tm, deepCopy(tm.getMesh()));
				}
			}
		}

		private final List<Point3f> deepCopy(final List<Point3f> t) {
			final ArrayList<Point3f> list = new ArrayList<Point3f>(t.size());
			for (final Point3f p : t)
				list.add(new Point3f(p));
			return list;
		}
	}

	static private final void smooth(final CustomTriangleMesh tm, final int iterations, final Originals originals) {
		// Start always from the original mesh
		final List<Point3f> triangles = originals.getCopyOfOriginals(tm);
		MeshEditor.smooth2(triangles, iterations);
		apply(tm, triangles);
	}

	static private final void smooth(final Content c, final int iterations, final Originals originals) {
		if (null == c) return;
		ContentNode cn = c.getContent();
		// First check for CustomMultiMesh, given that it extends CustomMeshNode
		if(cn instanceof CustomMultiMesh) {
			originals.add(c); // ensure it's there
			CustomMultiMesh multi = (CustomMultiMesh)cn;
			for(int i=0; i<multi.size(); i++) {
				CustomMesh m = multi.getMesh(i);
				if(m instanceof CustomTriangleMesh)
					smooth((CustomTriangleMesh)m, iterations, originals);
			}
		} else if(cn instanceof CustomMeshNode) {
			originals.add(c); // ensure it's there
			CustomMesh mesh = ((CustomMeshNode)cn).getMesh();
			if(mesh instanceof CustomTriangleMesh)
				smooth((CustomTriangleMesh)mesh, iterations, originals);
		} else {
			IJ.log("Cannot smooth content of class " + cn.getClass());
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
		}
	}

	static private Task launchTask(final GenericDialog gd, final Scrollbar s, final Choice c, final Object pivot, final Task[] state, final Runnable r) {
		return new Task(pivot, state) {
			{ setPriority(Thread.NORM_PRIORITY); }
			public void run() {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							s.setEnabled(false);
							c.setEnabled(false);
							gd.setEnabled(false);
						}
					});
					try {
						r.run();
					} catch (Throwable t) {
						t.printStackTrace();
					}
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							s.setEnabled(true);
							c.setEnabled(true);
							gd.setEnabled(true);
						}
					});
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

		// Prepare the dialog
		final GenericDialog gd = new GenericDialog("Smooth meshes") {
			public void windowClosed(WindowEvent we) {
				GenericDialog gd = (GenericDialog) we.getSource();
				if (gd.wasCanceled()) {
					// ... or the window closed.
					originals.restore();
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
		final Task[] lastTask = new Task[1];
		final Object pivot = new Object();

		final Listener listener = new Listener() {
			public void itemStateChanged(ItemEvent ie) {
				run(slider.getValue());
			}
			public void adjustmentValueChanged(AdjustmentEvent ae) {
				if (ae.getValueIsAdjusting()) return; // wait until the slider stops
				// Check that the value has really changed
				final int v = slider.getValue();
				if (v == lastValue[0]) return;
				lastValue[0] = v;
				run(v);
			}
			private final void run(final int iterations) {
				synchronized (pivot) {
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
						// Quit if nothing to smooth (zero iterations)
						if (0 == iterations) {
							originals.restore();
							return;
						} else if (1 == all[0] && 0 == choiceValue) {
							// changing from smooth all to smooth only the selected mesh
							originals.restore();
						}
						all[0] = choiceValue;
						// Smooth meshes
						if (1 == choice.getSelectedIndex()) {
							for (final Content c : (Collection<Content>)(Collection) univ.getContents()) {
								smooth(c, iterations, originals);
							}
						} else {
							smooth(univ.getSelected(), iterations, originals);
						}
					}
				});
			}
		};

		slider.addAdjustmentListener(listener);
		choice.addItemListener(listener);

		gd.setModal(false);

		gd.showDialog();
		if (gd.wasCanceled()) {
			originals.restore();
		}
	}
}
