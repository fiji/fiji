/* @author rich
 * Created on 15-Mar-2004
 */
package org.lsmp.djep.groupJep.groups;

import org.lsmp.djep.groupJep.GroupI;
import org.lsmp.djep.groupJep.values.*;
import org.lsmp.djep.groupJep.interfaces.*;

/**
 * The group of permutations.
 * 
 * TODO not sure if this works, not really tested.
 * 
 * @author Rich Morris
 * @see org.lsmp.djep.groupJep.values.Permutation
 * Created on 15-Mar-2004
 */
public class PermutationGroup extends Group implements GroupI , HasListI {

	protected Permutation zeroPerm;
	
	public PermutationGroup(int n)
	{
		Integer perm[] = new Integer[n];
		for(int i=0;i<n;++i)
			perm[i]=new Integer(i+1);
		zeroPerm = new Permutation(this,perm);
	}

	public Number getZERO() {
		return zeroPerm;
	}

	public Number getInverse(Number a) {
		return ((Permutation) a).getInverse();
	}

	public Number add(Number a, Number b) {
		return ((Permutation) a).add((Permutation) b);
	}

	public Number sub(Number a, Number b) {
		return ((Permutation) a).sub((Permutation) b);
	}

	public boolean equals(Number a, Number b) {
		return ((Permutation) a).equals((Permutation) b);
	}

	public Number valueOf(String s) {
		return Integer.valueOf(s);
	}

	public Number valueOf(Number[] eles) {
		
		Integer perm[] = new Integer[eles.length];
		for(int i=0;i<eles.length;++i)
			perm[i]=new Integer(eles[i].intValue());
		Permutation res = new Permutation(this,perm);
		return res;
	}

	public Number list(Number[] eles) {
		return this.valueOf(eles);
	}

}
