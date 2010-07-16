/**
	This class implements Jacobi's algorithm to find the eigenvectors
	of a symmetric real matrix.
	It is based on Numerical Recipes in C, ch. 11, sec. 1
*/

package mpicbg.spim.vib.math3d;

import mpicbg.spim.io.IOFunctions;

public class JacobiDouble {
	private final double[][] matrix;
	private final double[][] eigenmatrix;
	private final double[] eigenvalues;
	private int numberOfRotationsNeeded;
	private final int maxSweeps;

	public JacobiDouble(final double[][] matrix) {
		this(matrix,50);
	}
	public JacobiDouble(final double[][] matrix, final int maxSweeps) {
		this.matrix=matrix;
		for(int i=0;i<matrix.length;i++) {
			for(int j=i+1;j<matrix.length;j++)
				if(!isSmallComparedTo(Math.abs(matrix[i][j]-matrix[j][i]),matrix[i][j]))
					throw new RuntimeException("Matrix is not symmetric!");
		}
		eigenmatrix=new double[matrix.length][matrix.length];
		eigenvalues=new double[matrix.length];
		this.maxSweeps=maxSweeps;
		perform();
	}

	/** returns the eigenvectors as an array,
	    such that result[0] is the first vector */
	public double[][] getEigenVectors() {
		return FastMatrixN.transpose(eigenmatrix);
	}
	public double[][] getEigenMatrix() {
		return eigenmatrix;
	}
	/** returns the eigenvalues corresponding to
            the eigenvectors */
	public double[] getEigenValues() {
		return eigenvalues;
	}
	/** returns the number of rotations needed to
	    converge */
	public int getNumberOfRotations() {
		return numberOfRotationsNeeded;
	}

	/** The sum of the off diagonal elements */
	private double offDiagonalSum() {
		double sum=0;
		for(int i=0;i<matrix.length-1;i++)
			for(int j=i+1;j<matrix.length;j++)
				sum+=Math.abs(matrix[i][j]);
		return sum;
	}

	/** perform a rotation */
	private void rotate(int i,int j,int k,int l,double s,double tau) {
		double tmp1=matrix[i][j];
		double tmp2=matrix[k][l];
		matrix[i][j]=tmp1-s*(tmp2+tmp1*tau);
		matrix[k][l]=tmp2+s*(tmp1-tmp2*tau);
	}
	private void rotateEigenMatrix(int i,int j,int k,int l,double s,double tau) {
		double tmp1=eigenmatrix[i][j];
		double tmp2=eigenmatrix[k][l];
		eigenmatrix[i][j]=tmp1-s*(tmp2+tmp1*tau);
		eigenmatrix[k][l]=tmp2+s*(tmp1-tmp2*tau);
	}

	/** evaluates significance */
	private boolean isSmallComparedTo(double value,double reference) {
		return Math.abs(reference)+value==Math.abs(reference);
	}

