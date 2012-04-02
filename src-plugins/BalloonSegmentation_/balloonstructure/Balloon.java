package balloonstructure;

/*********************************************************************
 * Version: January, 2008
 *
 * Balloon Class
 * This class defines the properties and behaviour of a 2D generic balloon for the segmentation of multicellular imaging data.
 * Balloon are submitted to increasing internal pressure and external pressure depending on the pixel intensity.
 * These forces translates in deformation (elastic and inelastic) of the balloon which progrssively follows the shapes of the cells.
 *
 * TODOS:
 * - using one single vector/matrix library in all the code
 * - outputs not kinematics-oriented
 * - loading of segmented data can inflate again
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

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.awt.image.*;
import java.util.*;
import Jama.*;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.lang.System;

public class Balloon extends Thread  	/* BEGIN Balloon class definition */
{
	/* model parameter */
	double stiffness = 0.2;																					// spring stiffness
	double plasticity_factor = 0.01;																		// add some permanent deformation
	double BendStiff = 0.05;																				// stiffness inf rotation
	double stretch = 30.;  																					// maximum wall streching ratio
	double interface_width = 4;																				// width of the interface (stops the expansion)
	double mass = 1;																						// mass of points
	double dt = 0.05;																						// time increment
	double visc = 2.;																						// viscous component of point motion
	public int n0 = 15;           																					// number of vertex
	public double Radius0 = 4.;																					// initial ration of the balloon



	/* balloon shape and dynamical properties */
	public int id;																									// id of the vert																							// array of Y coordinates of the vertices defining the balloon (int for interface)
	public double[] XX;																							// array of X coordinates of the vertices defining the balloon (double for computation)
	public double[] YY;																							// array of Y coordinates of the vertices defining the balloon (double for computation)
	private double[] L0s;																					// natural length of springs (for inelastic deformation)
	private double[] VX;																					// array of vert X velocities
	private double[] VY;																					// array of vert Y velocities
	private double[] VX0;																					// X velocities at time t-1
	private double[] VY0;																					// Y velocities at time t-1																		//        //
	public int[] FIX;																								// fixed points e.g. due to contact with neighbours
	public int x0;   																							// center of the balloon
	public int y0;
	BalloonPopulation POP;
	public PolygonRoi Proi;																					// ROI for the balloon
	/* initial balloon properties */
	private double Length0 = Radius0*Math.sqrt( Math.pow((Math.cos(3.14159*2/n0) - Radius0) , 2) + Math.pow(Math.sin(3.14159*2/n0) , 2) );//6.3/n0*Radius0;		// initial length of spring

	private double angle0 = -3.14159;// - 2*Math.atan((1-Math.cos(3.1416/n0))/Math.sin(3.1416/n0));				// angle between springs (bending forces)
	private double pressure = (6.3*stiffness/n0-Length0/Radius0)*2; 			 								// pressure to inflate (set so that it is in equilibrium)
	double PressRatio = 0.05;																				// how pixel intensity applies pressure on normals
	public double PixLevel = 0;


	public double Ixx;																						// components of the intertia matrix
	public double Iyy;
	public double Ixy;
	public double lx;																						// size of cell in the x / y direction
	public double ly;

	public double area;																							// area of the cell
	public double radius;																							// approx radius of the balloon
	public double radius_min;																							// approx radius of the balloon

	public boolean docontact = false;																				// with or without contact

	/* Images & properties*/
	ImageProcessor ipb;																						// Image processor for getting pixel intensity
	ImageProcessor ip_gradx_p;				// Image processor for the gradient X of the image
	ImageProcessor ip_grady_p;				// Image processor for the gradient Y of the image
	ImageProcessor ip_gradx_m;				// Image processor for the gradient X of the image
	ImageProcessor ip_grady_m;				// Image processor for the gradient Y of the image
	int channel = 1;					// channel to be used for initiation resp (1,2,3 for R,G,B)

	int w;
	int h;

	/* Tensor components */
	public double sig_vol;																							// volumetric straining
	public Matrix StrainVector;																					// principal axis
	public double[] StrainValues;																					//
	public Matrix StrainTensor = new Matrix(2,2); 																	// strain tensor


	/* lineage  */
	public int id_mother = -1;																						// id of the mother cell
	public int n_generation = 0;																					// number of division prior to the birth or this particular cell(i.e. generation of the cell)
	public int id_line = -1;																						// number that all descedants of a mother cell share
	public int div_x0;																		// segement defining the cell division plane
	public int div_y0;
	public int div_x1;
	public int div_y1;

	// convergence of deformation
	public boolean is_growing = true;
	int length_history = 30;
	public ArrayList<Double> history_radius = new ArrayList<Double>(); 	// array of balloons radius

	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//	                  CONSTRUCTOR
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	*/
	/** Balloon Constructor used before inflation: need initial spatial coordinates (x,y) and ID of the balloon to be given **/
	public Balloon(int x, int y, int ID, ImageProcessor ip, BalloonPopulation BP, int rgbchannel)
	{ /* BEGIN Balloon*/

		channel = rgbchannel;
		ip_gradx_p = BP.ip_gradx_p;
		ip_grady_p = BP.ip_grady_p;
		ip_gradx_m = BP.ip_gradx_m;
		ip_grady_m = BP.ip_grady_m;
		POP = BP;

		init(x,y,ID,ip);
		loadProperties();
		InitiateGrowingRegion ();
		float sti = 0;
		for (int i=0;i<length_history;i++)
		{
			history_radius.add(sti*0.05 + sti);
		}
	 } /* END Balloon*/

	/** Balloon Constructor with of previously shaped structure: need list spatial coordinates (x,y) of the vertices of the envelop and ID of the balloon to be given **/
	 public Balloon(int[] Ax, int[] Ay, int ID, ImageProcessor ip, BalloonPopulation BP, int rgbchannel)
	 { /* BEGIN Balloon*/
		loadProperties();
		channel = rgbchannel;
		ip_gradx_p = BP.ip_gradx_p;
		ip_grady_p = BP.ip_grady_p;
		ip_gradx_m = BP.ip_gradx_m;
		ip_grady_m = BP.ip_grady_m;
		POP = BP;

		init(0,0,ID,ip);
		n0 = Ax.length;
		XX = new double[n0];  YY = new double[n0];
		VX = new double[n0]; VY = new double[n0];  VX0 = new double[n0]; VY0 = new double[n0];
		FIX = new int[n0];
		L0s = new double[n0];
		for (int i=0;i<n0;i++)
			{
			// use the list of spatial coordinate for setting the vertices spatial coordinates
			XX[i] = (double)(Ax[i]);
			YY[i] = (double)(Ay[i]);

			// set dynamical terms to 0
			VX0[i] = 0;
			VY0[i] = 0;
			VX[i] = 0;
			VY[i] = 0;
			FIX[i] = 0;
			L0s[i] = Length0;
			}
		// compute the gemetric properties of the cell
		mass_geometry();

		float sti = 0;
		for (int i=0;i<length_history;i++)
		{
			history_radius.add(sti*0.05 + sti);
		}

	 } /*END Balloon*/

