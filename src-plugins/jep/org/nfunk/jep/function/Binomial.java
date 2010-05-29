/* @author rich
 * Created on 13-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

/**
 * Binomial coeficients: binom(n,i).
 * Requires n,i integers >=0.
 * Often written nCi or column vector (n,i).
 * (n,0) = 1, (n,1) = n, (n,n-1) = n, (n,n) = 1<br>
 * (n,i) = n! / ( i! (n-i)! )<br>
 * Pascals triangle rule: (n,i) = (n-1,i-1) + (n-1,i)<br>
 * Binomial theorem: (a+b)^n = sum (n,i) a^i b^(n-i), i=0..n.
 * <p>
 * For efficiency the binomial coefficients are stored in a static array. 
 * @author Rich Morris
 * Created on 13-Feb-2005
 */
public class Binomial extends PostfixMathCommand
{
	static final int initN = 20;
	static int[][] coeffs = new int[initN+1][];
	/** Static initialiser for binomial coeffs */
	{
		coeffs[0] = new int[1];
		coeffs[0][0] = 1;
		coeffs[1] = new int[2];
		coeffs[1][0] = 1; coeffs[1][1] = 1;
		for(int n=2;n<=initN;++n)
		{
			coeffs[n] = new int[n+1];
			coeffs[n][0] = 1;
			coeffs[n][n] = 1;
			for(int j=1;j<n;++j)
				coeffs[n][j] = coeffs[n-1][j-1]+coeffs[n-1][j];
		}
	}
	/** If necessary expand the table to include coeffs(N,i) */
	static void expand(int N)
	{
		int oldN = coeffs.length-1;
		if(N<=oldN) return;
		int[][] newCoeffs = new int[N+1][];
		for(int i=0;i<=oldN;++i)
			newCoeffs[i] = coeffs[i];
		for(int n=oldN+1;n<=N;++n)
		{
			newCoeffs[n] = new int[n+1];
			newCoeffs[n][0] = 1;
			newCoeffs[n][n] = 1;
			for(int j=1;j<n;++j)
				newCoeffs[n][j] = newCoeffs[n-1][j-1]+newCoeffs[n-1][j];
		}
		coeffs = newCoeffs;
	}
	/**
	 * 
	 */
	public Binomial()
	{
		super();
		this.numberOfParameters = 2;
	}

	public void run(Stack s) throws ParseException
	{
		Object iObj = s.pop();
		Object nObj = s.pop();
		if((!(iObj instanceof Number )) || (!(nObj instanceof Number)))
			throw new ParseException("Binomial: both arguments must be integers. They are "+nObj+"("+nObj.getClass().getName()+") and "+iObj+"("+nObj.getClass().getName()+")");
		int iInt = ((Number) iObj).intValue();
		int nInt = ((Number) nObj).intValue();
		if(nInt < 0 || iInt < 0 || iInt > nInt)
			throw new ParseException("Binomial: illegal values for arguments 0<i<n. They are "+nObj+" and "+iObj);
		
		expand(nInt);
		int res = coeffs[nInt][iInt];
		s.push(new Integer(res));
	}
	/** Returns the binomial coefficients.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if n<0, i<0 or i>n
	 */
	static public int binom(int n,int i) throws ArrayIndexOutOfBoundsException
	{
		expand(n);
		return coeffs[n][i];
	}
}
