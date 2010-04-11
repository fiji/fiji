/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import java.io.File;

public class FileCreation {
	// Test with e.g. in JRuby:
	// include_class 'util.FileCreation'
	// f = FileCreation.makeFile( java.io.File.new("/etc"), [ "network", "interfaces" ].to_java(:String) )
	public static File makeFile( File f, String [] pathElements ) {
		for( String pathElement : pathElements ) {
			f = new File( f, pathElement );
		}
		return f;
	}
}