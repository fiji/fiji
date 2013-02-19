package fiji.build;

import java.io.File;

import java.util.ArrayList;
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

	@SuppressWarnings("unchecked")
	public Iterable<Rule> getDependencies() throws FakeException {
		Iterable<Rule> result = super.getDependencies();
		List<Rule> additional = new ArrayList<Rule>();
		String prereq = null;
		String argv0 = getArgv0();
		if (argv0.endsWith(".py"))
			prereq = "jars/jython.jar";
		else if (argv0.endsWith(".bsh"))
			prereq = "jars/bsh.jar";
		if (prereq != null) {
			Rule rule = parser.getRule(prereq);
			if (rule != null)
				additional.add(rule);
			Rule launcher = parser.getRule("ImageJ");
			if (launcher != null)
				additional.add(launcher);
			if (additional.size() > 0)
				return new MultiIterable<Rule>(result, additional);
		}
		return result;
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
		if ((program.startsWith("../../fiji --ant") || program.startsWith("../../ImageJ --ant")) &&
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

	@Override
	public ExecuteProgram copy() {
		ExecuteProgram copy = new ExecuteProgram(parser, target, prerequisites, program);
		copy.prerequisiteString = prerequisiteString;
		return copy;
	}
}