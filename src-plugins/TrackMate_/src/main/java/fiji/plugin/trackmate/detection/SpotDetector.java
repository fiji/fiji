package fiji.plugin.trackmate.detection;

import java.util.List;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate_;


/**
 * Interface for Spot detector classes, that are able to segment spots of a given
 * estimated radius within a 2D or 3D image.
 * <p>
 * Normally, concrete implementation are not expected to be multi-threaded.
 * Indeed, the {@link TrackMate_} plugin generates one instance of the concrete 
 * implementation per thread, to process multiple frames simultaneously.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2012
 *
 */
public interface SpotDetector <T extends RealType<T> & NativeType<T>> extends Algorithm, Benchmark, InfoTextable {

	/**
	 * Set the image that will be segmented by this algorithm, with the settings specified
	 * in the concrete {@link DetectorSettings} object.
	 * The target {@link ImgPlus} needs to have a proper spatial calibration 
	 * for it will be used to convert pixel coordinates 
	 * in physical spot coordinates. 
	 */
	public void setTarget(ImgPlus<T> image, DetectorSettings<T> settings);
	
	/**
	 * Return the list of Spot resulting from the detection process. 
	 */
	public List<Spot> getResult();

	/**
	 * Create a default {@link DetectorSettings} implementation, suitable for this concrete spot detector.
	 * The concrete implementation returned will be of type {@link DetectorSettings}, but the actual instance
	 * will be one suitable for the concrete detector implementation.
	 */
	public DetectorSettings<T> createDefaultSettings();
	
	/**
	 * @return  a new instance of the concrete implementation.
	 */
	public SpotDetector<T> createNewDetector();
	
	
	/** 
	 * @return  the name of this detector.
	 */
	@Override
	public String toString();
}
