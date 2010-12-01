package ai;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instance;
import weka.core.Instances;

public class BalancedRandomTree implements Runnable, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** original data */
	Instances data = null;
	/** indices of the samples in the bag for this tree */
	ArrayList<Integer> bagIndices = null;
	/** split function generator */
	Splitter splitter = null;
	/** root node */
	BaseNode rootNode = null;
	
	/**
	 * Build random tree  
	 * @param numOfFeatures
	 * @param data
	 * @param bagIndices
	 */
	public BalancedRandomTree(
			final Instances data,
			ArrayList<Integer> bagIndices,
			final Splitter splitter)
	{
		this.data = data;
		this.bagIndices = bagIndices;
		this.splitter = splitter;
	}
	
	/**
	 * Build the random tree based on the data specified 
	 * in the constructor 
	 */
	public void run() 
	{
		rootNode = new InteriorNode(data, bagIndices, 0, splitter);		
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
		public abstract double[] eval( Instance instance );
		public int getDepth()
		{
			return 0;
		}
	}
	
	/**
	 * Leaf node in the tree 
	 *
	 */
	class LeafNode extends BaseNode implements Serializable
	{
		double[] probability;
		
		@Override
		public double[] eval(Instance instance) 
		{		
			return probability;
		}
		
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
		}
		
	}
	/**
	 * Interior node of the tree
	 *
	 */
	class InteriorNode extends BaseNode implements Serializable
	{
		BaseNode left;
		BaseNode right;
		final int depth;
		final SplitFunction splitFn;
		
		/**
		 * Construct interior node of the tree
		 * 
		 * @param data pointer to the original set of samples
		 * @param indices indices of the samples at this node
		 * @param depth current tree depth
		 */
		public InteriorNode(
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
		}
		
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
	
	
	
	
}
