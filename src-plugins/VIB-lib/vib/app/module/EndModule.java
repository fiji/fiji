package vib.app.module;

import vib.app.gui.Console;

public class EndModule extends Module {
	public String getName() { return "EndModule"; }
	protected String getMessage() { return "Running the VIB protocol"; }
	protected boolean runsOnce() { return true; }

	protected void run(State state, int index) {
		if (index != 0)
			return;
		prereqsDone(state, index);

		new AverageBrain().runOnAllImages(state);
		new Show().runOnAllImages(state);
		console.append("VIB protocol finished.");
	}
}
