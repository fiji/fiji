package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;

/**
 * This {@link FeatureAnalyzer} computes both the 
 * <a href=http://en.wikipedia.org/wiki/Michelson_contrast#Formula>Michelson contrast</a> and the SNR for each spot:
 * <p>
 * The contrast is defined as <code>C = (I_in - I_out) / (I_in + I_out)</code> where 
 * <code>I_in</code> is the mean intensity inside the spot volume (computed from its 
 * {@link Spot#RADIUS} feature), and <code>I_out</code> is the mean intensity in a ring 
 * ranging from its radius to twice its radius.
 * <p>
 * The spots's SNR is computed a <code>(I_in - I_out) / std_in</code> where <code>std_in</code> is the standard
 * deviation computed within the spot.
 * <p>
 * <u>Important</u>: this analyzer relies on some results provided by the {@link BlobDescriptiveStatistics}
 * analyzer. Thus, it <b>must</b> be run after it.
 *  
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011 - 2012
 */
public class BlobContrastAndSNR<T extends RealType<T>> extends IndependentSpotFeatureAnalyzer<T> {

	/** The single feature key name that this analyzer computes. */
	public static final String						CONTRAST = 	"CONTRAST";
	public static final String						SNR = 		"SNR";
	public static final ArrayList<String> 			FEATURES = new ArrayList<String>(2);
	public static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(2);
	public static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(2);
	public static final HashMap<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(2);
	static {
		FEATURES.add(CONTRAST);
		FEATURES.add(SNR);
		FEATURE_NAMES.put(CONTRAST, "Contrast");
		FEATURE_NAMES.put(SNR, "Signal/Noise ratio");
		FEATURE_SHORT_NAMES.put(CONTRAST, "Constrast");
		FEATURE_SHORT_NAMES.put(SNR, "SNR");
		FEATURE_DIMENSIONS.put(CONTRAST, Dimension.NONE);
		FEATURE_DIMENSIONS.put(SNR, Dimension.NONE);
	}
	
	protected static final double RAD_PERCENTAGE = 1f;
	public static final String NAME = "Spot contrast and SNR";  
	
	
	@Override
	public void process(Spot spot) {
		double[] vals = getContrastAndSNR(spot);
		double contrast = vals[0];
		double snr = vals[1];
		spot.putFeature(CONTRAST, contrast);
		spot.putFeature(SNR, snr);
	}
	
	/**
	 * Compute the contrast for the given spot.
	 */
	protected double[] getContrastAndSNR(final Spot spot) {
		
		SpotNeighborhood<T> neighborhood = new SpotNeighborhood<T>(spot, img);
		
		final double radius = spot.getFeature(Spot.RADIUS);
		double radius2 = radius * radius;
		int n_out = 0; // inner number of pixels
		double dist2;
		double sum_out = 0;
		
		// Compute mean in the outer ring
		SpotNeighborhoodCursor<T> cursor = neighborhood.cursor();
		while(cursor.hasNext()) {
			cursor.fwd();
			dist2 = cursor.getDistanceSquared();
			if (dist2 > radius2) {
				n_out++;
				sum_out += cursor.get().getRealFloat();				
			} 
		}
		double mean_out = sum_out / n_out;
		double mean_in = spot.getFeature(BlobDescriptiveStatistics.MEAN_INTENSITY);
		double std_in  = spot.getFeature(BlobDescriptiveStatistics.STANDARD_DEVIATION);

		// Compute contrast
		double contrast = (mean_in - mean_out) / (mean_in + mean_out);
		
		// Compute snr
		double snr = (mean_in - mean_out) / std_in;
		
		final double[] ret = new double[2];
		ret[0] = contrast;
		ret[1] = snr;
		return ret;
	}
}
