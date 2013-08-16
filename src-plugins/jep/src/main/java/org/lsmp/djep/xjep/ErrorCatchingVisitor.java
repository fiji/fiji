/* @author Rich Morris
 * Created on 19-Jun-2003
 */
package org.lsmp.djep.xjep;
//import org.lsmp.djep.matrixParser.*;
import org.nfunk.jep.*;

/**
 * An abstract ParserVisitor
 * which adds some useful error handling facilities.
 * Visitors which require these facilities should extend this class.
 * General format should be
 * <pre>  
 * 	clearErrors();
 *	Object res = (Node) node.jjtAccept(this,data);
 *	if(hasErrors())
 *		throw new ParseException(getErrors());
 *</pre>
 * @author Rich Morris
 * Created on 19-Jun-2003
 */
abstract public class ErrorCatchingVisitor extends DoNothingVisitor
{
	/** The current error list. */
	private Exception error=null;

	/** calls jjtAccept inside a try catch block, adding the error if necessary */
	public Object acceptCatchingErrors(Node node,Object data)
	{
		Object res=null;
		clearErrors();
		try
		{
			res = node.jjtAccept(this,data);
		}
		catch (ParseException e) { addError(e); }
		return res;
	}
	/** Reset the list of errors. */
	public void clearErrors() {	error = null;	}

	/** Are their any errors? */	
	public boolean hasErrors() { return error != null; }
	
	/** Adds an error message to the list of errors. */
	public void addError(Exception e) 	{error = e;	}

	/** Returns the error messages.	 */
	public String getErrorsMessage() {
		if(error==null) return null;
		return error.getMessage();
	}
	/** Returns the Exception or null if no error. */
	public Exception getError() { return error; }
}
