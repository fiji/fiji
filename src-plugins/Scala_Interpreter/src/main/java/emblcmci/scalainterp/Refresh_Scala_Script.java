/** Refresh-Scala_Script.java
 * 
 * ImageJ plugin for loading scala scripts in defined folder (Scripts)
 * under Fiji root. 
 * Loads scala script file (.scala file) and executes.
 *  
 * Kota Miura (miura@embl)
 * http://cmci.embl.de
 * Nov 13, 2012 -
 * 
 * Sample Scala script
 * ----
	import ij._
	println ("Hello from external script :-)")
	IJ.log("test ij")
	IJ.log(IJ.getVersion())
	val imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif")
	imp.show()
	IJ.wait(2000)
	imp.close()
 * 
 */
package emblcmci.scalainterp;

import ij.IJ;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import scala.collection.immutable.List;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;

import common.RefreshScripts;

/**
 * @author miura
 *
 */
public class Refresh_Scala_Script extends RefreshScripts {

	IMain imain = null;
	
    public void run(String arg) {
        setLanguageProperties(".scala", "Scala");
        setVerbose(false);
        super.run(arg);
    }

	/* 
	 * Runs .scala script in file system
	 */
	@Override
	public void runScript(String path) {
        try {
            if (!path.endsWith(".scala") || !new File(path).exists()) {
                IJ.log("Not a scala script or not found: " + path);
                return;
            }        	
            runScript(new BufferedInputStream(new FileInputStream(new File(path))));
        } catch (Throwable t) {
        	IJ.log("Refresh_Scala_Script: Failed loading" + path);
            printError(t);
        }
	}
	
	@Override
	public void runScript(InputStream arg0) {		
		String line;
		BufferedReader br= new BufferedReader(new InputStreamReader(arg0));
		StringBuilder sb = new StringBuilder();
		try {
			while ((line = br.readLine()) != null)
				sb.append(line + ";") ;
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		runScriptString(sb.toString());
		
	}
	/** Takes Scala script as a String argument and then interprets it. 
	 * 
	 * @param script
	 */
	public void runScriptString(String script){
		Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		Settings settings = new Settings();
		List<String> param = List.make(1, "true");
		settings.usejavacp().tryToSet(param);
		PrintWriter stream = new PrintWriter(System.out);
		imain = new IMain(settings, stream);
		imain.interpret(script);
	}


	/**
	 * Debugging main. 
	 * @param args
	 */
	public static void main(String[] args) {
		Refresh_Scala_Script refresh = new Refresh_Scala_Script();
		//refresh.run("");
		String path = "/Users/miura/Dropbox/codes/mavenscala/ijscalascript/scripts/helloscript.scala";
		refresh.runScript(path);
	}

}
