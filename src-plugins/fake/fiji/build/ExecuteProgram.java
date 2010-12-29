package fiji.build;

import java.io.File;
import java.io.IOException;

import java.util.List;

public class ExecuteProgram extends Rule {
	String program;

	ExecuteProgram(Parser parser, String target, List<String> prerequisites, String program) {
		super(parser, target, prerequisites);
		this.program = program;
		int space = program.indexOf(' ');
		String argv0 = space < 0 ? program : program.substring(0, space);
		if (argv0.endsWith(".py") && parser.allRules.containsKey("jars/jython.jar"))
			prerequisites.add("jars/jython.jar");
	}

	boolean checkUpToDate() {
		boolean result = super.checkUpToDate();

		if (!result)
			return result;

		/*
		 * Ignore prerequisites if none of them
		 * exist as files.
		 */
		for (String prereq : prerequisites) {
			if (new File(Util.makePath(parser.cwd, prereq)).exists())
				return true;
		}

		// special-case ant, since it's slow
		if (program.startsWith("../fiji --ant") &&
				prerequisites.size() == 0 &&
				upToDateRecursive(parser.cwd, new File(target)))
			return true;

		return false;
	}

	void action() throws FakeException {
		if (program.equals(""))
			return;
		try {
			String expanded = parser.expandVariables(program, target);
			parser.fake.execute(Util.splitCommandLine(expanded), parser.cwd,
				getVarBool("VERBOSE", program));
		} catch (Exception e) {
			if (!(e instanceof FakeException))
				e.printStackTrace();
			throw new FakeException("Program failed: '"
				+ program + "'\n" + e);
		}
	}
}
