/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.transforms;

import ij.ImagePlus;
import ij.measure.Calibration;
import java.util.StringTokenizer;
import java.util.Vector;
import math3d.Point3d;
import vib.FastMatrix;
import math3d.Triangle;
import math3d.JacobiDouble;

public class FastMatrixTransform extends FastMatrix implements Transform {

	public FastMatrixTransform() { }
	
	public FastMatrixTransform(double f) {
		super(f);
	}
	
	public FastMatrixTransform(double[][] m) {
		super(m);
	}
	
	public FastMatrixTransform(FastMatrix f) {
		super(f);
	}

	/* FIXME: duplicated from FastMatrix */
	private FastMatrixTransform invert3x3() {
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
		
		FastMatrixTransform result = new FastMatrixTransform();
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

	public FastMatrixTransform inverse() {
		FastMatrixTransform result = invert3x3();
		result.apply(-a03, -a13, -a23);
		result.a03 = result.x;
		result.a13 = result.y;
		result.a23 = result.z;
		return result;
	}
	
	/* <double-only> */

        public int getTransformType() {
                return Transform.FASTMATRIX;
        }

        public Transform composeWith(Transform followedBy) {
                switch(followedBy.getTransformType()) {
                case Transform.BOOKSTEIN:
                        return null;
                case Transform.FASTMATRIX:
                {
                        FastMatrix f=(FastMatrix)followedBy;
                        return composeWithFastMatrix(f);
                }
                default:
                        return null;
                }

        }

	/* </double-only> */

        public FastMatrixTransform composeWithFastMatrix(FastMatrix followedBy) {

                // Alias this and followedBy to A and B, with entries a_ij...

                FastMatrixTransform A = this;
                FastMatrixTransform B = new FastMatrixTransform(followedBy);

                FastMatrixTransform result = new FastMatrixTransform();

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

	/* <double-only> */

        public FastMatrixTransform[] decomposeFully() {

                // We return a shearing matrix, a scaling matrix, a
                // rotation matrix and a translation matrix, in that
                // order.

                // If the full transformation is A, then:
                //
                // A.d = t
                // A.d = T.(R.(S.(H.d))) = t
                //
                // ... where:  result[0] is H (shearing)
                //             result[1] is S (scaling)
                //             result[2] is R (rotation)
                //             result[3] is T (translation)

                FastMatrixTransform[] result=new FastMatrixTransform[4];

                FastMatrixTransform copy=new FastMatrixTransform(this);

                // Remember we apply by post-multiplying with the
                // domain vector...

                // Take off the translation:

                result[3]=new FastMatrixTransform(1.0);
                result[3].a03=a03;
                result[3].a13=a13;
                result[3].a23=a23;

                // System.out.println("The translation only part is:\n"+result[3].toStringIndented("    ") );

                // Actually we can do this by just setting the translation to zero:
                copy.a03=0;
                copy.a13=0;
                copy.a23=0;

                // Now 'copy' is R.S.H, so find the rotation.  We'll
                // use rotateToAlign vectors here, and assume that i =
                // (1,0,0) is not sheared by H or S and j is only
                // moved in a plane defined by i and j.

                Point3d i=new Point3d(1,0,0);
                Point3d j=new Point3d(0,1,0);
                Point3d k=new Point3d(0,0,1);

                copy.apply(i);
                Point3d i_prime=copy.getResult();
                copy.apply(j);
                Point3d j_prime=copy.getResult();
                copy.apply(k);
                Point3d k_prime=copy.getResult();

                result[2] = new FastMatrixTransform( rotateToAlignVectors( i_prime.toArray(),
									   j_prime.toArray(),
									   i.toArray(),
									   j.toArray() ) );

                // System.out.println("The rotation only part is:\n"+result[2].toStringIndented("    ") );

                FastMatrixTransform rotation_inverse=result[2].inverse();

                // Reduce copy to S.H by premultiplying with
                // rotation_inverse...

                copy=copy.composeWithFastMatrix(rotation_inverse);

                // Make the scaling factor a scaling that gets i to
                // i_prime's length.

                result[1] = new FastMatrixTransform((double)i_prime.length());

                FastMatrixTransform scaling_inverse=result[1].inverse();

                // System.out.println("The scaling only part is:\n"+result[1].toStringIndented("    ") );

                // Reduce copy to H by premultiplying with
                // scaling_inverse.

                copy=copy.composeWithFastMatrix(scaling_inverse);

                result[0] = copy;

                // System.out.println("The shearing and reflection only part is:\n"+result[0].toStringIndented("    ") );

                // System.out.println("If they're all composed:");

                // System.out.println("Effect of each part of the composed transform:");

                // System.out.println("The 'everything else' transform maps:\n");
                // result[0].apply(i);
                // System.out.println("  i to: "+result[0].x+","+result[0].y+","+result[0].z);
                // result[0].apply(j);
                // System.out.println("  j to: "+result[0].x+","+result[0].y+","+result[0].z);
                // result[0].apply(k);
                // System.out.println("  k to: "+result[0].x+","+result[0].y+","+result[0].z);
                //
                // System.out.println("The scaling transform maps:\n");
                // result[1].apply(i);
                // System.out.println("  i to: "+result[1].x+","+result[1].y+","+result[1].z);
                // result[1].apply(j);
                // System.out.println("  j to: "+result[1].x+","+result[1].y+","+result[1].z);
                // result[1].apply(k);
                // System.out.println("  k to: "+result[1].x+","+result[1].y+","+result[1].z);
                //
                // System.out.println("The rotation transform maps:\n");
                // result[2].apply(i);
                // System.out.println("  i to: "+result[2].x+","+result[2].y+","+result[2].z);
                // result[2].apply(j);
                // System.out.println("  j to: "+result[2].x+","+result[2].y+","+result[2].z);
                // result[2].apply(k);
                // System.out.println("  k to: "+result[2].x+","+result[2].y+","+result[2].z);
                //
                // System.out.println("The translation transform maps:\n");
                // result[3].apply(i);
                // System.out.println("  i to: "+result[3].x+","+result[3].y+","+result[3].z);
                // result[3].apply(j);
                // System.out.println("  j to: "+result[3].x+","+result[3].y+","+result[3].z);
                // result[3].apply(k);
                // System.out.println("  k to: "+result[3].x+","+result[3].y+","+result[3].z);

                // FastMatrixTransform allComposed=result[0].composeWithFastMatrix(result[1]);
                // allComposed=allComposed.composeWithFastMatrix(result[2]);
                // allComposed=allComposed.composeWithFastMatrix(result[3]);
                //
                // System.out.println("All composed is:\n"+allComposed.toStringIndented("    "));
                // System.out.println("Original is:\n"+toStringIndented("    "));

                return result;
        }

	/* </double-only> */
	
        public void apply(double x, double y, double z, double[] result) {
                result[0] = x * a00 + y * a01 + z * a02 + a03;
                result[1] = x * a10 + y * a11 + z * a12 + a13;
                result[2] = x * a20 + y * a21 + z * a22 + a23;
        }


        public void setTranslation( double x, double y, double z ) {
                a03=x;
                a13=y;
                a23=z;
        }
	
        public static FastMatrixTransform fromCalibrationWithoutOrigin(ImagePlus image) {
		// FIXME: What idiocy prompted me to write these methods?
		throw new RuntimeException("BUG: it's very unlikely that you really want to be calling fromCalibrationWithoutOrigin");
/*
                return fromCalibrationWithoutOrigin(image.getCalibration());
*/
        }

        public static FastMatrixTransform fromCalibrationWithoutOrigin(Calibration calib) {

		// FIXME: What idiocy prompted me to write these methods?
		throw new RuntimeException("BUG: it's very unlikely that you really want to be calling fromCalibrationWithoutOrigin");
/*
                double x_spacing = 1.0;
                double y_spacing = 1.0;
                double z_spacing = 1.0;

                if( (calib.pixelWidth != 0.0) &&
                    (calib.pixelHeight != 0.0) &&
                    (calib.pixelDepth != 0.0) ) {

                        x_spacing = 1.0;
                        y_spacing = (double)calib.pixelHeight / (double)calib.pixelWidth;
                        z_spacing = (double)calib.pixelDepth / (double)calib.pixelWidth;
                }

                return new FastMatrixTransform(1.0).scale( x_spacing,
							   y_spacing,
							   z_spacing );

*/
        }

	/* FIXME: duplicating the code saves an extra objection
	   construction, but it's basically nonsense to be duplicating
	   these. */

	public FastMatrixTransform scale(double x, double y, double z) {
		FastMatrixTransform result = new FastMatrixTransform();
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
	
	public FastMatrixTransform times(FastMatrixTransform o) {
		// return new FastMatrixTransform(super.times(o));
		FastMatrixTransform result = new FastMatrixTransform();
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

	public FastMatrixTransform plus(FastMatrixTransform other) {
		
		FastMatrixTransform result = new FastMatrixTransform();
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

	public static FastMatrixTransform translate(double x, double y, double z) {
		FastMatrixTransform result = new FastMatrixTransform();
		result.a00 = result.a11 = result.a22 = (double)1.0;
		result.a03 = x;
		result.a13 = y;
		result.a23 = z;
		return result;
	}

}
