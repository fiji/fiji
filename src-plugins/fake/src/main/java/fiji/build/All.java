package fiji.build;

import java.util.List;

class All extends Rule {
	All(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, prerequisites);
	}

	public void action() throws FakeException {
	}

	public boolean checkUpToDate() {
		return false;
	}

	@Override
	public All copy() {
		All copy = new All(parser, target, prerequisites);
		copy.prerequisiteString = prerequisiteString;
		return copy;
	}
}