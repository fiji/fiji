/* @author rich
 * Created on 01-Oct-2004
 */
package org.lsmp.djep.xjep;
import java.util.*;
/**
 * @author Rich Morris
 * Created on 01-Oct-2004
 */
public class XMath {

	static class LongPair { 
		long a,b;
		public LongPair(long x,long y) { a=x; b=y;}
		long x() { return a; }
		long y() { return b; }
		public boolean equals(Object o) {
			if(!( o instanceof LongPair)) return false;
			LongPair p = (LongPair) o;
			return (a == p.a) && (b == p.b); 
		}

		public int hashCode() {
			int result = 17;
			int xi = (int)(a^(a>>32));
			int yi = (int)(b^(b>>32));
			result = 37*result+xi;
			result = 37*result+yi;
			return result;
		}
		public String toString() { return "("+a+","+b+")"; }
		
	}	
	static Hashtable pascal = new Hashtable();

	public static long binomial(long n,long i)
	{
		if(i==0 || n==i) return 1;
		if(i==1 || n==i-1 ) return n;
		return binomial(new LongPair(n,i));
	}
	
	public static long binomial(LongPair pair)
	{
		Object find = pascal.get(pair);
		if(find == null)
		{
			long l = binomial(pair.x()-1,pair.y()-1);
			long r = binomial(pair.x()-1,pair.y());
			pascal.put(pair,new Long(l+r));
			return l+r; 
		}
		return ((Long) find).longValue();
	}
}
