package balloonstructure;
/*********************************************************************
 * Version: February, 2008
 *
 * TODOS:
 * - redondance points et BallList
 * - enregistrer la topologie et s'en servir pour reconnecter a la lecture
 * - tick_contact is does not use the potential contacts. Nedd to cnahge the topo structure
 ********************************************************************/

/*********************************************************************
 * Lionel DupuyF
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

import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.util.*;
import Jama.*;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.ContrastEnhancer;

// reading/writing properties files
import java.io.*;
import java.util.*;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.lang.System;
import java.lang.Thread;

import ij.plugin.filter.Convolver;


public class BalloonPopulation
{ /* BEGIN BalloonPopulation*/

	// DATA
	public ArrayList<Balloon> BallList = new ArrayList<Balloon>(); 	// array of Balloons
	int channel = 1;					// channel to be used for initiation resp (1,2,3 for R,G,B)
	public ImageProcessor ipb;				// Image processor attached to the layout of cells, for getting pixel intensity
	ImageProcessor ip_gradx_p;				// Image processor for the gradient X of the image
	ImageProcessor ip_grady_p;				// Image processor for the gradient Y of the image
	ImageProcessor ip_gradx_m;				// Image processor for the gradient X of the image
	ImageProcessor ip_grady_m;				// Image processor for the gradient Y of the image

	public int N = 0;						// Number of cells / points
	public int id = 0;								// id of population
	int n_envelop = 351;						// number of verts on the envelop defining boundaries of the object
	public boolean topo[][] ;				// matrix of potential connections between cells
											// initiated at the begining by a delaunay algorithm, modified to accept more point
	public int IMB[][][];					// boundaries
	public int contacts[][];				// N x n0 array saying on which balloon the vertex stopped
											// a value of -10 means no contact /
											// a value of -1 mean stopped at the edge of the colony
											// a value of  i>=0 stopped at balloon i



	public Matrix ref_topo ;				// matrix of connection. equivalent to topo, but using the Matrix class to compute norms and distance
	public int area;								// area of the whole population
	public int[]XXi;						// list of x coordinates defining the envelop around the population
	public int[]YYi;						// list of y coordinates defining the envelop around the population

	// parameters to refine balloons
	public int max_n0 = 0;
	public int max_length = 15;				// length of balloon segment above which it is subdivided

	// parameters for finding cell centers (seeds for the algorithm)
	double wmin,hmin,wmax,hmax;				// position of object
	int optimisation_cycles = 10;			// number of cycles to optimize the positioning of points on the image
	int n_random_points = 500;				// number of points drawned randomly at each cycle

	// Parameters for Delaunay triangulation
	int use_delaunay = 1;					// use delaunay
	double triangle_size_factor = 3.5;		// Maximum size of triangle for triangulation (proportion of average cell area)
	double min_triangle_angle = 0.2;		// Minimum Angle in triangulation allowed
	double crit_inclusion = 0.6;			// tolerance for inclusion of points in triangle

	// Parameters for KNN triangulation
	int use_knn = 0;						// use the knn instead of delaunay
	int KN = 7;								// number of neighbours

	// Contact Algorithm
	boolean is_contact_all = false;			// Is contact checked for all possible balloon combination or not
	boolean do_contact = true;   			// Is contact checked for all possible balloon combination or not

	int period_contact_check = 2;			// periodicity for checking if balloons are likely to be in contact (integer x3, x5 ...)
	double fine_contact_check = 0.8;		// crietria for changing to fine checking (ratio : radius of both cells / distance between)

	// stop growth
	boolean is_growing = true;				// population is still growing (automatic stopping)
	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//	Constructor & initialization
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	*/

	/** BalloonPopulation constructor: need to specify the image processor it is associated with and the id of the population*/
	public BalloonPopulation(ImageProcessor ip, int index, int rgbchannel)
		{ /* BEGIN BalloonPopulation*/
		ipb = ip;
		ImageProcessor ipEdge=ipb.duplicate();
		ipEdge.findEdges();
		channel = rgbchannel;
		// prepare imageprocessor to work with
		float s = (float)2;
		float[] kernelYp = {1/s,2/s,1/s,0,0,0,-1/s,-2/s,-1/s};
		float[] kernelXp = {1/s,0,-1/s,2/s,0,-2/s,1/s,0,-1/s};
		float[] kernelYm = {-1/s,-2/s,-1/s,0,0,0,1/s,2/s,1/s};
		float[] kernelXm = {-1/s,0,1/s,-2/s,0,2/s,-1/s,0,1/s};

		ip_gradx_p = ipEdge.duplicate();
		ip_gradx_p.convolve( kernelXp, 3, 3);
		ip_gradx_m = ipEdge.duplicate();
		ip_gradx_m.convolve( kernelXm, 3, 3);

		ip_grady_p = ipEdge.duplicate();
		ip_grady_p.convolve(kernelYp, 3, 3);
		ip_grady_m = ipEdge.duplicate();
		ip_grady_m.convolve( kernelYm, 3, 3);

		id = index;
		loadProperties();
		} /* END BalloonPopulation*/

	 /** Add a balloon with custom shape defined by arrays X and Y*/
	public void AddNewBalloon (int[] X, int[] Y)
		{ /* BEGIN AddNewBalloon*/
		Balloon bal1 = new Balloon(X,Y,N,ipb, this, channel);
		BallList.add(bal1);
		if (max_n0<bal1.n0){max_n0=bal1.n0;}
		N+=1;
		} /* END AddNewBalloon*/

	 /** Add a circular balloon centrerd on x,y with id ID*/
	public void AddNewBalloon(int x, int y)
		{ /* BEGIN AddNewBalloon*/
		Balloon ball = new Balloon(x,y,N,ipb, this, channel);
		BallList.add(ball);
		if (max_n0<ball.n0){max_n0=ball.n0;}
		N+=1;
		} /* END AddNewBalloon*/

	 /** Add a circular balloon centrerd on x,y with id ID*/
	public void importSeeds(String directory, String file_name)
	{ /* BEGIN importSeeds*/
		StringBuffer contents = new StringBuffer();
		BufferedReader input = null;
		try {
			input = new BufferedReader( new FileReader(directory + file_name));
			String line = null;
			int i=0;

			/* read seeds coordinates */
			while ((line = input.readLine()) != null)
				{
				String[] parts = line.split("\t");		// split the line
				double x = Double.parseDouble(parts[0].trim());
				double y = Double.parseDouble(parts[1].trim());
				AddNewBalloon ((int)x, (int)y);
				}
			}
		catch (IOException e) {
			IJ.error("Could not load the data from file: setStructureFromFile");
			return;
			}
	} /* END importSeeds*/


	 /** remove balloon i*/
	public void remove(int i)
		{ /* BEGIN remove*/
		if (i<N)
			{
			BallList.remove(i);
			N-=1;
			for (int k=0;k<N;k++)
				{
				Balloon B = (Balloon)(BallList.get(k));
				B.id = k;
				}
			}
		} /* END remove*/

	 /** clear all balloons in population*/
	public void clear()
		{ /* BEGIN clear*/

		if (BallList != null)
		{
			BallList.clear();
		}
		is_growing = true;
		N=0;
		} /* END clear*/

	/** load algorithm properties from the properties file*/
    private void loadProperties() {

        InputStream propsFile;
        Properties tempProp = new Properties();

        try {
            //propsFile = new FileInputStream("plugins\\balloonplugin\\BalloonSegmentation.properties");
            propsFile = getClass().getResourceAsStream("/BalloonSegmentation.properties");
            tempProp.load(propsFile);
            propsFile.close();
	}
        catch (IOException ioe) {
            IJ.log("I/O Exception: cannot read .properties file");
        }


            // load properties
            triangle_size_factor = Double.parseDouble(tempProp.getProperty("triangle_size_factor"));
            min_triangle_angle = Double.parseDouble(tempProp.getProperty("min_triangle_angle"));
            crit_inclusion = Double.parseDouble(tempProp.getProperty("crit_inclusion"));
            is_contact_all = Boolean.parseBoolean(tempProp.getProperty("is_contact_all"));
            fine_contact_check = Double.parseDouble(tempProp.getProperty("fine_contact_check"));
            period_contact_check = Integer.parseInt(tempProp.getProperty("period_contact_check"));
            do_contact = Boolean.parseBoolean(tempProp.getProperty("do_contact"));
            optimisation_cycles = Integer.parseInt(tempProp.getProperty("optimisation_cycles"));
            n_random_points = Integer.parseInt(tempProp.getProperty("n_random_points"));
            n_envelop = Integer.parseInt(tempProp.getProperty("n_envelop"));
		use_delaunay = Integer.parseInt(tempProp.getProperty("use_delaunay"));
		use_knn = Integer.parseInt(tempProp.getProperty("use_knn"));
		KN = Integer.parseInt(tempProp.getProperty("KN"));
		max_length = Integer.parseInt(tempProp.getProperty("max_length"));
		max_length = Math.max(max_length,15);
    }

	 /** initialise the contact data structure
	  *  used when loading from file because connectivity is not saved currently (TODO)*/
	public void ConnectExistingBalloons()
		{ /* BEGIN ConnectExistingBalloons*/
		mass_Geometry ();
		if (BallList.size()>0)
			 {
				Balloon B = (Balloon)(BallList.get(0));
				contacts = new int[N][max_n0];
				for (int i=0;i<N;i++) { for (int j=0;j<B.n0;j++) { contacts[i][j] = -10; } }  // no contact
			 }
		else { IJ.log("Cannot initialize the contact detection in population: " + this.id);   }

		topo = new boolean[N][N];
		for (int i = 0; i < N; i++) {
	        for (int j = 0; j < i; j++) {
			topo[i][j] = false;
			if (N < 4 & i!=j) { topo[i][j] = true; }
			}
	        }
			if (N < 4) { for (int i = 0; i < N; i++) { for (int j = 0; j < N; j++) { topo[i][j] = true; } } }
		} /* END ConnectExistingBalloons*/


	 /** initialise the contact data structure after refine**/
	public void ConnectRefinedBalloons()
		{ /* BEGIN ConnectExistingBalloons*/
		mass_Geometry ();
		if (BallList.size()>0)
			 {
				Balloon B = (Balloon)(BallList.get(0));
				int[][]NewContacts = new int[N][max_n0];
				for (int i=0;i<N;i++) { for (int j=0;j<B.n0/2;j++) {
					NewContacts[i][(2*j)%B.n0] = contacts[i][j];
					NewContacts[i][(2*j+1)%B.n0] = contacts[i][j];
					} }
			contacts = NewContacts;
			 }
		else { IJ.log("Cannot initialize the contact detection in population: " + this.id);   }


		} /* END ConnectExistingBalloons*/


	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
