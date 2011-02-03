package mpicbg.spim.vib;

import java.util.StringTokenizer;
import java.util.Vector;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Tuple3d;
import javax.vecmath.Tuple3f;

import math3d.JacobiFloat;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.vib.math3d.*;

import ij.measure.Calibration;
import ij.ImagePlus;

public class FastMatrix{

	public double x, y, z;

	protected double a00, a01, a02, a03,
		a10, a11, a12, a13,
		a20, a21, a22, a23;
	
	public FastMatrix() { }
	
	public FastMatrix(double f) { a00 = a11 = a22 = f; }
	
	public FastMatrix(double[][] m) {
		if ((m.length != 3 && m.length != 4)
		    || m[0].length != 4)
			throw new RuntimeException("Wrong dimensions: "
						   + m.length + "x"
						   + m[0].length);
		
		a00 = (double)m[0][0];
		a01 = (double)m[0][1];
		a02 = (double)m[0][2];
		a03 = (double)m[0][3];
		a10 = (double)m[1][0];
		a11 = (double)m[1][1];
		a12 = (double)m[1][2];
		a13 = (double)m[1][3];
		a20 = (double)m[2][0];
		a21 = (double)m[2][1];
		a22 = (double)m[2][2];
		a23 = (double)m[2][3];
	}
	
	public FastMatrix(FastMatrix f) {
		x = y = z = 0;
		a00 = f.a00;
		a01 = f.a01;
		a02 = f.a02;
		a03 = f.a03;
		a10 = f.a10;
		a11 = f.a11;
		a12 = f.a12;
		a13 = f.a13;
		a20 = f.a20;
		a21 = f.a21;
		a22 = f.a22;
		a23 = f.a23;
	}

/*	public FastMatrix copyFrom(FloatMatrix f) {
		x = y = z = 0;
		a00 = f.a00;
		a01 = f.a01;
		a02 = f.a02;
		a03 = f.a03;
		a10 = f.a10;
		a11 = f.a11;
		a12 = f.a12;
		a13 = f.a13;
		a20 = f.a20;
		a21 = f.a21;
		a22 = f.a22;
		a23 = f.a23;
		return this;
	}
*/
	public boolean isJustTranslation() {
		
		FastMatrix toTest = new FastMatrix(this);
		toTest.a03 -= a03;
		toTest.a13 -= a13;
		toTest.a23 -= a23;
		
		return toTest.isIdentity();
		
	}
	
	public boolean noTranslation() {
		
		double eps = (double)1e-10;
		
		return ((double)Math.abs(a03) < eps)
			&& ((double)Math.abs(a13) < eps)
			&& ((double)Math.abs(a23) < eps);
		
	}
	
	public FastMatrix composeWith(FastMatrix followedBy) {
		
		// Alias this and followedBy to A and B, with entries a_ij...
		
		FastMatrix A = this;
		FastMatrix B = followedBy;
		
		FastMatrix result = new FastMatrix();
		
		result.a00 = (A.a00 * B.a00) + (A.a10 * B.a01) + (A.a20 * B.a02);
		result.a10 = (A.a00 * B.a10) + (A.a10 * B.a11) + (A.a20 * B.a12);
		result.a20 = (A.a00 * B.a20) + (A.a10 * B.a21) + (A.a20 * B.a22);
		
		result.a01 = (A.a01 * B.a00) + (A.a11 * B.a01) + (A.a21 * B.a02);
		result.a11 = (A.a01 * B.a10) + (A.a11 * B.a11) + (A.a21 * B.a12);
		result.a21 = (A.a01 * B.a20) + (A.a11 * B.a21) + (A.a21 * B.a22);
		
		result.a02 = (A.a02 * B.a00) + (A.a12 * B.a01) + (A.a22 * B.a02);
		result.a12 = (A.a02 * B.a10) + (A.a12 * B.a11) + (A.a22 * B.a12);
		result.a22 = (A.a02 * B.a20) + (A.a12 * B.a21) + (A.a22 * B.a22);
		
		result.a03 = (A.a03 * B.a00) + (A.a13 * B.a01) + (A.a23 * B.a02) + B.a03;
		result.a13 = (A.a03 * B.a10) + (A.a13 * B.a11) + (A.a23 * B.a12) + B.a13;
		result.a23 = (A.a03 * B.a20) + (A.a13 * B.a21) + (A.a23 * B.a22) + B.a23;
		
		return result;
	}
	
	/* This decomposes the transformation into the 3x3 part (the first
	   FastMatrix in the returned array) and the translation (the
	   second FastMatrix).  (So, applying these two in the order
	   returned in the array should be the same as applying the
	   original.)
	   
	*/
	
	public FastMatrix[] decompose() {
		
		FastMatrix[] result = new FastMatrix[2];
		
		result[0].a00 = a00;
		result[0].a01 = a01;
		result[0].a02 = a02;
		result[0].a10 = a10;
		result[0].a11 = a11;
		result[0].a12 = a12;
		result[0].a20 = a20;
		result[0].a21 = a21;
		result[0].a22 = a22;
		
		result[1].a03 = a03;
		result[1].a13 = a13;
		result[1].a23 = a23;
		
		return result;
		
	}
	
	public FastMatrix plus(FastMatrix other) {
		
		FastMatrix result = new FastMatrix();
		result.a00 = other.a00 + this.a00;
		result.a01 = other.a01 + this.a01;
		result.a02 = other.a02 + this.a02;
		result.a03 = other.a03 + this.a03;
		result.a10 = other.a10 + this.a10;
		result.a11 = other.a11 + this.a11;
		result.a12 = other.a12 + this.a12;
		result.a13 = other.a13 + this.a13;
		result.a20 = other.a20 + this.a20;
		result.a21 = other.a21 + this.a21;
		result.a22 = other.a22 + this.a22;
		result.a23 = other.a23 + this.a23;
		return result;
	}
	
	/*
    public FastMatrix(Jama.Matrix m) {
        if ((m.getRowDimension() != 3 && m.getRowDimension() != 4)
                || m.getColumnDimension() != 4)
            throw new RuntimeException("Wrong dimensions: "
                    + m.getRowDimension() + "x"
                    + m.getColumnDimension());

        a00 = (double)m.get(0,0);
        a01 = (double)m.get(0,1);
        a02 = (double)m.get(0,2);
        a03 = (double)m.get(0,3);
        a10 = (double)m.get(1,0);
        a11 = (double)m.get(1,1);
        a12 = (double)m.get(1,2);
        a13 = (double)m.get(1,3);
        a20 = (double)m.get(2,0);
        a21 = (double)m.get(2,1);
        a22 = (double)m.get(2,2);
        a23 = (double)m.get(2,3);
    }
    */
	
