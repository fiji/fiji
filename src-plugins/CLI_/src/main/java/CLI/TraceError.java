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


/* VERSION: 1.01
 * RELEASE DATE: 2004/10/01
 * AUTHOR: Albert Cardona at albert at pensament.net
 */

import java.io.CharArrayWriter;
import java.io.PrintWriter;

public class TraceError {

	String error_message = null;
	
	public TraceError(Throwable e) {

		CharArrayWriter caw = new CharArrayWriter();
		PrintWriter pw = new PrintWriter(caw);
		e.printStackTrace(pw);
		String s = "Some problem occurred:\n" + caw.toString();
		if (isMacintosh()) {
			if (s.indexOf("ThreadDeath")>0)
				;//return null;
			else s = fixNewLines(s);
		}
		
		error_message = s; //System.out.println(s);
	}
	
	public String toString() {
		return error_message;
	}

    /** Converts carriage returns to line feeds. Copied from ij.util.tools by Wayne Rasband*/
    
    String fixNewLines(String s) {
	char[] chars = s.toCharArray();
	for (int i=0; i<chars.length; i++)
	    {if (chars[i]=='\r') chars[i] = '\n';}
	return new String(chars);
    }

    static String osname;
    static boolean isWin, isMac;

    static {
	osname = System.getProperty("os.name");
	isWin = osname.startsWith("Windows");
	isMac = !isWin && osname.startsWith("Mac");
    }

    boolean isMacintosh() {
	return isMac; 
    }

}
