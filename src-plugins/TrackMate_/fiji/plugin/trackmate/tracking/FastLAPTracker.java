package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.SchindelinHungarianAlgorithm;

public class FastLAPTracker extends LAPTracker {

	public FastLAPTracker(final SpotCollection spots, final TrackerSettings settings) {
		super(spots, settings);
	}
	
	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new SchindelinHungarianAlgorithm();
	}

}
