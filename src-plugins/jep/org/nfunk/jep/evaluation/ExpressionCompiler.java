/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/


package org.nfunk.jep.evaluation;

import org.nfunk.jep.*;
import java.util.*;


public class ExpressionCompiler implements ParserVisitor {
	/** Commands */
	private Vector commands;
		
	public ExpressionCompiler() {
		commands = new Vector();
	}
	
	public CommandElement[] compile(Node node) throws ParseException{
		commands.removeAllElements();
		node.jjtAccept(this, null);
		CommandElement[] temp = new CommandElement[commands.size()];
		Enumeration en = commands.elements();
		int i = 0;
		while (en.hasMoreElements()) {
			 temp[i++] = (CommandElement)en.nextElement();
		}
		return temp;
	}

	public Object visit(ASTFunNode node, Object data) throws ParseException {
		node.childrenAccept(this,data);
		
		CommandElement c = new CommandElement();
		c.setType(CommandElement.FUNC);
		c.setPFMC(node.getPFMC());
		c.setNumParam(node.jjtGetNumChildren());
		commands.addElement(c);
		
		return data;
	}

	public Object visit(ASTVarNode node, Object data) {
		CommandElement c = new CommandElement();
		c.setType(CommandElement.VAR);
		c.setVarName(node.getName());
		commands.addElement(c);

		return data;
	}

	public Object visit(ASTConstant node, Object data) {
		CommandElement c = new CommandElement();
		c.setType(CommandElement.CONST);
		c.setValue(node.getValue());
		commands.addElement(c);

		return data;
	}
	
	public Object visit(SimpleNode node, Object data) {
		return data;
	}
	
	public Object visit(ASTStart node, Object data) {
		return data;
	}
}
