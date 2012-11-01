


/*
	Blas.java copyright claim:

    This software is based on public domain LINPACK routines.
    It was translated from FORTRAN to Java by a US government employee
    on official time.  Thus this software is also in the public domain.


    The translator's mail address is:

    Steve Verrill
    USDA Forest Products Laboratory
    1 Gifford Pinchot Drive
    Madison, Wisconsin
    53705


    The translator's e-mail address is:

    steve@ws13.fpl.fs.fed.us


***********************************************************************

DISCLAIMER OF WARRANTIES:

THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
THE TRANSLATOR DOES NOT WARRANT, GUARANTEE OR MAKE ANY REPRESENTATIONS
REGARDING THE SOFTWARE OR DOCUMENTATION IN TERMS OF THEIR CORRECTNESS,
RELIABILITY, CURRENTNESS, OR OTHERWISE. THE ENTIRE RISK AS TO
THE RESULTS AND PERFORMANCE OF THE SOFTWARE IS ASSUMED BY YOU.
IN NO CASE WILL ANY PARTY INVOLVED WITH THE CREATION OR DISTRIBUTION
OF THE SOFTWARE BE LIABLE FOR ANY DAMAGE THAT MAY RESULT FROM THE USE
OF THIS SOFTWARE.

Sorry about that.

***********************************************************************


History:

Date        Translator        Changes

2/25/97     Steve Verrill     Translated
6/3/97      Steve Verrill     Java/C style indexing
3/10/98     Steve Verrill     isamax and colisamax added

*/




/**
*
*


*This class contains Java versions of a number of the LINPACK
*basic linear algebra subroutines (blas):
*


*1. isamax_j
*2. daxpy_j
*3. ddot_j
*4. dscal_j
*5. dswap_j
*6. dnrm2_j
*7. dcopy_j
*8. drotg_j
*


*It also contains utility routines that the translator found useful
*while translating the FORTRAN code to Java code.  "col" indicates that
*the routine operates on two columns of a matrix.  "colv" indicates that
*the routine operates on a column of a matrix and a vector.  The "p"
*at the end of dscalp, dnrm2p, and dcopyp indicates that these
*routines operate on a portion of a vector:
*
*


*1. colisamax_j
*2. colaxpy_j
*3. colvaxpy_j
*4. colvraxpy_j
*5. coldot_j
*6. colvdot_j
*7. colscal_j
*8. dscalp_j
*9. colswap_j
*10. colnrm2_j
*11. dnrm2p_j
*12. dcopyp_j
*13. colrot_j
*14. sign_j
*


*
*




*IMPORTANT:  The "_j" suffixes indicate that these routines use
*Java style indexing.  For example, you will see
*
*   for (i = 0; i < n; i++)
*
*rather than (FORTRAN style)
*
*   for (i = 1; i <= n; i++)
*
*To use the "_j" routines you will have to
*fill elements 0 through n - 1 of vectors rather than elements 1
through n.
*[Also, before using the isamax and colisamax methods
*make sure that they are doing what you expect them to do.]
*Versions of these programs that use FORTRAN style indexing are
*also available.  They end with the suffix "_f77".
*
*




*This class was translated by a statistician from FORTRAN versions of
*the LINPACK blas.  It is NOT an official translation.  When
*public domain Java numerical analysis routines become available
*from the people who produce LAPACK, then THE CODE PRODUCED
*BY THE NUMERICAL ANALYSTS SHOULD BE USED.
*
*




*Meanwhile, if you have suggestions for improving this
*code, please contact Steve Verrill at steve@ws13.fpl.fs.fed.us.
*
*@author Steve Verrill
*@version .5 --- June 3, 1997
*
*/

package bijnum;
import bijnum.*;

public class Blas extends Object {

/**
*
*




*This method finds the index of the element of a vector
*that has the maximum absolute value.  It is a translation
*from FORTRAN to Java of the LINPACK function ISAMAX.
*In the LINPACK listing ISAMAX is attributed to Jack Dongarra
*with a date of March 11, 1978. Before you use this
*version of isamax, make certain that it is doing what you
*expect it to do.
*
*Translated by Steve Verrill, March 10, 1998.
*
*@param  n        The number of elements to be checked
*@param  x[ ]  vector
*@param  incx     The subscript increment for x[ ]
*
*/


