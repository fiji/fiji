package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.algorithm.MultiThreaded;

import mpicbg.imglib.multithreading.SimpleMultiThreading;

/**
 * A utility class that wrap the {@link SortedMap} we use to store the spots contained
 * in each frame with a few utility methods.
 * <p>
 * Internally we rely on ConcurrentSkipListMap to allow concurrent access without clashes.
 * <p>
 * This class is {@link MultiThreaded}. There are a few processes that can benefit from multithreaded
 * computation ({@link #filter(Collection)}, {@link #filter(FeatureFilter)}
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Feb 2011 - 2013
 *
 */
public class SpotCollection implements Iterable<Spot>, SortedMap<Integer, List<Spot>>, MultiThreaded  {

	/** The frame by frame list of spot this object wrap. */
	private ConcurrentSkipListMap<Integer, List<Spot>> content;
	private int numThreads;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a new SpotCollection by wrapping the given {@link TreeMap} (and using its 
	 * comparator, if any).
	 */
	public SpotCollection(ConcurrentSkipListMap<Integer, List<Spot>> content) {
		this.content = content;
		setNumThreads();
	}

	/**
	 * Construct a new empty spot collection, with the natural order based comparator.
	 */
	public SpotCollection() {
		this(new ConcurrentSkipListMap<Integer, List<Spot>>());
	}

	/*
	 * METHODS
	 */

	@Override
	public String toString() {
		String str = super.toString();
		str += ": contains "+getNSpots()+" spots in "+keySet().size()+" different frames:\n";
		for (int key : content.keySet()) {
			str += "\tframe "+key+": "+getNSpots(key)+" spots.\n";
		}
		return str;
	}

	/**
	 * Return a new SpotCollection that contains only the given spots, at the right frame, taken from this
	 * spot collection. If one of the given spots is not found in this collection, it is not added to the
	 * new collection.
	 */
	public SpotCollection subset(Collection<Spot> spots) {
		SpotCollection newCollection = new SpotCollection();
		Integer frame;
		for(Spot spot : spots) {
			frame = getFrame(spot);
			if (null == frame)
				continue;
			newCollection.add(spot, frame);
		}
		return newCollection;
	}

	/**
	 * Remove the given spot from the given frame only.
	 * <p>
	 * If the given frame is <code>null</code>, or this collection does not have spots for the given frame,
	 * or the spot list for the given frame does not contain the spot, nothing is done and false
	 * is returned.
	 */
	public boolean remove(Spot spot, Integer frame) {
		if (null == frame)
			return false;
		List<Spot> spots = content.get(frame);
		if (null == spots)
			return false;
		return spots.remove(spot);		
	}

	/**
	 * Add the given spot to this collection, at the given frame. If the frame collection does not exist yet,
	 * it is created. If <code>null</code> is passed for the frame, nothing is done and false is returned. 
	 * Upon adding, the added spot has its feature {@link Spot#FRAME} updated with the passed frame value,
	 * if it is not <code>null</code>.
	 * @return true if adding was successful.
	 */
	public boolean add(Spot spot, Integer frame) {
		if (null == frame)
			return false;
		List<Spot> spots = content.get(frame);
		if (null == spots) {
			spots = new ArrayList<Spot>(1);
			content.put(frame, spots);
		}
		spot.putFeature(Spot.FRAME, frame);
		return spots.add(spot);
	}

