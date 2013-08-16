/*
   Copyright 2005, 2006 by Lars Knipping

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.siox.example;

import java.io.File;
import java.util.*;
import javax.swing.filechooser.FileFilter;

/**
 * File filter for <CODE>JFileChooser</CODE>, filtering regular files
 * according to their extensions.
 *
 * @author Lars Knipping
 * @version 1.00
 */
public class ExtensionFileFilter extends FileFilter {

	// CHANGELOG
	// 2005-11-10 1.00 initial release

	/** Lookup structure for accepted file extensions. */
	private final HashSet extensionSet;
	/** Textual description of this filter. */
	private final String descString;

	/**
	 * Constructs an file filter accepting regular files with given
	 * extension and any directories.
	 *
	 * @param descString Description of this filter to be displayed.
	 * @param extensions List of all accepted extension, without the
	 *        separating dot. The empty String can be used to accept
	 *        files without extension. Case of extension is ignored.
	 */
	public ExtensionFileFilter(String descString, String[] extensions) {
		this.descString = descString;
	extensionSet = new HashSet(Arrays.asList(extensions));
	}

	/** Returns the file extension of the given name. */
	private String getExtension(String fileName) {
		final int idx = fileName.lastIndexOf('.');
	return (idx>=0) ?  fileName.substring(idx+1) : "";
	}

	/**  Whether the file extension is found in the set of accepted ones. */
	private boolean isAcceptedFilename(String fileName) {
		return extensionSet.contains(getExtension(fileName).toLowerCase());
	}

	/**
	 * Whether the given file is regular file with recognized extension or a
	 * directory.
	 */
	public boolean accept(File file) {
		if (file.isDirectory())
			return true;
	return file.isFile() && isAcceptedFilename(file.getName());
	}

	/** The description of this filter. */
	public String getDescription() {
		return descString;
	}
}
