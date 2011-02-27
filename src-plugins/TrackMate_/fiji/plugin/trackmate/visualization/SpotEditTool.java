package fiji.plugin.trackmate.visualization;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.tool.AbstractTool;


public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener {
	
	Spot editedSpot;
	private HyperStackDisplayer displayer;

	/*
	 * CONSTRUCTOR
	 */
	
	public SpotEditTool(final HyperStackDisplayer displayer) {
		this.displayer = displayer;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public void mouseClicked(MouseEvent e) {
		final Spot clickLocation = displayer.getCLickLocation(e);
		final int frame = displayer.imp.getFrame() - 1;		
		Spot target = displayer.spotsToShow.getClosestSpot(clickLocation, frame);
		
		// Check desired behavior
		switch (e.getClickCount()) {
		
		case 1: {
			// Change selection
			// only if we are nut currently editing
			if (null != editedSpot)
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
				final float radius = target.getFeature(Feature.RADIUS);
				if (target.squareDistanceTo(clickLocation) > radius*radius) {
					// Create a new spot if not inside one
					target = clickLocation;
					target.putFeature(Feature.RADIUS, displayer.settings.segmenterSettings.expectedRadius);
					// Add it to collections
					displayer.spotsToShow.add(target, frame);
					displayer.spots.add(target, frame);
					if (null != displayer.trackGraph)
						displayer.trackGraph.addVertex(target);
				}
				editedSpot = target;
				
			} else {
				// We leave editing mode
				// A hack: we update the current z and t of the edited spot to the current one, 
				// because it is not updated otherwise: there is no way to listen to slice change
				final float zslice = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(Feature.POSITION_Z, zslice);
				Integer initFrame = displayer.spotsToShow.getFrame(editedSpot);
				if (initFrame != frame) {
					// Move it to the new frame
					displayer.spotsToShow.remove(editedSpot, initFrame);
					displayer.spots.remove(editedSpot, initFrame);
					displayer.spotsToShow.add(editedSpot, frame);
					displayer.spots.add(editedSpot, frame);
				}
				// Move it in Z
				final float z = (displayer.imp.getSlice()-1) * displayer.calibration[2];
				editedSpot.putFeature(Feature.POSITION_Z, z);
				editedSpot.putFeature(Feature.POSITION_T, frame * displayer.settings.dt);

				// Forget edited spot
				editedSpot = null;
			}
			displayer.spotOverlay.setEditedSpot(editedSpot);
			break;
		}
		}
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


	
	

}
