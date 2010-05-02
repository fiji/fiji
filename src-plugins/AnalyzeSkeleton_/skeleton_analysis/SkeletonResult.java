package skeleton_analysis;

import java.util.ArrayList;

/**
 * AnalyzeSkeleton_ plugin for ImageJ(C) and Fiji.
 * Copyright (C) 2009,2010 Daniel Hornung
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 */

/**
 * Utility class to transfer the analysis results between different plugins.
 * <p>
 * The meaning of its members should become clear when looking at
 * AnalyzeSkeleton_'s source code.
 *
 * @version 1.0 2009-12-17
 * @author Daniel Hornung <daniel.hornung@ds.mpg.de>
 *
 */

public class SkeletonResult
{
	int numOfTrees;

	// skeleton tree fields
	/** number of branches for every specific tree */
	int[] numberOfBranches = null;
	/** number of end points voxels of every tree */
	int[] numberOfEndPoints = null;
	/** number of junctions voxels of every tree*/
	int[] numberOfJunctionVoxels = null;
	/** number of slab voxels of every specific tree */
	int[] numberOfSlabs = null;
	/** number of junctions of every specific tree*/
	int[] numberOfJunctions = null;
	/** number of triple points in every tree */
	int[] numberOfTriplePoints = null;
	/** number of quadruple points in every tree */
	int[] numberOfQuadruplePoints = null;
	/** average branch length */
	double[] averageBranchLength = null;
	/** maximum branch length */
	double[] maximumBranchLength = null;
	/** total number of voxels of every tree */
	int[] numberOfVoxels = null;

	/** list of end point coordinates in the entire image */
	ArrayList <Point> listOfEndPoints = null;
	/** list of junction coordinates in the entire image */
	ArrayList <Point> listOfJunctionVoxels = null;
	/** list of slab coordinates in the entire image */
	ArrayList <Point> listOfSlabVoxels = null;
	/** list of slab coordinates in the entire image */
	ArrayList <Point> listOfStartingSlabVoxels = null;

	/** array of graphs (one per tree) */
	private Graph[] graph = null;


	public SkeletonResult(int numOfTrees)
	{
		this.numOfTrees = numOfTrees;
	}

	// setter methods
	public void setNumOfTrees(int numOfTrees)
	{
		this.numOfTrees = numOfTrees;
	}

	public void setBranches(int[] numberOfBranches)
	{
		this.numberOfBranches = numberOfBranches;
	}

	public void setJunctions(int[] numberOfJunctions)
	{
		this.numberOfJunctions = numberOfJunctions;
	}

	public void setEndPoints(int[] numberOfEndPoints)
	{
		this.numberOfEndPoints = numberOfEndPoints;
	}

	public void setJunctionVoxels(int[] numberOfJunctionVoxels)
	{
		this.numberOfJunctionVoxels = numberOfJunctionVoxels;
	}

	public void setSlabs(int[] numberOfSlabs)
	{
		this.numberOfSlabs = numberOfSlabs;
	}

	public void setNumberOfVoxels(int[] numberOfVoxels)
	{
		this.numberOfVoxels = numberOfVoxels;
	}

	public void setTriples(int[] numberOfTriplePoints)
	{
		this.numberOfTriplePoints = numberOfTriplePoints;
	}

	public void setQuadruples(int[] numberOfQuadruplePoints)
	{
		this.numberOfQuadruplePoints = numberOfQuadruplePoints;
	}

	public void setAverageBranchLength(double[] averageBranchLength)
	{
		this.averageBranchLength = averageBranchLength;
	}

	public void setMaximumBranchLength(double[] maximumBranchLength)
	{
		this.maximumBranchLength = maximumBranchLength;
	}


	public void setListOfEndPoints(ArrayList <Point> listOfEndPoints)
	{
		this.listOfEndPoints = listOfEndPoints;
	}

	public void setListOfJunctionVoxels(ArrayList <Point> listOfJunctionVoxels)
	{
		this.listOfJunctionVoxels = listOfJunctionVoxels;
	}

	public void setListOfSlabVoxels(ArrayList <Point> listOfSlabVoxels)
	{
		this.listOfSlabVoxels = listOfSlabVoxels;
	}

	public void setListOfStartingSlabVoxels(ArrayList <Point> listOfStartingSlabVoxels)
	{
		this.listOfStartingSlabVoxels = listOfStartingSlabVoxels;
	}


	public void setGraph(Graph[] graph)
	{
		this.graph = graph;
	}



	// getter methods
	public int getNumOfTrees()
	{
		return numOfTrees;
	}

	public int[] getBranches()
	{
		return numberOfBranches;
	}

	public int[] getJunctions()
	{
		return numberOfJunctions;
	}

	public int[] getEndPoints()
	{
		return numberOfEndPoints;
	}

	public int[] getJunctionVoxels()
	{
		return numberOfJunctionVoxels;
	}

	public int[] getSlabs()
	{
		return numberOfSlabs;
	}

	public int[] getTriples()
	{
		return numberOfTriplePoints;
	}

	public int[] getQuadruples()
	{
		return numberOfQuadruplePoints;
	}

	public double[] getAverageBranchLength()
	{
		return averageBranchLength;
	}

	public double[] getMaximumBranchLength()
	{
		return maximumBranchLength;
	}

	public int[] getNumberOfVoxels()
	{
		return numberOfVoxels;
	}


	public ArrayList <Point> getListOfEndPoints()
	{
		return listOfEndPoints;
	}

	public ArrayList <Point> getListOfJunctionVoxels()
	{
		return listOfJunctionVoxels;
	}

	public ArrayList <Point> getListOfSlabVoxels()
	{
		return listOfSlabVoxels;
	}

	public ArrayList <Point> getListOfStartingSlabVoxels()
	{
		return listOfStartingSlabVoxels;
	}


	public Graph[] getGraph()
	{
		return graph;
	}

	// utility methods

	/**
	 * Calculates and saves the sum of voxels for every tree as the sum of end
	 * points, junction voxels and "normal" voxels.
	 * <p>
	 *
	 * numberOfEndPoints, numberOfJunctionVoxels and numberOfSlabs must exit
	 * and have at least numOfTrees fields each.
	 *
	 * @return An array with the number of voxels in every tree
	 */
	public int[] calculateNumberOfVoxels()
	{
		numberOfVoxels = new int[numOfTrees];
		for (int i = 0; i < numOfTrees; ++i) {
			numberOfVoxels[i] = numberOfEndPoints[i]
				+ numberOfJunctionVoxels[i]
				+ numberOfSlabs[i];
		}

		return numberOfVoxels;
	}

}
