package fiji.build;

import java.io.File;

import java.util.Collections;
import java.util.List;

public class ExecuteProgram extends Rule {
	String program;

	ExecuteProgram(Parser parser, String target, List<String> prerequisites, String program) {
		super(parser, target, prerequisites);
		this.program = program;
	}

	public String getArgv0() {
		int space = program.indexOf(' ');
		return space < 0 ? program : program.substring(0, space);
	}

	public Iterable<Rule> getDependencies() throws FakeException {
		String prereq = null;
		String argv0 = getArgv0();
		if (argv0.endsWith(".py"))
			prereq = "jars/jython.jar";
		else if (argv0.endsWith(".bsh"))
			prereq = "jars/bsh.jar";
		Rule rule = prereq == null ? null : parser.getRule(prereq);
		if (rule == null)
			return super.getDependencies();
		return new MultiIterable<Rule>(super.getDependencies(), Collections.<Rule>singleton(rule));
	}

	boolean checkUpToDate() {
		boolean result = super.checkUpToDate();

		if (!result || prerequisites.size() == 0)
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
		if (program.startsWith("../../fiji --ant") &&
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

	protected void clean(boolean dryRun) {
		if (!"".equals(program))
			super.clean(dryRun);
	}
}