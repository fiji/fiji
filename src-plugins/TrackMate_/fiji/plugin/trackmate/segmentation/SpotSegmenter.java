package fiji.plugin.trackmate.segmentation;

import java.util.List;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate_;


/**
 * Interface for Spot segmenter classes, that are able to segment spots of a given
 * estimated radius within a 2D or 3D image.
 * <p>
 * Normally, concrete implementation are not expected to be multi-threaded.
 * Indeed, the {@link TrackMate_} plugin generates one instance of the concrete 
 * implementation per thread, to process multiple frames simultaneously.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2010
 *
 */
public interface SpotSegmenter <T extends RealType<T>> extends Algorithm, Benchmark, InfoTextable {

	/**
	 * Set the image that will be segmented by this algorithm, with the settings specified
	 * in the concrete {@link SegmenterSettings} object.
	 * The calibration float array will be used to convert pixel coordinates 
	 * in physical spot coordinates. 
	 */
	public void setTarget(Image<T> image, float[] calibration, SegmenterSettings settings);
	
	/**
	 * Return the list of Spot resulting from the segmentation process. 
	 */
	public List<Spot> getResult();

	/**
	 * Create a default {@link SegmenterSettings} implementation, suitable for this concrete spot segmenter.
	 * The concrete implementation returned will be of type {@link SegmenterSettings}, but the actual instance
	 * will be one suitable for the concrete segmenter implementation.
	 */
	public SegmenterSettings createDefaultSettings();
	
	/**
	 * @return  a new instance of the concrete implementation.
	 */
	public SpotSegmenter<T> createNewSegmenter();
	
	
	/** 
	 * @return  the name of this segmenter.
	 */
	@Override
	public String toString();
}
