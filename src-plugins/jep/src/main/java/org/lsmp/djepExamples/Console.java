/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

/*****************************************************************************

JEP - Java Math Expression Parser 2.24
	  December 30 2002
	  (c) Copyright 2002, Nathan Funk
	  See LICENSE.txt for license information.

*****************************************************************************/

/**
 * Console - JEP Example Applet
 * Copyright (c) 2000 Nathan Funk
 *
 * @author Nathan Funk , Richard Morris
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import java.io.*;
import java.util.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

/**
* This class implements a simple command line utility for evaluating
* mathematical expressions.
* <pre>
*   Usage: java org.lsmp.djepExamples.Console [expression]
* </pre>
* If an argument is passed, it is interpreted as an expression
* and evaluated. Otherwise, a prompt is printed, and the user can enter
* expressions to be evaluated. 
* 
* <p>
* This class has been designed to be sub classed to allow different
* consol applications.
* The methods
* <pre>
* public void initialise()
* public void processEquation(Node node) throws Exception
* public boolean testSpecialCommands(String command)
* public void printPrompt()
* public void printIntroText()
* public void printHelp()
* </pre>
* can all be overwritten.
* </p>
* <p>
* Furthermore main should be overwritten. For example
* <pre> 
* 	public static void main(String args[]) {
*		Console c = new DJepConsole();
*		c.run(args);
*	}
*</pre>
*/

public class Console extends Applet implements KeyListener {
	
	private static final long serialVersionUID = 9035584745289937584L;

	/** Main JEP object */
	protected JEP j;	
	
	/** The input reader */
	private BufferedReader br;

	/** Text area for user input in applets. */
	protected TextArea ta = null;
		
	/** Constructor */
	public Console() {
		br = new BufferedReader(new InputStreamReader(System.in));
	}

	/** Applet initialization */
		
	public void init() 
	{
		initialise();
		this.setLayout(new BorderLayout(1,1));
		ta = new TextArea("",10,80,TextArea.SCROLLBARS_BOTH);
		ta.setEditable(true);
		ta.addKeyListener(this);
		add("Center",ta);
		printIntroText();
		print(getPrompt());
	}
	
	/** Creates a new Console object and calls run() */
	public static void main(String args[]) {
		Console c = new Console();
		c.run(args);
	}

	/** The main entry point with command line arguments 
	 */
	public void run(String args[]) {
		initialise();
		 
		if (args.length>0) {
			for (int i=1; i<args.length; i++)
			{
				processCommand(args[i]);
			}
		}
		else
			inputLoop();
	}

	/**
	 * The main input loop for interactive operation.
	 * Repeatedly calls getCommand() and processCommand().
	 */
	public void inputLoop() {
		String command="";
				
		printIntroText();
		print(getPrompt());
		while((command = getCommand()) != null) 
		{
			if( !processCommand(command)) break;
			print(getPrompt());
		}
	}
	
	/** 
	 * Process a single command.
	 * <ol>
	 * <li>Tests for exit, quit, and help.</li>
	 * <li>Tests for any special commands used by sub classes.
	 * {@link #testSpecialCommands(String)}</li>
	 * <li>Parses the command.</li>
	 * <li>Processes the node. {@link #processEquation(Node)}<li>
	 * <li>Checks for errors. {@link #handleError(Exception)}</li>
	 * </ol>
	 * 
	 * @param command The line to be processed
	 * @return false if un-recoverable error or 'quit' or 'exit'
	 */
	public boolean processCommand(String command) 
	{	
		if(command.equals("quit") || command.equals("exit"))
			return false;

		if(command.equals("help"))	{
			printHelp();
			return true;
		}

		if(command.equals("functions"))	{
			printFuns();
			return true;
		}

		if(command.equals("operators"))	{
			printOps();
			return true;
		}

		if(command.equals("variables"))	{
			printVars();
			return true;
		}
		if(!testSpecialCommands(command)) return true;
			
		try {
			Node n = j.parse(command);
			processEquation(n);
		}
		catch(Exception e) { return handleError(e); }
		
		return true;
	}


	/** sets up all the needed objects. */
	public void initialise()
	{
		j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setAllowAssignment(true);
		j.setImplicitMul(true);
	}
	
	/** Performs the required operation on a node. 
	 * Typically evaluates the node and prints the value.
	 * 
	 * @param node Node representing expression
	 * @throws ParseException if a Parse or evaluation error
	 */ 
	public void processEquation(Node node) throws ParseException
	{
		Object res = j.evaluate(node);
		println(res);
	}

	
	/**
	 * Get a command from the input.
	 * @return null if an IO error or EOF occurs.
	 */
	protected String getCommand() {
		String s=null;
		
		if (br == null)	return null;

		try
		{
			if ( (s = br.readLine()) == null) return null;
		}
		catch(IOException e)
		{
			println("IOError exiting"); return null;
		}
		return s;
	}