	/**
	 * Return a subset of this collection, containing only the spots with the 
	 * feature satisfying the filter given. 
	 */
	public final SpotCollection filter(final FeatureFilter featurefilter) {
		final SpotCollection selectedSpots = new SpotCollection();
		selectedSpots.setNumThreads(numThreads);

		final int[] keys = new int[content.keySet().size()];
		Iterator<Integer> it = content.keySet().iterator();
		for (int i = 0; i < keys.length; i++) {
			keys[i] = it.next();
		}
		final AtomicInteger ai = new AtomicInteger();
		Thread[] threads = SimpleMultiThreading.newThreads();

		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate filtering spot thread "+ithread) {

				public void run() {

					Collection<Spot> spotThisFrame, spotToRemove;
					List<Spot> spotToKeep;
					Double val, tval;	

					for (int i = ai.getAndIncrement(); i < keys.length; i = ai.getAndIncrement()) {
					
						int timepoint = keys[i];
						spotThisFrame = content.get(timepoint);
						spotToKeep = new ArrayList<Spot>(spotThisFrame);
						spotToRemove = new ArrayList<Spot>(spotThisFrame.size());

						tval = featurefilter.value;
						if (null != tval) {

							if (featurefilter.isAbove) {
								for (Spot spot : spotToKeep) {
									val = spot.getFeature(featurefilter.feature);
									if (null == val)
										continue;
									if ( val < tval)
										spotToRemove.add(spot);
								}

							} else {
								for (Spot spot : spotToKeep) {
									val = spot.getFeature(featurefilter.feature);
									if (null == val)
										continue;
									if ( val > tval)
										spotToRemove.add(spot);
								}
							}
							spotToKeep.removeAll(spotToRemove); // no need to treat them multiple times

						}

						selectedSpots.put(timepoint, spotToKeep);
					}
				}
			};
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		
		return selectedSpots;
	}

	/**
	 * Return a subset of this collection, containing only the spots with the 
	 * feature satisfying all the filters given. 
	 */
	public final SpotCollection filter(final Collection<FeatureFilter> filters) {
		SpotCollection selectedSpots = new SpotCollection();
		selectedSpots.setNumThreads(numThreads);
		Collection<Spot> spotThisFrame, spotToRemove;
		List<Spot> spotToKeep;
		Double val, tval;	

		for (int timepoint : content.keySet()) {

			spotThisFrame = content.get(timepoint);
			spotToKeep = new ArrayList<Spot>(spotThisFrame);
			spotToRemove = new ArrayList<Spot>(spotThisFrame.size());

			for (FeatureFilter filter : filters) {

				tval = filter.value;
				if (null == tval)
					continue;
				spotToRemove.clear();

				if (filter.isAbove) {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(filter.feature);
						if (null == val)
							continue;
						if ( val < tval)
							spotToRemove.add(spot);
					}

				} else {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(filter.feature);
						if (null == val)
							continue;
						if ( val > tval)
							spotToRemove.add(spot);
					}
				}
				spotToKeep.removeAll(spotToRemove); // no need to treat them multiple times
			}
			selectedSpots.put(timepoint, spotToKeep);
		}
		return selectedSpots;
	}

	/**
	 * Return the closest {@link Spot} to the given location (encoded as a 
	 * Spot), contained in the frame <code>frame</code>. If the frame has no spot,
	 * return <code>null</code>.
	 */
	public final Spot getClosestSpot(final Spot location, final int frame) {
		final List<Spot> spots = content.get(frame);
		if (null == spots)	
			return null;
		double d2;
		double minDist = Double.POSITIVE_INFINITY;
		Spot target = null;
		for(Spot s : spots) {
			d2 = s.squareDistanceTo(location);
			if (d2 < minDist) {
				minDist = d2;
				target = s;
			}
		}
		return target;
	}

	/**
	 * Return the {@link Spot} at the given location (encoded as a Spot), contained 
	 * in the frame <code>frame</code>. A spot is returned <b>only</b> if there exists a spot
	 * such that the given location is within the spot radius. Otherwise <code>null</code> is
	 * returned. 
	 */
	public final Spot getSpotAt(final Spot location, final int frame) {
		final List<Spot> spots = content.get(frame);
		if (null == spots)	
			return null;
		double d2;
		for(Spot s : spots) {
			d2 = s.squareDistanceTo(location);
			if (d2 < s.getFeature(Spot.RADIUS) * s.getFeature(Spot.RADIUS)) {
				return s;
			}
		}
		return null;
	}


	/**
	 * Return the <code>n</code> closest {@link Spot} to the given location (encoded as a 
	 * Spot), contained in the frame <code>frame</code>. If the number of 
	 * spots in the frame is exhausted, a shorter set is returned.
	 * <p>
	 * The list is ordered by increasing distance to the given location.
	 *  TODO Rewrite using kD tree.
	 */
	public final List<Spot> getNClosestSpots(final Spot location, final int frame, int n) {
		final List<Spot> spots = content.get(frame);
		final TreeMap<Double, Spot> distanceToSpot = new TreeMap<Double, Spot>();

		double d2;
		for(Spot s : spots) {
			d2 = s.squareDistanceTo(location);
			distanceToSpot.put(d2, s);
		}

		final List<Spot> selectedSpots = new ArrayList<Spot>(n);
		final Iterator<Double> it = distanceToSpot.keySet().iterator();
		while (n > 0 && it.hasNext()) {
			selectedSpots.add(distanceToSpot.get(it.next()));
			n--;
		}
		return selectedSpots;
	}

	/**
	 * Finds the frame this spot belongs to if it is in this collection. If it is not
	 * in this collection, return <code>null</code>.
	 */
	public final Integer getFrame(final Spot spot) {
		/* NOTE: we have to search manually for the frame, which costs time and his
		 * a bit inelegant. The alternative would be to maintain a Map<Spot, Integer> 
		 * that knows in what frame can be found is a spot.		 */
		// Check if we can trust the Spot.FRAME feature
		Double candidateFrame = spot.getFeature(Spot.FRAME);
		if (null != candidateFrame) {
			int cf = candidateFrame.intValue();
			if (null != content.get(cf) && content.get(cf).contains(spot)) {
				return cf;
			}
		}
		// We have to find it manually
		Integer frame = null;
		for(Integer key : content.keySet()) {
			if (content.get(key).contains(spot)) {
				frame = key;
				break;
			}
		}
		return frame;
	}

	/**
	 * @return the total number of spots in this collection, over all frames.
	 */
	public final int getNSpots() {
		int nspots = 0;
		for(List<Spot> spots : content.values())
			nspots += spots.size();
		return nspots;
	}


	/**
	 * @return the number of spots at the given frame.
	 */
	public int getNSpots(int key) {
		List<Spot> spots = content.get(key);
		if (null == spots)
			return 0;
		else
			return spots.size();
	}

	/**
	 * @return a new list made of all the spot in this collection.
	 * <p>
	 * Spots are listed according to the comparator given to the content
	 * treemap (if none was given, the it is the natural order for the frame 
	 * they belong to).
	 */
	public final List<Spot> getAllSpots() {
		List<Spot> allSpots = new ArrayList<Spot>(getNSpots()); 
		for(List<Spot> spots : content.values())
			allSpots.addAll(spots);
		return allSpots;
	}

	/*
	 * ITERABLE & co
	 */

	/**
	 * Return an iterator that iterates over all the spots contained in this collection.
	 */
	@Override
	public Iterator<Spot> iterator() {
		return getAllSpots().iterator();
	}

	/**
	 * Return an iterator that iterates over the spots in the given frame.
	 */
	public Iterator<Spot> iterator(Integer frame) {
		return content.get(frame).iterator();
	}

	/*
	 * SORTEDMAP
	 */

	@Override
	public void clear() {
		content.clear();
	}

	@Override
	public int size() {
		return content.size();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return content.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return content.containsValue(value);
	}

	/**
	 * Return the spots for the specified frame. An empty list is returned
	 * if the specified frame contains no spots.
	 */
	@Override
	public List<Spot> get(Object key) {
		List<Spot> spots = content.get(key);
		if (null == spots) {
			return Collections.emptyList();
		}
		return spots;
	}

	@Override
	public List<Spot> put(Integer key, List<Spot> value) {
		if (key == null)
			return null;
		for (Spot spot : value) {
			spot.putFeature(Spot.FRAME, key);
		}
		return content.put(key, value);
	}

	@Override
	public List<Spot> remove(Object key) {
		return content.remove(key);
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends List<Spot>> map) {
		throw new UnsupportedOperationException("Adding a map to a SpotCollection is not supported."); // because it messes with the Spot#FRAME feature
	}

	@Override
	public Comparator<? super Integer> comparator() {
		return content.comparator();
	}

	@Override
	public SortedMap<Integer, List<Spot>> subMap(Integer fromKey, Integer toKey) {
		return content.subMap(fromKey, toKey);
	}

	@Override
	public SortedMap<Integer, List<Spot>> headMap(Integer toKey) {
		return content.headMap(toKey);
	}

	@Override
	public SortedMap<Integer, List<Spot>> tailMap(Integer fromKey) {
		return content.tailMap(fromKey);
	}

	@Override
	public Integer firstKey() {
		return content.firstKey();
	}

	@Override
	public Integer lastKey() {
		return content.lastKey();
	}

	@Override
	public Set<Integer> keySet() {
		return content.keySet();
	}

	@Override
	public Collection<List<Spot>> values() {
		return content.values();
	}

	@Override
	public Set<java.util.Map.Entry<Integer, List<Spot>>> entrySet() {
		return content.entrySet();
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}


}
