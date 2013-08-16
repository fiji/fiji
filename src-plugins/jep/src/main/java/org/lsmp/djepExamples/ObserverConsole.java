/* @author rich
 * Created on 28-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import java.util.Observable;
import java.util.Observer;
import java.util.Enumeration;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.nfunk.jep.Node;
/**
 * @author Rich Morris
 * Created on 28-Mar-2005
 */
public class ObserverConsole extends DJepConsole implements Observer
{
	private static final long serialVersionUID = 5393968786564920519L;

	public void update(Observable arg0, Object arg1)
	{
		if(arg0 instanceof Variable)
		{
			if(arg1 instanceof Node)
				println("Equation changed: "+arg0);
			else
				println("Var changed: "+arg0);
		}
		else if(arg0 instanceof SymbolTable.StObservable)
		{
			println("New var: "+arg1);
			((Variable) arg1).addObserver(this);
		}
	}

	public void initialise()
	{
		super.initialise();
		SymbolTable st = j.getSymbolTable();
		st.addObserver(this);
		st.addObserverToExistingVariables(this);

		for(Enumeration en = st.elements();en.hasMoreElements();) {
			Variable var = (Variable) en.nextElement();
			println("Existing variable "+var);
			//var.addObserver(this);
		}
	}

	public static void main(String args[]) {
		Console c = new ObserverConsole();
		c.run(args);
	}
}
