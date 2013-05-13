package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SpotCollection;


public class SimpleLAPTracker extends LAPTracker {

	public  static final String TRACKER_KEY = "SIMPLE_LAP_TRACKER";
	public static final String NAME = "Simple LAP tracker";
	public static final String INFO_TEXT = "<html>" +
				"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
				"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
				"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
				"in having non-branching tracks." +
				" </html>";

	public SimpleLAPTracker(SpotCollection spots, Logger logger) {
		super(spots, logger);
	}
	
	public SimpleLAPTracker(SpotCollection spots) {
		this(spots, Logger.VOID_LOGGER);
	}

	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public String getKey() {
		return TRACKER_KEY;
	}
	
}
