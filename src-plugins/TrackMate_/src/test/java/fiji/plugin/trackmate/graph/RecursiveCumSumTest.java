package fiji.plugin.trackmate.graph;

import static org.junit.Assert.*;

import net.imglib2.type.numeric.integer.IntType;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.BeforeClass;
import org.junit.Test;

public class RecursiveCumSumTest {

	private static SimpleDirectedGraph<IntType, DefaultEdge> tree;
	private static IntType root;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tree = new SimpleDirectedGraph<IntType, DefaultEdge>(DefaultEdge.class);
		
		root = new IntType(1);
		IntType c1 = new IntType(1);
		IntType c11 = new IntType(1);
		IntType c111 = new IntType(1);
		IntType c112 = new IntType(1);
		IntType c12 = new IntType(1);
		IntType c2 = new IntType(1);
		
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
		OutputFunction<IntType> function = new OutputFunction<IntType>() {
			@Override
			public IntType compute(IntType input1, IntType input2) {
				return new IntType(input1.get() + input2.get());
			}
		};
		RecursiveCumSum<IntType, DefaultEdge> fun = new RecursiveCumSum<IntType, DefaultEdge>(tree, function);
		IntType val = fun.apply(root);
		assertEquals(7, val.get());
	}

}
