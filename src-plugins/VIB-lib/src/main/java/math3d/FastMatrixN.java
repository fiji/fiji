package math3d;

import ij.IJ;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FastMatrixN {
	public static void invert(double[][] matrix) {
		invert(matrix, false);
	}

	public static void invert(double[][] matrix, boolean showStatus) {
		int M = matrix.length;

		if (M != matrix[0].length)
			throw new RuntimeException("invert: no square matrix");

		double[][] other = new double[M][M];
		for (int i = 0; i < M; i++)
			other[i][i] = 1;

		// empty lower left triangle
		for (int i = 0; i < M; i++) {
			if (showStatus)
				IJ.showStatus("invert matrix: "
						+ i + "/" + (2 * M));
			// find pivot
			int p = i;
			for (int j = i + 1; j < M; j++)
				if (Math.abs(matrix[j][i]) >
						Math.abs(matrix[p][i]))
					p = j;

			if (p != i) {
				double[] d = matrix[p];
				matrix[p] = matrix[i];
				matrix[i] = d;
				d = other[p];
				other[p] = other[i];
				other[i] = d;
			}

			// normalize
			if (matrix[i][i] != 1.0) {
				double f = matrix[i][i];
				for (int j = i; j < M; j++)
					matrix[i][j] /= f;
				for (int j = 0; j < M; j++)
					other[i][j] /= f;
			}

			// empty rest of column
			for (int j = i + 1; j < M; j++) {
				double f = matrix[j][i];
				for (int k = i; k < M; k++)
					matrix[j][k] -= f * matrix[i][k];
				for (int k = 0; k < M; k++)
					other[j][k] -= f * other[i][k];
			}
		}

		// empty upper right triangle
		for (int i = M - 1; i > 0; i--) {
			if (showStatus)
				IJ.showStatus("invert matrix: "
						+ (2 * M - i) + "/" + (2 * M));
			for (int j = i - 1; j >= 0; j--) {
				double f = matrix[j][i] / matrix[i][i];
				for (int k = i; k < M; k++)
					matrix[j][k] -= f * matrix[i][k];
				for (int k = 0; k < M; k++)
					other[j][k] -= f * other[i][k];
			}
		}

		// exchange
		for (int i = 0; i < M; i++)
			matrix[i] = other[i];
	}

	public static double[][] clone(double[][] matrix) {
		int M = matrix.length, N = matrix[0].length;
		double[][] result = new double[M][N];
		for (int i = 0; i < M; i++)
			System.arraycopy(matrix[i], 0, result[i], 0, N);
		return result;
	}

	public static double[][] times(double[][] m1, double[][] m2) {
		int K = m2.length;
		if (m1[0].length != m2.length)
			throw new RuntimeException("rank mismatch");
		int M = m1.length, N = m2[0].length;
		double[][] result = new double[M][N];
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				for (int k = 0; k < K; k++)
					result[i][j] += m1[i][k] * m2[k][j];
		return result;
	}

	public static double[] times(double[][] m, double[] v) {
		int K = v.length;
		if (m[0].length != v.length)
			throw new RuntimeException("rank mismatch");
		int M = m.length;
		double[] result = new double[M];
		for (int i = 0; i < M; i++)
			for (int k = 0; k < K; k++)
					result[i] += m[i][k] * v[k];
		return result;
	}

	/**
	 * @return The lower triangular form resulting from a
	 * LU decomposition
	 * Note: no pivoting is done // TODO
	 */
	static double[][] LU_decomposition(double[][] m){

		int N = m.length;
		double[][] R = new double[N][N], L = new double[N][N];

		for(int i=0;i<N;i++){
			for(int j=i;j<N;j++){
				R[i][j] = m[i][j];
				for(int k=0;k<i;k++){
					R[i][j] -= L[i][k] * R[k][j];
				}
			}
			for(int j=i+1;j<N;j++){
				L[j][i] = m[j][i];
				for(int k=0;k<i;k++){
					L[j][i] -= L[j][k] * R[k][i];
				}
				L[j][i] /= R[i][i];
			}
		}
		double[][] LU = new double[N][N];
		for(int i=0;i<N;i++)
			for(int j=0;j<N;j++)
				LU[i][j] = L[i][j] + R[i][j];
		return LU;
	}

	/**
	 * @return The upper triangular form resulting from the
	 * Cholensky decomposition
	 * @see http://en.wikipedia.org/wiki/Cholesky_decomposition
	 */
	static double[][] choleskyDecomposition(double[][] m){

		if(m.length != m[0].length){
			throw new RuntimeException("Row and column rank "
				+ "must be equal");
		}
		int N = m.length;
		double[][] l = new double[N][N];
		for(int i=0;i<N;i++)
			for(int j=0;j<N;j++)
				l[i][j] = 0.0;
		double sum = 0.0;
		for(int i=0;i<N;i++){
			// l[i][i]
			sum = 0.0;
			for(int k = 0;k<i;k++){
				sum += l[k][i] * l[k][i];
			}
			if(m[i][i] - sum < 0){
				throw new RuntimeException("Matrix must be "
					+ "positive definite (trace is "
					+ sum + ", but diagonal element "
					+ i + " is " + m[i][i] + ")");
			}
			l[i][i] = (double)Math.sqrt(m[i][i] - sum);
			// l[i][j]
			for(int j=i+1;j<N;j++){
				sum = 0.0;
				for(int k=0;k<i;k++){
					sum += l[k][j] * l[k][i];
				}
				l[i][j] = (m[i][j] - sum)/l[i][i];
			}
		}
		return l;
	}

	public static double[][] transpose(double[][]m){
		double[][] ret = new double[m[0].length][m.length];
		for(int i=0;i<ret.length;i++){
			for(int j=0;j<ret[i].length;j++){
				ret[i][j] = m[j][i];
			}
		}
		return ret;
	}

	public static double[] solve_UL(double[][] A, double[] b){
		double[][] LU = LU_decomposition(A);

		// forward substitution
		double[] y = new double[b.length];
		for(int i=0;i<y.length;i++){
			double sum = 0.0;
			for(int j=0;j<i;j++){
				sum += LU[i][j]*y[j];
			}
			y[i] = (b[i] - sum);
		}

		// backward substitution
		double[] x = new double[b.length];
		for(int i=x.length-1;i>=0;i--){
			double sum = 0.0;
			for(int j=i+1;j<x.length;j++){
				sum += LU[i][j]*x[j];
			}
			x[i] = (y[i] - sum)/LU[i][i];
		}
		return x;
	}

	/**
	 * Solve Ax = b. Note: A has to be symmetric and positive definite
	 * @param A matrix to be applied
	 * @param b result
	 * @return x
	 * @see http://planetmath.org/?op=getobj&from=objects&id=1287
	 */
	public static double[] solve_cholesky(double[][]A, double[] b){

		// get the cholesky decomposition of A which is in upper triangle form
		double[][] U;
		try {
			U = choleskyDecomposition(A);
		} catch (RuntimeException e) {
			throw e;
		}
		U = choleskyDecomposition(A);
		double [][] L = transpose(U);
		// first solve Ly = b for y
		double[] y = forward_substitution(L, b);
		// then solve Ux = y for x
		double[] x = backward_substitution(U, y);

		return x;
	}

	/**
	 * Backward substitution algorithm. Solves a linear equation system
	 * Ux = b for x, where U is a matrix in upper trianglular form
	 * @return x
	 */
	private static double[] backward_substitution(double[][]U, double[] b){
		// solve Ux = b for x
		double[] x = new double[b.length];
		for(int i=x.length-1;i>=0;i--){
			double sum = 0.0;
			for(int j=i+1;j<x.length;j++){
				sum += U[i][j]*x[j];
			}
			x[i] = (b[i] - sum)/U[i][i];
		}
		return x;
	}

	/**
	 * Forward substitution algorithm. Solves a linear equation system
	 * Ly = b for y, where L is a matrix in lower trianglular form
	 * @return y
	 */
	private static double[] forward_substitution(double[][] L, double[] b){
		// solve Ly = b for y
		double[] y = new double[b.length];
		for(int i=0;i<y.length;i++){
			double sum = 0.0;
			for(int j=0;j<i;j++){
				sum += L[i][j]*y[j];
			}
			y[i] = (b[i] - sum)/L[i][i];
		}
		return y;
	}

	/**
	 * Calculates b in Ax = b
	 * @param A matrix to apply
	 * @param x vector
	 * @return b
	 */
	public static double[] apply(double[][]A, double x[]){
		int m = A.length;
		int n = A[0].length;
		double[] b = new double[x.length];
		for(int i=0;i<m;i++){
			b[i] = 0.0;
			for(int j=0;j<n;j++){
				b[i] += A[i][j] * x[j];
			}
		}
		return b;
	}

	public static void print(double[] v){
		System.out.print("[");
		for(int i=0;i<v.length;i++){
			System.out.print(v[i] + ", ");
		}
		System.out.print("]");
		System.out.println();
	}

	public static void print(double[][] m) {
		print(m,System.out);
	}

	public static void print(double[][] m, PrintStream out, char del){
		DecimalFormat f = new DecimalFormat("0.00");
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[i].length; j++)
				out.print(f.format(m[i][j]) + del);
			out.println("");
		}
		out.println();
	}

	public static float round(double d, int scale,RoundingMode mode){
		BigDecimal bd = BigDecimal.valueOf(d);
		return (bd.setScale(scale, mode)).floatValue();
	}

	public static void print(double[][] m, PrintStream out) {
		print(m,out,'\t');
	}

	public static void main(String[] args) {

		double dou = 1234.1234;
		BigDecimal bd = BigDecimal.valueOf(dou);
		System.out.println(bd.unscaledValue() + " " + bd.scale());

		int inte = BigDecimal.valueOf(dou).movePointLeft(2).unscaledValue().intValue() * 100;
		bd = bd.movePointLeft(2);
		System.out.println(bd.unscaledValue());

		System.out.println("Test rounding");
		double d = 1.234567889;
		System.out.println("Math.round(" + d + ") = " + Math.round(d));
		System.out.println("round(" + d + ",2) = " + round(d,2,RoundingMode.HALF_EVEN));
		double[][] m = {
			{1, 2, 3, 2},
			{-1, 0, 2, -3},
			{-2, 1, 1, 1},
			{0, -2, 3, 0}};

		double[][] m1 = clone(m);
		invert(m1);

		double[][] m2 = times(m, m1);
		print(m2);

		// test it with a hilbert matrix
		double[][] k = new double[5][5];
		for(int i=0;i<k.length;i++)
			for(int j=i;j<k.length;j++)
				k[i][j] = k[j][i] = 1.0/(i+j+1);

		System.out.println("Original matrix ");
		print(k);
		double[][] l = choleskyDecomposition(k);
		System.out.println("Upper triangular form u of cholesky decomposition ");
		print(l);
		double[][] l_t = transpose(l);
		System.out.println("Transposed form u^T of u ");
		print(l_t);
		double[][] prod = times(l_t,l);
		System.out.println("Finally the product of the u^T and u, which should give the original matrix ");
		print(prod);

		double[] x = new double[]{1.0,2.0,3.0, 4.0, 5.0};
		System.out.println("A vector x: x = [1.0 2.0 3.0]^T\n");
		double[] b = apply(k, x);
		System.out.println("Applying the original matrix to x gives b: ");
		print(b);

		System.out.println("\n\nTest different solve methods");
		System.out.println("\nTest Cholesky decomposition");
		double[] x_n = solve_cholesky(k,b);
		System.out.println("Now solve Ax = b for x and see if it is the original x");
		print(x_n);

		System.out.println("\nTest LU decomposition");
		System.out.println("Now solve Ax = b for x and see if it is the original x");
		x_n = solve_UL(k, b);
		print(x_n);

		System.out.println("\nTest ordinary invert method");
		System.out.println("Now solve Ax = b for x and see if it is the original x");
		double[][] k_inv = clone(k);
		invert(k_inv);
		x_n = apply(k_inv,b);
		print(x_n);
	}
}

