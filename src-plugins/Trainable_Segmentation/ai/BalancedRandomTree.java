package ai;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import weka.core.Instance;
import weka.core.Instances;

/**
 * This class implements a random tree based on the split
 * function specified by the template in Splitter
 * 
 */
public class BalancedRandomTree implements Serializable
{
	/** Generated serial version UID */
	private static final long serialVersionUID = 41518309467L;
	/** root node */
	private final BaseNode rootNode;

	/**
	 * Build random tree for a balanced random forest  
	 * 
	 * @param data original data
	 * @param bagIndices indices of the data samples to use
	 * @param splitter split function generator
	 */
	public BalancedRandomTree(final Instances data, final ArrayList<Integer> bagIndices, final Splitter splitter)
	{
		this.rootNode = createNode( data, bagIndices, splitter );
	}

	/**
	 * Build the random tree based on the data specified 
	 * in the constructor 
	 */
	private final BaseNode createNode(final Instances data, final ArrayList<Integer> bagIndices, final Splitter splitter)
	{
		final long start = System.currentTimeMillis();
		try {
			return createTree(data, bagIndices, 0, splitter);
		} finally {
			final long end = System.currentTimeMillis();
			IJ.log("Creating tree took: " + (end-start) + "ms");
		}
	}

	/**
	 * Evaluate sample
	 * 
	 * @param instance sample to evaluate
	 * @return array of class probabilities
	 */
	public double[] evaluate(Instance instance)
	{
		if (null == rootNode)
			return null;
		return rootNode.eval(instance);
	}


	/**
	 * Basic node of the tree
	 *
	 */
	abstract class BaseNode implements Serializable
	{

		/** serial version ID */
		private static final long serialVersionUID = 46734234231L;
		/**
		 * Evaluate an instance
		 * @param instance input sample
		 * @return class probabilities
		 */
		public abstract double[] eval( Instance instance );
		/**
		 * Get the node depth
		 * 
		 * @return tree depth at that node
		 */
		public int getDepth()
		{
			return 0;
		}
	} // end class BaseNode

	/**
	 * Leaf node in the tree 
	 *
	 */
	class LeafNode extends BaseNode implements Serializable
	{
		/** serial version ID */
		private static final long serialVersionUID = 2019873470157L;
		/** class probabilites */
		double[] probability;

		@Override
		public double[] eval(Instance instance) 
		{		
			return probability;
		}
		/**
		 * Create a leaf node
		 * 
		 * @param probability class probabilities
		 */
		public LeafNode(double[] probability)
		{
			this.probability = probability;
		}

		/**
		 * Create leaf node based on the current split data
		 *  
		 * @param data pointer to original data
		 * @param indices indices at this node
		 */
		public LeafNode(
				final Instances data, 
				ArrayList<Integer> indices)
		{
			this.probability = new double[ data.numClasses() ];
			for(final Integer it : indices)
			{
				this.probability[ (int) data.get( it.intValue() ).classValue()] ++;
			}
			// Divide by the number of elements
			for(int i=0; i<data.numClasses(); i++)
				this.probability[i] /= (double) indices.size();
		}

	} //end class LeafNode
	
	/**
	 * Interior node of the tree
	 *
	 */
	class InteriorNode extends BaseNode implements Serializable
	{
		/** serial version ID */
		private static final long serialVersionUID = 9972970234021L;
		/** left son */
		BaseNode left;
		/** right son */
		BaseNode right;
		/** node depth */
		final int depth;
		/** split function that divides the samples into left and right sons */
		final SplitFunction splitFn;

		/**
		 * Constructs an interior node of the random tree
		 * 
		 * @param depth tree depth at this node
		 * @param splitFn split function
		 */
		private InteriorNode(int depth, SplitFunction splitFn) 
		{
			this.depth = depth;
			this.splitFn = splitFn;
		}

