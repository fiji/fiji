package CLI;
/**
 *
 * Command Line Interface plugin for ImageJ(C).
 * Copyright (C) 2004 Albert Cardona.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 *
 * You may contact Albert Cardona at albert at pensament.net, and at http://www.lallum.org/java/index.html
 *
 * **/



/*
 * TODO:
 * 	- cp command
 * 	- ~ gets replaced at fixDir for user.home
 * 	- when executing macro, print full name and source (recorded list,  or dir)
 * 	- add an option to choose min and max size of files to open (-m for min and -M for max, in megabytes)
 * 	FIX
 * 		- some problem when ' open folder/here/foto.jpg in windows'
 * 			The problem is severe! After typing cd ../.. the pwd reports D:/, but lsi attempts to list from D:/Albert/ImageJ/  !!
 * 		- still double // on some cases (file is not first argument) at root. Must check carefully!
 * 		TODO: I did some repairs but didn't test them under windows
 *
 * 	FIXED
 * 		- when magic on, fill() was becomming fill("") and failing
 * 		- when backslash at the end, magic was missbehaving
 * 		- when testing for number of quotes, the variable tested  was totally wrong
 * 		- when checking for isShellCommand, if the first white space was beyond the end of the command name it failed. Added also several checks to make sure a non-shell command is not tried to be executed as one.
 * 		- TAB was not scrolling down the bar when printing possible files
 * 		- TAB ws not expanding the file_name at the prompt up to the minimum common substring
 * 		- typing 'exec' with zero recorded macros throw an exeception
 * 		- typing 'open' on its own was being directed to the macro interpreted nad was being printed to screen!
 * 		- bug in fixDir that would append incorrectly an extra slash
 * 		- bug in doTab that would append incorrectly an extra slash when expanding paths at the root dir
 * 		- added macro command expansion by TAB!
 * 		- sometimes opening an image would open all images, specially with .tiff files.
 * 		- typing commands without arguments was not safe in some cases, such as "cd" and "rm"
 * 		- added usage printing when typing in commands that require arguments but no arguments are specified.
 * 		- magic softened so that quotes and parenthesis are not added for for/while/if statements and { }
 * 		- doTab was prepending an extra slash when calling directories from the root directory.
 * 		- macro names are printed as complete at all times, even when entered as partial names
 * 		- Added a Record command in the contextual menu to record as macros the selection.
 * 		- Added a toggle_edit command to enable direct screen editing.
 * 		- Extended the view function to print also any macro in the macros dir or the user_dir
 * 		- Added a show command to do a slide show on current dir or user specified dir
 */

/* VERSION: 1.07
 * RELEASE DATE: 2005-02-05
 * AUTHOR: Albert Cardona at albert at pensament.net
 */

import ij.plugin.PlugIn;
import ij.IJ;
import ij.macro.Interpreter;
import ij.WindowManager;
import ij.macro.MacroConstants;
import ij.gui.GenericDialog;
import ij.plugin.frame.Recorder;

import common.AbstractInterpreter;

import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import java.util.ArrayList;
import javax.swing.JPopupMenu;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.Toolkit;
import java.awt.FileDialog;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.awt.Font;
import java.awt.Dimension;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.Component;
import java.util.Arrays;


public class CLI_ extends AbstractInterpreter {

	String macro = "\n";//System.getProperty("line.separator");
	final String l = "\n";//System.getProperty("line.separator");
	final String pre = "> ";
	JPopupMenu popup_menu;
	String selection;
	PopupListener popup_listener = new PopupListener();
	static String user_dir = fixWindowsPath(System.getProperty("user.dir"));
	
	static final String file_separator = "/"; //using it like this to prevent windows OS havoc due to escape char being also the file.separator //System.getProperty("file.separator");
	
	static final String trash_can = user_dir + "/plugins/CLITrashCan";

	boolean magic = false;
	//boolean enable_direct_editing = false; //to put away focus or not

	static final String dir_macros = user_dir + "/macros";
	boolean allow_print = true;

	String current_root_dir;

	static String[] all_macro_functions = new String[MacroConstants.functions.length + MacroConstants.numericFunctions.length + MacroConstants.stringFunctions.length + MacroConstants.arrayFunctions.length];

	static String fixWindowsPath(String path) {
		if (IJ.isWindows()) {
			char[] c = new char[path.length()];
			path.getChars(0, c.length, c, 0);
			for (int i=0; i<c.length; i++) {
				if ('\\' == c[i]) {
					c[i] = '/';
				}
			}
			return new String(c);
		}
		//default
		return path;
	}
	
	public void run(String arg) {
		setTitle("ImageJ Terminal v1.07");
		super.run(arg);

		println("-->  Welcome. Type   help   in the text field below.\n");
		//set current_root_dir value
		if (IJ.isWindows()) {
			current_root_dir = user_dir.substring(0,2); //just the 'c:' or 'd:' etc
		} else {
			current_root_dir = "/"; //unix-like systems
		}

		//setup all_macro_functions static array
		//static String[] all_macro_functions = new String[MacroConstants.functions.length + MacroConstants.numericFunctions.length + MacroConstants.stringFunctions.length + MacroConstants.arrayFunctions.length];
		System.arraycopy(MacroConstants.functions, 0, all_macro_functions, 0, MacroConstants.functions.length);
		System.arraycopy(MacroConstants.numericFunctions, 0, all_macro_functions, MacroConstants.functions.length, MacroConstants.numericFunctions.length);
		int start_index = MacroConstants.functions.length + MacroConstants.numericFunctions.length;
		System.arraycopy(MacroConstants.stringFunctions, 0, all_macro_functions, start_index, MacroConstants.stringFunctions.length);
		start_index += MacroConstants.stringFunctions.length;
		System.arraycopy(MacroConstants.arrayFunctions, 0, all_macro_functions, start_index, MacroConstants.arrayFunctions.length);

	}

	protected void makeGUI() {
		super.makeGUI();
		// Replace popup
		popup_menu = new JPopupMenu();
		addPopupMenuItem("Execute Selection");
		addPopupMenuItem("Record");
		addPopupMenuItem("Copy");
		addPopupMenuItem("Save Selection");
		addPopupMenuItem("Save & Exec Selection");
	}

	void addPopupMenuItem(String name) {
		JMenuItem mi = new JMenuItem(name);
		mi.addActionListener(popup_listener);
		popup_menu.add(mi);
	}

	protected String getLineCommentMark() { return "//"; }

	protected Object eval(String temp) {
		try {
			//get input line
			//String temp = prompt.getText();
			//remove trailing or leading spaces
			temp = temp.trim();

			//check for contents
			if (0 == temp.length()) {
				return null;
			}

			//check if command is custom shell-like
			if (isShellCommand(temp)) {
				return null;
			}

			//check if function needs special editing (such as open("....")
			//It is important that this is called after testing for isShellCommand!!
			temp = specialEditing(temp);

			//check for contents (done again after specialEditing ... why?)
			if (0 == temp.length()) {
				return null;
			}

			//append newline char to temp
			temp += l;
			
			//run macro interpreter or record
			if (temp.lastIndexOf('\\') == temp.length()-2) { // -2 because there is a \n at the end
				String newline = "";
				//print shell-like line start
				if (1 == macro.length()) { //1 because of the newline character
					//used only for the very first line of the multiline statement
					//screen.append(pre); // not anymore
					newline += l;
				}
				//record locally without ending backslash
				newline += temp.substring(0,temp.lastIndexOf('\\'));
				macro += newline;
				//record to MacroRecorded if needed
				if (MacroRecord.isRecording()) {
					MacroRecord.appendToCurrent(newline + l); //appending \n because it was lost beyond the backslash
				}
			} else {
				//print shell-like line start
				/* // no need
				if (1 == macro.length()) {
					screen.append(pre);
				}
				*/
				//record locally
				macro += temp;
				//record to MacroRecorded if needed
				if (MacroRecord.isRecording()) {
					MacroRecord.appendToCurrent(temp);
				}
				//execute
				execMacro(macro);
				//reset macro
				macro = l;
			}

			//if (allow_print) screen.append(temp);
			//reset allow_print
			allow_print = true;

			return null;
		}catch(Exception e) {
			println("Some error ocurred: " + e + "\n" + new TraceError(e));
			//reset macro
			macro = "\n";
			return null;
		}
	}

