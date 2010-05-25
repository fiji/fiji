/* @author rich
 * Created on 04-May-2004
 */
package org.lsmp.djep.mrpe;




/** A list of commands evaluated in sequence by the evaluator.  */
public final class MRpCommandList {
	
	/** Data type for the command string */
	static final class MRpCommand {
		short command;
		short aux1; 
		short aux2; 
		MRpCommand(short command,short aux){
			this.command = command; this.aux1 = aux; this.aux2 = -1;
		}
		MRpCommand(short command,short aux1,short aux2){
			this.command = command; this.aux1 = aux1; this.aux2 = aux2;
		}
		public String toString() {
			switch(command)
			{
				case MRpEval.CONST: return "Constant\tnum "+aux1;
				case MRpEval.VAR: return "Variable\ttype "+MRpEval.dimTypeToDimension(aux1)+"\tnum "+aux2;
				case MRpEval.ADD: return "ADD\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.SUB: return "SUB\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.MUL: return "MUL\ttype "+MRpEval.dimTypeToDimension(aux1)+" by "+MRpEval.dimTypeToDimension(aux2);

				case MRpEval.DIV: return "DIV\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.MOD: return "MOD\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.POW: return "POW\ttype "+MRpEval.dimTypeToDimension(aux1);

				case MRpEval.AND: return "AND\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.OR: return "OR\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.NOT: return "NOT\ttype "+MRpEval.dimTypeToDimension(aux1);

				case MRpEval.LT: return "LT\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.GT: return "GT\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.LE: return "LE\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.GE: return "GE\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.NE: return "NE\ttype "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.EQ: return "EQ\ttype "+MRpEval.dimTypeToDimension(aux1);
	
				case MRpEval.ASSIGN: return "Assign type "+MRpEval.dimTypeToDimension(aux1)+" no "+aux2;
				case MRpEval.LIST: return "List type "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.DOT: return "Dot type "+MRpEval.dimTypeToDimension(aux1);
				case MRpEval.CROSS: return "Cross type "+MRpEval.dimTypeToDimension(aux1);

				case MRpEval.FUN: return "Function\tnum "+aux1;
				case MRpEval.UMINUS: return "UMinus\ttype "+MRpEval.dimTypeToDimension(aux1);
			}
			return "Warning unknown command: "+command+" "+aux1+" "+aux2;
		}
	}

	/** Incremental size for list of commands **/
	private static final int STACK_INC=10;
	/** List of commands **/
	MRpCommand commands[] = new MRpCommand[STACK_INC];
	/** Current position in the command Stack. **/
	private short commandPos;
	/** The return type at end of evaluation */
	private int finalType;
	
	/** Package private constructor */
	MRpCommandList() {}
	/** Adds a command to the list */
	final void addCommand(short command,short aux)
	{
		if(commandPos == commands.length)
		{
			MRpCommand newCommands[] = new MRpCommand[commands.length+STACK_INC];
			System.arraycopy(commands,0,newCommands,0,commands.length);
			commands = newCommands;
		}
		commands[commandPos]=new MRpCommand(command,aux);
		++commandPos;
	}
	/** Adds a command to the list */
	final void addCommand(short command,short aux1,short aux2)
	{
		if(commandPos == commands.length)
		{
			MRpCommand newCommands[] = new MRpCommand[commands.length+STACK_INC];
			System.arraycopy(commands,0,newCommands,0,commands.length);
			commands = newCommands;
		}
		commands[commandPos]=new MRpCommand(command,aux1,aux2);
		++commandPos;
	}
	/** number of commands in list. */
	public int getNumCommands() { return commandPos;}
	/** The return type of argument. */
	int getFinalType() {	return finalType;	}
	void setFinalType(int i) { finalType = i;}
	/** converts list to a string. */	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<commandPos;++i) {
			sb.append(commands[i].toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
