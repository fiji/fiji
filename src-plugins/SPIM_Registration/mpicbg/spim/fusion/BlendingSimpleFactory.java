package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class BlendingSimpleFactory implements CombinedPixelWeightenerFactory<BlendingSimple>
{
	final float boundary, percentScaling;
	final float[] boundaryArray;
	
	public BlendingSimpleFactory( final float boundary, final float percentScaling )
	{
		this.boundary = boundary;
		this.boundaryArray = null;
		this.percentScaling = percentScaling;
	}
	
	public BlendingSimpleFactory( final float[] boundary, final float percentScaling )
	{
		this.boundaryArray = boundary;
		this.boundary = 0;
		this.percentScaling = percentScaling;
	}
	
	@Override
	public BlendingSimple createInstance( ArrayList<ViewDataBeads> views ) 
	{ 
		final BlendingSimple blending = new BlendingSimple( views );
		
		if ( boundaryArray == null )
			blending.setBorder( boundary );
		else
			blending.setBorder( boundaryArray );
		
		blending.setPercentScaling( percentScaling );
		
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