	synchronized protected void doTab(ActionEvent ae) {
		
		//if magic is on, append ' ', else append '()' and place the caret in between
		//Get all macro functions from ij.macro.MacroConstants.functions[] and .numericFunctions[]

		String input = prompt.getText();

		//skip null inputs:
		if (null == input || input.equals("")) {
			return;
		}
		
		//expands path names at prompt after pushing tab, or prints all possibilities at screen
		//get the file name to expand
		String command = null;
		String output = null;
		if (input.length() > 5 && equal(input.substring(0,6), "open(\"")) {
			command = input.substring(0,6);
			output = fixDir(input.substring(6));
		} else {
			int space = input.lastIndexOf(' ');
			if (-1 != space) {
				command = input.substring(0, space+1);
				output = fixDir(input.substring(space+1));
			}
		}

		if (null != output && 0 != output.length()) {//if (null != command && 0 != command.length())


		//1 - expand file paths
		String the_dir = current_root_dir;
		String file_part = "";
		int last_slash = output.lastIndexOf(file_separator);
		if (0 < last_slash || (1 == output.indexOf(":/") && 2 < last_slash)) { //avoiding -1 and 0
			the_dir = output.substring(0, last_slash);
			file_part = output.substring(last_slash +1);
		} else if ((0 == output.indexOf('/') || 1 == output.indexOf(':')) && 0 == last_slash) {
			the_dir = current_root_dir;
			file_part = output.substring(1);
		}

		//put asterisk! This is the trick for the file filter
		File f_the_dir = new File(the_dir);
		String[] files = f_the_dir.list(new CustomFileFilter(file_part+"*"));
		if (null != files && files.length > 1) {
			//print options to screen
			println("-->  Possible files in " + f_the_dir.getName() + " folder:");
			for (int i=0; i<files.length; i++) {
				File f = new File(the_dir + file_separator + files[i]);
				screen.append("\n-->  " + files[i]);
				if (f.isDirectory()) {
					screen.append("/");
				}
			}
			println("");

			String expanded = getMaxExpanded(file_part, files);

			if (-1 == input.indexOf("..") && -1 == input.indexOf('/')) {
				prompt.setText(command + expanded); //showing full path is unnecesary and unconvenient
			} else if ((0 == output.indexOf('/') || 1 == output.indexOf(':')) && (0 == output.lastIndexOf('/') || 1 == output.lastIndexOf(':'))) {
				//meaning, if the_dir is current_root_dir and we are expanding at level zero
				prompt.setText(command + the_dir + expanded);
			} else {
				prompt.setText(command + the_dir + file_separator + expanded);
			}
			
		} else if (1 == files.length) {
			String path = null;
			if (-1 == input.indexOf("..") && -1 == input.indexOf('/')) {
				path = files[0];
			} else if ((the_dir.length() -1) != the_dir.lastIndexOf('/')) {
				path = the_dir + file_separator + files[0];
			} else {
				//used for when root dir is a slash in unix-like systems
				path = the_dir + files[0];
			}

			//append last slash if expanded file exists and is a directory
			File f = new File(user_dir + file_separator + path);
			if (f.exists() && f.isDirectory()) {
				path += file_separator;
			}
			
			prompt.setText(command + path);
		}

		} //end of: if (null != command && 0 != command.length())

		else {
		//2 - expand macro commands
		// using the String[] MacroConstants.functions
		// and the String[] MacroConstants.numericFunctions
			
			String partial_macro_command = null;
			int start = -1;
			if (-1 != (start = input.lastIndexOf(' '))
			 || -1 != (start = input.lastIndexOf('('))
			 || -1 != (start = input.lastIndexOf(','))
			 || -1 != (start = input.lastIndexOf('='))
			 || -1 != (start = input.lastIndexOf('+'))
			 || -1 != (start = input.lastIndexOf('-'))
			 || -1 != (start = input.lastIndexOf('*'))
			 || -1 != (start = input.lastIndexOf('/'))
			 ) {
				partial_macro_command = input.substring(start+1);
			} else {
				partial_macro_command = input;
			}

			try {
			ArrayList possibles = new ArrayList();

			String expanded_macro_command = getMaxExpandedAndPossibleList(partial_macro_command, all_macro_functions, possibles);

			if (-1 != start) {
				prompt.setText(input.substring(0, start+1) + expanded_macro_command);
			} else {
				prompt.setText(expanded_macro_command);
			}
			//print possible list
			if (1 < possibles.size()) {
				screen.append("\n-->  Possible macro commands:");
				for (int i=0; i< possibles.size(); i++) {
					screen.append("\n-->    " + (String)possibles.get(i));
				}
				println("");
			}
			}catch(Exception e) { IJ.write("Error! " + new TraceError(e));}
		}


		//scroll down bar
		screen.setCaretPosition(screen.getText().length());
	}


	String getMaxExpanded(String part, String[] names) {
		//expand name in prompt up to the minimum common starting substring
		byte[] read = new byte[names.length];
		byte one = 1;
		Arrays.fill(read, one);
		int a = part.length();
		String expanded = part; //names[0].substring(0, a);
		String previous = null;
		int index = -1;
		int num_equals = 0;
		int i = 0;
		int steps = 0;
		while(true) {
			//step counter
			steps++;
			//scan for similar starting substrings
			for (i=0 ; i<names.length; i++) {
				if (1 == read[i] && a <= names[i].length() && equal(names[i].substring(0, a), expanded)) {
					num_equals++;
					index = i;
				} else {
					read[i] = 0; //skip it next time
				}
			}
			if (names.length == num_equals && -1 != index) {
				a++;
				previous = expanded;
				expanded = names[index].substring(0, a);
			} else {
				if (1 == steps && 1 == num_equals) {
					//just one word suitable, just one step
					expanded = names[index];
				} else {
					expanded = previous;
				}
				break;
			}
			//reset all vars
			index = -1;
			num_equals = 0;
		}

		return expanded;
	}

	String getMaxExpandedAndPossibleList(String part, String[] names, ArrayList possibles) {
		//expand name in prompt up to the minimum common starting substring
		byte[] read = new byte[names.length];
		byte one = 1;
		Arrays.fill(read, one);
		int a = part.length();
		String expanded = part;
		String previous = null;
		int index = -1;
		int num_equals = 0;
		int i = 0;
		int steps = 0;
		while(true) {
			//step counter
			steps++;
			//scan for similar starting substrings
			for (i=0 ; i<names.length; i++) {
				if (1 == read[i] && a <= names[i].length() && equal(names[i].substring(0, a), expanded)) {
					num_equals++;
					index = i;
					possibles.add(names[i]);
				} else {
					read[i] = 0; //skip it next time
				}
			}
			if (names.length == num_equals && -1 != index) {
				a++;
				previous = expanded;
				expanded = names[index].substring(0, a);
			} else {
				if (1 == steps && 1 == num_equals) {
					//just one word suitable, just one step
					expanded = names[index];
				} else if (1 == steps) {
					//a few words suitable, just one step
					expanded = part;
				} else {
					//a few words suitable, a few steps
					expanded = previous;
				}
				break;
			}
			//reset all vars
			index = -1;
			num_equals = 0;
			possibles.clear();
		}

		return expanded;
	}

