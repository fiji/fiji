package mpicbg.spim.mpicbg;

import mpicbg.models.TileConfiguration;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewStructure;

public class TileConfigurationPhaseCorrelation extends TileConfiguration 
{
	int debugLevel;
	
	@Override
	protected void println( String s )
	{ 
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( s ); 
	}
	
	public TileConfigurationPhaseCorrelation( final int debugLevel )
	{
		this.debugLevel = debugLevel;
	}
}
