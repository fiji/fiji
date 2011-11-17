package mpicbg.stitching;

import ij.ImagePlus;
import mpicbg.models.Model;
import mpicbg.models.Tile;
import mpicbg.models.TranslationModel2D;

public class ImagePlusTimePoint extends Tile implements Comparable< ImagePlusTimePoint >
{
	final ImagePlus imp;
	final int impId;
	final int timePoint, dimensionality;
	
	public ImagePlusTimePoint( final ImagePlus imp, final int impId, final int timepoint, final Model model )
	{
		super( model );
		this.imp = imp;
		this.impId = impId;
		this.timePoint = timepoint;
		
		if ( TranslationModel2D.class.isInstance( model ) )
			dimensionality = 2;
		else
			dimensionality = 3;
	}
	
	public int getImpId() { return impId; }
	public ImagePlus getImagePlus() { return imp; }
	public int getTimePoint() { return timePoint; }

	@Override
	public int compareTo( final ImagePlusTimePoint o ) 
	{
		if ( timePoint < o.timePoint )
			return -1;
		else if ( timePoint > o.timePoint )
			return 1;
		else
		{
			if ( impId < o.impId )
				return -1;
			else if ( impId > o.impId )
				return 1;
			else 
				return 0;
		}			
	}
}
