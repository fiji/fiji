/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This is a cut-down version of the TrakEM2 VectorString3D class. */

/**

TrakEM2 plugin for ImageJ(C).
Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

In addition, as a special exception, the copyright holders give
you permission to combine this program with free software programs or
libraries that are released under the Apache Public License. 

You may contact Albert Cardona at acardona at ini.phys.ethz.ch
Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
**/

package ij3d;

import java.util.List;
import java.util.ArrayList;

import javax.vecmath.Point3f;

import Jama.Matrix;

import ij.IJ;

// A mini version of VectorString3D taken from ini.trakem2.vector.VectorString3D:
class VectorString3D {

	/** Points. */
	private double[] x, y, z;
	/** Vectors, after resampling. */
	private double[] vx, vy, vz;
	/** Relative vectors, after calling 'relative()'. */
	private double[] rvx, rvy, rvz;
	/** Length of points and vectors - since arrays may be a little longer. */
	private int length = 0;
	/** The point interdistance after resampling. */
	private double delta = 0;


	public VectorString3D(double[] x, double[] y, double[] z) {
		if (!(x.length == y.length && x.length == z.length))
			throw new RuntimeException("x,y,z must have the same length.");
		this.length = x.length;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/** Dependent arrays that will get resampled along. */
	private double[][] dep;

	/** Add an array that will get resampled along; must be of the same length as the value returned by length() */
	public void addDependent(final double[] a) throws Exception {
		if (a.length != this.length) throw new Exception("Dependent array must be of the same size as thevalue returned by length()");
		if (null == dep) {
			dep = new double[1][];
			dep[0] = a;
		} else {
			// resize and append
			double[][] dep2 = new double[dep.length + 1][];
			for (int i=0; i<dep.length; i++) dep2[i] = dep[i];
			dep2[dep.length] = a;
			dep = dep2;
		}
	}
	public double[] getDependent(final int i) {
		return dep[i];
	}

	/** Return the average point interdistance. */
	public double getAverageDelta() {
		double d = 0;
		for (int i=length -1; i>0; i--) {
			d += Math.sqrt( Math.pow(x[i] - x[i-1], 2) + Math.pow(y[i] - y[i-1], 2) + Math.pow(z[i] - z[i-1], 2));
		}
		return d / length;
	}

	/** Homogenize the average point interdistance to 'delta'. */
	public void resample(double delta) {
		if (Math.abs(delta - this.delta) < 0.0000001) {
			// delta is the same
			return;
		}
		this.delta = delta; // store for checking purposes
		this.resample();
	}

	/** The length of this string, that is, the number of points (and vectors) in it. */
	public final int length() { return length; }
	public double[] getPoints(final int dim) {
		switch (dim) {
		case 0: return x;
		case 1: return y;
		case 2: return z;
		}
		return null;
	}

	private boolean isClosed() {
		return false;
	}

	private static class ResamplingData {

		private double[] rx, ry, rz,
			vx, vy, vz;
		private double[][] dep;

		/** Initialize with a starting length. */
		ResamplingData(final int length, final double[][] dep) {
			// resampled points
			rx = new double[length];
			ry = new double[length];
			rz = new double[length];
			// vectors
			vx = new double[length];
			vy = new double[length];
			vz = new double[length];
			// dependents
			if (null != dep) this.dep = new double[dep.length][length];
		}
		/** Arrays are enlarged if necessary.*/
		final void setP(final int i, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.rx[i] = xval;
			this.ry[i] = yval;
			this.rz[i] = zval;
		}
		/** Arrays are enlarged if necessary.*/
		final void setV(final int i, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.vx[i] = xval;
			this.vy[i] = yval;
			this.vz[i] = zval;
		}
		/** Arrays are enlarged if necessary.*/
		final void setPV(final int i, final double rxval, final double ryval, final double rzval, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.rx[i] = rxval;
			this.ry[i] = ryval;
			this.rz[i] = rzval;
			this.vx[i] = xval;
			this.vy[i] = yval;
			this.vz[i] = zval;
		}
		final void resize(final int new_length) {
			this.rx = Utils.copy(this.rx, new_length);
			this.ry = Utils.copy(this.ry, new_length);
			this.rz = Utils.copy(this.rz, new_length);
			this.vx = Utils.copy(this.vx, new_length);
			this.vy = Utils.copy(this.vy, new_length);
			this.vz = Utils.copy(this.vz, new_length);
			if (null != dep) {
				// java doesn't have generators! ARGH
				double[][] dep2 = new double[dep.length][];
				for (int i=0; i<dep.length; i++) dep2[i] = Utils.copy(dep[i], new_length);
				dep = dep2;
			}
		}
		final double x(final int i) { return rx[i]; }
		final double y(final int i) { return ry[i]; }
		final double z(final int i) { return rz[i]; }
		/** Distance from point rx[i],ry[i], rz[i] to point x[j],y[j],z[j] */
		final double distance(final int i, final double x, final double y, final double z) {
			return Math.sqrt(Math.pow(x - rx[i], 2)
					 + Math.pow(y - ry[i], 2)
					 + Math.pow(z - rz[i], 2));
		}
		final void put(final VectorString3D vs, final int length) {
			vs.x = Utils.copy(this.rx, length); // crop away empty slots
			vs.y = Utils.copy(this.ry, length);
			vs.z = Utils.copy(this.rz, length);
			vs.vx = Utils.copy(this.vx, length);
			vs.vy = Utils.copy(this.vy, length);
			vs.vz = Utils.copy(this.vz, length);
			vs.length = length;
			if (null != dep) {
				vs.dep = new double[dep.length][];
				for (int i=0; i<dep.length; i++) vs.dep[i] = Utils.copy(dep[i], length);
			}
		}
		final void setDeps(final int i, final double[][] src_dep, final int[] ahead, final double[] weight, final int len) {
			if (null == dep) return;
			if (i >= rx.length) resize(i+10);
			//
			for (int k=0; k<dep.length; k++) {
				for (int j=0; j<len; j++) {
					dep[k][i] += src_dep[k][ahead[j]] * weight[j];
				}
			} // above, the ahead and weight arrays (which have the same length) could be of larger length than the given 'len', thus len is used.
		}
	}

	private static class Vector {
		private double x, y, z;
		private double length;
		// 0 coords and 0 length, virtue of the 'calloc'
		Vector() {}
		Vector(final double x, final double y, final double z) {
			set(x, y, z);
		}
		Vector(final Vector v) {
			this.x = v.x;
			this.y = v.y;
			this.z = v.z;
			this.length = v.length;
		}
		final public Object clone() {
			return new Vector(this);
		}
		final void set(final double x, final double y, final double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.length = computeLength();
		}
		final void normalize() {
			if (0 == length) return;
			// check if length is already 1
			if (Math.abs(1 - length) < 0.00000001) return; // already normalized
			this.x /= length;
			this.y /= length;
			this.z /= length;
			this.length = computeLength(); // should be 1
		}
		final double computeLength() {
			return Math.sqrt(x*x + y*y + z*z);
		}
		final double length() {
			return length;
		}
		final void scale(final double factor) {
			this.x *= factor;
			this.y *= factor;
			this.z *= factor;
			this.length = computeLength();
		}
		final void add(final Vector v, final boolean compute_length) {
			this.x += v.x;
			this.y += v.y;
			this.z += v.z;
			if (compute_length) this.length = computeLength();
		}
		final void setLength(final double len) {
			normalize();
			scale(len);
		}
		final void put(final int i, final ResamplingData r) {
			r.setPV(i, r.x(i-1) + this.x, r.y(i-1) + this.y, r.z(i-1) + this.z, this.x, this.y, this.z);
		}
		/** As row. */
		final void put(final double[] d) {
			d[0] = x;
			d[1] = y;
			d[2] = z;
		}
		/** As column. */
		final void put(final double[][] d, final int col) {
			d[0][col] = x;
			d[1][col] = y;
			d[2][col] = z;
		}
		final void put(final int i, final double[] x, final double[] y, final double[] z) {
			x[i] = this.x;
			y[i] = this.y;
			z[i] = this.z;
		}
		final Vector getCrossProduct(final Vector v) {
			// (a1; a2; a3) x (b1; b2; b3) = (a2b3 - a3b2; a3b1 - a1b3; a1b2 - a2b1)
			return new Vector(y * v.z - z * v.y,
					  z * v.x - x * v.z,
					  x * v.y - y * v.x);
		}
		final void setCrossProduct(final Vector v, final Vector w) {
			this.x = v.y * w.z - v.z * w.y;
			this.y = v.z * w.x - v.x * w.z;
			this.z = v.x * w.y - v.y * w.x;
		}
		/** Change coordinate system. */
		final void changeRef(final Vector v_delta, final Vector v_i1, final Vector v_new1) { // this vector works like new2
			// ortogonal system 1: the target
			// (a1'; a2'; a3')
			Vector a2 = new Vector(  v_new1   );  // vL
			a2.normalize();
			Vector a1 = a2.getCrossProduct(v_i1); // vQ
			a1.normalize();
			Vector a3 = a2.getCrossProduct(a1);
			// no need //a3.normalize();

			final double[][] m1 = new double[3][3];
			a1.put(m1, 0);
			a2.put(m1, 1);
			a3.put(m1, 2);
			final Matrix mat1 = new Matrix(m1);

			// ortogonal system 2: the current
			// (a1'; b2'; b3')
			Vector b2 = new Vector(  v_delta  ); // vA
			b2.normalize();
			Vector b3 = a1.getCrossProduct(b2); // vQ2

			final double[][] m2 = new double[3][3];
			a1.put(m2, 0);
			b2.put(m2, 1);
			b3.put(m2, 2);
			final Matrix mat2 = new Matrix(m2).transpose();

			final Matrix R = mat1.times(mat2);
			final Matrix mthis = new Matrix(new double[]{this.x, this.y, this.z}, 1);
			// The rotated vector as a one-dim matrix
			// (i.e. the rescued difference vector as a one-dimensional matrix)
			final Matrix v_rot = R.transpose().times(mthis.transpose()); // 3x3 times 3x1, hence the transposing of the 1x3
			final double[][] arr = v_rot.getArray();
			// done!
			this.x = arr[0][0];
			this.y = arr[1][0];
			this.z = arr[2][0];
		}
	}

	private final void recalculate(final double[] w, final int length, final double sum_) {
		double sum = 0;
		int q;
		for (q=0; q<length; q++) {
			w[q] = w[q] / sum_;
			sum += w[q];
		}
		double error = 1.0 - sum;
		// make it be an absolute value
		if (error < 0.0) {
			error = -error;
		}
		if (error < 0.005) {
			w[0] += 1.0 - sum;
		} else if (sum > 1.0) {
			recalculate(w, length, sum);
		}
	}

	private void resample() {
		// parameters
		final int MAX_AHEAD = 6;
		final double MAX_DISTANCE = 2.5 * delta;

		// convenient data carrier and editor
		final ResamplingData r = new ResamplingData(this.length, this.dep);
		final Vector vector = new Vector();

		// first resampled point is the same as point zero
		r.setDeps(0, dep, new int[]{0}, new double[]{1.0}, 1);
		r.setP(0, x[0], y[0], z[0]);
		// the first vector is 0,0,0 unless the path is closed, in which case it contains the vector from last-to-first.

		// index over x,y,z
		int i = 1;
		// index over rx,ry,rz (resampled points)
		int j = 1;
		// some vars
		int t, s, ii, u, iu, k;
		int prev_i = i;
		double dist_ahead, dist1, dist2, sum;
		final double[] w = new double[MAX_AHEAD];
		final double[] distances = new double[MAX_AHEAD];
		final Vector[] ve = new Vector[MAX_AHEAD];
		int next_ahead;
		for (next_ahead = 0; next_ahead < MAX_AHEAD; next_ahead++) ve[next_ahead] = new Vector();
		final int[] ahead = new int[MAX_AHEAD];

		try {

			// start infinite loop
			for (;prev_i <= i;) {
				if (prev_i > i || (!isClosed() && i == this.length -1)) break;
				// get distances of MAX_POINTs ahead from the previous point
				next_ahead = 0;
				for (t=0; t<MAX_AHEAD; t++) {
					s = i + t;
					// fix 's' if it goes over the end
					if (s >= this.length) {
						if (isClosed()) s -= this.length;
						else break;
					}
					dist_ahead = r.distance(j-1, x[s], y[s], z[s]);
					if (dist_ahead < MAX_DISTANCE) {
						ahead[next_ahead] = s;
						distances[next_ahead] = dist_ahead;
						next_ahead++;
					}
				}
				if (0 == next_ahead) {
					// No points (ahead of the i point) are found within MAX_DISTANCE
					// Just use the next point as target towards which create a vector of length delta
					vector.set(x[i] - r.x(j-1), y[i] - r.y(j-1), z[i] - r.z(j-1));
					dist1 = vector.length();
					vector.setLength(delta);
					vector.put(j, r);
					if (null != dep) r.setDeps(j, dep, new int[]{i}, new double[]{1.0}, 1);

					//System.out.println("j: " + j + " (ZERO)  " + vector.computeLength() + "  " + vector.length());

					//correct for point overtaking the not-close-enough point ahead in terms of 'delta_p' as it is represented in MAX_DISTANCE, but overtaken by the 'delta' used for subsampling:
					if (dist1 <= delta) {
						//look for a point ahead that is over distance delta from the previous j, so that it will lay ahead of the current j
						for (u=i; u<this.length; u++) {
							dist2 = Math.sqrt(Math.pow(x[u] - r.x(j-1), 2)
									  + Math.pow(y[u] - r.y(j-1), 2)
									  + Math.pow(z[u] - r.z(j-1), 2));
							if (dist2 > delta) {
								prev_i = i;
								i = u;
								break;
							}
						}
					}
				} else {
					// Compose a point ahead out of the found ones.
					//
					// First, adjust weights for the points ahead
					w[0] = distances[0] / MAX_DISTANCE;
					double largest = w[0];
					for (u=1; u<next_ahead; u++) {
						w[u] = 1 - (distances[u] / MAX_DISTANCE);
						if (w[u] > largest) {
							largest = w[u];
						}
					}
					// normalize weights: divide by largest
					sum = 0;
					for (u=0; u<next_ahead; u++) {
						w[u] = w[u] / largest;
						sum += w[u];
					}
					// correct error. The closest point gets the extra
					if (sum < 1.0) {
						w[0] += 1.0 - sum;
					} else {
						recalculate(w, next_ahead, sum);
					}
					// Now, make a vector for each point with the corresponding weight
					vector.set(0, 0, 0);
					for (u=0; u<next_ahead; u++) {
						iu = i + u;
						if (iu >= this.length) iu -= this.length;
						ve[u].set(x[iu] - r.x(j-1), y[iu] - r.y(j-1), z[iu] - r.z(j-1));
						ve[u].setLength(w[u] * delta);
						vector.add(ve[u], u == next_ahead-1); // compute the length only on the last iteration
					}
					// correct potential errors
					if (Math.abs(vector.length() - delta) > 0.00000001) {
						vector.setLength(delta);
					}
					// set
					vector.put(j, r);
					if (null != dep) r.setDeps(j, dep, ahead, w, next_ahead);

					//System.out.println("j: " + j + "  (" + next_ahead + ")   " + vector.computeLength() + "  " + vector.length());


					// find the first point that is right ahead of the newly added point
					// so: loop through points that lay within MAX_DISTANCE, and find the first one that is right past delta.
					ii = i;
					for (k=0; k<next_ahead; k++) {
						if (distances[k] > delta) {
							ii = ahead[k];
							break;
						}
					}
					// correct for the case of unseen point (because all MAX_POINTS ahead lay under delta):
					prev_i = i;
					if (i == ii) {
						i = ahead[next_ahead-1] +1; //the one after the last.
						if (i >= this.length) {
							if (isClosed()) i = i - this.length; // this.length is the length of the x,y,z, the original points
							else i = this.length -1;
						}
					} else {
						i = ii;
					}
				}
				//advance index in the new points
				j += 1;
			} // end of for loop

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Some data: x,y,z .length = " + x.length + "," + y.length + "," + z.length
					   + "\nj=" + j + ", i=" + i + ", prev_i=" + prev_i
				);
		}


		dist_ahead = r.distance(j-1, x[this.length-1], y[this.length-1], z[this.length-1]);

		//System.out.println("delta: " + delta + "\nlast point: " + x[x.length-1] + ", " + y[y.length-1] + ", " + z[z.length-1]);
		//System.out.println("last resampled point: x,y,z " + r.x(j-1) + ", " + r.y(j-1) + ", " + r.z(j-1));
		//System.out.println("distance: " + dist_ahead);

		// see whether the subsampling terminated too early, and fill with a line of points.
		final int last_i = isClosed() ? 0 : this.length -1;
		if (dist_ahead > delta*1.2) {
			//TODO//System.out.println("resampling terminated too early. Why?");
			while (dist_ahead > delta*1.2) {
				// make a vector from the last resampled point to the last point
				vector.set(x[last_i] - r.x(j-1), y[last_i] - r.y(j-1), z[last_i] - r.z(j-1));
				// resize it to length delta
				vector.setLength(delta);
				vector.put(j, r);
				j++;
				dist_ahead = r.distance(j-1, x[last_i], y[last_i], z[last_i]);
			}
		}
		// done!
		r.put(this, j); // j acts as length of resampled points and vectors
		// vector at zero is left as 0,0 which makes no sense. Should be the last point that has no vector, or has it only in the event that the list of points is declared as closed: a vector to the first point. Doesn't really matter though, as long as it's clear: as of right now, the first point has no vector unless the path is closed, in which case it contains the vector from the last-to-first.
	}
}