   public static int isamax_j (int n, double x[], int incx) {

      double xmax;
      int isamax,i,ix;

      if (n < 1) {

         isamax = 0;

      } else if (n == 1) {

         isamax = 1;

      } else if (incx == 1) {

         isamax = 1;
         xmax = Math.abs(x[0]);

         for (i = 1; i < n; i++) {

            if (Math.abs(x[i]) > xmax) {

               isamax = i+1;
               xmax = Math.abs(x[i]);

            }

         }

      } else {

         isamax = 1;
         ix = 0;
         xmax = Math.abs(x[ix]);
         ix += incx;

//  The i = 2; i <= n material is correct here
//  because we are not indexing x[] by i.

         for (i = 2; i <= n; i++) {

            if (Math.abs(x[ix]) > xmax) {

               isamax = i;
               xmax = Math.abs(x[ix]);

            }

            ix += incx;

         }

      }

      return isamax;

   }



/**
*
*




*This method finds the index of the element of a portion of a
*column of a matrix that has the maximum absolute value.
*It is a modification of the LINPACK function ISAMAX.
*In the LINPACK listing ISAMAX is attributed to Jack Dongarra
*with a date of March 11, 1978.
*
*Translated by Steve Verrill, March 10, 1998.
*
*@param  n              The number of elements to be checked
*@param  x[ ][ ]  The matrix
*@param  incx           The subscript increment for x[ ][ ]
*@param  begin          The starting row
*@param  j              The id of the column
*
*/

// NOTE THAT THERE IS NO DIFFERENCE BETWEEN
// colisamax_j AND colisamax_f77.  THIS IS NOT A MISTAKE.

   public static int colisamax_j (int n, double x[][], int incx,
                                    int begin, int j) {

      double xmax;
      int isamax,i,ix;

      if (n < 1) {

         isamax = 0;

      } else if (n == 1) {

         isamax = 1;

      } else if (incx == 1) {

         isamax = 1;
         ix = begin;
         xmax = Math.abs(x[ix][j]);
         ix++;

         for (i = 2; i <= n; i++) {

            if (Math.abs(x[ix][j]) > xmax) {

               isamax = i;
               xmax = Math.abs(x[ix][j]);

            }

            ix++;

         }

      } else {

         isamax = 1;
         ix = begin;
         xmax = Math.abs(x[ix][j]);
         ix += incx;

         for (i = 2; i <= n; i++) {

            if (Math.abs(x[ix][j]) > xmax) {

               isamax = i;
               xmax = Math.abs(x[ix][j]);

            }

            ix += incx;

         }

      }

      return isamax;

   }



/**
*
*




*This method multiplies a constant times a vector and adds the product
*to another vector --- dy[ ] = da*dx[ ] + dy[ ].
*It uses unrolled loops for increments equal to
*one.  It is a translation from FORTRAN to Java of the LINPACK subroutine
*DAXPY.  In the LINPACK listing DAXPY is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, June 3, 1997.
*
*@param  n         The order of the vectors dy[ ] and dx[ ]
*@param  da        The constant
*@param  dx[ ]  This vector will be multiplied by the constant da
*@param  incx      The subscript increment for dx[ ]
*@param  dy[ ]  This vector will be added to da*dx[ ]
*@param  incy      The subscript increment for dy[ ]
*
*/

   public static void daxpy_j (int n, double da, double dx[], int incx, double
                      dy[], int incy) {

      int i,ix,iy,m;

      if (n <= 0) return;
      if (da == 0.0) return;

      if ((incx == 1) && (incy == 1)) {

//   both increments equal to 1

         m = n%4;

         for (i = 0; i < m; i++) {

            dy[i] += da*dx[i];

         }

         for (i = m; i < n; i += 4) {

            dy[i]   += da*dx[i];
            dy[i+1] += da*dx[i+1];
            dy[i+2] += da*dx[i+2];
            dy[i+3] += da*dx[i+3];

         }

         return;

      } else {

//   at least one increment not equal to 1

         ix = 0;
         iy = 0;

         if (incx < 0) ix = (-n+1)*incx;
         if (incy < 0) iy = (-n+1)*incy;

         for (i = 0; i < n; i++) {

            dy[iy] += da*dx[ix];

            ix += incx;
            iy += incy;

         }

         return;

      }

   }



/**
*
*




*This method calculates the dot product of two vectors.
*It uses unrolled loops for increments equal to
*one.  It is a translation from FORTRAN to Java of the LINPACK function
*DDOT.  In the LINPACK listing DDOT is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, June 3, 1997.
*
*@param  n         The order of the vectors dx[ ] and dy[ ]
*@param  dx[ ]  vector
*@param  incx      The subscript increment for dx[ ]
*@param  dy[ ]  vector
*@param  incy      The subscript increment for dy[ ]
*
*/

