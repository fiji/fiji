package mpicbg.stitching;

import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.models.TileConfiguration;

public class TileConfigurationStitching extends TileConfiguration
{
	protected Tile< ? > worstTile = null;

	final public Tile getWorstTile() {	return worstTile; }

	/**
	 * Estimate min/max/average displacement of all
	 * {@link PointMatch PointMatches} in all {@link Tile Tiles}.
	 */
	@Override
	protected void updateErrors()
	{
		double cd = 0.0;
		minError = Double.MAX_VALUE;
		maxError = 0.0;
		for ( Tile< ? > t : tiles )
		{
			t.updateCost();
			double d = t.getDistance();
			if ( d < minError ) minError = d;
			
			// >= because if they are all 0.0, worstTile would be null
			if ( d >= maxError )
			{
				maxError = d;
				worstTile = t;
			}
			cd += d;
		}
		cd /= tiles.size();
		error = cd;

	}
	
}
