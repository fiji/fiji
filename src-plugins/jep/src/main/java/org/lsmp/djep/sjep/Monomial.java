/* @author rich
 * Created on 14-Dec-2004
 */
package org.lsmp.djep.sjep;
import org.nfunk.jep.*;
/**
 * Represents an imutable monomial a x^i * y^j * ... * z^k, a constant.
 * 
 * @author Rich Morris
 * Created on 14-Dec-2004
 */
public class Monomial extends AbstractPNode {

	PConstant coeff;
	PNodeI   vars[];
	PNodeI	 powers[];
	/**
	 * 
	 */
	Monomial(PolynomialCreator pc,PConstant coeff,PNodeI vars[],PNodeI powers[]) {
		super(pc);
		if(vars.length != powers.length)
			throw new IllegalArgumentException("Monomial.valueOf length of variables and powers must be equal. they are "+vars.length+" "+powers.length);
		this.coeff = coeff;
		this.vars = vars;
		this.powers = powers;
	}

	Monomial(PolynomialCreator pc,PConstant coeff,PNodeI var) {
		super(pc);
		this.coeff = coeff;
		this.vars = new PNodeI[]{var};
		this.powers = new PNodeI[]{pc.oneConstant};
	}

	Monomial(PolynomialCreator pc,PConstant coeff,PNodeI var,PNodeI power) {
		super(pc);
		this.coeff = coeff;
		this.vars = new PNodeI[]{var};
		this.powers = new PNodeI[]{power};
	}

	PNodeI valueOf(PConstant coefficient,PNodeI terms[],PNodeI pows[])
	{
		if(coefficient.isZero()) return pc.zeroConstant;
		if(terms.length ==0) return coefficient;
		return new Monomial(pc,coefficient,terms,pows);
	}

	MutiableMonomial toMutiableMonomial()
	{
		PNodeI newTerms[] = new PNodeI[vars.length];
		PNodeI newPows[] = new PNodeI[vars.length];
		for(int i=0;i<vars.length;++i){
			newTerms[i] = vars[i];
			newPows[i] = powers[i];
		}
		return new MutiableMonomial(pc,coeff,newTerms,newPows);
	}
	
	public PNodeI mul(PNodeI node) throws ParseException
	{
		if(node instanceof PConstant)
			return this.valueOf((PConstant) coeff.mul(node),vars,powers);

		if(node instanceof Monomial)
			return mul((Monomial) node);
		
		MutiableMonomial mm = this.toMutiableMonomial();
		mm.mul(node,pc.oneConstant);
		return mm.toPNode();				
	}

	public PNodeI div(PNodeI node) throws ParseException
	{
		if(node instanceof PConstant)
			return this.valueOf((PConstant) coeff.div(node),vars,powers);

		if(node instanceof Monomial)
			return div((Monomial) node);
		
		MutiableMonomial mm = this.toMutiableMonomial();
		mm.mul(node,pc.minusOneConstant);
		return mm.toPNode();				
	}

	PNodeI mul(Monomial m) throws ParseException
	{
		MutiableMonomial mm = this.toMutiableMonomial();
		mm.mul(m.coeff);
		for(int i=0;i<m.vars.length;++i)
			mm.mul(m.vars[i],m.powers[i]);
		return mm.toPNode();
	}
	
	PNodeI div(Monomial m) throws ParseException
	{
		MutiableMonomial mm = this.toMutiableMonomial();
		mm.div(m.coeff);
		for(int i=0;i<vars.length;++i)
			mm.mul(m.vars[i],m.powers[i].negate());
		return mm.toPNode();
	}
	
	public PNodeI pow(PNodeI pow) throws ParseException
	{
		if(pow instanceof PConstant)
		{
			MutiableMonomial mm = this.toMutiableMonomial();
			mm.power((PConstant) pow);
			return mm.toPNode();			
		}
		return super.pow(pow);
	}

	public PNodeI negate() throws ParseException
	{
		return new Monomial(pc,(PConstant) coeff.negate(),vars,powers);
	}
	