   public static double ddot_j (int n, double dx[], int incx, double
                      dy[], int incy) {

      double ddot;
      int i,ix,iy,m;

      ddot = 0.0;

      if (n <= 0) return ddot;

      if ((incx == 1) && (incy == 1)) {

//   both increments equal to 1

         m = n%5;

         for (i = 0; i < m; i++) {

            ddot += dx[i]*dy[i];

         }

         for (i = m; i < n; i += 5) {

            ddot += dx[i]*dy[i] + dx[i+1]*dy[i+1] + dx[i+2]*dy[i+2] +
                    dx[i+3]*dy[i+3] + dx[i+4]*dy[i+4];

         }

         return ddot;

      } else {

//   at least one increment not equal to 1

         ix = 0;
         iy = 0;

         if (incx < 0) ix = (-n+1)*incx;
         if (incy < 0) iy = (-n+1)*incy;

         for (i = 0; i < n; i++) {

            ddot += dx[ix]*dy[iy];

            ix += incx;
            iy += incy;

         }

         return ddot;

      }

   }


/**
*
*




*This method scales a vector by a constant.
*It uses unrolled loops for an increment equal to
*one.  It is a translation from FORTRAN to Java of the LINPACK subroutine
*DSCAL.  In the LINPACK listing DSCAL is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, June 3, 1997.
*
*@param  n         The order of the vector dx[ ]
*@param  da        The constant
*@param  dx[ ]  This vector will be multiplied by the constant da
*@param  incx      The subscript increment for dx[ ]
*
*/

   public static void dscal_j (int n, double da, double dx[], int incx) {

      int i,m,nincx;

      if (n <= 0 || incx <= 0) return;

      if (incx == 1) {

//   increment equal to 1

         m = n%5;

         for (i = 0; i < m; i++) {

            dx[i] *= da;

         }

         for (i = m; i < n; i += 5) {

            dx[i]   *= da;
            dx[i+1] *= da;
            dx[i+2] *= da;
            dx[i+3] *= da;
            dx[i+4] *= da;

         }

         return;

      } else {

//   increment not equal to 1

         nincx = n*incx;

         for (i = 0; i < nincx; i += incx) {

            dx[i] *= da;

         }

         return;

      }

   }


/**
*
*




*This method interchanges two vectors.
*It uses unrolled loops for increments equal to
*one.  It is a translation from FORTRAN to Java of the LINPACK function
*DSWAP.  In the LINPACK listing DSWAP is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, June 3, 1997.
*
*@param  n         The order of the vectors dx[ ] and dy[ ]
*@param  dx[ ]  vector
*@param  incx      The subscript increment for dx[ ]
*@param  dy[ ]  vector
*@param  incy      The subscript increment for dy[ ]
*
*/

   public static void dswap_j (int n, double dx[], int incx, double
                      dy[], int incy) {

      double dtemp;
      int i,ix,iy,m;

      if (n <= 0) return;

      if ((incx == 1) && (incy == 1)) {

//   both increments equal to 1

         m = n%3;

         for (i = 0; i < m; i++) {

            dtemp = dx[i];
            dx[i] = dy[i];
            dy[i] = dtemp;

         }

         for (i = m; i < n; i += 3) {

            dtemp = dx[i];
            dx[i] = dy[i];
            dy[i] = dtemp;

            dtemp = dx[i+1];
            dx[i+1] = dy[i+1];
            dy[i+1] = dtemp;

            dtemp = dx[i+2];
            dx[i+2] = dy[i+2];
            dy[i+2] = dtemp;

         }

         return;

      } else {

//   at least one increment not equal to 1

         ix = 0;
         iy = 0;

         if (incx < 0) ix = (-n+1)*incx;
         if (incy < 0) iy = (-n+1)*incy;

         for (i = 0; i < n; i++) {

            dtemp = dx[ix];
            dx[ix] = dy[iy];
            dy[iy] = dtemp;

            ix += incx;
            iy += incy;

         }

         return;

      }

   }


/**
*
*




*This method calculates the Euclidean norm of the vector
*stored in dx[ ] with storage increment incx.
*It is a translation from FORTRAN to Java of the LINPACK function
*DNRM2.
*In the LINPACK listing DNRM2 is attributed to C.L. Lawson
*with a date of January 8, 1978.  The routine below is based
*on a more recent DNRM2 version that is attributed in LAPACK
*documentation to Sven Hammarling.
*
*Translated by Steve Verrill, June 3, 1997.
*
*@param  n        The order of the vector x[ ]
*@param  x[ ]  vector
*@param  incx     The subscript increment for x[ ]
*
*/


