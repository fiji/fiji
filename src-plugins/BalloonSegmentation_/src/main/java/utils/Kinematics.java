package utils;
/*********************************************************************
 * Version: January, 2008
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

/***********************************************************************************
*
*	Constructor & initialization
*
************************************************************************************/


import ij.*;
import ij.process.*;
import java.awt.*;
import java.util.*;
import balloonstructure.*;
import Jama.*;

public class Kinematics {
    BalloonSequence pop_sequence;									//
    BalloonPopulation[] PopList; 									// array of BalloonPopulation
	int Nseq = 0;													// Number of time sequences

    public Kinematics(BalloonSequence seq)
    {
	pop_sequence = seq;
	PopList = seq.PopList;
	Nseq = seq.N;
    }
	/***********************************************************************************
	*
	*	Interface to kinematic algorithms
	*
	************************************************************************************/
	 public void KinematicAnalysis()
	 {
		 IJ.log("" + "Time" + " \t " + "ID" + " \t " + "n_generation" + " \t " + "id_mother" + " \t "+"area");
		 for (int i=1;i<Nseq;i++)
		 {
			BalloonPopulation pop = PopList[i];
			BalloonPopulation pop0 = PopList[i-1];

			if (pop.N>0 &pop0.N>0){
				lineage(pop0,pop,i);								// find lineage of cells
				Kinematics_inertia(pop0,pop);						// compute deformations
				}
		 }
		 smooth_straining();
		 KinematicOutput();
		 lineageOutput();

		 //BalloonOutput();

	 }
	 public void smooth_straining()
	 {
		 int n_balloon = (PopList[Nseq-1]).BallList.size();
		 n_balloon*=3;												// here I consider that the id of the balloons in any population does not exceed 3 times the number of balloons in the last population
		 int[][] ball_trajectory = new int[n_balloon][Nseq];		// store the index in BallList of each balloon / for each sequence. (-1 if the id is not in the frame)

		 // Get the trajectories of cells
		 for (int i=0;i<n_balloon;i++)   							// for each potential balloon id
		 {	 for (int j=0;j<Nseq;j++)								// for each population in the
			 {	 int kk = 0;
				 boolean add = false;
				 for (int k=0;k<(PopList[j]).BallList.size();k++) 	// for each elements in BallList
				 {	 kk = (k+i)%((PopList[j]).BallList.size());   	// start from i because i and id may correspond
					 Balloon bal = (Balloon)((PopList[j]).BallList.get(kk));
					 if(bal.id == i) {add = true; break;}
				 }
				 if (add == true){ball_trajectory[i][j] = kk;}
				 else{ball_trajectory[i][j] = -1;}
			 }
		 }

		 // Smooth the strain components
		for (int i=0;i<n_balloon;i++)								// for each balloon id
		{
			double[] sig_vol = new double[Nseq];						// volumetric straining
			double[] vol =  new double[Nseq];							// volume of the ball
			Matrix[] StrainVector= new Matrix[Nseq];					// principal axis
			double[] Sxx= new double[Nseq];
			double[] Syy= new double[Nseq];

			for (int t=0;t<Nseq;t++)									// for each sequence in the stack
			{
				if (ball_trajectory[i][t]>0)
				{
					Balloon bal = (Balloon)((PopList[t]).BallList.get(ball_trajectory[i][t]));
					sig_vol[t] = bal.sig_vol;						// volumetric straining
					StrainVector[t] = bal.StrainVector;				// principal axis
					Sxx[t] = bal.StrainValues[0];							//
					Syy[t] = bal.StrainValues[1];
					vol[t] = bal.area;
				}
			}

			for (int t=1;t<Nseq-1;t++)		// for each sequence in the stack
			{
				// Cell division: inherit mechanical strain from mother
				if (ball_trajectory[i][t+1]>-1 & ball_trajectory[i][t]>-1 & vol[t+1]/vol[t]<0.75)
				{
					Balloon bal0 = (Balloon)((PopList[t]).BallList.get(ball_trajectory[i][t]));
					Balloon bal1 = (Balloon)((PopList[t+1]).BallList.get(ball_trajectory[i][t+1]));
					bal1.StrainVector = bal0.StrainVector;
					bal1.StrainValues = bal0.StrainValues;
					bal1.sig_vol = bal0.sig_vol;
				}
				// time averaging
				if (ball_trajectory[i][t]>-1 & ball_trajectory[i][t+1]>-1 & ball_trajectory[i][t-1]>0)
				{
					Balloon bal = (Balloon)((PopList[t]).BallList.get(ball_trajectory[i][t]));
				}
			}
		}
	 }
	 public void KinematicOutput()
	 {
		BalloonPopulation pop0 = PopList[0];
		ColorProcessor cp = new ColorProcessor((pop0.ipb).getWidth(), (pop0.ipb).getHeight());
		ImagePlus imp_kine =  new ImagePlus("Kinematics",cp);  // temporry output
	    ImageStack stack = imp_kine.createEmptyStack();

	    for(int i=0; i<Nseq; i++) {
		BalloonPopulation pop = PopList[i];
			ColorProcessor ip_kine = pop.Draw_kinematics();
			stack.addSlice("", ip_kine);//.convertToByte(true));
		}
	    imp_kine.setStack(null, stack);
	    imp_kine.show();
	 }
	 public void lineageOutput()
	 {
		BalloonPopulation pop0 = PopList[0];
		ColorProcessor cp = new ColorProcessor((pop0.ipb).getWidth(), (pop0.ipb).getHeight());
		ImagePlus imp_kine =  new ImagePlus("lineage",cp);  // temporry output
	    ImageStack stack = imp_kine.createEmptyStack();

	    for(int i=0; i<Nseq; i++) {
		BalloonPopulation pop = PopList[i];
			ColorProcessor ip_kine = pop.Draw_lineage(i);

			// add division plane
			if (i==Nseq-1)
			{
				for (int j=0; j<Nseq; j++)
				{
					BalloonPopulation pop_div = PopList[j];
					for (int k=0;k<pop_div.BallList.size();k++)
					{
						Balloon bal = (Balloon)pop_div.BallList.get(k);
						if (bal.id_line == 6) {
							ip_kine.setLineWidth(4);
							ip_kine.setColor(new Color(0,250,0));  //
							ip_kine.drawLine(bal.div_x0, bal.div_y0,bal.div_x1, bal.div_y1);

						}
					}
				}
			}
			// make a stack image
			stack.addSlice("", ip_kine);//.convertToByte(true));
		}
	    imp_kine.setStack(null, stack);
	    imp_kine.show();
	 }