	 /** Init function used in all Constructors of balloon **/
	private void init(int x, int y, int ID, ImageProcessor ip)
		{ /* BEGIN init*/
			ipb =  ip; //POP.ipEdge; //
			w = ipb.getWidth();
			h = ipb.getHeight();

			x0=x;
			y0=y;
																				// array of Y coordinates of the vertices defining the balloon (int for interface)
			XX= new double[n0];																			// array of X coordinates of the vertices defining the balloon (double for computation)
			YY= new double[n0];																			// array of Y coordinates of the vertices defining the balloon (double for computation)
			L0s = new double[n0];																	// natural length of springs (for inelastic deformation)
			VX = new double[n0];																	// array of vert X velocities
			VY = new double[n0];																	// array of vert Y velocities
			VX0 = new double[n0];																	// X velocities at time t-1
			VY0 = new double[n0];																	// Y velocities at time t-1																		//        //
			FIX = new int[n0];																				// fixed points e.g. due to contact with neighbours


			Ixx = 0; Iyy = 0; Ixy = 0;
			lx=0;ly=0;
			id = ID;
			id_mother = ID;
			id_line = ID;
			double[] eigValues = {0,0};
			double[][] eigVectors = {{0,0},{0,0}};

			StrainVector = new Matrix(eigVectors);
			StrainValues = eigValues;
			sig_vol =0;
		 } /* END init*/

	/** Init function used to re-init ballons (called with InitiateGrowingRegion() but without  the constructor)*/
	public void init()
	{ /* BEGIN init*/
																	// array of Y coordinates of the vertices defining the balloon (int for interface)
		XX= new double[n0];																			// array of X coordinates of the vertices defining the balloon (double for computation)
		YY= new double[n0];																			// array of Y coordinates of the vertices defining the balloon (double for computation)
		L0s = new double[n0];																	// natural length of springs (for inelastic deformation)
		VX = new double[n0];																	// array of vert X velocities
		VY = new double[n0];																	// array of vert Y velocities
		VX0 = new double[n0];																	// X velocities at time t-1
		VY0 = new double[n0];																	// Y velocities at time t-1																		//        //
		FIX = new int[n0];																				// fixed points e.g. due to contact with neighbours


		Ixx = 0; Iyy = 0; Ixy = 0;
		lx=0;ly=0;

		double[] eigValues = {0,0};
		double[][] eigVectors = {{0,0},{0,0}};

		StrainVector = new Matrix(eigVectors);
		StrainValues = eigValues;
		sig_vol =0;
	 } /* END init*/

	/** Load properties from file*/

	public void run() {}

	public void loadProperties() {

        InputStream propsFile;
        Properties tempProp = new Properties();

        try {
            //propsFile = new FileInputStream("plugins\\balloonplugin\\BalloonSegmentation.properties");
            propsFile = getClass().getResourceAsStream("/BalloonSegmentation.properties");
            tempProp.load(propsFile);
            propsFile.close();

            // load properties
            stiffness = Double.parseDouble(tempProp.getProperty("stiffness"));
		plasticity_factor = Double.parseDouble(tempProp.getProperty("plasticity_factor"));
		BendStiff = Double.parseDouble(tempProp.getProperty("BendStiff"));
		//int max_length = (int)(Integer.parseInt(tempProp.getProperty("stretch")));
		//POP.max_length
		interface_width = Double.parseDouble(tempProp.getProperty("interface_width"));
		mass = Double.parseDouble(tempProp.getProperty("mass"));
		dt = Double.parseDouble(tempProp.getProperty("dt"));
		visc = Double.parseDouble(tempProp.getProperty("visc"));
		n0 = (int)(Integer.parseInt(tempProp.getProperty("n0")));
		Radius0 = Double.parseDouble(tempProp.getProperty("Radius0"));
		}
        catch (IOException ioe) {
            IJ.error("I/O Exception: cannot read .properties file");
        }
    }


	 /*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//	                  PHYSICAL ENGINE
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	*/

	/** Initiatiation Balloons as circles before inflation **/
	 public void InitiateGrowingRegion ()
			{ /* BEGIN InitiateGrowingRegion*/
			// set up initial balloon
		    Length0 = Radius0*Math.sqrt( Math.pow((Math.cos(3.14159*2/n0) - 1) , 2) + Math.pow(Math.sin(3.14159*2/n0) , 2) );
			float sti = 0;
			for (int i=0;i<length_history;i++)
			{
				history_radius.add(sti*0.05 + sti);
			}
			double angle;
			for (int i=0;i<n0;i++)
				{
				angle = i*360./(n0);
				XX[i] = x0 + (Radius0*Math.cos(angle*3.14159/180));
				YY[i] = y0 + (Radius0*Math.sin(angle*3.14159/180));
				VX0[i] = 0;
				VY0[i] = 0;
				VX[i] = 0;
				VY[i] = 0;
				FIX[i] = 0;
				L0s[i] = Length0;
				}
			}	/* END InitiateGrowingRegion*/

