/* @author rich
 * Created on 22-Dec-2004
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.sjep;

import org.nfunk.jep.ParseException;

/**
 * Default methods, when more specific methods do not work. 
 * 
 * @author Rich Morris
 * Created on 22-Dec-2004
 */
public abstract class AbstractPNode implements PNodeI
{
	/** A reference to the PolynomialCreator instance. */
	protected PolynomialCreator pc;
	
	private AbstractPNode() {}
	public AbstractPNode(PolynomialCreator pc)	{
		this.pc = pc;
	}
	/**
	 * 
	 */

	public PNodeI add(PNodeI node) throws ParseException
	{
		if(node.isZero()) return this;
		if(this.isZero()) return node;
		
		if(this.equals(node))
			return new Monomial(pc,pc.twoConstant,this);
			
		if(node instanceof Polynomial)
			return node.add(this);

		if(this.compareTo(node) < 0)
			return new Polynomial(pc,new PNodeI[]{this,node}); // x+y
		return new Polynomial(pc,new PNodeI[]{node,this}); // x+y
	}

	public PNodeI sub(PNodeI node) throws ParseException
	{
		if(node.isZero()) return this;
		if(this.isZero()) return node.negate();

		if(this.equals(node))
			return pc.zeroConstant;
		
		if(node instanceof Polynomial)
			return node.negate().add(this);
		
		if(node instanceof PConstant)
			return this.add(node.negate());
			
		return new Polynomial(pc,new PNodeI[]{this,
				new Monomial(pc,pc.minusOneConstant,node)}); // x-y
	}

	public PNodeI negate() throws ParseException
	{
		return new Monomial(pc,pc.minusOneConstant,this);
	}

	public PNodeI mul(PNodeI node) throws ParseException
	{
		if(node.isZero()) return pc.zeroConstant;
		if(node.isOne())
			return this;

		if(this.equals(node))
			return new Monomial(pc,pc.oneConstant,this,pc.twoConstant);

		if(node instanceof PConstant)
			return new Monomial(pc,(PConstant) node,this);
		
		if(node instanceof Monomial)
			return ((Monomial) node).mul(this);
		

		if(this instanceof PConstant)
		{
//			if(node instanceof Polynomial)
//				return ((Polynomial) node).mul((Constant) this);
			return new Monomial(pc,(PConstant) this,node);
		}
			
		if(this.compareTo(node) < 0)
			return new Monomial(pc,
				pc.oneConstant,
				new PNodeI[]{this,node},
				new PNodeI[]{pc.oneConstant,pc.oneConstant});
		
		return new Monomial(pc,
			pc.oneConstant,
			new PNodeI[]{node,this},
			new PNodeI[]{pc.oneConstant,pc.oneConstant});
	}

	public PNodeI div(PNodeI node) throws ParseException
	{
		if(this.equals(node))
			return pc.oneConstant;
		if(node.isZero())
			return pc.infConstant;
		if(node.isOne())
			return this;
	
		if(node instanceof Monomial)
			return ((Monomial) node).invert().mul(this);

		if(this instanceof PConstant)
			return new Monomial(pc,(PConstant) this,node,pc.minusOneConstant);
		
		if(node instanceof PConstant)
			return new Monomial(pc,(PConstant) node.invert(),this);
			
		return new Monomial(pc,
			pc.oneConstant,
			new PNodeI[]{this,node},
			new PNodeI[]{pc.oneConstant,pc.minusOneConstant});
	}

	public PNodeI invert() throws ParseException
	{
		return pow(pc.minusOneConstant); // x^-1
	}

	public PNodeI pow(PNodeI node) throws ParseException
	{
		if(node.isZero()) return pc.oneConstant;
		if(node.isOne()) return this;
		return new Monomial(pc,
			pc.oneConstant,this,node);
	}

	public boolean equals(PNodeI node)	{return false;	}
	public boolean isZero()	{return false;	}
	public boolean isOne()	{return false;	}
	/**
	this < arg ---> -1
	this > arg ---> 1
	*/
	public int compareTo(PNodeI node)	{
		if(this instanceof PConstant)
		{
			if(node instanceof PConstant)
				return ((PConstant) this).compareTo((PConstant) node);
			return -1;
		}
		if(this instanceof PVariable)
		{
			if(node instanceof PConstant) return 1;
			if(node instanceof PVariable)
				return ((PVariable) this).compareTo((PVariable) node);
			if( node instanceof PFunction 
			 || node instanceof POperator)
				return -1;
		}
		if(this instanceof POperator)
		{
			if( node instanceof PConstant
			 || node instanceof PVariable) return 1;
			if( node instanceof POperator)
				return ((POperator) this).compareTo((POperator) node);
			if( node instanceof PFunction)
				return -1;
		}
		if(this instanceof PFunction)
		{
			if( node instanceof PConstant
			 || node instanceof PVariable
			 || node instanceof PFunction) return 1;
			if( node instanceof PFunction)
				return ((PFunction) this).compareTo((PFunction) node);
		}
		if( this instanceof Monomial
		 || this instanceof Polynomial)
			throw new IllegalStateException("Comparison failed "+this.getClass().getName()+" "+node.getClass().getName());
			
		if(node instanceof Monomial)
			return -((Monomial) node).compareTo(this);
		if(node instanceof Polynomial)
			return -((Polynomial) node).compareTo(this);
			
		throw new IllegalArgumentException("Comparison failed "+this.getClass().getName()+" "+node.getClass().getName());
	}

}