	public void apply(double x, double y, double z) {
		this.x = x * a00 + y * a01 + z * a02 + a03;
		this.y = x * a10 + y * a11 + z * a12 + a13;
		this.z = x * a20 + y * a21 + z * a22 + a23;
	}
	
	public void apply(Point3d p) {
		this.x = (double)(p.x * a00 + p.y * a01 + p.z * a02 + a03);
		this.y = (double)(p.x * a10 + p.y * a11 + p.z * a12 + a13);
		this.z = (double)(p.x * a20 + p.y * a21 + p.z * a22 + a23);
	}
	
	public void apply(double[] p) {
		this.x = (double)(p[0] * a00 + p[1] * a01 + p[2] * a02 + a03);
		this.y = (double)(p[0] * a10 + p[1] * a11 + p[2] * a12 + a13);
		this.z = (double)(p[0] * a20 + p[1] * a21 + p[2] * a22 + a23);
	}
	
	public void applyWithoutTranslation(double x, double y, double z) {
		this.x = x * a00 + y * a01 + z * a02;
		this.y = x * a10 + y * a11 + z * a12;
		this.z = x * a20 + y * a21 + z * a22;
	}
	
	public void applyWithoutTranslation(Point3d p) {
		this.x = (double)(p.x * a00 + p.y * a01 + p.z * a02);
		this.y = (double)(p.x * a10 + p.y * a11 + p.z * a12);
		this.z = (double)(p.x * a20 + p.y * a21 + p.z * a22);
	}
	
	public Point3d getResult() {
		return new Point3d(x, y, z);
	}
	
	public FastMatrix scale(double x, double y, double z) {
		FastMatrix result = new FastMatrix();
		result.a00 = a00 * x;
		result.a01 = a01 * x;
		result.a02 = a02 * x;
		result.a03 = a03 * x;
		result.a10 = a10 * y;
		result.a11 = a11 * y;
		result.a12 = a12 * y;
		result.a13 = a13 * y;
		result.a20 = a20 * z;
		result.a21 = a21 * z;
		result.a22 = a22 * z;
		result.a23 = a23 * z;
		return result;
	}
	
	public FastMatrix times(FastMatrix o) {
		FastMatrix result = new FastMatrix();
		result.a00 = o.a00 * a00 + o.a10 * a01 + o.a20 * a02;
		result.a10 = o.a00 * a10 + o.a10 * a11 + o.a20 * a12;
		result.a20 = o.a00 * a20 + o.a10 * a21 + o.a20 * a22;
		result.a01 = o.a01 * a00 + o.a11 * a01 + o.a21 * a02;
		result.a11 = o.a01 * a10 + o.a11 * a11 + o.a21 * a12;
		result.a21 = o.a01 * a20 + o.a11 * a21 + o.a21 * a22;
		result.a02 = o.a02 * a00 + o.a12 * a01 + o.a22 * a02;
		result.a12 = o.a02 * a10 + o.a12 * a11 + o.a22 * a12;
		result.a22 = o.a02 * a20 + o.a12 * a21 + o.a22 * a22;
		apply(o.a03, o.a13, o.a23);
		result.a03 = x;
		result.a13 = y;
		result.a23 = z;
		return result;
	}
	
	public double det( ) {
		double sub00 = a11 * a22 - a12 * a21;
		double sub01 = a10 * a22 - a12 * a20;
		double sub02 = a10 * a21 - a11 * a20;
		/*double sub10 = a01 * a22 - a02 * a21;
		double sub11 = a00 * a22 - a02 * a20;
		double sub12 = a00 * a21 - a01 * a20;
		double sub20 = a01 * a12 - a02 * a11;
		double sub21 = a00 * a12 - a02 * a10;
		double sub22 = a00 * a11 - a01 * a10;*/
		return a00 * sub00 - a01 * sub01 + a02 * sub02;
	}
	
	/* this inverts just the first 3 columns, interpreted as 3x3 matrix */
	private FastMatrix invert3x3() {
		double sub00 = a11 * a22 - a12 * a21;
		double sub01 = a10 * a22 - a12 * a20;
		double sub02 = a10 * a21 - a11 * a20;
		double sub10 = a01 * a22 - a02 * a21;
		double sub11 = a00 * a22 - a02 * a20;
		double sub12 = a00 * a21 - a01 * a20;
		double sub20 = a01 * a12 - a02 * a11;
		double sub21 = a00 * a12 - a02 * a10;
		double sub22 = a00 * a11 - a01 * a10;
		double det = a00 * sub00 - a01 * sub01 + a02 * sub02;
		
		FastMatrix result = new FastMatrix();
		result.a00 = sub00 / det;
		result.a01 = -sub10 / det;
		result.a02 = sub20 / det;
		result.a10 = -sub01 / det;
		result.a11 = sub11 / det;
		result.a12 = -sub21 / det;
		result.a20 = sub02 / det;
		result.a21 = -sub12 / det;
		result.a22 = sub22 / det;
		return result;
	}
	
	public FastMatrix inverse() {
		FastMatrix result = invert3x3();
		result.apply(-a03, -a13, -a23);
		result.a03 = result.x;
		result.a13 = result.y;
		result.a23 = result.z;
		return result;
	}
	
	public static FastMatrix rotate(double angle, int axis) {
		FastMatrix result = new FastMatrix();
		double c = (double)Math.cos(angle);
		double s = (double)Math.sin(angle);
		switch(axis) {
		case 0:
			result.a11 = result.a22 = c;
			result.a12 = -(result.a21 = s);
			result.a00 = (double)1.0;
			break;
		case 1:
			result.a00 = result.a22 = c;
			result.a02 = -(result.a20 = s);
			result.a11 = (double)1.0;
			break;
		case 2:
			result.a00 = result.a11 = c;
			result.a01 = -(result.a10 = s);
			result.a22 = (double)1.0;
			break;
		default:
			throw new RuntimeException("Illegal axis: "+axis);
		}
		return result;
	}

