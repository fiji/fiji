package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.imglib2.algorithm.MultiThreaded;

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
public class SpotCollection implements MultiThreaded  {

	/** The frame by frame list of spot this object wrap. */
	private ConcurrentSkipListMap<Integer, ConcurrentHashMap<Spot, Boolean>> content = 	
			new ConcurrentSkipListMap<Integer, ConcurrentHashMap<Spot, Boolean>>();
	private int numThreads;

	/*
	 * CONSTRUCTORS
	 */


	/**
	 * Construct a new empty spot collection, with the natural order based comparator.
	 */
	public SpotCollection() {
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public String toString() {
		String str = super.toString();
		str += ": contains "+getNSpots(false)+" spots total in "+keySet().size()+" different frames, over which " + getNSpots(true) + " are visible:\n";
		for (int key : content.keySet()) {
			str += "\tframe "+key+": "+getNSpots(key, false)+" spots total, "+getNSpots(key, true)+" visible.\n";
		}
		return str;
	}

	/**
	 * Add the given spot to this collection, at the specified frame, and mark it as visible.
	 * <p>
	 * If the frame does not exist yet in the collection,  it is created and added. 
	 * Upon adding, the added spot has its feature {@link Spot#FRAME} updated with 
	 * the passed frame value.
	 */
	public void add(Spot spot, Integer frame) {
		ConcurrentHashMap<Spot,Boolean> spots = content.get(frame);
		if (null == spots) {
			spots = new ConcurrentHashMap<Spot, Boolean>();
			content.put(frame, spots);
		}
		spot.putFeature(Spot.FRAME, frame);
		spots.put(spot, Boolean.TRUE);
	}

	/**
	 * Remove the given spot from this collection, at the specified frame.
	 * <p>
	 * If the spot frame collection does not exist yet, nothing is done and <code>false</code> 
	 * is returned. If the spot cannot be found in the frame content, 
	 * nothing is done and <code>false</code> is returned.
	 */
	public boolean remove(Spot spot, Integer frame) {
		ConcurrentHashMap<Spot,Boolean> spots = content.get(frame);
		if (null == spots) {
			return false;
		}
		return (null != spots.remove(spot));
	}

	/**
	 * Returns <code>true</code> if the specified {@link Spot} found in the specified
	 * frame is visible, <code>false</code> otherwise.
	 * @param spot  the spot to test.
	 * @param frame  the frame it can be found in in this collection.
	 * @return  <code>true</code> if the spot is marked as visible.
	 * @throws NullPointerException if the frame does not exist in the collection or
	 * if the spot does not belong in the specified frame content.
	 */
	public boolean isVisible(Spot spot, Integer frame) {
		return content.get(frame).get(spot);
	}

	/**
	 * Sets the visibility of the specified spot in the specified frame.
	 * @param spot  the spot to mark as visible/invisible.
	 * @param frame  the frame it can be found in.
	 * @param visible  the visibility flag.
	 * @throws NullPointerException if the frame does not exist in the collection or
	 * if the spot does not belong in the specified frame content.
	 */
	public void setVisible(Spot spot, Integer frame, boolean visible) {
		if (!content.get(frame).containsKey(spot)) {
			throw new NullPointerException("Spot " + spot + " does not belong in the frame " + frame + ".");
		}
		content.get(frame).put(spot, Boolean.valueOf(visible));
	}


	public final long filter(final FeatureFilter featurefilter) {

		Collection<Integer> frames = content.keySet();
		List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>(content.size());

		for (final Integer frame : frames) {
			
			Callable<Integer> command = new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {

					double val, tval;	

					ConcurrentHashMap<Spot,Boolean> visibility = content.get(frame);
					int nspots = 0;
					tval = featurefilter.value;

					if (featurefilter.isAbove) {

						for (Spot spot : visibility.keySet()) {
							val = spot.getFeature(featurefilter.feature);
							if ( val < tval ) {
								visibility.put(spot, Boolean.FALSE);
							} else {
								visibility.put(spot, Boolean.TRUE);
								nspots++;
							}
						}

					} else {

						for (Spot spot : visibility.keySet()) {
							val = spot.getFeature(featurefilter.feature);
							if ( val > tval ) {
								visibility.put(spot, Boolean.FALSE);
							} else {
								visibility.put(spot, Boolean.TRUE);
								nspots++;
							}
						}
					}
					return nspots;
				}
			};
			tasks.add(command);
		}

		ExecutorService executors = Executors.newFixedThreadPool(numThreads);
		long nfiltered = 0;
		try {
			List<Future<Integer>> results = executors.invokeAll(tasks);
			for (Future<Integer> future : results) {
				nfiltered += future.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			executors.shutdown();
		}
		return nfiltered;
	}

	
	public final long filter(final Collection<FeatureFilter> filters) {

		Collection<Integer> frames = content.keySet();
		List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>(content.size());

		for (final Integer frame : frames) {
			Callable<Integer> command = new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					ConcurrentHashMap<Spot,Boolean> visibility = content.get(frame);

					double val, tval;
					int nspots = 0;
					boolean isAbove, shouldNotBeVisible;
					for (Spot spot : visibility.keySet()) {

						shouldNotBeVisible = false;
						for (FeatureFilter featureFilter : filters) {

							val = spot.getFeature(featureFilter.feature);
							tval = featureFilter.value;
							isAbove = featureFilter.isAbove;

							if ( isAbove && val < tval || !isAbove && val > tval) {
								shouldNotBeVisible = true;
								break;
							}
						} // loop over filters

						if (shouldNotBeVisible) {
							visibility.put(spot, Boolean.FALSE);
						} else {
							visibility.put(spot, Boolean.TRUE);
							nspots++;
						}
					} // loop over spots

					return nspots;
				} 

			};
			tasks.add(command);
		}

