package fiji.plugin.trackmate.graph;

import static org.junit.Assert.assertEquals;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.BeforeClass;
import org.junit.Test;

public class RecursiveCumSumTest {

	private static SimpleDirectedGraph<int[], DefaultEdge> tree;
	private static int[] root;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tree = new SimpleDirectedGraph<int[], DefaultEdge>(DefaultEdge.class);
		
		root = new int[] { 1};
		int[] c1 = new int[] {1};
		int[] c11 = new int[] {1};
		int[] c111 = new int[] {1};
		int[] c112 = new int[] {1};
		int[] c12 = new int[] {1};
		int[] c2 = new int[] {1};
		
		tree.addVertex(root);
		tree.addVertex(c1);
		tree.addVertex(c2);
		tree.addVertex(c11);
		tree.addVertex(c12);
		tree.addVertex(c111);
		tree.addVertex(c112);
		
		tree.addEdge(root, c1);
		tree.addEdge(root, c2);
		tree.addEdge(c1, c11);
		tree.addEdge(c1, c12);
		tree.addEdge(c11, c111);
		tree.addEdge(c11, c112);
	}

	@Test
	public final void testApply() {
		Function2<int[], int[]> function = new Function2<int[], int[]>() {
			@Override
			public void compute(int[] input1, int[] input2, int[] output) {
				output[0] =  input1[0] + input2[0];
			}
		};
		RecursiveCumSum<int[], DefaultEdge> fun = new RecursiveCumSum<int[], DefaultEdge>(tree, function);
		int[] val = fun.apply(root);
		assertEquals(7, val[0]);
	}

}
