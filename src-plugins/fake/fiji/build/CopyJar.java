package fiji.build;

import java.util.List;

public class CopyJar extends Rule {
	String source, configPath;
	CopyJar(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, prerequisites);
		source = getLastPrerequisite();
		configPath = getPluginsConfig();
	}

	void action() throws FakeException {
		copyJar(source, target, parser.cwd, configPath);
	}

	boolean checkUpToDate() {
		if (super.checkUpToDate() &&
				upToDate(configPath))
			return true;

		return parser.fake.jarUpToDate(source, target,
			getVarBool("VERBOSE"));
	}
}