	 /** perform a time step of the physical engine. Computes the deformation of the balloon according to pressure and resistance generated by the image topography **/
	 public void Inflate_inc()
		{	/* BEGIN Inflate_inc */
			//  initiate variables
			int i,j,n;						// iterators/ index and stuff
			n = n0;
			/*int id_min_crit = -1;			// id and criteria of vertex having the lowest criteria for redistributing verts
			double min_crit = 10000;		//
			int id_max_crit=-1;				// id of vertex having the highest criteria for redistributing verts
			double max_crit=0;				//
			double crit;					// current node criteria
			*/
			double[] F= new double[3];		// Nodal Force
			double[] NX= new double[3];		// Nodal tangent vector (averaging NX1 & NX2)
			double[] NX1= new double[3];	// Edge1 tangent vector
			double[] NX2= new double[3];	// Edge2 tangent vector
			double[] NY= new double[3];		// Nodal normal vector (outward normal vector of NX)
			double[] NZ= {0,0,-1};			// Nodal Z axis
			double Dl2 = 0;					// Edge1 expansion
			double Dl1 = 0;					// Edge2 espansion
			double x = 0;					// temporary new balloon center X
			double y = 0;					// temporary new balloon center Y
			int iim1,ii0,ii1,ii2,ii3;		// vertex indices
			//						  curr. vert
			//					... <= ... | ... => ...
			// ----- iim1 ----- ii0 ----- ii1 ----- ii2 ----- ii3
			//						 L1  		L2
			//if (is_growing)
			//{
				for (i=0;i<n0;i++) /* BEGIN loop over the nodes of the structure */
				{

					if (FIX[i]<10)  // motion onnly if no contact
					{

						/* compute normal and nodal force*/
						ii3 = (n+i+2)%n;
						ii2 = (n+i+1)%n;
						ii1 = (i+n)%n;
						ii0 = (i-1+n)%n;
						iim1 = (i-2+n)%n;
						double L2 = getSegmentLength(ii1);
						double L1 = getSegmentLength(ii0);
						double L20 = Math.sqrt(Math.pow((XX[ii2]-VX0[ii2]*dt)-(XX[ii1]-VX0[ii1]*dt),2) + Math.pow((YY[ii2]-VY0[ii2]*dt)-(YY[ii1]-VY0[ii1]*dt),2));
						double L10 = Math.sqrt(Math.pow((XX[ii1]-VX0[ii1]*dt)-(XX[ii0]-VX0[ii0]*dt),2) + Math.pow((YY[ii1]-VY0[ii1]*dt)-(YY[ii0]-VY0[ii0]*dt),2));

						Dl2 = (L2-L0s[i])*stiffness;									// elastic deformation of the first spring connected to vertex i
						Dl1 = (L1-L0s[i])*stiffness;									//
						L0s[i] += (L2-L0s[i])*plasticity_factor;						// add some plasticity

						NX2[0] = (XX[ii2]-XX[ii1]);
						NX2[1] = (YY[ii2]-YY[ii1]);
						NX2[2] = 0;
						NX1[0] = (XX[ii1]-XX[ii0]);
						NX1[1] = (YY[ii1]-YY[ii0]);
						NX1[2] = 0;

						NX = prod(add(NX1,NX2),0.5); 									// averaging
						NY = cross(NZ,NX);												// normal vector
						double[] NR = {XX[i]-x0,YY[i]-y0,0};							// Nodal radial vector
						NR = normalized(NR);											// XXX

						/* spring forces in balloon edges*/
						double force = Math.max((PixLevel-(double)ipb.get((int)(XX[i]+0.5),(int)(YY[i]+0.5)))*PressRatio,0);
						F = prod(NY, force);  		// internal pressure //

						if (Dl1>0 & Dl2>0)
						{
						F = add(F,prod(NX2,Dl2)); 										// spring force 1 (elasticity) //
						F = add(F,prod(NX1,-Dl1)); 										// spring force 2 (elasticity)//
						}
						/* roational spring forces at vertices*/
						double angle2 = angle(XX[ii3]-XX[ii2],YY[ii3]-YY[ii2],XX[ii1]-XX[ii2],YY[ii1]-YY[ii2]);
						double angle1 = angle(XX[ii2]-XX[ii1],YY[ii2]-YY[ii1],XX[ii0]-XX[ii1],YY[ii0]-YY[ii1]);
						double angle0 = angle(XX[ii1]-XX[ii0],YY[ii1]-YY[ii0],XX[iim1]-XX[ii0],YY[iim1]-YY[ii0]);
						double rota2 = 3.14159 - angle2;
						double rota1 = 3.14159 - angle1;
						double rota0 = 3.14159 - angle0;
						rota2 = Math.pow(rota2,3)*0.002;
						rota1 = Math.pow(rota1,3)*0.002;
						rota0 = Math.pow(rota0,3)*0.002;


						double LN = (L1+L2)/2;
						if (radius_min/Math.abs(LN*Math.cos(3.14159 - angle1))>45)
						{
							XX[ii1] += ((XX[ii0]+XX[ii2])/2. - XX[ii1]);
							YY[ii1] += ((YY[ii0]+YY[ii2])/2. - YY[ii1]);
							VX0[ii1] = 0;
							VY0[ii1] = 0;
							F[0] = 0;//add(F,prod(NY,BendStiff*rota2/LN)); 						//   /L2
							F[1] = 0;//add(F,prod(NY,BendStiff*rota0/LN)); 						//   /L1
							F[2] = 0;//sub(F,prod(NY,2*BendStiff*rota1/LN) );

						}
						/*else if ((3.14159 - angle1)<-0.5)
						{
							XX[ii1] = (XX[ii0]-XX[ii2]);
							YY[ii1] = (YY[ii0]-YY[ii2]);
							VX0[ii1] = 0;
							VY0[ii1] = 0;
							F[0] = 0;//add(F,prod(NY,BendStiff*rota2/LN)); 						//   /L2
							F[1] = 0;//add(F,prod(NY,BendStiff*rota0/LN)); 						//   /L1
							F[2] = 0;//sub(F,prod(NY,2*BendStiff*rota1/LN) );

						}*/
						else
						{
							F = add(F,prod(NY,BendStiff*rota2/LN)); 						//   /L2
							F = add(F,prod(NY,BendStiff*rota0/LN)); 						//   /L1
							F = sub(F,prod(NY,2*BendStiff*rota1/LN) );
						}

						if (FIX[i] > 0 & FIX[i]<10)
						{
							if (dot(F,NX2)>0)
							{
								NX2 = normalized(NX2);
								F = prod(NX2,dot(F,NX2));
								FIX[i]=0;
							}
							else
							{
								NX1 = normalized(NX1);
								F = prod(NX1,dot(F,NX1));
								FIX[i]=0;
							}
						}

						/* point mass */
						if (FIX[i]<10)
							{
							VX[i] = VX0[i] + (F[0] - visc*VX0[i])*dt/mass;    				// Principle of dynamics X: forces = acceleration*mass
							VY[i] = VY0[i] + (F[1] - visc*VY0[i])*dt/mass;	  				// Principle of dynamics Y: forces = acceleration*mass
							XX[i] += VX[i]*dt;												// update position of vertex X
							YY[i] += VY[i]*dt;												// update position of vertex Y
							VX0[i] = VX[i];
							VY0[i] = VY[i];
							}
					}

				/* find cell center approximately */
				x += XX[i]/n;
				y += YY[i]/n;

				/* find the radius (used to initiate search for contacts) */
				double dx = (XX[i]-(double)x0);
				double dy = (YY[i]-(double)y0);
				radius = Math.max(radius, Math.sqrt(dx*dx + dy*dy));
				radius_min = Math.min(radius, Math.sqrt(dx*dx + dy*dy));
				}	 /* END loop over the nodes of the structure */

		// update history
		history_radius.add(radius);
		history_radius.remove(0);
		//
		/* update the center of the balloon */
		x0 = (int)Math.round(x);
		y0 = (int)Math.round(y);
		}/* END Inflate_inc */
	 /** perform a time step for optimal positioning of the balloon (snake)**/
	 public void Optimize_inc()
		{	/* BEGIN Inflate_inc */
			//  initiate variables

			int i,j,n;						// iterators/ index and stuff
			n = n0;
			/*int id_min_crit = -1;			// id and criteria of vertex having the lowest criteria for redistributing verts
			double min_crit = 10000;		//
			int id_max_crit=-1;				// id of vertex having the highest criteria for redistributing verts
			double max_crit=0;				//
			double crit;					// current node criteria
			*/
			double[] F= new double[3];		// Nodal Force
			double[] NX= new double[3];		// Nodal tangent vector (averaging NX1 & NX2)
			double[] NX1= new double[3];	// Edge1 tangent vector
			double[] NX2= new double[3];	// Edge2 tangent vector
			double[] NY= new double[3];		// Nodal normal vector (outward normal vector of NX)
			double[] NZ= {0,0,-1};			// Nodal Z axis
			double Dl2 = 0;					// Edge1 expansion
			double Dl1 = 0;					// Edge2 espansion
			double x = 0;					// temporary new balloon center X
			double y = 0;					// temporary new balloon center Y
			int iim1,ii0,ii1,ii2,ii3;		// vertex indices
			//						  curr. vert
			//					... <= ... | ... => ...
			// ----- iim1 ----- ii0 ----- ii1 ----- ii2 ----- ii3
			//						 L1  		L2
			//if (is_growing)
			//{
				for (i=0;i<n0;i++) /* BEGIN loop over the nodes of the structure */
				{

					boolean is_out_of_bound = false;
					if (XX[i]<=0){XX[i]=0;is_out_of_bound = true;}
					if (XX[i]>=w-1){XX[i]=w-1;is_out_of_bound = true;}
					if (YY[i]<=0){YY[i]=0;is_out_of_bound = true;}
					if (YY[i]>=h-1){YY[i]=h-1;is_out_of_bound = true;
					}
					if (FIX[i]<1 | !(is_out_of_bound))  // motion onnly if no contact
					{

						// compute normal and nodal force //
						ii3 = (n+i+2)%n;
						ii2 = (n+i+1)%n;
						ii1 = (i+n)%n;
						ii0 = (i-1+n)%n;
						iim1 = (i-2+n)%n;
						double L2 = getSegmentLength(ii1);
						double L1 = getSegmentLength(ii0);
						double L20 = Math.sqrt(Math.pow((XX[ii2]-VX0[ii2]*dt)-(XX[ii1]-VX0[ii1]*dt),2) + Math.pow((YY[ii2]-VY0[ii2]*dt)-(YY[ii1]-VY0[ii1]*dt),2));
						double L10 = Math.sqrt(Math.pow((XX[ii1]-VX0[ii1]*dt)-(XX[ii0]-VX0[ii0]*dt),2) + Math.pow((YY[ii1]-VY0[ii1]*dt)-(YY[ii0]-VY0[ii0]*dt),2));

						Dl2 = (L2-L0s[i])*stiffness;									// elastic deformation of the first spring connected to vertex i
						Dl1 = (L1-L0s[i])*stiffness;									//

						NX2[0] = (XX[ii2]-XX[ii1]);
						NX2[1] = (YY[ii2]-YY[ii1]);
						NX2[2] = 0;
						NX1[0] = (XX[ii1]-XX[ii0]);
						NX1[1] = (YY[ii1]-YY[ii0]);
						NX1[2] = 0;

						NX = prod(add(NX1,NX2),0.5); 									// averaging
						NY = cross(NZ,NX);												// normal vector
						double[] NR = {XX[i]-x0,YY[i]-y0,0};							// Nodal radial vector
						NR = normalized(NR);											// XXX

						// spring forces in balloon edges //
						F[0] = -(ip_gradx_p.get((int)(XX[i]+0.5),(int)(YY[i]+0.5)) - ip_gradx_m.get((int)(XX[i]+0.5),(int)(YY[i]+0.5)))*PressRatio;
						F[1] = -(ip_grady_p.get((int)(XX[i]+0.5),(int)(YY[i]+0.5)) - ip_grady_m.get((int)(XX[i]+0.5),(int)(YY[i]+0.5)))*PressRatio;
						F[2] = 0;

						if (Dl1>0 & Dl2>0)
						{
							F = add(F,prod(NX2,Dl2)); 										// spring force 1 (elasticity) //
							F = add(F,prod(NX1,-Dl1)); 										// spring force 2 (elasticity)//
						}
						/* roational spring forces at vertices*/
						double angle2 = angle(XX[ii3]-XX[ii2],YY[ii3]-YY[ii2],XX[ii1]-XX[ii2],YY[ii1]-YY[ii2]);
						double angle1 = angle(XX[ii2]-XX[ii1],YY[ii2]-YY[ii1],XX[ii0]-XX[ii1],YY[ii0]-YY[ii1]);
						double angle0 = angle(XX[ii1]-XX[ii0],YY[ii1]-YY[ii0],XX[iim1]-XX[ii0],YY[iim1]-YY[ii0]);
						double rota2 = 3.14159 - angle2;
						double rota1 = 3.14159 - angle1;
						double rota0 = 3.14159 - angle0;
						rota2 = Math.pow(rota2,3)*0.002;
						rota1 = Math.pow(rota1,3)*0.002;
						rota0 = Math.pow(rota0,3)*0.002;

						double LN = (L1+L2)/2;
						if (radius_min/(LN*Math.cos(3.14159 - angle1))>45)
						{
							XX[ii1] += ((XX[ii0]+XX[ii2])/2. - XX[ii1]);
							YY[ii1] += ((YY[ii0]+YY[ii2])/2. - YY[ii1]);
							VX0[ii1] = 0;
							VY0[ii1] = 0;
							F[0] = 0;//add(F,prod(NY,BendStiff*rota2/LN)); 						//   /L2
							F[1] = 0;//add(F,prod(NY,BendStiff*rota0/LN)); 						//   /L1
							F[2] = 0;//sub(F,prod(NY,2*BendStiff*rota1/LN) );

						}
						/*else if ((3.14159 - angle1)<-1)
						{
							XX[ii1] = (XX[ii0]-XX[ii2]);
							YY[ii1] = (YY[ii0]-YY[ii2]);
							VX0[ii1] = 0;
							VY0[ii1] = 0;
							F[0] = 0;//add(F,prod(NY,BendStiff*rota2/LN)); 						//   /L2
							F[1] = 0;//add(F,prod(NY,BendStiff*rota0/LN)); 						//   /L1
							F[2] = 0;//sub(F,prod(NY,2*BendStiff*rota1/LN) );

						}*/
						else
						{
							F = add(F,prod(NY,BendStiff*rota2/LN)); 						//   /L2
							F = add(F,prod(NY,BendStiff*rota0/LN)); 						//   /L1
							F = sub(F,prod(NY,2*BendStiff*rota1/LN) );
						}


						if (FIX[i] > 0 & FIX[i]<10)
						{
							if (dot(F,NX2)>0)
							{
								NX2 = normalized(NX2);
								F = prod(NX2,dot(F,NX2));
								FIX[i]=0;

							}
							else
							{
								NX1 = normalized(NX1);
								F = prod(NX1,dot(F,NX1));
								FIX[i]=0;

							}
						}

						// point mass //
						if (FIX[i]<10)						// vertex is not fixed
							{

							VX[i] = VX0[i] + (F[0] - visc*VX0[i]*10)*dt/mass*0.3;    				// Principle of dynamics X: forces = acceleration*mass
							VY[i] = VY0[i] + (F[1] - visc*VY0[i]*10)*dt/mass*0.3;	  				// Principle of dynamics Y: forces = acceleration*mass
							XX[i] += Math.min(VX[i]*dt,0.05*Length0);												// update position of vertex X
							YY[i] += Math.min(VY[i]*dt,0.05*Length0);												// update position of vertex Y


							VX0[i] = VX[i];
							VY0[i] = VY[i];
							}

						// point mass //
						//VX[i] = VX0[i] + (F[0] - visc*VX0[i])*dt/mass*0.1;    				// Principle of dynamics X: forces = acceleration*mass
						//VY[i] = VY0[i] + (F[1] - visc*VY0[i])*dt/mass*0.1;	  				// Principle of dynamics Y: forces = acceleration*mass
						//XX[i] += Math.min(VX[i]*dt,0.05*L0s[i]);												// update position of vertex X
						//YY[i] += Math.min(VY[i]*dt,0.05*L0s[i]);												// update position of vertex Y
						//VX0[i] = VX[i];
						//VY0[i] = VY[i];


					/*
						// compute normal and nodal force //
						ii3 = (n+i+2)%n;
						ii2 = (n+i+1)%n;
						ii1 = (i+n)%n;
						ii0 = (i-1+n)%n;
						iim1 = (i-2+n)%n;
						double L2 = getSegmentLength(ii1);
						double L1 = getSegmentLength(ii0);
						double L20 = Math.sqrt(Math.pow((XX[ii2]-VX0[ii2]*dt)-(XX[ii1]-VX0[ii1]*dt),2) + Math.pow((YY[ii2]-VY0[ii2]*dt)-(YY[ii1]-VY0[ii1]*dt),2));
						double L10 = Math.sqrt(Math.pow((XX[ii1]-VX0[ii1]*dt)-(XX[ii0]-VX0[ii0]*dt),2) + Math.pow((YY[ii1]-VY0[ii1]*dt)-(YY[ii0]-VY0[ii0]*dt),2));

						Dl2 = (L2-L0s[i])*stiffness;									// elastic deformation of the first spring connected to vertex i
						Dl1 = (L1-L0s[i])*stiffness;									//
						//L0s[i] += (L2-L0s[i])*plasticity_factor;						// add some plasticity

						NX2[0] = (XX[ii2]-XX[ii1]);
						NX2[1] = (YY[ii2]-YY[ii1]);
						NX2[2] = 0;
						NX1[0] = (XX[ii1]-XX[ii0]);
						NX1[1] = (YY[ii1]-YY[ii0]);
						NX1[2] = 0;

						NX = prod(add(NX1,NX2),0.5); 									// averaging
						NY = cross(NZ,NX);												// normal vector
						double[] NR = {XX[i]-x0,YY[i]-y0,0};							// Nodal radial vector
						NR = normalized(NR);											// XXX

						// spring forces in balloon edges //
						F[0] = -(ip_gradx_p.get((int)XX[i],(int)YY[i]) - ip_gradx_m.get((int)XX[i],(int)YY[i]))*PressRatio*0.1;
						F[1] = -(ip_grady_p.get((int)XX[i],(int)YY[i]) - ip_grady_m.get((int)XX[i],(int)YY[i]))*PressRatio*0.1;
						F[2] = 0;

						F = add(F,prod(NX2,Dl2*0.1)); 										// spring force 1 (elasticity) //
						F = add(F,prod(NX1,-Dl1*0.1)); 										// spring force 2 (elasticity)//

						// roational spring forces at vertices //
						double rota2 = angle0 - angle(XX[ii3]-XX[ii2],YY[ii3]-YY[ii2],YY[ii3]-YY[ii2],YY[ii1]-YY[ii2]);
						double rota0 = angle0 - angle(XX[ii1]-XX[ii0],YY[ii1]-YY[ii0],XX[iim1]-XX[ii0],YY[iim1]-YY[ii0]);
						F = add(F,prod(NY,BendStiff*rota2/L2*0.1)); 						//
						F = add(F,prod(NY,BendStiff*rota0/L1*0.1)); 						//

						if (FIX[i] > 0 & FIX[i]<10)
						{
							if (dot(F,NX2)>0)
							{
								NX2 = normalized(NX2);
								F = prod(NX2,dot(F,NX2));
								FIX[i]=0;

							}
							else
							{
								NX1 = normalized(NX1);
								F = prod(NX1,dot(F,NX1));
								FIX[i]=0;

							}
						}

						// point mass //
						if (FIX[i]<10)						// vertex is not fixed
							{

							VX[i] = VX0[i] + (F[0] - visc*VX0[i]*10)*dt/mass*0.3;    				// Principle of dynamics X: forces = acceleration*mass
							VY[i] = VY0[i] + (F[1] - visc*VY0[i]*10)*dt/mass*0.3;	  				// Principle of dynamics Y: forces = acceleration*mass
							XX[i] += Math.min(VX[i]*dt,0.05*Length0);												// update position of vertex X
							YY[i] += Math.min(VY[i]*dt,0.05*Length0);												// update position of vertex Y


							VX0[i] = VX[i];
							VY0[i] = VY[i];
							}
					*/
					}

				// find cell center approximately //
				x += XX[i]/n;
				y += YY[i]/n;

				// find the radius (used to initiate search for contacts) //
				double dx = (XX[i]-(double)x0);
				double dy = (YY[i]-(double)y0);

				radius = Math.max(radius, Math.sqrt(dx*dx + dy*dy));
				radius_min = Math.min(radius, Math.sqrt(dx*dx + dy*dy));
			}	 /* END loop over the nodes of the structure */

		// update history
		history_radius.add(radius);
		history_radius.remove(0);
		//
		/* update the center of the balloon */
		x0 = (int)Math.round(x);
		y0 = (int)Math.round(y);
		}/* END Inflate_inc */

