package fiji.build;

import java.util.ArrayList;

public abstract class Special extends Rule {
	Special(Parser parser, String target) {
		super(parser, target, new ArrayList<String>());
	}

	boolean checkUpToDate() {
		return false;
	}
}