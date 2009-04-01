import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * AnalyzeSkeleton_ plugin for ImageJ(C).
 * Copyright (C) 2008 Ignacio Arganda-Carreras 
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
 * @version 1.0 03/05/2009
 * @author Ignacio Arganda-Carreras <ignacio.arganda@uam.es>
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
	/** number of end points voxels */
	private int numberOfEndPoints = 0;
	/** number of junctions voxels */
	private int numberOfJunctionVoxels = 0;
	/** number of slab voxels */
	private int numberOfSlabs = 0;	
	/** number of branches */
	private int numberOfBranches = 0;
	/** number of junctions */
	private int numberOfJunctions = 0;
	/** number of triple points */
	private int numberOfTriplePoints = 0;
	
	/** average branch length */
	private double averageBranchLength = 0;
	
	/** maximum branch length */
	private double maximumBranchLength = 0;
	
	/** list of end point coordinates */
	private ArrayList <int[]> listOfEndPoints = new ArrayList<int[]>();
	/** list of junction coordinates */
	private ArrayList <int[]> listOfJunctionVoxels = new ArrayList<int[]>();
	/** list of groups of junction voxels that belong to the same tree junction */
	private ArrayList < ArrayList <int[]> > listOfSingleJunctions = new ArrayList < ArrayList <int[]> >();
	
	/** stack image containing the corresponding skeleton tags (end point, junction or slab) */
	private ImageStack taggedImage = null;
	
	/** auxiliary temporary point */
	private int[] auxPoint = null;
	/** largest branch coordinates initial point */
	private int[] initialPoint = null;
	/** largest branch coordinates final point */
	private int[] finalPoint = null;
		
	
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
		//markTrees(taggedImage);
		
		// Visit skeleton and measure distances.
		visitSkeleton(taggedImage);
		
		// Calculate number of junctions (skipping neighbor junction voxels)
		groupJunctions();
		
		// Calculate triple points (junctions with exactly 3 branches)
		calculateTriplePoints();
		
		// Show results table
		showResults();
		
	} /* end run */
	
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
		
		rt.incrementCounter();
		
        rt.addValue(1, this.numberOfBranches);        
        rt.addValue(2, this.numberOfJunctions);
        rt.addValue(3, this.numberOfEndPoints);
        rt.addValue(4, this.numberOfJunctionVoxels);
        rt.addValue(5, this.numberOfSlabs);
        rt.addValue(6, this.averageBranchLength);
        rt.addValue(7, this.numberOfTriplePoints);
        rt.addValue(8, this.maximumBranchLength);
	
		rt.show("Results");
		
		IJ.log("Coordinates of the largest branch:");
		IJ.log("Initial point: (" + (this.initialPoint[0] * this.imRef.getCalibration().pixelWidth) + ", " 
				+ (this.initialPoint[1] * this.imRef.getCalibration().pixelHeight) + ", "
				+ (this.initialPoint[2] * this.imRef.getCalibration().pixelDepth) + ")" );
		IJ.log("Final point: (" + (this.finalPoint[0] * this.imRef.getCalibration().pixelWidth) + ", " 
				+ (this.finalPoint[1] * this.imRef.getCalibration().pixelHeight) + ", "
				+ (this.finalPoint[2] * this.imRef.getCalibration().pixelDepth) + ")" );
		IJ.log("Euclidean distance: " + this.calculateDistance(this.initialPoint, this.finalPoint));
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Visit skeleton from end points and register measures.
	 * 
	 * @param taggedImage
	 */
	private void visitSkeleton(ImageStack taggedImage) 
	{
	
		// length of branches
		double branchLength = 0;
	
		// Visit branches starting at end points
		for(int i = 0; i < this.numberOfEndPoints; i++)
		{			
			int[] endPointCoord = this.listOfEndPoints.get(i);
					 
			// else, visit branch until next junction or end point.
			double length = visitBranch(endPointCoord);
						
			if(length == 0)
				continue;
			
			// increase number of branches
			this.numberOfBranches++;
			branchLength += length;				
			
			// update maximum branch length
			if(length > this.maximumBranchLength)
			{
				this.maximumBranchLength = length;
				this.initialPoint = endPointCoord;
				this.finalPoint = this.auxPoint;
			}
		}
		
	
		// Now visit branches starting at junctions
		for(int i = 0; i < this.numberOfJunctionVoxels; i++)
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
					this.numberOfBranches++;
					// update maximum branch length
					if(length > this.maximumBranchLength)
					{
						this.maximumBranchLength = length;
						this.initialPoint = junctionCoord;
						this.finalPoint = this.auxPoint;
					}
				}
				
				nextPoint = getNextUnvisitedVoxel(junctionCoord);
			}					
		}

		// Average length
		this.averageBranchLength = branchLength / this.numberOfBranches;
		
	} /* end visitSkeleton */
	
	/* -----------------------------------------------------------------------*/
	/**
	 * Color the different trees in the skeleton.
	 * 
	 * @param taggedImage
	 */
	private void markTrees(ImageStack taggedImage) 
	{
		// Create output image
		ImageStack outputImage = new ImageStack(this.width, this.height, taggedImage.getColorModel());	
		for (int z = 0; z < depth; z++)
		{
			outputImage.addSlice(taggedImage.getSliceLabel(z+1), new ByteProcessor(this.width, this.height));	
		}
	
		int numOfTrees = 0;
	
		// Visit trees starting at end points
		for(int i = 0; i < this.numberOfEndPoints; i++)
		{			
			int[] endPointCoord = this.listOfEndPoints.get(i);
			
			byte color = (byte) (255 * Math.random());
		
			// else, visit branch until next junction or end point.
			int length = visitTree(endPointCoord, outputImage, color);
						
			if(length == 0)
				continue;
			
			// increase number of branches
			numOfTrees++;
		}
		
		System.out.println("Number of trees = " + numOfTrees);

		// Show tags image.
		ImagePlus treesIP = new ImagePlus("Trees skeleton", outputImage);
		treesIP.show();
		
		// Set same calibration as the input image
		treesIP.setCalibration(this.imRef.getCalibration());
		
		// We apply the Fire LUT and reset the min and max to be between 0-255.
		IJ.run("Fire");
		
		//IJ.resetMinAndMax();
		treesIP.resetDisplayRange();
		treesIP.updateAndDraw();

		// Reset visited variable
		this.visited = null;
		this.visited = new boolean[this.width][this.height][this.depth];

		
	} /* end markTrees */

	
	/* --------------------------------------------------------------*/
	/**
	 * 
	 * @param startingPoint
	 * @param outputImage
	 * @param color
	 * @return
	 */
	private int visitTree(int[] startingPoint, ImageStack outputImage,
			byte color) 
	{
		int numOfVoxels = 0;
		
		if(isVisited(startingPoint))	
			return 0;
		// Set pixel color
		this.setPixel(outputImage, startingPoint[0], startingPoint[1], startingPoint[2], color);
		
		ArrayList <int[]> toRevisit = new ArrayList <int []>();
		
		int[] nextPoint = getNextUnvisitedVoxel(startingPoint);
		
		while(nextPoint != null || toRevisit.size() != 0)
		{
			if(nextPoint != null)
			{
				if(!isVisited(nextPoint))
				{
					numOfVoxels++;
					
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
				toRevisit.remove(0);
				// Calculate next point to visit
				nextPoint = getNextUnvisitedVoxel(nextPoint);
			}				
		}
		
		return numOfVoxels;
	}

	/* -----------------------------------------------------------------------*/
	/**
	 * Visit a branch and calculate length
	 * 
	 * @param startingPoint starting coordinates
	 * @return branch length
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
	 */
	private void groupJunctions() 
	{
					
		// Visit list of junction voxels
		for(int i = 0; i < this.listOfJunctionVoxels.size(); i ++)
		{
			int[] pi = this.listOfJunctionVoxels.get(i);
			boolean grouped = false;
			
			for(int j = 0; j < this.listOfSingleJunctions.size(); j++)
			{
				ArrayList <int[]> groupOfJunctions = this.listOfSingleJunctions.get(j);
				for(int k = 0; k < groupOfJunctions.size(); k++)
				{
					int[] pk = groupOfJunctions.get(k);				

					// If two junction voxels are neigbors, we group them
					// in the same list
					if(isNeighbor(pi, pk))
					{
						groupOfJunctions.add(pi);
						grouped = true;
						break;
					}

				}
				
				if(grouped)
					break;					
			}
			
			if(!grouped)
			{
				ArrayList <int[]> newGroup = new ArrayList<int[]>();
				newGroup.add(pi);
				this.listOfSingleJunctions.add(newGroup);
			}
		}
		
				
		this.numberOfJunctions = this.listOfSingleJunctions.size();
		
	}	

	/* -----------------------------------------------------------------------*/
	/**
	 * Calculate number of triple points in the skeleton. Triple points are
	 * junctions with exactly 3 branches.
	 */
	private void calculateTriplePoints() 
	{
					
		// Visit the groups of junction voxels
		for(int i = 0; i < this.numberOfJunctions; i ++)
		{
			
			ArrayList <int[]> groupOfJunctions = this.listOfSingleJunctions.get(i);
			
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
				this.numberOfTriplePoints ++;
			
		}
		
				
		this.numberOfJunctions = this.listOfSingleJunctions.size();
		
	}	
	

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
							this.numberOfEndPoints++;
							int[] endPoint = new int[]{x, y, z};
							this.listOfEndPoints.add(endPoint);							
						}
						else if(numOfNeighbors > 2)
						{
							setPixel(outputImage, x, y, z, AnalyzeSkeleton_.JUNCTION);
							int[] junction = new int[]{x, y, z};
							this.listOfJunctionVoxels.add(junction);	
							this.numberOfJunctionVoxels++;
						}
						else
						{
							setPixel(outputImage, x, y, z, AnalyzeSkeleton_.SLAB);
							this.numberOfSlabs++;
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
