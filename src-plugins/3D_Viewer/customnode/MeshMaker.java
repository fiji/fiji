/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/**
 * Example plugin on how to add spheres and tubes to the 3D Viewer.
 * Albert Cardona 2008-12-09
 * Released under the General Public License, latest version.
 */

package customnode;

import ij.IJ;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import ij3d.Pipe;

public class MeshMaker {

	public static void main(String[] args) {
		new ij.ImageJ();
		IJ.runPlugIn("ij3d.Mesh_Maker", "");
	}

	static public List<Point3f> createSphere(final double x, final double y, final double z, final double r) {
		return createSphere(x, y, z, r, 12, 12);
	}

	static public List<Point3f> createSphere(final double x, final double y, final double z,
			                final double r, final int meridians, final int parallels) {
		final double[][][] globe = generateGlobe(meridians, parallels);
		// Scale by radius 'r', and translate to x,y,z
		for (int j=0; j<globe.length; j++) {
			for (int k=0; k<globe[0].length; k++) {
				globe[j][k][0] = globe[j][k][0] * r + x;
				globe[j][k][1] = globe[j][k][1] * r + y;
				globe[j][k][2] = globe[j][k][2] * r + z;
			}
		}
		// create triangular faces and add them to the list
		final ArrayList<Point3f> list = new ArrayList<Point3f>();
		for (int j=0; j<globe.length-1; j++) { // the parallels
			for (int k=0; k<globe[0].length -1; k++) { // meridian points
				if(j != globe.length-2) {
					// half quadrant (a triangle)
					list.add(new Point3f((float)globe[j+1][k+1][0], (float)globe[j+1][k+1][1], (float)globe[j+1][k+1][2]));
					list.add(new Point3f((float)globe[j][k][0], (float)globe[j][k][1], (float)globe[j][k][2]));
					list.add(new Point3f((float)globe[j+1][k][0], (float)globe[j+1][k][1], (float)globe[j+1][k][2]));
				}
				if(j != 0) {
					// the other half quadrant
					list.add(new Point3f((float)globe[j][k][0], (float)globe[j][k][1], (float)globe[j][k][2]));
					list.add(new Point3f((float)globe[j+1][k+1][0], (float)globe[j+1][k+1][1], (float)globe[j+1][k+1][2]));
					list.add(new Point3f((float)globe[j][k+1][0], (float)globe[j][k+1][1], (float)globe[j][k+1][2]));
				}
			}
		}
		return list;
	}

	static public List<Point3f> createQuadSphere(final double x, final double y, final double z,
			                final double r, final int meridians, final int parallels) {
		final double[][][] globe = generateGlobe(meridians, parallels);
		// Scale by radius 'r', and translate to x,y,z
		for (int j=0; j<globe.length; j++) {
			for (int k=0; k<globe[0].length; k++) {
				globe[j][k][0] = globe[j][k][0] * r + x;
				globe[j][k][1] = globe[j][k][1] * r + y;
				globe[j][k][2] = globe[j][k][2] * r + z;
			}
		}
		// create triangular faces and add them to the list
		final ArrayList<Point3f> list = new ArrayList<Point3f>();
		for (int j=0; j<globe.length-1; j++) { // the parallels
			for (int k=0; k<globe[0].length -1; k++) { // meridian points
				list.add(new Point3f((float)globe[j]  [k][0], (float)globe[j]  [k][1], (float)globe[j]  [k][2]));
				list.add(new Point3f((float)globe[j+1][k][0], (float)globe[j+1][k][1], (float)globe[j+1][k][2]));
				list.add(new Point3f((float)globe[j+1][k+1][0], (float)globe[j+1][k+1][1], (float)globe[j+1][k+1][2]));
				list.add(new Point3f((float)globe[j]  [k+1][0], (float)globe[j]  [k+1][1], (float)globe[j]  [k+1][2]));
			}
		}
		return list;
	}

