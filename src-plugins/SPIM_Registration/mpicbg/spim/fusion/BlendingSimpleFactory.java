package mpicbg.spim.fusion;

import ij.IJ;

import java.util.ArrayList;

import mpicbg.imglib.util.Util;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class BlendingSimpleFactory implements CombinedPixelWeightenerFactory<BlendingSimple>
{
	final float boundary;
	final float[] boundaryArray;
	
	public BlendingSimpleFactory( final float boundary )
	{
		this.boundary = boundary;
		this.boundaryArray = null;
	}
	
	public BlendingSimpleFactory( final float[] boundary )
	{
		this.boundaryArray = boundary;
		this.boundary = 0;
	}
	
	@Override
	public BlendingSimple createInstance( ArrayList<ViewDataBeads> views ) 
	{ 
		final BlendingSimple blending = new BlendingSimple( views );
		
		if ( boundaryArray == null )
			blending.setBorder( boundary );
		else
			blending.setBorder( boundaryArray );
		
		blending.setPercentScaling( 0.5f );
		
		IJ.log( "border: " + Util.printCoordinates( blending.border ) );
		IJ.log( "percent: " + blending.percentScaling );
		
		return blending;
	}
	
	@Override
	public void printProperties()
	{
		IOFunctions.println("BlendingSimpleFactory(): no special properties.");		
	}
	@Override
	public String getDescriptiveName() { return "Blending"; }
	
	@Override
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
}
