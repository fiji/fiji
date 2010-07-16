package mpicbg.spim.fusion;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;

public abstract class IsolatedPixelWeightener<I>
{
	final ViewDataBeads view;
	final SPIMConfiguration conf;
	int debugLevel;
	
	protected IsolatedPixelWeightener( ViewDataBeads view )
	{
		this.view = view;
		this.conf = view.getViewStructure().getSPIMConfiguration();
		this.debugLevel = view.getViewStructure().getDebugLevel();
	}	
	
	public abstract LocalizableByDimCursor<FloatType> getResultIterator();
	public abstract LocalizableByDimCursor<FloatType> getResultIterator( OutOfBoundsStrategyFactory<FloatType> factory);	
	public abstract void close();
}