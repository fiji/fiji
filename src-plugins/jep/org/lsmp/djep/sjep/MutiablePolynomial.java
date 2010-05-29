/* @author rich
 * Created on 23-Dec-2004
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.sjep;
import org.nfunk.jep.*;
/**
 * A mutable polynomial representing a + b + c.
 * There are no requirements that this is in a reduced form
 * so some powers can be zero.
 * 
 * @author Rich Morris
 * Created on 23-Dec-2004
 */
public class MutiablePolynomial
{
	PolynomialCreator pc;
	PNodeI terms[];
	/**
	 * Note arrays parsed may be modified.
	 */
	public MutiablePolynomial(PolynomialCreator pc,PNodeI nodes[])
	{
		this.pc = pc;
		terms = nodes;
	}

	public void add(PNodeI term) throws ParseException
	{
		if(term instanceof PConstant)
			for(int i=0;i<terms.length;++i)
				if(terms[i] instanceof PConstant) {
					terms[i] = terms[i].add(term);
					return;
				}				

		if(term instanceof Polynomial)
		{
			Polynomial p = (Polynomial) term;
			for(int i=0;i<p.terms.length;++i)
				add(p.terms[i]);
			return;
		}
		for(int i=0;i<terms.length;++i) {
			if(terms[i] instanceof Monomial) {
				if(((Monomial) terms[i]).equalsIgnoreConstant(term)){
					terms[i] = terms[i].add(term);
					return;		
				}
			}
			else if(terms[i].equals(term)) {
				terms[i] = terms[i].add(term);
				return;
			}
		}
		// insert in correct posn
		PNodeI newTerms[] = new PNodeI[terms.length+1]; 
		int pos=0; boolean done = false;
		for(int i=0;i<terms.length;++i) {
			if(!done && terms[i].compareTo(term) > 0) {
		 		newTerms[pos] = term;
		 		++pos;
		 		done = true;
			}
			newTerms[pos] = terms[i];
			++pos;
		}
		if(!done)
		{
			newTerms[pos] = term;
			++pos;
		}
		terms = newTerms;
	}

	/**
	 * Multiplies this by a polynomial and expands the results.
	 * (1+x)*(1+x) --> 1+2*x+x^2
	 * @param p the polynomial to multiply by
	 * @throws ParseException
	 */
	void expandMul(Polynomial p) throws ParseException
	{
		PNodeI newTerms[][] = new PNodeI[terms.length][p.terms.length];
		for(int i=0;i<terms.length;++i)
			for(int j=0;j<p.terms.length;++j)
				newTerms[i][j] = terms[i].mul(p.terms[j]);
		int oldLen = terms.length;
		terms = new PNodeI[0];
		for(int i=0;i<oldLen;++i)
			for(int j=0;j<p.terms.length;++j)
				add(newTerms[i][j]);		
	}
	/**
	 * Multiplies this by a node and expands the results.
	 * (1+x)*(1+x) --> 1+2*x+x^2
	 * @param node
	 * @throws ParseException
	 */
	void expandMul(PNodeI node) throws ParseException
	{	
		if(node instanceof Polynomial)	{
			expandMul((Polynomial) node);
			return;
		}
		PNodeI newTerms[] = new PNodeI[terms.length];
		for(int i=0;i<terms.length;++i)
			newTerms[i] = terms[i].mul(node);
		terms = new PNodeI[0];
		for(int i=0;i<newTerms.length;++i)
			add(newTerms[i]);
	}
	/** removes terms like x^0, 1^x, 2^3 */
	private void reduce() throws ParseException
	{
		int numZeros=0;
		int numConst=0;
		PConstant c = pc.zeroConstant;
		for(int i=0;i<terms.length;++i)
		{
			if(terms[i].isZero()) ++numZeros;
			else if(terms[i] instanceof PConstant)
			{
				++numConst;
				c = (PConstant) c.add(terms[i]);
			}
		}
		if(numZeros == 0 && numConst == 0 )
			return;
		int newLen = terms.length-numZeros-numConst;
		if(!c.isZero()) ++newLen;
		PNodeI newTerms[] = new PNodeI[newLen];
		int pos=0;
		if(!c.isZero())
			newTerms[pos++] = c;
		
		for(int i=0;i<terms.length;++i)
		{
			if(terms[i].isZero()) {} // 1^x --> 1
			else if(terms[i] instanceof PConstant) {}
			else {
				newTerms[pos] = terms[i];
				++pos;
			}
		}
		terms = newTerms;
	}
	
	PNodeI toPNode() throws ParseException
	{
		reduce();
		if(terms.length ==0) return pc.zeroConstant;
		if(terms.length == 1) return terms[0];
		return new Polynomial(pc,terms);
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<terms.length;++i){
			if(i>0) sb.append('+');
			sb.append(terms[i].toString());
		}
		return sb.toString();
	}
}
