package fiji.build;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompileClass extends Rule {
	CompileClass(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, prerequisites);
	}

	void action() throws FakeException {
		compileJavas(prerequisites, getBuildDir(),
			new HashSet<String>(), new HashSet<String>());

		// copy class files, if necessary
		int slash = target.lastIndexOf('/') + 1;
		String destPrefix = target.substring(0, slash);

		String prefix = getLastPrerequisite();
		slash = prefix.lastIndexOf('/') + 1;
		prefix = prefix.substring(0, slash);

		Set<String> exclude = parser.fake.expandToSet(getVar("NO_COMPILE"), parser.cwd);
		for (String source : parser.fake.java2classFiles(prerequisites, parser.cwd, getBuildDir(), exclude, new HashSet<String>())) {
			if (!source.startsWith(prefix))
				continue;
			int slash2 = source.lastIndexOf('/');
			Util.copyFile(source, destPrefix +
				source.substring(slash2), parser.cwd);
		}
	}

	@Override
	public CompileClass copy() {
		CompileClass copy = new CompileClass(parser, target, prerequisites);
		copy.prerequisiteString = prerequisiteString;
		return copy;
	}
}