		ExecutorService executors = Executors.newFixedThreadPool(numThreads);
		long nfiltered = 0;
		try {
			List<Future<Integer>> results = executors.invokeAll(tasks);
			for (Future<Integer> future : results) {
				nfiltered += future.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			executors.shutdown();
		}
		return nfiltered;
	}

	/**
	 * Returns the closest {@link Spot} to the given location (encoded as a  Spot), 
	 * contained in the frame <code>frame</code>. If the frame has no spot,
	 * return <code>null</code>.
	 * @param location  the location to search for.
	 * @param frame  the frame to inspect.
	 * @param visibleSpotsOnly if true, will only search though visible spots. If false, will search through all spots. 
	 * @return  the closest spot to the specified location, member of this collection.
	 */
	public final Spot getClosestSpot(final Spot location, final int frame, boolean visibleSpotsOnly) {
		final ConcurrentHashMap<Spot,Boolean> visibility = content.get(frame);
		if (null == visibility)	
			return null;
		double d2;
		double minDist = Double.POSITIVE_INFINITY;
		Spot target = null;
		for (Spot s : visibility.keySet()) {

			if (visibleSpotsOnly && !visibility.get(s)) {
				continue;
			}

			d2 = s.squareDistanceTo(location);
			if (d2 < minDist) {
				minDist = d2;
				target = s;
			}

		}
		return target;
	}

	/**
	 * Returns the {@link Spot} at the given location (encoded as a Spot), contained 
	 * in the frame <code>frame</code>. A spot is returned <b>only</b> if there exists a spot
	 * such that the given location is within the spot radius. Otherwise <code>null</code> is
	 * returned. 
	 * @param location  the location to search for.
	 * @param frame  the frame to inspect.
	 * @param visibleSpotsOnly if true, will only search though visible spots. If false, will search through all spots. 
	 * @return  the closest spot to the specified location, member of this collection.
	 */
	public final Spot getSpotAt(final Spot location, final int frame, boolean visibleSpotsOnly) {
		final ConcurrentHashMap<Spot,Boolean> visibility = content.get(frame);
		if (null == visibility)	
			return null;
		double d2;
		for (Spot s : visibility.keySet()) {

			if (visibleSpotsOnly && !visibility.get(s)) {
				continue;
			}

			d2 = s.squareDistanceTo(location);
			if (d2 < s.getFeature(Spot.RADIUS) * s.getFeature(Spot.RADIUS)) {
				return s;
			}
		}
		return null;
	}