	/*
	 * This rotates in a "geodesic" fashion: if you imagine an equator
	 * through a and b, the resulting rotation will have the poles as
	 * invariants.
	 */
	public static FastMatrix rotateFromTo(double aX, double aY, double aZ,
			double bX, double bY, double bZ) {
		double l = (double)Math.sqrt(aX * aX + aY * aY + aZ * aZ);
		aX /= l; aY /= l; aZ /= l;
		l = (double)Math.sqrt(bX * bX + bY * bY + bZ * bZ);
		bX /= l; bY /= l; bZ /= l;
		double cX, cY, cZ;
		cX = aY * bZ - aZ * bY;
		cY = aZ * bX - aX * bZ;
		cZ = aX * bY - aY * bX;
		double pX, pY, pZ;
		pX = cY * aZ - cZ * aY;
		pY = cZ * aX - cX * aZ;
		pZ = cX * aY - cY * aX;
		double qX, qY, qZ;
		qX = cY * bZ - cZ * bY;
		qY = cZ * bX - cX * bZ;
		qZ = cX * bY - cY * bX;
		FastMatrix result = new FastMatrix();
		result.a00 = aX; result.a01 = aY; result.a02 = aZ;
		result.a10 = cX; result.a11 = cY; result.a12 = cZ;
		result.a20 = pX; result.a21 = pY; result.a22 = pZ;
		FastMatrix transp = new FastMatrix();
		transp.a00 = bX; transp.a01 = cX; transp.a02 = qX;
		transp.a10 = bY; transp.a11 = cY; transp.a12 = qY;
		transp.a20 = bZ; transp.a21 = cZ; transp.a22 = qZ;
		return transp.times(result);
	}

	// ------------------------------------------------------------------------
	// FIXME: This probably isn't the best place for these static functions...
	
	public static double dotProduct(double[] a, double[] b) {
		double result = 0;
		if (a.length != b.length)
			throw new IllegalArgumentException(
				"In dotProduct, the vectors must be of the same length.");
		if (a.length < 1)
			throw new IllegalArgumentException(
				"Can't dotProduct vectors of zero length.");
		for (int i = 0; i < a.length; ++i)
			result += (a[i] * b[i]);
		return result;
	}
	
	public static double sizeSquared(double[] a) {
		return dotProduct(a,a);
	}
	
	public static double size(double[] a) {
		return (double)Math.sqrt(dotProduct(a,a));
	}
	
	public static double angleBetween(double[] v1, double[] v2) {
		return (double)Math.acos(dotProduct(v1,v2)/(size(v1)*size(v2)));
	}
	
	public static double[] crossProduct(double[] a, double[] b) {
		
		double[] result = { a[1] * b[2] - a[2] * b[1],
				    a[2] * b[0] - a[0] * b[2],
				    a[0] * b[1] - a[1] * b[0] };
		
		return result;
		
	}
	
