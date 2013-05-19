package fiji.plugin.trackmate.graph;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.BeforeClass;
import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class SortedDepthFirstIteratorTest {

	private static final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static Random rnd = new Random();
	private static int N_CHILDREN = 50;
	private static int N_LEVELS = 5;
	private static TrackMateModel model;
	private static Spot root;
	private static String[] names;
	private static Comparator<Spot> spotNameComparator;



	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		/*
		 * The comparator
		 */
		spotNameComparator = new Comparator<Spot>() {
			@Override
			public int compare(Spot o1, Spot o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		
		
		/*
		 * The graph
		 */
		
		model = new TrackMateModel();
		model.beginUpdate();
		try {

			// Root
			root = new Spot(new double[3], "Root");
			model.addSpotTo(root, 0);
			
			// First level
			names = new String[N_CHILDREN];
			Spot[][] spots = new Spot[N_LEVELS][N_CHILDREN];
			for (int i = 0; i < names.length; i++) {
				
				names[i] = randomString(5);
				Spot spotChild = new Spot(new double[3], names[i]);
				model.addSpotTo(spotChild, 1);
				model.addEdge(root, spotChild, -1);
				spots[0][i] = spotChild;
				
				spots[0][i] = spotChild;
				for (int j = 1; j < spots.length; j++) {
					Spot spot = new Spot(new double[3], "  "+j+"_"+randomString(3));
					spots[j][i] = spot;
					model.addSpotTo(spot, j+1);
					model.addEdge(spots[j-1][i], spots[j][i], -1);
				}
			}
		} finally {
			model.endUpdate();
		}
	}



	@Test
	public final void testBehavior() {
		
		// Sort names
		String[] expectedSortedNames = names.clone();
		Comparator<String> alphabeticalOrder = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		};
		Arrays.sort(expectedSortedNames, 0, expectedSortedNames.length, alphabeticalOrder);
		
		// Collect names in the tree
		SortedDepthFirstIterator<Spot, DefaultWeightedEdge> iterator = model.getTrackModel().getSortedDepthFirstIterator(root, spotNameComparator, true);
		String[] actualNames = new String[N_CHILDREN];
		int index = 0;
		while (iterator.hasNext()) {
			Spot spot = iterator.next();
			if (model.getTrackModel().getEdge(root, spot) != null){
				actualNames[index++] = spot.getName();
			}
		}

		assertArrayEquals(expectedSortedNames, actualNames);
	}


	private final static String randomString( int len )	{
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
			sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
		return sb.toString();
	}

}
