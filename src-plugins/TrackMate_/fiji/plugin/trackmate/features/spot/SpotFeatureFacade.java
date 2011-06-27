package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Collection;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;

/**
 * This class aims at being a facade to simplify the calculation of spot
 * feature. Instantiating it in another class should be sufficient to compute
 * all currently implemented feature, without having to deal with the numerous
 * feature analyzers this would require.
 * @author Jean-Yves Tinevez (tinevez@pasteur.fr) Aug 27, 2010
 */
public class SpotFeatureFacade <T extends RealType<T>> {
	
	private Image<T> rawImage;
	private float[] calibration;
	/** The number of radiuses to use in the {@link RadiusEstimator} feature analyzer. */
	private int nDiameters = 10;
	/** The contrast feature analyzer. */
	private BlobContrast<T> contrast;
	/** The descriptive statistics feature estimator. */
	private BlobDescriptiveStatistics<T> descriptiveStatistics;
	/** The blob morphology estimator. */
	private BlobMorphology<T> morphology;
	/** The best radius feature estimator. */ 
	private RadiusEstimator<T> radiusEstimator;
	/** Hold all the feature analyzers this facade deals with. */
	private ArrayList<SpotFeatureAnalyzer> featureAnalyzers;
	private SpotIconGrabber<T> imageGrabber;
	
	/*
	 * CONSTRUCTORS
	 */

	public SpotFeatureFacade(Image<T> rawImage, float[] calibration) {
		this.rawImage = rawImage;
		this.calibration = calibration;
		initFeatureAnalyzer();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Return a {@link SpotFeatureAnalyzer} that can compute the given feature.
	 */
	public SpotFeatureAnalyzer getAnalyzerForFeature(SpotFeature feature) {
		switch (feature) {
		case CONTRAST:
			return contrast;
		case ELLIPSOIDFIT_AXISPHI_A:
		case ELLIPSOIDFIT_AXISPHI_B:
		case ELLIPSOIDFIT_AXISPHI_C:
		case ELLIPSOIDFIT_AXISTHETA_A:
		case ELLIPSOIDFIT_AXISTHETA_B:
		case ELLIPSOIDFIT_AXISTHETA_C:
		case ELLIPSOIDFIT_SEMIAXISLENGTH_A:
		case ELLIPSOIDFIT_SEMIAXISLENGTH_B:
		case ELLIPSOIDFIT_SEMIAXISLENGTH_C:
			return morphology;
		case ESTIMATED_DIAMETER:
			return radiusEstimator;
		case KURTOSIS:
		case SKEWNESS:
		case TOTAL_INTENSITY:
		case MEAN_INTENSITY:
		case MEDIAN_INTENSITY:
		case MIN_INTENSITY:
		case MAX_INTENSITY:
		case STANDARD_DEVIATION:
		case VARIANCE:
			return descriptiveStatistics;
		}
		return null;
	}
	
	/**
	 * Compute all features for all the spots given.
	 */
	public void processAllFeatures(Collection<Spot> spots) {
		for (SpotFeatureAnalyzer analyzer : featureAnalyzers) 
			analyzer.process(spots);
	}
	
	/**
	 * Compute all features for the spot given.
	 */
	public void processAllFeatures(Spot spot) {
		for (SpotFeatureAnalyzer analyzer : featureAnalyzers) 
			analyzer.process(spot);
	}
	
	/**
	 * Compute the given feature for all the spots given. 
	 * <p>
	 * Because a {@link SpotFeatureAnalyzer}
	 * can process multiple features in a row, multiple features might be added
	 * to the spots. However, it is ensured that the required feature will be 
	 * processed by this method call.
	 */
	public void processFeature(SpotFeature feature, Collection<? extends Spot> spots) {
		getAnalyzerForFeature(feature).process(spots);
	}
	
	/**
	 * Compute the given feature for the spot given. 
	 * <p>
	 * Because a {@link SpotFeatureAnalyzer}
	 * can process multiple features in a row, multiple features might be added
	 * to the spots. However, it is ensured that the required feature will be 
	 * processed by this method call.
	 */
	public void processFeatures(SpotFeature feature, Spot spot) {
		getAnalyzerForFeature(feature).process(spot);
	}
	
	
	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Instantiate all the feature analyzer with the images set by constructor. Should be
	 * called only once. 
	 */
	private void initFeatureAnalyzer() {
		this.contrast = new BlobContrast<T>(rawImage, calibration);
		this.descriptiveStatistics = new BlobDescriptiveStatistics<T>(rawImage, calibration);
		this.morphology = new BlobMorphology<T>(rawImage, calibration);
		this.radiusEstimator = new RadiusEstimator<T>(rawImage, nDiameters , calibration);
		this.imageGrabber = new SpotIconGrabber<T>(rawImage, calibration);
		
		featureAnalyzers = new ArrayList<SpotFeatureAnalyzer>();
		featureAnalyzers.add(descriptiveStatistics);
		featureAnalyzers.add(contrast);
		featureAnalyzers.add(morphology);
		featureAnalyzers.add(radiusEstimator);
		featureAnalyzers.add(imageGrabber);
	}
	
	
	

}
