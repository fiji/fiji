package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.SchindelinHungarianAlgorithm;

public class FastLAPTracker extends LAPTracker {

	public FastLAPTracker() {
		super();
	}

	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new SchindelinHungarianAlgorithm();
	}

	@Override
	public String toString() {
		return "Fast LAP Tracker";
	}
	
	@Override
	public String getInfoText() {
		return "<html>" +
		"This tracker is identical to the LAP tracker, expect that it uses <br>" +
		"Johannes Schindelin implementation of the Hungarian solver, that solves <br>" +
		"an assignment problem in O(n^3) instead of O(n^4)." +
		" </html>";
	}

}
