package fiji.plugin.trackmate.visualization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureFacade;
import fiji.plugin.trackmate.util.TMUtils;

public class TrackMateModelManager implements TMModelEditListener {

	private TrackMateModel model;
	
	private static final boolean DEBUG = true;

	/*
	 * CONSTRUCTOR
	 */
	
	public TrackMateModelManager(TrackMateModel model) {
		this.model = model;
	}
	
	/*
	 * LISTENER METHODS
	 */

	@Override
	public void modelChanged(TMModelEditEvent event) {
		
		ArrayList<Spot> spotsToAdd = new ArrayList<Spot>();
		ArrayList<Integer> addToFrame = new ArrayList<Integer>();
		ArrayList<Spot> spotsToRemove = new ArrayList<Spot>();
		ArrayList<Integer> removeFromFrame = new ArrayList<Integer>();
		ArrayList<Spot> spotsToMove = new ArrayList<Spot>();
		ArrayList<Integer> moveFrom = new ArrayList<Integer>();
		ArrayList<Integer> moveTo = new ArrayList<Integer>();
		ArrayList<Spot> spotsToModify = new ArrayList<Spot>();
		
		for(Spot spot : event.getSpots()) {
			
			switch (event.getSpotFlag(spot)) {

			case TMModelEditEvent.SPOT_ADDED: 
				spotsToAdd.add(spot);
				addToFrame.add(event.getToFrame(spot));
				break;
			
			case TMModelEditEvent.SPOT_DELETED:
				spotsToRemove.add(spot);
				removeFromFrame.add(event.getFromFrame(spot));
				break;
			
			case TMModelEditEvent.SPOT_FRAME_CHANGED: 
				spotsToMove.add(spot);
				moveFrom.add(event.getFromFrame(spot));
				moveTo.add(event.getToFrame(spot));
				break;

			case TMModelEditEvent.SPOT_MODIFIED: 
				spotsToModify.add(spot);
				break;	
			}
		}
		
		addSpotsTo(spotsToAdd, addToFrame);
		deleteSpotsFrom(spotsToRemove, removeFromFrame);
		moveSpotsFrom(spotsToMove, moveFrom, moveTo);
	
		ArrayList<Spot> toUpdate = new ArrayList<Spot>(spotsToAdd.size() + spotsToMove.size() + spotsToModify.size());
		toUpdate.addAll(spotsToAdd);
		toUpdate.addAll(spotsToMove);
		toUpdate.addAll(spotsToModify);
		updateFeatures(toUpdate);
	}

	/*
	 * UPDATING METHODS
	 */
	
	public void moveSpotsFrom(List<Spot> spots, List<Integer> fromFrame, List<Integer> toFrame) {
		SpotCollection sc = model.getSpots();
		if (null != sc) 
			for (int i = 0; i < spots.size(); i++) {
				sc.add(spots.get(i), toFrame.get(i));
				sc.remove(spots.get(i), fromFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModelManager] Moving "+spots.get(i)+" from frame "+fromFrame.get(i)+" to frame "+toFrame.get(i));
			}
		SpotCollection ssc = model.getSelectedSpots();
		if (null != ssc) 
			for (int i = 0; i < spots.size(); i++) {
				ssc.add(spots.get(i), toFrame.get(i));
				ssc.remove(spots.get(i), fromFrame.get(i));
			}
	}

	public void addSpotsTo(List<Spot> spots, List<Integer> toFrame) {
		SpotCollection sc = model.getSpots();
		if (null != sc) 
			for (int i = 0; i < spots.size(); i++) {
				sc.add(spots.get(i), toFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModelManager] Adding "+spots.get(i)+" spots to frame "+ toFrame.get(i));
			}
		SpotCollection ssc = model.getSelectedSpots();
		if (null != ssc) 
			for (int i = 0; i < spots.size(); i++) 
				ssc.add(spots.get(i), toFrame.get(i));
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = model.getTrackGraph();
		if (null != graph)
			for (Spot spot : spots) 
				graph.addVertex(spot);
	}

	public void deleteSpotsFrom(List<Spot> spots, List<Integer> fromFrame) {
		SpotCollection sc = model.getSpots();
		if (null != sc) 
			for (int i = 0; i < spots.size(); i++) {
				sc.remove(spots.get(i), fromFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModelManager] Removing "+spots.get(i)+" spots to frame "+ fromFrame.get(i));
			}
		SpotCollection ssc = model.getSelectedSpots();
		if (null != ssc) 
			for (int i = 0; i < spots.size(); i++) 
				ssc.remove(spots.get(i), fromFrame.get(i));
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = model.getTrackGraph();
		if (null != graph)
			for (Spot spot : spots) 
				graph.removeVertex(spot);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateFeatures(Collection<Spot> spots) {
		if (DEBUG)
			System.out.println("TrackMateModelManager: Updating the features of "+spots.size()+" spots");
		SpotCollection sc = model.getSpots();
		if (null == sc)
			return;
		
		// Find common frames
		SpotCollection toCompute = new SpotCollection();
		for (Spot spot : spots) {
			Integer frame = sc.getFrame(spot);
			if (null == frame)
				continue;
			toCompute.add(spot, frame);
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
