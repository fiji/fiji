package fiji.plugin.trackmate.tracking.kdtree;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Element;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.util.TMUtils;

public class NearestNeighborTrackerSettings<T extends RealType<T> & NativeType<T>> implements TrackerSettings<T> {

	private static final double DEFAULT_MAX_LINKING_DISTANCE = 10;
	private static final String MAX_LINKING_DISTANCE_ATTRIBUTE = "maxdistance";
		
	/**
	 * The maximal physical distance above which spot linking will be forbidden.
	 */
	public double maxLinkingDistance = DEFAULT_MAX_LINKING_DISTANCE;
	
	@Override
	public void marshall(Element element) {
		element.setAttribute(MAX_LINKING_DISTANCE_ATTRIBUTE, ""+maxLinkingDistance);
	}

	@Override
	public void unmarshall(Element element) {
		TMUtils.readDoubleAttribute(element, MAX_LINKING_DISTANCE_ATTRIBUTE, Logger.VOID_LOGGER);
	}

	@Override
	public String toString() {
		String str = "  Max linking distance: " + maxLinkingDistance + "\n";
		return str;
	}
	
}
