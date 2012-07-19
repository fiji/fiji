package fiji.plugin.trackmate.tracking;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


public class SimpleLAPTracker <T extends RealType<T> & NativeType<T>> extends LAPTracker<T> {

	@Override
	public String toString() {
		return "Simple LAP tracker";
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
				"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
				"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
				"in having non-branching tracks." +
				" </html>";
	}
	
	@Override
	public TrackerSettings<T> createDefaultSettings() {
		LAPTrackerSettings<T> ts = new LAPTrackerSettings<T>();
		ts.setUseSimpleConfigPanel(true);
		ts.allowMerging = false;
		ts.allowSplitting = false;
		return ts;
	}
	
}
