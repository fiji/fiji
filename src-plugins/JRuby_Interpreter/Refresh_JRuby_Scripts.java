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
		try {
			// runScript(InputStream) will close the stream
			runScript(new FileInputStream(filename));
		} catch( IOException e ) {
			throw new RuntimeException("Couldn't open the script: "+filename);
		}
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream) {
		runScript(istream, "");
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream, String filename) {
		System.out.println("Starting JRuby in runScript()...");
		Ruby rubyRuntime = Ruby.newInstance(System.in, new PrintStream(super.out), new PrintStream(super.err));
		System.out.println("Done.");
		rubyRuntime.evalScriptlet(JRuby_Interpreter.getStartupScript());

		try {
			rubyRuntime.runFromMain(istream, filename);
		} catch( Throwable t ) {
			printError(t);
		} finally {
			try {
				istream.close();
			} catch (Exception e) {
				System.out.println("JRuby runScript could not close the stream!");
				e.printStackTrace();
			}
		}

		// Undesirably this throws an exception, so just let the 
		// JRuby runtime get finalized whenever...

		// rubyRuntime.evalScriptlet("exit");
	}
}