	String specialEditing(String temp_) {
		
		String temp = fixWindowsPath(temp_);
		//repair trailing backslash if any (why? Gets interpreted here?)
		if (temp_.length()-1 == temp_.lastIndexOf('\\')) {
			temp = temp.substring(0, temp.length()-1) + "\\";
		}
		
		if (temp.length() > 4 && equal("open(", temp.substring(0, temp.indexOf('(') + 1))) {
			int first_quote_index = temp.indexOf('"');
			int second_quote_index = temp.indexOf('"', first_quote_index+1);
			String path = temp.substring(first_quote_index+1, second_quote_index);
			if (path.startsWith("/") || 1 == path.indexOf(":/")) { //second statement added to work under windows	
				//do nothing
			} else {
				path = user_dir + file_separator + path;
			}
			return "open(\"" + path + temp.substring(second_quote_index);
		}

		if (temp.length() > 3 && equal("open", temp.substring(0, 4))) { //was: "open " and 0, 5
			if (4 == temp.length()) {
				println("-->  Usage:  open <file_name>\n-->    Asterisks allowed:  open *name | *name* | *name\n-->    and multiple files:  open file1 file2");
				return "";
			}
			StringTokenizer stsp;
			if (-1 == temp.indexOf('\"')) {
				stsp = new StringTokenizer(temp, " ");
			} else {
				stsp = new StringTokenizer(temp, "\"");
			}
			//for each token except the first one, find files
			String open = stsp.nextToken(); ///dummy
			String txt = "";
			boolean not_first = false;
			String dir_path;
			File dir;
			String[] image_file;
			String backslash = "\\";
			while(stsp.hasMoreElements()) {
				String name = stsp.nextToken();
				if (equal(name, backslash)) {
					txt += backslash; //reappending ending backslash
					break; //must break and ignore any other files if any, to prevent havoc
				}
				if (name.startsWith(file_separator) || 1 == name.indexOf(":/")) { //second statement added to work under windows
					//searching for file 'name' using provided argument as full path
					int last_slash = name.lastIndexOf(file_separator);
					if (0 == last_slash || (1 == name.indexOf(':') && 3 >= name.length())) {  //second statement added to work under windows
						//open files from the root dir itself
						dir_path = name.substring(0, last_slash); //current_root_dir;
						dir = new File(dir_path);
						image_file = dir.list(new ImageFileFilter(name.substring(1)));
					} else {
						//open files from the /root/other/dirs
						dir_path = current_root_dir + name.substring(0, last_slash);
						dir = new File(dir_path); //already contains a file_separator slash, because it starts with it
						image_file = dir.list(new ImageFileFilter(name.substring(last_slash+1))); //passing the name of the file, i.e. chars after last slash
					}
				} else if (-1 == name.indexOf(file_separator)) {
					//open files in the user_dir
					dir = new File(user_dir);
					image_file = dir.list(new ImageFileFilter(name));
				} else {
					//open files in the user_dir/other/dirs
					int last_slash = name.lastIndexOf(file_separator);
					dir_path = user_dir + file_separator + name.substring(0, last_slash);
					dir = new File(dir_path);
					image_file = dir.list(new ImageFileFilter(name.substring(last_slash+1)));
				}

				if (0 == image_file.length) {
					println("\n--> No such file/s.");
				} else {
					for (int i=0; i<image_file.length; i++) {
						if (not_first) txt += l;
						txt += "open(\"";
						try {
							txt += fixWindowsPath(dir.getCanonicalPath()) + file_separator + image_file[i] + "\");";
						}catch(IOException ioe) {
							IJ.showMessage("Can't find canonical path for " + dir.getName());
						}
						not_first = true;
				}

				}
			}
			
			return txt;
		}

		if (magic) {
			String after_magic = temp.trim();
			boolean append_backslash = false;
			if ((after_magic.length())-1 == after_magic.lastIndexOf('\\')) {
				after_magic = after_magic.substring(0, after_magic.length()-1).trim();
				append_backslash = true;
			}
					
			int a = 0;

			//0 - insert parenthesis when needed
			//So that dc particle analyzer... --> dc(particle analyzer...);

			if (-1 == after_magic.indexOf('(') && 0 != after_magic.indexOf('{') && 0 != after_magic.indexOf('}')) {
				int first_space = after_magic.indexOf(" ");
				if (-1 != first_space) {
					//put second part inside a parenthesis
					after_magic = after_magic.substring(0, first_space) + "(" + after_magic.substring(first_space+1) + ");";
				} else {
					//append parenthesis at the end
					after_magic += "();";
				}
			}

			//1 - insert " " where necessary, so that after magic:
			//   dc(particle analyzer...") -> doCommand("Particle Analyzer...");
			//   But numeric arguments don't get commas
			a = after_magic.indexOf('(');
			int b = after_magic.indexOf("(\"");
			if (-1 != a && a != b && -1 == after_magic.indexOf('\"') && -1 == after_magic.indexOf(';') && 0 != after_magic.indexOf("for") && 0 != after_magic.indexOf("while") && 0 != after_magic.indexOf("if") ) {
				//some " " are needed
				int index_comma = after_magic.indexOf(',');
				int end = after_magic.lastIndexOf(')');
				String the_command = after_magic.substring(a+1, end);
				if (0 == the_command.length()) {
					//do nothing!
					//this happens for the fill() function, etc
				} else if (-1 == index_comma) {
					//assume single statement between parenthesis
					try {
						double num = Double.parseDouble(the_command);
						after_magic = after_magic.substring(0, a) + "(" + the_command + after_magic.substring(end);
					}catch(NumberFormatException nfe) {
						after_magic = after_magic.substring(0, a) + "(\"" + the_command + "\"" + after_magic.substring(end);
					}
				} else {
					//several statements between the parentesis
					//Wrap in " " the text ones only
					StringTokenizer st = new StringTokenizer(the_command, ",");
					String multiple_command = "";
					String quote = "\"";
					String comma = ",";
					while(st.hasMoreElements()) {
						String token = st.nextToken().trim();
						try {
							double num = Double.parseDouble(token);
							multiple_command += token;
						}catch(NumberFormatException nfe) {
							multiple_command += quote + token + quote;
						}
						multiple_command += comma;
					}
					//remove last comma (StringTokenizer.countTokens() method is unreliable, so I can'y use it above
					multiple_command = multiple_command.substring(0, multiple_command.length()-1);
				}
			}

			//2 - substitute dc for doCommand
			if (0 == after_magic.indexOf("dc(\"")) {
				after_magic = "doCommand" + after_magic.substring(2);
			}

			//3 - capitalize first letter of each word inside the " " of a command, except for the 'open' command
			a = after_magic.indexOf("(\"");
			if (-1 != a  && !equal(after_magic.substring(0, a), "open(") && -1 == after_magic.indexOf('[')) {
				int start = after_magic.indexOf("\"");
				int end = after_magic.indexOf("\"", start+1);
				StringTokenizer st = new StringTokenizer(after_magic.substring(start+1, end), " ");
				String the_command = "";
				while(st.hasMoreElements()) {
					//capitalize first letter
					String c = st.nextToken();
					the_command += c.substring(0,1).toUpperCase() + c.substring(1) + " ";
				}
				the_command = the_command.trim(); //removing last space, very silly, yeah, but StringTokenizer sucks
				after_magic = after_magic.substring(0, start+1) + the_command + after_magic.substring(end);
			}

			//last - append backslash if needed
			if (append_backslash) {
				after_magic += "\\";
			}

			return after_magic;
		}
			
		//default
		return temp;
	}

	void putTokens(String from, String separator, ArrayList al) {
		StringTokenizer st = new StringTokenizer(from, separator);
		while(st.hasMoreElements()) {
			String token = st.nextToken();
			if (null != token && 0 != token.length()) {
				al.add(token);
			}
		}
	}