   public static double dnrm2_j (int n, double x[], int incx) {

      double absxi,norm,scale,ssq,fac;
      int ix,limit;

      if (n < 1 || incx < 1) {

         norm = 0.0;

      } else if (n == 1) {

         norm = Math.abs(x[0]);

      } else {

         scale = 0.0;
         ssq = 1.0;

         limit = (n - 1)*incx;

         for (ix = 0; ix <= limit; ix += incx) {

            if (x[ix] != 0.0) {

               absxi = Math.abs(x[ix]);

               if (scale < absxi) {

                  fac = scale/absxi;
                  ssq = 1.0 + ssq*fac*fac;
                  scale = absxi;

               } else {

                  fac = absxi/scale;
                  ssq += fac*fac;

               }

            }

         }

         norm = scale*Math.sqrt(ssq);

      }

      return norm;

   }


/**
*
*




*This method copies the vector dx[ ] to the vector dy[ ].
*It uses unrolled loops for increments equal to
*one.  It is a translation from FORTRAN to Java of the LINPACK subroutine
*DCOPY.  In the LINPACK listing DCOPY is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, March 1, 1997.
*
*@param  n         The order of dx[ ] and dy[ ]
*@param  dx[ ]  vector
*@param  incx      The subscript increment for dx[ ]
*@param  dy[ ]  vector
*@param  incy      The subscript increment for dy[ ]
*
*/

   public static void dcopy_j (int n, double dx[], int incx, double
                      dy[], int incy) {

      double dtemp;
      int i,ix,iy,m;

      if (n <= 0) return;

      if ((incx == 1) && (incy == 1)) {

//   both increments equal to 1

         m = n%7;

         for (i = 0; i < m; i++) {

            dy[i] = dx[i];

         }

         for (i = m; i < n; i += 7) {

            dy[i]   = dx[i];
            dy[i+1] = dx[i+1];
            dy[i+2] = dx[i+2];
            dy[i+3] = dx[i+3];
            dy[i+4] = dx[i+4];
            dy[i+5] = dx[i+5];
            dy[i+6] = dx[i+6];

         }

         return;

      } else {

//   at least one increment not equal to 1

         ix = 0;
         iy = 0;

         if (incx < 0) ix = (-n+1)*incx;
         if (incy < 0) iy = (-n+1)*incy;

         for (i = 0; i < n; i++) {

            dy[iy] = dx[ix];

            ix += incx;
            iy += incy;

         }

         return;

      }

   }



/**
*
*




*This method constructs a Givens plane rotation.
*It is a translation from FORTRAN to Java of the LINPACK subroutine
*DROTG.  In the LINPACK listing DROTG is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, March 3, 1997.
*
*@param  rotvec[]   Contains the a,b,c,s values.  In Java they
*                   cannot be passed as primitive types (e.g., double
*                   or int or ...) if we want their return values to
*                   be altered.
*
*/

