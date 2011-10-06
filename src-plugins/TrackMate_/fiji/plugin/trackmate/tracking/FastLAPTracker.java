package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.SchindelinHungarianAlgorithm;

public class FastLAPTracker extends LAPTracker {

	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new SchindelinHungarianAlgorithm();
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This tracker is identical to the LAP tracker, expect that it <br>" +
				"uses Johannes Schindelin implementation of the Hungarian solver, <br> " +
				"that solves an assignment problem in O(n^3) instead of O(n^4)." +
				" </html>";	
	}

}
