package util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * REMOVEME
 *
 * Just a very temporary method, taken from BatchLog_.java and made less
 * useful :(
 */

public class StupidLog {

	
	static String logFile = "/var/tmp/stupid.log";
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void log(String message) {
		try {
			FileWriter out = new FileWriter(logFile, true);
			String wholeMessage = "[" + dateFormat.format(new Date())
				+ "] " + message + "\n";
			out.write(wholeMessage);
			out.close();		
		} catch( IOException e ) {	
		}
	}
}

