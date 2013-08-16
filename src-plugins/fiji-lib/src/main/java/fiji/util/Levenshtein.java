package fiji.util;

/*
 * This class implements the Damerau-Levenshtein algorithm to
 * calculate a distance between strings.
 *
 * Basically, it says how many letters need to be swapped, substituted,
 * deleted from, or added to s1, at least, to get s2.
 *
 * The idea is to build a distance matrix for the substrings of both
 * strings.  To avoid a large space complexity, only the last three rows
 * are kept in memory (if swaps had the same or higher cost as one deletion
 * plus one insertion, only two rows would be needed).
 *
 * At any stage, "i + 1" denotes the length of the current substring of
 * s1 that the distance is calculated for.
 *
 * row2 holds the current row, row1 the previous row (i.e. for the substring
 * of s1 of length "i"), and row0 the row before that.
 *
 * In other words, at the start of the big loop, row2[j + 1] contains the
 * Damerau-Levenshtein distance between the substring of s1 of length
 * "i" and the substring of s2 of length "j + 1".
 *
 * All the big loop does is determine the partial minimum-cost paths.
 *
 * It does so by calculating the costs of the path ending in characters
 * i (in s1) and j (in s2), respectively, given that the last
 * operation is a substitution, a swap, a deletion, or an insertion.
 *
 * This implementation allows the costs to be weighted:
 *
 * - w (as in "sWap")
 * - s (as in "Substitution")
 * - a (for insertion, AKA "Add")
 * - d (as in "Deletion")
 * - b (as in "insertion at the Beginning")
 * - e (as in "insertion at the End")
 *
 * Note that this algorithm calculates a distance _iff_ d == a.
 */
public class Levenshtein {
	protected int w, s, a, d, b, e;

	public Levenshtein(int swap, int substitute, int add, int delete,
			int begin, int end) {
		w = swap;
		s = substitute;
		a = add;
		d = delete;
		b = begin;
		e = end;
	}

	protected static boolean equals(String s1, int index1,
			String s2, int index2) {
		return s1.charAt(index1) == s2.charAt(index2);
	}

	protected static int different(String s1, int index1,
			String s2, int index2) {
		return s1.charAt(index1) == s2.charAt(index2) ? 0 : 1;
	}

	public int cost(String s1, String s2) {
		int len1 = s1.length(), len2 = s2.length();
		int[] row0 = new int[len2 + 1];
		int[] row1 = new int[len2 + 1];
		int[] row2 = new int[len2 + 1];

		for (int j = 0; j <= len2; j++)
			row1[j] = Math.min(j * Math.min(a, b), (len2 - j) * e);
		for (int i = 0; i < len1; i++) {
			row2[0] = (i + 1) * d;
			for (int j = 0; j < len2; j++) {
				/* substitution */
				row2[j + 1] = row1[j]
					+ s * different(s1, i, s2, j);
				/* swap */
				if (i > 0 && j > 0 &&
						equals(s1, i - 1, s2, j) &&
						equals(s1, i, s2, j - 1) &&
						row2[j + 1] > row0[j - 1] + w)
					row2[j + 1] = row0[j - 1] + w;
				/* deletion */
				if (row2[j + 1] > row1[j + 1] + d)
					row2[j + 1] = row1[j + 1] + d;
				/* insertion */
				if (row2[j + 1] > row2[j] + a)
					row2[j + 1] = row2[j] + a;
			}

			int[] dummy = row0;
			row0 = row1;
			row1 = row2;
			row2 = dummy;
		}

		return row1[len2];
	}

	public static void main(String[] args) {
		Levenshtein levenshtein = new Levenshtein(
			args.length > 2 ? Integer.parseInt(args[0]) : 0,
			args.length > 3 ? Integer.parseInt(args[0]) : 10,
			args.length > 4 ? Integer.parseInt(args[0]) : 1,
			args.length > 5 ? Integer.parseInt(args[0]) : 5,
			args.length > 6 ? Integer.parseInt(args[0]) : 0,
			args.length > 7 ? Integer.parseInt(args[0]) : 0
		);
		System.out.println("distance between "
				+ args[0] + " and " + args[1] + " is "
				+ levenshtein.cost(args[0], args[1]));
	}
}
