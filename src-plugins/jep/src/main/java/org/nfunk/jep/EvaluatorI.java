/* @author rich
 * Created on 22-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.nfunk.jep;

/**
 * @author Rich Morris
 * Created on 22-Apr-2005
 */
public interface EvaluatorI {
	/*
	 * The following methods was used to facilitate 
	 * using visitors which implemented a interface
	 * which sub-classed ParserVisitor.
	 *  
	 * If sub-classed to extend to implement a different visitor
	 * this method should be overwritten to ensure the correct 
	 * accept method is called.
	 * This method simply calls the jjtAccept(ParserVisitor this,Object data) of node.
	 *
	 * We no longer need this as we use ParserVisitor everywhere,
	 * but kept for future reference.
	 * 
	 private Object nodeAccept(Node node, Object data) throws ParseException
	 {
	 return node.jjtAccept(this,data);
	 }
	 */
	
	/**
	 * Evaluates a node and returns and object with the value of the node.
	 * 
	 * @throws ParseException if errors occur during evaluation;
	 */
	public abstract Object eval(Node node) throws ParseException;
}
