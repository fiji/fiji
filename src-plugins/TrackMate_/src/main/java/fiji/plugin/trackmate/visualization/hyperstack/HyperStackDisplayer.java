package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.StackWindow;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class HyperStackDisplayer extends AbstractTrackMateModelView  {

	private static final boolean DEBUG = false;
	protected ImagePlus imp;
	OverlayedImageCanvas canvas;
	float[] calibration;
	Settings settings;
	private StackWindow window;
	SpotOverlay spotOverlay;
	private TrackOverlay trackOverlay;

	private SpotEditTool editTool;

	/*
	 * CONSTRUCTORS
	 */

	public HyperStackDisplayer() {	}

	/*
	 * DEFAULT METHODS
	 */

	final Spot getCLickLocation(final Point point) {
		final double ix = canvas.offScreenXD(point.x) + 0.5f;
		final double iy =  canvas.offScreenYD(point.y) + 0.5f;
		final float x = (float) (ix * calibration[0]);
		final float y = (float) (iy * calibration[1]);
		final float z = (imp.getSlice()-1) * calibration[2];
		return new SpotImp(new float[] {x, y, z});
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected SpotOverlay createSpotOverlay() {
		return new SpotOverlay(model, imp, displaySettings);
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected TrackOverlay createTrackOverlay() {
		return new TrackOverlay(model, imp, displaySettings);
	}

	@Override
	public void setModel(TrackMateModel model) {
		super.setModel(model);
		this.settings = model.getSettings();
		this.imp = settings.imp;
		this.calibration = settings.getCalibration();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged(TrackMateModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[HyperStackDisplayer] Received model changed event ID: "+event.getEventID()+" from "+event.getSource());
		boolean redoOverlay = false;

		switch (event.getEventID()) {

		case TrackMateModelChangeEvent.MODEL_MODIFIED:
			// Rebuild track overlay only if edges were added or removed, or if at least one spot was removed. 
			final List<DefaultWeightedEdge> edges = event.getEdges();
			if (edges != null && edges.size() > 0) {
				trackOverlay.computeTrackColors();
				redoOverlay = true;				
			} else {
				final List<Spot> spots = event.getSpots();
				if ( spots != null && spots.size() > 0) {
					for (Spot spot : event.getSpots()) {
						if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_REMOVED) {
							trackOverlay.computeTrackColors();
							redoOverlay = true;
							break;
						}
					}
				}
			}
			break;

		case TrackMateModelChangeEvent.SPOTS_COMPUTED:
			spotOverlay.computeSpotColors();
			redoOverlay = true;
			break;

		case TrackMateModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case TrackMateModelChangeEvent.TRACKS_COMPUTED:
			trackOverlay.computeTrackColors();
			redoOverlay = true;
			break;
		}

		if (redoOverlay)
			refresh();
	}

	@Override
	public void highlightEdges(Collection<DefaultWeightedEdge> edges) {
		trackOverlay.setHighlight(edges);
	}

	@Override
	public void highlightSpots(Collection<Spot> spots) {
		spotOverlay.setSpotSelection(spots);
		imp.updateAndDraw();				
	}

	@Override
	public void centerViewOn(Spot spot) {
		int frame = - 1;
		for(int i : model.getFilteredSpots().keySet()) {
			List<Spot> spotThisFrame = model.getFilteredSpots().get(i);
			if (spotThisFrame.contains(spot)) {
				frame = i;
				break;
			}
		}
		if (frame == -1)
			return;
		int z = Math.round(spot.getFeature(Spot.POSITION_Z) / calibration[2] ) + 1;
		imp.setPosition(1, z, frame+1);
	}

	@Override
	public void render() {
		if (null == imp) {
			this.imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes*settings.nslices, NewImage.FILL_BLACK);
			this.imp.setDimensions(1, settings.nslices, settings.nframes);
		}
		clear();
		imp.setOpenAsHyperStack(true);
		//
		spotOverlay = createSpotOverlay();
		//
		trackOverlay = createTrackOverlay(); 
		//
		canvas = new OverlayedImageCanvas(imp);
		window = new StackWindow(imp, canvas);
		window.setVisible(true);
		canvas.addOverlay(spotOverlay);
		canvas.addOverlay(trackOverlay);
		imp.updateAndDraw();
		registerEditTool();
	}


	@Override
	public void refresh() { 
		imp.updateAndDraw();
	}

	@Override
	public void clear() {
		if (canvas != null)
			canvas.clearOverlay();
	}	

	@Override
	public String getInfoText() {
		return "<html>" +
				"This displayer overlays the spots and tracks on the current <br>" +
				"ImageJ hyperstack window. <br>" +
				"<p> " +
				"This displayer allows manual editing of spots, thanks to the spot <br> " +
				"edit tool that appear in ImageJ toolbar." +
				"<p>" +
				"Double-clicking in a spot toggles the editing mode: The spot can <br> " +
				"be moved around in a XY plane by mouse dragging. To move it in Z <br>" +
				"or in time, simply change the current plane and time-point by <br>" +
				"using the hyperstack sliders. To change its radius, hold the <br>" +
				"<tt>alt</tt> key down and rotate the mouse-wheel. Holding the <br>" +
				"<tt>shift</tt> key on top changes it faster. " +
				"<p>" +
				"Alternatively, keyboard can be used to edit spots: " +
				"<ul>" +
				"	<li><b>A</b> creates a new spot under the mouse" +
				"	<li><b>D</b> deletes the spot under the mouse" +
				"	<li><b>Q</b> and <b>E</b> decreases and increases the radius of the spot " +
				"under the mouse (shift to go faster)" +
				"	<li><b>Space</b> + mouse drag moves the spot under the mouse" +
				"</ul>" +
				"</html>";
	}
	
	@Override
	public String toString() {
		return "HyperStack Displayer";
	}


	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool() {
		editTool = SpotEditTool.getInstance();
		if (!SpotEditTool.isLaunched())
			editTool.run("");
		else {
			editTool.imageOpened(imp);
		}
		editTool.register(imp, this);
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		super.setDisplaySettings(key, value);
		// If we modified the feature coloring, then we recompute NOW the colors.
		if (key == TrackMateModelView.KEY_SPOT_COLOR_FEATURE) {
			spotOverlay.computeSpotColors();
		}
		if (key == TrackMateModelView.KEY_TRACK_COLOR_FEATURE) {
			trackOverlay.computeTrackColors();
		}
	}
}
