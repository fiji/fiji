package fiji.plugin.trackmate.graph;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.VertexFactory;
import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackGraphModel;

public class GraphUtils {


	/**
	 * @return a pretty-print string representation of a {@link TrackGraphModel}, as long it is 
	 * a tree (each spot must not have more than one predecessor).
	 * @throws IllegalArgumentException if the given graph is not a tree.
	 */
	public static final String toString(final TrackGraphModel model) {
		/*
		 * Get directed cache
		 */
		DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache = model.getDirectedNeighborIndex();
		
		/*
		 * Check input
		 */
		if (!isTree(model, cache)) {
			throw new IllegalArgumentException("toString cannot be applied to graphs that are not trees (each vertex must have at most one predecessor).");
		}
		
		/*
		 * Get column widths
		 */
		Map<Spot, Integer> widths = cumulativeBranchWidth(model);
		
		/*
		 * By the way we compute the largest spot name
		 */
		int largestName = 0;
		for (Spot spot : model.vertexSet()) {
			if (spot.getName().length() > largestName) {
				largestName = spot.getName().length();
			}
		}
		largestName += 2;

		/*
		 * Find how many different frames we have
		 */
		TreeSet<Integer> frames = new TreeSet<Integer>();
		for (Spot spot : model.vertexSet()) {
			frames.add(spot.getFeature(Spot.FRAME).intValue());
		}
		int nframes = frames.size();


		/*
		 * Build string, one StringBuilder per frame
		 */
		HashMap<Integer, StringBuilder> strings = new HashMap<Integer, StringBuilder>(nframes);
		for (Integer frame : frames) {
			strings.put(frame, new StringBuilder());
		}

		HashMap<Integer, StringBuilder> below = new HashMap<Integer, StringBuilder>(nframes);
		for (Integer frame : frames) {
			below.put(frame, new StringBuilder());
		}

		/*
		 * Keep track of where the carret is for each spot
		 */
		Map<Spot, Integer> carretPos = new HashMap<Spot, Integer>(model.vertexSet().size()); 

		/*
		 * Comparator to have spots order by name
		 */
		Comparator<Spot> comparator = Spot.nameComparator;
		
		/*
		 * Let's go!
		 */

		for (Integer trackID : model.getFilteredTrackIDs()) {
			
			/*
			 *  Get the 'first' spot for an iterator that starts there
			 */
			Set<Spot> track = model.getTrackSpots(trackID);
			Iterator<Spot> it = track.iterator();
			Spot first = it.next();
			for (Spot spot : track) {
				if (first.diffTo(spot, Spot.FRAME) > 0) {
					first = spot;
				}
			}

			/*
			 * First, fill the linesBelow with spaces
			 */
			for (Integer frame : frames) {
				int columnWidth = widths.get(first);
				below.get(frame).append(makeSpaces(columnWidth*largestName));
			}
			
			/*
			 * Iterate down the tree
			 */
			SortedDepthFirstIterator<Spot,DefaultWeightedEdge> iterator = model.getSortedDepthFirstIterator(first, comparator, true);
			while (iterator.hasNext()) {

				Spot spot = iterator.next();
				int frame = spot.getFeature(Spot.FRAME).intValue();
				boolean isLeaf = cache.successorsOf(spot).size() == 0;

				int columnWidth = widths.get(spot);
				String str = spot.getName();
				int nprespaces = largestName/2 - str.length()/2;
				strings.get(frame).append(makeSpaces(columnWidth / 2 * largestName));
				strings.get(frame).append(makeSpaces(nprespaces));
				strings.get(frame).append(str);
				// Store bar position - deal with bars below
				int currentBranchingPosition = strings.get(frame).length() - str.length()/2;
				carretPos.put(spot, currentBranchingPosition);
				// Resume filling the branch
				strings.get(frame).append(makeSpaces(largestName - nprespaces - str.length()));
				strings.get(frame).append(makeSpaces( (columnWidth*largestName) - (columnWidth/2*largestName) - largestName));

				// is leaf? then we fill all the columns below
				if (isLeaf) {
					SortedSet<Integer> framesToFill = frames.tailSet(frame, false);
					for (Integer subsequentFrame : framesToFill) {
						strings.get(subsequentFrame).append(makeSpaces(columnWidth * largestName));
					}
				} else {
					// Is there an empty slot below? Like when a link jumps above several frames?
					Set<Spot> successors = cache.successorsOf(spot);
					for (Spot successor : successors) {
						if (successor.diffTo(spot, Spot.FRAME) > 1) {
							for (int subFrame = successor.getFeature(Spot.FRAME).intValue(); subFrame <= successor.getFeature(Spot.FRAME).intValue(); subFrame++) {
								strings.get(subFrame-1).append(makeSpaces(columnWidth * largestName));
							}
						}
					}
				}
				
				

			} // Finished iterating over spot of the track
			
			// Fill remainder with spaces
			
			for (Integer frame : frames) {
				int columnWidth = widths.get(first);
				StringBuilder sb = strings.get(frame);
				int pos = sb.length();
				int nspaces = columnWidth * largestName - pos;
				if (nspaces > 0) {
					sb.append(makeSpaces(nspaces));
				}
			}

		} // Finished iterating over the track
		
		
		/*
		 * Second iteration over edges
		 */
		
		Set<DefaultWeightedEdge> edges = model.edgeSet();
		for (DefaultWeightedEdge edge : edges) {
			
			Spot source = model.getEdgeSource(edge);
			Spot target = model.getEdgeTarget(edge);
			
			int sourceCarret = carretPos.get(source) - 1;
			int targetCarret = carretPos.get(target) - 1;
			
			int sourceFrame = source.getFeature(Spot.FRAME).intValue();
			int targetFrame = target.getFeature(Spot.FRAME).intValue();
			
			for (int frame = sourceFrame; frame < targetFrame; frame++) {
				below.get(frame).setCharAt(sourceCarret, '|');
			}
			for (int frame = sourceFrame+1; frame < targetFrame; frame++) {
				strings.get(frame).setCharAt(sourceCarret, '|');
			}
			
			if (cache.successorsOf(source).size() > 1) {
				// We have branching
				int minC = Math.min(sourceCarret, targetCarret);
				int maxC = Math.max(sourceCarret, targetCarret);
				StringBuilder sb = below.get(sourceFrame);
				for (int i = minC+1; i < maxC; i++) {
					if (sb.charAt(i) == ' ') {
						sb.setCharAt(i, '-');
					}
				}
				sb.setCharAt(minC, '+');
				sb.setCharAt(maxC, '+');
			}
		}
		

		/*
		 * Concatenate strings
		 */

		StringBuilder finalString = new StringBuilder();
		for (Integer frame : frames) {

			finalString.append(strings.get(frame).toString());
			finalString.append('\n');
			finalString.append(below.get(frame).toString());
			finalString.append('\n');
		}


		return finalString.toString();

	}
	
	
	
	
	public static final boolean isTree(TrackGraphModel model, DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache) {
		return isTree(model.vertexSet(), cache);
	}
	

	
	public static final boolean isTree(Iterable<Spot> spots, DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache) {
		for (Spot spot : spots) {
			if (cache.predecessorsOf(spot).size() > 1) {
				return false;
			}
		}
		return true;
	}
	
	
	
	
	public static final Map<Spot, Integer> cumulativeBranchWidth(final TrackGraphModel model) {

		/*
		 * Elements stored:
		 * 	0. cumsum of leaf
		 */
		VertexFactory<int[]> factory = new VertexFactory<int[]>() {
			@Override
			public int[] createVertex() {
				return new int[1];
			}
		};

		/*
		 * Build isleaf tree
		 */

		final DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache = model.getDirectedNeighborIndex();

		Function1<Spot, int[]> isLeafFun = new Function1<Spot, int[]>() {
			@Override
			public void compute(Spot input, int[] output) {
				if (cache.successorsOf(input).size() == 0) {
					output[0] = 1;
				} else {
					output[0] = 0;
				}
			}
		};


		Map<Spot, int[]> mappings = new HashMap<Spot, int[]>();
		SimpleDirectedWeightedGraph<int[], DefaultWeightedEdge> leafTree = model.copy(factory, isLeafFun, mappings);

		/*
		 * Find root spots & first spots
		 * Roots are spots without any ancestors. There might be more than one per track.
		 * First spots are the first root found in a track. There is only one per track.
		 * 
		 * By the way we compute the largest spot name
		 */

		Map<Integer, Set<Spot>> tracks = model.getTrackSpots();
		Set<Spot> roots = new HashSet<Spot>(tracks.size()); // approx
		Set<Spot> firsts = new HashSet<Spot>(tracks.size()); // exact
		for (Set<Spot> track : tracks.values()) {
			boolean firstFound = false;
			for (Spot spot : track) {

				if (cache.predecessorsOf(spot).size() == 0) {
					if (!firstFound) {
						firsts.add(spot);
					}
					roots.add(spot);
					firstFound = true;
				}
			}
		}

		/*
		 * Build cumsum value
		 */

		Function2<int[], int[]> cumsumFun = new Function2<int[], int[]>() {
			@Override
			public void compute(int[] input1, int[] input2, int[] output) {
				output[0] = input1[0] + input2[0];
			}
		};

		RecursiveCumSum<int[], DefaultWeightedEdge> cumsum = new RecursiveCumSum<int[], DefaultWeightedEdge>(leafTree, cumsumFun);
		for(Spot root : firsts) {
			int[] current = mappings.get(root);
			cumsum.apply(current);
		}
		
		/*
		 * Convert to map of spot vs integer 
		 */
		Map<Spot, Integer> widths = new HashMap<Spot, Integer>();
		for (Spot spot : model.vertexSet()) {
			widths.put(spot, mappings.get(spot)[0]);
		}
		
		return widths;
	}
	
	
	

	private static char[] makeSpaces(int width) {
		return makeChars(width, ' ');
	}


	private static char[] makeChars(int width, char c) {
		char[] chars = new char[width];
		Arrays.fill(chars, c);
		return chars;
	}


	/**
	 * @return true only if the given model is a tree; that is: every spot has one or less
	 * predecessors.
	 */
	public static final Set<Spot> getSibblings(final DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache, final Spot spot) {
		HashSet<Spot> sibblings = new HashSet<Spot>();
		Set<Spot> predecessors = cache.predecessorsOf(spot);
		for (Spot predecessor : predecessors) {
			sibblings.addAll(cache.successorsOf(predecessor));
		}
		return sibblings;
	}


}
