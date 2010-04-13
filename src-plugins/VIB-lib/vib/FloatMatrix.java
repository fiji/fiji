/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.ImagePlus;
import ij.measure.Calibration;
import java.util.StringTokenizer;
import java.util.Vector;
import math3d.Point3d;
import math3d.Triangle;
import math3d.JacobiFloat;
import math3d.FloatMatrixN;

public class FloatMatrix {

	public float x, y, z;

	protected float a00, a01, a02, a03,
		a10, a11, a12, a13,
		a20, a21, a22, a23;
	
	public FloatMatrix() { }
	
	public FloatMatrix(float f) { a00 = a11 = a22 = f; }
	
	public FloatMatrix(float[][] m) {
		if ((m.length != 3 && m.length != 4)
		    || m[0].length != 4)
			throw new RuntimeException("Wrong dimensions: "
						   + m.length + "x"
						   + m[0].length);
		
		a00 = (float)m[0][0];
		a01 = (float)m[0][1];
		a02 = (float)m[0][2];
		a03 = (float)m[0][3];
		a10 = (float)m[1][0];
		a11 = (float)m[1][1];
		a12 = (float)m[1][2];
		a13 = (float)m[1][3];
		a20 = (float)m[2][0];
		a21 = (float)m[2][1];
		a22 = (float)m[2][2];
		a23 = (float)m[2][3];
	}
	
