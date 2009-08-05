import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * AnalyzeSkeleton_ plugin for ImageJ(C).
 * Copyright (C) 2008,2009 Ignacio Arganda-Carreras 
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
 * Main class.
 * This class is a plugin for the ImageJ interface for analyzing
 * 2D/3D skeleton images.
 * <p>
 * For more information, visit the AnalyzeSkeleton_ homepage:
 * http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:analyzeskeleton:start
 *
 *
 * @version 1.0 08/05/2009
 * @author Ignacio Arganda-Carreras <ignacio.arganda@gmail.com>
 *
 */
public class AnalyzeSkeleton_ implements PlugInFilter
{
	/** end point flag */
	public static byte END_POINT = 30;
	/** junction flag */
	public static byte JUNCTION = 70;
	/** slab flag */
	public static byte SLAB = 127;
	
	/** working image plus */
	private ImagePlus imRef;

	/** working image width */
	private int width = 0;
	/** working image height */
	private int height = 0;
	/** working image depth */
	private int depth = 0;
	/** working image stack*/
	private ImageStack inputImage = null;
	
	/** visit flags */
	private boolean [][][] visited = null;
	
	// Measures
	/** total number of end points voxels */
	private int totalNumberOfEndPoints = 0;
	/** total number of junctions voxels */
	private int totalNumberOfJunctionVoxels = 0;
	/** total number of slab voxels */
	private int totalNumberOfSlabs = 0;	
	
	// Tree fields
	/** number of branches for every specific tree */
	private int[] numberOfBranches = null;
	/** number of end points voxels of every tree */
	private int[] numberOfEndPoints = null;
	/** number of junctions voxels of every tree*/
	private int[] numberOfJunctionVoxels = null;
	/** number of slab voxels of every specific tree */
	private int[] numberOfSlabs = null;	
	/** number of junctions of every specific tree*/
	private int[] numberOfJunctions = null;
	/** number of triple points in every tree */
	private int[] numberOfTriplePoints = null;
	/** list of end points in every tree */
	private ArrayList <int[]> endPointsTree [] = null;
	/** list of junction voxels in every tree */
	private ArrayList <int[]> junctionVoxelTree [] = null;
	/** list of special slab coordinates where circular tree starts */
	private ArrayList <int[]> startingSlabTree [] = null;
	
	/** average branch length */
	private double[] averageBranchLength = null;
	
	/** maximum branch length */
	private double[] maximumBranchLength = null;
	
	/** list of end point coordinates in the entire image */
	private ArrayList <int[]> listOfEndPoints = new ArrayList<int[]>();
	/** list of junction coordinates in the entire image */
	private ArrayList <int[]> listOfJunctionVoxels = new ArrayList<int[]>();
	/** list of slab coordinates in the entire image */
	private ArrayList <int[]> listOfSlabVoxels = new ArrayList<int[]>();
	/** list of slab coordinates in the entire image */
	private ArrayList <int[]> listOfStartingSlabVoxels = new ArrayList<int[]>();
	
	/** list of groups of junction voxels that belong to the same tree junction (in every tree) */
	private ArrayList < ArrayList <int[]> > listOfSingleJunctions[] = null;
	
	/** stack image containing the corresponding skeleton tags (end point, junction or slab) */
	private ImageStack taggedImage = null;
	
	/** auxiliary temporary point */
	private int[] auxPoint = null;
	/** largest branch coordinates initial point */
	private int[][] initialPoint = null;
	/** largest branch coordinates final point */
	private int[][] finalPoint = null;
	
