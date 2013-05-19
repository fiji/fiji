/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.kdtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import mpicbg.imglib.algorithm.kdtree.node.Leaf;
import mpicbg.imglib.algorithm.kdtree.node.Node;
import mpicbg.imglib.algorithm.kdtree.node.NonLeaf;

/**
 * TODO
 *
 * @author Johannes Schindelin
 * @author Stephan Preibisch
 */
public class KDTree<T extends Leaf<T>>
{
	/*
	 * Use only a subset of at most medianLength semi-randomly picked
	 * values to determine the splitting point.
	 */
	final protected int medianLength;

	final protected int dimension;
	final protected Node<T> root;

	public static boolean debug = false;

	protected ArrayList<T> duplicates = new ArrayList<T>();

	/**
	 * Construct a KDTree from the elements in the given list.
	 *
	 * The elements must implement the interface Leaf.
	 *
	 * The parameter 'leaves' must be a list and cannot be an iterator,
	 * as the median needs to be calculated (or estimated, if the length
	 * is greater than medianLength).
	 */
	public KDTree(final List<T> leaves) {
		this( leaves, 100000 );
	}

	public KDTree(final List<T> leaves, final int maxMedianLength) {
		this.medianLength = maxMedianLength;
		this.dimension = leaves.get( 0 ).getNumDimensions();

		// test that dimensionality is preserved
		int i = 0;
		for (final T leaf : leaves) {
			if (leaf.getNumDimensions() != dimension)
				throw new RuntimeException("Dimensionality of nodes is not preserved, first entry has dimensionality " + dimension + " entry " + i + " has dimensionality " + leaf.getNumDimensions() );

			++i;
		}

		root = makeNode(leaves, 0);
	}

	protected Node<T> makeNode(final List<T> leaves, final int depth) {
		final int length = leaves.size();

		if (length == 0)
			return null;

		if (length == 1)
			return leaves.get(0);

		final int k = (depth % dimension);
		final float median = median(leaves, k);

		final List<T> left = new ArrayList<T>();
		final List<T> right = new ArrayList<T>();

		for (int i = 0; i < length; i++) {
			final T leaf = leaves.get(i);
			if (leaf.get(k) <= median)
				left.add(leaf);
			else
				right.add(leaf);
		}

		/*
		 * This fails for the following example:
		 *
		 * P1( 1, 1, 0 )
		 * P2( 0, 1, 1 )
		 * P3( 1, 0, 1 )
		 *
		 * k = 0
		 * ( 1; 0; 1 ) median = 1, all are <= 1
		 *
		 * k = 1
		 * ( 1; 1; 0 ) median = 1, all are <= 1
		 *
		 * k = 2
		 * ( 0; 1; 1 ) median = 1, all are <= 1
		 *
		 * That's why added the check for "leaf.get(k) < median"
		 */

		if (right.size() == 0) {
			if (allIdentical(left)) {
				final T result = leaves.get(0);
				left.remove(0);
				duplicates.addAll(left);
				return result;
			}
			else {
				left.clear();
				right.clear();

				for (int i = 0; i < length; i++) {
					final T leaf = leaves.get(i);
					if (leaf.get(k) < median)
						left.add(leaf);
					else
						right.add(leaf);
				}

			}

		}

		return new NonLeaf<T>(median, dimension, makeNode(left, depth + 1), makeNode(right, depth + 1));
	}

	protected boolean allIdentical(final List<T> list) {
		T first = null;
		for (final T leaf : list) {
			if (first == null)
				first = leaf;
			else {
				final T next = leaf;

				for (int j = 0; j < dimension; j++)
					if (next.get(j) != first.get(j))
						return false;
			}
		}
		return true;
	}

	public ArrayList<T> getDuplicates() {
		return duplicates;
	}

	public boolean hasDuplicates() {
		return duplicates.size() > 0;
	}

	protected float median(final List<T> leaves, final int k) {
		float[] list;
		if (leaves.size() <= medianLength) {
			list = new float[leaves.size()];
			for (int i = 0; i < list.length; i++) {
				T leaf = leaves.get(i);
				list[i] = leaf.get(k);
			}
		}
		else {
			list = new float[medianLength];
			Random random = new Random();
			for (int i = 0; i < list.length; i++) {
				int index = Math.abs(random.nextInt()) % list.length;
				T leaf = leaves.get(index);
				list[i] = leaf.get(k);
			}
		}

		Arrays.sort(list);

		return (list.length & 1) == 1 ? list[list.length / 2] :	(list[list.length / 2] + list[list.length / 2 - 1]) / 2;
	}

	public Node<T> getRoot() {
		return root;
	}

	public int getDimension() {
		return dimension;
	}

	public String toString(Node<T> node, String indent) {
		if (node == null)
			return indent + "null";
		if ( Leaf.class.isInstance(node) )
			return indent + node.toString();
		NonLeaf<T> nonLeaf = (NonLeaf<T>)node;
		return toString(nonLeaf.left, indent + "\t") + "\n"
			+ indent + nonLeaf.coordinate + "\n"
			+ toString(nonLeaf.right, indent + "\t") + "\n";
	}

	public String toString() {
		return toString(root, "");
	}

}
