package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;

import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

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
	protected final ImagePlus imp;
	protected SpotOverlay spotOverlay;
	protected TrackOverlay trackOverlay;

	private SpotEditTool editTool;
	private Roi initialROI;

	/*
	 * CONSTRUCTORS
	 */

	public HyperStackDisplayer(final TrackMateModel model, final SelectionModel selectionModel,  final ImagePlus imp) {	
		super(model, selectionModel);
		this.imp = imp;
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return the spot overlay
	 */
	protected SpotOverlay createSpotOverlay() {
		return new SpotOverlay(model, imp, displaySettings);
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return the track overlay
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
			if (spotOverlay != null) spotOverlay.computeSpotColors();
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
		trackOverlay.setHighlight(selectionModel.getEdgeSelection());
		spotOverlay.setSpotSelection(selectionModel.getSpotSelection());
		// Center on last spot
		super.selectionChanged(event);
		// Redraw
		imp.updateAndDraw();				
	}

	@Override
	public void centerViewOn(Spot spot) {
		int frame = spot.getFeature(Spot.FRAME).intValue();
		double dz = imp.getCalibration().pixelDepth;
		long z = Math.round(spot.getFeature(Spot.POSITION_Z) / dz  ) + 1;
		imp.setPosition(1, (int) z, frame+1);
	}

	@Override
	public void render() {
		initialROI = imp.getRoi();
		if (initialROI != null) {
			imp.killRoi();
		}

		clear();
		imp.setOpenAsHyperStack(true);
		//
		spotOverlay = createSpotOverlay();
		//
		trackOverlay = createTrackOverlay(); 
		//

		addOverlay(spotOverlay);
		addOverlay(trackOverlay);
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
		Overlay overlay = imp.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}
		overlay.clear();
		if (initialROI != null) {
			imp.getOverlay().add(initialROI);
		}
	}	

	public void addOverlay(Roi overlay) {
		imp.getOverlay().add(overlay);
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

	public SelectionModel getSelectionModel() {
		return selectionModel;
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
