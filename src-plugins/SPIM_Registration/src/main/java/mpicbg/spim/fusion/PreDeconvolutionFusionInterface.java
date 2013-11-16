package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.postprocessing.deconvolution.ExtractPSF;

public interface PreDeconvolutionFusionInterface 
{
	public Image<FloatType> getFusedImage( final int index );
	public Image<FloatType> getWeightImage( final int index );
	public Image<FloatType> getOverlapImage();
	public ArrayList< Image< FloatType > > getPointSpreadFunctions();
	public ExtractPSF getExtractPSFInstance();
}
