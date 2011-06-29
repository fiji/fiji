package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.StackWindow;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class HyperStackDisplayer extends AbstractTrackMateModelView  {

	private static final boolean DEBUG = true;
	ImagePlus imp;
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

	public HyperStackDisplayer(final TrackMateModel model) {
		setModel(model);
		this.settings = model.getSettings();
		this.imp = settings.imp;
		this.calibration = settings.getCalibration();
	}

	/*
	 * DEFAULT METHODS
	 */

	final Spot getCLickLocation(final MouseEvent e) {
		final double ix = canvas.offScreenXD(e.getX());
		final double iy =  canvas.offScreenYD(e.getY());
		final float x = (float) (ix * calibration[0]);
		final float y = (float) (iy * calibration[1]);
		final float z = (imp.getSlice()-1) * calibration[2];
		return new SpotImp(new float[] {x, y, z});
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged(TrackMateModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[HyperStackDisplayer] Received model changed event ID: "+event.getEventID());
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
		int z = Math.round(spot.getFeature(SpotFeature.POSITION_Z) / calibration[2] ) + 1;
		imp.setPosition(1, z, frame+1);
	}

	@Override
	public void render() {
		if (null == imp) {
			this.imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes*settings.nslices, NewImage.FILL_BLACK);
			this.imp.setDimensions(1, settings.nslices, settings.nframes);
		}
		imp.setOpenAsHyperStack(true);
		canvas = new OverlayedImageCanvas(imp);
		window = new StackWindow(imp, canvas);
		window.show();
		//
		spotOverlay = new SpotOverlay(model, imp, displaySettings);
		//
		trackOverlay = new TrackOverlay(model, imp, displaySettings);
		//
		canvas.addOverlay(spotOverlay);
		canvas.addOverlay(trackOverlay);
		imp.updateAndDraw();
		registerEditTool();
		//		 Add a listener to unregister this instance from model listener list
		ImagePlus.addImageListener(new ImageListener() {
			@Override
			public void imageUpdated(ImagePlus imp) {}
			@Override
			public void imageOpened(ImagePlus imp) {}
			@Override
			public void imageClosed(ImagePlus source ) {
				if (imp != source)
					return;
				model.removeTrackMateModelChangeListener(HyperStackDisplayer.this);
				model.removeTrackMateSelectionChangeListener(HyperStackDisplayer.this);
				editTool.imageClosed(imp);
			}
		});
	}


	@Override
	public void refresh() {
		imp.updateAndDraw();
	}

	@Override
	public void clear() {
		canvas.clearOverlay();
	}	


	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool() {
		editTool = SpotEditTool.getInstance();
		if (!SpotEditTool.isLaunched())
			editTool.run("");
		editTool.register(imp, this);
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		super.setDisplaySettings(key, value);
		// If we modified the feature coloring, then we recompute NOW the colors.
		if (key == TrackMateModelView.KEY_SPOT_COLOR_FEATURE) {
			spotOverlay.computeSpotColors();
		}
	}
}
