package bijnum;
import java.util.*;
import ij.*;
/**
 * Principal component analysis (PCA) on 2-D sequences (ie. image sequences).
 * Image sequences are organized as matrix with each image as a column vector.
 * This is how you do it (Snapshot method by Sirovich et al.):
 * <pre>
 * ' is transpose
 * cov = a * a'
 * eiv = eigen(cov)
 * sort eiv system by eigenvalues, largest first.
 * diag = diagonal matrix of eigevnvalues
 * psi = a' * eigenvectors * diag
 * eigenimages = psi'
 * </pre>
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
public class BIJpca
{
	/**
	 * The matrix of eigenimages, i.e. the eigenvectors of the covariance matrix.
         * Each eigenimage is one of the columnvectors.
	 * In some literature, these are known as the psi(n), or
	 * column vectors of spatial principal components.
	*/
	public float [][] eigenimages;
	/** The eigenvalues of the transpose of the covariance matrix. */
	public float [] eigenvalues;
	/**
	 * The eigenvectors of the transpose of the covariance matrix.
	*/
	public float [][] eigenvectors;
        /*
        * The coordinates of the eigenimages, sometimes these are known as an(t), or
        * column vectors of temporal principal components.
        * Or, for images, they are the projection of the iegnimages on a.
        */
        protected float [][] an;

	/** The matrix of images. */
	public float [][] a;
	/** Total accumulative variance. */
	public double totalVariance;

	/**
	 * Set up PCA.
	 */
	public BIJpca() {}
        /**
         * Set up PCA for already computed coordinates and eigenimages.
         */
        public BIJpca(float [][] eigenimages, float [][] eigenvectors)
        {
                this.eigenimages = eigenimages;
                this.eigenvectors = eigenvectors;
        }
	/**
	 * Compute the PCA.
         * a is modified. a is a '3-D' array of images, organized as the columns
	 * of a 2-D matrix.
         * @param a a matrix of float[][]. column size should be > row size.
	 * @return psi, the eigenimages in corrected format (sorted by eigenvalue).
	 */
	public float [][] compute(float [][] a)
	throws Exception
	{
		//IJ.showStatus("Computing pca...normalizing images");
                System.out.println("Computing pca...normalizing images");
                this.a = a;
		double avg = BIJstats.means(a);
		//IJ.write(BIJutil.toString(avg));
		BIJmatrix.add(a, -avg);
                System.out.println("PCA uses "+(a.length*a[0].length * 4)+" bytes (a).");
		// Compute covariance matrix.
                System.out.println("Computing covariance matrix");
		float [][] cov = BIJstats.covariance(a, true);
                System.out.println("Computing pca...covariance matrix ("+(cov.length*cov[0].length)+"bytes) ok "+(new Date()).toString());
		// cov is not positive definite!
		//IJ.write("Covariance matrix\n"+BIJutil.toString(cov));
		// Prepare computing eigenvectors.
		//Jacobi jacobi = new Jacobi(cov.length);
		BIJJacobi jacobi = new BIJJacobi(cov, true);
                System.out.println("PCA...computing eigenvectors ");
		jacobi.compute();
                cov = null;
                System.gc();
                System.out.println("Computing pca...eigenvector matrix ok "+(new Date()).toString());
		jacobi.sort();
		//IJ.write("Eigenvalues:\n"+BIJutil.toString(jacobi.eigenvalues));
		//IJ.write("Eigensystem:\n"+jacobi.toString());
		//IJ.write(BIJutil.toString(jacobi.eigenvectors));
		this.eigenvalues = BIJmatrix.copy(jacobi.eigenvalues);
                // Will be transposed later on.
		this.eigenvectors = BIJmatrix.copy(jacobi.eigenvectors);
		// Convert the eigenvectors of A'*A into eigenvectors of A*A'  and
		// make a diagonal matrix out of the eigenvalue vector: the eigenvalues will be on the diagonal.
		computeEigenimages();
		return eigenimages;
	}
	/**
	 * Compute the spatial eigenimages, i.e. the eigenvectors that belong to a,
	 * from the eigenvectors/eigenvalues from the eigensystem of a'.
	 * Uses at, eigenvectors and eigenvalues as computed in compute().
	 * In some literature, the eigenimages (eigenvectors) are know as the psi(n)(x).
	 * @return the eigenimages (eigenvectors) as a float[][].
	 */
	protected float [][] computeEigenimages()
	{
		// eigenimages = (a' ev)'
                System.out.println("PCA...computing eigenimages");
                // Compute the eigenimages: transpose(a) * eigenvectors of a'.
		// eigenimages = BIJmatrix.transpose(BIJmatrix.mul(at, eigenvectors, true));
                eigenimages = BIJmatrix.mulT(a, eigenvectors, true);
                System.out.println("Computing pca...eigenimages ("+(eigenimages.length*eigenimages[0].length)+"bytes) ok "+(new Date()).toString());
                a = null;
                System.gc();
		// The internal eigenvectors is the transpose of the calculated one for the transposed images.
		eigenvectors = BIJmatrix.transpose(eigenvectors);
		// Clean up eigensystem.
                System.out.println("PCA...cleaning eigensystem");
                for (int j = 0; j < eigenimages.length; j++)
		{
			// Make all non relevant eigenvalues and the corresponding eigenvectors zero.
			if (eigenvalues[j] < 0)
			{
			        eigenvalues[j] = 0f;
			        for (int i = 0; i < eigenimages[j].length; i++) eigenimages[j][i] = 0;
			        for (int i = 0; i < eigenvectors[j].length; i++) eigenvectors[j][i] = 0;
			}
		}
		totalVariance = BIJstats.sum(eigenvalues);
		return eigenimages;
	}
	/**
	 * Compute the projection of a vector v onto the set of eigenimages.
         * @param v a float[] vector (an image).
	 */
	public float [] computeProjection(float [] v)
	throws Exception
	{
		IJ.showStatus("Computing projection...");
		float [] proj = BIJmatrix.mul(eigenimages, v);
		return proj;
	}
        /**
         * Compute the linear combination of all eigenimages. Should be the same as original sequence.
         * f = SUM(an-i psi-i)
         */
        public float [][] computeLinearCombination()
        {
                float [] factor = new float[eigenimages.length];
                for (int j = 0; j < eigenimages.length; j++)
                        factor[j] = 1;
                return computeLinearCombination(factor);
        }
        /**
        * Compute the normalized image for eigenimage j so that it can be displayed with the same
        * gray-scale as all others.
        */
        public float [] getEigenImage(int j)
        {
                float norm = BIJmatrix.norm(eigenimages[j]);
                float [] normImage = new float[eigenimages[j].length];
                BIJmatrix.mulElements(normImage, eigenimages[j], 1/norm);
                return normImage;
        }
        /**
        * Get the eigenvector coordinate matrix an as a matrix.
        * The coordinates are not the same as the eigenvectors of a' because internally a different format is used
        * to save time for computations.
        * @return the coordinates matrix as a float[][]
        */
        public float [][] getCoordinates()
        {
                if (eigenvalues == null)
                        return eigenvectors;
                else if (an == null)
                {
                        an = new float[eigenvectors.length][eigenvectors[0].length];
                        for (int j = 0; j < eigenvectors.length; j++)
                        for (int i = 0; i < eigenvectors[0].length; i++)
                        {
                                if (eigenvalues[j] != 0)
                                        an[j][i] = (float) (eigenvectors[j][i] / Math.sqrt(eigenvalues[j]));
                                else
                                        an[j][i] = 0;
                        }
                }
                return an;
        }
        /**
        * Get the eigenvector coordinate matrix an as an image (a float []) of width x height = eigenvectors.length.
        * The coordinates are not the same as the eigenvectors of a' because internally a different format is used
        * to save time for computations.
        * @return the coordinates matrix as a float[][]
        */
        public float [] getCoordinatesImage()
        {
                // Lambda is the diagonal matrix of the eigenvalues.
                float [] v = new float[eigenvectors.length*eigenvectors[0].length];
                for (int j = 0; j < eigenvectors.length; j++)
                for (int i = 0; i < eigenvectors[0].length; i++)
                {
                       if (eigenvalues[j] != 0)
                                v[j*eigenvectors[0].length+i] = (float) (eigenvectors[j][i] / Math.sqrt(eigenvalues[j]));
                        else
                                v[j*eigenvectors[0].length+i] = 0;
                }
                return v;
        }
        /**
         * Compute a (compacted) linear combination of the imagevectors, using only with factor set to 1.
         * f = SUM(factor-i an-i psi-i)
         * @param an the eigenvector coordinates (an) for the desired linear combination.
         * @return the linear combination as a vector.
         */
        public float [][] computeLinearCombination(float [] factor)
        {
                int n = 0;
                for (int j = 0; j < eigenimages.length; j++)
                       if (factor[j] != 0) n++;
                float [][] lc = new float[n][eigenimages[0].length];
                IJ.showStatus("Computing linear combination ("+n+")...");
                n = 0;
                for (int j = 0; j < eigenvectors.length; j++)
                {
                        IJ.showProgress(j, eigenvectors.length);
                        if (factor[j] != 0)
                        {
                                for (int k = 0; k < eigenvectors[j].length; k++)
                                {
                                        for (int i = 0; i < eigenimages[j].length; i++)
                                                lc[n][i] += factor[j] * eigenimages[k][i] * eigenvectors[k][j];
                                }
                                n++;
                        }
                }
                return lc;
        }
	/**
	 * Compute the accumulated variance spectrum.
	 * The values are the additional variance that each component explains.
	 * @return the variance spectrum as a float[].
	 */
	public float [] varianceSpectrum()
	{
                float [] spectrum = new float[eigenvalues.length];
		spectrum[0] = eigenvalues[0];
		float sum = 0;
		for (int i = 0; i < spectrum.length; i++)
			sum += eigenvalues[i];
		for (int i = 1; i < eigenvalues.length; i++)
	                spectrum[i] = spectrum[i-1] + eigenvalues[i];
		// Normalize.
		for (int i = 0; i < spectrum.length; i++)
			spectrum[i] /= sum;
                return spectrum;
	}
	public double getTotalVariance() { return totalVariance; }
}
