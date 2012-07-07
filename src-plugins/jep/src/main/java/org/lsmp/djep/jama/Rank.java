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
 * Find the rank of a matrix.
 * Serves a wrapper around the Jama linear algebra function.
 * @see <a href="http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a>
 * 
 * @author Rich Morris
 * Created on 15-Feb-2005
 */
public class Rank extends PostfixMathCommand implements UnaryOperatorI 
{
	public Rank()
	{
		this.numberOfParameters = 1;
	}


	public void run(Stack s) throws ParseException
	{
		Object o = s.pop();
		if(!(o instanceof Matrix))
			throw new ParseException("inverse: can only be applied to a matrix");
		Jama.Matrix m = JamaUtil.toJama((Matrix) o);
		int rank = m.rank();
		s.push(new Integer(rank));
	}

	public Dimensions calcDim(Dimensions ldim)
	{
		return Dimensions.ONE;
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs)
		throws ParseException
	{
		if(!(lhs instanceof Matrix))
			throw new ParseException("inverse: can only be applied to a matrix");
		if(!(res instanceof Matrix))
			throw new ParseException("inverse: result should be a matrix");
		Jama.Matrix m = JamaUtil.toJama((Matrix) lhs);
		int rank = m.rank();
		res.setEle(0,new Integer(rank));
		return res;
	}

}