		/***********************************************************************************
		*
		*	Kinematics Algorithms
		*
		************************************************************************************/

	 /**	Cell division and cell lineage algorithm 	  */
	private void lineage (BalloonPopulation BP, BalloonPopulation BP1, int seq)
	 {  /* BEGIN lineage*/
		 /**
		  * find the id of the mother cell
		  */

		 // copy attributes from old cells from the previous sequence
		 for (int i=0;i<BP.N;i++)
		 {
			 Balloon Bnew = (Balloon)(BP1.BallList.get(i));
			 Balloon Bmother= (Balloon)(BP.BallList.get(i));
			 Bnew.id_mother = Bmother.id_mother;
			 Bnew.n_generation = Bmother.n_generation;
			 Bnew.id_line = Bmother.id_line;
		 }

		if (BP1.N-BP.N>0) {

			/* Find a random permutation of Cell ids */
			int n_new = BP1.N-BP.N;
			RandPermutation rndPermute = new RandPermutation(n_new);					// custom class for generating random permutation

			/* list of mothers ids (in relation to the permuted list of cells) */
			// global final properties (initiated once)
			int[] IDmothers = new int[n_new];											// list of mother ids
			int[] final_perm = new int[n_new];											// corresponding permutation
			double score_global = 9999999;												// score of the complete association of mother cells in the sequence

			int[] perm = rndPermute.next();
			Matrix final_topo = projected_topo(BP, BP1, null , -1 ,-1, seq);					//new Matrix(BP.N,BP.N,0);
			int t=0;
			boolean is_loop = true;
			while (is_loop)
			{
				t+=1;
				// global candidate_properties (initiated at each loop over the whole population)
				double score_candidate_global = 0;										// score of the complete association of mother cells in the sequence
				int[] IDmothers_candidate = new int[n_new];								// list of mother ids
				int[] perm_candidate = rndPermute.next();								// a random order

				for (int ii=BP.N;ii<BP1.N;ii++)  /** run through all new balloons to find their mother  */
				 {
					// real index
					int i = BP.N + perm[ii-BP.N];					// order of new balloon is permutted randomly

					// local final properties
					double score = 15;

					Balloon Bnew = (Balloon)(BP1.BallList.get(i));		// new cell found
					int[] new_neighb = BP1.contacts[i]; 				// neighbours of the new cell
					Balloon Bmother= (Balloon)(BP.BallList.get(0)); // initiate the mother cell

					for (int j=0;j<BP.N;j++)  /** run through all potential mother cells  */
					 {
						// local candidate properties
						Balloon Bcandidate = (Balloon)(BP.BallList.get(j));  //  candidate to be examined (from the previous sequence)
						int[] candidate_neighb_0 = BP.contacts[j];			 //  neighbours of the candidate
						int[] candidate_neighb_1 = BP1.contacts[j];			 	 //  neighbours of the candidate
						double score_candidate = 15;
						if (BP1.topo[Bnew.id][Bcandidate.id])
							{score_candidate = score_lineage(final_topo, BP, BP1, Bnew.id, Bcandidate.id, seq);}  // score the current configuration
						else {score_candidate = 15;}

						if (score_candidate < score)
						{
							score = score_candidate;
							Bmother = Bcandidate;
						}
					 }

					 // the second daugther cell produced during the same division
					 Balloon Bpair = (Balloon)(BP1.BallList.get(Bmother.id));


					 // update the final_topo so that it is used as a starting point for the next cell to be matched
					 final_topo = projected_topo(BP,BP1, final_topo , Bnew.id ,Bpair.id, seq);
					 IDmothers_candidate[i-BP.N] = Bmother.id;
					 score_candidate_global+=1;

				 }
				if (score_candidate_global < score_global)
				{
					score_global = score_candidate_global;					// score of the complete association of mother cells in the sequence
					IDmothers = IDmothers_candidate;							// list of mother ids
					perm = perm_candidate;									// a random order
				}

				// test if there are no 2 mothers
				if (t>5){is_loop=false;}
			}

			//
			// set the Balloon lineage properties
			//
			for (int i=0;i<n_new;i++)
			{
				 // get the two daughter and mother cells
				 Balloon Bmother = (Balloon)(BP.BallList.get(IDmothers[i]));			// mother cell
				 Balloon Bpair = (Balloon)(BP1.BallList.get(IDmothers[i]));				// daughter 1 (id< BP.N)
				 Balloon Bnew = (Balloon)(BP1.BallList.get(BP.N + i));			// daughter 2 (id=> BP.N)

				 // Write the lineage properties of each cell
				 Bnew.n_generation = Bmother.n_generation + 1;			// generation number
				 Bpair.n_generation = Bnew.n_generation;				// generation number

				 Bnew.id_mother = Bmother.id;							// id of mother cell in BP
				 Bpair.id_mother = Bmother.id;							// id of mother cell in BP
				 find_divisionplane(Bnew, Bpair, BP, BP1);					// find the division plane

				 Bnew.id_line = Bmother.id_line;						// inherit the id of the initial cell
				 Bpair.id_line = Bmother.id_line;						// inherit the id of the initial cell
			}
		}
	 } /* END lineage*/

