package isosurface;

import javax.vecmath.*;
import java.util.List;

/**
 * References.
 * 
 * Brian Mirtich, 2006, "Fast and Accurate Computation of Polyhedral Mass 
 * Properties", journal of graphics tools, volume 1, number 2, 1996.
 *
 * and
 *
 * http://www.geometrictools.com/Documentation/PolyhedralMassProperties.pdf
 */
public class MeshProperties {

	/**
	 * Returns the mass.
	 * @param p List of vertices (Point3fs).
	 * @param cm contains the center of gravity after the calculation
	 * @param inertia contains the inertia matrix after the calculation.
	 */
	public static double compute(List p, Point3d cm, double[][] inertia) {

		int tmax = p.size() / 3;
		final double[] mult = {1d/6, 1d/24, 1d/24,1d/24, 1d/60, 1d/60,
						1d/60, 1d/120, 1d/120, 1d/120};
		// order: 1, x, y, z, x^2, y^2, z^2, xy, yz, zx
		double[] intg = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; 

		double[] fg = new double[6];

		for(int t = 0; t < tmax; t++) {
			// get vertices of triangle t
			int i0 = 3 * t, i1 = 3 * t + 1, i2 = 3 * t + 2;

			double x0 = ((Point3f)p.get(i0)).x;
			double y0 = ((Point3f)p.get(i0)).y;
			double z0 = ((Point3f)p.get(i0)).z;
			double x1 = ((Point3f)p.get(i1)).x;
			double y1 = ((Point3f)p.get(i1)).y;
			double z1 = ((Point3f)p.get(i1)).z;
			double x2 = ((Point3f)p.get(i2)).x;
			double y2 = ((Point3f)p.get(i2)).y;
			double z2 = ((Point3f)p.get(i2)).z;

			// get edges and cross product of edges
			double a1 = x1 - x0, b1 = y1 - y0, c1 = z1 - z0;
			double a2 = x2 - x0, b2 = y2 - y0, c2 = z2 - z0;
			double d0 = b1 * c2 - b2 * c1;
			double d1 = a2 * c1 - a1 * c2;
			double d2 = a1 * b2 - a2 * b1;

			// compute integral terms
			subexpr(x0, x1, x2, fg);
			double f1x = fg[0], f2x = fg[1], f3x = fg[2];
			double g0x = fg[3], g1x = fg[4], g2x = fg[5];
			subexpr(y0, y1, y2, fg);
			double f1y = fg[0], f2y = fg[1], f3y = fg[2];
			double g0y = fg[3], g1y = fg[4], g2y = fg[5];
			subexpr(z0, z1, z2, fg);
			double f1z = fg[0], f2z = fg[1], f3z = fg[2];
			double g0z = fg[3], g1z = fg[4], g2z = fg[5];

			// update integrals
			intg[0] += d0 * f1x;
			intg[1] += d0 * f2x;
			intg[2] += d1 * f2y;
			intg[3] += d2 * f2z;
			intg[4] += d0 * f3x;
			intg[5] += d1 * f3y;
			intg[6] += d2 * f3z;
			intg[7] += d0*(y0 * g0x + y1 * g1x + y2 * g2x);
			intg[8] += d1*(z0 * g0y + z1 * g1y + z2 * g2y);
			intg[9] += d2*(x0 * g0z + x1 * g1z + x2 * g2z);
		}

		for (int i = 0; i < 10; i++)
			intg[i] *= mult[i];

		double mass = intg[0];

		// center of mass
		cm.x = (float)(intg[1] / mass);
		cm.y = (float)(intg[2] / mass);
		cm.z = (float)(intg[3] / mass);

		// inertia tensor relative to center of mass
		inertia[0][0] = intg[5]+intg[6]-mass*(cm.y*cm.y+cm.z*cm.z);
		inertia[1][1] = intg[4]+intg[6]-mass*(cm.z*cm.z+cm.x*cm.x);
		inertia[2][2] = intg[4]+intg[5]-mass*(cm.x*cm.x+cm.y*cm.y);
		inertia[0][1] = -(intg[7]-mass*cm.x*cm.y);
		inertia[1][2] = -(intg[8]-mass*cm.y*cm.z);
		inertia[0][2] = -(intg[9]-mass*cm.z*cm.x);

		return mass;
	}

	static void subexpr(double w0, double w1, double w2, double[] fg) {
		double temp0 = w0 + w1;
		fg[0] = temp0 + w2;
		double temp1 = w0 * w0;
		double temp2 = temp1 + w1 * temp0;
		fg[1] = temp2 + w2 * fg[0];
		fg[2] = w0 * temp1 + w1 * temp2 + w2 * fg[1];
		fg[3] = fg[1] + w0 * (fg[0] + w0);
		fg[4] = fg[1] + w1 * (fg[0] + w1);
		fg[5] = fg[1] + w2 * (fg[0] + w2);
	}

	public static void calculateMinMaxPoint(
			List mesh, Point3d min, Point3d max) {

		if(mesh == null)
			return;

		min.x = min.y = min.z = Double.MAX_VALUE;
		max.x = max.y = max.z = Double.MIN_VALUE;
		for(int i = 0; i < mesh.size(); i++) {
			Point3f p = (Point3f)mesh.get(i);
			if(p.x < min.x) min.x = p.x;
			if(p.y < min.y) min.y = p.y;
			if(p.z < min.z) min.z = p.z;
			if(p.x > max.x) max.x = p.x;
			if(p.y > max.y) max.y = p.y;
			if(p.z > max.z) max.z = p.z;
		}
	}
}