	boolean isShellCommand(String input_line) {
		ArrayList al = new ArrayList();
		//safety check:
		if (-1 != input_line.indexOf('(')) {
			//not a shell command if it has a parenthesis
			return false;
		}
		if (-1 == input_line.indexOf('\"')) {
			//cut the input by spaces
			StringTokenizer st = new StringTokenizer(input_line, " ");
			while(st.hasMoreElements()) {
				al.add(st.nextToken());
			}
		} else {
			//take into account the quotes (used to input words with white spaces within)

			//count quotes: if number is not even, stop.
			int num_quotes = 0;
			int p = input_line.indexOf('\"');
			while (-1 != p) {
				num_quotes++;
				p = input_line.indexOf('\"', p+1);
			}
			if (0 != num_quotes%2.0) {
				println("\n-->  Wrong number of quotes!");
				return true; //this terminates it, the entered macro line is no longer taken into account
			}
			
			//single out the command
			String a_command = null;
			int first_space = input_line.indexOf(' ');

			//check out for abnormal cutting
			if (-1 != first_space) {
				a_command = input_line.substring(0, first_space);
			}
			if (null != a_command) {
				if (-1 == a_command.indexOf('(')
					&& -1 == a_command.indexOf('+')
					&& -1 == a_command.indexOf('\"')
					&& -1 == a_command.indexOf('-')
					&& -1 == a_command.indexOf('*')
					&& -1 == a_command.indexOf('/')) {
					//ok, the command contains letters only (hopefully)
					//do nothing
				} else {
					return false; //this allows continuing processing of macro line
				}
			} else {
				//the a_command substring is null, check input_line
				if (-1 == input_line.indexOf('(')
					&& -1 == input_line.indexOf('+')
					&& -1 == input_line.indexOf('\"')
					&& -1 == input_line.indexOf('-')
					&& -1 == input_line.indexOf('*')
					&& -1 == input_line.indexOf('/')) {
					//ok, the command contains letters only (hopefully)
					a_command = input_line;
				} else {
					return false; //this allows continuing processing of macro line
				}
			}
			
			al.add(a_command);
			//check for arguments
			String args = null;
			if (-1 != first_space) {
				args = input_line.substring(first_space+1).trim();//if there is a quote, input_line must be longer that the command plus a space
			}
			if (null != args && 0 != args.length()) {
				int start_quote = args.indexOf('\"');
				int end_quote = args.indexOf('\"', start_quote+1);
				if (0 != start_quote) {
					//there are some args without quotes in front of some with quotes
					putTokens(args.substring(0, start_quote), " ", al);
				}

				int stopper = 3; //just to prevent infinite loops due to errors
				
				while (-1 != start_quote) {
					String one_arg = args.substring(start_quote+1, end_quote);
					if (null != one_arg && 0 != one_arg.length()) {
						al.add(one_arg);
					}
					//stop if the arg string does not continue beyond end_quote
					if (args.length() <= end_quote+1) {
						break;
					}
					int next_quote = args.indexOf('\"', end_quote+1);
					if (-1 != next_quote && (next_quote-2) != end_quote) { //&& ' ' == args.charAt(end_quote+1)) { //(next_quote-1) != args.indexOf(" \"", end_quote+1)) {
						//there is a string without quotes
						putTokens(args.substring(end_quote+1, next_quote).trim(), " ", al);
						start_quote = next_quote;
						end_quote = args.indexOf('\"', next_quote+1);
					} else if (-1 == next_quote) {
						putTokens(args.substring(end_quote+1).trim(), " ", al);
						start_quote = -1; //to finish the loop
					} else {
						start_quote = next_quote;
						end_quote = args.indexOf('\"', start_quote+1);
					}

					stopper--;
					if (stopper == 0) break;
				}
			}
			
		}

		String command = (String)al.get(0);
		
		// 1 - cd : CHANGE DIRECTORY
		if (equal(command, "cd")) {
			//check for the existence of arguments!
			if (1 == al.size()) {
				println("\n-->  Usage:  cd <directory_name>");
				return true;
			}
			String dir = (String)al.get(1);

			//remove last slash if any
			int dir_last_index = dir.length() -1;
			if (dir_last_index != 0 && dir.lastIndexOf('/') == dir_last_index) {
				dir = dir.substring(0, dir_last_index);
			}
			String new_dir = null;
			if (equal(dir, "..")) {
				//check whether we are at top directory already
				if (IJ.isWindows()) {
					if (equal(dir, current_root_dir + file_separator)) {
						println("\n-->  Such directory doesn't make sense.");
						return true;
					}
				} else {
					if (equal(dir, current_root_dir)) {
						println("\n-->  Such directory doesn't make sense.");
						return true;
					}
				}
				//else move up one dir
				int last_slash = user_dir.lastIndexOf("/");
				new_dir = user_dir.substring(0, last_slash);
				if (-1 == new_dir.indexOf('/')) {
					//if we are at top level, we need one slash!
					new_dir += "/";
				}
			} else if (IJ.isWindows() && 4 < dir.length() && dir.startsWith(current_root_dir + file_separator + "..")) { 
				println("\n-->  Such directory doesn't make sense.");
				return true;
			} else if (2 < dir.length() && dir.startsWith(current_root_dir + "..")) { //for unix-like systems and also weird windows entries
				println("\n-->  Such directory doesn't make sense.");
				return true;
			} else if (-1 != dir.indexOf("..")) {
				String target_dir = null;
				if (dir.startsWith("/") || 1 == dir.indexOf(":/")) {
					target_dir = dir;
				} else {
					target_dir = user_dir + file_separator + dir;
				}
				int two_points = target_dir.indexOf("..");
				while(-1 != two_points) {
					String temp = target_dir.substring(0, two_points-1);//skipping last slash, never will make out of bounds exception
					int ending_slash = temp.lastIndexOf('/');
					String parent_dir = temp.substring(0, ending_slash);
					String trailing_stuff = "";
					if (two_points+3 < target_dir.length()) {
						trailing_stuff = file_separator + target_dir.substring(two_points+3);
					}
					target_dir = parent_dir + trailing_stuff;
					two_points = target_dir.indexOf("..");
				}
				//set user_dir
				new_dir = target_dir;
			} else  if (equal(dir, ".")) {
				//do nothing
				return true;
			} else if (dir.startsWith("/") || 1 == dir.indexOf(":/")) { //second statement added to work under windows
					new_dir = dir;
			} else {
				new_dir = user_dir + file_separator + dir;
			}

			File f_new_dir = new File(new_dir);
			if (f_new_dir.exists() && f_new_dir.isDirectory()) {
				user_dir = new_dir;
			} else {
				println("\n-->  No such directory.");
				return true;
			}
			//update current_root_dir value
			if (IJ.isWindows()) {
				current_root_dir = user_dir.substring(0,2); //just the 'c:' or 'd:' etc
			} else {
				current_root_dir = "/"; //unix-like systems
			}

			//notify user about the change
			String dir_name = user_dir;
			//repairing name for root dir
			if (-1 == user_dir.lastIndexOf('/')) {
				dir_name += "/";
			}
			println("\n-->  changed directory to: " + dir_name);
			
			return true;
		}

		//2 - pwd : PRINT CURRENT DIRECTORY
		else if (equal(command, "pwd")) {
			println("\n-->  current directory:");
			String dir_name = user_dir;
			//fix unix-like current_root_dir, which is of zero length
			if (-1 == user_dir.lastIndexOf('/')) {
				dir_name += "/";
			}
			println("--> " + dir_name);
			return true;
		}

		//3 - lsi : LIST IMAGES
		else if (equal(command, "lsi")) {
			File f = new File(user_dir);

			String[] image_name;
			if (2 == al.size()) {
				image_name = f.list(new ImageFileFilter((String)al.get(1)));
			} else {
				image_name = f.list(new ImageFileFilter());
			}
			if (0 < image_name.length) {
				println("-->  Images in "+ user_dir + " :");
				char space = ' ';
				for (int im=0; im<image_name.length; im++) {
					StringBuffer data = new StringBuffer();
					String path = user_dir + file_separator  + image_name[im];
					data.append("\n-->  " + new File(path).length()/1000.0  + " KB");
					while(data.length() < 30) {
						data.append(space);
					}
					data.append(image_name[im]);
					screen.append(data.toString());
				}
			} else {
				screen.append(l + "-->  No [such] images in " + user_dir);
			}
			println("");
			return true;
		}

		// 4 - lsd : LIST DIRECTORIES
		else if (equal(command, "lsd")) {
			println("-->  Directories in " + user_dir + " :");
			File f = new File(user_dir);
			File[] all;
			if (2 == al.size()) {
				all = f.listFiles(new CustomFileFilter((String)al.get(1)));
			} else {
				all = f.listFiles();
			}
			boolean no_dirs = true;
			for (int i=0; i<all.length; i++) {
				if (all[i].isDirectory()) {
					screen.append("\n-->  \t" + all[i].getName() + file_separator);
					no_dirs = false;
				}
			}
			if (no_dirs) {
				screen.append("\n-->  There are no [such] directories in " + user_dir);
			}
			println("");
			return true;
		}

		//5 - ls : LIST ALL FILES
		else if (equal(command, "ls")) {
			String dir = user_dir;
			if (-1 == user_dir.lastIndexOf('/')) {
				dir += "/";
			}
			println("-->  Files in " + dir + " :");
			File f = new File(dir);
			String[] file_name;
			if (2 == al.size()) {
				file_name = f.list(new CustomFileFilter((String)al.get(1)));
			} else {
				file_name = f.list();
			}
			if (0 == file_name.length) {
				println("-->  No [such] file/s.");
				return true;
			}
			String slash = "/";
			String nothing = "";
			char space = ' ';
			for (int im=0; im<file_name.length; im++) {
				StringBuffer data = new StringBuffer();
				String path = user_dir + file_separator  + file_name[im];
				File file = new File(path);
				if (file.isDirectory()) {
					data.append("\n-->                          ");
				} else {
					data.append("\n-->  " + file.length()/1000.0  + " KB");
					while(data.length() < 30) {
						data.append(space);
					}
				}
				data.append(file_name[im]);
				if (file.isDirectory()) {
					data.append(slash);
				}
				screen.append(data.toString());
			}
			println("");
			return true;
		}

		//6 - record : record macro
		else if (equal(command, "record")) {
			if (1 == al.size()) {
				println("-->  A name must be provided: 'record macroname'");
				return true; //true to prevent 'record' being sent to the interpreter //false;
			}
			MacroRecord.setRecording(true);
			String macro_name = (String)al.get(1);
			MacroRecord.makeNew(macro_name);
			println("-->  Recording to: " + macro_name);
			return true;
		}
		//7 - stop : stop recording macro
		else if (equal(command, "stop")) {
			if (MacroRecord.isRecording()) {
				MacroRecord.setRecording(false);
				println("\n-->  Finished recording.");
			} else {
				println("\n-->  Nothing is being recorded.");
			}
			return true;
		}
		//8 - execute a recorded macro
		else if(equal(command, "exec")) {
			String the_macro = null;
			if (1 == al.size()) {
				//if none specified, run last
				the_macro = MacroRecord.getCurrentCode();
			} else {
				//else get the one named
				the_macro = MacroRecord.getCode((String)al.get(1));
			}
			if (the_macro != null) {
				println("\n-->  Executing" + ((al.size()>1)?" " + MacroRecord.autoCompleteName((String)al.get(1)):MacroRecord.getCurrentName()) + ".");
				println(the_macro);
				execMacro(the_macro);
			} else if (1 < al.size()) {
				String[] the_macro2 = new String[2];
				if ((the_macro2 = findMacro((String)al.get(1))) != null) {
					println("-->  Executing " + the_macro2[0]);
					println(the_macro2[1]);
					execMacro(the_macro2[1]);
				} else {
					println("\n-->  No such macro: " + (String)al.get(1));
				}
			} else {
				println("n-->  No macros recorded.");
			}
			return true;
		}
		//9 - print recorded macro list
		else if (equal(command, "list")) {
			String[] macro_name = MacroRecord.getList();
			if (0 == macro_name.length) {
				println("\n-->  Zero recorded macros.");
				return true;
			}
			println("\n-->  Recorded macros:");
			for (int i=0; i<macro_name.length; i++) {
				println("\n-->  \t" + macro_name[i]);
			}
			println("\n");
			return true;
		}
		//10 - save recorded macro
		else if(equal(command, "save")) {
			if (1 == al.size()) {
				println("\n-->  A macro name must be specified.");
				return true;
			}
			MacroRecord mcr = MacroRecord.find((String)al.get(1));
			if (null == mcr) {
				println("\n-->  No recorded macro named " + (String)al.get(1));
				return true;
			}
			String macro_code = mcr.getCodeForSystem();
			saveMacro(macro_code);
			return true;
		}
		//11 - rm : remove file
		else if (equal(command, "rm")) {
			if (1 == al.size()) {
				println("\n-->  Usage:  rm <file_name>\n-->    No asterisks allowed.");
				return true;
			}

			//check if TrashCan exists, create it otherwise
			File trashcan = new File(trash_can);
			if (!trashcan.exists()) {
				boolean check = trashcan.mkdir();
				if (!check) {
					println("\n-->  Trash Can does not exist and could not be created.");
				}
				//sleep  1/10 of a second to let the system create the dir before using it
				try {
					Thread.currentThread().sleep(100);
				}catch(InterruptedException ie) {}
			}
			
			if (1 == al.size()) {
				println("\n-->  rm : A file name must be specified.");
				return true;
			}
			String file_name = (String)al.get(1);
			
			//wild cards not allowed
			if (-1 != file_name.indexOf('*')) {
				println("\n--> Wild cards '*' not allowed in rm command.");
				return true;
			}
			
			File f;
			//adjust file_name
			if (file_name.startsWith("/")) {
				f = new File(file_name);
			} else {
				file_name = user_dir + file_separator + file_name;
				f = new File(file_name);
			}
			if (f.exists()) {
				if (f.isDirectory()) {
					String[] list = f.list();
					if (0 != list.length) {
						if (2 == list.length) {
							if (equal(list[0], ".") && equal(list[1], "..")) {
							//dir is empty. Ok to delete
							} else {
								println("\n-->  " + file_name + " is a non-empty directory! Deleting stopped");
								return true;
							}
						}
					}
				}
				
				String trashed_name = trash_can + file_separator + f.getName();
				File file_trashed = new File(trashed_name);
				int i = 1;
				//prevent overwriting
				while(file_trashed.exists()) {
					trashed_name = trash_can + file_separator + f.getName() + "_" + i;
					file_trashed = new File(trashed_name);
					i++;
				}
				if (f.renameTo(file_trashed)) {
					println("\n-->  " + file_name.substring(file_name.lastIndexOf(file_separator)+1) + " successfully moved to the trash can.");
				} else {
					println("\n-->  " + file_name.substring(file_name.lastIndexOf(file_separator)) + " could NOT be trashed.");
				}
			} else {
				println("\n-->  " + file_name + " does not exist!");
			}
			return true;
		}
		//12 - mkdir : make new directory
		else if (equal(command, "mkdir")) {
			if (1 == al.size()) {
				println("\n-->  Usage : mkdir <new_dir_name>");
				return true;
			}
			File f;
			String dir_name = (String)al.get(1);
			if (dir_name.startsWith("/")) {
				f = new File(dir_name);
			} else {
				dir_name = user_dir + file_separator + dir_name;
				f = new File(dir_name);
			}
			if (f.exists()) {
				println("\n-->  Directory " + dir_name + " already exists!");
			} else {
				if (f.mkdir()) {
					println("\n-->  Directory " + dir_name + " sucessfully created");
				} else {
					println("\n-->  Could NOT create the directory!");
				}
			}
			
			return true;
		}

		//13 - magic : toggle capitalizing words inside the first pair of " " inside a command
		else if (equal(command, "magic")) {
			magic = !magic;
			println("\n-->  magic is " + (magic?"ON":"OFF"));
			return true;
		}

		//14 - erase : erase lines from a recorded macro
		else if (equal(command, "erase")) {
			if (al.size() < 2) {
				//erase the last line
				MacroRecord.eraseLinesFromCurrent(1);
				return true;
			}
			if (equal((String)al.get(1), "-l") && 2 == al.size()) {
				println("\n--> Line number not specified!");
				return true;
			}
			if (equal((String)al.get(1), "-l")) {
				//erase line number specified at al.get(2)
				try {
					int line = Integer.parseInt((String)al.get(2));
					if (MacroRecord.eraseLineFromCurrent(line)) {
						println("\n-->  line " + line + " erased.");
					} else {
						println("\n--> line " + line + " out of range.\n");
					}
				}catch(Exception e) {
					println("\n--> Supplied argument is not a valid number.\n");
				}
			} else {
				try {
					int num_lines = Integer.parseInt((String)al.get(1));
					int erased_lines = MacroRecord.eraseLinesFromCurrent(num_lines);
					if (-1 == erased_lines) {
						println("\n-->  All lines erased.");
					} else if (-2 == erased_lines) {
						println("\n-->  No recorded macro to edit.");
					} else {
						println("\n-->  " + erased_lines + " lines erased.");
					}
				}catch(Exception e) {
					println("\n--> Supplied argument is not a valid number.");
				}
			}
			return true;
		}

		//15 - front : bring a macro to the front to edit it
		else if (equal(command, "front")) {
			boolean activate = false;
			if (al.size() < 2) {
				//activate = MacroRecord.setActive(null);
				String[] list = MacroRecord.getList();
				if (list.length == 0) {
					println("\n-->  No recorded macro.");
				} else {
					println("\n-->  Front macro is " + MacroRecord.getCurrentName() + " and it is " + ((MacroRecord.isRecording())?"":"not") + " being edited.");
				}
			} else {
				activate = MacroRecord.setActive((String)al.get(1));
			}
			if (activate) {
				println("\n-->  Now recording on: " + (String)al.get(1) + l);
			}
			return true;
		}

		//16 - view : view a macro without executing it
		else if(equal(command, "view")) {
			MacroRecord mc;
			if (al.size() == 1) {
				mc = MacroRecord.getCurrent();
			} else {
				mc = MacroRecord.find((String)al.get(1));
			}
				
			if (null != mc) {
				println("\n-->  Macro : " + mc.getName());
				println("\n" + mc.getCode());
			} else if (1 < al.size()) {
				//show a macro from the ImageJ/macros/ folder
				String[] a_macro = null;
				if ((a_macro = findMacro((String)al.get(1))) != null) {
					println("\n-->  Macro : " + a_macro[0]);
					println(a_macro[1]);
				} else {
					println("\n-->  No such macro: " + (String)al.get(1));
				}
			} else {
				println("\n-->  No macro recorded or no such macro.");
			}
			return true;
		}

		//17 - help : print list of commands and some examples
		else if (equal(command, "help")) {
			println("\n-->  Command line interface for ImageJ");
			println("-->  -- Albert Cardona 2004 --");
			println("-->  Just type in any ImageJ macro code and it will be executed after pushing intro.");
			println("-->  Multiline macro commands can be typed by adding an ending \\");
			println("-->  Unix-like basic shell functions available.");
			println("-->  TAB key expands file names and macro functions names.");
			println("-->  UP and DOWN arrows bring back entered commands.");
			println("-->  Mouse selecting text brings contextual menu.");
			println("-->  \n-->  Macro Commands:");
			println("-->    record <macro_name> : start recording a macro.");
			println("-->    stop : stop the recording.");
			println("-->    view [<macro_name>] : print the macro code from macro macro_name without executing it, or from the front macro.\n-->       An attempt will be made to match uncompleted names\n-->       from the recorded list, the current directory, and the ImageJ macros directory.");
			println("-->    list : list all recorded macros.");
			println("-->    save <macro_name>: save recorded macro to a file.");
			println("-->    exec [<macro_name>] : execute a recorded macro macro_name, or the front macro.\n-->       An attempt will be made to match uncompleted names\n-->       from the recorded list, the current directory, and the ImageJ macros directory.");
			println("-->    front [<macro_name>] : start editing macro macro_name, or just print who is the front macro.");
			println("-->    erase [-l line_number]|[num_lines] : erase line line_number or erase num_lines starting from the end, or just the last line.");
			println("-->    toggle_edit : enable/disable direct screen editing.");
			println("-->    magic : toggle magic ON/OFF. When on, the program attempts to guess several things \n-->       and transform the input. Example: dc invert  -> doCommand(\"Invert\") ,\n-->       or makeRectangle 10,10,30,40 -> makeRectangle(10,10,30,40)");
			println("-->    doc [<url>]: show ImageJ website macro documentation pages, or a given url.");
			println("-->  \n-->  Shell-like Commands:");
			println("-->    open <image_file/s>|<directory> : open an image file or a list of space-separated image names or paths.\n-->      Accepts wildcard (*) at start, end, or both.\n-->      Will print the correct macro code to open the image.\n-->      Alternatively, it will open as a stack all images in the specified directory.\n-->      Without arguments, opens current directory images as a stack.");
			println("-->    ls [<file_name>]: list all files in working directory.");
			println("-->    lsi [<file_name>]: list images in working directory.");
			println("-->    lsd [<file_name>]: list directories in the working directory.");
			println("-->    pwd : print working directory.");
			println("-->    cd <directory> : change directory.");
			println("-->    rm <file_name> : move file_name to the trash can located at this plugin folder.");
			println("-->    empty_trash : empty the CLI Trash Can.");
			println("-->    clear : clear screen.");
			println("-->    screenshot [window_name [target_file_name [delay_in_seconds]]] : idem.");
			println("-->    show [directory [file [time]]]: slide show on current or specified directory,\n-->      of files <file> (accepts *) and every <time> (in seconds).");
			println("-->  \n-->  Contextual Menu:");
			println("-->    Select any piece of text from the screen.\n-->    Lines starting with '-->  ' will be ignored,\n-->    as well as the starting '> ' and ending '\\' characters.");
			println("-->      Execute Selection : idem");
			println("-->      Record : make a new macro from selection.");
			println("-->      Copy : copy selection to system paste buffer.");
			println("-->      Save Selection : open file dialog to save selection as a macro text file.");
			println("-->      Save & Exec Selection : idem.");
			println("-->  ");
			
			return true;
		}

		//18 - empty_trash : empty the terminal trash = delete all files from trash_can
		else if(equal(command, "empty_trash")) {
			File trash = new File(trash_can);
			File[] f = trash.listFiles();
			int failed = 0;
			for (int i=0; i<f.length; i++) {
				String file_name = f[i].getName();
				boolean check = false;
				if (!equal(file_name, ".") && !equal(file_name, "..")) {
					check = f[i].delete();
				}
				if (false == check) {
					println("\n-->  Could not delete file " + file_name);
					failed++;
				}
			}
			if (failed == 0) {
				println("\n-->  Trash can successfully emptied.");
			} else {
				println("\n-->  Some files may have not been deleted.");
			}

			return true;
		}

		//19 - clear : clear screen
		else if(equal(command, "clear")) {
			screen.setText("");
			return true;
		}

		//20 - screenshot capture
		else if(equal(command, "screenshot")) {
			//1 : frame
			//2 : file_name
			//3 : seconds
			
			Screenshot s = null;
			if (1 == al.size()) {
				s = new Screenshot(null, 0, user_dir, null);
			} else if (2 == al.size()) {
				java.awt.Frame frame = WindowManager.getFrame((String)al.get(1));
				if (null != frame) {
					s = new Screenshot(frame, 0, user_dir, null);
				} else {
					println("\n-->  No such window: " + (String)al.get(1));
				}
			} else if (3 == al.size()) {
				java.awt.Frame frame = WindowManager.getFrame((String)al.get(1));
				if (null != frame) {
					s = new Screenshot(frame, 0, user_dir, (String)al.get(2));
				} else {
					println("\n-->  No such window: " + (String)al.get(1));
				}
			} else if (4 == al.size()) {
				java.awt.Frame frame = WindowManager.getFrame((String)al.get(1));
				if (null != frame) {
					try {
						s = new Screenshot(frame, Integer.parseInt((String)al.get(3)), user_dir, (String)al.get(2));
					}catch(NumberFormatException nfe) {
						println("\n-->  Wrong number format for seconds. Stopping.");
					}
				} else {
					println("\n-->  No such window: " + (String)al.get(1));
				}
				
			}
			s.setOut(screen); //doesn't work?
			new Thread(s).start();
			println(s.getReport());

			return true;
		}

		//21 - mv : move file to new file name or into a directory
		else if(equal(command, "mv")) {
			//TODO:
			//	- allow ".." to be converted to parent dir
			//	- allow wildcards to move several files (only for first arg, and only into a directory)
			
			try {
			if(2 < al.size()) {
				String file_name = (String)al.get(1);
				//prepare the file_name
				if (0 == file_name.indexOf('/') || 1 == file_name.indexOf(':')) {
					//given path starts from root
					//do nothing
				} else {
					//given path needs user_dir to be prepended
					file_name = user_dir + file_separator + file_name;
				}
				//prepare the new_file_name
				String new_file_name = fixDir((String)al.get(2));
				if (null == new_file_name) {
					println("\n-->  Incorrect target file_name or dir. File/s not moved.");
					return true;
				}
				//asterisks not allowed in new_file_name
				if (-1 != new_file_name.indexOf('*')) {
					println("\n-->  No wildcards allowed in target file_name or dir");
					return true;
				}
				File new_file = new File(new_file_name);
				
				//collect the file_names from file_name (if there are any asterisks)
				String files_dir = file_name.substring(0, file_name.lastIndexOf('/'));
				File f_files_dir = new File(files_dir);

				String[] file_names = f_files_dir.list(new CustomFileFilter(file_name.substring(file_name.lastIndexOf('/')+1)));
				
				//check whether there's any file to move
				if (0 == file_names.length) {
					println("\n-->  No such file/s: \n-->  " + file_name);
					return true;
				}
				
				if (new_file.exists() && new_file.isDirectory()) {
					//attempt to move files into a directory
					for (int i=0; i<file_names.length; i++) {
						File source_file = new File(files_dir + file_separator + file_names[i]);
						String target_file_name = new_file_name + file_separator + file_names[i];
						File target_file = new File(target_file_name);
						if (target_file.exists()) {
							//prevent overwriting or error
							println("\n-->  A file named \n-->  " + target_file.getName() + "\n-->  already exists in target directory \n-->  " + new_file_name);
							continue;
						}
						boolean check = source_file.renameTo(target_file);
						if (check) {
							println("\n-->  File successfully moved to:\n-->  " + target_file_name);
						} else {
							println("\n-->  Could not move the file \n-->  "+ target_file_name  + "\n-->       into directory " + new_file_name);
						}

					}
				} else if (1 == file_names.length && !new_file.isDirectory()) {
					//attempt to rename the single file
					if (new_file.exists()) {
						//prevent overwriting
						println("\n-->  A file named " + new_file.getName() + " already exists!\n-->  Not moving the file " + file_names[0]);
						return true;
					} else {
						//attempt to move the file
						File source_file = new File(files_dir + file_separator + file_names[0]);
						boolean check = source_file.renameTo(new_file);
						if (check) {
							println("\n-->  File successfully moved to:\n-->  " + new_file_name);
						} else {
							println("\n-->  Could not move the file \n-->  "+ file_names[0]  + "\n--> to file " + new_file_name);
						}
					}
				}
				return true;

			} else {
				println("\n-->  Usage: mv <file_name> <dir | new_file_name>");
				return true;
			}
			}catch(Exception e) {
				IJ.write("Some error ocurred:\n" + new TraceError(e));
			}
			return true;
		}

		//22 - doc : macro dictionary
		else if (equal(command, "doc")) {
			String url = "http://rsb.info.nih.gov/ij/developer/macro/macros.html";
			if (al.size() == 2) {
				url = (String)al.get(1);
			}
			println("\n-->  Opening " + url);
				
			try {
				JEditorPane jep = new JEditorPane(url);
				jep.setPreferredSize(new Dimension(500, 600));
				jep.setEditable(false);
				jep.addHyperlinkListener(new HyperlinkAdapter(jep));
				JScrollPane scroll = new JScrollPane(jep);
				scroll.setPreferredSize(new Dimension(500, 600));
				JFrame f = new JFrame("Macro Functions List");
				f.setSize(new Dimension(500, 600));
				f.getContentPane().add(scroll);
				f.pack();
				f.show();
			}catch(Exception ioe) {
				println("\n-->  Dictionary could not be found at url:\n-->  " + url);
			}
			return true;
		}
		//23 - open : special open that will take only a directory as an option and open its files in a stack
		else if(equal(command, "open")) {
			if (al.size() < 2) {
				//continue normally into specialEditing of the 'open' command
				return false;
			}
			String dir_path = fixDir((String)al.get(1));
			File dir = new File(dir_path);
			if (!(dir.exists() && dir.isDirectory())) {
				//continue normally into specialEditing
				return false;
			}
			OpenDirectory od = new OpenDirectory(dir_path, OpenDirectory.STACK);
			println("\n-->  " + od.getMessage());
			return true;
		}
		//24 - toggle_edit : enable direct editing of the screen
		/*
		else if (equal(command, "toggle_edit")) {
			if (enable_direct_editing) {
				enable_direct_editing = false;
				println("\n-->  Direct screen edition disabled.");
			} else {
				enable_direct_editing = true;
				screen.append("\n-->  Direct screen edition enabled.");
			}
			return true;
		}
		*/
		//25 - show : idem
		else if (equal(command, "show")) {
			String the_macro = null;
			if (al.size() < 2) {
				//if no arguments
				//do slide show on the current directory with default settings
				the_macro = "run(\"Slide Show\", \"folder="+ user_dir  +" file=* time=4\")\n";
			} else {
				//if at least one argument
				//do slide show on the specified directory, with specified options if any
				the_macro = "run(\"Slide Show\", \"folder="+ fixDir((String)al.get(1));
				if (al.size() > 2) {
					the_macro += " file=" + (String)al.get(2);
				}
				if (al.size() > 3) {
					the_macro += " time=" + (String)al.get(3);
				}
				the_macro += "\")\n";
			}
			println(the_macro);
			execMacro(the_macro);
			return true;
		}

		//default: means it's not a shell command
		return false;
	}

