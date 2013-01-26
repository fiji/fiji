package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.StackWindow;

import java.awt.Point;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class HyperStackDisplayer extends AbstractTrackMateModelView  {

	private static final boolean DEBUG = false;
	public static final String NAME = "HyperStack Displayer";
	public static final String INFO_TEXT = "<html>" +
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
	protected ImagePlus imp;
	OverlayedImageCanvas canvas;
	double[] calibration;
	Settings settings;
	private StackWindow window;
	SpotOverlay spotOverlay;
	private TrackOverlay trackOverlay;

	private SpotEditTool editTool;

	/*
	 * CONSTRUCTORS
	 */

	public HyperStackDisplayer(TrackMateModel model) {	
		super(model);
		this.settings = model.getSettings();
	}

	/*
	 * DEFAULT METHODS
	 */

	final Spot getCLickLocation(final Point point) {
		final double ix = canvas.offScreenXD(point.x) - 0.5d;
		final double iy =  canvas.offScreenYD(point.y) - 0.5d;
		final double x = ix * calibration[0];
		final double y = iy * calibration[1];
		final double z = (imp.getSlice()-1) * calibration[2];
		return new Spot(new double[] {x, y, z});
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
		TrackOverlay to = new TrackOverlay(model, imp, displaySettings);
		TrackColorGenerator colorGenerator = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);
		to.setTrackColorGenerator(colorGenerator);
		return to;
	}


	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[HyperStackDisplayer] Received model changed event ID: " 
					 + event.getEventID() +" from "+event.getSource());
		boolean redoOverlay = false;

		switch (event.getEventID()) {

		case ModelChangeEvent.MODEL_MODIFIED:
			// Rebuild track overlay only if edges were added or removed, or if at least one spot was removed. 
			final Set<DefaultWeightedEdge> edges = event.getEdges();
			if (edges != null && edges.size() > 0) {
				redoOverlay = true;				
			}
			break;

		case ModelChangeEvent.SPOTS_COMPUTED:
			spotOverlay.computeSpotColors();
			redoOverlay = true;
			break;

		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case ModelChangeEvent.TRACKS_COMPUTED:
			redoOverlay = true;
			break;
		}

		if (redoOverlay)
			refresh();
	}

	@Override
	public void selectionChanged(SelectionChangeEvent event) {
		// Highlight selection
		trackOverlay.setHighlight(model.getSelectionModel().getEdgeSelection());
		spotOverlay.setSpotSelection(model.getSelectionModel().getSpotSelection());
		// Center on last spot
		super.selectionChanged(event);
		// Redraw
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
		long z = Math.round(spot.getFeature(Spot.POSITION_Z) / calibration[2] ) + 1;
		imp.setPosition(1, (int) z, frame+1);
	}

	@Override
	public void render() {
		this.imp = settings.imp;
		if (null == imp) {
			this.imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes*settings.nslices, NewImage.FILL_BLACK);
			this.imp.setDimensions(1, settings.nslices, settings.nframes);
		}
		this.calibration = TMUtils.getSpatialCalibration(imp);

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
		if (null != imp) {
			imp.updateAndDraw();
		}
	}

	@Override
	public void clear() {
		if (canvas != null)
			canvas.clearOverlay();
	}	

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
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
		if (key == TrackMateModelView.KEY_TRACK_COLORING) {
			// unregister the old one
			TrackColorGenerator oldColorGenerator = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);
			oldColorGenerator.terminate();
			// pass the new one to the track overlay - we ignore its spot coloring and keep the spot coloring
			TrackColorGenerator colorGenerator = (TrackColorGenerator) value;
			trackOverlay.setTrackColorGenerator(colorGenerator);
		}
	}
}