	private Matrix projected_topo(BalloonPopulation BP0,BalloonPopulation BP1, Matrix current_topo , int new_id ,int candidate_id, int seq)
		{ /* BEGIN projected_topo*/
		int n_topo0 = BP0.topo.length;
		Matrix updated_topo = new Matrix(n_topo0,n_topo0,0);

		/**
		 * Initiation of topo matrix before the computation of the optimal lineage
		 */
		if (current_topo == null | new_id<0 | candidate_id<0)  // used for initiating ref_topo and fill the first current_topo
		{
			BP1.ref_topo = new Matrix(n_topo0,n_topo0,0);
			for (int i=0;i<n_topo0;i++)
			{
			for (int j=0;j<i;j++)
				{
				double ref_entry = 0;
				if (BP0.topo[i][j] ){
					BP1.ref_topo.set(i,j, ref_entry);
					BP1.ref_topo.set(j,i, ref_entry);}

				double entry0 = 0;
				if (BP1.topo[i][j] ){
					updated_topo.set(i,j, entry0);
					updated_topo.set(j,i, entry0);}

				}
			}
		}
		/**
		 * Update the current_topo matrix in order to compute the distance with the ref_topo matrix
		 */
		else
			{
			for (int i=0;i<n_topo0;i++)
				{
				double entry2 = 0;
				if (BP1.topo[new_id][i]){entry2 = 1;}
				for (int j=0;j<i;j++)
					{
					double entry1 = 0;
					if (BP0.topo[i][j] ){entry1 = 1;}

					// ref topo
					BP1.ref_topo.set(i,j, entry1);
					BP1.ref_topo.set(j,i, entry1);

					// projection if the two divided cells where placed back into the previous layout
					updated_topo.set(i,j, (current_topo.get(i,j) + updated_topo.get(i,j)));
					updated_topo.set(j,i, (current_topo.get(i,j) + updated_topo.get(i,j)));
					}

				if (i!=candidate_id) 					// complete the missing link by those of the new link
					{updated_topo.set(candidate_id,i , Math.max(entry2, updated_topo.get(candidate_id,i)));
					 updated_topo.set(i,candidate_id , Math.max(entry2, updated_topo.get(candidate_id,i)));
					//if (seq == 13 & candidate_id == 7){System.out.println("     " + candidate_id + " / " + i + " : " + entry2);}
					}
				}
			}

		return updated_topo;
		} /* END projected_topo*/
	private double score_lineage(Matrix current_topo, BalloonPopulation BP0, BalloonPopulation BP1, int new_id ,int candidate_id, int seq)
	 { /* BEGIN score_lineage*/
		//optimize_topo(BP0 , new_id ,candidate_id, seq);
		// topological difference betweenbefore and after
		int n_topo0 = BP0.topo.length;
		Matrix candidate_topo;
		candidate_topo = projected_topo(BP0, BP1, current_topo , new_id ,candidate_id, seq);

		double norm0 = BP1.ref_topo.normF();
		double dist = (BP1.ref_topo.minus(candidate_topo)).normF();
		double score = Math.sqrt(dist/norm0);

		// variation in cell size before and after needs to be minimal
		Balloon Bnew = (Balloon)(BP1.BallList.get(new_id));		// new cell
		Balloon Bpair = (Balloon)(BP1.BallList.get(candidate_id));		// new cell
		Balloon Bmother= (Balloon)(BP0.BallList.get(candidate_id)); // initiate the mother cell
		double delta_volume = (Bnew.area + Bpair.area - Bmother.area)/Bmother.area;
		if (delta_volume<-0.15 | delta_volume > 0.25){score+=2;}

		return score;
	 } /* END score_lineage*/
	private void find_divisionplane(Balloon bal1, Balloon bal2, BalloonPopulation BP0, BalloonPopulation BP1)
	 { /* BEGIN find_divisionplane*/
		// draw cell division
		boolean start = false;
		double x0 = 0;
		double y0 = 0;
		double x1 = 0;
		double y1 = 0;
		double x=0;
		double y=0;
		double xm=0; double ym=0;					// point in the middle of the wall


		//
		//	identify the points that constitute the plan of cell difivion of the cell and place them in div_plan
		//
		Vector div_plan = new Vector();					// list of points that constitue the cell wall
		for (int j=0;j<bal1.n0;j++)
			{
			if (BP1.contacts[bal1.id][j] == bal2.id)
				{
				double dist = 100000;
				x0 = bal1.XX[j];
				y0 = bal1.YY[j];
				for (int k=0;k<bal2.n0;k++)
					{
					if (Math.sqrt((x0-bal2.XX[k])*(x0-bal2.XX[k]) + (y0-bal2.YY[k])*(y0-bal2.YY[k]))<dist)
						{
						x1 = bal2.XX[k];
						y1 = bal2.YY[k];
						dist = Math.sqrt((x0-bal2.XX[k])*(x0-bal2.XX[k]) + (y0-bal2.YY[k])*(y0-bal2.YY[k]));
						}
					}
				double[] xy = {(x0+x1)/2, (y0+y1)/2};
				xm+=(x0+x1)/2; ym+=(y0+y1)/2;

				Balloon bal_mother = (Balloon)(BP0.BallList.get(bal1.id_mother));
				div_plan.addElement(xy);
				}
			}
		int n = div_plan.size();
		Matrix X = new Matrix(n,2,1);
		Matrix Y = new Matrix(n,1,0);
		xm/=n; ym/=n;

		//
		//  least square to fin the line going through the points
		//
		Balloon Bmother = (Balloon)(BP0.BallList.get(bal1.id_mother));

		for (int j=0;j<div_plan.size();j++)
		{
			double[] xy = (double[])(div_plan.get(j));
			X.set(j,0,xy[0]);
			Y.set(j,0,xy[1]);
		}


		//
		// Find the limit for the axis from the mother cell boundaries
		//
		boolean is_loop = true;
		double alpha;
		if (X.rank()>1)
			{Matrix coeff = X.solve(Y);			// coeffs of the regression over the points defining the division plant
			alpha = Math.atan(coeff.get(0,0));
			}
		else{
			alpha = (bal1.y0 - bal2.y0)/(bal2.x0 - bal1.x0);
			}

		int i=1;
		int CX[] = new int[Bmother.XX.length];
		int CY[] = new int[Bmother.XX.length];
		for (int k=0;k<CX.length;k++)
		{
			CX[k] = (int)Bmother.XX[k];
			CY[k] = (int)Bmother.YY[k];
		}
		Polygon cell = new Polygon(CX,CY,CX.length);
		while (is_loop)
		{
			x0 = (int)(xm-i*Math.cos(alpha));
			y0 = (int)(ym-i*Math.sin(alpha));
			is_loop = cell.contains(x0,y0);
			i+=1;
		}
		Bmother.div_x0 = (int)(xm-i*Math.cos(alpha)*0.5); Bmother.div_y0 = (int)(ym-i*Math.sin(alpha)*0.5);
		is_loop = true;
		while (is_loop)
		{
			x1 = (int)(xm+i*Math.cos(alpha));
			y1 = (int)(ym+i*Math.sin(alpha));
			is_loop = cell.contains(x1,y1);
			i+=1;
		}
		Bmother.div_x1 = (int)(xm+i*Math.cos(alpha)*0.5); Bmother.div_y1 = (int)(ym+i*Math.sin(alpha)*0.5);
	 } /* END find_divisionplane*/

