package fiji.plugin.trackmate.segmentation;

public class LogSegmenterSettings extends SegmenterSettings {

	private static final boolean	DEFAULT_ALLOW_EDGE_MAXIMA	= false;
	
	/** If true, blob found at the edge of the image will not be discarded. */
	public boolean 	allowEdgeMaxima = DEFAULT_ALLOW_EDGE_MAXIMA;
	
	@Override
	public String toString() {
		String str = super.toString();
		str += "  Allow edge maxima: "+allowEdgeMaxima+'\n';
		return str;
	}
	
}
