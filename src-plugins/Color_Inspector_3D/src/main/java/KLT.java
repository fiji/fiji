import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Random;


/**
 * Karhunen-Loeve-Tranformation */
public class KLT {
	double[][] m;
	private int size;
	private EigenRaum[] ers;

	private class EigenRaum implements Comparable {
		double ew;
		int evindex;

		EigenRaum(double ew, int evindex) {
			this.ew = ew;
			this.evindex = evindex;
		}

		public int compareTo(Object o) throws ClassCastException {
			if(!(o instanceof EigenRaum))
				throw new ClassCastException();

			EigenRaum er = (EigenRaum)o;

			if(Math.abs(ew) == Math.abs(er.ew))
				return 0;
			else
				return (Math.abs(ew) > Math.abs(er.ew)) ? -1 : 1;
		}
	}


	/**
	 * Returns the Autocovariance matrice of the double arrays with
	 * which it was called. */
	private static double[][] makeAutoCovarianceMatrice_(double[][] vec) {

		int dim = vec[0].length;
		double[][] out = new double[dim][dim];

		double  _n = 1. / vec.length;

		for(int k = 0; k < vec.length; k++) {
			double[] x = vec[k];
			for(int i = 0; i < dim; i++)
				for(int j = i; j < dim; j++)
					out[i][j] += x[i]*x[j];
		}

		for(int i = 0; i < dim; i++)
			for(int j = i; j < dim; j++)
				out[i][j] *= _n;
		
		for(int i = 0; i < dim; i++)
			for(int j = i; j < dim; j++)
				out[j][i] = out[i][j];
		
		return out;
	}

		public KLT(double[][] vecs) {
		size = vecs[0].length;
	
		m = makeAutoCovarianceMatrice_(vecs);
		//SymMat.printMatrice(m);
		//System.out.println("-------------------------------------");
		
		double[] d_ = new double[size];
		double[] e_ = new double[size];

		SymMat.tred2_(m, size, d_, e_);
		//SymMat.printMatrice(m);
		
		
		SymMat.tqli_(d_, e_, size, m);
			
		//System.out.println("-------------------------------------");
		//SymMat.printMatrice(m);
		
		ers = new EigenRaum[size];

		for(int i = 0; i < size; i++)
			ers[i] = new EigenRaum(d_[i], i);
	}

	public double[] getEigenValues() {
		double[] ev = new double[ers.length];

		for (int i = 0; i < ev.length; i++)
			ev[i] = ers[i].ew;

		return ev;	
	}

	public double[][] getEigenVectors() {
		double[][] ev = new double[ers.length][size];

		for (int i = 0; i < ev.length; i++)
			for (int j = 0; j < ev[0].length; j++)
				ev[i][j] = m[i][j];

		return ev;	
	}

	public double[] transform(double[] x) {

		double[] erg = new double[size];

		for(int i = 0; i < size; i++)
			for(int j = 0; j < size; j++)
				erg[i] += m[j][i]*x[j];

		return erg;
	}
	
	public void transform(double[][] in, double[][] out, double[] mean) {
		
		double[] x = new double[size];
		
		for (int n = 0; n < in.length; n++) {
			double[] erg = out[n];
			double[] in_ = in[n];
			
			for(int i = 0; i < size; i++)
				x[i] = in_[i] - mean[i];
			
			for(int i = 0; i < size; i++)
				for(int j = 0; j < size; j++)
					erg[i] += m[j][i]*x[j];	
		}
	}

	public double[] inverseTransform(double[] x) {
		
		double[] erg = new double[size];

		for(int i = 0; i < size; i++)
			for(int j = 0; j < size; j++)
				erg[i] += m[i][j]*x[j];

		return erg;
	}

	
	public void inverseTransform(double[][] in, double[][] out, double[] mean, double[] weights) {

		for (int n = 0; n < in.length; n++) {
			double[] x = in[n];
			double[] erg = out[n];

			for(int i = 0; i < size; i++) {
				for(int j = 0; j < size; j++)
					erg[i] += m[i][j]*x[j]*weights[j];
				erg[i] += mean[i];
			}
		}
	}

	public void inverseTransform(double[][] in, double[][] out, double[] mean) {
		
		for (int n = 0; n < in.length; n++) {
			double[] x = in[n];
			double[] erg = out[n];

			for(int i = 0; i < size; i++) {
				for(int j = 0; j < size; j++)
					erg[i] += m[i][j]*x[j];
				erg[i] += mean[i];
			}
		}
	}
	
	public double[] inverseTransform(double[] x, double[] weights) {
		
		double[] erg = new double[size];

		for(int i = 0; i < size; i++)
			for(int j = 0; j < size; j++)
				erg[i] += m[i][j]*x[j]*weights[j];

		return erg;
	}



	public void printBase() {
		for(int x = 0; x < size; x++) {
			for(int y = 0; y < size; y++)
				//System.err.print(format(m[x+1][ers[y].evindex]) + " ");
				System.err.print(format(m[x][y]) + " ");
			System.err.println();
		}
		System.err.println();
	}

	public static double[][] normalize(double[][] vecs, double[] mean) {
		if(vecs == null || vecs.length == 0 || mean.length < vecs[0].length)
			return null;

		double[][] out = new double[vecs.length][vecs[0].length];

		int n = vecs.length;

		for(int i = 0; i < vecs[0].length; i++)
			mean[i] = 0;

		for(int i = 0; i < vecs[0].length; i++)
			for(int k = 0; k < n; k++)
				mean[i] += vecs[k][i];

		for(int i = 0; i < vecs[0].length; i++)
			mean[i] /= n;

		for(int i = 0; i < vecs[0].length; i++)
			for(int k = 0; k < n; k++)
				out[k][i] = vecs[k][i] - mean[i];

		return out;
	}


	public static double[] denormal(double[] v, double[] mean) {
		double[] out = new double[v.length];

		for(int i = 0; i < v.length; i++)
			out[i] = v[i] + mean[i];

		return out;
	}

	private static String format(double x, int vor, int nach) {
		char[] sp = new char[vor];
		for (int i = 0; i < vor; i++)
			sp[i] = ' ';
		String spaces = new String(sp);
		DecimalFormat f = new DecimalFormat();
		f.setMinimumFractionDigits(nach);
		StringBuffer buf = new StringBuffer();
		FieldPosition fpos = new FieldPosition(NumberFormat.INTEGER_FIELD);
		f.format(x, buf, fpos);
		int anzSp = vor - (fpos.getEndIndex() - fpos.getBeginIndex());
		if (x < 0)
			--anzSp;
		if (anzSp < 0)
			return buf.toString();
		else
			return spaces.substring(0, anzSp) + buf.toString();
	}

	private static final String format(double x) {
		return format(x, 10, 5);
	}

}
