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


/* VERSION: 1.0
 * RELEASE DATE: 2004-09-27
 * AUTHOR: Albert Cardona at albert at pensament.net
 */


import java.util.ArrayList;
import java.util.StringTokenizer;
import ij.IJ;

class MacroRecord {

	private static boolean recording = false;
	private static ArrayList macros = new ArrayList();
	private static int active = -1;
	
	//private StringBuffer sb_macro;
	private ArrayList code;
	private String name;
	
	//constructor used to record a macro on the fly
	MacroRecord(String name) {
		//sb_macro = new StringBuffer();
		code = new ArrayList();
		this.name = name;
	}

	//constructor used to load macro from a file
	MacroRecord(String name, ArrayList code_lines/*StringBuffer sb_macro*/) {
		//this.sb_macro = sb_macro;
		this.code = code_lines;
		this.name = name;
		macros.add(this);
		active = macros.size()-1;
	}

	//constructor used to load macro from a String taken from a selection
	MacroRecord(String name, String the_macro) {
		this.name = name;
		this.code = new ArrayList();
		StringTokenizer st = new StringTokenizer(the_macro, "\n");
		while(st.hasMoreElements()) {
			this.code.add(st.nextToken() + "\n");
		}
		macros.add(this);
		active = macros.size()-1;
	}

	String getName() {
		return name;
	}
	
	void append(String line) {
		//sb_macro.append(line);
		code.add(line);
	}
	
	String getCode() {
		//return sb_macro.toString();
		StringBuffer sb = new StringBuffer();
		int n_lines = code.size();
		for (int i=0; i<n_lines; i++) {
			sb.append((String)code.get(i));//.append("\n");
		}
		return sb.toString();
	}

	String getCodeForSystem() {
		String newline = System.getProperty("line.separator");
		String code1 = getCode();
		int start = code1.indexOf('\n');
		int end = code1.indexOf('\n', start+1);
		String code2 = code1.substring(0,start) + newline;
		while(-1 != end) {
			code2 += code1.substring(start+1, end) + newline;
			start = end;
			end = code1.indexOf('\n', end+1);
		}
		String last = code1.substring(start+1);
		if (0 < last.length()) { 
			code2 += code1.substring(start+1);
		}
		
		return code2;
	}

	static void appendToCurrent(String line) {
		//((MacroRecord)macros.get(macros.size()-1)).append(line);
		((MacroRecord)macros.get(active)).append(line);
	}

	static boolean eraseLineFromCurrent(int line) {
		//uses line 1 as the first line and code.size() as the last line
		MacroRecord mc = (MacroRecord)macros.get(active);
		if (null != mc && line <= mc.code.size() && line > 0) {
			mc.code.remove(line-1); //-1 because of the above comment
			return true;
		}
		return false;
	}

	static int eraseLinesFromCurrent(int num_lines) {
		//erases num_lines starting from last line
		int n = num_lines;
		MacroRecord mc = (MacroRecord)macros.get(active);
		if (null == mc) {
			return -2;
		}
		if (n > mc.code.size()) {
			mc.code.clear();
			return -1;
		} else {
			while(n > 0) {
				mc.code.remove(mc.code.size()-1);
				n--;
			}
			return num_lines;
		}
	}

	static void setRecording(boolean b) {
		recording = b;
	}
	
	static boolean isRecording() {
		return recording;
	}

	static void makeNew(String name) {
		macros.add(new MacroRecord(name));
		active = macros.size()-1;
	}

	static MacroRecord getCurrent() {
		if (macros.size() > 0) {
			return (MacroRecord)macros.get(active);
		} else {
			return null;
		}
	}

	static String getCurrentName() {
		MacroRecord mc = getCurrent();
		if (null != mc) {
			return mc.getName();
		} else {
			return "none";
		}
	}

	static String getCurrentCode() {
		//returns the code
		//return ((MacroRecord)macros.get(macros.size()-1)).getCode();
		if (-1 == active) {
			return null;
		} else {
			return ((MacroRecord)macros.get(active)).getCode();
		}
	}

	static String getCode(String the_name) {
		//returns the code
		MacroRecord[] the_macros = new MacroRecord[macros.size()];
		macros.toArray(the_macros);
		int name_size = the_name.length();
		for (int i = the_macros.length-1; i>-1; i--) {
			if(the_macros[i].getName().length() >= name_size
					&& the_macros[i].getName().startsWith(the_name)) {
				//return the first good match
				return the_macros[i].getCode();
			}
			/*if (the_macros[i].getName().hashCode() == the_name.hashCode()) {
				return the_macros[i].getCode();
			}*/
		}
		return null;
	}
	
	static boolean setActive(String the_name) {
		MacroRecord mr = find(the_name);
		if (null != mr) {
			active = macros.indexOf(mr);
			recording = true;
			return true;
		} else {
			//activate last one if any
			if (macros.size() > 0) {
				active = macros.size()-1;
				recording = true;
				return true;
			}
		}
		//default
		return false;
	}
	
	static MacroRecord find(String the_name) {
		if (null == the_name) {
			return null;
		}
		MacroRecord[] the_macros = new MacroRecord[macros.size()];
		macros.toArray(the_macros);
		int name_size = the_name.length();
		for (int i = the_macros.length-1; i>-1; i--) {
			if(the_macros[i].getName().length() >= name_size
					&& the_macros[i].getName().startsWith(the_name)) {
				//return the first good match
				return the_macros[i];
			}
		}
		//default
		return null;
	}

	static String autoCompleteName(String the_name) {
		if (null == the_name) {
			return null;
		}
		MacroRecord[] the_macros = new MacroRecord[macros.size()];
		macros.toArray(the_macros);
		int name_size = the_name.length();
		for (int i = the_macros.length-1; i>-1; i--) {
			if(the_macros[i].getName().length() >= name_size
					&& the_macros[i].getName().startsWith(the_name)) {
				//return the first good match
				return the_macros[i].getName();
			}
		}
		//default
		return null;
	}
	
	static boolean exists(String the_name) {
		if (null != find(the_name)) {
			return true;
		}
		return false;
	}

	static String[] getList() {
		MacroRecord[] the_macros = new MacroRecord[macros.size()];
		macros.toArray(the_macros);
		String[] names = new String[the_macros.length];
		for (int i=0; i < the_macros.length; i++) {
			names[i] = the_macros[i].getName();
		}
		return names;
	}
}
