package fiji.build;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class JavaCompiler {
	protected PrintStream err, out;
	protected static Method javac;

	public JavaCompiler(PrintStream err, PrintStream out) {
		this.err = err;
		this.out = out;
	}

	// this function handles the javac singleton
	public void call(String[] arguments,
			boolean verbose) throws FakeException {
		synchronized(this) {
			try {
				if (javac == null) {
					JarClassLoader loader = discoverJavac();
					String className = "com.sun.tools.javac.Main";
					Class<?> main = loader.forceLoadClass(className);
					Class<?>[] argsType = new Class[] {
						arguments.getClass(),
						PrintWriter.class
					};
					javac = main.getMethod("compile", argsType);
				}

				Object result = javac.invoke(null,
						new Object[] { arguments, new PrintWriter(err) });
				if (!result.equals(new Integer(0))) {
					FakeException e = new FakeException("Compile error");
					e.printStackTrace();
					throw e;
				}
				return;
			} catch (FakeException e) {
				/* was compile error */
				throw e;
			} catch (Exception e) {
				e.printStackTrace(err);
				err.println("Could not find javac " + e
					+ ", falling back to system javac");
			}
		}

		// fall back to calling javac
		String[] newArguments = new String[arguments.length + 1];
		newArguments[0] = "javac";
		System.arraycopy(arguments, 0, newArguments, 1,
				arguments.length);
		try {
			execute(newArguments, new File("."), verbose);
		} catch (Exception e) {
			throw new FakeException("Could not even fall back "
				+ " to javac in the PATH");
		}
	}

	protected void execute(String[] args, File dir, boolean verbose)
			throws IOException, FakeException {
		if (verbose) {
			String output = "Executing:";
			for (int i = 0; i < args.length; i++)
				output += " '" + args[i] + "'";
			err.println(output);
		}

		/* stupid, stupid Windows... */
		if (Util.getPlatform().startsWith("win")) {
			// handle .sh scripts
			if (args[0].endsWith(".sh")) {
				String[] newArgs = new String[args.length + 1];
				newArgs[0] = "sh.exe";
				System.arraycopy(args, 0, newArgs, 1, args.length);
				args = newArgs;
			}
			for (int i = 0; i < args.length; i++)
				args[i] = quoteArg(args[i]);
			// stupid, stupid, stupid Windows taking all my time!!!
			if (args[0].startsWith("../"))
				args[0] = new File(dir,
						args[0]).getAbsolutePath();
			else if ((args[0].equals("bash") || args[0].equals("sh")) && Util.getPlatform().equals("win64")) {
				String[] newArgs = new String[args.length + 2];
				newArgs[0] = System.getenv("WINDIR") + "\\SYSWOW64\\cmd.exe";
				newArgs[1] = "/C";
				System.arraycopy(args, 0, newArgs, 2, args.length);
				args = newArgs;
				if (verbose) {
					String output = "Executing (win32 on win64 using SYSWOW64\\cmd.exe):";
					for (int i = 0; i < args.length; i++)
						output += " '" + args[i] + "'";
					err.println(output);
				}

			}
		}

		Process proc = Runtime.getRuntime().exec(args, null, dir);
		new StreamDumper(proc.getErrorStream(), err).start();
		new StreamDumper(proc.getInputStream(), out).start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new FakeException(e.getMessage());
		}
		int exitValue = proc.exitValue();
		if (exitValue != 0)
			throw new FakeException("Failed: " + exitValue);
	}

	private static String quotables = " \"\'";
	public static String quoteArg(String arg) {
		return quoteArg(arg, quotables);
	}

	public static String quoteArg(String arg, String quotables) {
		for (int j = 0; j < arg.length(); j++) {
			char c = arg.charAt(j);
			if (quotables.indexOf(c) >= 0) {
				String replacement;
				if (c == '"') {
					if (System.getenv("MSYSTEM") != null)
						replacement = "\\" + c;
					else
						replacement = "'" + c + "'";
				}
				else
					replacement = "\"" + c + "\"";
				arg = arg.substring(0, j)
					+ replacement
					+ arg.substring(j + 1);
				j += replacement.length() - 1;
			}
		}
		return arg;
	}

	protected static JarClassLoader discoverJavac() throws IOException {
		File ijHome = new File(System.getProperty("ij.dir"));
		File javac = new File(ijHome, "jars/javac.jar");
		if (!javac.exists()) {
			javac = new File(ijHome, "precompiled/javac.jar");
			if (!javac.exists()) {
				System.err.println("No javac.jar found (looked in " + ijHome + ")!");
				return null;
			}
		}
		return new JarClassLoader(javac.getPath());
	}
}