	 /** rest prior to optimisation step (snake) */
	 public void init_opt()
	 {
			for (int i=0;i<n0;i++) /* BEGIN loop over the nodes of the structure */
			{
				/* compute normal and nodal force*/
				int ii1 = (i+n0)%n0;
				double L2 = getSegmentLength(ii1);
				L0s[i] = L2;
				VX0[i] = 0;
				VY0[i] = 0;
				VX[i] = 0;
				VY[i] = 0;
				FIX[i] = 0;
			}
	 }


	 /** reshape balloon prior to optimisation step (snake) */
	 public void refineStructure()
	 {
		int n1 = n0;

		ArrayList<Double> Xnew = new ArrayList<Double>();
		ArrayList<Double> Ynew = new ArrayList<Double>();

		for (int i=1;i<n0+1;i++) {
			int ii1 = (i+n0)%n0;
			int ii0 = (i-1);
			int iim1 = (i+n0-2)%n0;

			double leni = Math.sqrt(Math.pow((XX[i%n0]-XX[(i-1)]),2) + Math.pow((YY[i%n0]-YY[(i-1)]),2));
			double len2i = Math.sqrt(Math.pow((XX[(i+n0)%n0]-XX[(i+n0-2)%n0]),2) + Math.pow((YY[(i+n0)%n0]-YY[(i+n0-2)%n0]),2));
			double angle = angle(XX[ii1]-XX[ii0],YY[ii1]-YY[ii0],XX[iim1]-XX[ii0],YY[iim1]-YY[ii0]);
			double rota = 3.14159 - angle;
			if (leni>POP.max_length)
			{
				Xnew.add(XX[(i-1)]);
				Ynew.add(YY[(i-1)]);
				Xnew.add((XX[i-1] + XX[(i)%n0])/2);
				Ynew.add((YY[i-1] + YY[(i)%n0])/2);
				n1+=1;
			}
			else if ((n0>20) & (leni<POP.max_length/4. ))
			{
				n1-=1;
			}
			else
			{
				Xnew.add(XX[(i-1)]);
				Ynew.add(YY[(i-1)]);
			}
		}

		// redistribute vertices
		XX= new double[n1];						// new balloon
		YY= new double[n1];						// new balloon
		for (int i=0;i<n1;i++)
			{
			 XX[i] = (Double)(Xnew.get(i));
			 YY[i] = (Double)(Ynew.get(i));
			}
		n0 = n1 ;
		if (n0> POP.max_n0){POP.max_n0 = n0;}

		L0s = new double[n1];																	// natural length of springs (for inelastic deformation)
		VX = new double[n1];																	// array of vert X velocities
		VY = new double[n1];																	// array of vert Y velocities
		VX0 = new double[n1];																	// X velocities at time t-1
		VY0 = new double[n1];																	// Y velocities at time t-1																		//        //
		FIX = new int[n1];																				// fixed points e.g. due to contact with neighbours
		for (int i=0;i<n0;i++) /* BEGIN loop over the nodes of the structure */
		{
				// compute normal and nodal force //
				int i1 = (i+n0)%n0;
				double L2 = getSegmentLength(i1);
				L0s[i] = L2;
		}
	 }