	class HyperlinkAdapter implements HyperlinkListener {

		JEditorPane jep;
		
		HyperlinkAdapter(JEditorPane jep) {
			this.jep = jep;
		}
		
		public void hyperlinkUpdate(HyperlinkEvent he) {
			if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				try {
					jep.setPage(he.getURL());
				}catch(Exception e) {
					IJ.showMessage("Can't follow link.");
				}
			}
		}
	}
	

	boolean equal(String a, String b) {
		return a.toLowerCase().hashCode() == b.toLowerCase().hashCode();
	}


	String fixDir(String dir) {
		//function to fix dir path
		String fixed_dir = dir;
		if (equal(dir, "..")) {
			//check whether we are at top directory already
			
			//if (IJ.isWindows()) {
				if (equal(user_dir, current_root_dir)) { //TODO: test
				//if (equal(dir, current_root_dir + file_separator))
					println("\n-->  Such directory doesn't make sense.");
					return null;
				}
			//} else {
			//	//does this lines below make sense ? //TODO: test
			//	if (equal(dir, current_root_dir)) {
			//		screen.append("\n-->  Such directory doesn't make sense.");
			//		return null;
			//	}
			//}
			//else move up one dir from user_dir
			int last_slash = user_dir.lastIndexOf("/");
			fixed_dir = user_dir.substring(0, last_slash);
			if (-1 == fixed_dir.indexOf('/')) {
				//if we are at top level, we need to append one slash because current_root_dir does not store the slash for windows
				fixed_dir += "/";
			}
			//IJ.write("fix1: " + fixed_dir);
		} else if (IJ.isWindows() && 4 < dir.length() && dir.startsWith(current_root_dir + file_separator+ "..")) { 
			println("\n-->  Such directory doesn't make sense.");
			return null;
		} else if (2 < dir.length() && dir.startsWith(current_root_dir + "..")) { //for unix-like systems
			println("\n-->  Such directory doesn't make sense.");
			return null;
		} else if (-1 != dir.indexOf("..")) {
			//repair all instances of ".." in the string 'dir'
			String target_dir = null;
			if (dir.startsWith("/") || 1 == dir.indexOf(":/")) {
				target_dir = dir;
			} else {
				target_dir = user_dir + file_separator + dir;
			}
			int two_points = target_dir.indexOf("..");
			while(-1 != two_points) {
				String temp = target_dir.substring(0, two_points-1);//skipping last slash, never will make out of bounds exception
				int ending_slash = temp.lastIndexOf('/');
				String parent_dir = temp.substring(0, ending_slash);
				String trailing_stuff = "";
				//if any dirs specified after the ".." then reappend them
				if (two_points+3 < target_dir.length()) {
					trailing_stuff = file_separator + target_dir.substring(two_points+3);
				}
				target_dir = parent_dir + trailing_stuff;
				two_points = target_dir.indexOf("..");
			}
			//set user_dir
			fixed_dir = target_dir;
			return fixed_dir;
		} else  if (equal(dir, ".")) {
			return user_dir;
		} else if (dir.startsWith("/") || 1 == dir.indexOf(":/")) { //second statement added to work under windows
			//nothing needs to be repaired on dir
			fixed_dir = dir;
		} else {
			//if dir does not start with root directory, preappend it
			if (equal(user_dir, current_root_dir)) { //user_dir does not store the file_separator as the last char
			//if ((0 == user_dir.lastIndexOf('/') && 1 == dir.length())
			// || (1 == user_dir.lastIndexOf(':') && 2 == dir.length()))
				//user dir is root dir
				if (IJ.isWindows()) {
					fixed_dir = user_dir + file_separator  + dir;
			//IJ.write("fix4: " + fixed_dir);
				} else {
					//for unix-like systems, do not place a file_separator char since the root dir itself is such a char
					fixed_dir = user_dir + dir;
			//IJ.write("fix5: " + fixed_dir);
				}
			} else {
				//user dir is not equal to root dir
				fixed_dir = user_dir + file_separator + dir;
			//IJ.write("fix6: " + fixed_dir + "    dir: __" + dir);
			}
		}

		return fixed_dir;
	}
	

	String[] findMacro(String name) {
		//return macro name and macro code
		
		//Look for macros in this order:
		//1 - recorded macros : done before
		//2 - user_dir
		//3 - ImageJ macro's folder

		String[] macro = null;

		//2 - find in user_dir
		if (null == macro) {
			macro = findMacro(user_dir, name);
		}
		//3 - find the named macro in the ImageJ macro folder
		if (null == macro) {
			macro = findMacro(dir_macros, name);
		}

		return macro;
	}

	String[] findMacro(String dir, String name) {
		String[] macro = new String[2]; //macro name and code
		File f_dir_macros = new File(dir);
		String[] names = f_dir_macros.list();
		for (int i=0; i<names.length; i++) {
			if(name.length() <= names[i].length() && names[i].startsWith(name)) {
				try {
					//read the first one that matches
					String f_dir_macros_canonical_path = fixWindowsPath(f_dir_macros.getCanonicalPath());
					if (IJ.isWindows()) {
						//avoiding windows path escape chars havoc
						//TODO this may no longer be necessary as it is done above.
						f_dir_macros_canonical_path.replace('\\', '/');
					}
					macro[0] = names[i];
					macro[1] = readFile(f_dir_macros_canonical_path + file_separator + names[i]);
				}catch(Exception e) {
					println("\n-->  Macro file " + name + " or similar could not be found or read in directory " + f_dir_macros.getName());
				}
			//finish here:
			return macro;
			}
		}
		//default:
		return null;
	}
	

	class CustomMouseAdapter extends MouseAdapter {

		public void mouseReleased(MouseEvent me) {
			selection = screen.getSelectedText();
			//show popup menu
			if (null != selection && 0 < selection.length()) {
				popup_menu.show(screen, me.getX(), me.getY());
			}
			//set focus to prompt
			/*
			if(!enable_direct_editing) {
				prompt.requestFocus();
			}*/
		}
	}

	class PopupListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			String action = ae.getActionCommand();
			try {

			//get macro code
			String macro = getCleanLinesFromSelection();

			if (null == macro || 0 == macro.length()) {
				return;
			}

			if (equal(action, "Execute Selection")) {
				execMacro(macro);
			}
			else if (equal(action, "Record")) {
				String macro_name = promptForName(null, "Macro name: ");
				while (MacroRecord.exists(macro_name)) {
					macro_name = promptForName("Macro " + macro_name + " exists!", "Macro name: ");
				}
				if (null != macro_name) {
					new MacroRecord(macro_name, macro);
					println("\n-->  Recorded new macro as " + macro_name);
				}
			}
			else if (equal(action, "Copy")) {
				//put macro into paste buffer
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable transfer = new StringSelection(macro);
				cb.setContents(transfer, (ClipboardOwner)transfer);
			}
			else if (equal(action, "Save Selection")) {
				//save macro into a new macro file in the macros dir
				saveMacro(macro);
			}
			else if (equal(action, "Save & Exec Selection")) {
				saveMacro(macro);
				execMacro(macro);
			}
			
			}catch(Exception e) {
				IJ.write("Problems in popup menu actions: " + new TraceError(e));
			}
		}

	}

	String promptForName(String extra, String label) {
		GenericDialog gd = new GenericDialog("Name it");
		if (null != extra) {
			gd.addMessage(extra);
		}
		gd.addStringField(label, "");
		gd.showDialog();
		if (gd.wasCanceled()) {
			return null;
		}
		return gd.getNextString();
	}

	void execMacro(String macro) {
		String macrop = macro;
		//execute new Intepreter
		if (0 != macro.length()) {
			try {
				//if selection is an image file, then open it!
				//debug://println("user_dir: " + user_dir + "\n" + "macro: " + macro);
				if (new ImageFileFilter().accept(new File(user_dir), macro)) {
					//macrop = "open(\"" + fixWindowsPath(new File(user_dir).getCanonicalPath()) + file_separator + macro + "\");";
					println(macrop);
				}/* else {
					println("debug: MACRO IS NOT AN IMAGE \n");
				}*/
				new Interpreter().run(macrop);
			}catch(Exception e) {
				// Mark line as invalid
				// TODO should mark previous lines as well if this was part of a multiline
				valid_lines.set(valid_lines.size() -1, false);
				//ImageJ has its own way of notifying errors
				println("\n-->  macro not executable or canceled.\n");
				
				if (!magic) {
					int ispace = macro.indexOf(' ');
					if (-1 != ispace || ispace < macro.indexOf('(')) {
						println("\n-->    Try to toggle magic ON by typing:  magic");
					}
				}
				allow_print = false;
			}
			screen.setCaretPosition(screen.getText().length());
		}
	}

	void saveMacro(String macro) {
		FileDialog fd = new FileDialog(window, "Save", FileDialog.SAVE);
		fd.setDirectory(user_dir);
		fd.show();

		if (null == fd.getFile()) return;
		String file_path = fixWindowsPath(fd.getDirectory()) + fd.getFile();
		if (file_path.length() > 3 && !file_path.endsWith(".txt")) {
			file_path += ".txt";
		}
		boolean check = saveFile(file_path, macro);
		if (check) {
			println("\n-->  Macro saved as " + file_path);
		} else {
			println("\n-->  Macro NOT saved.");
		}
		screen.setCaretPosition(screen.getText().length());
	}
	
	boolean saveFile(String file_path, String file_contents) {
		try {
			File f = new File(file_path);
			int i = 1;
			int dot = file_path.lastIndexOf(".");
			String extension = file_path.substring(dot);
			while(f.exists()) {
				file_path = file_path.substring(0, dot) + "_" + i + extension;
				f = new File(file_path);
				i++;
			}
			
			
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f),file_contents.length()));

			dos.writeBytes(file_contents);
			dos.flush();

			return true;

		} catch(SecurityException se) {
			IJ.write(se + "\nError at d.o.s. SE.\n" + new TraceError(se));
		}
		catch (IOException ioe) {
			IJ.write(ioe + "\nError at d.o.s. IOE.\n" + new TraceError(ioe));
		}
		return false;
	}

	String readFile(String file_path) throws FileNotFoundException {
		File f = new File(file_path);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String one_line = "";
		ArrayList macro_code = new ArrayList();
		do {
			try {
				one_line = br.readLine();
			}catch(Exception e) {
				println("\n-->  Error when reading file " + file_path);
			}
			if (one_line != null) {
				macro_code.add(one_line + l);
			}
		}while(one_line != null);
		try {
			br.close();
		}catch(Exception e) {
			println("\n-->  Error when closing reading buffer for " + file_path);
		}

		//pass the macro to the MacroRecord
		MacroRecord macro = new MacroRecord(f.getName(), macro_code);
		return macro.getCode();
	}

	String getCleanLinesFromSelection() {
		try {
		//1 - break in lines
		ArrayList ar = new ArrayList();
		if (-1 == selection.indexOf(l)) {
			//just one line
			ar.add(selection);
		} else {
			int start = selection.indexOf(l);
			int end = selection.indexOf(l, start+1);
			ar.add(selection.substring(0, start)); //at least there is one line break
			while(-1 != end) {
				ar.add(selection.substring(start+1, end));
				start = end;
				end = selection.indexOf(l, end+1);
			}
			String last = selection.substring(start+1);
			if(0 < last.length()) ar.add(last);
		}

		//2 - check startsWith("> ") and endsWith("\\")
		String macro = "";
		String newline = System.getProperty("line.separator"); //now correct lines under windows OS
		for (int i=0; i<ar.size(); i++) {
			String line = (String)ar.get(i);
			if (0 == line.length()) {
				continue;
			}
			if (line.length() > 4 && line.startsWith("-->  ")) {
				//ignore this message lines
				continue;
			}
			if (line.startsWith("> ")) {
				line = line.substring(2);
			}
			if (line.endsWith("\\")) {
				line = line.substring(0, line.length()-1);
			}
			macro += line + newline;//((i != ar.size()-1)? newline:"");
		}
		return macro;

		}catch(Exception e) {
			IJ.write("Problems at cleaning lines:\n" + new TraceError(e));
		}
		//default;
		return null;
	}

	public String getImportStatement(String packageName, Iterable<String> classNames) {
		throw new RuntimeException("Cannot import classes into the macro interpreter");
	}
}
