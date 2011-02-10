package mpicbg.pointdescriptor.matcher;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.AbstractPointDescriptor;
import mpicbg.pointdescriptor.fit.FitResult;

public class SubsetMatcher implements Matcher
{
	final int subsetSize;
	final int numNeighbors;
	final int numCombinations;
	final int numMatchings;
	final int[][] neighbors;
	
	/**
	 * Matches n out of m neighbor points, the effort increases exponentially(!)
	 * 
	 * For example {@link SubsetMatcher}(4,4) is identical to calling {@link SimpleMatcher}(4)
	 * 
	 * @param subsetSize (n) - how many neighbors are matched in each try
	 * @param numNeighbors (m) - out of how many neighbors to choose
	 */
	public SubsetMatcher( final int subsetSize, final int numNeighbors )
	{
		this.subsetSize = subsetSize;
		this.numNeighbors = numNeighbors;
		
		this.neighbors = computePDRecursive( numNeighbors - subsetSize, subsetSize, 0 );
		
		this.numCombinations = this.neighbors.length;
		this.numMatchings = this.numCombinations * this.numCombinations;
	}
	
	public int getSubsetSize() { return subsetSize; }
	public int getNumNeighbors() { return numNeighbors; }
	public int getNumCombinations() { return numCombinations; }
	public int getNumMatchings() { return numMatchings; }
	
	@Override
	public int getRequiredNumNeighbors() { return numNeighbors; }
	
	@Override
	public < P extends Point, F extends AbstractPointDescriptor<P, F> > ArrayList<ArrayList<PointMatch>> createCandidates( final AbstractPointDescriptor<P, F> pd1, final AbstractPointDescriptor<P, F> pd2 )
	{
		final ArrayList<ArrayList<PointMatch>> matchesList = new ArrayList<ArrayList<PointMatch>>();		

		for ( int a = 0; a < numCombinations; ++a )
			for ( int b = 0; b < numCombinations; ++b )
			{
				final ArrayList<PointMatch> matches = new ArrayList<PointMatch>( subsetSize );		
				
				for ( int i = 0; i < subsetSize; ++i )
				{
					final PointMatch pointMatch = new PointMatch( pd1.getDescriptorPoint( neighbors[ a ][ i ] ), pd2.getDescriptorPoint( neighbors[ b ][ i ] ) );
					matches.add( pointMatch );
				}		

				matchesList.add( matches );
				
			}
				
		return matchesList;
	}

	@Override
	public double getNormalizationFactor( final ArrayList<PointMatch> matches, final FitResult fitResult ) { return 1;	}
	
	/**
	 * Computes recursively how to create different PointDescriptors of the same Point when some tolerance 
	 * is allowed, e.g. there are outliers which should be identified. For 3 neighbors allowing 2 outliers
	 * and starting with the second nearest neigbor (the nearest one is in most cases the one you use as input)
	 * the result looks like that:
	 * 
	 * (Each coloum will be one PointDescriptor)
	 *    
	 * 		|1| |1| |1| |2| |1| |1| |2| |1| |2| |3| 
	 * 		|2| |2| |3| |3| |2| |3| |3| |4| |4| |4| 
	 * 		|3| |4| |4| |4| |5| |5| |5| |5| |5| |5| 
	 * 
	 * @param tolerance - How many tolerance is accepted [0 ... m]
	 * @param n - initialized with the number of neighbors
	 * @param offset - the starting position for the neighbors, usually 1
	 * @return an array containing the neighbors for each PointDescriptor as array
	 */
	protected static int[][] computePDRecursive( final int tolerance, final int n, final int offset )
	{		
		if ( tolerance == 0 )
		{
			final int[][] neighbors = new int[1][n];
			
			for (int i = 0; i < n; i++)
				neighbors[0][i] = i + offset;
			
			return neighbors;
		}
		else 
		{
			final ArrayList<int[][]> allneighbors = new ArrayList<int[][]>();
			int size = 1;
			
			// compute the subgroups
			for (int k = tolerance + n - 1; k > tolerance - 1; k--)
			{
				final int[][] neighbors = computePDRecursive(tolerance - 1, k - tolerance + 1, offset);
				
				allneighbors.add(neighbors);
				size += neighbors.length;
			}
			
			// fill the final array
			final int[][] neighbors = new int[size][n];
						
			int pos = 0;
			
			for (int[][] subn : allneighbors)
			{
				for (int i = 0; i < subn.length; i++)
				{
					for (int j = 0; j < subn[i].length; j++)
						neighbors[i + pos][j] = subn[i][j];

					for (int j = subn[i].length; j < n; j++)
						neighbors[i + pos][j] = j + offset + tolerance;
				}

				pos += subn.length;
			}
			
			for (int j = 0; j < n; j++)
				neighbors[pos][j] = j + offset + tolerance;
			
			return neighbors;
		}
	}
	
}