	 /** translate current balloon to (x,y) */
	 public void translateTo (int x, int y)
		{
		 int Tx = x-x0;
		 int Ty = y-y0;
		 x0 = x;
		 y0 = y;

			for (int i=0;i<n0;i++)
			{
				XX[i] += Tx;
				YY[i] +=Ty;
			}
		}

	 /** test if a vertex of coorinate (x,y) is in contact with the given balloon  */
	 public boolean contact (double x, double y, int bx0, int by0)
		{ /* BEGIN contact */
			// draw the boundaries of the balloon with interface, then test if xy is inside this region
			Polygon cell = new Polygon();
			double[] r = new double[2];									// x and y component of the distance between center and vertex (named radius here)
			for (int i = 0;i<n0;i++)
			{
				r[0] = XX[i]-x0;										// radius x
				r[1] = YY[i]-y0;										// radius y
				r = prod(normalized(r),interface_width);
				cell.addPoint((int)(XX[i]+r[0]+0.5),(int)(YY[i] + r[1]+0.5));
			}
			boolean contain1 = cell.contains((int)(x+0.5),(int)(y+0.5));

			// use the cell without the interface and see if the point moved to the interface is inside the cell
			// use Polygon's contains method for that
			cell = new Polygon();
			for (int i = 0;i<n0;i++) { 	cell.addPoint((int)(XX[i]+0.5),(int)(YY[i]+0.5)); }
			r[0] = x-bx0;
			r[1] = y-by0;
			r = prod(normalized(r),interface_width);
			boolean contain2 = cell.contains((int)(x+r[0]+0.5),(int)(y+r[1]+0.5));

			return (contain1|contain2);
		} /* END contact */

