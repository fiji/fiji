package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij.gui.Toolbar;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.tool.AbstractTool;


public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
	
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
	private ArrayList<SpotCollectionEditListener> spotCollectionEditListeners = new ArrayList<SpotCollectionEditListener>();

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
		if (null == instance)
			instance = new SpotEditTool();
		return instance;
	}

	/**
	 * Return true if the tool is currently present in ImageJ toolbar.
	 */
	public static boolean isLaunched() {
		Toolbar toolbar = Toolbar.getInstance();
		if (toolbar.getToolId(TOOL_NAME) >= 0) 
			return true;
		return false;
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Add a listener to this displayer that will be notified when the spot collection is being changed 
	 * by this tool.
	 */
	public void addSpotCollectionEditListener(SpotCollectionEditListener listener) {
		this.spotCollectionEditListeners.add(listener);
	}
	
	/**
	 * Remove a listener from the list of the spot collection edit listeners list. 
	 * @param listener  the listener to remove
	 * @return  true if the listener was found in the list maintained by 
	 * this displayer and successfully removed.
	 */
	public boolean removeSpotCollectionEditListener(SpotCollectionEditListener listener) {
		return spotCollectionEditListeners.remove(listener);
	}

	
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
		displayers.put(imp, displayer);
	}
	
	
	/*
	 * MOUSE AND MOUSE MOTION
	 */
	
	
	@Override
	public void mouseClicked(MouseEvent e) {
		final ImagePlus imp = getImagePlus(e);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		
		final Spot clickLocation = displayer.getCLickLocation(e);
		final int frame = displayer.imp.getFrame() - 1;		
		Spot target = displayer.spotsToShow.getClosestSpot(clickLocation, frame);
		Spot editedSpot = editedSpots.get(imp);
		
		// Check desired behavior
		switch (e.getClickCount()) {
		
		case 1: {
			// Change selection
			// only if we are not currently editing and if target is non null
			if (null != editedSpot || target == null)
				return;
			final int addToSelectionMask = MouseEvent.SHIFT_DOWN_MASK;
			final int flag;
			if ((e.getModifiersEx() & addToSelectionMask) == addToSelectionMask) 
				flag = SpotDisplayer.MODIFY_SELECTION_FLAG;
			else 
				flag = SpotDisplayer.REPLACE_SELECTION_FLAG;
			displayer.spotSelectionChanged(target, frame, flag);
			break;
		}
		
		case 2: {
			// Edit spot
			
			// Empty current selection
			displayer.spotSelectionChanged(null, frame, SpotDisplayer.REPLACE_SELECTION_FLAG);
			
			if (null == editedSpot) {
				// No spot is currently edited, we pick one to edit
				float radius;
				if (null != target)
					radius = target.getFeature(Feature.RADIUS);
				else 
					radius = displayer.settings.segmenterSettings.expectedRadius;
				if (null == target || target.squareDistanceTo(clickLocation) > radius*radius) {
					// Create a new spot if not inside one
					target = clickLocation;
					target.putFeature(Feature.RADIUS, radius);
				}
				editedSpot = target;
				
			} else {
				// We leave editing mode
				// A hack: we update the current z and t of the edited spot to the current one, 
				// because it is not updated otherwise: there is no way to listen to slice change
				final float zslice = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(Feature.POSITION_Z, zslice);
				Integer initFrame = displayer.spotsToShow.getFrame(editedSpot);
				// Move it in Z
				final float z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(Feature.POSITION_Z, z);
				editedSpot.putFeature(Feature.POSITION_T, frame * displayer.settings.dt);
				if (initFrame == null) {
					// Means that the spot was created 
					fireSpotCollectionEdit(new Spot[] { editedSpot }, SpotCollectionEditEvent.SPOT_CREATED, null, frame);
				} else if (initFrame != frame) {
					// Move it to the new frame
					fireSpotCollectionEdit(new Spot[] { editedSpot }, SpotCollectionEditEvent.SPOT_FRAME_CHANGED, initFrame, frame);
				} else {
					// The spots pre-existed and was not moved accross frames
					fireSpotCollectionEdit(new Spot[] { editedSpot }, SpotCollectionEditEvent.SPOT_MODIFIED, null, null);
				}

				// Forget edited spot
				editedSpot = null;
			}
			displayer.spotOverlay.setEditedSpot(editedSpot);
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
		final int ix = displayer.canvas.offScreenX(e.getX());
		final int iy =  displayer.canvas.offScreenX(e.getY());
		final float x = ix * displayer.calibration[0];
		final float y = iy * displayer.calibration[1];
		final float z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
		editedSpot.putFeature(Feature.POSITION_X, x);
		editedSpot.putFeature(Feature.POSITION_Y, y);
		editedSpot.putFeature(Feature.POSITION_Z, z);
		displayer.imp.updateAndDraw();
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
		float radius = editedSpot.getFeature(Feature.RADIUS);
		if (e.isShiftDown()) 
			radius += e.getWheelRotation() * displayer.calibration[0] * COARSE_STEP;
		else 
			radius += e.getWheelRotation() * displayer.calibration[0] * FINE_STEP;
 		editedSpot.putFeature(Feature.RADIUS, radius);
		displayer.imp.updateAndDraw();
		e.consume();
	}

	/*
	 * KEYLISTENER
	 */
	
	@Override
	public void keyTyped(KeyEvent e) {
		final ImagePlus imp = getImagePlus(e);
		final HyperStackDisplayer displayer = displayers.get(imp);
		if (null == displayer)
			return;
		Spot editedSpot = editedSpots.get(imp);
		if (null == editedSpot)
			return;
		
		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			Integer initFrame = displayer.spotsToShow.getFrame(editedSpot);
			if (initFrame != null) {
				displayer.spotsToShow.remove(editedSpot, initFrame);
				displayer.spots.remove(editedSpot, initFrame);
			}
			editedSpot = null;
			editedSpots.put(imp, null);
			imp.updateAndDraw();
		} else {
			System.out.println("Got the key: "+e.paramString());// DEBUG
		}
	}

	@Override
	public void keyPressed(KeyEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }	
	

	/*
	 * PRIVATE METHODS
	 */
	
	private void fireSpotCollectionEdit(Spot[] spots, int flag, Integer fromFrame, Integer toFrame) {
		SpotCollectionEditEvent event = new SpotCollectionEditEvent(this, spots, flag, fromFrame, toFrame);
		for (SpotCollectionEditListener listener : spotCollectionEditListeners) {
			listener.collectionChanged(event);
		}
	}
	
}
