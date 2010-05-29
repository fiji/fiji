/* @author rich
 * Created on 23-Dec-2004
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.sjep;
import org.nfunk.jep.*;
/**
 * A mutable monomial representing a * x^i * y^j * ... * z^k.
 * There are no requirements that this is in a reduced form
 * so some powers can be zero.
 * 
 * @author Rich Morris
 * Created on 23-Dec-2004
 */
public class MutiableMonomial
{
	PolynomialCreator pc;
	PConstant coeff;
	int length;
	PNodeI terms[];
	PNodeI powers[];
	/**
	 * Note arrays parsed may be modified.
	 */
	public MutiableMonomial(PolynomialCreator pc,PConstant coeff,PNodeI nodes[],PNodeI pows[])
	{
		this.pc = pc;
		this.coeff = coeff;
		length = nodes.length;
		terms = nodes;
		powers = pows;
	}

	public void mul(PConstant c) throws ParseException
	{
		coeff = (PConstant) coeff.mul(c);
	}
	
	public void div(PConstant c) throws ParseException
	{
		coeff = (PConstant) coeff.div(c);
	}

	public void mul(PNodeI term,PNodeI power) throws ParseException
	{
		for(int i=0;i<length;++i) {
			if(terms[i].equals(term)) {
				powers[i] = powers[i].add(power);
				return;
			}
		}
		// insert in correct posn
		PNodeI newTerms[] = new PNodeI[length+1]; 
		PNodeI newPowers[] = new PNodeI[length+1];
		int pos=0; boolean done = false;
		for(int i=0;i<length;++i) {
			if(!done && terms[i].compareTo(term) > 0) {
		 		newTerms[pos] = term;
		 		newPowers[pos] = power;
		 		++pos;
		 		done = true;
			}
			newTerms[pos] = terms[i];
			newPowers[pos] = powers[i];
			++pos;
		}
		if(!done)
		{
			newTerms[pos] = term;
			newPowers[pos] = power;
			++pos;
		}
		length = length+1;
		terms = newTerms;
		powers = newPowers;
	}
	
	void power(PConstant c) throws ParseException
	{
		coeff = (PConstant) coeff.pow(c);
		for(int i=0;i<length;++i)
			powers[i] = powers[i].mul(c);
	}
	/** removes terms like x^0, 1^x, 2^3 */
	private void reduce() throws ParseException
	{
		int numZeros=0;
		int numOnes=0;
		int numConst=0;
		for(int i=0;i<length;++i)
		{
			if(powers[i].isZero()) ++numZeros;
			else if(terms[i].isOne()) ++numOnes;
			else if(terms[i] instanceof PConstant && powers[i] instanceof PConstant)
				++numConst;
		}
		if(numZeros == 0 && numOnes ==0 && numConst == 0)
			return;
		int newLen = length-numZeros-numOnes-numConst;

		PNodeI newTerms[] = new PNodeI[newLen];
		PNodeI newPowers[] = new PNodeI[newLen];
		int pos=0;
		for(int i=0;i<length;++i)
		{
			if(powers[i].isZero()) {} // x^0 --> 1
			else if(terms[i].isOne()) {} // 1^x --> 1
			else if(terms[i] instanceof PConstant && powers[i] instanceof PConstant)
			{
				coeff = (PConstant) coeff.mul(terms[i].pow(powers[i]));
			}
			else {
				newTerms[pos] = terms[i];
				newPowers[pos] = powers[i];
				++pos;
			}
		}
		length = newLen;
		terms = newTerms;
		powers = newPowers;
	}
	
	PNodeI toPNode() throws ParseException
	{
		reduce();
		if(length ==0) return coeff;
		if(coeff.isZero()) return pc.zeroConstant;

		return new Monomial(pc,coeff,terms,powers);
	}

	public String toString()
	{	
		StringBuffer sb = new StringBuffer();
		sb.append(coeff.toString());
		for(int i=0;i<length;++i)
		{
			sb.append(terms[i]);
			sb.append('^');
			sb.append(powers[i]);
		}
		return sb.toString();
	}

}
