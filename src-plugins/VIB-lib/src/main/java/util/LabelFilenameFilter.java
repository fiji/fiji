/*
 * LabelFilenameFilter.java
 * 
 * Created on Nov 7, 2007, 5:58:26 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import java.io.File;
import java.io.FilenameFilter;

class LabelFilenameFilter implements FilenameFilter {

        String extension;

	public LabelFilenameFilter(String extension) {
                if( extension == null )
                        this.extension = ".labels";
                else
            		this.extension = extension;
	}

	public boolean accept(File directory, String name) {
		return name.endsWith(extension);
	}

}
