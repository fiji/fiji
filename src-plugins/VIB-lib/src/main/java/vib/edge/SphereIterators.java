package vib.edge;

import java.util.Vector;

public class SphereIterators {

	final static double EPSILON=1e-15;

	/*
	 * This method returns a sampling of a sphere along a spiral.
	 * The spiral is defined by
	 *
	 *          / sin(t*PI/N)cos(2*PI*t) \
	 *  f(t) = (  sin(t*PI/N)sin(2*PI*t)  ), 0 <= t <= N
	 *          \      cos(t*PI/N)       /
	 *
	 * where N tells how many times the spiral turns around the z axis.
	 * The idea is that the spherical distance between adjacent turns is
	 * constant: spacing = PI/N.
	 *
	 * Since the curve approximates at each z=cos(t*PI/N) the circle with
	 * radius r=sin(t*PI/N), the approximate derivative of the length
	 *
	 *  l(t) = \int_0^t |f'(u)| du
	 *
	 * is given by
	 *
	 *  l'(t) \approx 2*PI*sin(t*PI/N).
	 *
	 * Therefore,
	 *
	 *  l(t) \approx 2*N*(1-cos(t*PI/N))
	 *
	 * and thus
	 *
	 *  t(l) \approx N/PI arccos(1-l/2/N)
	 *
	 * If not a sphere is to be sampled, but an ellipsoid whose z extent
	 * differs from the x/y extent, the argument to the sin in the x and
	 * y component of the spiral becomes
	 *
	 *   t/zfactor*PI/N
	 *
	 * instead of
	 *
	 *   t*PI/N,
	 *
	 * and therefore approximation of the curve length's derivative
	 *
	 *   l'(t) \approx 2*PI*sin(t/zfactor*PI/N).
	 *
	 * And so,
	 *
	 *   l(t) \approx 2*N*zfactor*(1-cos(t/zfactor*PI/N))
	 *
	 * which means
	 *
	 *   t(l) \approx zfactor*N/PI*arccos(1-l/2/N/zfactor)
	 *
	 */
	public static double[][] SampleSphereSurface(double radius, double spacing) {
		return SampleSphereSurface(radius,spacing,1.0);
	}

	public static double[][] SampleSphereSurface(double radius, double spacing,double zfactor) {
		spacing/=radius;
		double N=Math.PI/spacing;
		double totalLength=(int)4*N;
		int count=(int)(totalLength*1.0/spacing);
		double[][] result = new double[count][3];

		for(int i=0;i<count;i++) {
			double l=spacing*i;
			double t=zfactor*N/Math.PI*Math.acos(1-l/2.0/N/zfactor)+EPSILON;

			result[i][0]=radius*Math.sin(t/zfactor*Math.PI/N)*Math.cos(2*Math.PI*t);
			result[i][1]=radius*Math.sin(t/zfactor*Math.PI/N)*Math.sin(2*Math.PI*t);
			result[i][2]=zfactor*radius*Math.cos(t/zfactor*Math.PI/N);

		}

		return result;
	}

	public static void printLandmarkSet(int[][] points) {
		double[][] dpoints=new double[points.length][3];
		for(int i=0;i<points.length;i++)
			for(int j=0;j<3;j++)
				dpoints[i][j]=points[i][j];
		printLandmarkSet(dpoints);
	}

	public static void printLandmarkSet(double[][] points) {
		System.out.println("# HyperMesh 3D ASCII 1.0\n"
			+"\n"
			+"define Markers "+points.length+"\n"
			+"\n"
			+"Parameters {\n"
			+"	ContentType \"LandmarkSet\",\n"
			+"	NumSets 1\n"
			+"}\n"
			+"\n"
			+"Markers { float[3] Coordinates }  @1\n"
			+"\n"
			+"# Data section follows^L\n"
			+"@1\n");

		for(int i=0;i<points.length;i++)
			System.out.println(points[i][0]+" "+points[i][1]+" "+points[i][2]);

		System.out.println();

	}

	public static boolean isUpperHalf(int[] v,double[] normal) throws Exception {
		return isUpperHalf(v[0],v[1],v[2],normal);
	}

	public static boolean isUpperHalf(int x,int y,int z,double[] normal) throws Exception {
		double result=x*normal[0]+y*normal[1]+z*normal[2];
		if(result==0) {
			throw new Exception("Neither upper nor lower half!");
			/*
			if(x==0 && y==0 && z==0)
				return true;
			System.err.println("Warning: had to adjust normal");
			// wiggle a bit farther from the z-axis
			if(normal[0]==0 && normal[1]==0)
				normal[0]=EPSILON;
			else {
				normal[0]*=1+EPSILON;
				normal[1]*=1+EPSILON;
			}
			return isUpperHalf(x,y,z,normal);
			*/
		}
		return (result>0);
	}

	public static int[][] SphereIterator(double radius) {
		return SphereIterator(radius,1.0);
	}

	public static int[][] SphereIterator(double radius,double zFactor) {
		return SphereIterator(radius, 1, 1, zFactor);
	}

