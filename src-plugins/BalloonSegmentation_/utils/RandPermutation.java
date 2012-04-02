package utils;
/*********************************************************************
 * Version: January, 2008
 ********************************************************************/

/*********************************************************************
 * Lionel Dupuy
 * SCRI
 * EPI program
 * Invergowrie
 * DUNDEE DD2 5DA
 * Scotland
 * UK
 *
 * Lionel.Dupuy@scri.ac.uk
 * http://www.scri.ac.uk/staff/lioneldupuy
 ********************************************************************/


public // Generating random permutations.
// used because for searching division planes through neighbouring relation
// uses rendomness because we are not searching through the entire set of combinations
class RandPermutation {
    private static int[] source;

    // constructor: generate the ordered list to be permutation
    public RandPermutation(int n)
    {
	source = new int[n];
	for (int i=0;i<n;i++)
	{
		source[i] = i;
	}
    }


    // It produces the next random permutation
    public int[] next(){
		int[] b = (int[])source.clone();
		for (int k = b.length - 1; k > 0; k--) {
		    int w = (int)Math.floor(Math.random() * (k+1));
		    int temp = b[w];
		    b[w] = b[k];
		    b[k] = temp;
		}
		return b;
    }
}
