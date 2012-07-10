package fiji.build;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class CompileCProgram extends Rule {
	boolean linkCPlusPlus = false;

	CompileCProgram(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, prerequisites);
		if (Util.getPlatform().startsWith("win"))
			this.target += ".exe";
	}

	void action() throws FakeException {
		try {
			action(prerequisites);
		} catch (IOException e) {
			fallBackToPrecompiled(e.getMessage());
		}
	}

	void action(Iterable<String> paths)
			throws IOException, FakeException {
		List<String> out = new ArrayList<String>();
		for (String path : paths) {
			if (path.endsWith(".c")) {
				out.add(compileC(path));
			}
			else if (path.endsWith(".cxx"))
				out.add(compileCXX(path));
			else
				throw new FakeException("Cannot"
					+ " compile " + path);
		}
		link(target, out);
	}

	void addFlags(String variable, String path, List<String> arguments) throws FakeException {
		String value = parser.getVariable(variable, path, Util.getPlatform());
		arguments.addAll(Util.splitCommandLine(value));
	}

	String compileCXX(String path) throws IOException, FakeException {
		linkCPlusPlus = true;
		return compile(path, gxx(), "CXXFLAGS");
	}

	String compileC(String path) throws IOException, FakeException {
		return compile(path, gcc(), "CFLAGS");
	}

	String compile(String path, String compiler,
			String flags)
			throws IOException, FakeException {
		List<String> arguments = new ArrayList<String>();
		arguments.add(compiler);
		if (getVarBool("DEBUG"))
			arguments.add("-g");
		arguments.add("-c");
		addFlags(flags, path, arguments);
		arguments.add(path);
		try {
			parser.fake.execute(arguments, path,
				getVarBool("VERBOSE", path));
			return path.substring(0,
				path.length() - (compiler.endsWith("++") ? 4 : 2)) + ".o";
		} catch(FakeException e) {
			return error("compile", path, e);
		}
	}

	void link(String target, List<String> objects)
			throws FakeException {
		File file = new File(target);
		try {
			Util.moveFileOutOfTheWay(file);
		} catch(FakeException e) {
			file = Fake.moveToUpdateDirectory(file);
		}
		List<String> arguments = new ArrayList<String>();
		arguments.add(linkCPlusPlus ? gxx() : gcc());
		arguments.add("-o");
		arguments.add(file.getAbsolutePath());
		addFlags("LDFLAGS", target, arguments);
		arguments.addAll(objects);
		addFlags("LIBS", target, arguments);
		try {
			parser.fake.execute(arguments, target,
				getVarBool("VERBOSE", target));
		} catch(Exception e) {
			error("link", target, e);
		}
	}

	String gcc() {
		return getenv("CC", "gcc");
	}

	String gxx() {
		return getenv("CXX", "g++");
	}

	String getenv(String key, String fallback) {
		String value = System.getenv(key);
		return value == null ? fallback : value;
	}

	void fallBackToPrecompiled(String reason)
			throws FakeException {
		String precompiled =
			getVar("PRECOMPILEDDIRECTORY");
		if (precompiled == null)
			error(reason);
		parser.fake.err.println("Falling back to copying "
			+ target + " from " + precompiled);
		File file = new File(Util.makePath(parser.cwd, target));
		if (!precompiled.endsWith("/"))
			precompiled += "/";
		String source =
			precompiled + file.getName();
		if (!new File(source).exists()) {
			if (Util.getPlatform().startsWith("win")) {
				int len = source.length();
				source = source.substring(0,
						len - 4) +
					"-" + Util.getPlatform() +
					".exe";
			}
			else
				source += "-" + Util.getPlatform();
		}
		Util.moveFileOutOfTheWay(Util.makePath(parser.cwd, target));
		Util.copyFile(source, target, parser.cwd);
		if (!Util.getPlatform().startsWith("win")) try {
			/* avoid Java6-ism */
			Runtime.getRuntime().exec(new String[]
				{ "chmod", "0755", target});
		} catch (Exception e) { /* ignore */ }
	}

	String error(String action, String file, Exception e)
			throws FakeException {
		throw new FakeException("Could not " + action
			+ " " + file + ": " + e);
	}

	@Override
	public CompileCProgram copy() {
		CompileCProgram copy = new CompileCProgram(parser, target, prerequisites);
		copy.prerequisiteString = prerequisiteString;
		return copy;
	}
}