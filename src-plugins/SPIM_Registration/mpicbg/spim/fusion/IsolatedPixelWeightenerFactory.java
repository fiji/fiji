package mpicbg.spim.fusion;

import mpicbg.imglib.Factory;
import mpicbg.spim.registration.ViewDataBeads;

public interface IsolatedPixelWeightenerFactory<I extends IsolatedPixelWeightener<I>> extends Factory
{
	public abstract String getDescriptiveName();
	public I createInstance( ViewDataBeads view );
}