	/** Generate a globe of radius 1.0 that can be used for any Ball. First dimension is Z, then comes a double array x,y. Minimal accepted meridians and parallels is 3.*/
	static public double[][][] generateGlobe(int meridians, int parallels) {
		if (meridians < 3) meridians = 3;
		if (parallels < 3) parallels = 3;
		/* to do: 2 loops:
		-first loop makes horizontal circle using meridian points.
		-second loop scales it appropriately and makes parallels.
		Both loops are common for all balls and so should be done just once.
		Then this globe can be properly translocated and resized for each ball.
		*/
		// a circle of radius 1
		double angle_increase = 2*Math.PI / meridians;
		double temp_angle = 0;
		final double[][] xy_points = new double[meridians+1][2];    //plus 1 to repeat last point
		xy_points[0][0] = 1;     // first point
		xy_points[0][1] = 0;
		for (int m=1; m<meridians; m++) {
			temp_angle = angle_increase*m;
			xy_points[m][0] = Math.cos(temp_angle);
			xy_points[m][1] = Math.sin(temp_angle);
		}
		xy_points[xy_points.length-1][0] = 1; // last point
		xy_points[xy_points.length-1][1] = 0;

		// Build parallels from circle
		angle_increase = Math.PI / parallels;   // = 180 / parallels in radians
		final double[][][] xyz = new double[parallels+1][xy_points.length][3];
		for (int p=1; p<xyz.length-1; p++) {
			double radius = Math.sin(angle_increase*p);
			double Z = Math.cos(angle_increase*p);
			for (int mm=0; mm<xyz[0].length-1; mm++) {
				//scaling circle to appropriate radius, and positioning the Z
				xyz[p][mm][0] = xy_points[mm][0] * radius;
				xyz[p][mm][1] = xy_points[mm][1] * radius;
				xyz[p][mm][2] = Z;
			}
			xyz[p][xyz[0].length-1][0] = xyz[p][0][0];  //last one equals first one
			xyz[p][xyz[0].length-1][1] = xyz[p][0][1];
			xyz[p][xyz[0].length-1][2] = xyz[p][0][2];
		}

		// south and north poles
		for (int ns=0; ns<xyz[0].length; ns++) {
			xyz[0][ns][0] = 0;	//south pole
			xyz[0][ns][1] = 0;
			xyz[0][ns][2] = 1;
			xyz[xyz.length-1][ns][0] = 0;    //north pole
			xyz[xyz.length-1][ns][1] = 0;
			xyz[xyz.length-1][ns][2] = -1;
		}

		return xyz;
	}

	static public List<Point3f> createTube(final double[] x, final double[] y, final double[] z,
			              final double[] r, final int parallels, final boolean do_resample) {
		return Pipe.generateTriangles(Pipe.makeTube(x, y, z, r, 1, parallels, do_resample, null, null, null), 1, null, null);
	}

	static public List<Point3f> createDisc(double x, double y, double z,
				      double nx, double ny, double nz,
				      double radius,
				      int edgePoints ) {
		double ax, ay, az;

		if( Math.abs(nx) >= Math.abs(ny) ) {
			double scale = 1 / Math.sqrt( nx*nx + nz*nz  );
			ax = -nz * scale;
			ay = 0;
			az = nx * scale;
		} else {
			double scale = 1 / Math.sqrt( ny*ny + nz*nz  );
			ax = 0;
			ay = nz * scale;
			az = - ny * scale;
		}

		/* Now to find the other vector in that plane, do the
		 * cross product of (ax,ay,az) with (nx,ny,nz) */

		double bx = (ay * nz - az * ny);
		double by = (az * nx - ax * nz);
		double bz = (ax * ny - ay * nx);
		double bScale = 1 / Math.sqrt( bx*bx + by*by + bz*bz );
		bx *= bScale;
		by *= bScale;
		bz *= bScale;

		double [] circleX = new double[edgePoints+1];
		double [] circleY = new double[edgePoints+1];
		double [] circleZ = new double[edgePoints+1];

		for( int i = 0; i < edgePoints + 1; ++i ) {
			double angle = (i * 2 * Math.PI) / edgePoints;
			double c = Math.cos(angle);
			double s = Math.sin(angle);
			circleX[i] = x + radius * c * ax + radius * s * bx;
			circleY[i] = y + radius * c * ay + radius * s * by;
			circleZ[i] = z + radius * c * az + radius * s * bz;
		}
		final ArrayList<Point3f> list = new ArrayList<Point3f>();
		Point3f centre = new Point3f( (float)x, (float)y, (float)z );
		for( int i = 0; i < edgePoints; ++i ) {
			Point3f t2 = new Point3f( (float)circleX[i], (float)circleY[i], (float)circleZ[i] );
			Point3f t3 = new Point3f( (float)circleX[i+1], (float)circleY[i+1], (float)circleZ[i+1] );
			list.add( centre );
			list.add( t2 );
			list.add( t3 );
			list.add( centre );
			list.add( t3 );
			list.add( t2 );
		}
		return list;
	}