	 /** test if a vertex of coorinate (x,y) is contained in given balloon  */
	 public boolean contain (double x, double y, int bx0, int by0)
		{ /* BEGIN contain */
			// use the cell without the interface and see if the point moved to the interface is inside the cell
			// use Polygon's contains method for that
			Polygon cell = new Polygon();
			for (int i = 0;i<n0;i++) { 	cell.addPoint((int)(XX[i]+0.5),(int)(YY[i]+0.5)); }
			boolean contain = cell.contains((int)(x+0.5),(int)(y+0.5));
			return (contain);
		} /* END contain */



	 /**
	  * expand prospectively the balloon for visualization contact
	  */
	 public int[][] Cexpand(boolean is_interface)
		{ /* BEGIN Cexpand */

			int n = n0;
			int[][] OUTPT = new int[2][n];
			double rx,ry;
			for (int i = 0;i<n;i++)
			{
				rx = XX[i]-x0;
				ry = YY[i]-y0;
				double d = Math.sqrt(rx*rx + ry*ry);
				rx=0;ry=0;
				if (is_interface){
					rx = interface_width*rx/d;
					ry = interface_width*ry/d;}
				OUTPT[0][i] = (int)(XX[i]+rx+0.5);
				OUTPT[1][i] = (int)(YY[i] + ry+0.5);
			}
			return OUTPT;
		} /* END Cexpand */

