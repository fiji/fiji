
package org.lsmp.djepExamples;
import org.nfunk.jep.*;

/**
* Example code illustrating how block control structures could be implemented.
* <p>
* Sample session
* <pre>
* JEP > a=5
* 5.0
* JEP > a>4
* 1.0
* JEP > TrueBlock
* JEP > b=a^2
* 25.0
* JEP > c=a^3
* 125.0
* JEP > FalseBlock
* JEP > b=a^3
* JEP > c=a^2
* JEP > EndBlock
* JEP > b
* 25.0
*</pre>
*This code does not allow nesting on condition statements.
**/

public class BlockStatments extends Console {
	
	private static final long serialVersionUID = 9035584745289937584L;

	/** Sets up three possible states */
	private static final int NoState = 0;
	private static final int TrueState = 1;
	private static final int FalseState = 2;
	/** Indicates current state where in */
	private int state = NoState;
	private int conditionValue = 0;
	
	/** 
	 * Catches macros which are not handled by JEP
	 * 
	 * @return false - stops further processing of the line
	 */
	public boolean testSpecialCommands(String command) 
	{	
		if(command.equals("TrueBlock")) {
			state = TrueState;
			return false;
		}
		if(command.equals("FalseBlock")) {
			state = FalseState;
			return false;
		}
		if(command.equals("EndBlock")) {
			state = NoState;
			return false;
		}
		return true;
	}

	/** Evaluates a node, but only if the state corresponds to the conditionValue.
	 * Also saves the result of evaluation in conditionValue for use in subsequent calls
	 *  
	 * @param node Node representing expression
	 * @throws ParseException if a Parse or evaluation error
	 */ 
	public void processEquation(Node node) throws ParseException
	{
		if(state==NoState
			|| ( state==TrueState && conditionValue !=0 )
			|| ( state==FalseState && conditionValue ==0 )
			)
		{
			Object res = j.evaluate(node);
			println(res);
			if(state==NoState)
				conditionValue = ((Number) res).intValue();
		}
	}
}
