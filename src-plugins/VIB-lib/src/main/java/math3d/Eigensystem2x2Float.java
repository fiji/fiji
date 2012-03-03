/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package math3d;

/*
     The float version of this class is automatically generated from
     the double version.  Unfortunately, simply converting every
     double to a float causes errors that accumulate to such a degree
     that the float version finds complex roots where it shouldn't, so
     we do all the arithmetic in doubles even in the float version.
     It's just for convenience of interface that we generate the float
     version, essentially.

     Thanks to Ting Zhao for suggesting calculating the eigenvalues
     directly in this way rather than using an iterative method.
 */

public class Eigensystem2x2Float {

	float [][] m;

	float [] eigenVectors;
	float [] eigenValues;

	public Eigensystem2x2Float(float [][] symmetricMatrix) {
		this.m = symmetricMatrix;
		if( m[0][1] != m[1][0] ) {
			throw new RuntimeException("Eigensystem2x2Float only works with symmetric matrices");
		}
	}

	public void getEvalues(float [] eigenValues) {
		eigenValues[0] = this.eigenValues[0];
		eigenValues[1] = this.eigenValues[1];
	}

	public float [] getEvaluesCopy() {
		return eigenValues.clone();
	}

	public float [] getEvalues() {
		return eigenValues;
	}

	public boolean findEvalues() {

		eigenValues = new float[2];

		double A = (double)m[0][0];
		double B = (double)m[0][1];
		double C = (double)m[1][1];
		
		double a = 1;
		double b = -(A + C);
		double c = A * C - B * B;

		double discriminant = b * b - 4 * a * c;
		if( discriminant < 0 ) {
			/*
			String problemMatrix =
				"[" + A + ", " + B + "]\n" +
				"[" + B + ", " + C + "]\n";

			throw new RuntimeException( "(BUG) Some complex roots found for matrix:\n" + problemMatrix +
						    "\ndiscriminant was: "+discriminant);
			*/

			return false;

		} else {
			eigenValues[0] = (float)( ( - b + Math.sqrt(discriminant) ) / (2 * a) );
			eigenValues[1] = (float)( ( - b - Math.sqrt(discriminant) ) / (2 * a) );
			
			return true;
		}
	}
}