	/** number of trees (skeletons) in the image */
	private int numOfTrees = 0;
		
	
	/* -----------------------------------------------------------------------*/
	/**
	 * This method is called once when the filter is loaded.
	 * 
	 * @param arg argument specified for this plugin
	 * @param imp currently active image
	 * @return flag word that specifies the filters capabilities
	 */
	public int setup(String arg, ImagePlus imp) 
	{
		this.imRef = imp;
		
		if (arg.equals("about")) 
		{
			showAbout();
			return DONE;
		}

		return DOES_8G; 
	} /* end setup */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Process the image: tag skeleton and show results.
	 * 
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	public void run(ImageProcessor ip) 
	{
		
		this.width = this.imRef.getWidth();
		this.height = this.imRef.getHeight();
		this.depth = this.imRef.getStackSize();
		this.inputImage = this.imRef.getStack();
		
		// initialize visit flags
		this.visited = new boolean[this.width][this.height][this.depth];
		
							
		// Prepare data: classify voxels and tag them.
		this.taggedImage = tagImage(this.inputImage);		
		
		// Show tags image.
		ImagePlus tagIP = new ImagePlus("Tagged skeleton", taggedImage);
		tagIP.show();
		
		// Set same calibration as the input image
		tagIP.setCalibration(this.imRef.getCalibration());
		
		// We apply the Fire LUT and reset the min and max to be between 0-255.
		IJ.run("Fire");
		
		//IJ.resetMinAndMax();
		tagIP.resetDisplayRange();
		tagIP.updateAndDraw();
		
		// Mark trees
		ImageStack treeIS = markTrees(taggedImage);
		
		// Ask memory for every tree
		this.numberOfBranches = new int[this.numOfTrees];
		this.numberOfEndPoints = new int[this.numOfTrees];
		this.numberOfJunctionVoxels = new int[this.numOfTrees];
		this.numberOfJunctions = new int[this.numOfTrees];
		this.numberOfSlabs = new int[this.numOfTrees];
		this.numberOfTriplePoints = new int[this.numOfTrees];
		this.averageBranchLength = new double[this.numOfTrees];
		this.maximumBranchLength = new double[this.numOfTrees];
		this.initialPoint = new int[this.numOfTrees][];
		this.finalPoint = new int[this.numOfTrees][];
		this.endPointsTree = new ArrayList[this.numOfTrees];		
		this.junctionVoxelTree = new ArrayList[this.numOfTrees];
		this.startingSlabTree = new ArrayList[this.numOfTrees];
		this.listOfSingleJunctions = new ArrayList[this.numOfTrees];
		for(int i = 0; i < this.numOfTrees; i++)
		{
			this.endPointsTree[i] = new ArrayList <int[]>();
			this.junctionVoxelTree[i] = new ArrayList <int[]>();
			this.startingSlabTree[i] = new ArrayList <int[]>();
			this.listOfSingleJunctions[i] = new ArrayList < ArrayList <int[]> > ();
		}
		                                      
		// Divide groups of end-points and junction voxels
		if(this.numOfTrees > 1)
			divideVoxelsByTrees(treeIS);
		else
		{
			this.endPointsTree[0] = this.listOfEndPoints;
			this.junctionVoxelTree[0] = this.listOfJunctionVoxels;
		}
		
		// Visit skeleton and measure distances.
		for(int i = 0; i < this.numOfTrees; i++)
			visitSkeleton(taggedImage, treeIS, i+1);
		
		// Calculate number of junctions (skipping neighbor junction voxels)
		groupJunctions(treeIS);
		
		// Calculate triple points (junctions with exactly 3 branches)
		calculateTriplePoints();
		
		// Show results table
		showResults();
		
	} /* end run */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Divide end point, junction and special (starting) slab voxels in the 
	 * corresponding tree lists
	 * 
	 *  @param treeIS tree image
	 */
	private void divideVoxelsByTrees(ImageStack treeIS) 
	{
		// Add end points to the corresponding tree
		for(int i = 0; i < this.totalNumberOfEndPoints; i++)
		{
			final int[] p = this.listOfEndPoints.get(i);
			this.endPointsTree[getShortPixel(treeIS, p) - 1].add(p);
		}
		
		// Add junction voxels to the corresponding tree
		for(int i = 0; i < this.totalNumberOfJunctionVoxels; i++)
		{
			final int[] p = this.listOfJunctionVoxels.get(i);			
			this.junctionVoxelTree[getShortPixel(treeIS, p) - 1].add(p);
		}
		
		// Add special slab voxels to the corresponding tree
		for(int i = 0; i < this.listOfStartingSlabVoxels.size(); i++)
		{
			final int[] p = this.listOfStartingSlabVoxels.get(i);			
			this.startingSlabTree[getShortPixel(treeIS, p) - 1].add(p);
		}
		
	} // end divideVoxelsByTrees