   public static void drotg_j (double rotvec[]) {

//   construct a Givens plane rotation

      double a,b,c,s,roe,scale,r,z,ra,rb;

      a = rotvec[0];
      b = rotvec[1];

      roe = b;

      if (Math.abs(a) > Math.abs(b)) roe = a;

      scale = Math.abs(a) + Math.abs(b);

      if (scale != 0.0) {

         ra = a/scale;
         rb = b/scale;
         r = scale*Math.sqrt(ra*ra + rb*rb);
         r = sign_j(1.0,roe)*r;
         c = a/r;
         s = b/r;
         z = 1.0;
         if (Math.abs(a) > Math.abs(b)) z = s;
         if ((Math.abs(b) >= Math.abs(a)) && (c != 0.0)) z = 1.0/c;

      } else {

         c = 1.0;
         s = 0.0;
         r = 0.0;
         z = 0.0;

      }

      a = r;
      b = z;

      rotvec[0] = a;
      rotvec[1] = b;
      rotvec[2] = c;
      rotvec[3] = s;

      return;

   }



/**
*
*




*This method multiplies a constant times a portion of a column
*of a matrix and adds the product to the corresponding portion
*of another column of the matrix --- a portion of col2 is
replaced by the corresponding portion of a*col1 + col2.
*It uses unrolled loops.
*It is a modification of the LINPACK subroutine
*DAXPY.  In the LINPACK listing DAXPY is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, February 26, 1997.
*
*@param  nrow           The number of rows involved
*@param  a              The constant
*@param  x[ ][ ]  The matrix
*@param  begin          The starting row
*@param  j1             The id of col1
*@param  j2             The id of col2
*
*/


   public static void colaxpy_j (int nrow, double a, double x[][], int begin,
                        int j1, int j2) {

      int i,m,mpbegin,end;

      if (nrow <= 0) return;
      if (a == 0.0) return;

      m = nrow%4;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         x[i][j2] += a*x[i][j1];

      }

      for (i = mpbegin; i <= end; i += 4) {

         x[i][j2]   += a*x[i][j1];
         x[i+1][j2] += a*x[i+1][j1];
         x[i+2][j2] += a*x[i+2][j1];
         x[i+3][j2] += a*x[i+3][j1];

      }

      return;

   }


/**
*
*




*This method multiplies a constant times a portion of a column
*of a matrix x[ ][ ] and adds the product to the corresponding portion
*of a vector y[ ] --- a portion of y[ ] is replaced by the corresponding
*portion of ax[ ][j] + y[ ].
*It uses unrolled loops.
*It is a modification of the LINPACK subroutine
*DAXPY.  In the LINPACK listing DAXPY is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, March 1, 1997.
*
*@param  nrow           The number of rows involved
*@param  a              The constant
*@param  x[ ][ ]  The matrix
*@param  y[ ]        The vector
*@param  begin          The starting row
*@param  j              The id of the column of the x matrix
*
*/

   public static void colvaxpy_j (int nrow, double a, double x[][], double y[],
                        int begin, int j) {

      int i,m,mpbegin,end;

      if (nrow <= 0) return;
      if (a == 0.0) return;

      m = nrow%4;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         y[i] += a*x[i][j];

      }

      for (i = mpbegin; i <= end; i += 4) {

         y[i]   += a*x[i][j];
         y[i+1] += a*x[i+1][j];
         y[i+2] += a*x[i+2][j];
         y[i+3] += a*x[i+3][j];

      }

      return;

   }


/**
*
*




*This method multiplies a constant times a portion of a vector y[ ]
*and adds the product to the corresponding portion
*of a column of a matrix x[ ][ ] --- a portion of column j of x[ ][ ]
*is replaced by the corresponding
*portion of ay[ ] + x[ ][j].
*It uses unrolled loops.
*It is a modification of the LINPACK subroutine
*DAXPY.  In the LINPACK listing DAXPY is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, March 3, 1997.
*
*@param  nrow           The number of rows involved
*@param  a              The constant
*@param  y[ ]        The vector
*@param  x[ ][ ]  The matrix
*@param  begin          The starting row
*@param  j              The id of the column of the x matrix
*
*/

   public static void colvraxpy_j (int nrow, double a, double y[], double x[][],
                        int begin, int j) {

      int i,m,mpbegin,end;

      if (nrow <= 0) return;
      if (a == 0.0) return;

      m = nrow%4;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         x[i][j] += a*y[i];

      }

      for (i = mpbegin; i <= end; i += 4) {

         x[i][j]   += a*y[i];
         x[i+1][j] += a*y[i+1];
         x[i+2][j] += a*y[i+2];
         x[i+3][j] += a*y[i+3];

      }

      return;

   }


