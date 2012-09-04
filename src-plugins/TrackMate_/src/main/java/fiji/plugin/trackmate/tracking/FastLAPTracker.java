package fiji.plugin.trackmate.tracking;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.MunkresKuhnAlgorithm;

public class FastLAPTracker <T extends RealType<T> & NativeType<T>> extends LAPTracker<T> {
	
	public static final String TRACKER_KEY = "FAST_LAP_TRACKER";
	public static final String NAME ="LAP Tracker";
	public static final String INFO_TEXT = "<html>" +
			"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
			"Its implementation is adapted from the following paper: <br>" +
			"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
			"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
			"<p>" +
			"Tracking happens in 2 steps: First spots are linked from frame to frame to <br>" +
			"build track segments. These track segments are investigated in a second step <br>" +
			"for gap-closing (missing detection), splitting and merging events.  <br> " +
			"<p>" +
			"Linking costs are proportional to the square distance between source and  <br> " +
			"target spots, which makes this tracker suitable for Brownian motion.  <br> " +
			"Penalties can be set to favor linking between spots that have similar  <br> " +
			"features. " +
			"<p>" +
			"Solving the LAP relies on the Munkres-Kuhn solver, <br> " +
			"that solves an assignment problem in O(n^3) instead of O(n^4)." +
			" </html>";	
	
	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new MunkresKuhnAlgorithm();
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}
}