	/*
	* Cell Deformation
	*/
	private void Kinematics_inertia (BalloonPopulation BP, BalloonPopulation BP1)
		{ /* BEGIN Kinematics_inertia*/
		 /**
		  * Calculation of strain tensor using the inertia method
		  *  Cf. my lab notebook (02/10/2007) LXD
		  */
		BP1.mass_Geometry();					// compute center of cell qnd inertia components

		int w = BP1.ipb.getWidth();
		int h = BP1.ipb.getHeight();
		ColorProcessor cp = new ColorProcessor(w, h);

		// fill the image
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++){
			int pix = BP1.ipb.getPixel(x,y);
			int red=pix;
			int green=pix;
			int blue=pix;
			cp.putPixel(x,y, (((int)red & 0xff) <<16)+ (((int)green & 0xff) << 8) + ((int)blue & 0xff));
			}
		}
		ImagePlus imp =  new ImagePlus("Disps", cp);  // temporry output
		ImageProcessor ip = imp.getProcessor();

		for (int i=0;i<BP.N;i++)
			{

			Balloon B0 = (Balloon)(BP.BallList.get(i));
			Balloon B1 = (Balloon)(BP1.BallList.get(i));
			double dIxx,dIyy,dIxy, Ixx, Iyy, Ixy;//

			/**   Displacement field based on displacements of center of mass */
			/** this is done in 5 ugly steps, but should work the same */
			int[] neig_list = new int[BP.N];

			// fill neig_list with 0 (no neighbour)
			for (int j=0;j<BP.N;j++){neig_list[j]=0;}

			// find neighbours of the cell i
			for (int j=0;j<B0.n0;j++) {
				if (BP.contacts[i][j]>0) {
					neig_list[BP.contacts[i][j]] = 1;
				} }

			// Extract X,Y and displacement for the set of neighbour vertices
			int n=0;
			Vector V = new Vector();
			Vector PXY = new Vector();

			for (int j=-1;j<BP.N;j++)
				{
				if (j==-1)
					{
					double[] u = {B1.x0-B0.x0, B1.y0-B0.y0};
					double[] xy = {B0.x0, B0.y0};
					V.addElement(u);
					PXY.addElement(xy);
					}

				else if (BP1.topo[i][j])//(neig_list[j]>0)
					{
					Balloon B0n = (Balloon)(BP.BallList.get(j));
					Balloon B1n = (Balloon)(BP1.BallList.get(j));

					double[] u = {B1n.x0-B0n.x0, B1n.y0-B0n.y0};
					double[] xy = {B0n.x0, B0n.y0};
					V.addElement(u);
					PXY.addElement(xy);

					}
				}

			double [][] X = new double [V.size()][3];
			double [][] Yx = new double [V.size()][1];
			double [][] Yy = new double [V.size()][1];
			for (int k=0;k<V.size();k++)
				{
				double[] M = (double[])(V.get(k));
				double[] xy2 = (double[])(PXY.get(k));
				X[k][0] = xy2[0];
				X[k][1] = xy2[1];
				X[k][2] = 1;
				Yx[k][0] = M[0];
				Yy[k][0] = M[1];
				}
			if (V.size()>2)
				{
				// compute coefficient of the linear displacment field
				Matrix MX = new Matrix(X);
				Matrix MYx = new Matrix(Yx);
				Matrix MYy = new Matrix(Yy);
				Matrix COEFx = MX.solve(MYx);
				Matrix COEFy = MX.solve(MYy);

				/** singular value decomposition (see wikipedia for definition)
				 *  Used to find the rigid motion (matrix U)
				 *  */
				double [][] DispCoeff = {{COEFx.get(0,0),COEFx.get(1,0)},{COEFy.get(0,0),COEFy.get(1,0)}};
				Matrix DispField = new Matrix(DispCoeff);
				SingularValueDecomposition SVD = new SingularValueDecomposition(DispField);
				Matrix U = (SVD.getU()).times((SVD.getV()).transpose());


				/** draw the displacement fied */
				double[][] xyg = {{B0.x0}, {B0.y0}};
				Matrix XYg = new Matrix(xyg);
				Matrix DispG = DispField.times(XYg);

				ip.setColor(new Color(0,0,250));  //
				ip.drawLine(B1.x0,B1.y0,B1.x0+(int)(DispG.get(0,0) + COEFx.get(2,0))*10,B1.y0+(int)(DispG.get(1,0)+ COEFy.get(2,0))*10);
				ip.setColor(new Color(0,0,250));  //
				ip.drawLine(B1.x0-1,B1.y0-1,B1.x0+1,B1.y0+1);

				ip.setColor(new Color(0,250,0));  //
				ip.drawLine(B1.x0,B1.y0, B1.x0+(int)(U.get(0,0))*10,B1.y0+(int)(U.get(1,0))*10);
				ip.setColor(new Color(250,0,0));  //
				ip.drawLine(B1.x0,B1.y0,B1.x0+((int)U.get(0,1))*10,B1.y0+(int)U.get(1,1)*10);

				/**   Find variation of Inertia  */
				// find inital inertia components
				Ixx = B0.Ixx;
				Iyy = B0.Iyy;
				Ixy = B0.Ixy;

				// change the local axis according to rotation U found previously
				double [][] I2_array = {{B1.Ixx, B1.Ixy},{B1.Ixy, B1.Iyy}};
				Matrix I2 = new Matrix(I2_array);
				//I2 = ((U.transpose()).times(I2)).times(U);   // filter here so that only values above a certain value re used

				// determine dIij
				dIxx = I2.get(0,0)- Ixx;
				dIxy = I2.get(0,1) - Ixy;
				dIyy = I2.get(1,1) - Iyy;
				//dIxx = B1.Ixx - Ixx;
				//dIxy = B1.Ixy - Ixy;
				//dIyy = B1.Iyy - Iyy;

				double [][] dI = {{dIxx},{dIxy},{dIyy}};
				double [][] M = {{3*Ixx,2*Ixy,Iyy},{2*Ixy,Ixx+Iyy, 2*Ixy},{Iyy,2*Ixy,3*Iyy}};
				Matrix SYS = new Matrix(M);


				/** Find the principal components*/
				try{
					// solve the linear system
					Matrix Solution = SYS.solve(new Matrix(dI));

					// diagonalize to find principal componants
					double [][] Strain = {{Solution.get(0,0),Solution.get(1,0)},{Solution.get(1,0),Solution.get(2,0)}};
					Matrix StrainTensor = new Matrix(Strain);
					EigenvalueDecomposition eig = StrainTensor.eig();
					double[] eigValues = eig.getRealEigenvalues();
					Matrix eigVectors = eig.getV();
					B1.StrainVector = eigVectors;
					B1.StrainValues = eigValues;
					B1.sig_vol = (B1.area - B0.area)/B0.area;
					B1.StrainTensor = StrainTensor;

					}
				catch (Exception err) {
					// Print out the exception that occurred
					IJ.log("Unable to find the principal axis of deformation " + B0.id+ " : " + B0.Ixx + " , " + B0.Iyy);
					SYS.print(3,3);
					err.printStackTrace();
					}
				}
			}
		} /* END Kinematics_inertia*/
}


