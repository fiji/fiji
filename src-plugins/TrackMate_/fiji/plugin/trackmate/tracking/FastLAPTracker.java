package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.SchindelinHungarianAlgorithm;

public class FastLAPTracker extends LAPTracker {

	public FastLAPTracker(final TrackMateModel model) {
		super(model);
	}
	
	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new SchindelinHungarianAlgorithm();
	}

}