	 /**
	 * Fix the vertex i (e.g. contact with another balloon)
	 */
	 public void fix (int i)
	{ /* BEGIN fix */
		FIX[i] = 1;
	}/* END fix */

	 public void encastre (int i)
	{ /* BEGIN fix */
		FIX[i] = 10;
	}/* END fix */


	/**
	* Finding the center of mass of the balloon and quadratic moments of the balloons
	*/
	 public void mass_geometry()
	{/* BEGIN mass_geometry*/
		/* find the bounding box of the balloon */
		int x_min,x_max,y_min,y_max;  	// bounds
		int[] SXi = new int[n0]; 		// orderered list ofcoordinates
		int[] SYi = new int[n0]; 		// orderered list ofcoordinates
		int[] XXi = getXXi();			//
		int[] YYi = getYYi();			//

		for (int i=0;i<n0;i++) 			// filling
			{
			SXi[i] = (int)(XX[i]+0.5);
			SYi[i] = (int)(YY[i]+0.5);
			}
		Arrays.sort(SXi);				// sorting
		Arrays.sort(SYi);
		x_min = SXi[0];
		x_max = SXi[n0-1];
		y_min = SYi[0];
		y_max = SYi[n0-1];
		/* compute center of mass */
		Proi = new PolygonRoi(XXi,YYi,XXi.length,Roi.POLYGON);
		int nn = 0;
		x0=0; y0=0;
		for (int i=x_min;i<x_max+1;i++)
			{
			for (int j=y_min;j<y_max+1;j++)
				{
				if (Proi.contains(i,j))
					{
					x0+=i;
					y0+=j;
					nn +=1;
					}
				}
			}
		if (nn>0){
			x0 /= nn;
			y0 /= nn;}
		/* compute inertia components (quadratic moments) */
		Ixx = 0; Iyy = 0; Ixy = 0; area = 0;
		for (int i=x_min;i<x_max+1;i++)
			{
			for (int j=y_min;j<y_max+1;j++)
				{
				if (Proi.contains(i,j))
					{
					Ixx += (i-x0)*(i-x0);
					Iyy += (j-y0)*(j-y0);
					Ixy += (i-x0)*(j-y0);
					area +=1;
					}
				}
			}
		/* compute radius of the balloon */
		radius = 0;
		double minx=100000; double maxx=-1;double miny=100000; double maxy=-1;
		for (int i=0;i<n0;i++)
			{
			double dx = (XX[i]-(double)x0);
			double dy = (YY[i]-(double)y0);
			radius = Math.max(radius, Math.sqrt(dx*dx + dy*dy));
			}
		/* compute the x/y dimensions */
		lx=x_max-x_min;ly=y_max-y_min;
		lx=0;
		for (int i=x_min;i<x_max;i++)
		{
			if (Proi.contains(i,(int)(y_max/2+y_min/2+0.5)))
			{
				lx+=1;
			}
		}

	}	/* END mass_geometry*/

	 /*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//	                  OUTPUT RESULTS
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	*/



	 /** Decorates the balloon according to field and tensor values in the cell**/
	public void Fill_balloon (ImagePlus i1)
	 {	/* BEGIN Fill_balloon*/
			ImageProcessor ip = i1.getProcessor();

			double scale_volumetric = 350;//750;
			double scale_principal = 50;//450;

			/* Draw the volumetric straining */
			double si = (Math.abs(sig_vol)) + sig_vol;
			ip.setColor(new Color((int)Math.min(si*scale_volumetric,255),0,0));  //
			int[] XXi = getXXi();
			int[] YYi = getYYi();

			Proi = new PolygonRoi(XXi,YYi,XXi.length,Roi.POLYGON);
			ip.setRoi(Proi);
			ip.fill(Proi.getMask());

			/* Draw principal deformations */
			double nx, ny,mx,my;
			nx = StrainVector.get(0,0)*(StrainValues[0] + Math.abs(StrainValues[0]))/2;
			ny = StrainVector.get(1,0)*(StrainValues[0] + Math.abs(StrainValues[0]))/2;
			mx = StrainVector.get(0,1)*(StrainValues[1] + Math.abs(StrainValues[1]))/2;
			my = StrainVector.get(1,1)*(StrainValues[1] + Math.abs(StrainValues[1]))/2;
			ip.setLineWidth(4);
			ip.setColor(new Color(0,250,0));  //
			ip.drawLine(x0-(int)(nx*scale_principal+0.5),y0-(int)(ny*scale_principal+0.5),x0+(int)(nx*scale_principal+0.5),y0+(int)(ny*scale_principal+0.5));
			ip.setColor(new Color(0,0,250));  //
			ip.drawLine(x0-(int)(mx*scale_principal+0.5),y0-(int)(my*scale_principal+0.5),x0+(int)(mx*scale_principal+0.5),y0+(int)(my*scale_principal+0.5));

			/* Draw boundaries */
			ip.setColor(new Color(250,250,250));
			ip.setLineWidth(2);
			int n = XX.length;
			for (int i=0;i<n;i++) { ip.drawLine((int)(XX[i]+0.5),(int)(YY[i]+0.5),(int)(XX[(i+1)%n]+0.5),(int)(YY[(i+1)%n]+0.5)); }
		} /* END Fill_balloon*/