	public PNodeI invert() throws ParseException
	{
		PNodeI newPows[] = new PNodeI[vars.length];
		for(int i=0;i<vars.length;++i)
			newPows[i] = powers[i].negate();
		return new Monomial(pc,(PConstant) coeff.invert(),vars,newPows);
	}
	
	public PNodeI add(PNodeI node) throws ParseException
	{
		if(node instanceof PVariable)
		{
			if(this.equalsIgnoreConstant(node))
			{
				return valueOf((PConstant)coeff.add(pc.oneConstant),
						vars,powers);		
			}
		}
		if(node instanceof Monomial)
		{
			Monomial mon = (Monomial) node;
			if(this.equalsIgnoreConstant(mon))
			{
				return valueOf((PConstant)coeff.add(mon.coeff),
					vars,powers);
			}
		}
		return super.add(node);
	}

	public PNodeI sub(PNodeI node) throws ParseException
	{
		if(node instanceof PVariable)
		{
			if(this.equalsIgnoreConstant(node))
			{
				return valueOf((PConstant)coeff.sub(pc.oneConstant),
						vars,powers);		
			}
		}
		if(node instanceof Monomial)
		{
			Monomial mon = (Monomial) node;
			if(this.equalsIgnoreConstant(mon))
			{
				return valueOf((PConstant)coeff.sub(mon.coeff),
					vars,powers);
			}
		}
		return super.sub(node);
	}
	
	PNodeI addConstant(PConstant c) throws ParseException
	{
		return valueOf((PConstant) coeff.add(c),vars,powers);
	}
	//////////////////// Comparison functions
	
	public boolean equals(PNodeI node)
	{
		if(!(node instanceof Monomial)) return false;
		if(!coeff.equals(((Monomial) node).coeff)) return false;
		return equalsIgnoreConstant((Monomial) node);
	}
	
	boolean equalsIgnoreConstant(Monomial mon)
	{
		if(vars.length != mon.vars.length) return false;
		for(int i=0;i<vars.length;++i)
		{
			if(!vars[i].equals(mon.vars[i])) return false;
			if(!powers[i].equals(mon.powers[i])) return false;
		}
		return true;
	}

	boolean equalsIgnoreConstant(PNodeI node)
	{
		if(node instanceof Monomial)
			return equalsIgnoreConstant((Monomial) node);
			
		if(vars.length != 1) return false;
		if(!vars[0].equals(node)) return false;
		if(!powers[0].isOne()) return false;
		return true;
	}
	/** Compare this to argument.
	 * x < y
	 * 2 x < 3 x
	 * x < x^2
	 * x^2 < x^3
	 * x < x y
	 * TODO x y < x^2
	 * 
	 * @return this < arg ---> -1,	this > arg ---> 1
	 */

	public int compareTo(PNodeI node)
	{
		if(node instanceof PConstant) return 1;
		if(node instanceof Monomial)
		{
			Monomial mon = (Monomial) node;
			for(int i=0;i<vars.length;++i)
			{
				if(i>=mon.vars.length) return 1;
				int res = vars[i].compareTo(mon.vars[i]);
				if(res!=0) return res;
				res = powers[i].compareTo(mon.powers[i]);
				if(res!=0) return res;
			}
			if(vars.length > mon.vars.length) return 1;
			if(vars.length < mon.vars.length) return -1;
			return coeff.compareTo(mon.coeff);
		}
		// compare with first term
		int res = vars[0].compareTo(node);
		if(res==0)
			res = powers[0].compareTo(pc.oneConstant);
		return res;
	}
	