	public FloatMatrix(FloatMatrix f) {
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

	public FloatMatrix copyFrom(FloatMatrix f) {
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

	public boolean isJustTranslation() {
		
		FloatMatrix toTest = new FloatMatrix(this);
		toTest.a03 -= a03;
		toTest.a13 -= a13;
		toTest.a23 -= a23;
		
		return toTest.isIdentity();
		
	}
	
	public boolean noTranslation() {
		
		float eps = (float)1e-10;
		
		return ((float)Math.abs(a03) < eps)
			&& ((float)Math.abs(a13) < eps)
			&& ((float)Math.abs(a23) < eps);
		
	}
	
	public FloatMatrix composeWith(FloatMatrix followedBy) {
		
		// Alias this and followedBy to A and B, with entries a_ij...
		
		FloatMatrix A = this;
		FloatMatrix B = followedBy;
		
		FloatMatrix result = new FloatMatrix();
		
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
	   FloatMatrix in the returned array) and the translation (the
	   second FloatMatrix).  (So, applying these two in the order
	   returned in the array should be the same as applying the
	   original.)
	   
	*/
	
	public FloatMatrix[] decompose() {
		
		FloatMatrix[] result = new FloatMatrix[2];
		
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
	
	public FloatMatrix plus(FloatMatrix other) {
		
		FloatMatrix result = new FloatMatrix();
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
    public FloatMatrix(Jama.Matrix m) {
        if ((m.getRowDimension() != 3 && m.getRowDimension() != 4)
                || m.getColumnDimension() != 4)
            throw new RuntimeException("Wrong dimensions: "
                    + m.getRowDimension() + "x"
                    + m.getColumnDimension());

        a00 = (float)m.get(0,0);
        a01 = (float)m.get(0,1);
        a02 = (float)m.get(0,2);
        a03 = (float)m.get(0,3);
        a10 = (float)m.get(1,0);
        a11 = (float)m.get(1,1);
        a12 = (float)m.get(1,2);
        a13 = (float)m.get(1,3);
        a20 = (float)m.get(2,0);
        a21 = (float)m.get(2,1);
        a22 = (float)m.get(2,2);
        a23 = (float)m.get(2,3);
    }
    */
	
	public void apply(float x, float y, float z) {
		this.x = x * a00 + y * a01 + z * a02 + a03;
		this.y = x * a10 + y * a11 + z * a12 + a13;
		this.z = x * a20 + y * a21 + z * a22 + a23;
	}
	
	public void apply(Point3d p) {
		this.x = (float)(p.x * a00 + p.y * a01 + p.z * a02 + a03);
		this.y = (float)(p.x * a10 + p.y * a11 + p.z * a12 + a13);
		this.z = (float)(p.x * a20 + p.y * a21 + p.z * a22 + a23);
	}
	
	public void apply(float[] p) {
		this.x = (float)(p[0] * a00 + p[1] * a01 + p[2] * a02 + a03);
		this.y = (float)(p[0] * a10 + p[1] * a11 + p[2] * a12 + a13);
		this.z = (float)(p[0] * a20 + p[1] * a21 + p[2] * a22 + a23);
	}
	
	public void applyWithoutTranslation(float x, float y, float z) {
		this.x = x * a00 + y * a01 + z * a02;
		this.y = x * a10 + y * a11 + z * a12;
		this.z = x * a20 + y * a21 + z * a22;
	}
	
	public void applyWithoutTranslation(Point3d p) {
		this.x = (float)(p.x * a00 + p.y * a01 + p.z * a02);
		this.y = (float)(p.x * a10 + p.y * a11 + p.z * a12);
		this.z = (float)(p.x * a20 + p.y * a21 + p.z * a22);
	}
	
	public Point3d getResult() {
		return new Point3d(x, y, z);
	}
	
	public FloatMatrix scale(float x, float y, float z) {
		FloatMatrix result = new FloatMatrix();
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
	
	public FloatMatrix times(FloatMatrix o) {
		FloatMatrix result = new FloatMatrix();
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
	
	public float det( ) {
		float sub00 = a11 * a22 - a12 * a21;
		float sub01 = a10 * a22 - a12 * a20;
		float sub02 = a10 * a21 - a11 * a20;
		float sub10 = a01 * a22 - a02 * a21;
		float sub11 = a00 * a22 - a02 * a20;
		float sub12 = a00 * a21 - a01 * a20;
		float sub20 = a01 * a12 - a02 * a11;
		float sub21 = a00 * a12 - a02 * a10;
		float sub22 = a00 * a11 - a01 * a10;
		return a00 * sub00 - a01 * sub01 + a02 * sub02;
	}
	
	/* this inverts just the first 3 columns, interpreted as 3x3 matrix */
	private FloatMatrix invert3x3() {
		float sub00 = a11 * a22 - a12 * a21;
		float sub01 = a10 * a22 - a12 * a20;
		float sub02 = a10 * a21 - a11 * a20;
		float sub10 = a01 * a22 - a02 * a21;
		float sub11 = a00 * a22 - a02 * a20;
		float sub12 = a00 * a21 - a01 * a20;
		float sub20 = a01 * a12 - a02 * a11;
		float sub21 = a00 * a12 - a02 * a10;
		float sub22 = a00 * a11 - a01 * a10;
		float det = a00 * sub00 - a01 * sub01 + a02 * sub02;
		
		FloatMatrix result = new FloatMatrix();
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
	
	public FloatMatrix inverse() {
		FloatMatrix result = invert3x3();
		result.apply(-a03, -a13, -a23);
		result.a03 = result.x;
		result.a13 = result.y;
		result.a23 = result.z;
		return result;
	}
	
	public static FloatMatrix rotate(float angle, int axis) {
		FloatMatrix result = new FloatMatrix();
		float c = (float)Math.cos(angle);
		float s = (float)Math.sin(angle);
		switch(axis) {
		case 0:
			result.a11 = result.a22 = c;
			result.a12 = -(result.a21 = s);
			result.a00 = (float)1.0f;
			break;
		case 1:
			result.a00 = result.a22 = c;
			result.a02 = -(result.a20 = s);
			result.a11 = (float)1.0f;
			break;
		case 2:
			result.a00 = result.a11 = c;
			result.a01 = -(result.a10 = s);
			result.a22 = (float)1.0f;
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
	public static FloatMatrix rotateFromTo(float aX, float aY, float aZ,
			float bX, float bY, float bZ) {
		float l = (float)Math.sqrt(aX * aX + aY * aY + aZ * aZ);
		aX /= l; aY /= l; aZ /= l;
		l = (float)Math.sqrt(bX * bX + bY * bY + bZ * bZ);
		bX /= l; bY /= l; bZ /= l;
		float cX, cY, cZ;
		cX = aY * bZ - aZ * bY;
		cY = aZ * bX - aX * bZ;
		cZ = aX * bY - aY * bX;
		float pX, pY, pZ;
		pX = cY * aZ - cZ * aY;
		pY = cZ * aX - cX * aZ;
		pZ = cX * aY - cY * aX;
		float qX, qY, qZ;
		qX = cY * bZ - cZ * bY;
		qY = cZ * bX - cX * bZ;
		qZ = cX * bY - cY * bX;
		FloatMatrix result = new FloatMatrix();
		result.a00 = aX; result.a01 = aY; result.a02 = aZ;
		result.a10 = cX; result.a11 = cY; result.a12 = cZ;
		result.a20 = pX; result.a21 = pY; result.a22 = pZ;
		FloatMatrix transp = new FloatMatrix();
		transp.a00 = bX; transp.a01 = cX; transp.a02 = qX;
		transp.a10 = bY; transp.a11 = cY; transp.a12 = qY;
		transp.a20 = bZ; transp.a21 = cZ; transp.a22 = qZ;
		return transp.times(result);
	}

	// ------------------------------------------------------------------------
	// FIXME: This probably isn't the best place for these static functions...
	
	public static float dotProduct(float[] a, float[] b) {
		float result = 0;
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
	
	public static float sizeSquared(float[] a) {
		return dotProduct(a,a);
	}
	
	public static float size(float[] a) {
		return (float)Math.sqrt(dotProduct(a,a));
	}
	
	public static float angleBetween(float[] v1, float[] v2) {
		return (float)Math.acos(dotProduct(v1,v2)/(size(v1)*size(v2)));
	}
	
	public static float[] crossProduct(float[] a, float[] b) {
		
		float[] result = { a[1] * b[2] - a[2] * b[1],
				    a[2] * b[0] - a[0] * b[2],
				    a[0] * b[1] - a[1] * b[0] };
		
		return result;
		
	}
	
	public static float[] normalize( float[] a ) {
		float magnitude = size(a);
		float[] result = new float[a.length];
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
	
	public static FloatMatrix rotateToAlignVectors(float[] v2_template,
						      float[] v1_template,
						      float[] v2_domain,
						      float[] v1_domain ) {
		
		float angleBetween = angleBetween(v2_domain,
						   v2_template);
		
		float[] normal = crossProduct(v2_domain,
					       v2_template);
		
		float[] normalUnit = normalize(normal);
		
		FloatMatrix rotation = FloatMatrix.rotateAround(normalUnit[0],
							      normalUnit[1],
							      normalUnit[2],
							      angleBetween);
		
                /*
		  If v2 is the vector with the largest eigenvalue and v1 is
		  that with the second largest, then the projection of v1
		  onto the plane defined by v2 as a normal is:

 		  v1 - ( (v1.v2) / |v2||v2| ) v2

		*/

		float scale_v2_domain =  dotProduct(v1_domain,v2_domain)
			/ sizeSquared(v2_domain);

		float[] v1_orthogonal_domain = new float[3];
		
		v1_orthogonal_domain[0] = v1_domain[0] - scale_v2_domain * v2_domain[0];
		v1_orthogonal_domain[1] = v1_domain[1] - scale_v2_domain * v2_domain[1];
		v1_orthogonal_domain[2] = v1_domain[2] - scale_v2_domain * v2_domain[2];
		
		// Now for the template as well:

		float scale_v2_template = dotProduct(v1_template,v2_template)
			/ sizeSquared(v2_template);
		
		float [] v1_orthogonal_template = new float[3];
		
		v1_orthogonal_template[0] = v1_template[0] - scale_v2_template * v2_template[0];
		v1_orthogonal_template[1] = v1_template[1] - scale_v2_template * v2_template[1];
		v1_orthogonal_template[2] = v1_template[2] - scale_v2_template * v2_template[2];
		
		// Now we should rotate the one in the domain by the same
		// rotation as we applied to the most significant eigenvector...
		
		rotation.apply(v1_orthogonal_domain[0],
			       v1_orthogonal_domain[1],
			       v1_orthogonal_domain[2]);
		
		float[] v1_orthogonal_domain_rotated = new float[3];
		v1_orthogonal_domain_rotated[0] = rotation.x;
		v1_orthogonal_domain_rotated[1] = rotation.y;
		v1_orthogonal_domain_rotated[2] = rotation.z;
		
		// Now we need to find the rotation around v2 in the template
		// that will line up the projected v1s...
		
		float angleBetweenV1sA = angleBetween(v1_orthogonal_domain_rotated,
						       v1_orthogonal_template );
		
		float[] normalToV1sA = crossProduct(v1_orthogonal_domain_rotated,
						      v1_orthogonal_template );

		
		float[] normalToV1sAUnit = normalize(normalToV1sA);
		
		FloatMatrix secondRotationA = FloatMatrix.rotateAround(normalToV1sAUnit[0],
								     normalToV1sAUnit[1],
								     normalToV1sAUnit[2],
								     angleBetweenV1sA);

		return rotation.composeWith(secondRotationA);

	}
	
	public static FloatMatrix rotateAround(float nx, float ny, float nz,
					      float angle) {
		FloatMatrix r = new FloatMatrix();
		float c = (float)Math.cos(angle), s = (float)Math.sin(angle);
		
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
	public static FloatMatrix rotateEuler(float a1, float a2, float a3) {
		FloatMatrix r = new FloatMatrix();
		float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
		float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);
		float c3 = (float)Math.cos(a3), s3 = (float)Math.sin(a3);
		
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
	public static FloatMatrix rotateEulerAt(float a1, float a2, float a3,
					       float cx, float cy, float cz) {
		FloatMatrix r = new FloatMatrix();
		float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
		float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);
		float c3 = (float)Math.cos(a3), s3 = (float)Math.sin(a3);
		
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
	public void guessEulerParameters(float[] parameters) {
		if (parameters.length != 6)
			throw new IllegalArgumentException(
				"Need 6 parameters, got "
				+ parameters.length);
		guessEulerParameters(parameters, null);
	}
	
	public void guessEulerParameters(float[] parameters, Point3d center) {
		if (center != null && parameters.length != 9)
			throw new IllegalArgumentException(
				"Need 9 parameters, got "
				+ parameters.length);
		
		if (a21 == 0.0f && a20 == 0.0f) {
			/*
			 * s2 == 0, therefore a2 == 0, therefore a1 and a3
			 * are not determined (they are both rotations around
			 * the z axis. Choose a3 = 0.
			 */
			parameters[2] = 0;
			parameters[1] = 0;
			parameters[0] = (float)Math.atan2(a10, a00);
		} else {
			parameters[2] = (float)Math.atan2(a20, a21);
			parameters[1] = (float)Math.atan2(
				Math.sqrt(a21 * a21 + a20 * a20), a22);
			parameters[0] = (float)Math.atan2(a02, -a12);
		}
		
		/*
		 * If a center of rotation was given, the parameters will
		 * contain:
		 * (angleZ, angleX, angleZ2, transX, transY, transZ,
		 *  centerX, centerY, centerZ) where trans is the translation
		 *  _after_ the rotation around center.
		 */
		if (center != null) {
			parameters[6] = (float)center.x;
			parameters[7] = (float)center.y;
			parameters[8] = (float)center.z;
			apply(center);
			parameters[3] = x - (float)center.x;
			parameters[4] = y - (float)center.y;
			parameters[5] = z - (float)center.z;
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
		if (a03 == 0.0f && a13 == 0.0f && a23 == 0.0f) {
			parameters[3] = parameters[4] = parameters[5] = 0;
		} else {
			apply(a03, a13, a23);
			Triangle t = new Triangle(
				new Point3d(0, 0, 0),
				new Point3d(a03, a13, a23),
				new Point3d(x, y, z));
			t.calculateCircumcenter2();
			parameters[3] = (float)t.center.x;
			parameters[4] = (float)t.center.y;
			parameters[5] = (float)t.center.z;
		}
	}
	
	public static FloatMatrix translate(float x, float y, float z) {
		FloatMatrix result = new FloatMatrix();
		result.a00 = result.a11 = result.a22 = (float)1.0f;
		result.a03 = x;
		result.a13 = y;
		result.a23 = z;
		return result;
	}
	
	/*
	 * least squares fitting of a linear transformation which maps
	 * the points x[i] to y[i] as best as possible.
	 */
	public static FloatMatrix bestLinear(Point3d[] x, Point3d[] y) {
		if (x.length != y.length)
			throw new RuntimeException("different lengths");
		if (x.length != 4 )
			throw new RuntimeException("The arrays passed to bestLinear must be of length 4");
		
		float[][] a = new float[4][4];
		float[][] b = new float[4][4];
		
		for (int i = 0; i < a.length; i++) {
			a[0][0] += (float)(x[i].x * x[i].x);
			a[0][1] += (float)(x[i].x * x[i].y);
			a[0][2] += (float)(x[i].x * x[i].z);
			a[0][3] += (float)(x[i].x);
			a[1][1] += (float)(x[i].y * x[i].y);
			a[1][2] += (float)(x[i].y * x[i].z);
			a[1][3] += (float)(x[i].y);
			a[2][2] += (float)(x[i].z * x[i].z);
			a[2][3] += (float)(x[i].z);
			
			b[0][0] += (float)(x[i].x * y[i].x);
			b[0][1] += (float)(x[i].y * y[i].x);
			b[0][2] += (float)(x[i].z * y[i].x);
			b[0][3] += (float)(y[i].x);
			b[1][0] += (float)(x[i].x * y[i].y);
			b[1][1] += (float)(x[i].y * y[i].y);
			b[1][2] += (float)(x[i].z * y[i].y);
			b[1][3] += (float)(y[i].y);
			b[2][0] += (float)(x[i].x * y[i].z);
			b[2][1] += (float)(x[i].y * y[i].z);
			b[2][2] += (float)(x[i].z * y[i].z);
			b[2][3] += (float)(y[i].z);
		}
		
		a[1][0] = a[0][1];
		a[2][0] = a[0][2];
		a[2][1] = a[1][2];
		a[3][0] = a[0][3];
		a[3][1] = a[1][3];
		a[3][2] = a[2][3];
		a[3][3] = 1;
		FloatMatrixN.invert(a);
		float[][] r = FloatMatrixN.times(b, a);
		
		FloatMatrix result = new FloatMatrix();
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
	public static FloatMatrix bestRigid(Point3d[] set1, Point3d[] set2) {
		return bestRigid(set1, set2, true);
	}

	public static FloatMatrix bestRigid(Point3d[] set1, Point3d[] set2,
			boolean allowScaling) {
		if (set1.length != set2.length)
			throw new RuntimeException("different lengths");

		float c1x, c1y, c1z, c2x, c2y, c2z;
		c1x = c1y = c1z = c2x = c2y = c2z = 0;

		for (int i = 0; i < set1.length; i++) {
			c1x += (float)set1[i].x;
			c1y += (float)set1[i].y;
			c1z += (float)set1[i].z;
			c2x += (float)set2[i].x;
			c2y += (float)set2[i].y;
			c2z += (float)set2[i].z;
		}
		c1x /= set1.length;
		c1y /= set1.length;
		c1z /= set1.length;
		c2x /= set1.length;
		c2y /= set1.length;
		c2z /= set1.length;

		float s = 1;
		if (allowScaling) {
			float r1, r2;
			r1 = r2 = 0;
			for (int i = 0; i < set1.length; i++) {
				float x1 = (float)set1[i].x - c1x;
				float y1 = (float)set1[i].y - c1y;
				float z1 = (float)set1[i].z - c1z;
				float x2 = (float)set2[i].x - c2x;
				float y2 = (float)set2[i].y - c2y;
				float z2 = (float)set2[i].z - c2z;
				r1 += x1 * x1 + y1 * y1 + z1 * z1;
				r2 += x2 * x2 + y2 * y2 + z2 * z2;
			}
			s = (float)Math.sqrt(r2 / r1);
		}

		// calculate N
		float Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for (int i = 0; i < set1.length; i++) {
			float x1 = ((float)set1[i].x - c1x) * s;
			float y1 = ((float)set1[i].y - c1y) * s;
			float z1 = ((float)set1[i].z - c1z) * s;
			float x2 = (float)set2[i].x - c2x;
			float y2 = (float)set2[i].y - c2y;
			float z2 = (float)set2[i].z - c2z;
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
		float[][] N = new float[4][4];
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
		JacobiFloat jacobi = new JacobiFloat(N);
		float[][] eigenvectors = jacobi.getEigenVectors();
		float[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		float[] q = eigenvectors[index];
		float q0 = q[0], qx = q[1], qy = q[2], qz = q[3];


		// turn into matrix
		FloatMatrix result = new FloatMatrix();
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
	
	public static FloatMatrix average(FloatMatrix[] array) {
		FloatMatrix result = new FloatMatrix();
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
			result.a00 /= (float)n;
			result.a01 /= (float)n;
			result.a02 /= (float)n;
			result.a03 /= (float)n;
			result.a10 /= (float)n;
			result.a11 /= (float)n;
			result.a12 /= (float)n;
			result.a13 /= (float)n;
			result.a20 /= (float)n;
			result.a21 /= (float)n;
			result.a22 /= (float)n;
			result.a23 /= (float)n;
		}
		return result;
	}

	public float[] rowwise16() {
		return new float[] {
			a00, a01, a02, a03,
			a10, a11, a12, a13,
			a20, a21, a22, a23,
			0, 0, 0, 1};
	}
	
	/*
	 * parses both uniform 4x4 matrices (column by column), and
	 * 3x4 matrices (row by row).
	 */
	public static FloatMatrix parseMatrix(String m) {
		FloatMatrix matrix = new FloatMatrix();
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
			
			matrix.a00 = (float)Float.parseFloat(tokenizer.nextToken());
			matrix.a10 = (float)Float.parseFloat(tokenizer.nextToken());
			matrix.a20 = (float)Float.parseFloat(tokenizer.nextToken());
			float dummy = (float)Float.parseFloat(tokenizer.nextToken());
			if (dummy != 0.0f) {
				is4x4Columns = false;
				matrix.a03 = dummy;
			}
			matrix.a01 = (float)Float.parseFloat(tokenizer.nextToken());
			matrix.a11 = (float)Float.parseFloat(tokenizer.nextToken());
			matrix.a21 = (float)Float.parseFloat(tokenizer.nextToken());
			dummy = (float)Float.parseFloat(tokenizer.nextToken());
			if (is4x4Columns && dummy != 0.0f)
				is4x4Columns = false;
			if (!is4x4Columns)
				matrix.a13 = dummy;
			
			matrix.a02 = (float)Float.parseFloat(tokenizer.nextToken());
			matrix.a12 = (float)Float.parseFloat(tokenizer.nextToken());
			matrix.a22 = (float)Float.parseFloat(tokenizer.nextToken());
			dummy = (float)Float.parseFloat(tokenizer.nextToken());
			if (is4x4Columns && dummy != 0.0f)
				is4x4Columns = false;
			if (!is4x4Columns)
				matrix.a23 = dummy;
			
			if (is4x4Columns) {
				if (!tokenizer.hasMoreTokens())
					is4x4Columns = false;
			} else if (tokenizer.hasMoreTokens())
				throw new RuntimeException("Not a uniform matrix: "+m);
			
			if (is4x4Columns) {
				matrix.a03 = (float)Float.parseFloat(tokenizer.nextToken());
				matrix.a13 = (float)Float.parseFloat(tokenizer.nextToken());
				matrix.a23 = (float)Float.parseFloat(tokenizer.nextToken());
				if (Float.parseFloat(tokenizer.nextToken()) != 1.0f)
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
	
	public static FloatMatrix[] parseMatrices(String m) {
		Vector vector = new Vector();
		StringTokenizer tokenizer = new StringTokenizer(m, ",");
		while (tokenizer.hasMoreTokens()) {
			String matrix = tokenizer.nextToken().trim();
			if (matrix.equals(""))
				vector.add(null);
			else
				vector.add(parseMatrix(matrix));
		}
		FloatMatrix[] result = new FloatMatrix[vector.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (FloatMatrix)vector.get(i);
		return result;
	}
	
	public static FloatMatrix fromCalibration(ImagePlus image) {
		Calibration calib = image.getCalibration();
		FloatMatrix result = new FloatMatrix();
		result.a00 = (float)Math.abs(calib.pixelWidth);
		result.a11 = (float)Math.abs(calib.pixelHeight);
		result.a22 = (float)Math.abs(calib.pixelDepth);
		result.a03 = (float)calib.xOrigin;
		result.a13 = (float)calib.yOrigin;
		result.a23 = (float)calib.zOrigin;
		return result;
	}
	
	//
	public static FloatMatrix translateToCenter(ImagePlus image) {
		Calibration calib = image.getCalibration();
		FloatMatrix result = new FloatMatrix();
		result.a00 = (float)1;
		result.a11 = (float)1;
		result.a22 = (float)1;
		result.a03 = (float)(calib.xOrigin + calib.pixelWidth * image.getWidth() / 2.0f);
		result.a13 = (float)(calib.yOrigin + calib.pixelHeight * image.getHeight() / 2.0f);
		result.a23 = (float)(calib.yOrigin + calib.pixelDepth * image.getStack().getSize() / 2.0f);
		return result;
	}
	
	final public boolean isIdentity() {
		return isIdentity((float)1e-10);
	}

	final public boolean equals( FloatMatrix other ) {
		float eps = (float)1e-10;
		return eps > (float)Math.abs( a00 - other.a00 ) &&
			eps > (float)Math.abs( a01 - other.a01 ) &&
			eps > (float)Math.abs( a02 - other.a02 ) &&
			eps > (float)Math.abs( a03 - other.a03 ) &&
			eps > (float)Math.abs( a10 - other.a10 ) &&
			eps > (float)Math.abs( a11 - other.a11 ) &&
			eps > (float)Math.abs( a12 - other.a12 ) &&
			eps > (float)Math.abs( a13 - other.a13 ) &&
			eps > (float)Math.abs( a20 - other.a20 ) &&
			eps > (float)Math.abs( a21 - other.a21 ) &&
			eps > (float)Math.abs( a22 - other.a22 ) &&
			eps > (float)Math.abs( a23 - other.a23 );

	}
	
	final public boolean isIdentity(float eps) {
		return eps > (float)Math.abs(a00 - 1) &&
			eps > (float)Math.abs(a11 - 1) &&
			eps > (float)Math.abs(a22 - 1) &&
			eps > (float)Math.abs(a01) &&
			eps > (float)Math.abs(a02) &&
			eps > (float)Math.abs(a03) &&
			eps > (float)Math.abs(a10) &&
			eps > (float)Math.abs(a12) &&
			eps > (float)Math.abs(a13) &&
			eps > (float)Math.abs(a20) &&
			eps > (float)Math.abs(a21) &&
			eps > (float)Math.abs(a23);
	}

	public void copyToFlatFloatArray( float [] result ) {
		result[0] = a00;
		result[1] = a01;
		result[2] = a02;
		result[3] = a03;
		result[4] = a10;
		result[5] = a11;
		result[6] = a12;
		result[7] = a13;
		result[8] = a20;
		result[9] = a21;
		result[10] = a22;
		result[11] = a23;
	}

	public void setFromFlatFloatArray( float [] result ) {
		a00 = result[0];
		a01 = result[1];
		a02 = result[2];
		a03 = result[3];
		a10 = result[4];
		a11 = result[5];
		a12 = result[6];
		a13 = result[7];
		a20 = result[8];
		a21 = result[9];
		a22 = result[10];
		a23 = result[11];
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
		FloatMatrix ma = rotateFromTo(1, 0, 0, 0, 1, 0);
		ma.apply(0, 0, 1);
		System.err.println("expect 0 0 1: " +
				ma.x + " " + ma.y + " " + ma.z);
		ma.apply(1, 0, 0);
		System.err.println("expect 0 1 0: " +
				ma.x + " " + ma.y + " " + ma.z);
		ma.apply(0, 1, 0);
		System.err.println("expect -1 0 0: " +
				ma.x + " " + ma.y + " " + ma.z);
	}
}