		/**
		 * Construct interior node of the tree
		 * 
		 * @param data pointer to the original set of samples
		 * @param indices indices of the samples at this node
		 * @param depth current tree depth
		 */
		/*		public InteriorNode(
				final Instances data,
				final ArrayList<Integer> indices,
				final int depth,
				final Splitter splitFnProducer)
		{
			this.splitFn = splitFnProducer.getSplitFunction(data, indices);


			this.depth = depth;

			// left and right new arrays
			final ArrayList<Integer> leftArray = new ArrayList<Integer>();
			final ArrayList<Integer> rightArray = new ArrayList<Integer>();

			// split data
			int totalLeft = 0;
			int totalRight = 0;
			for(final Integer it : indices)
			{
				if( splitFn.evaluate( data.get(it.intValue()) ) )
				{
					leftArray.add(it);
					totalLeft ++;					
				}
				else
				{
					rightArray.add(it);
					totalRight ++;
				}
			}
			//System.out.println("total left = " + totalLeft + ", total rigth = " + totalRight + ", depth = " + depth);					
			//indices.clear();
			if( totalLeft == 0 )
			{
				left = new LeafNode(data, rightArray);
			}
			else if ( totalRight == 0 )
			{
				left = new LeafNode(data, leftArray);
			}
			else
			{
				left = new InteriorNode(data, leftArray, depth+1, splitFnProducer);
				right = new InteriorNode(data, rightArray, depth+1, splitFnProducer);
			}				
		}*/


		/**
		 * Evaluate sample at this node
		 */
		public double[] eval(Instance instance) 
		{
			if( null != right)
			{
				if(this.splitFn.evaluate( instance ) )
				{
					return left.eval(instance);
				}
				else
					return right.eval(instance);
			}
			else // leaves are always left nodes 
				return left.eval(instance);				
		}


		/**
		 * Get node depth
		 */
		public int getDepth()
		{
			return this.depth;
		}
	}

	/**
	 * Create random tree (non-recursively)
	 * 
	 * @param data original data
	 * @param indices indices of the samples to use
	 * @param depth starting depth
	 * @param splitFnProducer split function producer
	 * @return root node 
	 */
	private InteriorNode createTree(
			final Instances data,
			final ArrayList<Integer> indices,
			final int depth,
			final Splitter splitFnProducer)
	{
		int maxDepth = depth;
		// Create root node
		InteriorNode root = new InteriorNode(depth, splitFnProducer.getSplitFunction(data, indices));
		
		// Create list of nodes to process and add the root to it
		final LinkedList<InteriorNode> remainingNodes = new LinkedList<InteriorNode>();
		remainingNodes.add(root);
		
		// Create list of indices to process (it must match all the time with the node list)
		final LinkedList<ArrayList<Integer>> remainingIndices = new LinkedList<ArrayList<Integer>>();
		remainingIndices.add(indices);
		
		// While there is still nodes to process
		while (!remainingNodes.isEmpty()) 
		{
			final InteriorNode currentNode = remainingNodes.removeLast();
			final ArrayList<Integer> currentIndices = remainingIndices.removeLast();
			// new arrays of indices for the left and right sons
			final ArrayList<Integer> leftArray = new ArrayList<Integer>();
			final ArrayList<Integer> rightArray = new ArrayList<Integer>();

			// split data
			for(final Integer it : currentIndices)
			{
				if( currentNode.splitFn.evaluate( data.get(it.intValue()) ) )
				{
					leftArray.add(it);
				}
				else
				{
					rightArray.add(it);
				}
			}
			//System.out.println("total left = " + leftArray.size() + ", total right = " + rightArray.size() + ", depth = " + currentNode.depth);					
			// Update maximum depth (for the record)
			if(currentNode.depth > maxDepth)
				maxDepth = currentNode.depth;

			if( leftArray.isEmpty() )
			{
				currentNode.left = new LeafNode(data, rightArray);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else if ( rightArray.isEmpty() )
			{
				currentNode.left = new LeafNode(data, leftArray);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else
			{
				currentNode.left = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(data, leftArray));
				remainingNodes.add((InteriorNode)currentNode.left);
				remainingIndices.add(leftArray);

				currentNode.right = new InteriorNode(currentNode.depth+1, splitFnProducer.getSplitFunction(data, rightArray));
				remainingNodes.add((InteriorNode)currentNode.right);
				remainingIndices.add(rightArray);
			}
		}

		System.out.println("Max depth = " + maxDepth);
		return root;
	}

}