	public static double[] normalize( double[] a ) {
		double magnitude = size(a);
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; ++i)
			result[i] = a[i]/magnitude;
		return result;
	}
	
	/* Find a first rotation to map v2_domain to v2_template.
	   Then project the v1s down onto the plane defined by
	   v2_template, and find the rotation about v2_domain that
	   lines up the projected V1s. */
	
	/* FIXME: with the PCA results, then v2 and v1 are always
	   going to be orthogonal anyway, so in fact the code that
	   projects onto the plane hasn't been sensibly tested (and is
	   useless for the moment.). */
	
	public static FastMatrix rotateToAlignVectors(double[] v2_template,
						      double[] v1_template,
						      double[] v2_domain,
						      double[] v1_domain ) {
		
		double angleBetween = angleBetween(v2_domain,
						   v2_template);
		
		double[] normal = crossProduct(v2_domain,
					       v2_template);
		
		double[] normalUnit = normalize(normal);
		
		FastMatrix rotation = FastMatrix.rotateAround(normalUnit[0],
							      normalUnit[1],
							      normalUnit[2],
							      angleBetween);
		
                /*
		  If v2 is the vector with the largest eigenvalue and v1 is
		  that with the second largest, then the projection of v1
		  onto the plane defined by v2 as a normal is:

 		  v1 - ( (v1.v2) / |v2||v2| ) v2

		*/

		double scale_v2_domain =  dotProduct(v1_domain,v2_domain)
			/ sizeSquared(v2_domain);

		double[] v1_orthogonal_domain = new double[3];
		
		v1_orthogonal_domain[0] = v1_domain[0] - scale_v2_domain * v2_domain[0];
		v1_orthogonal_domain[1] = v1_domain[1] - scale_v2_domain * v2_domain[1];
		v1_orthogonal_domain[2] = v1_domain[2] - scale_v2_domain * v2_domain[2];
		
		// Now for the template as well:

		double scale_v2_template = dotProduct(v1_template,v2_template)
			/ sizeSquared(v2_template);
		
		double [] v1_orthogonal_template = new double[3];
		
		v1_orthogonal_template[0] = v1_template[0] - scale_v2_template * v2_template[0];
		v1_orthogonal_template[1] = v1_template[1] - scale_v2_template * v2_template[1];
		v1_orthogonal_template[2] = v1_template[2] - scale_v2_template * v2_template[2];
		
		// Now we should rotate the one in the domain by the same
		// rotation as we applied to the most significant eigenvector...
		
		rotation.apply(v1_orthogonal_domain[0],
			       v1_orthogonal_domain[1],
			       v1_orthogonal_domain[2]);
		
		double[] v1_orthogonal_domain_rotated = new double[3];
		v1_orthogonal_domain_rotated[0] = rotation.x;
		v1_orthogonal_domain_rotated[1] = rotation.y;
		v1_orthogonal_domain_rotated[2] = rotation.z;
		
		// Now we need to find the rotation around v2 in the template
		// that will line up the projected v1s...
		
		double angleBetweenV1sA = angleBetween(v1_orthogonal_domain_rotated,
						       v1_orthogonal_template );
		
		double[] normalToV1sA = crossProduct(v1_orthogonal_domain_rotated,
						      v1_orthogonal_template );

		
		double[] normalToV1sAUnit = normalize(normalToV1sA);
		
		FastMatrix secondRotationA = FastMatrix.rotateAround(normalToV1sAUnit[0],
								     normalToV1sAUnit[1],
								     normalToV1sAUnit[2],
								     angleBetweenV1sA);

		return rotation.composeWith(secondRotationA);

	}
	
	public static FastMatrix rotateAround(double nx, double ny, double nz,
					      double angle) {
		FastMatrix r = new FastMatrix();
		double c = (double)Math.cos(angle), s = (double)Math.sin(angle);
		
		r.a00 = -(c-1)*nx*nx + c;
		r.a01 = -(c-1)*nx*ny - s*nz;
		r.a02 = -(c-1)*nx*nz + s*ny;
		r.a03 = 0;
		r.a10 = -(c-1)*nx*ny + s*nz;
		r.a11 = -(c-1)*ny*ny + c;
		r.a12 = -(c-1)*ny*nz - s*nx;
		r.a13 = 0;
		r.a20 = -(c-1)*nx*nz - s*ny;
		r.a21 = -(c-1)*ny*nz + s*nx;
		r.a22 = -(c-1)*nz*nz + c;
		r.a23 = 0;

		return r;
	}
	
	/*
	 * Euler rotation means to rotate around the z axis first, then
	 * around the rotated x axis, and then around the (twice) rotated
	 * z axis.
	 */
	public static FastMatrix rotateEuler(double a1, double a2, double a3) {
		FastMatrix r = new FastMatrix();
		double c1 = (double)Math.cos(a1), s1 = (double)Math.sin(a1);
		double c2 = (double)Math.cos(a2), s2 = (double)Math.sin(a2);
		double c3 = (double)Math.cos(a3), s3 = (double)Math.sin(a3);
		
		r.a00 = c3*c1-c2*s1*s3;
		r.a01 = -s3*c1-c2*s1*c3;
		r.a02 = s2*s1;
		r.a03 = 0;
		r.a10 = c3*s1+c2*c1*s3;
		r.a11 = -s3*s1+c2*c1*c3;
		r.a12 = -s2*c1;
		r.a13 = 0;
		r.a20 = s2*s3;
		r.a21 = s2*c3;
		r.a22 = c2;
		r.a23 = 0;
		
		return r;
	}

	/*
	 * same as rotateEuler, but with a center different from the origin
	 */
	public static FastMatrix rotateEulerAt(double a1, double a2, double a3,
					       double cx, double cy, double cz) {
		FastMatrix r = new FastMatrix();
		double c1 = (double)Math.cos(a1), s1 = (double)Math.sin(a1);
		double c2 = (double)Math.cos(a2), s2 = (double)Math.sin(a2);
		double c3 = (double)Math.cos(a3), s3 = (double)Math.sin(a3);
		
		r.a00 = c3*c1-c2*s1*s3;
		r.a01 = -s3*c1-c2*s1*c3;
		r.a02 = s2*s1;
		r.a03 = 0;
		r.a10 = c3*s1+c2*c1*s3;
		r.a11 = -s3*s1+c2*c1*c3;
		r.a12 = -s2*c1;
		r.a13 = 0;
		r.a20 = s2*s3;
		r.a21 = s2*c3;
		r.a22 = c2;
		r.a23 = 0;
		
		r.apply(cx, cy, cz);
		r.a03 = cx - r.x;
		r.a13 = cy - r.y;
		r.a23 = cz - r.z;
		
		return r;
	}
	
	/*
	 * Calculate the parameters needed to generate this matrix by
	 * rotateEulerAt()
	 */
	public void guessEulerParameters(double[] parameters) {
		if (parameters.length != 6)
			throw new IllegalArgumentException(
				"Need 6 parameters, got "
				+ parameters.length);
		guessEulerParameters(parameters, null);
	}
	
	public void guessEulerParameters(double[] parameters, Point3d center) {
		if (center != null && parameters.length != 9)
			throw new IllegalArgumentException(
				"Need 9 parameters, got "
				+ parameters.length);
		
		if (a21 == 0.0 && a20 == 0.0) {
			/*
			 * s2 == 0, therefore a2 == 0, therefore a1 and a3
			 * are not determined (they are both rotations around
			 * the z axis. Choose a3 = 0.
			 */
			parameters[2] = 0;
			parameters[1] = 0;
			parameters[0] = (double)Math.atan2(a10, a00);
		} else {
			parameters[2] = (double)Math.atan2(a20, a21);
			parameters[1] = (double)Math.atan2(
				Math.sqrt(a21 * a21 + a20 * a20), a22);
			parameters[0] = (double)Math.atan2(a02, -a12);
		}
		
		/*
		 * If a center of rotation was given, the parameters will
		 * contain:
		 * (angleZ, angleX, angleZ2, transX, transY, transZ,
		 *  centerX, centerY, centerZ) where trans is the translation
		 *  _after_ the rotation around center.
		 */
		if (center != null) {
			parameters[6] = (double)center.x;
			parameters[7] = (double)center.y;
			parameters[8] = (double)center.z;
			apply(center);
			parameters[3] = x - (double)center.x;
			parameters[4] = y - (double)center.y;
			parameters[5] = z - (double)center.z;
			return;
		}
		
		/*
		 * The center (if none was specified) is ambiguous along
		 * the rotation axis.
		 * To find a center, we rotate the origin twice, and
		 * calculate the circumcenter of the resulting triangle.
		 * This also happens to be the point on the axis which
		 * is closest to the origin.
		 */
		if (a03 == 0.0 && a13 == 0.0 && a23 == 0.0) {
			parameters[3] = parameters[4] = parameters[5] = 0;
		} else {
			apply(a03, a13, a23);
			Triangle t = new Triangle(
				new Point3d(0, 0, 0),
				new Point3d(a03, a13, a23),
				new Point3d(x, y, z));
			t.calculateCircumcenter2();
			parameters[3] = (double)t.center.x;
			parameters[4] = (double)t.center.y;
			parameters[5] = (double)t.center.z;
		}
	}
	
	public static FastMatrix translate(double x, double y, double z) {
		FastMatrix result = new FastMatrix();
		result.a00 = result.a11 = result.a22 = (double)1.0;
		result.a03 = x;
		result.a13 = y;
		result.a23 = z;
		return result;
	}
	
	/*
	 * least squares fitting of a linear transformation which maps
	 * the points x[i] to y[i] as best as possible.
	 */
	public static FastMatrix bestLinear(Point3d[] x, Point3d[] y) {
		if (x.length != y.length)
			throw new RuntimeException("different lengths");
		
		double[][] a = new double[4][4];
		double[][] b = new double[4][4];
		
		for (int i = 0; i < a.length; i++) {
			a[0][0] += (double)(x[i].x * x[i].x);
			a[0][1] += (double)(x[i].x * x[i].y);
			a[0][2] += (double)(x[i].x * x[i].z);
			a[0][3] += (double)(x[i].x);
			a[1][1] += (double)(x[i].y * x[i].y);
			a[1][2] += (double)(x[i].y * x[i].z);
			a[1][3] += (double)(x[i].y);
			a[2][2] += (double)(x[i].z * x[i].z);
			a[2][3] += (double)(x[i].z);
			
			b[0][0] += (double)(x[i].x * y[i].x);
			b[0][1] += (double)(x[i].y * y[i].x);
			b[0][2] += (double)(x[i].z * y[i].x);
			b[0][3] += (double)(y[i].x);
			b[1][0] += (double)(x[i].x * y[i].y);
			b[1][1] += (double)(x[i].y * y[i].y);
			b[1][2] += (double)(x[i].z * y[i].y);
			b[1][3] += (double)(y[i].y);
			b[2][0] += (double)(x[i].x * y[i].z);
			b[2][1] += (double)(x[i].y * y[i].z);
			b[2][2] += (double)(x[i].z * y[i].z);
			b[2][3] += (double)(y[i].z);
		}
		
		a[1][0] = a[0][1];
		a[2][0] = a[0][2];
		a[2][1] = a[1][2];
		a[3][0] = a[0][3];
		a[3][1] = a[1][3];
		a[3][2] = a[2][3];
		a[3][3] = 1;
		FastMatrixN.invert(a);
		double[][] r = FastMatrixN.times(b, a);
		
		FastMatrix result = new FastMatrix();
		result.a00 = r[0][0];
		result.a01 = r[0][1];
		result.a02 = r[0][2];
		result.a03 = r[0][3];
		result.a10 = r[1][0];
		result.a11 = r[1][1];
		result.a12 = r[1][2];
		result.a13 = r[1][3];
		result.a20 = r[2][0];
		result.a21 = r[2][1];
		result.a22 = r[2][2];
		result.a23 = r[2][3];
		return result;
	}

	/**
	 * Find the best rigid transformation from set1 to set2.
	 * This function uses the method by Horn, using quaternions:
	 * Closed-form solution of absolute orientation using unit quaternions,
	 * Horn, B. K. P., Journal of the Optical Society of America A,
	 * Vol. 4, page 629, April 1987
	 */
	public static FastMatrix bestRigid(Point3d[] set1, Point3d[] set2) {
		return bestRigid(set1, set2, true);
	}

	public static FastMatrix bestRigid(Point3d[] set1, Point3d[] set2,
			boolean allowScaling) {
		if (set1.length != set2.length)
			throw new RuntimeException("different lengths");

		double c1x, c1y, c1z, c2x, c2y, c2z;
		c1x = c1y = c1z = c2x = c2y = c2z = 0;

		for (int i = 0; i < set1.length; i++) {
			c1x += (double)set1[i].x;
			c1y += (double)set1[i].y;
			c1z += (double)set1[i].z;
			c2x += (double)set2[i].x;
			c2y += (double)set2[i].y;
			c2z += (double)set2[i].z;
		}
		c1x /= set1.length;
		c1y /= set1.length;
		c1z /= set1.length;
		c2x /= set1.length;
		c2y /= set1.length;
		c2z /= set1.length;

		double s = 1;
		if (allowScaling) {
			double r1, r2;
			r1 = r2 = 0;
			for (int i = 0; i < set1.length; i++) {
				double x1 = (double)set1[i].x - c1x;
				double y1 = (double)set1[i].y - c1y;
				double z1 = (double)set1[i].z - c1z;
				double x2 = (double)set2[i].x - c2x;
				double y2 = (double)set2[i].y - c2y;
				double z2 = (double)set2[i].z - c2z;
				r1 += x1 * x1 + y1 * y1 + z1 * z1;
				r2 += x2 * x2 + y2 * y2 + z2 * z2;
			}
			s = (double)Math.sqrt(r2 / r1);
		}

		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for (int i = 0; i < set1.length; i++) {
			double x1 = ((double)set1[i].x - c1x) * s;
			double y1 = ((double)set1[i].y - c1y) * s;
			double z1 = ((double)set1[i].z - c1z) * s;
			double x2 = (double)set2[i].x - c2x;
			double y2 = (double)set2[i].y - c2y;
			double z2 = (double)set2[i].z - c2z;
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}
		double[][] N = new double[4][4];
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		JacobiDouble jacobi = new JacobiDouble(N);
		double[][] eigenvectors = jacobi.getEigenVectors();
		double[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		double[] q = eigenvectors[index];
		double q0 = q[0], qx = q[1], qy = q[2], qz = q[3];


		// turn into matrix
		FastMatrix result = new FastMatrix();
		// rotational part
		result.a00 = s * (q0 * q0 + qx * qx - qy * qy - qz * qz);
		result.a01 = s * 2 * (qx * qy - q0 * qz);
		result.a02 = s * 2 * (qx * qz + q0 * qy);
		result.a10 = s * 2 * (qy * qx + q0 * qz);
		result.a11 = s * (q0 * q0 - qx * qx + qy * qy - qz * qz);
		result.a12 = s * 2 * (qy * qz - q0 * qx);
		result.a20 = s * 2 * (qz * qx - q0 * qy);
		result.a21 = s * 2 * (qz * qy + q0 * qx);
		result.a22 = s * (q0 * q0 - qx * qx - qy * qy + qz * qz);
		// translational part
		result.apply(c1x, c1y, c1z);
		result.a03 = c2x - result.x;
		result.a13 = c2y - result.y;
		result.a23 = c2z - result.z;
		return result;
	}

	public static Matrix4d bestRigid(final Tuple3d[] set1, final Tuple3d[] set2, final float[] w) 
	{
		return bestRigid(set1, set2, w, true);
	}

	public static Matrix4d bestRigid(final Tuple3d[] set1, final Tuple3d[] set2, final boolean allowScaling) 
	{
		float[] w = new float[set1.length];
		for (int i = 0; i < w.length; i++)
			w[i] = 1.0f;
		
		return bestRigid(set1, set2, w, allowScaling);
	}
	
	public static Matrix4d bestRigid(final Tuple3d[] set1, final Tuple3d[] set2, final float[] w, final boolean allowScaling) 
	{
		if (set1.length != set2.length)
			throw new RuntimeException("different lengths");

		double c1x, c1y, c1z, c2x, c2y, c2z;
		c1x = c1y = c1z = c2x = c2y = c2z = 0;
		
		double sumW = 0;

		for (int i = 0; i < set1.length; i++) {
			c1x += (double)set1[i].x * w[i];
			c1y += (double)set1[i].y * w[i];
			c1z += (double)set1[i].z * w[i];
			c2x += (double)set2[i].x * w[i];
			c2y += (double)set2[i].y * w[i];
			c2z += (double)set2[i].z * w[i];
			sumW += w[i];
		}
		c1x /= sumW;
		c1y /= sumW;
		c1z /= sumW;
		c2x /= sumW;
		c2y /= sumW;
		c2z /= sumW;

		double s = 1;
		if (allowScaling) {
			double r1, r2;
			r1 = r2 = 0;
			for (int i = 0; i < set1.length; i++) {
				double x1 = ((double)set1[i].x - c1x) * w[i];
				double y1 = ((double)set1[i].y - c1y) * w[i];
				double z1 = ((double)set1[i].z - c1z) * w[i];
				double x2 = ((double)set2[i].x - c2x) * w[i];
				double y2 = ((double)set2[i].y - c2y) * w[i];
				double z2 = ((double)set2[i].z - c2z) * w[i];
				r1 += x1 * x1 + y1 * y1 + z1 * z1;
				r2 += x2 * x2 + y2 * y2 + z2 * z2;
			}
			s = (double)Math.sqrt(r2 / r1);
		}

		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for (int i = 0; i < set1.length; i++) {
			double x1 = ((double)set1[i].x  - c1x) * w[i] * s;
			double y1 = ((double)set1[i].y - c1y) * w[i] * s;
			double z1 = ((double)set1[i].z - c1z) * w[i] * s;
			double x2 = ((double)set2[i].x - c2x) * w[i];
			double y2 = ((double)set2[i].y - c2y) * w[i];
			double z2 = ((double)set2[i].z - c2z) * w[i];
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}
		final double[][] N = new double[4][4];
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		final JacobiDouble jacobi = new JacobiDouble(N);
		final double[][] eigenvectors = jacobi.getEigenVectors();
		final double[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final double[] q = eigenvectors[index];
		final double q0 = q[0], qx = q[1], qy = q[2], qz = q[3];


		// turn into matrix
		final Matrix4d result = new Matrix4d();
		// rotational part
		result.m00 = s * (q0 * q0 + qx * qx - qy * qy - qz * qz);
		result.m01 = s * 2 * (qx * qy - q0 * qz);
		result.m02 = s * 2 * (qx * qz + q0 * qy);
		result.m10 = s * 2 * (qy * qx + q0 * qz);
		result.m11 = s * (q0 * q0 - qx * qx + qy * qy - qz * qz);
		result.m12 = s * 2 * (qy * qz - q0 * qx);
		result.m20 = s * 2 * (qz * qx - q0 * qy);
		result.m21 = s * 2 * (qz * qy + q0 * qx);
		result.m22 = s * (q0 * q0 - qx * qx - qy * qy + qz * qz);
		result.m30 = result.m31 = result.m32 = 0;
		result.m03 = result.m13 = result.m23 = 0;
		result.m33 = 1;
		
		// translational part
		final Transform3D t = new Transform3D(result);
		javax.vecmath.Point3d translation = new javax.vecmath.Point3d(c1x, c1y, c1z); 
		t.transform(translation);
		
		result.m03 = c2x - translation.x;
		result.m13 = c2y - translation.y;
		result.m23 = c2z - translation.z;
		return result;
	}

	public static Matrix4f bestRigid(final Tuple3f[] set1, final Tuple3f[] set2, final float[] w, final boolean allowScaling) 
	{
		if (set1.length != set2.length)
			throw new RuntimeException("different lengths");

		double c1x, c1y, c1z, c2x, c2y, c2z;
		c1x = c1y = c1z = c2x = c2y = c2z = 0;
		
		double sumW = 0;

		for (int i = 0; i < set1.length; i++) {
			c1x += (double)set1[i].x * w[i];
			c1y += (double)set1[i].y * w[i];
			c1z += (double)set1[i].z * w[i];
			c2x += (double)set2[i].x * w[i];
			c2y += (double)set2[i].y * w[i];
			c2z += (double)set2[i].z * w[i];
			sumW += w[i];
		}
		c1x /= sumW;
		c1y /= sumW;
		c1z /= sumW;
		c2x /= sumW;
		c2y /= sumW;
		c2z /= sumW;

		double s = 1;
		if (allowScaling) {
			double r1, r2;
			r1 = r2 = 0;
			for (int i = 0; i < set1.length; i++) {
				double x1 = ((double)set1[i].x - c1x) * w[i];
				double y1 = ((double)set1[i].y - c1y) * w[i];
				double z1 = ((double)set1[i].z - c1z) * w[i];
				double x2 = ((double)set2[i].x - c2x) * w[i];
				double y2 = ((double)set2[i].y - c2y) * w[i];
				double z2 = ((double)set2[i].z - c2z) * w[i];
				r1 += x1 * x1 + y1 * y1 + z1 * z1;
				r2 += x2 * x2 + y2 * y2 + z2 * z2;
			}
			s = (double)Math.sqrt(r2 / r1);
		}

		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for (int i = 0; i < set1.length; i++) {
			double x1 = ((double)set1[i].x  - c1x) * w[i] * s;
			double y1 = ((double)set1[i].y - c1y) * w[i] * s;
			double z1 = ((double)set1[i].z - c1z) * w[i] * s;
			double x2 = ((double)set2[i].x - c2x) * w[i];
			double y2 = ((double)set2[i].y - c2y) * w[i];
			double z2 = ((double)set2[i].z - c2z) * w[i];
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}
		final double[][] N = new double[4][4];
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		final JacobiDouble jacobi = new JacobiDouble(N);
		final double[][] eigenvectors = jacobi.getEigenVectors();
		final double[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final double[] q = eigenvectors[index];
		final double q0 = q[0], qx = q[1], qy = q[2], qz = q[3];


		// turn into matrix
		final Matrix4f result = new Matrix4f();
		// rotational part
		result.m00 = (float)(s * (q0 * q0 + qx * qx - qy * qy - qz * qz));
		result.m01 = (float)(s * 2 * (qx * qy - q0 * qz));
		result.m02 = (float)(s * 2 * (qx * qz + q0 * qy));
		result.m10 = (float)(s * 2 * (qy * qx + q0 * qz));
		result.m11 = (float)(s * (q0 * q0 - qx * qx + qy * qy - qz * qz));
		result.m12 = (float)(s * 2 * (qy * qz - q0 * qx));
		result.m20 = (float)(s * 2 * (qz * qx - q0 * qy));
		result.m21 = (float)(s * 2 * (qz * qy + q0 * qx));
		result.m22 = (float)(s * (q0 * q0 - qx * qx - qy * qy + qz * qz));
		result.m30 = result.m31 = result.m32 = 0;
		result.m03 = result.m13 = result.m23 = 0;
		result.m33 = 1;
		
		// translational part
		final Transform3D t = new Transform3D(result);
		javax.vecmath.Point3f translation = new javax.vecmath.Point3f((float)c1x, (float)c1y, (float)c1z); 
		t.transform(translation);
		
		result.m03 = (float)(c2x - translation.x);
		result.m13 = (float)(c2y - translation.y);
		result.m23 = (float)(c2z - translation.z);
		return result;
	}

	public static final Matrix3f bestRigidNoTranslation(final Tuple3f[] set1, final Tuple3f[] set2) {
		return bestRigidNoTranslation(set1, set2, true);
	}

	public static final Matrix3f bestRigidNoTranslation(final Tuple3f[] set1, final Tuple3f[] set2, final boolean allowScaling) 
	{
		if (set1.length != set2.length)
			throw new RuntimeException("different lengths");

		final float c1x, c1y, c1z, c2x, c2y, c2z;
		c1x = c1y = c1z = c2x = c2y = c2z = 0;

		/*for (int i = 0; i < set1.length; i++) {
			c1x += set1[i].x;
			c1y += set1[i].y;
			c1z += set1[i].z;
			c2x += set2[i].x;
			c2y += set2[i].y;
			c2z += set2[i].z;
		}
		c1x /= set1.length;
		c1y /= set1.length;
		c1z /= set1.length;
		c2x /= set1.length;
		c2y /= set1.length;
		c2z /= set1.length;*/

		final float s;
		if (allowScaling) 
		{
			float r1, r2;
			r1 = r2 = 0;
			for (int i = 0; i < set1.length; i++) 
			{
				float x1 = set1[i].x - c1x;
				float y1 = set1[i].y - c1y;
				float z1 = set1[i].z - c1z;
				float x2 = set2[i].x - c2x;
				float y2 = set2[i].y - c2y;
				float z2 = set2[i].z - c2z;
				r1 += x1 * x1 + y1 * y1 + z1 * z1;
				r2 += x2 * x2 + y2 * y2 + z2 * z2;
			}
			s = (float)Math.sqrt(r2 / r1);
		}
		else
		{
			s = 1;
		}

		// calculate N
		float Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for (int i = 0; i < set1.length; i++) {
			float x1 = (set1[i].x - c1x) * s;
			float y1 = (set1[i].y - c1y) * s;
			float z1 = (set1[i].z - c1z) * s;
			float x2 = set2[i].x - c2x;
			float y2 = set2[i].y - c2y;
			float z2 = set2[i].z - c2z;
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}
		
		final float[][] N = new float[4][4];
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		final JacobiFloat jacobi = new JacobiFloat(N);
		final float[][] eigenvectors = jacobi.getEigenVectors();
		final float[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final float[] q = eigenvectors[index];
		final float q0 = q[0], qx = q[1], qy = q[2], qz = q[3];


		// turn into matrix
		Matrix3f result = new Matrix3f();
		// rotational part
		result.m00 = s * (q0 * q0 + qx * qx - qy * qy - qz * qz);
		result.m01 = s * 2 * (qx * qy - q0 * qz);
		result.m02 = s * 2 * (qx * qz + q0 * qy);
		result.m10 = s * 2 * (qy * qx + q0 * qz);
		result.m11 = s * (q0 * q0 - qx * qx + qy * qy - qz * qz);
		result.m12 = s * 2 * (qy * qz - q0 * qx);
		result.m20 = s * 2 * (qz * qx - q0 * qy);
		result.m21 = s * 2 * (qz * qy + q0 * qx);
		result.m22 = s * (q0 * q0 - qx * qx - qy * qy + qz * qz);
		/*
		// translational part
		result.apply(c1x, c1y, c1z);
		result.a03 = c2x - result.x;
		result.a13 = c2y - result.y;
		result.a23 = c2z - result.z;
		*/
		return result;
	}

	public static final void bestRigidNoTranslationNoScaling(final Tuple3f[] set1, final Tuple3f[] set2, final Matrix3f result, final float[][] N) 
	{
		if (set1.length != set2.length)
			throw new RuntimeException("different lengths");

		// calculate N
		float Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for (int i = 0; i < set1.length; i++) {
			float x1 = set1[i].x;
			float y1 = set1[i].y;
			float z1 = set1[i].z;
			float x2 = set2[i].x;
			float y2 = set2[i].y;
			float z2 = set2[i].z;
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}
		
		//final double[][] N = new double[4][4];
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		final JacobiFloat jacobi = new JacobiFloat(N);
		final float[][] eigenvectors = jacobi.getEigenVectors();
		final float[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final float[] q = eigenvectors[index];
		final float q0 = q[0], qx = q[1], qy = q[2], qz = q[3];


		// turn into matrix
		//Matrix3d result = new Matrix3d();
		
		// rotational part
		result.m00 = (q0 * q0 + qx * qx - qy * qy - qz * qz);
		result.m01 = 2 * (qx * qy - q0 * qz);
		result.m02 = 2 * (qx * qz + q0 * qy);
		result.m10 = 2 * (qy * qx + q0 * qz);
		result.m11 = (q0 * q0 - qx * qx + qy * qy - qz * qz);
		result.m12 = 2 * (qy * qz - q0 * qx);
		result.m20 = 2 * (qz * qx - q0 * qy);
		result.m21 = 2 * (qz * qy + q0 * qx);
		result.m22 = (q0 * q0 - qx * qx - qy * qy + qz * qz);
		/*
		// translational part
		result.apply(c1x, c1y, c1z);
		result.a03 = c2x - result.x;
		result.a13 = c2y - result.y;
		result.a23 = c2z - result.z;
		*/
		
		//return result;
	}
	
	public static FastMatrix average(FastMatrix[] array) {
		FastMatrix result = new FastMatrix();
		int n = 0;
		for (int i = 0; i < array.length; i++)
			if (array[i] != null) {
				n++;
				result.a00 += array[i].a00;
				result.a01 += array[i].a01;
				result.a02 += array[i].a02;
				result.a03 += array[i].a03;
				result.a10 += array[i].a10;
				result.a11 += array[i].a11;
				result.a12 += array[i].a12;
				result.a13 += array[i].a13;
				result.a20 += array[i].a20;
				result.a21 += array[i].a21;
				result.a22 += array[i].a22;
				result.a23 += array[i].a23;
			}
		if (n > 0) {
			result.a00 /= (double)n;
			result.a01 /= (double)n;
			result.a02 /= (double)n;
			result.a03 /= (double)n;
			result.a10 /= (double)n;
			result.a11 /= (double)n;
			result.a12 /= (double)n;
			result.a13 /= (double)n;
			result.a20 /= (double)n;
			result.a21 /= (double)n;
			result.a22 /= (double)n;
			result.a23 /= (double)n;
		}
		return result;
	}

	public double[] rowwise16() {
		return new double[] {
			a00, a01, a02, a03,
			a10, a11, a12, a13,
			a20, a21, a22, a23,
			0, 0, 0, 1};
	}
	
	/*
	 * parses both uniform 4x4 matrices (column by column), and
	 * 3x4 matrices (row by row).
	 */
	public static FastMatrix parseMatrix(String m) {
		FastMatrix matrix = new FastMatrix();
		StringTokenizer tokenizer = new StringTokenizer(m);
		try {
			/*
			 * Amira notates a uniform matrix in 4x4 notation,
			 * column by column.
			 * Common notation is to notate 3x4 notation, row by
			 * row, since the last row does not bear any
			 * information (but is always "0 0 0 1").
			 */
			boolean is4x4Columns = true;
			
			matrix.a00 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a10 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a20 = (double)Double.parseDouble(tokenizer.nextToken());
			double dummy = (double)Double.parseDouble(tokenizer.nextToken());
			if (dummy != 0.0) {
				is4x4Columns = false;
				matrix.a03 = dummy;
			}
			matrix.a01 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a11 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a21 = (double)Double.parseDouble(tokenizer.nextToken());
			dummy = (double)Double.parseDouble(tokenizer.nextToken());
			if (is4x4Columns && dummy != 0.0)
				is4x4Columns = false;
			if (!is4x4Columns)
				matrix.a13 = dummy;
			
			matrix.a02 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a12 = (double)Double.parseDouble(tokenizer.nextToken());
			matrix.a22 = (double)Double.parseDouble(tokenizer.nextToken());
			dummy = (double)Double.parseDouble(tokenizer.nextToken());
			if (is4x4Columns && dummy != 0.0)
				is4x4Columns = false;
			if (!is4x4Columns)
				matrix.a23 = dummy;
			
			if (is4x4Columns) {
				if (!tokenizer.hasMoreTokens())
					is4x4Columns = false;
			} else if (tokenizer.hasMoreTokens())
				throw new RuntimeException("Not a uniform matrix: "+m);
			
			if (is4x4Columns) {
				matrix.a03 = (double)Double.parseDouble(tokenizer.nextToken());
				matrix.a13 = (double)Double.parseDouble(tokenizer.nextToken());
				matrix.a23 = (double)Double.parseDouble(tokenizer.nextToken());
				if (Double.parseDouble(tokenizer.nextToken()) != 1.0)
					throw new RuntimeException("Not a uniform matrix: "+m);
			} else {
				// swap rotation part
				dummy = matrix.a01; matrix.a01 = matrix.a10; matrix.a10 = dummy;
				dummy = matrix.a02; matrix.a02 = matrix.a20; matrix.a20 = dummy;
				dummy = matrix.a12; matrix.a12 = matrix.a21; matrix.a21 = dummy;
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return matrix;
	}
	
	public static FastMatrix[] parseMatrices(String m) {
		Vector<FastMatrix> vector = new Vector<FastMatrix>();
		StringTokenizer tokenizer = new StringTokenizer(m, ",");
		while (tokenizer.hasMoreTokens()) {
			String matrix = tokenizer.nextToken().trim();
			if (matrix.equals(""))
				vector.add(null);
			else
				vector.add(parseMatrix(matrix));
		}
		FastMatrix[] result = new FastMatrix[vector.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (FastMatrix)vector.get(i);
		return result;
	}
	
	public static FastMatrix fromCalibration(ImagePlus image) {
		Calibration calib = image.getCalibration();
		FastMatrix result = new FastMatrix();
		result.a00 = (double)Math.abs(calib.pixelWidth);
		result.a11 = (double)Math.abs(calib.pixelHeight);
		result.a22 = (double)Math.abs(calib.pixelDepth);
		result.a03 = (double)calib.xOrigin;
		result.a13 = (double)calib.yOrigin;
		result.a23 = (double)calib.zOrigin;
		return result;
	}
	
	//
	public static FastMatrix translateToCenter(ImagePlus image) {
		Calibration calib = image.getCalibration();
		FastMatrix result = new FastMatrix();
		result.a00 = (double)1;
		result.a11 = (double)1;
		result.a22 = (double)1;
		result.a03 = (double)(calib.xOrigin + calib.pixelWidth * image.getWidth() / 2.0);
		result.a13 = (double)(calib.yOrigin + calib.pixelHeight * image.getHeight() / 2.0);
		result.a23 = (double)(calib.yOrigin + calib.pixelDepth * image.getStack().getSize() / 2.0);
		return result;
	}
	
	final public boolean isIdentity() {
		return isIdentity((double)1e-10);
	}
	
	final public boolean isIdentity(double eps) {
		return eps > (double)Math.abs(a00 - 1) &&
			eps > (double)Math.abs(a11 - 1) &&
			eps > (double)Math.abs(a22 - 1) &&
			eps > (double)Math.abs(a01) &&
			eps > (double)Math.abs(a02) &&
			eps > (double)Math.abs(a03) &&
			eps > (double)Math.abs(a10) &&
			eps > (double)Math.abs(a12) &&
			eps > (double)Math.abs(a13) &&
			eps > (double)Math.abs(a20) &&
			eps > (double)Math.abs(a21) &&
			eps > (double)Math.abs(a23);
	}
	
	public String resultToString() {
		return "" + x + " " + y + " " + z;
	}
	
	public String toStringIndented( String indent ) {
		String result = indent + a00 + ", " + a01 + ", " + a02 + ", " + a03 + "\n";
		result += indent + a10 + ", " + a11 + ", " + a12 + ", " + a13 + "\n";
		result += indent + a20 + ", " + a21 + ", " + a22 + ", " + a23 + "\n";
		return result;
	}
	
	public String toString() {
		return "" + a00 + " " + a01 + " " + a02 + " " + a03 + "   "
			+ a10 + " " + a11 + " " + a12 + " " + a13 + "   "
			+ a20 + " " + a21 + " " + a22 + " " + a23 + "   ";
	}
	
	public String toStringForAmira() {
		return "" + a00 + " " + a10 + " " + a20 + " 0 "
			+ a01 + " " + a11 + " " + a21 + " 0 "
			+ a02 + " " + a12 + " " + a22 + " 0 "
			+ a03 + " " + a13 + " " + a23 + " 1";
	}
	
	public static void main(String[] args) {
		FastMatrix ma = rotateFromTo(1, 0, 0, 0, 1, 0);
		ma.apply(0, 0, 1);
		IOFunctions.printErr("expect 0 0 1: " +
				ma.x + " " + ma.y + " " + ma.z);
		ma.apply(1, 0, 0);
		IOFunctions.printErr("expect 0 1 0: " +
				ma.x + " " + ma.y + " " + ma.z);
		ma.apply(0, 1, 0);
		IOFunctions.printErr("expect -1 0 0: " +
				ma.x + " " + ma.y + " " + ma.z);
	}
}
