package org.lsmp.djep.rpe;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.Variable;

/** Data type for the command string */
public final class RpCommand {
	short command;
	short aux1; 
	RpEval rpe;
	private RpCommand() {};
	RpCommand(RpEval rpe,short command){
		this.command = command; this.aux1 = -1; this.rpe = rpe;
	}
	RpCommand(RpEval rpe,short command,short aux){
		this.command = command; this.aux1 = aux; this.rpe = rpe;
	}
	public String toString() {
		switch(command)
		{
			case RpEval.CONST: return "Constant\tno "+aux1;
			case RpEval.VAR: return "Variable\tnum "+aux1;
			case RpEval.ADD: return "ADD";
			case RpEval.SUB: return "SUB";
			case RpEval.MUL: return "MUL";
			case RpEval.DIV: return "DIV";
			case RpEval.MOD: return "MOD";
			case RpEval.POW: return "POW";
			case RpEval.AND: return "AND";
			case RpEval.OR: return "OR";
			case RpEval.NOT: return "NOT";
			case RpEval.LT: return "LT";
			case RpEval.LE: return "LE";
			case RpEval.GT: return "GT";
			case RpEval.GE: return "GE";
			case RpEval.EQ: return "EQ";
			case RpEval.NE: return "NE";
			case RpEval.ASSIGN: return "Assign\tnum "+aux1;
			case RpEval.FUN: return "Function\tnum "+aux1;
		}
		return "WARNING unknown command: "+command+" "+aux1;
	}
	/**
	 * Returns the type of an individual command. The return value will be one of the constants defined in RpEval. 
	 * These include RpEval.CONST - constants, RpEval.VAR - variables, RpEval.ASSIGN assinments x=..., RpEval.FUN functions. 
	 * Other indicies correspond to unary and binary operators,  RpEval.ADD.
	 * @return an integer representing the type
	 */
	public int getType() { return command; }
	public int getRef() { return aux1; }
	public double getConstantValue() throws ParseException
	{
		if(command!=RpEval.CONST) throw new ParseException("This element is not a constant.");
		return rpe.constVals[aux1];
	}
	
	public Variable getVariable() throws ParseException
	{
		if(command!=RpEval.VAR) throw new ParseException("This element is not a variable.");
		return rpe.getVariable(aux1);
	}
	
	public String getFunction() throws ParseException
	{
		if(command!=RpEval.FUN) throw new ParseException("This element is not a function.");
		return rpe.getFunction(aux1);
	}
}
