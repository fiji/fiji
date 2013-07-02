package fiji.plugin.trackmate.visualization.hyperstack;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.semiauto.SemiAutoTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;

public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ToolWithOptions {

	private static final boolean DEBUG = false;

	private static final double COARSE_STEP = 2;
	private static final double FINE_STEP = 0.2f;
	private static final String TOOL_NAME = "Spot edit tool";
	private static final String TOOL_ICON ="CdffL10e0"
			+ "CafdD01C67aD11C6aaD21C6fbL31e1CbfeDf1"
			+ "CaedD02C64aD12C35aD22C5fbD32C6fbL42e2CbfdDf2"
			+ "CafdD03C5eaD13C4bbD23C38bD33C5daD43C6fbL53e3CbfdDf3"
			+ "CafdD04C6fbL1424C48aD34C42aD44C5caD54C6fbL64e4CbfdDf4"
			+ "CafdD05C6fbL1525C5caD35C35aD45C4bbD55C6fbL65e5CbfdDf5"
			+ "CafdD06C6fbL1636C5dbD46C38bD56C6fbL66e6CbfdDf6"
			+ "CafdD07C6fbL1747C38bD57C49aD67C6fbL77e7CbfdDf7"
			+ "CafdD08C6fbL1848C58aD58C31aD68C5baD78C6fbL88e8CbfdDf8"
			+ "CafdD09C6fbL1949C4abD59C37bD69C38bD79C4bbD89C5eaD99C6fbLa9e9CbfdDf9"
			+ "CafdD0aC6fbL1a4aC39bD5aC5ebL6a7aC44aD8aC43aD9aC39bDaaC46aDbaC55aDcaC6fbLdaeaCbfdDfa"
			+ "CafdD0bC6fbL1b3bC58aD4bC36aD5bC6fbL6b7bC5aaD8bC59aD9bC5dbDabC47aDbbC34aDcbC5cbDdbC6fbDebCbfdDfb"
			+ "CafdD0cC6fbL1c2cC5cbD3cC33aD4cC55aD5cC6fbL6cbcC5dbDccC38bDdcC5ebDecCbfdDfc"
			+ "CafdD0dC58aD1dC47aD2dC38bD3dC5cbD4dC6eaD5dC6fbL6dcdC49aDddC43aDedCbddDfd"
			+ "CafdD0eC64aD1eC45aD2eC5fbD3eC6fbL4eceC5baDdeC65aDeeCbddDfe"
			+ "CeffD0fCbedD1fCbeeD2fCcfeL3fdfCbfeDefCeffDff";


	/** Fall back default radius when the settings does not give a default radius to use. */
	private static final double FALL_BACK_RADIUS = 5;

	/** The singleton instance. */
	private static SpotEditTool instance;
	/** Stores the edited spot in each {@link ImagePlus}. */
	private HashMap<ImagePlus, Spot> editedSpots = new HashMap<ImagePlus, Spot>();
	/** Stores the view possible attached to each {@link ImagePlus}. */
	HashMap<ImagePlus, HyperStackDisplayer> displayers = new HashMap<ImagePlus, HyperStackDisplayer>();
	/** The radius of the previously edited spot. */
	private Double previousRadius = null;
	private Spot quickEditedSpot;
	/** Flag for the auto-linking mode. */
	private boolean autolinkingmode = false;

	SpotEditToolParams params = new SpotEditToolParams();

	private Logger logger = Logger.VOID_LOGGER;

	private SpotEditToolConfigPanel configPanel;
	/**
	 * The last {@link ImagePlus} on which an action happened.
	 */
	ImagePlus imp;



	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Singleton
	 */
	private SpotEditTool() {	}

	/**
	 * Return the singleton instance for this tool. If it was not previously instantiated, this calls
	 * instantiates it. 
	 */
	public static SpotEditTool getInstance() {
		if (null == instance) {
			instance = new SpotEditTool();
			if (DEBUG)
				System.out.println("[SpotEditTool] Instantiating: "+instance);
		}
		if (DEBUG)
			System.out.println("[SpotEditTool] Returning instance: "+instance);
		return instance;
	}

	/**
	 * Return true if the tool is currently present in ImageJ toolbar.
	 */
	public static boolean isLaunched() {
		Toolbar toolbar = Toolbar.getInstance();
		if (null != toolbar && toolbar.getToolId(TOOL_NAME) >= 0) 
			return true;
		return false;
	}

	/*
	 * METHODS
	 */

	@Override
	public String getToolName() {
		return TOOL_NAME;
	}	

	@Override
	public String getToolIcon() {
		return TOOL_ICON;
	}
	
	/**
	 * Overridden so that we can keep track of the last ImagePlus actions are taken on. 
	 * Very much like ImageJ.
	 */
	@Override
	public ImagePlus getImagePlus(ComponentEvent e) {
		imp = super.getImagePlus(e); 
		return imp;
	}

	/**
	 * Register the given {@link HyperStackDisplayer}. If this method id not called, the tool will not
	 * respond.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	 */
	public void register(final ImagePlus imp, final HyperStackDisplayer displayer) {
		if (DEBUG) System.out.println("[SpotEditTool] Currently registered: " + displayers);

		if (displayers.containsKey(imp)) {
			unregisterTool(imp);
			if (DEBUG) System.out.println("[SpotEditTool] De-registering " + imp + " as tool listener.");
		}

		displayers.put(imp, displayer);
		if (DEBUG) {
			System.out.println("[SpotEditTool] Registering "+imp+" and "+displayer + "." +
					" Currently registered: " + displayers);
		}
	}

	/*
	 * MOUSE AND MOUSE MOTION
	 */

	@Override
	public void mouseClicked(MouseEvent e) {

		final ImagePlus imp = getImagePlus(e);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (DEBUG) {
			System.out.println("[SpotEditTool] @mouseClicked");
			System.out.println("[SpotEditTool] Got "+imp+ " as ImagePlus");
			System.out.println("[SpotEditTool] Matching displayer: "+displayer);

			for (MouseListener ml : imp.getCanvas().getMouseListeners()) {
				System.out.println("[SpotEditTool] mouse listener: "+ml);
			}

		}

		if (null == displayer)
			return;

		final Spot clickLocation = makeSpot(imp, displayer, getImageCanvas(e), e.getPoint());
		final int frame = displayer.imp.getFrame() - 1;
		final Model model = displayer.getModel();
		Spot target = model.getSpots().getSpotAt(clickLocation, frame, true);
		Spot editedSpot = editedSpots.get(imp);

		SelectionModel selectionModel = displayer.getSelectionModel();
		
		// Check desired behavior
		switch (e.getClickCount()) {

		case 1: {
			// Change selection
			// only if we are not currently editing.
			if (null != editedSpot) {
				return;
			}
			// If no target, we clear selection
			if (null == target) {
				
				if (!autolinkingmode) {
					selectionModel.clearSelection();
				}

			} else {

				updateStatusBar(target, imp.getCalibration().getUnits());
				final int addToSelectionMask = InputEvent.SHIFT_DOWN_MASK;
				if ((e.getModifiersEx() & addToSelectionMask) == addToSelectionMask) { 
					if (selectionModel.getSpotSelection().contains(target)) {
						selectionModel.removeSpotFromSelection(target);
					} else {
						selectionModel.addSpotToSelection(target);
					}
				} else {
					selectionModel.clearSpotSelection();
					selectionModel.addSpotToSelection(target);
				}
			}
			break;
		}

		case 2: {
			// Edit spot

			if (null == editedSpot) {
				// No spot is currently edited, we pick one to edit
				Double radius;
				if (null != target && null != target.getFeature(Spot.RADIUS)) {
					radius = target.getFeature(Spot.RADIUS);
				} else {
					radius = previousRadius;
					if (null == radius) {
						radius = FALL_BACK_RADIUS;
					} 
				}
				if (null == target || target.squareDistanceTo(clickLocation) > radius*radius) {
					// Create a new spot if not inside one
					target = clickLocation;
					if (null == previousRadius) {
						previousRadius = radius;
					}
					target.putFeature(Spot.RADIUS, previousRadius);
				}
				editedSpot = target;
				displayer.spotOverlay.editingSpot = editedSpot;
				displayer.refresh();
				// Edit spot
				if (DEBUG)
					System.out.println("[SpotEditTool] mouseClicked: Set "+editedSpot+" as editing spot for this imp.");

			} else {
				// We leave editing mode
				if (DEBUG)
					System.out.println("[SpotEditTool] mouseClicked: Got "+editedSpot+" as editing spot for this imp, leaving editing mode.");


				// A hack: we update the current z and t of the edited spot to the current one, 
				// because it is not updated otherwise: there is no way to listen to slice change
				double calibration[] = TMUtils.getSpatialCalibration(imp);
				final double zslice = (displayer.imp.getSlice()-1) * calibration[2];
				editedSpot.putFeature(Spot.POSITION_Z, zslice);
				Double initFrame = editedSpot.getFeature(Spot.FRAME);
				// Move it in Z
				final double z = (displayer.imp.getSlice()-1) * calibration[2];
				editedSpot.putFeature(Spot.POSITION_Z, z);
				editedSpot.putFeature(Spot.POSITION_T, frame * imp.getCalibration().frameInterval);
				editedSpot.putFeature(Spot.FRAME, Double.valueOf(frame));

				model.beginUpdate();
				try {
					if (initFrame == null) {
						// Means that the spot was created 
						model.addSpotTo(editedSpot, frame);
					} else if (initFrame != frame) {
						// Move it to the new frame
						model.moveSpotFrom(editedSpot, initFrame.intValue(), frame);
					} else {
						// The spots pre-existed and was not moved across frames
						model.updateFeatures(editedSpot);
					}

				} finally {
					model.endUpdate();
				}
				
				/*
				 * If we are in auto-link mode, we create an edge with spot in selection,
				 * if there is just one and if it is in a previous frame
				 */
				if (autolinkingmode) {
					Set<Spot> spotSelection = selectionModel.getSpotSelection();
					if (spotSelection.size() == 1) {
						Spot source = spotSelection.iterator().next();
						if (editedSpot.diffTo(source, Spot.FRAME) > 0) {
							model.beginUpdate();
							try {
								model.addEdge(source, editedSpot, -1);
								logger.log("Created a link between " + source + " and " + editedSpot + ".\n");
							} finally {
								model.endUpdate();
							}
						}
					}
				}
				
				// Set selection
				selectionModel.clearSpotSelection();
				selectionModel.addSpotToSelection(editedSpot);
				
				// Forget edited spot, but remember its radius
				previousRadius = editedSpot.getFeature(Spot.RADIUS);
				editedSpot = null;
				displayer.spotOverlay.editingSpot = null;
			}
			break;
		}
		}
		editedSpots.put(imp, editedSpot);
	}



	@Override
	public void mousePressed(MouseEvent e) {}


	@Override
	public void mouseReleased(MouseEvent e) {}


	@Override
	public void mouseEntered(MouseEvent e) {}


	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		final ImagePlus imp = getImagePlus(e);
		final double[] calibration = TMUtils.getSpatialCalibration(imp);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null == editedSpot)
			return;
		
		Point mouseLocation = e.getPoint();
		ImageCanvas canvas = getImageCanvas(e);
		double x = (-0.5 + canvas.offScreenXD(mouseLocation.x) ) * calibration[0];
		double y = (-0.5 + canvas.offScreenYD(mouseLocation.y) ) * calibration[1];
		double z = (imp.getSlice() - 1) * calibration[2];
		editedSpot.putFeature(Spot.POSITION_X, x);
		editedSpot.putFeature(Spot.POSITION_Y, y);
		editedSpot.putFeature(Spot.POSITION_Z, z);
		displayer.imp.updateAndDraw();
		updateStatusBar(editedSpot, imp.getCalibration().getUnits());	
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (quickEditedSpot == null)
			return;
		final ImagePlus imp = getImagePlus(e);
		final double[] calibration = TMUtils.getSpatialCalibration(imp);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null != editedSpot)
			return;
		
		Point mouseLocation = e.getPoint();
		ImageCanvas canvas = getImageCanvas(e);
		double x = (-0.5 + canvas.offScreenXD(mouseLocation.x) ) * calibration[0];
		double y = (-0.5 + canvas.offScreenYD(mouseLocation.y) ) * calibration[1];
		double z = (imp.getSlice() - 1) * calibration[2];
				
		quickEditedSpot.putFeature(Spot.POSITION_X, x);
		quickEditedSpot.putFeature(Spot.POSITION_Y, y);
		quickEditedSpot.putFeature(Spot.POSITION_Z, z);
		displayer.imp.updateAndDraw();

	}


	/*
	 * MOUSEWHEEL 
	 */

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		final ImagePlus imp = getImagePlus(e);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null == editedSpot || !e.isAltDown())
			return;
		double radius = editedSpot.getFeature(Spot.RADIUS);
		double dx = imp.getCalibration().pixelWidth;
		if (e.isShiftDown()) {
			radius += e.getWheelRotation() * dx * COARSE_STEP;
		} else { 
			radius += e.getWheelRotation() * dx * FINE_STEP;
		}
		if (radius < dx) {
			return;
		}
		
		editedSpot.putFeature(Spot.RADIUS, radius);
		displayer.imp.updateAndDraw();
		e.consume();
		updateStatusBar(editedSpot, imp.getCalibration().getUnits());
	}

	/*
	 * KEYLISTENER
	 */

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyPressed(KeyEvent e) { 

		if (DEBUG) 
			System.out.println("[SpotEditTool] keyPressed: "+e.getKeyChar());

		final ImagePlus imp = getImagePlus(e);
		if (imp == null)
			return;
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;

		Model model = displayer.getModel();
		SelectionModel selectionModel = displayer.getSelectionModel();
		Spot editedSpot = editedSpots.get(imp);
		final ImageCanvas canvas = getImageCanvas(e);

		int keycode = e.getKeyCode(); 

		switch (keycode) {

		// Delete currently edited spot
		case KeyEvent.VK_DELETE: {

			if (null == editedSpot) {
				ArrayList<Spot> spotSelection = new ArrayList<Spot>(selectionModel.getSpotSelection());
				ArrayList<DefaultWeightedEdge> edgeSelection = new ArrayList<DefaultWeightedEdge>(selectionModel.getEdgeSelection());
				model.beginUpdate();
				try {
					selectionModel.clearSelection();
					for(DefaultWeightedEdge edge : edgeSelection) {
						model.removeEdge(edge);
					}
					for(Spot spot : spotSelection) {
						model.removeSpot(spot);
					}
				} finally {
					model.endUpdate();
				}

			} else {
				model.beginUpdate();
				try {
					model.removeSpot(editedSpot);
				} finally {
					model.endUpdate();
				}
				editedSpot = null;
				editedSpots.put(imp, null);
			}
			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick add spot at mouse
		case KeyEvent.VK_A: {

			if (null == editedSpot) {
				
				if (e.isShiftDown()) {
					
					// Semi-auto tracking
					semiAutoTracking(model, selectionModel, imp);
					
				} else {

					// Create and drop a new spot
					double radius;
					if (null != previousRadius) {
						radius = previousRadius; 
					} else { 
						radius = FALL_BACK_RADIUS;
					}

					Spot newSpot = makeSpot(imp, displayer, canvas, null);
					double dt = imp.getCalibration().frameInterval;
					int frame = displayer.imp.getFrame() - 1;
					newSpot.putFeature(Spot.POSITION_T, frame * dt);
					newSpot.putFeature(Spot.FRAME, Double.valueOf(frame));
					newSpot.putFeature(Spot.RADIUS, radius);
					newSpot.putFeature(Spot.QUALITY, -1d);

					model.beginUpdate();
					try {
						model.addSpotTo(newSpot, frame);
					} finally {
						model.endUpdate();
					}

					/*
					 * If we are in auto-link mode, we create an edge with spot in selection,
					 * if there is just one and if it is in a previous frame
					 */
					if (autolinkingmode) {
						Set<Spot> spotSelection = selectionModel.getSpotSelection();
						if (spotSelection.size() == 1) {
							Spot source = spotSelection.iterator().next();
							if (newSpot.diffTo(source, Spot.FRAME) > 0) {
								model.beginUpdate();
								try {
									model.addEdge(source, newSpot, -1);
									logger.log("Created a link between " + source + " and " + newSpot + ".\n");
								} finally {
									model.endUpdate();
								}
							}
						}
						selectionModel.clearSpotSelection();
						selectionModel.addSpotToSelection(newSpot);
					}

					imp.updateAndDraw();
					e.consume();
				}

			} else {

			}
			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D: {

			if (null == editedSpot) {

				int frame = displayer.imp.getFrame() - 1;
				Spot clickLocation = makeSpot(imp, displayer, canvas, null);
				Spot target = model.getSpots().getSpotAt(clickLocation, frame, true);
				if (null == target) {
					e.consume(); // Consume it anyway, so that we are not bothered by IJ
					return; 
				}
				
				selectionModel.removeSpotFromSelection(target);
				model.beginUpdate();
				try {
					model.removeSpot(target);
				} finally {
					model.endUpdate();
				}

				imp.updateAndDraw();

			} else {

			}
			e.consume();
			break;
		}

		// Quick move spot under the mouse
		case KeyEvent.VK_SPACE: {

			if (null == quickEditedSpot) {
				int frame = displayer.imp.getFrame() - 1;
				Spot clickLocation = makeSpot(imp, displayer, canvas, null);
				quickEditedSpot = model.getSpots().getSpotAt(clickLocation, frame, true);
				if (null == quickEditedSpot) {
					return; // un-consumed event
				}
			}
			e.consume();
			break;

		}

		// Quick change spot radius
		case KeyEvent.VK_Q:
		case KeyEvent.VK_E: {

			e.consume();
			if (null == editedSpot) {

				int frame = displayer.imp.getFrame() - 1;
				Spot clickLocation = makeSpot(imp, displayer, canvas, null);
				Spot target = model.getSpots().getSpotAt(clickLocation, frame, true);
				if (null == target) {
					return;
				}

				int factor;
				if (e.getKeyCode() == KeyEvent.VK_Q) {
					factor = -1;
				} else {
					factor = 1;
				}
				double radius = target.getFeature(Spot.RADIUS);
				double dx = imp.getCalibration().pixelWidth;
				if (e.isShiftDown()) {
					radius += factor * dx * COARSE_STEP;
				} else { 
					radius += factor * dx * FINE_STEP;
				}
				if (radius <= dx) {
					return;
				}

				target.putFeature(Spot.RADIUS, radius);
				model.beginUpdate();
				try {
					model.updateFeatures(target);
				} finally {
					model.endUpdate();
				}

				imp.updateAndDraw();
			}

			break;
		}

		// Copy spots from previous frame
		case KeyEvent.VK_V: {
			if (e.isShiftDown()) {

				int currentFrame = imp.getFrame() - 1;
				if (currentFrame > 0) {

					SpotCollection spots = model.getSpots();
					if (spots.getNSpots(currentFrame-1, true) == 0) {
						e.consume();
						break;
					}
					HashSet<Spot> copiedSpots = new HashSet<Spot>(spots.getNSpots(currentFrame-1, true));
					HashSet<String> featuresKey = new HashSet<String>(spots.iterator(currentFrame-1, true).next().getFeatures().keySet());
					featuresKey.remove(Spot.POSITION_T); // Deal with time separately
					double dt = imp.getCalibration().frameInterval;
					if (dt == 0) {
						dt = 1;
					}

					for (Iterator<Spot> it =  spots.iterator(currentFrame-1, true); it.hasNext();) {
						Spot spot = it.next();
						double[] coords = new double[3];
						TMUtils.localize(spot, coords);
						Spot newSpot = new Spot(coords, spot.getName());
						// Deal with features
						Double val;
						for(String key : featuresKey) {
							val = spot.getFeature(key);
							if (val == null) {
								continue;
							}
							newSpot.putFeature(key, val);
						}
						newSpot.putFeature(Spot.POSITION_T, spot.getFeature(Spot.POSITION_T) + dt);
						copiedSpots.add(newSpot);
					}

					model.beginUpdate();
					try {
						// Remove old ones
						for (Iterator<Spot> it =  spots.iterator(currentFrame, true); it.hasNext();) {
							model.removeSpot(it.next());
						}
						// Add new ones
						for(Spot spot : copiedSpots) {
							model.addSpotTo(spot, currentFrame);
						}
					} finally {
						model.endUpdate();
						imp.updateAndDraw();
					}
				}


				e.consume();
			}
			break;
		}

		case KeyEvent.VK_L: {

			if (e.isShiftDown()) {
				/*
				 * Toggle auto-linking mode
				 */
				autolinkingmode = !autolinkingmode;
				logger.log("Toggled auto-linking mode " + (autolinkingmode ? "on.\n" : "off.\n"));

			} else {
				/*
				 * Toggle a link between two spots.
				 */
				Set<Spot> selectedSpots = selectionModel.getSpotSelection();
				if (selectedSpots.size() == 2) {
					Iterator<Spot> it = selectedSpots.iterator();
					Spot source = it.next();
					Spot target = it.next();

					if (model.getTrackModel().containsEdge(source, target)) {
						/*
						 * Remove it
						 */
						model.beginUpdate();
						try {
							model.removeEdge(source, target);
							logger.log("Removed edge between " + source + " and " + target + ".\n");
						} finally {
							model.endUpdate();
						}


					} else {
						/*
						 * Create a new link
						 */
						int ts = source.getFeature(Spot.FRAME).intValue();
						int tt = target.getFeature(Spot.FRAME).intValue();

						if (tt != ts ) {
							model.beginUpdate();
							try {
								model.addEdge(source, target, -1);
								logger.log("Created an edge between " + source + " and " + target + ".\n");
							} finally { 
								model.endUpdate();
							}
							/*
							 * To emulate a kind of automatic linking, we put the last 
							 * spot to the selection, so several spots can be tracked
							 * in a row without having to de-select one
							 */
							Spot single;
							if (tt > ts) {
								single = target;
							} else {
								single = source;
							}
							selectionModel.clearSpotSelection();
							selectionModel.addSpotToSelection(single);
							
						} else {
							logger.error("Cannot create an edge between two spots belonging in the same frame.");
						}
					}

				} else {
					logger.error("Expected selection to contain 2 spots, found " + selectedSpots.size() + ".\n");
				}

			}
			e.consume();
			break;


		}

		case KeyEvent.VK_W: {
			e.consume(); // consume it: we do not want IJ to close the window
			break;
		}

		}

	}

	private Spot makeSpot(ImagePlus imp, HyperStackDisplayer displayer, ImageCanvas canvas, Point mouseLocation) {
		if (displayer == null) {
			displayer = displayers.get(imp);
		}
		if (mouseLocation == null) {
			mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen(mouseLocation, canvas);
		}
		final double[] calibration = TMUtils.getSpatialCalibration(imp);
		return new Spot(new double[] {
				(-0.5 + canvas.offScreenXD(mouseLocation.x) ) * calibration[0],
				(-0.5 + canvas.offScreenYD(mouseLocation.y) ) * calibration[1],
				(imp.getSlice() - 1) * calibration[2]
		});
	}

	@Override
	public void keyReleased(KeyEvent e) { 
		if (DEBUG) 
			System.out.println("[SpotEditTool] keyReleased: "+e.getKeyChar());

		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE: {
			if (null == quickEditedSpot)
				return;
			final ImagePlus imp = getImagePlus(e);
			if (imp == null)
				return;
			final HyperStackDisplayer displayer = displayers.get(imp);
			if (null == displayer)
				return;
			Model model = displayer.getModel();
			model.beginUpdate();
			try {
				model.updateFeatures(quickEditedSpot);
			} finally {
				model.endUpdate();
			}
			quickEditedSpot = null;
			break;
		}
		}

	}	


	/*
	 * PRIVATE METHODS
	 */

	private void updateStatusBar(final Spot spot, final String units) {
		if (null == spot)
			return;
		String statusString = "";
		if (null == spot.getName() || spot.getName().equals("")) { 
			statusString = String.format("Spot ID%d, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", 
					spot.ID(), spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
					spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.RADIUS), units );
		} else {
			statusString = String.format("Spot %s, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", 
					spot.getName(), spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
					spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.RADIUS), units );
		}
		IJ.showStatus(statusString);
	}
	
	void semiAutoTracking(final Model model, SelectionModel selectionModel, ImagePlus imp) {
		@SuppressWarnings("rawtypes")
		final SemiAutoTracker autotracker = new SemiAutoTracker(model, selectionModel, imp, logger);
		autotracker.setParameters(params.qualityThreshold, params.distanceTolerance);
		autotracker.setNumThreads(4);
		new Thread("TrackMate semi-automated tracking thread") {
			@Override
			public void run() {
				boolean ok = autotracker.checkInput() && autotracker.process();
				if (!ok) {
					logger.error(autotracker.getErrorMessage());
				}
			}
		}.start();
	}

	@Override
	public void showOptionDialog() {
		if (null == configPanel) {
			configPanel = new SpotEditToolConfigPanel(this);
			configPanel.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					logger = Logger.VOID_LOGGER;
				}
			});
		}
		configPanel.setLocation(toolbar.getLocationOnScreen());
		configPanel.setVisible(true);
		logger = configPanel.getLogger();
	}
	
	
	
	/*
	 * INNER CLASSES
	 */
	
	static class SpotEditToolParams {
		/*
		 * Semi-auto tracking parameters
		 */
		/** The fraction of the initial quality above which we keep new spots. The highest, the more intolerant. */
		double qualityThreshold = 0.5;
		/** How close must be the new spot found to be accepted, in radius units. */
		double distanceTolerance = 2d;
		
		@Override
		public String toString() {
			return super.toString() + ": "
					+ "QualityThreshold = " + qualityThreshold 
					+ ", DistanceTolerance = " + distanceTolerance;
		}
	}


}
