
/*********************************************************************
 * BalloonSequence is a class constituted of an array of Balloon populations
 * Methods includes computation of Kinematics parameters and outputing of the analysis
 * Version: October, 2007
 ********************************************************************/

/*********************************************************************
 * Lionel Dupuy
 * SCRI
 * EPI program
 * Invergowrie
 * DUNDEE DD2 5DA
 * Scotland
 * UK
 *
 * Lionel.Dupuy@scri.ac.uk
 * http://www.scri.ac.uk/staff/lioneldupuy
 ********************************************************************/
/**
	This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA."""
*/
package balloonstructure;

import ij.*;
import ij.process.*;
import java.io.*;
import java.util.*;


public class BalloonSequence
{

	public BalloonPopulation[] PopList; 		// array of BalloonPopulation
	ImagePlus imp;						// stack to be analysed
	public int N = 0;							// Number of time sequences
	int IMB[][][];						// THIS IS to give the same IMB to all sequences when loading file because can be saved
	int channel = 1;					// channel to be used for initiation resp (1,2,3 for R,G,B)

/* 	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	Constructor & initialization
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
*/

	/** BalloonSequence constructor: initiate a sequence of Balloon population from a stack of images*/
	 public BalloonSequence(ImagePlus imp_in)
	 {
		imp = imp_in;
		N = imp.getStackSize();
		ImageStack stack = imp.getStack();
		PopList = new BalloonPopulation[N];

		int w = (stack.getProcessor(1)).getWidth();
		int h = (stack.getProcessor(1)).getHeight();
		IMB = new int[w][h][3];
		for (int i=0;i<w;i++)
			{
			for (int j=0;j<h;j++)
				{
				IMB[i][j][0]= 0;
				IMB[i][j][1]= 0;
				IMB[i][j][2]= 0;
				}
			}
	 }

	 /** initialize the data in BalloonSequence*/
	 private void reinitPopulation(int currentSlice)
	 {
			ImageProcessor impross = (PopList[currentSlice]).ipb;
			PopList[currentSlice] = new BalloonPopulation(impross, currentSlice, channel);
			PopList[currentSlice].set_boundaries(IMB);
	 }


	 /**initiate a new balloon population*/
	 public void setSequence(int currentSlice, ImageProcessor ipWallSegment, int rgbchannel)
	 {
			PopList[currentSlice] = new BalloonPopulation(ipWallSegment, currentSlice, channel);
			PopList[currentSlice].id = currentSlice;
			channel = rgbchannel;
	 }

	 /***/
	 public void setSequence(int currentSlice, ImageProcessor ipWallSegment, ArrayList X0, ArrayList Y0, int rgbchannel)
	 {
			channel = rgbchannel;
			PopList[currentSlice] = new BalloonPopulation(ipWallSegment, currentSlice, channel);
			PopList[currentSlice].id = currentSlice;
			double x;
			double y;
			for (int i=0;i<X0.size();i++)
			{
				x = ((Double) (X0.get(i))).doubleValue();
				y = ((Double) (Y0.get(i))).doubleValue();
				PopList[currentSlice].AddNewBalloon((int)x,(int)y);			// XXX
			}
	 }

	/*//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//	Inputs / Outputs
	//-------------------------------------------------------------------------------
	//------------------------------------------------------------------------------- */
	 /** Read files with XYZ position for centres
	  *  Circular balloon centrerd on x,y with id ID*/
		public ArrayList<double[]> importSeeds(String directory, String file_name, int currentSlice)
		{ /* BEGIN importSeeds*/
			StringBuffer contents = new StringBuffer();
			BufferedReader input = null;
			try {
				input = new BufferedReader( new FileReader(directory + file_name));
				String line = null;
				int i=0;
				ArrayList<double[]> SeedList = new ArrayList<double[]>();
				/* read seeds coordinates */
				while ((line = input.readLine()) != null)
					{
					String[] parts = line.split("\t");		// split the line
					if ( Character.isDigit( (parts[0].toCharArray())[0] ) & Character.isDigit( (parts[1].toCharArray())[0] ))
					{

						double x = Double.parseDouble(parts[0].trim());
						double y = Double.parseDouble(parts[1].trim());
						double z = -1;
						if (parts.length >2)
						{
							if(Double.parseDouble(parts[2].trim())>=0 &
								z<Double.parseDouble(parts[2].trim()) )
							{
								z = Double.parseDouble(parts[2].trim());
							}
						}
						double[] row = {x,y,z};
						SeedList.add(row);
					}
				}
				return SeedList;
			}
			catch (IOException e) {
				IJ.error("Could not load the data from file: setStructureFromFile");
				return null;
				}
		} /* END importSeeds*/

	/**
	 * Output a formatted data of the balloon structure and sequence
	 */

