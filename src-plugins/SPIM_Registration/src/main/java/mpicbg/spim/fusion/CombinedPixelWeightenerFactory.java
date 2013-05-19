package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.imglib.Factory;
import mpicbg.spim.registration.ViewDataBeads;

public interface CombinedPixelWeightenerFactory<I extends CombinedPixelWeightener<I>> extends Factory
{
	public abstract String getDescriptiveName();
	public I createInstance( ArrayList<ViewDataBeads> views );
}