	/** Prints the prompt string. */
	public String getPrompt() { return "JEP > "; }

	/** Prints a standard help message. 
	 * Type 'quit' or 'exit' to quit, 'help' for help.
	 **/
	public final void printStdHelp() {
		if(ta == null)
			println("Type 'quit' or 'exit' to quit, 'help' for help.");
		else 
			println("Type 'help' for help.");
	}		

	/** Print help message. */
	public void printHelp() { 
		printStdHelp();
		println("'functions' lists defined functions"); 
		println("'operators' lists defined operators"); 
		println("'variables' lists variables and constants"); 
	}

	/** Prints introductory text. */
	public void printIntroText() {
		println("JEP Console.");
		printStdHelp();
	}

	/** Prints a list of defined functions. */
	public void printFuns() {
		FunctionTable ft = j.getFunctionTable();
		println("Known functions:");
		for(Enumeration  loop = ft.keys();loop.hasMoreElements();)
		{
			String s = (String) loop.nextElement();
			println("\t"+s);
		}
	}

	/** Prints a list of defined operators. */
	public void printOps() {
		OperatorSet opset = j.getOperatorSet();
		Operator ops[] = opset.getOperators();
		println("Known operators:");
		for(int i=0;i<ops.length;++i)
			println("\t"+ops[i].toString());
	}

	/** Prints a list of constants. */
	public void printVars() {
		SymbolTable st = j.getSymbolTable();
		println("Variables:");
		for(Enumeration  loop = st.keys();loop.hasMoreElements();)
		{
			String s = (String) loop.nextElement();
			Object val = st.getValue(s);
			println("\t"+s+"\t"+val);
		}
	}
	/**
	 * Checks for special commands used by subclasses.
	 * For example a subclass may have a verbose mode
	 * switched on of off using the command
	 * <pre>verbose on</pre>
	 * This method can be used detected this input, 
	 * perform required actions and skip normal processing by returning true.
	 * 
	 * @param command
	 * @return true indicates normal processing should continue (default) false if the command is special and no further processing should be performed (parsing and evaluating)
     * @see #split(String)
	 */
	public boolean testSpecialCommands(String command)	{ return true; }		

	/**
	 * Handle an error in the parse and evaluate routines.
	 * @param e
	 * @return false if the error cannot be recovered and the program should exit
	 */
	public boolean handleError(Exception e)
	{
		if(e instanceof ParseException) { 
			println("Parse Error: "+e.getMessage()); }
		else
			println("Error: "+e.getClass().getName()+" "+e.getMessage());

		return true;
	}

	/** Splits a string on spaces.
	 * 
	 * @param s the input string
	 * @return an array of the tokens in the string
	 */	
	public String[] split(String s)
	{
		StringTokenizer st = new StringTokenizer(s);
		int tokCount = st.countTokens();
		String res[] = new String[tokCount];
		int pos=0;
		while (st.hasMoreTokens()) {
			res[pos++]=st.nextToken();
		}
		return res;	
	}

	/** Prints a line of text no newline.
	 * Subclasses should call this method rather than 
	 * System.out.print to allow for output to different places.
	 * 
	 */
	public void print(Object o)
	{
		String s=null;
		if(o == null) s = "null";
		else s = o.toString();
		
		if(ta != null)
			ta.append(s);
		else
			System.out.print(s);
	}

	/** Prints a line of text no newline.
	 * Subclasses should call this method rather than 
	 * System.out.print to allow for output to different places.
	 */
	public void println(Object o)
	{
		String s=null;
		if(o == null) s = "null";
		else s = o.toString();
		
		if(ta != null)
			ta.append(s + "\n");
		else
			System.out.println(s);
	}

	/**
	 * Handles keyRelease events
	 */
	public void keyReleased(KeyEvent event)
	{
		int code = event.getKeyCode();
		if(code == KeyEvent.VK_ENTER)
		{
			int cpos = ta.getCaretPosition();
			String alltext = ta.getText();
			String before = alltext.substring(0,cpos-1);
			int startOfLine = before.lastIndexOf('\n');
			if(startOfLine > 0)
					before = before.substring(startOfLine+1);
			String prompt = getPrompt();
			String line=null;
			if(before.startsWith(prompt))
			{
				line = before.substring(prompt.length());					
				this.processCommand(line);
			}
//			System.out.println("line ("+line+")");
			//if(!flag) this.exit();
			this.print(getPrompt());
		}
	}

	public void keyPressed(KeyEvent arg0)
	{
	}

	public void keyTyped(KeyEvent arg0)
	{
	}

	public String getAppletInfo()
	{
		return "Jep Console applet\n" +
			"R Morris Mar 2005\n" +
			"See http://www.singsurf.org/djep/";
	}

}
