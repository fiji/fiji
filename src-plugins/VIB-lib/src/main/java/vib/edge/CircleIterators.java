package vib.edge;

import java.util.*;

public class CircleIterators {
	public static int[][] FullCircle(double radius) {
		return FullCircle(radius,true);
	}

	public static int[][] FullCircle(double radius,boolean withOrigin) {
		List list = new ArrayList();
		for(int i=(int)radius;i>=0;i--) {
			int quarter=(int)Math.sqrt(radius*radius-i*i);
			for(int j=-quarter;j<=quarter;j++) {
				if(!withOrigin && i==0 && j==0)
					continue;
				int[] x=new int[2];
				x[0]=j;
				x[1]=i;
				list.add(x);
			}
		}
		for(int i=1;i<=(int)radius;i++) {
			int quarter=(int)Math.sqrt(radius*radius-i*i);
			for(int j=-quarter;j<=quarter;j++) {
				int[] x=new int[2];
				x[0]=j;
				x[1]=-i;
				list.add(x);
			}
		}
		int count = list.size();
		int[][] result = new int[count][2];
		for(int i=0;i<count;i++)
			result[i]=(int[])list.get(count-1-i);
		return result;
	}

	/* This iterator sorts the coordinates by distance to the center */
	public static int[][] FullCircleSortedByDistance(double radius,boolean ascending) {
		int[][] result=FullCircle(radius,true);
		DistanceComparator compare=new DistanceComparator(ascending);
		Arrays.sort(result,compare);
		return result;
	}

	
	/* this returns a list of coordinates in the upper half circle,
	   sorted counter clockwise by angle, starting with the
	   right most coordinate */
	public static int[][] SortedHalfCircle(double radius) {
		List list = new ArrayList();
		for(int i=(int)radius;i>=0;i--) {
			int quarter=(int)Math.sqrt(radius*radius-i*i);
			for(int j=(i>0?-quarter:1);j<=quarter;j++) {
				int[] x=new int[2];
				x[0]=j;
				x[1]=i;
				list.add(x);
			}
		}
		Collections.sort(list,new PointComparator());
		int count = list.size();
		int[][] result = new int[count][2];
		for(int i=0;i<count;i++)
			result[i]=(int[])list.get(count-1-i);
		return result;
	}

	/* this returns a list of lines through a circle with a given radius.
	input shc: a circle
	Edgelets[k] will be a line intersecting the circle through its center 
	at the same angle as the line between the center and shc[k]*/
	// TODO: maybe there's a more efficient way to do it?
	public static int[][][] Edgelets(int [][] shc,double radius) {
		int[][][] result=new int[shc.length][][];
		List plist = new ArrayList();
		for(int k=0;k<shc.length;k++) {
			double dist=Math.sqrt(shc[k][0]*shc[k][0]+shc[k][1]*shc[k][1]);
			double dx=shc[k][0]/dist;
			double dy=shc[k][1]/dist;
			int oldx=-2*(int)radius;
			int oldy=oldx;
			int npoints=0;
			int[] x=new int[2];
			// count different points
			for(int i=-(int)radius;i<=(int)radius;i++) {
				x[0]=(int)(dx*i);
				x[1]=(int)(dy*i);
				if(x[0]!=oldx || x[1]!=oldy) {
					oldx=x[0];
					oldy=x[1];
					plist.add(x);
					x=new int[2];
				}
			}
			result[k]=new int[(int)(plist.size())][2];
			
			for(int i=0;i<(int)(plist.size());i++) {
				result[k][i]=(int[])plist.get(i);
			}
			plist.clear();
		}
		return result;
	}

	/* This procedure returns a list of the right most coordinates
	   of the circle.
	*/
	public static int[][] RightSickle(double radius) {
		int halfCount=(int)radius;
		int[][] result =new int[2*halfCount+1][2];
		for(int i=halfCount;i>=-halfCount;i--) {
			result[i+halfCount][0]=(int)Math.sqrt(radius*radius-i*i);
			result[i+halfCount][1]=i;
		}
		return result;
	}

	public static void main(String[] args) {
		int[][] list = SortedHalfCircle(5);  //RightSickle(5); //FullCircle(5); //FullCircleSortedByDistance(5,true);
		int[][] pixels = new int[11][11];
		for(int i=0;i<list.length;i++) {
			System.out.print("("+list[i][0]+","+list[i][1]+") ");
			pixels[5+list[i][0]][5+list[i][1]]=i+1;
		}
		System.out.println("");
		for(int i=-5;i<=5;i++) {
			for(int j=-5;j<=5;j++)
				System.out.print((char)(64+pixels[5+j][5+i]));
			System.out.println("");
		}

		int[][] slist=SortedHalfCircle(5);
		int[][][] slines=Edgelets(slist,5);
		for(int i=0;i<slist.length;i++) {
			System.out.println("Point ("+slist[i][0]+","+slist[i][1]+")");
			for(int j=0;j<slines[i].length;j++)
				System.out.print("("+slines[i][j][0]+","+slines[i][j][1]+")");
			System.out.println();
		}
	}
}

/* According to this comparator, p1<p2 iff the angle of p1 is smaller than
   the angle of p2, or in the case the angles are equal, the length of p1
   is smaller than that of p2. */
class PointComparator implements Comparator {
	public int compare(Object a,Object b) {
		int[] c=(int[])a;
		int[] d=(int[])b;
		int result = c[0]*d[1]-c[1]*d[0];
		if(result==0)
			return Math.abs(c[0])+Math.abs(c[1])-Math.abs(d[0])-Math.abs(d[1]);
		else
			return result;
	}
}

/* This compares points by their distance to 0. */
class DistanceComparator implements Comparator {
	boolean ascending;

	public DistanceComparator(boolean a) {
		ascending=a;
	}

	public int compare(Object a,Object b) {
		int[] c=(int[])a;
		int[] d=(int[])b;
		return (d[0]*d[0]+d[1]*d[1]-c[0]*c[0]-c[1]*c[1])
			*(ascending?-1:1);
	}
}

