package fiji.plugin.trackmate.tracking.dumb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class DumbTracker implements SpotTracker {

	public static final String KEY = "DUMB_TRACKER";
	private static final String BASE_ERROR_MESSAGE = "[DumbTracker] ";
	/**
	 * How many times the std of previously created link distances
	 * we allow for the creation of new links.
	 */
	private static final double STD_FACTOR = 5d;
	/**
	 * The number of links to create before we apply limitations based
	 * on previously created link distance statistics.
	 */
	private static final int STATS_THRESHOLD = 3;
	/**
	 * Links shorter than this amount times the source spot will
	 * always be allowed to be created.
	 */
	private static final double RADIUS_FACTOR = 1d;

	private SpotCollection spots;
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private String errorMessage;
	private double mean;
	private double std;
	private int nstats;
	private double M2;

	public DumbTracker() {
	}

	@Override
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getResult() {
		return graph;
	}

	@Override
	public boolean checkInput() {
		if (null == spots) {
			errorMessage = BASE_ERROR_MESSAGE + "spots is null.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {
		graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for (final Spot spot : spots.iterable(true)) {
			graph.addVertex(spot);
		}

		/*
		 * Prepare frame array.
		 * Since we will need to go back and forth, an array seems fine
		 */

		final NavigableSet<Integer> nkeys = spots.keySet();
		final int[] frames = new int[nkeys.size()];
		int index = 0;
		for (final Integer frame : nkeys) {
			frames [ index++ ] = frame;
		}

		/*
		 * Prepare unlinked spots storage
		 */

		final HashMap<Integer, List<Spot>> openStarts = new HashMap<Integer, List<Spot>>(frames.length);
		final HashMap<Integer, List<Spot>> openEnds = new HashMap<Integer, List<Spot>>(frames.length);

		/*
		 * Reset link stats.
		 */

		mean = 0;
		std = 0;
		nstats = 0;
		M2 = 0;

		/*
		 * First step: we link spots frame to frame, using the closest pair,
		 * then the second closest pair, etc... until all source spots or
		 * all target spots have a link.
		 */

		for (int i = 0; i < frames.length - 1; i++) {

			final int frameSource = frames[i];
			final int frameTarget = frames[i+1];

			final List<Spot> now = new ArrayList<Spot>(spots.getNSpots(frameSource, true));
			for (final Spot spot : spots.iterable(frameSource, true)) {
				now.add(spot);
			}
			final List<Spot> after = new ArrayList<Spot>(spots.getNSpots(frameTarget, true));
			for (final Spot spot : spots.iterable(frameTarget, true)) {
				after.add(spot);
			}

			/*
			 * Find possible links between two sets.
			 * The method return the set of spot indices that have not been linked.
			 */

			final List<Set<Integer>> unmatchedIndices = link(now, after);
			final Set<Integer> indicesJ = unmatchedIndices.get(0);
			final Set<Integer> indicesK = unmatchedIndices.get(1);

			/*
			 * We have finished this frame pair. We then store the spots that
			 * have not been linked, either as a source or as a target. This
			 * will be used later to make gap-closing links.
			 */

			final List<Spot> sourceLeftOvers = new ArrayList<Spot>(indicesJ.size());
			for (final Integer is : indicesJ) {
				sourceLeftOvers.add(now.get(is));
			}
			openEnds.put(frameSource, sourceLeftOvers);


			final List<Spot> targetLeftOvers = new ArrayList<Spot>(indicesK.size());
			for (final Integer is : indicesK) {
				targetLeftOvers.add(after.get(is));
			}
			openStarts.put(frameTarget, targetLeftOvers);

		}

		/*
		 * Second step:
		 * We re-iterate over the data, and this time we create links over
		 * separated frames. We privilege close frames rather than close spots.
		 */

		for (int delta = 2; delta < frames.length; delta++) {

			for (int i = 0; i < frames.length - delta; i++) {

				final int frameSource = frames[i];
				final int frameTarget = frames[i+delta];

				final List<Spot> sources = openEnds.get(frameSource);
				final List<Spot> targets = openStarts.get(frameTarget);

				final List<Set<Integer>> unmatchedIndices = link(sources, targets);
				final Set<Integer> indicesSources = unmatchedIndices.get(0);
				final Set<Integer> indicesTargets = unmatchedIndices.get(1);

				/*
				 * Change the open ends and starts to only contain what has been linked
				 */

				final ArrayList<Spot> newSources = new ArrayList<Spot>(indicesSources.size());
				for (final Integer iJ : indicesSources) {
					newSources.add( sources.get(iJ) );
				}
				openEnds.put(i, newSources);

				final ArrayList<Spot> newTargets = new ArrayList<Spot>(indicesTargets.size());
				for (final Integer iJ : indicesTargets) {
					newTargets.add( targets.get(iJ) );
				}
				openStarts.put(i, newTargets);

			}

		}


		return true;
	}


	private void addToStats(final double dist) {
		final int n1 = nstats;
        nstats++;
        final double delta = dist - mean;
        final double delta_n = delta / nstats;
        final double term1 = delta * delta_n * n1;
        mean = mean + delta_n;
        M2 = M2 + term1;
        final double var = M2 / nstats;
        std = Math.sqrt(var);
	}


	private List<Set<Integer>> link(final List<Spot> now, final List<Spot> after) {
		/*
		 * Build cost matrix, using brute force.
		 * Takes O(nm) :(
		 */

		final double[][] costs = new double[now.size()][after.size()];
		for (int j = 0; j < now.size(); j++) {
			final Spot sa = now.get(j);
			for (int k = 0; k < after.size(); k++) {
				final Spot sb = after.get(k);
				costs[j][k] = sa.squareDistanceTo(sb);
			}
		}

		/*
		 * Loop over possible links until source or target spots are
		 * exhausted.
		 */

		final Set<Integer> indicesJ = getIndices(now.size());
		final Set<Integer> indicesK = getIndices(after.size());

		while (!indicesJ.isEmpty() && !indicesK.isEmpty()) {

			/*
			 * Find THE closest pair. In the non-pruned row and columns.
			 */

			double minCost = Double.POSITIVE_INFINITY;
			int minJ = -1, minK = -1;
			for (final int j : indicesJ) {
				for (final int k : indicesK) {
					if (minCost > costs[j][k]) {
						minCost = costs[j][k];
						minJ = j;
						minK = k;
					}
				}
			}

			/*
			 * Can we create a link in the graph?
			 * Check accumulated statistics. A special case: the user might have created
			 * succeeding spots that are at the same location. That way, the statistics
			 * would be tricked towards 0-std, which would prevent any other link creation.
			 * To avoid that, we say that a link will always be created between two spots
			 * that are closer than the first spot radius.
			 */

			final Spot source = now.get(minJ);
			final Spot target = after.get(minK);
			final double dist = Math.sqrt(minCost);
			if (nstats < STATS_THRESHOLD || dist < mean + STD_FACTOR * std || dist < RADIUS_FACTOR * source.getFeature(Spot.RADIUS) ) {

				/*
				 * Ok, then create a link in the graph.
				 */

				final DefaultWeightedEdge edge = graph.addEdge(source, target);
				graph.setEdgeWeight(edge, minCost);

				/*
				 * Change the iterating indices so that we do not iterate over the
				 * row and column that we have found now. That way, we empty the cost
				 * matrix little by little.
				 */

				indicesJ.remove(minJ);
				indicesK.remove(minK);

				/*
				 * Accumulate statistics
				 */

				addToStats(dist);

			} else {

				/*
				 * If the closest pair do not match the requirements, no other pair
				 * will and we can stop here.
				 *
				 */
				break;

			}
		}

		final List<Set<Integer>> unmatchedIndices = new ArrayList<Set<Integer>>(2);
		unmatchedIndices.add(indicesJ);
		unmatchedIndices.add(indicesK);
		return unmatchedIndices;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public void setTarget(final SpotCollection spots, final Map<String, Object> settings) {
		this.spots = spots;
	}


	/*
	 * STATIC METHODS
	 */

	private static final Set<Integer> getIndices(final int n) {
		final Set<Integer> indices = new HashSet<Integer>(n);
		for (int i = 0; i < n; i++) {
			indices.add(i);
		}
		return indices;
	}

}
