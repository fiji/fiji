package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class TrackCollection implements Iterable<Set<DefaultWeightedEdge>>, List<Set<DefaultWeightedEdge>> {

	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private List<Set<DefaultWeightedEdge>> trackEdges;
	private List<Set<Spot>> trackSpots;
	/**
	 * Feature storage. We use a List of Map as a 2D Map. The list maps each track to its feature map.
	 * We use the same index that for {@link #trackEdges} and {@link #trackSpots}.
	 * The feature map maps each {@link TrackFeature} to its float value for the selected track. 
	 */
	private List<EnumMap<TrackFeature, Float>> features;

	/*
	 * CONSTRUCTOR
	 */
	
	/**
	 * Construct a {@link TrackCollection} that contains all the tracks of the given graph.
	 */
	public TrackCollection(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		this.graph = graph;
		this.trackSpots = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		this.trackEdges = new ArrayList<Set<DefaultWeightedEdge>>(trackSpots.size());
		initFeatureMap();
		
		for(Set<Spot> spotTrack : trackSpots) {
			Set<DefaultWeightedEdge> spotEdge = new HashSet<DefaultWeightedEdge>();
			for(Spot spot : spotTrack)
				spotEdge.addAll(graph.edgesOf(spot));
			trackEdges.add(spotEdge);
		}
	}
	
	/**
	 * Construct a {@link TrackCollection} that contains only the given track. Each track <b>must</b> belong
	 * to the given graph, otherwise NPEs will be fired when trying to iterate over them or retrieving spots.
	 */
	public TrackCollection(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, List<Set<DefaultWeightedEdge>> tracks) {
		this.graph = graph;
		this.trackEdges = tracks;
		initFeatureMap();
	}
	
	
	/*
	 * FEATURES
	 */
	
	public void putFeature(final int trackIndex, final TrackFeature feature, final Float value) {
		features.get(trackIndex).put(feature, value);
	}
	
	/*
	 * GETTERS / SETTERS
	 */
	
	public Set<Spot> getTrackSpot(int index) {
		return trackSpots.get(index);
	}
	
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getGraph() {
		return graph;
	}
	
	
	/*
	 * UTILITY METHODS
	 */
	
	/**
	 * Return an iterator that iterates over the tracks as a set of spots. This class privileges tracks
	 * seen as a set of {@link DefaultWeightedEdge}s; this method is here to also have it as a collection
	 * of spots.
	 */
	public Iterator<Set<Spot>> spotIterator() {
		return trackSpots.iterator();
	}
	
	/**
	 * Return an array of exactly 2 spots which are the 2 vertices of the given edge.
	 * If the {@link SpotFeature#POSITION_T} is calculated for both spot, the array 
	 * will be sorted by increasing time.
	 */
	public Spot[] getSpotsFor(final DefaultWeightedEdge edge) {
		Spot[] spots = new Spot[2];
		Spot spotA = graph.getEdgeSource(edge);
		Spot spotB = graph.getEdgeTarget(edge);
		Float tA = spotA.getFeature(SpotFeature.POSITION_T);
		Float tB = spotB.getFeature(SpotFeature.POSITION_T);
		if (tA != null && tB != null && tA > tB) {
			spots[0] = spotB;
			spots[1] = spotA;
		} else {
			spots[0] = spotA;
			spots[1] = spotB;
		}
		return spots;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Instantiate an empty feature 2D map.
	 */
	private void initFeatureMap() {
		this.features = new ArrayList<EnumMap<TrackFeature,Float>>(trackEdges.size());
		for (int i = 0; i < trackEdges.size(); i++) {
			EnumMap<TrackFeature, Float> featureMap = new EnumMap<TrackFeature, Float>(TrackFeature.class);
			features.add(featureMap);
		}
	}
	
	
	/*
	 * ITERABLE
	 */
	
	@Override
	public Iterator<Set<DefaultWeightedEdge>> iterator() {
		return trackEdges.iterator();
	}
	
	/*
	 * LIST
	 */

	@Override
	public int size() {
		return trackEdges.size();
	}

	@Override
	public boolean isEmpty() {
		return (trackEdges == null) || trackEdges.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return trackEdges.contains(o);
	}

	@Override
	public Object[] toArray() {
		return trackEdges.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return trackEdges.toArray(a);
	}

	@Override
	public boolean add(Set<DefaultWeightedEdge> e) {
		return trackEdges.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return trackEdges.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return trackEdges.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Set<DefaultWeightedEdge>> c) {
		return trackEdges.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Set<DefaultWeightedEdge>> c) {
		return trackEdges.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return trackEdges.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return trackEdges.retainAll(c);
	}

	@Override
	public void clear() {
		trackEdges.clear();
	}

	@Override
	public Set<DefaultWeightedEdge> get(int index) {
		return trackEdges.get(index);
	}

	@Override
	public Set<DefaultWeightedEdge> set(int index, Set<DefaultWeightedEdge> element) {
		return trackEdges.set(index, element);
	}

	@Override
	public void add(int index, Set<DefaultWeightedEdge> element) {
		trackEdges.add(index, element);
	}

	@Override
	public Set<DefaultWeightedEdge> remove(int index) {
		return trackEdges.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return trackEdges.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return trackEdges.lastIndexOf(o);
	}

	@Override
	public ListIterator<Set<DefaultWeightedEdge>> listIterator() {
		return trackEdges.listIterator();
	}

	@Override
	public ListIterator<Set<DefaultWeightedEdge>> listIterator(int index) {
		return trackEdges.listIterator(index);
	}

	@Override
	public List<Set<DefaultWeightedEdge>> subList(int fromIndex, int toIndex) {
		return trackEdges.subList(fromIndex, toIndex);
	}


}
