package bijnum;

/*
    SVDC_j.java copyright claim:

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

3/3/97      Steve Verrill     Translated
6/6/97                        Java/C style indexing

*/




/**
*
*


*This class contains the LINPACK DSVDC (singular value
*decomposition) routine.
*
*




*IMPORTANT:  The "_j" suffixes indicate that these routines use
*Java/C style indexing.  For example, you will see
*
*   for (i = 0; i < n; i++)
*
*rather than
*
*   for (i = 1; i <= n; i++)
*
*To use the "_j" routines you will have
*to fill elements 0 through n - 1 rather than elements 1 through n.
*Versions of these programs that use FORTRAN style indexing
*are also available.  They end with the suffix "_f77".
*
*




*This class was translated by a statistician from FORTRAN versions of
*the LINPACK routines.  It is NOT an official translation.  When
*public domain Java numerical analysis routines become available
*from the people who produce LAPACK, then THE CODE PRODUCED
*BY THE NUMERICAL ANALYSTS SHOULD BE USED.
*
*




*Meanwhile, if you have suggestions for improving this
*code, please contact Steve Verrill at steve@ws13.fpl.fs.fed.us.
*
*@author Steve Verrill
*@version .5 --- June 6, 1997
*
*/


public class SVDC extends Object {

/**
*
*




*This method decomposes a n by p
*matrix X into a product UDV´ where ...
*For details, see the comments in the code.
*This method is a translation from FORTRAN to Java
*of the LINPACK subroutine DSVDC.
*In the LINPACK listing DSVDC is attributed to G.W. Stewart
*with a date of 3/19/79.
*
*Translated by Steve Verrill, March 3, 1997.
*
*@param  x     The matrix to be decomposed
*@param  n     The number of rows of X
*@param  p     The number of columns of X
*@param  s     The singular values of X
*@param  e     See the documentation in the code
*@param  u     The left singular vectors
*@param  v     The right singular vectors
*@param  work  A work array
*@param  job   See the documentation in the code
*@param  info  See the documentation in the code
*
*/