	public static int[][] SphereIterator(double radius,
			double xFactor, double yFactor, double zFactor) {
		Vector result = new Vector();
		double r2 = radius * radius;
		int zDiff = (int)Math.floor(radius / zFactor);
		for(int z = -zDiff; z <= zDiff; z++) {
			double z1 = z * zFactor;
			int yDiff = (int)(Math.sqrt(r2 - z1 * z1) / yFactor);
			for(int y = -yDiff; y <= yDiff; y++) {
				double y1 = y * yFactor;
				int xDiff = (int)(Math.sqrt(r2 - z1 * z1
							- y1 * y1)
						/ xFactor);
				for(int x = -xDiff; x <= xDiff; x++) {
					int[] p=new int[3];
					p[0]=x; p[1]=y; p[2]=z;
					result.add(p);
				}
			}
		}
		int[][] ret=new int[result.size()][3];
		for(int i=0;i<ret.length;i++)
			ret[i]=(int[])result.get(i);
		return ret;
	}

	static boolean isNull(int[] v) {
		return v[0]==0 && v[1]==0 && v[2]==0;
	}

	/* This method returns an integer sampling of a sphere, where the
	 * scalar product with the normal is positive.
	 */
	public static int[][] HalfSphereIterator(double radius,double[] normal) {
		return HalfSphereIterator(radius,normal,1.0);
	}

	public static int[][] HalfSphereIterator(double radius,double[] normal,double zfactor) {
		Vector result=new Vector();
		int[][] sphereIterator=SphereIterator(radius,zfactor);
		for(int i=0;i<sphereIterator.length;i++)
			try {
				if(!isNull(sphereIterator[i]) &&
				  isUpperHalf(sphereIterator[i],normal))
					result.add(sphereIterator[i]);
			} catch(Exception e) { /* do not count */ }
		int[][] ret=new int[result.size()][3];
		for(int i=0;i<ret.length;i++)
			ret[i]=(int[])result.get(i);
		return ret;
	}

	/* This method returns an iterator to iterators of coordinates.
	 * Each of these iterators points to the coordinates which are in
	 * the upper sphere corresponding to its normal, but not its successor.
	 */
	public static int[][][] HalfSphereIteratorsIterator(double radius, double[][] normals) {
		return HalfSphereIteratorsIterator(radius,normals,1.0);
	}

	public static int[][][] HalfSphereIteratorsIterator(double radius, double[][] normals,double zfactor) {
		int length=normals.length-1;
		if(length<0) {
			return new int[0][][];
		}
		int[][][] result=new int[length][][];
		int[][] sphereIterator=SphereIterator(radius,zfactor);
		for(int i=0;i<normals.length-1;i++) {
			Vector v=new Vector();
			for(int j=0;j<sphereIterator.length;j++)
				try {
					if(isUpperHalf(sphereIterator[j],normals[i])!=isUpperHalf(sphereIterator[j],normals[i+1]))
						v.add(sphereIterator[j]);
				} catch(Exception e) { /* do not count */ }
			result[i]=new int[v.size()][3];
			for(int j=0;j<result[i].length;j++)
				result[i][j]=(int[])v.get(j);
		}
		return result;
	}

	static void put(boolean[][][] sphere,int r,int[] c,boolean v) {
		sphere[r+c[0]][r+c[1]][r+c[2]]=v;
	}

	static boolean get(boolean[][][] sphere,int r,int[] c) {
		return sphere[r+c[0]][r+c[1]][r+c[2]];
	}

	public static boolean testHalfSphereIteratorsIterator(double radius, double[][] normals) {
		int r=(int)radius;
		boolean[][][] sphere=new boolean[2*r+1][2*r+1][2*r+1];
System.err.println("Building HalfSphereIterator");
		int[][] halfSphereIterator=HalfSphereIterator(radius,normals[0]);
System.err.println("... returned "+halfSphereIterator.length+" coordinates");
System.err.println("Building Sphere");
		for(int i=0;i<halfSphereIterator.length;i++) {
			put(sphere,r,halfSphereIterator[i],true);
			sphere[r-halfSphereIterator[i][0]][r-halfSphereIterator[i][1]][r-halfSphereIterator[i][2]]=false;
		}
System.err.println("Checking Sphere");
		for(int i=0;i<halfSphereIterator.length;i++) {
			if(get(sphere,r,halfSphereIterator[i])!=true) {
				System.err.println("Detected error in HalfSphereIterator");
				return false;
			}
		}
System.err.println("Building HalfSphereIteratorsIterator");
		int[][][] iterator2=HalfSphereIteratorsIterator(radius,normals);
		if(iterator2.length+1!=normals.length) {
			System.err.println("Wrong length of HalfSphereIteratorsIterator");
			return false;
		}
System.err.println("... returned "+iterator2.length+" iterators");
System.err.println("Checking HalfSphereIteratorsIterator");
		for(int i=0;i<iterator2.length;i++) {
System.err.println("Applying "+i+" ("+iterator2[i].length+" coordinates)");
			// apply change from iterator2
			for(int j=0;j<iterator2[i].length;j++)
				put(sphere,r,iterator2[i][j],!get(sphere,r,iterator2[i][j]));
			// check
System.err.println("Building HalfSphereIterator");
			halfSphereIterator=HalfSphereIterator(radius,normals[i+1]);
System.err.println("Checking with HalfSphereIterator");
			for(int j=0;j<halfSphereIterator.length;j++)
				if(!get(sphere,r,halfSphereIterator[j])) {
					System.err.println("HalfSphereIteratorsIterator failed at "+i+", "+j);
					return false;
				}
		}
		return true;
	}


	public static void main(String[] args) {
		if(args.length>0 && args[0].equals("-landmarks")) {
			double[][] sphere=SampleSphereSurface(100,30);
			printLandmarkSet(sphere);
			return;
		}
		double radius=10;
		double[][] normals=SampleSphereSurface(radius,5);
		testHalfSphereIteratorsIterator(radius,normals);
	}
}

