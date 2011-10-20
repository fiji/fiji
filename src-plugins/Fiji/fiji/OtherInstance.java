package fiji;

import ij.IJ;
import ij.ImageJ;
import ij.Prefs;

import ij.io.OpenDialog;
import ij.io.Opener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Method;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.Properties;

/*
 * This class tries to contact another instance on the same machine, started
 * by the current user.  If such an instance is found, the arguments are
 * sent to that instance.  If no such an instance is found, listen for clients.
 *
 * No need for extra security, as the stub (and its serialization) contain
 * a hard-to-guess hash code.
 */

public class OtherInstance {
	interface ImageJInstance extends Remote {
		void sendArgument(String arg) throws RemoteException;
	}

	static class Implementation implements ImageJInstance {
		int counter = 0;

		public void sendArgument(String cmd) {
			if (IJ.debugMode) IJ.log("SocketServer: command: \""+ cmd+"\"");
			if (cmd.startsWith("open "))
				(new Opener()).openAndAddToRecent(cmd.substring(5));
			else if (cmd.startsWith("macro ")) {
				String name = cmd.substring(6);
				String name2 = name;
				String arg = null;
				if (name2.endsWith(")")) {
					int index = name2.lastIndexOf("(");
					if (index>0) {
						name = name2.substring(0, index);
						arg = name2.substring(index+1, name2.length()-1);
					}
				}
				IJ.runMacroFile(name, arg);
			} else if (cmd.startsWith("run "))
				IJ.run(cmd.substring(4));
			else if (cmd.startsWith("eval ")) {
				String rtn = IJ.runMacro(cmd.substring(5));
				if (rtn!=null)
					System.out.print(rtn);
			} else if (cmd.startsWith("user.dir "))
				OpenDialog.setDefaultDirectory(cmd.substring(9));
		}
	}

	public static String getStubPath() {
		String display = System.getenv("DISPLAY");
		return System.getProperty("java.io.tmpdir") + "/ImageJ-"
			+ System.getProperty("user.name") + "-"
			+ (display == null ? "" : (display.replace(':', '_') + "-"))
			+ ImageJ.getPort() + ".stub";
	}

	public static void makeFilePrivate(String path) {
		try {
			File file = new File(path);
			file.deleteOnExit();

			// File.setReadable() is Java 6
			Class[] types = { boolean.class, boolean.class };
			Method m = File.class.getMethod("setReadable", types);
			Object[] arguments = { Boolean.FALSE, Boolean.FALSE };
			m.invoke(file, arguments);
			arguments = new Object[] { Boolean.TRUE, Boolean.TRUE };
			m.invoke(file, arguments);
			types = new Class[] { boolean.class };
			m = File.class.getMethod("setWritable", types);
			arguments = new Object[] { Boolean.FALSE };
			m.invoke(file, arguments);
			return;
		} catch (Exception e) {
			if (IJ.debugMode)
				System.err.println("Java < 6 detected,"
					+ " trying chmod 0600 " + path);
		}
		try {
			String[] command = {
				"chmod", "0600", path
			};
			Runtime.getRuntime().exec(command);
		} catch (Exception e) {
			if (IJ.debugMode)
				System.err.println("Even chmod failed.");
		}
	}

	public static boolean sendArguments(String[] args) {
		if (!isRMIEnabled())
			return false;
		String file = getStubPath();
		if (args.length > 0) try {
			FileInputStream in = new FileInputStream(file);
			ImageJInstance instance = (ImageJInstance)
				new ObjectInputStream(in).readObject();
			in.close();

			instance.sendArgument("user.dir "+System.getProperty("user.dir"));
			int macros = 0;
			for (int i=0; i<args.length; i++) {
				String arg = args[i];
				if (arg==null) continue;
				String cmd = null;
				if (macros==0 && arg.endsWith(".ijm")) {
					cmd = "macro " + arg;
					macros++;
				} else if (arg.startsWith("-macro") && i+1<args.length) {
					String macroArg = i+2<args.length?"("+args[i+2]+")":"";
					cmd = "macro " + args[i+1] + macroArg;
					instance.sendArgument(cmd);
					break;
				} else if (arg.startsWith("-eval") && i+1<args.length) {
					cmd = "eval " + args[i+1];
					args[i+1] = null;
				} else if (arg.startsWith("-run") && i+1<args.length) {
					cmd = "run " + args[i+1];
					args[i+1] = null;
				} else if (arg.indexOf("ij.ImageJ")==-1 && !arg.startsWith("-"))
					cmd = "open " + arg;
				if (cmd!=null)
					instance.sendArgument(cmd);
			} // for

			return true;
		} catch (Exception e) {
			if (IJ.debugMode) {
				System.err.println("Client exception: " + e);
				e.printStackTrace();
			}
			new File(file).delete();
		}
		if (!new File(file).exists())
			startServer();
		return false;
	}

	static ImageJInstance stub;
	static Implementation implementation;

	public static void startServer() {
		if (IJ.debugMode)
			System.err.println("Starting server");
		try {
			implementation = new Implementation();
			stub = (ImageJInstance)
				UnicastRemoteObject.exportObject(implementation,
						0);

			// Write serialized object
			String path = getStubPath();
			FileOutputStream out = new FileOutputStream(path);
			makeFilePrivate(path);
			new ObjectOutputStream(out).writeObject(stub);
			out.close();

			if (IJ.debugMode)
				System.err.println("Server ready");
		} catch (Exception e) {
			if (IJ.debugMode) {
				System.err.println("Server exception: " + e);
				e.printStackTrace();
			}
		}
	}

	public static final String ENABLE_RMI = "enable.rmi.listener";

	public static boolean isRMIEnabled() {
		Properties ijProps = loadPrefs();
		if (ijProps == null)
			return true;
		Object result = ijProps.get("." + ENABLE_RMI);
		if (result == null)
			return true;
		String string = result.toString();
		return !string.equalsIgnoreCase("true") && !string.equals("1");
	}

	protected static Properties loadPrefs() {
		Properties result = new Properties();
		File file = new File(getPrefsDirectory(), "IJ_Prefs.txt");
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(file));
			result.load(in);
			in.close();
		} catch (IOException e) { /* ignore */ }
		return result;
	}

	protected static String getPrefsDirectory() {
		String env = System.getenv("IJ_PREFS_DIR");
		if (env != null)
			return env;
		String prefsDir = System.getProperty("user.home");
		if (System.getProperty("os.name").startsWith("Mac"))
			prefsDir += "/Library/Preferences";
		else
			prefsDir += "/.imagej";
		return prefsDir;
	}
}