/*
 * function used to determine how good the estimated transformation match with the initial and final deformed cells
 * place the line of code: double score[] = score_transfo(B1, B0);
 * at the end of the kinematics_inertia function in BalloonPopulation for getting the output
 * */
 /*
public double[] score_transfo(Balloon B1, Balloon B0)
	{
		int[] XX2 = B0.XX;
		int[] YY2 = B0.YY;
		int[] XX1 = B1.XX;
		int[] YY1 = B1.YY;
		int n0 = XX1.length;
		//
		// find the bounding box of the balloon
		//

		//
		// compute center of mass of balloon B0
		//
		PolygonRoi Proi = new PolygonRoi(XX2,YY2,XX2.length,Roi.POLYGON);
		int nn = 0;
		double xgn=0; double ygn=0;

		// transform B0 and get bounding box
		int x_min = 100000; int x_max = 0 ; int y_min = 1000000 ; int y_max = 0;  	// bounds
		for (int k=0;k<n0;k++) 			// filling
			{
			double [][] xyscore= {{XX2[k]-B0.x0},{YY2[k]-B0.y0}};
			Matrix XYscore = new Matrix(xyscore);;
			Matrix Vscore = new Matrix(2,2);
			//bal1.StrainTensor.print(3,3);
			Vscore = (B1.StrainTensor).times(XYscore);
			XX2[k] = XX2[k] + (int)(Vscore.get(0,0));
			YY2[k] = YY2[k] + (int)(Vscore.get(1,0));

			x_min = Math.min(x_min, XX2[k]);
			x_max = Math.max(x_max, XX2[k]);
			y_min = Math.min(y_min, YY2[k]);
			y_max = Math.max(y_max, YY2[k]);
			}
		// computation
		for (int i=x_min;i<x_max+1;i++)
			{
			for (int j=y_min;j<y_max+1;j++)
				{
				if (Proi.contains(i,j))
					{
					xgn+=i;
					ygn+=j;
					nn +=1;
					}
				}
			}
		xgn /= nn;
		ygn /= nn;


		//
		// translate B0 to coincde with B1 (centers of mass)
		//
		for (int k=0;k<n0;k++)
		{
			XX2[k] = XX2[k] + (int)(B1.x0 - xgn);
			YY2[k] = YY2[k] + (int)(B1.y0 - ygn);
		}

		//
		// count the miss matched pixels
		//
		PolygonRoi Proi1 = new PolygonRoi(XX1,YY1,XX1.length,Roi.POLYGON);
		PolygonRoi Proi2 = new PolygonRoi(XX2,YY2,XX2.length,Roi.POLYGON);

		int n_match=0;
		int n_1out=0;
		int n_2out=0;
		int n_tot = 0;
		double Ixx = 0; double Iyy = 0; double Ixy = 0; double Carea = 0;

		x_min = 100000; x_max = 0 ; y_min = 1000000 ; y_max = 0;  	// bounds
		for (int k=0;k<n0;k++) 			// filling
			{
			x_min = Math.min(x_min, XX1[k]);
			x_max = Math.max(x_max, XX1[k]);
			y_min = Math.min(y_min, YY1[k]);
			y_max = Math.max(y_max, YY1[k]);
			}

		for (int i=(int)(x_min*0.8);i<x_max*1.2+1;i++)
			{
			for (int j = (int)(y_min*0.8);j<y_max*1.2+1;j++)
				{
				if (Proi2.contains(i,j))
					{
					Ixx += (i-B1.x0)*(i-B1.x0);
					Iyy += (j-B1.y0)*(j-B1.y0);
					Ixy += (i-B1.x0)*(j-B1.y0);
					Carea +=1;
					}
				if (Proi1.contains(i,j) & Proi2.contains(i,j) )
					{
					n_match +=1;
					n_tot +=1;
					}
				else if (Proi1.contains(i,j) & !Proi2.contains(i,j) )
					{
					n_2out +=1;
					n_tot +=1;
					}
				else if (!Proi1.contains(i,j) & Proi2.contains(i,j) )
					{
					n_1out +=1;
					n_tot +=1;
					}
				}
			}
		//double [] res = {Math.abs(Ixx-B1.Ixx)/Math.abs(B1.Ixx-B0.Ixx),Math.abs(Iyy-B1.Iyy)/Math.abs(B1.Iyy-B0.Iyy),Math.abs(Ixy-B1.Ixy)/Math.abs(B1.Ixy-B0.Ixy)};
		double [] res = {n_tot,n_1out,n_2out};
		return res;
	}
 */