	/**
	 * Returns the <code>n</code> closest {@link Spot} to the given location (encoded as a 
	 * Spot), contained in the frame <code>frame</code>. If the number of 
	 * spots in the frame is exhausted, a shorter list is returned.
	 * <p>
	 * The list is ordered by increasing distance to the given location.
	 * @param location  the location to search for.
	 * @param frame  the frame to inspect.
	 * @param n the number of spots to search for.
	 * @param visibleSpotsOnly  if true, will only search though visible spots. If false, will search through all spots.
	 * @return  a new list, with of at most <code>n</code> spots, ordered by increasing distance from the specified location.
	 */
	public final List<Spot> getNClosestSpots(final Spot location, final int frame, int n, boolean visibleSpotsOnly) {
		// TODO Rewrite using kD tree.
		final ConcurrentHashMap<Spot,Boolean> visibility = content.get(frame);
		final TreeMap<Double, Spot> distanceToSpot = new TreeMap<Double, Spot>();

		double d2;
		for(Spot s : visibility.keySet()) {

			if (visibleSpotsOnly && !visibility.get(s)) {
				continue;
			}

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
	 * Returns the total number of spots in this collection, over all frames.
	 * @param visibleSpotsOnly  if true, will only count visible spots. 
	 * If false count all spots.
	 * @return the total number of spots in this collection.
	 */
	public final int getNSpots(boolean visibleSpotsOnly) {
		int nspots = 0;
		if (visibleSpotsOnly) {
			Iterator<Spot> it = iterator(true);
			while (it.hasNext()) {
				it.next();
				nspots++;
			}

		} else {
			for(ConcurrentHashMap<Spot, Boolean> spots : content.values())
				nspots += spots.size();
		}
		return nspots;
	}


	/**
	 * Returns the number of spots at the given frame.
	 * @param visibleSpotsOnly  if true, will only count visible spots. 
	 * If false count all spots.
	 * @return the number of spots at the given frame.
	 */
	public int getNSpots(int frame, boolean visibleSpotsOnly) {
		if (visibleSpotsOnly) {
			Iterator<Spot> it = iterator(frame, true);
			int nspots = 0;
			while (it.hasNext()) {
				nspots++;
			}
			return nspots;

		} else {
			ConcurrentHashMap<Spot,Boolean> spots = content.get(frame);
			if (null == spots)
				return 0;
			else
				return spots.size();
		}
	}

	/*
	 * ITERABLE & co
	 */

	/**
	 * Return an iterator that iterates over all the spots contained in this collection.
	 * @param visibleSpotsOnly  if true, the returned iterator will only iterate through visible
	 * spots. If false, it will iterate over all spots.
	 * @return an iterator that iterates over this collection.
	 */
	public Iterator<Spot> iterator(boolean visibleSpotsOnly) {
		if (visibleSpotsOnly) {
			return new VisibleSpotsIterator();
		} else {
			return new AllSpotsIterator();
		}
	}

	/**
	 * Return an iterator that iterates over the spots in the specified frame.
	 * If the specified frame is not a key of this collection, <code>null</code> is returned.
	 * @param visibleSpotsOnly  if true, the returned iterator will only iterate through visible
	 * spots. If false, it will iterate over all spots.
	 * @param frame  the frame to iterate over.
	 * @return an iterator that iterates over the content of a frame of this collection.
	 */
	public Iterator<Spot> iterator(Integer frame, boolean visibleSpotsOnly) {
		ConcurrentHashMap<Spot, Boolean> frameContent = content.get(frame);
		if (null == frameContent) {
			return null;
		}
		if (visibleSpotsOnly) {
			return new VisibleSpotsFrameIterator(frameContent);
		} else {
			return frameContent.keySet().iterator();
		}
	}

	/*
	 * SORTEDMAP
	 */

	/**
	 * Stores the specified spots as the content of the specified frame.
	 * The added spots are all marked as not visible. Their {@link Spot#FRAME}
	 * is updated to be the specified frame. 
	 * @param frame  the frame to store these spots at. The specified spots replace 
	 * the previous content of this frame, if any.
	 * @param spots  the spots to store.
	 */
	public void put(Integer frame, Collection<Spot> spots) {
		ConcurrentHashMap<Spot, Boolean> value = new ConcurrentHashMap<Spot, Boolean>(spots.size());
		for (Spot spot : spots) {
			spot.putFeature(Spot.FRAME, frame);
			value.put(spot, Boolean.FALSE);
		}
		content.put(frame, value);
	}

	public Integer firstKey() {
		return content.firstKey();
	}

	public Integer lastKey() {
		return content.lastKey();
	}

	public Set<Integer> keySet() {
		return content.keySet();
	}


	/*
	 * MULTITHREADING
	 */

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


	/*
	 * PRIVATE CLASSES
	 */

	private class AllSpotsIterator implements Iterator<Spot> {

		private boolean hasNext = true;
		private final Iterator<Integer> frameIterator;
		private Iterator<Spot> contentIterator;
		private Spot next = null;

		public AllSpotsIterator() {
			this.frameIterator = content.keySet().iterator();
			if (!frameIterator.hasNext()) {
				hasNext = false;
				return;
			}
			ConcurrentHashMap<Spot, Boolean> currentFrameContent = content.get(frameIterator.next());
			contentIterator = currentFrameContent.keySet().iterator();
			iterate();
		}

		private void iterate() {
			while (true) {

				// Is there still spots in current content?
				if (!contentIterator.hasNext()) {
					// No. Then move to next frame.
					// Is there still frames to iterate over? 
					if (!frameIterator.hasNext()) {
						// No. Then we are done
						hasNext = false;
						next = null;
						return;
					} else {
						contentIterator = content.get(frameIterator.next()).keySet().iterator();
						continue;
					}
				}
				next = contentIterator.next();
				return;
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Spot next() {
			Spot toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove operation is not supported for SpotCollection iterators.");
		}

	}



	private class VisibleSpotsIterator implements Iterator<Spot> {

		private boolean hasNext = true;
		private final Iterator<Integer> frameIterator;
		private Iterator<Spot> contentIterator;
		private Spot next = null;
		private ConcurrentHashMap<Spot, Boolean> currentFrameContent;

		public VisibleSpotsIterator() {
			this.frameIterator = content.keySet().iterator();
			if (!frameIterator.hasNext()) {
				hasNext = false;
				return;
			}
			currentFrameContent = content.get(frameIterator.next());
			contentIterator = currentFrameContent.keySet().iterator();
			iterate();
		}

		private void iterate() {

			while (true) {
				// Is there still spots in current content?
				if (!contentIterator.hasNext()) {
					// No. Then move to next frame.
					// Is there still frames to iterate over? 
					if (!frameIterator.hasNext()) {
						// No. Then we are done
						hasNext = false;
						next = null;
						return;
					} else {
						// Yes. Then start iterating over the next frame.
						currentFrameContent = content.get(frameIterator.next());
						contentIterator = currentFrameContent.keySet().iterator();
						continue;
					}
				}
				next = contentIterator.next();
				// Is it visible? 
				if (currentFrameContent.get(next)) {
					// Yes! Be happy and return
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Spot next() {
			Spot toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove operation is not supported for SpotCollection iterators.");
		}

	}


	private class VisibleSpotsFrameIterator implements Iterator<Spot> {

		private boolean hasNext = true;
		private Spot next = null;
		private final Iterator<Spot> contentIterator;
		private final ConcurrentHashMap<Spot, Boolean> frameContent;

		public VisibleSpotsFrameIterator(ConcurrentHashMap<Spot, Boolean> frameContent) {
			this.frameContent = frameContent;
			this.contentIterator = frameContent.keySet().iterator();
			iterate();
		}

		private void iterate() {
			while (true) {
				if (!contentIterator.hasNext()) {
					// No. Then we are done
					hasNext = false;
					next = null;
					return;
				}
				next = contentIterator.next();
				// Is it visible? 
				if (frameContent.get(next)) {
					// Yes. Be happy, and return.
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Spot next() {
			Spot toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove operation is not supported for SpotCollection iterators.");
		}

	}
}