//	                  FINDING CELL CENTERS
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	*/

	/** PLace randomly seeds on the image */
		public void sample(float HL) {
			int add;
			for (int rep=0;rep<optimisation_cycles;rep++)
				{
				IJ.showStatus("Sampling point: " + (int)((double)(rep)/(double)(optimisation_cycles)*100) +"%");
				for (int i=0;i<n_random_points;i++)
					{
					int xc = (int)(wmin + Math.random()*(wmax-wmin));
					int yc = (int)(hmin + Math.random()*(hmax-hmin));

					add = 1;
					float crit;
					int n = N;
					if (250<IMB[xc][yc][2])
						{add = 0;}
					else
						{
						for (int j=0;j<n;j++)
							{
							crit = couple(j,xc,yc);

							if (crit<HL)
								{
								add = 0;
								break;
								}
							}
						}
					if(add > 0)
						{
						AddNewBalloon(xc,yc);
						}
					}
				optimize(HL);
				}
			IJ.showStatus("");
			}
	/** Uses the properties of the lines connecting 2 points to decide if these points belong to the same cell	*/
		private float couple(int j, int x, int y) {
			int xj = ((Balloon)(BallList.get(j))).x0;
			int yj = ((Balloon)(BallList.get(j))).y0;
			int xk = x;
			int yk = y;

			float ux = (float)(xk-xj);
			float uy = (float)(yk-yj);
			float length  = (float)Math.sqrt(ux*ux + uy*uy);
			float zmean, zmax;
			ux=ux/length;
			uy=uy/length;

			int sub = 2;
			int[] Pix_max = {xj,yj,ipb.get(xj,yj)};
			int[][] line = new int[(int)length/sub][3];
			int nsub = (int)(length/sub);
			for (int i=0;i<nsub;i++)
				{
				int xi,yi,xii,yii;
				xii = (int)(xk-sub*i*ux);   // for symetry of the criteria
				yii = (int)(yk-sub*i*uy);
				if (i<nsub-1)
					{
					xi = (int)(xj+sub*i*ux);
					yi = (int)(yj+sub*i*uy);
					}
				else
					{
					xi = (int)(xk);
					yi = (int)(yk);
					}

				line[i][0] = (int)xi;
				line[i][1] = (int)yi;
				line[i][2] = (int)ipb.get(xi,yi);
				if (Pix_max[2]<line[i][2])
					{
					Pix_max[0] = (int)xi;
					Pix_max[1] = (int)yi;
					Pix_max[2] = line[i][2];
					}

				if (Pix_max[2]<(int)ipb.get(xii,yii))  // for symetry of the criteria
						{
						Pix_max[0] = (int)xii;
						Pix_max[1] = (int)yii;
						Pix_max[2] = (int)ipb.get(xii,yii);
						}
				}

			if (length<4){
				zmean = 0;
				zmax = 0;
				length = 0;
				}
			else{
				zmean = (float)line[line.length-1][2];
				zmax = Pix_max[2]-zmean;
				}
			float[] features = {zmean,zmax,length};
			return features[1]*features[2]*features[2];
			}

		/** Removes more points to get a better point distribution	*/
		private void optimize(float HL) {
			MakeTopo();					// establish temporary neighbouring through a modified delaunay algorithm
			boolean[] deletion = new boolean[N];
			for (int i=N-1;i>-1;i--)
				{
				int rem = 0;
				double xi = (double)((Balloon)(BallList.get(i))).x0;
				double yi = (double)((Balloon)(BallList.get(i))).y0;
				deletion[i] = false;
				for (int j=0;j<N;j++)
					{
					if (topo[i][j] & i!=j){
						for (int k=0;k<8;k++)
							{
							double angle = k*360./8.*3.14159/180.;
							int xx = (int)(xi + 4.5*Math.cos(angle));
							int yy = (int)(yi + 4.5*Math.sin(angle));
							xx = (int)Math.max(Math.min((double)xx,wmax),wmin);
							yy = (int)Math.max(Math.min((double)yy,hmax),hmin);
							float crit = couple(j,xx,yy);
							if ((crit<HL && deletion[j] == false) | IMB[(int)xx][(int)yy][2]>50)
								{
								deletion[i] = true;
								rem = 1;
								break;
								}
							}
						}
						if (rem == 1){break;}
					}
				}
		for (int i=N-1;i>-1;i--)
				{
				if(deletion[i] == true) { remove(i); }
				}
		}

	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//  Find envelop delimiting the boundaries of the population
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	 */
	/**Find envelop delimiting the boundaries of the population*/
	public void EnvelopBoundaries()
	{
		// find center of the object to be segmented
		int m = 0;
		double xc = 0;
		double yc = 0;
		int w = ipb.getWidth();
		int h = ipb.getHeight();
		for (int i=0;i<w;i+=1)
			{
			for (int j=0;j<h;j+=1)
				{
				if (IMB[i][j][2]<50)
					{
					m+=1;
					xc+=i;
					yc+=j;
					}
				}
			}
		xc = xc/m;
		yc = yc/m;
		double angle;
		int n = n_envelop;
		XXi = new int[n];
		YYi = new int[n];

		// init bounding box of the object to segment
		wmin = ipb.getWidth()/2;
		wmax = ipb.getWidth()/2;
		hmin = ipb.getHeight()/2;
		hmax = ipb.getHeight()/2;
		for (int i=0;i<n;i++)
			{
			angle = (i%n)*360./(n);
			double xr = xc;
			double yr = yc;
			for (int r=1;r<8*w;r++)
				{
				xr = xc + (r*Math.cos(angle*3.14159/180)/8.);
				yr = yc + (r*Math.sin(angle*3.14159/180)/8.);
				if ((xr<2) | (w-2<xr) | (yr<2) | (h-2<yr) | 50 < IMB[(int)xr][(int)yr][2]) {
					XXi[i] = (int)(xr);
					YYi[i] = (int)(yr);
					wmin = Math.min(wmin,xr);
					wmax = Math.max(wmax,xr);
					hmin = Math.min(hmin,yr);
					hmax = Math.max(hmax,yr);
					break;
					}
				}
			}
	}
	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//  Refine the structure of the balloons
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	 */
	/**Refine the structure of the balloons*/
	public void refineStructure()
	{
	for (int i=0;i<N;i++)
		{
		Balloon B1 = (Balloon)(BallList.get(i));
		B1.refineStructure();
		}
	ConnectRefinedBalloons();
	}

	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//  Run balloon algorithm
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	 */
	public void InitiateGrowingRegion()
	{
		for (int i=0;i<N;i++)
		{
		Balloon B1 = (Balloon)(BallList.get(i));
		B1.loadProperties();
		B1.init();
		B1.InitiateGrowingRegion ();
		}
	}

	/** Initiate the matrix of cells in contact (the "contacts" 2D array) by null contacts,
	 * and set the potential connection (the "topo" 2D array) for checking during expansion of balloons by a modified delaunay algorithm*/
	public void MakeTopo()
		{ /* BEGIN MakeTopo*/
		/* initialise the "contacts" matrix with no initial contacts. Cf comments at the declaration of contacts */
		 if (BallList.size()>0)
			{
			Balloon B = (Balloon)(BallList.get(0));
			contacts = new int[N][max_n0];
			for (int i=0;i<N;i++)
				{
				for (int j=0;j<max_n0;j++)
					{
					contacts[i][j] = -10;     // no contact
					}
			B = (Balloon)(BallList.get(i));
				}
			}
		 else
			{
			IJ.log("Could not initialize the contact detection in population: " + this.id);
			}

		/* make delaunay triangulation prior to expansion so that contact is checked only through neighbours */
		 // 1 - Start from a fully connected graph of interactions
		 topo = new boolean[N][N];
		 for (int i = 0; i < N; i++) {
	         for (int j = 0; j < i; j++) {
			topo[i][j] = false;
				if (N < 4 & i!=j)
					{
					topo[i][j] = true;  						// set true for all pairs of cells
					}

			}
			}
			if (N < 4)											// special case for n<4
			{
			for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
					topo[i][j] = true;
				}
			}
			}

		// 2 - Run delaunay algorithm (not the fastest version, but simplest and easier to read)
		if (use_delaunay==1)
		{
			for (int i = 0; i < N; i++) {
		         for (int j = i+1; j < N; j++) {
		             for (int k = j+1; k < N; k++) {
		                 boolean isTriangle = true;
		                 for (int a = 0; a < N; a++) {
		                        if (a == i || a == j || a == k) continue;
								/* For each triangle (Pi,Pj,Pk), check if there is any Pa included in the circle bounding
								the triangle + some tolerance */
		                        Point Pi,Pj,Pk,Pa;
		                        Pi = ((Balloon)BallList.get(i)).getPoint();
		                        Pj = ((Balloon)BallList.get(j)).getPoint();
		                        Pk = ((Balloon)BallList.get(k)).getPoint();
		                        Pa = ((Balloon)BallList.get(a)).getPoint();

		                        Polygon triangle = new Polygon();
		                        if (contains(Pi,Pj,Pk,Pa)) {          // contains is the function that implement a specific criteria for excluding points
						isTriangle = false;
		                           break;
		                        }
		                    }
		                    if (isTriangle == true) {

		                        topo[i][j] = true;
		                        topo[j][i] = true;
		                        topo[i][k] = true;
		                        topo[k][i] = true;
		                        topo[j][k] = true;
		                        topo[k][j] = true;
		                    }

		                }
		            }
		        }
			}

			// 2 - Run nearest neighbour algorithm
		else{
			int[][] BUF = new int[N][2];
			ArrayList<double[]> KNLIST = new ArrayList<double[]>();
		     for (int i = 0; i < N; i++) {
		         for (int j = 0; j < N; j++) {
	                 Point Pi = ((Balloon)BallList.get(i)).getPoint();
	                 Point Pj = ((Balloon)BallList.get(j)).getPoint();
				 double xi = Pi.getX();  									// first point of the triangle
				 double yi = Pi.getY();										//				""
				 double xj = Pj.getX();										// second point of the triangle
				 double yj = Pj.getY();										//				""
	                 double d = (xi-xj)*(xi-xj)+(yi-yj)*(yi-yj);

	                 for (int k=0;k<KN;k++)
	                 {
				 if (k>=KNLIST.size()){
					 if (i!=j){
						 double[] ins = {j,d};
						 KNLIST.add(ins); }
					 break;}

				 if(  (  d<((double[])(KNLIST.get(k)))[1] )  & i!=j)
				 {
					 double[] ins = {j,d};
					 KNLIST.add(k,ins);
					 if (KNLIST.size()>KN){
						 KNLIST.remove(KN);
					 }
					 break;
				 }

		            }
	                 if (KNLIST.size()==0 & i!=j)
	                 {
		                 double[] ins0 = {j,d};
		                 KNLIST.add(ins0);
	                 }
		         }

		         for (int jj = 0; jj < KNLIST.size(); jj++) {
				 int n = (int)(((double[])(KNLIST.get(jj)))[0]);
				 topo[i][n] = true;
				 topo[n][i] = true;
				}
		         KNLIST.clear();
			}
			}

		}	/* END MakeTopo*/

	 /** perform a time step of the physical engine for all balloons in the population by calling the Inflate_inc method in balloon
	  * Checks for contacts **/
	public void Tick_inflate(int PixLevel, int t)
		{ /* BEGIN */
			for (int i=0;i<N;i++)
				{

				Balloon B = (Balloon)(BallList.get(i));

				if (B.docontact == false & t%period_contact_check == 0)						// coarse contact checking
				{Tick_contact(B);}
				else if (B.docontact == true)						// fine contact checking
				{Tick_contact(B);}

				B.PixLevel = PixLevel-1;
				B.Inflate_inc();												// inflate the individual balloon
			}


		} /* END Tick_inflate*/

	public void Tick_inflate(int PixLevel)
	{ /* BEGIN */
		int t=0;
		//
		//Optimize with snake & image laplacian (sobel-based)
		//
		for (int i=0;i<N;i++)
		{
			Balloon B = (Balloon)(BallList.get(i));
			B.init_opt();												// reset internal forces
		}

		for (int k=1;k<150;k++)
		{
		t+=1;
		is_growing = true;//false;
		for (int i=0;i<N;i++)
			{

			Balloon B = (Balloon)(BallList.get(i));

			if (B.docontact == false & t%period_contact_check == 0)						// coarse contact checking
			{Tick_contact(B);}
			else if (B.docontact == true & t%1 == 0)						// fine contact checking
			{Tick_contact(B);}

			B.Optimize_inc();												// inflate the individual balloon

			Double r_ini = (Double)((B.history_radius).get(0)); //Float.valueOf
			Double r_end = (Double)((B.history_radius).get(B.length_history-1)); //
			if (2*(r_end-r_ini)/(r_end + r_ini) <0.0001){B.is_growing = false;}
			if (B.is_growing == true){is_growing = true;}
			}
		if (is_growing == false){break;}
		}


		} /* END Tick_inflate*/

	/** perform a contact check from the potential connection "topo" identified before running the expansion */
	public void Tick_contact(Balloon B)
		{ /* BEGIN */
		int i = B.id;
		for (int k=0;k<B.n0;k++)
			{
			boolean contactb = isEdge((int)B.XX[k],(int)B.YY[k]);
	        //if (B.FIX[k]>0)
	        //   	{B.fix(k);
			//	}
	        if (contactb | B.FIX[k]==10) //
			{B.encastre(k);
				if (contactb) { contacts[i][k] = -1;}
			}
			else
				{
				if (do_contact)
				{
					for (int j=0;j<N;j++)
						{
						Balloon Bj;
						Bj = (Balloon)BallList.get(j);
						double dx = (B.x0-Bj.x0);
						double dy = (B.y0-Bj.y0);
						if (topo[i][j] && i!=j && B.radius + Bj.radius > fine_contact_check*Math.sqrt(dx*dx+dy*dy))
							{
							B.docontact = true;
							Bj.docontact = true;
							boolean contact = Bj.contact(B.XX[k],B.YY[k], B.x0, B.y0);

							if (contact )
								{
									contacts[i][k] = j;
									B.fix(k);
									boolean contain = Bj.contain(B.XX[k],B.YY[k], B.x0, B.y0);
									//IJ.log("test contain:  " + contain ); //
									if (contain){B.encastre(k);}
									B.encastre(k);
									break;
								}
							}
						}
					}
				}
			}
		} /* END Tick_contact*/

	/**
	*	check if point Pa is included in the circle going through points Pi, Pj, Pk
	*  used for setting the potential contacts between different balloon before their expansion
	*/
	private boolean contains(Point Pi, Point Pj, Point Pk, Point Pa)
	 { /* BEGIN contain*/
		double xi = Pi.getX();  									// first point of the triangle
		double yi = Pi.getY();										//				""
		double xj = Pj.getX();										// second point of the triangle
		double yj = Pj.getY();										//				""
		double xk = Pk.getX();										// third point of the triangle
		double yk = Pk.getY(); 										//				""
		double xa = Pa.getX();										// candidate point for being enclosed in the circonscrit circle
		double ya = Pa.getY();										//				""

		/* 1- determines the position of the center of mass */
		// let U:(yi-yj, xj-xi) be the normal vector to Pi, Pj, then (Pi+Pj)/2 + a.U gives the center of circonscrit circle
		double a,b;
		if (yi==yj)
		{
			b = (xj-xk)/(yi-yk)/2;
			a = -((yj-yk)/2 - b*(xk-xi))/(xj-xi);
			}
		else if (yi==yk)
		{
			a = -(yj-yk)/2/(yi-yj);
			b = -((yi-yk)/2 - (xk-xi))/(xj-xi);
			}
		else
		{
			a = (yj-yk)/2 - (xk-xi)*(xj-xk)/(yi-yk)/2;
			a /= -(xj-xi)+(yi-yj)*(xk-xi)/(yi-yk);
			b = (xj-xk)/2/(yi-yk) - a*(yi-yj)/(yi-yk);
		}

		double xc = (xi+xj)/2 + a*(yi-yj);      						// centre of circonscrit circle
		double yc = (yi+yj)/2 + a*(xj-xi);      						// centre of circonscrit circle


		/*	2 - find radius of the circle */
		double ri = Math.sqrt((xi-xc)*(xi-xc) + (yi-yc)*(yi-yc));   	// distance from Pc to Pi
		double rj = Math.sqrt((xj-xc)*(xj-xc) + (yj-yc)*(yj-yc));		// distance from Pc to Pj
		double rk = Math.sqrt((xk-xc)*(xk-xc) + (yk-yc)*(yk-yc)); 		// distance from Pc to Pk
		double ra = Math.sqrt((xa-xc)*(xa-xc) + (ya-yc)*(ya-yc)); 		// distance from Pc to Pa
		double dr = Math.abs(ri-rj) + Math.abs(ri-rk) + Math.abs(rj-rk);// cumulative of deviation from perfect inclusion of the triangle

		double lij = Math.sqrt((xi-xj)*(xi-xj) + (yi-yj)*(yi-yj));   	// distance from Pj to Pi
		double lik = Math.sqrt((xi-xk)*(xi-xk) + (yi-yk)*(yi-yk));   	// distance from Pk to Pi
		double ljk = Math.sqrt((xj-xk)*(xj-xk) + (yj-yk)*(yj-yk));   	// distance from Pk to Pj
		double n1x = (xi-xj)/lij;										// normal vector PiPj
		double n1y = (yi-yj)/lij;
		double n2x = (xi-xk)/lik;										// normal vector PiPk
		double n2y = (yi-yk)/lik;
		double n3x = (xj-xk)/ljk;										// normal vector PjPk
		double n3y = (yj-yk)/ljk;

		double det1 = Math.abs(n1x*n2y - n2x*n1y);						// gives the angle between n1 and n2 (a triangle being to flat contains all point)
		double det2 = Math.abs(n1x*n3y - n3x*n1y);						// gives the angle between n1 and n2 (a triangle being to flat contains all point)
		double det3 = Math.abs(n2x*n3y - n3x*n2y);						// gives the angle between n1 and n2 (a triangle being to flat contains all point)
		double det = Math.min(Math.min(det1,det2),det3);


		/* 3 - different reason for refusing the candidate triangle according to the position of Pa: */
		double size_indic = Math.sqrt(area/N)*triangle_size_factor;		// size indicator of the triangle
																		// (they can exist on the boundaries because of the tolerance on radius)
		boolean size_crit = true;
		double max_l = Math.max(ljk,Math.max(lij,lik));
		if (size_indic>=max_l | area == 0){size_crit=false;}



		//   is in a radius 	// the deviation from // angle between sides
		//   no more than 20% 	// perfect inclusion  // no smaller thant
		//   bigger				// no more than 0.1   // a certain threshold
		if (ra<ri*crit_inclusion			|| dr>0.1  ||  det<min_triangle_angle							||  size_crit)
			 { 	return true;}
		else { return false;}
		}  /* END contain*/

	/** test if the coordinates (ix,iy) are on the edge of the image*/
	private boolean isEdge (int ix, int iy)
		{ /* BEGIN isEdge*/
			int w = ipb.getWidth();  //
			int h = ipb.getHeight();
			if ((ix<2) | (w-2<ix) | (iy<2) | (h-2<iy) ) { return true; }
			else if (50 < IMB[ix][iy][2]) { return true; }
			else { return false; }
		} /* END isEdge*/


	/** set the matrix of inside/outside pixels IMB and compute the area occupied by the colony*/
	public void set_boundaries(int B[][][])
		{ /* BEGIN set_boundaries*/
		IMB = B;
		calc_area();
		} /* END set_boundaries*/

	/** Modify the boundaries given a new set of coordinates*/
	public void modify_boundaries(int[] XXi, int[] YYi)
		{ /* BEGIN modify_boundaries*/
		// set edge of the colony in IMB
		PolygonRoi Proi = new PolygonRoi(XXi,YYi,XXi.length,Roi.POLYGON);
		int sx = IMB.length;
		int sy = IMB[0].length;
		int sz = IMB[0][0].length;
		for (int i=0;i<sx;i++)
			{
			for (int j=0;j<sy;j++)
				{
				if (Proi.contains(i,j)) {IMB[i][j][2]= 0;}
				else {IMB[i][j][2]= 255;}
				}
			}
		calc_area();
		} /* END modify_boundaries*/

	/** compute the area occupied by the colony*/
	public void calc_area()
	{/* BEGIN calc_area*/
		area = 0;
		int sx = IMB.length;
		int sy = IMB[0].length;
		for (int i=0;i<sx;i++)
			{
			for (int j=0;j<sy;j++)
				{
				if (IMB[i][j][2]<5)
					{
					area +=1;
					}
				}
			}
	}/* END calc_area*/


	/** Determines the geometric properties of each cell, by running the mass_geometry method for every cell in the population */
	public void mass_Geometry ()
	{	/* BEGIN mass_Geometry*/
	for (int i=0;i<N;i++)
		{
		Balloon B = (Balloon)(BallList.get(i));
		B.mass_geometry();
		}
	} /* END mass_Geometry*/


	/*//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//
	//	Outputs
	//-------------------------------------------------------------------------------
	//------------------------------------------------------------------------------- */

	/** Use the image prossessor associated with the population of cell and use the Fill_balloon method to add a plot of the each balloon*/
	public ColorProcessor Draw_kinematics ()
	{ /* BEGIN Draw_kinematics*/
		int w = ipb.getWidth();
		int h = ipb.getHeight();
		ColorProcessor cp = new ColorProcessor(w, h);

		// fill the image
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++){
			int pix = ipb.getPixel(x,y);
			int red=pix;
			int green=pix;
			int blue=pix;
			cp.putPixel(x,y, (((int)red & 0xff) <<16)+ (((int)green & 0xff) << 8) + ((int)blue & 0xff));
			}
		}
		ImagePlus imp =  new ImagePlus("Kinematics", cp);

		// draw boundaries
		for (int i=0;i<BallList.size();i++)
		{
			Balloon bal;
			bal = (Balloon)BallList.get(i);
			bal.Fill_balloon(imp);
		}

		cp = (ColorProcessor)(imp.getProcessor());
		return cp;
	} /* END Draw_kinematics*/

	/** Use the image prossessor associated with the population of cell and use the Fill_balloon method to add a plot of the each balloon*/
	public ColorProcessor Draw_lineage(int seq)
	{ /* BEGIN Draw_lineage*/
		int w = ipb.getWidth();
		int h = ipb.getHeight();
		ColorProcessor cp = new ColorProcessor(w, h);

		// fill the image
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++){
			int pix = ipb.getPixel(x,y);
			int red=pix;
			int green=pix;
			int blue=pix;
			cp.putPixel(x,y, (((int)red & 0xff) <<16)+ (((int)green & 0xff) << 8) + ((int)blue & 0xff));
			}
		}
		ImagePlus imp =  new ImagePlus("Generation", cp);

		// draw boundaries
		for (int i=0;i<BallList.size();i++)
		{
			Balloon bal = (Balloon)BallList.get(i);
			if (bal.id_line == 2) {
				bal.Fill_balloon(imp, Math.min((int)(Math.pow(bal.n_generation,2)*10),255));
				}
			else  {bal.Fill_balloon(imp, Math.min(0,255));}
		}

		cp = (ColorProcessor)(imp.getProcessor());
		return cp;
	} /* END Draw_lineage*/

	/**Produce an array with centers of balloons*/
	public int[][] CenterList()
	{
		int [][] LIST = new int[N][2];
		for (int i=0;i<N;i++)
		{
			Balloon B = (Balloon)(BallList.get(i));
			LIST[i][0] = B.x0;
			LIST[i][1] = B.y0;
		}
		return LIST;
	}
}  /* END Balloon population */
