/**
	This class implements Jacobi's algorithm to find the eigenvectors
	of a symmetric real matrix.

	It is based on the description of the maths in

		Numerical Recipes in C, ch. 11, sec. 1

	It was not derived from the source code therein.
*/

package math3d;

public class JacobiFloat {
	private float[][] matrix;
	private float[][] eigenmatrix;
	private float[] eigenvalues;
	private int numberOfRotationsNeeded,maxSweeps;

	public JacobiFloat(float[][] matrix) {
		this(matrix,50);
	}
	public JacobiFloat(float[][] matrix,int maxSweeps) {
		this.matrix=matrix;
		for(int i=0;i<matrix.length;i++) {
			for(int j=i+1;j<matrix.length;j++)
				if(!isSmallComparedTo(Math.abs(matrix[i][j]-matrix[j][i]),matrix[i][j]))
					throw new RuntimeException("Matrix is not symmetric!");
		}
		eigenmatrix=new float[matrix.length][matrix.length];
		eigenvalues=new float[matrix.length];
		this.maxSweeps=maxSweeps;
		perform();
	}

	/** returns the eigenvectors as an array,
	    such that result[0] is the first vector */
	public float[][] getEigenVectors() {
		return FloatMatrixN.transpose(eigenmatrix);
	}
	public float[][] getEigenMatrix() {
		return eigenmatrix;
	}
	/** returns the eigenvalues corresponding to
            the eigenvectors */
	public float[] getEigenValues() {
		return eigenvalues;
	}
	/** returns the number of rotations needed to
	    converge */
	public int getNumberOfRotations() {
		return numberOfRotationsNeeded;
	}

	/** The sum of the off diagonal elements */
	private float offDiagonalSum() {
		float sum=0;
		for(int i=0;i<matrix.length-1;i++)
			for(int j=i+1;j<matrix.length;j++)
				sum+=Math.abs(matrix[i][j]);
		return sum;
	}

	/** perform a rotation */
	private void rotate(int i,int j,int k,int l,float s,float tau) {
		float tmp1=matrix[i][j];
		float tmp2=matrix[k][l];
		matrix[i][j]=tmp1-s*(tmp2+tmp1*tau);
		matrix[k][l]=tmp2+s*(tmp1-tmp2*tau);
	}
	private void rotateEigenMatrix(int i,int j,int k,int l,float s,float tau) {
		float tmp1=eigenmatrix[i][j];
		float tmp2=eigenmatrix[k][l];
		eigenmatrix[i][j]=tmp1-s*(tmp2+tmp1*tau);
		eigenmatrix[k][l]=tmp2+s*(tmp1-tmp2*tau);
	}

	/** evaluates significance */
	private boolean isSmallComparedTo(float value,float reference) {
		return Math.abs(reference)+value==Math.abs(reference);
	}

	private void perform() {
		float[] b=new float[matrix.length];
		float[] z=new float[matrix.length];
		for(int i=0;i<matrix.length;i++) {
			for(int j=0;j<matrix.length;j++)
				eigenmatrix[i][j]=0;
			eigenmatrix[i][i]=1;
			b[i]=eigenvalues[i]=matrix[i][i];
			z[i]=0;
		}

		numberOfRotationsNeeded=0;
		for(int sweeps=0;sweeps<maxSweeps;sweeps++) {
			float sum=offDiagonalSum();
			// This should be the normal way out
			if(sum==0.0f)
				return;

			float thresh=0;
			if(sweeps<3)
				thresh=0.2f*sum/(matrix.length*matrix.length);

			// perform sweep
			for(int p=0;p<matrix.length-1;p++)
				for(int q=p+1;q<matrix.length;q++) {
					float tmp=100.0f*Math.abs(matrix[p][q]);
					if(sweeps>3 && isSmallComparedTo(tmp,eigenvalues[p])
						    && isSmallComparedTo(tmp,eigenvalues[q]))
						matrix[p][q]=0;
					else if(Math.abs(matrix[p][q])>thresh) {
						float diff=eigenvalues[q]-eigenvalues[p];
						float t;
						if(isSmallComparedTo(tmp,diff))
							t=matrix[p][q]/diff;
						else {
							float theta=0.5f*diff/matrix[p][q];
							t=1.0f/(float)(Math.abs(theta)+Math.sqrt(1.0f+theta*theta));
							if(theta<0)
								t=-t;
						}
						float c=1.0f/(float)Math.sqrt(1+t*t);
						float s=t*c;
						float tau=s/(1.0f+c);
						float h=t*matrix[p][q];
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

	public static String toString(float[] floatArray) {
		String result="{";
		for(int i=0;i<floatArray.length;i++) {
			if(i>0)
				result+=",";
			result+=floatArray[i];
		}
		return result+"}";
	}
	public static String toString(float[][] float2Array) {
		String result="{";
		for(int i=0;i<float2Array.length;i++) {
			if(i>0)
				result+=",";
			result+=toString(float2Array[i]);
		}
		return result+"}";
	}

	public static float[] getColumn(float[][] matrix,int i) {
		float[] result=new float[matrix.length];
		for(int j=0;j<matrix.length;j++)
			result[j]=matrix[j][i];
		return result;
	}

	public static float[][] matMult(float[][] m1,float[][] m2) {
		int r=m1.length;
		int c=m2[0].length;
		float[][] result=new float[r][c];
		int i,j,k;
		for(i=0;i<r;i++)
			for(j=0;j<c;j++) {
				result[i][j]=0.0f;
				for(k=0;k<m2.length;k++) result[i][j]+=m1[i][k]*m2[k][j];
			}
		return result;
	}

	public static float[] vecMult(float[][] m,float[] v) {
		int r=m.length;
		float[] result=new float[r];
		int i,j,k;
		for(i=0;i<r;i++) {
			result[i]=0.0f;
			for(k=0;k<v.length;k++)
				result[i]+=m[i][k]*v[k];
		}
		return result;
	}

	public static float[][] transpose(float[][] m) {
		int r=m.length;
		int c=m[0].length;
		float[][] result=new float[c][r];
		int i,j;
		for(i=0;i<r;i++)
			for(j=0;j<c;j++)
				result[j][i]=m[i][j];
		return result;
	}

	public static void main(String[] args) {
		float[][] matrix={{1,2},{2,1}};
		JacobiFloat jacobi=new JacobiFloat(matrix);

		float[] eigenValuesVector=jacobi.getEigenValues();
		float[][] eigenValues=new float[eigenValuesVector.length][eigenValuesVector.length];
		for(int i=0;i<eigenValuesVector.length;i++)
			eigenValues[i][i]=eigenValuesVector[i];
		float[][] eigenVectors=jacobi.getEigenVectors();

		float[][] result=matMult(eigenVectors,matMult(eigenValues,transpose(eigenVectors)));
		System.out.println("out: "+toString(result));
	}
}
