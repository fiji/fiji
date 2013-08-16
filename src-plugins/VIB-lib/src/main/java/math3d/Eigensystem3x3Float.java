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

public class Eigensystem3x3Float {

	float [][] m;

	float [] eigenVectors;
	float [] eigenValues;

	public Eigensystem3x3Float(float [][] symmetricMatrix) {
		this.m = symmetricMatrix;
		if( m[0][1] != m[1][0] || m[0][2] != m[2][0] || m[1][2] != m[2][1] ) {
			throw new RuntimeException("Eigensystem3x3Float only works with symmetric matrices");
		}
	}

	public void getEvalues(float [] eigenValues) {
		eigenValues[0] = this.eigenValues[0];
		eigenValues[1] = this.eigenValues[1];
		eigenValues[2] = this.eigenValues[2];
	}

	public float [] getEvaluesCopy() {
		return eigenValues.clone();
	}

	public float [] getEvalues() {
		return eigenValues;
	}

	public boolean findEvalues() {

		eigenValues = new float[3];

		// Find the coefficients of the characteristic polynomial:
		// http://en.wikipedia.org/wiki/Eigenvalue_algorithm

		// The matrix looks like:
		/*
			A  B  C
			B  D  E
			C  E  F
		*/

		// In the double version these identity casts should have no cost:

		double A = (double)m[0][0];
		double B = (double)m[0][1];
		double C = (double)m[0][2];
		double D = (double)m[1][1];
		double E = (double)m[1][2];
		double F = (double)m[2][2];

		double a = -1;

		double b =
			+ A
			+ D
			+ F;

		double c =
			+ B * B
			+ C * C
			+ E * E
			- A * D
			- A * F
			- D * F;

		// ADF - AEE - BBF + 2BCE - CCD

		double d =
			+ A * D * F
			- A * E * E
			- B * B * F
			+ 2 * B * C * E
			- C * C * D;

		/*
		System.out.println("a: "+a);
		System.out.println("b: "+b);
		System.out.println("c: "+c);
		System.out.println("d: "+d);
		*/

		final double third = 0.333333333333333333333333333333333333;

		// Now use the root-finding formula described here:
		// http://en.wikipedia.org/wiki/Cubic_equation#oot-finding_formula

		double q = (3*a*c - b*b) / (9*a*a);
		double r = (9*a*b*c - 27*a*a*d - 2*b*b*b) / (54*a*a*a);

		/*
		System.out.println("q is: "+q);
		System.out.println("r is: "+r);
		*/

		double discriminant = q*q*q + r*r;

		if( discriminant > 0 ) {

			/* Some of the roots are complex.  This should
			   never happen, since this is a real
			   symmetric matrix. */

			/*
			String problemMatrix =
				"[" + A + ", " + B + ", " + C +"]\n" +
				"[" + B + ", " + D + ", " + E +"]\n" +
				"[" + C + ", " + E + ", " + F +"]\n";

			throw new RuntimeException( "(BUG) Some complex roots found for matrix:\n" + problemMatrix +
				"\ndiscriminant was: "+discriminant);
			*/

			return false;

		} else if( discriminant < 0 ) {

			double rootThree = 1.7320508075688772935;

			double innerSize = Math.sqrt( r*r - discriminant );
			double innerAngle;

			if( r > 0 )
				innerAngle = Math.atan( Math.sqrt(-discriminant) / r );
			else
				innerAngle = ( Math.PI - Math.atan( Math.sqrt(-discriminant) / -r ) );

			// So now s is the cube root of innerSize * e ^ (   innerAngle * i )
			//    and t is the cube root of innerSize * e ^ ( - innerAngle * i )

			double stSize = Math.pow(innerSize,third);

			double sAngle = innerAngle / 3;
			double tAngle = - innerAngle / 3;

			double sPlusT = 2 * stSize * Math.cos(sAngle);

			eigenValues[0] = (float)( sPlusT - (b / (3*a)) );

			double firstPart = - (sPlusT / 2) - (b / 3*a);

			double lastPart = - rootThree * stSize * Math.sin(sAngle);

			eigenValues[1] = (float)( firstPart + lastPart );
			eigenValues[2] = (float)( firstPart - lastPart );

			return true;

		} else {

			// The discriminant is zero (or very small),
			// so the second two solutions are the same:

			double sPlusT;
			if( r >= 0 )
				sPlusT = 2 * Math.pow(r,third);
			else
				sPlusT = -2 * Math.pow(-r,third);

			double bOver3A = b / (3 * a);

			eigenValues[0] = (float)( sPlusT - bOver3A );
			eigenValues[1] = (float)( - sPlusT / 2 - bOver3A );
			eigenValues[2] = eigenValues[1];

			return true;
		}
	}
}