/**
*
*




*This method calculates the dot product of portions of two
*columns of a matrix.  It uses unrolled loops.
*It is a modification of the LINPACK function
*DDOT.  In the LINPACK listing DDOT is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, February 27, 1997.
*
*@param  nrow           The number of rows involved
*@param  x[ ][ ]  The matrix
*@param  begin          The starting row
*@param  j1             The id of the first column
*@param  j2             The id of the second column
*
*/

   public static double coldot_j (int nrow, double x[][], int begin,
                         int j1, int j2) {

      double coldot;
      int i,m,mpbegin,end;

      coldot = 0.0;

      if (nrow <= 0) return coldot;

      m = nrow%5;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         coldot += x[i][j1]*x[i][j2];

      }

      for (i = mpbegin; i <= end; i += 5) {

         coldot += x[i][j1]*x[i][j2] +
                   x[i+1][j1]*x[i+1][j2] +
                   x[i+2][j1]*x[i+2][j2] +
                   x[i+3][j1]*x[i+3][j2] +
                   x[i+4][j1]*x[i+4][j2];

      }

      return coldot;

   }


/**
*
*




*This method calculates the dot product of a portion of a
*column of a matrix and the corresponding portion of a
*vector.  It uses unrolled loops.
*It is a modification of the LINPACK function
*DDOT.  In the LINPACK listing DDOT is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, March 1, 1997.
*
*@param  nrow           The number of rows involved
*@param  x[ ][ ]  The matrix
*@param  y[ ]        The vector
*@param  begin          The starting row
*@param  j              The id of the column of the matrix
*
*/

   public static double colvdot_j (int nrow, double x[][], double y[],
                         int begin, int j) {

      double colvdot;
      int i,m,mpbegin,end;

      colvdot = 0.0;

      if (nrow <= 0) return colvdot;

      m = nrow%5;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         colvdot += x[i][j]*y[i];

      }

      for (i = mpbegin; i <= end; i += 5) {

         colvdot += x[i][j]*y[i] +
                   x[i+1][j]*y[i+1] +
                   x[i+2][j]*y[i+2] +
                   x[i+3][j]*y[i+3] +
                   x[i+4][j]*y[i+4];

      }

      return colvdot;

   }


/**
*
*




*This method scales a portion of a column of a matrix by a constant.
*It uses unrolled loops.
*It is a modification of the LINPACK subroutine
*DSCAL.  In the LINPACK listing DSCAL is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, February 27, 1997.
*
*@param  nrow           The number of rows involved
*@param  a              The constant
*@param  x[ ][ ]  The matrix
*@param  begin          The starting row
*@param  j              The id of the column
*
*/

   public static void colscal_j (int nrow, double a, double x[][], int begin, int j) {

      int i,m,mpbegin,end;

      if (nrow <= 0) return;

      m = nrow%5;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         x[i][j] *= a;

      }

      for (i = mpbegin; i <= end; i += 5) {

         x[i][j]   *= a;
         x[i+1][j] *= a;
         x[i+2][j] *= a;
         x[i+3][j] *= a;
         x[i+4][j] *= a;

      }

      return;

   }


/**
*
*




*This method scales a portion of a vector by a constant.
*It uses unrolled loops.
*It is a modification of the LINPACK subroutine
*DSCAL.  In the LINPACK listing DSCAL is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, March 3, 1997.
*
*@param  nrow           The number of rows involved
*@param  a              The constant
*@param  x[ ]        The vector
*@param  begin          The starting row
*
*/

   public static void dscalp_j (int nrow, double a, double x[], int begin) {

      int i,m,mpbegin,end;

      if (nrow <= 0) return;

      m = nrow%5;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

         x[i] *= a;

      }

      for (i = mpbegin; i <= end; i += 5) {

         x[i]   *= a;
         x[i+1] *= a;
         x[i+2] *= a;
         x[i+3] *= a;
         x[i+4] *= a;

      }

      return;

   }