	/** Decorates the balloon according to field and tensor values in the cell**/
	 public void Fill_balloon (ImagePlus i1, int level)
	 { /* BEGIN Fill_balloon*/
			ImageProcessor ip = i1.getProcessor();
			int[] XXi = getXXi();
			int[] YYi = getYYi();

			ip.setColor(new Color(level,0,255-level));  //
			Proi = new PolygonRoi(XXi,YYi,XXi.length,Roi.POLYGON);
			ip.setRoi(Proi);
			ip.fill(Proi.getMask());

			/* Draw boundaries */
			ip.setColor(new Color(250,250,250));
			ip.setLineWidth(2);
			int n = XX.length;
			for (int i=0;i<n;i++) { ip.drawLine((int)(XX[i]+0.5),(int)(YY[i]+0.5),(int)(XX[(i+1)%n]+0.5),(int)(YY[(i+1)%n]+0.5));
				}
		} /* END Fill_balloon*/


	 /*
		//-------------------------------------------------------------------------------
		//-------------------------------------------------------------------------------
		//	                  USEFULL FUNCTIONS
		//-------------------------------------------------------------------------------
		//-------------------------------------------------------------------------------
		*/
	 /** Get a Point at the center of the cell*/
	 public Point getPoint()
	 {
		 Point P = new Point(x0,y0);
		 return P;
	 }

	 /** Get the length of the segment i*/
	 private double getSegmentLength(int i)
	 {
		 return Math.sqrt(Math.pow((XX[(i+1)%n0]-XX[i]),2) + Math.pow((YY[(i+1)%n0]-YY[i]),2));
	 }

	 /**set X coordinates of the balloon*/
	 public void setXX(int[] X)
	 {
		 for (int i=0;i<X.length;i++)
		 {
			 XX[i] = (double)X[i];
		 }
	 }

	 /**set X coordinates of the balloon*/
	 public void setYY(int[] Y)
	 {
		 for (int i=0;i<Y.length;i++)
		 {
			 YY[i] = (double)Y[i];
		 }
	 }

	 /**get X coordinates of the balloon as a array of integer*/
	 public int[] getXXi()
	 {
		 int[] XXi = new int[n0];
		 for (int i=0;i<XXi.length;i++)
		 {
			 XXi[i] = (int)(XX[i]+0.5);
		 }
		 return XXi;
	 }

	 /**get Y coordinates of the balloon as a array of integer*/
	 public int[] getYYi()
	 {
		 int[] YYi = new int[n0];
		 for (int i=0;i<YYi.length;i++)
		 {
			 YYi[i] = (int)(YY[i]+0.5);
		 }
		 return YYi;
	 }


	 /** Math vector operators: cross product of vector a by vector b in R3 (I made this before I start using JAMA library)  */
	 public double[] cross(double[] a,double[] b)
		{
			double[] r = new double[3];
			if (a.length == 3 & b.length == 3){
				r[0] = a[1] * b[2] - b[1] * a[2];
				r[1] = a[2] * b[0] - b[2] * a[0];
				r[2] = a[0] * b[1] - b[0] * a[1];}
			else {IJ.log("Cross product is defined for 3D arrays only"); r[0]=0;r[1]=0;r[2]=0;}
			return r;
		}

		/** Math vector operators: componentwise multiplication of vector a and vector b (I made this before I start using JAMA library)  */
		public double[] prod(double[] a,double[] b)
		{
			int n = Math.min(a.length,b.length);
			double[] r = new double[n];
			for (int i=0;i<n;i++){
				r[i] = a[i] * b[i];
			}
			if(a.length!=b.length){IJ.log("Warning: Arrays of different dimensions");}
			return r;
		}

		/** Math vector operators: multiplication of a vector a by a scala b (I made this before I start using JAMA library) */
		public double[] prod(double[] a,double b)
		{
		double[] r = new double[a.length];
		for (int i=0;i<a.length;i++){ r[i] = a[i] * b; }
		return r;
		}

		/** Math vector operators: dot product (I made this before I start using JAMA library) */
		public double dot(double[] a,double[] b)
		{
		double r = 0;
		for (int i=0;i<a.length;i++){ r += a[i]* b[i]; }
		return r;
		}


		/** Math vector operators: addition of 2 vectors a+b (I made this before I start using JAMA library)  */
		public double[] add(double[] a,double[] b)
		{
			int n = Math.min(a.length,b.length);
			double[] r = new double[n];
			for (int i=0;i<n;i++){ r[i] = a[i] + b[i]; }
			if(a.length!=b.length){IJ.log("Warning: Arrays of different dimensions");}
			return r;
		}


		/** Math vector operators: substraction a-b (I made this before I start using JAMA library) */
		public double[] sub(double[] a,double[] b)
		{
			double[] r =add(a,prod(b,-1));
			return r;
		}


		/** Math vector operators: distance between 2 points (I made this before I start using JAMA library)*/
		public double dist(double[] a, double[] b)
		{
			double l = Math.sqrt(Math.pow(a[0]-b[0],2) + Math.pow(a[1]-b[1],2) + Math.pow(a[2]-b[2],2));
			return l;
		}

		/** Math vector operators:	norm2 of vector (I made this before I start using JAMA library) */
		public double norm(double x, double y)
		{
			double l = Math.sqrt(x*x + y*y);
			return l;
		}

		/** Math vector operators:	norm2 of vector a (I made this before I start using JAMA library)  */
		public double norm(double[] a)
		{
			double l = 0;
			for(int i=0;i<a.length;i++){l+=a[i]*a[i];}
			return Math.sqrt(l);
		}

		/** Math vector operators:	return the normalized vector (I made this before I start using JAMA library) */
		public double[] normalized(double[] a)
		{
			double[] r;
			r = new double[a.length];
			double l = norm(a);
			if (l>0){r = prod(a,1/l);}
			else{r=a;}//IJ.log("Normalization of nul vector");
			return r;
		}

		/**  Math vector operators:	determine angle between v2 (x2, y2) and v1 (x1,y1) (I made this before I start using JAMA library)  */
		public double angle(double x1, double y1, double x2, double y2)
			{
			double c = (x1*x2 + y1*y2)/(Math.sqrt(x1*x1+y1*y1)*Math.sqrt(x2*x2+y2*y2));  // cosine = a.b/(|a||b|)
			double s = (x1*y2 - y1*x2)/(Math.sqrt(x1*x1+y1*y1)*Math.sqrt(x2*x2+y2*y2));  // sine = a^b/(|a||b|)
			if (c<-0.98){return -3.14159;}
			else if (s>=0){return Math.acos(c);}
			else {return 3.14159*2 - Math.acos(c);}
			}
}	/* END Balloon class definition */
