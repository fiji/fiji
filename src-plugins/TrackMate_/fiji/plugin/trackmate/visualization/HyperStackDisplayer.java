package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.util.gui.OverlayedImageCanvas;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.StackWindow;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class HyperStackDisplayer extends SpotDisplayer implements SpotCollectionEditListener {

	ImagePlus imp;
	OverlayedImageCanvas canvas;
	float[] calibration;
	private Feature currentFeature;
	private float featureMaxValue;
	private float featureMinValue;
	Settings settings;
	private StackWindow window;
	SpotOverlay spotOverlay;
	private TrackOverlay trackOverlay;
	
	/** A mapping of the spots versus the color they must be drawn in. */
	private Map<Spot, Color> spotColor = new HashMap<Spot, Color>();
	private SpotEditTool editTool;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public HyperStackDisplayer(final Settings settings) {
		this.imp = settings.imp;
		this.calibration = new float[] { settings.dx, settings.dy, settings.dz };
		this.settings = settings;
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

	public void collectionChanged(SpotCollectionEditEvent event) {
		event.setSource(this);
		fireSpotCollectionEdit(event);
	}
	
	@Override
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		super.setDisplayTrackMode(mode, displayDepth);
		trackOverlay.setDisplayTrackMode(mode, displayDepth);
		imp.updateAndDraw();
	}
	
	@Override
	public void highlightEdges(Set<DefaultWeightedEdge> edges) {
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
		for(int i : spotsToShow.keySet()) {
			List<Spot> spotThisFrame = spotsToShow.get(i);
			if (spotThisFrame.contains(spot)) {
				frame = i;
				break;
			}
		}
		if (frame == -1)
			return;
		int z = Math.round(spot.getFeature(Feature.POSITION_Z) / calibration[2] ) + 1;
		imp.setPosition(1, z, frame+1);
	}
	
	@Override
	public void setTrackVisible(boolean trackVisible) {
		trackOverlay.setTrackVisisble(trackVisible);
		imp.updateAndDraw();
	}
	
	@Override
	public void setSpotVisible(boolean spotVisible) {
		spotOverlay.setSpotVisible(spotVisible);
		imp.updateAndDraw();
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
		spotOverlay = new SpotOverlay(imp, calibration);
		trackOverlay = new TrackOverlay(imp, calibration);
		canvas.addOverlay(spotOverlay);
		canvas.addOverlay(trackOverlay);
		imp.updateAndDraw();
		registerEditTool();
		editTool.addSpotCollectionEditListener(this);
	}
	
	@Override
	public void setRadiusDisplayRatio(float ratio) {
		super.setRadiusDisplayRatio(ratio);
		spotOverlay.setRadiusRatio(ratio);
		refresh();
	}
	
	@Override
	public void setColorByFeature(Feature feature) {
		currentFeature = feature;
		// Get min & max
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		Float val;
		for (int key : spots.keySet()) {
			for (Spot spot : spots.get(key)) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				if (val > max) max = val;
				if (val < min) min = val;
			}
		}
		featureMinValue = min;
		featureMaxValue = max;
		prepareSpotOverlay();
		refresh();
	}
		
	@Override
	public void refresh() {
		imp.updateAndDraw();
	}
		
	@Override
	public void setTrackGraph(SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph) {
		super.setTrackGraph(trackGraph);
		trackOverlay.setTrackGraph(trackGraph, spotsToShow);
		trackOverlay.setTrackColor(trackColors);
		imp.updateAndDraw();
		
	}
	
	@Override
	public void setSpotsToShow(SpotCollection spotsToShow) {
		super.setSpotsToShow(spotsToShow);
		prepareSpotOverlay();
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
		
	private void prepareSpotOverlay() {
		if (null == spotsToShow)
			return;
		Float val;
		for(Spot spot : spotsToShow) {
			val = spot.getFeature(currentFeature);
			if (null == currentFeature || null == val)
				spotColor.put(spot, color);
			else
				spotColor.put(spot, colorMap.getPaint((val-featureMinValue)/(featureMaxValue-featureMinValue)) );
		}
		spotOverlay.setTarget(spotsToShow);
		spotOverlay.setTargettColor(spotColor);
	}
}
