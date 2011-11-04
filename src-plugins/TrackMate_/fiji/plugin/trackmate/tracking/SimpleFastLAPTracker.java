package fiji.plugin.trackmate.tracking;

public class SimpleFastLAPTracker extends FastLAPTracker {

	public SimpleFastLAPTracker() {
		super();
	}
	
	
	@Override
	public TrackerSettings createDefaultSettings() {
		LAPTrackerSettings ts = new LAPTrackerSettings();
		ts.setUseSimpleConfigPanel(true);
		ts.allowMerging = false;
		ts.allowSplitting = false;
		return ts;
	}


	@Override
	public String toString() {
		return "Simple Fast LAP tracker";
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This tracker is identical to the Fast LAP tracker present in this plugin, except that it <br>" +
				"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
				"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
				"in having non-branching tracks." +
				" </html>";	
	}

}