	private void perform() {
		final double[] b=new double[matrix.length];
		final double[] z=new double[matrix.length];
		for(int i=0;i<matrix.length;i++) {
			for(int j=0;j<matrix.length;j++)
				eigenmatrix[i][j]=0;
			eigenmatrix[i][i]=1;
			b[i]=eigenvalues[i]=matrix[i][i];
			z[i]=0;
		}

		numberOfRotationsNeeded=0;
		for(int sweeps=0;sweeps<maxSweeps;sweeps++) {
			final double sum=offDiagonalSum();
			// This should be the normal way out
			if(sum==0.0)
				return;

			double thresh=0;
			if(sweeps<3)
				thresh=0.2f*sum/(matrix.length*matrix.length);

			// perform sweep
			for(int p=0;p<matrix.length-1;p++)
				for(int q=p+1;q<matrix.length;q++) {
					double tmp=100.0f*Math.abs(matrix[p][q]);
					if(sweeps>3 && isSmallComparedTo(tmp,eigenvalues[p])
						    && isSmallComparedTo(tmp,eigenvalues[q]))
						matrix[p][q]=0;
					else if(Math.abs(matrix[p][q])>thresh) {
						double diff=eigenvalues[q]-eigenvalues[p];
						double t;
						if(isSmallComparedTo(tmp,diff))
							t=matrix[p][q]/diff;
						else {
							double theta=0.5f*diff/matrix[p][q];
							t=1.0f/(double)(Math.abs(theta)+Math.sqrt(1.0f+theta*theta));
							if(theta<0)
								t=-t;
						}
						double c=1.0f/(double)Math.sqrt(1+t*t);
						double s=t*c;
						double tau=s/(1.0f+c);
						double h=t*matrix[p][q];
						z[p]-=h;
						z[q]+=h;
						eigenvalues[p]-=h;
						eigenvalues[q]+=h;
						matrix[p][q]=0;
						for(int j=0;j<=p-1;j++)
							rotate(j,p,j,q,s,tau);
						for(int j=p+1;j<=q-1;j++)
							rotate(p,j,j,q,s,tau);
						for(int j=q+1;j<matrix.length;j++)
							rotate(p,j,q,j,s,tau);
						for(int j=0;j<matrix.length;j++)
							rotateEigenMatrix(j,p,j,q,s,tau);
						numberOfRotationsNeeded++;
					}
				}
			for(int p=0;p<matrix.length;p++) {
				// Update eigenvalues with sum of t*matrix[p][q], reinint z
				b[p]+=z[p];
				eigenvalues[p]=b[p];
				z[p]=0;
			}
		}
	}

	public static String toString(double[] doubleArray) {
		String result="{";
		for(int i=0;i<doubleArray.length;i++) {
			if(i>0)
				result+=",";
			result+=doubleArray[i];
		}
		return result+"}";
	}
	public static String toString(double[][] double2Array) {
		String result="{";
		for(int i=0;i<double2Array.length;i++) {
			if(i>0)
				result+=",";
			result+=toString(double2Array[i]);
		}
		return result+"}";
	}

	public static double[] getColumn(double[][] matrix,int i) {
		double[] result=new double[matrix.length];
		for(int j=0;j<matrix.length;j++)
			result[j]=matrix[j][i];
		return result;
	}

	public static double[][] matMult(double[][] m1,double[][] m2) {
		int r=m1.length;
		int c=m2[0].length;
		double[][] result=new double[r][c];
		int i,j,k;
		for(i=0;i<r;i++)
			for(j=0;j<c;j++) {
				result[i][j]=0.0f;
				for(k=0;k<m2.length;k++) result[i][j]+=m1[i][k]*m2[k][j];
			}
		return result;
	}

	public static double[] vecMult(double[][] m,double[] v) {
		int r=m.length;
		double[] result=new double[r];
		int i,k;
		for(i=0;i<r;i++) {
			result[i]=0.0f;
			for(k=0;k<v.length;k++)
				result[i]+=m[i][k]*v[k];
		}
		return result;
	}

	public static double[][] transpose(double[][] m) {
		int r=m.length;
		int c=m[0].length;
		double[][] result=new double[c][r];
		int i,j;
		for(i=0;i<r;i++)
			for(j=0;j<c;j++)
				result[j][i]=m[i][j];
		return result;
	}

	public static void main(String[] args) {
		double[][] matrix={{1,2},{2,1}};
		JacobiDouble jacobi=new JacobiDouble(matrix);

		double[] eigenValuesVector=jacobi.getEigenValues();
		double[][] eigenValues=new double[eigenValuesVector.length][eigenValuesVector.length];
		for(int i=0;i<eigenValuesVector.length;i++)
			eigenValues[i][i]=eigenValuesVector[i];
		double[][] eigenVectors=jacobi.getEigenVectors();

		double[][] result=matMult(eigenVectors,matMult(eigenValues,transpose(eigenVectors)));
		IOFunctions.println("out: "+toString(result));
	}
}
