package mpicbg.spim.fusion;

import mpicbg.imglib.Factory;
import mpicbg.spim.registration.ViewStructure;

public interface CombinedPixelWeightenerFactory<I extends CombinedPixelWeightener<I>> extends Factory
{
	public abstract String getDescriptiveName();
	public I createInstance( ViewStructure viewStructure );
}
