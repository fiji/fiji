/* @author rich
 * Created on 04-May-2004
 */
package org.lsmp.djep.rpe;

/** A list of commands */
public final class RpCommandList {
	
	/** Incremental size for list of commands **/
	private static final int STACK_INC=10;
	/** List of commands **/
	RpCommand commands[] = new RpCommand[STACK_INC];
	/** Current position in the command Stack. **/
	private short commandPos;
	private RpEval rpe;
	/** Package private constructor */
	private RpCommandList() {}
	RpCommandList(RpEval rpe) {this.rpe = rpe;}
	/** Adds a command to the list */
	final void addCommand(short command,short aux)
	{
		if(commandPos == commands.length)
		{
			RpCommand newCommands[] = new RpCommand[commands.length+STACK_INC];
			System.arraycopy(commands,0,newCommands,0,commands.length);
			commands = newCommands;
		}
		commands[commandPos]=new RpCommand(rpe,command,aux);
		++commandPos;
//		++maxCommands;
	}
	final void addCommand(short command)
	{
		if(commandPos == commands.length)
		{
			RpCommand newCommands[] = new RpCommand[commands.length+STACK_INC];
			System.arraycopy(commands,0,newCommands,0,commands.length);
			commands = newCommands;
		}
		commands[commandPos]=new RpCommand(rpe,command);
		++commandPos;
//		++maxCommands;
	}

	public int getNumCommands() { return commandPos;}
	public RpCommand getCommand(int i) { return commands[i]; }
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<commandPos;++i) {
			sb.append(commands[i].toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