	/* -----------------------------------------------------------------------*/
	/**
	 * Show results table
	 */
	private void showResults() 
	{
		ResultsTable rt = new ResultsTable();
		
		String[] head = {"Skeleton", "# Branches","# Junctions", "# End-point voxels",
						 "# Junction voxels","# Slab voxels","Average Branch Length", 
						 "# Triple points", "Maximum Branch Length"};
		
		for (int i = 0; i < head.length; i++)
			rt.setHeading(i,head[i]);	
		
		for(int i = 0 ; i < this.numOfTrees; i++)
		{
			rt.incrementCounter();

			rt.addValue(1, this.numberOfBranches[i]);        
			rt.addValue(2, this.numberOfJunctions[i]);
			rt.addValue(3, this.numberOfEndPoints[i]);
			rt.addValue(4, this.numberOfJunctionVoxels[i]);
			rt.addValue(5, this.numberOfSlabs[i]);
			rt.addValue(6, this.averageBranchLength[i]);
			rt.addValue(7, this.numberOfTriplePoints[i]);
			rt.addValue(8, this.maximumBranchLength[i]);

			if (0 == i % 100) rt.show("Results");
			
			StringBuilder sb = new StringBuilder();

			sb.append("--- Skeleton #").append(i+1).append(" ---\n")
			  	.append("Coordinates of the largest branch:\n")
				.append("Initial point: (").append(this.initialPoint[i][0] * this.imRef.getCalibration().pixelWidth).append( ", ") 
				.append(this.initialPoint[i][1] * this.imRef.getCalibration().pixelHeight).append( ", ")
				.append(this.initialPoint[i][2] * this.imRef.getCalibration().pixelDepth).append(")\n")
				.append("Final point: (").append(this.finalPoint[i][0] * this.imRef.getCalibration().pixelWidth).append(", ") 
				.append(this.finalPoint[i][1] * this.imRef.getCalibration().pixelHeight).append(", ")
				.append(this.finalPoint[i][2] * this.imRef.getCalibration().pixelDepth).append( ")\n" )
				.append("Euclidean distance: ").append( this.calculateDistance(this.initialPoint[i], this.finalPoint[i]));
			IJ.log(sb.toString());
		}
		rt.show("Results");
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Visit skeleton from end points and register measures.
	 * 
	 * @param taggedImage
	 * 
	 * @deprecated
	 */
	private void visitSkeleton(ImageStack taggedImage) 
	{
	
		// length of branches
		double branchLength = 0;		
		int numberOfBranches = 0;
		double maximumBranchLength = 0;
		double averageBranchLength = 0;
		int[] initialPoint = null;
		int[] finalPoint = null;
		
	
		// Visit branches starting at end points
		for(int i = 0; i < this.totalNumberOfEndPoints; i++)
		{			
			int[] endPointCoord = this.listOfEndPoints.get(i);
					 
			// visit branch until next junction or end point.
			double length = visitBranch(endPointCoord);
						
			if(length == 0)						
				continue;			
			
			// increase number of branches
			numberOfBranches++;
			branchLength += length;				
			
			// update maximum branch length
			if(length > maximumBranchLength)
			{
				maximumBranchLength = length;
				initialPoint = endPointCoord;
				finalPoint = this.auxPoint;
			}
		}
		
	
		// Now visit branches starting at junctions
		for(int i = 0; i < this.totalNumberOfJunctionVoxels; i++)
		{
			int[] junctionCoord = this.listOfJunctionVoxels.get(i);
			
			// Mark junction as visited
			setVisited(junctionCoord, true);
					
			int[] nextPoint = getNextUnvisitedVoxel(junctionCoord);
			
			while(nextPoint != null)
			{
				branchLength += calculateDistance(junctionCoord, nextPoint);								
								
				double length = visitBranch(nextPoint);
				
				branchLength += length;
				
				// Increase number of branches
				if(length != 0)
				{
					numberOfBranches++;
					// update maximum branch length
					if(length > maximumBranchLength)
					{
						maximumBranchLength = length;
						initialPoint = junctionCoord;
						finalPoint = this.auxPoint;
					}
				}
				
				nextPoint = getNextUnvisitedVoxel(junctionCoord);
			}					
		}

		// Average length
		averageBranchLength = branchLength / numberOfBranches;
		
	} /* end visitSkeleton */
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Visit skeleton from end points and register measures.
	 * 
	 * @param taggedImage
	 * @param treeImage skeleton image with tree classification
	 * @param currentTree number of the tree to be visited
	 */
	private void visitSkeleton(ImageStack taggedImage, ImageStack treeImage, int currentTree) 
	{
	
		// IJ.log(" Analyzing tree number " + currentTree);
		// length of branches
		double branchLength = 0;
		
		// tree index
		final int iTree = currentTree - 1;
		
		this.maximumBranchLength[iTree] = 0;
		this.numberOfEndPoints[iTree] = this.endPointsTree[iTree].size();
		this.numberOfJunctionVoxels[iTree] = this.junctionVoxelTree[iTree].size();
		this.numberOfSlabs[iTree] = 0;
	
		// Visit branches starting at end points
		for(int i = 0; i < this.numberOfEndPoints[iTree]; i++)
		{			
			final int[] endPointCoord = this.endPointsTree[iTree].get(i);
			
			// Skip when visited
			if(isVisited(endPointCoord))
			{
				//if(this.initialPoint[iTree] == null)
				//	IJ.error("WEIRD:" + " (" + endPointCoord[0] + ", " + endPointCoord[1] + ", " + endPointCoord[2] + ")");
				//IJ.log("visited = (" + endPointCoord[0] + ", " + endPointCoord[1] + ", " + endPointCoord[2] + ")");
				continue;
			}
					 
			// Otherwise, visit branch until next junction or end point.
			final double length = visitBranch(endPointCoord, iTree);
						
			// If length is 0, it means the tree is formed by only one voxel.
			if(length == 0)
			{
				//IJ.log("set initial point to final point");
				this.initialPoint[iTree] = this.finalPoint[iTree] = endPointCoord;
				continue;
			}
			
			// increase number of branches
			this.numberOfBranches[iTree]++;
			branchLength += length;				
			
			// update maximum branch length
			if(length > this.maximumBranchLength[iTree])
			{
				this.maximumBranchLength[iTree] = length;
				this.initialPoint[iTree] = endPointCoord;
				this.finalPoint[iTree] = this.auxPoint;
			}
		}
		
	
		// Now visit branches starting at junctions
		for(int i = 0; i < this.numberOfJunctionVoxels[iTree]; i++)
		{
			final int[] junctionCoord = this.junctionVoxelTree[iTree].get(i);					
			
			// Mark junction as visited
			setVisited(junctionCoord, true);
					
			int[] nextPoint = getNextUnvisitedVoxel(junctionCoord);
			
			while(nextPoint != null)
			{
				branchLength += calculateDistance(junctionCoord, nextPoint);								
								
				double length = visitBranch(nextPoint, iTree);
				
				branchLength += length;
				
				// Increase number of branches
				if(length != 0)
				{
					this.numberOfBranches[iTree]++;
					// update maximum branch length
					if(length > this.maximumBranchLength[iTree])
					{
						this.maximumBranchLength[iTree] = length;
						this.initialPoint[iTree] = junctionCoord;
						this.finalPoint[iTree] = this.auxPoint;
					}
				}
				
				nextPoint = getNextUnvisitedVoxel(junctionCoord);
			}					
		}
		
		// Finally visit branches starting at slabs (special case for circular trees)
		if(this.startingSlabTree[iTree].size() == 1)
		{
			final int[] startCoord = this.startingSlabTree[iTree].get(0);					
			
			// visit branch until finding visited voxel.
			final double length = visitBranch(startCoord, iTree);
						
			if(length != 0)
			{				
				// increase number of branches
				this.numberOfBranches[iTree]++;
				branchLength += length;				
				
				// update maximum branch length
				if(length > this.maximumBranchLength[iTree])
				{
					this.maximumBranchLength[iTree] = length;
					this.initialPoint[iTree] = startCoord;
					this.finalPoint[iTree] = this.auxPoint;
				}
			}
		}						

		if(this.numberOfBranches[iTree] == 0)
			return;
		// Average length
		this.averageBranchLength[iTree] = branchLength / this.numberOfBranches[iTree];
		
	} /* end visitSkeleton */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Color the different trees in the skeleton.
	 * 
	 * @param taggedImage
	 * 
	 * @return image with every tree tagged with a different number 
	 */
	private ImageStack markTrees(ImageStack taggedImage) 
	{
		// Create output image
		ImageStack outputImage = new ImageStack(this.width, this.height, taggedImage.getColorModel());	
		for (int z = 0; z < depth; z++)
		{
			outputImage.addSlice(taggedImage.getSliceLabel(z+1), new ShortProcessor(this.width, this.height));	
		}
	
		this.numOfTrees = 0;
				
		short color = 0;
	
		// Visit trees starting at end points
		for(int i = 0; i < this.totalNumberOfEndPoints; i++)
		{			
			int[] endPointCoord = this.listOfEndPoints.get(i);
			
			if(isVisited(endPointCoord))
				continue;
			
			color++;
			
			if(color == Short.MAX_VALUE)
			{
				IJ.error("More than " + (Short.MAX_VALUE-1) +
						" skeletons in the image. AnalyzeSkeleton can only process up to "+ (Short.MAX_VALUE-1));
				return null;
			}
		
			// else, visit the entire tree.
			int length = visitTree(endPointCoord, outputImage, color);
						
			//if(length == 0)
			//{
			//	color--; // the color was not used
			//	continue;
			//}
			
			// increase number of trees			
			this.numOfTrees++;
		}
		
		// Visit trees starting at junction points 
		// (some circular trees do not have end points)
		// Visit trees starting at end points
		for(int i = 0; i < this.totalNumberOfJunctionVoxels; i++)
		{			
			int[] junctionCoord = this.listOfJunctionVoxels.get(i);
			if(isVisited(junctionCoord))
				continue;
			
			color++;
			
			if(color == Short.MAX_VALUE)
			{
				IJ.error("More than " + (Short.MAX_VALUE-1) + " skeletons in the image. AnalyzeSkeleton can only process up to 255");
				return null;
			}
		
			// else, visit branch until next junction or end point.
			int length = visitTree(junctionCoord, outputImage, color);
						
			if(length == 0)
			{
				color--; // the color was not used
				continue;
			}
			
			// increase number of trees			
			this.numOfTrees++;
		}
		
		// Check for unvisited slab voxels
		// (just in case there are circular trees without junctions)
		for(int i = 0; i < this.listOfSlabVoxels.size(); i++)
		{
			int[] p = (int[]) this.listOfSlabVoxels.get(i);
			if(isVisited(p) == false)
			{
				// Mark that voxel as the start point of the circular skeleton
				this.listOfStartingSlabVoxels.add(p);
				
				color++;
				
				if(color == Short.MAX_VALUE)
				{
					IJ.error("More than " + (Short.MAX_VALUE-1) + " skeletons in the image. AnalyzeSkeleton can only process up to 255");
					return null;
				}
			
				// else, visit branch until next junction or end point.
				int length = visitTree(p, outputImage, color);
							
				if(length == 0)
				{
					color--; // the color was not used
					continue;
				}
				
				// increase number of trees			
				this.numOfTrees++;
			}
		}
		
		
		
		//System.out.println("Number of trees = " + this.numOfTrees);

		// Show tree image.
		/*
		ImagePlus treesIP = new ImagePlus("Trees skeleton", outputImage);
		treesIP.show();
		
		// Set same calibration as the input image
		treesIP.setCalibration(this.imRef.getCalibration());
		
		// We apply the Fire LUT and reset the min and max to be between 0-255.
		IJ.run("Fire");
		
		//IJ.resetMinAndMax();
		treesIP.resetDisplayRange();
		treesIP.updateAndDraw();
		*/
		
		// Reset visited variable
		this.visited = null;
		this.visited = new boolean[this.width][this.height][this.depth];

		
		//IJ.log("Number of trees: " + this.numOfTrees + ", # colors = " + color);
		
		return outputImage;
		
	} /* end markTrees */

	
	/* --------------------------------------------------------------*/
	/**
	 * Visit tree marking the voxels with a reference tree color
	 * 
	 * @param startingPoint starting tree point
	 * @param outputImage 3D image to visit
	 * @param color reference tree color
	 * @return number of voxels in the tree
	 */
	private int visitTree(int[] startingPoint, ImageStack outputImage,
			short color) 
	{
		int numOfVoxels = 0;
		
		//IJ.log("visiting " + pointToString(startingPoint) + " color = " + color);
		
		if(isVisited(startingPoint))	
			return 0;
		// Set pixel color
		this.setPixel(outputImage, startingPoint[0], startingPoint[1], startingPoint[2], color);
		setVisited(startingPoint, true);
		
		ArrayList <int[]> toRevisit = new ArrayList <int []>();
		
		int[] nextPoint = getNextUnvisitedVoxel(startingPoint);
		
		while(nextPoint != null || toRevisit.size() != 0)
		{
			if(nextPoint != null)
			{
				if(!isVisited(nextPoint))
				{
					numOfVoxels++;
					
					//IJ.log("visiting " + pointToString(nextPoint)+ " color = " + color);
					
					// Set color and visit flat
					this.setPixel(outputImage, nextPoint[0], nextPoint[1], nextPoint[2], color);
					setVisited(nextPoint, true);
					
					// If it is a junction, add it to the revisit list
					if(isJunction(nextPoint))
						toRevisit.add(nextPoint);
					
					// Calculate next point to visit
					nextPoint = getNextUnvisitedVoxel(nextPoint);
				}				
			}
			else // revisit list
			{				
				nextPoint = toRevisit.get(0);
				//IJ.log("visiting " + pointToString(nextPoint)+ " color = " + color);
												
				// Calculate next point to visit
				nextPoint = getNextUnvisitedVoxel(nextPoint);
				// Maintain junction in the list until there is no more branches
				if (nextPoint == null)
					toRevisit.remove(0);									
			}				
		}
		
		return numOfVoxels;
	} // end method visitTree

	/* -----------------------------------------------------------------------*/
	/**
	 * Visit a branch and calculate length
	 * 
	 * @param startingPoint starting coordinates
	 * @return branch length
	 * 
	 * @deprecated
	 */
	private double visitBranch(int[] startingPoint) 
	{
		double length = 0;
		
		// mark starting point as visited
		setVisited(startingPoint, true);
		
		// Get next unvisited voxel
		int[] nextPoint = getNextUnvisitedVoxel(startingPoint);
		
		if (nextPoint == null)
			return 0;
		
		int[] previousPoint = startingPoint;
		
		// We visit the branch until we find an end point or a junction
		while(nextPoint != null && isSlab(nextPoint))
		{
			// Add length
			length += calculateDistance(previousPoint, nextPoint);
			
			// Mark as visited
			setVisited(nextPoint, true);
			
			// Move in the graph
			previousPoint = nextPoint;			
			nextPoint = getNextUnvisitedVoxel(previousPoint);			
		}
		
		
		if(nextPoint != null)
		{
			// Add distance to last point
			length += calculateDistance(previousPoint, nextPoint);
		
			// Mark last point as visited
			setVisited(nextPoint, true);
		}
		
		this.auxPoint = previousPoint;
		
		return length;
	} /* end visitBranch*/

	/* -----------------------------------------------------------------------*/
	/**
	 * Visit a branch and calculate length in a specifc tree
	 * 
	 * @param startingPoint starting coordinates
	 * @param iTree tree index
	 * @return branch length
	 */
	private double visitBranch(int[] startingPoint, int iTree) 
	{
		//IJ.log("startingPoint = (" + startingPoint[0] + ", " + startingPoint[1] + ", " + startingPoint[2] + ")");
		double length = 0;
		
		// mark starting point as visited
		setVisited(startingPoint, true);
		
		// Get next unvisited voxel
		int[] nextPoint = getNextUnvisitedVoxel(startingPoint);
		
		if (nextPoint == null)
			return 0;
		
		int[] previousPoint = startingPoint;
		
		// We visit the branch until we find an end point or a junction
		while(nextPoint != null && isSlab(nextPoint))
		{
			this.numberOfSlabs[iTree]++;
		
			// Add length
			length += calculateDistance(previousPoint, nextPoint);
			
			// Mark as visited
			setVisited(nextPoint, true);
			
			// Move in the graph
			previousPoint = nextPoint;			
			nextPoint = getNextUnvisitedVoxel(previousPoint);			
		}
		
		
		if(nextPoint != null)
		{
			// Add distance to last point
			length += calculateDistance(previousPoint, nextPoint);
		
			// Mark last point as visited
			setVisited(nextPoint, true);
		}
		
		this.auxPoint = previousPoint;
		
		//IJ.log("finalPoint = (" + nextPoint[0] + ", " + nextPoint[1] + ", " + nextPoint[2] + ")");
		return length;
	} /* end visitBranch*/	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Calculate distance between two points in 3D
	 * 
	 * @param point1 first point coordinates
	 * @param point2 second point coordinates
	 * @return distance (in the corresponding units)
	 */
	private double calculateDistance(int[] point1, int[] point2) 
	{		
		return Math.sqrt(  Math.pow( (point1[0] - point2[0]) * this.imRef.getCalibration().pixelWidth, 2) 
				          + Math.pow( (point1[1] - point2[1]) * this.imRef.getCalibration().pixelHeight, 2)
				          + Math.pow( (point1[2] - point2[2]) * this.imRef.getCalibration().pixelDepth, 2));
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Calculate number of junction skipping neighbor junction voxels
	 * 
	 * @param treeIS tree stack
	 */
	private void groupJunctions(ImageStack treeIS) 
	{
		// Reset visited variable
		this.visited = null;
		this.visited = new boolean[this.width][this.height][this.depth];
		
		for (int iTree = 0; iTree < this.numOfTrees; iTree++)
		{
			// Visit list of junction voxels
			for(int i = 0; i < this.numberOfJunctionVoxels[iTree]; i ++)
			{
				int[] pi = this.junctionVoxelTree[iTree].get(i);
				
				if(! isVisited(pi))
					fusionNeighborJunction(pi, this.listOfSingleJunctions[iTree]);
			}
		}		
				
		// Count number of single junctions for every tree in the image
		for (int iTree = 0; iTree < this.numOfTrees; iTree++)
		{
			this.numberOfJunctions[iTree] = this.listOfSingleJunctions[iTree].size();
		}
				
		
	}	

	// -----------------------------------------------------------------------
	/**
	 * 
	 * @param startingPoint
	 * @param singleJunctionsList
	 */
	private void fusionNeighborJunction(int[] startingPoint,
			ArrayList<ArrayList<int[]>> singleJunctionsList) 
	{
		// Create new group of junctions
		ArrayList <int[]> newGroup = new ArrayList<int[]>();
		newGroup.add(startingPoint);
		
		// Mark the starting junction as visited
		setVisited(startingPoint, true);
		
		// Look for neighbor junctions and add them to the new group
		ArrayList <int[]> toRevisit = new ArrayList <int []>();
		toRevisit.add(startingPoint);
		
		int[] nextPoint = getNextUnvisitedJunctionVoxel(startingPoint);
		
		while(nextPoint != null || toRevisit.size() != 0)
		{
			if(nextPoint != null && !isVisited(nextPoint))
			{			
				// Add to the group
				newGroup.add(nextPoint);
				// Mark as visited
				setVisited(nextPoint, true);

				// add it to the revisit list
				toRevisit.add(nextPoint);

				// Calculate next junction point to visit
				nextPoint = getNextUnvisitedJunctionVoxel(nextPoint);								
			}
			else // revisit list
			{				
				nextPoint = toRevisit.get(0);
				//IJ.log("visiting " + pointToString(nextPoint)+ " color = " + color);
												
				// Calculate next point to visit
				nextPoint = getNextUnvisitedJunctionVoxel(nextPoint);
				// Maintain junction in the list until there is no more branches
				if (nextPoint == null)
					toRevisit.remove(0);									
			}				
		}
		
		// Add group to the single junction list
		singleJunctionsList.add(newGroup);
		
	}

	// -----------------------------------------------------------------------
	/**
	 *  Check if two groups of voxels are neighbors
	 * @param g1 first group
	 * @param g2 second group
	 * 
	 * @return true if the groups have any neighbor voxel
	 */
	boolean checkNeighborGroups(ArrayList <int[]> g1,  ArrayList <int[]> g2)
	{
		for(int i =0 ; i  < g1.size(); i++)
		{
			int[] pi = g1.get(i);
			for(int j =0 ; j  < g2.size(); j++)
			{
				int[] pj = g2.get(j);
				if(isNeighbor(pi, pj))
					return true;
			}
		}			
		return false;
	}
	
	
	// -----------------------------------------------------------------------
	/**
	 * Calculate number of triple points in the skeleton. Triple points are
	 * junctions with exactly 3 branches.
	 */
	private void calculateTriplePoints() 
	{
		for (int iTree = 0; iTree < this.numOfTrees; iTree++)
		{			
			// Visit the groups of junction voxels
			for(int i = 0; i < this.numberOfJunctions[iTree]; i ++)
			{

				ArrayList <int[]> groupOfJunctions = this.listOfSingleJunctions[iTree].get(i);

				// Count the number of slab neighbors of every voxel in the group
				int nSlab = 0;
				for(int j = 0; j < groupOfJunctions.size(); j++)
				{
					int[] pj = groupOfJunctions.get(j);

					// Get neighbors and check the slabs
					byte[] neighborhood = this.getNeighborhood(this.taggedImage, pj[0], pj[1], pj[2]);
					for(int k = 0; k < 27; k++)
						if (neighborhood[k] == AnalyzeSkeleton_.SLAB)
							nSlab++;
				}
				// If the junction has only 3 slab neighbors, then it is a triple point
				if (nSlab == 3)	
					this.numberOfTriplePoints[iTree] ++;

			}		
				
		}
		
	}// end calculateTriplePoints
	

	/* -----------------------------------------------------------------------*/
	/**
	 * Calculate if two points are neighbors
	 * 
	 * @param point1 first point
	 * @param point2 second point
	 * @return true if the points are neighbors (26-pixel neighborhood)
	 */
	private boolean isNeighbor(int[] point1, int[] point2) 
	{		
		return Math.sqrt(  Math.pow( (point1[0] - point2[0]), 2) 
		          + Math.pow( (point1[1] - point2[1]), 2)
		          + Math.pow( (point1[2] - point2[2]), 2)) <= Math.sqrt(3);
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Check if the point is slab
	 *  
	 * @param point actual point
	 * @return true if the point has slab status
	 */
	private boolean isSlab(int[] point) 
	{		
		return getPixel(this.taggedImage, point[0], point[1], point[2]) == AnalyzeSkeleton_.SLAB;
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Check if the point is a junction
	 *  
	 * @param point actual point
	 * @return true if the point has slab status
	 */
	private boolean isJunction(int[] point) 
	{		
		return getPixel(this.taggedImage, point[0], point[1], point[2]) == AnalyzeSkeleton_.JUNCTION;
	}	
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Check if the point is a junction
	 *  
	 * @param x x- voxel coordinate
	 * @param y y- voxel coordinate
	 * @param z z- voxel coordinate
	 * @return true if the point has slab status
	 */
	private boolean isJunction(int x, int y, int z) 
	{		
		return getPixel(this.taggedImage, x, y, z) == AnalyzeSkeleton_.JUNCTION;
	}	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get next unvisited neighbor voxel 
	 * 
	 * @param point starting point
	 * @return unvisited neighbor or null if all neighbors are visited
	 */
	private int[] getNextUnvisitedVoxel(int[] point) 
	{
		int[] unvisitedNeighbor = null;

		// Check neighbors status
		for(int x = -1; x < 2; x++)
			for(int y = -1; y < 2; y++)
				for(int z = -1; z < 2; z++)
				{
					if(x == 0 && y == 0 && z == 0)
						continue;
					
					if(getPixel(this.inputImage, point[0] + x, point[1] + y, point[2] + z) != 0
						&& isVisited(point[0] + x, point[1] + y, point[2] + z) == false)						
					{					
						unvisitedNeighbor = new int[]{point[0] + x, point[1] + y, point[2] + z};
						break;
					}
					
				}
		
		return unvisitedNeighbor;
	}/* end getNextUnvisitedVoxel */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get next unvisited junction neighbor voxel 
	 * 
	 * @param point starting point
	 * @return unvisited neighbor or null if all neighbors are visited
	 */
	private int[] getNextUnvisitedJunctionVoxel(int[] point) 
	{
		int[] unvisitedNeighbor = null;

		// Check neighbors status
		for(int x = -1; x < 2; x++)
			for(int y = -1; y < 2; y++)
				for(int z = -1; z < 2; z++)
				{
					if(x == 0 && y == 0 && z == 0)
						continue;
					
					if(getPixel(this.inputImage, point[0] + x, point[1] + y, point[2] + z) != 0
						&& isVisited(point[0] + x, point[1] + y, point[2] + z) == false 
						&& isJunction(point[0] + x, point[1] + y, point[2] + z))						
					{					
						unvisitedNeighbor = new int[]{point[0] + x, point[1] + y, point[2] + z};
						break;
					}
					
				}
		
		return unvisitedNeighbor;
	}// end getNextUnvisitedJunctionVoxel 

	/* -----------------------------------------------------------------------*/
	/**
	 * Check if a voxel is visited taking into account the borders. 
	 * Out of range voxels are considered as visited. 
	 * 
	 * @param point
	 * @return true if the voxel is visited
	 */
	private boolean isVisited(int [] point) 
	{
		return isVisited(point[0], point[1], point[2]);
	}
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Check if a voxel is visited taking into account the borders. 
	 * Out of range voxels are considered as visited. 
	 * 
	 * @param x x- voxel coordinate
	 * @param y y- voxel coordinate
	 * @param z z- voxel coordinate
	 * @return true if the voxel is visited
	 */
	private boolean isVisited(int x, int y, int z) 
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			return this.visited[x][y][z];
		return true;
	}
	

	/* -----------------------------------------------------------------------*/
	/**
	 * Set value in the visited flags matrix
	 * 
	 * @param x x- voxel coordinate
	 * @param y y- voxel coordinate
	 * @param z z- voxel coordinate
	 * @param b
	 */
	private void setVisited(int x, int y, int z, boolean b) 
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			this.visited[x][y][z] = b;		
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Set value in the visited flags matrix
	 * 
	 * @param point voxel coordinates
	 * @param b visited flag value
	 */
	private void setVisited(int[] point, boolean b) 
	{
		int x = point[0];
		int y = point[1];
		int z = point[2];
		
 		setVisited(x, y, z, b);	
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Tag skeleton dividing the voxels between end points, junctions and slab,
	 *  
	 * @param inputImage2 skeleton image to be tagged
	 * @return tagged skeleton image
	 */
	private ImageStack tagImage(ImageStack inputImage2) 
	{
		// Create output image
		ImageStack outputImage = new ImageStack(this.width, this.height, inputImage2.getColorModel());
			
		// Tag voxels
		for (int z = 0; z < depth; z++)
		{
			outputImage.addSlice(inputImage2.getSliceLabel(z+1), new ByteProcessor(this.width, this.height));			
			for (int x = 0; x < width; x++) 
				for (int y = 0; y < height; y++)
				{
					if(getPixel(inputImage2, x, y, z) != 0)
					{
						int numOfNeighbors = getNumberOfNeighbors(inputImage2, x, y, z);
						if(numOfNeighbors < 2)
						{
							setPixel(outputImage, x, y, z, AnalyzeSkeleton_.END_POINT);
							this.totalNumberOfEndPoints++;
							int[] endPoint = new int[]{x, y, z};
							this.listOfEndPoints.add(endPoint);							
						}
						else if(numOfNeighbors > 2)
						{
							setPixel(outputImage, x, y, z, AnalyzeSkeleton_.JUNCTION);
							int[] junction = new int[]{x, y, z};
							this.listOfJunctionVoxels.add(junction);	
							this.totalNumberOfJunctionVoxels++;
						}
						else
						{
							setPixel(outputImage, x, y, z, AnalyzeSkeleton_.SLAB);
							int[] slab = new int[]{x, y, z};
							this.listOfSlabVoxels.add(slab);
							this.totalNumberOfSlabs++;
						}
					}					
				}
		}
		
		return outputImage;
	}/* end tagImage */

	/* -----------------------------------------------------------------------*/
	/**
	 * Get number of neighbors of a voxel in a 3D image (0 border conditions) 
	 * 
	 * @param image 3D image (ImageStack)
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	private int getNumberOfNeighbors(ImageStack image, int x, int y, int z)
	{
		int n = 0;
		byte[] neighborhood = getNeighborhood(image, x, y, z);
		
		for(int i = 0; i < 27; i ++)
			if(neighborhood[i] != 0)
				n++;
		// We return n-1 because neighborhood includes the actual voxel.
		return (n-1);			
	}
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions) 
	 * 
	 * @param image 3D image (ImageStack)
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	private byte[] getNeighborhood(ImageStack image, int x, int y, int z)
	{
		byte[] neighborhood = new byte[27];
		
		neighborhood[ 0] = getPixel(image, x-1, y-1, z-1);
		neighborhood[ 1] = getPixel(image, x  , y-1, z-1);
		neighborhood[ 2] = getPixel(image, x+1, y-1, z-1);
		
		neighborhood[ 3] = getPixel(image, x-1, y,   z-1);
		neighborhood[ 4] = getPixel(image, x,   y,   z-1);
		neighborhood[ 5] = getPixel(image, x+1, y,   z-1);
		
		neighborhood[ 6] = getPixel(image, x-1, y+1, z-1);
		neighborhood[ 7] = getPixel(image, x,   y+1, z-1);
		neighborhood[ 8] = getPixel(image, x+1, y+1, z-1);
		
		neighborhood[ 9] = getPixel(image, x-1, y-1, z  );
		neighborhood[10] = getPixel(image, x,   y-1, z  );
		neighborhood[11] = getPixel(image, x+1, y-1, z  );
		
		neighborhood[12] = getPixel(image, x-1, y,   z  );
		neighborhood[13] = getPixel(image, x,   y,   z  );
		neighborhood[14] = getPixel(image, x+1, y,   z  );
		
		neighborhood[15] = getPixel(image, x-1, y+1, z  );
		neighborhood[16] = getPixel(image, x,   y+1, z  );
		neighborhood[17] = getPixel(image, x+1, y+1, z  );
		
		neighborhood[18] = getPixel(image, x-1, y-1, z+1);
		neighborhood[19] = getPixel(image, x,   y-1, z+1);
		neighborhood[20] = getPixel(image, x+1, y-1, z+1);
		
		neighborhood[21] = getPixel(image, x-1, y,   z+1);
		neighborhood[22] = getPixel(image, x,   y,   z+1);
		neighborhood[23] = getPixel(image, x+1, y,   z+1);
		
		neighborhood[24] = getPixel(image, x-1, y+1, z+1);
		neighborhood[25] = getPixel(image, x,   y+1, z+1);
		neighborhood[26] = getPixel(image, x+1, y+1, z+1);
		
		return neighborhood;
	} /* end getNeighborhood */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get pixel in 3D image (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */
	private byte getPixel(ImageStack image, int x, int y, int z)
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			return ((byte[]) image.getPixels(z + 1))[x + y * this.width];
		else return 0;
	} /* end getPixel */
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get pixel in 3D image (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */
	private short getShortPixel(ImageStack image, int x, int y, int z)
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			return ((short[]) image.getPixels(z + 1))[x + y * this.width];
		else return 0;
	} /* end getShortPixel */

	/* -----------------------------------------------------------------------*/
	/**
	 * Get pixel in 3D image (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param point point to be evaluated
	 * @return corresponding pixel (0 if out of image)
	 */
	private short getShortPixel(ImageStack image, int [] point)
	{
		return getShortPixel(image, point[0], point[1], point[2]);
	} /* end getPixel */	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Get pixel in 3D image (0 border conditions) 
	 * 
	 * @param image 3D image
	 * @param point point to be evaluated
	 * @return corresponding pixel (0 if out of image)
	 */
	private byte getPixel(ImageStack image, int [] point)
	{
		return getPixel(image, point[0], point[1], point[2]);
	} /* end getPixel */
	
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Set pixel in 3D image 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @param value pixel value
	 */
	private void setPixel(ImageStack image, int x, int y, int z, byte value)
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			((byte[]) image.getPixels(z + 1))[x + y * this.width] = value;
	} /* end getPixel */

	/* -----------------------------------------------------------------------*/
	/**
	 * Set pixel in 3D (short) image 
	 * 
	 * @param image 3D image
	 * @param x x- coordinate
	 * @param y y- coordinate
	 * @param z z- coordinate (in image stacks the indexes start at 1)
	 * @param value pixel value
	 */
	private void setPixel(ImageStack image, int x, int y, int z, short value)
	{
		if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
			((short[]) image.getPixels(z + 1))[x + y * this.width] = value;
	} /* end getPixel */	
	
	
	/**
	 * 
	 */
	String pointToString(int[] p)
	{
		return new String("(" + p[0] + ", " + p[1] + ", " + p[2] + ")");
	}
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Show plug-in information.
	 * 
	 */
	void showAbout() 
	{
		IJ.showMessage(
						"About AnalyzeSkeleton...",
						"This plug-in filter analyzes a 2D/3D image skeleton.\n");
	} /* end showAbout */

}
