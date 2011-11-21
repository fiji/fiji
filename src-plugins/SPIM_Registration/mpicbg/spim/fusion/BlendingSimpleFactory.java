package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class BlendingSimpleFactory implements CombinedPixelWeightenerFactory<BlendingSimple>
{
	final float boundary;
	
	public BlendingSimpleFactory( final float boundary )
	{
		this.boundary = boundary;
	}
	
	@Override
	public BlendingSimple createInstance( ArrayList<ViewDataBeads> views ) 
	{ 
		final BlendingSimple blending = new BlendingSimple( views );
		
		blending.setBorder( boundary );
		
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
