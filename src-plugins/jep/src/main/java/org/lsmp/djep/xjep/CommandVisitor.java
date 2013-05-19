/* @author rich
 * Created on 18-Jun-2003
 */

package org.lsmp.djep.xjep;
//import org.lsmp.djep.matrixParser.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.PostfixMathCommandI;

/**
 * Executes commands like diff and eval embedded in expression trees.
 * For example you could do 
 * <pre>eval(diff(x^3,x),x,2)</pre>
 * to differentiate x^3 and then substitute x=2 to get the value 12. 
 * To use do
 * <pre>
 * JEP j = ...; Node in = ...;
 * TreeUtils tu = new TreeUtils(j);
 * CommandVisitor cv = new CommandVisitor(tu);
 * Node out = cv.process(in);
 * </pre>
 * Commands to be executed must implement
 * {@link org.lsmp.djep.xjep.CommandVisitorI CommandVisitorI} and {@link  org.nfunk.jep.function.PostfixMathCommandI PostfixMathCommandI}.
 * See {@link org.lsmp.djep.xjep.Eval Eval} for an example of this. 
 * See {@link org.nfunk.jep.ParserVisitor ParserVisitor} for details on the VisitorPattern.
 * @author R Morris
 * Created on 19-Jun-2003
 */
public class CommandVisitor extends DoNothingVisitor
{
  private XJep xjep;
  /** private default constructor to prevent init without a tree utils
   */
    public CommandVisitor()
  {
  }
  
  /** 
   * Descends the tree processing all diff, eval and simplify options
   */

  public Node process(Node node,XJep xj) throws ParseException
  {
  	this.xjep=xj;
	Node res = (Node) node.jjtAccept(this,null);
	return res;
  }

  public Object visit(ASTFunNode node, Object data) throws ParseException
  {
	Node children[] = acceptChildrenAsArray(node,data);

	PostfixMathCommandI pfmc = node.getPFMC();
	if(pfmc instanceof CommandVisitorI )
	{
		CommandVisitorI com = (CommandVisitorI) pfmc;
		return com.process(node,children,xjep);
	}
	TreeUtils.copyChildrenIfNeeded(node,children);
	return node;
  }
}
