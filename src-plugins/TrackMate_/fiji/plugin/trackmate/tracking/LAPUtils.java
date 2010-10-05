package fiji.plugin.trackmate.tracking;

public class LAPUtils {
	
	/*
	 * STATIC METHODS - UTILS
	 */
	
	
	public static final void echoMatrix(final double[][] m) {
		int nlines = m.length;
		int nrows = m[0].length;
		double val;
		System.out.print("L\\C\t");
		for (int j = 0; j < nrows; j++) {
			System.out.print(String.format("%7d: ", j));
		}
		System.out.println();
		for (int i = 0; i < nlines; i++) {
			System.out.print(i+":\t");
			for (int j = 0; j < nrows; j++) {
				val = m[i][j];
				if (val > Double.MAX_VALUE/2)
					System.out.print("     B   ");
				else
					System.out.print(String.format("%7.1f  ", val));
			}
			System.out.println();
		}
	}
	
	
	public static void echoSolutions(final int[][] solutions) {
		for (int i = 0; i < solutions.length; i++)
			System.out.println(String.format("%3d: %3d -> %3d", i, solutions[i][0], solutions[i][1]));
	}

}
