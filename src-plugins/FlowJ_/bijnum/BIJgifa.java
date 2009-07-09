package bijnum;
import java.util.*;
import ij.*;
/**
 * Implementation of GIFA (generalized indicator function approach).
 * Does a maximization of images with maximum signal power and signal to noise ratio.
 * Is a subset of BIJpca because it needs the PCA routines and output.
 *
 * (c) 2003 Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class BIJgifa extends BIJpca
{
        public float [][] phi; // column vectors of indicator functions.
        public float [][] rho; // column vectors of coefficients (phi = psi * beta)
        public float [] gamma; // eigenvalues from GIFA eigenvalue problem.
        public float [] snr; // signal to noise ratios of rho's
        public int k;

        /*
         * @param k the number of different conditions in a.
        */
        public BIJgifa()
        {
                super();
        }
        /**
        * Compute GIFA for k conditions.
        */
        public float[][] compute(float [][] a, int k)
        {
                this.a = a;
                this.k = k;
                int N = eigenvectors.length;
                int num = eigenvectors[0].length;
                int t = N / k;
                float p = 4;

                // Diagonal matrix of eigenvalues.

                float [] ev2 = new float[eigenvalues.length];
                BIJmatrix.pow(ev2, eigenvalues, 0.5f);
                float [][] S = BIJmatrix.diag(ev2);
                float [] abar = BIJstats.meanColumn(eigenvectors);
                float [][] condavg = submean(eigenvectors, k);
                // Forming the signal covariance matrix.
                float [][] Cs = new float[num][num];
                for (int i = 0; i < k; i++)
                {
                        float [] r = BIJmatrix.sub(condavg[i], abar);
                        float [][] m = BIJmatrix.mulOuter(r, r);
                        BIJmatrix.add(Cs, m);
                }
                BIJmatrix.mul(Cs, Cs, 1/(k-1));
                // Forming noise covariance matrix.
                float [][] ahat = new float[N][num];
                // for  i=1:k ahat(t*(i-1)+1:t,:)=repmat(condavg(i,:),t,1); end
                // Simplifies to:
                for (int j = 0; j < N; j++)
                for (int i = 0; i < num; i++)
                        ahat[j][i] = condavg[j / t][i];
                float [][] Cn = new float[num][num];
                // idx=[(i-1)*t+1:t*i];
                // Cn = Cn+ (a(idx,:)-ahat(idx,:))'*(a(idx,:)-ahat(idx,:)); end;
                // Simplifies to:
                for (int i = 0; i < k; i++)
                {
                        float [][] Cnt = new float[t][num];
                        for (int h = 0; h < t; h++)
                        for (int g = 0; g < num; g++)
                                Cnt[h][g] = a[h * t][g] - ahat[h * t][g];
                        float [][] Cnp = new float[num][num];
                        BIJmatrix.mul(Cnp, BIJmatrix.transpose(Cnt), Cnt, true);
                        BIJmatrix.add(Cn, Cnp);
                }
                BIJmatrix.mul(Cn, Cn, 1/(k*t-k));
                float [][] SCsS = new float[num][num];
                BIJmatrix.mul(SCsS, S, Cs, true);
                BIJmatrix.mul(SCsS, S, true);
                float [][]pSCnS = new float[num][num];
                BIJmatrix.mul(pSCnS, S, Cn, true);
                BIJmatrix.mul(pSCnS, S, true);
                BIJmatrix.mul(pSCnS, pSCnS, p);
                float [][]SCnSminuspSCnS = new float[num][num];
                BIJmatrix.sub(SCnSminuspSCnS, SCsS, pSCnS);
                // Now compute eigenvalues and eigenvectors.
                // BIJJacobi(SCnSminuspSCnS);
                return null;
        }
        /**
        * Computes the mean of the submatrices of a MxN matrix F.
        * F is divided up in k submatrices of equal size,
        * F = [F1, F2, Fk]
        * The -ith submatrix Fi is (M/k)xN
        * @param F a MxN matrix of the above form.
        * @param k = number of submatrices
        * @return a kxN matrix of the form [mean(G1), mean(G2, mean(Fk)]
        */
        protected float [][] submean(float [][] F, int k)
        {
                int iM = F.length;
                int iN = F[0].length;
                float avg = (float) iM / k;
                int iavg = Math.round(avg);
                float [][] G = new float[k][iN];
                for (int n = 0; n < k; n++)
                for (int j = 0; j < G[0].length; j++)
                {
                        G[n][j] = 0;
                        for (int i = 0; i < iavg; i++)
                               G[n][j] += F[iavg * n + i][j];
                        G[n][j] /= iavg;
                }
                return G;
        }
}