   public static void dsvdc_j (double x[][], int n, int p, double s[],
                                 double e[], double u[][], double v[][],
                                 double work[], int job)
                                 throws SVDCException {


/*

Here is a copy of the LINPACK documentation (from the SLATEC version):


C***BEGIN PROLOGUE  DSVDC
C***DATE WRITTEN   790319   (YYMMDD)
C***REVISION DATE  861211   (YYMMDD)
C***CATEGORY NO.  D6
C***KEYWORDS  LIBRARY=SLATEC(LINPACK),
C             TYPE=DOUBLE PRECISION(SSVDC-S DSVDC-D CSVDC-C),
C             LINEAR ALGEBRA,MATRIX,SINGULAR VALUE DECOMPOSITION
C***AUTHOR  STEWART, G. W., (U. OF MARYLAND)
C***PURPOSE  Perform the singular value decomposition of a d.p. NXP
C            matrix.
C***DESCRIPTION
C
C     DSVDC is a subroutine to reduce a double precision NxP matrix X
C     by orthogonal transformations U and V to diagonal form.  The
C     diagonal elements S(I) are the singular values of X.  The
C     columns of U are the corresponding left singular vectors,
C     and the columns of V the right singular vectors.
C
C     On Entry
C
C         X         DOUBLE PRECISION(LDX,P), where LDX .GE. N.
C                   X contains the matrix whose singular value
C                   decomposition is to be computed.  X is
C                   destroyed by DSVDC.
C
C         LDX       INTEGER.
C                   LDX is the leading dimension of the array X.
C
C         N         INTEGER.
C                   N is the number of rows of the matrix X.
C
C         P         INTEGER.
C                   P is the number of columns of the matrix X.
C
C         LDU       INTEGER.
C                   LDU is the leading dimension of the array U.
C                   (See below).
C
C         LDV       INTEGER.
C                   LDV is the leading dimension of the array V.
C                   (See below).
C
C         WORK      DOUBLE PRECISION(N).
C                   WORK is a scratch array.
C
C         JOB       INTEGER.
C                   JOB controls the computation of the singular
C                   vectors.  It has the decimal expansion AB
C                   with the following meaning
C
C                        A .EQ. 0    do not compute the left singular
C                                  vectors.
C                        A .EQ. 1    return the N left singular vectors
C                                  in U.
C                        A .GE. 2    return the first MIN(N,P) singular
C                                  vectors in U.
C                        B .EQ. 0    do not compute the right singular
C                                  vectors.
C                        B .EQ. 1    return the right singular vectors
C                                  in V.
C
C     On Return
C
C         S         DOUBLE PRECISION(MM), where MM=MIN(N+1,P).
C                   The first MIN(N,P) entries of S contain the
C                   singular values of X arranged in descending
C                   order of magnitude.
C
C         E         DOUBLE PRECISION(P).
C                   E ordinarily contains zeros.  However see the
C                   discussion of INFO for exceptions.
C
C         U         DOUBLE PRECISION(LDU,K), where LDU .GE. N.
C                   If JOBA .EQ. 1, then K .EQ. N.
C                   If JOBA .GE. 2, then K .EQ. MIN(N,P).
C                   U contains the matrix of right singular vectors.
C                   U is not referenced if JOBA .EQ. 0.  If N .LE. P
C                   or if JOBA .EQ. 2, then U may be identified with X
C                   in the subroutine call.
C
C         V         DOUBLE PRECISION(LDV,P), where LDV .GE. P.
C                   V contains the matrix of right singular vectors.
C                   V is not referenced if JOB .EQ. 0.  If P .LE. N,
C                   then V may be identified with X in the
C                   subroutine call.
C
C         INFO      INTEGER.
C                   The singular values (and their corresponding
C                   singular vectors) S(INFO+1),S(INFO+2),...,S(M)
C                   are correct (here M=MIN(N,P)).  Thus if
C                   INFO .EQ. 0, all the singular values and their
C                   vectors are correct.  In any event, the matrix
C                   B = TRANS(U)*X*V is the bidiagonal matrix
C                   with the elements of S on its diagonal and the
C                   elements of E on its super-diagonal (TRANS(U)
C                   is the transpose of U).  Thus the singular
C                   values of X and B are the same.


   For the Java version, info
   is returned as an argument to SVDCException
   if the decomposition fails.


C
C     LINPACK.  This version dated 03/19/79 .
C     G. W. Stewart, University of Maryland, Argonne National Lab.
C
C     DSVDC uses the following functions and subprograms.
C
C     External DROT
C     BLAS DAXPY,DDOT,DSCAL,DSWAP,DNRM2,DROTG
C     Fortran DABS,DMAX1,MAX0,MIN0,MOD,DSQRT
C***REFERENCES  DONGARRA J.J., BUNCH J.R., MOLER C.B., STEWART G.W.,
C                 *LINPACK USERS  GUIDE*, SIAM, 1979.
C***ROUTINES CALLED  DAXPY,DDOT,DNRM2,DROT,DROTG,DSCAL,DSWAP
C***END PROLOGUE  DSVDC

*/

//   There is a bug in the 1.0.2 Java compiler that
//   forces me to skimp on the number of local
//   variables.  This is the cause of the following changes:

//      double t,b,c,cs,el,emm1,f,g,scale1,scale2,scale,shift,sl,sm,
//             sn,smm1,t1,test,ztest,fac;

//      int i,iter,j,jobu,k,kase,kk,l,ll,lls,lm1,lp1,ls,lu,
//          m,maxit,mm,mm1,mp1,nct,nctp1,ncu,nrt,nrtp1;

      double t,b,c,cs,f,g,scale,shift,
             sn,t1,test,ztest;

      double rotvec[] = new double[4];

      int i,iter,j,jobu,k,kase,kk,l,ll,lls,lm1,lp1,ls,lu,
          m,maxit,mm,mp1,nct,ncu,nrt;

      int jm1,lsm1,km1,mm1;

      boolean wantu,wantv;

//   Java requires that all local variables be initialized before they
//   are used.  This is the reason for the initialization
//   line below:

      ls = 0;

//   set the maximum number of iterations

      maxit = 30;

//   determine what is to be computed

      wantu = false;
      wantv = false;

      jobu = (job%100)/10;

      ncu = n;

      if (jobu > 1) ncu = Math.min(n,p);
      if (jobu != 0) wantu = true;
      if ((job%10) != 0) wantv = true;

//   reduce x to bidiagonal form, storing the diagonal elements
//   in s and the super-diagonal elements in e

      nct = Math.min(n-1,p);
      nrt = Math.max(0,Math.min(p-2,n));
      lu = Math.max(nct,nrt);

//      if (lu >= 1) {       This test is not necessary under Java.
//                           The loop will be skipped if lu < 1 = the
//                           starting value of l.

         for (l = 1; l <= lu; l++) {

            lm1 = l - 1;

            lp1 = l + 1;

            if (l <= nct) {

//   compute the transformation for the l-th column and
//   place the l-th diagonal in s[lm1]

               s[lm1] = Blas.colnrm2_j(n-l+1,x,lm1,lm1);

               if (s[lm1] != 0.0) {

                  if (x[lm1][lm1] != 0.0) s[lm1] = Blas.sign_j(s[lm1],x[lm1][lm1]);
                  Blas.colscal_j(n-l+1,1.0/s[lm1],x,lm1,lm1);
                  x[lm1][lm1]++;

               }

               s[lm1] = -s[lm1];

            }

            for (j = l; j < p; j++) {

               if ((l <= nct) && (s[lm1] != 0.0)) {

//   apply the transformation

                  t = -Blas.coldot_j(n-l+1,x,lm1,lm1,j)/x[lm1][lm1];
                  Blas.colaxpy_j(n-l+1,t,x,lm1,lm1,j);

               }

//   place the l-th row of x into e for the
//   subsequent calculation of the row transformation

               e[j] = x[lm1][j];

            }


            if (wantu && (l <= nct)) {

//   place the transformation in u for subsequent
//   back multiplication

               for (i = lm1; i < n; i++) {

                  u[i][lm1] = x[i][lm1];

               }

            }

            if (l <= nrt) {

//   compute the l-th row transformation and place the
//   l-th super-diagonal in e[lm1]

               e[lm1] = Blas.dnrm2p_j(p-l,e,l);

               if (e[lm1] != 0.0) {

                  if (e[l] != 0.0) e[lm1] = Blas.sign_j(e[lm1],e[l]);
                  Blas.dscalp_j(p-l,1.0/e[lm1],e,l);
                  e[l]++;

               }

               e[lm1] = -e[lm1];

               if ((lp1 <= n) && (e[lm1] != 0.0)) {

//   apply the transformation

                  for (i = l; i < n; i++) {

                     work[i] = 0.0;

                  }

                  for (j = l; j < p; j++) {

                     Blas.colvaxpy_j(n-l,e[j],x,work,l,j);

                  }

                  for (j = l; j < p; j++) {

                     Blas.colvraxpy_j(n-l,-e[j]/e[l],work,x,l,j);

                  }

               }

               if (wantv) {

//   place the transformation in v for subsequent
//   back multiplication

                  for (i = l; i < p; i++) {

                     v[i][lm1] = e[i];

                  }

               }

            }

         }

//      }         This test (see above) is not necessary under Java

//   set up the final bidiagonal matrix of order m

      m = Math.min(p,n+1);

      if (nct < p) s[nct] = x[nct][nct];
      if (n < m) s[m-1] = 0.0;
      if (nrt+1 < m) e[nrt] = x[nrt][m-1];

      e[m-1] = 0.0;

//   if required, generate u

      if (wantu) {


         for (j = nct; j < ncu; j++) {

            for (i = 0; i < n; i++) {

               u[i][j] = 0.0;

            }

            u[j][j] = 1.0;

         }


//         if (nct >= 1) {            This test is not necessary under Java.
//                                    The loop will be skipped if nct < 1.


            for (ll = 1; ll <= nct; ll++) {

               l = nct - ll + 1;
               lm1 = l - 1;

               if (s[lm1] != 0.0) {

//                  lp1 = l + 1;


                  for (j = l; j < ncu; j++) {

                     t = -Blas.coldot_j(n-l+1,u,lm1,lm1,j)/u[lm1][lm1];
                     Blas.colaxpy_j(n-l+1,t,u,lm1,lm1,j);

                  }


                  Blas.colscal_j(n-l+1,-1.0,u,lm1,lm1);
                  u[lm1][lm1]++;


                  for (i = 0; i < lm1; i++) {

                     u[i][lm1] = 0.0;

                  }


               } else {

                  for (i = 0; i < n; i++) {

                     u[i][lm1] = 0.0;

                  }

                  u[lm1][lm1] = 1.0;

               }

            }

//         }      This test is not necessary under Java.  See above.

      }

//   if it is required, generate v

      if (wantv) {

         for (ll = 1; ll <= p; ll++) {

            l = p - ll + 1;
            lm1 = l - 1;

            if ((l <= nrt) && (e[lm1] != 0.0)) {

               for (j = l; j < p; j++) {

                  t = -Blas.coldot_j(p-l,v,l,lm1,j)/v[l][lm1];
                  Blas.colaxpy_j(p-l,t,v,l,lm1,j);

               }

            }

            for (i = 0; i < p; i++) {

               v[i][lm1] = 0.0;

            }

            v[lm1][lm1] = 1.0;

         }

      }

//   main iteration loop for the singular values

      mm = m;
      iter = 0;

      while (true) {

//   quit if all of the singular values have been found

         if (m == 0) return;

//   if too many iterations have been performed,
//   set flag and return

         if (iter >= maxit) {

            throw new SVDCException(m);

         }

/*

   This section of the program inspects for
   negligible elements in the s and e arrays.
   On completion the variables kase and l are
   set as follows:

      kase = 1     If s[m] and e[l-1] are negligible and l < m
      kase = 2     If s[l] is negligible and l < m
      kase = 3     If e[l-1] is negligible, l < m, and
                   s[l], ..., s[m] are not negligible (QR step)
      kase = 4     If e[m-1] is negligible (convergence)

    The material above is for FORTRAN style indexing.  Subtract
    indices by 1 for Java style indexing.

*/


         for (ll = 1; ll <= m; ll++) {

            l = m - ll;
            lm1 = l - 1;

            if (l == 0) break;

            test = Math.abs(s[lm1]) + Math.abs(s[l]);
            ztest = test + Math.abs(e[lm1]);

            if (ztest == test) {

               e[lm1] = 0.0;
               break;

            }

         }


         if (l == m - 1) {

            kase = 4;

         } else {

            lp1 = l + 1;
            mp1 = m + 1;

            for (lls = lp1; lls <= mp1; lls++) {

               ls = m - lls + lp1;
               lsm1 = ls - 1;
               if (ls == l) break;

               test = 0.0;
               if (ls != m)  test += Math.abs(e[lsm1]);
               if (ls != l+1) test += Math.abs(e[lsm1-1]);

               ztest = test + Math.abs(s[lsm1]);

               if (ztest == test) {

                  s[lsm1] = 0.0;
                  break;

               }

            }

            if (ls == l) {

               kase = 3;

            } else if (ls == m) {

               kase = 1;

            } else {

               kase = 2;
               l = ls;

            }

         }

         l++;

         lm1 = l - 1;

         mm1 = m - 1;

//   perform the task indicated by kase

         switch (kase) {

            case 1:

//   deflate negligible s[m]

//   There is a bug in the 1.0.2 Java compiler that
//   forces me to skimp on the number of local
//   variables.  This is the cause of the following changes:

//               mm1 = m - 1;

               f = e[m-2];
               e[m-2] = 0.0;

               for (kk = l; kk <= mm1; kk++) {

                  k = (mm1) - kk + l;
                  km1 = k - 1;

                  t1 = s[km1];

//   Although objects are passed by reference, primitive types
//   (e.g., doubles, ints, ...) are passed by value.  Thus
//   rotvec is needed.

                  rotvec[0] = t1;
                  rotvec[1] = f;
                  Blas.drotg_j(rotvec);
                  t1 = rotvec[0];
                  f  = rotvec[1];
                  cs = rotvec[2];
                  sn = rotvec[3];

                  s[km1] = t1;

                  if (k != l) {

                     f = -sn*e[k-2];
                     e[k-2] *= cs;

                  }

                  if (wantv) Blas.colrot_j(p,v,km1,mm1,cs,sn);

               }

               break;

            case 2:

//   split at negligible s[lm1]

               f = e[l-2];
               e[l-2] = 0.0;

               for (k = l; k <= m; k++) {

                  km1 = k - 1;

                  t1 = s[km1];

//   Although objects are passed by reference, primitive types
//   (e.g., doubles, ints, ...) are passed by value.  Thus
//   rotvec is needed.

                  rotvec[0] = t1;
                  rotvec[1] = f;
                  Blas.drotg_j(rotvec);
                  t1 = rotvec[0];
                  f  = rotvec[1];
                  cs = rotvec[2];
                  sn = rotvec[3];

                  s[km1] = t1;

                  f = -sn*e[km1];

                  e[km1] *= cs;

                  if (wantu) Blas.colrot_j(n,u,km1,l-2,cs,sn);

               }

               break;

            case 3:

//   perform one QR step

//   calculate the shift

//   There is a bug in the 1.0.2 Java compiler that
//   forces me to skimp on the number of local
//   variables.  Otherwise the following would have
//   been shorter:

               scale = Math.max(Math.abs(s[mm1]),Math.abs(s[m-2]));
               scale = Math.max(Math.abs(e[m-2]),scale);
               scale = Math.max(Math.abs(s[lm1]),scale);
               scale = Math.max(Math.abs(e[lm1]),scale);


//   There is a bug in the 1.0.2 Java compiler that
//   forces me to skimp on the number of local
//   variables.  This is the cause of the following changes:

//               sm = s[m]/scale;

//               smm1 = s[mm1]/scale;
//               emm1 = e[mm1]/scale;
//               sl = s[l]/scale;
//               el = e[l]/scale;

//               b = ((smm1 + sm)*(smm1 - sm) + emm1*emm1)/2.0;
//               c = (sm*emm1)*(sm*emm1);


               b = ((s[m-2] + s[mm1])*(s[m-2] - s[mm1]) +
                     e[m-2]*e[m-2])/(2.0*scale*scale);
               c = (s[mm1]*e[m-2])*(s[mm1]*e[m-2])/(scale*scale*scale*scale);

               shift = 0.0;

               if ((b != 0.0) || (c != 0.0)) {

                  shift = Math.sqrt(b*b + c);
                  if (b < 0.0) shift = -shift;
                  shift = c/(b + shift);

               }

//               f = (sl + sm)*(sl - sm) - shift;
//               g = sl*el;

               f = (s[lm1] + s[mm1])*(s[lm1] - s[mm1])/(scale*scale) - shift;
               g = s[lm1]*e[lm1]/(scale*scale);

//   chase zeros


//   There is a bug in the 1.0.2 Java compiler that
//   forces me to skimp on the number of local
//   variables.  This is the cause of the following changes:

//               mm1 = m - 1;

//               for (k = l; k <= mm1; k++) {

               for (k = l; k <= mm1; k++) {

                  km1 = k - 1;

//   Although objects are passed by reference, primitive types
//   (e.g., doubles, ints, ...) are passed by value.  Thus
//   rotvec is needed.

                  rotvec[0] = f;
                  rotvec[1] = g;
                  Blas.drotg_j(rotvec);
                  f = rotvec[0];
                  g  = rotvec[1];
                  cs = rotvec[2];
                  sn = rotvec[3];

                  if (k != l) e[k-2] = f;
                  f = cs*s[km1] + sn*e[km1];
                  e[km1] = cs*e[km1] - sn*s[km1];
                  g = sn*s[k];
                  s[k] *= cs;

                  if (wantv) Blas.colrot_j(p,v,km1,k,cs,sn);

//   Although objects are passed by reference, primitive types
//   (e.g., doubles, ints, ...) are passed by value.  Thus
//   rotvec is needed.

                  rotvec[0] = f;
                  rotvec[1] = g;
                  Blas.drotg_j(rotvec);
                  f = rotvec[0];
                  g  = rotvec[1];
                  cs = rotvec[2];
                  sn = rotvec[3];

                  s[km1] = f;
                  f = cs*e[km1] + sn*s[k];
                  s[k] = -sn*e[km1] + cs*s[k];
                  g = sn*e[k];
                  e[k] *= cs;

                  if (wantu && (k < n)) Blas.colrot_j(n,u,km1,k,cs,sn);

               }

               e[m-2] = f;
               iter++;

               break;

            case 4:

//   convergence

//   make the singular value positive

               if (s[lm1] < 0.0) {

                  s[lm1] = -s[lm1];
                  if (wantv) Blas.colscal_j(p,-1.0,v,0,lm1);

               }

//   order the singular value

               while (l != mm) {

                  if (s[lm1] >= s[l]) break;

                  t = s[lm1];
                  s[lm1] = s[l];
                  s[l] = t;

                  if (wantv && (l < p)) Blas.colswap_j(p,v,lm1,l);

                  if (wantu && (l < n)) Blas.colswap_j(n,u,lm1,l);

                  l++;

               }

               iter = 0;
               m--;

               break;

         }

      }

   }

}





