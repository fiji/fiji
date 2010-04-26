/* @author rich
 * Created on 09-Mar-2004
 */
package org.lsmp.djep.groupJep.values;

import org.lsmp.djep.groupJep.groups.AlgebraicExtension;

/**
 * An element of the algrabraic extension K(t).
 * a0 + a1 t + a(n-1) t^(n-1) 
 * where t is defined to be the the solution of a polynomial equation.
 *
 * @see AlgebraicExtension
 * @author Rich Morris
 * Created on 09-Mar-2004
 */
public class AlgebraicExtensionElement extends FreeGroupElement {

	AlgebraicExtension ae;

	/**
	 * An element of the algebraic extension K(t).
	 * a0 + a1 t + a(n-1) t^(n-1) 
	 * where t is defined to be the the solution of a polynomial equation.
	 * If the degree of the polynomial specified by coeffs is greater
	 * than n then the polynomial will be reduced by using
	 * the equation t^n = ..... 
	 * @param K the algebraic extension.
	 * @param coeffs array of coefficients for this algebraic number. c0 + c1 t + ... + cn t^n 
	 */
	public AlgebraicExtensionElement(AlgebraicExtension K, Number coeffs[])
	{
		super(K,coeffs);
		this.ae = K;
		int deg_p = ae.getPoly().getDegree();
		while(this.getCoeffs().length > deg_p)
		{
			Polynomial poly2 = ae.getSubsPoly();
			int deg_c = this.getCoeffs().length-1;
			// coeffs = (a_m s^(m-n)+...+a_n) s^n + (a_(n-1) s^(n-1)+...+a_0)
			//        = p1 * s^n + p2;
			//        = p2 - p1 * q;
			Number p1Coeffs[] = new Number[deg_c-deg_p+1];
			Number p2Coeffs[] = new Number[deg_p];
			System.arraycopy(this.getCoeffs(),deg_p,p1Coeffs,0,deg_c-deg_p+1);
			System.arraycopy(this.getCoeffs(),0,p2Coeffs,0,deg_p);
			Polynomial p1 = new Polynomial(ae.getBaseRing(),ae.getPoly().getSymbol(),p1Coeffs);
			Polynomial p2 = new Polynomial(ae.getBaseRing(),ae.getPoly().getSymbol(),p2Coeffs);
			Polynomial p3 = p1.mul(poly2);
			Polynomial p4 = p3.add(p2);
			super.setCoeffs(p4.getCoeffs());
		}
	}

	/** sub classes should overwrite this to make the correct type. */
	protected Polynomial valueOf(Number lcoeffs[])
	{
		AlgebraicExtensionElement g = new AlgebraicExtensionElement(ae,lcoeffs);
		return g;
	}
}