/**
*
*




*This method interchanges two columns of a matrix.
*It uses unrolled loops.
*It is a modification of the LINPACK function
*DSWAP.  In the LINPACK listing DSWAP is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, February 26, 1997.
*
*@param  n              The number of rows of the matrix
*@param  x[ ][ ]  The matrix
*@param  j1             The id of the first column
*@param  j2             The id of the second column
*
*/

   public static void colswap_j (int n, double x[][], int j1, int j2) {

      double temp;
      int i,m;

      if (n <= 0) return;

      m = n%3;

      for (i = 0; i < m; i++) {

         temp = x[i][j1];
         x[i][j1] = x[i][j2];
         x[i][j2] = temp;

      }

      for (i = m; i < n; i += 3) {

         temp = x[i][j1];
         x[i][j1] = x[i][j2];
         x[i][j2] = temp;

         temp = x[i+1][j1];
         x[i+1][j1] = x[i+1][j2];
         x[i+1][j2] = temp;

         temp = x[i+2][j1];
         x[i+2][j1] = x[i+2][j2];
         x[i+2][j2] = temp;

      }

      return;

   }



/**
*
*




*This method calculates the Euclidean norm of a portion of a
*column of a matrix.
*It is a modification of the LINPACK function
*dnrm2.
*In the LINPACK listing dnrm2 is attributed to C.L. Lawson
*with a date of January 8, 1978.  The routine below is based
*on a more recent dnrm2 version that is attributed in LAPACK
*documentation to Sven Hammarling.
*
*Translated and modified by Steve Verrill, February 26, 1997.
*
*@param  nrow           The number of rows involved
*@param  x[ ][ ]  The matrix
*@param  begin          The starting row
*@param  j              The id of the column
*
*/


   public static double colnrm2_j (int nrow, double x[][], int begin, int j) {

      double absxij,norm,scale,ssq,fac;
      int i,end;

      if (nrow < 1) {

         norm = 0.0;

      } else if (nrow == 1) {

         norm = Math.abs(x[begin][j]);

      } else {

         scale = 0.0;
         ssq = 1.0;

         end = begin + nrow - 1;

         for (i = begin; i <= end; i++) {

            if (x[i][j] != 0.0) {

               absxij = Math.abs(x[i][j]);

               if (scale < absxij) {

                  fac = scale/absxij;
                  ssq = 1.0 + ssq*fac*fac;
                  scale = absxij;

               } else {

                  fac = absxij/scale;
                  ssq += fac*fac;

               }

            }

         }

         norm = scale*Math.sqrt(ssq);

      }

      return norm;

   }


/**
*
*




*This method calculates the Euclidean norm of a portion
*of a vector x[ ].
*It is a modification of the LINPACK function
*dnrm2.
*In the LINPACK listing dnrm2 is attributed to C.L. Lawson
*with a date of January 8, 1978.  The routine below is based
*on a more recent dnrm2 version that is attributed in LAPACK
*documentation to Sven Hammarling.
*
*Translated by Steve Verrill, March 3, 1997.
*
*@param  nrow     The number of rows involved
*@param  x[ ]  vector
*@param  begin    The starting row
*
*/


   public static double dnrm2p_j (int nrow, double x[], int begin) {

      double absxi,norm,scale,ssq,fac;
      int i,end;

      if (nrow < 1) {

         norm = 0.0;

      } else if (nrow == 1) {

         norm = Math.abs(x[begin]);

      } else {

         scale = 0.0;
         ssq = 1.0;

         end = begin + nrow - 1;

         for (i = begin; i <= end; i++) {

            if (x[i] != 0.0) {

               absxi = Math.abs(x[i]);

               if (scale < absxi) {

                  fac = scale/absxi;
                  ssq = 1.0 + ssq*fac*fac;
                  scale = absxi;

               } else {

                  fac = absxi/scale;
                  ssq += fac*fac;

               }

            }

         }

         norm = scale*Math.sqrt(ssq);

      }

      return norm;

   }



/**
*
*




*This method copies a portion of vector x[ ] to the corresponding
portion of vector y[ ].
*It uses unrolled loops.
*It is a modification of the LINPACK subroutine
*dcopy.  In the LINPACK listing dcopy is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated by Steve Verrill, March 1, 1997.
*
*@param  nrow     The number of rows involved
*@param  x[ ]  vector
*@param  y[ ]  vector
*@param  begin    The starting row
*
*/

   public static void dcopyp_j (int nrow, double x[], double y[], int begin) {

      double temp;
      int i,m,mpbegin,end;

      m = nrow%7;
      mpbegin = m + begin;
      end = begin + nrow - 1;

      for (i = begin; i < mpbegin; i++) {

            y[i] = x[i];

         }

      for (i = mpbegin; i <= end; i += 7) {

         y[i]   = x[i];
         y[i+1] = x[i+1];
         y[i+2] = x[i+2];
         y[i+3] = x[i+3];
         y[i+4] = x[i+4];
         y[i+5] = x[i+5];
         y[i+6] = x[i+6];

      }

       return;

   }


