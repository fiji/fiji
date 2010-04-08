/* @author rich
 * Created on 15-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.jama;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;
import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.*;
import org.lsmp.djep.vectorJep.function.*;

/**
 * z = solve(x,y) solves x*z = y where x,y,z are real matricies.
 * Serves a wrapper around the Jama linear algebra function.
 * @see <a href="http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a>
 * 
 * @author Rich Morris
 * Created on 15-Feb-2005
 */
public class Solve extends PostfixMathCommand implements BinaryOperatorI 
{
	public Solve()
	{
		this.numberOfParameters = 2;
	}


	public void run(Stack s) throws ParseException
	{
		Object r = s.pop();
		if(!(r instanceof Matrix))
			throw new ParseException("solve: can only be applied to a matrix");
		Object l = s.pop();
		if(!(l instanceof Matrix))
			throw new ParseException("solve: can only be applied to a matrix");
		Jama.Matrix m = JamaUtil.toJama((Matrix) l);
		Jama.Matrix b = JamaUtil.toJama((Matrix) r);
		Jama.Matrix solve = m.solve(b);
		Matrix res = JamaUtil.fromJama(solve);
		s.push(res);
	}

	public Dimensions calcDim(Dimensions ldim,Dimensions rdim)
	{
		int rows = ldim.getLastDim();
		int cols = rdim.getLastDim();
		return Dimensions.valueOf(rows,cols);
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs,MatrixValueI rhs)
		throws ParseException
	{
		if(!(lhs instanceof Matrix))
			throw new ParseException("solve: can only be applied to a matrix");
		if(!(res instanceof Matrix))
			throw new ParseException("inverse: result should be a matrix");
		Jama.Matrix m = JamaUtil.toJama((Matrix) lhs);
		Jama.Matrix b = JamaUtil.toJama((Matrix) rhs);
		Jama.Matrix solve = m.solve(b);
		JamaUtil.fromJama(solve,(Matrix) res);
		return res;
	}

}
