package plugin;

import ij.IJ;
import ij.plugin.PlugIn;

public class Stitching_Grid implements PlugIn
{
	public static int defaultGridChoice1 = 0;
	public static int defaultGridChoice2 = 0;
	
	@Override
	public void run( String arg0 ) 
	{
		final GridType grid = new GridType();
		
		
		
		IJ.log( grid.getType() +  " "  + grid.getOrder() );
		
		if ( grid.getType() == -1 || grid.getOrder() == -1 )
			return;
	}
	
	
}
 