/**
*
*




*This method "applies a plane rotation."
*It is a modification of the LINPACK function
*DROT.  In the LINPACK listing DROT is attributed to Jack Dongarra
*with a date of 3/11/78.
*
*Translated and modified by Steve Verrill, March 4, 1997.
*
*@param  n              The order of x[ ][ ]
*@param  x[ ][ ]  The matrix
*@param  j1             The id of the first column
*@param  j2             The id of the second column
*@param  c              "cos"
*@param  s              "sin"
*
*/

   public static void colrot_j (int n, double x[][],
                       int j1, int j2, double c, double s) {

      double temp;
      int i;


      if (n <= 0) return;

      for (i = 0; i < n; i++) {

         temp = c*x[i][j1] + s*x[i][j2];
         x[i][j2] = c*x[i][j2] - s*x[i][j1];
         x[i][j1] = temp;

      }

      return;

   }



/**
*
*




*This method implements the FORTRAN sign (not sin) function.
*See the code for details.
*
*Created by Steve Verrill, March 1997.
*
*@param  a   a
*@param  b   b
*
*/

   public static double sign_j (double a, double b) {

      if (b < 0.0) {

         return -Math.abs(a);

      } else {

         return Math.abs(a);

      }

   }



/**
*
*




*This method multiplies an n x p matrix by a p x r matrix.
*
*Created by Steve Verrill, March 1997.
*
*@param  a[ ][ ]   The left matrix
*@param  b[ ][ ]   The right matrix
*@param  c[ ][ ]   The product
*@param  n   n
*@param  p   p
*@param  r   r
*
*/

   public static void matmat_j (double a[][], double b[][], double c[][],
                                int n, int p, int r) {

      double vdot;
      int i,j,k,m;

      for (i = 0; i < n; i++) {

         for (j = 0; j < r; j++) {

            vdot = 0.0;

            m = p%5;

            for (k = 0; k < m; k++) {

               vdot += a[i][k]*b[k][j];

            }

            for (k = m; k < p; k += 5) {

               vdot += a[i][k]*b[k][j] +
                       a[i][k+1]*b[k+1][j] +
                       a[i][k+2]*b[k+2][j] +
                       a[i][k+3]*b[k+3][j] +
                       a[i][k+4]*b[k+4][j];

            }

            c[i][j] = vdot;

         }

      }

   }



/**
*
*




*This method obtains the transpose of an n x p matrix.
*
*Created by Steve Verrill, March 1997.
*
*@param  a[ ][ ]    matrix
*@param  at[ ][ ]   transpose of the matrix
*@param  n                n
*@param  p                p
*
*/

   public static void mattran_j (double a[][], double at[][],
                                int n, int p) {

      int i,j;

      for (i = 0; i < n; i++) {

         for (j = 0; j < p; j++) {

            at[j][i] = a[i][j];

         }

      }

   }



/**
*
*




*This method multiplies an n x p matrix by a p x 1 vector.
*
*Created by Steve Verrill, March 1997.
*
*@param  a[ ][ ]   The matrix
*@param  b[ ]         The vector
*@param  c[ ]         The product
*@param  n               n
*@param  p               p
*
*/

   public static void matvec_j (double a[][], double b[], double c[],
                                int n, int p) {

      double vdot;
      int i,j,m;

      for (i = 0; i < n; i++) {

         vdot = 0.0;

         m = p%5;

         for (j = 0; j < m; j++) {

            vdot += a[i][j]*b[j];

         }

         for (j = m; j < p; j += 5) {

               vdot += a[i][j]*b[j] +
                       a[i][j+1]*b[j+1] +
                       a[i][j+2]*b[j+2] +
                       a[i][j+3]*b[j+3] +
                       a[i][j+4]*b[j+4];

         }

         c[i] = vdot;

      }

   }




}





