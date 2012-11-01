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

import java.io.File;
import java.io.FilenameFilter;

public class CustomFileFilter implements FilenameFilter {
	
	String file_name;
	final String ASTERISK = "*";
	
	CustomFileFilter(String file_name) {
		this.file_name = file_name;
	}

	CustomFileFilter() {
		file_name = null;
	}
	
	public boolean accept(File dir, String name) {

		if (null != file_name) {
			// 'open *'	if file_name is an ASTERISK
			if (equal(file_name, ASTERISK)) {
				return true;
			}
			else if (name.length() >= file_name.length()) {
				if (equal(name, file_name)) {
					return true;
				}
				// 'open *name*'	file_name starts and ends with ASTERISK and name contains file_name
				else if (file_name.startsWith(ASTERISK)
				&& file_name.endsWith(ASTERISK)
				&& -1 != name.indexOf(file_name.substring(1, file_name.length()-1))
		 		) {
					return true;
				}
				//'open name*'		name starts with file_name and ends with ASTERISK
				else if (file_name.endsWith(ASTERISK)
				&& name.startsWith(file_name.substring(0,file_name.length()-1))
		 		) {
					return true;
				}
				//'open *name'		name ends with file_name
				else if (file_name.startsWith(ASTERISK) 
				&& name.endsWith(file_name.substring(1))) {
					return true;
				}
			//reject
			} else {
				return false;
			}
			
		}

		//default
		return false;
	}

	boolean equal(String a, String b) {
		return a.toLowerCase().hashCode() == b.toLowerCase().hashCode();
	}
}
