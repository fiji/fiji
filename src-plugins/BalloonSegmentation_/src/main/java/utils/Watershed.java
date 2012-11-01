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

import ij.*;
import ij.process.*;
import java.util.*;
import Jama.*;
import ij.plugin.filter.*;

public class Watershed
{
	int IM[][];											// the image personal format (DEPRECATRED)
	ArrayList VERTEX = new ArrayList();						// cell vertices
	ArrayList CELL = new ArrayList();							// Cell definition (1 cell = list of vertices)
	ArrayList active_growth2 = new ArrayList();		//xxx	// active pixel for checking neighbors for merging
	double[] CENTx;										// lsit of Cell center x position
	double[] CENTy;										// lsit of Cell center y position
	Matrix[] NEGB = new Matrix[8];						// neighbor of a pixel
	int b_level;										// buffer for level of boundary seeds

	public int sx;												// size of the image
	public int sy;
	int sz;
	double Max_field;									// maximum luminance value
	int n_basin;										// number of basins
	int n_vertex;										// number of vertex in VERTEX
	double min_distance_between_vertices;				// minimal distance between vertices accepted
	int n_border_seeds;									// number of border seeds for border edge detection
	double A;											// number of pixels occupied by the cells
	double time;										// estimation of time needed for the operation
	int IM2[][];										// matrix of basin index
	public int IMB[][][];										// segmented image
	ImageProcessor image;


//	__________________________________________________________________________
//
//
//								Constructor
//
// ___________________________________________________________________________



public Watershed() 	{
	Max_field=255;
	n_vertex=0;
	}

public Watershed(ImageProcessor imp)    //   int n, int m,
{
	int n = imp.getWidth();
	int m = imp.getHeight();



	Max_field=255;
	n_vertex=0;
	sx = n;
	sy = m;
	A = n*m;
	b_level = 0;

	IM2 = new int[sx+1][sy+1];
	IMB = new int[sx+1][sy+1][3];

	//ImageProcessor image = imp
	for (int i=0;i<sx;i++) 	{
		for (int j=0;j<sy;j++) 		{
			IM2[i][j] = Math.min(imp.getPixel(i,j) , 254);
			IMB[i][j][0] = Math.min(imp.getPixel(i,j),254);

		}
	}
	append_i2(0,0,0);							  // seeds for boundaries

}


//__________________________________________________________________________
//
//
//							IMMERSION ALGORITHM
//
// ___________________________________________________________________________



/** Immerse the image untill maximum pixel intensity */
public void Flow_bound(int inc)
{
	// selective basin growth
	Flow_opt(b_level, b_level + inc);
	b_level += inc;
}



/** Immerse the image untill maximum pixel intensity */
public void Flow(int init)
{
	// initiation of active_growth
	for (int i=1; i<n_basin+1;i++)
	{
			double x = CENTx[i-1];
			double y = CENTy[i-1];

			append_i2(x,y,i);
	}

	Flow_opt(init,255);
}



/** Immerse basins between _begin and _end until the stop_level */
private void Flow_opt(int init, int stop_level)
{
	/**
	//  [init] and [stop_level] are value of pixel intensity at which the algorithme starts and ends
	//  [_begin] / [_end] deprecated
	*/
	int inc = 0;
    int res = 0;
	int res_loc = 0;
	int max_iter = 0;
	for (int i = (int)init; i < stop_level; i++ )		//Max_field
		{
		IJ.showStatus("Watershed Finding bounds: " + (int)((double)(i)/(double)(stop_level)*100) +"%");
		double x = 0;
		ArrayList Vtmp = (ArrayList)(active_growth2.get(i));
		int act_s = Vtmp.size();
		while (act_s>0)// & (inc < 500))    			// loop for considering also plateau
			{
			update_Wactive2(i);
			act_s = ((ArrayList)active_growth2.get(i)).size();
			max_iter +=1;
			}
		}
	IJ.showStatus("");
}



/** UPDATE THE GROWING REGION IN THE IMAGE (2nd generation algo) */
private int update_Wactive2(int level)
{
	ArrayList lst = new ArrayList();
	lst = (ArrayList)(((ArrayList)(active_growth2.get(level))).clone());
	Matrix x;
	int stop = 1;
	int im,jm,basin;
	int n = lst.size();

	((ArrayList)(active_growth2.get(level))).clear();


	// - get the basin to the flow level: level
	int index = 0;
	for (int k=0;k<n;k++) {
		x = (Matrix)(lst.get(k));
        im = (int)(x.get(0,0));
        jm = (int)(x.get(0,1));
		//if (im == 495)
		basin = (int)(x.get(0,2));

		// change pixel value when watershed level reach its altitude
		IM2[im][jm] = (basin+1)*1000;
		IMB[im][jm][0] = 255;
		neighbour( im, jm );

		//  add verts from the neighbors to active_growth region
        for (int j=0; j<8; j++)
			{
			Matrix negbi = NEGB[j];
            int i0 = (int)negbi.get(0,1);
            int j0 = (int)negbi.get(0,2);

			if (IMB[i0][j0][2] <499 & IM2[i0][j0]<499)
				{
				double[][] addedPoint = {{i0,j0,basin}};
				((ArrayList)(active_growth2.get( Math.max((int)level,Math.min((int)negbi.get(0,0),255)) ))).add(new Matrix(addedPoint));

				if (Math.max((int)level,Math.min((int)negbi.get(0,0),255))==0)
					{
					index+=1;
					}

				if (IM2[i0][j0] < 256){
					A -= 1;
					IMB[i0][j0][2] = 500;
					}
				}
			}
	}
	return stop;
}



//	__________________________________________________________________________
//
//
//							USEFUL FUNCTIONS
//
// ___________________________________________________________________________



/** IMPORT CEL CENTERS FROM PYTHON */
public void GetCenter(double[] u, double[] v)
{
	CENTx = u;
	CENTy = v;
	n_basin = v.length;
}


/** INITIATION OF THE GROWING REGIONS (2nd generation algorithm) */
//public ArrayList<Balloon> BallList = new ArrayList<Balloon>()
private void append_i2(double x, double y, int k)
{
	for (int i=0;i<256;i++)
	{
		ArrayList row = new ArrayList();
		active_growth2.add(row);
	}
	ArrayList r= new ArrayList();
	r.add(new Integer(-1));
	CELL.add(r);

	Matrix vec;
	ArrayList row = new ArrayList();
	if (k==0)										// outside the plant: several seeds points are inserted for the SAME BASIN at boundaries
	{
    int n_bseeds1 = 4;
    int n_edg = n_bseeds1/4 + 1;
		for (int i=0;i<n_edg;i++)
		{
			int level;
			double[][] addedPoint1 = {{i*sx/n_edg+2,4,0}};
			level = IM2[i*sx/n_edg+2][4];
			Matrix MAT = new Matrix(addedPoint1);
			((ArrayList)active_growth2.get(level)).add(MAT);

			double[][] addedPoint2 = {{4,i*sy/n_edg + 2,0}};
			level = IM2[4][i*sy/n_edg + 2];
			((ArrayList)active_growth2.get(level)).add(new Matrix(addedPoint2));

			double[][] addedPoint3 = {{i*sx/n_edg + 2,sy - 4,0}};
			level = IM2[i*sx/n_edg + 2][sy - 4];
			((ArrayList)active_growth2.get(level)).add(new Matrix(addedPoint3));


			double[][] addedPoint4 = {{sx - 4,i*sy/n_edg + 2,0}};
			level = IM2[sx - 4][i*sy/n_edg + 2];
			((ArrayList)active_growth2.get(level)).add(new Matrix(addedPoint4));


			IMB[i*sx/n_edg+2][4][2] = 500;
			IMB[4][i*sy/n_edg + 2][2] = 500;
			IMB[i*sx/n_edg + 2][sy - 4][2] = 500;
			IMB[sx - 4][i*sy/n_edg + 2][2] = 500;

		}
	}
	else
		{
			int level = Math.min(IM2[(int)x][(int)y],255);
			double[][] addedPoint = {{x,y,k}};
			((ArrayList)active_growth2.get(level)).add(new Matrix(addedPoint));
			IMB[(int)x][(int)y][2] = 500;
		}

}


/** ADD A NEW VERTEX TO THE DATA STRUCTURE (CELL and VERTEX)*/
private void append_v(int x, int y, int[] LL)
{
	ArrayList r = new ArrayList();;										// to append CELL
	ArrayList row = new ArrayList();;										// to append to VERTEX
	row.add(new Integer(x));
	row.add( new Integer(y));
	int Lsize = LL.length;

	for (int i=0;i<Lsize;i++)
	{
		row.add(new Integer(LL[i]));
	}
	VERTEX.add(row);
	n_vertex +=1;

	// Add the new vertex to the cells sharing this node
	for (int i=0;i<Lsize;i++)
	{
		if ( ( (Integer)((ArrayList)(CELL.get((int)(LL[i]/1000)-1))).get(0) ).intValue() < 0   ) {
		r.add(new Integer(n_vertex-1));
		}
		else
		{
		r = (ArrayList)CELL.get((int)(LL[i]/1000)-1);
		r.add(new Integer(n_vertex-1));
		}
		CELL.set((int)(LL[i]/1000)-1,r);   /** should be set(...)*/
		r.clear();
	}
}




/** FIND NEIGHBOURS OF A PIXEL */
private void neighbour(int i, int j)
{
//
//    7   6   5
//    8  i,j  4
//    1   2   3
//

    if ((i>1) & (j>1) & (i<sx-1) & (j<sy-1))
	{
		double[][] addedPoint1 = {{IM2[i-1][j-1],i-1,j-1}};
        NEGB[0] = new Matrix(addedPoint1);

		double[][] addedPoint2 = {{IM2[i][j-1],i,j-1}};
        NEGB[1] = new Matrix(addedPoint2);

		double[][] addedPoint3 = {{IM2[i+1][j-1],i+1,j-1}};
        NEGB[2] = new Matrix(addedPoint3);

		double[][] addedPoint4 = {{IM2[i+1][j],i+1,j}};
        NEGB[3] = new Matrix(addedPoint4);

		double[][] addedPoint5 = {{IM2[i+1][j+1],i+1,j+1}};
        NEGB[4] = new Matrix(addedPoint5);

		double[][] addedPoint6 = {{IM2[i][j+1],i,j+1}};
        NEGB[5] = new Matrix(addedPoint6);

		double[][] addedPoint7 = {{IM2[i-1][j+1],i-1,j+1}};
        NEGB[6] = new Matrix(addedPoint7);

		double[][] addedPoint8 = {{IM2[i-1][j],i-1,j}};
        NEGB[7] = new Matrix(addedPoint8);

	}
    else
	{
		double[][] addedPoint1 = {{263,i-1,j-1}};
		NEGB[0] = new Matrix(addedPoint1);

		double[][] addedPoint2 = {{263,i,j-1}};
		NEGB[1] = new Matrix(addedPoint2);

		double[][] addedPoint3 = {{263,i+1,j-1}};
		NEGB[2] = new Matrix(addedPoint3);

		double[][] addedPoint4 = {{263,i+1,j}};
		NEGB[3] = new Matrix(addedPoint4);

		double[][] addedPoint5 = {{263,i+1,j+1}};
		NEGB[4] = new Matrix(addedPoint5);

		double[][] addedPoint6 = {{263,i,j+1}};
        NEGB[5] = new Matrix(addedPoint6);

		double[][] addedPoint7 = {{263,i-1,j+1}};
        NEGB[6] = new Matrix(addedPoint7);

		double[][] addedPoint8 = {{263,i-1,j}};
        NEGB[7] = new Matrix(addedPoint8);
	}

}


/** SORT THE NEIGHBOURS OF A PIXEL ACCORDING TO THEIR LUMINANCE VALUE */
private int[] _sort(int ii, int jj)
{
	ArrayList row = new ArrayList();
	int LI = IM2[ii][jj];

	if (LI>Max_field)
		row.add(new Integer(LI));

	for (int i=0;i<8;i++)
	{
		Matrix x = NEGB[i];
		if (x.get(0,0)>Max_field+20)
		{
			int n = row.size();
			int add = 1;
			for (int j=0;j<n;j++)
			{
				if ( (int)(x.get(0,0)) == ((Integer)row.get(j)).intValue())
				{
					add = 0;
					break;
				}
			}
			if (add ==1)
				row.add(new Integer((int)x.get(0,0)));
		}
	}
	//sort(row.begin(),row.end());
	int Rrow[]= new int[row.size()];
	for (int i=0;i<row.size();i++)
	{
		Rrow[i] = ((Integer)(row.get(i))).intValue();
	}
	Arrays.sort(Rrow);
	return Rrow;
}

}
