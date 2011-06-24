package fiji.plugin.trackmate.visualization.hyperstack;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.tool.AbstractTool;


public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
	
	private static final boolean DEBUG = false;
	private static final float COARSE_STEP = 2;
	private static final float FINE_STEP = 0.2f;
	private static final String TOOL_NAME = "Spot edit tool";
	private static final String TOOL_ICON = "C444D01C777D11C999D21C000D31C777L4151C222D61"
		+ "CcccD71C222D81C331D91Ceb5Da1Cd95Lb1c1Cda3Dd1Ca82De1C000Df1"
		+ "CbbbD02C000D32CdddD42CcccD62C777D82C100D92Ca85Da2CfedLb2c2Cd94Dd2C641De2C111Df2"
		+ "C000D33CdddD43C761D83C664D93C544Da3CfedLb3c3CdcaDd3C863De3C111Df3"
		+ "C000D34CdddD44Cec3D74C776D84Cdc9D94C000Da4Cdb9Db4CfdaDc4C776Dd4Cc95De4C111Df4"
		+ "C000D35CdddD45Cec3D65CffcD75C875D85Cfe7D95C542La5b5Cda7Dc5C653Dd5C111Df5"
		+ "C000D36CdddD46Cec3D56CffcD66CffbD76C773D86Cfd4D96Ccb7Da6C000Db6C642Dc6CeeeDd6C111Df6"
		+ "C000D37Cb92D47CffcD57CffbD67Cfe6D77C541D87Cff9D97Ceb6Da7C321Db7C555Dc7C111Df7"
		+ "C999D28C000D38C665D48CeeaD58Cfe6D68Ca93D78C110D88C974D98Ce94Da8CaaaDb8CcccDc8CaaaDe8C000Df8"
		+ "Cc92D29CfecD39CffbD49Cfe6D59Cfd4D69Cff9D79Ceb6D89Ce94D99"
		+ "Ca62D0aCc92D1aCedbD2aCdb6D3aCfe6D4aCfd4D5aCff9D6aCeb6D7aCe94D8a"
		+ "C972D0bCfedL1b2bCb83D3bCca3D4bCec7D5bCda5D6bCc83D7b"
		+ "C972D0cCfedD1cCfb6D2cCfa4D3cCc61D4cCb61D5cCb73D6c"
		+ "C641D0dCda6D1dCfdaD2dCfdbD3dCc95D4dCb73D5d"
		+ "C641L0e1eCa72D2eCb73D3eCc94D4e";
	

	private static SpotEditTool instance;
	private HashMap<ImagePlus, Spot> editedSpots = new HashMap<ImagePlus, Spot>();
	private HashMap<ImagePlus, HyperStackDisplayer> displayers = new HashMap<ImagePlus, HyperStackDisplayer>();
	
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
		}
		
		if (null == displayer)
			return;

		final Spot clickLocation = displayer.getCLickLocation(e);
		final int frame = displayer.imp.getFrame() - 1;
		final TrackMateModel model = displayer.getModel();
		Spot target = model.getFilteredSpots().getClosestSpot(clickLocation, frame);
		Spot editedSpot = editedSpots.get(imp);
		updateStatusBar(target, imp.getCalibration().getUnits());

		
		// Check desired behavior
		switch (e.getClickCount()) {
		
		case 1: {
			// Change selection
			// only if we are not currently editing and if target is non null
			if (null != editedSpot || target == null)
				return;
			final int addToSelectionMask = MouseEvent.SHIFT_DOWN_MASK;
			if ((e.getModifiersEx() & addToSelectionMask) == addToSelectionMask) { 
				if (model.getSpotSelection().contains(target)) {
					model.removeSpotFromSelection(target);
				} else {
					model.addSpotToSelection(target);
				}
			} else {
				model.clearSpotSelection();
				model.addSpotToSelection(target);
			}
			break;
		}
		
		case 2: {
			// Edit spot
			if (DEBUG)
				System.out.println("[SpotEditTool] Got "+editedSpot+" as editing spot for this imp.");
			
			// Empty current selection
			model.clearSelection();
						
			if (null == editedSpot) {
				// No spot is currently edited, we pick one to edit
				float radius;
				if (null != target && null != target.getFeature(SpotFeature.RADIUS))
					radius = target.getFeature(SpotFeature.RADIUS);
				else 
					radius = displayer.settings.segmenterSettings.expectedRadius;
				if (null == target || target.squareDistanceTo(clickLocation) > radius*radius) {
					// Create a new spot if not inside one
					target = clickLocation;
					target.putFeature(SpotFeature.RADIUS, radius);
				}
				editedSpot = target;
				
			} else {
				// We leave editing mode
				// A hack: we update the current z and t of the edited spot to the current one, 
				// because it is not updated otherwise: there is no way to listen to slice change
				final float zslice = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(SpotFeature.POSITION_Z, zslice);
				Integer initFrame = displayer.getModel().getFilteredSpots().getFrame(editedSpot);
				// Move it in Z
				final float z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(SpotFeature.POSITION_Z, z);
				editedSpot.putFeature(SpotFeature.POSITION_T, frame * displayer.settings.dt);
				if (initFrame == null) {
					// Means that the spot was created 
					model.addSpotTo(editedSpot, frame, true);
				} else if (initFrame != frame) {
					// Move it to the new frame
					model.moveSpotsFrom(editedSpot, initFrame, frame, true);
				} else {
					// The spots pre-existed and was not moved across frames
					model.updateFeatures(editedSpot, true);
				}

				// Forget edited spot
				editedSpot = null;
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
		final double ix = displayer.canvas.offScreenXD(e.getX());
		final double iy =  displayer.canvas.offScreenYD(e.getY());
		final float x = (float) (ix * displayer.calibration[0]);
		final float y = (float) (iy * displayer.calibration[1]);
		final float z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
		editedSpot.putFeature(SpotFeature.POSITION_X, x);
		editedSpot.putFeature(SpotFeature.POSITION_Y, y);
		editedSpot.putFeature(SpotFeature.POSITION_Z, z);
		displayer.imp.updateAndDraw();
		updateStatusBar(editedSpot, imp.getCalibration().getUnits());	
	}

	@Override
	public void mouseMoved(MouseEvent e) {	}

	
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
		float radius = editedSpot.getFeature(SpotFeature.RADIUS);
		if (e.isShiftDown()) 
			radius += e.getWheelRotation() * displayer.calibration[0] * COARSE_STEP;
		else 
			radius += e.getWheelRotation() * displayer.calibration[0] * FINE_STEP;
 		editedSpot.putFeature(SpotFeature.RADIUS, radius);
		displayer.imp.updateAndDraw();
		e.consume();
		updateStatusBar(editedSpot, imp.getCalibration().getUnits());
	}

	/*
	 * KEYLISTENER
	 */
	
	@Override
	public void keyTyped(KeyEvent e) {	}

	@Override
	public void keyPressed(KeyEvent e) { 
		final ImagePlus imp = getImagePlus(e);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null == editedSpot)
			return;
		
		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			Integer initFrame = displayer.getModel().getFilteredSpots().getFrame(editedSpot);
			TrackMateModel model = displayer.getModel();
			model.removeSpotFrom(editedSpot, initFrame, true);
			editedSpot = null;
			editedSpots.put(imp, null);
			imp.updateAndDraw();
			e.consume();
		}	
	}

	@Override
	public void keyReleased(KeyEvent e) { }	
	

	/*
	 * PRIVATE METHODS
	 */
	
	private void updateStatusBar(final Spot spot, final String units) {
		if (null == spot)
			return;
		String statusString = "";
		if (null == spot.getName() || spot.getName().equals("")) { 
			statusString = String.format("Spot ID%d, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", 
					spot.ID(), spot.getFeature(SpotFeature.POSITION_X), spot.getFeature(SpotFeature.POSITION_Y), 
					spot.getFeature(SpotFeature.POSITION_Z), spot.getFeature(SpotFeature.RADIUS), units );
		} else {
			statusString = String.format("Spot %s, x = %.1f, y = %.1f, z = %.1f %s", 
					spot.getName(), spot.getFeature(SpotFeature.POSITION_X), spot.getFeature(SpotFeature.POSITION_Y), 
					spot.getFeature(SpotFeature.POSITION_Z), spot.getFeature(SpotFeature.RADIUS), units );
		}
		IJ.showStatus(statusString);
	}
	
}