	 public StringBuffer getStructureInfo() {
		// initiate text file
		StringBuffer info = new StringBuffer("%Balloon data \n");

		// find the first balloon available
		int ini_pop = 0;
		while (PopList[ini_pop].N<=0){ini_pop+=1;}
		Balloon bal0 = (Balloon)((PopList[ini_pop].BallList).get(0));


		// write the header
		info.append("N:" + bal0.n0 + " \n");
		info.append(" \n");
		info.append(" \n");
		info.append(" \n");

		// fill the text with sequence / population / balloon information
		for (int i=0;i<N;i++)
			{
			info.append("%Frame: " + i+ " \n");
			BalloonPopulation pop = PopList[i];
			for (int j = 0; j<pop.N; j++) {

				// get the corrresponding balloon
				Balloon bal = (Balloon)((pop.BallList).get(j));

				// write center
				info.append("%%\tCenter:");
				info.append("\t" + j + "\t" + bal.x0 + "\t" + bal.y0 + "\n");

				// write the position of vertices
				info.append("%%%\t\tVertex Positions: \n");
				for (int k = 0; k<bal.n0; k++)
					{
					info.append("\t\t" + k + "\t" + Math.round(bal.XX[k]) + "\t" + Math.round(bal.YY[k]));
					info.append("\n");
					}
				}
			info.append("\n");
			}

		// save the topology of the connections
		info.append("*Topology \n");

		for (int i=0;i<N;i++)
		{
		info.append("**TFrame:"+"\t" + i+ " \n");
		BalloonPopulation pop = PopList[i];
		for (int j = 0; j<pop.N; j++) {

			// get the corrresponding balloon
			Balloon bal = (Balloon)((pop.BallList).get(j));

			// write center
			info.append("***\tBalloon:");
			info.append("\t" + bal.id + "\n");

			// write the neighbours of the current balloon
			for (int k = 0; k<bal.n0; k++)
				{
				info.append("\t\t" + k + "\t" + pop.contacts[j][k]);
				info.append("\n");
				}
			}
		}
		return info;
	}

	/**
	* Reconstruct the structure from saved data
	*/
	public void setStructureFromFile(String directory, String file_name) {
		StringBuffer contents = new StringBuffer();
		BufferedReader input = null;

		try {
			input = new BufferedReader( new FileReader(directory + file_name));
			String line = null;
			int i=0;
			int index_pop = 0;
			int index_bal = 0;
			int index_vert = 0;
			int N_vert = 0;

			/* read general info in header */
			while (i<6)
				{
				line = input.readLine();
				String[] parts = line.split(":");		// split the line
				if (i==1) {N_vert = Integer.parseInt(parts[1].trim());}
				i+=1;
				}

			int[] XX = new int[N_vert];
			int[] YY = new int[N_vert];

			reinitPopulation(0);     // reinitiate population 0 because the first flag "%" is encountered for population 1


			/* read Balloon shapes and geometric properties */
			while ((line = input.readLine()) != null)
				{
				if(index_pop >= N){break;}  			// if number of population > number of slices in the image, break the loop
				String[] parts = line.split("\t");		// split the line

				// balloon definition flag
				if (line.startsWith("%%%")){}

				// centre definition flag
				else if (line.startsWith("%%")) {
					int x,y,id;
				    id = Integer.parseInt(parts[2].trim());	// find balloon spec.
				    x = Integer.parseInt(parts[3].trim());	//
				    y = Integer.parseInt(parts[4].trim());	//
					index_bal +=1;
					}

				// time sequence flag
				else if (line.startsWith("%"))
					{
					// terminate building of current population
					PopList[index_pop].ConnectExistingBalloons ();	// make connection and fill the data structure properly

					// initiate the next one
					index_pop +=1;									// update indexes
					index_bal = 0;									//
					reinitPopulation(index_pop);					// initiate a new population
					}

				// vertex definition flag
				else if (parts.length>2){
					int x,y;
				if (index_vert < N_vert-1)
					{
					YY[index_vert] = Integer.parseInt(parts[4].trim());		 // fill the XY balloon coordinates
					XX[index_vert] = Integer.parseInt(parts[3].trim());		 //
						index_vert +=1;
					}
				else if (index_vert == N_vert-1)
					{
					YY[index_vert] = Integer.parseInt(parts[4].trim()); 		// fill the XY balloon coordinates
					XX[index_vert] = Integer.parseInt(parts[3].trim());			//
					PopList[index_pop].AddNewBalloon(XX, YY);					// add the new balloon when finished
						index_vert = 0;
					}
					}
					i+=1;
					if (line.startsWith("*")){break;}
				}
			PopList[index_pop].ConnectExistingBalloons ();		//   there are case not included in the algo. Need to add this here

			/* read topological properties of the population */
			index_pop = 0;
			index_bal = 0;
			index_vert = 0;
			int nbh_id = 0;
			BalloonPopulation pop = PopList[0];
			while ((line = input.readLine()) != null)
				{
				String[] parts = line.split("\t");		// split the line
				if (line.startsWith("**") & ! (line.startsWith("***")))
					{
					index_pop = Integer.parseInt(parts[1].trim());
					pop = PopList[index_pop];
					}
				else if (line.startsWith("***"))
					{
					index_bal = Integer.parseInt(parts[2].trim());
					}
				else
					{
					if(parts.length <4){continue;}  			// if there is not enough data on topology, go to the next line
					index_vert = Integer.parseInt(parts[2].trim());
					nbh_id = Integer.parseInt(parts[3].trim());

					Balloon bal = (Balloon)((pop.BallList).get(index_bal));
					pop.contacts[index_bal][index_vert]	= nbh_id;
					if (nbh_id>=0)
						{
						pop.topo[nbh_id][index_bal] = true;
						pop.topo[index_bal][nbh_id] = true;
						}
					}
				}

			}
			catch (IOException e) {
				IJ.error("Could not load the data from file: setStructureFromFile");
				return;
		}
	}
}  /* End BalloonSequence class */
