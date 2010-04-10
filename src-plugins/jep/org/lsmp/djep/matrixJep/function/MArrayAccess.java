/*
Created 17-May-2006 - Richard Morris
*/
package org.lsmp.djep.matrixJep.function;

import org.lsmp.djep.xjep.PrintVisitor;

import org.lsmp.djep.vectorJep.function.ArrayAccess;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

public class MArrayAccess extends ArrayAccess implements PrintVisitor.PrintRulesI{

	public void append(Node node, PrintVisitor pv) throws ParseException {
		// TODO Auto-generated method stub
		//pv.append("[");
		node.jjtGetChild(0).jjtAccept(pv, null);
		node.jjtGetChild(1).jjtAccept(pv, null);
	}

	public MArrayAccess() {
		super();
		// TODO Auto-generated constructor stub
	}

}
