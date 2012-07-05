/* @author rich
 * Created on 24-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.matrixJep.function;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.function.NaryOperatorI;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.lsmp.djep.xjep.function.Sum;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;

/**
 * @author Rich Morris
 * Created on 24-Apr-2005
 */
public class MSum extends Sum implements NaryOperatorI {

	/**
	 * @param j
	 */
	public MSum(JEP j) {
		super(j);
	}

	public Dimensions calcDim(Dimensions[] dims) throws ParseException {
		return dims[0];
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI[] inputs)
			throws ParseException {
		throw new ParseException("calcValue method for MSum called");
	}

}
