package fiji.plugin.trackmate.segmentation;

import java.util.List;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;


/**
 * Interface for Spot segmenter classes, that are able to segment spots of a given
 * estimated radius within a 2D or 3D image.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 27, 2010
 *
 */
public interface SpotSegmenter <T extends RealType<T>> extends Algorithm {

	/**
	 * Set the image that will be segmented by this algorithm.
	 */
	public void setImage(Image<T> image);
	
	/**
	 * Set the calibration array that will be used to convert pixel coordinates 
	 * in physical spot coordinates. 
	 */
	public void setCalibration(float[] calibration);
	
	/**
	 * Return a list of Spot resulting from the segmentation process. The spot coordinates will be
	 * calculated with respect to the top-left-lower corner of the given image.
	 */
	public List<Spot> getResult();

	/**
	 * Return a new copy of the list of Spot resulting from the segmentation process. The spot coordinates will be
	 * translated assuming the given image was cropped using the {@link Settings} given: they will
	 * be relative to the top-left-lower corner of the raw image <b>before</b> it was cropped.
	 */
	public List<Spot> getResult(Settings settings);

	
	/**
	 * Return the intermediate image used to get Spot locations.
	 * Can be of use to evaluate filtered values or debug the process. 
	 */
	public Image<T> getIntermediateImage();
	
	/**
	 * Return the current {@link SegmenterSettings} suitable for this concrete implementation. 
	 */
	public SegmenterSettings getSettings();
		
}