	static final private float phi = (1 + (float)Math.sqrt(5)) / 2;
	static final private float[][] icosahedron = { { phi, 1, 0 },
					{ -phi, 1, 0 },
					{ phi, -1, 0 },
					{ -phi, -1, 0 },
					{ 1, 0, phi },
					{ 1, 0, -phi },
					{-1, 0, phi },
					{-1, 0, -phi },
					{0, phi, 1 },
					{0, -phi, 1},
					{0, phi, -1 },
					{0, -phi, -1} };
	static final private int[][] icosfaces =    { { 0, 8, 4 },
					{ 0, 5, 10 },
					{ 2, 4, 9 },
					{ 2, 11, 5 },
					{ 1, 6, 8 },
					{ 1, 10, 7 },
					{ 3, 9, 6 },
					{ 3, 7, 11 },
					{ 0, 10, 8 },
					{ 1, 8, 10 },
					{ 2, 9, 11 },
					{ 3, 11, 9 },
					{ 4, 2, 0 },
					{ 5, 0, 2 },
					{ 6, 1, 3 },
					{ 7, 3, 1 },
					{ 8, 6, 4 },
					{ 9, 4, 6 },
					{ 10, 5, 7 },
					{ 11, 7, 5 } };

	/** Returns a "3D Viewer"-ready list mesh, centered at 0,0,0 and with radius as the radius of the enclosing sphere. */
	static public final List<Point3f> createIcosahedron(int subdivisions, final float radius) {
		List<Point3f> ps = new ArrayList<Point3f>();
		for (int i=0; i<icosfaces.length; i++) {
			for (int k=0; k<3; k++) {
				ps.add(new Point3f(icosahedron[icosfaces[i][k]]));
			}
		}
		while (subdivisions-- > 0) {
			final List<Point3f> sub = new ArrayList<Point3f>();
			// Take three consecutive points, which define a face, and create 4 faces out of them.
			for (int i=0; i<ps.size(); i+=3) {
				Point3f p0 = ps.get(i);
				Point3f p1 = ps.get(i+1);
				Point3f p2 = ps.get(i+2);

				Point3f p01 = new Point3f((p0.x + p1.x)/2, (p0.y + p1.y)/2, (p0.z + p1.z)/2);
				Point3f p02 = new Point3f((p0.x + p2.x)/2, (p0.y + p2.y)/2, (p0.z + p2.z)/2);
				Point3f p12 = new Point3f((p1.x + p2.x)/2, (p1.y + p2.y)/2, (p1.z + p2.z)/2);
				// lower left:
				sub.add(p0);
				sub.add(p01);
				sub.add(p02);
				// upper:
				sub.add(new Point3f(p01)); // as copies
				sub.add(p1);
				sub.add(p12);
				// lower right:
				sub.add(new Point3f(p12));
				sub.add(p2);
				sub.add(new Point3f(p02));
				// center:
				sub.add(new Point3f(p01));
				sub.add(new Point3f(p12));
				sub.add(new Point3f(p02));
			}
			ps = sub;
		}

		// Project all vertices to the surface of a sphere of radius 1
		final Vector3f v = new Vector3f();
		for (final Point3f p : ps) {
			v.set(p);
			v.normalize();
			v.scale(radius);
			p.set(v);
		}

		return ps;
	}

	static public final List<Point3f> copyTranslated(final List<Point3f> ps, final float dx, final float dy, final float dz) {
		final HashMap<Point3f,Point3f> m = new HashMap<Point3f,Point3f>();
		final ArrayList<Point3f> verts = new ArrayList<Point3f>();
		for (final Point3f p : ps) {
			Point3f p2 = m.get(p);
			if (null == p2) {
				p2 = new Point3f(p.x + dx, p.y + dy, p.z + dz);
				m.put(p, p2);
			}
			verts.add(p2);
		}
		return verts;
	}
}