	private boolean negativePower(PNodeI pow) {
		return( pow instanceof PConstant
		 && ((PConstant) pow).isNegative()); 

	}
	private void printPower(StringBuffer sb,PNodeI pow)
	{
		if(pow.isOne()) return;
		if(pow instanceof PConstant 
			|| pow instanceof PVariable 
			|| pow instanceof PFunction)
		{	
			sb.append('^');
			sb.append(pow.toString());	
		}
		else
		{
			sb.append("^(");
			sb.append(pow.toString());	
			sb.append(")");
		}
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer();

		boolean flag = false;
		if( coeff.isMinusOne())
			sb.append('-');
		else if( coeff.isOne()) {	}
		else {
			sb.append(coeff.toString());
			flag = true;
		}
		// first print positive and complicated powers
		int numNeg = 0;
		for(int i=0;i<vars.length;++i)
		{
			if( negativePower(powers[i])) { ++numNeg; continue;} 

			if(flag)
				sb.append('*');
			if(vars[i] instanceof Polynomial)
			{
				sb.append('(');
				sb.append(vars[i].toString());
				sb.append(')');
			}
			else
				sb.append(vars[i].toString());
			printPower(sb,powers[i]);
			flag = true; 
		}
		// now negative powers
		if(numNeg >0)
		{
			if(!flag) sb.append('1');
			if(numNeg > 1) sb.append("/(");
			else		sb.append("/");
			flag = false;
			for(int i=0;i<vars.length;++i)
			{
				if( negativePower(powers[i]) )
				{
					if(flag)
						sb.append('*');
					if(vars[i] instanceof Polynomial)
					{
						sb.append('(');
						sb.append(vars[i].toString());
						sb.append(')');
					}
					else
						sb.append(vars[i].toString());
					try {
						printPower(sb,powers[i].negate());
					} catch(ParseException e) {
						throw new IllegalStateException(e.getMessage());
					}
					flag = true; 
				}
			}
			if(numNeg > 1) sb.append(")");
		}
		
		return sb.toString();
	}
	
	public Node toNode() throws ParseException
	{
		int nCoeff = coeff.isOne() ? 0 : 1;

		int numDivisors = 0;
		for(int i=0;i<vars.length;++i)
			if(negativePower(powers[i]))
				++numDivisors;

		Node args[] = new Node[nCoeff+vars.length-numDivisors];
		int pos=0;
		if(nCoeff>0)
			args[pos++]=coeff.toNode();

		for(int i=0;i<vars.length;++i)
		{
			if(negativePower(powers[i])) continue;
			if(powers[i].isOne())
				args[pos++]=vars[i].toNode();
			else
				args[pos++] = pc.nf.buildOperatorNode(pc.os.getPower(),
					vars[i].toNode(),powers[i].toNode());
		}
		Node top;
		if(args.length==0) top = coeff.toNode();
		else if(args.length==1) top = args[0];
		else top = pc.nf.buildOperatorNode(
			pc.os.getMultiply(),args);
		
		if(numDivisors == 0) return top;
		
		Node divisors[] = new Node[numDivisors];
		pos = 0;
		for(int i=0;i<vars.length;++i)
		{
			if(negativePower(powers[i]))
			{
				PNodeI pow = powers[i].negate();
				if(powers[i] instanceof PConstant && ((PConstant) powers[i]).isMinusOne())
					divisors[pos++]=vars[i].toNode();
				else
					divisors[pos++] = pc.nf.buildOperatorNode(pc.os.getPower(),
						vars[i].toNode(),pow.toNode());
			}
		}
		Node bottom; 
		if(divisors.length==1) bottom = divisors[0];
		else bottom = pc.nf.buildOperatorNode(
			pc.os.getMultiply(),divisors);
		return pc.nf.buildOperatorNode(pc.os.getDivide(),top,bottom);
	}
	
	boolean negativeCoefficient()
	{
		return coeff.isNegative();
	}
	
	public PNodeI expand() throws ParseException
	{ 
		MutiablePolynomial mp = new MutiablePolynomial(pc,new PNodeI[]{this.coeff});
		for(int i=0;i<vars.length;++i)
		{
			if(powers[i] instanceof PConstant)
			{
				PConstant pow = (PConstant) powers[i];
				if(pow.isZero()) {}
				else if(pow.isOne())
					mp.expandMul(vars[i].expand());
				else if(pow.isInteger())
				{
					int intpow = pow.intValue();
					if(intpow >0)
					{
						PNodeI res = vars[i].expand();
						for(int j=1;j<=intpow;++j)
							mp.expandMul(res);	
					}
					else
						mp.expandMul(new Monomial(pc,pc.oneConstant,vars[i].expand(),powers[i]));
				}
			}
			else
				mp.expandMul(new Monomial(pc,pc.oneConstant,vars[i].expand(),powers[i]));
		}
		return mp.toPNode();		
	}

}
