package fiji.plugin.trackmate.tracking;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class SimpleFastLAPTracker <T extends RealType<T> & NativeType<T>> extends FastLAPTracker<T> {

	public static final String TRACKER_KEY = "SIMPLE_FAST_LAP_TRACKER";
	public static final String NAME = "Simple LAP tracker";
	public static final String INFO_TEXT =  "<html>" +
			"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
			"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
			"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
			"in having non-branching tracks." +
			" </html>";	
	
	
	public SimpleFastLAPTracker() {
		super();
	}
	
	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

}
