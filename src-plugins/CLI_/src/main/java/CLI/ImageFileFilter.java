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
 * RELEASE DATE: 2004-09-30
 * AUTHOR: Albert Cardona at albert at pensament.net
 */

import java.io.File;
import java.io.FilenameFilter;

public class ImageFileFilter implements FilenameFilter {
	
	final String image_file_name;
	static final String ASTERISK = "*";

	ImageFileFilter(String image_file_name) {
		this.image_file_name = image_file_name;
	}

	ImageFileFilter() {
		image_file_name = null;
	}
	
	public boolean accept(final File dir, final String name) {

		if (new File(dir.getAbsolutePath().replace('\\', '/') + "/" + name).isHidden()) {
			return false;
		}

		final int dot_index = name.lastIndexOf('.');
		if (-1 == dot_index) {
			return false;
		}
		
		String extension = name.substring(dot_index).toLowerCase();

		if (null != image_file_name) {
			// 'open name'	if file is an image and image_file_name equals name
			if (equal(image_file_name, name))
			{
				return true;
			}
			// 'open *'	if file is an image and image_file_name is an ASTERISK
			if (equal(image_file_name, ASTERISK))
		 	{
				return true;
			}
			// 'open *name*'	if file is an image, image_file_name starts and ends with ASTERISK and name contains image_file_name
			else if (image_file_name.startsWith(ASTERISK)
			      && image_file_name.endsWith(ASTERISK)
			      && -1 != name.indexOf(image_file_name.substring(1, image_file_name.length()-1))
			    ) {
				return true;
			}
			//'open name*' or if file is an image and starts with image_file_name and ends with ASTERISK
			else if (image_file_name.endsWith(ASTERISK)
			      && name.startsWith(image_file_name.substring(0,image_file_name.length()-1))
		 	) {
				return true;
			}
			//'open *name'	or if file is an image and ends with image_file_name
			else if (image_file_name.startsWith(ASTERISK)
			      && name.endsWith(image_file_name.substring(1)) 
		 	) {
				return true;
			}
			//reject
			else {
				return false;
			}
			
		} else {
			return true;
		}
	}

	boolean equal(String a, String b) {
		return a.toLowerCase().hashCode() == b.toLowerCase().hashCode();
	}
}
