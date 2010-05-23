/* @author rich
 * Created on 15-Mar-2004
 */
package org.lsmp.djep.groupJep.values;
import org.lsmp.djep.groupJep.*;
/**
 * @author Rich Morris
 * Created on 15-Mar-2004
 */
public class Permutation extends Number {

	protected GroupI group;
	protected Integer perm[];
	protected int len;
	/**
	 * 
	 */
	public Permutation(GroupI group,Integer perm[]) {
		super();
		this.group = group;
		this.perm = perm;
		this.len = perm.length;
	}

	public Permutation add(Permutation p1)
	{
		Integer res[] = new Integer[p1.perm.length];
		for(int i=0;i<len;++i)
		 	res[i]= p1.perm[this.perm[i].intValue()-1];
		return valueOf(res);
	}

	
	public Permutation getInverse() {
		Integer res[] = new Integer[len];
		for(int i=0;i<len;++i)
			res[this.perm[i].intValue()-1]= new Integer(i+1);
		return valueOf(res);
	}
	
	public Permutation sub(Permutation p1)
	{
		return this.add(p1.getInverse());
	}

	public boolean equals(Permutation p1)
	{
		for(int i=0;i<len;++i)
			if(this.perm[i] != p1.perm[i])
				return false;
		return true;	
	}

	public Permutation valueOf(Integer p[])
	{
		return new Permutation(this.group,p);
	}

	public Permutation valueOf(Number p[])
	{
		Integer res[] = new Integer[p.length];
		for(int i=0;i<p.length;++i)
			res[i]=(Integer) p[i];
		return new Permutation(this.group,res);
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for(int i=0;i<this.perm.length;++i)
		{
			if(i>0) sb.append(",");
			sb.append(this.perm[i].toString());
		}
		sb.append("]");
		return sb.toString();
	}
	/** Just returns 0. Minimal implematation for compatability with Number. */
	public double doubleValue() {return 0;	}
	public float floatValue() {return 0;	}
	public int intValue() {	return 0;	}
	public long longValue() {return 0;	}
}
