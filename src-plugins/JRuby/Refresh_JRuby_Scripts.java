/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package JRuby;

import ij.IJ;

import common.RefreshScripts;
import org.jruby.*;

import java.io.*;

public class Refresh_JRuby_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".rb","Ruby");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String filename) {
		PrintStream outPS=new PrintStream(System.out);
		System.out.println("Starting JRuby in runScript()...");
		Ruby rubyRuntime = Ruby.newInstance(System.in,outPS,outPS);
		System.out.println("Done.");
		rubyRuntime.evalScriptlet(JRuby_Interpreter.getStartupScript());

		FileInputStream fis=null;
		try {
			fis = new FileInputStream(filename);
		} catch( IOException e ) {
			throw new RuntimeException("Couldn't open the script: "+filename);
		}

		try {
			rubyRuntime.runFromMain(fis,filename);
		} catch( Throwable t ) {
			printError(t);
		}

		// Undesirably this throws an exception, so just let the 
		// JRuby runtime get finalized whenever...

		// rubyRuntime.evalScriptlet("exit");
	}
}
