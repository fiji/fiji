package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.InfoTextable;

public enum TrackerType implements InfoTextable {
	SIMPLE_LAP_TRACKER,
	LAP_TRACKER,
	SIMPLE_FAST_LAPT,
	FAST_LAPT;

	@Override
	public String toString() {
		switch(this) {
		case SIMPLE_LAP_TRACKER:
			return "Simple LAP tracker";
		case LAP_TRACKER:
			return "LAP tracker";
		case SIMPLE_FAST_LAPT:
			return "Simple Fast LAP tracker";
		case FAST_LAPT:
			return "Fast LAP tracker";
		}
		return null;
	}

	/**
	 * Create a new {@link TrackerSettings} object suited to the {@link SpotTracker} referenced by this enum.
	 */
	public TrackerSettings createSettings() {
		final TrackerSettings ts;
		switch(this) {
		case LAP_TRACKER:
		case FAST_LAPT: {
			ts = new TrackerSettings();
			ts.trackerType = this;
			break;
		}
		case SIMPLE_LAP_TRACKER:
		case SIMPLE_FAST_LAPT: {
			ts = new TrackerSettings();
			ts.allowMerging = false;
			ts.allowSplitting = false;
			ts.trackerType = this;
			break;
		}
		default: {
			ts = null;
		}
		}
		return ts;
	}

	@Override
	public String getInfoText() {
		switch(this) {
		case LAP_TRACKER:
			return "<html>" +
			"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
			"Its implementation is derived from the following paper: <br>" +
			"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
			"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
			" </html>";
		case SIMPLE_LAP_TRACKER:
			return "<html>" +
			"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
			"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
			"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
			"in having non-branching tracks." +
			" </html>";
		case FAST_LAPT:
			return "<html>" +
			"This tracker is identical to the " + TrackerType.LAP_TRACKER.toString() + ", expect that it <br>" +
			"uses Johannes Schindelin implementation of the Hungarian solver, that solves an assignment <br>" +
			"problem in O(n^3) instead of O(n^4)." +
			" </html>";
		case SIMPLE_FAST_LAPT:
			return "<html>" +
			"This tracker is identical to the " + TrackerType.SIMPLE_LAP_TRACKER.toString() + ", expect that it <br>" +
			"uses Johannes Schindelin implementation of the Hungarian solver, that solves an assignment <br>" +
			"problem in O(n^3) instead of O(n^4)." +
			" </html>";

		}

		return null;
	}
}
