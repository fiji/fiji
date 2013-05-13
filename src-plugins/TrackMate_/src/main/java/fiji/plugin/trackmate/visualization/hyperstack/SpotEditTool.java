package fiji.plugin.trackmate.visualization.hyperstack;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.tool.AbstractTool;

public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {

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


	private static SpotEditTool instance;
	private HashMap<ImagePlus, Spot> editedSpots = new HashMap<ImagePlus, Spot>();
	private HashMap<ImagePlus, HyperStackDisplayer> displayers = new HashMap<ImagePlus, HyperStackDisplayer>();
	/** The radius of the previously edited spot. */
	private Double previousRadius = null;
	private Spot quickEditedSpot;


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
	 * Register the given {@link HyperStackDisplayer}. If this method id not called, the tool will not
	 * respond.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	 */
	public void register(final ImagePlus imp, final HyperStackDisplayer displayer) {
		if (DEBUG)
			System.out.println("[SpotEditTool] Registering "+imp+" and "+displayer);
		displayers.put(imp, displayer);
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

		final Point clickPoint = e.getPoint();
		final Spot clickLocation = displayer.getCLickLocation(clickPoint);
		final int frame = displayer.imp.getFrame() - 1;
		final TrackMateModel model = displayer.getModel();
		Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
		Spot editedSpot = editedSpots.get(imp);

		// Check desired behavior
		switch (e.getClickCount()) {

		case 1: {
			// Change selection
			// only if we are not currently editing and if target is non null
			if (null != editedSpot || target == null)
				return;
			updateStatusBar(target, imp.getCalibration().getUnits());
			final int addToSelectionMask = MouseEvent.SHIFT_DOWN_MASK;
			if ((e.getModifiersEx() & addToSelectionMask) == addToSelectionMask) { 
				if (model.getSelectionModel().getSpotSelection().contains(target)) {
					model.getSelectionModel().removeSpotFromSelection(target);
				} else {
					model.getSelectionModel().addSpotToSelection(target);
				}
			} else {
				model.getSelectionModel().clearSpotSelection();
				model.getSelectionModel().addSpotToSelection(target);
			}
			break;
		}

		case 2: {
			// Edit spot

			// Empty current selection
			model.getSelectionModel().clearSelection();

			if (null == editedSpot) {
				// No spot is currently edited, we pick one to edit
				double radius;
				if (null != target && null != target.getFeature(Spot.RADIUS)) {
					radius = target.getFeature(Spot.RADIUS);
				} else {
					Map<String, Object> ss = displayer.settings.detectorSettings;
					Object obj = ss.get(DetectorKeys.DEFAULT_RADIUS);
					if (null == obj) {
						radius = FALL_BACK_RADIUS;
					} else {
						if (Double.class.isInstance(obj)) {
							radius = (Double) obj;
						} else {
							radius = FALL_BACK_RADIUS;
						}
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
				// Edit spot
				if (DEBUG)
					System.out.println("[SpotEditTool] mouseClicked: Set "+editedSpot+" as editing spot for this imp.");

			} else {
				// We leave editing mode
				if (DEBUG)
					System.out.println("[SpotEditTool] mouseClicked: Got "+editedSpot+" as editing spot for this imp, leaving editing mode.");


				// A hack: we update the current z and t of the edited spot to the current one, 
				// because it is not updated otherwise: there is no way to listen to slice change
				final double zslice = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(Spot.POSITION_Z, zslice);
				Integer initFrame = displayer.getModel().getFilteredSpots().getFrame(editedSpot);
				// Move it in Z
				final double z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(Spot.POSITION_Z, z);
				editedSpot.putFeature(Spot.POSITION_T, frame * displayer.settings.dt);
				editedSpot.putFeature(Spot.FRAME, frame);

				model.beginUpdate();
				try {
					if (initFrame == null) {
						// Means that the spot was created 
						model.addSpotTo(editedSpot, frame);
					} else if (initFrame != frame) {
						// Move it to the new frame
						model.moveSpotFrom(editedSpot, initFrame, frame);
					} else {
						// The spots pre-existed and was not moved across frames
						model.updateFeatures(editedSpot);
					}

				} finally {
					model.endUpdate();
				}
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
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null == editedSpot)
			return;
		final double ix = displayer.canvas.offScreenXD(e.getX()) - 0.5d;  // relative to pixel center
		final double iy =  displayer.canvas.offScreenYD(e.getY()) - 0.5d;
		final double x = (double) (ix * displayer.calibration[0]);
		final double y = (double) (iy * displayer.calibration[1]);
		final double z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
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
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null != editedSpot)
			return;

		final double ix = displayer.canvas.offScreenXD(e.getX()) - 0.5d;  // relative to pixel center
		final double iy =  displayer.canvas.offScreenYD(e.getY()) - 0.5d;
		final double x = (double) (ix * displayer.calibration[0]);
		final double y = (double) (iy * displayer.calibration[1]);
		final double z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
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
		if (e.isShiftDown()) 
			radius += e.getWheelRotation() * displayer.calibration[0] * COARSE_STEP;
		else 
			radius += e.getWheelRotation() * displayer.calibration[0] * FINE_STEP;
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

		TrackMateModel model = displayer.getModel();
		Spot editedSpot = editedSpots.get(imp);

		int keycode = e.getKeyCode(); 

		switch (keycode) {

		// Delete currently edited spot
		case KeyEvent.VK_DELETE: {

			if (null == editedSpot) {
				ArrayList<Spot> spotSelection = new ArrayList<Spot>(model.getSelectionModel().getSpotSelection());
				ArrayList<DefaultWeightedEdge> edgeSelection = new ArrayList<DefaultWeightedEdge>(model.getSelectionModel().getEdgeSelection());
				model.beginUpdate();
				try {
					model.getSelectionModel().clearSelection();
					for(DefaultWeightedEdge edge : edgeSelection) {
						model.removeEdge(edge);
					}
					for(Spot spot : spotSelection) {
						model.removeSpotFrom(spot, null);
					}
				} finally {
					model.endUpdate();
				}

			} else {
				Integer initFrame = displayer.getModel().getFilteredSpots().getFrame(editedSpot);
				model.beginUpdate();
				try {
					model.removeSpotFrom(editedSpot, initFrame);
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
				// Create and drop a new spot
				double radius;
				if (null != previousRadius) {
					radius = previousRadius; 
				} else { 
					Map<String, Object> ss = displayer.settings.detectorSettings;
					Object obj = ss.get(DetectorKeys.DEFAULT_RADIUS);
					if (null == obj) {
						radius = FALL_BACK_RADIUS;
					} else {
						if (Double.class.isInstance(obj)) {
							radius = (Double) obj;
						} else {
							radius = FALL_BACK_RADIUS;
						}
					}
				}

				Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
				SwingUtilities.convertPointFromScreen(mouseLocation, displayer.canvas);
				Spot newSpot = displayer.getCLickLocation(mouseLocation);
				double zpos = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				int frame = displayer.imp.getFrame() - 1;
				newSpot.putFeature(Spot.POSITION_T, frame * displayer.settings.dt);
				newSpot.putFeature(Spot.FRAME, frame);
				newSpot.putFeature(Spot.POSITION_Z, zpos);
				newSpot.putFeature(Spot.RADIUS, radius);

				model.beginUpdate();
				try {
					model.addSpotTo(newSpot, frame);
				} finally {
					model.endUpdate();
				}

				imp.updateAndDraw();
				e.consume();

			} else {

			}
			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D: {

			if (null == editedSpot) {

				Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
				SwingUtilities.convertPointFromScreen(mouseLocation, displayer.canvas);
				int frame = displayer.imp.getFrame() - 1;
				Spot clickLocation = displayer.getCLickLocation(mouseLocation);
				Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
				if (null == target) {
					e.consume(); // Consume it anyway, so that we are not bothered by IJ
					return; 
				}

				model.beginUpdate();
				try {
					model.removeSpotFrom(target, frame);
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

			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen(mouseLocation, displayer.canvas);
			if (null == quickEditedSpot) {
				int frame = displayer.imp.getFrame() - 1;
				Spot clickLocation = displayer.getCLickLocation(mouseLocation);
				quickEditedSpot = model.getFilteredSpots().getSpotAt(clickLocation, frame);
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

			if (null == editedSpot) {

				Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
				SwingUtilities.convertPointFromScreen(mouseLocation, displayer.canvas);
				int frame = displayer.imp.getFrame() - 1;
				Spot clickLocation = displayer.getCLickLocation(mouseLocation);
				Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
				if (null == target) {
					return; // un-consumed event
				}

				int factor;
				if (e.getKeyCode() == KeyEvent.VK_Q) {
					factor = -1;
				} else {
					factor = 1;
				}
				double radius = target.getFeature(Spot.RADIUS);
				if (e.isShiftDown()) 
					radius += factor * displayer.calibration[0] * COARSE_STEP;
				else 
					radius += factor * displayer.calibration[0] * FINE_STEP;
				if (radius <= 0)
					return;

				target.putFeature(Spot.RADIUS, radius);
				model.beginUpdate();
				try {
					model.updateFeatures(target);
				} finally {
					model.endUpdate();
				}

				imp.updateAndDraw();
				e.consume();
			} else {

			}

			break;
		}

		// Copy spots from previous frame
		case KeyEvent.VK_V: {
			if (e.isShiftDown()) {

				int currentFrame = imp.getFrame() - 1;
				if (currentFrame > 0) {

					List<Spot> previousFrameSpots = model.getFilteredSpots().get(currentFrame-1);
					if (previousFrameSpots.isEmpty()) {
						e.consume();
						break;
					}
					ArrayList<Spot> copiedSpots = new ArrayList<Spot>(previousFrameSpots.size());
					HashSet<String> featuresKey = new HashSet<String>(previousFrameSpots.get(0).getFeatures().keySet());
					featuresKey.remove(Spot.POSITION_T); // Deal with time separately
					double dt = model.getSettings().dt;
					if (dt == 0)
						dt = 1;

					for(Spot spot : previousFrameSpots) {
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
						List<Spot> spotsToRemove = model.getFilteredSpots().get(currentFrame);
						for(Spot spot : new ArrayList<Spot>(spotsToRemove)) {
							model.removeSpotFrom(spot, currentFrame);
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

		case KeyEvent.VK_W: {
			e.consume(); // consume it: we do not want IJ to close the window
			break;
		}

		}

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
			TrackMateModel model = displayer.getModel();
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

}
