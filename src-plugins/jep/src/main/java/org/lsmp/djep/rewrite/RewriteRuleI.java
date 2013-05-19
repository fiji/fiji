/* @author rich
 * Created on 01-Oct-2004
 */
package org.lsmp.djep.rewrite;
import org.nfunk.jep.*;
/**
 * @author Rich Morris
 * Created on 01-Oct-2004
 */
public interface RewriteRuleI {
    /** Returns true if node needs to be rewritten, according to this rule. */
	public boolean test(ASTFunNode node,Node children[]);
	/** Rewrites the node */
	public Node apply(ASTFunNode node,Node children[]) throws ParseException;
}
