package fiji.plugin.trackmate.visualization;

import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TMUtils;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.features.FeatureFacade;

public class TrackMateModelManager implements SpotCollectionEditListener {

	private TrackMateModelInterface model;

	/*
	 * CONSTRUCTOR
	 */
	
	public TrackMateModelManager(TrackMateModelInterface model) {
		this.model = model;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public void collectionChanged(SpotCollectionEditEvent event) {
		switch (event.getFlag()) {
		
		case SpotCollectionEditEvent.SPOT_CREATED: {
			addSpotsTo(event.getSpots(), event.getToFrame());
			updateFeatures(event.getSpots());
			break;
		}
		
		case SpotCollectionEditEvent.SPOT_DELETED: {
			deleteSpotsFrom(event.getSpots(), event.getFromFrame());
			break;
		}
		
		case SpotCollectionEditEvent.SPOT_FRAME_CHANGED: {
			deleteSpotsFrom(event.getSpots(), event.getFromFrame());
			addSpotsTo(event.getSpots(), event.getToFrame());
			updateFeatures(event.getSpots());
			break;
		}
		
		case SpotCollectionEditEvent.SPOT_MODIFIED: {
			updateFeatures(event.getSpots());
		}
		
		}
		
	}	

	
	public void addSpotsTo(Spot[] spots, Integer toFrame) {
		SpotCollection sc = model.getSpots();
		if (null != sc) 
			for (Spot spot : spots) 
				sc.add(spot, toFrame);
		SpotCollection ssc = model.getSelectedSpots();
		if (null != ssc) 
			for (Spot spot : spots) 
				ssc.add(spot, toFrame);
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = model.getTrackGraph();
		if (null != graph)
			for (Spot spot : spots) 
				graph.addVertex(spot);
	}
	
	public void deleteSpotsFrom(Spot[] spots, Integer fromFrame) {
		SpotCollection sc = model.getSpots();
		if (null != sc) 
			for (Spot spot : spots) 
				sc.remove(spot, fromFrame);
		SpotCollection ssc = model.getSelectedSpots();
		if (null != ssc) 
			for (Spot spot : spots) 
				ssc.remove(spot, fromFrame);
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = model.getTrackGraph();
		if (null != graph)
			for (Spot spot : spots) 
				graph.removeVertex(spot);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateFeatures(Spot[] spots) {
		SpotCollection sc = model.getSpots();
		if (null == sc)
			return;
		
		// Find common frames
		SpotCollection toCompute = new SpotCollection();
		for (Spot spot : spots) {
			Integer frame = sc.getFrame(spot);
			if (null == frame)
				continue;
		}
		
		// Calculate features
		final Settings settings = model.getSettings();
		final float[] calibration = new float[] { settings.dx, settings.dy, settings.dz };
		FeatureFacade<? extends RealType> featureCalculator;
		for (Integer key : toCompute.keySet()) {
			List<Spot> spotsToCompute = toCompute.get(key);
			Image<? extends RealType> img = TMUtils.getSingleFrameAsImage(settings.imp, key, settings); // will be cropped according to settings
			featureCalculator = new FeatureFacade(img, calibration);
			featureCalculator.processAllFeatures(spotsToCompute);
		}
	}

}
